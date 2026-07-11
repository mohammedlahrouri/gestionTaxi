package com.moham.taxi.data.service

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

import java.util.Locale

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

    suspend fun getSuggestions(query: String, biasCity: String? = null): List<PlaceSuggestion> = withContext(Dispatchers.IO) {
        if (query.trim().length < 3) return@withContext emptyList()
        val suggestions = mutableListOf<PlaceSuggestion>()
        var connection: HttpURLConnection? = null
        try {
            val encodedQuery = URLEncoder.encode(query, "UTF-8")
            val countryCode = Locale.getDefault().country.lowercase(Locale.ROOT)
            
            // Querying Photon (by Komoot)
            var urlString = "https://photon.komoot.io/api?q=$encodedQuery&limit=5"
            if (countryCode.isNotEmpty()) {
                urlString += "&countrycode=$countryCode"
            }

            if (!biasCity.isNullOrBlank()) {
                val coords = getCityCoords(biasCity)
                if (coords != null) {
                    urlString += "&lat=${coords.first}&lon=${coords.second}&location_bias_scale=0.2"
                }
            }

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
                val features = jsonObject.optJSONArray("features")
                if (features != null) {
                    for (i in 0 until features.length()) {
                        val feature = features.getJSONObject(i)
                        
                        // Parse coordinates (GeoJSON is [longitude, latitude])
                        val geometry = feature.optJSONObject("geometry")
                        val coordinates = geometry?.optJSONArray("coordinates")
                        if (coordinates != null && coordinates.length() >= 2) {
                            val lon = coordinates.optDouble(0, 0.0)
                            val lat = coordinates.optDouble(1, 0.0)
                            
                            // Parse properties to build a clean display name
                            val properties = feature.optJSONObject("properties")
                            if (properties != null) {
                                val name = properties.optString("name", "")
                                val housenumber = properties.optString("housenumber", "")
                                val city = properties.optString("city", "")
                                val postcode = properties.optString("postcode", "")
                                val street = properties.optString("street", "")
                                
                                val parts = mutableListOf<String>()
                                
                                // Build main address line
                                val mainPart = if (name.isNotEmpty()) {
                                    if (street.isNotEmpty() && name != street) {
                                        if (housenumber.isNotEmpty()) "$name ($street $housenumber)" else "$name ($street)"
                                    } else {
                                        if (housenumber.isNotEmpty()) "$name $housenumber" else name
                                    }
                                } else if (street.isNotEmpty()) {
                                    if (housenumber.isNotEmpty()) "$street $housenumber" else street
                                } else {
                                    ""
                                }
                                
                                if (mainPart.isNotEmpty()) {
                                    parts.add(mainPart)
                                }
                                
                                // Build city/postcode line
                                if (city.isNotEmpty()) {
                                    if (postcode.isNotEmpty()) {
                                        parts.add("$postcode $city")
                                    } else {
                                        parts.add(city)
                                    }
                                } else if (postcode.isNotEmpty()) {
                                    parts.add(postcode)
                                }
                                
                                val displayName = parts.joinToString(", ")
                                if (displayName.isNotEmpty()) {
                                    suggestions.add(PlaceSuggestion(displayName, lat, lon))
                                }
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
