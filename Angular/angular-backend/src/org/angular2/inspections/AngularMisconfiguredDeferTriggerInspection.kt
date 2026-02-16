package org.angular2.inspections

import com.intellij.codeInspection.LocalInspectionTool
import com.intellij.codeInspection.ProblemHighlightType
import com.intellij.codeInspection.ProblemsHolder
import com.intellij.lang.javascript.psi.JSReferenceExpression
import com.intellij.modcommand.ModPsiUpdater
import com.intellij.modcommand.PsiUpdateModCommandQuickFix
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiElementVisitor
import com.intellij.psi.util.childrenSequence
import org.angular2.codeInsight.Angular2HighlightingUtils.TextAttributesKind.NG_DEFER_TRIGGER
import org.angular2.codeInsight.Angular2HighlightingUtils.withColor
import org.angular2.codeInsight.blocks.BLOCK_DEFER
import org.angular2.codeInsight.blocks.PARAMETER_ON
import org.angular2.codeInsight.blocks.PARAMETER_PREFIX_PREFETCH
import org.angular2.codeInsight.blocks.TRIGGER_HOVER
import org.angular2.codeInsight.blocks.TRIGGER_IMMEDIATE
import org.angular2.codeInsight.blocks.TRIGGER_INTERACTION
import org.angular2.codeInsight.blocks.TRIGGER_TIMER
import org.angular2.codeInsight.blocks.TRIGGER_VIEWPORT
import org.angular2.lang.Angular2Bundle
import org.angular2.lang.expr.psi.Angular2BlockParameter
import org.angular2.lang.expr.psi.Angular2DeferredTimeLiteralExpression
import org.angular2.lang.html.psi.Angular2HtmlBlock
import org.angular2.lang.html.psi.Angular2HtmlElementVisitor

class AngularMisconfiguredDeferTriggerInspection : LocalInspectionTool() {

  private class RemoveParameterFix : PsiUpdateModCommandQuickFix() {
    override fun getFamilyName(): String =
      Angular2Bundle.message("angular.inspection.defer-trigger-misconfiguration.remove-trigger.fix")

    override fun applyFix(project: Project, element: PsiElement, updater: ModPsiUpdater) {
      element.delete()
    }
  }

  override fun buildVisitor(holder: ProblemsHolder, isOnTheFly: Boolean): PsiElementVisitor =
    object : Angular2HtmlElementVisitor() {

      override fun visitBlock(block: Angular2HtmlBlock) {
        if (block.name != BLOCK_DEFER) return

        val mains = block.parameters.filter { it.prefix == null && it.name == PARAMETER_ON }
        val prefetches = block.parameters.filter { it.prefix == PARAMETER_PREFIX_PREFETCH && it.name == PARAMETER_ON }

        // 'on immediate' dominance
        val immediateMain = mains.find { it.triggerName == TRIGGER_IMMEDIATE }
        if (immediateMain != null) {
          val immediateTriggerName = TRIGGER_IMMEDIATE.withColor(NG_DEFER_TRIGGER, block)
          mains.forEach {
            if (it != immediateMain) {
              registerRedundantTrigger(it, "angular.inspection.defer-trigger-misconfiguration.immediate-redundant-mains",
                                       immediateTriggerName)
            }
          }
          prefetches.forEach {
            registerRedundantTrigger(it, "angular.inspection.defer-trigger-misconfiguration.immediate-redundant-prefetch",
                                     immediateTriggerName)
          }
        }

        // If there is exactly one main and at least one prefetch, compare them
        if (mains.size == 1 && prefetches.isNotEmpty()) {
          val main = mains[0]

          for (prefetch in prefetches) {
            checkPrefetchAgainstMain(block, main, prefetch)
          }
        }
      }

      private fun checkPrefetchAgainstMain(
        block: Angular2HtmlBlock,
        main: Angular2BlockParameter,
        prefetch: Angular2BlockParameter,
      ) {
        val mainTrigger = main.triggerName
        val prefetchTrigger = prefetch.triggerName

        if (mainTrigger == null || prefetchTrigger == null || mainTrigger != prefetchTrigger) return

        when (mainTrigger) {
          // Timer vs Timer: warn when prefetch delay >= main delay
          TRIGGER_TIMER -> {
            val mainDelay = main.timerDelay
            val prefetchDelay = prefetch.timerDelay
            if (mainDelay != null && prefetchDelay != null && prefetchDelay >= mainDelay) {
              registerRedundantTrigger(
                prefetch,
                "angular.inspection.defer-trigger-misconfiguration.timer-prefetch-not-before-main",
                formatTimerCall(prefetchDelay, block), formatTimerCall(mainDelay, block)
              )
            }
          }

          // Reference-based triggers: only warn if both have a reference and references are identical
          TRIGGER_HOVER, TRIGGER_INTERACTION, TRIGGER_VIEWPORT -> {
            val mainRef = main.referenceArgument
            val prefetchRef = prefetch.referenceArgument
            if (mainRef != null && prefetchRef != null && mainRef.referenceName == prefetchRef.referenceName) {
              registerRedundantTrigger(
                prefetch,
                "angular.inspection.defer-trigger-misconfiguration.prefetch-same-as-main",
                prefetchTrigger.withColor(NG_DEFER_TRIGGER, block)
              )
            }
          }

          // Syntactic identical: same trigger type for immediate/idle/never etc.
          else ->
            registerRedundantTrigger(
              prefetch,
              "angular.inspection.defer-trigger-misconfiguration.prefetch-same-as-main",
              prefetchTrigger.withColor(NG_DEFER_TRIGGER, block)
            )
        }
      }

      private fun formatTimerCall(delay: Int, context: PsiElement): String =
        "${TRIGGER_TIMER.withColor(NG_DEFER_TRIGGER, context)} (${delay}ms)"

      private fun registerRedundantTrigger(parameter: Angular2BlockParameter, messageKey: String, vararg args: Any) {
        holder.registerProblem(
          parameter,
          Angular2Bundle.htmlMessage(messageKey, *args),
          ProblemHighlightType.LIKE_UNUSED_SYMBOL,
          RemoveParameterFix()
        )
      }

      private val Angular2BlockParameter.triggerName: String?
        get() = if (name == PARAMETER_ON && block?.name == BLOCK_DEFER)
          childrenSequence.firstNotNullOfOrNull { it as? JSReferenceExpression }?.referenceName
        else
          null

      private val Angular2BlockParameter.timerDelay: Int?
        get() {
          val timeExpr = childrenSequence
            .firstNotNullOfOrNull { it as? Angular2DeferredTimeLiteralExpression }
          // Parse the text to extract numeric value (e.g., "100ms" -> 100, "2s" -> 2000)
          val text = timeExpr?.text ?: return null
          val numericPart = text.replace(Regex("[^0-9]"), "").toIntOrNull() ?: return null
          return when {
            text.endsWith("s") && !text.endsWith("ms") -> numericPart * 1000
            else -> numericPart // default is ms
          }
        }

      private val Angular2BlockParameter.triggerReference: JSReferenceExpression?
        get() = childrenSequence.firstNotNullOfOrNull { it as? JSReferenceExpression }

      private val Angular2BlockParameter.referenceArgument: JSReferenceExpression?
        get() {
          val trigger = triggerReference ?: return null
          return this.children
            .lastOrNull { it is JSReferenceExpression && it != trigger } as? JSReferenceExpression
        }

    }
}
