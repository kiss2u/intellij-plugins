// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package org.angular2.lang.svg

import com.intellij.psi.impl.source.html.HtmlFileImpl
import com.intellij.psi.stubs.PsiFileStub
import com.intellij.psi.tree.IStubFileElementType
import org.angular2.lang.stubs.Angular2HtmlLanguageStubDefinition

class Angular181SvgFileElementType private constructor()
  : IStubFileElementType<PsiFileStub<HtmlFileImpl>>("svg.angular181", Angular181SvgLanguage) {
  override fun getStubVersion(): Int {
    return Angular2HtmlLanguageStubDefinition.angular2HtmlStubVersion
  }

  companion object {
    @JvmField
    val INSTANCE: IStubFileElementType<PsiFileStub<HtmlFileImpl>> = Angular181SvgFileElementType()
  }
}