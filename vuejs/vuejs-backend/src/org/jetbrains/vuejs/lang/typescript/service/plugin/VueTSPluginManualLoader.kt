// Copyright 2000-2026 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package org.jetbrains.vuejs.lang.typescript.service.plugin

import com.intellij.javascript.nodejs.util.NodePackageRef
import com.intellij.lang.typescript.lsp.PackageVersion
import com.intellij.lang.typescript.lsp.TSPluginLoader
import com.intellij.openapi.project.Project
import org.jetbrains.vuejs.lang.typescript.service.VueTSPluginVersion
import org.jetbrains.vuejs.lang.typescript.service.vuePluginPath
import org.jetbrains.vuejs.options.VueSettings

internal object VueTSPluginManualLoader : TSPluginLoader(
  packageDescriptor = VueTSPluginPackageDescriptor(vueTSPluginManualPackageVersion),
) {
  override fun getSelectedPackageRef(project: Project): NodePackageRef {
    val settings = VueSettings.instance(project)
    return settings.manualSettings.tsPluginPackageRef
  }
}

private val vueTSPluginManualPackageVersion = PackageVersion.bundled<VueTSPluginPackageDescriptor>(
  version = VueTSPluginVersion.defaultTSPlugin.versionString,
  pluginPath = vuePluginPath,
  localPath = "vue-language-tools/typescript-plugin/${VueTSPluginVersion.defaultTSPlugin.versionString}",
  isBundledEnabled = { true },
)