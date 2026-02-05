// Copyright 2000-2026 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package com.intellij.prettierjs.next

import com.intellij.javascript.nodejs.library.yarn.pnp.YarnPnpNodePackage
import com.intellij.javascript.nodejs.util.NodePackage
import com.intellij.javascript.nodejs.util.NodePackageRef
import com.intellij.lang.javascript.buildTools.npm.PackageJsonUtil
import com.intellij.lang.javascript.modules.TestNpmPackage
import com.intellij.lang.javascript.nodejs.library.yarn.AbstractYarnPnpIntegrationTest
import com.intellij.openapi.vfs.newvfs.impl.VfsRootAccess
import com.intellij.prettierjs.PrettierConfiguration
import com.intellij.prettierjs.PrettierUtil
import com.intellij.prettierjs.stable.PRETTIER_LATEST_TEST_PACKAGE_SPEC
import com.intellij.prettierjs.stable.ReformatWithPrettierGenericTest

/**
 * Prettier tests running against the latest version (prettier@latest).
 * Note: These tests may fail when Prettier releases breaking changes.
 */
@TestNpmPackage(PRETTIER_LATEST_TEST_PACKAGE_SPEC)
class ReformatWithPrettierLatestTest : ReformatWithPrettierGenericTest() {
  fun testNextVersion() = withInstallation {
    doReformatFile<Throwable>("toReformat", "js") {
      performNpmInstallForPackageJson("package.json")
      val nextPrettier = NodePackage(myFixture.tempDirPath + "/node_modules/prettier")
      PrettierConfiguration.getInstance(project).withLinterPackage(NodePackageRef.create(nextPrettier))
    }
  }

  /**
   * Tests Prettier integration with Yarn Berry PnP (Plug'n'Play).
   * Installs Yarn globally, configures Yarn Berry, and uses YarnPnpNodePackage for resolution.
   */
  fun testYarnPrettierBasicExample() = withInstallation {
    doReformatFile<Throwable>("toReformat", "js") {
      val file = myFixture.findFileInTempDir("toReformat.js")
      val root = file.parent
      val yarnPkg = AbstractYarnPnpIntegrationTest.installYarnGlobally(nodeJsAppRule)
      VfsRootAccess.allowRootAccess(myFixture.testRootDisposable, yarnPkg.systemIndependentPath)
      AbstractYarnPnpIntegrationTest.configureYarnBerryAndRunYarnInstall(project, yarnPkg, nodeJsAppRule, root)
      configureYarnPrettierPackage(root)
    }
  }

  private fun configureYarnPrettierPackage(root: com.intellij.openapi.vfs.VirtualFile) {
    val configuration = PrettierConfiguration.getInstance(project)
    val packageJsonFile = PackageJsonUtil.findChildPackageJsonFile(root)
    assertNotNull("package.json not found in $root", packageJsonFile)
    val yarnPrettierPkg = YarnPnpNodePackage.create(
      project,
      packageJsonFile!!,
      PrettierUtil.PACKAGE_NAME, false, false
    )
    assertNotNull(yarnPrettierPkg)
    configuration.withLinterPackage(NodePackageRef.create(yarnPrettierPkg!!))
    val readYarnPrettierPkg = configuration.getPackage(null)
    assertInstanceOf(readYarnPrettierPkg, YarnPnpNodePackage::class.java)
    assertEquals("yarn:package.json:prettier", readYarnPrettierPkg.systemIndependentPath)
  }
}
