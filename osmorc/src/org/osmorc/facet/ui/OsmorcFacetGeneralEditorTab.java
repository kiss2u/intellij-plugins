/*
 * Copyright (c) 2007-2009, Osmorc Development Team
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without modification,
 * are permitted provided that the following conditions are met:
 *     * Redistributions of source code must retain the above copyright notice, this list
 *       of conditions and the following disclaimer.
 *     * Redistributions in binary form must reproduce the above copyright notice, this
 *       list of conditions and the following disclaimer in the documentation and/or other
 *       materials provided with the distribution.
 *     * Neither the name of 'Osmorc Development Team' nor the names of its contributors may be
 *       used to endorse or promote products derived from this software without specific
 *       prior written permission.
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND ANY
 * EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF
 * MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL
 * THE COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 * SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT
 * OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION)
 * HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR
 * TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE,
 * EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package org.osmorc.facet.ui;

import com.intellij.compiler.server.BuildManager;
import com.intellij.facet.ui.FacetEditorContext;
import com.intellij.facet.ui.FacetEditorTab;
import com.intellij.facet.ui.FacetEditorValidator;
import com.intellij.facet.ui.FacetValidatorsManager;
import com.intellij.facet.ui.ValidationResult;
import com.intellij.framework.library.DownloadableLibraryService;
import com.intellij.openapi.fileChooser.FileChooser;
import com.intellij.openapi.fileChooser.FileChooserDescriptor;
import com.intellij.openapi.fileChooser.FileChooserDescriptorFactory;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.roots.ModuleRootManager;
import com.intellij.openapi.roots.ui.configuration.libraries.AddCustomLibraryDialog;
import com.intellij.openapi.roots.ui.configuration.libraries.CustomLibraryDescription;
import com.intellij.openapi.ui.TextFieldWithBrowseButton;
import com.intellij.openapi.util.Key;
import com.intellij.openapi.util.io.FileUtil;
import com.intellij.openapi.vfs.VfsUtilCore;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.ui.IdeBorderFactory;
import com.intellij.ui.UserActivityWatcher;
import com.intellij.ui.components.ActionLink;
import com.intellij.ui.components.JBLabel;
import com.intellij.uiDesigner.core.GridConstraints;
import com.intellij.uiDesigner.core.GridLayoutManager;
import com.intellij.uiDesigner.core.Spacer;
import com.intellij.util.ui.UIUtil;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.osgi.jps.model.ManifestGenerationMode;
import org.osmorc.facet.OsgiCoreLibraryType;
import org.osmorc.facet.OsmorcFacetConfiguration;
import org.osmorc.i18n.OsmorcBundle;
import org.osmorc.settings.ProjectSettings;

import javax.swing.AbstractButton;
import javax.swing.BorderFactory;
import javax.swing.ButtonGroup;
import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.border.TitledBorder;
import java.awt.Insets;
import java.io.File;
import java.lang.reflect.Method;
import java.util.ResourceBundle;

/**
 * The facet editor tab which is used to set up general Osmorc facet settings.
 *
 * @author <a href="mailto:janthomae@janthomae.de">Jan Thomä</a>
 * @author <a href="mailto:robert@beeger.net">Robert F. Beeger</a>
 */
public class OsmorcFacetGeneralEditorTab extends FacetEditorTab {
  static final Key<Boolean> MANUAL_MANIFEST_EDITING_KEY = Key.create("MANUAL_MANIFEST_EDITING");
  static final Key<Boolean> EXT_TOOL_MANIFEST_CREATION_KEY = Key.create("EXT_TOOL_MANIFEST_CREATION");

