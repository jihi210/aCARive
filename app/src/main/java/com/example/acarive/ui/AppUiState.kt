package com.example.acarive.ui

import com.example.acarive.data.SampleData
import com.example.acarive.data.model.DriveSuggestion
import com.example.acarive.data.model.HeroRecommendation
import com.example.acarive.data.model.NowPlaying
import com.example.acarive.data.model.Route
import com.example.acarive.data.model.UserProfile
import com.example.acarive.data.model.VehicleInfo
import com.example.acarive.data.model.Weather

data class AppUiState(
    // 저장된 값을 읽기 전에는 기본값 (AppViewModel.loadProfile)
    val profile: UserProfile = UserProfile(),
    // 카탈로그를 읽기 전에는 비어 있다 (AppViewModel.loadCourses)
    val routes: List<Route> = emptyList(),
    val hero: HeroRecommendation? = null,
    val weather: Weather = SampleData.weather,
    val nowPlaying: NowPlaying = SampleData.nowPlaying,
    val favoriteRouteIds: Set<String> = emptySet(),
    val isDriveMode: Boolean = true,
    /**
     * 차량이 주행 제한 상태인지 여부. true 면 설정·동의처럼 조작이 많은 기능과 긴 글을 막는다.
     * [AppViewModel] 참고.
     */
    val isDrivingRestricted: Boolean = false,
    val isPlaying: Boolean = false,
    /** null 이면 차량 미연결 상태 */
    val vehicle: VehicleInfo? = null,
    // 주행 중 추천 카드. 비어 있으면 카드를 띄우지 않는다 (AppViewModel.setFreeDriving)
    val driveSuggestions: List<DriveSuggestion> = emptyList(),
    val toastMessage: String? = null,
)
