// Copyright 2000-2025 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package org.jetbrains.vuejs.options

import com.intellij.javascript.nodejs.util.NodePackageRef
import com.intellij.javascript.util.JSLogOnceService
import com.intellij.lang.javascript.library.typings.TypeScriptPackageName
import com.intellij.lang.typescript.compiler.TypeScriptCompilerSettings
import com.intellij.lang.typescript.lsp.JSBundledServiceNodePackage
import com.intellij.lang.typescript.lsp.createPackageRef
import com.intellij.lang.typescript.lsp.extractPath
import com.intellij.lang.typescript.lsp.extractRefText
import com.intellij.lang.typescript.lsp.restartTypeScriptServicesAsync
import com.intellij.openapi.Disposable
import com.intellij.openapi.components.SerializablePersistentStateComponent
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.State
import com.intellij.openapi.components.Storage
import com.intellij.openapi.components.StoragePathMacros
import com.intellij.openapi.components.service
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Disposer
import com.intellij.openapi.util.NlsSafe
import com.intellij.util.text.SemVer
import kotlinx.serialization.Serializable
import org.jetbrains.annotations.TestOnly
import org.jetbrains.vuejs.lang.typescript.service.VueTSPluginVersion
import org.jetbrains.vuejs.lang.typescript.service.lsp.VueLspServerLoader
import org.jetbrains.vuejs.lang.typescript.service.plugin.VueTSPluginBundledLoaderFactory.getLoader
import org.jetbrains.vuejs.lang.typescript.service.vueTSPluginPackageName

@TestOnly
fun configureVueService(project: Project, disposable: Disposable, serviceSettings: VueLSMode) {
  val vueSettings = VueSettings.instance(project)
  val old = vueSettings.serviceType
  vueSettings.serviceType = serviceSettings

  Disposer.register(disposable) {
    vueSettings.serviceType = old
  }
}

@Service(Service.Level.PROJECT)
@State(
  name = "VueSettings",
  storages = [Storage(StoragePathMacros.WORKSPACE_FILE)],
)
class VueSettings(private val project: Project) :
  SerializablePersistentStateComponent<VueSettings.State>(State()) {
  var serviceType: VueLSMode
    get() = state.serviceType
    set(value) {
      if (value == state.serviceType)
        return

      updateState { state -> state.copy(serviceType = value) }
      restartTypeScriptServicesAsync(project)
    }

  var useTypesFromServer: Boolean
    get() {
      return (TypeScriptCompilerSettings.useTypesFromServerInTests ?: state.useTypesFromServer).also {
        with(project.service<JSLogOnceService>()) {
          LOG.infoOnce { "'Service-powered type engine' option of VueSettings: $it" }
        }
      }
    }
    set(value) {
      if (value == state.useTypesFromServer)
        return

      updateState { state -> state.copy(useTypesFromServer = value) }
      restartTypeScriptServicesAsync(project)
    }

  val manualSettings: ManualSettings = ManualSettings()

  inner class ManualSettings {
    var mode: ManualMode
      get() = state.manual.mode
      set(value) {
        if (value == state.manual.mode)
          return

        updateState { state -> state.copy(manual = state.manual.copy(mode = value)) }
        restartTypeScriptServicesAsync(project)
      }

    var lspServerPackageRef: NodePackageRef
      get() {
        return createPackageRef(
          ref = state.manual.lspServerPackagePath,
          defaultPackage = VueLspServerLoader.packageDescriptor.serverPackage,
        )
      }
      set(value) {
        val refText = extractRefText(value)
        if (refText == state.manual.lspServerPackagePath)
          return

        updateState { state ->
          state.copy(
            manual = state.manual.copy(lspServerPackagePath = refText)
          )
        }
        restartTypeScriptServicesAsync(project)
      }

    var tsPluginPackageRef: NodePackageRef
      get() {
        val tsPluginPackage = state.manual.tsPluginPackage
        return when (tsPluginPackage) {
          is ManualPackageBundled -> createBundledPackageRef(
            versionString = tsPluginPackage.version,
            project = project,
          )

          is ManualPackageFS -> createPackageRef(
            ref = tsPluginPackage.path,
            defaultPackage = getDefaultTsPluginPackage(),
          )

          null -> createBundledPackageRef(
            versionString = VueTSPluginVersion.defaultTSPlugin.versionString,
            project = project,
          )
        }
      }
      set(value) {
        val newTSPluginPackage = when (value.constantPackage) {
          is JSBundledServiceNodePackage -> ManualPackageBundled(
            version = value.constantPackage?.version.toString(),
          )

          else -> ManualPackageFS(
            path = extractPath(value, project) ?: "",
          )
        }

        if (newTSPluginPackage == state.manual.tsPluginPackage)
          return

        updateState { state ->
          state.copy(
            manual = state.manual.copy(tsPluginPackage = newTSPluginPackage)
          )
        }
        restartTypeScriptServicesAsync(project)
      }
  }

  companion object {
    private val LOG = logger<VueSettings>()
    fun instance(project: Project): VueSettings = project.service()
  }

  @Serializable
  data class State(
    val serviceType: VueLSMode = VueLSMode.AUTO,
    val useTypesFromServer: Boolean = false,
    val manual: ManualSettingsState = ManualSettingsState(),
  )

  @Serializable
  data class ManualSettingsState(
    val mode: ManualMode = ManualMode.ONLY_TS_PLUGIN,
    val lspServerPackagePath: String? = null,
    val tsPluginPackage: ManualServicePackage? = null,
  )

  @Serializable
  enum class ManualMode(
    @param:NlsSafe
    val displayName: String,
  ) {
    ONLY_TS_PLUGIN("TypeScript plugin"),
    ONLY_LSP_SERVER("Language server"),

    ;
  }

  @Serializable
  sealed interface ManualServicePackage

  @Serializable
  data class ManualPackageBundled(
    val version: String,
  ) : ManualServicePackage

  @Serializable
  data class ManualPackageFS(
    val path: String,
  ) : ManualServicePackage
}

private fun createBundledPackageRef(
  versionString: String,
  project: Project,
): NodePackageRef {
  val path = getLoader(versionString).getAbsolutePath(project)

  return NodePackageRef.create(JSBundledServiceNodePackage(
    packageName = vueTSPluginPackageName,
    packageVersion = SemVer.parseFromText(versionString),
    path = path,
  ))
}

private fun getDefaultTsPluginPackage(): TypeScriptPackageName {
  val latest = VueTSPluginVersion.defaultTSPlugin
  return getLoader(latest)
    .packageDescriptor
    .serverPackage
}

@Serializable
enum class VueLSMode {
  AUTO,
  MANUAL,
  DISABLED,

  ;

  fun isEnabled(): Boolean = this != DISABLED
}