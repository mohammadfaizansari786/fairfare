package com.example.data.remote

import com.example.data.model.GeoPoint

/**
 * Google's Encoded Polyline Algorithm, precision 5.
 *
 * OSRM and most routing providers return geometry in this format rather than as a
 * coordinate array — it is roughly an order of magnitude smaller over the wire.
 *
 * The format stores successive *deltas* as base64-ish chunks: each value is
 * zig-zag encoded (so negatives stay compact), split into 5-bit groups, and each
 * group but the last gets a continuation bit set.
 */
internal object PolylineCodec {

    fun decode(encoded: String): List<GeoPoint> {
        if (encoded.isEmpty()) return emptyList()

        val points = ArrayList<GeoPoint>(encoded.length / 4)
        var index = 0
        var lat = 0
        var lng = 0

        while (index < encoded.length) {
            val latDelta = decodeValue(encoded, index) ?: break
            index = latDelta.nextIndex
            lat += latDelta.value

            val lngDelta = decodeValue(encoded, index) ?: break
            index = lngDelta.nextIndex
            lng += lngDelta.value

            points.add(GeoPoint(lat / 1e5, lng / 1e5))
        }

        return points
    }

    private data class Decoded(val value: Int, val nextIndex: Int)

    private fun decodeValue(encoded: String, startIndex: Int): Decoded? {
        var index = startIndex
        var shift = 0
        var result = 0

        while (index < encoded.length) {
            val chunk = encoded[index].code - 63
            index++
            result = result or ((chunk and 0x1F) shl shift)
            if (chunk < 0x20) {
                // Undo zig-zag: the low bit is the sign.
                val value = if (result and 1 != 0) (result shr 1).inv() else result shr 1
                return Decoded(value, index)
            }
            shift += 5
            // A well-formed value never needs more than six 5-bit groups. Bail out
            // rather than looping on malformed input.
            if (shift > 30) return null
        }

        return null
    }
}
