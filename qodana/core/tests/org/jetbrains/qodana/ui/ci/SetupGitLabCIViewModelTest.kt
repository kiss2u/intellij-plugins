package org.jetbrains.qodana.ui.ci

import com.intellij.openapi.application.ApplicationInfo
import com.intellij.openapi.application.invokeAndWaitIfNeeded
import com.intellij.openapi.application.edtWriteAction
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.vfs.readText
import com.intellij.testFramework.EditorTestUtil
import com.intellij.testFramework.TestDataPath
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import org.assertj.core.api.Assertions.assertThat
import org.intellij.lang.annotations.Language
import org.jetbrains.qodana.QodanaPluginHeavyTestBase
import org.jetbrains.qodana.assertSingleNotificationWithMessage
import org.jetbrains.qodana.coroutines.QodanaDispatchers
import org.jetbrains.qodana.dispatchAllTasksOnUi
import org.jetbrains.qodana.report.BannerContentProvider
import org.jetbrains.qodana.runDispatchingOnUi
import org.jetbrains.qodana.ui.ProjectVcsDataProviderMock
import org.jetbrains.qodana.ui.ci.providers.CIConfigFileState
import org.jetbrains.qodana.ui.ci.providers.gitlab.SetupGitLabCIViewModel
import kotlin.io.path.Path
import kotlin.io.path.pathString

@TestDataPath("\$CONTENT_ROOT/test-data/SetupGitLabCIViewModelTest")
class SetupGitLabCIViewModelTest : QodanaPluginHeavyTestBase() {
  private val emptyProjectVcsDataProvider = ProjectVcsDataProviderMock()
  
  override fun getBasePath(): String = Path(super.getBasePath(), "SetupGitLabCIViewModelTest").pathString

  override fun setUp() {
    super.setUp()
    setUpProject()
  }

  private fun setUpProject() {
    invokeAndWaitIfNeeded {
      copyProjectTestData(getTestName(true).trim())
    }
  }

  override fun tearDown() {
    try {
      scope.cancel()
      EditorTestUtil.releaseAllEditors()
    }
    catch (e: Throwable) {
      addSuppressedException(e)
    }
    finally {
      super.tearDown()
    }
  }

  override fun runInDispatchThread(): Boolean = false

  fun `test no yaml in project`() = runDispatchingOnUi {
    val path = Path(myFixture.tempDirFixture.tempDirPath)
    val viewModel = SetupGitLabCIViewModel(path, project, scope, emptyProjectVcsDataProvider)

    val configState = viewModel.baseSetupCIViewModel.configEditorStateFlow.filterNotNull().first()
    assertThat(configState).isNotNull
    assertThat(configState.ciConfigFileState).isNotNull
    assertThat(configState.ciConfigFileState).isInstanceOf(CIConfigFileState.InMemory::class.java)
  }

  fun `test yaml in project`() = runDispatchingOnUi {
    val path = Path(myFixture.tempDirFixture.tempDirPath)
    val viewModel = SetupGitLabCIViewModel(path, project, scope, emptyProjectVcsDataProvider)

    val configState = viewModel.baseSetupCIViewModel.configEditorStateFlow.filterNotNull().first()
    assertThat(configState).isNotNull
    assertThat(configState.ciConfigFileState).isNotNull
    assertThat(configState.ciConfigFileState).isInstanceOf(CIConfigFileState.Physical::class.java)
    assertThat(configState.ciConfigFileState.document.text.updateVersion()).isEqualTo(expectedText)
  }

  fun `test yaml in project without qodana section`() = runDispatchingOnUi {
    val path = Path(myFixture.tempDirFixture.tempDirPath)
    val viewModel = SetupGitLabCIViewModel(path, project, scope, emptyProjectVcsDataProvider)

    val configState = viewModel.baseSetupCIViewModel.configEditorStateFlow.filterNotNull().first()
    assertThat(configState).isNotNull
    assertThat(configState.ciConfigFileState).isNotNull
    assertThat(configState.ciConfigFileState).isInstanceOf(CIConfigFileState.InMemoryPatchOfPhysicalFile::class.java)
    assertThat(configState.ciConfigFileState.document.text).startsWith(expectedText)
  }

