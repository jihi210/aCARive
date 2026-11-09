package com.acarive.scenic.domain

import com.acarive.scenic.domain.MoodTag.*

data class EnergyRange(val min: Int, val max: Int)

object Mappings {
    val weather: Map<Weather, Map<MoodTag, Double>> = mapOf(
        Weather.SUNNY to mapOf(LIVELY to 1.0, GRAND to 0.5, SLEEK to 0.5, LONELY to -0.5),
        Weather.CLOUDY to mapOf(SENTIMENTAL to 1.0, EASY to 1.0, DREAMY to 0.5, LONELY to 0.5, LIVELY to -0.5),
        Weather.RAIN to mapOf(SENTIMENTAL to 1.0, DREAMY to 1.0, LONELY to 0.5, EASY to 0.5, LIVELY to -0.5, GRAND to -0.5),
        Weather.SNOW to mapOf(SENTIMENTAL to 1.0, DREAMY to 1.0, EASY to 0.5, LONELY to 0.5, LIVELY to -0.5),
    )

    val temp: Map<TempBand, Map<MoodTag, Double>> = mapOf(
        TempBand.FREEZING to mapOf(WARM to 1.0, REFRESHING to -1.0),
        TempBand.COLD to mapOf(WARM to 0.5),
        TempBand.MILD to emptyMap(),
        TempBand.WARM_DAY to mapOf(REFRESHING to 0.5),
        TempBand.HOT to mapOf(WARM to -1.0, REFRESHING to 1.0),
    )

    val time: Map<TimeSlot, Map<MoodTag, Double>> = mapOf(
        TimeSlot.MORNING to mapOf(LIVELY to 1.0, EASY to 0.5, DREAMY to -0.5, LONELY to -0.5),
        TimeSlot.DAY to mapOf(LIVELY to 1.0, POWERFUL to 0.5, GRAND to 0.5, EASY to 0.5, DREAMY to -0.5),
        TimeSlot.EVENING to mapOf(SENTIMENTAL to 1.0, SLEEK to 0.5, GRAND to 0.5, POWERFUL to -0.5),
        TimeSlot.NIGHT to mapOf(DREAMY to 1.0, SLEEK to 1.0, SENTIMENTAL to 0.5, LONELY to 0.5, LIVELY to -0.5, POWERFUL to -0.5),
    )

    val energyTarget: Map<SpeedBand, EnergyRange> = mapOf(
        SpeedBand.JAM to EnergyRange(1, 2),
        SpeedBand.CITY to EnergyRange(2, 4),
        SpeedBand.ROAD to EnergyRange(3, 4),
        SpeedBand.HIGHWAY to EnergyRange(3, 4),
    )
}
