package com.acarive.scenic.context

import android.util.Log
import com.acarive.scenic.domain.Weather
import com.example.acarive.location.LocationPoint
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLDecoder
import java.net.URLEncoder
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import kotlin.math.cos
import kotlin.math.floor
import kotlin.math.ln
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.tan

private const val TAG = "KmaWeather"
private const val BASE_URL = "https://apis.data.go.kr/1360000/VilageFcstInfoService_2.0"

// 위치를 못 받았을 때 쓰는 기준 지점 (서울시청)
private val FALLBACK_LOCATION = LocationPoint(37.5665, 126.9780)

// 기상청 단기예보 API (공공데이터포털 VilageFcstInfoService_2.0) 로 현재 날씨를 가져온다.
//  - 초단기실황(getUltraSrtNcst): 기온(T1H), 강수형태(PTY)
//  - 초단기예보(getUltraSrtFcst): 하늘상태(SKY)  <- 실황에는 하늘상태가 없어서, 비/눈이 아닐 때만 추가로 조회
// location 은 호출할 때마다 읽는다. 위치를 못 받으면(null) 서울시청 기준.
// 실패하면 null 을 돌려주고, ContextAggregator 가 직전 값을 유지한다.
class KmaWeatherSource(
    private val serviceKey: String,
    private val location: () -> LocationPoint?,
    private val now: () -> LocalDateTime = { LocalDateTime.now() },
) : WeatherSource {

    override suspend fun current(): WeatherInfo? = withContext(Dispatchers.IO) {
        try {
            val point = location() ?: FALLBACK_LOCATION
            val (nx, ny) = KmaGrid.fromLatLng(point.lat, point.lng)
            val t = now()

            val ncst = fetch("getUltraSrtNcst", ncstParams(t, nx, ny)) ?: return@withContext null
            val values = ncst.associate { it.get("category").asString to it.get("obsrValue").asString }
            val temp = values["T1H"]?.toDoubleOrNull() ?: return@withContext null

            val weather = fromPrecipitation(values["PTY"]?.toIntOrNull() ?: 0)
                ?: fetchSky(t, nx, ny)
                ?: Weather.SUNNY
            WeatherInfo(weather, temp)
        } catch (e: kotlinx.coroutines.CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.w(TAG, "기상청 날씨 조회 실패", e)
            null
        }
    }

    // 초단기실황은 매시 정각 기준이고 40분 이후에 올라온다
    private fun ncstParams(t: LocalDateTime, nx: Int, ny: Int): Map<String, String> {
        val base = t.minusMinutes(40)
        return baseParams(base.format(DATE), base.format(HOUR) + "00", nx, ny)
    }

    // 초단기예보는 매시 30분 기준이고 45분 이후에 올라온다
    private fun fcstParams(t: LocalDateTime, nx: Int, ny: Int): Map<String, String> {
        val shifted = t.minusMinutes(45)
        val base = if (shifted.minute >= 30) shifted else shifted.minusHours(1)
        return baseParams(base.format(DATE), base.format(HOUR) + "30", nx, ny)
    }

    private fun baseParams(date: String, time: String, nx: Int, ny: Int) = mapOf(
        "base_date" to date,
        "base_time" to time,
        "nx" to nx.toString(),
        "ny" to ny.toString(),
    )

    private fun fetchSky(t: LocalDateTime, nx: Int, ny: Int): Weather? {
        val items = fetch("getUltraSrtFcst", fcstParams(t, nx, ny)) ?: return null
        // 여러 시각이 오는데 가장 가까운 시각(fcstTime 이 가장 작은 것)의 SKY 를 쓴다
        val sky = items
            .filter { it.get("category").asString == "SKY" }
            .minByOrNull { it.get("fcstTime").asString }
            ?.get("fcstValue")?.asString?.toIntOrNull()
        return fromSky(sky)
    }

    // item 배열을 돌려준다. 오류 응답이거나 못 읽으면 null
    private fun fetch(operation: String, params: Map<String, String>): List<JsonObject>? {
        val query = buildString {
            append("serviceKey=").append(encodedKey())
            append("&pageNo=1&numOfRows=100&dataType=JSON")
            params.forEach { (k, v) -> append('&').append(k).append('=').append(v) }
        }
        val conn = URL("$BASE_URL/$operation?$query").openConnection() as HttpURLConnection
        try {
            conn.connectTimeout = 8_000
            conn.readTimeout = 8_000
            val code = conn.responseCode
            val text = (if (code in 200..299) conn.inputStream else conn.errorStream)
                ?.bufferedReader()?.use { it.readText() }.orEmpty()
            if (code !in 200..299) {
                Log.w(TAG, "$operation HTTP $code: ${text.take(200)}")
                return null
            }
            return parseItems(operation, text)
        } finally {
            conn.disconnect()
        }
    }

    private fun parseItems(operation: String, text: String): List<JsonObject>? {
        // 키가 잘못되면 JSON 이 아니라 XML 로 에러가 온다
        val root = try {
            JsonParser.parseString(text).asJsonObject
        } catch (e: Exception) {
            Log.w(TAG, "$operation 응답이 JSON 이 아님: ${text.take(200)}")
            return null
        }
        val response = root.getAsJsonObject("response") ?: return null
        val resultCode = response.getAsJsonObject("header")?.get("resultCode")?.asString
        if (resultCode != "00") {
            Log.w(TAG, "$operation 결과 코드 $resultCode: ${response.getAsJsonObject("header")?.get("resultMsg")}")
            return null
        }
        val array = response.getAsJsonObject("body")
            ?.getAsJsonObject("items")
            ?.getAsJsonArray("item") ?: return null
        return array.map { it.asJsonObject }
    }

    // 포털에서 복사한 키가 이미 URL 인코딩된 것(Encoding)이든 아니든(Decoding) 한 번만 인코딩되게 맞춘다
    private fun encodedKey(): String =
        URLEncoder.encode(URLDecoder.decode(serviceKey, "UTF-8"), "UTF-8")

    companion object {
        private val DATE = DateTimeFormatter.ofPattern("yyyyMMdd")
        private val HOUR = DateTimeFormatter.ofPattern("HH")

        // 강수형태: 0 없음, 1 비, 2 비/눈, 3 눈, 5 빗방울, 6 빗방울눈날림, 7 눈날림
        // 없음(0)이면 null -> 하늘상태로 판단
        internal fun fromPrecipitation(pty: Int): Weather? = when (pty) {
            1, 2, 5, 6 -> Weather.RAIN
            3, 7 -> Weather.SNOW
            else -> null
        }

        // 하늘상태: 1 맑음, 3 구름많음, 4 흐림
        internal fun fromSky(sky: Int?): Weather? = when (sky) {
            1 -> Weather.SUNNY
            3, 4 -> Weather.CLOUDY
            else -> null
        }
    }
}

