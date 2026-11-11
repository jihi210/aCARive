package com.acarive.scenic.domain

enum class FactorKind { PREFERENCE, WEATHER, TEMP, TIME, SPEED }

data class Weights(
    val preference: Double = 0.35,
    val weather: Double = 0.25,
    val temp: Double = 0.12,
    val time: Double = 0.10,
    val speed: Double = 0.18,
)

data class ScoreConfig(
    val weights: Weights = Weights(),
    val highwayEnergyCap: Int = 4,
    val offGenreScore: Double = 0.2,
    val speedPenaltyPerStep: Double = 0.35,
)

data class FactorScore(val kind: FactorKind, val score: Double, val weight: Double)

data class ScoredTrack(val track: Track, val total: Double, val factors: List<FactorScore>)

class Scorer(val config: ScoreConfig = ScoreConfig()) {

    fun passesSafety(track: Track, ctx: DrivingContext): Boolean =
        !(ctx.speedBand == SpeedBand.HIGHWAY && track.energy > config.highwayEnergyCap)

    fun preferenceScore(track: Track, pref: UserPreference, tagBias: Map<MoodTag, Double>): Double {
        val genre = if (pref.genres.isEmpty() || track.genre in pref.genres) 1.0 else config.offGenreScore
        val bias = if (track.tags.isEmpty()) 0.0 else track.tags.map { tagBias[it] ?: 0.0 }.average()
        return (genre + bias).coerceIn(0.0, 1.0)
    }

    private fun tagScore(tags: List<MoodTag>, weights: Map<MoodTag, Double>): Double {
        val sum = tags.sumOf { weights[it] ?: 0.0 }.coerceIn(-1.0, 1.0)
        return (sum + 1.0) / 2.0
    }

    private fun speedScore(energy: Int, band: SpeedBand): Double {
        val r = Mappings.energyTarget.getValue(band)
        val dist = when {
            energy < r.min -> r.min - energy
            energy > r.max -> energy - r.max
            else -> 0
        }
        return (1.0 - dist * config.speedPenaltyPerStep).coerceAtLeast(0.0)
    }

    fun score(
        track: Track,
        ctx: DrivingContext,
        pref: UserPreference,
        tagBias: Map<MoodTag, Double> = emptyMap(),
    ): ScoredTrack {
        val w = config.weights
        val parts = buildList<Triple<FactorKind, Double, Double>> {
            add(Triple(FactorKind.PREFERENCE, preferenceScore(track, pref, tagBias), w.preference))
            ctx.weather?.let { add(Triple(FactorKind.WEATHER, tagScore(track.tags, Mappings.weather.getValue(it)), w.weather)) }
            ctx.tempBand?.let { add(Triple(FactorKind.TEMP, tagScore(track.tags, Mappings.temp.getValue(it)), w.temp)) }
            ctx.timeSlot?.let { add(Triple(FactorKind.TIME, tagScore(track.tags, Mappings.time.getValue(it)), w.time)) }
            ctx.speedBand?.let { add(Triple(FactorKind.SPEED, speedScore(track.energy, it), w.speed)) }
        }
        val totalWeight = parts.sumOf { it.third }
        val factors = parts.map { FactorScore(it.first, it.second, it.third / totalWeight) }
        return ScoredTrack(track, factors.sumOf { it.score * it.weight }, factors)
    }
}
