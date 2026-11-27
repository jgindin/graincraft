package com.jaygindin.graincraft.app.ui.shop

import com.jaygindin.graincraft.app.testutil.FakePlatform
import com.jaygindin.graincraft.core.shop.ShopProfile
import com.jaygindin.graincraft.core.units.Length
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain

@OptIn(ExperimentalCoroutinesApi::class)
class ShopViewModelTest {
  @BeforeTest fun setUp() = Dispatchers.setMain(UnconfinedTestDispatcher())

  @AfterTest fun tearDown() = Dispatchers.resetMain()

  @Test
  fun updateSavesAndResetRestoresDefaults() = runTest {
    val platform = FakePlatform()
    platform.store.shopProfile = ShopProfile(kerf = Length.inches(0, 3, 32))
    val vm = ShopViewModel(platform.store)
    assertEquals(Length.inches(0, 3, 32), vm.profile.value.kerf)

    vm.update(vm.profile.value.copy(glue = "Hide glue"))
    assertEquals("Hide glue", platform.store.shopProfile.glue)

    vm.resetToDefaults()
    assertEquals(ShopProfile(), vm.profile.value)
    assertEquals(ShopProfile(), platform.store.shopProfile)
  }
}
