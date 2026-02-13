// Copyright 2000-2025 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package org.jetbrains.vuejs

import com.intellij.javascript.nodejs.PackageJsonData
import com.intellij.javascript.testFramework.web.WebFrameworkTestCase
import com.intellij.javascript.testFramework.web.WebFrameworkTestModule
import com.intellij.lang.javascript.HybridTestMode
import com.intellij.lang.javascript.waitEmptyServiceQueueForService
import com.intellij.lang.typescript.compiler.languageService.TypeScriptServerServiceImpl
import com.intellij.lang.typescript.tsc.TypeScriptServiceTestMixin
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.testFramework.runInEdtAndWait
import org.jetbrains.vuejs.index.VUE_MODULE
import org.jetbrains.vuejs.lang.VueTestModule
import org.jetbrains.vuejs.lang.getVueTestDataPath
import org.jetbrains.vuejs.lang.typescript.service.plugin.VuePluginTypeScriptService
import org.jetbrains.vuejs.options.VueTSPluginVersion
import org.jetbrains.vuejs.options.getVueSettings

enum class VueTestMode {
  DEFAULT,
  LEGACY_PLUGIN,
  NO_PLUGIN,

  ;
}

private val DEFAULT_VUE_MODULES: Array<out WebFrameworkTestModule> = arrayOf(
  VueTestModule.VUE_3_5_0,
  VueTestModule.VUE_TSCONFIG_0_8_1,
)

abstract class VueTestCase(
  override val testCasePath: String,
  private val testMode: VueTestMode = VueTestMode.DEFAULT,
) : WebFrameworkTestCase(
  mode = if (testMode != VueTestMode.NO_PLUGIN) HybridTestMode.CodeInsightFixture else HybridTestMode.BasePlatform,
) {

  override fun adjustModules(
    modules: Array<out WebFrameworkTestModule>,
  ): Array<out WebFrameworkTestModule> {
    val hasVueDependency = modules.any { VUE_MODULE in it.packageNames }
    if (hasVueDependency)
      return modules

    return modules.asList()
      .plus(DEFAULT_VUE_MODULES)
      .toTypedArray()
  }

  override fun beforeConfiguredTest(configuration: TestConfiguration) {
    val tsPluginVersion = when (testMode) {
      VueTestMode.DEFAULT,
        -> if (useLatestPlugin())
        VueTSPluginVersion.V3_2_4
      else
        VueTSPluginVersion.V3_0_10

      VueTestMode.LEGACY_PLUGIN,
        -> VueTSPluginVersion.V3_0_10

      VueTestMode.NO_PLUGIN,
        -> return
    }

    getVueSettings(project).tsPluginVersion = tsPluginVersion

    val service = TypeScriptServiceTestMixin.setUpTypeScriptService(myFixture) {
      it::class == VuePluginTypeScriptService::class
    } as TypeScriptServerServiceImpl

    service.assertProcessStarted()
    runInEdtAndWait {
      waitEmptyServiceQueueForService(service)
    }

    if (configuration.configurators.any { it is VueTsConfigFile }) {
      TypeScriptServerServiceImpl.requireTSConfigsForTypeEvaluation(
        testRootDisposable,
        myFixture.tempDirFixture.getFile(VueTsConfigFile.FILE_NAME)!!,
      )
    }

    runInEdtAndWait {
      FileDocumentManager.getInstance().saveAllDocuments()
    }
  }

  private fun useLatestPlugin(): Boolean {
    val packageJson = myFixture.tempDirFixture.getFile("node_modules/vue/package.json")
                      ?: return true

    val version = PackageJsonData.getOrCreate(packageJson).version
                  ?: return true

    return version.major != 2
  }

  override val testDataRoot: String
    get() = getVueTestDataPath()

  override val defaultExtension: String
    get() = "vue"

  override val defaultDependencies: Map<String, String>
    get() = mapOf()
}