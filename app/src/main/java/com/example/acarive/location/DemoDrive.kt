package com.example.acarive.location

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.math.roundToInt
import kotlin.math.sin

// 1초마다 한 칸
private const val TICK_MS = 1_000L
// 화면 속도는 실제처럼 보이고, 위치는 이 배수만큼 빨리 움직인다 (72km/h -> 1초에 400m)
private const val TIME_SCALE = 20.0
// 화면 속도: 기본 65km/h 에서 ±15 로 천천히 오르내린다
private const val BASE_KMH = 65.0
private const val WOBBLE_KMH = 15.0
private const val WOBBLE_PERIOD_TICKS = 40.0

// 데모 상태. start 가 null 이면 데모가 꺼진 것 (실제 위치를 쓴다)
data class DemoDriveState(
    // 지도에서 찍은 출발 위치
    val start: LocationPoint? = null,
    // 지금 시뮬레이션 위치
    val position: LocationPoint? = null,
    // 움직이는 중이면 화면용 속도, 멈춰 있으면 0
    val speedKmh: Int = 0,
) {
    val isActive: Boolean get() = start != null
}

// 시연용 가짜 주행. 지도에서 현재 위치를 찍으면 그 위치를 실제 위치 대신 쓰고,
// 주행을 시작하면 경유지를 실제 도로로 이어 따라간다 (RoadRoute). 앱 전체에서 하나만 쓴다 (AppViewModel, DriveRecordViewModel).
object DemoDrive {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private var driveJob: Job? = null

    private val _state = MutableStateFlow(DemoDriveState())
    val state: StateFlow<DemoDriveState> = _state.asStateFlow()

    val isActive: Boolean get() = _state.value.isActive

    // 데모가 켜져 있으면 시뮬레이션 위치
    val position: LocationPoint? get() = _state.value.position

    // 찍은 위치에서 다시 시작한다
    fun setStart(point: LocationPoint) {
        stopDriving()
        _state.value = DemoDriveState(start = point, position = point)
    }

    fun clear() {
        stopDriving()
        _state.value = DemoDriveState()
    }

    // 지금 위치에서 waypoints 를 순서대로 지나간다. 다 가면 마지막 지점에 멈춘다.
    // 실제 도로 경로를 먼저 받아서 따라가고, 못 받으면 경유지를 직선으로 잇는다
    fun driveAlong(waypoints: List<LocationPoint>) {
        val from = position ?: return
        stopDriving()
        val stops = listOf(from) + waypoints
        if (stops.size < 2) return
        driveJob = scope.launch {
            val path = RoadRoute.fetch(stops) ?: stops
            var tick = 0
            var segment = 0
            var current = from
            while (segment < path.size - 1) {
                val kmh = BASE_KMH + WOBBLE_KMH * sin(2 * PI * tick / WOBBLE_PERIOD_TICKS)
                var stepKm = kmh / 3600 * TIME_SCALE * (TICK_MS / 1000.0)
                // 한 칸에 경유지를 여러 개 지날 수도 있다
                while (stepKm > 0 && segment < path.size - 1) {
                    val target = path[segment + 1]
                    val left = distanceKm(current.lat, current.lng, target.lat, target.lng)
                    if (left <= stepKm) {
                        current = target
                        stepKm -= left
                        segment++
                    } else {
                        current = lerp(current, target, stepKm / left)
                        stepKm = 0.0
                    }
                }
                val moving = segment < path.size - 1
                val point = current
                _state.update { it.copy(position = point, speedKmh = if (moving) kmh.roundToInt() else 0) }
                tick++
                delay(TICK_MS)
            }
        }
    }

    // 그 자리에 멈춘다
    fun stopDriving() {
        driveJob?.cancel()
        driveJob = null
        _state.update { it.copy(speedKmh = 0) }
    }
}

// 도로 좌표 사이는 짧아서 위경도를 그대로 선형 보간해도 된다
private fun lerp(a: LocationPoint, b: LocationPoint, t: Double) =
    LocationPoint(a.lat + (b.lat - a.lat) * t, a.lng + (b.lng - a.lng) * t)
