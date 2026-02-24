package org.jetbrains.plugins.cucumber.java.steps.reference;

import com.intellij.codeInsight.daemon.ImplicitUsageProvider;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiMethod;
import org.jetbrains.annotations.NotNullByDefault;
import org.jetbrains.plugins.cucumber.java.CucumberJavaUtil;

@NotNullByDefault
public final class CucumberJavaImplicitUsageProvider implements ImplicitUsageProvider {
  @Override
  public boolean isImplicitUsage(PsiElement element) {
    if (element instanceof PsiClass psiClass) {
      return CucumberJavaUtil.isStepDefinitionClass(psiClass);
    }
    else if (element instanceof PsiMethod method) {
      return CucumberJavaUtil.isHook(method) || CucumberJavaUtil.isParameterType(method);
    }
    return false;
  }

  @Override
  public boolean isReferencedByAlternativeNames(PsiElement element) {
    return element instanceof PsiMethod method && CucumberJavaUtil.isAnnotationStepDefinition(method);
  }

  @Override
  public boolean isImplicitRead(PsiElement element) {
    return false;
  }

  @Override
  public boolean isImplicitWrite(PsiElement element) {
    return false;
  }
}
