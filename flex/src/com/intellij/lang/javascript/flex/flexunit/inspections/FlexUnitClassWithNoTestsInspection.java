// Copyright 2000-2024 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package com.intellij.lang.javascript.flex.flexunit.inspections;

import com.intellij.codeInspection.ProblemHighlightType;
import com.intellij.codeInspection.ProblemsHolder;
import com.intellij.lang.ASTNode;
import com.intellij.lang.javascript.flex.FlexBundle;
import com.intellij.lang.javascript.flex.flexunit.FlexUnitSupport;
import com.intellij.lang.javascript.psi.JSFunction;
import com.intellij.lang.javascript.psi.ecmal4.JSClass;
import org.jetbrains.annotations.NotNull;

public final class FlexUnitClassWithNoTestsInspection extends FlexUnitClassInspectionBase {

  @Override
  public @NotNull String getShortName() {
    return "FlexUnitClassWithNoTestsInspection";
  }

  @Override
  protected void visitPotentialTestClass(JSClass aClass, @NotNull ProblemsHolder holder, FlexUnitSupport support) {
    if (!support.isTestClass(aClass, false)) return;

    for (JSFunction method : aClass.getFunctions()) {
      if (support.isTestMethod(method)) {
        return;
      }
    }

    final ASTNode nameIdentifier = aClass.findNameIdentifier();
    if (nameIdentifier != null) {
      holder.registerProblem(nameIdentifier.getPsi(), FlexBundle.message("flexunit.inspection.testclasswithnotests.message"),
                             ProblemHighlightType.GENERIC_ERROR_OR_WARNING);
    }
  }
}