package com.example.acarive.data.tour

import kotlinx.serialization.Serializable

//KorService2 엔드포인트별 응답 item. 일반 목록, 위치 기반 목록, 변경분 목록을 하나의 클래스로 가져온다.
// 좌표: mapx = 경도(longitude), mapy = 위도(latitude). WGS84, 시각: createdtime / modifiedtime = "yyyyMMddHHmmss" (한국 시간).


object TourEndpoint {
    const val BASE_URL = "https://apis.data.go.kr/B551011/KorService2/"

    // 지역 기반 목록. 월간 전체 수집용
    const val AREA_BASED_LIST = "areaBasedList2"

    // 좌표 반경(최대 20km) 목록. 코스 주변 풍경 지점 서치용
    const val LOCATION_BASED_LIST = "locationBasedList2"

    // 변경분 목록. 지난 수집 이후 바뀌거나 내려간 콘텐츠만 수집 (showflag 포함)
    const val AREA_BASED_SYNC_LIST = "areaBasedSyncList2"

    const val DETAIL_COMMON = "detailCommon2"
    const val DETAIL_INTRO = "detailIntro2"
    const val DETAIL_INFO = "detailInfo2"
    const val DETAIL_IMAGE = "detailImage2"

    // 법정동 코드 / 신규 분류체계 코드 조회
    const val LDONG_CODE = "ldongCode2"
    const val LCLS_SYSTM_CODE = "lclsSystmCode2"
}

// TourAPI 콘텐츠 타입 (국문).
enum class TourContentType(val id: Int) {
    TouristSpot(12),
    CultureFacility(14),
    Festival(15),
    TravelCourse(25),
    Leports(28),
    Stay(32),
    Shopping(38),
    Restaurant(39),
    ;

    companion object {
        fun from(id: String?): TourContentType? = id?.trim()?.toIntOrNull()?.let { v -> entries.find { it.id == v } }
    }
}

// areaBasedList2 / locationBasedList2 / areaBasedSyncList2 의 목록 item.
@Serializable
data class TourListItem(
    val contentid: String,
    val contenttypeid: String? = null,
    val title: String? = null,
    val addr1: String? = null,
    val addr2: String? = null,
    val zipcode: String? = null,
    val tel: String? = null,
    val areacode: String? = null, // 기존 지역코드
    val sigungucode: String? = null,
    // 법정 시도/시군구 코드
    val lDongRegnCd: String? = null,
    val lDongSignguCd: String? = null,
    // 기존 서비스 분류 (대/중/소)
    val cat1: String? = null,
    val cat2: String? = null,
    val cat3: String? = null,
    // 신규 분류체계 (대/중/소, KorService2 에서 추가)
    val lclsSystm1: String? = null,
    val lclsSystm2: String? = null,
    val lclsSystm3: String? = null,
    val mapx: String? = null,
    val mapy: String? = null,
    val mlevel: String? = null,
    // 대표 이미지 원본 / 썸네일
    val firstimage: String? = null,
    val firstimage2: String? = null,
    // 이미지 저작권 유형 (Type1: 출처표시, Type3: 출처표시+변경금지)
    val cpyrhtDivCd: String? = null,
    val createdtime: String? = null,
    val modifiedtime: String? = null,
    // locationBasedList2 만: 기준 좌표에서의 거리(m)
    val dist: String? = null,
    // areaBasedSyncList2 만: "1" 노출, "0" 비노출(삭제·비공개). 0 이면 로컬 카탈로그에서 삭제
    val showflag: String? = null,
)

// detailCommon2: 콘텐츠 공통 정보 + 개요.
@Serializable
data class TourCommonDetail(
    val contentid: String,
    val contenttypeid: String? = null,
    val title: String? = null,
    val overview: String? = null,
    val homepage: String? = null,
    val tel: String? = null,
    val telname: String? = null,
    val addr1: String? = null,
    val addr2: String? = null,
    val zipcode: String? = null,
    val areacode: String? = null,
    val sigungucode: String? = null,
    val lDongRegnCd: String? = null,
    val lDongSignguCd: String? = null,
    val cat1: String? = null,
    val cat2: String? = null,
    val cat3: String? = null,
    val lclsSystm1: String? = null,
    val lclsSystm2: String? = null,
    val lclsSystm3: String? = null,
    val mapx: String? = null,
    val mapy: String? = null,
    val mlevel: String? = null,
    val firstimage: String? = null,
    val firstimage2: String? = null,
    val cpyrhtDivCd: String? = null,
    val createdtime: String? = null,
    val modifiedtime: String? = null,
)

//  detailIntro2 (contentTypeId=25, 여행코스)
@Serializable
data class TourCourseIntro(
    val contentid: String,
    val contenttypeid: String? = null,
    /** 코스 총 거리. 예: "23.5km", "총 거리 : 약 120km" 처럼 자유 형식 */
    val distance: String? = null,
    /** 코스 총 소요시간. 예: "4시간", "1일" 처럼 자유 형식 */
    val taketime: String? = null,
    /** 일정. 예: "당일" */
    val schedule: String? = null,
    /** 코스 테마 */
    val theme: String? = null,
    val infocentertourcourse: String? = null,
)

/** detailIntro2 (contentTypeId=12, 관광지). 풍경 지점에서 드라이브에 필요한 항목만 받는다. */
@Serializable
data class TourSpotIntro(
    val contentid: String,
    val contenttypeid: String? = null,
    val infocenter: String? = null,
    val restdate: String? = null,
    val usetime: String? = null,
    /** 이용 시기. 예: "봄~가을" */
    val useseason: String? = null,
    val parking: String? = null,
    val chkpet: String? = null,
)

/** detailInfo2 (contentTypeId=25, 여행코스): 코스를 이루는 경유지 하나. */
@Serializable
data class TourCourseStopItem(
    val contentid: String,
    val contenttypeid: String? = null,
    /** 코스 내 순번 (0부터 시작하는 문자열) */
    val subnum: String? = null,
    /** 경유지의 contentid. 좌표는 이 id 로 detailCommon2 를 다시 조회해 얻는다. */
    val subcontentid: String? = null,
    val subname: String? = null,
    val subdetailoverview: String? = null,
    val subdetailimg: String? = null,
    val subdetailalt: String? = null,
)

/** detailImage2: 콘텐츠의 추가 이미지. */
@Serializable
data class TourImageItem(
    val contentid: String,
    val originimgurl: String? = null,
    val smallimageurl: String? = null,
    val imgname: String? = null,
    val serialnum: String? = null,
    val cpyrhtDivCd: String? = null,
)
