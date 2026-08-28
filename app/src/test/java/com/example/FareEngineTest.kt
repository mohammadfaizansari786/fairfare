package com.example

import com.example.data.local.InitialData
import com.example.data.model.OverchargeCategory
import com.example.data.model.TransportType
import com.example.engine.FareCalculatorEngine
import com.example.engine.OverchargeEngine
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Fare and overcharge engine behaviour.
 *
 * These are the calculations the whole app is built on, and they were previously
 * untested — the only unit test in the project asserted that 2 + 2 == 4.
 */
class FareEngineTest {

  private val autoTariff = InitialData.DEFAULT_TARIFFS.first {
    it.transportType == TransportType.AUTO_RICKSHAW
  }

  @Test
  fun `base distance is not charged per km`() {
    val result = FareCalculatorEngine.calculateFare(
      tariff = autoTariff,
      distanceKm = autoTariff.baseDistanceKm,
      forceNightMode = false
    )

    assertEquals(0.0, result.distanceCharge, 0.001)
    assertEquals(autoTariff.baseFare, result.estimatedFare, 1.0)
  }

  @Test
  fun `distance beyond the base is charged at the per km rate`() {
    val result = FareCalculatorEngine.calculateFare(
      tariff = autoTariff,
      distanceKm = autoTariff.baseDistanceKm + 4.0,
      forceNightMode = false
    )

    assertEquals(4.0 * autoTariff.perKmRate, result.distanceCharge, 0.001)
  }

  @Test
  fun `night mode adds a surcharge and is reported`() {
    val day = FareCalculatorEngine.calculateFare(
      tariff = autoTariff,
      distanceKm = 8.0,
      forceNightMode = false
    )
    val night = FareCalculatorEngine.calculateFare(
      tariff = autoTariff,
      distanceKm = 8.0,
      forceNightMode = true
    )

    assertTrue(night.estimatedFare > day.estimatedFare)
    assertTrue(night.isNightApplied)
    assertEquals(0.0, day.nightCharge, 0.001)
  }

  @Test
  fun `first luggage item is free`() {
    val oneBag = FareCalculatorEngine.calculateFare(
      tariff = autoTariff,
      distanceKm = 5.0,
      luggageCount = 1,
      forceNightMode = false
    )
    val twoBags = FareCalculatorEngine.calculateFare(
      tariff = autoTariff,
      distanceKm = 5.0,
      luggageCount = 2,
      forceNightMode = false
    )

    assertEquals(0.0, oneBag.luggageCharge, 0.001)
    assertEquals(autoTariff.luggageRatePerItem, twoBags.luggageCharge, 0.001)
  }

  @Test
  fun `fare never falls below the minimum`() {
    val result = FareCalculatorEngine.calculateFare(
      tariff = autoTariff,
      distanceKm = 0.1,
      forceNightMode = false
    )

    assertTrue(result.fareRangeMin >= autoTariff.minFare)
  }

  @Test
  fun `comparison marks exactly one cheapest option`() {
    val tariffs = InitialData.tariffsForCityOrFallback("Lucknow")
    val results = FareCalculatorEngine.compareTransports(
      tariffs = tariffs,
      distanceKm = 10.0,
      forceNightMode = false
    )

    assertEquals(1, results.count { it.isCheapest })
    assertTrue(results.first().isCheapest)
  }

  @Test
  fun `a quote inside the fair range is judged fair`() {
    val calculation = FareCalculatorEngine.calculateFare(
      tariff = autoTariff,
      distanceKm = 6.0,
      forceNightMode = false
    )

    val analysis = OverchargeEngine.analyze(
      driverQuote = calculation.fareRangeMax,
      calculation = calculation
    )

    assertEquals(OverchargeCategory.FAIR, analysis.category)
  }

  @Test
  fun `a quote at double the fair maximum is judged significantly high`() {
    val calculation = FareCalculatorEngine.calculateFare(
      tariff = autoTariff,
      distanceKm = 6.0,
      forceNightMode = false
    )

    val analysis = OverchargeEngine.analyze(
      driverQuote = calculation.fareRangeMax * 2.0,
      calculation = calculation
    )

    assertEquals(OverchargeCategory.VERY_HIGH, analysis.category)
    assertTrue(analysis.differenceAmount > 0.0)
  }

  @Test
  fun `tariff lookup falls back to the primary city when data is missing`() {
    val unknown = InitialData.tariffsForCityOrFallback("Atlantis")
    val fallback = InitialData.tariffsForCityOrFallback(InitialData.CITIES.first().name)

    assertTrue(unknown.isNotEmpty())
    assertEquals(fallback.size, unknown.size)
  }

  @Test
  fun `corridor fare calculation accounts for base fare and per km rate accurately`() {
    val lucknowTariffs = InitialData.tariffsForCityOrFallback("Lucknow")
    val routes = com.example.engine.TrafficRouteEngine.generateTrafficRoutes(
      fromName = "Hazratganj",
      toName = "Charbagh",
      baseDistanceKm = 5.0,
      city = "Lucknow",
      tariffs = lucknowTariffs
    )

    assertTrue(routes.isNotEmpty())
    val fastest = routes.first { it.isRecommended }
    // 5.4 km in Lucknow: Auto base 25 (1.5km) + 3.9 * 10.50 = ~66
    assertTrue(fastest.estimatedAutoFare >= 60.0 && fastest.estimatedAutoFare <= 75.0)
    assertTrue(fastest.estimatedCabFare >= 90.0)
  }

  @Test
  fun `official municipal bus fares calculate exact stage slabs for cities`() {
    // Lucknow: 0-3km = 10, 3-6km = 15, 6-10km = 20, 10-15km = 25
    assertEquals(10.0, FareCalculatorEngine.calculateBusFare(2.0, "Lucknow"), 0.01)
    assertEquals(15.0, FareCalculatorEngine.calculateBusFare(5.0, "Lucknow"), 0.01)
    assertEquals(20.0, FareCalculatorEngine.calculateBusFare(8.0, "Lucknow"), 0.01)
    assertEquals(25.0, FareCalculatorEngine.calculateBusFare(12.0, "Lucknow"), 0.01)

    // Delhi: 0-4km = 5, 4-10km = 10, >10km = 15
    assertEquals(5.0, FareCalculatorEngine.calculateBusFare(3.0, "Delhi NCR"), 0.01)
    assertEquals(10.0, FareCalculatorEngine.calculateBusFare(7.0, "Delhi NCR"), 0.01)
    assertEquals(15.0, FareCalculatorEngine.calculateBusFare(14.0, "Delhi NCR"), 0.01)

    // Mumbai: 0-5km = 5, 5-10km = 10, 10-15km = 15
    assertEquals(5.0, FareCalculatorEngine.calculateBusFare(4.0, "Mumbai"), 0.01)
    assertEquals(10.0, FareCalculatorEngine.calculateBusFare(8.0, "Mumbai"), 0.01)
    assertEquals(15.0, FareCalculatorEngine.calculateBusFare(12.0, "Mumbai"), 0.01)

    // Bengaluru: 0-2km = 5, 2-4km = 10, 4-6km = 15, 6-8km = 18
    assertEquals(5.0, FareCalculatorEngine.calculateBusFare(1.5, "Bengaluru"), 0.01)
    assertEquals(10.0, FareCalculatorEngine.calculateBusFare(3.5, "Bengaluru"), 0.01)
    assertEquals(15.0, FareCalculatorEngine.calculateBusFare(5.5, "Bengaluru"), 0.01)
  }
}
