// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package org.jetbrains.vuejs.libraries.vuex.index

import com.intellij.lang.ASTNode
import com.intellij.lang.javascript.JSElementTypes
import com.intellij.lang.javascript.JSTokenTypes
import com.intellij.lang.javascript.index.FrameworkIndexingHandler
import com.intellij.lang.javascript.index.JSSymbolUtil
import com.intellij.lang.javascript.psi.*
import com.intellij.lang.javascript.psi.ecma6.impl.JSStringTemplateExpressionImpl.isStringTemplateExpressionWithoutArguments
import com.intellij.lang.javascript.psi.impl.JSCallExpressionImpl
import com.intellij.lang.javascript.psi.impl.JSLiteralExpressionImpl.isQuotedLiteral
import com.intellij.lang.javascript.psi.impl.JSReferenceExpressionImpl
import com.intellij.lang.javascript.psi.resolve.JSEvaluateContext
import com.intellij.lang.javascript.psi.resolve.JSTypeEvaluator
import com.intellij.lang.javascript.psi.stubs.JSElementIndexingData
import com.intellij.lang.javascript.psi.stubs.JSImplicitElement
import com.intellij.lang.javascript.psi.stubs.JSImplicitElementStructure
import com.intellij.lang.javascript.psi.stubs.impl.JSImplicitElementImpl
import com.intellij.lang.javascript.psi.types.JSContext
import com.intellij.lang.javascript.psi.types.JSNamedTypeFactory
import com.intellij.lang.javascript.psi.types.JSTypeSource
import com.intellij.psi.PsiElement
import com.intellij.psi.stubs.IndexSink
import com.intellij.util.asSafely
import org.jetbrains.vuejs.index.createIndexRecord
import org.jetbrains.vuejs.libraries.vuex.VuexUtils.CREATE_STORE
import org.jetbrains.vuejs.libraries.vuex.VuexUtils.PROP_NAMESPACED
import org.jetbrains.vuejs.libraries.vuex.VuexUtils.PROP_ROOT
import org.jetbrains.vuejs.libraries.vuex.VuexUtils.REGISTER_MODULE
import org.jetbrains.vuejs.libraries.vuex.VuexUtils.STORE
import org.jetbrains.vuejs.libraries.vuex.VuexUtils.VUEX_MAPPERS
import org.jetbrains.vuejs.libraries.vuex.VuexUtils.VUEX_NAMESPACE
import org.jetbrains.vuejs.libraries.vuex.types.VuexStoreTypeProvider

class VuexFrameworkHandler : FrameworkIndexingHandler() {

  private val VUEX_INDEXES = mapOf(
    createIndexRecord(VUEX_STORE_INDEX_KEY)
  )

  override fun shouldCreateStubForCallExpression(node: ASTNode): Boolean {
    if (node.elementType === JSElementTypes.CALL_EXPRESSION) {
      val reference = node.let { JSCallExpressionImpl.getMethodExpression(it) }
                        ?.takeIf { it.elementType === JSElementTypes.REFERENCE_EXPRESSION }
                      ?: return false
      val refName = JSReferenceExpressionImpl.getReferenceName(reference) ?: return false
      if (JSReferenceExpressionImpl.getQualifierNode(reference) == null) {
        return VUEX_MAPPERS.contains(refName)
      }
      else {
        return REGISTER_MODULE == refName || CREATE_STORE == refName
      }
    }
    else {
      // new Vuex.Store call
      return node
        .takeIf {
          it.elementType === JSElementTypes.NEW_EXPRESSION
          || it.elementType === JSElementTypes.TYPESCRIPT_NEW_EXPRESSION
        }
        ?.let { JSCallExpressionImpl.getMethodExpression(it) }
        ?.takeIf { it.elementType === JSElementTypes.REFERENCE_EXPRESSION }
        ?.let { reference ->
          JSReferenceExpressionImpl.getQualifierNode(reference)
            ?.let { JSReferenceExpressionImpl.getReferenceName(it) } == VUEX_NAMESPACE
          && JSReferenceExpressionImpl.getReferenceName(reference) == STORE
        } == true
    }
  }

  override fun shouldCreateStubForArrayLiteral(node: ASTNode): Boolean {
    val grandParent = node.treeParent?.treeParent
    return grandParent != null && shouldCreateStubForCallExpression(grandParent)
  }

  override fun shouldCreateStubForLiteral(node: ASTNode): Boolean {
    val firstChildNode = node.firstChildNode ?: return false
    if (firstChildNode.elementType === JSTokenTypes.TRUE_KEYWORD) {
      return node.treeParent?.psi
        ?.asSafely<JSProperty>()
        ?.name
        ?.let { it == PROP_NAMESPACED || it == PROP_ROOT } == true
    }
    if (isQuotedLiteral(node) || isStringTemplateExpressionWithoutArguments(node)) {
      val parent = node.treeParent
      when (parent?.elementType) {
        JSElementTypes.ARGUMENT_LIST -> {
          return parent.treeParent?.let { shouldCreateStubForCallExpression(it) } ?: false
        }
        JSElementTypes.ARRAY_LITERAL_EXPRESSION -> {
          return shouldCreateStubForArrayLiteral(parent)
        }
      }
    }
    return false
  }

  override fun hasSignificantValue(expression: JSLiteralExpression): Boolean {
    return shouldCreateStubForLiteral(expression.node)
  }

  override fun processCallExpression(callExpression: JSCallExpression, outData: JSElementIndexingData) {
    val reference = callExpression.methodExpression
      ?.asSafely<JSReferenceExpression>()
    val referenceName = reference?.referenceName ?: return
    if ((callExpression is JSNewExpression
         && JSSymbolUtil.isAccurateReferenceExpressionName(reference, VUEX_NAMESPACE, STORE))
        || referenceName == CREATE_STORE) {
      outData.addImplicitElement(
        JSImplicitElementImpl.Builder(STORE, callExpression)
          .setUserString(this, VUEX_STORE_INDEX_JS_KEY)
          .setType(JSImplicitElement.Type.Variable)
          .forbidAstAccess()
          .toImplicitElement())
    }
    else if (referenceName == REGISTER_MODULE) {
      val type = callExpression.arguments.getOrNull(1).asSafely<JSReferenceExpression>()
        ?.referenceName
      outData.addImplicitElement(
        JSImplicitElementImpl.Builder(REGISTER_MODULE, callExpression)
          .setUserString(this, VUEX_STORE_INDEX_JS_KEY)
          .setJSType(type?.let {
            JSNamedTypeFactory.createType(it, JSTypeSource.EMPTY, JSContext.STATIC)
          })
          .forbidAstAccess()
          .toImplicitElement())
    }
  }

  override fun indexImplicitElement(element: JSImplicitElementStructure, sink: IndexSink?): Boolean {
    val index = VUEX_INDEXES[element.userString]
    if (index != null) {
      sink?.occurrence(index, element.name)
    }
    return false
  }

  override fun addTypeFromResolveResult(evaluator: JSTypeEvaluator, context: JSEvaluateContext, result: PsiElement): Boolean =
    VuexStoreTypeProvider.addTypeFromResolveResult(evaluator, result)

  override fun computeJSImplicitElementUserStringKeys(): Set<String> {
    return setOf(VUEX_STORE_INDEX_JS_KEY)
  }
}
