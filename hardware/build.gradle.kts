/*
 * Copyright (c) 2026 NextFTC Team
 *
 *  Use of this source code is governed by an BSD-3-clause
 *  license that can be found in the LICENSE.md file at the root of this repository or at
 *  https://opensource.org/license/bsd-3-clause.
 */

plugins {
  alias(libs.plugins.android.library)
  alias(libs.plugins.kotlin.android)
  alias(libs.plugins.spotless)
}

android {
  namespace = "dev.nextftc.v2.hardware"
  compileSdk = 30

  defaultConfig {
    minSdk = 24
  }

  compileOptions {
    sourceCompatibility = JavaVersion.VERSION_1_8
    targetCompatibility = JavaVersion.VERSION_1_8
  }

  publishing {
    singleVariant("release")
  }

  testOptions {
    targetSdk = 28
    unitTests.isReturnDefaultValues = true
  }
}

kotlin {
  jvmToolchain(8)
  compilerOptions {
    freeCompilerArgs.addAll("-Xconsistent-data-class-copy-visibility")
  }
}

dependencies {
  api(project(":control"))
  api(libs.functional.interfaces)
  compileOnly(libs.bundles.ftc)
  implementation(libs.sloth)

  testImplementation(libs.bundles.kotest)
  testImplementation(libs.mockk)
}

description =
  "The hardware library for NextFTC, a user-friendly library for FTC. " +
  "Includes hardware interfaces, wrapper implementations, and hardware commands."

nextFTCPublishing {
  displayName = "NextFTC Hardware"
  logoPath = "../assets/logo-icon.svg"
}

dokka {
  dokkaSourceSets.configureEach {
    includes.from("Module.md")
    externalDocumentationLinks.create("FTC RobotCore") {
      url("https://javadoc.io/doc/org.firstinspires.ftc/RobotCore/${libs.versions.ftc.get()}/")
      packageListUrl(
        "https://javadoc.io/doc/org.firstinspires.ftc/RobotCore/${libs.versions.ftc.get()}/package-list",
      )
    }
    externalDocumentationLinks.create("FTC Hardware") {
      url("https://javadoc.io/doc/org.firstinspires.ftc/Hardware/${libs.versions.ftc.get()}/")
      packageListUrl(
        "https://javadoc.io/doc/org.firstinspires.ftc/Hardware/${libs.versions.ftc.get()}/package-list",
      )
    }
  }
}

tasks.withType<Test>().configureEach {
  javaLauncher.set(javaToolchains.launcherFor { languageVersion.set(JavaLanguageVersion.of(11)) })
  useJUnitPlatform()
}
