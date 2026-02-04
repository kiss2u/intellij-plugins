// Copyright 2000-2026 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package com.intellij.prettierjs.stable

import com.intellij.javascript.debugger.NodeJsAppRule
import com.intellij.javascript.debugger.NodeJsAppRule.Companion.LATEST_23
import com.intellij.lang.javascript.modules.TestNpmPackage

@TestNpmPackage(PRETTIER_3_TEST_PACKAGE_SPEC)
class ReformatWithPrettierTypeScriptConfigV3Test : PrettierPackageLockTest() {
  override fun configureInterpreterVersion(): NodeJsAppRule {
    return LATEST_23
  }

  fun testTypeScriptConfig() {
    doReformatFile("ts")
  }
}
