// Copyright 2000-2024 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package com.intellij.lang.javascript.refactoring.extractMethod;

import com.intellij.lang.javascript.psi.JSExpression;
import com.intellij.lang.javascript.psi.JSNamedElement;
import com.intellij.lang.javascript.psi.JSType;
import com.intellij.lang.javascript.psi.JSVariable;
import com.intellij.lang.javascript.psi.ecmal4.JSAttributeList;
import com.intellij.lang.javascript.psi.types.JSContext;
import com.intellij.lang.javascript.refactoring.JSNamesValidation;
import com.intellij.lang.javascript.refactoring.changeSignature.JSChangeSignatureDialog;
import com.intellij.lang.javascript.refactoring.introduce.BasicIntroducedEntityInfoProvider;
import com.intellij.lang.javascript.refactoring.introduce.IntroducedEntityInfoProvider;
import com.intellij.lang.javascript.refactoring.introduce.JSBaseClassBasedIntroduceDialog;
import com.intellij.lang.javascript.refactoring.ui.JSVisibilityPanel;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.refactoring.ui.MethodSignatureComponent;
import com.intellij.refactoring.ui.NameSuggestionsField;
import com.intellij.ui.AnActionButton;
import com.intellij.ui.AnActionButtonRunnable;
import com.intellij.ui.EditorComboBox;
import com.intellij.ui.IdeBorderFactory;
import com.intellij.ui.ToolbarDecorator;
import com.intellij.ui.table.JBTable;
import com.intellij.uiDesigner.core.GridConstraints;
import com.intellij.uiDesigner.core.GridLayoutManager;
import com.intellij.util.ui.JBUI;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.AbstractButton;
import javax.swing.Action;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTable;
import javax.swing.border.TitledBorder;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.DefaultTableColumnModel;
import javax.swing.table.TableColumn;
import java.awt.Dimension;
import java.awt.Insets;
import java.lang.reflect.Method;
import java.util.ResourceBundle;

/**
 * @author Maxim.Mossienko
 */
