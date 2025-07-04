// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package org.jetbrains.vuejs.model.source

import com.intellij.lang.javascript.psi.JSCallExpression
import com.intellij.lang.javascript.psi.JSObjectLiteralExpression
import com.intellij.model.Pointer
import com.intellij.openapi.project.DumbService
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFileManager
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiManager
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.psi.search.GlobalSearchScopesCore
import com.intellij.psi.util.CachedValueProvider.Result
import com.intellij.psi.util.CachedValuesManager
import com.intellij.psi.util.PsiModificationTracker
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.util.containers.MultiMap
import com.intellij.util.indexing.FileBasedIndex
import org.jetbrains.vuejs.codeInsight.fromAsset
import org.jetbrains.vuejs.codeInsight.toAsset
import org.jetbrains.vuejs.index.*
import org.jetbrains.vuejs.model.*
import java.util.*

class VueSourceGlobal(override val project: Project, override val packageJsonUrl: String?) : VueGlobal {

  override val global: VueGlobal = this
  override val libraries: List<VueLibrary>
    get() = emptyList()
  override val source: PsiElement? = null
  override val parents: List<VueEntitiesContainer> = emptyList()

  override val directives: Map<String, VueDirective>
    get() = getCachedValue { buildDirectives(it) }
  override val filters: Map<String, VueFilter>
    get() = getCachedValue { buildFiltersMap(it) }
  override val apps: List<VueApp>
    get() = getCachedValue { buildAppsList(it) }
  override val mixins: List<VueMixin>
    get() = getCachedValue { buildMixinsList(it) }

  override val components: Map<String, VueComponent>
    get() = getComponents(true)

  override val unregistered: VueEntitiesContainer = object : VueEntitiesContainer {
    override val components: Map<String, VueComponent>
      get() = getComponents(false)
    override val directives: Map<String, VueDirective> = emptyMap()
    override val filters: Map<String, VueFilter> = emptyMap()
    override val mixins: List<VueMixin> get() = emptyList()
    override val source: PsiElement? = null
    override val parents: List<VueEntitiesContainer> = emptyList()
    override fun createPointer(): Pointer<out VueEntitiesContainer> = Pointer.hardPointer(this)
  }

  override fun equals(other: Any?): Boolean =
    (other as? VueSourceGlobal)?.let {
      it.project == project && it.packageJsonUrl == packageJsonUrl
    } ?: false

  override fun hashCode(): Int = (project.hashCode()) * 31 + packageJsonUrl.hashCode()

  override fun createPointer(): Pointer<out VueGlobal> = Pointer.hardPointer(this)

  override fun getParents(scopeElement: VueScopeElement): List<VueEntitiesContainer> =
    emptyList()

  private fun getComponents(global: Boolean): Map<String, VueComponent> =
    getCachedValue { scope ->

      val localComponents: MultiMap<String, VueComponent> = MultiMap.createLinked()
      val globalComponents: MultiMap<String, VueComponent> = MultiMap.createLinked()

      // Add Vue files without regular initializer as possible imports
      val psiManager = PsiManager.getInstance(project)
      FileBasedIndex.getInstance().getFilesWithKey(
        VUE_NO_INITIALIZER_COMPONENTS_INDEX, setOf(true),
        { file ->
          psiManager.findFile(file)
            ?.let { psiFile ->
              VueModelManager.getComponent(VueSourceEntityDescriptor(source = psiFile))
            }
            ?.let { localComponents.putValue(fromAsset(file.nameWithoutExtension), it) }
          true
        }, scope)

      // Add components from global and local indices
      val componentsData = VueComponentsCalculation.calculateScopeComponents(scope, false)
      val moduleComponents = componentsData.map

      moduleComponents.entries
        .asSequence()
        .filter { !it.value.second }
        .mapNotNull { (name, data) -> VueModelManager.getComponent(data.first)?.let { Pair(name, it) } }
        .fold(localComponents) { map, (name, component) -> map.also { it.putValue(name, component) } }

      moduleComponents.entries
        .asSequence()
        .filter { it.value.second }
        .mapNotNull { (name, data) -> VueModelManager.getComponent(data.first)?.let { Pair(name, it) } }
        .fold(globalComponents) { map, (name, component) -> map.also { it.putValue(name, component) } }

      componentsData.libCompResolveMap.forEach { (alias, target) ->
        localComponents[target].firstOrNull()
          ?.let { if (localComponents[alias].isEmpty()) localComponents.putValue(alias, it) }
        globalComponents[target].firstOrNull()
          ?.let { if (globalComponents[alias].isEmpty()) globalComponents.putValue(alias, it) }
      }

      // Contribute components from providers.
      val sourceComponents = VueContainerInfoProvider.ComponentsInfo(localComponents.copy(), globalComponents.copy())
      VueContainerInfoProvider.getProviders()
        .mapNotNull { it.getAdditionalComponents(scope, sourceComponents) }
        .forEach {
          globalComponents.putAllValues(it.global)
          localComponents.putAllValues(it.local)
        }

      Pair(
        localComponents.entrySet().asSequence()
          .mapNotNull { (name, components) -> components.lastOrNull()?.let { Pair(name, it) } }
          .sortedBy { it.first }.toMap(),
        globalComponents.entrySet().asSequence()
          .mapNotNull { (name, components) -> components.lastOrNull()?.let { Pair(name, it) } }
          .sortedBy { it.first }.toMap())
    }.let {
      if (global) it.second else it.first
    }

