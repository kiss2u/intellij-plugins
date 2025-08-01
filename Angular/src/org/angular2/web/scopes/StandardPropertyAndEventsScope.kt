// Copyright 2000-2022 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package org.angular2.web.scopes

import com.intellij.documentation.mdn.MdnSymbolDocumentation
import com.intellij.documentation.mdn.getDomEventDocumentation
import com.intellij.polySymbols.html.StandardHtmlSymbol
import com.intellij.polySymbols.js.types.PROP_JS_TYPE
import com.intellij.javascript.web.js.WebJSTypesUtil
import com.intellij.lang.javascript.evaluation.JSTypeEvaluationLocationProvider.withTypeEvaluationLocation
import com.intellij.lang.javascript.psi.JSFile
import com.intellij.lang.javascript.psi.ecma6.TypeScriptPropertySignature
import com.intellij.lang.javascript.psi.ecmal4.JSAttributeList
import com.intellij.lang.javascript.psi.types.JSTypeSourceFactory
import com.intellij.model.Pointer
import com.intellij.openapi.project.Project
import com.intellij.polySymbols.*
import com.intellij.polySymbols.html.HTML_ELEMENTS
import com.intellij.polySymbols.js.JS_EVENTS
import com.intellij.polySymbols.js.JS_PROPERTIES
import com.intellij.polySymbols.query.PolySymbolNameMatchQueryParams
import com.intellij.polySymbols.query.PolySymbolQueryStack
import com.intellij.polySymbols.query.PolySymbolScope
import com.intellij.polySymbols.utils.PolySymbolScopeWithCache
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.psi.createSmartPointer
import com.intellij.psi.util.PsiModificationTracker
import org.angular2.Angular2Framework
import org.angular2.codeInsight.attributes.DomElementSchemaRegistry
import org.angular2.lang.html.parser.Angular2AttributeNameParser
import org.angular2.lang.types.Angular2TypeUtils
import org.angular2.web.Angular2PsiSourcedSymbol
import org.angular2.web.Angular2SymbolOrigin
import org.angular2.web.EVENT_ATTR_PREFIX

class StandardPropertyAndEventsScope(private val templateFile: PsiFile) : PolySymbolScope {

  override fun getModificationCount(): Long =
    PsiModificationTracker.getInstance(templateFile.project).modificationCount

  override fun getMatchingSymbols(
    qualifiedName: PolySymbolQualifiedName,
    params: PolySymbolNameMatchQueryParams,
    stack: PolySymbolQueryStack,
  ): List<PolySymbol> =
    if (qualifiedName.matches(HTML_ELEMENTS)) {
      listOf(HtmlElementStandardPropertyAndEventsExtension(templateFile, "", qualifiedName.name))
    }
    else emptyList()

  override fun createPointer(): Pointer<StandardPropertyAndEventsScope> {
    val templateFile = this.templateFile.createSmartPointer()
    return Pointer {
      templateFile.dereference()?.let { StandardPropertyAndEventsScope(it) }
    }
  }

  override fun equals(other: Any?): Boolean =
    other is StandardPropertyAndEventsScope
    && other.templateFile == templateFile

  override fun hashCode(): Int =
    templateFile.hashCode()

  private class HtmlElementStandardPropertyAndEventsExtension(
    templateFile: PsiFile, tagNamespace: String, tagName: String,
  ) : PolySymbolScopeWithCache<PsiFile, Pair<String, String>>(Angular2Framework.ID, templateFile.project,
                                                              templateFile, Pair(tagNamespace, tagName)), PolySymbol {

    override fun provides(qualifiedKind: PolySymbolQualifiedKind): Boolean =
      qualifiedKind == JS_PROPERTIES
      || qualifiedKind == JS_EVENTS

    override val name: String
      get() = key.second

    override val extension: Boolean
      get() = true

    override val origin: PolySymbolOrigin
      get() = Angular2SymbolOrigin.empty

    override val qualifiedKind: PolySymbolQualifiedKind
      get() = HTML_ELEMENTS

    override fun getModificationCount(): Long =
      PsiModificationTracker.getInstance(project).modificationCount

    override fun createPointer(): Pointer<HtmlElementStandardPropertyAndEventsExtension> {
      val templateFile = this.dataHolder.createSmartPointer()
      val tagName = this.key.first
      val tagNamespace = this.key.second
      return Pointer {
        templateFile.dereference()?.let { HtmlElementStandardPropertyAndEventsExtension(it, tagNamespace, tagName) }
      }
    }

    override fun initialize(consumer: (PolySymbol) -> Unit, cacheDependencies: MutableSet<Any>) {
      val templateFile = dataHolder
      val tagNamespace = key.first
      val tagName = key.second

      val typeSource = if (templateFile is JSFile)
        JSTypeSourceFactory.createTypeSource(templateFile)
      else
        Angular2TypeUtils.createJSTypeSourceForXmlElement(templateFile)
      val tagClass = withTypeEvaluationLocation(templateFile) { WebJSTypesUtil.getHtmlElementClassType(typeSource, tagName) }
      val elementEventMap = Angular2TypeUtils.getElementEventMap(typeSource).asRecordType(templateFile)

      val allowedElementProperties = DomElementSchemaRegistry.getElementProperties(tagNamespace, tagName).toMutableSet()
      val eventNames = DomElementSchemaRegistry.getElementEvents(tagNamespace, tagName).toMutableSet()
      elementEventMap.propertyNames.forEach { name -> eventNames.add(name) }

      fun addStandardProperty(name: String, project: Project, source: TypeScriptPropertySignature?) {
        propToAttrName[name]?.let { consumer(Angular2StandardProperty(it, project, source, templateFile)) }
        consumer(Angular2StandardProperty(name, project, source, templateFile))
      }

      for (property in tagClass.asRecordType(templateFile).properties) {
        val propertyDeclaration = property.memberSource.singleElement
        if (propertyDeclaration is TypeScriptPropertySignature) {
          if (propertyDeclaration.attributeList?.hasModifier(JSAttributeList.ModifierType.READONLY) == true) {
            continue
          }
          cacheDependencies.add(propertyDeclaration.containingFile)
          val name: String
          if (property.memberName.startsWith(EVENT_ATTR_PREFIX)) {
            val eventName = property.memberName.substring(2)
            eventNames.remove(eventName)
            consumer(Angular2StandardEvent(
              eventName,
              propertyDeclaration.project, propertyDeclaration,
              elementEventMap.findPropertySignature(eventName)
                ?.memberSource?.singleElement as? TypeScriptPropertySignature,
              templateFile
            ))
          }
          else {
            name = property.memberName
            if (!allowedElementProperties.remove(name)) {
              continue
            }
            addStandardProperty(name, propertyDeclaration.project, propertyDeclaration)
          }
        }
      }
      for (name in allowedElementProperties) {
        addStandardProperty(name, templateFile.project, null)
      }
      for (eventName in eventNames) {
        consumer(Angular2StandardEvent(
          eventName,
          templateFile.project,
          null,
          elementEventMap.findPropertySignature(eventName)
            ?.memberSource?.singleElement as? TypeScriptPropertySignature,
          templateFile
        ))
      }
      if (cacheDependencies.isEmpty()) {
        cacheDependencies.add(PsiModificationTracker.MODIFICATION_COUNT)
      }
    }

  }

