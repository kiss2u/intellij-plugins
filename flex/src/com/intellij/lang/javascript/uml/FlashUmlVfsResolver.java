// Copyright 2000-2024 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package com.intellij.lang.javascript.uml;

import com.intellij.diagram.DiagramVfsResolver;
import com.intellij.javascript.flex.resolve.ActionScriptClassResolver;
import com.intellij.lang.javascript.flex.FlexSupportLoader;
import com.intellij.lang.javascript.psi.JSCommonTypeNames;
import com.intellij.lang.javascript.psi.JSFile;
import com.intellij.lang.javascript.psi.ecmal4.JSClass;
import com.intellij.lang.javascript.psi.ecmal4.JSQualifiedNamedElement;
import com.intellij.lang.javascript.psi.ecmal4.XmlBackedJSClassFactory;
import com.intellij.lang.javascript.psi.impl.JSPsiImplUtils;
import com.intellij.lang.javascript.psi.resolve.JSResolveUtil;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleManager;
import com.intellij.openapi.module.ModuleUtilCore;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiDirectory;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.psi.xml.XmlFile;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public final class FlashUmlVfsResolver implements DiagramVfsResolver<Object> {
  private static final Logger LOG = Logger.getInstance(FlashUmlVfsResolver.class.getName());
  public static final String SEPARATOR = ":";

  @Override
  public String getQualifiedName(@Nullable Object element) {
    return getQualifiedNameStatic(element);
  }

  public static @Nullable String getQualifiedNameStatic(Object element) {
    if (element == null) {
      return null;
    }

    if (element instanceof PsiElement) {
      if (((PsiElement)element).getProject().isDisposed()) {
        return null;
      }
      if (element instanceof JSQualifiedNamedElement qualifiedNamedElement) {
        String qName = qualifiedNamedElement.getQualifiedName();
        if (qName == null) return null;
        return combineWithModuleName(qualifiedNamedElement, fixVectorTypeName(qName));
      }
      else if (element instanceof JSFile) {
        return getQualifiedNameStatic(JSPsiImplUtils.findQualifiedElement((JSFile)element));
      }
      else if (element instanceof XmlFile && FlexSupportLoader.isFlexMxmFile((PsiFile)element)) {
        return getQualifiedNameStatic(XmlBackedJSClassFactory.getXmlBackedClass((XmlFile)element));
      }
      else if (element instanceof PsiDirectory directory) {
        return JSResolveUtil.getExpectedPackageNameFromFile(directory.getVirtualFile(), directory.getProject());
      }
    }
    else if (element instanceof String) {
      return (String)element;
    }
    LOG.error("can't get qualified name of " + element);
    return null;
  }

  private static @Nullable String combineWithModuleName(final @NotNull PsiElement element, final @Nullable String qName) {
    if (qName == null) return null;
    if (ApplicationManager.getApplication().isUnitTestMode()) return qName;
    Module module = ModuleUtilCore.findModuleForPsiElement(element);
    if (module != null) {
      return module.getName() + SEPARATOR + qName;
    }
    return qName;
  }

  @Override
  public Object resolveElementByFQN(@NotNull String fqn, @NotNull Project project) {
    return resolveElementByFqnStatic(fqn, project);
  }

  public static @Nullable Object resolveElementByFqnStatic(String fqn, Project project) {
    final GlobalSearchScope searchScope;
    int separatorIndex = fqn.indexOf(SEPARATOR);
    if (separatorIndex != -1) {
      String moduleName = fqn.substring(0, separatorIndex);
      Module module = ModuleManager.getInstance(project).findModuleByName(moduleName);
      if (module == null) {
        return null;
      }
      fqn = fqn.substring(separatorIndex + 1);
      searchScope = module.getModuleScope(true);
    }
    else {
      searchScope = GlobalSearchScope.allScope(project);
    }
    final PsiElement clazz = ActionScriptClassResolver.findClassByQNameStatic(fqn, searchScope);
    if (clazz instanceof JSClass) return clazz;
    if (FlashUmlElementManager.packageExists(project, fqn, searchScope)) return fqn;
    return null;
  }

  static String fixVectorTypeName(String name) {
    if (isVectorType(name)) {
      return JSCommonTypeNames.VECTOR_CLASS_NAME;
    }
    return name;
  }

  static boolean isVectorType(final String name) {
    return name.startsWith("Vector$");
  }
}