  private fun <T> getCachedValue(provider: (GlobalSearchScope) -> T): T {
    val psiFile: PsiFile? = VueGlobalImpl.findFileByUrl(packageJsonUrl)
      ?.let { PsiManager.getInstance(project).findFile(it) }
    val searchScope = psiFile?.parent
                        ?.let {
                          GlobalSearchScopesCore.directoryScope(it, true)
                            .intersectWith(GlobalSearchScope.projectScope(project))
                        }
                      ?: GlobalSearchScope.projectScope(project)
    val manager = CachedValuesManager.getManager(project)
    return CachedValuesManager.getManager(project).getCachedValue(
      psiFile ?: project,
      manager.getKeyForClass(provider::class.java),
      {
        Result.create(
          provider(searchScope),
          VirtualFileManager.VFS_STRUCTURE_MODIFICATIONS,
          PsiModificationTracker.MODIFICATION_COUNT,
          DumbService.getInstance(project),
        )
      },
      false)
  }

  companion object {
    private fun buildDirectives(searchScope: GlobalSearchScope): Map<String, VueDirective> =
      getForAllKeys(searchScope, VUE_GLOBAL_DIRECTIVES_INDEX_KEY)
        .map { Pair(it.name, VueSourceDirective(it.name, it.parent)) }
        // TODO properly support multiple directives with the same name
        .distinctBy { it.first }
        .toMap(TreeMap())

    private fun buildMixinsList(scope: GlobalSearchScope): List<VueMixin> =
      resolve(GLOBAL, scope, VUE_MIXIN_BINDING_INDEX_KEY)
        .asSequence()
        .mapNotNull { VueComponents.vueMixinDescriptorFinder(it) }
        .mapNotNull { VueModelManager.getMixin(it) }
        .toList()

    private fun buildAppsList(scope: GlobalSearchScope): List<VueApp> =
      getForAllKeys(scope, VUE_OPTIONS_INDEX_KEY)
        .filter(VueComponents.Companion::isNotInLibrary)
        .mapNotNull { it as? JSObjectLiteralExpression ?: PsiTreeUtil.getContextOfType(it, JSObjectLiteralExpression::class.java) }
        .map { VueModelManager.getApp(it) }
        .filter { it.element != null }
        .plus(
          resolve(CREATE_APP_FUN, scope, VUE_COMPOSITION_APP_INDEX_KEY)
            .asSequence()
            .filter(VueComponents.Companion::isNotInLibrary)
            .mapNotNull { (it.context as? JSCallExpression)?.let { call -> VueCompositionApp(call) } }
        )
        .toList()

    private fun buildFiltersMap(scope: GlobalSearchScope): Map<String, VueFilter> =
      getForAllKeys(scope, VUE_GLOBAL_FILTERS_INDEX_KEY)
        .mapNotNull { element ->
          VueModelManager.getFilter(element)?.let {
            Pair(toAsset(element.name), it)
          }
        }
        // TODO properly support multiple filters with the same name
        .distinctBy { it.first }
        .toMap(TreeMap())
  }
}
