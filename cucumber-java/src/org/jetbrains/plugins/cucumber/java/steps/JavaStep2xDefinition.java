// Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package org.jetbrains.plugins.cucumber.java.steps;

import com.intellij.openapi.module.Module;
import com.intellij.psi.PsiElement;
import org.jetbrains.annotations.NotNull;

/// Step definition in Cucumber v2 and later.
public class JavaStep2xDefinition extends JavaAnnotatedStepDefinition {
  public JavaStep2xDefinition(@NotNull PsiElement element, @NotNull Module module, @NotNull String annotationValue) {
    super(element, module, annotationValue);
  }

  @Override
  public String toString() {
    return "JavaStep2xDefinition{stepDef: " + getElement() + "}";
  }
}
