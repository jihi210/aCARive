package com.example.acarive.navi

import ai.pleos.playground.navi.constants.NaviErrorCode
import ai.pleos.playground.navi.constants.RouteOption
import ai.pleos.playground.navi.constants.WaypointIndex
import ai.pleos.playground.navi.data.DestinationArrivedInfo
import ai.pleos.playground.navi.data.NavigationScreenRequest
import ai.pleos.playground.navi.data.RequestWaypointInfo
import ai.pleos.playground.navi.data.RouteInfo
import ai.pleos.playground.navi.data.RouteWithWaypointsRequest
import ai.pleos.playground.navi.helper.NaviHelper
import ai.pleos.playground.navi.helper.listener.NaviHelperEventListener
import android.content.Context
import android.content.Intent
import android.util.Log
import com.example.acarive.location.LocationPoint
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

private const val TAG = "CarNavigation"
// 차량 내비 서비스(NaviService)가 받는 액션. 이게 없으면 차량 내비가 없는 기기
private const val NAVI_SERVICE_ACTION = "ai.pleos.playground.intent.action.NAVI_START"

// 내비로 보낼 지점 하나
data class NaviStop(val name: String, val location: LocationPoint)

// 내비가 알려 주는 일. 메인 스레드로 온다는 보장이 없어서 받는 쪽에서 처리한다
interface CarNavigationListener {
    // 경로를 못 찾았을 때 (너무 멀거나 가까움 등)
    fun onRouteFailed(code: NaviErrorCode)
    fun onArrived()
}

// Pleos NaviHelper 래퍼. 코스를 차량 내비 경로(경유지 + 목적지)로 보내 길안내는 차량 내비가 한다.
// 내비는 경유지를 2개까지만 받는다 (WaypointIndex FIRST/SECOND)
class CarNavigation(context: Context, private val listener: CarNavigationListener) {
    private val appContext = context.applicationContext
    private val helper = NaviHelper(appContext)
    private var ready = false

    private val events = object : NaviHelperEventListener {
        override fun onError(code: NaviErrorCode) {
            Log.w(TAG, "내비 오류: $code")
            listener.onRouteFailed(code)
        }

        override fun onDestinationArrived(info: DestinationArrivedInfo) = listener.onArrived()
    }

    // 처음 쓸 때 한 번 연결한다. 실패하면 false
    private suspend fun ensureReady(): Boolean {
        if (ready) return true
        ready = suspendCancellableCoroutine { cont ->
            try {
                helper.initialize(
                    { if (cont.isActive) cont.resume(true) },
                    { e ->
                        Log.e(TAG, "내비 연결 실패", e)
                        if (cont.isActive) cont.resume(false)
                    },
                )
            } catch (e: Exception) {
                Log.e(TAG, "내비 연결 실패", e)
                if (cont.isActive) cont.resume(false)
            }
        }
        if (ready) helper.addListener(events)
        return ready
    }

    // stops 의 마지막이 목적지, 그 앞(최대 2개)이 경유지. 요청을 보냈으면 true
    suspend fun sendRoute(stops: List<NaviStop>): Boolean {
        val destination = stops.lastOrNull() ?: return false
        val waypoints = stops.dropLast(1)
        require(waypoints.size <= MAX_WAYPOINTS) { "경유지는 ${MAX_WAYPOINTS}개까지" }
        if (!ensureReady()) return false
        return try {
            helper.requestRouteWithWaypoints(
                RouteWithWaypointsRequest(
                    destination.toRouteInfo(),
                    waypoints.mapIndexed { i, stop -> stop.toWaypoint(WAYPOINT_INDEX[i]) },
                ),
            )
            true
        } catch (e: Exception) {
            Log.e(TAG, "경로 요청 실패", e)
            false
        }
    }

    // 차량 내비 화면을 앞으로
    suspend fun showNavigation(): Boolean {
        if (!ensureReady()) return false
        return try {
            helper.requestNavigationScreen(NavigationScreenRequest.Foreground)
            true
        } catch (e: Exception) {
            Log.e(TAG, "내비 화면 요청 실패", e)
            false
        }
    }

    // 주행 종료: 내비 안내도 끝낸다
    fun cancelRoute() {
        if (!ready) return
        try {
            helper.cancelRoute()
        } catch (e: Exception) {
            Log.e(TAG, "경로 취소 실패", e)
        }
    }

    fun release() {
        if (!ready) return
        helper.removeListener(events)
        helper.release()
        ready = false
    }

    companion object {
        const val MAX_WAYPOINTS = 2
        private val WAYPOINT_INDEX = listOf(WaypointIndex.FIRST, WaypointIndex.SECOND)

        // 차량 내비 서비스가 있는 기기인지 (일반 폰·에뮬레이터는 false)
        fun isAvailable(context: Context): Boolean =
            context.packageManager.queryIntentServices(Intent(NAVI_SERVICE_ACTION), 0).isNotEmpty()
    }
}

// 내비 POI 가 아니라 좌표로 보내서 POI id·주소는 비워 둔다
private fun NaviStop.toRouteInfo() = RouteInfo(
    latitude = location.lat,
    longitude = location.lng,
    poiName = name,
    poiId = "",
    address = "",
    poiSubId = "",
    routeOption = RouteOption.RECOMMENDED,
)

private fun NaviStop.toWaypoint(index: WaypointIndex) = RequestWaypointInfo(
    latitude = location.lat,
    longitude = location.lng,
    poiName = name,
    waypointIndex = index,
    poiId = "",
    address = "",
    poiSubId = "",
)

// 코스를 차량 내비로 보낸 상태 (주행 화면 표시용)
enum class NaviStatus {
    // 코스 안내 중이 아님
    Idle,
    Sending,
    Sent,
    // 차량 내비가 없는 기기 (일반 폰 등). 진행 상황만 앱에서 보여준다
    Unavailable,
    // 내비에 연결하지 못했거나 요청을 못 보냄
    Failed,
    // 요청은 갔는데 내비가 경로를 못 찾음 (너무 멂, 인터넷 없음 등)
    RouteFailed,
}
