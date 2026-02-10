// Copyright 2000-2026 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package org.intellij.terraform.config.model.local

import com.intellij.openapi.components.service
import com.intellij.openapi.project.ProjectManager
import com.intellij.openapi.roots.ProjectFileIndex
import com.intellij.openapi.vfs.AsyncFileListener
import com.intellij.openapi.vfs.newvfs.events.VFileEvent

internal class TfDirectoryExcludeVfsListener : AsyncFileListener {
  override fun prepareChange(events: List<VFileEvent>): AsyncFileListener.ChangeApplier? {
    val terraformDirs = events.asSequence()
      .mapNotNull { it.file }
      .filter { isValidTfDirectory(it) }
      .toList()
    if (terraformDirs.isEmpty()) return null

    return object : AsyncFileListener.ChangeApplier {
      override fun afterVfsChange() {
        val projects = ProjectManager.getInstance().openProjects

        for (project in projects) {
          val fileIndex = ProjectFileIndex.getInstance(project)
          val dirsToExclude = terraformDirs.filter { fileIndex.isInProject(it) }.toSet()

          if (dirsToExclude.isNotEmpty()) {
            project.service<TfDirectoryExcludeService>().excludeTerraformDirs(dirsToExclude)
          }
        }
      }
    }
  }
}