package com.moham.taxi.data.service

import android.content.Context
import android.location.Address
import android.location.Geocoder
import android.os.Build
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.util.Locale
import kotlin.coroutines.resume

data class PlaceSuggestion(
    val displayName: String,
    val latitude: Double,
    val longitude: Double
)

data class RouteResult(
    val distanceKm: Double,
    val durationSeconds: Double
)

object GeocodingService {
    private const val USER_AGENT = "Mitaxi/1.0 (Android Taxi App)"

    private val cachedCityCoords = java.util.concurrent.ConcurrentHashMap<String, Pair<Double, Double>>()

    private suspend fun getCityCoords(cityName: String): Pair<Double, Double>? = withContext(Dispatchers.IO) {
        val normalized = cityName.trim().lowercase(Locale.ROOT)
        if (normalized.isEmpty()) return@withContext null
        
        cachedCityCoords[normalized]?.let { return@withContext it }
        
        try {
            val encodedCity = URLEncoder.encode(cityName, "UTF-8")
            val countryCode = Locale.getDefault().country.lowercase(Locale.ROOT)
            var urlString = "https://photon.komoot.io/api?q=$encodedCity&limit=1"
            if (countryCode.isNotEmpty()) {
                urlString += "&countrycode=$countryCode"
            }
            val url = URL(urlString)
            val connection = url.openConnection() as HttpURLConnection
            connection.requestMethod = "GET"
            connection.setRequestProperty("User-Agent", USER_AGENT)
            connection.connectTimeout = 5000
            connection.readTimeout = 5000

            val responseCode = connection.responseCode
            if (responseCode == HttpURLConnection.HTTP_OK) {
                val reader = BufferedReader(InputStreamReader(connection.inputStream))
                val response = StringBuilder()
                var line: String?
                while (reader.readLine().also { line = it } != null) {
                    response.append(line)
                }
                reader.close()

                val jsonObject = JSONObject(response.toString())
                val features = jsonObject.optJSONArray("features")
                if (features != null && features.length() > 0) {
                    val feature = features.getJSONObject(0)
                    val geometry = feature.optJSONObject("geometry")
                    val coordinates = geometry?.optJSONArray("coordinates")
                    if (coordinates != null && coordinates.length() >= 2) {
                        val lon = coordinates.optDouble(0, 0.0)
                        val lat = coordinates.optDouble(1, 0.0)
                        val coords = Pair(lat, lon)
                        cachedCityCoords[normalized] = coords
                        connection.disconnect()
                        return@withContext coords
                    }
                }
            }
            connection.disconnect()
        } catch (e: Exception) {
            e.printStackTrace()
        }
        null
    }

    suspend fun getSuggestions(query: String, biasCity: String? = null): List<PlaceSuggestion> {
        return getSuggestions(context = null, query = query, biasCity = biasCity)
    }

    suspend fun getSuggestions(
        context: Context? = null,
        query: String,
        biasCity: String? = null
    ): List<PlaceSuggestion> = withContext(Dispatchers.IO) {
        val trimmed = query.trim()
        if (trimmed.length < 3) return@withContext emptyList()

        // 1. Intentar Geocoder nativo de Android (Google Maps) si hay contexto disponible
        if (context != null && Geocoder.isPresent()) {
            try {
                val geocoder = Geocoder(context, Locale.getDefault())
                val addresses = try {
                    withTimeoutOrNull(3500L) {
                        getAddressesFromGeocoder(geocoder, trimmed, 6)
                    } ?: emptyList()
                } catch (e: Exception) {
                    emptyList()
                }

                if (addresses.isNotEmpty()) {
                    val seen = mutableSetOf<String>()
                    val results = mutableListOf<PlaceSuggestion>()
                    for (addr in addresses) {
                        val formatted = formatGeocoderAddress(addr)
                        if (formatted.isNotBlank() && seen.add(formatted)) {
                            results.add(PlaceSuggestion(formatted, addr.latitude, addr.longitude))
                        }
                    }
                    if (results.isNotEmpty()) {
                        return@withContext results
                    }
                }
            } catch (e: Exception) {
                // Fallback silencioso a Photon
            }
        }

        // 2. Fallback a Photon (OpenStreetMap) sin forzar ciudad para permitir viajes interprovinciales
        getSuggestionsPhoton(trimmed)
    }

