// Copyright 2000-2026 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package org.jetbrains.vuejs.editor

import com.intellij.platform.trialPromotion.common.PluginWithFreeTierEditorNotificationProvider
import com.intellij.polySymbols.context.PolyContext
import com.intellij.polySymbols.framework.PolySymbolFramework.Companion.KIND_FRAMEWORK
import com.intellij.psi.PsiDocumentManager
import com.intellij.psi.PsiFile

internal class VueFreeTierEditorNotificationProvider : PluginWithFreeTierEditorNotificationProvider {

  override val pluginName: String
    get() = "Vue"

  override fun showNotification(file: PsiFile): Boolean {

    if (PolyContext.get(KIND_FRAMEWORK, file) != "vue") return false

    if (file.name.endsWith(".ts")) {
      val document = PsiDocumentManager.getInstance(file.project).getDocument(file)
                     ?: return false
      val text = document.charsSequence
      // In TypeScript files show banner only if the file has a Vue file import, or a vue import
      if (vueImportRegex.containsMatchIn(text))
        return false
    }
    // In HTML or Vue files show the banner always
    else if (!file.name.endsWith(".vue") && !file.name.endsWith(".html"))
      return false

    return true
  }

}

private val vueImportRegex = Regex("'vue'|\"vue\"|\\.vue['\"]", RegexOption.MULTILINE)