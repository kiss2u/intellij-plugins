// Copyright 2000-2021 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package org.angular2.codeInsight.tags

import com.intellij.polySymbols.html.StandardHtmlSymbol
import com.intellij.polySymbols.html.elements.HtmlElementSymbolDescriptor
import com.intellij.polySymbols.html.elements.HtmlElementSymbolInfo
import com.intellij.polySymbols.utils.unwrapMatchedSymbols
import com.intellij.psi.xml.XmlTag
import org.angular2.codeInsight.attributes.Angular2ApplicableDirectivesProvider
import org.angular2.entities.Angular2Directive
import org.angular2.web.Angular2DescriptorSymbolsProvider

class Angular2ElementDescriptor(info: HtmlElementSymbolInfo, tag: XmlTag) : HtmlElementSymbolDescriptor(info, tag) {

  @get:JvmName("isImplied")
  val implied: Boolean get() = tagInfoProvider.errorSymbols.isEmpty() && tagInfoProvider.nonDirectiveSymbols.isNotEmpty()

  /**
   * Represents most of the matched directives, even these out-of-scopes. Some directives
   * might be filtered out though, if there is a better match. This property should not
   * be used where perfect matching is required.
   *
   * Prefer to use [Angular2ApplicableDirectivesProvider] and [org.angular2.codeInsight.Angular2DeclarationsScope]
   */
  val sourceDirectives: List<Angular2Directive> get() = tagInfoProvider.directives

  private val tagInfoProvider by lazy(LazyThreadSafetyMode.PUBLICATION) { Angular2DescriptorSymbolsProvider(this.symbol) }

  override fun isCustomElement(): Boolean =
    symbol.unwrapMatchedSymbols()
      .none { it is StandardHtmlSymbol }

}