  fun `test yaml appeared in project`() = runDispatchingOnUi {
    val path = Path(myFixture.tempDirFixture.tempDirPath)
    val viewModel = SetupGitLabCIViewModel(path, project, scope, emptyProjectVcsDataProvider)

    val configState = viewModel.baseSetupCIViewModel.configEditorStateFlow.filterNotNull().first()
    assertThat(configState).isNotNull
    assertThat(configState.ciConfigFileState).isNotNull
    assertThat(configState.ciConfigFileState).isInstanceOf(CIConfigFileState.InMemory::class.java)
    dispatchAllTasksOnUi()

    createPhysicalConfigYml()

    val newConfigState = viewModel.baseSetupCIViewModel.configEditorStateFlow.filter { it !== configState }.first()
    assertThat(newConfigState).isNotNull
    assertThat(newConfigState!!.ciConfigFileState).isNotNull
    assertThat(newConfigState.ciConfigFileState).isInstanceOf(CIConfigFileState.Physical::class.java)
    assertThat(newConfigState.ciConfigFileState.document.text).isEqualTo(expectedText)
  }

  fun `test yaml disappeared in project`() = runDispatchingOnUi {
    val path = Path(myFixture.tempDirFixture.tempDirPath)
    val viewModel = SetupGitLabCIViewModel(path, project, scope, emptyProjectVcsDataProvider)

    val configState = viewModel.baseSetupCIViewModel.configEditorStateFlow.filterNotNull().first()
    assertThat(configState).isNotNull
    assertThat(configState.ciConfigFileState).isNotNull
    assertThat(configState.ciConfigFileState).isInstanceOf(CIConfigFileState.Physical::class.java)
    assertThat(configState.ciConfigFileState.document.text.updateVersion()).isEqualTo(expectedText)

    dispatchAllTasksOnUi()

    deletePhysicalConfigYml()

    val newConfigState = viewModel.baseSetupCIViewModel.configEditorStateFlow.filter { it !== configState }.first()
    assertThat(newConfigState).isNotNull
    assertThat(newConfigState!!.ciConfigFileState).isNotNull
    assertThat(newConfigState.ciConfigFileState).isInstanceOf(CIConfigFileState.InMemory::class.java)
  }

  fun `test write inMemory`() = runDispatchingOnUi {
    val path = Path(myFixture.tempDirFixture.tempDirPath)
    val viewModel = SetupGitLabCIViewModel(path, project, scope, emptyProjectVcsDataProvider)

    val configState = viewModel.baseSetupCIViewModel.configEditorStateFlow.filterNotNull().first()
    assertThat(configState).isNotNull
    assertThat(configState.ciConfigFileState).isNotNull
    assertThat(configState.ciConfigFileState).isInstanceOf(CIConfigFileState.InMemory::class.java)
    assertThat(configState.ciConfigFileState.document.text).isEqualTo(expectedText)

    val nullFile = physicalConfigYml()
    assertThat(nullFile).isNull()

    configState.ciConfigFileState.writeFile()

    val physicalConfigFile = physicalConfigYml()
    assertThat(physicalConfigFile).isNotNull
    assertThat(physicalConfigFile?.readText()).isEqualTo(expectedText)
  }

  fun `test update inMemoryPatch config`() = runDispatchingOnUi {
    val path = Path(myFixture.tempDirFixture.tempDirPath)
    val viewModel = SetupGitLabCIViewModel(path, project, scope, emptyProjectVcsDataProvider)

    @Language("YAML")
    val initialText = """
      another:
        image: another-image
    """.trimIndent()

    val configState = viewModel.baseSetupCIViewModel.configEditorStateFlow.filterNotNull().first()
    assertThat(configState).isNotNull
    assertThat(configState.ciConfigFileState).isNotNull
    assertThat(configState.ciConfigFileState).isInstanceOf(CIConfigFileState.InMemoryPatchOfPhysicalFile::class.java)
    assertThat(configState.ciConfigFileState.document.text).isNotEqualTo(expectedText)

    configState.ciConfigFileState.writeFile()

    val physicalConfigFile = physicalConfigYml()
    assertThat(physicalConfigFile).isNotNull
    assertThat(physicalConfigFile?.readText()).isEqualTo(expectedText + "\n\n" + initialText)
  }

