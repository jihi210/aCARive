package com.example.acarive.data.course

import com.example.acarive.location.LocationPoint
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class CourseProgressTest {
    // 위도 0.01 ≈ 1.1km
    private val stops = listOf(
        KeyStop("A", LocationPoint(37.00, 127.0)),
        KeyStop("B", LocationPoint(37.05, 127.0)),
        KeyStop("C", LocationPoint(37.10, 127.0)),
    )

    @Test
    fun startsAtFirstStopWithRemainingDistance() {
        val p = CourseProgress.start(stops, LocationPoint(36.95, 127.0))
        assertEquals(0, p.nextIndex)
        assertEquals("A", p.nextStop?.name)
        // 직선 약 16.7km x 1.3
        assertTrue(p.remainingKm in 20.0..23.0)
        assertTrue(p.remainingMinutes > 0)
    }

    @Test
    fun passesStopWhenClose() {
        val p = CourseProgress.update(stops, 0, LocationPoint(37.002, 127.0))
        assertEquals(1, p.nextIndex)
        assertEquals("B", p.nextStop?.name)
    }

    // 중간 경유지를 안 지나도 뒤 경유지에 닿으면 따라잡는다
    @Test
    fun catchesUpWhenSkippingStop() {
        val p = CourseProgress.update(stops, 0, LocationPoint(37.10, 127.0))
        assertTrue(p.isFinished)
        assertNull(p.nextStop)
        assertEquals(0.0, p.remainingKm, 0.0)
    }

    // 지난 경유지 근처로 돌아가도 진행이 되돌아가지 않는다
    @Test
    fun neverGoesBackward() {
        val p = CourseProgress.update(stops, 2, LocationPoint(37.00, 127.0))
        assertEquals(2, p.nextIndex)
    }

    @Test
    fun keyStopsKeepsStartMiddleEnd() {
        val many = (0 until 6).map { i ->
            CourseStop(i, null, "S$i", LocationPoint(37.0 + i * 0.01, 127.0), null, null)
        } + CourseStop(6, null, "좌표 없음", null, null, null)
        val course = DriveCourse(
            id = "t", source = CourseSource(CourseSource.Provider.TourApi, "t"), name = "t", area = "",
            regionCode = null, overview = "", imageUrl = null, imageCopyright = null, themes = emptySet(),
            stops = many, distanceKm = null, takeTimeMinutes = null, distanceText = null, takeTimeText = null,
            sourceModifiedAt = null, collectedAt = 0,
        )
        assertEquals(listOf("S0", "S3", "S5"), course.keyStops().map { it.name })
    }
}
