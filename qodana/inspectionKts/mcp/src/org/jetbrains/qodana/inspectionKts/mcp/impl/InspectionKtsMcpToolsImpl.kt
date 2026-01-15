@file:Suppress("RedundantSuspendModifier")

package org.jetbrains.qodana.inspectionKts.mcp.impl

import com.intellij.codeInspection.GlobalInspectionContext
import com.intellij.codeInspection.InspectionEngine
import com.intellij.codeInspection.InspectionManager
import com.intellij.codeInspection.InspectionManagerBase
import com.intellij.codeInspection.LocalInspectionTool
import com.intellij.codeInspection.LocalQuickFix
import com.intellij.codeInspection.ProblemDescriptor
import com.intellij.codeInspection.ProblemHighlightType
import com.intellij.codeInspection.ProblemsHolder
import com.intellij.codeInspection.QuickFix
import com.intellij.codeInspection.ex.DynamicInspectionDescriptor
import com.intellij.lang.annotation.HighlightSeverity
import com.intellij.lang.annotation.ProblemGroup
import com.intellij.mcpserver.util.resolveInProject
import com.intellij.openapi.application.readAction
import com.intellij.openapi.application.writeAction
import com.intellij.openapi.editor.colors.TextAttributesKey
import com.intellij.openapi.fileTypes.FileTypeRegistry
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.TextRange
import com.intellij.openapi.vfs.LocalFileSystem
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiFileFactory
import com.intellij.psi.PsiManager
import com.intellij.psi.util.elementType
import org.jetbrains.qodana.inspectionKts.InspectionKtsFileStatus
import org.jetbrains.qodana.inspectionKts.KtsInspectionsManager
import org.jetbrains.qodana.inspectionKts.examples.InspectionKtsExample
import org.jetbrains.qodana.inspectionKts.mcp.InspectionKtsRunResult
import org.jetbrains.qodana.inspectionKts.mcp.InspectionProblem
import org.jetbrains.qodana.inspectionKts.mcp.McpPsiFileFactory
import org.jetbrains.qodana.inspectionKts.templates.InspectionKtsTemplate
import java.nio.file.Path
import kotlin.io.path.createTempFile
import kotlin.io.path.writeText

private val ANY_LANGUAGE_TEMPLATES = setOf(
  "ANY_LANGUAGE_GLOBAL",
  "ANY_LANGUAGE",
)

private val IGNORED_EXAMPLES = setOf(
  "JSON and YAML"
)

internal suspend fun generatePsiTreeImpl(project: Project, code: String, language: String): String {
  val fileExtension = when (language.lowercase()) {
    "java" -> "java"
    "kotlin", "kt" -> "kt"
    else -> return "Error: Unsupported language '$language'. Supported: Java, Kotlin"
  }

  val psiFile = writeAction {
    val fileType = FileTypeRegistry.getInstance().getFileTypeByExtension(fileExtension)
    PsiFileFactory.getInstance(project).createFileFromText("Placeholder.$fileExtension", fileType, code)
  }

  return generatePsiTreeText(psiFile)
}

internal suspend fun generateInspectionKtsExamplesImpl(language: String, includeAdditionalExamples: Boolean): String {
  val languageUpper = language.uppercase()
  val languageTemplates = ANY_LANGUAGE_TEMPLATES + languageUpper

  val templateBlocks = InspectionKtsTemplate.Provider.templates()
    .filter { it.uiDescriptor.id in languageTemplates }
    .map { it.templateContent("AnExampleFileName.ReplaceMe") }
    .joinToString(separator = "\n") { content ->
      """
      <Example>
      $content
      </Example>
      """.trimIndent()
    }

  val additionalBlocks = if (includeAdditionalExamples) {
    InspectionKtsExample.Provider
      .examples()
      .filter { it.text !in IGNORED_EXAMPLES }
      .map {
        val content = it.resourceUrl.readText()
        val comment = "// Only Code of localInspection { ... }\n"
        comment + content
      }
      .joinToString("\n") { content ->
        """
        <Example>
        $content
        </Example>
        """.trimIndent()
      }
  }
  else ""

  return buildString {
    appendLine("<Examples>")
    appendLine(templateBlocks)
    if (additionalBlocks.isNotBlank()) {
      appendLine(additionalBlocks)
    }
    append("</Examples>")
  }
}

