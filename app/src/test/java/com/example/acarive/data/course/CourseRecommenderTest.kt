package com.example.acarive.data.course

import com.example.acarive.location.LocationPoint
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class CourseRecommenderTest {

    private val seoul = LocationPoint(37.5665, 126.9780)

    // 위도 0.01도 ≈ 1.1km. 서울에서 북쪽으로 lat 만큼 떨어진 곳에 경유지 하나짜리 코스
    private fun course(id: String, latOffset: Double, themes: Set<CourseTheme> = emptySet()) = DriveCourse(
        id = id,
        source = CourseSource(CourseSource.Provider.TourApi, id),
        name = id,
        area = "",
        regionCode = null,
        overview = "",
        imageUrl = null,
        imageCopyright = null,
        themes = themes,
        stops = listOf(
            CourseStop(0, null, "s", LocationPoint(seoul.lat + latOffset, seoul.lng), null, null),
        ),
        distanceKm = null,
        takeTimeMinutes = null,
        distanceText = null,
        takeTimeText = null,
        sourceModifiedAt = null,
        collectedAt = 0,
    )

    @Test
    fun estimateIsRoundTrip() {
        val e = CourseRecommender.estimate(seoul, course("a", 0.5))!!
        assertEquals(e.toStartMinutes, e.returnMinutes)
        assertEquals(0, e.courseMinutes)
        // 직선 약 55km -> 도로 약 72km -> 80km/h 로 54분 + 10분
        assertTrue(e.toStartMinutes in 60..68)
    }

    @Test
    fun noCoordinatesHasNoEstimate() {
        val c = course("a", 0.5).copy(stops = listOf(CourseStop(0, null, "s", null, null, null)))
        assertNull(CourseRecommender.estimate(seoul, c))
    }

    @Test
    fun inWindowFirstThenPreferredTheme() {
        val near = course("near", 0.05) // 왕복 1시간 미만
        val far = course("far", 3.0) // 왕복 몇 시간
        val inWindow = course("inWindow", 0.6)
        val inWindowOcean = course("inWindowOcean", 0.6, setOf(CourseTheme.Ocean))
        val recs = CourseRecommender.recommend(listOf(near, far, inWindow, inWindowOcean), seoul, setOf(CourseTheme.Ocean))
        assertEquals(listOf("inWindowOcean", "inWindow"), recs.take(2).map { it.course.id })
        assertTrue(recs.take(2).all { it.inWindow })
        // 범위 밖은 범위에 가까운 쪽(near)이 먼저
        assertEquals(listOf("near", "far"), recs.drop(2).map { it.course.id })
    }

    @Test
    fun preferencesMatchLooseLabels() {
        val themes = CourseRecommender.themesFromPreferences(listOf("바다", "한적한 길", "노을", "없는 테마"))
        assertEquals(setOf(CourseTheme.Ocean, CourseTheme.Quiet, CourseTheme.Sunset), themes)
    }
}
