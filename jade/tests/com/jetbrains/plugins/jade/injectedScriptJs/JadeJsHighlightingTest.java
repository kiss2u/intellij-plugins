// Copyright 2000-2025 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package com.jetbrains.plugins.jade.injectedScriptJs;

import com.intellij.testFramework.fixtures.BasePlatformTestCase;
import com.jetbrains.plugins.jade.JadeTestUtil;
import org.jetbrains.annotations.NotNull;

/**
 * @author Konstantin Bulenkov
 */
public class JadeJsHighlightingTest extends BasePlatformTestCase {

  @NotNull
  @Override
  protected String getTestDataPath() {
    return JadeTestUtil.getBaseTestDataPath() + "/injectedScriptJs/";
  }

  public void testHighlightingVar() {
    myFixture.testHighlighting(getTestName(true) + "." + "jade");
  }

  public void testArrowFunctionExpression() {
    myFixture.testHighlighting(getTestName(true) + "." + "jade");
  }
}
