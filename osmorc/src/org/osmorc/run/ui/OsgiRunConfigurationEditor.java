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
package org.osmorc.run.ui;

import com.intellij.execution.ui.DefaultJreSelector;
import com.intellij.execution.ui.JrePathEditor;
import com.intellij.openapi.fileChooser.FileChooserDescriptorFactory;
import com.intellij.openapi.options.ConfigurationException;
import com.intellij.openapi.options.SettingsEditor;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.CompilerProjectExtension;
import com.intellij.openapi.ui.TextFieldWithBrowseButton;
import com.intellij.openapi.vfs.VfsUtilCore;
import com.intellij.ui.RawCommandLineEditor;
import com.intellij.ui.SpeedSearchComparator;
import com.intellij.ui.TableSpeedSearch;
import com.intellij.ui.ToolbarDecorator;
import com.intellij.ui.components.JBLabel;
import com.intellij.ui.components.JBTabbedPane;
import com.intellij.ui.table.JBTable;
import com.intellij.uiDesigner.core.GridConstraints;
import com.intellij.uiDesigner.core.GridLayoutManager;
import com.intellij.uiDesigner.core.Spacer;
import org.jetbrains.annotations.NotNull;
import org.osmorc.frameworkintegration.FrameworkInstanceDefinition;
import org.osmorc.frameworkintegration.FrameworkIntegrator;
import org.osmorc.frameworkintegration.FrameworkIntegratorRegistry;
import org.osmorc.run.OsgiRunConfiguration;
import org.osmorc.run.OsgiRunConfigurationChecker;
import org.osmorc.run.OsgiRunConfigurationCheckerProvider;
import org.osmorc.settings.ApplicationSettings;
import org.osmorc.util.FrameworkInstanceRenderer;

import javax.swing.AbstractButton;
import javax.swing.ButtonGroup;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JSpinner;
import javax.swing.JTabbedPane;
import javax.swing.JTable;
import javax.swing.SpinnerNumberModel;
import javax.swing.table.AbstractTableModel;
import javax.swing.text.DefaultFormatterFactory;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Insets;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.ResourceBundle;
import java.util.stream.IntStream;

import static org.osmorc.i18n.OsmorcBundle.message;

/**
 * Editor for a bundle run configuration.
 *
 * @author <a href="mailto:janthomae@janthomae.de">Jan Thomä</a>
 * @author <a href="mailto:robert@beeger.net">Robert F. Beeger</a>
 */
public class OsgiRunConfigurationEditor extends SettingsEditor<OsgiRunConfiguration> {
  private final JTabbedPane root;
  // framework& bundles tab
  private final JComboBox<FrameworkInstanceDefinition> myFrameworkInstances;
  private final JSpinner myFrameworkStartLevel;
  private final JSpinner myDefaultStartLevel;
  private final JPanel myBundlesPanel;
  private final JTable myBundlesTable;
  // parameters tab
  private final RawCommandLineEditor myVmOptions;
  private final RawCommandLineEditor myProgramParameters;
  private final JRadioButton myOsmorcControlledDir;
  private final JRadioButton myUserDefinedDir;
  private final TextFieldWithBrowseButton myWorkingDirField;
  private final JrePathEditor myJrePathEditor;
  private final JCheckBox myClassPathAllBundles;
  // additional properties tab
  private final JPanel myAdditionalPropertiesPanel;

  private final Project myProject;
  private OsgiRunConfiguration myRunConfiguration;
  private FrameworkRunPropertiesEditor myCurrentRunPropertiesEditor;

