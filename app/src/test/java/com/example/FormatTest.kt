package com.example

import com.example.ui.util.formatDuration
import com.example.ui.util.formatKm
import com.example.ui.util.formatRupeeRange
import com.example.ui.util.formatRupees
import com.example.ui.util.formatSignedPercent
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Formatting is the last thing between a correct calculation and a number the
 * user reads, so it is covered directly. These also lock in the locale-independent
 * behaviour: the previous code used String.format without a Locale, which rendered
 * "16,4 km" on comma-decimal devices.
 */
class FormatTest {

  @Test
  fun `rupees round to whole units`() {
    assertEquals("₹120", formatRupees(119.6))
    assertEquals("₹119", formatRupees(119.4))
    assertEquals("₹0", formatRupees(0.0))
  }

  @Test
  fun `rupees group thousands`() {
    assertEquals("₹1,250", formatRupees(1250.0))
  }

  @Test
  fun `rupee range collapses when both ends match`() {
    assertEquals("₹95 – ₹110", formatRupeeRange(95.0, 110.0))
    assertEquals("₹100", formatRupeeRange(100.0, 100.4))
  }

  @Test
  fun `distance always uses a dot decimal separator`() {
    assertEquals("16.4 km", formatKm(16.44))
    assertEquals("0.5 km", formatKm(0.5))
  }

  @Test
  fun `non-finite values do not leak NaN into the ui`() {
    assertEquals("0.0 km", formatKm(Double.NaN))
    assertEquals("₹0", formatRupees(Double.POSITIVE_INFINITY))
  }

  @Test
  fun `duration switches to hours past sixty minutes`() {
    assertEquals("8 min", formatDuration(8))
    assertEquals("1 h", formatDuration(60))
    assertEquals("1 h 05 min", formatDuration(65))
    assertEquals("2 h 30 min", formatDuration(150))
  }

  @Test
  fun `negative duration is clamped rather than rendered`() {
    assertEquals("0 min", formatDuration(-5))
  }

  @Test
  fun `percentages carry an explicit sign`() {
    assertEquals("+34%", formatSignedPercent(34.2))
    assertEquals("-12%", formatSignedPercent(-12.4))
    assertEquals("0%", formatSignedPercent(0.0))
  }
}
