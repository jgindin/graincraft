package com.jaygindin.graincraft.app.ui.library

import com.jaygindin.graincraft.app.testutil.FakePlatform
import com.jaygindin.graincraft.core.design.BoardDesign
import com.jaygindin.graincraft.core.design.Dimensions
import com.jaygindin.graincraft.core.design.EdgeTreatments
import com.jaygindin.graincraft.core.design.GrainMode
import com.jaygindin.graincraft.core.shop.ShopProfile
import com.jaygindin.graincraft.core.units.Length
import com.jaygindin.graincraft.core.wood.Woods

/** A saved design of the given kind at 18 × 12 × 1-1/2, as the library would hold it. */
fun sampleDesign(
  id: String,
  name: String,
  kind: PatternKind = PatternKind.CHECKERBOARD,
  updatedAtMillis: Long = 1_700_000_000_000L,
): BoardDesign =
  BoardDesign(
    id = id,
    name = name,
    revision = 1,
    updatedAtMillis = updatedAtMillis,
    grainMode = if (kind == PatternKind.STRIPES) GrainMode.EDGE_GRAIN else GrainMode.END_GRAIN,
    finished = Dimensions(Length.inches(18), Length.inches(12), Length.inches(1, 1, 2)),
    pattern = defaultPattern(kind),
    edges = EdgeTreatments(),
  )

fun libraryViewModel(platform: FakePlatform) =
  LibraryViewModel(platform.graph, { Woods(emptyList()) }, { ShopProfile() })
