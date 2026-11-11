package com.example.acarive.data.tour

import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonDecoder
import kotlinx.serialization.json.JsonEncoder
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject

//한국관광공사 TourAPI 4.0 국문 관광정보 서비스(KorService2) 응답 공통 구조, 페이지 정보 담기
// 추천에는 [com.example.acarive.data.course] 의 도메인 모델로 갈아끼워서 사용
@Serializable
data class TourApiResponse<T>(
    val response: TourResponse<T>,
)

@Serializable
data class TourResponse<T>(
    val header: TourResponseHeader,
    val body: TourResponseBody<T>? = null,
)

@Serializable
data class TourResponseHeader(
    val resultCode: String,
    val resultMsg: String = "",
) {
    val isSuccess: Boolean get() = resultCode == SUCCESS_CODE

    companion object {
        const val SUCCESS_CODE = "0000"
    }
}

@Serializable
data class TourResponseBody<T>(
    /** 결과가 없으면 빈 목록. `items` 가 빈 문자열이거나 `item` 이 객체 하나로 와도 목록으로 맞춘다. */
    @Serializable(with = TourItemsSerializer::class)
    val items: List<T> = emptyList(),
    val numOfRows: Int = 0,
    val pageNo: Int = 0,
    val totalCount: Int = 0,
) {
    /** 다음 페이지가 남았는지. 한 달에 한 번 전체 수집할 때 페이지를 끝까지 돈다. */
    val hasNextPage: Boolean get() = numOfRows > 0 && pageNo * numOfRows < totalCount
}

/**
 * `items` 필드를 목록으로 읽는다. TourAPI 는 결과 개수에 따라 모양이 달라진다.
 * - 여러 개: `{"item": [ {...}, {...} ]}`
 * - 한 개: `{"item": {...}}` 로 오는 경우가 있다
 * - 없음: `""` (빈 문자열) 또는 `{}`
 */
class TourItemsSerializer<T>(private val itemSerializer: KSerializer<T>) : KSerializer<List<T>> {
    private val listSerializer = ListSerializer(itemSerializer)
    override val descriptor: SerialDescriptor = listSerializer.descriptor

    override fun deserialize(decoder: Decoder): List<T> {
        val json = (decoder as JsonDecoder).json
        val items = decoder.decodeJsonElement() as? JsonObject ?: return emptyList()
        return when (val item = items["item"]) {
            is JsonArray -> json.decodeFromJsonElement(listSerializer, item)
            is JsonObject -> listOf(json.decodeFromJsonElement(itemSerializer, item))
            else -> emptyList()
        }
    }

    override fun serialize(encoder: Encoder, value: List<T>) {
        val json = (encoder as JsonEncoder).json
        encoder.encodeJsonElement(
            buildJsonObject { put("item", json.encodeToJsonElement(listSerializer, value)) },
        )
    }
}

/**
 * TourAPI 응답용 Json 설정. 숫자도 문자열로 내려오고(`"mapx": "128.91"`) 문서에 없는 필드가
 * 추가되기도 해서 느슨하게 읽는다.
 */
val TourJson: Json = Json {
    ignoreUnknownKeys = true
    isLenient = true
    coerceInputValues = true
    explicitNulls = false
}
