package com.example.data.remote

import com.example.data.model.GeoPoint
import com.example.data.model.PlaceCategory
import com.example.data.model.PlaceSearchResult
import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

/**
 * Google Maps Places Autocomplete & Geocoding API.
 */
interface GooglePlacesApi {

    @GET("maps/api/place/autocomplete/json")
    suspend fun autocomplete(
        @Query("input") input: String,
        @Query("key") apiKey: String,
        @Query("location") location: String? = null,
        @Query("radius") radiusMeters: Int = 50000,
        @Query("components") components: String = "country:in",
        @Query("language") language: String = "en"
    ): GoogleAutocompleteResponse

    @GET("maps/api/geocode/json")
    suspend fun geocodeByPlaceId(
        @Query("place_id") placeId: String,
        @Query("key") apiKey: String
    ): GoogleGeocodeResponse

    @GET("maps/api/geocode/json")
    suspend fun geocodeByAddress(
        @Query("address") address: String,
        @Query("key") apiKey: String,
        @Query("components") components: String = "country:IN"
    ): GoogleGeocodeResponse
}

@JsonClass(generateAdapter = true)
data class GoogleAutocompleteResponse(
    val status: String? = null,
    val predictions: List<GooglePlacePrediction> = emptyList(),
    @Json(name = "error_message") val errorMessage: String? = null
)

@JsonClass(generateAdapter = true)
data class GooglePlacePrediction(
    @Json(name = "place_id") val placeId: String? = null,
    val description: String? = null,
    @Json(name = "structured_formatting") val structuredFormatting: GoogleStructuredFormatting? = null,
    val types: List<String> = emptyList()
)

@JsonClass(generateAdapter = true)
data class GoogleStructuredFormatting(
    @Json(name = "main_text") val mainText: String? = null,
    @Json(name = "secondary_text") val secondaryText: String? = null
)

@JsonClass(generateAdapter = true)
data class GoogleGeocodeResponse(
    val status: String? = null,
    val results: List<GoogleGeocodeResult> = emptyList(),
    @Json(name = "error_message") val errorMessage: String? = null
)

@JsonClass(generateAdapter = true)
data class GoogleGeocodeResult(
    @Json(name = "place_id") val placeId: String? = null,
    @Json(name = "formatted_address") val formattedAddress: String? = null,
    val geometry: GoogleGeocodeGeometry? = null
)

@JsonClass(generateAdapter = true)
data class GoogleGeocodeGeometry(
    val location: GoogleLatLng? = null
)

@JsonClass(generateAdapter = true)
data class GoogleLatLng(
    val lat: Double = 0.0,
    val lng: Double = 0.0
)

/**
 * Photon OpenStreetMap Geocoding API (Keyless, Worldwide + India).
 */
interface PhotonGeocodingApi {

    @GET("api/")
    suspend fun search(
        @Query("q") query: String,
        @Query("lat") lat: Double? = null,
        @Query("lon") lon: Double? = null,
        @Query("limit") limit: Int = 10,
        @Query("lang") lang: String = "en"
    ): PhotonResponse
}

@JsonClass(generateAdapter = true)
data class PhotonResponse(
    val features: List<PhotonFeature> = emptyList()
)

@JsonClass(generateAdapter = true)
data class PhotonFeature(
    val geometry: PhotonGeometry? = null,
    val properties: PhotonProperties? = null
)

@JsonClass(generateAdapter = true)
data class PhotonGeometry(
    /** [longitude, latitude] */
    val coordinates: List<Double> = emptyList()
)

@JsonClass(generateAdapter = true)
data class PhotonProperties(
    val name: String? = null,
    val street: String? = null,
    val district: String? = null,
    val city: String? = null,
    val state: String? = null,
    val country: String? = null,
    @Json(name = "osm_key") val osmKey: String? = null,
    @Json(name = "osm_value") val osmValue: String? = null
)

/**
 * OpenStreetMap Nominatim Search API for deep address & locality search.
 */
interface NominatimSearchApi {

    @GET("search")
    suspend fun search(
        @Query("q") query: String,
        @Query("format") format: String = "jsonv2",
        @Query("addressdetails") addressdetails: Int = 1,
        @Query("countrycodes") countrycodes: String = "in",
        @Query("limit") limit: Int = 10,
        @Query("accept-language") language: String = "en"
    ): List<NominatimResultItem>
}

@JsonClass(generateAdapter = true)
data class NominatimResultItem(
    @Json(name = "place_id") val placeId: Long? = null,
    val name: String? = null,
    @Json(name = "display_name") val displayName: String? = null,
    val lat: String? = null,
    val lon: String? = null,
    val category: String? = null,
    val type: String? = null,
    val address: NominatimAddress? = null
)

@JsonClass(generateAdapter = true)
data class NominatimAddress(
    val road: String? = null,
    val suburb: String? = null,
    @Json(name = "city_district") val cityDistrict: String? = null,
    val city: String? = null,
    val state: String? = null,
    val country: String? = null
)

/**
 * TomTom Fuzzy Search API (POI, Street, Point Address & Locality index).
 */
