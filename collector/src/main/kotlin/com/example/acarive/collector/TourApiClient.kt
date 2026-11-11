package com.example.acarive.collector

import com.example.acarive.data.tour.TourApiResponse
import com.example.acarive.data.tour.TourCommonDetail
import com.example.acarive.data.tour.TourContentType
import com.example.acarive.data.tour.TourCourseIntro
import com.example.acarive.data.tour.TourCourseStopItem
import com.example.acarive.data.tour.TourEndpoint
import com.example.acarive.data.tour.TourJson
import com.example.acarive.data.tour.TourListItem
import com.example.acarive.data.tour.TourResponseBody
import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.jsonObject
import java.io.IOException
import java.net.URI
import java.net.URLEncoder
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.time.Duration

// 수집에 필요한 TourAPI 호출. 테스트에서는 가짜로 바꿔 끼운다.
interface TourSource {
    fun list(params: Map<String, String>, page: Int, pageSize: Int): TourResponseBody<TourListItem>
    fun detailCommon(contentId: String): TourCommonDetail?
    fun courseIntro(contentId: String): TourCourseIntro?
    fun courseStops(contentId: String): List<TourCourseStopItem>
}

// 호출 한도를 다 썼다. 상태를 저장하고 멈춘 뒤 다음 날 이어서 실행한다.
class QuotaExceededException(message: String) : Exception(message)

// 인증키 오류처럼 계속해도 소용없는 오류. 수집을 멈춘다.
class FatalApiException(message: String) : Exception(message)

// 콘텐츠 하나만 실패한 오류. 그 콘텐츠만 건너뛴다.
class TourApiException(message: String) : Exception(message)

class TourApiClient(
    private val config: CollectorConfig,
    private val http: HttpClient = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build(),
) : TourSource {

    var callsMade = 0
        private set

    // Decoding 키는 인코딩하고, 이미 인코딩된 Encoding 키(% 포함)는 그대로 쓴다.
    private val encodedKey =
        if ('%' in config.serviceKey) config.serviceKey else encode(config.serviceKey)

    override fun list(params: Map<String, String>, page: Int, pageSize: Int): TourResponseBody<TourListItem> =
        fetch(
            TourEndpoint.AREA_BASED_LIST,
            params + mapOf("pageNo" to page.toString(), "numOfRows" to pageSize.toString()),
            TourListItem.serializer(),
        )

    override fun detailCommon(contentId: String): TourCommonDetail? =
        fetch(TourEndpoint.DETAIL_COMMON, mapOf("contentId" to contentId), TourCommonDetail.serializer())
            .items.firstOrNull()

    override fun courseIntro(contentId: String): TourCourseIntro? =
        fetch(TourEndpoint.DETAIL_INTRO, courseParams(contentId), TourCourseIntro.serializer()).items.firstOrNull()

    override fun courseStops(contentId: String): List<TourCourseStopItem> =
        fetch(TourEndpoint.DETAIL_INFO, courseParams(contentId), TourCourseStopItem.serializer()).items

    private fun courseParams(contentId: String) =
        mapOf("contentId" to contentId, "contentTypeId" to TourContentType.TravelCourse.id.toString())

    private fun <T> fetch(endpoint: String, params: Map<String, String>, item: KSerializer<T>): TourResponseBody<T> {
        if (callsMade >= config.callLimit) {
            throw QuotaExceededException("이번 실행의 호출 한도(${config.callLimit}번)를 다 썼어요")
        }
        if (callsMade > 0) Thread.sleep(config.requestDelayMs)
        callsMade++

        val query = (
            mapOf(
                "MobileOS" to config.mobileOs,
                "MobileApp" to config.mobileApp,
                "_type" to "json",
            ) + params
            ).entries.joinToString("&") { (k, v) -> "${encode(k)}=${encode(v)}" }
        val uri = URI.create("${TourEndpoint.BASE_URL}$endpoint?serviceKey=$encodedKey&$query")
        val request = HttpRequest.newBuilder(uri).timeout(Duration.ofSeconds(20)).GET().build()

        val response = try {
            http.send(request, HttpResponse.BodyHandlers.ofString(Charsets.UTF_8))
        } catch (e: IOException) {
            throw TourApiException("$endpoint 네트워크 오류: ${e.message}")
        }
        return parse(endpoint, response.statusCode(), response.body(), item)
    }

    companion object {
        // data.go.kr 게이트웨이 오류 코드 (returnReasonCode)
        private const val CODE_QUOTA = "22"
        private val FATAL_CODES = setOf("12", "20", "30", "31", "32", "33")

        internal fun <T> parse(endpoint: String, status: Int, body: String, item: KSerializer<T>): TourResponseBody<T> {
            val text = body.trim()
            when (status) {
                429 -> throw QuotaExceededException("호출 한도 초과 (HTTP 429)")
                401, 403 -> throw FatalApiException("인증 실패 (HTTP $status): 인증키와 활용신청 승인 여부를 확인해 주세요. $text")
            }

            // 게이트웨이 오류는 _type=json 이어도 XML 로 온다.
            if (text.startsWith("<")) {
                val code = Regex("<returnReasonCode>(\\d+)</returnReasonCode>").find(text)?.groupValues?.get(1)
                val msg = Regex("<returnAuthMsg>([^<]*)</returnAuthMsg>").find(text)?.groupValues?.get(1)
                    ?: Regex("<resultMsg>([^<]*)</resultMsg>").find(text)?.groupValues?.get(1)
                    ?: text.take(200)
                throw classify(endpoint, code, msg)
            }
            if (status !in 200..299) throw TourApiException("$endpoint HTTP $status: ${text.take(200)}")

            val root = try {
                TourJson.parseToJsonElement(text).jsonObject
            } catch (_: Exception) {
                throw TourApiException("$endpoint 응답을 읽지 못했어요: ${text.take(200)}")
            }
            // 오류는 최상위에 resultCode 만 오기도 한다.
            if ("response" !in root) {
                throw classify(endpoint, root.string("resultCode"), root.string("resultMsg") ?: text.take(200))
            }
            val parsed = try {
                TourJson.decodeFromJsonElement(TourApiResponse.serializer(item), root)
            } catch (e: SerializationException) {
                throw TourApiException("$endpoint 응답 형식이 달라요: ${e.message}")
            } catch (e: IllegalArgumentException) {
                throw TourApiException("$endpoint 응답 형식이 달라요: ${e.message}")
            }
            val header = parsed.response.header
            if (!header.isSuccess) throw classify(endpoint, header.resultCode, header.resultMsg)
            return parsed.response.body ?: TourResponseBody()
        }

        private fun classify(endpoint: String, code: String?, msg: String): Exception = when (code) {
            CODE_QUOTA -> QuotaExceededException("오늘 호출 한도를 다 썼어요 ($msg)")
            in FATAL_CODES -> FatalApiException("$endpoint 오류 $code: $msg (인증키·활용신청을 확인해 주세요)")
            else -> TourApiException("$endpoint 오류 ${code ?: "?"}: $msg")
        }

        private fun JsonObject.string(key: String): String? =
            (this[key] as? JsonPrimitive)?.content

        private fun encode(value: String): String = URLEncoder.encode(value, Charsets.UTF_8)
    }
}
