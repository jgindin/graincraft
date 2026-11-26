plugins {
  alias(libs.plugins.kotlinMultiplatform)
  alias(libs.plugins.kotlinSerialization)
  alias(libs.plugins.kover)
}

kotlin {
  jvmToolchain(21)
  jvm()
  wasmJs { nodejs() }

  sourceSets {
    commonMain.dependencies { implementation(libs.kotlinx.serialization.json) }
    commonTest.dependencies { implementation(libs.kotlin.test) }
  }
}

kover { reports { verify { rule { minBound(85) } } } }