interface TomTomSearchApi {

    @GET("search/2/search/{query}.json")
    suspend fun search(
        @Path("query") query: String,
        @Query("key") apiKey: String,
        @Query("lat") lat: Double? = null,
        @Query("lon") lon: Double? = null,
        @Query("radius") radiusMeters: Int = 50000,
        @Query("limit") limit: Int = 15,
        @Query("countrySet") countrySet: String = "IN",
        @Query("typeahead") typeahead: Boolean = true,
        @Query("idxSet") idxSet: String = "POI,PAD,Str,XStr,Geo,Addr",
        @Query("language") language: String = "en-GB"
    ): TomTomSearchResponse
}

@JsonClass(generateAdapter = true)
data class TomTomSearchResponse(
    val results: List<TomTomSearchResultItem> = emptyList()
)

@JsonClass(generateAdapter = true)
data class TomTomSearchResultItem(
    val id: String? = null,
    val type: String? = null,
    val poi: TomTomPoi? = null,
    val address: TomTomAddress? = null,
    val position: TomTomPosition? = null
)

@JsonClass(generateAdapter = true)
data class TomTomPoi(
    val name: String? = null,
    val categories: List<String> = emptyList()
)

@JsonClass(generateAdapter = true)
data class TomTomAddress(
    val freeformAddress: String? = null,
    val streetNumber: String? = null,
    val streetName: String? = null,
    val municipalitySubdivision: String? = null,
    val municipality: String? = null,
    val countrySubdivision: String? = null,
    val postalCode: String? = null
)

@JsonClass(generateAdapter = true)
data class TomTomPosition(
    val lat: Double = 0.0,
    val lon: Double = 0.0
)

/**
 * Converts a Google prediction to [PlaceSearchResult].
 */
fun GooglePlacePrediction.toPlaceSearchResult(idSuffix: Int): PlaceSearchResult? {
    val mainText = structuredFormatting?.mainText?.takeIf { it.isNotBlank() }
        ?: description?.split(",")?.firstOrNull()?.trim()?.takeIf { it.isNotBlank() }
        ?: return null

    val secondary = structuredFormatting?.secondaryText?.takeIf { it.isNotBlank() }
        ?: description?.split(",")?.drop(1)?.joinToString(",")?.trim()
        ?: "Location"

    val category = mapGoogleTypesToCategory(types, mainText)

    return PlaceSearchResult(
        id = placeId ?: "gplace_$idSuffix",
        name = mainText,
        secondaryText = secondary,
        category = category,
        coordinates = GeoPoint(0.0, 0.0) // Resolved via geocoding when selected
    )
}

/**
 * Converts a Photon feature to [PlaceSearchResult].
 */
fun PhotonFeature.toPlaceSearchResult(idSuffix: Int): PlaceSearchResult? {
    val coords = geometry?.coordinates ?: return null
    if (coords.size < 2) return null
    val lng = coords[0]
    val lat = coords[1]

    val props = properties ?: return null
    val name = props.name?.takeIf { it.isNotBlank() }
        ?: props.street?.takeIf { it.isNotBlank() }
        ?: return null

    val subtitleParts = listOfNotNull(
        props.street?.takeIf { it != name },
        props.district?.takeIf { it != name },
        props.city,
        props.state
    ).distinct()

    val subtitle = if (subtitleParts.isNotEmpty()) {
        subtitleParts.joinToString(", ")
    } else {
        props.country ?: "Location"
    }

    val category = mapOsmToCategory(props.osmKey, props.osmValue, name)

    return PlaceSearchResult(
        id = "osm_${lat}_${lng}_$idSuffix",
        name = name,
        secondaryText = subtitle,
        category = category,
        coordinates = GeoPoint(latitude = lat, longitude = lng)
    )
}

/**
 * Converts a Nominatim result item to [PlaceSearchResult].
 */
fun NominatimResultItem.toPlaceSearchResult(idSuffix: Int): PlaceSearchResult? {
    val latVal = lat?.toDoubleOrNull() ?: return null
    val lonVal = lon?.toDoubleOrNull() ?: return null

    val mainName = name?.takeIf { it.isNotBlank() }
        ?: address?.road?.takeIf { it.isNotBlank() }
        ?: displayName?.split(",")?.firstOrNull()?.trim()?.takeIf { it.isNotBlank() }
        ?: return null

    val subtitleParts = listOfNotNull(
        address?.suburb?.takeIf { it != mainName },
        address?.cityDistrict?.takeIf { it != mainName },
        address?.city,
        address?.state
    ).distinct()

    val subtitle = if (subtitleParts.isNotEmpty()) {
        subtitleParts.joinToString(", ")
    } else {
        displayName?.split(",")?.drop(1)?.joinToString(",")?.trim() ?: "Location"
    }

    val cat = mapOsmToCategory(category, type, mainName)

    return PlaceSearchResult(
        id = "nom_${placeId ?: idSuffix}_$idSuffix",
        name = mainName,
        secondaryText = subtitle,
        category = cat,
        coordinates = GeoPoint(latitude = latVal, longitude = lonVal)
    )
}

