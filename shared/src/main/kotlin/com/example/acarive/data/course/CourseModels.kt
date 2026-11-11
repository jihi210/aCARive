package com.example.acarive.data.course

import com.example.acarive.location.LocationPoint
import kotlinx.serialization.Serializable

/*
 * 추천에 쓰는 코스 카탈로그 모델.
 *
 * TourAPI 에서 한 달에 한 번 수집한 원본(data.tour)을 이 모델로 정리해 둔다.
 * 추천 기준 (CLAUDE.md "(2) 좋은 풍경이 있는 코스 추천"):
 *  - 현재 위치에서 왕복 2~3시간 → [DriveCourse.start] 와 [DriveCourse.takeTimeMinutes]
 *  - 명시적 프로필의 취향 태그 → [DriveCourse.themes] ([CourseTheme])
 *  - 타 사용자의 드라이브 기록 → [CourseUsageStats] (서버 집계, 카탈로그와 갱신 주기가 달라 따로 둔다)
 *  - 풍경 데이터 → [ScenicSpot] (코스 주변 자연 관광지)
 * 서버가 생기면 이 모델이 그대로 API 응답 형태가 되도록 @Serializable 로 둔다.
 */

/** 한 달 주기로 수집한 코스·풍경 지점 묶음. */
@Serializable
data class CourseCatalog(
    val collectedAt: Long,
    val courses: List<DriveCourse>,
    val spots: List<ScenicSpot>,
) {
    /** 수집한 지 [REFRESH_INTERVAL_MS] 가 지났으면 다시 수집한다. */
    fun isStale(now: Long): Boolean = now - collectedAt >= REFRESH_INTERVAL_MS

    companion object {
        const val REFRESH_INTERVAL_MS = 30L * 24 * 60 * 60 * 1000
    }
}

/** 드라이브 코스 하나. TourAPI 여행코스(contentTypeId=25) 한 건에서 만든다. */
@Serializable
data class DriveCourse(
    /** 앱 내부 id. 출처를 앞에 붙인다. 예: "tour:2345678" */
    val id: String,
    val source: CourseSource,
    val name: String,
    /** "강원 강릉시" 처럼 짧은 지역명 */
    val area: String,
    /** 법정동 시도 코드. 지역별로 나눠 수집·필터할 때 쓴다. */
    val regionCode: String?,
    /** HTML 태그를 지운 개요 */
    val overview: String,
    val imageUrl: String?,
    val imageCopyright: String?,
    val themes: Set<CourseTheme>,
    /** 경유 순서대로 */
    val stops: List<CourseStop>,
    /** TourAPI 가 준 코스 총 거리(km). 형식이 제각각이라 못 읽으면 null */
    val distanceKm: Double?,
    /** TourAPI 가 준 코스 총 소요시간(분). "1일" 같은 값은 null. 현 위치 왕복 시간은 추천할 때 따로 계산한다. */
    val takeTimeMinutes: Int?,
    /** 원본 문자열. 파싱이 틀렸을 때 확인용 */
    val distanceText: String?,
    val takeTimeText: String?,
    /** TourAPI modifiedtime. 다음 수집 때 바뀐 것만 다시 받기 위한 기준 */
    val sourceModifiedAt: Long?,
    val collectedAt: Long,
) {
    /** 코스 출발점: 좌표가 있는 첫 경유지 */
    val start: LocationPoint? get() = stops.firstNotNullOfOrNull { it.location }

    /** 좌표가 있는 경유지만 (지도 경로·Send to Car 용) */
    val waypoints: List<LocationPoint> get() = stops.mapNotNull { it.location }
}

@Serializable
data class CourseSource(
    val provider: Provider,
    /** 출처에서의 id. TourAPI 는 contentid */
    val externalId: String,
) {
    enum class Provider { TourApi }
}

/** 코스의 경유지. TourAPI detailInfo2 의 sub 항목 + 경유지 자체의 detailCommon2. */
@Serializable
data class CourseStop(
    /** 0부터 */
    val order: Int,
    /** 경유지의 TourAPI contentid. 없는 경유지도 있다. */
    val contentId: String?,
    val name: String,
    val location: LocationPoint?,
    val overview: String?,
    val imageUrl: String?,
)

