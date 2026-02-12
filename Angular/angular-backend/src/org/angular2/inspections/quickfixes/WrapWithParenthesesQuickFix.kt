package org.angular2.inspections.quickfixes

import com.intellij.codeInsight.intention.HighPriorityAction
import com.intellij.codeInspection.LocalQuickFix
import com.intellij.codeInspection.ProblemDescriptor
import com.intellij.codeInspection.util.IntentionFamilyName
import com.intellij.codeInspection.util.IntentionName
import com.intellij.lang.ecmascript6.psi.impl.ES6ImportPsiUtil
import com.intellij.lang.javascript.psi.JSBlockStatement
import com.intellij.lang.javascript.psi.JSFunctionExpression
import com.intellij.lang.javascript.psi.JSParenthesizedExpression
import com.intellij.lang.javascript.psi.impl.JSChangeUtil
import com.intellij.openapi.project.Project
import com.intellij.util.asSafely
import org.angular2.Angular2DecoratorUtil.FORWARD_REF_FUN
import org.angular2.lang.Angular2Bundle

class WrapWithParenthesesQuickFix : LocalQuickFix, HighPriorityAction {

  override fun getFamilyName(): @IntentionFamilyName String =
    Angular2Bundle.message("angular.quickfix.wrap-with-parentheses.family")

  override fun getName(): @IntentionName String =
    familyName

  override fun applyFix(project: Project, descriptor: ProblemDescriptor) {
    val block = descriptor.psiElement as? JSBlockStatement ?: return

    val wrappedBlock = JSChangeUtil.createExpressionWithContext("() => (${block.text})", block)?.psi
                         ?.asSafely<JSFunctionExpression>()
                         ?.children
                         ?.find { it is JSParenthesizedExpression }
                       ?: return

    block.replace(wrappedBlock)
  }

}