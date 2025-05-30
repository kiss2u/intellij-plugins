// Copyright 2000-2025 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package org.intellij.terraform.config.inspection

import com.intellij.codeInsight.completion.CodeCompletionHandlerBase
import com.intellij.codeInsight.completion.CompletionType
import com.intellij.codeInspection.*
import com.intellij.modcommand.ActionContext
import com.intellij.modcommand.ModPsiUpdater
import com.intellij.modcommand.PsiUpdateModCommandAction
import com.intellij.modcommand.PsiUpdateModCommandQuickFix
import com.intellij.openapi.application.WriteAction
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiElementVisitor
import com.intellij.psi.PsiFile
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.util.ThrowableRunnable
import com.intellij.util.text.UniqueNameGenerator
import org.intellij.terraform.config.codeinsight.TfInsertHandlerService
import org.intellij.terraform.config.codeinsight.TfModelHelper
import org.intellij.terraform.config.patterns.TfPsiPatterns
import org.intellij.terraform.config.refactoring.TfElementRenameValidator
import org.intellij.terraform.hcl.HCLBundle
import org.intellij.terraform.hcl.psi.HCLBlock
import org.intellij.terraform.hcl.psi.HCLElementVisitor
import org.intellij.terraform.hcl.psi.HCLStringLiteral
import org.intellij.terraform.isTerraformCompatiblePsiFile

class TfBlockNameValidnessInspection : LocalInspectionTool() {

  override fun isAvailableForFile(file: PsiFile): Boolean {
    return isTerraformCompatiblePsiFile(file)
  }

  override fun buildVisitor(holder: ProblemsHolder, isOnTheFly: Boolean): PsiElementVisitor {
    return MyEV(holder)
  }

  override fun getID(): String {
    return "BlockNameValidness"
  }

  override fun getBatchSuppressActions(element: PsiElement?): Array<SuppressQuickFix> {
    return super.getBatchSuppressActions(PsiTreeUtil.getParentOfType(element, HCLBlock::class.java, false))
  }

  inner class MyEV(private val holder: ProblemsHolder) : HCLElementVisitor() {
    override fun visitStringLiteral(o: HCLStringLiteral) {
      val parent = o.parent as? HCLBlock ?: return
      if (parent.nameIdentifier !== o) return
      if (o.value.isBlank()) {
        holder.registerProblem(o, HCLBundle.message("block.name.validness.inspection.block.name.should.not.be.empty.error.message"),
                               ProblemHighlightType.GENERIC_ERROR_OR_WARNING)
      }
    }

    override fun visitBlock(o: HCLBlock) {
      if (!TfPsiPatterns.TerraformConfigFile.accepts(o.containingFile)) return
      val validator = TfElementRenameValidator()
      val identifier = o.nameIdentifier
      if (identifier != null && validator.pattern.accepts(o)) {
        if (!validator.isInputValid(o.name, o)) {
          holder.registerProblem(identifier, HCLBundle.message("block.name.validness.inspection.invalid.name.error.message"),
                                 ProblemHighlightType.GENERIC_ERROR_OR_WARNING, RenameBlockFix)
        }
      }

      val type = TfModelHelper.getAbstractBlockType(o) ?: return
      val nameElements = o.nameElements

      val required = (type.args + 1) - nameElements.size
      if (required == 0) return
      if (required > 0) {
        val range = TextRange(nameElements.first().startOffsetInParent, nameElements.last().let { it.startOffsetInParent + it.textLength })
        holder.registerProblem(o, HCLBundle.message("block.name.validness.inspection.missing.block.name.error.message", required),
                               ProblemHighlightType.GENERIC_ERROR_OR_WARNING, range, AddNameElementsQuickFix(o))
      }
      else {
        val extra = nameElements.toList().takeLast(-1 * required)
        val range = TextRange(extra.first().startOffsetInParent, extra.last().let { it.startOffsetInParent + it.textLength })
        holder.problem(o, HCLBundle.message("block.name.validness.inspection.extra.block.name.error.message"))
          .range(range).fix(RemoveExtraNameElementsQuickFix(o)).register()
      }
    }
  }

  class AddNameElementsQuickFix(element: HCLBlock) : LocalQuickFixAndIntentionActionOnPsiElement(element) {
    override fun getText(): String = HCLBundle.message("block.name.validness.inspection.add.name.quick.fix.name")
    override fun getFamilyName(): String = text
    override fun startInWriteAction(): Boolean = false

    override fun invoke(project: Project, psiFile: PsiFile, editor: Editor?, startElement: PsiElement, endElement: PsiElement) {
      val block = startElement as? HCLBlock ?: return
      if (editor == null) return
      if (!TfPsiPatterns.TerraformConfigFile.accepts(block.containingFile)) return
      val type = TfModelHelper.getAbstractBlockType(block) ?: return
      val nameElements = block.nameElements
      val required = (type.args + 1) - nameElements.size
      if (required <= 0) return

      WriteAction.run(ThrowableRunnable {
        val offset = nameElements.last().let { it.textOffset + it.textLength }
        editor.caretModel.moveToOffset(offset)
        TfInsertHandlerService.addArguments(required, editor)
        editor.caretModel.moveToOffset(offset + required * 3 - 1)
      })
      CodeCompletionHandlerBase.createHandler(CompletionType.BASIC).invokeCompletion(project, editor)
    }
  }

  class RemoveExtraNameElementsQuickFix(element: HCLBlock) : PsiUpdateModCommandAction<HCLBlock>(element) {
    override fun getFamilyName(): String = HCLBundle.message("block.name.validness.inspection.remove.name.quick.fix.name")

    override fun invoke(context: ActionContext, block: HCLBlock, updater: ModPsiUpdater) {
      val obj = block.`object` ?: return
      if (!TfPsiPatterns.TerraformConfigFile.accepts(block.containingFile)) return
      val type = TfModelHelper.getAbstractBlockType(block) ?: return
      val extra = block.nameElements.size - (type.args + 1)
      if (extra <= 0) return
      val toRemove = block.nameElements.toList().takeLast(extra)
      var end = obj.prevSibling!!
      if (end.textContains('\n')) end = toRemove.last()
      block.deleteChildRange(toRemove.first(), end)
    }
  }

  private object RenameBlockFix : PsiUpdateModCommandQuickFix() {
    override fun getFamilyName(): String = HCLBundle.message("block.name.validness.inspection.rename.block.quick.fix.name")

    override fun applyFix(project: Project, element: PsiElement, updater: ModPsiUpdater) {
      val block = (element as? HCLStringLiteral)?.parent as? HCLBlock ?: return
      val newName = UniqueNameGenerator.generateUniqueName("new_name") { name -> TfElementRenameValidator.isInputValid(name) }
      updater.rename(block, listOf(newName))
    }
  }
}


