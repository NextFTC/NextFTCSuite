/*
 * Copyright (c) 2026 NextFTC Team
 *
 *  Use of this source code is governed by an BSD-3-clause
 *  license that can be found in the LICENSE.md file at the root of this repository or at
 *  https://opensource.org/license/bsd-3-clause.
 */

plugins {
  id("dev.nextftc.suite.android-library")
}

android {
  namespace = "dev.nextftc.v2.robot"
}

dependencies {
  api(project(":hardware"))
  api(libs.ivy)
  implementation(libs.functional.interfaces)
  compileOnly(libs.bundles.ftc)
  implementation(libs.sloth)
  implementation(libs.kotlin.reflect)

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
