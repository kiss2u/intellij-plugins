@file:Suppress("FunctionName", "unused")
@file:OptIn(ExperimentalSerializationApi::class)

package org.jetbrains.qodana.inspectionKts.mcp

import com.intellij.mcpserver.McpToolset
import com.intellij.mcpserver.annotations.McpDescription
import com.intellij.mcpserver.annotations.McpTool
import com.intellij.mcpserver.project
import com.intellij.openapi.application.readAction
import com.intellij.openapi.application.writeAction
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFileFactory
import com.intellij.psi.util.elementType
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.serialization.ExperimentalSerializationApi
import org.jetbrains.qodana.inspectionKts.examples.InspectionKtsExample
import org.jetbrains.qodana.inspectionKts.templates.InspectionKtsTemplate

private val ANY_LANGUAGE_TEMPLATES = setOf(
  "ANY_LANGUAGE_GLOBAL",
  "ANY_LANGUAGE",
)

private val IGNORED_EXAMPLES = setOf(
  "JSON and YAML"
)

/**
 * MCP Toolset for InspectionKts-related tools.
 * Provides tools for generating PSI trees, fetching inspection examples, and retrieving API documentation.
 */
class InspectionKtsMcpToolset : McpToolset {

  @McpTool
  @McpDescription(
    """
    Creates a PSI tree for provided Java or Kotlin code and returns it as indented text.
    Use this tool to understand the PSI structure of code snippets when writing inspections.
    The output shows element types and their hierarchy, with hints about when node.children() is needed.
    """
  )
  suspend fun generate_psi_tree(
    @McpDescription("Source code snippet to parse")
    code: String,
    @McpDescription("Programming language: 'Java' or 'Kotlin'")
    language: String,
  ): String {
    val project = currentCoroutineContext().project
    val fileExtension = when (language.lowercase()) {
      "java" -> "java"
      "kotlin", "kt" -> "kt"
      else -> return "Error: Unsupported language '$language'. Supported: Java, Kotlin"
    }

    val psiFile = writeAction {
      PsiFileFactory.getInstance(project).createFileFromText(
        "Placeholder.$fileExtension",
        code
      )
    }

    return generatePsiTreeText(psiFile)
  }

  @McpTool
  @McpDescription(
    """
    Returns example inspection.kts templates for the target language to guide code generation.
    Provides XML-wrapped examples showing how to write inspections using the InspectionKts API.
    """
  )
  suspend fun generate_inspection_kts_examples(
    @McpDescription("Target language for examples: 'Java', 'Kotlin', or 'Any' (default)")
    language: String = "Any",
    @McpDescription("If true, includes additional curated examples besides templates")
    includeAdditionalExamples: Boolean = true,
  ): String {
    val languageUpper = language.uppercase()
    val languageTemplates = ANY_LANGUAGE_TEMPLATES + languageUpper

    val templateBlocks = InspectionKtsTemplate.Provider.templates()
      .filter { it.uiDescriptor.id in languageTemplates }
      .map { it.templateContent("AnExampleFileName.ReplaceMe") }
      .joinToString(separator = "\n") { content ->
        """
        <Example>
        $content
        </Example>
        """.trimIndent()
      }

    val additionalBlocks = if (includeAdditionalExamples) {
      InspectionKtsExample.Provider
        .examples()
        .filter { it.text !in IGNORED_EXAMPLES }
        .map {
          val content = it.resourceUrl.readText()
          val comment = "// Only Code of localInspection { ... }\n"
          comment + content
        }
        .joinToString("\n") { content ->
          """
          <Example>
          $content
          </Example>
          """.trimIndent()
        }
    } else ""

    return buildString {
      appendLine("<Examples>")
      appendLine(templateBlocks)
      if (additionalBlocks.isNotBlank()) {
        appendLine(additionalBlocks)
      }
      append("</Examples>")
    }
  }

  @McpTool
  @McpDescription(
    """
    Returns the Inspection KTS API documentation for the target language.
    Provides available classes and functions that can be used when writing inspection.kts files.
    """
  )
  suspend fun generate_inspection_kts_api(
    @McpDescription("Target language: 'Java' or 'Kotlin'")
    language: String,
    @McpDescription("If true, wraps the API content in <API> and <api.kt> tags")
    wrapInTags: Boolean = true,
  ): String {
    val langName = when (language.lowercase()) {
      "java" -> "Java"
      "kotlin", "kt" -> "Kotlin"
      else -> return "Error: Unsupported language '$language'. Supported: Java, Kotlin"
    }

    val resourcePath = "apiClasses/classes$langName.txt"
    val classes = this::class.java.classLoader.getResource(resourcePath)?.readText()
                  ?: return "Error: Cannot find API documentation for $langName at $resourcePath"

    return if (wrapInTags) {
      """
      <API>
      <api.kt>
      $classes
      </api.kt>
      </API>
      """.trimIndent()
    } else {
      classes
    }
  }

  private suspend fun generatePsiTreeText(element: PsiElement, level: Int = 0): String {
    val nodeType = getNodeTypeDescription(element) ?: return ""
    val childrenInfo = getChildrenInfo(element)
    val indentation = "  ".repeat(level)

    val currentLine = "$indentation$nodeType$childrenInfo\n"

    return buildString {
      append(currentLine)

      val children = readAction { element.children.toList() }
      for (child in children) {
        append(generatePsiTreeText(child, level + 1))
      }
    }
  }

  private suspend fun getNodeTypeDescription(element: PsiElement): String? {
    return readAction {
      when {
        element.javaClass.simpleName == "LeafPsiElement" -> {
          val elementType = element.elementType
          "${elementType?.javaClass?.simpleName}($elementType)"
        }
        else -> element.javaClass.simpleName
      }
    }
  }

  private suspend fun getChildrenInfo(element: PsiElement): String {
    val hasChildren = readAction { element.children.isNotEmpty() }
    val firstChild = readAction { element.firstChild }
    val hasDirectChildren = firstChild != null

    return if (!hasDirectChildren && hasChildren) {
      " -> children retrieved with node.children()"
    } else {
      ""
    }
  }
}
