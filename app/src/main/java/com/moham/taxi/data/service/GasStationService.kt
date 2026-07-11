package com.moham.taxi.data.service

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.BufferedReader
import java.io.File
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL
import java.text.Normalizer
import java.util.Calendar
import java.util.Locale
import java.util.regex.Pattern

data class GasStation(
    val id: String,
    val name: String,
    val address: String,
    val postalCode: String,
    val city: String,
    val schedule: String,
    val latitude: Double,
    val longitude: Double,
    val brand: String,
    val prices: Map<String, Double> // maps fuelType (gnc, glp, gasolina 95, gasolina 98, diesel) to price
)

data class Province(val id: String, val name: String)

object GasStationService {
    private const val USER_AGENT = "Mitaxi/1.0 (Android Taxi App)"
    private const val BASE_URL = "https://sedeaplicaciones.minetur.gob.es/ServiciosRESTCarburantes/PreciosCarburantes/"

    val PROVINCES_LIST = listOf(
        Province("01", "Álava / Araba"),
        Province("02", "Albacete"),
        Province("03", "Alicante / Alacant"),
        Province("04", "Almería"),
        Province("05", "Ávila"),
        Province("06", "Badajoz"),
        Province("07", "Balears (Illes)"),
        Province("08", "Barcelona"),
        Province("09", "Burgos"),
        Province("10", "Cáceres"),
        Province("11", "Cádiz"),
        Province("12", "Castellón / Castelló"),
        Province("13", "Ciudad Real"),
        Province("14", "Córdoba"),
        Province("15", "A Coruña"),
        Province("16", "Cuenca"),
        Province("17", "Girona / Gerona"),
        Province("18", "Granada"),
        Province("19", "Guadalajara"),
        Province("20", "Gipuzkoa / Guipúzcoa"),
        Province("21", "Huelva"),
        Province("22", "Huesca"),
        Province("23", "Jaén"),
        Province("24", "León"),
        Province("25", "Lleida / Lérida"),
        Province("26", "La Rioja"),
        Province("27", "Lugo"),
        Province("28", "Madrid"),
        Province("29", "Málaga"),
        Province("30", "Murcia"),
        Province("31", "Navarra / Nafarroa"),
        Province("32", "Ourense / Orense"),
        Province("33", "Asturias"),
        Province("34", "Palencia"),
        Province("35", "Las Palmas"),
        Province("36", "Pontevedra"),
        Province("37", "Salamanca"),
        Province("38", "Santa Cruz de Tenerife"),
        Province("39", "Cantabria"),
        Province("40", "Segovia"),
        Province("41", "Sevilla"),
        Province("42", "Soria"),
        Province("43", "Tarragona"),
        Province("44", "Teruel"),
        Province("45", "Toledo"),
        Province("46", "Valencia / València"),
        Province("47", "Valladolid"),
        Province("48", "Bizkaia / Vizcaya"),
        Province("49", "Zamora"),
        Province("50", "Zaragoza"),
        Province("51", "Ceuta"),
        Province("52", "Melilla")
    )

    private val PROVINCES_MAP = mapOf(
        "alava" to "01", "araba" to "01", "albacete" to "02", "alicante" to "03", "alacant" to "03", "almeria" to "04", "avila" to "05",
        "badajoz" to "06", "balears" to "07", "baleares" to "07", "palma" to "07", "palmademallorca" to "07", "barcelona" to "08", "burgos" to "09",
        "caceres" to "10", "cadiz" to "11", "castellon" to "12", "castello" to "12", "ciudadreal" to "13", "cordoba" to "14",
        "coruña" to "15", "acoruña" to "15", "cuenca" to "16", "girona" to "17", "gerona" to "17", "granada" to "18",
        "guadalajara" to "19", "gipuzkoa" to "20", "guipuzcoa" to "20", "huelva" to "21", "huesca" to "22", "jaen" to "23",
        "leon" to "24", "lleida" to "25", "lerida" to "25", "larioja" to "26", "logroño" to "26", "lugo" to "27",
        "madrid" to "28", "malaga" to "29", "murcia" to "30", "navarra" to "31", "pamplona" to "31", "ourense" to "32",
        "orense" to "32", "asturias" to "33", "oviedo" to "33", "gijon" to "33", "palencia" to "34", "laspalmas" to "35",
        "pontevedra" to "36", "vigo" to "36", "salamanca" to "37", "santacruzdetenerife" to "38", "tenerife" to "38",
        "cantabria" to "39", "santander" to "39", "segovia" to "40", "sevilla" to "41", "soria" to "42", "tarragona" to "43",
        "teruel" to "44", "toledo" to "45", "valencia" to "46", "valencia/valència" to "46", "valència" to "46", "valladolid" to "47",
        "bizkaia" to "48", "vizcaya" to "48", "bilbao" to "48", "zamora" to "49", "zaragoza" to "50", "ceuta" to "51", "melilla" to "52"
    )

