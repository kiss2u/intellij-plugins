package org.jetbrains.qodana.staticAnalysis.inspections.runner

import com.intellij.codeInspection.ex.InspectionProfileImpl
import com.intellij.openapi.util.Disposer
import com.intellij.openapi.util.registry.Registry
import org.jetbrains.qodana.registry.QodanaRegistry.SCOPE_EXTENDING_ENABLE_KEY
import org.jetbrains.qodana.staticAnalysis.QodanaTestCase
import org.jetbrains.qodana.staticAnalysis.scopes.InspectionToolScopeExtender
import org.jetbrains.qodana.staticAnalysis.scopes.QodanaScopeExtenderProvider
import org.jetbrains.qodana.staticAnalysis.script.scoped.SCOPED_SCRIPT_NAME
import org.jetbrains.qodana.staticAnalysis.testFramework.reinstantiateInspectionRelatedServices
import org.junit.Test

class QodanaScopedScriptConfigurationIntegrationTest : QodanaConfigurationIntegrationBaseTest() {
  public override fun setUp() {
    super.setUp()
    val initInspections = InspectionProfileImpl.INIT_INSPECTIONS
    InspectionProfileImpl.INIT_INSPECTIONS = true
    Disposer.register(testRootDisposable) {
      InspectionProfileImpl.INIT_INSPECTIONS = initInspections
    }
    val registryValue = Registry.get(SCOPE_EXTENDING_ENABLE_KEY)
    registryValue.setValue(true)
    Disposer.register(testRootDisposable) {
      registryValue.resetToDefault()
    }
    reinstantiateInspectionRelatedServices(project, testRootDisposable)
    InspectionToolScopeExtender.EP_NAME.point.registerExtension(InspectionToolScopeExtenderMock, testRootDisposable)
    QodanaScopeExtenderProvider.EP_NAME.point.registerExtension(QodanaScopeExtenderProviderMock, testRootDisposable)
  }

  @Test
  fun `scoped with empty scope`() {
    val extendedScope = buildExtendedScope("""
      {
        "files" : []
      }
    """.trimIndent())
    assertEmpty(extendedScope)
  }

  @Test
  fun `scoped with extended scope`() {
    val extendedScope = buildExtendedScope("""
      {
        "files" : [ {
          "path" : "A.java",
          "added" : [ ],
          "deleted" : [ ]
        }]
      }
    """.trimIndent())
    assertDoesntContain(extendedScope, "A.java")
    assertContainsElements(extendedScope, "B.java")
    assertDoesntContain(extendedScope, "C.java")
  }

  @Test
  fun `scoped with extended scope by multi files`() {
    val extendedScope = buildExtendedScope("""
      {
        "files" : [{
            "path" : "A.java",
            "added" : [ ],
            "deleted" : [ ]
          },
          {
            "path" : "B.java",
            "added" : [ ],
            "deleted" : [ ]
          }
        ]
      }
    """.trimIndent())
    assertDoesntContain(extendedScope, "A.java")
    assertDoesntContain(extendedScope, "B.java")
    assertContainsElements(extendedScope, "C.java")
  }

  @Test
  fun `scoped with disabled extending inspection on one file`() {
    val extendedScope = buildExtendedScope("""
      {
        "files" : [{
            "path" : "A.java",
            "added" : [ ],
            "deleted" : [ ]
          },
          {
            "path" : "B.java",
            "added" : [ ],
            "deleted" : [ ]
          }
        ]
      }
    """.trimIndent(), fileToIgnore = "B.java")
    // A.java -> B.java. B.java should not be included in extendedScope even though no extenders are enabled on it
    // Since B.java is not included in extendedScope, it is not being processed, so C.java doesn't get there as well.
    assertEmpty(extendedScope)
  }

  @Test
  fun `scoped with non referenced file`() {
    val extendedScope = buildExtendedScope("""
      {
        "files" : [{
            "path" : "C.java",
            "added" : [ ],
            "deleted" : [ ]
          }]
      }
    """.trimIndent())
    assertEmpty(extendedScope)
  }

  @Test
  fun `scoped with non extending inspection profile`() {
    val extendedScope = buildExtendedScope("""
      {
        "files" : [{
            "path" : "A.java",
            "added" : [ ],
            "deleted" : [ ]
          }]
      }
    """.trimIndent(), inspectionToInclude = "unused")
    assertEmpty(extendedScope)
  }

  private fun buildExtendedScope(scopeFileText: String, inspectionToInclude: String = "ConstantValue", fileToIgnore: String = ""): Collection<String> {
    val testProjectPath = project.basePath

    val qodanaYAML = """
      version: 1.0
      profile:
        path: profile.yaml
      script:
        name: $SCOPED_SCRIPT_NAME
        parameters:
          scope-file: scope
      runPromoInspections: false
      disableSanityInspections: true
    """.trimIndent()
    val ignorePart = if (fileToIgnore.isNotEmpty()) """
          ignore:
            - "$fileToIgnore"
    """.trim() else ""

    val profileYAML = """
      inspections:
        - group: ALL
          enabled: false
        - inspection: $inspectionToInclude
          enabled: true
          ${ignorePart}
    """.trimIndent()

    val projectFiles = listOf(
      "qodana.yaml" to qodanaYAML,
      "profile.yaml" to profileYAML,
      "scope" to scopeFileText,
      "A.java" to "class A {}",
      "B.java" to "class B {}",
      "C.java" to "class C {}",
    )

    val cliArgs = listOf(
      "$testProjectPath",
      "$testProjectPath/out")

    val additionalFiles = mutableSetOf<String>()
    QodanaTestCase.runTest {
      val script = buildScript(cliArgs, project, projectFiles, this)
      additionalFiles.addAll(script.runContext.getAdditionalFiles())
    }
    return additionalFiles
  }

  private fun QodanaRunContext.getAdditionalFiles() = when(this) {
    is QodanaRunIncrementalContext -> scopeExtended.keys.map { it.name }
    else -> emptyList()
  }
}
