package com.example.acarive.data.favorite

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class FavoriteRepositoryTest {

    @Test
    fun toggleAddsThenRemovesAndPersists(): Unit = runBlocking {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val repo = FavoriteRepository(context)
        val before = repo.routeIds.first()
        val id = "test:${System.nanoTime()}"

        repo.toggle(id)
        assertEquals(before + id, repo.routeIds.first())
        // 파일에 저장됐는지: 새 저장소 객체로 읽어도 같은 값
        assertEquals(before + id, FavoriteRepository(context).routeIds.first())

        repo.toggle(id)
        assertEquals(before, repo.routeIds.first())
    }
}
