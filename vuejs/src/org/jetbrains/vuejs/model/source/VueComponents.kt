// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package org.jetbrains.vuejs.model.source

import com.intellij.diagnostic.PluginException
import com.intellij.extapi.psi.StubBasedPsiElementBase
import com.intellij.lang.ecmascript6.psi.ES6ExportDefaultAssignment
import com.intellij.lang.javascript.JSElementTypes
import com.intellij.lang.javascript.JSElementTypesImpl
import com.intellij.lang.javascript.index.JSSymbolUtil
import com.intellij.lang.javascript.library.JSLibraryUtil
import com.intellij.lang.javascript.psi.*
import com.intellij.lang.javascript.psi.ecma6.ES6Decorator
import com.intellij.lang.javascript.psi.ecmal4.JSClass
import com.intellij.lang.javascript.psi.resolve.ES6QualifiedNameResolver
import com.intellij.lang.javascript.psi.stubs.JSImplicitElement
import com.intellij.lang.javascript.psi.util.JSProjectUtil
import com.intellij.lang.javascript.psi.util.JSStubBasedPsiTreeUtil
import com.intellij.lang.javascript.psi.util.stubSafeCallArguments
import com.intellij.model.Pointer
import com.intellij.openapi.diagnostic.thisLogger
import com.intellij.openapi.util.Key
import com.intellij.psi.PsiElement
import com.intellij.psi.createSmartPointer
import com.intellij.psi.impl.source.html.HtmlFileImpl
import com.intellij.psi.util.CachedValue
import com.intellij.psi.util.CachedValueProvider
import com.intellij.psi.util.CachedValuesManager
import com.intellij.psi.util.PsiUtilCore
import com.intellij.util.applyIf
import com.intellij.util.asSafely
import org.jetbrains.vuejs.codeInsight.resolveElementTo
import org.jetbrains.vuejs.index.getFunctionNameFromVueIndex
import org.jetbrains.vuejs.index.getVueIndexData
import org.jetbrains.vuejs.lang.html.isVueFile
import org.jetbrains.vuejs.libraries.componentDecorator.isComponentDecorator
import org.jetbrains.vuejs.model.typed.VueTypedEntitiesProvider

/**
 * Basic resolve from index here (when we have the name literal and the descriptor literal/reference)
 */
