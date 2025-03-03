// Copyright 2000-2024 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package com.jetbrains.lang.dart.util;

import com.intellij.openapi.util.Key;
import com.intellij.openapi.util.Pair;
import com.intellij.openapi.util.TextRange;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.*;
import com.intellij.psi.impl.source.tree.LeafPsiElement;
import com.intellij.psi.util.*;
import com.intellij.util.PathUtil;
import com.intellij.util.containers.ContainerUtil;
import com.jetbrains.lang.dart.DartTokenTypes;
import com.jetbrains.lang.dart.psi.*;
import com.jetbrains.lang.dart.resolve.DartResolveProcessor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;


public final class DartPsiImplUtil {

  private static final String TRIPLE_APOS = "'''";
  private static final String TRIPLE_QUOTE = "\"\"\"";
  private static final String APOS = "'";
  private static final String QUOTE = "\"";
  private static final String R_TRIPLE_APOS = "r'''";
  private static final String R_TRIPLE_QUOTE = "r\"\"\"";
  private static final String R_APOS = "r'";
  private static final String R_QUOTE = "r\"";

  private static final Key<CachedValue<PsiElement>> DART_TYPE_CACHED_RESOLVE_RESULT_KEY = Key.create("DART_TYPE_CACHED_RESOLVE_RESULT_KEY");

  public static @NotNull String getUriString(final @NotNull DartUriBasedDirective uriBasedDirective) {
    return getUnquotedDartStringAndItsRange(uriBasedDirective.getUriElement().getText()).first;
  }

  public static @NotNull Pair<String, TextRange> getUriStringAndItsRange(final @NotNull DartUriElement uriElement) {
    return getUnquotedDartStringAndItsRange(uriElement.getText());
  }

  public static @NotNull Pair<String, TextRange> getUnquotedDartStringAndItsRange(final @NotNull String quotedDartString) {
    // r'''dart:core'''
    // """package:angular/angular.dart"""
    // "../foo/bar.dart"
    // also can be not closed string when completing for example import '<caret>
    final int startOffset;
    final int endOffset;
    if (quotedDartString.startsWith(TRIPLE_APOS)) {
      startOffset = TRIPLE_APOS.length();
      endOffset = quotedDartString.endsWith(TRIPLE_APOS) && quotedDartString.length() >= TRIPLE_APOS.length() * 2
                  ? quotedDartString.length() - TRIPLE_APOS.length() : quotedDartString.length();
    }
    else if (quotedDartString.startsWith(TRIPLE_QUOTE)) {
      startOffset = TRIPLE_QUOTE.length();
      endOffset = quotedDartString.endsWith(TRIPLE_QUOTE) && quotedDartString.length() >= TRIPLE_QUOTE.length() * 2
                  ? quotedDartString.length() - TRIPLE_QUOTE.length() : quotedDartString.length();
    }
    else if (quotedDartString.startsWith(APOS)) {
      startOffset = APOS.length();
      endOffset = quotedDartString.endsWith(APOS) && quotedDartString.length() >= APOS.length() * 2
                  ? quotedDartString.length() - APOS.length() : quotedDartString.length();
    }
    else if (quotedDartString.startsWith(QUOTE)) {
      startOffset = QUOTE.length();
      endOffset = quotedDartString.endsWith(QUOTE) && quotedDartString.length() >= QUOTE.length() * 2
                  ? quotedDartString.length() - QUOTE.length() : quotedDartString.length();
    }
    else if (quotedDartString.startsWith(R_TRIPLE_APOS)) {
      startOffset = R_TRIPLE_APOS.length();
      endOffset = quotedDartString.endsWith(TRIPLE_APOS) && quotedDartString.length() >= R_TRIPLE_APOS.length() + TRIPLE_APOS.length()
                  ? quotedDartString.length() - TRIPLE_APOS.length() : quotedDartString.length();
    }
    else if (quotedDartString.startsWith(R_TRIPLE_QUOTE)) {
      startOffset = R_TRIPLE_QUOTE.length();
      endOffset = quotedDartString.endsWith(TRIPLE_QUOTE) && quotedDartString.length() >= R_TRIPLE_QUOTE.length() + TRIPLE_QUOTE.length()
                  ? quotedDartString.length() - TRIPLE_QUOTE.length() : quotedDartString.length();
    }
    else if (quotedDartString.startsWith(R_APOS)) {
      startOffset = R_APOS.length();
      endOffset = quotedDartString.endsWith(APOS) && quotedDartString.length() >= R_APOS.length() + APOS.length()
                  ? quotedDartString.length() - APOS.length() : quotedDartString.length();
    }
    else if (quotedDartString.startsWith(R_QUOTE)) {
      startOffset = R_QUOTE.length();
      endOffset = quotedDartString.endsWith(QUOTE) && quotedDartString.length() >= R_QUOTE.length() + QUOTE.length()
                  ? quotedDartString.length() - QUOTE.length() : quotedDartString.length();
    }
    else {
      startOffset = 0;
      endOffset = quotedDartString.length();
    }

    return Pair.create(quotedDartString.substring(startOffset, endOffset), TextRange.create(startOffset, endOffset));
  }

