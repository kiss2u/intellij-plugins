// Copyright 2000-2025 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.

package com.intellij.prettierjs.stable

import com.intellij.javascript.nodejs.util.NodePackage
import com.intellij.javascript.nodejs.util.NodePackageRef
import com.intellij.lang.javascript.linter.ActionsOnSaveTestUtil
import com.intellij.lang.javascript.modules.JSTempDirWithNodeInterpreterTest
import com.intellij.lang.javascript.modules.NodeModuleUtil
import com.intellij.lang.javascript.modules.TestNpmPackage
import com.intellij.lang.javascript.modules.TestNpmPackageInstaller
import com.intellij.openapi.actionSystem.IdeActions
import com.intellij.openapi.application.runWriteAction
import com.intellij.openapi.util.Condition
import com.intellij.openapi.util.text.StringUtil
import com.intellij.openapi.vfs.VfsUtil
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.vfs.newvfs.impl.VfsRootAccess
import com.intellij.prettierjs.PrettierConfiguration
import com.intellij.prettierjs.PrettierJSTestUtil
import com.intellij.prettierjs.PrettierLanguageServiceManager
import com.intellij.prettierjs.PrettierUtil
import com.intellij.prettierjs.ReformatWithPrettierAction
import com.intellij.psi.PsiFile
import com.intellij.util.ThrowableRunnable
import org.junit.Assert
import java.io.File
import java.nio.file.Path

/**
 * Prettier version constants for tests.
 *
 * If you change a version number, please sync the stored lock files for the corresponding version here:
 * $PROJECT_ROOT/contrib/prettierJS/testData/reformat/_package-locks-store
 */
const val PRETTIER_3_TEST_PACKAGE_SPEC: String = "prettier@3.8.1"

// next versions
const val PRETTIER_LATEST_TEST_PACKAGE_SPEC: String = "prettier@latest"

/**
 * Placeholder for prettier version in package.json files.
 * This placeholder will be replaced with the actual version from @TestNpmPackage annotation at test runtime.
 */
const val PRETTIER_VERSION_PLACEHOLDER: String = "\$PRETTIER_VERSION\$"

/**
 * Base class for Prettier tests using cached package-lock.json files.
 *
 * Subclasses must be annotated with [@TestNpmPackage][TestNpmPackage]("prettier@X.Y.Z").
 */
abstract class PrettierPackageLockTest : JSTempDirWithNodeInterpreterTest() {

  private var localNodePackage: NodePackage? = null

  override fun getGlobalPackageVersionsToInstall(): Map<String, String> = emptyMap()

  override fun setUpForTempRoot(rootDir: Path) {
    // Skip parent implementation that installs global packages
    // We'll install packages locally in setUp() after project directory is available
  }

  override fun setUp() {
    super.setUp()

    myFixture.testDataPath = PrettierJSTestUtil.getTestDataPath() + "reformat"

    val projectDir = myFixture.tempDirFixture.getFile(".")
                     ?: error("Project directory not found")

    // Install packages using TestNpmPackageInstaller
    withBatchChange {
      TestNpmPackageInstaller(project, myFixture, nodeJsAppRule)
        .installForTest(this::class.java, projectDir)
    }

    // Set up NodePackage from locally installed prettier
    val prettierPath = projectDir.toNioPath()
      .resolve(NodeModuleUtil.NODE_MODULES)
      .resolve(PrettierUtil.PACKAGE_NAME)

    check(File(prettierPath.toString()).exists()) {
      "Expected prettier package to exist at $prettierPath"
    }

    val nodePackage = NodePackage(prettierPath.toString())
    localNodePackage = nodePackage
    VfsRootAccess.allowRootAccess(myFixture.testRootDisposable, nodePackage.systemDependentPath)

    // Configure Prettier to use the locally installed package
    PrettierConfiguration.getInstance(project)
      .withLinterPackage(NodePackageRef.create(nodePackage))
      .state.configurationMode = PrettierConfiguration.ConfigurationMode.MANUAL
  }