    private val SUB_CITIES_MAP = mapOf(
        // Madrid
        "alcaladehenares" to "28", "mostoles" to "28", "leganes" to "28", "getafe" to "28", "fuenlabrada" to "28", 
        "alcobendas" to "28", "lasrozas" to "28", "torrejondeardoz" to "28", "alcorcon" to "28", "majadahonda" to "28", "pozuelo" to "28",
        // Barcelona
        "sabadell" to "08", "hospitalet" to "08", "l'hospitalet" to "08", "l'hospitaletdellobregat" to "08", "badalona" to "08", 
        "terrassa" to "08", "mataro" to "08", "santacoloma" to "08", "cornellà" to "08", "santboi" to "08", "santcugat" to "08",
        // Cádiz
        "jerez" to "11", "jerezdelafrontera" to "11", "algeciras" to "11", "sanfernando" to "11", "elpuertodesantamaria" to "11", 
        "chiclana" to "11", "lineadelaconcepcion" to "11",
        // Pontevedra
        "vigo" to "36", "vilagarcia" to "36", "redondela" to "36",
        // Asturias
        "gijon" to "33", "aviles" to "33", "siero" to "33", "langreo" to "33",
        // Alicante
        "elche" to "03", "elx" to "03", "torrevieja" to "03", "orihuela" to "03", "benidorm" to "03", "alcoy" to "03", "elda" to "03",
        // Sevilla
        "doshermanas" to "41", "alcaladeguadaira" to "41", "utrera" to "41",
        // Málaga
        "marbella" to "29", "fuengirola" to "29", "estepona" to "29", "torremolinos" to "29", "benalmadena" to "29", "velez-malaga" to "29", "velezmalaga" to "29",
        // Murcia
        "cartagena" to "30", "lorca" to "30", "molinadesegura" to "30",
        // Almería
        "elejido" to "04", "roquetasdemar" to "04",
        // Tarragona
        "reus" to "43", "tortosa" to "43", "vendrell" to "43",
        // A Coruña
        "santiagodecompostela" to "15", "santiago" to "15", "ferrol" to "15", "naron" to "15",
        // Bizkaia
        "barakaldo" to "48", "getxo" to "48", "portugalete" to "48", "santurtzi" to "48", "basauri" to "48",
        // Gipuzkoa
        "irun" to "20", "errenteria" to "20",
        // Badajoz
        "merida" to "06", "donbenito" to "06", "almendralejo" to "06",
        // León
        "ponferrada" to "24",
        // Toledo
        "talaveradelareina" to "45",
        // Granada
        "motril" to "18",
        // Jaén
        "linares" to "23", "ubeda" to "23",
        // Ciudad Real
        "puertollano" to "13", "tomelloso" to "13",
        // Las Palmas
        "arrecife" to "35", "puertodelrosario" to "35",
        // Santa Cruz de Tenerife
        "adeje" to "38", "arona" to "38", "lalaguna" to "38", "sancristobaldelalaguna" to "38",
        // Burgos
        "mirandadeebro" to "09",
        // Navarra
        "tudela" to "31"
    )

    private val FUEL_TYPE_TO_PRODUCT_ID = mapOf(
        "gasolina 95" to "1",
        "gasolina 98" to "3",
        "diesel" to "4",
        "glp" to "17",
        "gnc" to "18"
    )

    private fun String.unaccent(): String {
        val temp = Normalizer.normalize(this, Normalizer.Form.NFD)
        val pattern = Pattern.compile("\\p{InCombiningDiacriticalMarks}+")
        return pattern.matcher(temp).replaceAll("")
    }

    private fun normalizeString(input: String): String {
        return input.trim().lowercase(Locale.ROOT)
            .unaccent()
            .replace(Regex("[^a-z0-9]"), "")
    }

    fun getProvinceIdByCity(city: String): String? {
        val normalized = normalizeString(city)
        if (normalized.isEmpty()) return null
        PROVINCES_MAP[normalized]?.let { return it }
        SUB_CITIES_MAP[normalized]?.let { return it }
        return null
    }

