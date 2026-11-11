package com.example.acarive.data.course

import com.example.acarive.data.model.HeroRecommendation
import com.example.acarive.data.model.Route
import java.util.Locale

// 카탈로그 코스(DriveCourse) -> 화면용 Route 변환

private const val UNKNOWN = "-"

fun DriveCourse.toRoute(): Route = Route(
    id = id,
    name = name,
    area = area,
    duration = takeTimeMinutes?.let(::formatMinutes) ?: takeTimeText ?: UNKNOWN,
    distance = distanceKm?.let { String.format(Locale.KOREA, "%.0f km", it) } ?: distanceText ?: UNKNOWN,
    // 테마 정의 순서대로 보여준다
    tags = CourseTheme.entries.filter { it in themes }.map { it.label },
    // 코스 자체 이미지가 없으면 사진이 있는 첫 경유지
    imageUrl = (imageUrl ?: stops.firstNotNullOfOrNull { it.imageUrl })?.toHttps(),
    congestion = null,
    description = overview.cleanText(),
)

// 홈/코스 목록 순서: 테마가 붙은 코스를 먼저 (위치 기반 추천 전 임시 정렬)
fun CourseCatalog.toRoutes(): List<Route> =
    courses.sortedBy { it.themes.isEmpty() }.map { it.toRoute() }

// TODO(recommend): 현재 위치·날씨·취향을 반영한 문구와 실제 소요 시간으로 교체
fun heroFor(route: Route): HeroRecommendation {
    val theme = route.tags.firstOrNull()?.let(CourseTheme::fromLabel)
    return HeroRecommendation(
        route = route,
        headline = theme?.headline ?: "오늘은 이 길로\n떠나보세요",
        etaLabel = "${route.area} · 코스 ${route.duration}",
    )
}

private val CourseTheme.headline: String
    get() = when (this) {
        CourseTheme.Ocean -> "바다를 곁에 두고\n달려보세요"
        CourseTheme.Riverside -> "물빛 따라\n달려보세요"
        CourseTheme.Forest -> "초록 숲길 사이로\n달려보세요"
        CourseTheme.Mountain -> "산과 계곡을 따라\n달려보세요"
        CourseTheme.Viewpoint -> "탁 트인 풍경 속으로\n달려보세요"
        CourseTheme.Sunset -> "노을을 향해\n달려보세요"
        CourseTheme.NightView -> "반짝이는 야경 속으로\n달려보세요"
        CourseTheme.Seasonal -> "계절이 물든 길로\n달려보세요"
        CourseTheme.Quiet -> "한적한 길에서\n쉬어가세요"
        CourseTheme.Winding -> "굽이진 길을\n즐겨보세요"
    }

internal fun formatMinutes(minutes: Int): String {
    val h = minutes / 60
    val m = minutes % 60
    return when {
        h == 0 -> "${m}분"
        m == 0 -> "${h}시간"
        else -> "${h}시간 ${m}분"
    }
}

// 안드로이드는 기본으로 http 이미지를 막는다. visitkorea 이미지는 https 도 된다.
internal fun String.toHttps(): String = if (startsWith("http://")) "https://" + removePrefix("http://") else this

// TourAPI 개요에 글자 그대로 들어 있는 "\n" 과 겹친 공백 정리
internal fun String.cleanText(): String =
    replace("\\n", " ").replace(Regex("\\s+"), " ").trim()
