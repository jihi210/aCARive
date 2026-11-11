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

enum class VehicleBrand(val label: String) {
    Hyundai("현대"),
    Genesis("제네시스"),
    Kia("기아"),
}

data class VehicleInfo(
    val name: String,
    val plateNumber: String,
    val lastSynced: String,
    val speedKmh: Int,
    val batteryPercent: Int,
    val rangeKm: Int,
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
