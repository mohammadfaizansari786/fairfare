package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

  @Test
  fun `read string from context`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val appName = context.getString(R.string.app_name)
    assertEquals("FairFare", appName)
  }

  @Test
  fun `instant suggestions match active city and do not leak other cities`() {
    val app = ApplicationProvider.getApplicationContext<android.app.Application>()
    val viewModel = com.example.ui.viewmodel.FareViewModel(app)

    // Default city is Lucknow
    val lucknowResults = viewModel.searchPlaceSuggestionsInstant("Hazratganj")
    org.junit.Assert.assertTrue(lucknowResults.isNotEmpty())
    assertEquals("Hazratganj", lucknowResults.first().name)

    // Searching "Airport" in Lucknow returns Lucknow CCS Airport, not IGI or BOM
    val airportResults = viewModel.searchPlaceSuggestionsInstant("Airport")
    org.junit.Assert.assertTrue(airportResults.isNotEmpty())
    org.junit.Assert.assertTrue(airportResults.first().name.contains("Chaudhary Charan Singh", ignoreCase = true) || airportResults.first().secondaryText.contains("Lucknow", ignoreCase = true))

    // Searching something specific to Kolkata or Mumbai while in Lucknow returns empty instant results (does not leak)
    val kolkataSearch = viewModel.searchPlaceSuggestionsInstant("Howrah")
    org.junit.Assert.assertTrue(kolkataSearch.isEmpty())
  }

  @Test
  fun `instant suggestions adapt when city changes`() {
    val app = ApplicationProvider.getApplicationContext<android.app.Application>()
    val viewModel = com.example.ui.viewmodel.FareViewModel(app)

    val delhi = com.example.data.local.InitialData.CITIES.first { it.name.contains("Delhi") }
    viewModel.selectCity(delhi)

    val delhiResults = viewModel.searchPlaceSuggestionsInstant("Rajiv Chowk")
    org.junit.Assert.assertTrue(delhiResults.isNotEmpty())
    org.junit.Assert.assertTrue(delhiResults.first().name.contains("Rajiv Chowk", ignoreCase = true))
  }
}
