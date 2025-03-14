@file:Suppress("LoopToCallChain")

import com.google.gson.JsonParser
import com.intellij.aws.cloudformation.CloudFormationConstants.awsServerless20161031TransformName
import com.intellij.aws.cloudformation.metadata.*
import com.intellij.aws.cloudformation.tests.TestUtil
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.jsoup.nodes.Element
import java.io.IOException
import java.net.URL
import java.util.*
import java.util.zip.GZIPInputStream

object ResourceTypesSaver {
  private const val FETCH_TIMEOUT_MS = 10000

  fun saveResourceTypes() {
    TestUtil.getTestDataFile("../metadata-package/build/jar/com/intellij/aws/meta").mkdirs()

    val limits = fetchLimits()
    val resourceTypeLocations = fetchResourceTypeLocations()
    val predefinedParameters = fetchPredefinedParameters()

    val unsupportedTypes = setOf( // broken links on website
      "AWS::M2::Application",
      "AMZN::SDC::Deployment",
      "AWS::CodeTest::PersistentConfiguration",
      "AWS::CodeTest::Series",
      "AWS::GammaDilithium::JobDefinition",
      "AWS::IoTThingsGraph::FlowTemplate"
    )

    val supportedTypeLocations = resourceTypeLocations.filter {
      !unsupportedTypes.contains(it.name)
    }

    val resourceTypes = supportedTypeLocations.pmap(numThreads = 10) {
      val location = when (it.name) {
        "AWS::CloudWatch::Dashboard" -> "https://docs.aws.amazon.com/AWSCloudFormation/latest/UserGuide/aws-properties-cw-dashboard.html"
        "AWS::ElasticBeanstalk::ConfigurationTemplate" -> "https://docs.aws.amazon.com/AWSCloudFormation/latest/UserGuide/aws-resource-beanstalk-configurationtemplate.html"
        else -> it.location
      }

      try {
        val builder = ResourceTypeBuilder(name = it.name, url = location)
        builder.transform = it.transform
        fetchResourceType(builder)

        // Check everything is set
        builder.toResourceType()
        builder.toResourceTypeDescription()

        builder
      } catch (e: Throwable) {
        throw Exception("Unable to parse resource type ${it.name} from $location: ${e.message}", e)
      }
    }

    val metadata = CloudFormationMetadata(
      resourceTypes = TreeMap(resourceTypes.associate { Pair(it.name, it.toResourceType()) }),
      predefinedParameters = predefinedParameters,
      limits = limits
    )

    val descriptions = CloudFormationResourceTypesDescription(
        resourceTypes = resourceTypes.associate { Pair(it.name, it.toResourceTypeDescription()) }
    )

    TestUtil.getTestDataFile("../metadata-package/build/jar/com/intellij/aws/meta/cloudformation-metadata.xml")
      .outputStream().use { outputStream -> MetadataSerializer.toXML(metadata, outputStream) }
    TestUtil.getTestDataFile("../metadata-package/build/jar/com/intellij/aws/meta/cloudformation-descriptions.xml")
      .outputStream().use { outputStream -> MetadataSerializer.toXML(descriptions, outputStream) }
  }

  private fun downloadDocumentHandlingPartialFiles(url: URL): Document {
    val doc = downloadDocument(url)

    if (doc.select("div").any { it.attr("id") == "main-col-body" }) {
      return doc
    }

    val partialLocation = doc.location().removeSuffix(".html") + ".partial.html"
    val partialDoc = downloadDocument(URL(partialLocation))

    if (partialDoc.select("div").any { it.attr("id") == "main-col-body" }) {
      return partialDoc
    }

    error("Could not fetch a valid AWS document page $url from both $doc and $partialDoc")
  }

  private fun downloadDocument(url: URL): Document {
    println("Downloading $url")
    for (retry in 1..4) {
      try {
        return Jsoup.parse(url, FETCH_TIMEOUT_MS)
      } catch (ignored: IOException) {
      }

      println("retry...")
    }

    throw RuntimeException("Could not download from $url")
  }

