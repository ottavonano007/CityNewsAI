package it.citynewsai

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

internal data class BriefingSource(val title: String, val url: String)
internal data class BriefingResult(val text: String, val sources: List<BriefingSource>, val generatedAt: String)

private const val BRIEFING_API_BASE_URL = "https://citynewsai-briefing-ottavonano007.onrender.com"

internal suspend fun fetchAiBriefing(province: Province, periodId: String): BriefingResult =
    withContext(Dispatchers.IO) {
        val base = BRIEFING_API_BASE_URL.trimEnd('/')
        val connection = (URL("$base/api/briefing").openConnection() as HttpURLConnection)
        try {
            connection.connectTimeout = 20000
            connection.readTimeout = 135000
            connection.requestMethod = "POST"
            connection.doOutput = true
            connection.setRequestProperty("Content-Type", "application/json; charset=utf-8")
            connection.setRequestProperty("Accept", "application/json")
            connection.setRequestProperty("User-Agent", "CityNewsAI-Android")
            val city = province.name
            val body = JSONObject().apply {
                put("city", city)
                put("province", province.name)
                put("region", province.region)
                put("period", periodId)
            }.toString()
            connection.outputStream.bufferedWriter(Charsets.UTF_8).use { it.write(body) }
            val status = connection.responseCode
            val stream = if (status in 200..299) connection.inputStream else connection.errorStream
            val raw = stream?.bufferedReader(Charsets.UTF_8)?.use { it.readText() }.orEmpty()
            val json = runCatching { JSONObject(raw) }.getOrNull()
            if (status !in 200..299) {
                val message = json?.optString("error").orEmpty().ifBlank {
                    "Servizio AI non raggiungibile (HTTP $status)."
                }
                throw IllegalStateException(message)
            }
            val text = json?.optString("text").orEmpty()
            if (text.isBlank()) throw IllegalStateException("La rassegna ricevuta è vuota.")
            val sources = buildList {
                val array = json?.optJSONArray("sources")
                if (array != null) for (i in 0 until array.length()) {
                    val item = array.optJSONObject(i) ?: continue
                    val url = item.optString("url")
                    if (url.startsWith("https://")) add(BriefingSource(item.optString("title", url), url))
                }
            }
            BriefingResult(text, sources, json?.optString("generated_at").orEmpty())
        } finally {
            connection.disconnect()
        }
    }
