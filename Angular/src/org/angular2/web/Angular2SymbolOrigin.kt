// Copyright 2000-2022 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package org.angular2.web

import com.intellij.javascript.nodejs.PackageJsonData
import com.intellij.polySymbols.js.types.TypeScriptSymbolTypeSupport
import com.intellij.lang.javascript.buildTools.npm.PackageJsonUtil
import com.intellij.openapi.util.Pair
import com.intellij.polySymbols.search.PsiSourcedPolySymbol
import com.intellij.polySymbols.PolySymbolOrigin
import com.intellij.polySymbols.utils.PolySymbolTypeSupport
import icons.AngularIcons
import org.angular2.Angular2Framework
import javax.swing.Icon

class Angular2SymbolOrigin(private val mySymbol: Angular2Symbol) : PolySymbolOrigin {

  private val versionAndName: Pair<String, String> by lazy(LazyThreadSafetyMode.PUBLICATION) {
    val source = if (mySymbol is PsiSourcedPolySymbol)
      (mySymbol as PsiSourcedPolySymbol).source
    else
      null
    val psiFile = source?.containingFile
    val virtualFile = psiFile?.virtualFile
    val pkgJson = if (virtualFile != null) PackageJsonUtil.findUpPackageJson(virtualFile) else null
    val data = if (pkgJson != null) PackageJsonData.getOrCreate(pkgJson) else null
    if (data != null)
      Pair.create(data.name, data.version?.toString())
    else
      Pair.create(null, null)
  }

  override val framework: String
    get() = Angular2Framework.ID

  override val library: String?
    get() = versionAndName.first

  override val version: String?
    get() = versionAndName.second

  override val defaultIcon: Icon
    get() = AngularIcons.Angular2

  override val typeSupport: PolySymbolTypeSupport?
    get() = TypeScriptSymbolTypeSupport()

  override fun equals(other: Any?): Boolean =
    other === this
    || other is Angular2SymbolOrigin
    && other.versionAndName == versionAndName

  override fun hashCode(): Int =
    versionAndName.hashCode()

  companion object {
    val empty: PolySymbolOrigin = PolySymbolOrigin.create(Angular2Framework.ID,
                                                          library = "@angular/core",
                                                          defaultIcon = AngularIcons.Angular2,
                                                          typeSupport = TypeScriptSymbolTypeSupport())
  }
}