class VueComponents {
  companion object {
    fun onlyLocal(elements: Collection<JSImplicitElement>): List<JSImplicitElement> {
      return elements.filter(this::isNotInLibrary)
    }

    fun meaningfulExpression(element: PsiElement?): PsiElement? {
      if (element == null) return null
      return JSStubBasedPsiTreeUtil.calculateMeaningfulElements(element)
        .firstOrNull { it !is JSEmbeddedContent }
    }

    fun isNotInLibrary(element: JSImplicitElement): Boolean {
      val file = element.containingFile.viewProvider.virtualFile
      return !JSProjectUtil.isInLibrary(file, element.project) && !JSLibraryUtil.isProbableLibraryFile(file)
    }

    fun vueMixinDescriptorFinder(implicitElement: JSImplicitElement): VueSourceEntityDescriptor? {
      getVueIndexData(implicitElement)?.descriptorQualifiedReference
        ?.takeIf { it.isNotBlank() }
        ?.let { resolveReferenceToVueComponent(implicitElement, it) }
        ?.asSafely<VueSourceEntityDescriptor>()
        ?.let { return it }

      val mixinObj = (implicitElement.parent as? JSProperty)?.parent as? JSObjectLiteralExpression
      if (mixinObj != null) return VueSourceEntityDescriptor.tryCreate(mixinObj)

      val call = implicitElement.parent as? JSCallExpression
      if (call != null) {
        return JSStubBasedPsiTreeUtil.findDescendants(call, JSElementTypesImpl.OBJECT_LITERAL_EXPRESSION)
          .firstOrNull { (it.context as? JSArgumentList)?.context == call || (it.context == call) }
          ?.let { VueSourceEntityDescriptor.tryCreate(it) }
      }
      return null
    }

    fun resolveReferenceToVueComponent(element: PsiElement, reference: String): VueEntityDescriptor? {
      val context = (element as? JSImplicitElement)?.parent ?: element

      return JSStubBasedPsiTreeUtil.resolveLocally(reference, context)
               ?.let { getVueComponentFromResolve(listOf(it)) }
               ?.let { return it }
             ?: getVueComponentFromResolve(ES6QualifiedNameResolver(context, true).resolveQualifiedName(reference))
    }

    private fun getVueComponentFromResolve(result: Collection<PsiElement>): VueEntityDescriptor? {
      return result.firstNotNullOfOrNull {
        getComponentDescriptor(it.applyIf(it is JSImplicitElement) { it.context ?: return@firstNotNullOfOrNull null })
      }
    }

    fun getClassComponentDescriptor(clazz: JSClass): VueSourceEntityDescriptor =
      VueSourceEntityDescriptor(
        initializer = getComponentDecorator(clazz)?.let { getDescriptorFromDecorator(it) },
        clazz = clazz)

    fun getComponentDecorator(element: JSClass): ES6Decorator? {
      element.attributeList
        ?.decorators
        ?.find(::isComponentDecorator)
        ?.let { return it }
      return (element.context as? ES6ExportDefaultAssignment)
        ?.attributeList
        ?.decorators
        ?.find(::isComponentDecorator)
    }

    fun getComponentDescriptor(element: PsiElement?): VueEntityDescriptor? =
      VueTypedEntitiesProvider.getComponentDescriptor(element)
      ?: getSourceComponentDescriptor(element)

    fun getSourceComponentDescriptor(element: PsiElement?): VueSourceEntityDescriptor? =
      when (val resolved = resolveElementTo(element, JSObjectLiteralExpression::class, JSCallExpression::class,
                                            JSClass::class, JSEmbeddedContent::class, HtmlFileImpl::class)) {
        // {...}
        is JSObjectLiteralExpression -> VueSourceEntityDescriptor.tryCreate(resolved)

        // Vue.extend({...})
        // defineComponent({...})
        is JSCallExpression ->
          if (isComponentDefiningCall(resolved)) {
            resolved.stubSafeCallArguments
              .getOrNull(0)
              ?.let { it as? JSObjectLiteralExpression }
              ?.let { VueSourceEntityDescriptor.tryCreate(it) }
          }
          else null

        // @Component({...}) class MyComponent {...}
        is JSClass ->
          VueSourceEntityDescriptor.tryCreate(getComponentDecorator(resolved)?.let { getDescriptorFromDecorator(it) },
                                              resolved)

        // <script setup>
        is JSEmbeddedContent ->
          VueSourceEntityDescriptor.tryCreate(source = resolved.containingFile)

        // Vue file without script section
        is HtmlFileImpl ->
          if (resolved.virtualFile.isVueFile)
            VueSourceEntityDescriptor.tryCreate(source = resolved)
          else null

        else -> null
      }

    @StubSafe
    fun getDescriptorFromDecorator(decorator: ES6Decorator): JSObjectLiteralExpression? {
      if (!isComponentDecorator(decorator)) return null

      if (decorator is StubBasedPsiElementBase<*>) {
        decorator.stub?.let {
          return (it.findChildStubByElementType(JSElementTypes.CALL_EXPRESSION) ?: it)
            .findChildStubByElementType(JSElementTypes.OBJECT_LITERAL_EXPRESSION)
            ?.psi as? JSObjectLiteralExpression
        }
      }
      val callExpression = decorator.expression as? JSCallExpression ?: return null
      val arguments = callExpression.arguments
      if (arguments.size == 1) {
        return arguments[0] as? JSObjectLiteralExpression
      }
      return null
    }

    @StubSafe
    fun isComponentDefiningCall(callExpression: JSCallExpression): Boolean =
      getFunctionNameFromVueIndex(callExpression).let {
        it == DEFINE_COMPONENT_FUN || it == DEFINE_NUXT_COMPONENT_FUN || it == EXTEND_FUN || it == DEFINE_OPTIONS_FUN
      }

    @StubSafe
    fun isDefineOptionsCall(callExpression: JSCallExpression): Boolean =
      getFunctionNameFromVueIndex(callExpression) == DEFINE_OPTIONS_FUN

    fun isStrictComponentDefiningCall(callExpression: JSCallExpression): Boolean =
      callExpression.methodExpression?.let {
        JSSymbolUtil.isAccurateReferenceExpressionName(it, DEFINE_COMPONENT_FUN) ||
        JSSymbolUtil.isAccurateReferenceExpressionName(it, DEFINE_NUXT_COMPONENT_FUN) ||
        JSSymbolUtil.isAccurateReferenceExpressionName(it, VUE_NAMESPACE, EXTEND_FUN) ||
        JSSymbolUtil.isAccurateReferenceExpressionName(it, DEFINE_OPTIONS_FUN)
      } ?: false
  }
}

