// Copyright 2000-2024 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package org.jetbrains.plugins.cucumber.steps;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleUtilCore;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiDirectory;
import com.intellij.psi.PsiFile;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.plugins.cucumber.BDDFrameworkType;
import org.jetbrains.plugins.cucumber.CucumberJvmExtensionPoint;
import org.jetbrains.plugins.cucumber.CucumberUtil;
import org.jetbrains.plugins.cucumber.OptionalStepDefinitionExtensionPoint;
import org.jetbrains.plugins.cucumber.inspections.CucumberStepDefinitionCreationContext;
import org.jetbrains.plugins.cucumber.psi.GherkinFile;
import org.jetbrains.plugins.cucumber.psi.GherkinStep;

import java.util.*;
import java.util.regex.Pattern;


public final class CucumberStepHelper {
  private static final Logger LOG = Logger.getInstance(CucumberStepHelper.class.getName());

  /**
   * Creates a file that will contain step definitions
   *
   * @param dir                      container for created file
   * @param fileNameWithoutExtension name of the file with out "." and extension
   * @param frameworkType            type of file to create
   */
  public static PsiFile createStepDefinitionFile(final @NotNull PsiDirectory dir,
                                                 final @NotNull String fileNameWithoutExtension,
                                                 final @NotNull BDDFrameworkType frameworkType) {
    final CucumberJvmExtensionPoint ep = getExtensionMap().get(frameworkType);
    if (ep == null) {
      LOG.error(String.format("Unsupported step definition file type %s", frameworkType.toString()));
      return null;
    }

    return ep.getStepDefinitionCreator().createStepDefinitionContainer(dir, fileNameWithoutExtension);
  }

  public static boolean validateNewStepDefinitionFileName(@NotNull Project project,
                                                          final @NotNull String fileName,
                                                          final @NotNull BDDFrameworkType frameworkType) {
    final CucumberJvmExtensionPoint ep = getExtensionMap().get(frameworkType);
    assert ep != null;
    return ep.getStepDefinitionCreator().validateNewStepDefinitionFileName(project, fileName);
  }


  /**
   * Searches for ALL step definitions, groups it by step definition class and sorts by pattern size.
   * For each step definition class it finds the largest pattern.
   *
   * @param featureFile file with steps
   * @param step        step itself
   * @return definitions
   */
  public static @NotNull Collection<AbstractStepDefinition> findStepDefinitions(final @NotNull PsiFile featureFile, final @NotNull GherkinStep step) {
    final Module module = ModuleUtilCore.findModuleForPsiElement(featureFile);
    if (module == null) {
      return Collections.emptyList();
    }
    String substitutedName = step.getSubstitutedName();
    if (substitutedName == null) {
      return Collections.emptyList();
    }

    Map<Class<? extends AbstractStepDefinition>, AbstractStepDefinition> definitionsByClass =
      new HashMap<>();
    List<AbstractStepDefinition> allSteps = loadStepsFor(featureFile, module);

    for (AbstractStepDefinition stepDefinition : allSteps) {
      if (stepDefinition == null) continue;
      boolean matches = stepDefinition.matches(substitutedName);
      if (matches && stepDefinition.supportsStep(step)) {
        final Pattern currentLongestPattern = getPatternByDefinition(definitionsByClass.get(stepDefinition.getClass()));
        final Pattern newPattern = getPatternByDefinition(stepDefinition);
        final int newPatternLength = ((newPattern != null) ? newPattern.pattern().length() : -1);
        if ((currentLongestPattern == null) || (currentLongestPattern.pattern().length() < newPatternLength)) {
          definitionsByClass.put(stepDefinition.getClass(), stepDefinition);
        }
      }
    }
    return definitionsByClass.values();
  }

  /**
   * Returns pattern from step definition (if exists)
   *
   * @param definition step definition
   * @return pattern or null if does not exist
   */
  private static @Nullable Pattern getPatternByDefinition(final @Nullable AbstractStepDefinition definition) {
    if (definition == null) {
      return null;
    }
    return definition.getPattern();
  }

  // ToDo: use binary search here
  public static List<AbstractStepDefinition> findStepDefinitionsByPattern(final @NotNull String pattern, final @NotNull Module module) {
    final List<AbstractStepDefinition> allSteps = loadStepsFor(null, module);
    final List<AbstractStepDefinition> result = new ArrayList<>();
    for (AbstractStepDefinition stepDefinition : allSteps) {
      final String elementText = stepDefinition.getCucumberRegex();
      if (elementText != null && elementText.equals(pattern)) {
        result.add(stepDefinition);
      }
    }
    return result;
  }

  public static List<AbstractStepDefinition> getAllStepDefinitions(final @NotNull PsiFile featureFile) {
    final Module module = ModuleUtilCore.findModuleForPsiElement(featureFile);
    if (module == null) return Collections.emptyList();
    return loadStepsFor(featureFile, module);
  }


  private static List<AbstractStepDefinition> loadStepsFor(final @Nullable PsiFile featureFile, final @NotNull Module module) {
    ArrayList<AbstractStepDefinition> result = new ArrayList<>();

    for (CucumberJvmExtensionPoint extension : getCucumberExtensions()) {
      result.addAll(CucumberUtil.loadFrameworkSteps(extension, featureFile, module));
    }
    return result;
  }

  public static Set<CucumberStepDefinitionCreationContext> getStepDefinitionContainers(final @NotNull GherkinFile featureFile) {
    Set<CucumberStepDefinitionCreationContext> result = new HashSet<>();
    for (CucumberJvmExtensionPoint ep : getCucumberExtensions()) {
      // Skip if framework file creation support is optional
      if ((ep instanceof OptionalStepDefinitionExtensionPoint) &&
          !((OptionalStepDefinitionExtensionPoint)ep).participateInStepDefinitionCreation(featureFile)) {
        continue;
      }
      final Collection<? extends PsiFile> psiFiles = ep.getStepDefinitionContainers(featureFile);
      final BDDFrameworkType frameworkType = ep.getStepFileType();
      for (final PsiFile psiFile : psiFiles) {
        result.add(new CucumberStepDefinitionCreationContext(psiFile, frameworkType));
      }
    }
    return result;
  }

  public static Map<BDDFrameworkType, CucumberJvmExtensionPoint> getExtensionMap() {
    HashMap<BDDFrameworkType, CucumberJvmExtensionPoint> result = new HashMap<>();
    for (CucumberJvmExtensionPoint e : getCucumberExtensions()) {
      result.put(e.getStepFileType(), e);
    }
    return result;
  }

  public static List<CucumberJvmExtensionPoint> getCucumberExtensions() {
    return CucumberJvmExtensionPoint.EP_NAME.getExtensionList();
  }

  public static int getExtensionCount() {
    return getCucumberExtensions().size();
  }

  public static boolean isGherkin6Supported(@NotNull Module module) {
    if (ApplicationManager.getApplication().isUnitTestMode()) {
      return true;
    }
    for (CucumberJvmExtensionPoint ep : getCucumberExtensions()) {
      if (ep.isGherkin6Supported(module)) {
        return true;
      }
    }
    return false;
  }
}
