package com.example.acarive.data.tour

import com.example.acarive.data.course.CourseTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class TourApiParsingTest {

    private inline fun <reified T> parse(json: String): TourApiResponse<T> = TourJson.decodeFromString(json)

    @Test
    fun `여러 건 목록과 문자열 숫자를 읽는다`() {
        val res = parse<TourListItem>(
            """
            {"response":{"header":{"resultCode":"0000","resultMsg":"OK"},
              "body":{"items":{"item":[
                {"contentid":"1","contenttypeid":"25","title":"동해안 드라이브","mapx":"128.9183","mapy":"37.7952","newfield":"x"},
                {"contentid":"2","contenttypeid":25,"title":"북한강"}
              ]},"numOfRows":"2","pageNo":1,"totalCount":"5"}}}
            """,
        )
        assertTrue(res.response.header.isSuccess)
        val body = res.response.body!!
        assertEquals(2, body.items.size)
        assertEquals("25", body.items[1].contenttypeid)
        assertEquals(5, body.totalCount)
        assertTrue(body.hasNextPage)
        assertEquals(37.7952, tourLocation(body.items[0].mapx, body.items[0].mapy)!!.lat, 1e-9)
    }

    @Test
    fun `item 이 객체 하나여도 목록으로 읽는다`() {
        val res = parse<TourListItem>(
            """{"response":{"header":{"resultCode":"0000"},"body":{"items":{"item":{"contentid":"9"}},"numOfRows":10,"pageNo":1,"totalCount":1}}}""",
        )
        assertEquals(listOf("9"), res.response.body!!.items.map { it.contentid })
        assertFalse(res.response.body!!.hasNextPage)
    }

    @Test
    fun `결과가 없으면 빈 목록`() {
        val res = parse<TourListItem>(
            """{"response":{"header":{"resultCode":"0000","resultMsg":"OK"},"body":{"items":"","numOfRows":10,"pageNo":1,"totalCount":0}}}""",
        )
        assertTrue(res.response.body!!.items.isEmpty())
    }

    @Test
    fun `여행코스 상세로 DriveCourse 를 만든다`() {
        val common = TourCommonDetail(
            contentid = "100",
            title = "해안 절벽 파노라마",
            overview = "바다를 따라 달리는 길<br>노을이 아름다운&nbsp;코스",
            addr1 = "강원특별자치도 강릉시 창해로 307",
            lDongRegnCd = "51",
            firstimage = "",
            modifiedtime = "20250512103000",
        )
        val intro = TourCourseIntro(contentid = "100", distance = "총 거리 : 약 112km", taketime = "2시간 50분")
        val stops = listOf(
            TourCourseStopItem(contentid = "100", subnum = "1", subcontentid = "b", subname = "정동진"),
            TourCourseStopItem(contentid = "100", subnum = "0", subcontentid = "a", subname = "경포 전망대"),
        )
        val details = mapOf(
            "a" to TourCommonDetail(contentid = "a", mapx = "128.9", mapy = "37.8"),
            "b" to TourCommonDetail(contentid = "b", mapx = "0", mapy = "0"),
        )

        val course = toDriveCourse(common, intro, stops, details, collectedAt = 1L)

        assertEquals("tour:100", course.id)
        assertEquals("강원 강릉시", course.area)
        assertEquals("바다를 따라 달리는 길\n노을이 아름다운 코스", course.overview)
        assertNull(course.imageUrl)
        assertEquals(listOf("경포 전망대", "정동진"), course.stops.map { it.name })
        assertEquals(37.8, course.start!!.lat, 1e-9)
        assertEquals(1, course.waypoints.size)
        assertEquals(112.0, course.distanceKm!!, 1e-9)
        assertEquals(170, course.takeTimeMinutes)
        assertTrue(CourseTheme.Ocean in course.themes)
        assertTrue(CourseTheme.Sunset in course.themes)
        assertTrue(CourseTheme.Viewpoint in course.themes)
        assertTrue(course.sourceModifiedAt != null)
    }

    @Test
    fun `소요시간 문구 해석`() {
        assertEquals(120, parseTakeTimeMinutes("약 2시간"))
        assertEquals(90, parseTakeTimeMinutes("90분"))
        assertNull(parseTakeTimeMinutes("1박2일"))
        assertEquals(0.5, parseDistanceKm("500m")!!, 1e-9)
        assertNull(parseDistanceKm("미정"))
    }

    @Test
    fun `비노출 표시`() {
        assertTrue(TourListItem(contentid = "1", showflag = "0").isHidden)
        assertFalse(TourListItem(contentid = "1", showflag = "1").isHidden)
    }
}
