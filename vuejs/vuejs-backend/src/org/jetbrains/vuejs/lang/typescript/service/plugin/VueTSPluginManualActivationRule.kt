// Copyright 2000-2026 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package org.jetbrains.vuejs.lang.typescript.service.plugin

import com.intellij.lang.typescript.lsp.ServiceActivationHelper
import com.intellij.lang.typescript.lsp.TSPluginActivationRule
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import org.jetbrains.vuejs.lang.typescript.service.isVueServiceContext
import org.jetbrains.vuejs.options.VueLSMode
import org.jetbrains.vuejs.options.VueSettings

internal object VueTSPluginManualActivationRule : TSPluginActivationRule(
  tsPluginLoader = VueTSPluginManualLoader,
  activationRule = VueTSPluginManualActivationHelper,
)

private object VueTSPluginManualActivationHelper : ServiceActivationHelper {
  override fun isProjectContext(project: Project, context: VirtualFile): Boolean {
    return isVueServiceContext(project, context)
  }

  override fun isEnabledInSettings(project: Project): Boolean {
    val settings = VueSettings.instance(project)
    return settings.serviceType == VueLSMode.MANUAL
           && settings.manualSettings.mode == VueSettings.ManualMode.ONLY_TS_PLUGIN
  }
}