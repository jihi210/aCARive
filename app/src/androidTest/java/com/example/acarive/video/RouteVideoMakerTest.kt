package com.example.acarive.video

import android.graphics.Bitmap
import android.media.MediaMetadataRetriever
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.example.acarive.data.drive.DriveSession
import com.example.acarive.data.drive.RoutePoint
import com.example.acarive.location.mockRoute
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File

// 목업 경로로 실제 영상을 만들어 본다. 지도 타일을 받아야 해서 인터넷이 필요하다.
@RunWith(AndroidJUnit4::class)
class RouteVideoMakerTest {

    @Test
    fun makesFiveSecondVideoFromRoute(): Unit = runBlocking {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val points = mockRoute.mapIndexed { i, p ->
            RoutePoint(sessionId = 1, seq = i, latitude = p.lat, longitude = p.lng, time = i * 1_000L)
        }
        val session = DriveSession(id = 1, startTime = 0, endTime = 30 * 60_000L, distanceMeters = 12_300.0, pointCount = points.size)
        val out = File(context.cacheDir, "test_drive.mp4").apply { delete() }

        val bg = makeMapBackground(context, points, sessionId = 1)
        makeRouteVideo(context, bg, session, out)

        assertTrue("영상 파일이 비어 있음", out.length() > 0)
        MediaMetadataRetriever().use { r ->
            r.setDataSource(out.absolutePath)
            val durationMs = r.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)!!.toLong()
            assertTrue("길이 ${durationMs}ms", durationMs in 4_500..5_500)
            assertEquals("yes", r.extractMetadata(MediaMetadataRetriever.METADATA_KEY_HAS_VIDEO))
            assertEquals("1280", r.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH))
            assertEquals("720", r.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_HEIGHT))
            // 눈으로 확인할 프레임 (경로 다 그린 뒤)
            r.getFrameAtTime(4_500_000)?.let { frame ->
                File(context.cacheDir, "test_drive_frame.png").outputStream().use {
                    frame.compress(Bitmap.CompressFormat.PNG, 100, it)
                }
            }
        }
    }
}
