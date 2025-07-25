// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package org.jetbrains.vuejs.intentions.extractComponent

import com.intellij.lang.javascript.refactoring.util.JSRefactoringUtil
import com.intellij.openapi.application.WriteAction
import com.intellij.openapi.command.CommandProcessor
import com.intellij.openapi.command.impl.StartMarkAction
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.TextRange
import com.intellij.psi.xml.XmlFile
import com.intellij.psi.xml.XmlTag
import com.intellij.refactoring.util.CommonRefactoringUtil
import com.intellij.util.PathUtilRt
import com.intellij.polySymbols.query.PolySymbolQueryExecutorFactory
import com.intellij.xml.DefaultXmlExtension
import org.jetbrains.annotations.Nls
import org.jetbrains.annotations.NonNls
import org.jetbrains.vuejs.VueBundle
import org.jetbrains.vuejs.codeInsight.fromAsset
import org.jetbrains.vuejs.codeInsight.toAsset
import org.jetbrains.vuejs.index.VUE_FILE_EXTENSION
import org.jetbrains.vuejs.intentions.extractComponent.VueComponentInplaceIntroducer.Companion.GROUP_ID
import org.jetbrains.vuejs.web.VUE_COMPONENTS

class VueExtractComponentRefactoring(
  private val project: Project,
  private val list: List<XmlTag>,
  private val editor: Editor,
) {
  fun perform(defaultName: String? = null, fireRefactoringEvents: Boolean = false) {
    if (list.isEmpty() ||
        list[0].containingFile == null ||
        list[0].containingFile.parent == null ||
        !CommonRefactoringUtil.checkReadOnlyStatus(project, list[0].containingFile)) return

    val oldText = getSelectedText()

    val data = VueExtractComponentDataBuilder(list)

    val refactoringName = VueBundle.message("vue.template.intention.extract.component.command.name")
    val commandProcessor = CommandProcessor.getInstance()
    commandProcessor.executeCommand(project, {
      if (fireRefactoringEvents)
        JSRefactoringUtil.registerRefactoringUndo(project, VueExtractComponentAction.REFACTORING_ID)

      var newlyAdded: XmlTag? = null
      val validator = TagNameValidator(list[0])
      var startMarkAction: StartMarkAction? = null
      WriteAction.run<RuntimeException> {
        startMarkAction = StartMarkAction.start(editor, project, refactoringName)
        startMarkAction!!.isGlobal = true
        newlyAdded = data.replaceWithNewTag(defaultName ?: "NewComponent") as? XmlTag
      }
      VueComponentInplaceIntroducer(newlyAdded!!, editor, data, oldText,
                                    validator::validate,
                                    startMarkAction!!,
                                    fireRefactoringEvents).performInplaceRefactoring(linkedSetOf())

    }, refactoringName, GROUP_ID)
  }

  private fun getSelectedText(): String =
    editor.document.getText(TextRange(list[0].textRange.startOffset, list[list.size - 1].textRange.endOffset))

  private class TagNameValidator(context: XmlTag) {
    private val folder = context.containingFile.parent!!
    private val forbidden: Set<String>
    private val alreadyExisting: Set<String>

    init {
      forbidden = DefaultXmlExtension.DEFAULT_EXTENSION.getAvailableTagNames(context.containingFile as XmlFile, context)
        .map { it.name }.toSet()
      alreadyExisting = PolySymbolQueryExecutorFactory.create(context)
        .codeCompletionQuery(VUE_COMPONENTS, "", 0)
        .run()
        .map { fromAsset(it.name) }
        .toSet()
    }

    fun validate(@NonNls text: String): @Nls String? {
      val normalized = fromAsset(text.trim())
      val fileName = toAsset(text.trim(), true) + VUE_FILE_EXTENSION
      if (normalized.isEmpty() || !PathUtilRt.isValidFileName(fileName, false) ||
          normalized.contains(' ') || forbidden.contains(normalized)) {
        return VueBundle.message("vue.template.intention.extract.component.error.component.name", normalized)
      }
      if (alreadyExisting.contains(normalized)) {
        return VueBundle.message("vue.template.intention.extract.component.error.component.exists", normalized)
      }
      if (folder.findFile(fileName) != null) {
        return VueBundle.message("vue.template.intention.extract.component.error.file.exists", fileName)
      }
      return null
    }
  }
}
