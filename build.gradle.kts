import com.diffplug.gradle.spotless.SpotlessExtension
import io.deepmedia.tools.deployer.DeployerExtension

plugins {
  alias(libs.plugins.kotlin) apply false
  alias(libs.plugins.kotlin.android) apply false
  alias(libs.plugins.android.library) apply false
  alias(libs.plugins.nextftc.publishing)
  alias(libs.plugins.spotless)
  alias(libs.plugins.dokka)
}

allprojects {
  apply(plugin = "com.diffplug.spotless")

  version = property("version") as String
  group = "dev.nextftc.v2"

  extensions.configure<SpotlessExtension> {
    kotlinGradle {
      ktlint().editorConfigOverride(
        mapOf(
          "ktlint_code_style" to "intellij_idea",
          "indent_size" to "2",
          "continuation_indent_size" to "2",
          "ktlint_standard_no-wildcard-imports" to "disabled",
          "max_line_length" to "108",
        ),
      )
    }
  }
}

subprojects {
  extensions.configure<DeployerExtension> {
    projectInfo {
      url = "https://nextftc.dev/"
      scm {
        fromGithub("NextFTC", "NextFTCSuite")
      }
      license("BSD 3-Clause License", "https://opensource.org/license/bsd-3-clause")
      developer("Zach Harel", "zach@zharel.me", url = "https://github.com/zachwaffle4")
      developer(
        "Davis Luxenberg",
        "davis.luxenberg@outlook.com",
        url = "https://github.com/BeepBot99",
      )
      developer("Rowan McAlpin", "rowan@nextftc.dev", url = "https://rowanmcalpin.com")
    }
  }

  dokka {
    pluginsConfiguration.html {
      footerMessage.set("Copyright © 2026 NextFTC - Licensed under the BSD-3-Clause license.")
    }
  }

  dependencies {
    dokkaPlugin("org.jetbrains.dokka:mathjax-plugin")
  }

  extensions.configure<SpotlessExtension> {
    kotlin {
      target("src/*/kotlin/**/*.kt")
      ktlint().editorConfigOverride(
        mapOf(
          "ktlint_code_style" to "intellij_idea",
          "indent_size" to "2",
          "continuation_indent_size" to "2",
          "ktlint_standard_no-wildcard-imports" to "disabled",
          "max_line_length" to "108",
        ),
      )
    }
  }
}

dependencies {
  dokka(project(":units"))
  dokka(project(":linalg"))
  dokka(project(":control"))
  dokka(project(":hardware"))
  dokka(project(":robot"))
  dokkaPlugin(libs.dokka.mathjax.plugin)
}
