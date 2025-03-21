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
import org.jetbrains.annotations.Nullable;

/** A shared interface implemented by proto-like literal elements. */
public interface ProtoLiteral extends PsiElement {
  /**
   * Return an appropriate object to represent this value. (for example, a String or Integer).
   *
   * @return the literal value
   */
  default @Nullable Object getValue() {
    return getText();
  }

  /**
   * Returns the string representation of the value represented by this literal.
   *
   * @return the string representation, or <code>null</code> if one cannot be determined
   */
  default @Nullable String getAsString() {
    Object value = getValue();
    return value != null ? value.toString() : null;
  }
}
