package com.example.acarive.location

import android.util.Log
import com.google.gson.JsonParser
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.HttpURLConnection
import java.net.URL

private const val TAG = "RoadRoute"
// OSRM 공개 데모 서버. 키 없이 쓸 수 있지만 가볍게만 써야 한다 (시연용)
private const val BASE_URL = "https://router.project-osrm.org/route/v1/driving"
private const val TIMEOUT_MS = 10_000
private const val USER_AGENT = "aCARive-demo/1.0"

// 데모 주행용 실제 도로 경로. points 를 순서대로 지나는 도로 위 좌표 목록을 돌려준다
object RoadRoute {
    // 못 받으면 null (호출한 쪽에서 직선으로 대신 간다)
    suspend fun fetch(points: List<LocationPoint>): List<LocationPoint>? = withContext(Dispatchers.IO) {
        if (points.size < 2) return@withContext null
        try {
            request(points)
        } catch (e: Exception) {
            Log.w(TAG, "도로 경로 못 받음", e)
            null
        }
    }

    private fun request(points: List<LocationPoint>): List<LocationPoint>? {
        val coords = points.joinToString(";") { "${it.lng},${it.lat}" }
        // steps=true 라야 구간(leg)별로 도로 좌표가 나온다
        val url = "$BASE_URL/$coords?overview=false&steps=true&geometries=geojson"
        val conn = URL(url).openConnection() as HttpURLConnection
        val text = try {
            conn.connectTimeout = TIMEOUT_MS
            conn.readTimeout = TIMEOUT_MS
            // 기본 User-Agent(Dalvik)는 OSRM 데모 서버가 403 으로 막는다
            conn.setRequestProperty("User-Agent", USER_AGENT)
            if (conn.responseCode !in 200..299) {
                Log.w(TAG, "HTTP ${conn.responseCode}")
                return null
            }
            conn.inputStream.bufferedReader().use { it.readText() }
        } finally {
            conn.disconnect()
        }

        val root = JsonParser.parseString(text).asJsonObject
        if (root.get("code")?.asString != "Ok") {
            Log.w(TAG, "OSRM 오류: ${text.take(200)}")
            return null
        }
        val legs = root.getAsJsonArray("routes")?.firstOrNull()?.asJsonObject
            ?.getAsJsonArray("legs") ?: return null
        if (legs.size() != points.size - 1) return null

        // 출발점 -> 각 구간 도로 좌표 -> 그 구간 끝 경유지.
        // 경유지가 도로에서 떨어져 있어도 꼭 들르게 해서 경유지 도착 판정(CourseProgress)이 되게 한다
        val path = mutableListOf(points.first())
        legs.forEachIndexed { i, leg ->
            leg.asJsonObject.getAsJsonArray("steps").forEach { step ->
                step.asJsonObject.getAsJsonObject("geometry").getAsJsonArray("coordinates").forEach { c ->
                    val lngLat = c.asJsonArray
                    val p = LocationPoint(lngLat[1].asDouble, lngLat[0].asDouble)
                    if (p != path.last()) path += p
                }
            }
            path += points[i + 1]
        }
        return path
    }
}
