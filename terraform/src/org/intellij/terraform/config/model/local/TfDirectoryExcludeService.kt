// Copyright 2000-2026 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package org.intellij.terraform.config.model.local

import com.intellij.openapi.application.readAndEdtWriteAction
import com.intellij.openapi.components.Service
import com.intellij.openapi.module.Module
import com.intellij.openapi.project.Project
import com.intellij.openapi.roots.ModuleRootModificationUtil
import com.intellij.openapi.roots.ProjectFileIndex
import com.intellij.openapi.vfs.VirtualFile
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

@Service(Service.Level.PROJECT)
internal class TfDirectoryExcludeService(val project: Project, val scope: CoroutineScope) {

  fun excludeTerraformDirs(terraformDirs: Set<VirtualFile>) {
    val validTerraformDirs = terraformDirs
      .asSequence()
      .filter(::isValidTfDirectory)
      .toList()
    if (validTerraformDirs.isEmpty()) return

    scope.launch {
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
}

private data class ModuleContentRoot(
  val module: Module,
  val contentRoot: VirtualFile,
)

internal fun isValidTfDirectory(file: VirtualFile): Boolean =
  file.isValid && file.isDirectory && file.name == TF_DIRECTORY_NAME

internal const val TF_DIRECTORY_NAME = ".terraform"