/**
 * Converts a TomTom result item to [PlaceSearchResult].
 */
fun TomTomSearchResultItem.toPlaceSearchResult(idSuffix: Int): PlaceSearchResult? {
    val pos = position ?: return null
    val poiName = poi?.name?.takeIf { it.isNotBlank() }
    val freeform = address?.freeformAddress?.takeIf { it.isNotBlank() }

    val name: String
    val subtitle: String

    if (poiName != null) {
        name = poiName
        subtitle = freeform ?: listOfNotNull(
            address?.municipalitySubdivision,
            address?.municipality,
            address?.countrySubdivision
        ).joinToString(", ")
    } else if (freeform != null) {
        val parts = freeform.split(", ")
        name = parts.firstOrNull() ?: freeform
        subtitle = if (parts.size > 1) {
            parts.drop(1).joinToString(", ")
        } else {
            listOfNotNull(
                address?.municipalitySubdivision,
                address?.municipality,
                address?.countrySubdivision
            ).joinToString(", ")
        }
    } else {
        return null
    }

    val category = mapPoiToCategory(poi?.categories.orEmpty(), name)

    return PlaceSearchResult(
        id = id ?: "tt_${pos.lat}_${pos.lon}_$idSuffix",
        name = name,
        secondaryText = subtitle,
        category = category,
        coordinates = GeoPoint(latitude = pos.lat, longitude = pos.lon)
    )
}

private fun mapGoogleTypesToCategory(types: List<String>, name: String): PlaceCategory {
    val lower = types.map { it.lowercase() }
    val lowerName = name.lowercase()
    return when {
        lower.contains("airport") || lowerName.contains("airport") -> PlaceCategory.AIRPORT
        lower.contains("transit_station") || lower.contains("subway_station") || lower.contains("train_station") || lower.contains("bus_station") || lowerName.contains("metro") || lowerName.contains("station") -> PlaceCategory.STATION
        lower.contains("university") || lower.contains("school") || lower.contains("secondary_school") || lowerName.contains("college") -> PlaceCategory.COLLEGE
        lower.contains("hospital") || lower.contains("doctor") || lower.contains("health") || lowerName.contains("hospital") || lowerName.contains("clinic") -> PlaceCategory.HOSPITAL
        lower.contains("shopping_mall") || lower.contains("store") || lower.contains("supermarket") || lowerName.contains("mall") || lowerName.contains("market") -> PlaceCategory.MARKET
        lower.contains("establishment") && (lowerName.contains("tech park") || lowerName.contains("tower") || lowerName.contains("office")) -> PlaceCategory.WORK
        else -> PlaceCategory.RECENT
    }
}

private fun mapOsmToCategory(osmKey: String?, osmValue: String?, name: String): PlaceCategory {
    val key = (osmKey ?: "").lowercase()
    val value = (osmValue ?: "").lowercase()
    val lowerName = name.lowercase()

    return when {
        key == "aeroway" || value.contains("airport") || lowerName.contains("airport") || lowerName.contains("terminal") -> PlaceCategory.AIRPORT
        key == "railway" || value.contains("station") || value.contains("subway") || lowerName.contains("station") || lowerName.contains("metro") -> PlaceCategory.STATION
        key == "amenity" && (value == "university" || value == "college" || value == "school") || lowerName.contains("college") || lowerName.contains("university") || lowerName.contains("campus") -> PlaceCategory.COLLEGE
        key == "amenity" && (value == "hospital" || value == "clinic" || value == "doctors") || lowerName.contains("hospital") || lowerName.contains("aiims") || lowerName.contains("medanta") -> PlaceCategory.HOSPITAL
        key == "shop" || value == "marketplace" || value == "mall" || lowerName.contains("mall") || lowerName.contains("bazaar") || lowerName.contains("market") -> PlaceCategory.MARKET
        key == "office" || value == "commercial" || lowerName.contains("tech park") || lowerName.contains("cyber") || lowerName.contains("towers") -> PlaceCategory.WORK
        else -> PlaceCategory.RECENT
    }
}

private fun mapPoiToCategory(categories: List<String>, name: String): PlaceCategory {
    val joined = categories.joinToString(" ").lowercase()
    val lowerName = name.lowercase()
    return when {
        joined.contains("airport") || lowerName.contains("airport") -> PlaceCategory.AIRPORT
        joined.contains("railway") || joined.contains("station") || joined.contains("transit") || lowerName.contains("metro") || lowerName.contains("station") -> PlaceCategory.STATION
        joined.contains("university") || joined.contains("college") || joined.contains("school") || lowerName.contains("college") -> PlaceCategory.COLLEGE
        joined.contains("hospital") || joined.contains("health") || lowerName.contains("hospital") -> PlaceCategory.HOSPITAL
        joined.contains("mall") || joined.contains("shop") || joined.contains("market") || lowerName.contains("mall") -> PlaceCategory.MARKET
        joined.contains("commercial") || joined.contains("business") || lowerName.contains("cyber") -> PlaceCategory.WORK
        else -> PlaceCategory.RECENT
    }
}
