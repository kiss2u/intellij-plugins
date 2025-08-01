// Copyright 2000-2025 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package org.intellij.terraform.config;

import com.intellij.openapi.actionSystem.IdeActions;
import com.intellij.openapi.fileEditor.FileDocumentManager;
import com.intellij.openapi.util.io.FileUtil;
import com.intellij.testFramework.fixtures.BasePlatformTestCase;
import org.intellij.terraform.TfTestUtils;

import java.io.File;
import java.io.IOException;

public class HCLStatementMoverTest extends BasePlatformTestCase {
  @Override
  protected String getTestDataPath() {
    return TfTestUtils.getTestDataPath() + "/terraform/mover";
  }

  private void both() throws IOException {
    doTest(true, true);
  }

  private void up() throws IOException {
    doTest(true, false);
  }

  private void down() throws IOException {
    doTest(false, true);
  }

  private void doTest(boolean up, boolean down) throws IOException {
    final String testName = getTestName(false);

    File source = new File(getTestDataPath() + "/" + testName + ".tf");
    if (!source.exists()) {
      assertTrue(source.createNewFile());
      fail("No source file exist, created");
    }

    if (up) {
      String expectedFile = testName + ".after_up.tf";
      File check = new File(getTestDataPath() + "/" + expectedFile);
      if (!check.exists()) {
        FileUtil.copy(source, check);
        fail("No check file exist, copied from base one");
      }

      myFixture.configureByFile(testName + ".tf");
      myFixture.performEditorAction(IdeActions.ACTION_MOVE_STATEMENT_UP_ACTION);
      myFixture.checkResultByFile(expectedFile, true);
    }

    if (down) {
      String expectedFile = testName + ".after_down.tf";
      File check = new File(getTestDataPath() + "/" + expectedFile);
      if (!check.exists()) {
        FileUtil.copy(source, check);
        fail("No check file exist, copied from base one");
      }
      if (up) {
        FileDocumentManager.getInstance().reloadFromDisk(myFixture.getDocument(myFixture.getFile()));
      }
      myFixture.configureByFile(testName + ".tf");
      myFixture.performEditorAction(IdeActions.ACTION_MOVE_STATEMENT_DOWN_ACTION);
      myFixture.checkResultByFile(expectedFile, true);
    }
  }


  public void testSimpleProperty() throws Exception {
    both();
  }

  public void testSimpleBlock() throws Exception {
    both();
  }

  public void testVariableBlocks() throws Exception {
    both();
  }

  public void testPropertiesInBlock() throws Exception {
    both();
  }

  public void testMixedPropertiesInBlock() throws Exception {
    both();
  }

  public void testWithMultipleSelection() throws Exception {
    both();
  }

  public void testNoEscapeFromArray() throws Exception {
    both();
  }
}