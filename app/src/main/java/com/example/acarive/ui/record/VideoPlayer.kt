package com.example.acarive.ui.record

import android.net.Uri
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.MediaItem
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import java.io.File

//mp4를 앱 안에서 재생
@Composable
fun VideoPlayer(file: File, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val player = remember(file) {
        ExoPlayer.Builder(context).build().apply {
            volume = 0f//소리 없음
            setMediaItem(MediaItem.fromUri(Uri.fromFile(file)))
            prepare()//영상 미리 읽어 재생 준비
            playWhenReady = true
        }
    }
    //사라질 때 영상 플레이어가 쓴 메모리, 디코더 반납
    DisposableEffect(player) { onDispose { player.release() } }

    AndroidView(
        modifier = modifier,
        //화면 만들 때 딱 한 번 이용함
        factory = { PlayerView(it).apply { this.player = player } }
    )
}