package com.example.acarive.ui

import com.example.acarive.data.SampleData
import com.example.acarive.data.model.HeroRecommendation
import com.example.acarive.data.model.NowPlaying
import com.example.acarive.data.model.Route
import com.example.acarive.data.model.UserProfile
import com.example.acarive.data.model.VehicleBrand
import com.example.acarive.data.model.VehicleInfo
import com.example.acarive.data.model.Weather

data class AppUiState(
    val profile: UserProfile = SampleData.profile,
    // 카탈로그를 읽기 전에는 비어 있다 (AppViewModel.loadCourses)
    val routes: List<Route> = emptyList(),
    val hero: HeroRecommendation? = null,
    val weather: Weather = SampleData.weather,
    val nowPlaying: NowPlaying = SampleData.nowPlaying,
    val favoriteRouteIds: Set<String> = emptySet(),
    val isDriveMode: Boolean = true,
    /**
     * 차량이 주행 제한 상태인지 여부. true 면 설정·로그인·동의처럼 조작이 많은 기능과 긴 글을 막는다.
     * [AppViewModel] 참고.
     */
    val isDrivingRestricted: Boolean = false,
    val isPlaying: Boolean = false,
    /** null 이면 차량 미연결 상태 */
    val vehicle: VehicleInfo? = null,
    val selectedBrand: VehicleBrand = VehicleBrand.Hyundai,
    val toastMessage: String? = null,
)