interface VueEntityDescriptor {
  val source: PsiElement
}

class VueSourceEntityDescriptor(
  val initializer: JSElement? /* JSObjectLiteralExpression | PsiFile */ = null,
  val clazz: JSClass? = null,
  override val source: PsiElement = clazz ?: initializer!!,
) : VueEntityDescriptor {
  init {
    assert(initializer == null || initializer is JSObjectLiteralExpression || initializer is JSFile)
    source.let { PsiUtilCore.ensureValid(it) }
    initializer?.let { PsiUtilCore.ensureValid(it) }
    clazz?.let { PsiUtilCore.ensureValid(it) }
  }

  fun <T> getCachedValue(provider: (descriptor: VueSourceEntityDescriptor) -> CachedValueProvider.Result<T>): T {
    val providerKey: Key<CachedValue<T>> = CachedValuesManager.getManager(source.project).getKeyForClass(provider::class.java)
    return when {
      clazz != null -> {
        val theClass = clazz
        CachedValuesManager.getCachedValue(theClass, providerKey) {
          val descriptor = VueComponents.getClassComponentDescriptor(theClass)
          provider(descriptor)
        }
      }
      initializer != null -> {
        val theInitializer = initializer
        CachedValuesManager.getCachedValue(theInitializer, providerKey) {
          provider(VueSourceEntityDescriptor(theInitializer))
        }
      }
      else -> {
        val theSource = source
        CachedValuesManager.getCachedValue(theSource, providerKey) {
          provider(VueSourceEntityDescriptor(source = theSource))
        }
      }
    }
  }

  override fun equals(other: Any?): Boolean =
    other === this ||
    (other is VueSourceEntityDescriptor
     && other.source == source)

  override fun hashCode(): Int =
    source.hashCode()

  fun createPointer(): Pointer<VueSourceEntityDescriptor> {
    val initializerPtr = this.initializer?.createSmartPointer()
    val clazzPtr = this.clazz?.createSmartPointer()
    val sourcePtr = this.source.createSmartPointer()
    return Pointer {
      val initializer = initializerPtr?.let { it.dereference() ?: return@Pointer null }
      val clazz = clazzPtr?.let { it.dereference() ?: return@Pointer null }
      val source = sourcePtr.dereference() ?: return@Pointer null
      VueSourceEntityDescriptor(initializer, clazz, source)
    }
  }

  companion object {
    fun tryCreate(
      initializer: JSElement? /* JSObjectLiteralExpression | PsiFile */ = null,
      clazz: JSClass? = null,
      source: PsiElement = clazz ?: initializer!!,
    ): VueSourceEntityDescriptor? =
      try {
        VueSourceEntityDescriptor(initializer, clazz, source)
      }
      catch (e: PluginException) {
        thisLogger().error(e)
        null
      }
  }
}
