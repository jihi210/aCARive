package com.example.acarive.data.favorite

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import java.io.IOException

private val Context.favoriteStore by preferencesDataStore(name = "favorites")

private val ROUTE_IDS = stringSetPreferencesKey("route_ids")

// 즐겨찾기한 코스 id ("tour:1846248") 를 기기에 저장한다
class FavoriteRepository(context: Context) {

    private val store = context.applicationContext.favoriteStore

    val routeIds: Flow<Set<String>> = store.data
        .catch { e -> if (e is IOException) emit(emptyPreferences()) else throw e }
        .map { it[ROUTE_IDS].orEmpty() }
        .distinctUntilChanged()

    // 저장된 값을 기준으로 뒤집는다. 빠르게 여러 번 눌러도 화면 값과 어긋나지 않는다
    suspend fun toggle(routeId: String) {
        store.edit {
            val ids = it[ROUTE_IDS].orEmpty()
            it[ROUTE_IDS] = if (routeId in ids) ids - routeId else ids + routeId
        }
    }
}