    private fun isToday(lastModified: Long): Boolean {
        val cacheCal = Calendar.getInstance().apply { timeInMillis = lastModified }
        val todayCal = Calendar.getInstance()
        return cacheCal.get(Calendar.YEAR) == todayCal.get(Calendar.YEAR) &&
               cacheCal.get(Calendar.DAY_OF_YEAR) == todayCal.get(Calendar.DAY_OF_YEAR)
    }

    suspend fun getCheapestStationsByProvince(
        context: Context,
        provinceId: String,
        fuelType: String,
        forceRefresh: Boolean = false
    ): List<GasStation> = withContext(Dispatchers.IO) {
        val cacheType = fuelType.replace(" ", "_")
        val cacheFile = File(context.cacheDir, "fuel_prices_${provinceId}_${cacheType}.json")
        var jsonContent: String? = null

        // Check cache if not forced refresh and cache is from today
        if (!forceRefresh && cacheFile.exists() && isToday(cacheFile.lastModified())) {
            try {
                jsonContent = cacheFile.readText()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        if (jsonContent == null) {
            val productId = FUEL_TYPE_TO_PRODUCT_ID[fuelType] ?: "1"
            jsonContent = fetchFromNetwork(provinceId, productId)
            if (jsonContent != null) {
                try {
                    cacheFile.writeText(jsonContent)
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }

        if (jsonContent.isNullOrBlank()) {
            return@withContext emptyList()
        }

        val stations = mutableListOf<GasStation>()
        try {
            val jsonObject = JSONObject(jsonContent)
            val list = jsonObject.optJSONArray("ListaEESSPrecio")
            if (list != null) {
                for (i in 0 until list.length()) {
                    val item = list.getJSONObject(i)
                    
                    val id = item.optString("IDEESS", "")
                    val name = item.optString("Rotulo", "")
                    val address = item.optString("Dirección", "")
                    val postalCode = item.optString("C.P.", "")
                    val city = item.optString("Municipio", "")
                    val schedule = item.optString("Horario", "")
                    
                    val latStr = item.optString("Latitud", "").replace(",", ".")
                    val lonStr = item.optString("Longitud (WGS84)", "").replace(",", ".")
                    val lat = latStr.toDoubleOrNull() ?: 0.0
                    val lon = lonStr.toDoubleOrNull() ?: 0.0
                    
                    val prices = mutableMapOf<String, Double>()
                    
                    val priceKeys = mapOf(
                        "gnc" to "Precio Gas Natural Comprimido",
                        "glp" to "Precio Gases licuados del petróleo",
                        "gasolina 95" to "Precio Gasolina 95 E5",
                        "gasolina 98" to "Precio Gasolina 98 E5",
                        "diesel" to "Precio Gasoleo A"
                    )
                    
                    for ((type, key) in priceKeys) {
                        val priceStr = item.optString(key, "").replace(",", ".")
                        priceStr.toDoubleOrNull()?.let {
                            prices[type] = it
                        }
                    }

                    // Fallback to "PrecioProducto" (returned when querying FiltroProvinciaProducto)
                    val priceProdStr = item.optString("PrecioProducto", "").replace(",", ".")
                    priceProdStr.toDoubleOrNull()?.let {
                        prices[fuelType] = it
                    }
                    
                    stations.add(
                        GasStation(
                            id = id,
                            name = name,
                            address = address,
                            postalCode = postalCode,
                            city = city,
                            schedule = schedule,
                            latitude = lat,
                            longitude = lon,
                            brand = name,
                            prices = prices
                        )
                    )
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        stations.filter { it.prices.containsKey(fuelType) }
            .sortedBy { it.prices[fuelType] }
            .take(10)
    }

    private fun fetchFromNetwork(provinceId: String, productId: String): String? {
        var connection: HttpURLConnection? = null
        try {
            val urlString = "${BASE_URL}EstacionesTerrestres/FiltroProvinciaProducto/$provinceId/$productId"
            val url = URL(urlString)
            connection = url.openConnection() as HttpURLConnection
            connection.requestMethod = "GET"
            connection.setRequestProperty("User-Agent", USER_AGENT)
            connection.connectTimeout = 10000
            connection.readTimeout = 10000

            val responseCode = connection.responseCode
            if (responseCode == HttpURLConnection.HTTP_OK) {
                val reader = BufferedReader(InputStreamReader(connection.inputStream))
                val response = StringBuilder()
                var line: String?
                while (reader.readLine().also { line = it } != null) {
                    response.append(line)
                }
                reader.close()
                return response.toString()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            connection?.disconnect()
        }
        return null
    }
}
