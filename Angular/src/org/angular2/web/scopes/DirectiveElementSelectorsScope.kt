// Copyright 2000-2021 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package org.angular2.web.scopes

import com.intellij.model.Pointer
import com.intellij.psi.PsiFile
import com.intellij.psi.util.PsiModificationTracker
import com.intellij.polySymbols.PolySymbol
import com.intellij.polySymbols.PolySymbolQualifiedKind
import com.intellij.polySymbols.utils.PolySymbolScopeWithCache
import org.angular2.Angular2Framework
import org.angular2.entities.Angular2EntitiesProvider
import org.angular2.web.Angular2DirectiveSymbolWrapper
import org.angular2.web.NG_DIRECTIVE_ELEMENT_SELECTORS

internal class DirectiveElementSelectorsScope(file: PsiFile)
  : PolySymbolScopeWithCache<PsiFile, Unit>(Angular2Framework.ID, file.project, file, Unit) {

  override fun provides(qualifiedKind: PolySymbolQualifiedKind): Boolean =
    qualifiedKind == NG_DIRECTIVE_ELEMENT_SELECTORS

  override fun createPointer(): Pointer<DirectiveElementSelectorsScope> =
    Pointer.hardPointer(this)

  override fun initialize(consumer: (PolySymbol) -> Unit, cacheDependencies: MutableSet<Any>) {
    Angular2EntitiesProvider.getAllElementDirectives(project)
      .asSequence()
      .filter { (name, list) -> list.isNotEmpty() && name.isNotEmpty() }
      .forEach { (name, list) ->
        list.forEach { directive ->
          consumer(Angular2DirectiveSymbolWrapper.create(directive, directive.selector.getSymbolForElement(name), dataHolder))
        }
      }
    cacheDependencies.add(PsiModificationTracker.MODIFICATION_COUNT)
  }

}