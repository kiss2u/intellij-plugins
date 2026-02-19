package org.jetbrains.qodana.actions

import com.intellij.notification.NotificationType
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.PlatformCoreDataKeys
import com.jetbrains.qodana.sarif.model.Result
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import org.jetbrains.annotations.VisibleForTesting
import org.jetbrains.qodana.QodanaBundle
import org.jetbrains.qodana.highlight.HighlightedReportData
import org.jetbrains.qodana.highlight.QodanaHighlightedReportService
import org.jetbrains.qodana.highlight.highlightedReportDataIfSelected
import org.jetbrains.qodana.notifications.QodanaNotifications
import org.jetbrains.qodana.problem.SarifProblem
import org.jetbrains.qodana.problem.SarifTrace
import org.jetbrains.qodana.staticAnalysis.sarif.QodanaSeverity
import org.jetbrains.qodana.ui.problemsView.tree.model.*
import org.jetbrains.qodana.ui.problemsView.tree.ui.*
import org.jetbrains.qodana.ui.problemsView.viewModel.QodanaProblemsViewModel
import javax.swing.JTree
import kotlin.io.path.createTempFile
import kotlin.io.path.writeText

typealias Selector = (SarifProblem) -> Boolean

private val json = Json {
  prettyPrint = true
  explicitNulls = false
}

@Serializable
data class ContextReport (
  val reportRevision: String?,
  val problems: List<ContextProblem>,
)


@Serializable
data class ContextReportWithPath (
  val reportRevision: String?,
  val path: String,
)

@Serializable
data class ContextProblem(
  val startLine: Int?,
  val startColumn: Int?,
  val path: String,
  val traces: Collection<SarifTrace>?,
  val message: String,
  val qodanaSeverity: QodanaSeverity,
  val inspectionId: String,
  val baselineState: Result.BaselineState,
)

fun buildContextString(e: AnActionEvent): String? {
  val project = e.project ?: return null
  val reportData = QodanaHighlightedReportService.getInstance(project).highlightedReportState.value.highlightedReportDataIfSelected
  if (reportData == null) {
    QodanaNotifications.General.notification(
      null,
      QodanaBundle.message("qodana.store.report.to.context.no.report"),
      NotificationType.WARNING,
    ).notify(project)
    return null
  }
  val selectedProblems = calcSelectedProblems(e, reportData)?.map { it.toContextProblem() }
  if (selectedProblems == null) {
    QodanaNotifications.General.notification(
      null,
      QodanaBundle.message("qodana.store.report.to.context.no.problems"),
      NotificationType.WARNING,
    ).notify(project)
    return null
  }

  val revision = reportData.vcsData.revision

  if (selectedProblems.size < 100) {
    val report = ContextReport(
      reportRevision = revision,
      problems = selectedProblems
    )
    return json.encodeToString(ContextReport.serializer(), report)
  }

  val problemsJson = json.encodeToString(selectedProblems)
  val reportPath = createTempFile("qodana-problems-context", ".json")
  reportPath.writeText(problemsJson)

  val context = ContextReportWithPath(reportRevision = revision, path = reportPath.toAbsolutePath().toString())

  return json.encodeToString(ContextReportWithPath.serializer(), context)
}

@VisibleForTesting
internal fun SarifProblem.toContextProblem(): ContextProblem {
  return ContextProblem(
    startLine,
    startColumn,
    relativePathToFile,
    traces.ifEmpty { null },
    message,
    qodanaSeverity,
    inspectionId,
    baselineState
  )
}

fun isQodanaTreeSelectionAvailable(e: AnActionEvent): Boolean {
  val isLoaded = e.qodanaProblemsViewModel?.uiStateFlow?.value is QodanaProblemsViewModel.UiState.Loaded
  val tree = e.getData(PlatformCoreDataKeys.CONTEXT_COMPONENT) as? JTree
  val hasSelection = tree?.selectionPaths?.any { it.lastPathComponent is QodanaUiTreeNode<*, *> } == true
  return isLoaded && hasSelection
}

