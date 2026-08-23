package dev.nextftc.suite.build

import org.gradle.api.Project
import org.gradle.api.model.ObjectFactory
import org.gradle.kotlin.dsl.property
import javax.inject.Inject

open class NextFtcPublishingExtension @Inject constructor(objects: ObjectFactory, project: Project) {
  val displayName = objects.property<String>()
  val version = objects.property<String>().convention(project.provider { project.version.toString() })
  val group = objects.property<String>().convention(project.provider { project.group.toString() })
  val automaticMavenCentralSync = objects.property<Boolean>()
    .convention(
      project.providers.gradleProperty("dev.nextftc.publishing.automaticMavenCentralSync").map(String::toBoolean),
    )
  val logoPath = objects.property<String>().convention("assets/logo-icon.svg")
}
