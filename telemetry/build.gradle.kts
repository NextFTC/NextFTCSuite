plugins {
  id("dev.nextftc.suite.kotlin-jvm")
}

description =
  "Pure-JVM telemetry for NextFTC: a registry, a WPILOG writer, a NetworkTables 4 server, and " +
  "WPILib struct schemas."

dependencies {
  implementation(libs.kotlin.reflect)
  implementation(libs.ktor.server.cio)
  implementation(libs.ktor.server.websockets)
  implementation(libs.kotlinx.serialization.json)
  implementation(libs.msgpack.core)

  testImplementation(libs.bundles.kotest)
  testImplementation(libs.ktor.client.cio)
  testImplementation(libs.ktor.client.websockets)
}

nextFTCPublishing {
  displayName = "NextFTC Telemetry"
  logoPath = "../assets/logo-icon.svg"
}

dokka {
  dokkaSourceSets.configureEach {
    includes.from("Module.md")
    if (name == "simulator") suppress.set(true)
  }
}

kotlin {
  compilerOptions {
    freeCompilerArgs.add("-jvm-default=no-compatibility")
  }

  // The simulator test reads the simulator's internal classes.
  target.compilations.getByName("test").associateWith(target.compilations.getByName("simulator"))
}

// The simulator is its own source set, in src/simulator: a fake robot built on the main code and
// never published. The tests use it too, to check what it does.
val simulator: SourceSet by sourceSets.creating {
  compileClasspath += sourceSets.main.get().output
  runtimeClasspath += sourceSets.main.get().output
}

configurations[simulator.implementationConfigurationName].extendsFrom(configurations.implementation.get())
configurations[simulator.runtimeOnlyConfigurationName].extendsFrom(configurations.runtimeOnly.get())

sourceSets.test {
  compileClasspath += simulator.output
  runtimeClasspath += simulator.output
}

// A fake robot that serves NetworkTables 4 on port 5810 and writes a log for each OpMode. Options go
// in -PsimArgs, for example -PsimArgs="--auto 5 --teleop 5". See LiveSimulator.kt.
tasks.register<JavaExec>("simulateLive") {
  group = "verification"
  description = "Runs a simulated robot that streams telemetry over NetworkTables 4 and logs a WPILOG."
  classpath = simulator.runtimeClasspath
  mainClass.set("dev.nextftc.telemetry.sim.LiveSimulatorKt")
  args = (findProperty("simArgs") as String?)?.split(" ")?.filter { it.isNotBlank() } ?: emptyList()
}