  companion object {
    private val propToAttrName = Angular2AttributeNameParser.ATTR_TO_PROP_MAPPING.entries.associateBy({ it.value }, { it.key })
  }

  private class Angular2StandardProperty(
    override val name: String,
    override val project: Project,
    override val source: TypeScriptPropertySignature?,
    private val templateFile: PsiFile,
  ) : Angular2PsiSourcedSymbol {


    override fun createPointer(): Pointer<Angular2StandardProperty> {
      val name = name
      val project = project
      val source = source?.createSmartPointer()
      val templateFilePtr = templateFile.createSmartPointer()
      return Pointer {
        val newSource = source?.let {
          it.dereference() ?: return@Pointer null
        }
        Angular2StandardProperty(name, project, newSource,
                                 templateFilePtr.dereference() ?: return@Pointer null)
      }
    }

    override fun <T : Any> get(property: PolySymbolProperty<T>): T? =
      when (property) {
        PROP_JS_TYPE -> property.tryCast(source?.getJSType(templateFile))
        else -> super.get(property)
      }

    override val qualifiedKind: PolySymbolQualifiedKind
      get() = JS_PROPERTIES

    override fun equals(other: Any?): Boolean =
      other === this
      || other is Angular2StandardProperty
      && other.name == name
      && other.project == project
      && other.source == source
      && other.templateFile == templateFile

    override fun hashCode(): Int {
      var result = name.hashCode()
      result = 31 * result + project.hashCode()
      result = 31 * result + source.hashCode()
      result = 31 * result + templateFile.hashCode()
      return result
    }

  }

  private class Angular2StandardEvent(
    override val name: String,
    override val project: Project,
    private val mainSource: TypeScriptPropertySignature?,
    private val mapSource: TypeScriptPropertySignature?,
    private val templateFile: PsiFile,
  ) : StandardHtmlSymbol(), Angular2PsiSourcedSymbol {

    override val source: PsiElement?
      get() = mainSource ?: mapSource

    override fun getMdnDocumentation(): MdnSymbolDocumentation? =
      getDomEventDocumentation(name)

    override fun createPointer(): Pointer<Angular2StandardEvent> {
      val name = this.name
      val project = this.project
      val mainSourcePtr = mainSource?.createSmartPointer()
      val mapSourcePtr = mapSource?.createSmartPointer()
      val templateFilePtr = templateFile.createSmartPointer()
      return Pointer {
        val mainSource = mainSourcePtr?.let {
          it.dereference() ?: return@Pointer null
        }
        val mapSource = mapSourcePtr?.let {
          it.dereference() ?: return@Pointer null
        }
        Angular2StandardEvent(name, project, mainSource, mapSource,
                              templateFilePtr.dereference() ?: return@Pointer null)
      }
    }

    override fun <T : Any> get(property: PolySymbolProperty<T>): T? =
      when (property) {
        PROP_JS_TYPE -> property.tryCast(
          Angular2TypeUtils.extractEventVariableType(mainSource?.getJSType(templateFile))
          ?: mapSource?.getJSType(templateFile)
        )
        else -> super<StandardHtmlSymbol>.get(property)
      }

    override val priority: PolySymbol.Priority
      get() = PolySymbol.Priority.NORMAL

    override val qualifiedKind: PolySymbolQualifiedKind
      get() = JS_EVENTS

    override fun equals(other: Any?): Boolean =
      other === this
      || other is Angular2StandardEvent
      && other.name == name
      && other.project == project
      && other.mainSource == mainSource
      && other.mapSource == mapSource
      && other.templateFile == templateFile

    override fun hashCode(): Int {
      var result = name.hashCode()
      result = 31 * result + project.hashCode()
      result = 31 * result + mainSource.hashCode()
      result = 31 * result + mapSource.hashCode()
      result = 31 * result + templateFile.hashCode()
      return result
    }

  }

}