internal fun generateInspectionKtsApiImpl(language: String, wrapInTags: Boolean): String {
  val langName = when (language.lowercase()) {
    "java" -> "Java"
    "kotlin", "kt" -> "Kotlin"
    else -> return "Error: Unsupported language '$language'. Supported: Java, Kotlin"
  }

  val resourcePath = "apiClasses/classes$langName.txt"
  val classes = object {}.javaClass.classLoader.getResource(resourcePath)?.readText()
                ?: return "Error: Cannot find API documentation for $langName at $resourcePath"

  return if (wrapInTags) {
    """
    <API>
    <api.kt>
    $classes
    </api.kt>
    </API>
    """.trimIndent()
  }
  else {
    classes
  }
}

internal suspend fun runInspectionKtsImpl(
  project: Project,
  inspectionKtsCode: String,
  contextPath: String,
  targetFileContent: String?,
): InspectionKtsRunResult {
  // Step 1: Compile the inspection.kts
  val tempFile = createTempFile("mcp-inspection", ".inspection.kts")
  tempFile.writeText(inspectionKtsCode)

  val compiledFile = KtsInspectionsManager.getInstance(project).doCompileInspectionKtsFile(tempFile)

  when (compiledFile) {
    is InspectionKtsFileStatus.Cancelled -> {
      return InspectionKtsRunResult(
        success = false,
        compilationError = "Inspection compilation was cancelled"
      )
    }
    is InspectionKtsFileStatus.Compiling -> {
      return InspectionKtsRunResult(
        success = false,
        compilationError = "Inspection is still compiling (unexpected state)"
      )
    }
    is InspectionKtsFileStatus.Error -> {
      return InspectionKtsRunResult(
        success = false,
        compilationError = compiledFile.exception.message ?: "Unknown compilation error",
        compilationErrorDetails = compiledFile.exception.stackTraceToString()
      )
    }
    is InspectionKtsFileStatus.Compiled -> {
      val inspection = compiledFile.inspections.inspections.firstOrNull()
      if (inspection == null) {
        return InspectionKtsRunResult(
          success = false,
          compilationError = "No inspection created after compilation"
        )
      }

      val localTool = (inspection as? DynamicInspectionDescriptor.Local)?.tool
      if (localTool == null) {
        return InspectionKtsRunResult(
          success = false,
          compilationError = "Compiled inspection is not a local inspection tool"
        )
      }


      val filePath = project.resolveInProject(contextPath, true)
      // Step 2: Run the inspection on the target file
      val psiFile = if (targetFileContent == null) {
        val virtualFile = LocalFileSystem.getInstance().findFileByNioFile(filePath)
                          ?: return InspectionKtsRunResult(
                            success = false,
                            compilationError = "File not found at path: $contextPath"
                          )
        readAction {
          PsiManager.getInstance(project).findFile(virtualFile)
        } ?: return InspectionKtsRunResult(
          success = false,
          compilationError = "File not found at path: $contextPath"
        )
      }
      else {
        McpPsiFileFactory.createPsiFile(project, filePath, targetFileContent)
      }
      return runInspectionOnPsiFile(localTool, psiFile)
    }
  }
}

internal suspend fun runInspectionOnPsiFile(
  tool: LocalInspectionTool,
  psiFile: PsiFile,
): InspectionKtsRunResult {
  val holder = ProblemsHolder(VerificationInspectionManager(psiFile.getProject()), psiFile, false)

  readAction {
    InspectionEngine.withSession(psiFile, psiFile.textRange, psiFile.textRange, HighlightSeverity.INFORMATION, false, null) { session ->
      val visitor = tool.buildVisitor(holder, false, session)
      visitor.visitFile(psiFile)
    }
  }

  val problems = readAction {
    holder.results.mapNotNull { descriptor ->
      val lineNumber = computeLineNumber(psiFile, descriptor.psiElement)
      InspectionProblem(
        message = descriptor.descriptionTemplate,
        lineNumber = lineNumber,
        highlightType = descriptor.highlightType.name,
        startOffset = descriptor.psiElement?.textRange?.startOffset,
        endOffset = descriptor.psiElement?.textRange?.endOffset,
        elementText = descriptor.psiElement?.text?.take(100)
      )
    }
  }

  return InspectionKtsRunResult(
    success = true,
    problems = problems,
    problemCount = problems.size
  )
}

private suspend fun generatePsiTreeText(element: PsiElement, level: Int = 0): String {
  return readAction {
    val nodeType = getNodeTypeDescription(element) ?: return@readAction ""
    val childrenInfo = getChildrenInfo(element)
    val indentation = "  ".repeat(level)

    val currentLine = "$indentation$nodeType$childrenInfo\n"

    buildString {
      append(currentLine)

      val children = element.children.toList()
      for (child in children) {
        append(generatePsiTreeTextSync(child, level + 1))
      }
    }
  }
}

