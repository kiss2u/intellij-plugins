package com.jetbrains.cidr.cpp.embedded.platformio.project

import com.intellij.clion.testFramework.nolang.junit5.core.clionProjectTestFixture
import com.intellij.openapi.components.service
import com.intellij.testFramework.junit5.TestApplication
import com.intellij.testFramework.junit5.fixture.tempPathFixture
import com.jetbrains.cidr.external.system.model.impl.ExternalResolveConfigurationBuilder
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

@TestApplication
internal class TestConfigureLanguages {

  private val dirFixture = tempPathFixture()

  private val dir by dirFixture
  private val project by clionProjectTestFixture(dirFixture)

  @Test
  fun testSwitchInQuotes() {
    val builder = ExternalResolveConfigurationBuilder("test-env", "PlatformIO", dir.toFile())
    val workspace = project.service<PlatformioWorkspace>()

    val jsonConfig = mapOf(
      "compiler_type" to "gcc",
      "cc_flags" to "\"-DCHIP_ADDRESS_RESOLVE_IMPL_INCLUDE_HEADER=<lib/address_resolve/AddressResolve_DefaultImpl.h>\"",
    )

    val languages = PlatformioProjectResolver.configureLanguages(jsonConfig, builder, workspace)

    val cLanguage = languages.single { !it.languageKind.isCpp }

    // Assert the switch is parsed without quotes
    assertThat(cLanguage.compilerSwitches).contains("-DCHIP_ADDRESS_RESOLVE_IMPL_INCLUDE_HEADER=<lib/address_resolve/AddressResolve_DefaultImpl.h>")
  }
}
