// Copyright 2000-2025 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package com.intellij.prettierjs.stable

import com.intellij.lang.javascript.modules.TestNpmPackage

/**
 * Prettier version for multi-plugin tests.
 */
const val PRETTIER_3_5_3_MULTI_PLUGINS_SPEC: String = "prettier@3.5.3"

@TestNpmPackage(PRETTIER_3_5_3_MULTI_PLUGINS_SPEC)
class ReformatWithPrettierMultiPluginTest : PrettierPackageLockTest() {

  fun testGracefulFallbackCursor() = withInstallation {
    doReformatFile("toReformat", "html")
  }
}
