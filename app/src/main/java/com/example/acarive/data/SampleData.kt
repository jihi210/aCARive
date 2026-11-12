package com.example.acarive.data

import com.example.acarive.data.model.HeroRecommendation
import com.example.acarive.data.model.NowPlaying
import com.example.acarive.data.model.Route
import com.example.acarive.data.model.UserProfile
import com.example.acarive.data.model.VehicleInfo
import com.example.acarive.data.model.Weather

/**
 * 디자인 시안에 들어 있던 목업 데이터. 코스는 이제 assets 카탈로그에서 읽고, 여기 코스는 미리보기에만 쓴다.
 * 나머지도 백엔드가 연결되면 [com.example.acarive.ui.AppViewModel] 에서 실제 데이터를 받아온다.
 */
object SampleData {
    private const val DESCRIPTION =
        "물과 숲이 번갈아 펼쳐지는 길을 따라 달리는 코스예요. 전망이 좋은 세 곳을 경유하며, 잠시 쉬어가기 좋은 카페도 포함했어요."

    val routes = listOf(
        Route(
            id = "sample:1",
            name = "북한강 물빛 드라이브",
            area = "양평 · 청평",
            duration = "2시간 20분",
            distance = "86 km",
            tags = listOf("강변", "한적한"),
            imageUrl = "https://images.unsplash.com/photo-1761775607993-cda287a4e1e5?auto=format&fit=crop&w=900&q=85",
            congestion = "여유",
            description = DESCRIPTION,
        ),
        Route(
            id = "sample:2",
            name = "해안 절벽 파노라마",
            area = "강릉 · 정동진",
            duration = "2시간 50분",
            distance = "112 km",
            tags = listOf("바다", "일몰"),
            imageUrl = "https://images.unsplash.com/photo-1778352490319-76e76a21f0f8?auto=format&fit=crop&w=900&q=85",
            congestion = "여유",
            description = DESCRIPTION,
        ),
        Route(
            id = "sample:3",
            name = "초록 능선 와인딩",
            area = "가평 · 유명산",
            duration = "2시간 10분",
            distance = "74 km",
            tags = listOf("숲길", "커브"),
            imageUrl = "https://images.unsplash.com/photo-1758384265569-f03bed6d0757?auto=format&fit=crop&w=900&q=85",
            congestion = "여유",
            description = DESCRIPTION,
        ),
    )

    val hero = HeroRecommendation(
        route = routes.first(),
        headline = "비가 그친 뒤,\n물빛 따라 달려보세요",
        etaLabel = "현 위치에서 18분 · 교통 원활",
    )

    val weather = Weather(temperature = 18, description = "가벼운 비")

    val nowPlaying = NowPlaying(
        mixName = "Rainy Road Mix",
        title = "Soft Drive",
        subtitle = "비 오는 오후 · 여유로운 주행",
        contextTags = listOf("# 빗길", "# 42km/h", "# 편안한 흐름"),
    )

    val connectedVehicle = VehicleInfo(
        name = "IONIQ 5",
        statusLabel = "목업 데이터",
        vinSuffix = "3456",
        gear = "D",
        speedKmh = 42,
        energyLabel = "배터리",
        energyPercent = 74,
        rangeKm = 338,
    )

    val profile = UserProfile(
        preferences = listOf("바다", "한적한 길", "노을"),
        spotifyConnected = true,
        recommendWhileDriving = true,
    )

    val favoriteRouteIds = setOf("sample:2")
}
