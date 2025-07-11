// Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package org.angular2.lang.html.psi.impl

import com.intellij.lang.ASTNode
import com.intellij.psi.PsiElementVisitor
import com.intellij.psi.PsiReference
import com.intellij.psi.PsiReferenceService
import com.intellij.psi.XmlElementVisitor
import com.intellij.psi.impl.source.resolve.reference.ReferenceProvidersRegistry
import com.intellij.psi.tree.IElementType
import org.angular2.lang.html.psi.Angular2HtmlElementVisitor
import org.angular2.lang.html.psi.Angular2HtmlPropertyBinding
import org.angular2.lang.html.psi.PropertyBindingType
import org.angular2.lang.html.stub.impl.Angular2HtmlBoundAttributeStubImpl

internal class Angular2HtmlPropertyBindingImpl : Angular2HtmlPropertyBindingBase, Angular2HtmlPropertyBinding {


  constructor(stub: Angular2HtmlBoundAttributeStubImpl, nodeType: IElementType)
    : super(stub, nodeType)

  constructor(node: ASTNode) : super(node)

  override fun getReferences(hints: PsiReferenceService.Hints): Array<PsiReference> {
    if (bindingType == PropertyBindingType.CLASS) {
      if (hints.offsetInElement != null) {
        val nameElement = nameElement
        if (nameElement == null || hints.offsetInElement!! > nameElement.startOffsetInParent + nameElement.textLength) {
          return PsiReference.EMPTY_ARRAY
        }
      }
      return ReferenceProvidersRegistry.getReferencesFromProviders(this)
    }
    return super.getReferences(hints)
  }

  override fun accept(visitor: PsiElementVisitor) {
    when (visitor) {
      is Angular2HtmlElementVisitor -> {
        visitor.visitPropertyBinding(this)
      }
      is XmlElementVisitor -> {
        visitor.visitXmlAttribute(this)
      }
      else -> {
        visitor.visitElement(this)
      }
    }
  }
}