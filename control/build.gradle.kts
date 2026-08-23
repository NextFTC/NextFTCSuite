plugins {
  id("dev.nextftc.suite.kotlin-jvm")
}

description = "A WPIMath inspired library for controls and other math classes and functions."

dependencies {
  api(project(":units"))
  api(project(":linalg"))

  testImplementation(libs.bundles.kotest)
}

nextFTCPublishing {
  displayName = "NextControl"
  logoPath = "../assets/logo-icon.svg"
}

dokka {
  dokkaSourceSets.configureEach {
    includes.from("Module.md")
  }
}
