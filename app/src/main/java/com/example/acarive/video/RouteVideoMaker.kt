package com.example.acarive.video

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.PorterDuff
import android.graphics.RectF
import android.net.Uri
import androidx.annotation.OptIn
import androidx.media3.common.MediaItem
import androidx.media3.common.MimeTypes
import androidx.media3.common.util.UnstableApi
import androidx.media3.effect.CanvasOverlay
import androidx.media3.effect.OverlayEffect
import androidx.media3.transformer.Composition
import androidx.media3.transformer.EditedMediaItem
import androidx.media3.transformer.Effects
import androidx.media3.transformer.ExportException
import androidx.media3.transformer.ExportResult
import androidx.media3.transformer.Transformer
import com.example.acarive.data.drive.DriveSession
import com.google.common.collect.ImmutableList
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import androidx.media3.common.Effect
import androidx.media3.common.audio.AudioProcessor
import androidx.media3.effect.TextureOverlay

private const val DRAW_US = 4_000_000L//경로 그리기 4초
private const val TOTAL_MS = 5_000L//영상 길이 5초
private const val FPS = 30
// 이 시간 동안 프레임이 하나도 안 써지면 Media3 가 내보내기를 취소한다 (기본 10초).
// 느린 차량 기기·에뮬레이터에서는 인코더 준비만 10초 가까이 걸려서 늘려 둔다
private const val MAX_MUXER_DELAY_MS = 30_000L

@OptIn(UnstableApi::class)
suspend fun makeRouteVideo(
    context: Context,
    bg: MapBackground,
    session: DriveSession,
    outFile: File
): File = withContext(Dispatchers.Main) {
    val bgItem = MediaItem.Builder()
        .setUri(Uri.fromFile(bg.pngFile))
        .setImageDurationMs(TOTAL_MS)
        .build()

    val edited = EditedMediaItem.Builder(bgItem)
        .setFrameRate(FPS)//1초에 30장
        .setEffects(
            Effects(
                /* audioProcessors = */ ImmutableList.of<AudioProcessor>(),
                /* videoEffects = */ ImmutableList.of<Effect>(
                    OverlayEffect(ImmutableList.of<TextureOverlay>(RouteOverlay(bg, session)))
                )
            )
        )
        .build()

    suspendCancellableCoroutine { cont ->
        val transformer = Transformer.Builder(context)
            .setVideoMimeType(MimeTypes.VIDEO_H264)
            .setMaxDelayBetweenMuxerSamplesMs(MAX_MUXER_DELAY_MS)
            .addListener(object : Transformer.Listener {
                override fun onCompleted(composition: Composition, exportResult: ExportResult) {
                    cont.resume(outFile)
                }
                override fun onError(
                    composition: Composition,
                    exportResult: ExportResult,
                    exportException: ExportException
                ) {
                    cont.resumeWithException(exportException)
                }
            })
            .build()
        //내보내기
        transformer.start(edited, outFile.absolutePath)
        //중간에 화면 나가면 내보내기 취소
        cont.invokeOnCancellation { transformer.cancel() }
    }
}

//프레임마다 호출(진행률만큼 경로,숫자 그리기)
@OptIn(UnstableApi::class)
private class RouteOverlay(
    private val bg: MapBackground,
    private val s: DriveSession
) : CanvasOverlay(/* useInputFrameAspectRatio = */ true) {

    private val line = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#3B6FE0"); style = Paint.Style.STROKE
        strokeWidth = 9f; strokeCap = Paint.Cap.ROUND; strokeJoin = Paint.Join.ROUND
    }
    private val dot = Paint(Paint.ANTI_ALIAS_FLAG)
    private val panel = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.argb(190, 16, 24, 40) }
    private val title = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.WHITE; textSize = 34f }
    private val big = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE; textSize = 52f; isFakeBoldText = true
    }
    private val dateText = SimpleDateFormat("yyyy.MM.dd", Locale.KOREA).format(Date(s.startTime))

    override fun onDraw(canvas: Canvas, presentationTimeUs: Long) {
        canvas.drawColor(Color.TRANSPARENT, PorterDuff.Mode.CLEAR)

        val sx = canvas.width.toFloat() / bg.width
        val sy = canvas.height.toFloat() / bg.height

        val progress = (presentationTimeUs.toFloat() / DRAW_US).coerceIn(0f, 1f)
        val px = bg.pixels
        val shown = px.take((px.size * progress).toInt().coerceIn(2, px.size))

        val path = Path().apply {
            moveTo(shown.first().x * sx, shown.first().y * sy)
            shown.drop(1).forEach { lineTo(it.x * sx, it.y * sy) }
        }
        canvas.drawPath(path, line)

        dot.color = Color.parseColor("#2E9E5B")
        canvas.drawCircle(shown.first().x * sx, shown.first().y * sy, 12f, dot)
        dot.color = Color.parseColor("#E5484D")
        canvas.drawCircle(shown.last().x * sx, shown.last().y * sy, 15f, dot)

        val km = s.distanceMeters / 1000 * progress
        val sec = ((s.endTime - s.startTime) / 1000 * progress).toInt()
        val h = canvas.height.toFloat()
        canvas.drawRoundRect(RectF(32f, h - 170f, 560f, h - 32f), 24f, 24f, panel)
        canvas.drawText("$dateText 드라이브", 60f, h - 118f, title)
        canvas.drawText("%.1f km · %d분 %d초".format(km, sec / 60, sec % 60), 60f, h - 58f, big)
        // 지도 출처(© OpenStreetMap contributors)는 지도 스냅샷에 이미 들어 있다
    }
}