  public static @NotNull List<VirtualFile> getLibraryFiles(final @NotNull DartPartOfStatement partOfStatement) {
    final DartLibraryId libraryId = partOfStatement.getLibraryId();
    if (libraryId != null) {
      String libraryName = libraryId.getText();
      return DartResolveUtil.findLibraryByName(partOfStatement, libraryName);
    }

    final DartUriElement uriElement = partOfStatement.getUriElement();
    assert uriElement != null : "[" + partOfStatement.toString() + "]";

    final String uri = uriElement.getUriStringAndItsRange().first;
    final VirtualFile file = DartResolveUtil.getRealVirtualFile(partOfStatement.getContainingFile());
    final VirtualFile targetFile = file == null ? null : DartResolveUtil.getImportedFile(partOfStatement.getProject(), file, uri);
    return ContainerUtil.createMaybeSingletonList(targetFile);
  }

  public static @NotNull String getLibraryName(final @NotNull DartPartOfStatement partOfStatement) {
    final DartLibraryId libraryId = partOfStatement.getLibraryId();
    if (libraryId != null) {
      return libraryId.getText();
    }

    final DartUriElement uriElement = partOfStatement.getUriElement();
    assert uriElement != null : "[" + partOfStatement.toString() + "]";

    final String uri = uriElement.getUriStringAndItsRange().first;
    final VirtualFile file = DartResolveUtil.getRealVirtualFile(partOfStatement.getContainingFile());
    final VirtualFile targetFile = file == null ? null : DartResolveUtil.getImportedFile(partOfStatement.getProject(), file, uri);
    final PsiFile targetPsiFile = targetFile == null || file.equals(targetFile) ? null : partOfStatement.getManager().findFile(targetFile);
    final DartLibraryStatement libraryStatement = targetPsiFile == null
                                                  ? null
                                                  : PsiTreeUtil.getChildOfType(targetPsiFile, DartLibraryStatement.class);
    DartLibraryNameElement nameElement = libraryStatement != null ? libraryStatement.getLibraryNameElement() : null;
    if (nameElement != null) {
      return nameElement.getName();
    }

    return PathUtil.getFileName(uri);
  }

  public static @NotNull List<DartMetadata> getMetadataList(@NotNull DartLabel element) {
    return Collections.emptyList();
  }

  public static @Nullable DartComponentName getComponentName(@NotNull DartEnumConstantDeclaration element) {
    return ContainerUtil.getFirstItem(element.getComponentNameList());
  }

  public static @NotNull List<DartMetadata> getMetadataList(@NotNull DartVarDeclarationListPart element) {
    return Collections.emptyList();
  }

  public static @Nullable DartComponentName getComponentName(@NotNull DartNamedConstructorDeclaration element) {
    final List<DartComponentName> list = element.getComponentNameList();
    return list.size() == 2 ? list.get(1) : null; // todo somehow remove this bogus code, it is here just to mimic old behavior
  }

  public static @Nullable DartComponentName getComponentName(@NotNull DartFactoryConstructorDeclaration element) {
    final List<DartComponentName> list = element.getComponentNameList();
    // todo somehow remove this bogus code, it is here just to mimic old behavior
    return list.size() == 2 ? list.get(1) : list.size() == 1 ? list.get(0) : null;
  }

  public static @Nullable DartReferenceExpression getReferenceExpression(final @NotNull DartType dartType) {
    final DartSimpleType simpleType = dartType.getSimpleType();
    return simpleType == null ? null : simpleType.getReferenceExpression();
  }

  public static @Nullable DartTypeArguments getTypeArguments(final @NotNull DartType dartType) {
    final DartSimpleType simpleType = dartType.getSimpleType();
    return simpleType == null ? null : simpleType.getTypeArguments();
  }

  public static @Nullable PsiElement resolveReference(final @NotNull DartType dartType) {
    CachedValue<PsiElement> cachedValue = dartType.getUserData(DART_TYPE_CACHED_RESOLVE_RESULT_KEY);

    if (cachedValue == null) {
      cachedValue = CachedValuesManager.getManager(dartType.getProject()).createCachedValue(
        () -> new CachedValueProvider.Result<>(doResolveTypeReference(dartType), PsiModificationTracker.MODIFICATION_COUNT), false);

      dartType.putUserData(DART_TYPE_CACHED_RESOLVE_RESULT_KEY, cachedValue);
    }

    return cachedValue.getValue();
  }

