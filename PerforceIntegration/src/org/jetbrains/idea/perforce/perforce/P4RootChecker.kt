// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package org.jetbrains.idea.perforce.perforce

import com.intellij.execution.process.ProcessNotCreatedException
import com.intellij.openapi.components.service
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.io.FileUtil
import com.intellij.openapi.vcs.VcsException
import com.intellij.openapi.vcs.VcsKey
import com.intellij.openapi.vcs.VcsRootChecker
import com.intellij.openapi.vfs.LocalFileSystem
import com.intellij.openapi.vfs.VfsUtil
import com.intellij.openapi.vfs.VirtualFile
import org.jetbrains.annotations.SystemIndependent
import org.jetbrains.idea.perforce.application.PerforceManager
import org.jetbrains.idea.perforce.application.PerforceVcs
import org.jetbrains.idea.perforce.perforce.connections.*

internal class P4RootChecker : VcsRootChecker() {
  override fun getSupportedVcs(): VcsKey {
    return PerforceVcs.getKey()
  }

  override fun isRoot(file: VirtualFile): Boolean {
    return false
  }

  override fun validateRoot(file: VirtualFile): Boolean {
    return true
  }

  override fun shouldAlwaysRunInitialDetection(): Boolean {
    return true
  }

  override fun detectProjectMappings(project: Project,
                                     projectRoots: Collection<VirtualFile>,
                                     mappedDirs: Set<VirtualFile>): Collection<VirtualFile> {
    var configs = project.service<PerforceWorkspaceConfigurator>()
      .configure(projectRoots, forceCreateConfig = false)
    trackConfigs(project, configs)

    var mappedRoots = doDetectMappings(project, projectRoots, mappedDirs)

    if (mappedRoots.isEmpty()) {
      configs = project.service<PerforceWorkspaceConfigurator>()
        .configure(projectRoots, forceCreateConfig = true)
      if (configs.isNotEmpty()) {
        trackConfigs(project, configs)
        mappedRoots = doDetectMappings(project, projectRoots, mappedDirs)
      }
    }

    if (PerforceManager.getInstance(project).isActive) {
      PerforceConnectionManager.getInstance(project).updateConnections()
    }

    return mappedRoots
  }

  private fun doDetectMappings(
    project: Project,
    projectRoots: Collection<VirtualFile>,
    mappedDirs: Set<VirtualFile>,
  ): Collection<VirtualFile> {
    return try {
      val connectionManager = PerforceConnectionManager.getInstance(project)
      if (connectionManager.isSingletonConnectionUsed) {
        LOG.debug("detecting for singleton connection")
        detectSingletonConnectionMappings(project, projectRoots, mappedDirs)
      }
      else {
        LOG.debug("detecting for context connection")
        detectContextConnectionMappings(project, projectRoots, mappedDirs)
      }
    }
    catch (e: VcsException) {
      throw e
    }
  }

  private fun trackConfigs(
    project: Project,
    configs: Collection<PerforceWorkspaceConfigurator.P4Config>
  ) {
    if (configs.isEmpty()) return

    val configTracker = project.service<PerforceExternalConfigTracker>()
    configTracker.startTracking()
    configTracker.addConfigsToTrack(configs.map { it.configFile.path }.toSet())
  }

  private fun detectSingletonConnectionMappings(project: Project,
                                                projectRoots: Collection<VirtualFile>,
                                                mappedDirs: Set<VirtualFile>): Collection<VirtualFile> {
    val unmappedRoots = collectUnmappedDirs(mappedDirs, projectRoots)
    if (unmappedRoots.isEmpty()) return emptyList()

    val settings = PerforceSettings.getSettings(project)
    val connection: P4Connection = SingletonConnection.getInstance(project)

    return detectMappingsForConnection(connection, settings, unmappedRoots)
  }

  private fun detectContextConnectionMappings(project: Project,
                                              projectRoots: Collection<VirtualFile>,
                                              mappedDirs: Set<VirtualFile>): Collection<VirtualFile> {
    val baseDir = project.basePath ?: return emptyList()

    val unmappedRoots = collectUnmappedDirs(mappedDirs, projectRoots)
    if (unmappedRoots.isEmpty()) return emptyList()

    val settings = PerforceSettings.getSettings(project)
    val parameters = detectEnvConnectionParametersFor(baseDir, settings)
    LOG.debug("detected connection parameters: $parameters")

    val testConnection = P4ParametersConnection(parameters, ConnectionId(null, baseDir))
    return detectMappingsForConnection(testConnection, settings, unmappedRoots)
  }

  private fun detectEnvConnectionParametersFor(baseDir: @SystemIndependent String,
                                               settings: PerforceSettings): P4ConnectionParameters {
    val localConnection: P4Connection = PerforceLocalConnection(baseDir)
    val p4SetOut = localConnection.runP4CommandLine(settings, arrayOf("set"), null)

    val defaultParameters = P4ConnectionParameters()
    val parameters = P4ConnectionParameters()
    P4ParamsCalculator.parseSetOutput(defaultParameters, parameters, p4SetOut.stdout)
    return parameters
  }

  // todo: use p4 clients?
  private fun detectMappingsForConnection(connection: P4Connection,
                                          settings: PerforceSettings,
                                          unmappedRoots: List<VirtualFile>): List<VirtualFile> {
    val p4InfoOut = connection.runP4CommandLine(settings, arrayOf("info"), null)
    val root = parseP4Info(p4InfoOut.stdout)
    if (root == null) {
      val stderr = p4InfoOut.stderr
      val processException = p4InfoOut.exception
      when {
        p4InfoOut.exitCode < 0 && processException is ProcessNotCreatedException -> return emptyList() //p4 not present
        processException != null -> throw VcsException(processException)
        stderr.isNotBlank() -> throw VcsException(stderr)
        else -> return emptyList()
      }
    }
    val rootFile = LocalFileSystem.getInstance().findFileByPath(root) ?: return emptyList()
    LOG.debug("found root file: ${rootFile.path}")

    return unmappedRoots.filter { VfsUtil.isAncestor(rootFile, it, false) }
  }

  private fun parseP4Info(output: String): String? {
    var result: String? = null
    for (line in output.lineSequence()) {
      if (line.startsWith(PerforceRunner.CLIENT_ROOT)) {
        val path = FileUtil.toSystemIndependentName(line.removePrefix(PerforceRunner.CLIENT_ROOT).trim())
        if (result != null) {
          LOG.warn("unexpected: two roots in output: '$result' and '$path'")
          return null
        }
        result = path
      }
    }
    LOG.debug("parseP4Info - root path: $result")
    return result
  }

  private fun collectUnmappedDirs(mappedDirs: Set<VirtualFile>,
                                  projectRoots: Collection<VirtualFile>): List<VirtualFile> {
    return projectRoots.filter { root ->
      generateSequence(root) { it.parent }
        .none { parent -> mappedDirs.contains(parent) }
    }
  }

  companion object {
    private val LOG = Logger.getInstance(P4RootChecker::class.java)
  }
}
