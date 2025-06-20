package com.jetbrains.cidr.cpp.embedded.platformio.project

import com.intellij.openapi.application.WriteAction
import com.intellij.openapi.externalSystem.model.task.*
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VfsUtil
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.vfs.readText
import com.intellij.testFramework.JUnit38AssumeSupportRunner
import com.intellij.testFramework.LightPlatformTestCase
import com.intellij.util.asSafely
import com.intellij.util.system.OS
import com.jetbrains.cidr.cpp.CPPTestUtil
import com.jetbrains.cidr.cpp.execution.manager.CLionRunConfigurationManager
import com.jetbrains.cidr.external.system.model.ExternalModule
import org.junit.Assume
import org.junit.runner.RunWith
import java.nio.file.Paths

@RunWith(JUnit38AssumeSupportRunner::class)
class TestProjectLibScan : LightPlatformTestCase() {

  private lateinit var projectPath: String
  private lateinit var projectDir: VirtualFile
  private val expectedSourceFiles = setOf(
    "/src/main.cpp",
    "/lib/confusing-name/src/confusing-name.cpp",
    "/lib/confusing-name-no-src/confusing-name-no-src.cpp",
    "/lib/confusing-name-nested-src/main/src/nested/confusing-name-nested-src.cpp",
    "/lib/confusing-name-nested-src/main/src/confusing-name-nested-src.cpp",
  )

  override fun setUp() {
    super.setUp()
    projectPath = BASE_TEST_DATA_PATH.resolve("project-scan-libraries").toString()
    projectDir = VfsUtil.findFile(Paths.get(projectPath), true)!!
    WriteAction.run<Throwable> {
      CLionRunConfigurationManager.getInstance(project).updateRunConfigurations(PlatformioRunConfigurationManagerHelper)
    }
  }

  fun testScanLibraries() {
    Assume.assumeFalse(CPPTestUtil.getTestToolSet().kind.isRemoteLike)

    val taskId: ExternalSystemTaskId = ExternalSystemTaskId.create(ID, ExternalSystemTaskType.RESOLVE_PROJECT, project)
    val testListener = ExternalSystemTaskNotificationListener.NULL_OBJECT
    val projectNode = PlatformioProjectResolverForTest().resolveProjectInfo(
      id = taskId,
      projectPath = projectPath,
      isPreviewMode = true,
      settings = null,
      listener = testListener,
      resolverPolicy = null
    )!!
    val actualSourceFiles = projectNode.children.first().children.first()
      .data.asSafely<ExternalModule>()!!
      .resolveConfigurations.first()
      .fileConfigurations
      .associate {
        it.file.path.replace(projectPath, "").replace('\\', '/') to it
      }
    assertEquals("Source file", expectedSourceFiles, actualSourceFiles.keys)
    val switchesWithDefines = actualSourceFiles["/lib/confusing-name-no-src/confusing-name-no-src.cpp"]!!.compilerSwitches
    assertNotNull(switchesWithDefines)
    assertTrue("MANDATORY_DEFINE_B1", switchesWithDefines!!.contains("-DMANDATORY_DEFINE_B1"))
    assertTrue("MANDATORY_DEFINE_B2", switchesWithDefines.contains("-DMANDATORY_DEFINE_B2"))
  }

  private inner class PlatformioProjectResolverForTest() : PlatformioProjectResolver() {

    /**
     * Mock data is loaded from file pio-project-config.json
     * The file is created by invoking `pio project config --json-output > pio-project-config.json`
     */
    override fun gatherConfigJson(id: ExternalSystemTaskId,
                                  pioRunEventId: String,
                                  project: Project,
                                  listener: ExternalSystemTaskNotificationListener): String =
      VfsUtil.loadText(projectDir.findChild("pio-project-config.json")!!)

    /**
     * Mock data is loaded from file pio-project-metadata-esp-wrover-kit.json
     * The file is created by invoking
     * `pio project metadata -e esp32 --json-output --json-output-path=pio-project-metadata-esp-wrover-kit.json`
     *  and then `libsource_dirs` is set to `["lib"]`
     *
     */
    override fun gatherEnvMetadata(id: ExternalSystemTaskId,
                                   pioRunEventId: String,
                                   project: Project,
                                   activeEnvName: String,
                                   listener: ExternalSystemTaskNotificationListener): String =
      projectDir.findChild("pio-project-metadata.json")!!.readText().replace("T:", projectDir.path)

    override fun createRunConfigurationIfRequired(project: Project) {}

    /**
     * Mock data is loaded from compile_commands.json or compile_commands_win.json on Windows
     * The file is created by invoking `pio run -t compiledb`,
     * dropping information about framework sources,
     * and clearing the `directory` entries to make the data not rely on absolute paths.
     * We inject the actual project directory here.
     */
    override fun gatherCompDB(id: ExternalSystemTaskId, pioRunEventId: String, project: Project, activeEnvName: String, listener: ExternalSystemTaskNotificationListener, projectPath: String): String {
      val compDbFileName = if (OS.CURRENT == OS.Windows) "compile_commands_win.json" else "compile_commands.json"
      return projectDir.findChild(compDbFileName)!!.readText().injectProjectPath()
    }

    private fun String.injectProjectPath() = this.replace("\"directory\": \"\"", "\"directory\": \"${projectPath.replace("\\", "\\\\")}\"")
  }
}
