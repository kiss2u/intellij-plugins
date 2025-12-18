// Copyright 2000-2025 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package org.jetbrains.vuejs.lang.typescript.service

import com.intellij.openapi.util.NlsSafe

enum class VueTSPluginVersion(
  @param:NlsSafe
  val versionString: String,
) {
  V3_2_4("3.2.4"),
  V3_0_10("3.0.10"),

  ;

  companion object {
    internal val defaultTSPlugin: VueTSPluginVersion =
      V3_2_4
  }
}
