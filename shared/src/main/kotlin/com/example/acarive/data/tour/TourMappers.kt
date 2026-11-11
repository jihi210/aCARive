package com.example.acarive.data.tour

import com.example.acarive.data.course.CategoryCodes
import com.example.acarive.data.course.CourseSource
import com.example.acarive.data.course.CourseStop
import com.example.acarive.data.course.CourseTheme
import com.example.acarive.data.course.DriveCourse
import com.example.acarive.data.course.ScenicSpot
import com.example.acarive.location.LocationPoint
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException

private val TourTimeFormat = DateTimeFormatter.ofPattern("yyyyMMddHHmmss")
private val KoreaZone = ZoneId.of("Asia/Seoul")
private const val TOUR_ID_PREFIX = "tour:"

/**
 * 여행코스 한 건을 [DriveCourse] 로 만든다.
 *
 * @param common 코스의 detailCommon2
 * @param intro 코스의 detailIntro2 (contentTypeId=25)
 * @param stops 코스의 detailInfo2 (경유지 목록)
 * @param stopDetails 경유지 contentid → 경유지의 detailCommon2. 좌표·이미지를 여기서 얻는다.
 */
fun toDriveCourse(
    common: TourCommonDetail,
    intro: TourCourseIntro?,
    stops: List<TourCourseStopItem>,
    stopDetails: Map<String, TourCommonDetail>,
    collectedAt: Long,
): DriveCourse {
    val courseStops = stops
        .sortedBy { it.subnum?.trim()?.toIntOrNull() ?: Int.MAX_VALUE }
        .mapIndexed { index, item ->
            val detail = item.subcontentid?.let { stopDetails[it] }
            CourseStop(
                order = index,
                contentId = item.subcontentid?.takeIf { it.isNotBlank() },
                name = item.subname?.cleanText() ?: detail?.title?.cleanText() ?: "",
                location = detail?.let { tourLocation(it.mapx, it.mapy) },
                overview = item.subdetailoverview?.cleanText(),
                imageUrl = item.subdetailimg.nonBlank() ?: detail?.firstimage.nonBlank(),
            )
        }
    val overview = common.overview?.cleanText().orEmpty()
    return DriveCourse(
        id = TOUR_ID_PREFIX + common.contentid,
        source = CourseSource(CourseSource.Provider.TourApi, common.contentid),
        name = common.title?.cleanText().orEmpty(),
        area = shortArea(common.addr1) ?: shortArea(stopDetails.values.firstOrNull()?.addr1).orEmpty(),
        regionCode = common.lDongRegnCd.nonBlank(),
        overview = overview,
        imageUrl = common.firstimage.nonBlank(),
        imageCopyright = common.cpyrhtDivCd.nonBlank(),
        themes = CourseTheme.infer(
            common.title,
            overview,
            intro?.theme,
            *courseStops.map { it.name }.toTypedArray(),
        ),
        stops = courseStops,
        distanceKm = parseDistanceKm(intro?.distance),
        takeTimeMinutes = parseTakeTimeMinutes(intro?.taketime),
        distanceText = intro?.distance.nonBlank(),
        takeTimeText = intro?.taketime.nonBlank(),
        sourceModifiedAt = parseTourTime(common.modifiedtime),
        collectedAt = collectedAt,
    )
}

/** 관광지(contentTypeId=12) 한 건을 [ScenicSpot] 으로 만든다. 좌표가 없으면 쓸 수 없어 null. */
fun TourCommonDetail.toScenicSpot(intro: TourSpotIntro?, collectedAt: Long): ScenicSpot? {
    val location = tourLocation(mapx, mapy) ?: return null
    val cleanOverview = overview?.cleanText()
    return ScenicSpot(
        id = TOUR_ID_PREFIX + contentid,
        source = CourseSource(CourseSource.Provider.TourApi, contentid),
        name = title?.cleanText().orEmpty(),
        area = shortArea(addr1).orEmpty(),
        location = location,
        overview = cleanOverview,
        imageUrl = firstimage.nonBlank(),
        imageCopyright = cpyrhtDivCd.nonBlank(),
        themes = CourseTheme.infer(title, cleanOverview),
        useSeason = intro?.useseason?.cleanText()?.takeIf { it.isNotBlank() },
        categoryCodes = CategoryCodes(cat1, cat2, cat3, lclsSystm1, lclsSystm2, lclsSystm3),
        sourceModifiedAt = parseTourTime(modifiedtime),
        collectedAt = collectedAt,
    )
}

