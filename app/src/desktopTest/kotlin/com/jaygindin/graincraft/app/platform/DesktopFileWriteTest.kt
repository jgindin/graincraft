package com.jaygindin.graincraft.app.platform

import java.io.File
import kotlin.io.path.createTempDirectory
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class DesktopFileWriteTest {
  @Test
  fun aWrittenFileIsSaved() {
    val dir = createTempDirectory().toFile()
    try {
      val file = File(dir, "board - shop packet.html")
      assertEquals(SaveResult.Saved(file.absolutePath), writeFile(file, "<html>"))
      assertEquals("<html>", file.readText())
    } finally {
      dir.deleteRecursively()
    }
  }

  @Test
  fun aWriteThatFailsIsReportedNotThrown() {
    val dir = createTempDirectory().toFile()
    try {
      // A plain file where the folder should be cannot hold anything, as with a missing mount.
      val notAFolder = File(dir, "plain").apply { writeText("") }
      val result = writeFile(File(notAFolder, "board - shop packet.html"), "<html>")
      assertTrue(result is SaveResult.Failed && result.reason.isNotBlank(), "$result")
    } finally {
      dir.deleteRecursively()
    }
  }
}
