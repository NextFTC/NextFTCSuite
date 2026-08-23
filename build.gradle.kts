plugins {
  alias(libs.plugins.kotlin) apply false
  alias(libs.plugins.kotlin.android) apply false
  alias(libs.plugins.android.library) apply false
  id("dev.nextftc.suite.publishing.multi-module")
  alias(libs.plugins.dokka)
}

dependencies {
  dokka(project(":units"))
  dokka(project(":linalg"))
  dokka(project(":control"))
  dokka(project(":hardware"))
  dokka(project(":robot"))
  dokkaPlugin(libs.dokka.mathjax.plugin)
}