  private fun getDocumentFromUrl(url: URL): Document {
    val doc = downloadDocumentHandlingPartialFiles(url)

    // Fix all links to be absolute URLs, this helps IDEA to navigate to them (opening external browser)
    val select = doc.select("a")
    for (e in select) {
      val absUrl = e.absUrl("href").replace(".partial.html", ".html")
      e.attr("href", absUrl)
    }

    return doc
  }

  @JvmStatic
  fun main(args: Array<String>) {
    val builder = ResourceTypeBuilder(
        "AWS::Cognito::UserPool",
        "https://docs.aws.amazon.com/AWSCloudFormation/latest/UserGuide/aws-resource-cognito-userpool.html")
    fetchResourceType(builder)
  }

  private fun fetchResourceType(builder: ResourceTypeBuilder) {
    println(builder.name)

    val doc = getDocumentFromUrl(URL(builder.url))

    val descriptionElement = doc.select("div").single { it.attr("id") == "main-col-body" }
    descriptionElement.getElementsByAttributeValueMatching("id", "language-filter").forEach { it.remove() }
    descriptionElement.getElementsByAttributeValueMatching("summary", "Breadcrumbs").forEach { it.remove() }
    builder.description = cleanupHtml(descriptionElement.toString())

    val vlists = doc.select("div.variablelist")
    if (!vlists.isEmpty()) {
      for (vlist in vlists) {
        var cur: Element? = vlist
        while (cur != null) {
          if (cur.tagName() == "h2") {
            break
          }
          cur = cur.previousElementSibling()
        }

        if (cur == null) continue
        assert(cur.tagName() == "h2")

        val sectionTitle = cur.text()

        if (sectionTitle.equals("Return Value", true) || sectionTitle.equals("Return Values", true)) {
          for (term in vlist.select("span.term")) {
            if (term.parent()?.parent()?.parent() !== vlist) {
              continue
            }

            val descr = term.parent()?.nextElementSibling()
            var name = term.text()

            if (name == "DomainArn (deprecated)") {
              name = "DomainArn"
            }

            builder.addAttribute(name).description = descr?.text()
          }

          continue
        }

        if ("Properties" != sectionTitle && "Members" != sectionTitle) {
          error("Unknown section $sectionTitle")
        }

        for (term in vlist.select("span.term")) {
          if (term.parent()?.parent()?.parent() !== vlist) {
            continue
          }

          val descr = term.parent()?.nextElementSibling()
          //descr.children().
          val descrElements = descr?.select("p")

          val name = term.text()

          val href = term.previousElementSibling() ?: term.parent()
          assert(href?.tagName() == "a" || href?.tagName() == "dt")
          assert(href?.hasAttr("id") ?: false)

          val propertyId = href?.attr("id")
          assert(propertyId?.contains(name, ignoreCase = true)?:false) {
            "Property anchor id ($propertyId) should have a property name ($name) as substring in ${builder.url}"
          }
          val docUrl = doc.location().replace(".partial.html", ".html") + "#" + propertyId

          val propertyBuilder = builder.addProperty(name)
          propertyBuilder.url = docUrl

          propertyBuilder.updateRequires = ""

          var requiredValue: String? = null
          var typeValue: String? = null

          propertyBuilder.description = ""

          if (descrElements != null) {
            for (element in descrElements) {
              if (element.parent() !== descr) {
                continue
              }

              val text = element.text()

              if (text.matches("[a-zA-Z ]+:.*".toRegex())) {
                val split = text.split(":".toRegex(), 2).toTypedArray()
                assert(split.size == 2) { text }

                val fieldName = split[0].trim { it <= ' ' }
                val fieldValue = split[1].trim { it <= ' ' }.replaceFirst("\\.$".toRegex(), "")

                when (fieldName) {
                  "Required" -> requiredValue = fieldValue
                  "Type" -> typeValue = element.toString().replace("Type:", "")
                  "Update requires" -> propertyBuilder.updateRequires = element.toString().replace("Update requires:", "")
                }
              } else {
                propertyBuilder.description = element.toString()
              }
            }
          }

          propertyBuilder.type = if (typeValue != null) {
            typeValue
          } else if (builder.name == "AWS::Redshift::Cluster" && name == "SnapshotClusterIdentifier") {
            "String"
          } else if (builder.name == "AWS::Cognito::IdentityPool" && name == "SupportedLoginProviders") {
            "String"
          } else if (builder.name == "AWS::ApiGateway::VpcLink" && name == "TargetArns") {
            "List of String"
          } else if (builder.name == "AWS::SNS::TopicPolicy" && name == "PolicyDocument") {
            "JSON or YAML"
          } else if (builder.name == "AWS::Route53::RecordSet" && name == "Region") {
            "String"
          } else {
            // TODO
//            if (resourceTypeName == "AWS::Route53::RecordSet" || name == "ScheduleExpression" && resourceTypeName == "AWS::Events::Rule") "" else {
//            }

            throw RuntimeException("Type is not found in property $name in ${builder.url}")
          }

          // TODO Handle "Required if NetBiosNameServers is specified; optional otherwise" in https://docs.aws.amazon.com/AWSCloudFormation/latest/UserGuide/aws-resource-ec2-dhcp-options.html
          // TODO Handle "Yes, for VPC security groups" in https://docs.aws.amazon.com/AWSCloudFormation/latest/UserGuide/aws-properties-ec2-security-group.html
          // TODO Handle "Can be used instead of GroupId for EC2 security groups" in https://docs.aws.amazon.com/AWSCloudFormation/latest/UserGuide/aws-properties-ec2-security-group-ingress.html
          // TODO Handle "Yes, for VPC security groups; can be used instead of GroupName for EC2 security groups" in https://docs.aws.amazon.com/AWSCloudFormation/latest/UserGuide/aws-properties-ec2-security-group-ingress.html
          // TODO Handle "Yes, for ICMP and any protocol that uses ports" in https://docs.aws.amazon.com/AWSCloudFormation/latest/UserGuide/aws-properties-ec2-security-group-ingress.html
          // TODO https://docs.aws.amazon.com/AWSCloudFormation/latest/UserGuide/aws-properties-elasticache-cache-cluster.html: If your cache cluster isn't in a VPC, you must specify this property
          // TODO https://docs.aws.amazon.com/AWSCloudFormation/latest/UserGuide/aws-properties-elasticache-cache-cluster.html: If your cache cluster is in a VPC, you must specify this property

          propertyBuilder.required =
              if (builder.name == "AWS::Batch::JobDefinition" && name == "Parameters") {
                // Most likely a documentation bug, it contradicts examples in the same article
                // see https://docs.aws.amazon.com/AWSCloudFormation/latest/UserGuide/aws-resource-batch-jobdefinition.html
                false
              } else if (builder.name == "AWS::Kinesis::Stream" && name == "StreamEncryption") {
                // Most likely a documentation bug, this property was introduced later
                // and will break existing code if it is mandatory
                false
              } else if (builder.name == "AWS::Neptune::DBCluster" && name == "IamAuthEnabled") {
                // Most likely a documentation bug, this property was introduced later
                false
              } else if (requiredValue != null) {
                if (requiredValue.equals("Yes", ignoreCase = true) ||
                    requiredValue.equals("true", ignoreCase = true)) {
                  true
                } else if (
                    requiredValue.startsWith("No", ignoreCase = true) ||
                    requiredValue.startsWith("Conditional") ||
                    requiredValue == "Required if NetBiosNameServers is specified; optional otherwise" ||
                    requiredValue == "Yes, for VPC security groups" ||
                    requiredValue == "Yes. The IamInstanceProfile and ServiceRole options are required" ||
                    requiredValue == "Can be used instead of GroupId for EC2 security groups" ||
                    requiredValue == "Yes, for VPC security groups; can be used instead of GroupName for EC2 security groups" ||
                    requiredValue == "Yes, for ICMP and any protocol that uses ports" ||
                    requiredValue == "If your cache cluster isn't in a VPC, you must specify this property" ||
                    requiredValue == "If your cache cluster is in a VPC, you must specify this property" ||
                    requiredValue == "Yes. If you specify the AuthorizerId property, specify CUSTOM for this property" ||
                    requiredValue == "Yes, for VPC security groups without a default VPC") {
                  false
                } else {
                  throw RuntimeException("Unknown value for required in property $name in ${builder.url}: $requiredValue")
                }
              } else {
                // TODO
                if (builder.name != "AWS::RDS::DBParameterGroup" &&
                    builder.name != "AWS::Route53::RecordSet" &&
                    builder.name != "AWS::EC2::EC2Fleet" &&
                    builder.name != "AWS::RDS::DBSecurityGroupIngress") {
                  throw RuntimeException("Required is not found in property $name in ${builder.url}")
                }

                false
              }

          if (builder.name == "AWS::AutoScaling::AutoScalingGroup" && name == "NotificationConfigurations") {
            builder.addProperty("NotificationConfiguration").apply {
              type = propertyBuilder.description
              description = propertyBuilder.description
              required = propertyBuilder.required
              updateRequires = propertyBuilder.updateRequires
              url = propertyBuilder.url
            }
          }
        }
      }
    } else {
        if (builder.name != "AWS::CloudFormation::WaitConditionHandle" &&
            builder.name != "AWS::SDB::Domain" &&
            builder.name != "AWS::CodeDeploy::Application" &&
            builder.name != "AWS::ECS::Cluster") {
          throw RuntimeException("No properties found in ${builder.url}")
        }
    }

    // De-facto changes not covered in documentation

    if (builder.name == "AWS::ElasticBeanstalk::Application") {
      // Not in official documentation yet, found in examples
      builder.addProperty("ConfigurationTemplates").apply {
        type = "Unknown"
        required = false
        url = ""
        updateRequires = ""
        description = ""
      }
      builder.addProperty("ApplicationVersions").apply {
        type = "Unknown"
        required = false
        url = ""
        updateRequires = ""
        description = ""
      }
    }

    if (builder.name == "AWS::IAM::AccessKey") {
      builder.addProperty("Status").required = false
    }

    if (builder.name == "AWS::RDS::DBInstance") {
      builder.addProperty("AllocatedStorage").required = false
    }

    // See #17, ToPort and FromPort are required with port-based ip protocols only, will implement special check later
    if (builder.name == "AWS::EC2::SecurityGroupEgress" || builder.name == "AWS::EC2::SecurityGroupIngress") {
      builder.addProperty("ToPort").required = false
      builder.addProperty("FromPort").required = false
    }
  }

