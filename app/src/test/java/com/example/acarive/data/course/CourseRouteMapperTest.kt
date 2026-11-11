package com.example.acarive.data.course

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class CourseRouteMapperTest {

    // 실제 assets 파일이 앱 설정으로 읽히는지 (수집기는 null 필드를 빼고 저장)
    @Test
    fun assetCatalogDecodesAndMaps() {
        val file = File("src/main/assets/${CourseCatalogRepository.ASSET_NAME}")
        val catalog = CourseCatalogRepository.decode(file.readText())
        assertTrue(catalog.courses.isNotEmpty())

        val routes = catalog.toRoutes()
        assertEquals(catalog.courses.size, routes.size)
        assertEquals(routes.size, routes.map { it.id }.toSet().size)
        routes.forEach { route ->
            assertTrue(route.name.isNotBlank())
            route.imageUrl?.let { assertTrue(it, it.startsWith("https://")) }
            assertTrue(!route.description.contains("\\n"))
        }
        // 테마 있는 코스가 앞에 온다
        val firstUntagged = routes.indexOfFirst { it.tags.isEmpty() }
        if (firstUntagged >= 0) assertTrue(routes.drop(firstUntagged).all { it.tags.isEmpty() })
    }

    @Test
    fun formatMinutes() {
        assertEquals("40분", formatMinutes(40))
        assertEquals("6시간", formatMinutes(360))
        assertEquals("6시간 30분", formatMinutes(390))
    }

    @Test
    fun httpImageBecomesHttps() {
        assertEquals("https://tong.visitkorea.or.kr/a.jpg", "http://tong.visitkorea.or.kr/a.jpg".toHttps())
        assertEquals("https://x/a.jpg", "https://x/a.jpg".toHttps())
    }
}
