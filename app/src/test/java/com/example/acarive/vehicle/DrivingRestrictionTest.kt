package com.example.acarive.vehicle

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class DrivingRestrictionTest {

    @Test
    fun parkedIsNotBlocked() {
        // 주차: DO=false, 제한 없음
        assertFalse(DrivingRestriction.isBlocking(false, 0))
    }

    @Test
    fun pleosMovingIsBlocked() {
        // Pleos 에뮬레이터 설정: 정차·주행 중 0x50 (NO_SETUP | NO_VIDEO)
        assertTrue(DrivingRestriction.isBlocking(true, 0x50))
    }

    @Test
    fun fullyRestrictedIsBlocked() {
        assertTrue(DrivingRestriction.isBlocking(true, 0x1ff))
    }

    @Test
    fun onlyUnrelatedFlagsIsNotBlocked() {
        // 다이얼패드·문자 제한(1, 128)만 걸리면 이 앱 화면은 막지 않는다
        assertFalse(DrivingRestriction.isBlocking(true, 1 or 128))
    }
}
