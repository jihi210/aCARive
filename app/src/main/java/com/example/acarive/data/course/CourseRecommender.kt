package com.example.acarive.data.course

import com.example.acarive.location.LocationPoint
import kotlin.math.abs
import kotlin.math.asin
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.math.sqrt

// 기획 (2) 코스 추천: 현재 위치에서 왕복 2~3시간 + 취향 테마
// TODO(recommend): 타 사용자 주행 기록(CourseUsageStats)이 생기면 점수에 더하기

// 현 위치 -> 출발점 -> 경유지 순서대로 -> 마지막 경유지 -> 현 위치, 직선거리로 추정한 운전 시간
data class DriveEstimate(
    val toStartMinutes: Int,
    val courseMinutes: Int,
    val returnMinutes: Int,
    val totalKm: Double,
) {
    val roundTripMinutes: Int get() = toStartMinutes + courseMinutes + returnMinutes
}

data class CourseRecommendation(
    val course: DriveCourse,
    // 좌표 있는 경유지가 없으면 null (목록 맨 뒤로)
    val estimate: DriveEstimate?,
    val matchedThemes: Set<CourseTheme>,
) {
    val inWindow: Boolean
        get() = estimate != null && estimate.roundTripMinutes in CourseRecommender.WINDOW_MINUTES
}

object CourseRecommender {
    val WINDOW_MINUTES = 120..180
    private const val TARGET_MINUTES = 150

    // 직선거리 -> 도로거리 보정
    private const val ROAD_FACTOR = 1.3
    // 출발점 오가는 길: 고속도로 위주로 보고, 시내 진출입 시간을 따로 더한다
    private const val APPROACH_KMH = 80.0
    private const val APPROACH_OVERHEAD_MIN = 10
    // 코스 안 경유지 사이: 국도·지방도
    private const val COURSE_KMH = 45.0

    // 순서: 왕복 2~3시간 안 -> 취향 테마 많이 겹침 -> 2시간 30분에 가까움.
    // 범위 안 코스가 모자라도 홈에 5개를 채울 수 있게 범위 밖은 범위에 가까운 순으로 뒤에 붙인다.
    fun recommend(
        courses: List<DriveCourse>,
        origin: LocationPoint,
        preferred: Set<CourseTheme>,
    ): List<CourseRecommendation> =
        courses
            .map { CourseRecommendation(it, estimate(origin, it), it.themes intersect preferred) }
            .sortedWith(
                compareBy<CourseRecommendation> { it.estimate == null }
                    .thenByDescending { it.inWindow }
                    .thenBy { if (it.inWindow) 0 else gapFromWindow(it.estimate!!.roundTripMinutes) }
                    .thenByDescending { it.matchedThemes.size }
                    .thenBy { abs((it.estimate?.roundTripMinutes ?: 0) - TARGET_MINUTES) },
            )

    // 주행 중 추천: 출발점이 지금 위치에서 이 직선거리 안인 코스만 (도로로 15분 안팎)
    const val NEARBY_KM = 10.0

    // 주행 중 추천. 출발점이 가까운 코스만 -> 취향 테마 많이 겹침 -> 가까운 순.
    // exclude: 이번 주행에서 이미 보여준 코스
    fun nearby(
        courses: List<DriveCourse>,
        here: LocationPoint,
        preferred: Set<CourseTheme>,
        exclude: Set<String>,
        limit: Int,
    ): List<CourseRecommendation> =
        courses
            .filter { it.id !in exclude && it.waypoints.isNotEmpty() }
            .map { it to distanceKm(here, it.waypoints.first()) }
            .filter { (_, km) -> km <= NEARBY_KM }
            .sortedWith(
                compareByDescending<Pair<DriveCourse, Double>> { (c, _) -> (c.themes intersect preferred).size }
                    .thenBy { (_, km) -> km },
            )
            .take(limit)
            .map { (c, _) -> CourseRecommendation(c, estimate(here, c), c.themes intersect preferred) }

    fun estimate(origin: LocationPoint, course: DriveCourse): DriveEstimate? {
        val points = course.waypoints
        if (points.isEmpty()) return null
        val toStartKm = distanceKm(origin, points.first())
        val returnKm = distanceKm(points.last(), origin)
        val courseKm = points.zipWithNext { a, b -> distanceKm(a, b) }.sum()
        return DriveEstimate(
            toStartMinutes = approachMinutes(toStartKm),
            courseMinutes = minutes(courseKm * ROAD_FACTOR, COURSE_KMH),
            returnMinutes = approachMinutes(returnKm),
            totalKm = (toStartKm + courseKm + returnKm) * ROAD_FACTOR,
        )
    }

    // "한적한 길" 처럼 프로필 문구가 테마 이름과 조금 달라도 맞춘다
    fun themesFromPreferences(preferences: List<String>): Set<CourseTheme> =
        preferences.mapNotNullTo(mutableSetOf()) { pref ->
            CourseTheme.entries.find { pref.contains(it.label) || it.label.contains(pref) }
        }

    private fun gapFromWindow(minutes: Int): Int = when {
        minutes < WINDOW_MINUTES.first -> WINDOW_MINUTES.first - minutes
        else -> minutes - WINDOW_MINUTES.last
    }

    private fun approachMinutes(straightKm: Double): Int =
        minutes(straightKm * ROAD_FACTOR, APPROACH_KMH) + APPROACH_OVERHEAD_MIN

    private fun minutes(km: Double, kmh: Double): Int = (km / kmh * 60).roundToInt()

    // 하버사인 직선거리(km)
    fun distanceKm(a: LocationPoint, b: LocationPoint): Double {
        val r = 6371.0
        val dLat = Math.toRadians(b.lat - a.lat)
        val dLng = Math.toRadians(b.lng - a.lng)
        val h = sin(dLat / 2).let { it * it } +
            cos(Math.toRadians(a.lat)) * cos(Math.toRadians(b.lat)) * sin(dLng / 2).let { it * it }
        return 2 * r * asin(sqrt(h))
    }
}
