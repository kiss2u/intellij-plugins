package com.intellij.protobuf.ide.highlighter;

import com.intellij.protobuf.TestUtils;
import com.intellij.protobuf.fixtures.PbCodeInsightFixtureTestCase;
import com.intellij.testFramework.DumbModeTestUtils;
import com.intellij.testFramework.fixtures.impl.CodeInsightTestFixtureImpl;

public class PbHighlightingAnnotatorWhileDumbTest extends PbCodeInsightFixtureTestCase {

  @Override
  public void setUp() throws Exception {
    super.setUp();
    myFixture.addFileToProject(
      TestUtils.OPENSOURCE_DESCRIPTOR_PATH, TestUtils.getOpensourceDescriptorText());
    TestUtils.addTestFileResolveProvider(
      getProject(), TestUtils.OPENSOURCE_DESCRIPTOR_PATH, getTestRootDisposable());
    TestUtils.registerTestdataFileExtension();
  }

  @Override
  public String getTestDataPath() {
    return super.getTestDataPath();
  }

  public void testAnnotatorInDumbMode() {
    myFixture.configureByFile("ide/highlighter/TopLevelString.proto.testdata");

    CodeInsightTestFixtureImpl.mustWaitForSmartMode(false, getTestRootDisposable());
    DumbModeTestUtils.runInDumbModeSynchronously(getProject(), () -> {
      myFixture.testFile("ide/highlighter/Highlighted.dumb.proto.testdata").checkSymbolNames().test();
    });
  }
}
