// Copyright 2000-2024 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package com.intellij.javascript.flex.compiled;

import com.intellij.javascript.flex.FlexApplicationComponent;
import com.intellij.lang.javascript.flex.FlexSupportLoader;
import com.intellij.lang.javascript.flex.importer.FlexImporter;
import com.intellij.lang.javascript.psi.stubs.impl.JSFileCachedData;
import com.intellij.lang.javascript.psi.stubs.impl.JSFileStubImpl;
import com.intellij.lang.javascript.stubs.JSStubVersionUtil;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.fileTypes.FileTypeRegistry;
import com.intellij.openapi.vfs.JarFileSystem;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.stubs.BinaryFileStubBuilder;
import com.intellij.psi.stubs.PsiFileStub;
import com.intellij.psi.stubs.PsiFileStubImpl;
import com.intellij.psi.stubs.StubElement;
import com.intellij.util.indexing.FileContent;
import org.jetbrains.annotations.NotNull;

import java.io.ByteArrayInputStream;
import java.util.Collections;

/**
 * @author Maxim.Mossienko
 */
public final class SwfFileStubBuilder implements BinaryFileStubBuilder {
  private static final int VERSION = 3;

  @Override
  public boolean acceptsFile(final @NotNull VirtualFile file) {
    return FileTypeRegistry.getInstance().isFileOfType(file, FlexApplicationComponent.SWF_FILE_TYPE) &&
           file.getPath().endsWith(JarFileSystem.JAR_SEPARATOR + file.getName());
  }

  @Override
  public StubElement<?> buildStubTree(@NotNull FileContent fileContent) {
    return buildFileStub(fileContent.getFile(), fileContent.getContent());
  }

  static PsiFileStub<?> buildFileStub(VirtualFile file, byte[] content) {
    PsiFileStubImpl<?> stub = new JSFileStubImpl(FlexSupportLoader.ECMA_SCRIPT_L4, new JSFileCachedData(), Collections.emptySet());
    try {

      FlexImporter.buildStubsInterfaceFromStream(
        new ByteArrayInputStream(content),
        stub
      );

    } catch (Exception ex) {
      Logger.getInstance(SwfFileStubBuilder.class.getName()).warn(file.getPath(), ex);
    }

    return stub;
  }

  @Override
  public int getStubVersion() {
    return JSStubVersionUtil.getVersion(VERSION);
  }
}