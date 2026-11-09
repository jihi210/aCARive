package com.example.acarive.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.acarive.data.SampleData
import com.example.acarive.data.model.VehicleBrand
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

private const val TOAST_DURATION_MS = 2_200L

class AppViewModel : ViewModel() {

    // TODO(backend): 초기 상태를 SampleData 대신 API 응답으로 채우기
    //  - 사용자 프로필/취향, 즐겨찾기 목록, 연결된 차량
    //  - 추천 코스 + 히어로 추천: 취향 프로필 + 타 사용자 주행 기록 기반 추천 API, 공공/외부 풍경 데이터
    //  - 날씨: 현재 위치 기반 날씨 API
    //  - 로딩/에러 상태도 AppUiState 에 추가 필요
    private val _uiState = MutableStateFlow(AppUiState())
    val uiState: StateFlow<AppUiState> = _uiState.asStateFlow()

    private var toastJob: Job? = null

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

    fun toggleFavorite(routeId: Int) {
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
