plugins {
  id("org.jetbrains.kotlin.jvm")
}

kotlin {
  jvmToolchain(8)
  compilerOptions {
    freeCompilerArgs.add("-Xconsistent-data-class-copy-visibility")
  }
}

java {
  toolchain {
    languageVersion.set(JavaLanguageVersion.of(8))
  }
}

tasks.withType<Test>().configureEach {
  useJUnitPlatform()
}
