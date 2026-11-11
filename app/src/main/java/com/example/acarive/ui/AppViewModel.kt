package com.example.acarive.ui

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.acarive.data.SampleData
import com.example.acarive.data.course.CourseCatalogRepository
import com.example.acarive.data.course.heroFor
import com.example.acarive.data.course.toRoutes
import com.example.acarive.data.model.VehicleBrand
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

private const val TOAST_DURATION_MS = 2_200L
private const val TAG = "AppViewModel"

class AppViewModel(application: Application) : AndroidViewModel(application) {

    // TODO(backend): 초기 상태를 SampleData 대신 API 응답으로 채우기
    //  - 사용자 프로필/취향, 즐겨찾기 목록(지금은 메모리에만 있어 앱을 끄면 사라짐), 연결된 차량
    //  - 추천: 지금은 assets 카탈로그 전체를 보여준다. 위치·취향·타 사용자 기록 기반 정렬은 아직
    //  - 날씨: 현재 위치 기반 날씨 API
    //  - 로딩/에러 상태도 AppUiState 에 추가 필요
    private val _uiState = MutableStateFlow(AppUiState())
    val uiState: StateFlow<AppUiState> = _uiState.asStateFlow()

    private var toastJob: Job? = null

    private val courseRepository = CourseCatalogRepository(application)

    init {
        loadCourses()
    }

    private fun loadCourses() {
        viewModelScope.launch {
            try {
                val routes = courseRepository.load().toRoutes()
                _uiState.update {
                    it.copy(routes = routes, hero = routes.firstOrNull { r -> r.tags.isNotEmpty() }?.let(::heroFor))
                }
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

    fun selectBrand(brand: VehicleBrand) {
        _uiState.update { it.copy(selectedBrand = brand) }
    }

    fun connectVehicle() {
        // TODO(backend): 선택한 브랜드의 커넥티드카 계정 OAuth 인증 + 데이터 이용 동의 저장 후
        //  실제 차량 정보와 실시간 데이터(속도, 주행 상태, 배터리)를 구독. 지금은 브랜드와 무관하게 목업 차량이 연결된다.
        val brand = _uiState.value.selectedBrand
        _uiState.update { it.copy(vehicle = SampleData.connectedVehicle) }
        showToast("${brand.label} 차량이 연결됐어요")
    }

    fun disconnectVehicle() {
        // TODO(backend): 차량 연결 해제 API 호출 및 데이터 수신 중단
        _uiState.update { it.copy(vehicle = null) }
        showToast("차량 연결을 해제했어요")
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
