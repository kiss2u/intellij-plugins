// Copyright 2000-2024 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package com.intellij.lang.javascript.flex.library;

import com.intellij.openapi.roots.libraries.LibraryProperties;
import com.intellij.util.xmlb.annotations.Attribute;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;

public class FlexLibraryProperties extends LibraryProperties<FlexLibraryProperties> {
  private @Nullable String myId;

  public FlexLibraryProperties() {
    this(null);
  }

  public FlexLibraryProperties(@Nullable String id) {
    myId = id;
  }

  @Override
  public FlexLibraryProperties getState() {
    return this;
  }

  @Override
  public void loadState(@NotNull FlexLibraryProperties state) {
    myId = state.myId;
  }

  @Override
  public boolean equals(Object obj) {
    return obj instanceof FlexLibraryProperties && Objects.equals(((FlexLibraryProperties)obj).myId, myId);
  }

  @Override
  public int hashCode() {
    return myId != null ? myId.hashCode() : 0;
  }

  @Attribute("id")
  public @Nullable String getId() {
    return myId;
  }

  public void setId(@Nullable String id) {
    myId = id;
  }
}
