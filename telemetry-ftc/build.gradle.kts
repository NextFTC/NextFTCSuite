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
  namespace = "dev.nextftc.v2.telemetry.ftc"

  testOptions {
    unitTests.isReturnDefaultValues = true
  }
}

dependencies {
  // the FTC SDK bundles its own slf4j-api dependency that conflicts with ktor's
  api(project(":telemetry")) {
    exclude(group = "org.slf4j")
  }
  compileOnly(libs.bundles.ftc)
  implementation(libs.sloth)

  // The backend implements telemetry types that live in the SDK, so tests need it on their classpath
  testImplementation(libs.ftc.robot.core) { isTransitive = false }
  testImplementation(libs.bundles.kotest)
  testImplementation(libs.mockk)
}

description =
  "FTC lifecycle integration for NextFTC telemetry: logs a WPILOG and serves NetworkTables 4 " +
  "for each OpMode, without the rest of NextFTC."

nextFTCPublishing {
  displayName = "NextFTC Telemetry FTC"
  logoPath = "../assets/logo-icon.svg"
}

dokka {
  dokkaSourceSets.configureEach {
    includes.from("Module.md")
  }
}
