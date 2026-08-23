plugins {
  id("dev.nextftc.suite.kotlin-jvm")
}

description = "A custom linear algebra library for NextControl."

dependencies {
  implementation(libs.ejml)

  testImplementation(libs.bundles.kotest)
}

nextFTCPublishing {
  displayName = "NextControl Linear Algebra"
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
