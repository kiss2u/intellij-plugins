// Copyright 2000-2025 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package org.intellij.terraform.hcl.psi

import com.intellij.lang.injection.InjectedLanguageManager
import com.intellij.openapi.progress.ProgressManager
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiReference
import com.intellij.psi.PsiWhiteSpace
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.psi.util.parentOfType
import com.intellij.psi.util.siblings
import org.intellij.terraform.config.Constants.HCL_CONNECTION_IDENTIFIER
import org.intellij.terraform.config.Constants.HCL_LIFECYCLE_IDENTIFIER
import org.intellij.terraform.config.Constants.HCL_POSTCONDITION_BLOCK_IDENTIFIER
import org.intellij.terraform.config.Constants.HCL_PROVISIONER_IDENTIFIER
import org.intellij.terraform.config.model.Module
import org.intellij.terraform.config.model.Variable
import org.intellij.terraform.config.model.getTerraformModule
import org.intellij.terraform.config.patterns.TfPsiPatterns
import org.intellij.terraform.hcl.psi.common.BaseExpression
import org.intellij.terraform.hcl.psi.common.Identifier
import org.intellij.terraform.hcl.psi.common.IndexSelectExpression
import org.intellij.terraform.hcl.psi.common.SelectExpression
import org.intellij.terraform.hil.psi.impl.getHCLHost

/**
 * Various helper methods for working with PSI of JSON language.
 */
internal object HCLPsiUtil {
  fun getIdentifierPsi(element: HCLElement): PsiElement? = when (element) {
    is HCLProperty -> element.firstChild
    is HCLBlock -> element.children.lastOrNull { it !is HCLObject }
    else -> null
  }

  /**
   * Checks that PSI element represents exact key of HCL property
   *
   * @param element PSI element to check
   * @return whether this PSI element is property key
   */
  fun isPropertyKey(element: PsiElement): Boolean {
    val parent = element.parent
    return parent is HCLProperty && element === parent.nameElement
  }

  /**
   * Checks that PSI element is part of a key of HCL property
   * E.g. `a.b = true`
   *
   * @param element PSI element to check
   * @return whether this PSI element is property key
   */

  fun isPartOfPropertyKey(element: PsiElement): Boolean {
    val property = PsiTreeUtil.getParentOfType(element, HCLProperty::class.java, true)
    if (property != null) {
      if (PsiTreeUtil.isAncestor(property.nameElement, element, false)) return true
    }
    return false
  }

  fun isBlockNameElement(element: HCLStringLiteral, ordinal: Int): Boolean {
    val parent = element.parent as? HCLBlock ?: return false
    return parent.nameElements.getOrNull(ordinal) === element
  }

  /**
   * Checks that PSI element represents value of HCL property
   * E.g. `a = b`
   *
   * @param element PSI element to check
   * @return whether this PSI element is property value
   */
  fun isPropertyValue(element: PsiElement): Boolean {
    val parent = element.parent
    return parent is HCLProperty && element === parent.value
  }

  /**
   * Checks that PSI element represents part of a value of HCL property
   * E.g. `a = b.c`
   *
   * @param element PSI element to check
   * @return whether this PSI element is property value
   */
  fun isPartOfPropertyValue(element: PsiElement): Boolean {
    val property = PsiTreeUtil.getParentOfType(element, HCLProperty::class.java, true)
    if (property != null) {
      if (PsiTreeUtil.isAncestor(property.value, element, false)) return true
    }
    return false
  }

  /**
   * Returns text of the given PSI element. Unlike obvious [PsiElement.getText] this method unescapes text of the element if latter
   * belongs to injected code fragment using [InjectedLanguageManager.getUnescapedText].

   * @param element PSI element which text is needed
   * *
   * @return text of the element with any host escaping removed
   */
  fun getElementTextWithoutHostEscaping(element: PsiElement): String {
    val manager = InjectedLanguageManager.getInstance(element.project)
    if (manager.isInjectedFragment(element.containingFile)) {
      return manager.getUnescapedText(element)
    }
    else {
      return element.text
    }
  }

