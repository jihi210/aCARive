package com.example.acarive.ui

import android.app.Application
import android.content.pm.PackageManager
import android.util.Log
import androidx.compose.runtime.snapshotFlow
import androidx.core.content.ContextCompat
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.acarive.data.SampleData
import com.example.acarive.data.course.CourseCatalog
import com.example.acarive.data.course.CourseCatalogRepository
import com.example.acarive.data.course.CourseRecommender
import com.example.acarive.data.course.heroFor
import com.example.acarive.data.course.toRoute
import com.example.acarive.data.model.VehicleInfo
import com.example.acarive.location.LocationPoint
import com.example.acarive.location.PleosLocation
import com.example.acarive.location.mockRoute
import com.example.acarive.ui.record.USE_MOCK_LOCATION
import com.example.acarive.vehicle.PleosVehicle
import com.example.acarive.vehicle.VehicleData
import com.example.acarive.vehicle.label
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

private const val TOAST_DURATION_MS = 2_200L
private const val TAG = "AppViewModel"

// 위치를 못 받았을 때 추천 기준 지점
private val DEFAULT_ORIGIN = LocationPoint(37.5665, 126.9780)
private const val DEFAULT_ORIGIN_LABEL = "서울시청"
// 이만큼 움직였을 때만 추천을 다시 계산한다
private const val RECOMMEND_MOVE_KM = 5.0

// true 면 Pleos 차량 서비스 대신 목업 차량을 연결한다. 일반 폰·에뮬레이터에서 화면 확인할 때만 켠다.
const val USE_MOCK_VEHICLE = false

class AppViewModel(application: Application) : AndroidViewModel(application) {

    // TODO(backend): 초기 상태를 SampleData 대신 API 응답으로 채우기
    //  - 사용자 프로필/취향, 즐겨찾기 목록(지금은 메모리에만 있어 앱을 끄면 사라짐), 연결된 차량
    //  - 추천: assets 카탈로그 + 위치·취향으로 앱에서 정렬 (CourseRecommender). 타 사용자 기록은 아직
    //  - 날씨: 현재 위치 기반 날씨 API
    //  - 로딩/에러 상태도 AppUiState 에 추가 필요
    private val _uiState = MutableStateFlow(AppUiState())
    val uiState: StateFlow<AppUiState> = _uiState.asStateFlow()

    private var toastJob: Job? = null

    private val courseRepository = CourseCatalogRepository(application)
    private var catalog: CourseCatalog? = null

    // 추천 기준 위치. 실제 위치를 받기 전에는 기본 지점
    private var origin: LocationPoint = if (USE_MOCK_LOCATION) mockRoute.first() else DEFAULT_ORIGIN
    private var hasRealOrigin = USE_MOCK_LOCATION
    private var location: PleosLocation? = null
    // 위치 변화로 계산한 속도. 차량 카드에 쓴다
    private val speedKmh = MutableStateFlow<Int?>(null)

    private var vehicle: PleosVehicle? = null
    private var vehicleJob: Job? = null

    init {
        loadCourses()
        startLocation()
    }

    // 위치 권한이 있으면 위치를 받기 시작한다. 권한을 받은 뒤 다시 불러도 된다.
    fun startLocation() {
        if (USE_MOCK_LOCATION || location != null) return
        val app = getApplication<Application>()
        val granted = ContextCompat.checkSelfPermission(app, PleosLocation.PERMISSION) ==
            PackageManager.PERMISSION_GRANTED
        if (!granted) return
        val source = PleosLocation(app).also { location = it }
        source.start()
        viewModelScope.launch {
            snapshotFlow { source.latest }.filterNotNull().collect(::onLocation)
        }
        viewModelScope.launch {
            snapshotFlow { source.speedKmh }.collect { speedKmh.value = it }
        }
    }

    private fun onLocation(point: LocationPoint) {
        if (hasRealOrigin && CourseRecommender.distanceKm(origin, point) < RECOMMEND_MOVE_KM) return
        origin = point
        hasRealOrigin = true
        recommend()
    }

    private fun recommend() {
        val catalog = catalog ?: return
        val preferred = CourseRecommender.themesFromPreferences(_uiState.value.profile.preferences)
        val recs = CourseRecommender.recommend(catalog.courses, origin, preferred)
        val originLabel = if (hasRealOrigin) null else DEFAULT_ORIGIN_LABEL
        _uiState.update {
            it.copy(routes = recs.map { r -> r.toRoute() }, hero = recs.firstOrNull()?.let { r -> heroFor(r, originLabel) })
        }
    }

