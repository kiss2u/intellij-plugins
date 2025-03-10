/*
 * Copyright 2019 Google LLC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.intellij.protobuf.lang.psi;

import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiReference;
import com.intellij.util.IncorrectOperationException;
import org.jetbrains.annotations.Nullable;

/** A class that provides most of the behavior for a {@link ProtoSymbolPath}. */
public abstract class ProtoSymbolPathDelegate {

  /** Implementation of {@link ProtoSymbolPath#getReference()}. */
  public @Nullable PsiReference getReference(ProtoSymbolPath path) {
    return null;
  }

  /** Implementation of {@link ProtoSymbolPath#getNameIdentifier()}. */
  public @Nullable PsiElement getNameIdentifier(ProtoSymbolPath path) {
    return null;
  }

  /** Implementation of {@link ProtoSymbolPath#getName()}. */
  public @Nullable String getName(ProtoSymbolPath path) {
    return null;
  }

  /** Implementation of {@link ProtoSymbolPath#setName(String)}. */
  public @Nullable PsiElement setName(ProtoSymbolPath path, String name) throws IncorrectOperationException {
    throw new IncorrectOperationException();
  }
}
