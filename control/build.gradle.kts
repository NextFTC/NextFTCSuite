plugins {
  alias(libs.plugins.kotlin)
  alias(libs.plugins.spotless)
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

kotlin {
  jvmToolchain(8)
  compilerOptions {
    freeCompilerArgs.addAll("-Xconsistent-data-class-copy-visibility")
  }
}

java {
  toolchain {
    languageVersion.set(JavaLanguageVersion.of(8))
  }
}

tasks.withType<Test>().configureEach { useJUnitPlatform() }
