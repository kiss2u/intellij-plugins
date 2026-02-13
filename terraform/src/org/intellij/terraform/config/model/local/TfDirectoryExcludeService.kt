// Copyright 2000-2026 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package org.intellij.terraform.config.model.local

import com.intellij.openapi.application.readAndEdtWriteAction
import com.intellij.openapi.components.Service
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.module.Module
import com.intellij.openapi.project.Project
import com.intellij.openapi.roots.ModuleRootModificationUtil
import com.intellij.openapi.roots.ProjectFileIndex
import com.intellij.openapi.vfs.VirtualFile
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.coroutines.cancellation.CancellationException

// Inspired by com.intellij.vcs.github.ultimate.features.cache.listeners.LocalActionsFileChangeListener
@Service(Service.Level.PROJECT)
internal class TfDirectoryExcludeService(val project: Project, val scope: CoroutineScope) {

  private val queue = Channel<List<VirtualFile>>(Channel.BUFFERED)

  init {
    scope.launch {
      while (isActive) {
        try {
          val files = receiveAllAvailable()
          process(files)
        }
        catch (e: Exception) {
          if (e is CancellationException) throw e
          Log.warn("Failed to exclude directories", e)
        }
      }
    }
  }

  fun excludeTerraformDirs(terraformDirs: List<VirtualFile>) {
    if (queue.trySend(terraformDirs).isFailure) {
      Log.warn("Failed to exclude directories: $terraformDirs")
    }
  }

  private suspend fun receiveAllAvailable(): List<VirtualFile> {
    val files = mutableListOf<VirtualFile>()
    files.addAll(queue.receive())
    while (currentCoroutineContext().isActive) {
      val remaining = queue.tryReceive().getOrNull() ?: break
      files.addAll(remaining)
    }
    return files
  }

  private suspend fun process(files: List<VirtualFile>) {
    val validTerraformDirs = files.asSequence()
      .filter(::isValidTfDirectory)
      .distinct()
      .toList()
    if (validTerraformDirs.isEmpty()) return

    readAndEdtWriteAction {
      val fileIndex = ProjectFileIndex.getInstance(project)
      val dirsToExclude = validTerraformDirs.filter { !fileIndex.isExcluded(it) }
      if (dirsToExclude.isEmpty())
        return@readAndEdtWriteAction value(Unit)

      val groupedFolders = mutableMapOf<ModuleContentRoot, MutableList<String>>()
      for (dir in dirsToExclude) {
        val module = fileIndex.getModuleForFile(dir) ?: continue
        val contentRoot = fileIndex.getContentRootForFile(dir) ?: continue

        val key = ModuleContentRoot(module, contentRoot)
        groupedFolders.getOrPut(key) { mutableListOf() }.add(dir.url)
      }
      if (groupedFolders.isEmpty())
        return@readAndEdtWriteAction value(Unit)

      writeAction {
        for ((key, urls) in groupedFolders) {
          ModuleRootModificationUtil.updateExcludedFolders(key.module, key.contentRoot, emptyList(), urls)
        }
      }
    }
  }
}

private val Log = logger<TfDirectoryExcludeService>()

private data class ModuleContentRoot(
  val module: Module,
  val contentRoot: VirtualFile,
)

internal fun isValidTfDirectory(file: VirtualFile): Boolean =
  file.isValid && file.isDirectory && file.name == TF_DIRECTORY_NAME

internal const val TF_DIRECTORY_NAME = ".terraform"