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
    val routes: List<Route> = SampleData.routes,
    val hero: HeroRecommendation = SampleData.hero,
    val weather: Weather = SampleData.weather,
    val nowPlaying: NowPlaying = SampleData.nowPlaying,
    val favoriteRouteIds: Set<Int> = SampleData.favoriteRouteIds,
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
