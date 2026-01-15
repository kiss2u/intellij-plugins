package org.jetbrains.qodana.inspectionKts.mcp

import com.intellij.openapi.extensions.ExtensionPointName
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiFile
import java.nio.file.Path

/**
 * Extension point for creating PSI files in MCP tools.
 * Implementations can provide language-specific PSI file creation logic,
 * such as setting up proper context modules for Kotlin files.
 */
interface McpPsiFileFactory {
  companion object {
    @JvmField
    val EP_NAME: ExtensionPointName<McpPsiFileFactory> =
      ExtensionPointName.create("org.jetbrains.qodana.inspectionKts.mcp.customPsiFileFactory")

    /**
     * Creates a PSI file using the appropriate factory for the given path.
     * Falls back to default implementation if no specific factory is found.
     */
    suspend fun createPsiFile(project: Project, contextPath: Path, content: String): PsiFile {
      for (factory in EP_NAME.extensionList) {
        if (factory.canHandle(contextPath)) {
          val psiFile = factory.createFile(project, contextPath, content)
          if (psiFile != null) return psiFile
        }
      }
      return DefaultMcpPsiFileFactory.createFile(project, contextPath, content)
    }
  }

  /**
   * Returns true if this factory can handle this path.
   */
  fun canHandle(contextPath: Path): Boolean

  /**
   * Creates a PSI file with the given path and content.
   * Implementations may set up additional context (e.g., context modules for Kotlin).
   */
  suspend fun createFile(project: Project, contextPath: Path, content: String): PsiFile?
}
