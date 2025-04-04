// Copyright 2000-2024 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package org.jetbrains.plugins.cucumber.inspections.suppress;

import com.intellij.codeInsight.daemon.impl.actions.AbstractBatchSuppressByNoInspectionCommentFix;
import com.intellij.psi.PsiElement;
import com.intellij.psi.util.PsiTreeUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.plugins.cucumber.CucumberBundle;
import org.jetbrains.plugins.cucumber.psi.GherkinStepsHolder;

public class GherkinSuppressForScenarioCommentFix extends AbstractBatchSuppressByNoInspectionCommentFix {
  GherkinSuppressForScenarioCommentFix(final @NotNull String toolId) {
    super(toolId, false);
  }

  @Override
  public @NotNull String getText() {
    return CucumberBundle.message("cucumber.inspection.suppress.scenario");
  }

  @Override
  public PsiElement getContainer(PsiElement context) {
    // steps holder
    return PsiTreeUtil.getNonStrictParentOfType(context, GherkinStepsHolder.class);
  }
}
