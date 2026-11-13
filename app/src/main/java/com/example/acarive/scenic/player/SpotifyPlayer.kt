package com.acarive.scenic.player

import android.content.Context
import com.spotify.android.appremote.api.ConnectionParams
import com.spotify.android.appremote.api.Connector
import com.spotify.android.appremote.api.SpotifyAppRemote
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import android.util.Log

/** Spotify App Remote SDK로 Spotify 앱을 원격 제어한다. Spotify 앱 설치·로그인과 Premium이 필요하다. */
class SpotifyPlayer(
    private val context: Context,
    private val clientId: String,
    private val redirectUri: String,
) : MusicPlayer {
    private var remote: SpotifyAppRemote? = null
    @Volatile var activityContext: Context? = null //테스트용
    private val _nowPlaying = MutableStateFlow<NowPlaying?>(null)
    override val nowPlaying: StateFlow<NowPlaying?> = _nowPlaying.asStateFlow()

    // 연결된 뒤 Spotify 앱이 꺼지거나 연결이 끊기면 false 로 바뀐다
    private val _connected = MutableStateFlow(false)
    val connected: StateFlow<Boolean> = _connected.asStateFlow()

    // showAuthView: 처음 연결할 때 Spotify 권한 화면을 띄울지. 앱 시작 시 자동 재연결은 false
    suspend fun connect(showAuthView: Boolean = true): Result<Unit> = suspendCancellableCoroutine { cont ->
        val params = ConnectionParams.Builder(clientId)
            .setRedirectUri(redirectUri)
            .showAuthView(showAuthView)
            .build()
        SpotifyAppRemote.connect(activityContext ?: context, params, object : Connector.ConnectionListener {
            override fun onConnected(appRemote: SpotifyAppRemote) {
                remote = appRemote
                _connected.value = true
                appRemote.playerApi.subscribeToPlayerState().setEventCallback { state ->
                    val track = state.track
                    _nowPlaying.value = if (track == null) null
                    else NowPlaying(track.uri, track.duration, state.isPaused, track.name.orEmpty(), track.artist?.name.orEmpty())
                }
                if (cont.isActive) cont.resume(Result.success(Unit))
            }
            // 연결 실패뿐 아니라 연결된 뒤 끊겼을 때도 불린다. 이미 돌려준 continuation 은 다시 건드리지 않는다
            override fun onFailure(error: Throwable) {
                remote = null
                _connected.value = false
                _nowPlaying.value = null
                if (cont.isActive) cont.resume(Result.failure(error))
            }
        })
    }

    override fun play(uri: String) {
        val r = remote
        if (r == null) { Log.w(TAG, "play 무시: 연결 안 됨 $uri"); return }
        r.playerApi.play(uri)
            .setResultCallback { Log.d(TAG, "play OK $uri") }
            .setErrorCallback { Log.e(TAG, "play 실패 $uri", it) }
    }

    override fun enqueue(uri: String) {
        val r = remote
        if (r == null) { Log.w(TAG, "enqueue 무시: 연결 안 됨 $uri"); return }
        r.playerApi.queue(uri)
            .setResultCallback { Log.d(TAG, "enqueue OK $uri") }
            .setErrorCallback { Log.e(TAG, "enqueue 실패 $uri", it) }
    }

    override fun skipNext() { remote?.playerApi?.skipNext() }
    fun pause() { remote?.playerApi?.pause() }
    fun resume() { remote?.playerApi?.resume() }

    override fun release() {
        remote?.let { SpotifyAppRemote.disconnect(it) }
        remote = null
        _connected.value = false
        _nowPlaying.value = null
    }

    private companion object { const val TAG = "SpotifyPlayer" }
}