// Copyright 2000-2024 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package com.intellij.javascript.flex.refactoring.moveClass;

import com.intellij.ide.util.PlatformPackageUtil;
import com.intellij.lang.LanguageNamesValidation;
import com.intellij.lang.javascript.JavaScriptSupportLoader;
import com.intellij.lang.javascript.flex.FlexBundle;
import com.intellij.lang.javascript.presentable.Capitalization;
import com.intellij.lang.javascript.presentable.JSNamedElementPresenter;
import com.intellij.lang.javascript.psi.ecmal4.JSQualifiedNamedElement;
import com.intellij.lang.javascript.psi.resolve.ActionScriptResolveUtil;
import com.intellij.lang.javascript.psi.resolve.JSResolveUtil;
import com.intellij.lang.javascript.refactoring.ui.JSReferenceEditor;
import com.intellij.lang.javascript.refactoring.util.JSRefactoringUtil;
import com.intellij.lang.javascript.ui.ActionScriptPackageChooserDialog;
import com.intellij.lang.refactoring.NamesValidator;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.editor.event.DocumentListener;
import com.intellij.openapi.module.ModuleUtilCore;
import com.intellij.openapi.options.ConfigurationException;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.PackageIndex;
import com.intellij.openapi.roots.ProjectRootManager;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.psi.PsiDirectory;
import com.intellij.psi.PsiDirectoryContainer;
import com.intellij.psi.PsiElement;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.refactoring.BaseRefactoringProcessor;
import com.intellij.refactoring.RefactoringBundle;
import com.intellij.refactoring.move.MoveCallback;
import com.intellij.refactoring.move.moveFilesOrDirectories.MoveFilesOrDirectoriesUtil;
import com.intellij.refactoring.ui.RefactoringDialog;
import com.intellij.refactoring.util.CommonRefactoringUtil;
import com.intellij.ui.DocumentAdapter;
import com.intellij.ui.NonFocusableCheckBox;
import com.intellij.uiDesigner.core.GridConstraints;
import com.intellij.uiDesigner.core.GridLayoutManager;
import com.intellij.uiDesigner.core.Spacer;
import com.intellij.util.IncorrectOperationException;
import com.intellij.util.ThreeState;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.AbstractButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.event.DocumentEvent;
import java.awt.Dimension;
import java.awt.Insets;
import java.lang.reflect.Method;
import java.util.Collection;
import java.util.ResourceBundle;

public class FlexMoveClassDialog extends RefactoringDialog {

  private static final Logger LOG = Logger.getInstance(FlexMoveClassDialog.class.getName());

  private final JLabel myElementLabel;
  private final JSReferenceEditor myTargetPackageField;
  private final JPanel myContentPane;
  private final NonFocusableCheckBox myCbSearchInComments;
  private final NonFocusableCheckBox myCbSearchTextOccurences;
  private final NonFocusableCheckBox myCbMoveToAnotherSourceFolder;
  private final JLabel myPackageLabel;
  private final JTextField myClassNameField;
  private final JLabel myClassNameLabel;
  private final Collection<JSQualifiedNamedElement> myElements;
  private final @Nullable PsiElement myTargetContainer;
  private final @Nullable MoveCallback myCallback;
  private final boolean myFileLocal;

