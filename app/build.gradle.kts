import java.util.Properties
import org.jetbrains.compose.desktop.application.dsl.TargetFormat

plugins {
  alias(libs.plugins.kotlinMultiplatform)
  alias(libs.plugins.kotlinSerialization)
  alias(libs.plugins.jetbrainsCompose)
  alias(libs.plugins.compose.compiler)
  alias(libs.plugins.roborazzi)
}

val versionName: String =
  Properties()
    .apply { rootProject.file("version.properties").inputStream().use { load(it) } }
    .getProperty("versionName") ?: error("versionName missing from version.properties")

// AppInfo.VERSION, so the About section shows the version from version.properties.
val generateAppInfo =
  tasks.register("generateAppInfo") {
    val version = versionName
    val outDir = layout.buildDirectory.dir("generated/appInfo/kotlin")
    inputs.property("version", version)
    outputs.dir(outDir)
    doLast {
      val file = outDir.get().file("com/jaygindin/graincraft/app/AppInfo.kt").asFile
      file.parentFile.mkdirs()
      file.writeText(
        """
        |package com.jaygindin.graincraft.app
        |
        |/** Generated from version.properties by the build. */
        |object AppInfo {
        |  const val VERSION = "$version"
        |}
        |"""
          .trimMargin(),
      )
    }
  }

kotlin {
  jvmToolchain(21)
  jvm("desktop")

  sourceSets {
    commonMain { kotlin.srcDir(generateAppInfo) }
    commonMain.dependencies {
      implementation(project(":core"))
      implementation(compose.runtime)
      implementation(compose.foundation)
      implementation(compose.material3)
      implementation(compose.ui)
      implementation(compose.components.resources)
      implementation(libs.androidx.lifecycle.viewmodel.compose)
      implementation(libs.kotlinx.coroutines.core)
      implementation(libs.kotlinx.serialization.json)
      implementation(libs.filekit.dialogs.compose)
    }
    named("desktopMain") {
      dependencies {
        implementation(compose.desktop.currentOs)
        implementation(libs.kotlinx.coroutines.swing)
      }
    }
    named("desktopTest") {
      dependencies {
        implementation(kotlin("test"))
        implementation(compose.desktop.uiTestJUnit4)
        implementation(compose.desktop.currentOs)
        implementation(libs.roborazzi.compose.desktop)
        implementation(libs.kotlinx.coroutines.test)
      }
    }
  }
}

roborazzi { outputDir.set(file("screenshots")) }

compose.desktop {
  application {
    mainClass = "com.jaygindin.graincraft.app.MainKt"
    // Run and package with the build's Java 21 toolchain rather than the JDK Gradle runs on, which
    // can be older than the class files.
    javaHome =
      javaToolchains
        .launcherFor { languageVersion = JavaLanguageVersion.of(21) }
        .get()
        .metadata
        .installationPath
        .asFile
        .absolutePath
    nativeDistributions {
      targetFormats(TargetFormat.Deb)
      // The launcher and menu entry name; the Debian package keeps a lower-case name below.
      packageName = "GrainCraft Studio"
      packageVersion = "$versionName.0"
      description = "Design end-grain and edge-grain cutting boards and print a shop packet."
      vendor = "Jay R. Gindin"
      linux {
        packageName = "graincraft-studio"
        debMaintainer = "Jay R. Gindin"
        menuGroup = "Graphics"
        appCategory = "Graphics"
        shortcut = true
        // An end-grain checker block, rendered once from a small Skia drawing.
        iconFile.set(project.file("icons/graincraft-studio.png"))
      }
    }
  }
}
