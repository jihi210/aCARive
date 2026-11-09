package com.acarive.scenic.player

import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class NowPlaying(val uri: String, val durationMs: Long, val isPaused: Boolean)

interface MusicPlayer {
    val nowPlaying: StateFlow<NowPlaying?>
    fun play(uri: String)
    fun enqueue(uri: String)
    fun skipNext()
    fun release()
}

class MockPlayer(
    private val scope: CoroutineScope,
    private val trackDurationMs: Long = 30_000,
) : MusicPlayer {
    private val queue = ArrayDeque<String>()
    private var timer: Job? = null
    private val _nowPlaying = MutableStateFlow<NowPlaying?>(null)
    override val nowPlaying: StateFlow<NowPlaying?> = _nowPlaying.asStateFlow()

    override fun play(uri: String) {
        Log.d("MockPlayer", "play $uri")
        timer?.cancel()
        _nowPlaying.value = NowPlaying(uri, trackDurationMs, false)
        timer = scope.launch { delay(trackDurationMs); advance() }
    }

    override fun enqueue(uri: String) {
        Log.d("MockPlayer", "enqueue $uri")
        queue.addLast(uri)
    }

    override fun skipNext() { timer?.cancel(); advance() }

    private fun advance() {
        val next = queue.removeFirstOrNull()
        if (next != null) play(next) else _nowPlaying.value = null
    }

    override fun release() { timer?.cancel() }
}