  private final JRadioButton myManuallyEditedRadioButton;
  private final JRadioButton myControlledByOsmorcRadioButton;
  private final TextFieldWithBrowseButton myManifestFileChooser;
  private final JPanel myRoot;
  private final JRadioButton myUseProjectDefaultManifestFileLocation;
  private final JRadioButton myUseModuleSpecificManifestFileLocation;
  private final JRadioButton myUseBndFileRadioButton;
  private final JCheckBox myUseBndMavenPluginCheckBox;
  private final JPanel myManifestPanel;
  private final TextFieldWithBrowseButton myBndFile;
  private final JPanel myBndPanel;
  private final JRadioButton myUseBundlorFileRadioButton;
  private final TextFieldWithBrowseButton myBundlorFile;
  private final JPanel myBundlorPanel;
  private final JCheckBox myDoNotSynchronizeFacetCheckBox;
  @SuppressWarnings("unused") private final ActionLink mySetupCoreLibLink;

  private final FacetEditorContext myEditorContext;
  private final FacetValidatorsManager myValidatorsManager;
  private final Module myModule;
  private boolean myModified;

  public OsmorcFacetGeneralEditorTab(FacetEditorContext editorContext, FacetValidatorsManager validatorsManager) {
    myEditorContext = editorContext;
    myModule = editorContext.getModule();
    myValidatorsManager = validatorsManager;
    {
      mySetupCoreLibLink = new ActionLink("", e -> {
        CustomLibraryDescription description = DownloadableLibraryService.getInstance().createDescriptionForType(OsgiCoreLibraryType.class);
        AddCustomLibraryDialog.createDialog(description, myModule, null).show();
      });
    }
    {
      // GUI initializer generated by IntelliJ IDEA GUI Designer
      // >>> IMPORTANT!! <<<
      // DO NOT EDIT OR ADD ANY CODE HERE!
      myRoot = new JPanel();
      myRoot.setLayout(new GridLayoutManager(4, 1, new Insets(0, 0, 0, 0), -1, -1));
      final JPanel panel1 = new JPanel();
      panel1.setLayout(new GridLayoutManager(7, 1, new Insets(0, 0, 0, 0), -1, -1));
      panel1.putClientProperty("BorderFactoryClass", "com.intellij.ui.IdeBorderFactory$PlainSmallWithoutIndent");
      myRoot.add(panel1, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH,
                                             GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW,
                                             GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null,
                                             0, false));
      panel1.setBorder(IdeBorderFactory.PlainSmallWithoutIndent.createTitledBorder(BorderFactory.createEtchedBorder(),
                                                                                   this.$$$getMessageFromBundle$$$("messages/OsmorcBundle",
                                                                                                                   "facet.form.general.group.creation"),
                                                                                   TitledBorder.DEFAULT_JUSTIFICATION,
                                                                                   TitledBorder.DEFAULT_POSITION, null, null));
      myControlledByOsmorcRadioButton = new JRadioButton();
      this.$$$loadButtonText$$$(myControlledByOsmorcRadioButton,
                                this.$$$getMessageFromBundle$$$("messages/OsmorcBundle", "facet.form.general.plugin.controlled"));
      panel1.add(myControlledByOsmorcRadioButton, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE,
                                                                      GridConstraints.SIZEPOLICY_CAN_SHRINK |
                                                                      GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED,
                                                                      null, null, null, 0, false));
      myManuallyEditedRadioButton = new JRadioButton();
      myManuallyEditedRadioButton.setSelected(true);
      this.$$$loadButtonText$$$(myManuallyEditedRadioButton,
                                this.$$$getMessageFromBundle$$$("messages/OsmorcBundle", "facet.form.general.existing"));
      panel1.add(myManuallyEditedRadioButton, new GridConstraints(1, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE,
                                                                  GridConstraints.SIZEPOLICY_CAN_SHRINK |
                                                                  GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED,
                                                                  null, null, null, 0, false));
      myManifestPanel = new JPanel();
      myManifestPanel.setLayout(new GridLayoutManager(2, 2, new Insets(0, 0, 0, 0), -1, -1));
      myManifestPanel.putClientProperty("BorderFactoryClass", "com.intellij.ui.IdeBorderFactory$PlainSmallWithIndent");
      panel1.add(myManifestPanel, new GridConstraints(2, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH,
                                                      GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW,
                                                      GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null,
                                                      null, null, 3, true));
      myUseProjectDefaultManifestFileLocation = new JRadioButton();
      myUseProjectDefaultManifestFileLocation.setSelected(true);
      this.$$$loadButtonText$$$(myUseProjectDefaultManifestFileLocation,
                                this.$$$getMessageFromBundle$$$("messages/OsmorcBundle", "facet.form.general.project.default"));
      myManifestPanel.add(myUseProjectDefaultManifestFileLocation,
                          new GridConstraints(0, 1, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE,
                                              GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW,
                                              GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
      final JPanel panel2 = new JPanel();
      panel2.setLayout(new GridLayoutManager(1, 2, new Insets(0, 0, 0, 0), -1, -1));
      myManifestPanel.add(panel2, new GridConstraints(1, 1, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH,
                                                      GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_WANT_GROW,
                                                      GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null,
                                                      null, null, 0, false));
      myUseModuleSpecificManifestFileLocation = new JRadioButton();
      myUseModuleSpecificManifestFileLocation.setHorizontalAlignment(10);
      this.$$$loadButtonText$$$(myUseModuleSpecificManifestFileLocation,
                                this.$$$getMessageFromBundle$$$("messages/OsmorcBundle", "facet.form.general.custom"));
      panel2.add(myUseModuleSpecificManifestFileLocation,
                 new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED,
                                     GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
      myManifestFileChooser = new TextFieldWithBrowseButton();
      myManifestFileChooser.setEnabled(false);
      panel2.add(myManifestFileChooser, new GridConstraints(0, 1, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL,
                                                            GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_FIXED, null,
                                                            null, null, 0, false));
      final JPanel panel3 = new JPanel();
      panel3.setLayout(new GridLayoutManager(2, 1, new Insets(0, 0, 0, 0), -1, 0));
      myManifestPanel.add(panel3, new GridConstraints(0, 0, 2, 1, GridConstraints.ANCHOR_NORTH, GridConstraints.FILL_HORIZONTAL,
                                                      GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW,
                                                      GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null,
                                                      null, null, 0, false));
      final JBLabel jBLabel1 = new JBLabel();
      this.$$$loadLabelText$$$(jBLabel1, this.$$$getMessageFromBundle$$$("messages/OsmorcBundle", "facet.form.general.custom.path"));
      panel3.add(jBLabel1,
                 new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED,
                                     GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
      final JBLabel jBLabel2 = new JBLabel();
      jBLabel2.setComponentStyle(UIUtil.ComponentStyle.SMALL);
      jBLabel2.setFontColor(UIUtil.FontColor.BRIGHTER);
      this.$$$loadLabelText$$$(jBLabel2, this.$$$getMessageFromBundle$$$("messages/OsmorcBundle", "facet.form.general.custom.comment"));
      panel3.add(jBLabel2,
                 new GridConstraints(1, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED,
                                     GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
      myUseBndFileRadioButton = new JRadioButton();
      this.$$$loadButtonText$$$(myUseBndFileRadioButton,
                                this.$$$getMessageFromBundle$$$("messages/OsmorcBundle", "facet.form.general.bnd"));
      panel1.add(myUseBndFileRadioButton, new GridConstraints(3, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE,
                                                              GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW,
                                                              GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
      myBndPanel = new JPanel();
      myBndPanel.setLayout(new GridLayoutManager(2, 2, new Insets(0, 0, 0, 0), -1, -1));
      myBndPanel.putClientProperty("BorderFactoryClass", "com.intellij.ui.IdeBorderFactory$PlainSmallWithoutIndent");
      panel1.add(myBndPanel, new GridConstraints(4, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH,
                                                 GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW,
                                                 GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null,
                                                 null, 3, true));
      myBndFile = new TextFieldWithBrowseButton();
      myBndPanel.add(myBndFile, new GridConstraints(0, 1, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL,
                                                    GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null,
                                                    null, 0, false));
      final JBLabel jBLabel3 = new JBLabel();
      this.$$$loadLabelText$$$(jBLabel3, this.$$$getMessageFromBundle$$$("messages/OsmorcBundle", "facet.form.general.bnd.path"));
      myBndPanel.add(jBLabel3, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_NONE,
                                                   GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0,
                                                   false));
      myUseBndMavenPluginCheckBox = new JCheckBox();
      this.$$$loadButtonText$$$(myUseBndMavenPluginCheckBox,
                                this.$$$getMessageFromBundle$$$("messages/OsmorcBundle", "facet.form.general.bndmavenplugin"));
      myBndPanel.add(myUseBndMavenPluginCheckBox, new GridConstraints(1, 1, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE,
                                                                      GridConstraints.SIZEPOLICY_CAN_SHRINK |
                                                                      GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED,
                                                                      null, null, null, 0, false));
      myUseBundlorFileRadioButton = new JRadioButton();
      this.$$$loadButtonText$$$(myUseBundlorFileRadioButton,
                                this.$$$getMessageFromBundle$$$("messages/OsmorcBundle", "facet.form.general.bundlor"));
      panel1.add(myUseBundlorFileRadioButton, new GridConstraints(5, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE,
                                                                  GridConstraints.SIZEPOLICY_CAN_SHRINK |
                                                                  GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED,
                                                                  null, null, null, 0, false));
      myBundlorPanel = new JPanel();
      myBundlorPanel.setLayout(new GridLayoutManager(1, 2, new Insets(0, 0, 0, 0), -1, -1));
      myBundlorPanel.putClientProperty("BorderFactoryClass", "com.intellij.ui.IdeBorderFactory$PlainSmallWithoutIndent");
      panel1.add(myBundlorPanel, new GridConstraints(6, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH,
                                                     GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW,
                                                     GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null,
                                                     null, null, 3, true));
      myBundlorFile = new TextFieldWithBrowseButton();
      myBundlorPanel.add(myBundlorFile, new GridConstraints(0, 1, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL,
                                                            GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_FIXED, null,
                                                            null, null, 0, false));
      final JBLabel jBLabel4 = new JBLabel();
      this.$$$loadLabelText$$$(jBLabel4, this.$$$getMessageFromBundle$$$("messages/OsmorcBundle", "facet.form.general.bundlor.path"));
      myBundlorPanel.add(jBLabel4, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_NONE,
                                                       GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null,
                                                       0, false));
      final JPanel panel4 = new JPanel();
      panel4.setLayout(new GridLayoutManager(1, 1, new Insets(0, 0, 0, 0), -1, -1));
      panel4.putClientProperty("BorderFactoryClass", "com.intellij.ui.IdeBorderFactory$PlainSmallWithoutIndent");
      myRoot.add(panel4, new GridConstraints(1, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH,
                                             GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW,
                                             GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null,
                                             0, false));
      panel4.setBorder(IdeBorderFactory.PlainSmallWithoutIndent.createTitledBorder(BorderFactory.createEtchedBorder(),
                                                                                   this.$$$getMessageFromBundle$$$("messages/OsmorcBundle",
                                                                                                                   "facet.form.general.group.maven"),
                                                                                   TitledBorder.DEFAULT_JUSTIFICATION,
                                                                                   TitledBorder.DEFAULT_POSITION, null, null));
      myDoNotSynchronizeFacetCheckBox = new JCheckBox();
      this.$$$loadButtonText$$$(myDoNotSynchronizeFacetCheckBox,
                                this.$$$getMessageFromBundle$$$("messages/OsmorcBundle", "facet.form.general.no.sync"));
      panel4.add(myDoNotSynchronizeFacetCheckBox, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE,
                                                                      GridConstraints.SIZEPOLICY_CAN_SHRINK |
                                                                      GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED,
                                                                      null, null, null, 0, false));
      this.$$$loadButtonText$$$(mySetupCoreLibLink,
                                this.$$$getMessageFromBundle$$$("messages/OsmorcBundle", "facet.form.general.core.conf"));
      myRoot.add(mySetupCoreLibLink, new GridConstraints(2, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH,
                                                         GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW,
                                                         GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null,
                                                         null, null, 0, false));
      final Spacer spacer1 = new Spacer();
      myRoot.add(spacer1, new GridConstraints(3, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_VERTICAL, 1,
                                              GridConstraints.SIZEPOLICY_WANT_GROW, null, null, null, 0, false));
      ButtonGroup buttonGroup;
      buttonGroup = new ButtonGroup();
      buttonGroup.add(myControlledByOsmorcRadioButton);
      buttonGroup.add(myManuallyEditedRadioButton);
      buttonGroup.add(myUseBndFileRadioButton);
      buttonGroup.add(myUseBundlorFileRadioButton);
      buttonGroup = new ButtonGroup();
      buttonGroup.add(myUseModuleSpecificManifestFileLocation);
      buttonGroup.add(myUseProjectDefaultManifestFileLocation);
    }

    myManifestFileChooser.addActionListener(e -> chooseFile(myManifestFileChooser));
    myBndFile.addActionListener(e -> chooseFile(myBndFile));
    myBundlorFile.addActionListener(e -> chooseFile(myBundlorFile));
    myUseProjectDefaultManifestFileLocation.addChangeListener(e -> manifestFileLocationSelectorChanged());

    UserActivityWatcher watcher = new UserActivityWatcher();
    watcher.addUserActivityListener(() -> {
      myModified = true;
      updateGui();
    });
    watcher.register(myRoot);

    myValidatorsManager.registerValidator(new FacetEditorValidator() {
      @Override
      public @NotNull ValidationResult check() {
        if (myManuallyEditedRadioButton.isSelected()) {
          String location = myUseModuleSpecificManifestFileLocation.isSelected() ? myManifestFileChooser.getText() :
                            ProjectSettings.getInstance(myModule.getProject()).getDefaultManifestFileLocation();
          if (findFileInContentRoots(location, myModule) == null) {
            return new ValidationResult(OsmorcBundle.message("facet.editor.manifest.not.found", location));
          }
        }
        if (myUseBndFileRadioButton.isSelected()) {
          String location = myBndFile.getText();
          if (findFileInContentRoots(location, myModule) == null) {
            return new ValidationResult(OsmorcBundle.message("facet.editor.bnd.not.found", location));
          }
        }
        if (myUseBundlorFileRadioButton.isSelected()) {
          String location = myBundlorFile.getText();
          if (findFileInContentRoots(location, myModule) == null) {
            return new ValidationResult(OsmorcBundle.message("facet.editor.bundlor.not.found", location));
          }
        }
        return ValidationResult.OK;
      }
    });
  }

  private static Method $$$cachedGetBundleMethod$$$ = null;

  /** @noinspection ALL */
  private String $$$getMessageFromBundle$$$(String path, String key) {
    ResourceBundle bundle;
    try {
      Class<?> thisClass = this.getClass();
      if ($$$cachedGetBundleMethod$$$ == null) {
        Class<?> dynamicBundleClass = thisClass.getClassLoader().loadClass("com.intellij.DynamicBundle");
        $$$cachedGetBundleMethod$$$ = dynamicBundleClass.getMethod("getBundle", String.class, Class.class);
      }
      bundle = (ResourceBundle)$$$cachedGetBundleMethod$$$.invoke(null, path, thisClass);
    }
    catch (Exception e) {
      bundle = ResourceBundle.getBundle(path);
    }
    return bundle.getString(key);
  }

  /** @noinspection ALL */
  private void $$$loadLabelText$$$(JLabel component, String text) {
    StringBuffer result = new StringBuffer();
    boolean haveMnemonic = false;
    char mnemonic = '\0';
    int mnemonicIndex = -1;
    for (int i = 0; i < text.length(); i++) {
      if (text.charAt(i) == '&') {
        i++;
        if (i == text.length()) break;
        if (!haveMnemonic && text.charAt(i) != '&') {
          haveMnemonic = true;
          mnemonic = text.charAt(i);
          mnemonicIndex = result.length();
        }
      }
      result.append(text.charAt(i));
    }
    component.setText(result.toString());
    if (haveMnemonic) {
      component.setDisplayedMnemonic(mnemonic);
      component.setDisplayedMnemonicIndex(mnemonicIndex);
    }
  }

  /** @noinspection ALL */
  private void $$$loadButtonText$$$(AbstractButton component, String text) {
    StringBuffer result = new StringBuffer();
    boolean haveMnemonic = false;
    char mnemonic = '\0';
    int mnemonicIndex = -1;
    for (int i = 0; i < text.length(); i++) {
      if (text.charAt(i) == '&') {
        i++;
        if (i == text.length()) break;
        if (!haveMnemonic && text.charAt(i) != '&') {
          haveMnemonic = true;
          mnemonic = text.charAt(i);
          mnemonicIndex = result.length();
        }
      }
      result.append(text.charAt(i));
    }
    component.setText(result.toString());
    if (haveMnemonic) {
      component.setMnemonic(mnemonic);
      component.setDisplayedMnemonicIndex(mnemonicIndex);
    }
  }

  /** @noinspection ALL */
  public JComponent $$$getRootComponent$$$() { return myRoot; }

  private void updateGui() {
    boolean isBnd = myUseBndFileRadioButton.isSelected() && !myUseBndMavenPluginCheckBox.isSelected();
    boolean isBndMavenPlugin = myUseBndFileRadioButton.isSelected() && myUseBndMavenPluginCheckBox.isSelected();
    boolean isBundlor = myUseBundlorFileRadioButton.isSelected();
    boolean isManuallyEdited = myManuallyEditedRadioButton.isSelected();

    myEditorContext.putUserData(MANUAL_MANIFEST_EDITING_KEY, isManuallyEdited);
    myEditorContext.putUserData(EXT_TOOL_MANIFEST_CREATION_KEY, isBnd || isBndMavenPlugin || isBundlor);

    myBndPanel.setEnabled(isBnd || isBndMavenPlugin);
    myBundlorPanel.setEnabled(isBundlor);
    myManifestPanel.setEnabled(isManuallyEdited);
    myUseProjectDefaultManifestFileLocation.setEnabled(isManuallyEdited);
    myUseModuleSpecificManifestFileLocation.setEnabled(isManuallyEdited);
    myManifestFileChooser.setEnabled(isManuallyEdited && !myUseProjectDefaultManifestFileLocation.isSelected());
    myBndFile.setEnabled(isBnd);
    myUseBndMavenPluginCheckBox.setEnabled(isBnd || isBndMavenPlugin);
    myBundlorFile.setEnabled(isBundlor);

    myValidatorsManager.validate();
  }

  private void chooseFile(TextFieldWithBrowseButton field) {
    FileChooserDescriptor descriptor = FileChooserDescriptorFactory.createSingleFileNoJarsDescriptor();
    VirtualFile toSelect = findFileInContentRoots(field.getText(), myModule);
    VirtualFile file = FileChooser.chooseFile(descriptor, myModule.getProject(), toSelect);
    if (file != null) {
      for (VirtualFile root : getContentRoots(myModule)) {
        String relativePath = VfsUtilCore.getRelativePath(file, root, File.separatorChar);
        if (relativePath != null) {
          if (field == myManifestFileChooser && file.isDirectory()) {
            relativePath += "/MANIFEST.MF"; //NON-NLS
          }
          field.setText(relativePath);
          break;
        }
      }
    }
  }

  private void manifestFileLocationSelectorChanged() {
    myManifestFileChooser.setEnabled(!myUseProjectDefaultManifestFileLocation.isSelected());
    myModified = true;
  }

  @Override
  public @Nls String getDisplayName() {
    return OsmorcBundle.message("facet.tab.general");
  }

  @Override
  public String getHelpTopic() {
    return "reference.settings.module.facet.osgi";
  }

  @Override
  public @NotNull JComponent createComponent() {
    return myRoot;
  }

  @Override
  public boolean isModified() {
    return myModified;
  }

  @Override
  public void apply() {
    OsmorcFacetConfiguration configuration = (OsmorcFacetConfiguration)myEditorContext.getFacet().getConfiguration();
    configuration.setManifestGenerationMode(
      myControlledByOsmorcRadioButton.isSelected() ? ManifestGenerationMode.OsmorcControlled :
      myUseBndFileRadioButton.isSelected() ?
      (!myUseBndMavenPluginCheckBox.isSelected() ? ManifestGenerationMode.Bnd : ManifestGenerationMode.BndMavenPlugin) :
      myUseBundlorFileRadioButton.isSelected() ? ManifestGenerationMode.Bundlor :
      ManifestGenerationMode.Manually);
    configuration.setManifestLocation(FileUtil.toSystemIndependentName(myManifestFileChooser.getText()));
    configuration.setUseProjectDefaultManifestFileLocation(myUseProjectDefaultManifestFileLocation.isSelected());
    configuration.setBndFileLocation(FileUtil.toSystemIndependentName(myBndFile.getText()));
    configuration.setBundlorFileLocation(FileUtil.toSystemIndependentName(myBundlorFile.getText()));
    configuration.setDoNotSynchronizeWithMaven(myDoNotSynchronizeFacetCheckBox.isSelected());

    if (myModified) {
      BuildManager.getInstance().clearState(myModule.getProject());
    }
    myModified = false;
  }

  @Override
  public void reset() {
    OsmorcFacetConfiguration configuration = (OsmorcFacetConfiguration)myEditorContext.getFacet().getConfiguration();
    if (configuration.isUseBndFile()) {
      myUseBndFileRadioButton.setSelected(true);
      myUseBndMavenPluginCheckBox.setSelected(false);
    }
    else if (configuration.isUseBndMavenPlugin()) {
      myUseBndFileRadioButton.setSelected(true);
      myUseBndMavenPluginCheckBox.setSelected(true);
    }
    else if (configuration.isUseBundlorFile()) {
      myUseBundlorFileRadioButton.setSelected(true);
    }
    else if (configuration.isOsmorcControlsManifest()) {
      myControlledByOsmorcRadioButton.setSelected(true);
    }
    else {
      myManuallyEditedRadioButton.setSelected(true);
    }
    myManifestFileChooser.setText(FileUtil.toSystemDependentName(configuration.getManifestLocation()));
    if (configuration.isUseProjectDefaultManifestFileLocation()) {
      myUseProjectDefaultManifestFileLocation.setSelected(true);
    }
    else {
      myUseModuleSpecificManifestFileLocation.setSelected(true);
    }
    myBndFile.setText(FileUtil.toSystemDependentName(configuration.getBndFileLocation()));
    myBundlorFile.setText(FileUtil.toSystemDependentName(configuration.getBundlorFileLocation()));
    myDoNotSynchronizeFacetCheckBox.setSelected(configuration.isDoNotSynchronizeWithMaven());

    updateGui();
    myModified = false;
  }

  @Override
  public void onTabEntering() {
    updateGui();
  }

  private static VirtualFile[] getContentRoots(Module module) {
    return ModuleRootManager.getInstance(module).getContentRoots();
  }

  private static VirtualFile findFileInContentRoots(String fileName, Module module) {
    for (VirtualFile root : getContentRoots(module)) {
      VirtualFile file = root.findFileByRelativePath(FileUtil.toSystemIndependentName(fileName));
      if (file != null) {
        return file;
      }
    }
    return null;
  }
}
