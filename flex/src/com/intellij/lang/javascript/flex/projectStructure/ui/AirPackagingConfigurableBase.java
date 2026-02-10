// Copyright 2000-2024 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package com.intellij.lang.javascript.flex.projectStructure.ui;

import com.intellij.lang.javascript.flex.FlexBundle;
import com.intellij.lang.javascript.flex.FlexUtils;
import com.intellij.lang.javascript.flex.projectStructure.model.AndroidPackagingOptions;
import com.intellij.lang.javascript.flex.projectStructure.model.IosPackagingOptions;
import com.intellij.lang.javascript.flex.projectStructure.model.ModifiableAirPackagingOptions;
import com.intellij.lang.javascript.flex.projectStructure.model.ModifiableAndroidPackagingOptions;
import com.intellij.lang.javascript.flex.projectStructure.model.ModifiableIosPackagingOptions;
import com.intellij.openapi.Disposable;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.ui.NamedConfigurable;
import com.intellij.openapi.util.ActionCallback;
import com.intellij.openapi.util.Disposer;
import com.intellij.openapi.wm.IdeFocusManager;
import com.intellij.ui.UserActivityListener;
import com.intellij.ui.UserActivityWatcher;
import com.intellij.ui.navigation.Place;
import com.intellij.uiDesigner.core.GridConstraints;
import com.intellij.uiDesigner.core.GridLayoutManager;
import com.intellij.uiDesigner.core.Spacer;
import com.intellij.util.EventDispatcher;
import com.intellij.util.ui.UIUtil;
import org.jetbrains.annotations.Nullable;

import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import java.awt.Dimension;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

