// Copyright 2000-2026 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package org.jetbrains.idea.perforce.checkout

import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.ValidationInfo
import com.intellij.openapi.util.Disposer
import com.intellij.openapi.vcs.CheckoutProvider
import com.intellij.openapi.vcs.ui.cloneDialog.VcsCloneDialogExtensionComponent
import javax.swing.JComponent

internal class PerforceCloneDialogExtensionComponent(
  project: Project,
) : VcsCloneDialogExtensionComponent() {

  private val cloneComponent = PerforceCloneDialogComponent(project, dialogStateListener)

  init {
    Disposer.register(this, cloneComponent)
  }

  override fun getView(): JComponent = cloneComponent.getView()

  override fun doClone(checkoutListener: CheckoutProvider.Listener) {
    cloneComponent.doClone(checkoutListener)
  }

  override fun doValidateAll(): List<ValidationInfo> = cloneComponent.doValidateAll()

  override fun getPreferredFocusedComponent(): JComponent = cloneComponent.getPreferredFocusedComponent()

  override fun onComponentSelected() {
    dialogStateListener.onOkActionNameChanged(cloneComponent.getOkButtonText())
    dialogStateListener.onOkActionEnabled(cloneComponent.isOkEnabled())
  }
}
