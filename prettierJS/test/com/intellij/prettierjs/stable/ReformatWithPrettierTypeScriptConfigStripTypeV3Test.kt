// Copyright 2000-2026 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package com.intellij.prettierjs.stable

import com.intellij.javascript.debugger.NodeJsAppRule
import com.intellij.javascript.debugger.NodeJsAppRule.Companion.LATEST_22
import com.intellij.lang.javascript.modules.TestNpmPackage

@TestNpmPackage("prettier@3.2.5")
class ReformatWithPrettierTypeScriptConfigStripTypeV3Test : PrettierPackageLockTest() {
  override fun configureInterpreterVersion(): NodeJsAppRule {
    return LATEST_22
  }

  fun testTypeScriptConfig() {
    doReformatFile("ts")
  }
}
