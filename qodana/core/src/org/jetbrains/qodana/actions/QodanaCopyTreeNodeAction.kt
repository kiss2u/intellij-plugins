package org.jetbrains.qodana.actions

import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.ide.CopyPasteManager
import com.intellij.openapi.project.DumbAwareAction
import com.intellij.platform.ide.progress.withBackgroundProgress
import kotlinx.coroutines.launch
import org.jetbrains.qodana.QodanaBundle
import org.jetbrains.qodana.coroutines.QodanaDispatchers
import org.jetbrains.qodana.coroutines.qodanaProjectScope
import java.awt.datatransfer.StringSelection

class QodanaCopyTreeNodeAction : DumbAwareAction() {
  override fun getActionUpdateThread(): ActionUpdateThread = ActionUpdateThread.EDT

  override fun update(e: AnActionEvent) {
    e.presentation.isEnabledAndVisible = isQodanaTreeSelectionAvailable(e)
  }

  override fun actionPerformed(e: AnActionEvent) {
    val project = e.project ?: return
    project.qodanaProjectScope.launch(QodanaDispatchers.Default) {
      withBackgroundProgress(project, QodanaBundle.message("qodana.store.report.to.context.progress.title")) {
        val jsonString = buildContextString(e) ?: return@withBackgroundProgress
        CopyPasteManager.getInstance().setContents(StringSelection(jsonString))
      }
    }
  }
}
