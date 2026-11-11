package com.example.acarive.data.course

import com.example.acarive.location.LocationPoint
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class CourseRouteMapperTest {

    // 실제 assets 파일이 앱 설정으로 읽히고 추천·변환까지 되는지 (수집기는 null 필드를 빼고 저장)
    @Test
    fun assetCatalogDecodesAndMaps() {
        val file = File("src/main/assets/${CourseCatalogRepository.ASSET_NAME}")
        val catalog = CourseCatalogRepository.decode(file.readText())
        assertTrue(catalog.courses.isNotEmpty())

        val seoul = LocationPoint(37.5665, 126.9780)
        val recs = CourseRecommender.recommend(catalog.courses, seoul, setOf(CourseTheme.Ocean))
        val routes = recs.map { it.toRoute() }
        assertEquals(catalog.courses.size, routes.size)
        assertEquals(routes.size, routes.map { it.id }.toSet().size)
        // 홈에 보여줄 앞쪽 5개는 왕복 2~3시간 안
        assertTrue(recs.take(5).all { it.inWindow })
        routes.forEach { route ->
            assertTrue(route.name.isNotBlank())
            route.imageUrl?.let { assertTrue(it, it.startsWith("https://")) }
            assertTrue(!route.description.contains("\\n"))
        }
    }

    @Test
    fun formatMinutes() {
        assertEquals("40분", formatMinutes(40))
        assertEquals("6시간", formatMinutes(360))
        assertEquals("6시간 30분", formatMinutes(390))
    }

    @Test
    fun roundTo10() {
        assertEquals(150, roundTo10(148))
        assertEquals(140, roundTo10(144))
        assertEquals(10, roundTo10(2))
    }

    @Test
    fun httpImageBecomesHttps() {
        assertEquals("https://tong.visitkorea.or.kr/a.jpg", "http://tong.visitkorea.or.kr/a.jpg".toHttps())
        assertEquals("https://x/a.jpg", "https://x/a.jpg".toHttps())
    }
}
