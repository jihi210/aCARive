package com.example.acarive.video

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.PointF
import android.util.Log
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import com.example.acarive.data.drive.RoutePoint
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import org.maplibre.android.MapLibre
import org.maplibre.android.geometry.LatLng
import org.maplibre.android.geometry.LatLngBounds
import org.maplibre.android.maps.Style
import org.maplibre.android.snapshotter.MapSnapshot
import org.maplibre.android.snapshotter.MapSnapshotter
import java.io.File
import kotlin.math.PI
import kotlin.math.ln
import kotlin.math.max
import kotlin.math.min
import kotlin.math.tan

private const val TAG = "MapBackground"
private const val STYLE_URL = "https://tiles.openfreemap.org/styles/positron"
private const val SNAPSHOT_TRIES = 3
// 인터넷이 애매하면 콜백이 안 올 수도 있어서 한 번에 이 시간까지만 기다린다
private const val SNAPSHOT_TIMEOUT_MS = 15_000L
// 지도 없이 그릴 때 배경색 (CI 팔레트의 밝은 베이지)
private const val PLAIN_BG = "#F6F3F2"

//지도 배경 결과
data class MapBackground(
    val pngFile: File,
    val width: Int,
    val height: Int,
    val pixels: List<PointF>
)

//경로 영역 지도 사진,좌표로 픽셀 위치 계산
//지도를 끝내 못 받으면 단색 배경에 경로 위치만 맞춰서 돌려준다
suspend fun makeMapBackground(
    context: Context,
    points: List<RoutePoint>,
    sessionId: Long,
    width: Int = 1280,
    height: Int = 720
): MapBackground {
    require(points.size>=2){
        "점 2개 이상 필요"
    }
    val route = simplifyRoute(points)

    repeat(SNAPSHOT_TRIES) { attempt ->
        try {
            return snapshotBackground(context, route, sessionId, width, height)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.w(TAG, "지도 스냅샷 실패 (${attempt + 1}/$SNAPSHOT_TRIES)", e)
            if (attempt < SNAPSHOT_TRIES - 1) delay(1_000L * (attempt + 1))
        }
    }
    Log.w(TAG, "지도 없이 단색 배경으로 만든다")
    return plainBackground(context, route, sessionId, width, height)
}

private suspend fun snapshotBackground(
    context: Context,
    points: List<RoutePoint>,
    sessionId: Long,
    width: Int,
    height: Int
): MapBackground = withContext(Dispatchers.Main) {
    MapLibre.getInstance(context)

    val options = MapSnapshotter.Options(width, height)
        .withStyleBuilder(Style.Builder().fromUri(STYLE_URL))
        .withRegion(paddedBounds(points, 0.15))
        .withPixelRatio(1f)

    //콜백 기다릴 수 있는 형태
    val snapshot = withTimeoutOrNull(SNAPSHOT_TIMEOUT_MS) {
        suspendCancellableCoroutine<MapSnapshot> { cont ->
            val snapshotter = MapSnapshotter(context, options)
            snapshotter.start(
                { snap -> if (cont.isActive) cont.resume(snap) },
                { error -> if (cont.isActive) cont.resumeWithException(IllegalStateException("지도 스냅샷 실패: $error")) }
            )
            cont.invokeOnCancellation { snapshotter.cancel() }
        }
    } ?: error("지도 스냅샷 시간 초과")

    //지도 사진의 정확한 픽셀 위치
    val pixels=points.map { snapshot.pixelForLatLng(LatLng(it.latitude, it.longitude)) }

    //배경 png로 저장
    val file= File(context.cacheDir,"bg_$sessionId.png")
    withContext(Dispatchers.IO){
        file.outputStream().use { snapshot.bitmap.compress(Bitmap.CompressFormat.PNG,100,it) }
    }
    MapBackground(file, snapshot.bitmap.width, snapshot.bitmap.height, pixels)
}

//단색 배경 + 메르카토르로 직접 픽셀 위치 계산 (지도와 같은 비율이라 경로 모양이 안 찌그러진다)
private suspend fun plainBackground(
    context: Context,
    points: List<RoutePoint>,
    sessionId: Long,
    width: Int,
    height: Int
): MapBackground = withContext(Dispatchers.IO) {
    val xs = points.map { Math.toRadians(it.longitude) }
    val ys = points.map { mercatorY(it.latitude) }
    val minX = xs.min(); val maxX = xs.max()
    val minY = ys.min(); val maxY = ys.max()
    // 아주 짧은 경로도 너무 크게 확대되지 않게
    val minSpan = Math.toRadians(0.004)
    val spanX = max(maxX - minX, minSpan)
    val spanY = max(maxY - minY, minSpan)
    val pad = 0.15
    val scale = min(width * (1 - 2 * pad) / spanX, height * (1 - 2 * pad) / spanY)
    val cx = (minX + maxX) / 2
    val cy = (minY + maxY) / 2
    val pixels = xs.indices.map { i ->
        PointF(
            (width / 2.0 + (xs[i] - cx) * scale).toFloat(),
            (height / 2.0 - (ys[i] - cy) * scale).toFloat()
        )
    }

    val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
    Canvas(bitmap).drawColor(Color.parseColor(PLAIN_BG))
    val file = File(context.cacheDir, "bg_$sessionId.png")
    file.outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
    bitmap.recycle()
    MapBackground(file, width, height, pixels)
}

private fun mercatorY(lat: Double): Double = ln(tan(PI / 4 + Math.toRadians(lat) / 2))

//경로 범위에 여백 둠(너무 짧은 경로도 최소 크기 보장)
private fun paddedBounds(points: List<RoutePoint>, ratio: Double): LatLngBounds {
    val minLat = points.minOf { it.latitude }
    val maxLat = points.maxOf { it.latitude }
    val minLng = points.minOf { it.longitude }
    val maxLng = points.maxOf { it.longitude }
    val latPad = max((maxLat - minLat) * ratio, 0.002)
    val lngPad = max((maxLng - minLng) * ratio, 0.002)
    return LatLngBounds.from(maxLat + latPad, maxLng + lngPad, minLat - latPad, minLng - lngPad)
}
