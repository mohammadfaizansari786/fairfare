package com.example

import com.example.data.model.PlaceCategory
import com.example.data.remote.PhotonFeature
import com.example.data.remote.PhotonGeometry
import com.example.data.remote.PhotonProperties
import com.example.data.remote.TomTomAddress
import com.example.data.remote.TomTomPoi
import com.example.data.remote.TomTomPosition
import com.example.data.remote.TomTomSearchResultItem
import com.example.data.remote.toPlaceSearchResult
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class GeocodingTest {

    @Test
    fun `photon feature maps to place search result with correct category and coordinates`() {
        val feature = PhotonFeature(
            geometry = PhotonGeometry(coordinates = listOf(80.9450, 26.8530)),
            properties = PhotonProperties(
                name = "Charbagh Metro Station",
                street = "Station Road",
                district = "Lucknow",
                city = "Lucknow",
                state = "Uttar Pradesh",
                country = "India",
                osmKey = "railway",
                osmValue = "station"
            )
        )

        val result = feature.toPlaceSearchResult(1)

        assertNotNull(result)
        assertEquals("Charbagh Metro Station", result?.name)
        assertEquals(26.8530, result?.coordinates?.latitude ?: 0.0, 0.0001)
        assertEquals(80.9450, result?.coordinates?.longitude ?: 0.0, 0.0001)
        assertEquals(PlaceCategory.STATION, result?.category)
    }

    @Test
    fun `photon hospital amenity maps to HOSPITAL category`() {
        val feature = PhotonFeature(
            geometry = PhotonGeometry(coordinates = listOf(77.2100, 28.5672)),
            properties = PhotonProperties(
                name = "AIIMS Hospital",
                city = "New Delhi",
                state = "Delhi",
                country = "India",
                osmKey = "amenity",
                osmValue = "hospital"
            )
        )

        val result = feature.toPlaceSearchResult(2)

        assertNotNull(result)
        assertEquals("AIIMS Hospital", result?.name)
        assertEquals(PlaceCategory.HOSPITAL, result?.category)
    }

    @Test
    fun `photon feature with empty coordinates returns null safely`() {
        val feature = PhotonFeature(
            geometry = PhotonGeometry(coordinates = emptyList()),
            properties = PhotonProperties(name = "Unknown Place")
        )

        val result = feature.toPlaceSearchResult(3)
        assertNull(result)
    }

    @Test
    fun `tomtom search result maps accurately`() {
        val item = TomTomSearchResultItem(
            id = "tt_123",
            poi = TomTomPoi(
                name = "Kempegowda Intl Airport",
                categories = listOf("Airport")
            ),
            address = TomTomAddress(
                freeformAddress = "Devanahalli, Bengaluru",
                municipality = "Bengaluru",
                countrySubdivision = "Karnataka"
            ),
            position = TomTomPosition(lat = 13.1986, lon = 77.7066)
        )

        val result = item.toPlaceSearchResult(1)

        assertNotNull(result)
        assertEquals("Kempegowda Intl Airport", result?.name)
        assertEquals(13.1986, result?.coordinates?.latitude ?: 0.0, 0.0001)
        assertEquals(77.7066, result?.coordinates?.longitude ?: 0.0, 0.0001)
        assertEquals(PlaceCategory.AIRPORT, result?.category)
    }

    @Test
    fun `nominatim result maps to place search result`() {
        val item = com.example.data.remote.NominatimResultItem(
            placeId = 98765,
            name = "Phoenix Palassio",
            displayName = "Phoenix Palassio, Amar Shaheed Path, Gomti Nagar, Lucknow, Uttar Pradesh, 226010, India",
            lat = "26.8123",
            lon = "80.9988",
            category = "shop",
            type = "mall",
            address = com.example.data.remote.NominatimAddress(
                road = "Amar Shaheed Path",
                suburb = "Gomti Nagar",
                city = "Lucknow",
                state = "Uttar Pradesh"
            )
        )

        val result = item.toPlaceSearchResult(1)

        assertNotNull(result)
        assertEquals("Phoenix Palassio", result?.name)
        assertEquals(26.8123, result?.coordinates?.latitude ?: 0.0, 0.0001)
        assertEquals(80.9988, result?.coordinates?.longitude ?: 0.0, 0.0001)
        assertEquals(PlaceCategory.MARKET, result?.category)
    }

    @Test
    fun `google place prediction maps to place search result`() {
        val item = com.example.data.remote.GooglePlacePrediction(
            placeId = "ChIJN1t_tDeuEmsRUsoyG83frY4",
            description = "Ansari House, Vibhuti Khand, Gomti Nagar, Lucknow, Uttar Pradesh",
            structuredFormatting = com.example.data.remote.GoogleStructuredFormatting(
                mainText = "Ansari House",
                secondaryText = "Vibhuti Khand, Gomti Nagar, Lucknow, Uttar Pradesh"
            ),
            types = listOf("premise", "establishment")
        )

        val result = item.toPlaceSearchResult(1)

        assertNotNull(result)
        assertEquals("Ansari House", result?.name)
        assertEquals("Vibhuti Khand, Gomti Nagar, Lucknow, Uttar Pradesh", result?.secondaryText)
        assertEquals("ChIJN1t_tDeuEmsRUsoyG83frY4", result?.id)
    }

    @Test
    fun `photon feature with airport category maps correctly`() {
        val feature = PhotonFeature(
            geometry = PhotonGeometry(coordinates = listOf(77.1000, 28.5562)),
            properties = PhotonProperties(
                name = "Indira Gandhi International Airport",
                city = "New Delhi",
                state = "Delhi",
                country = "India",
                osmKey = "aeroway",
                osmValue = "aerodrome"
            )
        )

        val result = feature.toPlaceSearchResult(4)
        assertNotNull(result)
        assertEquals(PlaceCategory.AIRPORT, result?.category)
    }

    @Test
    fun `photon feature with university maps to COLLEGE category`() {
        val feature = PhotonFeature(
            geometry = PhotonGeometry(coordinates = listOf(77.1920, 28.5450)),
            properties = PhotonProperties(
                name = "IIT Delhi Campus",
                city = "New Delhi",
                state = "Delhi",
                country = "India",
                osmKey = "amenity",
                osmValue = "university"
            )
        )

        val result = feature.toPlaceSearchResult(5)
        assertNotNull(result)
        assertEquals(PlaceCategory.COLLEGE, result?.category)
    }
}
