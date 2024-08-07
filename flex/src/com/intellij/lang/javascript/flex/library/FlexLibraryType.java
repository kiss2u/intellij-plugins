// Copyright 2000-2024 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package com.intellij.lang.javascript.flex.library;

import com.intellij.lang.javascript.flex.FlexModuleType;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleType;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.libraries.LibraryType;
import com.intellij.openapi.roots.libraries.LibraryTypeService;
import com.intellij.openapi.roots.libraries.NewLibraryConfiguration;
import com.intellij.openapi.roots.libraries.PersistentLibraryKind;
import com.intellij.openapi.roots.libraries.ui.LibraryEditorComponent;
import com.intellij.openapi.roots.libraries.ui.LibraryPropertiesEditor;
import com.intellij.openapi.roots.libraries.ui.LibraryRootsComponentDescriptor;
import com.intellij.openapi.roots.ui.configuration.FacetsProvider;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.util.PlatformIcons;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;

public final class FlexLibraryType extends LibraryType<FlexLibraryProperties> {
  public static final PersistentLibraryKind<FlexLibraryProperties> FLEX_LIBRARY = new PersistentLibraryKind<>("flex") {
    @Override
    public @NotNull FlexLibraryProperties createDefaultProperties() {
      return new FlexLibraryProperties();
    }
  };

  public FlexLibraryType() {
    super(FLEX_LIBRARY);
  }

  @Override
  public @NotNull String getCreateActionName() {
    return "ActionScript/Flex";
  }

  @Override
  public NewLibraryConfiguration createNewLibrary(@NotNull JComponent parentComponent,
                                                  @Nullable VirtualFile contextDirectory, final @NotNull Project project) {
    return LibraryTypeService.getInstance().createLibraryFromFiles(createLibraryRootsComponentDescriptor(), parentComponent, contextDirectory, this,
                                                                   project);
  }

  @Override
  public boolean isSuitableModule(@NotNull Module module, @NotNull FacetsProvider facetsProvider) {
    return ModuleType.get(module).equals(FlexModuleType.getInstance());
  }

  @Override
  public LibraryPropertiesEditor createPropertiesEditor(final @NotNull LibraryEditorComponent<FlexLibraryProperties> properties) {
    return null;
  }

  @Override
  public Icon getIcon(FlexLibraryProperties properties) {
    return PlatformIcons.LIBRARY_ICON;    // TODO: change icon to Flex specific only when automatic library converters are done
  }

  @Override
  public @NotNull LibraryRootsComponentDescriptor createLibraryRootsComponentDescriptor() {
    return new FlexLibraryRootsComponentDescriptor();
  }

  public static FlexLibraryType getInstance() {
    return LibraryType.EP_NAME.findExtension(FlexLibraryType.class);
  }
}