// 위경도 -> 기상청 격자(nx, ny). 기상청 공개 변환식(Lambert Conformal Conic, 5km 격자)
object KmaGrid {
    private const val RE = 6371.00877
    private const val GRID = 5.0
    private const val SLAT1 = 30.0
    private const val SLAT2 = 60.0
    private const val OLON = 126.0
    private const val OLAT = 38.0
    private const val XO = 43.0
    private const val YO = 136.0

    fun fromLatLng(lat: Double, lng: Double): Pair<Int, Int> {
        val d = Math.PI / 180.0
        val re = RE / GRID
        val slat1 = SLAT1 * d
        val slat2 = SLAT2 * d
        val olon = OLON * d
        val olat = OLAT * d

        val sn = ln(cos(slat1) / cos(slat2)) /
                ln(tan(Math.PI * 0.25 + slat2 * 0.5) / tan(Math.PI * 0.25 + slat1 * 0.5))
        val sf = tan(Math.PI * 0.25 + slat1 * 0.5).pow(sn) * cos(slat1) / sn
        val ro = re * sf / tan(Math.PI * 0.25 + olat * 0.5).pow(sn)
        val ra = re * sf / tan(Math.PI * 0.25 + lat * d * 0.5).pow(sn)

        var theta = lng * d - olon
        if (theta > Math.PI) theta -= 2.0 * Math.PI
        if (theta < -Math.PI) theta += 2.0 * Math.PI
        theta *= sn

        val x = floor(ra * sin(theta) + XO + 0.5).toInt()
        val y = floor(ro - ra * cos(theta) + YO + 0.5).toInt()
        return x to y
    }
}