  private static PsiElement doResolveTypeReference(final DartType dartType) {
    final DartReference expression = dartType.getReferenceExpression();
    if (expression == null) {
      return null;
    }
    final String typeName = expression.getText();
    if (typeName.indexOf('.') != -1) {
      return expression.resolve();
    }
    List<DartComponentName> result = new ArrayList<>();
    final DartResolveProcessor dartResolveProcessor = new DartResolveProcessor(result, typeName);

    final VirtualFile virtualFile = DartResolveUtil.getRealVirtualFile(dartType.getContainingFile());
    if (virtualFile != null) {
      DartResolveUtil.processTopLevelDeclarations(dartType, dartResolveProcessor, virtualFile, typeName);
    }

    // find type parameter
    if (result.isEmpty()) {
      PsiTreeUtil.treeWalkUp(dartResolveProcessor, dartType, null, ResolveState.initial());
      for (Iterator<DartComponentName> iterator = result.iterator(); iterator.hasNext(); ) {
        if (!(iterator.next().getParent() instanceof DartTypeParameter)) {
          iterator.remove();
        }
      }
    }

    // global
    if (result.isEmpty()) {
      final List<VirtualFile> libraryFiles = DartResolveUtil.findLibrary(dartType.getContainingFile());
      DartResolveUtil.processTopLevelDeclarations(dartType, dartResolveProcessor, libraryFiles, typeName);
    }

    return result.isEmpty() ? null : result.iterator().next();
  }

  public static @Nullable DartComponentName findComponentName(final @NotNull DartNormalFormalParameter normalFormalParameter) {
    final DartFunctionFormalParameter functionFormalParameter = normalFormalParameter.getFunctionFormalParameter();
    final DartFieldFormalParameter fieldFormalParameter = normalFormalParameter.getFieldFormalParameter();
    final DartSimpleFormalParameter simpleFormalParameter = normalFormalParameter.getSimpleFormalParameter();

    if (functionFormalParameter != null) return functionFormalParameter.getComponentName();
    if (fieldFormalParameter != null) return null;
    if (simpleFormalParameter != null) return simpleFormalParameter.getComponentName();

    return null;
  }

  public static DartExpression getParameterReferenceExpression(DartNamedArgument argument) {
    return PsiTreeUtil.getChildOfType(argument, DartExpression.class);
  }

  public static DartExpression getExpression(DartNamedArgument argument) {
    final DartExpression[] expressions = PsiTreeUtil.getChildrenOfType(argument, DartExpression.class);
    return expressions != null && expressions.length > 1 ? expressions[expressions.length - 1] : null;
  }

  public static @Nullable IDartBlock getBlock(DartFunctionBody functionBody) {
    return PsiTreeUtil.getChildOfType(functionBody, IDartBlock.class);
  }

  public static boolean isConstantObjectExpression(final @NotNull DartNewExpression newExpression) {
    final PsiElement child = newExpression.getFirstChild();
    return child != null && child.getNode().getElementType() == DartTokenTypes.CONST;
  }

  public static @Nullable DartArguments getArguments(final @NotNull DartNewExpression newExpression) {
    return PsiTreeUtil.findChildOfType(newExpression, DartArguments.class);
  }

  public static @Nullable DartArguments getArguments(final @NotNull DartCallExpression callExpression) {
    return PsiTreeUtil.findChildOfType(callExpression, DartArguments.class);
  }

  public static @Nullable DartExpression getCondition(@NotNull DartPsiCompositeElement ifStatement) {
    return PsiTreeUtil.findChildOfType(ifStatement, DartExpression.class);
  }

  public static @Nullable PsiElement getThenBranch(@NotNull DartIfStatement ifStatement) {
    return getBranchAfter(getCondition(ifStatement));
  }

  public static @Nullable PsiElement getElseBranch(@NotNull DartIfStatement ifStatement) {
    return getBranchAfter(getThenBranch(ifStatement));
  }

  private static @Nullable PsiElement getBranchAfter(@Nullable PsiElement child) {
    return PsiTreeUtil.skipSiblingsForward(child, LeafPsiElement.class, PsiWhiteSpace.class, PsiComment.class);
  }

  public static @Nullable PsiElement getDoBody(@NotNull DartDoWhileStatement doStatement) {
    return getBranchAfter(doStatement);
  }

  public static @Nullable PsiElement getForBody(@NotNull DartForStatement forStatement) {
    return forStatement.getLastChild();
  }

  public static @Nullable PsiElement getWhileBody(@NotNull DartWhileStatement whileStatement) {
    return getBranchAfter(getCondition(whileStatement));
  }
}
