package org.angular2.editor

import com.intellij.platform.trialPromotion.common.PluginWithFreeTierEditorNotificationProvider
import com.intellij.polySymbols.context.PolyContext
import com.intellij.polySymbols.framework.PolySymbolFramework.Companion.KIND_FRAMEWORK
import com.intellij.psi.PsiDocumentManager
import com.intellij.psi.PsiFile

internal class AngularFreeTierEditorNotificationProvider : PluginWithFreeTierEditorNotificationProvider {

  override val pluginName: String
    get() = "Angular"

  override fun showNotification(file: PsiFile): Boolean {
    if (PolyContext.get(KIND_FRAMEWORK, file) != "angular") return false

    if (file.name.endsWith(".ts")) {
      val document = PsiDocumentManager.getInstance(file.project).getDocument(file) ?: return false
      val text = document.charsSequence
      // In TypeScript files show banner only if the file has a component, directive, module or pipe
      if (!(text.contains("@Component") || text.contains("@Directive")
            || text.contains("@NgModule") || text.contains("@Pipe")
            || text.contains("createComponent")))
        return false
    }
    // In HTML files show the banner always
    else if (!file.name.endsWith(".html"))
      return false
    return true
  }
}
