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
  namespace = "dev.nextftc.v2.hardware"

  testOptions {
    unitTests.isReturnDefaultValues = true
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
