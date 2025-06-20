// Copyright 2000-2024 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package com.intellij.javascript.flex.mxml;

import com.intellij.lang.javascript.flex.FlexSupportLoader;
import com.intellij.lang.javascript.flex.XmlBackedJSClassImpl;
import com.intellij.lang.javascript.psi.JSFile;
import com.intellij.lang.javascript.psi.JSFunction;
import com.intellij.lang.javascript.psi.JSNamedElement;
import com.intellij.lang.javascript.psi.JSVariable;
import com.intellij.lang.javascript.psi.ecmal4.XmlBackedJSClass;
import com.intellij.lang.javascript.psi.ecmal4.XmlBackedJSClassFactory;
import com.intellij.lang.javascript.psi.resolve.JSResolveUtil;
import com.intellij.lang.javascript.psi.resolve.ResolveProcessor;
import com.intellij.lang.javascript.stubs.JSStubVersionUtil;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiRecursiveElementVisitor;
import com.intellij.psi.ResolveState;
import com.intellij.psi.xml.XmlFile;
import com.intellij.psi.xml.XmlTag;
import com.intellij.util.Consumer;
import com.intellij.util.indexing.*;
import com.intellij.util.io.EnumeratorStringDescriptor;
import com.intellij.util.io.KeyDescriptor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;

public final class FlexXmlBackedMembersIndex extends ScalarIndexExtension<String> {
  private static final int INDEX_VERSION = 1;

  public static final ID<String, Void> NAME = ID.create("FlexXmlBackedMembersIndex");

  @Override
  public @NotNull ID<String, Void> getName() {
    return NAME;
  }

  @Override
  public @NotNull DataIndexer<String, Void, FileContent> getIndexer() {
    return new DataIndexer<>() {

      @Override
      public @NotNull Map<String, Void> map(@NotNull FileContent inputData) {
        final XmlFile file = (XmlFile)inputData.getPsiFile();

        final Map<String, Void> result = new HashMap<>();
        process(file, element -> {
          String name = getName(element);
          if (name != null) {
            result.put(name, null);
          }
        }, false);
        return result;
      }
    };
  }

  static void process(XmlFile file, Consumer<PsiElement> consumer, boolean isPhysical) {
    visitScriptTagInjectedFilesForIndexing(file, new JSResolveUtil.JSInjectedFilesVisitor() {
      @Override
      protected void process(JSFile file) {
        ResolveState state = ResolveState.initial().put(XmlBackedJSClass.PROCESS_XML_BACKED_CLASS_MEMBERS_HINT, Boolean.TRUE);
        file.processDeclarations(new ResolveProcessor(null) {
          {
            setToProcessActionScriptImplicits(false);
          }

          @Override
          public boolean execute(@NotNull PsiElement element, @NotNull ResolveState state) {
            if (element instanceof JSFunction || element instanceof JSVariable) {
              consumer.consume(element);
            }
            return true;
          }
        }, state, null, file);
      }
    }, isPhysical);

    file.accept(new PsiRecursiveElementVisitor() {
      @Override
      public void visitElement(@NotNull PsiElement element) {
        if (element instanceof XmlTag tag) {
          if (tag.getAttributeValue("id") != null && MxmlJSClass.canBeReferencedById(tag)) {
            consumer.consume(tag);
          }
        }
        super.visitElement(element);
      }
    });
  }

  @Override
  public @NotNull KeyDescriptor<String> getKeyDescriptor() {
    return EnumeratorStringDescriptor.INSTANCE;
  }

  @Override
  public @NotNull FileBasedIndex.InputFilter getInputFilter() {
    return new DefaultFileTypeSpecificInputFilter(FlexSupportLoader.getMxmlFileType()) {
      @Override
      public boolean acceptInput(@NotNull VirtualFile file) {
        return FlexSupportLoader.isMxmlOrFxgFile(file);
      }
    };
  }

  @Override
  public boolean dependsOnFileContent() {
    return true;
  }

  @Override
  public int getVersion() {
    return JSStubVersionUtil.getVersion(INDEX_VERSION);
  }


  static @Nullable String getName(PsiElement element) {
    if (element instanceof XmlTag) {
      return ((XmlTag)element).getAttributeValue("id");
    }
    else {
      return ((JSNamedElement)element).getName();
    }
  }

  // We use light version of visitInjectedFiles that process injections only for Script tags,
  // all other tags / attributes need cross file resolve
  public static void visitScriptTagInjectedFilesForIndexing(XmlFile file,
                                                            XmlBackedJSClassImpl.InjectedFileVisitor visitor,
                                                            boolean physical) {
    new XmlBackedJSClassImpl.InjectedScriptsVisitor(
      XmlBackedJSClassFactory.getRootTag(file), MxmlJSClassProvider.getInstance(),
      false, true, visitor, physical).go();
  }
}
