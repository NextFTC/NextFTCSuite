plugins {
  id("dev.nextftc.suite.kotlin-jvm")
}

description = "A custom units library for NextControl."

dependencies { testImplementation(libs.bundles.kotest) }

nextFTCPublishing {
  displayName = "NextControl Units"
  logoPath = "../assets/logo-icon.svg"
}

dokka {
  dokkaSourceSets.configureEach {
    includes.from("Module.md")
  }
}

kotlin {
  compilerOptions {
    freeCompilerArgs.add("-jvm-default=no-compatibility")
  }
}
