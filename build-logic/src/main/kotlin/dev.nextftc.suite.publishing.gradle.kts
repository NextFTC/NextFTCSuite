import com.android.build.api.dsl.LibraryExtension
import com.diffplug.gradle.spotless.SpotlessExtension
import dev.nextftc.suite.build.NextFtcPublishingExtension
import dev.nextftc.suite.build.nextFtcKtlintEditorConfig
import org.gradle.jvm.tasks.Jar
import org.jetbrains.dokka.gradle.DokkaExtension
import org.jetbrains.dokka.gradle.engine.plugins.DokkaHtmlPluginParameters

plugins {
  id("org.jetbrains.dokka")
  id("io.deepmedia.tools.deployer")
  id("com.diffplug.spotless")
}

val extension = extensions.create<NextFtcPublishingExtension>("nextFTCPublishing")

dependencies {
  add("dokkaPlugin", "org.jetbrains.dokka:mathjax-plugin")
}

configure<SpotlessExtension> {
  kotlin {
    target("src/*/kotlin/**/*.kt")
    ktlint().editorConfigOverride(nextFtcKtlintEditorConfig)
  }
}

configure<DokkaExtension> {
  pluginManager.withPlugin("org.jetbrains.kotlin.jvm") {
    dokkaSourceSets.named("main") {
      sourceRoots.from(file("src/main/kotlin"))

      sourceLink {
        localDirectory.set(file("src/main/kotlin"))
        remoteUrl("https://github.com/NextFTC/NextFTCSuite/blob/main/${project.name}/src/main/kotlin")
        remoteLineSuffix.set("#L")
      }
    }
  }

  moduleName.set(extension.displayName)

  pluginsConfiguration.named<DokkaHtmlPluginParameters>("html") {
    footerMessage.set("Copyright © 2026 NextFTC - Licensed under the BSD-3-Clause license.")
    customAssets.from(extension.logoPath)
  }
}

val dokkaExtension = extensions.getByType<DokkaExtension>()

val dokkaJar by tasks.registering(Jar::class) {
  dependsOn(tasks.named("dokkaGenerate"))
  from(dokkaExtension.basePublicationsDirectory.dir("html"))
  archiveClassifier.set("html-docs")
}

deployer {
  projectInfo {
    url = "https://nextftc.dev/"
    scm {
      fromGithub("NextFTC", "NextFTCSuite")
    }
    license("BSD 3-Clause License", "https://opensource.org/license/bsd-3-clause")
    developer("Zach Harel", "zach@zharel.me", url = "https://github.com/zachwaffle4")
    developer(
      "Davis Luxenberg",
      "davis.luxenberg@outlook.com",
      url = "https://github.com/BeepBot99",
    )
    developer("Rowan McAlpin", "rowan@nextftc.dev", url = "https://rowanmcalpin.com")

    name.set(extension.displayName)
    groupId.set(extension.group)
  }

  signing {
    key.set(secret("MVN_GPG_KEY"))
    password.set(secret("MVN_GPG_PASSWORD"))
  }

  content {
    pluginManager.withPlugin("org.jetbrains.kotlin.jvm") {
      kotlinComponents {
        kotlinSources()
        docs(dokkaJar)
      }
    }

    pluginManager.withPlugin("com.android.library") {
      pluginManager.withPlugin("org.jetbrains.kotlin.android") {
        configure<LibraryExtension> {
          publishing {
            singleVariant("release") {
              withSourcesJar()
            }
          }
        }

        androidComponents("release") {
          docs(dokkaJar)
        }
      }
    }
  }

  release {
    version.set(extension.version)
  }

  localSpec()

  nexusSpec("snapshot") {
    repositoryUrl.set("https://central.sonatype.com/repository/maven-snapshots/")
    auth {
      user.set(secret("SONATYPE_USERNAME"))
      password.set(secret("SONATYPE_PASSWORD"))
    }
  }

  centralPortalSpec {
    auth {
      user.set(secret("SONATYPE_USERNAME"))
      password.set(secret("SONATYPE_PASSWORD"))
    }
    allowMavenCentralSync.set(extension.automaticMavenCentralSync)
  }
}
