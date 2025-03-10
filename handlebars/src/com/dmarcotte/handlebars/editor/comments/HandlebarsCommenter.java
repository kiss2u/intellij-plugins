// Copyright 2000-2024 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package com.dmarcotte.handlebars.editor.comments;

import com.intellij.lang.Commenter;
import org.jetbrains.annotations.Nullable;

/**
 * Commenter for native Handlebars comments: <pre>{{!-- comment --}}</pre>
 */
class HandlebarsCommenter implements Commenter {
  @Override
  public @Nullable String getLineCommentPrefix() {
    return null;
  }

  @Override
  public @Nullable String getBlockCommentPrefix() {
    return "{{!--";
  }

  @Override
  public @Nullable String getBlockCommentSuffix() {
    return "--}}";
  }

  @Override
  public @Nullable String getCommentedBlockCommentPrefix() {
    return null;
  }

  @Override
  public @Nullable String getCommentedBlockCommentSuffix() {
    return null;
  }
}