  /**
   * Returns content of the string literal (without escaping) striving to preserve as much of user data as possible.
   *
   *  * If literal length is greater than one and it starts and ends with the same quote and the last quote is not escaped, returns
   * text without first and last characters.
   *  * Otherwise if literal still begins with a quote, returns text without first character only.
   *  * Returns unmodified text in all other cases.
   *

   * @param text presumably result of [HCLStringLiteral.getText]
   * *
   * @return
   */
  @JvmStatic
  fun stripQuotes(text: String): String {
    if (text.isNotEmpty()) {
      val firstChar = text[0]
      val lastChar = text[text.length - 1]
      if (firstChar == '\'' || firstChar == '"') {
        if (text.length > 1 && firstChar == lastChar && !isEscapedChar(text, text.length - 1)) {
          return text.substring(1, text.length - 1)
        }
        return text.substring(1)
      }
    }
    return text
  }

  /**
   * Checks that character in given position is escaped with backslashes.

   * @param text     text character belongs to
   * *
   * @param position position of the character
   * *
   * @return whether character at given position is escaped, i.e. preceded by odd number of backslashes
   */
  fun isEscapedChar(text: String, position: Int): Boolean {
    var count = 0
    var i = position - 1
    while (i >= 0 && text[i] == '\\') {
      count++
      i--
    }
    return count % 2 != 0
  }

  fun getReferencesSelectAware(e: PsiElement?): Array<out PsiReference> {
    ProgressManager.checkCanceled()
    if (e == null) return PsiReference.EMPTY_ARRAY
    if (e is SelectExpression<*>) {
      if (e is IndexSelectExpression<*>) {
        return getReferencesSelectAware(e.from)
      }
      val field = e.field ?: return PsiReference.EMPTY_ARRAY
      if (field is HCLNumberLiteral) {
        return getReferencesSelectAware(e.from)
      }
      return field.references
    }
    if (e is HCLForObjectExpression)
      return e.value.references
    return e.references
  }

  fun isUnderPropertyUnderPropertyWithObjectValue(element: PsiElement?): Boolean {
    val property = PsiTreeUtil.getParentOfType(element, HCLProperty::class.java, true) ?: return false
    return property.parent is HCLObject && property.parent?.parent is HCLProperty
  }

  fun isUnderPropertyInsideObjectConditionalExpression(element: PsiElement?): Boolean {
    val property = PsiTreeUtil.getParentOfType(element, HCLProperty::class.java, true) ?: return false
    return property.parent is HCLObject && property.parent?.parent is HCLConditionalExpression
  }

  fun isUnderPropertyUnderPropertyWithObjectValueAndArray(element: PsiElement?): Boolean {
    val property = PsiTreeUtil.getParentOfType(element, HCLProperty::class.java, true) ?: return false
    return property.parent is HCLObject && property.parent?.parent is HCLArray && property.parent?.parent?.parent is HCLProperty
  }

  fun isUnderPropertyInsideObjectArgument(element: PsiElement?): Boolean {
    val property = PsiTreeUtil.getParentOfType(element, HCLProperty::class.java, true) ?: return false
    return property.parent is HCLObject && property.parent?.parent is HCLParameterList
  }

  fun PsiElement.getRequiredProviderProperty(): HCLProperty? {
    val providerProperty = when (this) {
      is HCLProperty -> this
      is Identifier if this.parent is HCLProperty -> this.parent as HCLProperty
      else -> return null
    }

    val requiredProvidersObject = providerProperty.parent as? HCLObject ?: return null
    val providerBlock = requiredProvidersObject.parent as? HCLBlock ?: return null
    if (!TfPsiPatterns.RequiredProvidersBlock.accepts(providerBlock)) {
      return null
    }
    return providerProperty
  }

  @JvmStatic
  fun PsiElement.getPrevSiblingNonWhiteSpace(): PsiElement? =
    this.siblings(forward = false, withSelf = false).firstOrNull { it !is PsiWhiteSpace }

