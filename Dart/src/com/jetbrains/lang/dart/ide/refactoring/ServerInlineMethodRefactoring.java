// Copyright 2000-2024 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package com.jetbrains.lang.dart.ide.refactoring;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.jetbrains.lang.dart.DartBundle;
import org.dartlang.analysis.server.protocol.*;
import org.jetbrains.annotations.NotNull;

/**
 * LTK wrapper around Analysis Server 'Inline Method' refactoring.
 */
public class ServerInlineMethodRefactoring extends ServerRefactoring {
  private final InlineMethodOptions options = new InlineMethodOptions(false, false);
  private String fullName;
  private boolean isDeclaration;

  public ServerInlineMethodRefactoring(final @NotNull Project project,
                                       final @NotNull VirtualFile file,
                                       final int offset,
                                       final int length) {
    super(project, DartBundle.message("progress.title.inline.method"), RefactoringKind.INLINE_METHOD, file, offset, length);
  }

  public String getFullName() {
    return fullName;
  }

  public boolean isDeclaration() {
    return isDeclaration;
  }

  public void setDeleteSource(boolean value) {
    options.setDeleteSource(value);
    setOptions(true, null);
  }

  public void setInlineAll(boolean value) {
    options.setInlineAll(value);
    setOptions(true, null);
  }

  @Override
  protected RefactoringOptions getOptions() {
    return options;
  }

  @Override
  protected void setFeedback(@NotNull RefactoringFeedback _feedback) {
    InlineMethodFeedback feedback = (InlineMethodFeedback)_feedback;
    String className = feedback.getClassName();
    String methodName = feedback.getMethodName();
    if (className != null) {
      fullName = className + "." + methodName;
    }
    else {
      fullName = methodName;
    }
    isDeclaration = feedback.isDeclaration();
  }
}
