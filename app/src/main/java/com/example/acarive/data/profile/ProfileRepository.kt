package com.example.acarive.data.profile

import android.content.Context
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.acarive.data.model.UserProfile
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import java.io.IOException

private val Context.profileStore by preferencesDataStore(name = "profile")

private val PREFERENCES = stringPreferencesKey("preferences")
private val SPOTIFY_CONNECTED = booleanPreferencesKey("spotify_connected")
private val RECOMMEND_WHILE_DRIVING = booleanPreferencesKey("recommend_while_driving")

// 취향 순서를 지키려고 Set 대신 줄바꿈으로 이어 붙여 저장한다
private const val SEPARATOR = "\n"

// 로그인이 없어서 취향·설정은 서버 대신 기기(DataStore)에 저장한다
class ProfileRepository(context: Context) {

    private val store = context.applicationContext.profileStore

    // 저장된 값이 없으면 UserProfile 기본값
    val profile: Flow<UserProfile> = store.data
        .catch { e -> if (e is IOException) emit(emptyPreferences()) else throw e }
        .map { it.toProfile() }
        .distinctUntilChanged()

    suspend fun setPreferences(preferences: List<String>) {
        store.edit { it[PREFERENCES] = preferences.joinToString(SEPARATOR) }
    }

    suspend fun setSpotifyConnected(connected: Boolean) {
        store.edit { it[SPOTIFY_CONNECTED] = connected }
    }

    suspend fun setRecommendWhileDriving(enabled: Boolean) {
        store.edit { it[RECOMMEND_WHILE_DRIVING] = enabled }
    }
}

private fun Preferences.toProfile(): UserProfile {
    val default = UserProfile()
    return UserProfile(
        preferences = this[PREFERENCES]?.split(SEPARATOR)?.filter { it.isNotBlank() } ?: default.preferences,
        spotifyConnected = this[SPOTIFY_CONNECTED] ?: default.spotifyConnected,
        recommendWhileDriving = this[RECOMMEND_WHILE_DRIVING] ?: default.recommendWhileDriving,
    )
}
