// Copyright 2000-2025 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package com.intellij.prettierjs.stable

import com.intellij.lang.javascript.modules.TestNpmPackage

/**
 * Prettier version for Svelte plugin tests.
 */
const val PRETTIER_3_4_2_SVELTE_SPEC: String = "prettier@3.4.2"

@TestNpmPackage(PRETTIER_3_4_2_SVELTE_SPEC)
class ReformatWithPrettierSvelteTest : PrettierPackageLockTest() {

  fun testCaretPositionReformatSvelte() = withInstallation {
    doReformatFile("toReformat", "svelte")
  }
}
