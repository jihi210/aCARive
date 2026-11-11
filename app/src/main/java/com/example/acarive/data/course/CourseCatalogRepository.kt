package com.example.acarive.data.course

import android.content.Context
import com.example.acarive.data.tour.TourJson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

// assets 의 코스 카탈로그(collector 가 만든 파일)를 읽는다.
// TODO(backend): 서버가 생기면 카탈로그 API 로 받고, assets 는 첫 실행·오프라인용으로만 쓴다.
class CourseCatalogRepository(private val context: Context) {

    suspend fun load(): CourseCatalog = withContext(Dispatchers.IO) {
        val text = context.assets.open(ASSET_NAME).bufferedReader().use { it.readText() }
        decode(text)
    }

    companion object {
        const val ASSET_NAME = "course_catalog.json"

        // 수집기가 null 필드를 빼고 저장하므로 같은 설정(TourJson)으로 읽는다.
        fun decode(text: String): CourseCatalog = TourJson.decodeFromString(CourseCatalog.serializer(), text)
    }
}