  fun `test finishProviderFlow`() = runDispatchingOnUi {
    val path = Path(myFixture.tempDirFixture.tempDirPath)
    val viewModel = SetupGitLabCIViewModel(path, project, scope, emptyProjectVcsDataProvider)

    val configState = viewModel.baseSetupCIViewModel.configEditorStateFlow.filterNotNull().first()
    assertThat(configState).isNotNull
    assertThat(configState.ciConfigFileState).isNotNull
    assertThat(configState.ciConfigFileState).isInstanceOf(CIConfigFileState.InMemory::class.java)
    assertThat(configState.ciConfigFileState.document.text).isEqualTo(expectedText)

    val nullFile = physicalConfigYml()
    assertThat(nullFile).isNull()

    val action = viewModel.finishProviderFlow.first()
    assertThat(action).isNotNull

    assertSingleNotificationWithMessage("Qodana will monitor code quality when changes are pushed to the remote") {
      action!!()
    }

    val physicalConfigFile = physicalConfigYml()
    assertThat(physicalConfigFile).isNotNull
    assertThat(physicalConfigFile?.readText()).isEqualTo(expectedText)
  }

  fun `test setting git branches`() = runDispatchingOnUi {
    val projectVcsDataProviderWithBranches = ProjectVcsDataProviderMock(
      projectBranches = listOf("main", "dev", "another"),
      currentBranch = "test-branch"
    )
    val path = Path(myFixture.tempDirFixture.tempDirPath)
    val viewModel = SetupGitLabCIViewModel(path, project, scope, projectVcsDataProviderWithBranches)

    val configState = viewModel.baseSetupCIViewModel.configEditorStateFlow.filterNotNull().first()
    assertThat(configState).isNotNull
    assertThat(configState.ciConfigFileState).isNotNull
    assertThat(configState.ciConfigFileState).isInstanceOf(CIConfigFileState.InMemory::class.java)

    assertThat(configState.ciConfigFileState.document.text).isEqualTo(expectedText)
  }

  fun `test sarif baseline in project`() = runDispatchingOnUi {
    val path = Path(myFixture.tempDirFixture.tempDirPath)
    val viewModel = SetupGitLabCIViewModel(path, project, scope, emptyProjectVcsDataProvider)

    val configState = viewModel.baseSetupCIViewModel.configEditorStateFlow.filterNotNull().first()
    assertThat(configState).isNotNull
    assertThat(configState.ciConfigFileState).isNotNull
    assertThat(configState.ciConfigFileState).isInstanceOf(CIConfigFileState.InMemory::class.java)

    assertThat(configState.ciConfigFileState.document.text).isEqualTo(expectedText)
  }

  fun `test banner visible`() = runDispatchingOnUi {
    val path = Path(myFixture.tempDirFixture.tempDirPath)
    val viewModel = SetupGitLabCIViewModel(path, project, scope, emptyProjectVcsDataProvider)
    dispatchAllTasksOnUi()

    var banner: BannerContentProvider? = null

    scope.launch(QodanaDispatchers.Default) {
      viewModel.baseSetupCIViewModel.bannerContentProviderFlow.collect {
        banner = it
      }
    }

    dispatchAllTasksOnUi()
    assertThat(banner).isNotNull
  }

  private val expectedText: String
    get() = myFixture.tempDirFixture.getFile("expected.yml")?.readText()?.updateVersion() ?: ""

  private fun physicalConfigYml(): VirtualFile? {
    return myFixture.tempDirFixture.getFile(".gitlab-ci.yml")
  }

  private suspend fun createPhysicalConfigYml() {
    edtWriteAction {
      myFixture.tempDirFixture.createFile(".gitlab-ci.yml", expectedText)
    }
  }

  private suspend fun deletePhysicalConfigYml() {
    edtWriteAction {
      myFixture.tempDirFixture.getFile(".gitlab-ci.yml")!!.delete(this)
    }
  }

  private fun String.updateVersion(): String {
    val ideMajorVersion = ApplicationInfo.getInstance().majorVersion
    val ideMinorVersion = ApplicationInfo.getInstance().minorVersionMainPart
    return this
      .replace("<VERSION_PLACEHOLDER>", "v$ideMajorVersion.$ideMinorVersion")
  }
}