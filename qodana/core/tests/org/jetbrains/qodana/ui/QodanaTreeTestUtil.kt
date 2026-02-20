package org.jetbrains.qodana.ui

import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.vfs.refreshAndFindVirtualFile
import com.jetbrains.qodana.sarif.SarifUtil
import com.jetbrains.qodana.sarif.model.SarifReport
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.emptyFlow
import org.jetbrains.qodana.highlight.HighlightedReportData
import org.jetbrains.qodana.highlight.HighlightedReportDataImpl
import org.jetbrains.qodana.problem.SarifProblemWithProperties
import org.jetbrains.qodana.report.AggregatedReportMetadata
import org.jetbrains.qodana.report.BannerContentProvider
import org.jetbrains.qodana.report.BrowserViewProvider
import org.jetbrains.qodana.report.LoadedReport
import org.jetbrains.qodana.report.NoProblemsContentProvider
import org.jetbrains.qodana.report.NotificationCallback
import org.jetbrains.qodana.report.ReportDescriptor
import org.jetbrains.qodana.report.ValidatedSarif
import org.jetbrains.qodana.ui.problemsView.tree.model.ModuleDataProvider
import org.jetbrains.qodana.ui.problemsView.tree.model.QodanaTreeContext
import org.jetbrains.qodana.ui.problemsView.tree.model.QodanaTreeRoot
import org.jetbrains.qodana.ui.problemsView.tree.model.impl.QodanaTreeRootBuilder
import org.jetbrains.qodana.ui.problemsView.viewModel.impl.QodanaTreeBuildConfiguration
import java.nio.file.Path

class QodanaTreeTestUtil(private val project: Project, private val projectDir: Path) {
  suspend fun loadProblems(reportPath: Path, treeConfiguration: QodanaTreeBuildConfiguration): QodanaTreeRoot? {
    val reportDescriptor = reportDescriptorFromSarifFile(reportPath)
    val report = reportDescriptor.loadReport(project) ?: return null
    val highlightedReportData = HighlightedReportDataImpl.create(project, reportDescriptor, report)
    return buildTreeRoot(highlightedReportData, treeConfiguration)
  }

  private fun reportDescriptorFromSarifFile(path: Path): ReportDescriptorMock {
    val sarif = SarifUtil.readReport(path)
    return ReportDescriptorMock(path.fileName.toString(), sarif)
  }

  private suspend fun buildTreeRoot(
    highlightedReportData: HighlightedReportData,
    treeConfiguration: QodanaTreeBuildConfiguration,
  ): QodanaTreeRoot {
    val sarifProblemPropertiesProvider = highlightedReportData.sarifProblemPropertiesProvider.value
    val sarifProblemsWithProperties = sarifProblemPropertiesProvider.problemsWithProperties
      .filter { if (treeConfiguration.showBaselineProblems) true else !it.problem.isInBaseline }
      .toList()

    val sarifProblemsWithVirtualFiles = computeSarifProblemsWithVirtualFiles(sarifProblemsWithProperties)

    val moduleDataProvider = if (treeConfiguration.groupByModule) {
      ModuleDataProvider.create(project, sarifProblemsWithVirtualFiles.map { it.first.problem to it.second })
    }
    else {
      null
    }
    val treeContext = QodanaTreeContext(
      treeConfiguration.groupBySeverity,
      treeConfiguration.groupByInspection,
      moduleDataProvider,
      treeConfiguration.groupByDirectory,
      null,
      project,
      highlightedReportData.inspectionsInfoProvider
    )
    return QodanaTreeRootBuilder(
      treeContext,
      sarifProblemsWithVirtualFiles,
      highlightedReportData.excludedDataFlow.value,
      QodanaTreeRoot.PrimaryData("", null, null, null)
    ).buildRoot()
  }

  private fun computeSarifProblemsWithVirtualFiles(
    sarifProblemsWithProperties: List<SarifProblemWithProperties>,
  ): List<Pair<SarifProblemWithProperties, VirtualFile>> {
    val sarifProblemFileToVirtualFile: Map<String, VirtualFile> = sarifProblemsWithProperties
      .map { it.problem }
      .distinctBy { it.relativePathToFile }
      .mapNotNull { sarifProblem ->
        val virtualFile = projectDir.resolve(sarifProblem.relativePathToFile).refreshAndFindVirtualFile() ?: return@mapNotNull null
        sarifProblem.relativePathToFile to virtualFile
      }
      .toMap()

    return sarifProblemsWithProperties.mapNotNull {
      it to (sarifProblemFileToVirtualFile[it.problem.relativePathToFile] ?: return@mapNotNull null)
    }
  }
}

private class ReportDescriptorMock(val id: String, private val report: SarifReport?) : ReportDescriptor {
  private val _isAvailableFlow: MutableSharedFlow<NotificationCallback?> = MutableSharedFlow(replay = 1)

  override val isReportAvailableFlow: Flow<NotificationCallback?> get() = _isAvailableFlow
  override val browserViewProviderFlow: Flow<BrowserViewProvider> = emptyFlow()
  override val bannerContentProviderFlow: Flow<BannerContentProvider?> = emptyFlow()
  override val noProblemsContentProviderFlow: Flow<NoProblemsContentProvider> = emptyFlow()

  override suspend fun refreshReport(): ReportDescriptor = error("must not be invoked")
  override suspend fun loadReport(project: Project) =
    report?.let { LoadedReport.Sarif(ValidatedSarif(it), AggregatedReportMetadata(emptyMap()), "") }

  override fun hashCode(): Int = id.hashCode()
  override fun equals(other: Any?): Boolean {
    if (other !is ReportDescriptorMock) return false
    return id == other.id
  }

  override fun toString(): String = id
}
