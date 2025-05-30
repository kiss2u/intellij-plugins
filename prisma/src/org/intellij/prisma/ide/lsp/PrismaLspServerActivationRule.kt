// Copyright 2000-2024 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package org.intellij.prisma.ide.lsp

import com.intellij.lang.typescript.compiler.languageService.TypeScriptLanguageServiceUtil
import com.intellij.lang.typescript.lsp.LspServerActivationRule
import com.intellij.lang.typescript.lsp.ServiceActivationHelper
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import org.intellij.prisma.lang.PrismaFileType

object PrismaLspServerActivationRule : LspServerActivationRule(PrismaLspServerLoader, PrismaActivationHelper) {
  override fun isFileAcceptable(file: VirtualFile): Boolean {
    if (!TypeScriptLanguageServiceUtil.IS_VALID_FILE_FOR_SERVICE.value(file)) return false
    return file.fileType == PrismaFileType
  }
}

object PrismaActivationHelper : ServiceActivationHelper {
  override fun isProjectContext(project: Project, context: VirtualFile): Boolean =
    context.fileType == PrismaFileType

  override fun isEnabledInSettings(project: Project): Boolean =
    PrismaServiceSettings.getInstance(project).serviceMode == PrismaServiceMode.ENABLED

  override fun isEnabledByEnvironment(project: Project, context: VirtualFile): Boolean =
    !ApplicationManager.getApplication().isUnitTestMode
}