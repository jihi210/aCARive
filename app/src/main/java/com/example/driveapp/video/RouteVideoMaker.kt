package com.example.driveapp.video

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.PointF
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
import com.example.driveapp.car.CarModel
import com.example.driveapp.car.TopDownCar
import com.example.driveapp.data.DriveSession
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
import kotlin.math.atan2
import kotlin.math.hypot
import androidx.media3.common.Effect
import androidx.media3.common.audio.AudioProcessor
import androidx.media3.effect.TextureOverlay

private const val DRAW_US = 4_000_000L//경로 그리기 4초
private const val TOTAL_MS = 5_000L//영상 길이 5초
private const val FPS = 30

@OptIn(UnstableApi::class)
suspend fun makeRouteVideo(
    context: Context,
    bg: MapBackground,
    session: DriveSession,
    car: CarModel,
    outFile: File
): File = withContext(Dispatchers.Main) {
    // 같은 드라이브를 다른 차로 다시 만들 때 이전 파일 지우기
    if (outFile.exists()) outFile.delete()

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
                    OverlayEffect(ImmutableList.of<TextureOverlay>(RouteOverlay(bg, session, car)))
                )
            )
        )
        .build()

    suspendCancellableCoroutine<File> { cont ->
        val transformer = Transformer.Builder(context)
            .setVideoMimeType(MimeTypes.VIDEO_H264)
            .addListener(object : Transformer.Listener {
                override fun onCompleted(composition: Composition, exportResult: ExportResult) {
                    if (cont.isActive) cont.resume(outFile)
                }
                override fun onError(
                    composition: Composition,
                    exportResult: ExportResult,
                    exportException: ExportException
                ) {
                    if (cont.isActive) cont.resumeWithException(exportException)
                }
            })
            .build()
        //내보내기
        transformer.start(edited, outFile.absolutePath)
        //중간에 화면 나가면 내보내기 취소
        cont.invokeOnCancellation { transformer.cancel() }
    }
}

//프레임마다 호출: 진행률만큼 경로를 그리고, 그 끝에 고른 차가 위에서 본 모습으로 달림
@OptIn(UnstableApi::class)
private class RouteOverlay(
    private val bg: MapBackground,
    private val s: DriveSession,
    private val car: CarModel
) : CanvasOverlay(/* useInputFrameAspectRatio = */ true) {

    private val line = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#3B6FE0"); style = Paint.Style.STROKE
        strokeWidth = 9f; strokeCap = Paint.Cap.ROUND; strokeJoin = Paint.Join.ROUND
    }
    private val dot = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.parseColor("#2E9E5B") }
    private val panel = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.argb(190, 16, 24, 40) }
    private val title = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.WHITE; textSize = 34f }
    private val big = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE; textSize = 52f; isFakeBoldText = true
    }
    private val credit = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.DKGRAY; textSize = 16f }
    private val dateText = SimpleDateFormat("yyyy.MM.dd", Locale.KOREA).format(Date(s.startTime))

    // 지도 사진 위 경로 점들과, 출발점부터 각 점까지 쌓인 길이(px)
    // 점 간격이 들쭉날쭉해도(정차 중엔 같은 점이 반복됨) 차가 일정한 속도로 움직이도록 길이 기준으로 이동
    private val pts = bg.pixels
    private val cum = FloatArray(pts.size).also { c ->
        for (i in 1 until pts.size) {
            c[i] = c[i - 1] + hypot(pts[i].x - pts[i - 1].x, pts[i].y - pts[i - 1].y)
        }
    }
    private val total = cum.last()

    // 출발점에서 d(px)만큼 간 위치와, 그 위치가 몇 번째 구간인지
    private fun pointAt(d: Float): Pair<PointF, Int> {
        if (total <= 0f) return PointF(pts.first().x, pts.first().y) to 0
        val dist = d.coerceIn(0f, total)
        var i = 1
        while (i < pts.size - 1 && cum[i] < dist) i++
        val segLen = cum[i] - cum[i - 1]
        val t = if (segLen > 0f) (dist - cum[i - 1]) / segLen else 0f
        val a = pts[i - 1]
        val b = pts[i]
        return PointF(a.x + (b.x - a.x) * t, a.y + (b.y - a.y) * t) to i
    }

    // 차가 향할 방향(도). 바로 앞 몇 px를 미리 봐서 정하면 방향이 덜 흔들림
    private var lastHeading = 0f
    private fun headingAt(d: Float): Float {
        val look = 14f
        val from = pointAt((d - look / 2).coerceAtMost(total - look))
        val to = pointAt((d + look / 2).coerceAtLeast(look))
        val dx = to.first.x - from.first.x
        val dy = to.first.y - from.first.y
        if (hypot(dx, dy) > 0.5f) {
            // atan2 기준(오른쪽 0도)을 '화면 위쪽 0도, 시계 방향'으로 바꿈
            lastHeading = Math.toDegrees(atan2(dy, dx).toDouble()).toFloat() + 90f
        }
        return lastHeading
    }

    override fun onDraw(canvas: Canvas, presentationTimeUs: Long) {
        canvas.drawColor(Color.TRANSPARENT, PorterDuff.Mode.CLEAR)

        val sx = canvas.width.toFloat() / bg.width
        val sy = canvas.height.toFloat() / bg.height

        val progress = (presentationTimeUs.toFloat() / DRAW_US).coerceIn(0f, 1f)
        val d = total * progress
        val (carPos, seg) = pointAt(d)

        // 지나온 경로 선: 출발점 → 지나온 점들 → 지금 차 위치
        val path = Path().apply {
            moveTo(pts.first().x * sx, pts.first().y * sy)
            for (i in 1 until seg) lineTo(pts[i].x * sx, pts[i].y * sy)
            lineTo(carPos.x * sx, carPos.y * sy)
        }
        canvas.drawPath(path, line)

        // 출발점(초록)
        canvas.drawCircle(pts.first().x * sx, pts.first().y * sy, 12f, dot)

        // 고른 차 (위에서 본 모습, 진행 방향으로 회전)
        TopDownCar.draw(
            canvas, car,
            cx = carPos.x * sx, cy = carPos.y * sy,
            lengthPx = canvas.height * 0.11f,
            headingDeg = headingAt(d)
        )

        // 숫자 카드(왼쪽 아래)
        val km = s.distanceMeters / 1000 * progress
        val sec = ((s.endTime - s.startTime) / 1000 * progress).toInt()
        val h = canvas.height.toFloat()
        canvas.drawRoundRect(RectF(32f, h - 170f, 600f, h - 32f), 24f, 24f, panel)
        canvas.drawText("$dateText · ${car.displayName}", 60f, h - 118f, title)
        canvas.drawText("%.1f km · %d분 %d초".format(km, sec / 60, sec % 60), 60f, h - 58f, big)

        val c = "© OpenStreetMap contributors"
        canvas.drawText(c, canvas.width - credit.measureText(c) - 16f, h - 12f, credit)
    }
}
