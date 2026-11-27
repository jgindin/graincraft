plugins {
  alias(libs.plugins.kotlinMultiplatform)
  alias(libs.plugins.kotlinSerialization)
  alias(libs.plugins.kover)
}

kotlin {
  jvmToolchain(libs.versions.java.get().toInt())
  jvm()
  wasmJs { nodejs() }

  sourceSets {
    commonMain.dependencies { implementation(libs.kotlinx.serialization.json) }
    commonTest.dependencies { implementation(libs.kotlin.test) }
  }
}

kover { reports { verify { rule { minBound(85) } } } }
