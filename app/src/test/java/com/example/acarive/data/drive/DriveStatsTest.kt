package com.example.acarive.data.drive

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDateTime
import java.time.ZoneId

class DriveStatsTest {
    private val zone = ZoneId.of("Asia/Seoul")

    private fun session(id: Long, year: Int, minutes: Long, meters: Double): DriveSession {
        val start = LocalDateTime.of(year, 5, 18, 9, 0).atZone(zone).toInstant().toEpochMilli()
        return DriveSession(
            id = id,
            startTime = start,
            endTime = start + minutes * 60_000,
            distanceMeters = meters,
            pointCount = 2,
        )
    }

    @Test
    fun `누적 수치는 올해 드라이브만 합산하고 최근 기록에는 지난해도 포함한다`() {
        val summary = listOf(
            session(2, 2026, minutes = 90, meters = 86_000.0),
            session(1, 2025, minutes = 30, meters = 1_000.0),
        ).toDriveSummary(2026, zone)

        assertEquals(86.0, summary.totalKm, 0.001)
        assertEquals(1, summary.driveCount)
        assertEquals("1시간 30분", summary.totalDuration)
        assertEquals(listOf(2L, 1L), summary.recentDrives.map { it.sessionId })
        assertEquals("05. 18", summary.recentDrives.first().date)
    }

    @Test
    fun `최근 기록은 다섯 건까지만 보여준다`() {
        val sessions = (1L..7L).map { session(it, 2026, minutes = 10, meters = 100.0) }

        assertEquals(5, sessions.toDriveSummary(2026, zone).recentDrives.size)
    }

    @Test
    fun `거리와 시간을 단위에 맞게 표시한다`() {
        assertEquals("850 m", formatDistance(850.0))
        assertEquals("1,234.5 km", formatDistance(1_234_500.0))
        assertEquals("0분", formatDuration(0))
        assertEquals("1분 미만", formatDuration(59_000))
        assertEquals("59분", formatDuration(59 * 60_000L))
        assertEquals("2시간", formatDuration(120 * 60_000L))
        assertEquals("2시간 5분", formatDuration(125 * 60_000L))
    }
}