public abstract class AirPackagingConfigurableBase<T extends ModifiableAirPackagingOptions> extends NamedConfigurable<T>
  implements Place.Navigator {

  public enum Location {
    CustomDescriptor("custom-descriptor-path"),
    PackageFileName("package-file-name"),
    FilesToPackage("files-to-package"),
    ProvisioningProfile("provisioning-profile"),
    Keystore("keystore"),
    IosSdkPath("ios-sdk-path");

    public final String errorId;

    Location(final String errorId) {
      this.errorId = errorId;
    }
  }

  public interface AirDescriptorInfoProvider {
    String getMainClass();

    String getAirVersion();

    String[] getExtensionIDs();

    boolean isAndroidPackagingEnabled();

    boolean isIOSPackagingEnabled();

    void setCustomDescriptorForAndroidAndIOS(String descriptorPath);
  }

  private final JPanel myMainPanel;

  private final JCheckBox myEnabledCheckBox;
  private final AirDescriptorForm myAirDescriptorForm;
  private final JTextField myPackageFileNameTextField;
  private final FilesToPackageForm myFilesToPackageForm;
  private final SigningOptionsForm mySigningOptionsForm;

  private final Module myModule;
  private final T myModel;

  private final boolean isAndroid;
  private final boolean isIOS;

  private final AirDescriptorInfoProvider myAirDescriptorInfoProvider;

  private final Disposable myDisposable;
  private final EventDispatcher<UserActivityListener> myUserActivityDispatcher;
  private boolean myFreeze;

  public AirPackagingConfigurableBase(final Module module, final T model, final AirDescriptorInfoProvider airDescriptorInfoProvider) {
    myModule = module;
    myModel = model;
    myAirDescriptorInfoProvider = airDescriptorInfoProvider;

    isAndroid = model instanceof ModifiableAndroidPackagingOptions;
    isIOS = model instanceof ModifiableIosPackagingOptions;

    {
      final Runnable descriptorCreator = () -> {
        final String folderPath = FlexUtils.getContentOrModuleFolderPath(myModule);
        final String mainClass = myAirDescriptorInfoProvider.getMainClass();
        final String airVersion = myAirDescriptorInfoProvider.getAirVersion();
        final String[] extensions = myAirDescriptorInfoProvider.getExtensionIDs();
        final boolean androidEnabled = myAirDescriptorInfoProvider.isAndroidPackagingEnabled();
        final boolean iosEnabled = myAirDescriptorInfoProvider.isIOSPackagingEnabled();

        final CreateAirDescriptorTemplateDialog dialog =
          new CreateAirDescriptorTemplateDialog(myModule.getProject(), folderPath, mainClass, airVersion, extensions,
                                                androidEnabled, iosEnabled);

        if (dialog.showAndGet()) {
          final String descriptorPath = dialog.getDescriptorPath();
          setUseCustomDescriptor(descriptorPath);

          if (androidEnabled && iosEnabled && dialog.isBothAndroidAndIosSelected()) {
            final int choice =
              Messages.showYesNoDialog(myModule.getProject(), FlexBundle.message("use.same.descriptor.for.android.and.ios"),
                                       CreateAirDescriptorTemplateDialog.getTitleText(), Messages.getQuestionIcon());
            if (choice == Messages.YES) {
              myAirDescriptorInfoProvider.setCustomDescriptorForAndroidAndIOS(descriptorPath);
            }
          }
        }
      };

      myAirDescriptorForm = new AirDescriptorForm(myModule.getProject(), descriptorCreator);
      myFilesToPackageForm = new FilesToPackageForm(myModule.getProject());

      final SigningOptionsForm.Mode mode = isIOS ? SigningOptionsForm.Mode.iOS
                                                 : isAndroid ? SigningOptionsForm.Mode.Android
                                                             : SigningOptionsForm.Mode.Desktop;
      mySigningOptionsForm = new SigningOptionsForm(myModule.getProject(), mode);
    }
    {
      // GUI initializer generated by IntelliJ IDEA GUI Designer
      // >>> IMPORTANT!! <<<
      // DO NOT EDIT OR ADD ANY CODE HERE!
      myMainPanel = new JPanel();
      myMainPanel.setLayout(new GridLayoutManager(6, 1, new Insets(0, 0, 0, 0), -1, -1));
      final Spacer spacer1 = new Spacer();
      myMainPanel.add(spacer1, new GridConstraints(5, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_VERTICAL, 1,
                                                   GridConstraints.SIZEPOLICY_WANT_GROW, null, null, null, 0, false));
      myMainPanel.add(myFilesToPackageForm.$$$getRootComponent$$$(),
                      new GridConstraints(3, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH,
                                          GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_WANT_GROW,
                                          GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_WANT_GROW, null, null, null, 0,
                                          false));
      myMainPanel.add(mySigningOptionsForm.$$$getRootComponent$$$(),
                      new GridConstraints(4, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL,
                                          GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_WANT_GROW,
                                          GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
      myMainPanel.add(myAirDescriptorForm.$$$getRootComponent$$$(),
                      new GridConstraints(1, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL,
                                          GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW,
                                          GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0,
                                          false));
      final JPanel panel1 = new JPanel();
      panel1.setLayout(new GridLayoutManager(1, 3, new Insets(0, 0, 0, 0), -1, -1));
      myMainPanel.add(panel1, new GridConstraints(2, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH,
                                                  GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW,
                                                  GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null,
                                                  null, 0, false));
      final JLabel label1 = new JLabel();
      label1.setText("Package file name (without extension):");
      label1.setDisplayedMnemonic('N');
      label1.setDisplayedMnemonicIndex(13);
      panel1.add(label1,
                 new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED,
                                     GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
      myPackageFileNameTextField = new JTextField();
      panel1.add(myPackageFileNameTextField, new GridConstraints(0, 1, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL,
                                                                 GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_FIXED,
                                                                 null, new Dimension(150, -1), new Dimension(200, -1), 0, false));
      final Spacer spacer2 = new Spacer();
      panel1.add(spacer2, new GridConstraints(0, 2, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL,
                                              GridConstraints.SIZEPOLICY_WANT_GROW, 1, null, null, null, 0, false));
      myEnabledCheckBox = new JCheckBox();
      myEnabledCheckBox.setText("Enabled");
      myEnabledCheckBox.setMnemonic('E');
      myEnabledCheckBox.setDisplayedMnemonicIndex(0);
      myMainPanel.add(myEnabledCheckBox, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE,
                                                             GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW,
                                                             GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
      label1.setLabelFor(myPackageFileNameTextField);
    }

    myEnabledCheckBox.addActionListener(new ActionListener() {
      @Override
      public void actionPerformed(final ActionEvent e) {
        updateControls();
      }
    });

    myDisposable = Disposer.newDisposable();
    UserActivityWatcher watcher = new UserActivityWatcher();
    watcher.register(myMainPanel);
    myUserActivityDispatcher = EventDispatcher.create(UserActivityListener.class);
    watcher.addUserActivityListener(new UserActivityListener() {
      @Override
      public void stateChanged() {
        if (myFreeze) {
          return;
        }
        myUserActivityDispatcher.getMulticaster().stateChanged();
      }
    }, myDisposable);
  }

  /** @noinspection ALL */
  public JComponent $$$getRootComponent$$$() { return myMainPanel; }

  public void addUserActivityListener(final UserActivityListener listener, final Disposable disposable) {
    myUserActivityDispatcher.addListener(listener, disposable);
  }

  public void removeUserActivityListeners() {
    for (UserActivityListener listener : myUserActivityDispatcher.getListeners()) {
      myUserActivityDispatcher.removeListener(listener);
    }
  }

  private void updateControls() {
    final boolean enabled = isPackagingEnabled();
    UIUtil.setEnabled(myMainPanel, enabled, true);
    myEnabledCheckBox.setEnabled(true);
    myAirDescriptorForm.updateControls();
    mySigningOptionsForm.setEnabled(enabled);
  }

  @Override
  public T getEditableObject() {
    return myModel;
  }

  @Override
  public void setDisplayName(final String name) {
  }

  @Override
  public String getBannerSlogan() {
    return getDisplayName();
  }

  @Override
  public JComponent createOptionsPanel() {
    return myMainPanel;
  }

  @Override
  public void reset() {
    myFreeze = true;
    try {
      myEnabledCheckBox.setVisible(isAndroid || isIOS);

      if (isAndroid) myEnabledCheckBox.setSelected(((AndroidPackagingOptions)myModel).isEnabled());
      if (isIOS) myEnabledCheckBox.setSelected(((IosPackagingOptions)myModel).isEnabled());

      myAirDescriptorForm.resetFrom(myModel);
      myPackageFileNameTextField.setText(myModel.getPackageFileName());
      myFilesToPackageForm.resetFrom(myModel.getFilesToPackage());
      mySigningOptionsForm.resetFrom(myModel.getSigningOptions());

      updateControls();
    }
    finally {
      myFreeze = false;
    }
  }

  @Override
  public boolean isModified() {
    if (isAndroid && myEnabledCheckBox.isSelected() != ((AndroidPackagingOptions)myModel).isEnabled()) return true;
    if (isIOS && myEnabledCheckBox.isSelected() != ((IosPackagingOptions)myModel).isEnabled()) return true;

    if (myAirDescriptorForm.isModified(myModel)) return true;
    if (!myModel.getPackageFileName().equals(myPackageFileNameTextField.getText().trim())) return true;
    if (myFilesToPackageForm.isModified(myModel.getFilesToPackage())) return true;
    if (mySigningOptionsForm.isModified(myModel.getSigningOptions())) return true;

    return false;
  }

  @Override
  public void apply() {
    applyTo(myModel);
  }

  public void applyTo(final ModifiableAirPackagingOptions model) {
    if (isAndroid) ((ModifiableAndroidPackagingOptions)model).setEnabled(myEnabledCheckBox.isSelected());
    if (isIOS) ((ModifiableIosPackagingOptions)model).setEnabled(myEnabledCheckBox.isSelected());

    myAirDescriptorForm.applyTo(model);
    model.setPackageFileName(myPackageFileNameTextField.getText().trim());
    model.setFilesToPackage(myFilesToPackageForm.getFilesToPackage());
    mySigningOptionsForm.applyTo(model.getSigningOptions());
  }

  @Override
  public void disposeUIResources() {
    Disposer.dispose(myDisposable);
  }

  public void setUseCustomDescriptor(final String descriptorPath) {
    myAirDescriptorForm.setUseCustomDescriptor(descriptorPath);
  }

  public boolean isPackagingEnabled() {
    return !myEnabledCheckBox.isVisible() || myEnabledCheckBox.isSelected();
  }

  @Override
  public ActionCallback navigateTo(final @Nullable Place place, final boolean requestFocus) {
    if (place != null && place.getPath(FlexBCConfigurable.LOCATION_ON_TAB) instanceof Location loc) {
      return switch (loc) {
        case CustomDescriptor -> myAirDescriptorForm.navigateTo(loc);
        case PackageFileName -> IdeFocusManager.findInstance().requestFocus(myPackageFileNameTextField, true);
        case FilesToPackage -> myFilesToPackageForm.navigateTo(loc);
        case ProvisioningProfile, Keystore, IosSdkPath -> mySigningOptionsForm.navigateTo(loc);
      };
    }
    return ActionCallback.DONE;
  }
}
