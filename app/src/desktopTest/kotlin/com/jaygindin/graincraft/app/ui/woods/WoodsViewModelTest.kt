package com.jaygindin.graincraft.app.ui.woods

import com.jaygindin.graincraft.app.testutil.FakePlatform
import com.jaygindin.graincraft.core.wood.PoreType
import com.jaygindin.graincraft.core.wood.Rating
import com.jaygindin.graincraft.core.wood.Rgb
import com.jaygindin.graincraft.core.wood.WoodCatalog
import com.jaygindin.graincraft.core.wood.WoodSpecies
import com.jaygindin.graincraft.core.wood.derivedTones
import com.jaygindin.graincraft.core.wood.rateCustomWood
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain

@OptIn(ExperimentalCoroutinesApi::class)
class WoodsViewModelTest {
  @BeforeTest fun setUp() = Dispatchers.setMain(UnconfinedTestDispatcher())

  @AfterTest fun tearDown() = Dispatchers.resetMain()

  private fun custom(id: String, janka: Int = 1200): WoodSpecies {
    val base = Rgb.hex("#8A5A3C")
    return WoodSpecies(
      id = id,
      name = "Mystery $id",
      botanicalName = "Custom",
      jankaLbf = janka,
      raw = derivedTones(base),
      oiled = derivedTones(base),
      poreType = PoreType.DIFFUSE,
      oily = false,
      bleedsColor = false,
      irritant = false,
      suitability = rateCustomWood(janka, PoreType.DIFFUSE, oily = false, irritant = false),
      notes = emptyList(),
      custom = true,
    )
  }

  @Test
  fun searchFilters() = runTest {
    val vm = WoodsViewModel(FakePlatform().store)

    assertEquals(WoodCatalog.builtIns.size, vm.visible.value.size)
    vm.setQuery("oak")

    assertEquals(listOf("red-oak", "white-oak"), vm.visible.value.map { it.id }.sorted())
    vm.setQuery("  OAK ")
    assertEquals(2, vm.visible.value.size)
  }

  @Test
  fun filtersNarrowTheList() = runTest {
    val platform = FakePlatform()
    val vm = WoodsViewModel(platform.store)
    vm.saveCustom(custom("custom-1"))

    vm.setFilter(WoodFilter.CUSTOM, true)
    assertEquals(listOf("custom-1"), vm.visible.value.map { it.id })
    vm.setFilter(WoodFilter.CUSTOM, false)

    vm.setFilter(WoodFilter.RECOMMENDED_ONLY, true)
    assertTrue(
      vm.visible.value.all {
        it.suitability.edgeGrain.rating == Rating.RECOMMENDED &&
          it.suitability.endGrain.rating == Rating.RECOMMENDED
      },
    )
    assertTrue(vm.visible.value.isNotEmpty())
    vm.setFilter(WoodFilter.RECOMMENDED_ONLY, false)

    vm.setFilter(WoodFilter.HIDE_NOT_SUITABLE, true)
    assertTrue(
      vm.visible.value.none {
        it.suitability.edgeGrain.rating == Rating.NOT_SUITABLE ||
          it.suitability.endGrain.rating == Rating.NOT_SUITABLE
      },
    )
    assertTrue(vm.visible.value.size < WoodCatalog.builtIns.size + 1)
  }

  @Test
  fun saveCustomAddsThenReplacesById() = runTest {
    val platform = FakePlatform()
    val vm = WoodsViewModel(platform.store)

    vm.saveCustom(custom("custom-1", janka = 1200))
    vm.saveCustom(custom("custom-1", janka = 500))

    assertEquals(500, vm.woods.value.byId("custom-1")?.jankaLbf)
    assertEquals(1, vm.woods.value.customs.size)
    assertEquals(listOf("custom-1"), platform.store.customWoods.map { it.id })
  }

  @Test
  fun deleteCustom() = runTest {
    val platform = FakePlatform()
    val vm = WoodsViewModel(platform.store)
    vm.saveCustom(custom("custom-1"))
    vm.saveCustom(custom("custom-2"))

    vm.deleteCustom("custom-1")

    assertEquals(listOf("custom-2"), vm.woods.value.customs.map { it.id })
    assertEquals(listOf("custom-2"), platform.store.customWoods.map { it.id })
  }

  @Test
  fun loadsSavedCustomWoods() = runTest {
    val platform = FakePlatform()
    platform.store.customWoods = listOf(custom("custom-9"))

    val vm = WoodsViewModel(platform.store)

    assertEquals(listOf("custom-9"), vm.woods.value.customs.map { it.id })
  }
}