public class ActionScriptExtractFunctionDialog extends JSBaseClassBasedIntroduceDialog<IntroducedEntityInfoProvider>
  implements JSExtractFunctionSettings {

  private final JPanel myPanel;
  private final NameSuggestionsField myFunctionName;
  private final JSVisibilityPanel myVisibilityPanel;
  private final EditorComboBox myVarType;
  private final MethodSignatureComponent myPreviewText;
  private JTable parametersTable;
  private final JPanel myVarTypePanel;
  private final JCheckBox myDeclareStaticCheckBox;
  private final JLabel myNameLabel;
  @SuppressWarnings("unused") private final JPanel myToolbarDecoratorPanel;
  private final JCheckBox myReplaceAllOccurrencesCheckBox;
  private ParametersInfo parametersInfo;
  private final ExtractedFunctionSignatureGenerator mySignatureGenerator;
  private final JSExtractFunctionHandler.ContextInfo contextInfo;
  private final JSExtractFunctionHandler.IntroductionScope myIntroductionScope;

  public ActionScriptExtractFunctionDialog(final @NotNull ExtractedFunctionSignatureGenerator signatureGenerator,
                                           @NotNull JSExtractFunctionHandler.ContextInfo ci,
                                           @NotNull JSExtractFunctionHandler.IntroductionScope introductionScope,
                                           JSExpression @NotNull [] occurrences) {
    super(
      ci.file.getProject(),
      new IntroducedEntityInfoProvider() {
        @Override
        public int getOccurrenceCount() {
          return occurrences.length;
        }

        @Override
        public @Nullable String evaluateType() {
          return null;
        }

        @Override
        public PsiFile getContainingFile() {
          return ci.file;
        }

        @Override
        public String[] suggestCandidateNames() {
          return JSExtractFunctionHandler.suggestNames(ci.myCodeFragment);
        }

        @Override
        public boolean checkConflicts(@NotNull String name) {
          return true;
        }

        @Override
        public PsiElement findNamedElementInScope(@NotNull String name, PsiElement place) {
          return null;
        }
      },
      "javascript.extract.method.title"
    );
    contextInfo = ci;
    myIntroductionScope = introductionScope;
    mySignatureGenerator = signatureGenerator;
    {
      myVisibilityPanel = new JSVisibilityPanel();
      myFunctionName = configureNameField();
      myVarType = configureTypeField();
      myPreviewText = new MethodSignatureComponent(mySignatureGenerator.fun(
        new DefaultJSExtractFunctionSettings(JSExtractFunctionHandler.DEFAULT_EXTRACTED_NAME, myIntroductionScope),
        contextInfo), myProject, JSChangeSignatureDialog.getFileTypeFromContext(contextInfo.getContextElement()));
      myPreviewText.setMinimumSize(JBUI.size(300, 40));

      parametersTable = new JBTable();
      setParametersInfo(JSExtractFunctionHandler.createDefaultParametersInfo(getIntroductionScope()));
      parametersTable.setAutoCreateColumnsFromModel(false);
      parametersTable.setModel(new AbstractTableModel() {
        @Override
        public int getRowCount() {
          return parametersInfo.variableOptions.size();
        }

        @Override
        public int getColumnCount() {
          return 2 + (isEcma4Context() ||
                      isTypeScriptContext() ? 1 : 0);
        }

        @Override
        public Object getValueAt(int rowIndex, int columnIndex) {
          JSNamedElement var = parametersInfo.variables.get(rowIndex);
          if (columnIndex == 0) return Boolean.valueOf(parametersInfo.variableOptions.get(var).used);
          if (columnIndex == 1) return parametersInfo.variableOptions.get(var).name;
          if (columnIndex == 2) return parametersInfo.variableOptions.get(var).type;

          return null;
        }

        @Override
        public boolean isCellEditable(int rowIndex, int columnIndex) {
          return true;
        }

        @Override
        public void setValueAt(Object aValue, int rowIndex, int columnIndex) {
          JSNamedElement var = parametersInfo.variables.get(rowIndex);
          ParameterInfo parameterInfo = parametersInfo.variableOptions.get(var);
          ParameterInfo newParameterInfo = null;

          if (columnIndex == 0) {
            newParameterInfo = new ParameterInfo(parameterInfo.name, (Boolean)aValue, parameterInfo.type, rowIndex);
          }
          else if (columnIndex == 1) {
            newParameterInfo = new ParameterInfo((String)aValue, parameterInfo.used, parameterInfo.type, rowIndex);
          }
          else if (columnIndex == 2) {
            newParameterInfo = new ParameterInfo(parameterInfo.name, parameterInfo.used, (String)aValue, rowIndex);
          }

          if (newParameterInfo != null) {
            parametersInfo.variableOptions.put(var, newParameterInfo);
          }
          initiateValidation();
        }

        @Override
        public Class<?> getColumnClass(int columnIndex) {
          if (columnIndex == 0) return Boolean.class;
          if (columnIndex == 1) return String.class;
          if (columnIndex == 2) return String.class;
          return super.getColumnClass(columnIndex);
        }
      });

      DefaultTableColumnModel tableColumnModel = new DefaultTableColumnModel();
      final TableColumn checkboxColumn = new TableColumn(0);
      checkboxColumn.setMaxWidth(60);
      tableColumnModel.addColumn(checkboxColumn);
      tableColumnModel.addColumn(new TableColumn(1));
      if (isTypeScriptContext() || isEcma4Context()) tableColumnModel.addColumn(new TableColumn(2));
      parametersTable.setColumnModel(tableColumnModel);
      parametersTable.setRowSelectionAllowed(false);
      parametersTable.getSelectionModel().setSelectionInterval(0, 0);

      myToolbarDecoratorPanel = ToolbarDecorator.createDecorator(parametersTable)
        .setMoveUpAction(new AnActionButtonRunnable() {
          @Override
          public void run(AnActionButton button) {
            int selectedRow = parametersTable.getSelectedRow();
            if (selectedRow - 1 >= 0) {
              changeRowNumber(selectedRow, selectedRow - 1);
              changeRowNumber(selectedRow - 1, selectedRow);
              swapVars(selectedRow, selectedRow - 1);
              ((AbstractTableModel)parametersTable.getModel()).fireTableRowsUpdated(selectedRow - 1, selectedRow);
              parametersTable.getSelectionModel().setSelectionInterval(selectedRow - 1, selectedRow - 1);
              initiateValidation();
            }
          }
        }).setMoveDownAction(new AnActionButtonRunnable() {
          @Override
          public void run(AnActionButton button) {
            int selectedRow = parametersTable.getSelectedRow();
            if (selectedRow + 1 < parametersTable.getRowCount()) {
              changeRowNumber(selectedRow, selectedRow + 1);
              changeRowNumber(selectedRow + 1, selectedRow);
              swapVars(selectedRow, selectedRow + 1);
              ((AbstractTableModel)parametersTable.getModel()).fireTableRowsUpdated(selectedRow, selectedRow + 1);
              parametersTable.getSelectionModel().setSelectionInterval(selectedRow + 1, selectedRow + 1);
              initiateValidation();
            }
          }
        }).createPanel();
    }
    {
      // GUI initializer generated by IntelliJ IDEA GUI Designer
      // >>> IMPORTANT!! <<<
      // DO NOT EDIT OR ADD ANY CODE HERE!
      myPanel = new JPanel();
      myPanel.setLayout(new GridLayoutManager(3, 2, new Insets(0, 0, 0, 0), -1, -1));
      final JPanel panel1 = new JPanel();
      panel1.setLayout(new GridLayoutManager(1, 1, new Insets(0, 0, 0, 0), -1, -1));
      panel1.putClientProperty("BorderFactoryClass", "com.intellij.ui.IdeBorderFactory$PlainSmallWithoutIndent");
      myPanel.add(panel1, new GridConstraints(2, 0, 1, 2, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH,
                                              GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_WANT_GROW,
                                              GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null,
                                              0, false));
      panel1.setBorder(IdeBorderFactory.PlainSmallWithoutIndent.createTitledBorder(null, this.$$$getMessageFromBundle$$$(
                                                                                     "messages/JavaScriptBundle", "extract.function.signature.preview"), TitledBorder.DEFAULT_JUSTIFICATION,
                                                                                   TitledBorder.DEFAULT_POSITION, null, null));
      panel1.add(myPreviewText, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH,
                                                    GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_WANT_GROW, null, null,
                                                    null, 0, false));
      final JPanel panel2 = new JPanel();
      panel2.setLayout(new GridLayoutManager(1, 1, new Insets(0, 0, 0, 0), -1, -1));
      panel2.putClientProperty("BorderFactoryClass", "com.intellij.ui.IdeBorderFactory$PlainSmallWithoutIndent");
      myPanel.add(panel2, new GridConstraints(1, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH,
                                              GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW,
                                              GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null,
                                              0, false));
      panel2.setBorder(IdeBorderFactory.PlainSmallWithoutIndent.createTitledBorder(null, this.$$$getMessageFromBundle$$$(
                                                                                     "messages/JavaScriptBundle", "extract.function.parameters"), TitledBorder.DEFAULT_JUSTIFICATION, TitledBorder.DEFAULT_POSITION,
                                                                                   null, null));
      panel2.add(myToolbarDecoratorPanel, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH,
                                                              GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW,
                                                              GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW,
                                                              null, null, null, 0, false));
      final JPanel panel3 = new JPanel();
      panel3.setLayout(new GridLayoutManager(4, 2, new Insets(0, 0, 0, 0), -1, -1));
      panel3.putClientProperty("BorderFactoryClass", "com.intellij.ui.IdeBorderFactory$PlainSmallWithoutIndent");
      myPanel.add(panel3, new GridConstraints(0, 0, 1, 2, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH,
                                              GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW,
                                              GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null,
                                              0, false));
      panel3.setBorder(IdeBorderFactory.PlainSmallWithoutIndent.createTitledBorder(null, this.$$$getMessageFromBundle$$$(
                                                                                     "messages/JavaScriptBundle", "extract.function.function"), TitledBorder.DEFAULT_JUSTIFICATION, TitledBorder.DEFAULT_POSITION, null,
                                                                                   null));
      myNameLabel = new JLabel();
      this.$$$loadLabelText$$$(myNameLabel, this.$$$getMessageFromBundle$$$("messages/JavaScriptBundle", "extract.function.name"));
      panel3.add(myNameLabel,
                 new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED,
                                     GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
      panel3.add(myFunctionName, new GridConstraints(0, 1, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL,
                                                     GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW,
                                                     GridConstraints.SIZEPOLICY_FIXED, null, new Dimension(150, -1), null, 0, false));
      myDeclareStaticCheckBox = new JCheckBox();
      myDeclareStaticCheckBox.setFocusable(false);
      this.$$$loadButtonText$$$(myDeclareStaticCheckBox,
                                this.$$$getMessageFromBundle$$$("messages/JavaScriptBundle", "extract.method.declare.static"));
      panel3.add(myDeclareStaticCheckBox, new GridConstraints(1, 0, 1, 2, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE,
                                                              GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW,
                                                              GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
      myVarTypePanel = new JPanel();
      myVarTypePanel.setLayout(new GridLayoutManager(1, 2, new Insets(0, 0, 0, 0), -1, -1));
      panel3.add(myVarTypePanel, new GridConstraints(3, 0, 1, 2, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH,
                                                     GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW,
                                                     GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null,
                                                     null, null, 0, false));
      myVarTypePanel.add(myVarType, new GridConstraints(0, 1, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL,
                                                        GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_FIXED, null,
                                                        new Dimension(150, -1), null, 0, false));
      final JLabel label1 = new JLabel();
      this.$$$loadLabelText$$$(label1, this.$$$getMessageFromBundle$$$("messages/JavaScriptBundle", "extract.function.return.type"));
      myVarTypePanel.add(label1, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE,
                                                     GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null,
                                                     0, false));
      myReplaceAllOccurrencesCheckBox = new JCheckBox();
      this.$$$loadButtonText$$$(myReplaceAllOccurrencesCheckBox, this.$$$getMessageFromBundle$$$("messages/JavaScriptBundle",
                                                                                                 "javascript.introduce.variable.replace.all.occurrences"));
      panel3.add(myReplaceAllOccurrencesCheckBox, new GridConstraints(2, 0, 1, 2, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE,
                                                                      GridConstraints.SIZEPOLICY_CAN_SHRINK |
                                                                      GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED,
                                                                      null, null, null, 0, false));
      myPanel.add(myVisibilityPanel, new GridConstraints(1, 1, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH,
                                                         GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW,
                                                         GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null,
                                                         null, null, 0, false));
      label1.setLabelFor(myVarType);
    }

    ChangeListener listener = new ChangeListener() {
      @Override
      public void stateChanged(ChangeEvent e) {
        initiateValidation();
      }
    };

    myVisibilityPanel.addListener(listener);

    myDeclareStaticCheckBox.addChangeListener(listener);
    myDeclareStaticCheckBox.setFocusable(true);

    boolean isClassContext = getIntroductionScope().isClassContext();
    myVisibilityPanel.setVisible(isClassContext);
    myVarTypePanel.setVisible(false);

    myDeclareStaticCheckBox.setVisible(isClassContext);
    if (isClassContext) {
      myDeclareStaticCheckBox.setEnabled(contextInfo.getJSContext() == JSContext.UNKNOWN
                                         && JSExtractFunctionHandler.possibleToExtractStaticFromInstance(ci, myIntroductionScope));
      myDeclareStaticCheckBox.setSelected(
        ActionScriptExtractFunctionHandler.getDeclareStatic() || contextInfo.getJSContext() == JSContext.STATIC);
    }
    myVisibilityPanel.configureForClassMember(false, false, ci.holder);
    doInit();
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
  public JComponent $$$getRootComponent$$$() { return myPanel; }

  private void swapVars(int selectedRow, int anotherRow) {
    JSNamedElement var = parametersInfo.variables.get(selectedRow);
    parametersInfo.variables.set(selectedRow, parametersInfo.variables.get(anotherRow));
    parametersInfo.variables.set(anotherRow, var);
  }

  private void changeRowNumber(int selectedRow, int newValue) {
    JSNamedElement currentVar = parametersInfo.variables.get(selectedRow);
    ParameterInfo currentVarParametersInfo = parametersInfo.variableOptions.get(currentVar);
    parametersInfo.variableOptions.put(
      currentVar,
      new ParameterInfo(currentVarParametersInfo.name, currentVarParametersInfo.used, currentVarParametersInfo.type, newValue)
    );
  }

  @Override
  protected void checkIsValid() {
    super.checkIsValid();
    myPreviewText.setSignature(mySignatureGenerator.fun(this, contextInfo));
    checkUniqueness(getIntroductionScope().parent);

    Action action = getOKAction();
    boolean nameValidationStatus = true;

    for (JSNamedElement var : parametersInfo.variables) {
      final ParameterInfo parameterInfo = parametersInfo.variableOptions.get(var);
      if (parameterInfo == null) continue;
      if (!JSNamesValidation.isIdentifier(parameterInfo.name, contextInfo.getContextElement())) {
        nameValidationStatus = false;
        break;
      }
    }

    action.setEnabled(action.isEnabled() && nameValidationStatus);
  }

  @Override
  protected String getDimensionServiceKey() {
    return getClass().getName();
  }

  @Override
  public JComboBox getVarTypeField() {
    return myVarType;
  }

  @Override
  public String getMethodName() {
    return getVariableName();
  }

  @Override
  public @Nullable ParametersInfo getParametersInfo() {
    return parametersInfo;
  }

  private void setParametersInfo(ParametersInfo parametersInfo) {
    this.parametersInfo = parametersInfo;
    int i = 0;
    for (JSNamedElement var : parametersInfo.variables) {
      if (JSExtractFunctionHandler.isArgumentsReference(var)) continue;
      String string = getTypeTextForVariable(var);
      parametersInfo.variableOptions.put(var, new ParameterInfo(var.getName(), true, string, i++));
    }
    ((AbstractTableModel)parametersTable.getModel()).fireTableDataChanged();
  }

  private static String getTypeTextForVariable(JSNamedElement var) {
    JSType type = var instanceof JSVariable ? ((JSVariable)var).getJSType() : null;
    return type == null ? null : type.getTypeText(JSType.TypeTextFormat.CODE);
  }

  @Override
  protected NameSuggestionsField getNameField() {
    return myFunctionName;
  }

  @Override
  protected JLabel getNameLabel() {
    return myNameLabel;
  }

  @Override
  protected JPanel getPanel() {
    return myPanel;
  }

  @Override
  protected JCheckBox getReplaceAllCheckBox() {
    return myReplaceAllOccurrencesCheckBox;
  }

  @Override
  public JSVisibilityPanel getVisibilityPanel() {
    return myVisibilityPanel;
  }

  @Override
  public boolean makeStatic() {
    return myDeclareStaticCheckBox.isSelected();
  }

  @Override
  public boolean isDeclarationTypeConfigurable() {
    return false;
  }

  @Override
  public @NotNull FunctionDeclarationType getDeclarationType() {
    return FunctionDeclarationType.FUNCTION;
  }

  @Override
  public JSExtractFunctionHandler.IntroductionScope getIntroductionScope() {
    return myIntroductionScope;
  }

  public boolean isEcma4Context() {
    return contextInfo.ecmaL4;
  }

  public boolean isTypeScriptContext() {
    return contextInfo.holder != null && contextInfo.holder.isTypeScript;
  }

  @Override
  protected String getHelpId() {
    if (isEcma4Context()) {
      return "refactoring.extractMethod.ActionScript";
    }
    else if (isTypeScriptContext()) {
      return "typescript_extract_method";
    }
    return "refactoring.extractMethod.JavaScript";
  }

  @Override
  protected void doOKAction() {
    PsiElement conflict = JSExtractFunctionHandler.findConflictingElementInScope(this.getMethodName(),
                                                                                 this.myIntroductionScope.parent,
                                                                                 this.makeStatic(), null);
    if (conflict != null && !BasicIntroducedEntityInfoProvider.showMemberAlreadyExists(conflict)) {
      return;
    }
    if (myDeclareStaticCheckBox.isEnabled() && myDeclareStaticCheckBox.isVisible()) {
      ActionScriptExtractFunctionHandler.saveDeclareStatic(myDeclareStaticCheckBox.isSelected());
    }
    super.doOKAction();
  }

  @Override
  protected boolean isValidName(String name, PsiElement context) {
    if (myIntroductionScope.isClassContext()) {
      return JSNamesValidation.isClassMemberName(name, context);
    }
    return super.isValidName(name, context);
  }

  @Override
  protected void saveLastUsedVisibility(@NotNull JSAttributeList.AccessType type) {
    ActionScriptExtractFunctionHandler.saveClassMemberVisibility(type);
  }

  @Override
  protected @Nullable JSAttributeList.AccessType getLastUsedVisibility() {
    return ActionScriptExtractFunctionHandler.getClassMemberVisibility();
  }
}
