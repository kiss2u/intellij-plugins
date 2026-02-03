// Copyright 2000-2026 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package org.jetbrains.idea.perforce.checkout

import com.intellij.openapi.application.ModalityState
import com.intellij.openapi.project.Project
import com.intellij.openapi.vcs.ui.cloneDialog.VcsCloneDialogExtension
import com.intellij.openapi.vcs.ui.cloneDialog.VcsCloneDialogExtensionComponent
import com.intellij.openapi.vcs.ui.cloneDialog.VcsCloneDialogExtensionStatusLine
import org.jetbrains.idea.perforce.PerforceBundle
import org.jetbrains.idea.perforce.PerforceIcons
import javax.swing.Icon

internal class PerforceCloneDialogExtension : VcsCloneDialogExtension {
  override fun getName(): String = PerforceBundle.message("perforce")

  override fun getIcon(): Icon = PerforceIcons.PerforceP4

  override fun getAdditionalStatusLines(): List<VcsCloneDialogExtensionStatusLine> = emptyList()

  override fun createMainComponent(
    project: Project,
    modalityState: ModalityState,
  ): VcsCloneDialogExtensionComponent = PerforceCloneDialogExtensionComponent(project)
}