  @JvmStatic
  fun PsiElement.getNextSiblingNonWhiteSpace(): PsiElement? =
    this.siblings(forward = true, withSelf = false).firstOrNull { it !is PsiWhiteSpace }
}

internal fun getTerraformModule(element: BaseExpression): Module? {
  val host = element.getHCLHost() ?: return null
  return host.getTerraformModule()
}

internal fun getLocalDefinedVariables(element: BaseExpression): List<Variable> {
  return getTerraformModule(element)?.getAllVariables() ?: emptyList()
}

internal fun getDefinedLocalsInModule(element: BaseExpression): List<String> {
  return getTerraformModule(element)?.getAllLocals()?.map { it.first } ?: emptyList()
}

internal fun getHclBlockForSelfContext(position: BaseExpression): HCLBlock? {
  val host = position.getHCLHost() ?: return null
  return getProvisionerOfResource(host) ?: getConnectionOfResource(host) ?: getPostConditionOfBlock(host)
}

internal fun getProvisionerOfResource(host: HCLElement): HCLBlock? {
  val provisioner = host.parentOfType<HCLBlock>() ?: return null

  return when (provisioner.getNameElementUnquoted(0)) {
    HCL_CONNECTION_IDENTIFIER -> getProvisionerOfResource(provisioner)
    HCL_PROVISIONER_IDENTIFIER -> getParentResourceBlock(provisioner)
    else -> null
  }
}

internal fun getConnectionOfResource(host: HCLElement): HCLBlock? {
  val connection = host.parentOfType<HCLBlock>() ?: return null

  return when (connection.getNameElementUnquoted(0)) {
    HCL_CONNECTION_IDENTIFIER -> getParentResourceBlock(connection)
    else -> null
  }
}

internal fun getPostConditionOfBlock(host: HCLElement): HCLBlock? {
  val postCondition = host.parentOfType<HCLBlock>() ?: return null
  if (postCondition.getNameElementUnquoted(0) != HCL_POSTCONDITION_BLOCK_IDENTIFIER)
    return null

  val lifecycle = postCondition.parentOfType<HCLBlock>()
  if (lifecycle?.getNameElementUnquoted(0) != HCL_LIFECYCLE_IDENTIFIER) {
    return null
  }

  val resourceOrData = lifecycle.parentOfType<HCLBlock>()
  return if (TfPsiPatterns.ResourceRootBlock.accepts(resourceOrData) || TfPsiPatterns.DataSourceRootBlock.accepts(resourceOrData))
    resourceOrData
  else null
}

internal fun getParentResourceBlock(element: HCLElement): HCLBlock? {
  val resource = element.parentOfType<HCLBlock>()
  return if (TfPsiPatterns.ResourceRootBlock.accepts(resource)) resource else null
}

internal fun getResource(position: BaseExpression): HCLBlock? {
  val host = position.getHCLHost() ?: return null
  return getParentResourceBlock(host)
}

internal fun getDataSource(position: BaseExpression): HCLBlock? {
  val host = position.getHCLHost() ?: return null
  val dataSource = host.parentOfType<HCLBlock>()
  return if (TfPsiPatterns.DataSourceRootBlock.accepts(dataSource)) dataSource else null
}

internal fun getContainingResourceOrDataSource(element: HCLElement?): HCLBlock? {
  if (element == null) return null
  return PsiTreeUtil.findFirstParent(element, true) {
    TfPsiPatterns.DataSourceRootBlock.accepts(it) ||
    TfPsiPatterns.ResourceRootBlock.accepts(it)
  } as? HCLBlock
}

internal fun getContainingResourceOrDataSourceOrModule(element: HCLElement?): HCLBlock? {
  if (element == null) return null
  return PsiTreeUtil.findFirstParent(element, true) {
    TfPsiPatterns.DataSourceRootBlock.accepts(it) ||
    TfPsiPatterns.ResourceRootBlock.accepts(it) ||
    TfPsiPatterns.ModuleRootBlock.accepts(it)
  } as? HCLBlock
}