// 목록 item 만으로 공통 정보를 채운다. 목록에도 좌표·주소·이미지가 있어 상세 호출을 아낄 수 있다 (개요만 없음).
fun TourListItem.toCommonDetail(): TourCommonDetail = TourCommonDetail(
    contentid = contentid,
    contenttypeid = contenttypeid,
    title = title,
    tel = tel,
    addr1 = addr1,
    addr2 = addr2,
    zipcode = zipcode,
    areacode = areacode,
    sigungucode = sigungucode,
    lDongRegnCd = lDongRegnCd,
    lDongSignguCd = lDongSignguCd,
    cat1 = cat1,
    cat2 = cat2,
    cat3 = cat3,
    lclsSystm1 = lclsSystm1,
    lclsSystm2 = lclsSystm2,
    lclsSystm3 = lclsSystm3,
    mapx = mapx,
    mapy = mapy,
    mlevel = mlevel,
    firstimage = firstimage,
    firstimage2 = firstimage2,
    cpyrhtDivCd = cpyrhtDivCd,
    createdtime = createdtime,
    modifiedtime = modifiedtime,
)

// 관광지 목록 item 으로 바로 풍경 지점을 만든다 (개요 없음). 지점마다 상세를 부르지 않으려고 쓴다.
fun TourListItem.toScenicSpot(collectedAt: Long): ScenicSpot? = toCommonDetail().toScenicSpot(null, collectedAt)

/** 변경분 목록에서 내려간(비노출) 콘텐츠인지. 로컬 카탈로그에서 지워야 한다. */
val TourListItem.isHidden: Boolean get() = showflag?.trim() == "0"

/** mapx(경도)/mapy(위도) 문자열을 좌표로. 비었거나 0 이면 null. */
fun tourLocation(mapx: String?, mapy: String?): LocationPoint? {
    val lng = mapx?.trim()?.toDoubleOrNull() ?: return null
    val lat = mapy?.trim()?.toDoubleOrNull() ?: return null
    if (lat == 0.0 || lng == 0.0) return null
    return LocationPoint(lat = lat, lng = lng)
}

/** "yyyyMMddHHmmss" (한국 시간) → epoch millis */
fun parseTourTime(value: String?): Long? {
    val text = value?.trim()?.takeIf { it.length == 14 } ?: return null
    return try {
        LocalDateTime.parse(text, TourTimeFormat).atZone(KoreaZone).toInstant().toEpochMilli()
    } catch (_: DateTimeParseException) {
        null
    }
}

/** "총 거리 : 약 23.5km", "120 km" 같은 문구에서 km 를 읽는다. */
fun parseDistanceKm(text: String?): Double? {
    if (text.isNullOrBlank()) return null
    Regex("""(\d+(?:\.\d+)?)\s*(km|킬로)""", RegexOption.IGNORE_CASE).find(text)?.let {
        return it.groupValues[1].toDouble()
    }
    Regex("""(\d+(?:\.\d+)?)\s*m\b""").find(text)?.let {
        return it.groupValues[1].toDouble() / 1_000
    }
    return null
}

/** "4시간 30분", "약 2시간", "90분" 같은 문구에서 분을 읽는다. "1일", "1박2일" 은 null. */
fun parseTakeTimeMinutes(text: String?): Int? {
    if (text.isNullOrBlank()) return null
    val hours = Regex("""(\d+(?:\.\d+)?)\s*시간""").find(text)?.groupValues?.get(1)?.toDouble()
    val minutes = Regex("""(\d+)\s*분""").find(text)?.groupValues?.get(1)?.toInt()
    if (hours == null && minutes == null) return null
    return ((hours ?: 0.0) * 60).toInt() + (minutes ?: 0)
}

/** "강원특별자치도 강릉시 창해로 307" → "강원 강릉시" */
internal fun shortArea(addr: String?): String? {
    val parts = addr?.trim()?.split(Regex("\\s+"))?.filter { it.isNotBlank() } ?: return null
    if (parts.isEmpty()) return null
    val province = parts[0]
        .removeSuffix("특별자치도").removeSuffix("특별자치시").removeSuffix("특별시")
        .removeSuffix("광역시").removeSuffix("도")
        .let { ProvinceShortNames[it] ?: it }
    return listOfNotNull(province, parts.getOrNull(1)).joinToString(" ")
}

private val ProvinceShortNames = mapOf(
    "충청북" to "충북", "충청남" to "충남", "전라북" to "전북",
    "전라남" to "전남", "경상북" to "경북", "경상남" to "경남",
)

/** TourAPI 개요에 섞여 오는 <br>, &nbsp; 등을 지운다. */
internal fun String.cleanText(): String =
    replace(Regex("(?i)<br\\s*/?>"), "\n")
        .replace(Regex("<[^>]+>"), "")
        .replace("&nbsp;", " ")
        .replace("&amp;", "&")
        .replace("&lt;", "<")
        .replace("&gt;", ">")
        .replace("&quot;", "\"")
        .replace(Regex("[ \\t]+"), " ")
        .replace(Regex("\\n\\s*\\n+"), "\n")
        .trim()

private fun String?.nonBlank(): String? = this?.trim()?.takeIf { it.isNotEmpty() }
