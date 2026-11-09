package com.acarive.scenic.session

import com.acarive.scenic.context.ContextAggregator
import com.acarive.scenic.domain.GenreGroup
import com.acarive.scenic.domain.Recommendation
import com.acarive.scenic.domain.Recommender
import com.acarive.scenic.domain.Track
import com.acarive.scenic.domain.TagFeedback
import com.acarive.scenic.domain.UserPreference
import com.acarive.scenic.player.MusicPlayer
import com.acarive.scenic.player.NowPlaying
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.distinctUntilChangedBy
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull

data class SessionConfig(
    val skipThresholdMs: Long = 10_000, // 이 시간 안에 넘어가면 강한 스킵으로 본다
    val feedbackStep: Double = 0.05,
)

class DriveSession(
    private val recommender: Recommender,
    private val aggregator: ContextAggregator,
    private val player: MusicPlayer,
    private val preference: UserPreference,
    private val scope: CoroutineScope,
    private val config: SessionConfig = SessionConfig(),
    private val clock: () -> Long = System::currentTimeMillis,
) {
    private data class Started(val track: Track, val startedAt: Long, val durationMs: Long)

    private val feedback = TagFeedback()
    private val played = LinkedHashSet<String>()      // 이번 드라이브에서 이미 고른 곡 (같은 곡 최대 1번)
    private val recs = HashMap<String, Recommendation>()
    private var pool: List<Track>? = null
    private var playing: Started? = null

    private val _current = MutableStateFlow<Recommendation?>(null)
    /** 지금 재생 중인 곡의 추천 정보(추천 이유 표시용). */
    val current: StateFlow<Recommendation?> = _current.asStateFlow()

    fun start() {
        scope.launch {
            // 첫 환경 값이 들어올 때까지 최대 5초 기다렸다가 첫 곡을 고른다.
            withTimeoutOrNull(5_000) { aggregator.context.first { it.speedBand != null || it.weather != null } }
            pick()?.let { player.play(it.scored.track.uri) }
        }
        scope.launch {
            player.nowPlaying.filterNotNull().distinctUntilChangedBy { it.uri }.collect { onTrackChanged(it) }
        }
        scope.launch {
            aggregator.context.map { it.speedBand }.distinctUntilChanged().collect { enforceSafety() }
        }
    }

    private fun pick(): Recommendation? {
        val ctx = aggregator.context.value
        val bias = feedback.snapshot()
        val p = pool ?: recommender.buildPool(preference, bias).also { pool = it }
        val rec = recommender.recommend(p, ctx, preference, played, bias)
            ?: run { played.clear(); recommender.recommend(p, ctx, preference, played, bias) } // 후보 소진 시 초기화
        return rec?.also { played += it.scored.track.id; recs[it.scored.track.uri] = it }
    }

    private fun onTrackChanged(np: NowPlaying) {
        val now = clock()
        playing?.let { applyFeedback(it, now) }
        val track = recommender.findByUri(np.uri)
        playing = track?.let { Started(it, now, np.durationMs) }
        _current.value = recs[np.uri]
        if (track != null) played += track.id

        if (track != null && !recommender.scorer.passesSafety(track, aggregator.context.value)) {
            replaceCurrent(); return
        }
        pick()?.let { player.enqueue(it.scored.track.uri) } // 다음 곡 예약
    }

    private fun enforceSafety() {
        val cur = playing?.track ?: return
        if (!recommender.scorer.passesSafety(cur, aggregator.context.value)) replaceCurrent()
    }

    private fun replaceCurrent() {
        playing = null // 시스템이 바꾼 것이므로 사용자 피드백으로 세지 않는다
        pick()?.let { player.play(it.scored.track.uri) }
    }

    private fun applyFeedback(s: Started, now: Long) {
        val listened = now - s.startedAt
        val ratio = if (s.durationMs > 0) listened.toDouble() / s.durationMs else 1.0
        val step = config.feedbackStep
        val delta = when {
            listened < config.skipThresholdMs -> -2 * step
            ratio < 0.5 -> -step
            ratio >= 0.8 -> step
            else -> 0.0
        }
        feedback.apply(s.track.tags, delta)
    }
}