  override fun tearDown() {
    localNodePackage = null
    super.tearDown()
  }

  protected fun getNodePackage(): NodePackage {
    return localNodePackage ?: error("NodePackage not initialized. Call setUp() first.")
  }

  /**
   * Replaces [PRETTIER_VERSION_PLACEHOLDER] in all package.json files under the given directory
   * with the actual prettier version from the @TestNpmPackage annotation.
   */
  protected fun replacePrettierVersionPlaceholders(rootDir: VirtualFile) {
    val annotation = this::class.java.getAnnotation(TestNpmPackage::class.java)
                     ?: error("Test class must be annotated with @TestNpmPackage")

    val packageSpec = annotation.packageSpec
    // Extract version from spec like "prettier@3.2.5"
    val version = packageSpec.substringAfter('@', "latest")

    val filesToUpdate = mutableListOf<Pair<VirtualFile, String>>()

    VfsUtil.processFileRecursivelyWithoutIgnored(rootDir) { file ->
      if (file.name == "package.json" && !file.path.contains("node_modules")) {
        val content = VfsUtil.loadText(file)
        if (content.contains(PRETTIER_VERSION_PLACEHOLDER)) {
          val newContent = content.replace(PRETTIER_VERSION_PLACEHOLDER, version)
          filesToUpdate.add(file to newContent)
        }
      }
      true
    }

    if (filesToUpdate.isNotEmpty()) {
      runWriteAction {
        for ((file, newContent) in filesToUpdate) {
          VfsUtil.saveText(file, newContent)
        }
      }
    }
  }

  // Helper methods from ReformatWithPrettierBaseTest

  protected fun doReformatFile(extension: String) {
    doReformatFile("toReformat", extension)
  }

  protected fun doReformatFile(fileNamePrefix: String, extension: String) {
    doReformatFile<Throwable>(fileNamePrefix, extension, null)
  }

  protected fun <T : Throwable> doReformatFile(
    fileNamePrefix: String,
    extension: String,
    configureFixture: ThrowableRunnable<T>?,
  ) {
    val dirName = getTestName(true)
    myFixture.copyDirectoryToProject(dirName, "")

    // Replace version placeholder in package.json files before any npm operations
    val projectDir = myFixture.tempDirFixture.getFile(".") ?: error("Project directory not found")
    replacePrettierVersionPlaceholders(projectDir)

    val extensionWithDot = if (StringUtil.isEmpty(extension)) "" else ".$extension"
    myFixture.configureFromExistingVirtualFile(myFixture.findFileInTempDir(fileNamePrefix + extensionWithDot))
    configureFixture?.run()
    runReformatAction()
    myFixture.checkResultByFile("$dirName/$fileNamePrefix" + "_after" + extensionWithDot)
  }

  protected fun runReformatAction() {
    myFixture.testAction(ReformatWithPrettierAction())
  }

  protected fun assertError(checkException: Condition<String>, runnable: Runnable) {
    runnable.run()

    val manager = PrettierLanguageServiceManager.getInstance(project)
    val service = manager.jsLinterServices.values.firstOrNull()
    val error = service?.service?.error
    Assert.assertTrue(
      "Expected condition to be valid for exception: ${error?.message}",
      checkException.value(error?.message)
    )
  }

