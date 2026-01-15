package org.jetbrains.qodana.inspectionKts.mcp

import com.intellij.openapi.application.writeAction
import com.intellij.openapi.fileTypes.FileTypeRegistry
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiFileFactory
import java.nio.file.Path

/**
 * Default implementation that creates PSI files using the standard PsiFileFactory.
 */
internal object DefaultMcpPsiFileFactory {
  suspend fun createFile(project: Project, contextPath: Path, content: String): PsiFile {
    return writeAction {
      val fileName = contextPath.fileName.toString()
      val fileType = FileTypeRegistry.getInstance().getFileTypeByFileName(fileName)
      PsiFileFactory.getInstance(project).createFileFromText(fileName, fileType, content)
    }
  }
}
