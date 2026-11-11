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
    // 혼잡도 데이터가 아직 없으면 null
    val congestion: String?,
    val description: String,
)

data class HeroRecommendation(
    val route: Route,
    val headline: String,
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

data class UserProfile(
    val firstName: String,
    val fullName: String,
    val initials: String,
    val levelLabel: String,
    val preferences: List<String>,
    val spotifyConnected: Boolean,
    val recommendWhileDriving: Boolean,
)