    private fun loadCourses() {
        viewModelScope.launch {
            try {
                catalog = courseRepository.load()
                recommend()
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "코스 카탈로그 읽기 실패", e)
                showToast("코스를 불러오지 못했어요")
            }
        }
    }

    // TODO(vehicle): Pleos Connect 빌드에서 CarUxRestrictionsManager 를 구독해 isDrivingRestricted 를 갱신
    //  (NO_SETUP, NO_KEYBOARD, LIMIT_CONTENT 플래그 기준). 지금은 항상 false 라 제한 UI가 동작하지 않는다.

    /** 주행 중 막힌 기능을 눌렀을 때 안내 */
    fun showParkingRequired() {
        showToast("주차 후 이용할 수 있어요")
    }

    fun toggleDriveMode() {
        // TODO(backend): 드라이브/일반 주행 구분을 서버에 저장
        _uiState.update { it.copy(isDriveMode = !it.isDriveMode) }
    }

    fun togglePlaying() {
        // TODO(backend): Spotify 재생/일시정지 연동. 재생 상태는 Spotify 콜백 기준으로 갱신
        // TODO(vehicle): Pleos Connect 오디오 포커스 정책 적용 (길안내 음성 시 일시정지/볼륨 조절, 포커스 손실 처리)
        _uiState.update { it.copy(isPlaying = !it.isPlaying) }
    }

    fun toggleFavorite(routeId: String) {
        // TODO(backend): 즐겨찾기 추가/삭제 API 호출 (실패 시 롤백)
        _uiState.update { state ->
            val favorites = state.favoriteRouteIds
            state.copy(
                favoriteRouteIds = if (routeId in favorites) favorites - routeId else favorites + routeId,
            )
        }
    }

    // 현대차만 지원. 로그인 없이 Pleos Connect 차량 서비스에서 바로 데이터를 받는다.
    // 차량 권한(PleosVehicle.PERMISSIONS)은 화면에서 먼저 요청한다.
    fun connectVehicle() {
        if (USE_MOCK_VEHICLE) {
            _uiState.update { it.copy(vehicle = SampleData.connectedVehicle) }
            showToast("목업 차량이 연결됐어요")
            return
        }
        val app = getApplication<Application>()
        if (!PleosVehicle.isAvailable(app)) {
            showToast("이 기기에서는 차량 데이터를 받을 수 없어요")
            return
        }
        if (vehicle != null) return
        val source = PleosVehicle(app).also { vehicle = it }
        source.start()
        vehicleJob = viewModelScope.launch {
            combine(source.data, speedKmh) { data, speed -> data.toVehicleInfo(speed) }
                .collect { info -> _uiState.update { it.copy(vehicle = info) } }
        }
        showToast("현대 차량이 연결됐어요")
    }

    fun disconnectVehicle() {
        stopVehicle()
        _uiState.update { it.copy(vehicle = null) }
        showToast("차량 연결을 해제했어요")
    }

    private fun stopVehicle() {
        vehicleJob?.cancel()
        vehicleJob = null
        vehicle?.stop()
        vehicle = null
    }

    override fun onCleared() {
        location?.stop()
        stopVehicle()
    }

    fun showToast(message: String) {
        toastJob?.cancel()
        _uiState.update { it.copy(toastMessage = message) }
        toastJob = viewModelScope.launch {
            delay(TOAST_DURATION_MS)
            _uiState.update { it.copy(toastMessage = null) }
        }
    }
}

// 차량 SDK 값 -> 화면용. 배터리 값이 오면 EV 로 본다.
private fun VehicleData.toVehicleInfo(speedKmh: Int?): VehicleInfo = VehicleInfo(
    // 차종 이름은 SDK 에서 주지 않는다
    name = "내 현대차",
    statusLabel = "실시간 수신 중",
    vinSuffix = vin?.takeLast(4),
    gear = gear?.label,
    speedKmh = speedKmh,
    energyLabel = if (evBatteryPercent != null) "배터리" else "연료",
    energyPercent = evBatteryPercent ?: fuelPercent,
    rangeKm = rangeMeters?.let { (it / 1000).roundToInt() },
)