/** 풍경이 좋은 지점. TourAPI 관광지(contentTypeId=12) 중 자연 경관. 코스 주변 볼거리·테마 보강에 쓴다. */
@Serializable
data class ScenicSpot(
    val id: String,
    val source: CourseSource,
    val name: String,
    val area: String,
    val location: LocationPoint,
    val overview: String?,
    val imageUrl: String?,
    val imageCopyright: String?,
    val themes: Set<CourseTheme>,
    /** "봄~가을" 처럼 즐기기 좋은 시기. 계절 추천에 쓴다. */
    val useSeason: String?,
    /** 원본 분류 코드. 테마 규칙을 바꿀 때 다시 분류할 수 있게 남겨 둔다. */
    val categoryCodes: CategoryCodes,
    val sourceModifiedAt: Long?,
    val collectedAt: Long,
)

/** TourAPI 의 기존 분류(cat)와 신규 분류체계(lclsSystm) 코드 */
@Serializable
data class CategoryCodes(
    val cat1: String? = null,
    val cat2: String? = null,
    val cat3: String? = null,
    val lclsSystm1: String? = null,
    val lclsSystm2: String? = null,
    val lclsSystm3: String? = null,
)

/**
 * 타 사용자 드라이브 기록 집계. "사람들이 자주 이용하는 경로" 추천에 쓴다.
 * TourAPI 가 아니라 우리 서버가 업로드된 주행 기록을 코스에 맞춰 집계해 채운다.
 */
@Serializable
data class CourseUsageStats(
    val courseId: String,
    /** 최근 [periodDays] 일 동안 이 코스로 달린 횟수 */
    val driveCount: Int,
    /** 같은 기간 달린 사람 수 */
    val driverCount: Int,
    /** 즐겨찾기한 사람 수 */
    val favoriteCount: Int,
    val periodDays: Int,
    val updatedAt: Long,
)

/**
 * 취향 태그. 명시적 프로필(취향 설문)에서 고르는 테마이자 코스 필터 칩.
 * [keywords] 는 TourAPI 제목·개요·테마 문구에서 테마를 추정할 때 쓴다.
 * 분류 코드(cat3/lclsSystm3) 기반 규칙은 실제 코드표를 확인한 뒤 추가한다.
 */
@Serializable
enum class CourseTheme(val label: String, val keywords: List<String>) {
    Ocean("바다", listOf("바다", "해안", "해변", "해수욕장", "항구", "포구", "등대", "갯벌", "바닷길")),
    Riverside("강변", listOf("강변", "강가", "한강", "호수", "호반", "저수지", "댐")),
    Forest("숲길", listOf("숲", "수목원", "휴양림", "숲길", "편백", "자작나무")),
    Mountain("산·계곡", listOf("계곡", "폭포", "능선", "국립공원", "고개", "산림")),
    Viewpoint("전망", listOf("전망", "조망", "파노라마", "절경", "경관")),
    Sunset("노을", listOf("노을", "일몰", "해넘이", "낙조")),
    NightView("야경", listOf("야경", "별빛", "은하수", "야간")),
    Seasonal("꽃·단풍", listOf("꽃", "벚꽃", "단풍", "억새", "유채", "메밀", "철쭉")),
    Quiet("한적한", listOf("한적", "조용", "호젓", "고즈넉")),
    Winding("커브", listOf("와인딩", "굽이", "고갯길")),
    ;

    companion object {
        /** 문구 안에 키워드가 들어 있는 테마를 모두 돌려준다. */
        fun infer(vararg texts: String?): Set<CourseTheme> {
            val text = texts.filterNotNull().joinToString(" ")
            if (text.isBlank()) return emptySet()
            return entries.filterTo(mutableSetOf()) { theme -> theme.keywords.any { it in text } }
        }

        fun fromLabel(label: String): CourseTheme? = entries.find { it.label == label }
    }
}
