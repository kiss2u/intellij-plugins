// Copyright 2000-2026 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package com.intellij.prettierjs.stable

import com.intellij.lang.javascript.JSTestUtils
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.openapi.util.text.StringUtil
import com.intellij.openapi.vfs.VfsUtilCore
import com.intellij.util.LineSeparator

/**
 * Generic base for Prettier formatting tests across different Prettier versions.
 *
 * Subclasses must be annotated with [@TestNpmPackage][com.intellij.lang.javascript.modules.TestNpmPackage].
 */
abstract class ReformatWithPrettierGenericTest : PrettierPackageLockTest() {

  // Basic Formatting Tests - Most likely to catch version incompatibilities

  fun testWithoutConfig() = withInstallation {
    doReformatFile("js")
  }

  fun testTypeScriptWithoutConfig() = withInstallation {
    doReformatFile("ts")
  }

  fun testWithPackageJsonConfig() = withInstallation {
    doReformatFile("js")
  }

  // File Detection Tests

  fun testJsonFileDetectedByExtension() = withInstallation {
    doReformatFile("json")
  }

  fun testFileDetectedByShebangLine() = withInstallation {
    doReformatFile("test", "")
  }

  // Config Resolution Tests - Config parsing may change between versions

  fun testResolveConfig() = withInstallation {
    doReformatFile("webc")
  }

  fun testWithEditorConfig() = withInstallation {
    doReformatFile("js")
  }

  // Error Handling Tests - Error message format often changes

  fun testInvalidConfigErrorReported() = withInstallation {
    assertError({ s -> s.contains("tabWidth") }) { doReformatFile("js") }
  }

  fun testNotSupportedFile() = withInstallation {
    assertError({ s -> s.contains("unsupported type") }) { doReformatFile("test", "txt") }
  }

  // Caret Position Tests - formatWithCursor API may change

  fun testCaretPositionReformat() = withInstallation {
    configureRunOnReformat { doTestEditorReformat("") }
  }

  // Line Separator Tests

  fun testWithCrlf() = withInstallation {
    doReformatFile<Throwable>("toReformat", "js") {
      JSTestUtils.ensureLineSeparators(myFixture.file, LineSeparator.CRLF)
    }
    FileDocumentManager.getInstance().saveAllDocuments()
    assertEquals(LineSeparator.CRLF, StringUtil.detectSeparators(VfsUtilCore.loadText(getFile().virtualFile)))
  }

  // Ignored File Tests

  fun testIgnoredFile() = withInstallation {
    doReformatFile("toReformat", "js")
  }

  // On-Save Tests

  fun testRunPrettierOnSaveDocument() = withInstallation {
    configureRunOnSave { doTestSaveAction("SaveDocument", "") }
  }

  // On-Reformat Tests

  fun testRunPrettierOnCodeReformat() = withInstallation {
    configureRunOnReformat { doTestEditorReformat("") }
  }
}
