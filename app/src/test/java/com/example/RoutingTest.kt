package com.example

import com.example.data.remote.OsrmAnnotation
import com.example.data.remote.PolylineCodec
import com.example.data.remote.TomTomSection
import com.example.data.remote.toCongestionSpans
import com.example.data.model.TrafficLevel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Routing decode and congestion mapping.
 *
 * These are the pieces that turn provider responses into the geometry the map
 * draws, so a silent failure here is what puts the route line back "in the air".
 */
class RoutingTest {

  @Test
  fun `polyline decodes to known coordinates`() {
    // Reference string from Google's Encoded Polyline Algorithm documentation.
    val points = PolylineCodec.decode("_p~iF~ps|U_ulLnnqC_mqNvxq`@")

    assertEquals(3, points.size)
    assertEquals(38.5, points[0].latitude, 0.0001)
    assertEquals(-120.2, points[0].longitude, 0.0001)
    assertEquals(40.7, points[1].latitude, 0.0001)
    assertEquals(-120.95, points[1].longitude, 0.0001)
    assertEquals(43.252, points[2].latitude, 0.0001)
    assertEquals(-126.453, points[2].longitude, 0.0001)
  }

  @Test
  fun `polyline decoder tolerates malformed input`() {
    // Truncated and non-polyline strings must not loop or throw.
    assertTrue(PolylineCodec.decode("").isEmpty())
    assertTrue(PolylineCodec.decode("_p~iF").isEmpty())
  }

  @Test
  fun `tomtom sections map to fractional congestion spans`() {
    val sections = listOf(
      TomTomSection(
        startPointIndex = 0,
        endPointIndex = 50,
        sectionType = "TRAFFIC",
        magnitudeOfDelay = 3
      ),
      // Non-traffic sections carry no congestion meaning and must be dropped.
      TomTomSection(startPointIndex = 50, endPointIndex = 100, sectionType = "TOLL_ROAD")
    )

    val spans = sections.toCongestionSpans(totalPoints = 101)

    assertEquals(1, spans.size)
    assertEquals(0f, spans[0].startFraction, 0.001f)
    assertEquals(0.5f, spans[0].endFraction, 0.001f)
    assertEquals(TrafficLevel.SEVERE, spans[0].level)
  }

  @Test
  fun `osrm speeds merge into contiguous spans`() {
    // Two slow samples then two fast ones: one congested span, not four.
    val annotation = OsrmAnnotation(speed = listOf(2.0, 2.5, 15.0, 16.0))

    val spans = annotation.toCongestionSpans(totalPoints = 4)

    assertEquals(1, spans.size)
    assertEquals(TrafficLevel.SEVERE, spans[0].level)
    assertTrue(spans[0].endFraction > spans[0].startFraction)
  }

  @Test
  fun `free flowing routes produce no congestion overlay`() {
    val annotation = OsrmAnnotation(speed = listOf(18.0, 19.0, 20.0))

    // ~65-72 km/h: nothing to highlight, so the base route colour stays visible.
    assertTrue(annotation.toCongestionSpans(totalPoints = 3).isEmpty())
  }
}
