package org.jetbrains.qodana.actions

import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.PlatformCoreDataKeys
import com.intellij.openapi.ide.CopyPasteManager
import com.intellij.openapi.project.DumbAwareAction
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import org.jetbrains.qodana.highlight.HighlightedReportState
import org.jetbrains.qodana.highlight.QodanaHighlightedReportService
import org.jetbrains.qodana.report.FileReportDescriptor
import org.jetbrains.qodana.ui.problemsView.tree.ui.QodanaUiTreeDirectoryNode
import org.jetbrains.qodana.ui.problemsView.tree.ui.QodanaUiTreeFileNode
import org.jetbrains.qodana.ui.problemsView.tree.ui.QodanaUiTreeInspectionCategoryNode
import org.jetbrains.qodana.ui.problemsView.tree.ui.QodanaUiTreeInspectionNode
import org.jetbrains.qodana.ui.problemsView.tree.ui.QodanaUiTreeModuleNode
import org.jetbrains.qodana.ui.problemsView.tree.ui.QodanaUiTreeNode
import org.jetbrains.qodana.ui.problemsView.tree.ui.QodanaUiTreeNodesWithoutModuleNode
import org.jetbrains.qodana.ui.problemsView.tree.ui.QodanaUiTreeProblemNode
import org.jetbrains.qodana.ui.problemsView.tree.ui.QodanaUiTreeRoot
import org.jetbrains.qodana.ui.problemsView.tree.ui.QodanaUiTreeSeverityNode
import org.jetbrains.qodana.ui.problemsView.viewModel.QodanaProblemsViewModel
import java.awt.datatransfer.StringSelection
import javax.swing.JTree
import kotlin.io.path.invariantSeparatorsPathString

@Serializable
data class SelectedContext(
  val qodanaSarifReportPath: String,
  val selectedProblemCategories: List<Attributes>?,
  val selectedProblems: List<Problem>?,
  val description: String,
)

@Serializable
data class Problem(
  val rule: String,
  val ruleName: String? = null,
  val path: String? = null,
  val message: String,
  val startLine: Int? = null,
  val startColumn: Int? = null,
  val baselineState: String,
)

@Serializable
data class Attributes(
  val severity: String? = null,
  val categoryId: String? = null,
  val inspectionId: String? = null,
  val moduleName: String? = null,
  val path: String? = null,
)

class QodanaCopyTreeNodeAction : DumbAwareAction() {
  override fun getActionUpdateThread(): ActionUpdateThread = ActionUpdateThread.EDT

  override fun update(e: AnActionEvent) {
    val isLoaded = e.qodanaProblemsViewModel?.uiStateFlow?.value is QodanaProblemsViewModel.UiState.Loaded
    val tree = e.getData(PlatformCoreDataKeys.CONTEXT_COMPONENT) as? JTree
    val hasSelection = tree?.selectionPaths?.any { it.lastPathComponent is QodanaUiTreeNode<*, *> } == true
    e.presentation.isEnabledAndVisible = isLoaded && hasSelection
  }

  override fun actionPerformed(e: AnActionEvent) {
    val project = e.project ?: return
    val tree = e.getData(PlatformCoreDataKeys.CONTEXT_COMPONENT) as? JTree ?: return
    val selectedNodes = tree.selectionPaths?.mapNotNull { it.lastPathComponent as? QodanaUiTreeNode<*, *> } ?: return
    if (selectedNodes.isEmpty()) return

    val reportState = QodanaHighlightedReportService.getInstance(project).highlightedReportState.value
    val selectedState = reportState as? HighlightedReportState.Selected ?: return
    val reportDescriptor = selectedState.highlightedReportData.sourceReportDescriptor
    val sarifReportPath = (reportDescriptor as? FileReportDescriptor)?.reportPath?.invariantSeparatorsPathString
                          ?: selectedState.highlightedReportData.reportName

    val data = buildTreeNodeData(selectedNodes, sarifReportPath)
    CopyPasteManager.getInstance().setContents(StringSelection(json.encodeToString(data)))
  }

  private fun buildTreeNodeData(uiNodes: List<QodanaUiTreeNode<*, *>>, sarifReportPath: String): SelectedContext {
    if (uiNodes.any { it is QodanaUiTreeRoot }) {
      return SelectedContext(
        qodanaSarifReportPath = sarifReportPath,
        selectedProblemCategories = null,
        selectedProblems = null,
        description = "All problems in the SARIF report are selected",
      )
    }

    val selectedSet = uiNodes.toSet()
    val topSelectedNodes = uiNodes.filter { node ->
      generateSequence(node.parent) { it.parent }.none { it in selectedSet }
    }

    val (categoryNodes, problemNodes) = topSelectedNodes.partition { it !is QodanaUiTreeProblemNode }

    val selectedProblemCategories = categoryNodes.map { toAttributes(it) }
    val selectedProblems = problemNodes.map {
      val sarifProblem = (it as QodanaUiTreeProblemNode).primaryData.sarifProblem
      Problem(
        rule = sarifProblem.inspectionId,
        path = sarifProblem.relativePathToFile,
        message = sarifProblem.message,
        startLine = sarifProblem.startLine,
        startColumn = sarifProblem.startColumn,
        baselineState = sarifProblem.baselineState.name,
      )
    }

    val description = when {
      selectedProblemCategories.isNotEmpty() && selectedProblems.isNotEmpty() ->
        "Selected ${selectedProblemCategories.size} problem categories and ${selectedProblems.size} individual problems"
      selectedProblemCategories.isNotEmpty() ->
        "Selected ${selectedProblemCategories.size} problem categories"
      selectedProblems.isNotEmpty() ->
        "Selected ${selectedProblems.size} individual problems"
      else -> ""
    }

    return SelectedContext(
      qodanaSarifReportPath = sarifReportPath,
      selectedProblemCategories = selectedProblemCategories.ifEmpty { null },
      selectedProblems = selectedProblems.ifEmpty { null },
      description = description,
    )
  }

  private fun toAttributes(node: QodanaUiTreeNode<*, *>): Attributes {
    var severity: String? = null
    var categoryId: String? = null
    var inspectionId: String? = null
    var moduleName: String? = null
    var path: String? = null

    for (n in generateSequence(node) { it.parent }) {
      when (n) {
        is QodanaUiTreeFileNode -> path = n.primaryData.file.invariantSeparatorsPathString
        is QodanaUiTreeDirectoryNode -> if (path == null) path = n.primaryData.fullPath.invariantSeparatorsPathString
        // Path dominates module
        is QodanaUiTreeModuleNode -> if (path == null) moduleName = n.primaryData.moduleData.module.name
        is QodanaUiTreeNodesWithoutModuleNode -> if (path == null) path = n.primaryData.path.invariantSeparatorsPathString
        is QodanaUiTreeInspectionNode -> inspectionId = n.primaryData.inspectionId
        // Inspection category dominates category
        is QodanaUiTreeInspectionCategoryNode -> if (categoryId == null && inspectionId == null) categoryId = n.primaryData.categoryId
        is QodanaUiTreeSeverityNode -> severity = n.primaryData.qodanaSeverity.toString()
        is QodanaUiTreeRoot -> {}
      }
    }

    return Attributes(
      severity = severity,
      categoryId = categoryId,
      inspectionId = inspectionId,
      moduleName = moduleName,
      path = path,
    )
  }
}

private val json = Json {
  prettyPrint = true
  explicitNulls = false
}