private fun generatePsiTreeTextSync(element: PsiElement, level: Int): String {
  val nodeType = getNodeTypeDescription(element) ?: return ""
  val childrenInfo = getChildrenInfo(element)
  val indentation = "  ".repeat(level)

  val currentLine = "$indentation$nodeType$childrenInfo\n"

  return buildString {
    append(currentLine)

    val children = element.children.toList()
    for (child in children) {
      append(generatePsiTreeTextSync(child, level + 1))
    }
  }
}

private fun getNodeTypeDescription(element: PsiElement): String? {
  return when {
    element.javaClass.simpleName == "LeafPsiElement" -> {
      val elementType = element.elementType
      "${elementType?.javaClass?.simpleName}($elementType)"
    }
    else -> element.javaClass.simpleName
  }
}

private fun getChildrenInfo(element: PsiElement): String {
  val hasChildren = element.children.isNotEmpty()
  val hasDirectChildren = element.firstChild != null

  return if (!hasDirectChildren && hasChildren) {
    " -> children retrieved with node.children()"
  }
  else {
    ""
  }
}

private fun computeLineNumber(psiFile: PsiElement, element: PsiElement?): Int {
  if (element == null) return -1
  val doc = psiFile.containingFile?.viewProvider?.document ?: return -1
  val start = element.textRange?.startOffset ?: return -1
  val zeroBased = doc.getLineNumber(start)
  return zeroBased + 1
}



class VerificationInspectionManager(project: Project) : InspectionManagerBase(project) {
  @Suppress("OVERRIDE_DEPRECATION", "removal")
  override fun createNewGlobalContext(reuse: Boolean): GlobalInspectionContext {
    throw IllegalStateException("Not supported")
  }

  override fun createNewGlobalContext(): GlobalInspectionContext {
    throw IllegalStateException("Not supported")
  }

  override fun createProblemDescriptor(
    psiElement: PsiElement,
    descriptionTemplate: String,
    fix: LocalQuickFix?,
    highlightType: ProblemHighlightType,
    onTheFly: Boolean,
  ): ProblemDescriptor {
    return VerificationProblemDescriptor(
      element = psiElement,
      description = descriptionTemplate,
      highlight = highlightType,
      fixes = listOfNotNull(fix),
      lineNumber = computeOneBasedLine(psiElement),
    )
  }

  override fun createProblemDescriptor(psiElement: PsiElement, descriptionTemplate: String, onTheFly: Boolean, fixes: Array<out LocalQuickFix>?, highlightType: ProblemHighlightType): ProblemDescriptor {
    return VerificationProblemDescriptor(
      element = psiElement,
      description = descriptionTemplate,
      highlight = highlightType,
      fixes = fixes.orEmpty().toList(),
      lineNumber = computeOneBasedLine(psiElement),
    )
  }

  private fun computeOneBasedLine(element: PsiElement): Int {
    val doc = element.containingFile?.viewProvider?.document
    val start = element.textRange?.startOffset ?: return -1
    val zeroBased = doc?.getLineNumber(start) ?: return -1
    return zeroBased + 1
  }
}

private class VerificationProblemDescriptor(
  private val element: PsiElement,
  private val description: String,
  private val highlight: ProblemHighlightType,
  private val fixes: List<LocalQuickFix>?,
  private val lineNumber: Int,
) : ProblemDescriptor {

  override fun getPsiElement(): PsiElement = element

  override fun getStartElement(): PsiElement = element

  override fun getEndElement(): PsiElement = element

  override fun getTextRangeInElement(): TextRange? = null

  override fun getLineNumber(): Int = lineNumber

  override fun getHighlightType(): ProblemHighlightType = highlight

  override fun isAfterEndOfLine(): Boolean = false

  override fun setTextAttributes(key: TextAttributesKey?) {}

  override fun getProblemGroup(): ProblemGroup? = null

  override fun setProblemGroup(problemGroup: ProblemGroup?) {}

  override fun showTooltip(): Boolean = false

  @Suppress("HardCodedStringLiteral")
  override fun getDescriptionTemplate(): String = description

  @Suppress("UNCHECKED_CAST")
  override fun getFixes(): Array<QuickFix<*>> = fixes.orEmpty().toTypedArray() as Array<QuickFix<*>>
}