  private val cleanElementIdPattern = Regex(""" ?id="[0-9a-f]{8}"""")
  private val cleanElementNamePattern = Regex(""" name="[0-9a-f]{8}"""")

  private fun cleanupHtml(s: String): String {
    return s.replace(cleanElementIdPattern, "").replace(cleanElementNamePattern, "")
  }

  private fun parseTable(table: Element): List<List<String>> {
    val tbody = table.getElementsByTag("tbody").first() ?: return emptyList()

    val result = ArrayList<List<String>>()
    for (tr in tbody.children()) {
      if (tr.tagName() != "tr") {
        continue
      }

      result.add(tr.children()
                   .asSequence()
                   .filter { it.tagName() == "td" }
                   .map { it.text() }
                   .toList())
    }

    return result
  }

  private data class ResourceTypeLocation(
    val name: String,
    val location: String,
    val transform: String? = null
  )

  private fun fetchResourceTypeLocations(url: String): Map<String, ResourceTypeLocation> {
    val content = try {
      URL(url).openStream().use { stream ->
        GZIPInputStream(stream).bufferedReader().readText()
      }
    } catch (t: Throwable) {
      throw IllegalStateException("Unable to fetch $url", t)
    }

    val root = JsonParser.parseString(content)

    val resourceTypes = root.asJsonObject["ResourceTypes"].asJsonObject

    return resourceTypes.entrySet()
        .mapNotNull { (key, resourceTypeJson) ->
          val documentationElement = resourceTypeJson.asJsonObject["Documentation"]
          if (documentationElement == null) {
            return@mapNotNull null
          } else {
            key to ResourceTypeLocation(
                name = key,
                location = documentationElement.asString.replace("http://", "https://")
            )
          }
        }
        .toMap()
  }

  private fun fetchResourceTypeLocations(): List<ResourceTypeLocation> {
    // from https://docs.aws.amazon.com/AWSCloudFormation/latest/UserGuide/cfn-resource-specification.html
    val urls = listOf(
//        "https://d1mta8qj7i28i2.cloudfront.net/latest/gzip/CloudFormationResourceSpecification.json", // EU (Frankfurt)
        "https://d3teyb21fexa9r.cloudfront.net/latest/gzip/CloudFormationResourceSpecification.json", // EU (Ireland)
        "https://d68hl49wbnanq.cloudfront.net/latest/gzip/CloudFormationResourceSpecification.json", // US West (N. California)
        "https://d201a2mn26r7lk.cloudfront.net/latest/gzip/CloudFormationResourceSpecification.json" //  US West (Oregon)
    )

    val standardResourceLocations = urls
      .map { fetchResourceTypeLocations(it) }
      .fold(mapOf<String, ResourceTypeLocation>()) { acc, map -> acc + map }
      .map { it.value }

    val serverlessResourceLocations = listOf(
      serverlessLocation("AWS::Serverless::Api", "https://docs.aws.amazon.com/serverless-application-model/latest/developerguide/sam-resource-api.html"),
      serverlessLocation("AWS::Serverless::Application", "https://docs.aws.amazon.com/serverless-application-model/latest/developerguide/sam-resource-application.html"),
      serverlessLocation("AWS::Serverless::Function", "https://docs.aws.amazon.com/serverless-application-model/latest/developerguide/sam-resource-function.html"),
      serverlessLocation("AWS::Serverless::HttpApi", "https://docs.aws.amazon.com/serverless-application-model/latest/developerguide/sam-resource-httpapi.html"),
      serverlessLocation("AWS::Serverless::LayerVersion", "https://docs.aws.amazon.com/serverless-application-model/latest/developerguide/sam-resource-layerversion.html"),
      serverlessLocation("AWS::Serverless::SimpleTable", "https://docs.aws.amazon.com/serverless-application-model/latest/developerguide/sam-resource-simpletable.html"),
      serverlessLocation("AWS::Serverless::StateMachine", "https://docs.aws.amazon.com/serverless-application-model/latest/developerguide/sam-resource-statemachine.html")
    )

    return (standardResourceLocations + serverlessResourceLocations)
        .sortedBy { it.name }
  }

  private fun serverlessLocation(name: String, url: String): ResourceTypeLocation {
    // Serverless types must have "Transform:" attribute
    return ResourceTypeLocation(name, url, awsServerless20161031TransformName)
  }

  private fun fetchPredefinedParameters(): List<String> {
    val url = URL("https://docs.aws.amazon.com/AWSCloudFormation/latest/UserGuide/pseudo-parameter-reference.html")
    val doc = getDocumentFromUrl(url)
    return doc.select("h2").asSequence().filter { it.attr("id").startsWith("cfn-pseudo-param") }.map { it.text() }.sorted().toList()
  }

  private fun fetchLimits(): CloudFormationLimits {
    val fnGetAttrDocUrl = URL("https://docs.aws.amazon.com/AWSCloudFormation/latest/UserGuide/cloudformation-limits.html")
    val doc = getDocumentFromUrl(fnGetAttrDocUrl)

    val tableElement = doc.select("div.table-contents").first()!!

    val table = parseTable(tableElement)

    val limits = table.filter { it.size == 4 }.associate { it[0] to it[2] }

    return CloudFormationLimits(
        maxMappings = Integer.parseInt(limits.getValue("Mappings").replace(" mappings", "")),
        maxParameters = Integer.parseInt(limits.getValue("Parameters").replace(" parameters", "")),
        maxOutputs = Integer.parseInt(limits.getValue("Outputs").replace(" outputs", ""))
    )
  }
}
