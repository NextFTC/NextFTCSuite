import com.diffplug.gradle.spotless.SpotlessExtension
import dev.nextftc.suite.build.nextFtcKtlintEditorConfig

allprojects {
  apply(plugin = "com.diffplug.spotless")

  version = property("version") as String
  group = "dev.nextftc.v2"

  extensions.configure<SpotlessExtension> {
    kotlinGradle {
      ktlint().editorConfigOverride(nextFtcKtlintEditorConfig)
    }
  }
}

subprojects {
  pluginManager.apply("dev.nextftc.suite.publishing")
}

tasks.register("deployCentralPortal") {
  group = "publishing"
  description = "Publishes all subprojects to Maven Central."
  dependsOn(subprojects.map { "${it.path}:deployCentralPortal" })
}

tasks.register("deployLocal") {
  group = "publishing"
  description = "Publishes all subprojects to Maven Local."
  dependsOn(subprojects.map { "${it.path}:deployLocal" })
}

tasks.register("deployNexusSnapshot") {
  group = "publishing"
  description = "Publishes all subprojects to Maven Central Snapshots."
  dependsOn(subprojects.map { "${it.path}:deployNexusSnapshot" })
}
