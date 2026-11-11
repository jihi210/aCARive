package com.example.acarive.collector

import com.example.acarive.data.tour.TourCommonDetail
import com.example.acarive.data.tour.TourCourseIntro
import com.example.acarive.data.tour.TourCourseStopItem
import com.example.acarive.data.tour.TourListItem
import com.example.acarive.data.tour.TourResponseBody
import com.example.acarive.data.tour.TourCourseStopItem as Stop
import kotlinx.serialization.builtins.serializer
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class CourseCollectorTest {

    // 코스 3개(c1~c3), 관광지 1개(s1). c1·c2 는 경유지 s1 과 p1 을 공유한다.
    private class FakeSource(private val limit: Int = Int.MAX_VALUE) : TourSource {
        var calls = 0
        val failing = mutableSetOf<String>()
        var courseModified = "20250101000000"

        private fun call() {
            if (calls >= limit) throw QuotaExceededException("한도")
            calls++
        }

        override fun list(params: Map<String, String>, page: Int, pageSize: Int): TourResponseBody<TourListItem> {
            call()
            val all = if (params["contentTypeId"] == "25") {
                listOf("c1", "c2", "c3").map { TourListItem(contentid = it, title = it, modifiedtime = courseModified) }
            } else {
                listOf(TourListItem(contentid = "s1", title = "경포 해변", mapx = "128.9", mapy = "37.8"))
            }
            val items = all.drop((page - 1) * pageSize).take(pageSize)
            return TourResponseBody(items, numOfRows = pageSize, pageNo = page, totalCount = all.size)
        }

        override fun detailCommon(contentId: String): TourCommonDetail? {
            call()
            if (contentId in failing) throw TourApiException("실패")
            return TourCommonDetail(
                contentid = contentId,
                title = "코스 $contentId",
                mapx = "127.5",
                mapy = "37.5",
                modifiedtime = courseModified,
            )
        }

        override fun courseIntro(contentId: String): TourCourseIntro? {
            call()
            return TourCourseIntro(contentid = contentId, taketime = "2시간")
        }

        override fun courseStops(contentId: String): List<TourCourseStopItem> {
            call()
            return listOf(
                Stop(contentid = contentId, subnum = "0", subcontentid = "s1", subname = "경포 해변"),
                Stop(contentid = contentId, subnum = "1", subcontentid = "p1", subname = "전망대"),
            )
        }
    }

    private val config = CollectorConfig(serviceKey = "k", pageSize = 2)
    private var time = 1_000L
    private fun collector(source: TourSource) = CourseCollector(source, config, now = { time }, log = {})

    @Test
    fun `목록과 상세를 모두 모으고 경유지는 캐시와 관광지 목록을 재사용한다`() {
        val source = FakeSource()
        val result = collector(source).run(null)

        assertTrue(result.finished)
        assertEquals(setOf("c1", "c2", "c3"), result.state.collected.keys)
        // 목록: 코스 2페이지 + 관광지 1페이지 = 3, 코스마다 3번 = 9, 경유지 p1 은 처음 한 번만 = 1
        assertEquals(13, source.calls)
        val course = result.state.collected.getValue("c1")
        assertEquals(37.8, course.start!!.lat, 1e-9) // s1 좌표는 관광지 목록에서
        assertEquals(120, course.takeTimeMinutes)

        val catalog = collector(source).buildCatalog(result.state)
        assertEquals(3, catalog.courses.size)
        assertEquals(1, catalog.spots.size)
    }

    @Test
    fun `한도에 걸리면 멈추고 다음 실행에서 이어간다`() {
        val first = collector(FakeSource(limit = 7)).run(null)
        assertFalse(first.finished)
        assertNotNull(first.stopReason)
        assertEquals(1, first.state.collected.size)

        val second = FakeSource()
        val result = collector(second).run(first.state)
        assertTrue(result.finished)
        assertEquals(3, result.state.collected.size)
        // 목록은 다시 부르지 않고, 남은 코스 2개 × 3번. p1 은 이미 캐시됨
        assertEquals(6, second.calls)
    }

    @Test
    fun `실패한 코스는 건너뛰고 나머지는 완성한다`() {
        val source = FakeSource().apply { failing += "c2" }
        val result = collector(source).run(null)
        assertTrue(result.finished)
        assertEquals(setOf("c1", "c3"), result.state.collected.keys)
        assertEquals(1, result.state.failures["c2"])
    }

    @Test
    fun `새 주기에는 수정일이 같은 코스를 재사용한다`() {
        val first = collector(FakeSource()).run(null).state
        time += 31L * 24 * 60 * 60 * 1000

        val source = FakeSource()
        val result = collector(source).run(first)
        assertTrue(result.finished)
        assertEquals(3, result.reused)
        assertEquals(3, source.calls) // 목록만 다시 부름

        // 이번 주기 수집이 끝난 뒤 다시 실행하면 아무것도 부르지 않는다
        val again = FakeSource()
        collector(again).run(result.state)
        assertEquals(0, again.calls)
    }

    @Test
    fun `수정된 코스는 다시 받는다`() {
        val first = collector(FakeSource()).run(null).state
        val source = FakeSource().apply { courseModified = "20250601000000" }
        val result = collector(source).run(first, forceNewCycle = true)
        assertEquals(0, result.reused)
        assertEquals(3, result.fetched)
    }

    @Test
    fun `게이트웨이 오류 응답을 구분한다`() {
        val quota = "<OpenAPI_ServiceResponse><cmmMsgHeader><returnAuthMsg>LIMITED_NUMBER_OF_SERVICE_REQUESTS_EXCEEDS_ERROR</returnAuthMsg><returnReasonCode>22</returnReasonCode></cmmMsgHeader></OpenAPI_ServiceResponse>"
        val badKey = quota.replace(">22<", ">30<")
        assertThrows<QuotaExceededException> { TourApiClient.parse("x", 200, quota, String.serializer()) }
        assertThrows<FatalApiException> { TourApiClient.parse("x", 200, badKey, String.serializer()) }
        assertThrows<FatalApiException> { TourApiClient.parse("x", 401, "Unauthorized", String.serializer()) }
        assertThrows<TourApiException> {
            TourApiClient.parse("x", 200, """{"response":{"header":{"resultCode":"10","resultMsg":"INVALID"}}}""", String.serializer())
        }
        val empty = TourApiClient.parse(
            "x", 200,
            """{"response":{"header":{"resultCode":"0000"},"body":{"items":"","numOfRows":10,"pageNo":1,"totalCount":0}}}""",
            TourListItem.serializer(),
        )
        assertTrue(empty.items.isEmpty())
        assertNull(empty.items.firstOrNull())
    }

    private inline fun <reified E : Throwable> assertThrows(block: () -> Unit) {
        try {
            block()
        } catch (e: Throwable) {
            if (e is E) return
            throw AssertionError("${E::class.simpleName} 가 아니라 ${e::class.simpleName}: ${e.message}")
        }
        throw AssertionError("${E::class.simpleName} 가 나지 않았어요")
    }
}
