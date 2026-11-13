package com.example.acarive.debug

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.lifecycleScope
import com.acarive.scenic.data.TrackRepository
import com.acarive.scenic.player.SpotifyPlayer
import com.example.acarive.BuildConfig
import kotlinx.coroutines.launch

class SpotifyDebugActivity : ComponentActivity() {
    private lateinit var player: SpotifyPlayer
    private val logs = mutableStateListOf<String>()

    private fun log(msg: String) {
        Log.d("SpotifyDebug", msg)
        logs.add(0, msg)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        player = SpotifyPlayer(this, BuildConfig.SPOTIFY_CLIENT_ID, "acarive://spotify-auth")
        val tracks = runCatching { TrackRepository(this).load() }.getOrDefault(emptyList())
        log("CLIENT_ID 설정됨=${BuildConfig.SPOTIFY_CLIENT_ID.isNotBlank()}, tracks.csv 곡 ${tracks.size}개")

        setContent {
            MaterialTheme {
                val connected by player.connected.collectAsState()
                val now by player.nowPlaying.collectAsState()
                Column(
                    Modifier.padding(16.dp).verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Text("연결: ${if (connected) "O" else "X"}")
                    Text(
                        "재생: " + (now?.let {
                            "${it.title} - ${it.artist}" + if (it.isPaused) " (일시정지)" else ""
                        } ?: "없음")
                    )
                    Button(onClick = {
                        lifecycleScope.launch {
                            log("연결 시도…")
                            player.connect(true)
                                .onSuccess { log("연결 성공") }
                                .onFailure { log("연결 실패: ${it::class.java.simpleName} - ${it.message}") }
                        }
                    }) { Text("1. Spotify 연결") }
                    Button(onClick = {
                        log("고정 곡 재생 요청")
                        player.play("spotify:track:5PvLNQRNRj61y3CU4SFIY4") // tracks.csv 1번 곡
                    }) { Text("2. 고정 곡 재생") }
                    Button(onClick = {
                        tracks.randomOrNull()
                            ?.let { log("랜덤 곡 요청: ${it.title}"); player.play(it.uri) }
                            ?: log("tracks.csv에서 읽은 곡이 0개")
                    }) { Text("3. tracks.csv 랜덤 곡 재생") }
                    Button(onClick = {
                        tracks.randomOrNull()
                            ?.let { log("큐 추가: ${it.title}"); player.enqueue(it.uri) }
                    }) { Text("4. 다음 곡 큐에 추가") }
                    Button(onClick = { player.pause() }) { Text("일시정지") }
                    Button(onClick = { player.resume() }) { Text("재개") }
                    Button(onClick = { player.skipNext() }) { Text("다음 곡") }
                    Button(onClick = { player.release(); log("연결 해제") }) { Text("연결 해제") }
                    logs.forEach { Text(it, style = MaterialTheme.typography.bodySmall) }
                }
            }
        }
    }

    override fun onDestroy() {
        player.release()
        super.onDestroy()
    }
}