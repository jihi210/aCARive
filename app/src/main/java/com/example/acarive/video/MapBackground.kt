package com.example.acarive.video

import android.content.Context
import android.graphics.Bitmap
import android.graphics.PointF
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import com.example.acarive.data.drive.RoutePoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import org.maplibre.android.MapLibre
import org.maplibre.android.geometry.LatLng
import org.maplibre.android.geometry.LatLngBounds
import org.maplibre.android.maps.Style
import org.maplibre.android.snapshotter.MapSnapshot
import org.maplibre.android.snapshotter.MapSnapshotter
import java.io.File
import kotlin.math.max

private const val STYLE_URL = "https://tiles.openfreemap.org/styles/positron"

//지도 배경 결과
data class MapBackground(
    val pngFile: File,
    val width: Int,
    val height: Int,
    val pixels: List<PointF>
)

//경로 영역 지도 사진,좌표로 픽셀 위치 계산
suspend fun makeMapBackground(
    context: Context,
    points: List<RoutePoint>,
    sessionId: Long,
    width: Int = 1280,
    height: Int = 720
): MapBackground= withContext(Dispatchers.Main){
    require(points.size>=2){
        "점 2개 이상 필요"
    }
    MapLibre.getInstance(context)

    val options = MapSnapshotter.Options(width, height)
        .withStyleBuilder(Style.Builder().fromUri(STYLE_URL))
        .withRegion(paddedBounds(points, 0.15))
        .withPixelRatio(1f)

    //콜백 기다릴 수 있는 형태
    val snapshot= suspendCancellableCoroutine < MapSnapshot>{cont->
        val snapshotter= MapSnapshotter(context,options)
        snapshotter.start(
            { snap -> cont.resume(snap) },
            {error -> cont.resumeWithException(IllegalStateException("지도 스냅샷 실패: $error"))}
        )
        cont.invokeOnCancellation { snapshotter.cancel() }
    }

    //지도 사진의 정확한 픽셀 위치
    val pixels=points.map { snapshot.pixelForLatLng(LatLng(it.latitude, it.longitude)) }

    //배경 png로 저장
    val file= File(context.cacheDir,"bg_$sessionId.png")
    withContext(Dispatchers.IO){
        file.outputStream().use { snapshot.bitmap.compress(Bitmap.CompressFormat.PNG,100,it) }
    }
    MapBackground(file, snapshot.bitmap.width, snapshot.bitmap.height, pixels)
}

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