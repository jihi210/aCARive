package com.acarive.scenic.domain

import kotlin.math.exp
import kotlin.random.Random

data class Recommendation(val scored: ScoredTrack, val reason: String)

class TagFeedback(private val maxBias: Double = 0.3) {
    private val bias = mutableMapOf<MoodTag, Double>()
    fun apply(tags: List<MoodTag>, delta: Double) {
        if (delta == 0.0) return
        tags.forEach { bias[it] = ((bias[it] ?: 0.0) + delta).coerceIn(-maxBias, maxBias) }
    }
    fun snapshot(): Map<MoodTag, Double> = bias.toMap()
}

class Recommender(
    private val tracks: List<Track>,
    val scorer: Scorer = Scorer(),
    private val random: Random = Random.Default,
    private val poolSize: Int = 100,
    private val topK: Int = 5,
    private val temperature: Double = 0.08, // 낮을수록 1등 위주, 높을수록 다양하게
) {
    private val byUri = tracks.associateBy { it.uri }
    fun findByUri(uri: String): Track? = byUri[uri]

    fun buildPool(pref: UserPreference, tagBias: Map<MoodTag, Double> = emptyMap()): List<Track> =
        tracks.shuffled(random).sortedByDescending { scorer.preferenceScore(it, pref, tagBias) }.take(poolSize)

    fun rank(
        pool: List<Track>,
        ctx: DrivingContext,
        pref: UserPreference,
        played: Set<String>,
        tagBias: Map<MoodTag, Double> = emptyMap(),
    ): List<ScoredTrack> {
        val safe = pool.filter { it.id !in played && scorer.passesSafety(it, ctx) }
        val base = safe.ifEmpty { tracks.filter { it.id !in played && scorer.passesSafety(it, ctx) } }
        return base.map { scorer.score(it, ctx, pref, tagBias) }.sortedByDescending { it.total }
    }

    fun recommend(
        pool: List<Track>,
        ctx: DrivingContext,
        pref: UserPreference,
        played: Set<String>,
        tagBias: Map<MoodTag, Double> = emptyMap(),
    ): Recommendation? {
        val top = rank(pool, ctx, pref, played, tagBias).take(topK)
        if (top.isEmpty()) return null
        val max = top.first().total
        val weights = top.map { exp((it.total - max) / temperature) }
        var r = random.nextDouble() * weights.sum()
        var chosen = top.last()
        for (i in top.indices) {
            r -= weights[i]
            if (r <= 0) { chosen = top[i]; break }
        }
        return Recommendation(chosen, reasonFor(chosen, ctx))
    }

    fun reasonFor(s: ScoredTrack, ctx: DrivingContext): String {
        val top = s.factors.maxByOrNull { (it.score - 0.5) * it.weight } ?: return "추천 곡이에요"
        return when (top.kind) {
            FactorKind.PREFERENCE -> "선호하신 분위기의 곡이에요"
            FactorKind.WEATHER -> "${ctx.weather?.label} 날씨에 어울리는 곡이에요"
            FactorKind.TEMP -> "지금 기온에 어울리는 곡이에요"
            FactorKind.TIME -> "${ctx.timeSlot?.label} 시간대에 어울리는 곡이에요"
            FactorKind.SPEED -> "지금 주행 속도에 맞는 곡이에요"
        }
    }
}
