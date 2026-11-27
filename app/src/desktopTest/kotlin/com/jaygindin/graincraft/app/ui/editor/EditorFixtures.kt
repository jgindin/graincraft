package com.jaygindin.graincraft.app.ui.editor

import com.jaygindin.graincraft.app.data.WorkspaceStore
import com.jaygindin.graincraft.app.testutil.FakeWorkspaceStore
import com.jaygindin.graincraft.core.compile.CompileResult
import com.jaygindin.graincraft.core.design.BoardDesign
import com.jaygindin.graincraft.core.shop.ShopProfile
import com.jaygindin.graincraft.core.units.FractionPrecision
import com.jaygindin.graincraft.core.units.LengthUnit
import com.jaygindin.graincraft.core.units.UnitFormatter
import com.jaygindin.graincraft.core.wood.PoreType
import com.jaygindin.graincraft.core.wood.Rgb
import com.jaygindin.graincraft.core.wood.WoodSpecies
import com.jaygindin.graincraft.core.wood.Woods
import com.jaygindin.graincraft.core.wood.derivedTones
import com.jaygindin.graincraft.core.wood.rateCustomWood
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent

const val START_MILLIS = 1_800_000_000_000L

val inchFormatter = UnitFormatter(LengthUnit.INCH, FractionPrecision.SIXTEENTH)

/** The shared flows and store an editor reads, each one a test can change. */
class EditorInputs(
  val store: WorkspaceStore = FakeWorkspaceStore(),
  val woods: MutableStateFlow<Woods> = MutableStateFlow(Woods(emptyList())),
  val profile: MutableStateFlow<ShopProfile> = MutableStateFlow(ShopProfile()),
  val formatter: MutableStateFlow<UnitFormatter> = MutableStateFlow(inchFormatter),
)

/** An editor driven by virtual time; it has started and compiled its first design on return. */
@OptIn(ExperimentalCoroutinesApi::class)
fun TestScope.editor(design: BoardDesign, inputs: EditorInputs = EditorInputs()): EditorViewModel {
  val vm =
    EditorViewModel(
      initial = design,
      store = inputs.store,
      woods = inputs.woods,
      profile = inputs.profile,
      formatter = inputs.formatter,
      clock = { START_MILLIS + testScheduler.currentTime },
      scope = backgroundScope,
      compileDispatcher = StandardTestDispatcher(testScheduler),
    )
  runCurrent()
  return vm
}

/** Moves virtual time on by [millis] and runs whatever is then due. */
@OptIn(ExperimentalCoroutinesApi::class)
fun TestScope.advance(millis: Long) {
  advanceTimeBy(millis)
  runCurrent()
}

fun EditorViewModel.compiledName(): String? =
  (state.value.compile as? CompileResult.Ok)?.compiled?.design?.name

fun customWood(id: String): WoodSpecies {
  val base = Rgb.hex("#8A5A3C")
  return WoodSpecies(
    id = id,
    name = "Mystery $id",
    botanicalName = "Custom",
    jankaLbf = 1200,
    raw = derivedTones(base),
    oiled = derivedTones(base),
    poreType = PoreType.DIFFUSE,
    oily = false,
    bleedsColor = false,
    irritant = false,
    suitability = rateCustomWood(1200, PoreType.DIFFUSE, oily = false, irritant = false),
    notes = emptyList(),
    custom = true,
  )
}

/** A store whose design saves fail while [failing] is set. */
class FlakyStore(val inner: FakeWorkspaceStore = FakeWorkspaceStore()) : WorkspaceStore by inner {
  var failing = true

  override suspend fun saveDesign(design: BoardDesign) {
    check(!failing) { "The disk is full" }
    inner.saveDesign(design)
  }
}

/** A store whose design saves wait until [gate] is completed, so a test can act mid-write. */
class GatedStore(val inner: FakeWorkspaceStore = FakeWorkspaceStore()) : WorkspaceStore by inner {
  val gate = CompletableDeferred<Unit>()

  override suspend fun saveDesign(design: BoardDesign) {
    gate.await()
    inner.saveDesign(design)
  }
}
