// This is a generated file. Not intended for manual editing.
package com.jetbrains.lang.makefile.psi.impl;

import java.util.List;
import org.jetbrains.annotations.*;
import com.intellij.lang.ASTNode;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiElementVisitor;
import com.intellij.psi.util.PsiTreeUtil;
import static com.jetbrains.lang.makefile.psi.MakefileTypes.*;
import com.jetbrains.lang.makefile.psi.*;

public class MakefilePrerequisiteImpl extends MakefilePrerequisiteMixin implements MakefilePrerequisite {

  public MakefilePrerequisiteImpl(@NotNull ASTNode node) {
    super(node);
  }

  public void accept(@NotNull MakefileVisitor visitor) {
    visitor.visitPrerequisite(this);
  }

  @Override
  public void accept(@NotNull PsiElementVisitor visitor) {
    if (visitor instanceof MakefileVisitor) accept((MakefileVisitor)visitor);
    else super.accept(visitor);
  }

  @Override
  @NotNull
  public List<MakefileFunction> getFunctionList() {
    return PsiTreeUtil.getChildrenOfTypeAsList(this, MakefileFunction.class);
  }

  @Override
  @NotNull
  public List<MakefileVariableUsage> getVariableUsageList() {
    return PsiTreeUtil.getChildrenOfTypeAsList(this, MakefileVariableUsage.class);
  }

  @Override
  public @NotNull MakefilePrerequisiteImpl updateText(@NotNull String newText) {
    return MakefilePsiImplUtil.updateText(this, newText);
  }

  @Override
  public boolean isPhonyTarget() {
    return MakefilePsiImplUtil.isPhonyTarget(this);
  }

}
