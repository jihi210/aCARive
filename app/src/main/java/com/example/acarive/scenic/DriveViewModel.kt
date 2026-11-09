package com.acarive.scenic

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.acarive.scenic.context.AggregatorConfig
import com.acarive.scenic.context.ContextAggregator
import com.acarive.scenic.context.MockSpeedSource
import com.acarive.scenic.context.MockWeatherSource
import com.acarive.scenic.context.WeatherInfo
import com.acarive.scenic.data.TrackRepository
import com.acarive.scenic.domain.GenreGroup
import com.acarive.scenic.domain.Recommender
import com.acarive.scenic.domain.UserPreference
import com.acarive.scenic.domain.Weather
import com.acarive.scenic.player.MockPlayer
import com.acarive.scenic.session.DriveSession
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.launch

class DriveViewModel(app: Application) : AndroidViewModel(app) {
    val speed = MockSpeedSource(0.0)
    val weather = MockWeatherSource(WeatherInfo(Weather.RAIN, 8.0))

    private val aggregator = ContextAggregator(speed, weather, AggregatorConfig.demo())
    private val player = MockPlayer(viewModelScope, trackDurationMs = 20_000)
    private val tracks = TrackRepository(app).load()
    private val session = DriveSession(
        recommender = Recommender(tracks),
        aggregator = aggregator,
        player = player,
        preference = UserPreference(setOf(GenreGroup.CHILL, GenreGroup.POP)),
        scope = viewModelScope,
    )

    fun startDrive() {
        Log.d("Drive", "loaded tracks = ${tracks.size}")
        aggregator.start(viewModelScope)
        session.start()

        viewModelScope.launch {
            session.current.filterNotNull().collect {
                Log.d("Drive", "▶ ${it.scored.track.title} | ${it.reason}")
            }
        }
        // 시연: 15초 뒤 고속 주행으로 바뀌는 상황
        viewModelScope.launch {
            speed.kmh = 25.0
            delay(15_000)
            speed.kmh = 100.0
            Log.d("Drive", "속도를 100km/h로 변경")
        }
    }
}