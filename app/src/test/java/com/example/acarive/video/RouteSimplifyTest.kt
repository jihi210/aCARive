package com.example.acarive.video

import com.example.acarive.data.drive.RoutePoint
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

class RouteSimplifyTest {
    private fun pt(seq: Int, lat: Double, lng: Double = 127.0) =
        RoutePoint(sessionId = 1, seq = seq, latitude = lat, longitude = lng, time = seq * 1000L)

    @Test
    fun keepsShortRouteAsIs() {
        val pts = (0 until 10).map { pt(it, 37.0 + it * 0.001) }
        assertSame(pts, simplifyRoute(pts, maxPoints = 400))
    }

    @Test
    fun reducesLongRouteKeepingEnds() {
        // 3시간 분량
        val pts = (0 until 10_800).map { pt(it, 37.0 + it * 0.0001) }
        val out = simplifyRoute(pts, maxPoints = 400)
        assertTrue(out.size in 300..400)
        assertEquals(pts.first(), out.first())
        assertEquals(pts.last(), out.last())
        // 순서 유지
        assertTrue(out.zipWithNext().all { (a, b) -> a.seq < b.seq })
    }

    @Test
    fun skipsStoppedPoints() {
        // 앞 절반은 정차, 뒤 절반만 이동
        val stopped = (0 until 1000).map { pt(it, 37.0) }
        val moving = (1000 until 2000).map { pt(it, 37.0 + (it - 999) * 0.0001) }
        val out = simplifyRoute(stopped + moving, maxPoints = 100)
        assertTrue(out.size <= 100)
        // 정차 구간에서는 첫 점만 남는다
        assertEquals(1, out.count { it.seq < 1000 })
    }

    @Test
    fun standingStillGivesTwoPoints() {
        val pts = (0 until 1000).map { pt(it, 37.0) }
        assertEquals(2, simplifyRoute(pts, maxPoints = 100).size)
    }
}