  protected fun configureRunOnSave(runnable: Runnable) {
    val configuration = PrettierConfiguration.getInstance(project)
    val runOnSave = configuration.state.runOnSave
    val runOnReformat = configuration.state.runOnReformat
    val configurationMode = configuration.state.configurationMode

    configuration.state.runOnSave = true
    configuration.state.runOnReformat = false
    configuration.state.configurationMode = PrettierConfiguration.ConfigurationMode.AUTOMATIC

    try {
      val dirName = getTestName(true)
      myFixture.copyDirectoryToProject(dirName, "")
      myFixture.tempDirFixture.copyAll(getNodePackage().systemIndependentPath, "node_modules/prettier")

      // Replace version placeholder in package.json files
      val projectDir = myFixture.tempDirFixture.getFile(".")
                       ?: error("Project directory not found")
      replacePrettierVersionPlaceholders(projectDir)

      runnable.run()
    }
    finally {
      configuration.state.runOnSave = runOnSave
      configuration.state.runOnReformat = runOnReformat
      configuration.state.configurationMode = configurationMode
    }
  }

  protected fun doTestSaveAction(actionId: String, subDir: String) {
    doTestSaveAction(actionId, subDir, null)
  }

  protected fun doTestSaveAction(actionId: String, subDir: String, configureFile: Runnable?) {
    val dirName = getTestName(true)
    myFixture.configureFromTempProjectFile(subDir + "toReformat.js")
    configureFile?.run()
    myFixture.type(' ')
    myFixture.performEditorAction(actionId)
    ActionsOnSaveTestUtil.waitForActionsOnSaveToFinish(myFixture.project)
    myFixture.checkResultByFile("$dirName/$subDir" + "toReformat_after.js")
  }

  protected fun configureRunOnReformat(runnable: Runnable) {
    val configuration = PrettierConfiguration.getInstance(project)
    val origRunOnReformat = configuration.state.runOnReformat
    val configurationMode = configuration.state.configurationMode

    configuration.state.runOnReformat = true
    configuration.state.configurationMode = PrettierConfiguration.ConfigurationMode.AUTOMATIC

    try {
      val dirName = getTestName(true)
      myFixture.copyDirectoryToProject(dirName, "")
      myFixture.tempDirFixture.copyAll(getNodePackage().systemIndependentPath, "node_modules/prettier")

      // Replace version placeholder in package.json files
      val projectDir = myFixture.tempDirFixture.getFile(".")
                       ?: error("Project directory not found")
      replacePrettierVersionPlaceholders(projectDir)

      runnable.run()
    }
    finally {
      configuration.state.runOnReformat = origRunOnReformat
      configuration.state.configurationMode = configurationMode
    }
  }

  protected fun doTestEditorReformat(subDir: String) {
    doTestEditorReformat(subDir, null)
  }

  protected fun doTestEditorReformat(subDir: String, configureFile: Runnable?) {
    val dirName = getTestName(true)
    myFixture.configureFromTempProjectFile(subDir + "toReformat.js")
    configureFile?.run()
    myFixture.performEditorAction(IdeActions.ACTION_EDITOR_REFORMAT)
    myFixture.checkResultByFile("$dirName/$subDir" + "toReformat_after.js")
  }

  protected fun configureFormatFilesOutsideDependencyScope(enabled: Boolean, runnable: Runnable) {
    val configuration = PrettierConfiguration.getInstance(project)
    val runOnSave = configuration.state.runOnSave
    val runOnReformat = configuration.state.runOnReformat
    val configurationMode = configuration.state.configurationMode
    val formatFilesOutsideDependencyScope = configuration.state.formatFilesOutsideDependencyScope

    configuration.state.runOnSave = true
    configuration.state.runOnReformat = true
    configuration.state.configurationMode = PrettierConfiguration.ConfigurationMode.MANUAL
    configuration.state.formatFilesOutsideDependencyScope = enabled

    try {
      val dirName = getTestName(true)
      myFixture.copyDirectoryToProject(dirName, "")

      runnable.run()
    }
    finally {
      configuration.state.runOnSave = runOnSave
      configuration.state.runOnReformat = runOnReformat
      configuration.state.configurationMode = configurationMode
      configuration.state.formatFilesOutsideDependencyScope = formatFilesOutsideDependencyScope
    }
  }

  override fun getFile(): PsiFile = myFixture.file
}
