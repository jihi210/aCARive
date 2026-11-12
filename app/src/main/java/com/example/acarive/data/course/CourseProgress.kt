package com.example.acarive.data.course

import com.example.acarive.location.LocationPoint
import kotlin.math.roundToInt

// 코스 주행 진행 상황. 길안내는 차량 내비가 하고, 앱은 다음 경유지와 남은 거리만 보여준다

// 이만큼 가까워지면 그 경유지를 지난 걸로 본다
private const val ARRIVE_KM = 0.5
// 남은 거리·시간 추정: 직선거리 -> 도로거리 보정, 평균 속도 (CourseRecommender 와 비슷한 값)
private const val ROAD_FACTOR = 1.3
private const val AVERAGE_KMH = 50.0
// 차량 내비가 받는 경유지 수 (CarNavigation.MAX_WAYPOINTS) + 목적지
private const val MAX_KEY_STOPS = 3

data class KeyStop(val name: String, val location: LocationPoint)

data class DriveProgress(
    val stops: List<KeyStop>,
    // 다음에 갈 경유지. stops.size 면 다 지났다
    val nextIndex: Int,
    val remainingKm: Double,
    val remainingMinutes: Int,
) {
    val isFinished: Boolean get() = nextIndex >= stops.size
    val nextStop: KeyStop? get() = stops.getOrNull(nextIndex)
}

// 좌표 있는 경유지 중 출발점·가운데·마지막만. 차량 내비는 경유지 2개 + 목적지까지 받는다
fun DriveCourse.keyStops(): List<KeyStop> {
    val located = stops.mapNotNull { s -> s.location?.let { KeyStop(s.name, it) } }
    if (located.size <= MAX_KEY_STOPS) return located
    return listOf(located.first(), located[located.size / 2], located.last())
}

object CourseProgress {
    fun start(stops: List<KeyStop>, here: LocationPoint?): DriveProgress = update(stops, 0, here)

    // here 에서 ARRIVE_KM 안에 있는 가장 뒤 경유지까지 지난 걸로 본다.
    // 내비가 다른 길로 가서 중간 경유지 근처를 안 지나도 다음 경유지에 닿으면 따라잡는다
    fun update(stops: List<KeyStop>, nextIndex: Int, here: LocationPoint?): DriveProgress {
        var next = nextIndex
        if (here != null) {
            for (i in stops.indices.reversed()) {
                if (i < next) break
                if (CourseRecommender.distanceKm(here, stops[i].location) <= ARRIVE_KM) {
                    next = i + 1
                    break
                }
            }
        }
        // 위치를 모르면 경유지 사이 거리만
        val km = remainingKm(stops, next, here)
        return DriveProgress(stops, next, km, (km / AVERAGE_KMH * 60).roundToInt())
    }

    // 지금 위치 -> 남은 경유지 순서대로, 도로 보정한 거리
    private fun remainingKm(stops: List<KeyStop>, next: Int, here: LocationPoint?): Double {
        val rest = stops.drop(next).map { it.location }
        if (rest.isEmpty()) return 0.0
        val path = listOfNotNull(here) + rest
        return path.zipWithNext { a, b -> CourseRecommender.distanceKm(a, b) }.sum() * ROAD_FACTOR
    }
}
