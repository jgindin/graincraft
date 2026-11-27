package com.jaygindin.graincraft.app.ui.theme

import androidx.compose.foundation.Image
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.v2.runComposeUiTest
import androidx.compose.ui.text.font.FontListFontFamily
import androidx.compose.ui.unit.Density
import com.jaygindin.graincraft.app.testutil.contrast
import graincraft.app.generated.resources.Res
import graincraft.app.generated.resources.ic_add
import graincraft.app.generated.resources.ic_arrow_back
import graincraft.app.generated.resources.ic_arrow_downward
import graincraft.app.generated.resources.ic_arrow_upward
import graincraft.app.generated.resources.ic_carpenter
import graincraft.app.generated.resources.ic_check
import graincraft.app.generated.resources.ic_close
import graincraft.app.generated.resources.ic_delete
import graincraft.app.generated.resources.ic_download
import graincraft.app.generated.resources.ic_edit
import graincraft.app.generated.resources.ic_error
import graincraft.app.generated.resources.ic_fit_screen
import graincraft.app.generated.resources.ic_folder_open
import graincraft.app.generated.resources.ic_forest
import graincraft.app.generated.resources.ic_grid_view
import graincraft.app.generated.resources.ic_info
import graincraft.app.generated.resources.ic_more_vert
import graincraft.app.generated.resources.ic_print
import graincraft.app.generated.resources.ic_redo
import graincraft.app.generated.resources.ic_search
import graincraft.app.generated.resources.ic_settings
import graincraft.app.generated.resources.ic_undo
import graincraft.app.generated.resources.ic_upload_file
import graincraft.app.generated.resources.ic_warning
import graincraft.app.generated.resources.ic_zoom_in
import graincraft.app.generated.resources.ic_zoom_out
import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.decodeToImageVector
import org.jetbrains.compose.resources.painterResource

/** The theme's colours are readable, its type never gets too small, and every icon loads. */
@OptIn(ExperimentalTestApi::class)
class ThemeTest {
  private class Themed(
    val scheme: ColorScheme,
    val type: Typography,
    val ratings: RatingColors,
  )

  private fun themed(dark: Boolean): Themed {
    var result: Themed? = null
    runComposeUiTest {
      setContent {
        GrainCraftTheme(dark) {
          result =
            Themed(MaterialTheme.colorScheme, MaterialTheme.typography, LocalRatingColors.current)
        }
      }
      waitForIdle()
    }
    return result!!
  }

  private fun forBothSchemes(check: (String, Themed) -> Unit) {
    check("light", themed(dark = false))
    check("dark", themed(dark = true))
  }

  private fun assertReadable(name: String, fg: Color, bg: Color) {
    val ratio = contrast(fg, bg)
    assertTrue(ratio >= 4.5f, "$name: $ratio")
  }

  @Test
  fun textRolesAreReadable() = forBothSchemes { mode, t ->
    val s = t.scheme
    assertReadable("$mode onSurface/surface", s.onSurface, s.surface)
    assertReadable(
      "$mode onSurfaceVariant/surfaceContainer",
      s.onSurfaceVariant,
      s.surfaceContainer,
    )
    assertReadable("$mode primary/background", s.primary, s.background)
    assertReadable(
      "$mode onPrimaryContainer/primaryContainer",
      s.onPrimaryContainer,
      s.primaryContainer,
    )
    assertReadable("$mode error/surfaceContainer", s.error, s.surfaceContainer)
    val r = t.ratings
    assertReadable("$mode onRecommended/recommended", r.onRecommended, r.recommended)
    assertReadable("$mode onCaution/caution", r.onCaution, r.caution)
    assertReadable("$mode onNotSuitable/notSuitable", r.onNotSuitable, r.notSuitable)
  }

  @Test
  fun bodyAndLabelTextAreNotTooSmall() = forBothSchemes { _, t ->
    assertTrue(t.type.bodySmall.fontSize.value >= 13f, "bodySmall: ${t.type.bodySmall.fontSize}")
    assertTrue(t.type.labelSmall.fontSize.value >= 11f, "labelSmall: ${t.type.labelSmall.fontSize}")
  }

  @Test
  fun usesTheBundledFont() {
    val t = themed(dark = false).type
    val styles = listOf(t.bodyLarge, t.bodyMedium, t.titleLarge, t.labelLarge, t.headlineSmall)
    styles.forEach {
      assertTrue(it.fontFamily is FontListFontFamily, "not the bundled family: ${it.fontFamily}")
    }
  }

  @Test
  fun everyIconLoads() = runComposeUiTest {
    val icons: List<DrawableResource> =
      listOf(
        Res.drawable.ic_add,
        Res.drawable.ic_arrow_back,
        Res.drawable.ic_close,
        Res.drawable.ic_delete,
        Res.drawable.ic_undo,
        Res.drawable.ic_redo,
        Res.drawable.ic_print,
        Res.drawable.ic_folder_open,
        Res.drawable.ic_upload_file,
        Res.drawable.ic_download,
        Res.drawable.ic_search,
        Res.drawable.ic_settings,
        Res.drawable.ic_carpenter,
        Res.drawable.ic_forest,
        Res.drawable.ic_grid_view,
        Res.drawable.ic_warning,
        Res.drawable.ic_info,
        Res.drawable.ic_error,
        Res.drawable.ic_check,
        Res.drawable.ic_edit,
        Res.drawable.ic_more_vert,
        Res.drawable.ic_zoom_in,
        Res.drawable.ic_zoom_out,
        Res.drawable.ic_fit_screen,
        Res.drawable.ic_arrow_upward,
        Res.drawable.ic_arrow_downward,
      )
    setContent { icons.forEach { Image(painterResource(it), contentDescription = null) } }
    waitForIdle()
    // painterResource decodes asynchronously and hides a malformed file, so decode each one here.
    val files = File("src/commonMain/composeResources/drawable").listFiles().orEmpty()
    assertEquals(icons.size, files.count { it.name.startsWith("ic_") }, "icon files vs list")
    files.forEach { file ->
      val vector = file.readBytes().decodeToImageVector(Density(1f))
      assertTrue(vector.defaultWidth.value > 0f, "${file.name} decoded empty")
    }
  }

  private val namedColors =
    listOf("Color.White", "Color.Black", "Color.Gray", "Color.LightGray", "Color.DarkGray")
  private val exempt = listOf("/ui/theme/", "/ui/preview/", "/ui/diagram/")

  /** Sizes and colours come from the theme; species colours come from data. */
  @Test
  fun screensUseTheThemeNotLiterals() {
    val root = File("src/commonMain/kotlin/com/jaygindin/graincraft/app")
    assertTrue(root.isDirectory, "run from the app module: ${root.absolutePath}")
    val offenders =
      root
        .walkTopDown()
        .filter { it.isFile && it.extension == "kt" }
        .filterNot { file -> exempt.any { file.path.contains(it) } }
        .flatMap { file ->
          file.readLines().mapIndexedNotNull { i, line ->
            val literal =
              line.contains("Color(0x") ||
                namedColors.any { line.contains(it) } ||
                line.contains("fontSize =")
            if (literal) "${file.name}:${i + 1}: ${line.trim()}" else null
          }
        }
        .toList()
    assertTrue(offenders.isEmpty(), offenders.joinToString("\n"))
  }
}
