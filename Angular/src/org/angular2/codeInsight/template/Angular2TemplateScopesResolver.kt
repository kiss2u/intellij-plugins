// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package org.angular2.codeInsight.template

import com.intellij.codeInsight.completion.CompletionUtil
import com.intellij.lang.injection.InjectedLanguageManager
import com.intellij.lang.javascript.psi.JSReferenceExpression
import com.intellij.psi.PsiElement
import com.intellij.psi.ResolveResult
import com.intellij.util.Processor
import org.angular2.lang.expr.Angular2Language
import org.angular2.lang.html.Angular2HtmlLanguage

object Angular2TemplateScopesResolver {

  @JvmStatic
  fun getScopes(element: PsiElement, providers: List<Angular2TemplateScopesProvider>? = null): List<Angular2TemplateScope> {
    val original = CompletionUtil.getOriginalOrSelf(element)
    if (!checkLanguage(original)) {
      return emptyList()
    }
    val expressionIsInjected = original.containingFile.language.isKindOf(Angular2Language)
    val hostElement: PsiElement?
    if (expressionIsInjected) {
      //we are working within injection
      hostElement = InjectedLanguageManager.getInstance(element.project).getInjectionHost(element)
                    ?: return emptyList()
    }
    else {
      hostElement = null
    }
    return (providers ?: Angular2TemplateScopesProvider.EP_NAME.extensionList)
      .flatMap { it.getScopes(element, hostElement) }
  }

  @JvmStatic
  fun resolve(element: PsiElement, processor: Processor<in ResolveResult>) {
    getScopes(element)
      .firstOrNull { it.resolveAllScopesInHierarchy(processor) }
  }

  @JvmStatic
  fun isImplicitReferenceExpression(expression: JSReferenceExpression): Boolean {
    return Angular2TemplateScopesProvider.EP_NAME.extensionList.any { provider -> provider.isImplicitReferenceExpression(expression) }
  }

  private fun checkLanguage(element: PsiElement): Boolean {
    return (element.language.isKindOf(Angular2Language)
            || element.language.isKindOf(Angular2HtmlLanguage
    ) || element.parent != null && (
              element.parent.language.isKindOf(Angular2Language)
              || element.parent.language.isKindOf(Angular2HtmlLanguage)))
  }
}
