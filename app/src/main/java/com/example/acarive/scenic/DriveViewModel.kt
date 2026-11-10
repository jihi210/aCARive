package com.acarive.scenic

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.acarive.scenic.context.AggregatorConfig
import com.acarive.scenic.context.ContextAggregator
import com.acarive.scenic.context.KmaWeatherSource
import com.acarive.scenic.context.MockSpeedSource
import com.acarive.scenic.context.MockWeatherSource
import com.acarive.scenic.context.WeatherInfo
import com.acarive.scenic.context.WeatherSource
import com.acarive.scenic.data.TrackRepository
import com.acarive.scenic.domain.GenreGroup
import com.acarive.scenic.domain.Recommender
import com.acarive.scenic.domain.UserPreference
import com.acarive.scenic.domain.Weather
import com.acarive.scenic.player.MockPlayer
import com.acarive.scenic.player.MusicPlayer
import com.acarive.scenic.player.SpotifyPlayer
import com.acarive.scenic.session.DriveSession
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.launch

class DriveViewModel(app: Application) : AndroidViewModel(app) {

    // 스위치: false로 두면 Mock으로 동작 (키가 없거나 기기에 Spotify가 없을 때)
    private val useSpotify = true
    private val useKma = true

    // 속도와 날씨 Mock (디버그용)
    val speed = MockSpeedSource(0.0)
    val mockWeather = MockWeatherSource(WeatherInfo(Weather.RAIN, 8.0))

    private val weatherSource: WeatherSource =
        if (useKma) KmaWeatherSource(
            serviceKey = BuildConfig.KMA_SERVICE_KEY,
            locationProvider = { 37.5665 to 126.9780 },   // 서울시청 고정 좌표
        ) else mockWeather

    private val aggregator = ContextAggregator(speed, weatherSource, AggregatorConfig.demo())

    private val player: MusicPlayer =
        if (useSpotify) SpotifyPlayer(app, BuildConfig.SPOTIFY_CLIENT_ID, "acarive://spotify-auth")
        else MockPlayer(viewModelScope, trackDurationMs = 20_000)

    private val tracks = TrackRepository(app).load()

    private val session = DriveSession(
        recommender = Recommender(tracks),
        aggregator = aggregator,
        player = player,
        preference = UserPreference(setOf(GenreGroup.CHILL, GenreGroup.POP)),
        scope = viewModelScope,
    )

    fun startDrive() {
        viewModelScope.launch {
            (player as? SpotifyPlayer)?.let { sp ->
                val result = sp.connect()
                if (result.isFailure) {
                    Log.e("Drive", "Spotify 연결 실패", result.exceptionOrNull())
                    return@launch
                }
                Log.d("Drive", "Spotify 연결 성공")
            }

            Log.d("Drive", "loaded tracks = ${tracks.size}")
            aggregator.start(viewModelScope)
            session.start()

            launch {
                session.current.filterNotNull().collect {
                    Log.d(
                        "Drive",
                        "▶ ${it.scored.track.title} | energy=${it.scored.track.energy} " +
                                "tags=${it.scored.track.tags.map { t -> t.label }} | ${it.reason}",
                    )
                }
            }
            // 시연: 15초 뒤 고속 주행으로 바뀌는 상황
            launch {
                speed.kmh = 25.0
                delay(15_000)
                speed.kmh = 100.0
                Log.d("Drive", "속도를 100km/h로 변경")
            }
        }
    }
}
