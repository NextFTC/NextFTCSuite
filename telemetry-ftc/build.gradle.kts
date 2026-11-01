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
}

dependencies {
  api(project(":telemetry"))
  compileOnly(libs.bundles.ftc)
  implementation(libs.sloth)

  testImplementation(libs.bundles.kotest)
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
