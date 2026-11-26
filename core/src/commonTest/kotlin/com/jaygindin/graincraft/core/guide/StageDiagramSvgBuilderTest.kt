package com.jaygindin.graincraft.core.guide

import com.jaygindin.graincraft.core.Examples
import com.jaygindin.graincraft.core.compile.CompileResult
import com.jaygindin.graincraft.core.compile.DesignCompiler
import com.jaygindin.graincraft.core.shop.ShopProfile
import com.jaygindin.graincraft.core.wood.Rgb
import com.jaygindin.graincraft.core.wood.Woods
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class StageDiagramSvgBuilderTest {
  private val diagram =
    Diagram(
      width = 10.0,
      height = 4.0,
      elements =
        listOf(
          DiagramElement.Box(0.5, 0.5, 2.0, 1.0, Rgb.hex("#5C4033"), "R&D <1A> \"x\""),
          DiagramElement.Box(2.5, 0.5, 1.5, 1.0, Rgb.hex("#F2E3C6"), null, hatched = true),
          DiagramElement.Box(4.0, 0.5, 1.5, 1.0, null, "plain"),
          DiagramElement.Line(1.0, 2.0, 1.0, 3.0, dashed = true),
          DiagramElement.Line(2.0, 2.0, 2.0, 3.0),
          DiagramElement.Text(6.0, 2.0, "Rows 2 & 4 <rotated>"),
          DiagramElement.Dimension(0.5, 3.5, 4.0, 3.5, "3-1/2\""),
        ),
    )

  private val svg = StageDiagramSvgBuilder.svg(diagram)

  @Test
  fun startsWithSvg() {
    assertTrue(svg.startsWith("<svg"), svg)
  }

  @Test
  fun isWellFormed() {
    XmlChecker(svg).check()
  }

  @Test
  fun escapesLabels() {
    assertTrue("R&amp;D &lt;1A&gt;" in svg, svg)
    assertTrue("Rows 2 &amp; 4 &lt;rotated&gt;" in svg, svg)
  }

  @Test
  fun oneRectPerBox() {
    assertEquals(3, Regex("<rect").findAll(svg).count())
  }

  @Test
  fun dashedLinesAreDashed() {
    assertEquals(1, Regex("stroke-dasharray").findAll(svg).count())
  }

  @Test
  fun scalesByPixelsPerInch() {
    assertTrue("width=\"240\"" in svg && "height=\"96\"" in svg, svg)
    assertTrue("width=\"120\"" in StageDiagramSvgBuilder.svg(diagram, pixelsPerInch = 12.0))
  }

  @Test
  fun noExternalReferences() {
    assertFalse("http" in svg.replace("http://www.w3.org/2000/svg", ""), svg)
  }

  @Test
  fun exampleDiagramsAreWellFormedWithOneRectPerBox() {
    for (design in listOf(Examples.a, Examples.c)) {
      val result = DesignCompiler.compile(design, Woods(emptyList()), ShopProfile(), Examples.fmt)
      assertTrue(result is CompileResult.Ok, "expected Ok but was $result")
      for (diagram in result.compiled.stages.mapNotNull { it.diagram }) {
        val out = StageDiagramSvgBuilder.svg(diagram)
        XmlChecker(out).check()
        val boxes = diagram.elements.count { it is DiagramElement.Box }
        assertEquals(boxes, Regex("<rect").findAll(out).count())
      }
    }
  }

  @Test
  fun theCheckerCatchesBadXml() {
    for (bad in listOf("<svg><g></svg>", "<svg>R&D</svg>", "<svg>a > b</svg>", "<svg x=1/>")) {
      assertFailsWith<IllegalStateException>(bad) { XmlChecker(bad).check() }
    }
  }
}

/**
 * A small XML well-formedness check: tags balance, attribute values are quoted, and `&`, `<` and
 * `>` appear in text and attribute values only as entities. Throws on the first problem.
 */
private class XmlChecker(private val xml: String) {
  private val stack = ArrayDeque<String>()
  private var at = 0
  private var rootClosed = false

  fun check() {
    while (at < xml.length) {
      when (xml[at]) {
        '<' -> tag()
        '>' -> error("raw '>' in text at $at")
        '&' -> entity(xml, at++)
        else -> at++
      }
    }
    check(stack.isEmpty()) { "unclosed ${stack.joinToString()}" }
  }

  private fun tag() {
    check(!rootClosed) { "content after the root element at $at" }
    val end = tagEnd(at)
    val body = xml.substring(at + 1, end)
    if (body.startsWith("/")) close(body.drop(1).trim()) else open(body)
    at = end + 1
  }

  private fun close(name: String) {
    check(stack.removeLastOrNull() == name) { "unbalanced </$name> at $at" }
    rootClosed = stack.isEmpty()
  }

  private fun open(body: String) {
    val inner = body.removeSuffix("/")
    val name = inner.takeWhile { !it.isWhitespace() }
    check(name.isNotEmpty()) { "empty tag name at $at" }
    attributes(inner.drop(name.length))
    if (body.endsWith("/")) rootClosed = stack.isEmpty() else stack.addLast(name)
  }

  /** The index of the `>` that ends the tag at `start`, skipping quoted attribute values. */
  private fun tagEnd(start: Int): Int {
    var quoted = false
    var j = start + 1
    while (j < xml.length && (quoted || xml[j] != '>')) {
      check(quoted || xml[j] != '<') { "'<' inside the tag at $start" }
      if (xml[j] == '"') quoted = !quoted
      j++
    }
    check(j < xml.length) { "unterminated tag at $start" }
    return j
  }

  private fun attributes(text: String) {
    var rest = text
    while (rest.isNotBlank()) {
      val match = ATTRIBUTE.find(rest)?.takeIf { it.range.first == 0 }
      checkNotNull(match) { "bad attribute '$rest'" }
      val value = match.groupValues[1]
      check('<' !in value && '>' !in value) { "raw '<' or '>' in attribute" }
      value.indices.filter { value[it] == '&' }.forEach { entity(value, it) }
      rest = rest.substring(match.range.last + 1)
    }
  }

  private fun entity(s: String, index: Int) =
    check(ENTITIES.any { s.startsWith(it, index) }) { "raw '&' at $index" }

  companion object {
    private val ATTRIBUTE = Regex("""\s+[A-Za-z_:][-A-Za-z0-9_:.]*="([^"]*)"""")
    private val ENTITIES = listOf("&amp;", "&lt;", "&gt;", "&quot;", "&apos;")
  }
}
