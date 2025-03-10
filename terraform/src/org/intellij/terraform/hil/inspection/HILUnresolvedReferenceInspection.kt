// Copyright 2000-2023 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package org.intellij.terraform.hil.inspection

import com.intellij.BundleBase
import com.intellij.codeInsight.daemon.EmptyResolveMessageProvider
import com.intellij.codeInspection.*
import com.intellij.lang.injection.InjectedLanguageManager
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.progress.ProgressIndicatorProvider
import com.intellij.openapi.progress.ProgressManager
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiElementVisitor
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiReference
import com.intellij.psi.impl.source.resolve.reference.impl.providers.FileReferenceOwner
import com.intellij.psi.util.parentOfType
import com.intellij.psi.util.parentsOfType
import com.intellij.util.containers.addIfNotNull
import com.intellij.util.containers.toArray
import org.intellij.terraform.config.actions.TfInitAction
import org.intellij.terraform.config.patterns.TfPsiPatterns.FromPropertyInMovedBlock
import org.intellij.terraform.hcl.HCLBundle
import org.intellij.terraform.hcl.HCLLanguage
import org.intellij.terraform.hcl.psi.HCLBlock
import org.intellij.terraform.hcl.psi.HCLProperty
import org.intellij.terraform.hcl.psi.HCLPsiUtil
import org.intellij.terraform.hcl.psi.common.Identifier
import org.intellij.terraform.hcl.psi.common.SelectExpression
import org.intellij.terraform.hil.HILLanguage
import org.intellij.terraform.hil.codeinsight.isResourceInstanceReference
import org.intellij.terraform.hil.codeinsight.isResourcePropertyReference
import org.intellij.terraform.hil.codeinsight.isScopeElementReference
import org.intellij.terraform.hil.psi.impl.getHCLHost
import org.intellij.terraform.hil.psi.resolve
import org.intellij.terraform.isTerraformCompatiblePsiFile
import org.intellij.terraform.opentofu.patterns.OpenTofuPatterns.EncryptionBlock
import org.jetbrains.annotations.Nls

class HILUnresolvedReferenceInspection : LocalInspectionTool() {

  private val allowedLanguages = setOf(HCLLanguage, HILLanguage)

  override fun isAvailableForFile(file: PsiFile): Boolean {
    return (file.language in allowedLanguages || isTerraformCompatiblePsiFile(file)) &&
           isTerraformCompatiblePsiFile(InjectedLanguageManager.getInstance(file.project).getTopLevelFile(file))
  }
  override fun buildVisitor(holder: ProblemsHolder, isOnTheFly: Boolean): PsiElementVisitor {
    return MyEV(holder)
  }

  companion object {
    private val LOG = Logger.getInstance(HILUnresolvedReferenceInspection::class.java)

  }

  inner class MyEV(val holder: ProblemsHolder) : PsiElementVisitor() {
    override fun visitElement(element: PsiElement) {
      if (element.parentsOfType<HCLBlock>(true).any { EncryptionBlock.accepts(it)}) return
      if (element is Identifier) return visitIdentifier(element)
      ProgressIndicatorProvider.checkCanceled()
    }

    private fun visitIdentifier(element: Identifier) {
      ProgressIndicatorProvider.checkCanceled()
      element.getHCLHost() ?: return

      val parent = element.parent as? SelectExpression<*> ?: return
      if (isFromPropertyInMovedBlock(parent)) {
        return
      }
      if (parent.from === element) {
        checkReferences(element)
      }
      else if (isScopeElementReference(element, parent)) {
        // TODO: Check scope parameter reference
        checkReferences(element)
      }
      else if (isResourceInstanceReference(element, parent)) {
        // TODO: Check and report "no such resource of type" error
        checkReferences(element)
      }
      else if (isResourcePropertyReference(element, parent)) {
        // TODO: Check and report "no such resource property" error (only if there such resource)
        checkReferences(element)
      }
    }

    private fun checkReferences(value: PsiElement) {
      doCheckRefs(value, HCLPsiUtil.getReferencesSelectAware(value))
    }

    private fun doCheckRefs(value: PsiElement, references: Array<out PsiReference>) {
      for (reference in references) {
        ProgressManager.checkCanceled()
        // In case of 'a.*.b' '*' bypasses references from 'a'
        if (reference.element !== value || isUrlReference(reference) || !hasBadResolve(reference, false)) {
          continue
        }
        val referenceRange = reference.rangeInElement

        val description = getErrorDescription(reference)
        if (referenceRange.startOffset > referenceRange.endOffset) {
          LOG.error("Reference range start offset > end offset:  $reference, start offset: ${referenceRange.startOffset}, end offset: ${referenceRange.endOffset}")
        }
        val fixes = buildList {
          if (reference is LocalQuickFixProvider) {
            addAll(reference.quickFixes.orEmpty())
          }
          addIfNotNull(TfInitAction.createQuickFixNotInitialized(reference.element))
        }
        holder.registerProblem(value, description, ProblemHighlightType.LIKE_UNKNOWN_SYMBOL, referenceRange, *fixes.toArray(LocalQuickFix.EMPTY_ARRAY))
      }
    }
  }

  private fun isFromPropertyInMovedBlock(expression: SelectExpression<*>): Boolean {
    val property = expression.parentOfType<HCLProperty>() ?: return false
    return FromPropertyInMovedBlock.accepts(property)
  }

  fun isUrlReference(reference: PsiReference): Boolean {
    return reference is FileReferenceOwner // || reference is com.intellij.xml.util.AnchorReference
  }

  @Nls
  fun getErrorDescription(reference: PsiReference): String {
    val messagePattern: String
    if (reference is EmptyResolveMessageProvider) {
      messagePattern = reference.unresolvedMessagePattern
    }
    else {
      // although the message has a parameter, it must be taken uninterpolated as it will be applied later
      @Suppress("InvalidBundleOrProperty")
      messagePattern = HCLBundle.message("hil.unresolved.reference.inspection.unresolved.reference.error.message")
    }

    @Nls var description: String
    try {
      description = BundleBase.format(messagePattern, reference.canonicalText) // avoid double formatting
    }
    catch (ex: IllegalArgumentException) {
      // unresolvedMessage provided by third-party reference contains wrong format string (e.g. {}), tolerate it
      description = messagePattern
    }

    return description
  }

  fun hasBadResolve(reference: PsiReference, checkSoft: Boolean): Boolean {
    if (!checkSoft && reference.isSoft) return false
    return resolve(reference, false, true).isEmpty()
  }
}