  public OsgiRunConfigurationEditor(final Project project) {
    myProject = project;
    {
      // GUI initializer generated by IntelliJ IDEA GUI Designer
      // >>> IMPORTANT!! <<<
      // DO NOT EDIT OR ADD ANY CODE HERE!
      root = new JBTabbedPane();
      final JPanel panel1 = new JPanel();
      panel1.setLayout(new GridLayoutManager(2, 1, new Insets(0, 0, 0, 0), -1, -1));
      panel1.setMinimumSize(new Dimension(500, 200));
      root.addTab(this.$$$getMessageFromBundle$$$("messages/OsmorcBundle", "run.configuration.form.tab.main"), panel1);
      final JPanel panel2 = new JPanel();
      panel2.setLayout(new GridLayoutManager(3, 3, new Insets(0, 0, 0, 0), -1, -1));
      panel1.add(panel2, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH,
                                             GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW,
                                             GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null,
                                             0, false));
      final JLabel label1 = new JLabel();
      this.$$$loadLabelText$$$(label1, this.$$$getMessageFromBundle$$$("messages/OsmorcBundle", "run.configuration.form.framework"));
      panel2.add(label1,
                 new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED,
                                     GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
      myFrameworkInstances = new JComboBox();
      panel2.add(myFrameworkInstances, new GridConstraints(0, 1, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL,
                                                           GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW,
                                                           GridConstraints.SIZEPOLICY_FIXED, null, new Dimension(100, -1), null, 0, false));
      final JLabel label2 = new JLabel();
      this.$$$loadLabelText$$$(label2, this.$$$getMessageFromBundle$$$("messages/OsmorcBundle", "run.configuration.form.start.framework"));
      panel2.add(label2,
                 new GridConstraints(1, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED,
                                     GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
      myFrameworkStartLevel = new JSpinner();
      panel2.add(myFrameworkStartLevel, new GridConstraints(1, 1, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL,
                                                            GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW,
                                                            GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
      final JLabel label3 = new JLabel();
      this.$$$loadLabelText$$$(label3, this.$$$getMessageFromBundle$$$("messages/OsmorcBundle", "run.configuration.form.start.default"));
      panel2.add(label3, new GridConstraints(2, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL,
                                             GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0,
                                             false));
      myDefaultStartLevel = new JSpinner();
      panel2.add(myDefaultStartLevel, new GridConstraints(2, 1, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL,
                                                          GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW,
                                                          GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
      final Spacer spacer1 = new Spacer();
      panel2.add(spacer1, new GridConstraints(1, 2, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL,
                                              GridConstraints.SIZEPOLICY_WANT_GROW, 1, null, null, null, 0, false));
      myBundlesPanel = new JPanel();
      myBundlesPanel.setLayout(new BorderLayout(0, 0));
      panel1.add(myBundlesPanel, new GridConstraints(1, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH,
                                                     GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW,
                                                     GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null,
                                                     null, null, 0, false));
      myBundlesTable = new JBTable();
      myBundlesPanel.add(myBundlesTable, BorderLayout.CENTER);
      final JPanel panel3 = new JPanel();
      panel3.setLayout(new GridLayoutManager(7, 3, new Insets(0, 0, 0, 0), -1, -1));
      root.addTab(this.$$$getMessageFromBundle$$$("messages/OsmorcBundle", "run.configuration.form.tab.parameters"), panel3);
      final JLabel label4 = new JLabel();
      this.$$$loadLabelText$$$(label4, this.$$$getMessageFromBundle$$$("messages/OsmorcBundle", "run.configuration.form.vm"));
      panel3.add(label4,
                 new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED,
                                     GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
      myVmOptions = new RawCommandLineEditor();
      myVmOptions.setDialogCaption(this.$$$getMessageFromBundle$$$("messages/OsmorcBundle", "run.configuration.form.vm.title"));
      panel3.add(myVmOptions, new GridConstraints(0, 1, 1, 2, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL,
                                                  GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_WANT_GROW,
                                                  GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
      final JBLabel jBLabel1 = new JBLabel();
      this.$$$loadLabelText$$$(jBLabel1, this.$$$getMessageFromBundle$$$("messages/OsmorcBundle", "run.configuration.form.parameters"));
      panel3.add(jBLabel1,
                 new GridConstraints(1, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED,
                                     GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
      myProgramParameters = new RawCommandLineEditor();
      myProgramParameters.setDialogCaption(
        this.$$$getMessageFromBundle$$$("messages/OsmorcBundle", "run.configuration.form.parameters.title"));
      panel3.add(myProgramParameters, new GridConstraints(1, 1, 1, 2, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL,
                                                          GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_WANT_GROW,
                                                          GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
      final JBLabel jBLabel2 = new JBLabel();
      this.$$$loadLabelText$$$(jBLabel2, this.$$$getMessageFromBundle$$$("messages/OsmorcBundle", "run.configuration.form.directory"));
      panel3.add(jBLabel2,
                 new GridConstraints(2, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED,
                                     GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
      myOsmorcControlledDir = new JRadioButton();
      this.$$$loadButtonText$$$(myOsmorcControlledDir,
                                this.$$$getMessageFromBundle$$$("messages/OsmorcBundle", "run.configuration.form.directory.recreate"));
      panel3.add(myOsmorcControlledDir,
                 new GridConstraints(2, 1, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED,
                                     GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
      myUserDefinedDir = new JRadioButton();
      this.$$$loadButtonText$$$(myUserDefinedDir,
                                this.$$$getMessageFromBundle$$$("messages/OsmorcBundle", "run.configuration.form.directory.path"));
      panel3.add(myUserDefinedDir,
                 new GridConstraints(3, 1, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED,
                                     GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
      myWorkingDirField = new TextFieldWithBrowseButton();
      panel3.add(myWorkingDirField, new GridConstraints(3, 2, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL,
                                                        GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null,
                                                        null, 0, false));
      myJrePathEditor = new JrePathEditor();
      panel3.add(myJrePathEditor, new GridConstraints(4, 0, 1, 3, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL,
                                                      GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_WANT_GROW,
                                                      GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null,
                                                      null, null, 0, false));
      myClassPathAllBundles = new JCheckBox();
      this.$$$loadButtonText$$$(myClassPathAllBundles,
                                this.$$$getMessageFromBundle$$$("messages/OsmorcBundle", "run.configuration.form.classpath"));
      panel3.add(myClassPathAllBundles, new GridConstraints(5, 0, 1, 3, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE,
                                                            GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW,
                                                            GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
      final Spacer spacer2 = new Spacer();
      panel3.add(spacer2, new GridConstraints(6, 1, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_VERTICAL, 1,
                                              GridConstraints.SIZEPOLICY_WANT_GROW, null, null, null, 0, false));
      myAdditionalPropertiesPanel = new JPanel();
      myAdditionalPropertiesPanel.setLayout(new BorderLayout(0, 0));
      root.addTab(this.$$$getMessageFromBundle$$$("messages/OsmorcBundle", "run.configuration.form.tab.special"),
                  myAdditionalPropertiesPanel);
      label1.setLabelFor(myFrameworkInstances);
      label2.setLabelFor(myFrameworkStartLevel);
      label3.setLabelFor(myDefaultStartLevel);
      ButtonGroup buttonGroup;
      buttonGroup = new ButtonGroup();
      buttonGroup.add(myOsmorcControlledDir);
      buttonGroup.add(myUserDefinedDir);
    }
    myJrePathEditor.setDefaultJreSelector(DefaultJreSelector.projectSdk(project));

    myFrameworkStartLevel.setModel(new SpinnerNumberModel(0, 0, Integer.MAX_VALUE, 1));
    JSpinner.DefaultEditor editor = (JSpinner.DefaultEditor)myFrameworkStartLevel.getEditor();
    editor.getTextField().setFormatterFactory(new DefaultFormatterFactory(new JSpinnerCellEditor.MyNumberFormatter("Auto")));

    myDefaultStartLevel.setModel(new SpinnerNumberModel(1, 1, Integer.MAX_VALUE, 1));

    DefaultComboBoxModel<FrameworkInstanceDefinition> model = new DefaultComboBoxModel<>();
    model.addElement(null);
    for (FrameworkInstanceDefinition instance : ApplicationSettings.getInstance().getActiveFrameworkInstanceDefinitions()) {
      model.addElement(instance);
    }
    myFrameworkInstances.setModel(model);
    myFrameworkInstances.addActionListener((e) -> onFrameworkChange());
    myFrameworkInstances.setRenderer(new FrameworkInstanceRenderer('[' + message("framework.project.default") + ']'));

    myBundlesTable.setModel(new RunConfigurationTableModel());
    myBundlesTable.setRowSelectionAllowed(true);
    myBundlesTable.setColumnSelectionAllowed(false);
    myBundlesTable.setDefaultEditor(Integer.class, new JSpinnerCellEditor());
    myBundlesTable.setDefaultRenderer(Integer.class, new JSpinnerCellEditor());
    myBundlesTable.setAutoCreateRowSorter(true);
    myBundlesPanel.add(
      ToolbarDecorator.createDecorator(myBundlesTable)
        .setAddAction((b) -> onAddClick())
        .setRemoveAction((b) -> onRemoveClick())
        .disableUpDownActions()
        .createPanel(), BorderLayout.CENTER
    );
    myBundlesTable.addComponentListener(new ComponentAdapter() {
      @Override
      public void componentResized(ComponentEvent e) {
        int width = myBundlesTable.getWidth();
        if (width > 200) {
          myBundlesTable.getColumnModel().getColumn(0).setPreferredWidth(width - 200);
          myBundlesTable.getColumnModel().getColumn(1).setPreferredWidth(80);
          myBundlesTable.getColumnModel().getColumn(2).setPreferredWidth(120);
        }
      }
    });
    TableSpeedSearch.installOn(myBundlesTable).setComparator(new SpeedSearchComparator(false));

    myOsmorcControlledDir.addChangeListener((e) -> {
      boolean isUserDefined = !myOsmorcControlledDir.isSelected();
      myWorkingDirField.setEnabled(isUserDefined);
    });

    myWorkingDirField.addBrowseFolderListener(null, FileChooserDescriptorFactory.createSingleFolderDescriptor()
      .withTitle(message("run.configuration.working.dir.title"))
      .withDescription(message("run.configuration.working.dir.description")));
    myWorkingDirField.getTextField().setColumns(30);

    // avoid text fields growing the dialog when much text is entered.
    myVmOptions.getTextField().setPreferredSize(new Dimension(100, 20));
    myProgramParameters.getTextField().setPreferredSize(new Dimension(100, 20));
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
  public JComponent $$$getRootComponent$$$() { return root; }

  /**
   * Called when the framework is changed. This will create a new editor for framework properties and will also remove
   * any framework bundles from the list, as they are no longer in classpath.
   */
  private void onFrameworkChange() {
    if (myFrameworkInstances.getSelectedItem() != null) {
      FrameworkInstanceDefinition instance = (FrameworkInstanceDefinition)myFrameworkInstances.getSelectedItem();
      FrameworkIntegrator integrator = FrameworkIntegratorRegistry.getInstance().findIntegratorByInstanceDefinition(instance);
      assert integrator != null : instance;

      // clear the panel
      myAdditionalPropertiesPanel.removeAll();

      // create and install a new editor (if present)
      myCurrentRunPropertiesEditor = integrator.createRunPropertiesEditor();
      if (myCurrentRunPropertiesEditor != null) {
        myAdditionalPropertiesPanel.removeAll();
        myAdditionalPropertiesPanel.add(myCurrentRunPropertiesEditor.getUI(), BorderLayout.CENTER);
        if (myRunConfiguration != null) {
          myCurrentRunPropertiesEditor.resetEditorFrom(myRunConfiguration);
          OsgiRunConfigurationChecker checker = null;
          if (integrator instanceof OsgiRunConfigurationCheckerProvider) {
            checker = ((OsgiRunConfigurationCheckerProvider)integrator).getOsgiRunConfigurationChecker();
          }
          myRunConfiguration.setAdditionalChecker(checker);
        }
      }

      // remove all framework bundles from the list
      RunConfigurationTableModel model = getTableModel();
      model.removeAllOfType(SelectedBundle.BundleType.FrameworkBundle);
    }
  }

  private RunConfigurationTableModel getTableModel() {
    return (RunConfigurationTableModel)myBundlesTable.getModel();
  }

  private void onAddClick() {
    FrameworkInstanceDefinition instance = (FrameworkInstanceDefinition)myFrameworkInstances.getSelectedItem();
    BundleSelector selector = new BundleSelector(myProject, instance, getBundlesToRun());
    if (selector.showAndGet()) {
      RunConfigurationTableModel model = getTableModel();
      for (SelectedBundle aModule : selector.getSelectedBundles()) {
        model.addBundle(aModule);
      }
    }
  }

  private void onRemoveClick() {
    int[] indices = IntStream.of(myBundlesTable.getSelectedRows()).map(myBundlesTable::convertRowIndexToModel).sorted().toArray();
    RunConfigurationTableModel model = getTableModel();
    for (int i = indices.length - 1; i >= 0; i--) {
      model.removeBundleAt(indices[i]);
    }
  }

  @Override
  protected void resetEditorFrom(@NotNull OsgiRunConfiguration osgiRunConfiguration) {
    myRunConfiguration = osgiRunConfiguration;
    myVmOptions.setText(osgiRunConfiguration.getVmParameters());
    myProgramParameters.setText(osgiRunConfiguration.getProgramParameters());
    myFrameworkInstances.setSelectedItem(osgiRunConfiguration.getInstanceToUse());
    myClassPathAllBundles.setSelected(osgiRunConfiguration.isIncludeAllBundlesInClassPath());

    if (myCurrentRunPropertiesEditor != null) {
      myCurrentRunPropertiesEditor.resetEditorFrom(osgiRunConfiguration);
    }

    // I deliberately set the list of modules as the last step here as
    // the framework specific modules are cleaned out when you change the framework instance
    // so the framework instance should be changed first
    List<SelectedBundle> modules = osgiRunConfiguration.getBundlesToDeploy();
    RunConfigurationTableModel model = getTableModel();
    while (model.getRowCount() > 0) {
      model.removeBundleAt(0);
    }
    for (SelectedBundle module : modules) {
      model.addBundle(module);
    }
    myBundlesTable.getColumnModel().getColumn(1).setPreferredWidth(200);

    myFrameworkStartLevel.setValue(osgiRunConfiguration.getFrameworkStartLevel());
    myDefaultStartLevel.setValue(osgiRunConfiguration.getDefaultStartLevel());

    boolean useUserDefinedFields = !osgiRunConfiguration.isGenerateWorkingDir();
    myWorkingDirField.setText(osgiRunConfiguration.getWorkingDir());
    if (myWorkingDirField.getText().isEmpty()) {
      final CompilerProjectExtension extension = CompilerProjectExtension.getInstance(myProject);
      if (extension != null) {
        final String outputDirPointer = extension.getCompilerOutputUrl();
        if (outputDirPointer != null) {
          myWorkingDirField.setText(VfsUtilCore.urlToPath(outputDirPointer + "/run.osgi/"));
        }
      }
    }

    myWorkingDirField.setEnabled(useUserDefinedFields);
    myUserDefinedDir.setSelected(useUserDefinedFields);
    myOsmorcControlledDir.setSelected(!useUserDefinedFields);
    myJrePathEditor.setPathOrName(osgiRunConfiguration.getAlternativeJrePath(), osgiRunConfiguration.isUseAlternativeJre());
  }

  @Override
  protected void applyEditorTo(@NotNull OsgiRunConfiguration osgiRunConfiguration) throws ConfigurationException {
    osgiRunConfiguration.setBundlesToDeploy(getBundlesToRun());
    osgiRunConfiguration.setVmParameters(myVmOptions.getText());
    osgiRunConfiguration.setProgramParameters(myProgramParameters.getText());
    osgiRunConfiguration.setIncludeAllBundlesInClassPath(myClassPathAllBundles.isSelected());
    osgiRunConfiguration.setWorkingDir(myWorkingDirField.getText().replace('\\', '/'));
    osgiRunConfiguration.setUseAlternativeJre(myJrePathEditor.isAlternativeJreSelected());
    osgiRunConfiguration.setAlternativeJrePath(myJrePathEditor.getJrePathOrName());
    osgiRunConfiguration.setFrameworkStartLevel((Integer)myFrameworkStartLevel.getValue());
    osgiRunConfiguration.setDefaultStartLevel((Integer)myDefaultStartLevel.getValue());
    osgiRunConfiguration.setGenerateWorkingDir(myOsmorcControlledDir.isSelected());
    osgiRunConfiguration.setInstanceToUse((FrameworkInstanceDefinition)myFrameworkInstances.getSelectedItem());

    if (myCurrentRunPropertiesEditor != null) {
      myCurrentRunPropertiesEditor.applyEditorTo(osgiRunConfiguration);
    }
  }

  private List<SelectedBundle> getBundlesToRun() {
    return getTableModel().getBundles();
  }

  @Override
  protected @NotNull JComponent createEditor() {
    return root;
  }

  private static class RunConfigurationTableModel extends AbstractTableModel {
    private final List<SelectedBundle> mySelectedBundles;

    RunConfigurationTableModel() {
      mySelectedBundles = new ArrayList<>();
    }

    @Override
    public Class<?> getColumnClass(int columnIndex) {
      return switch (columnIndex) {
        case 0 -> String.class;
        case 1 -> Integer.class;
        case 2 -> Boolean.class;
        default -> Object.class;
      };
    }

    @Override
    public String getColumnName(int columnIndex) {
      return switch (columnIndex) {
        case 0 -> message("run.configuration.col.bundle");
        case 1 -> message("run.configuration.col.level");
        case 2 -> message("run.configuration.col.start");
        default -> "";
      };
    }

    @Override
    public int getColumnCount() {
      return 3;
    }

    @Override
    public int getRowCount() {
      return mySelectedBundles.size();
    }

    @Override
    public boolean isCellEditable(int row, int column) {
      return column != 0;
    }

    @Override
    public Object getValueAt(int row, int column) {
      SelectedBundle bundle = getBundleAt(row);
      return switch (column) {
        case 0 -> bundle.toString();
        case 1 -> bundle.getStartLevel();
        case 2 -> bundle.isStartAfterInstallation();
        default -> throw new RuntimeException("Don't know column " + column);
      };
    }

    @Override
    public void setValueAt(Object o, int row, int column) {
      SelectedBundle bundle = getBundleAt(row);
      switch (column) {
        case 1 -> bundle.setStartLevel((Integer)o);
        case 2 -> bundle.setStartAfterInstallation((Boolean)o);
        default -> throw new RuntimeException("Cannot edit column " + column);
      }
    }

    public SelectedBundle getBundleAt(int index) {
      return mySelectedBundles.get(index);
    }

    public List<SelectedBundle> getBundles() {
      return mySelectedBundles;
    }

    public void addBundle(SelectedBundle bundle) {
      mySelectedBundles.add(bundle);
      fireTableRowsInserted(mySelectedBundles.size() - 1, mySelectedBundles.size() - 1);
    }

    public void removeBundleAt(int index) {
      mySelectedBundles.remove(index);
      fireTableRowsDeleted(index, index);
    }

    public void removeAllOfType(SelectedBundle.BundleType type) {
      for (Iterator<SelectedBundle> selectedBundleIterator = mySelectedBundles.iterator(); selectedBundleIterator.hasNext(); ) {
        SelectedBundle selectedBundle = selectedBundleIterator.next();
        if (selectedBundle.getBundleType() == type) {
          selectedBundleIterator.remove();
        }
      }
      fireTableDataChanged();
    }
  }
}
