package com.example.acarive.vehicle

import android.car.Car
import android.car.drivingstate.CarUxRestrictions
import android.car.drivingstate.CarUxRestrictionsManager
import android.content.Context
import android.content.pm.PackageManager
import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

private const val TAG = "DrivingRestriction"

// 이 중 하나라도 걸리면 설정·동의·긴 글·영상을 막는다 (AppUiState.isDrivingRestricted)
private const val BLOCKING_FLAGS =
    CarUxRestrictions.UX_RESTRICTIONS_NO_SETUP or
        CarUxRestrictions.UX_RESTRICTIONS_NO_KEYBOARD or
        CarUxRestrictions.UX_RESTRICTIONS_LIMIT_CONTENT or
        CarUxRestrictions.UX_RESTRICTIONS_NO_VIDEO

// 차량(AAOS)의 주행 중 UX 제한을 구독한다. 차가 움직이면 시스템이 제한을 건다.
// 차량이 아니면(일반 폰·에뮬레이터) 구독하지 않고 항상 false
class DrivingRestriction(context: Context) {
    private val app = context.applicationContext
    private var car: Car? = null
    private var manager: CarUxRestrictionsManager? = null

    private val _restricted = MutableStateFlow(false)
    val restricted: StateFlow<Boolean> = _restricted.asStateFlow()

    fun start() {
        if (car != null || !isCar(app)) return
        try {
            // 차량 서비스가 늦게 뜨거나 재시작돼도 다시 붙도록 리스너 방식으로 연결한다
            car = Car.createCar(app, null, Car.CAR_WAIT_TIMEOUT_DO_NOT_WAIT) { connected, ready ->
                if (ready) subscribe(connected) else manager = null
            }
        } catch (e: Throwable) {
            // android.car 라이브러리가 없는 기기면 NoClassDefFoundError
            Log.w(TAG, "차량 서비스 연결 실패", e)
        }
    }

    private fun subscribe(car: Car) {
        val m = car.getCarManager(Car.CAR_UX_RESTRICTION_SERVICE) as? CarUxRestrictionsManager ?: return
        manager = m
        m.registerListener(::update)
        val current = m.currentCarUxRestrictions
        Log.d(TAG, "UX 제한 구독 시작 (DO=${current?.isRequiresDistractionOptimization}, flags=${current?.activeRestrictions})")
        current?.let(::update)
    }

    private fun update(r: CarUxRestrictions) {
        val blocked = isBlocking(r.isRequiresDistractionOptimization, r.activeRestrictions)
        if (blocked != _restricted.value) Log.d(TAG, "주행 제한 ${if (blocked) "켜짐" else "꺼짐"} (flags=${r.activeRestrictions})")
        _restricted.value = blocked
    }

    fun stop() {
        runCatching { manager?.unregisterListener() }
        runCatching { car?.disconnect() }
        manager = null
        car = null
    }

    companion object {
        // 주행 최적화가 필요하고, 막아야 할 제한이 하나라도 걸려 있으면 true
        fun isBlocking(requiresDistractionOptimization: Boolean, activeRestrictions: Int): Boolean =
            requiresDistractionOptimization && (activeRestrictions and BLOCKING_FLAGS) != 0

        fun isCar(context: Context): Boolean =
            context.packageManager.hasSystemFeature(PackageManager.FEATURE_AUTOMOTIVE)
    }
}
