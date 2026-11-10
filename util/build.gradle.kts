plugins {
  id("dev.nextftc.suite.kotlin-jvm")
}

description =
  "Pure-JVM utilities shared across NextFTC: WPILib struct schemas, an event loop, and a logging facade."

dependencies {
  implementation(libs.kotlin.reflect)

  testImplementation(libs.bundles.kotest)
}

nextFTCPublishing {
  displayName = "NextFTC Util"
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
