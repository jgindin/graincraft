plugins {
  alias(libs.plugins.jetbrainsCompose) apply false
  alias(libs.plugins.compose.compiler) apply false
  alias(libs.plugins.kotlinMultiplatform) apply false
  alias(libs.plugins.kotlinSerialization) apply false
  alias(libs.plugins.roborazzi) apply false
  alias(libs.plugins.kover) apply false
  alias(libs.plugins.ktfmt)
  alias(libs.plugins.detekt)
}

// ktfmt, Google style, 100 columns.
//   ./gradlew ktfmtFormat   rewrites Kotlin sources and build scripts
//   ./gradlew ktfmtCheck    fails if any file is not formatted (what CI runs)
allprojects {
  apply(plugin = "com.ncorti.ktfmt.gradle")
  configure<com.ncorti.ktfmt.gradle.KtfmtExtension> {
    googleStyle()
    maxWidth.set(100)
  }
  apply(plugin = "dev.detekt")
  configure<dev.detekt.gradle.extensions.DetektExtension> {
    config.setFrom(rootProject.file("config/detekt/detekt.yml"))
    buildUponDefaultConfig = true
    // Take every Kotlin directory under src/, whatever the source set layout.
    source.setFrom(fileTree("src") { include("**/kotlin/**/*.kt") })
  }
  // The script tasks scan the whole project dir; keep them out of tool and VCS state.
  tasks.withType<com.ncorti.ktfmt.gradle.tasks.KtfmtBaseTask>().configureEach {
    exclude(".jj/**", ".git/**", ".gradle/**", ".kotlin/**", ".superpowers/**", "**/build/**")
  }
}
