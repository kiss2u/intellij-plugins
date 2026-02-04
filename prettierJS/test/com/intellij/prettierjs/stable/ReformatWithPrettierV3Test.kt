// Copyright 2000-2026 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package com.intellij.prettierjs.stable

import com.intellij.lang.javascript.JSTestUtils
import com.intellij.lang.javascript.modules.TestNpmPackage
import com.intellij.openapi.actionSystem.IdeActions
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.openapi.util.text.StringUtil
import com.intellij.openapi.vfs.VfsUtilCore
import com.intellij.prettierjs.PrettierConfiguration
import com.intellij.util.LineSeparator

@TestNpmPackage(PRETTIER_3_TEST_PACKAGE_SPEC)
class ReformatWithPrettierV3Test : ReformatWithPrettierGenericTest() {

  // Additional Basic Formatting Tests

  fun testTypeScriptWithEmptyConfig() {
    doReformatFile("ts")
  }

  fun testJsFileWithSelection() {
    doReformatFile("js")
  }

  // Additional File Detection Tests

  fun testJsonFileDetectedByName() {
    doReformatFile(".babelrc", "")
  }

  // Additional Ignored File Tests

  fun testSubFolderIgnoredFileInRoot() {
    doReformatFile("package/toReformat", "js")
  }

  fun testSubFolderIgnoredFileInsidePackage() {
    doReformatFile("package/toReformat", "js")
  }

  fun testSubFolderIgnoredFileInsideSubDir() {
    doReformatFile("package/subdir/toReformat", "js")
  }

  fun testSubFolderIgnoredFileManual() {
    doReformatFile<Throwable>("package/toReformat", "js") {
      PrettierConfiguration.getInstance(project).state.configurationMode =
        PrettierConfiguration.ConfigurationMode.MANUAL
    }
  }

  fun testSubFolderIgnoredFileManualSubDir() {
    doReformatFile<Throwable>("package/toReformat", "js") {
      val config = PrettierConfiguration.getInstance(project)
      config.state.configurationMode = PrettierConfiguration.ConfigurationMode.MANUAL

      val ignoreFile = myFixture.findFileInTempDir(".prettierignore")
      config.state.customIgnorePath = ignoreFile.toNioPath().toString()
    }
  }

  fun testSubFolderIgnoredManualFormat() {
    doReformatFile<Throwable>("package/toReformat", "js") {
      val config = PrettierConfiguration.getInstance(project)
      config.state.configurationMode = PrettierConfiguration.ConfigurationMode.MANUAL

      val ignoreFile = myFixture.findFileInTempDir(".prettierignore")
      config.state.customIgnorePath = ignoreFile.toNioPath().toString()
    }
  }

  // Additional Line Separator Tests

  fun testWithUpdatingLfToCrlf() {
    doReformatFile("toReformat", "js")
    FileDocumentManager.getInstance().saveAllDocuments()
    assertEquals(LineSeparator.CRLF, StringUtil.detectSeparators(VfsUtilCore.loadText(getFile().virtualFile)))
  }

  // Additional Caret Position Tests

  fun testCaretPosition() {
    configureRunOnSave {
      val actionId = "SaveDocument"
      doTestSaveAction(actionId, "")
    }
  }

  fun testCaretPositionEndFileReformat() {
    configureRunOnReformat { doTestEditorReformat("") }
  }

  fun testCaretPositionReformatParenthesis() {
    configureRunOnReformat { doTestEditorReformat("") }
  }

  fun testCrlfCaretPosition() {
    configureRunOnSave {
      val actionId = "SaveDocument"
      doTestSaveAction(actionId, "") {
        JSTestUtils.ensureLineSeparators(myFixture.file, LineSeparator.CRLF)
      }
    }
  }

  fun testCrlfCaretPositionReformat() {
    configureRunOnReformat {
      doTestEditorReformat("") {
        JSTestUtils.ensureLineSeparators(myFixture.file, LineSeparator.CRLF)
      }
    }
  }

  // Patch Tests

  fun testPatchApplied() {
    configureRunOnSave {
      val actionId = "SaveDocument"
      doTestSaveAction(actionId, "first/")
      doTestSaveAction(actionId, "second/")
    }
  }

  fun testPatchAppliedDeletion() {
    doReformatFile("js")
  }

  fun testPatchAppliedEndline() {
    doReformatFile("js")
  }

  // Additional On-Save Tests

  fun testRunPrettierOnSaveAll() {
    configureRunOnSave { doTestSaveAction("SaveAll", "") }
  }

  // Additional On-Reformat Tests

  fun testCommentAfterImports() {
    configureRunOnReformat { doTestEditorReformat("") }
  }

  fun testCommentAfterStatement() {
    configureRunOnReformat { doTestEditorReformat("") }
  }

  // Monorepo Tests

  fun testMonorepoIndirectDependency() {
    configureRunOnSave {
      val actionId = "SaveDocument"

      //file in the root without prettier
      doTestSaveAction(actionId, "")

      //package with an indirect dependecy prettier in subfolder
      doTestSaveAction(actionId, "package-a/")

      //package with prettier in subfolder
      doTestSaveAction(actionId, "package-b/")

      //package with prettier config in subfolder
      doTestSaveAction(actionId, "package-c/")
    }
  }

