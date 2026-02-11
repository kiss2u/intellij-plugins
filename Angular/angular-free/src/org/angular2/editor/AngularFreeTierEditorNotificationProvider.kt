package org.angular2.editor

import com.intellij.platform.trialPromotion.common.PluginWithFreeTierEditorNotificationProvider
import com.intellij.polySymbols.context.PolyContext
import com.intellij.polySymbols.framework.PolySymbolFramework.Companion.KIND_FRAMEWORK
import com.intellij.psi.PsiDocumentManager
import com.intellij.psi.PsiFile
import org.angular2.lang.Angular2Bundle
import org.jetbrains.annotations.Nls

internal class AngularFreeTierEditorNotificationProvider : PluginWithFreeTierEditorNotificationProvider {

  @Nls
  override fun getNotificationMessage(file: PsiFile): String? =
    if (PolyContext.get(KIND_FRAMEWORK, file) == "angular" && isFileWithAngularContent(file))
      Angular2Bundle.message("angular.free.tier.notification.message")
    else
      null

  private fun isFileWithAngularContent(file: PsiFile): Boolean =
    (file.name.endsWith(".ts") &&
     angularEntityRegex.containsMatchIn(PsiDocumentManager.getInstance(file.project).getDocument(file)?.charsSequence ?: ""))
    || file.name.endsWith(".html")

}

private val angularEntityRegex = Regex("@Component|@Directive|@NgModule|@Pipe|createComponent", RegexOption.MULTILINE)
