plugins {
  alias(libs.plugins.kotlin)
  alias(libs.plugins.spotless)
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
  jvmToolchain(8)
  compilerOptions {
    freeCompilerArgs.addAll("-jvm-default=no-compatibility", "-Xconsistent-data-class-copy-visibility")
  }
}

java {
  toolchain {
    languageVersion.set(JavaLanguageVersion.of(8))
  }
}

tasks.withType<Test>().configureEach { useJUnitPlatform() }
