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

/** Spotify App Remote SDK로 Spotify 앱을 원격 제어한다. Spotify 앱 설치·로그인과 Premium이 필요하다. */
class SpotifyPlayer(
    private val context: Context,
    private val clientId: String,
    private val redirectUri: String,
) : MusicPlayer {
    private var remote: SpotifyAppRemote? = null
    private val _nowPlaying = MutableStateFlow<NowPlaying?>(null)
    override val nowPlaying: StateFlow<NowPlaying?> = _nowPlaying.asStateFlow()

    suspend fun connect(): Result<Unit> = suspendCancellableCoroutine { cont ->
        val params = ConnectionParams.Builder(clientId)
            .setRedirectUri(redirectUri)
            .showAuthView(true)
            .build()
        SpotifyAppRemote.connect(context, params, object : Connector.ConnectionListener {
            override fun onConnected(appRemote: SpotifyAppRemote) {
                remote = appRemote
                appRemote.playerApi.subscribeToPlayerState().setEventCallback { state ->
                    val track = state.track
                    _nowPlaying.value = if (track == null) null
                    else NowPlaying(track.uri, track.duration, state.isPaused)
                }
                cont.resume(Result.success(Unit))
            }
            override fun onFailure(error: Throwable) { cont.resume(Result.failure(error)) }
        })
    }

    override fun play(uri: String) { remote?.playerApi?.play(uri) }
    override fun enqueue(uri: String) { remote?.playerApi?.queue(uri) }
    override fun skipNext() { remote?.playerApi?.skipNext() }
    override fun release() { remote?.let { SpotifyAppRemote.disconnect(it) } }
}
