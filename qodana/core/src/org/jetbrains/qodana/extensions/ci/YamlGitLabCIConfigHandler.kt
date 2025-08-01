package org.jetbrains.qodana.extensions.ci

import com.intellij.openapi.application.readAction
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiManager
import org.jetbrains.yaml.psi.YAMLFile
import org.jetbrains.yaml.psi.YAMLMapping
import org.jetbrains.yaml.psi.YAMLSequence

class YamlGitLabCIConfigHandler : GitLabCIConfigHandler {
  override suspend fun isQodanaPipelinePresent(project: Project, virtualFile: VirtualFile): Boolean {
    return readAction {
      val file = PsiManager.getInstance(project).findFile(virtualFile)
      val topMapping = (file as? YAMLFile)?.documents?.get(0)?.topLevelValue as? YAMLMapping ?: return@readAction false
      val isCliUsed = topMapping.keyValues.any {
        val value = it.value as? YAMLMapping ?: return@any false
        value.getKeyValueByKey("script")?.value?.text?.contains("qodana") ?: false
      }
      val isComponentIncluded = topMapping.keyValues.any { kv ->
        val value = kv.value as? YAMLSequence ?: return@any false
        value.children.any { child -> child.text.contains("component") && child.text.contains("qodana") }
      }
      isCliUsed || isComponentIncluded
    }
  }
}