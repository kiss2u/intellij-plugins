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
package com.intellij.protobuf.lang.psi.impl;

import com.intellij.lang.ASTNode;
import com.intellij.protobuf.lang.psi.PbDefinition;
import com.intellij.protobuf.lang.psi.PbStatement;
import com.intellij.protobuf.lang.psi.util.PbPsiImplUtil;
import com.intellij.protobuf.lang.stub.PbElementStub;
import com.intellij.psi.stubs.IStubElementType;
import org.jetbrains.annotations.NotNull;

import java.util.List;

abstract class PbStubbedDefinitionBase<T extends PbElementStub<?>> extends PbStubbedElementBase<T>
    implements PbDefinition {

  PbStubbedDefinitionBase(ASTNode node) {
    super(node);
  }

  PbStubbedDefinitionBase(T stub, IStubElementType nodeType) {
    super(stub, nodeType);
  }

  @Override
  public @NotNull List<PbStatement> getStatements() {
    return PbPsiImplUtil.getStatements(getBody());
  }
}