  public FlexMoveClassDialog(Project project,
                             Collection<JSQualifiedNamedElement> elements,
                             @Nullable PsiElement targetContainer,
                             @Nullable MoveCallback callback) {
    super(project, true);
    myElements = elements;
    myTargetContainer = targetContainer;
    myCallback = callback;
    {
      String initialPackage;
      if (myTargetContainer instanceof PsiDirectoryContainer) {
        initialPackage = PackageIndex.getInstance(myProject)
          .getPackageNameByDirectory(((PsiDirectoryContainer)myTargetContainer).getDirectories()[0].getVirtualFile());
      }
      else if (myTargetContainer instanceof PsiDirectory) {
        initialPackage = PackageIndex.getInstance(myProject)
          .getPackageNameByDirectory(((PsiDirectory)myTargetContainer).getVirtualFile());
      }
      else {
        if (myFileLocal) {
          initialPackage = StringUtil.getPackageName(myElements.iterator().next().getQualifiedName());
        }
        else {
          initialPackage =
            JSResolveUtil.getExpectedPackageNameFromFile(myElements.iterator().next().getContainingFile().getVirtualFile(), myProject);
        }
      }
      myTargetPackageField =
        ActionScriptPackageChooserDialog.createPackageReferenceEditor(initialPackage, myProject,
                                                                      FlexMoveClassDialog.class.getName() + ".target_package",
                                                                      GlobalSearchScope.projectScope(myProject),
                                                                      RefactoringBundle.message("choose.destination.package"));
    }
    {
      // GUI initializer generated by IntelliJ IDEA GUI Designer
      // >>> IMPORTANT!! <<<
      // DO NOT EDIT OR ADD ANY CODE HERE!
      myContentPane = new JPanel();
      myContentPane.setLayout(new GridLayoutManager(5, 2, new Insets(0, 0, 0, 0), -1, -1));
      myElementLabel = new JLabel();
      myElementLabel.setText("Move class 'Foo'");
      myContentPane.add(myElementLabel, new GridConstraints(0, 0, 1, 2, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE,
                                                            GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null,
                                                            null, 0, false));
      myPackageLabel = new JLabel();
      myPackageLabel.setText("To package:");
      myPackageLabel.setDisplayedMnemonic('G');
      myPackageLabel.setDisplayedMnemonicIndex(8);
      myContentPane.add(myPackageLabel, new GridConstraints(2, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE,
                                                            GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null,
                                                            null, 0, false));
      myContentPane.add(myTargetPackageField,
                        new GridConstraints(2, 1, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL,
                                            GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW,
                                            GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null,
                                            0, false));
      final Spacer spacer1 = new Spacer();
      myContentPane.add(spacer1, new GridConstraints(4, 1, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL,
                                                     GridConstraints.SIZEPOLICY_WANT_GROW, 1, null, null, null, 0, false));
      final Spacer spacer2 = new Spacer();
      myContentPane.add(spacer2, new GridConstraints(4, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_VERTICAL, 1,
                                                     GridConstraints.SIZEPOLICY_WANT_GROW, null, null, null, 0, false));
      final JPanel panel1 = new JPanel();
      panel1.setLayout(new GridLayoutManager(2, 2, new Insets(0, 0, 0, 0), -1, -1));
      myContentPane.add(panel1, new GridConstraints(3, 0, 1, 2, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH,
                                                    GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW,
                                                    GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null,
                                                    null, 0, false));
      myCbSearchInComments = new NonFocusableCheckBox();
      myCbSearchInComments.setMargin(new Insets(2, 0, 2, 3));
      myCbSearchInComments.setSelected(true);
      this.$$$loadButtonText$$$(myCbSearchInComments,
                                this.$$$getMessageFromBundle$$$("messages/RefactoringBundle", "search.in.comments.and.strings"));
      panel1.add(myCbSearchInComments, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_BOTH,
                                                           GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW,
                                                           GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW,
                                                           null, null, null, 0, false));
      myCbSearchTextOccurences = new NonFocusableCheckBox();
      myCbSearchTextOccurences.setSelected(true);
      this.$$$loadButtonText$$$(myCbSearchTextOccurences,
                                this.$$$getMessageFromBundle$$$("messages/RefactoringBundle", "search.for.text.occurrences"));
      panel1.add(myCbSearchTextOccurences, new GridConstraints(0, 1, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_BOTH,
                                                               GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW,
                                                               GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW,
                                                               null, null, null, 0, false));
      myCbMoveToAnotherSourceFolder = new NonFocusableCheckBox();
      myCbMoveToAnotherSourceFolder.setMargin(new Insets(2, 0, 2, 3));
      myCbMoveToAnotherSourceFolder.setSelected(true);
      this.$$$loadButtonText$$$(myCbMoveToAnotherSourceFolder, this.$$$getMessageFromBundle$$$("messages/RefactoringBundle",
                                                                                               "move.classes.move.to.another.source.folder"));
      panel1.add(myCbMoveToAnotherSourceFolder, new GridConstraints(1, 0, 1, 2, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_BOTH,
                                                                    GridConstraints.SIZEPOLICY_CAN_SHRINK |
                                                                    GridConstraints.SIZEPOLICY_CAN_GROW,
                                                                    GridConstraints.SIZEPOLICY_CAN_SHRINK |
                                                                    GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
      myClassNameLabel = new JLabel();
      myClassNameLabel.setText("Class name:");
      myClassNameLabel.setDisplayedMnemonic('N');
      myClassNameLabel.setDisplayedMnemonicIndex(6);
      myContentPane.add(myClassNameLabel, new GridConstraints(1, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE,
                                                              GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null,
                                                              null, null, 0, false));
      myClassNameField = new JTextField();
      myContentPane.add(myClassNameField, new GridConstraints(1, 1, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL,
                                                              GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_FIXED, null,
                                                              new Dimension(150, -1), null, 0, false));
      myClassNameLabel.setLabelFor(myClassNameField);
    }

    final JSQualifiedNamedElement firstElement = myElements.iterator().next();
    myFileLocal = ActionScriptResolveUtil.isFileLocalSymbol(firstElement);

    setSize(500, 130);

    String labelText;
    if (myFileLocal) {
      LOG.assertTrue(myElements.size() == 1);
      myClassNameLabel.setVisible(true);
      myClassNameLabel.setText(
        FlexBundle.message("element.name", new JSNamedElementPresenter(firstElement, Capitalization.UpperCase).describeElementKind()));
      myClassNameField.setVisible(true);
      myClassNameField.setText(myElements.iterator().next().getName());
      myClassNameField.selectAll();
      myPackageLabel.setText(FlexBundle.message("package.name.title"));
      setTitle(RefactoringBundle.message("move.inner.to.upper.level.title"));
    }
    else {
      myClassNameLabel.setVisible(false);
      myClassNameField.setVisible(false);
      myPackageLabel.setText(FlexBundle.message("to.package.title"));
      setTitle(RefactoringBundle.message("move.title"));
    }
    myElementLabel.setLabelFor(myTargetPackageField.getChildComponent());

    if (elements.size() == 1) {
      labelText = FlexBundle.message(myFileLocal ? "move.file.local.0" : "move.0",
                                     new JSNamedElementPresenter(firstElement).describeElementKind(),
                                     firstElement.getQualifiedName());
    }
    else {
      labelText = FlexBundle.message("move.elements");
    }
    myElementLabel.setText(labelText);
    myPackageLabel.setLabelFor(myTargetPackageField.getChildComponent());
    init();

    boolean canMoveToDifferentSourceRoot = !myFileLocal && ProjectRootManager.getInstance(myProject).getContentSourceRoots().length > 1;
    if (canMoveToDifferentSourceRoot) {
      myCbMoveToAnotherSourceFolder.setEnabled(true);
    }
    else {
      myCbMoveToAnotherSourceFolder.setEnabled(false);
      myCbMoveToAnotherSourceFolder.setSelected(false);
    }

    myClassNameField.getDocument().addDocumentListener(new DocumentAdapter() {
      @Override
      protected void textChanged(@NotNull DocumentEvent e) {
        validateButtons();
      }
    });

    myTargetPackageField.addDocumentListener(new DocumentListener() {
      @Override
      public void documentChanged(@NotNull com.intellij.openapi.editor.event.DocumentEvent e) {
        validateButtons();
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
  public JComponent $$$getRootComponent$$$() { return myContentPane; }

  @Override
  protected void canRun() throws ConfigurationException {
    final NamesValidator namesValidator = LanguageNamesValidation.INSTANCE.forLanguage(JavaScriptSupportLoader.JAVASCRIPT.getLanguage());

    if (myFileLocal) {
      final String className = myClassNameField.getText();
      if (StringUtil.isEmpty(className)) {
        throw new ConfigurationException(FlexBundle.message("element.name.empty",
                                                            new JSNamedElementPresenter(myElements.iterator().next(),
                                                                                        Capitalization.UpperCase).describeElementKind()));
      }
      if (!namesValidator.isIdentifier(className, myProject)) {
        throw new ConfigurationException(FlexBundle.message("invalid.element.name",
                                                            new JSNamedElementPresenter(myElements.iterator().next(),
                                                                                        Capitalization.UpperCase).describeElementKind(),
                                                            className));
      }
    }

    final String packageName = myTargetPackageField.getText();
    for (final String s : StringUtil.split(packageName, ".")) {
      if (!namesValidator.isIdentifier(s, myProject)) {
        throw new ConfigurationException(FlexBundle.message("invalid.package", packageName));
      }
    }
  }

  @Override
  protected JComponent createCenterPanel() {
    return myContentPane;
  }

  @Override
  public JComponent getPreferredFocusedComponent() {
    return myClassNameField.isVisible() ? myClassNameField : myTargetPackageField.getChildComponent();
  }

  @Override
  protected String getDimensionServiceKey() {
    return FlexMoveClassDialog.class.getName();
  }

  @Override
  protected void doAction() {
    myTargetPackageField.updateRecents();

    PsiElement firstElement = myElements.iterator().next();
    PsiDirectory baseDir;
    if (myTargetContainer instanceof PsiDirectory) {
      baseDir = (PsiDirectory)myTargetContainer;
    }
    else {
      baseDir = PlatformPackageUtil.getDirectory(firstElement);
    }

    String nameToCheck = myFileLocal ? myClassNameField.getText() : null;
    PsiDirectory targetDirectory =
      JSRefactoringUtil.chooseOrCreateDirectoryForClass(myProject, ModuleUtilCore.findModuleForPsiElement(firstElement),
                                                        GlobalSearchScope.projectScope(myProject), myTargetPackageField.getText(),
                                                        nameToCheck, baseDir,
                                                        myCbMoveToAnotherSourceFolder.isSelected() ? ThreeState.YES : ThreeState.NO);
    if (targetDirectory == null) {
      return;
    }

    // file-local case already checked by JSRefactoringUtil.chooseOrCreateDirectoryForClass (see nameToCheck)
    if (!myFileLocal) {
      try {
        for (PsiElement element : myElements) {
          MoveFilesOrDirectoriesUtil.checkMove(element.getContainingFile(), targetDirectory);
        }
      }
      catch (IncorrectOperationException e) {
        CommonRefactoringUtil.showErrorMessage(RefactoringBundle.message("error.title"), e.getMessage(), getHelpId(), myProject);
        return;
      }
    }
    BaseRefactoringProcessor processor;
    if (myFileLocal) {
      processor = new FlexMoveInnerClassProcessor(myElements.iterator().next(), targetDirectory,
                                                  StringUtil.notNullize(myClassNameField.getText()),
                                                  myTargetPackageField.getText(), myCbSearchInComments.isSelected(),
                                                  myCbSearchTextOccurences.isSelected(), myCallback);
    }
    else {
      processor = new FlexMoveClassProcessor(myElements, targetDirectory, myTargetPackageField.getText(), myCbSearchInComments.isSelected(),
                                             myCbSearchTextOccurences.isSelected(), myCallback);
    }
    invokeRefactoring(processor);
  }

  @Override
  protected String getHelpId() {
    return myFileLocal ? "Move_Inner_to_Upper_Level_Dialog_for_ActionScript" : "refactoring.moveClass";
  }
}