    private suspend fun getAddressesFromGeocoder(
        geocoder: Geocoder,
        query: String,
        maxResults: Int
    ): List<Address> {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            suspendCancellableCoroutine { continuation ->
                try {
                    geocoder.getFromLocationName(query, maxResults, object : Geocoder.GeocodeListener {
                        override fun onGeocode(addresses: MutableList<Address>) {
                            if (continuation.isActive) {
                                continuation.resume(addresses)
                            }
                        }

                        override fun onError(errorMessage: String?) {
                            if (continuation.isActive) {
                                continuation.resume(emptyList())
                            }
                        }
                    })
                } catch (e: Exception) {
                    if (continuation.isActive) {
                        continuation.resume(emptyList())
                    }
                }
            }
        } else {
            @Suppress("DEPRECATION")
            geocoder.getFromLocationName(query, maxResults) ?: emptyList()
        }
    }

    private fun formatGeocoderAddress(address: Address): String {
        val line0 = address.getAddressLine(0)
        if (!line0.isNullOrBlank()) {
            return line0
                .replace(Regex(",\\s*(España|Spain)\\s*$", RegexOption.IGNORE_CASE), "")
                .trim()
        }

        val parts = mutableListOf<String>()
        val street = address.thoroughfare
        val number = address.subThoroughfare
        val locality = address.locality ?: address.subAdminArea ?: address.adminArea
        val feature = address.featureName
        val postalCode = address.postalCode

        if (!street.isNullOrBlank()) {
            if (!number.isNullOrBlank()) {
                parts.add("$street, $number")
            } else {
                parts.add(street)
            }
        } else if (!feature.isNullOrBlank() && feature != number) {
            parts.add(feature)
        }

        if (!postalCode.isNullOrBlank() && !locality.isNullOrBlank()) {
            parts.add("$postalCode $locality")
        } else if (!locality.isNullOrBlank()) {
            parts.add(locality)
        }

        return parts.joinToString(", ")
    }

    private suspend fun getSuggestionsPhoton(query: String): List<PlaceSuggestion> = withContext(Dispatchers.IO) {
        val suggestions = mutableListOf<PlaceSuggestion>()
        val seen = mutableSetOf<String>()
        var connection: HttpURLConnection? = null
        try {
            val encodedQuery = URLEncoder.encode(query, "UTF-8")
            val countryCode = Locale.getDefault().country.lowercase(Locale.ROOT)
            
            // Limit 10 para poder filtrar ruido y mantener hasta 6 resultados limpios
            var urlString = "https://photon.komoot.io/api?q=$encodedQuery&limit=10"
            if (countryCode.isNotEmpty()) {
                urlString += "&countrycode=$countryCode"
            }

            val url = URL(urlString)
            connection = url.openConnection() as HttpURLConnection
            connection.requestMethod = "GET"
            connection.setRequestProperty("User-Agent", USER_AGENT)
            connection.connectTimeout = 7000
            connection.readTimeout = 7000

            val responseCode = connection.responseCode
            if (responseCode == HttpURLConnection.HTTP_OK) {
                val reader = BufferedReader(InputStreamReader(connection.inputStream))
                val response = StringBuilder()
                var line: String?
                while (reader.readLine().also { line = it } != null) {
                    response.append(line)
                }
                reader.close()

                val jsonObject = JSONObject(response.toString())
                val features = jsonObject.optJSONArray("features")
                if (features != null) {
                    for (i in 0 until features.length()) {
                        val feature = features.getJSONObject(i)
                        val properties = feature.optJSONObject("properties") ?: continue

                        // Filtrar paradas de bus y elementos irrelevantes
                        val osmKey = properties.optString("osm_key", "")
                        val osmValue = properties.optString("osm_value", "")
                        if (osmKey == "highway" && (osmValue == "bus_stop" || osmValue == "platform")) continue
                        if (osmValue in listOf("waste_basket", "bench", "waste_disposal", "recycling")) continue

                        // Parse coordinates (GeoJSON is [longitude, latitude])
                        val geometry = feature.optJSONObject("geometry")
                        val coordinates = geometry?.optJSONArray("coordinates")
                        if (coordinates != null && coordinates.length() >= 2) {
                            val lon = coordinates.optDouble(0, 0.0)
                            val lat = coordinates.optDouble(1, 0.0)

                            val name = properties.optString("name", "")
                            val housenumber = properties.optString("housenumber", "")
                            val city = properties.optString("city", properties.optString("town", properties.optString("village", "")))
                            val postcode = properties.optString("postcode", "")
                            val street = properties.optString("street", "")
                            val state = properties.optString("state", "")

                            val parts = mutableListOf<String>()

                            val mainPart = if (street.isNotEmpty()) {
                                if (housenumber.isNotEmpty()) {
                                    if (name.isNotEmpty() && name != street) "$street $housenumber ($name)" else "$street $housenumber"
                                } else {
                                    if (name.isNotEmpty() && name != street) "$street ($name)" else street
                                }
                            } else if (name.isNotEmpty()) {
                                if (housenumber.isNotEmpty()) "$name $housenumber" else name
                            } else {
                                ""
                            }

                            if (mainPart.isNotEmpty()) {
                                parts.add(mainPart)
                            }

                            if (city.isNotEmpty()) {
                                if (postcode.isNotEmpty()) {
                                    parts.add("$postcode $city")
                                } else {
                                    parts.add(city)
                                }
                            } else if (state.isNotEmpty()) {
                                parts.add(state)
                            }

                            val displayName = parts.joinToString(", ")
                            if (displayName.isNotEmpty() && seen.add(displayName)) {
                                suggestions.add(PlaceSuggestion(displayName, lat, lon))
                                if (suggestions.size >= 6) break
                            }
                        }
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            connection?.disconnect()
        }
        suggestions
    }

    suspend fun getRouteDistance(
        startLat: Double, startLon: Double,
        endLat: Double, endLon: Double
    ): RouteResult? = withContext(Dispatchers.IO) {
        // 1. Intentar BRouter (Perfil car-fast, mucho más exacto para coche)
        val brouterResult = getRouteDistanceBRouter(startLat, startLon, endLat, endLon)
        if (brouterResult != null) {
            return@withContext brouterResult
        }
        // 2. Fallback a OSRM si BRouter falla
        getRouteDistanceOSRM(startLat, startLon, endLat, endLon)
    }

    private suspend fun getRouteDistanceBRouter(
        startLat: Double, startLon: Double,
        endLat: Double, endLon: Double
    ): RouteResult? = withContext(Dispatchers.IO) {
        var connection: HttpURLConnection? = null
        try {
            // Parámetro lonlats=lon1,lat1|lon2,lat2
            val urlString = "https://brouter.de/brouter?lonlats=$startLon,$startLat|$endLon,$endLat&profile=car-fast&format=geojson"
            val url = URL(urlString)
            connection = url.openConnection() as HttpURLConnection
            connection.requestMethod = "GET"
            connection.setRequestProperty("User-Agent", USER_AGENT)
            connection.connectTimeout = 7000
            connection.readTimeout = 7000

            val responseCode = connection.responseCode
            if (responseCode == HttpURLConnection.HTTP_OK) {
                val reader = BufferedReader(InputStreamReader(connection.inputStream))
                val response = StringBuilder()
                var line: String?
                while (reader.readLine().also { line = it } != null) {
                    response.append(line)
                }
                reader.close()

                val jsonObject = JSONObject(response.toString())
                val features = jsonObject.optJSONArray("features")
                if (features != null && features.length() > 0) {
                    val feature = features.getJSONObject(0)
                    val properties = feature.optJSONObject("properties")
                    if (properties != null) {
                        val trackLengthStr = properties.optString("track-length", "0")
                        val timeValStr = properties.optString("time-val", "0")
                        val distanceMeters = trackLengthStr.toDoubleOrNull() ?: 0.0
                        val durationSeconds = timeValStr.toDoubleOrNull() ?: 0.0
                        if (distanceMeters > 0.0) {
                            return@withContext RouteResult(distanceMeters / 1000.0, durationSeconds)
                        }
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            connection?.disconnect()
        }
        null
    }

    private suspend fun getRouteDistanceOSRM(
        startLat: Double, startLon: Double,
        endLat: Double, endLon: Double
    ): RouteResult? = withContext(Dispatchers.IO) {
        var connection: HttpURLConnection? = null
        try {
            val urlString = "https://router.project-osrm.org/route/v1/driving/$startLon,$startLat;$endLon,$endLat?overview=false"
            val url = URL(urlString)
            connection = url.openConnection() as HttpURLConnection
            connection.requestMethod = "GET"
            connection.setRequestProperty("User-Agent", USER_AGENT)
            connection.connectTimeout = 8000
            connection.readTimeout = 8000

            val responseCode = connection.responseCode
            if (responseCode == HttpURLConnection.HTTP_OK) {
                val reader = BufferedReader(InputStreamReader(connection.inputStream))
                val response = StringBuilder()
                var line: String?
                while (reader.readLine().also { line = it } != null) {
                    response.append(line)
                }
                reader.close()

                val jsonObject = JSONObject(response.toString())
                val code = jsonObject.optString("code", "")
                if (code == "Ok") {
                    val routes = jsonObject.optJSONArray("routes")
                    if (routes != null && routes.length() > 0) {
                        val route = routes.getJSONObject(0)
                        val distanceMeters = route.optDouble("distance", 0.0)
                        val duration = route.optDouble("duration", 0.0)
                        return@withContext RouteResult(distanceMeters / 1000.0, duration)
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            connection?.disconnect()
        }
        null
    }
}
