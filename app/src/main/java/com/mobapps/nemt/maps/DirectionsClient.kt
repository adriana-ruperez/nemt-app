package com.mobapps.nemt.maps

import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.PolyUtil
import com.google.maps.android.SphericalUtil
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.net.URLEncoder
import java.net.URL
import java.util.Locale
import javax.net.ssl.HttpsURLConnection

/**
 * Google Directions API (REST). Enable "Directions API" for your key in Google Cloud Console.
 */
object DirectionsClient {

    suspend fun fetchDrivingDirections(
        origin: LatLng,
        destination: LatLng,
        apiKey: String
    ): TripDirections? = withContext(Dispatchers.IO) {
        if (apiKey.isBlank()) return@withContext null
        val o = "${origin.latitude},${origin.longitude}"
        val d = "${destination.latitude},${destination.longitude}"
        val urlStr =
            "https://maps.googleapis.com/maps/api/directions/json?" +
                "origin=${URLEncoder.encode(o, "UTF-8")}" +
                "&destination=${URLEncoder.encode(d, "UTF-8")}" +
                "&mode=driving&key=${URLEncoder.encode(apiKey, "UTF-8")}"
        val conn = URL(urlStr).openConnection() as HttpsURLConnection
        try {
            conn.connectTimeout = 20_000
            conn.readTimeout = 20_000
            conn.requestMethod = "GET"
            conn.connect()
            val stream = if (conn.responseCode in 200..299) {
                conn.inputStream
            } else {
                conn.errorStream ?: return@withContext null
            }
            val body = stream.bufferedReader().use { it.readText() }
            val json = JSONObject(body)
            val status = json.optString("status")
            if (status != "OK") return@withContext null
            val routes = json.optJSONArray("routes") ?: return@withContext null
            val route = routes.optJSONObject(0) ?: return@withContext null
            val poly = route.optJSONObject("overview_polyline") ?: return@withContext null
            val encoded = poly.optString("points", "").ifBlank { return@withContext null }
            val points = PolyUtil.decode(encoded).takeIf { it.size >= 2 } ?: return@withContext null
            val legs = route.optJSONArray("legs") ?: return@withContext null
            val legSum = summarizeLegs(legs)
            val turns = extractTurns(legs)
            TripDirections(
                polylinePoints = points,
                durationSeconds = legSum.durationSeconds,
                durationText = legSum.durationText,
                distanceMeters = legSum.distanceMeters,
                distanceText = legSum.distanceText,
                nextStepHint = legSum.nextStepHint,
                turns = turns
            )
        } catch (_: Exception) {
            null
        } finally {
            conn.disconnect()
        }
    }

    fun straightLineFallback(origin: LatLng, destination: LatLng): TripDirections {
        val meters = SphericalUtil.computeDistanceBetween(origin, destination)
        val distanceM = meters.toInt().coerceAtLeast(1)
        val km = meters / 1000.0
        val distanceText = String.format(Locale.US, "%.1f km", km)
        val avgMetersPerSec = 8.0
        val durationSec = (meters / avgMetersPerSec).toInt().coerceAtLeast(60)
        val minutes = (durationSec + 59) / 60
        return TripDirections(
            polylinePoints = listOf(origin, destination),
            durationSeconds = durationSec,
            durationText = "~$minutes min",
            distanceMeters = distanceM,
            distanceText = distanceText,
            nextStepHint = null,
            turns = emptyList()
        )
    }

    private data class LegSummary(
        val durationSeconds: Int,
        val durationText: String,
        val distanceMeters: Int,
        val distanceText: String,
        val nextStepHint: String?
    )

    private fun summarizeLegs(legs: JSONArray): LegSummary {
        var totalDurationSec = 0
        var totalMeters = 0
        val durationTexts = mutableListOf<String>()
        val distanceTexts = mutableListOf<String>()
        var nextHint: String? = null
        for (i in 0 until legs.length()) {
            val leg = legs.optJSONObject(i) ?: continue
            leg.optJSONObject("duration")?.let {
                totalDurationSec += it.optInt("value", 0)
                durationTexts.add(it.optString("text"))
            }
            leg.optJSONObject("distance")?.let {
                totalMeters += it.optInt("value", 0)
                distanceTexts.add(it.optString("text"))
            }
            if (nextHint == null) {
                val steps = leg.optJSONArray("steps")
                if (steps != null && steps.length() > 0) {
                    nextHint = steps.optJSONObject(0)?.optString("html_instructions")?.let(::stripHtml)
                }
            }
        }
        val durationText = if (durationTexts.size == 1) {
            durationTexts.first()
        } else {
            formatDurationMinutes(totalDurationSec)
        }
        val distanceText = if (distanceTexts.size == 1) {
            distanceTexts.first()
        } else {
            formatDistanceKm(totalMeters)
        }
        return LegSummary(
            durationSeconds = totalDurationSec,
            durationText = durationText,
            distanceMeters = totalMeters.coerceAtLeast(1),
            distanceText = distanceText,
            nextStepHint = nextHint
        )
    }

    private fun formatDurationMinutes(totalSeconds: Int): String {
        val m = ((totalSeconds + 59) / 60).coerceAtLeast(1)
        return "$m min"
    }

    private fun formatDistanceKm(meters: Int): String {
        val km = meters / 1000.0
        return String.format(Locale.US, "%.1f km", km)
    }

    private fun extractTurns(legs: JSONArray): List<TripTurn> {
        val out = mutableListOf<TripTurn>()
        for (i in 0 until legs.length()) {
            val leg = legs.optJSONObject(i) ?: continue
            val steps = leg.optJSONArray("steps") ?: continue
            for (s in 0 until steps.length()) {
                val step = steps.optJSONObject(s) ?: continue
                val start = step.optJSONObject("start_location") ?: continue
                val lat = start.optDouble("lat", Double.NaN)
                val lng = start.optDouble("lng", Double.NaN)
                if (lat.isNaN() || lng.isNaN()) continue
                val instruction = stripHtml(step.optString("html_instructions"))
                if (instruction.isBlank()) continue
                val maneuver = step.optString("maneuver").ifBlank { null }
                // Prefer explicit maneuvers and roundabouts.
                if (maneuver != null || instruction.contains("roundabout", ignoreCase = true)) {
                    out += TripTurn(
                        position = LatLng(lat, lng),
                        maneuver = maneuver,
                        instruction = instruction
                    )
                }
                if (out.size >= 8) return out
            }
        }
        return out
    }

    private fun stripHtml(html: String): String =
        html.replace(Regex("<[^>]+>"), "").replace("&nbsp;", " ").trim()
}