private fun calcSelectedProblems(e: AnActionEvent, reportData: HighlightedReportData): List<SarifProblem>? {
  val tree = e.getData(PlatformCoreDataKeys.CONTEXT_COMPONENT) as? JTree ?: return null
  val selectedNodes = tree.selectionPaths?.mapNotNull { it.lastPathComponent as? QodanaUiTreeNode<*, *> } ?: return null
  if (selectedNodes.isEmpty()) return null

  return filterProblems(selectedNodes, reportData.allProblems)
}

private fun filterProblems(uiNodes: List<QodanaUiTreeNode<*, *>>, allProblems: Set<SarifProblem>): List<SarifProblem> {
  val chains = uiNodes.mapNotNull { node ->
    node.computeAncestorsAndThisModelNodes().ifEmpty { null }
  }
  return filterProblemsFromNodeChains(chains, allProblems)
}

/**
 * Filters problems based on selected model tree node chains.
 *
 * Each chain is a path from the tree root to a selected node (as returned by
 * [QodanaUiTreeNode.computeAncestorsAndThisModelNodes]).
 */
@VisibleForTesting
internal fun filterProblemsFromNodeChains(
  selectedChains: List<List<QodanaTreeNode<*, *, *>>>,
  allProblems: Set<SarifProblem>,
): List<SarifProblem> {
  if (selectedChains.any { it.lastOrNull() is QodanaTreeRoot }) {
    return allProblems.toList()
  }

  // Keep only top-level chains: remove chains nested under another selected chain
  val topChains = selectedChains.filter { chain ->
    selectedChains.none { other ->
      other !== chain && isProperPrefixByPrimaryData(other, chain)
    }
  }

  val (categoryChains, problemChains) = topChains.partition { it.lastOrNull() !is QodanaTreeProblemNode }

  val selectors = categoryChains.map { buildSelectorFromNodeChain(it) }
  val selectedProblems = allProblems.filter { problem -> selectors.any { selector -> selector(problem) } }
  val directSelectedProblems = problemChains.mapNotNull {
    (it.lastOrNull() as? QodanaTreeProblemNode)?.primaryData?.sarifProblem
  }

  return selectedProblems + directSelectedProblems
}

private fun isProperPrefixByPrimaryData(
  shorter: List<QodanaTreeNode<*, *, *>>,
  longer: List<QodanaTreeNode<*, *, *>>,
): Boolean {
  if (shorter.size >= longer.size) return false
  return shorter.indices.all { i -> shorter[i].primaryData == longer[i].primaryData }
}

/**
 * Builds a problem selector from a chain of model tree nodes (root to selected node).
 *
 * Walks the chain in reverse (selected node → root) to give priority to the most specific nodes:
 * file dominates directory which dominates module; inspection dominates category.
 */
@VisibleForTesting
internal fun buildSelectorFromNodeChain(chain: List<QodanaTreeNode<*, *, *>>): Selector {
  var pathNode: QodanaTreeNode<*, *, *>? = null
  var inspectionNode: QodanaTreeNode<*, *, *>? = null

  val filteringNodes = mutableListOf<QodanaTreeNode<*, *, *>?>()
  for (n in chain.asReversed()) {
    when (n) {
      is QodanaTreeFileNode -> pathNode = n
      is QodanaTreeDirectoryNode -> if (pathNode == null) pathNode = n
      // Path dominates module
      is QodanaTreeModuleNode -> if (pathNode == null) pathNode = n

      is QodanaTreeNodesWithoutModuleNode -> if (pathNode == null) pathNode = n
      is QodanaTreeInspectionNode -> inspectionNode = n
      // Inspection dominates category
      is QodanaTreeInspectionCategoryNode -> if (inspectionNode == null) filteringNodes.add(n)
      is QodanaTreeSeverityNode -> filteringNodes.add(n)
      is QodanaTreeRoot -> {}
    }
  }

  filteringNodes.add(pathNode)
  filteringNodes.add(inspectionNode)
  val filters = filteringNodes.filterNotNull()

  return { problem -> filters.any { it.isRelatedToProblem(problem) } }
}
