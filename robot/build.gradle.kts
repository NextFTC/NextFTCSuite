/*
 * Copyright (c) 2026 NextFTC Team
 *
 *  Use of this source code is governed by an BSD-3-clause
 *  license that can be found in the LICENSE.md file at the root of this repository or at
 *  https://opensource.org/license/bsd-3-clause.
 */

plugins {
  alias(libs.plugins.kotlin.android)
  alias(libs.plugins.android.library)
  alias(libs.plugins.spotless)
}

android {
  namespace = "dev.nextftc.v2.robot"
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
  }
}

kotlin {
  jvmToolchain(8)
  compilerOptions {
    freeCompilerArgs.addAll("-Xconsistent-data-class-copy-visibility")
  }
}

dependencies {
  api(project(":hardware"))
  compileOnly(libs.ivy)
  implementation(libs.functional.interfaces)
  compileOnly(libs.bundles.ftc)
  implementation(libs.sloth)

  testImplementation(libs.bundles.kotest)
  testImplementation(kotlin("test"))
}

description = "The robot library for NextFTC, a user-friendly library for FTC."

nextFTCPublishing {
  displayName = "NextFTC Robot"
  logoPath = "../assets/logo-icon.svg"
}

dokka {
  dokkaSourceSets.configureEach {
    includes.from("Module.md")
  }
}

tasks.withType<Test>().configureEach {
  javaLauncher.set(javaToolchains.launcherFor { languageVersion.set(JavaLanguageVersion.of(11)) })
  useJUnitPlatform()
}
