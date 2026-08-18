plugins {
  `kotlin-dsl`
}

group = "dev.nextftc.suite.build"

repositories {
  google()
  mavenCentral()
}

dependencies {
  implementation("org.jetbrains.kotlin:kotlin-gradle-plugin:2.3.0")
  implementation("org.jetbrains.dokka:dokka-gradle-plugin:2.2.0")
  implementation("io.deepmedia.tools.deployer:deployer:0.18.0")
  implementation("com.diffplug.spotless:spotless-plugin-gradle:8.1.0")
  implementation("com.android.tools.build:gradle:8.7.3")
}

kotlin {
  jvmToolchain(17)
}
