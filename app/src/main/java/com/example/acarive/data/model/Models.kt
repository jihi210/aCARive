package com.example.acarive.data.model

data class Route(
    // 카탈로그 코스 id. 예: "tour:1846248"
    val id: String,
    val name: String,
    val area: String,
    val duration: String,
    val distance: String,
    val tags: List<String>,
    val imageUrl: String?,
    // 코스 경유지 수. 0 이면 화면에 "-"
    val stopCount: Int,
    val description: String,
)

data class HeroRecommendation(
    val route: Route,
    val headline: String,
    val etaLabel: String,
)

// 주행 중 추천 카드 한 줄
data class DriveSuggestion(
    val route: Route,
    // 예: "지금 위치에서 20분"
    val etaLabel: String,
)

data class Weather(
    val temperature: Int,
    val description: String,
)

data class NowPlaying(
    val mixName: String,
    val title: String,
    val subtitle: String,
    val contextTags: List<String>,
)

// 연결된 차량. 못 받은 값은 null 이고 화면에는 "-" 로 보인다.
data class VehicleInfo(
    val name: String,
    // "실시간 수신 중" 같은 연결 상태 문구
    val statusLabel: String,
    // VIN 끝 4자리 (번호판 대신 차량 구분용)
    val vinSuffix: String?,
    // P/R/N/D
    val gear: String?,
    val speedKmh: Int?,
    // EV 면 "배터리", 내연기관이면 "연료"
    val energyLabel: String,
    val energyPercent: Int?,
    val rangeKm: Int?,
)

data class DriveHistoryItem(
    val sessionId: Long,
    val date: String,
    val distance: String,
    val duration: String,
)

data class DriveSummary(
    val year: Int,
    val totalKm: Double,
    val driveCount: Int,
    val totalDuration: String,
    val recentDrives: List<DriveHistoryItem>,
)

// 로그인이 없어서 이름 같은 신원 정보는 두지 않는다. 기기에 저장되는 취향·설정만 담는다.
// 기본값 = 처음 설치했을 때 상태 (ProfileRepository)
data class UserProfile(
    val preferences: List<String> = emptyList(),
    // Spotify 를 연결해 둔 적이 있으면 true. 앱을 켤 때 자동으로 다시 연결한다 (실제 연결 상태는 AppUiState.spotify)
    val spotifyConnected: Boolean = false,
    val recommendWhileDriving: Boolean = true,
)
