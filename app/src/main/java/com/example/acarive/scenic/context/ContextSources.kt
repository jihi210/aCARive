package com.acarive.scenic.context

import com.acarive.scenic.domain.Weather

data class WeatherInfo(val weather: Weather, val tempC: Double)

interface SpeedSource {
    suspend fun currentSpeedKmh(): Double?
}

interface WeatherSource {
    suspend fun current(): WeatherInfo?
}

class MockSpeedSource(@Volatile var kmh: Double? = 0.0) : SpeedSource {
    override suspend fun currentSpeedKmh(): Double? = kmh
}

class MockWeatherSource(@Volatile var value: WeatherInfo? = WeatherInfo(Weather.SUNNY, 22.0)) : WeatherSource {
    override suspend fun current(): WeatherInfo? = value
}

// TODO: 기상청 단기예보 API(공공데이터포털)로 WeatherSource 구현 -> KmaWeatherSource
