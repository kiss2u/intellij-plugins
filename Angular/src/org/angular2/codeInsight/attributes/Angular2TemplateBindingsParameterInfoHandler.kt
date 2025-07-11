package org.angular2.codeInsight.attributes

import com.intellij.codeInsight.CodeInsightBundle
import com.intellij.javascript.findArgumentList
import com.intellij.lang.javascript.documentation.JSHtmlHighlightingUtil
import com.intellij.lang.javascript.psi.JSArgumentsHolder
import com.intellij.lang.javascript.psi.JSFunction
import com.intellij.lang.javascript.psi.JSVariable
import com.intellij.lang.parameterInfo.ParameterInfoHandlerWithColoredSyntax
import com.intellij.lang.parameterInfo.ParameterInfoHandlerWithColoredSyntax.ParameterInfoHandlerWithColoredSyntaxData
import com.intellij.lang.parameterInfo.ParameterInfoHandlerWithColoredSyntax.SignatureHtmlPresentation
import com.intellij.lang.parameterInfo.ParameterInfoHandlerWithTabActionSupport
import com.intellij.openapi.util.TextRange
import com.intellij.polySymbols.documentation.PolySymbolDocumentationTarget
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiWhiteSpace
import com.intellij.psi.TokenType
import com.intellij.psi.tree.IElementType
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.psi.util.parentOfType
import com.intellij.util.containers.addIfNotNull
import com.intellij.xml.util.XmlUtil
import org.angular2.codeInsight.Angular2HighlightingUtils.TextAttributesKind.*
import org.angular2.codeInsight.Angular2HighlightingUtils.withColor
import org.angular2.directiveInputToTemplateBindingVar
import org.angular2.lang.expr.psi.Angular2TemplateBinding
import org.angular2.lang.html.psi.Angular2HtmlTemplateBindings
import org.angular2.web.scopes.TemplateBindingKeyScope.Companion.getDirectiveInputsFor

class Angular2TemplateBindingsParameterInfoHandler : ParameterInfoHandlerWithColoredSyntax<Angular2HtmlTemplateBindings, SignatureHtmlPresentation>(),
                                                     ParameterInfoHandlerWithTabActionSupport<Angular2HtmlTemplateBindings, ParameterInfoHandlerWithColoredSyntaxData, Angular2TemplateBinding> {

  override fun getActualParameters(o: Angular2HtmlTemplateBindings): Array<out Angular2TemplateBinding> =
    o.bindings.bindings.filter {
      it.textRange.length > 0 && it.keyKind != Angular2TemplateBinding.KeyKind.AS
    }.toTypedArray()

  override fun getActualParameterDelimiterType(): IElementType =
    TokenType.WHITE_SPACE

  override fun getActualParametersRBraceType(): IElementType =
    TokenType.WHITE_SPACE //none

  override fun getArgumentListAllowedParentClasses(): Set<Class<*>> =
    setOf(PsiElement::class.java)

  override fun getArgListStopSearchClasses(): Set<Class<*>> =
    setOf(JSFunction::class.java, JSArgumentsHolder::class.java)

  override fun getArgumentListClass(): Class<Angular2HtmlTemplateBindings> =
    Angular2HtmlTemplateBindings::class.java

  override fun findParameterOwner(file: PsiFile, offset: Int): Angular2HtmlTemplateBindings? {
    if (findArgumentList(file, offset) != null) return null
    var element = file.findElementAt(offset)
    if (element is PsiWhiteSpace) element = PsiTreeUtil.prevLeaf(element)
    return element?.parentOfType<Angular2HtmlTemplateBindings>()
  }

  override val parameterListSeparator: String
    get() = ";<br>"

  override fun buildSignaturePresentations(parameterOwner: Angular2HtmlTemplateBindings): List<SignatureHtmlPresentation> {
    val templateName = parameterOwner.templateName

    val inputDefinitions = getDirectiveInputsFor(parameterOwner.bindings)
      .associateTo(mutableMapOf()) { input ->
        val bindingName = directiveInputToTemplateBindingVar(input.name, templateName)
        val definition = PolySymbolDocumentationTarget
          .create(input, parameterOwner.bindings) { _, _ -> }
          .documentation
          .definition
          .replaceFirst("${input.name}:",
                        bindingName.withColor(NG_INPUT, parameterOwner, false) +
                        (if (input.required) "" else "?") + ":")
        Pair(input.name, definition)
      }

    val result = mutableListOf<ParameterHtmlPresentation>()
    result.addIfNotNull(
      inputDefinitions.remove(templateName)
        ?.let { ParameterHtmlPresentation("&lt;expr&gt;$it", TextRange(0, 0)) }
    )

    parameterOwner.bindings.bindings
      .mapNotNullTo(result) {
        if (it.key == templateName)
          null
        else when (it.keyKind) {
          Angular2TemplateBinding.KeyKind.LET -> {
            ParameterHtmlPresentation("let".withColor(TS_KEYWORD, parameterOwner, false) + " " + it.key.withColor(NG_TEMPLATE_VARIABLE, parameterOwner, false) +
                                      renderJSType(it.variableDefinition), it.textRange)
          }
          Angular2TemplateBinding.KeyKind.BINDING -> {
            val inputDef = inputDefinitions.remove(it.key)
            if (inputDef != null) {
              ParameterHtmlPresentation(inputDef, it.textRange)
            }
            else {
              ParameterHtmlPresentation(directiveInputToTemplateBindingVar(it.key, templateName).withColor(ERROR, parameterOwner, false), it.textRange)
            }
          }
          Angular2TemplateBinding.KeyKind.AS -> null
        }
      }

    inputDefinitions.values.mapTo(result) {
      ParameterHtmlPresentation("[$it]", null)
    }
    if (result.isEmpty())
      result.add(ParameterHtmlPresentation(XmlUtil.escape(CodeInsightBundle.message("parameter.info.no.parameters"))))
    return listOf(SignatureHtmlPresentation(result))
  }

  private fun renderJSType(variable: JSVariable?): String {
    return JSHtmlHighlightingUtil.getElementHtmlHighlighting(variable ?: return "", "name", variable.jsType ?: return "")
      .toString()
      .replaceFirst("name", "")
  }
}