package com.example.acarive.data.course

import com.example.acarive.data.model.HeroRecommendation
import com.example.acarive.data.model.Route
import java.util.Locale

// 추천 결과(CourseRecommendation) -> 화면용 Route 변환

private const val UNKNOWN = "-"

fun CourseRecommendation.toRoute(): Route = Route(
    id = course.id,
    name = course.name,
    area = course.area,
    // 현 위치 기준 왕복 운전 시간. TourAPI 소요시간은 관광 시간까지 들어 있어 쓰지 않는다.
    duration = estimate?.let { formatMinutes(roundTo10(it.roundTripMinutes)) } ?: UNKNOWN,
    distance = estimate?.let { String.format(Locale.KOREA, "%.0f km", it.totalKm) } ?: UNKNOWN,
    // 테마 정의 순서대로 보여준다
    tags = CourseTheme.entries.filter { it in course.themes }.map { it.label },
    // 코스 자체 이미지가 없으면 사진이 있는 첫 경유지
    imageUrl = (course.imageUrl ?: course.stops.firstNotNullOfOrNull { it.imageUrl })?.toHttps(),
    congestion = null,
    description = course.overview.cleanText(),
)

// originLabel: 위치를 못 받았을 때 기준 지점 이름 ("서울시청"). null 이면 현 위치
// TODO(recommend): 날씨·시간대를 반영한 문구
fun heroFor(rec: CourseRecommendation, originLabel: String?): HeroRecommendation {
    val route = rec.toRoute()
    // 취향과 겹친 테마를 먼저
    val theme = CourseTheme.entries.firstOrNull { it in rec.matchedThemes }
        ?: CourseTheme.entries.firstOrNull { it in rec.course.themes }
    val eta = rec.estimate?.let {
        "${originLabel ?: "현 위치"}에서 ${formatMinutes(roundTo10(it.toStartMinutes))} · 왕복 ${route.duration}"
    } ?: route.area
    return HeroRecommendation(
        route = route,
        headline = theme?.headline ?: "오늘은 이 길로\n떠나보세요",
        etaLabel = eta,
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

// 추정치라 10분 단위로만 보여준다
internal fun roundTo10(minutes: Int): Int = ((minutes + 5) / 10 * 10).coerceAtLeast(10)

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