  fun testMonorepoManualScopeOnSave() {
    configureFormatFilesOutsideDependencyScope(false) {
      val actionId = "SaveDocument"

      //file in the root without prettier
      doTestSaveAction(actionId, "")

      //package with prettier in subfolder
      doTestSaveAction(actionId, "package-a/")

      //package without prettier in subfolder
      doTestSaveAction(actionId, "package-b/")
    }
  }

  fun testMonorepoManualScopeReformat() {
    configureFormatFilesOutsideDependencyScope(false) {
      //file in the root without prettier
      doTestEditorReformat("")

      //package with prettier in subfolder
      doTestEditorReformat("package-a/")

      //package without prettier in subfolder
      doTestEditorReformat("package-b/")
    }
  }

  fun testMonorepoManualWithoutScope() {
    configureFormatFilesOutsideDependencyScope(true) {
      val actionId = "SaveDocument"

      //file in the root without prettier
      doTestSaveAction(actionId, "")
      doTestEditorReformat("")

      //package with prettier in subfolder
      doTestSaveAction(actionId, "package-a/")
      doTestEditorReformat("package-a/")

      //package without prettier in subfolder
      doTestSaveAction(actionId, "package-b/")
      doTestEditorReformat("package-b/")
    }
  }

  fun testMonorepoOnSave() {
    configureRunOnSave {
      val actionId = "SaveDocument"

      //file in the root without prettier
      doTestSaveAction(actionId, "")

      //package with prettier in subfolder
      doTestSaveAction(actionId, "package-a/")

      //package without prettier in subfolder
      doTestSaveAction(actionId, "package-b/")
    }
  }

  fun testMonorepoSubDirEditorReformat() {
    configureRunOnReformat {
      //file in the root without prettier
      doTestEditorReformat("")

      //package with prettier in subfolder
      doTestEditorReformat("package-a/")

      //package without prettier in subfolder
      doTestEditorReformat("package-b/")
    }
  }

  fun testMonorepoSubDirOnSave() {
    configureRunOnSave {
      val actionId = "SaveDocument"

      //file in the root without prettier
      doTestSaveAction(actionId, "")

      //package with prettier in subfolder
      doTestSaveAction(actionId, "package-a/")

      //package without prettier in subfolder
      doTestSaveAction(actionId, "package-b/")
    }
  }

  fun testMonorepoSubDirReformatAction() {
    // file in the root without prettier but it should be formatted via reformat action
    val dirName = getTestName(true)
    myFixture.copyDirectoryToProject(dirName, "")
    myFixture.configureFromExistingVirtualFile(myFixture.findFileInTempDir("toReformat.js"))
    runReformatAction()
    myFixture.checkResultByFile("$dirName/toReformat_after.js")
  }

  // Special Cases

  fun testAutoconfigured() {
    PrettierConfiguration.getInstance(project).state.configurationMode =
      PrettierConfiguration.ConfigurationMode.AUTOMATIC
    doReformatFile<Throwable>("subdir/formatted", "js") {
      myFixture.tempDirFixture.copyAll(getNodePackage().systemIndependentPath, "subdir/node_modules/prettier")
    }

    myFixture.configureFromExistingVirtualFile(myFixture.findFileInTempDir("ignored.js"))
    runReformatAction()
    myFixture.checkResultByFile(getTestName(true) + "/ignored_after.js")
  }

  fun testChangeConfig() {
    val dirName = getTestName(true)
    myFixture.copyDirectoryToProject(dirName, "")
    myFixture.configureFromExistingVirtualFile(myFixture.findFileInTempDir("toReformat_after.js"))

    // set singleQuote to true
    val config = myFixture.createFile("prettier.config.mjs", """
      const config = {
        singleQuote: true,
      }

      export default config
      """.trimIndent())
    runReformatAction()
    myFixture.checkResultByFile("$dirName/toReformat_after.js")

    // change singleQuote to false
    myFixture.saveText(config, """
      const config = {
        singleQuote: false,
      }

      export default config
      """.trimIndent())
    runReformatAction()
    myFixture.checkResultByFile("$dirName/toReformat_after_1.js")
  }

  fun testIncompleteBlock() {
    val configuration = PrettierConfiguration.getInstance(project)
    val origRunOnReformat = configuration.state.runOnReformat
    configuration.state.runOnReformat = true
    try {
      val dirName = getTestName(true)
      myFixture.copyDirectoryToProject(dirName, "")
      myFixture.configureFromTempProjectFile("toReformat.js")
      // should be used exactly ACTION_EDITOR_REFORMAT instead of ReformatWithPrettierAction
      // to check a default formatter behavior combined with Prettier
      myFixture.performEditorAction(IdeActions.ACTION_EDITOR_REFORMAT)
      myFixture.checkResultByFile("$dirName/toReformat_after.js")
    } finally {
      configuration.state.runOnReformat = origRunOnReformat
    }
  }

  fun testRangeInVue() {
    // Prettier doesn't support range formatting in Vue (WEB-52196, https://github.com/prettier/prettier/issues/13399),
    // and even removes line break at the end of the file. This test checks IDE's workaround of Prettier bug.
    myFixture.configureByText("foo.vue", "<template>\n<selection><div/></selection>\n</template>\n")
    runReformatAction()
    myFixture.checkResult("<template>\n<selection>  <div /></selection>\n</template>\n")
  }

}
