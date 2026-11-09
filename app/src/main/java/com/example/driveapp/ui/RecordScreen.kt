package com.example.driveapp.ui

import android.location.Location
import android.os.Environment
import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.driveapp.data.DriveDao
import com.example.driveapp.data.DriveDatabase
import com.example.driveapp.data.DriveSession
import com.example.driveapp.data.RoutePoint
import com.example.driveapp.location.LocationPoint
import com.example.driveapp.location.mockRoute
import com.example.driveapp.video.makeMapBackground
import com.example.driveapp.video.makeRouteVideo
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.io.File

@Composable
fun RecordScreen(
    modifier: Modifier = Modifier,
    liveLocation: LocationPoint? = null,   // Pleos에서 받은 위치
    useMock: Boolean = true
) {
    val context = LocalContext.current
    val dao = remember { DriveDatabase.get(context).driveDao() }
    val scope = rememberCoroutineScope()//계속 실행될 수 있도록 suspend는 시간 걸리는 작업 담당이기에

    // 기록 상태
    var isRecording by remember { mutableStateOf(false) }
    val recorded = remember { mutableStateListOf<LocationPoint>() }
    val latestLive by rememberUpdatedState(liveLocation)
    var startTime by remember { mutableStateOf(0L) }
    var liveDistance by remember { mutableStateOf(0.0) }   //기록 중 누적 거리(m)

    // 저장 목록
    val sessionsFlow = remember(dao) { dao.getSessions() }
    val sessions by sessionsFlow.collectAsState(initial = emptyList())
    var selectedSession by remember { mutableStateOf<DriveSession?>(null) }
    var selectedPoints by remember { mutableStateOf<List<RoutePoint>>(emptyList()) }

    // 영상
    var makingVideo by remember { mutableStateOf(false) }
    var videoMsg by remember { mutableStateOf("") }
    var videoFile by remember { mutableStateOf<File?>(null) }

    // 기록 중이면 1초마다 위치 추가 + 거리 누적
    LaunchedEffect(isRecording) {
        if (isRecording) {
            recorded.clear()
            liveDistance = 0.0
            var i = 0
            val result = FloatArray(1)
            while (true) {
                val p = if (useMock) mockRoute[i % mockRoute.size] else latestLive
                if (p != null) {
                    recorded.lastOrNull()?.let { prev ->
                        Location.distanceBetween(prev.lat, prev.lng, p.lat, p.lng, result)
                        liveDistance += result[0]
                    }
                    recorded.add(p)
                }
                i++
                delay(1000)
            }
        }
    }

    //기록 중이면 실시간/고른 드라이브
    val centerPoints = if (isRecording) recorded.toList()
    else selectedPoints.map { LocationPoint(it.latitude, it.longitude) }

    Box(modifier = modifier.fillMaxSize()) {

        RouteCanvas(
            points = centerPoints,
            modifier = Modifier.fillMaxSize().padding(horizontal = 420.dp, vertical = 80.dp)
        )
        if (centerPoints.size < 2 && videoFile == null) {
            Text(
                if (isRecording) "경로를 기록하고 있어요" else "기록을 시작하면 경로가 여기에 그려져요",
                fontSize = 28.sp, color = Color.Gray,
                modifier = Modifier.align(Alignment.Center)
            )
        }

        //기록중이 아닐 경우 가운데에서 영상
        if (!isRecording) {
            videoFile?.let {
                VideoPlayer(
                    file = it,
                    modifier = Modifier.align(Alignment.Center).size(width = 960.dp, height = 540.dp)
                )
            }
        }

        //상태
        Column(modifier = Modifier.align(Alignment.TopStart).padding(32.dp)) {
            Text(if (isRecording) "● 기록 중" else "대기 중", fontSize = 32.sp,
                color = if (isRecording) Color(0xFFE5484D) else Color.Gray)
            if (isRecording) {
                Text("%.1f km · 점 %d개".format(liveDistance / 1000, recorded.size), fontSize = 26.sp)
            }
            Text(if (useMock) "MOCK" else "실제 위치", fontSize = 16.sp, color = Color.Gray)
        }

        // 저장된 드라이브 영상
        if (!isRecording) {
            Column(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(32.dp)
                    .width(360.dp)
                    .background(Color(0x14000000), RoundedCornerShape(16.dp))
                    .padding(20.dp)
            ) {
                Text("저장된 드라이브 ${sessions.size}개", fontSize = 24.sp)
                Spacer(Modifier.height(8.dp))
                sessions.take(3).forEach { s ->
                    Text(
                        "#${s.id}  ${"%.1f".format(s.distanceMeters / 1000)}km  ${(s.endTime - s.startTime) / 60000}분",
                        fontSize = 22.sp,
                        color = if (s.id == selectedSession?.id) Color(0xFF3B6FE0) else Color.Unspecified,
                        modifier = Modifier
                            .padding(vertical = 10.dp)
                            .clickable {
                                selectedSession = s
                                videoFile = null
                                videoMsg = ""
                                scope.launch { selectedPoints = dao.getPoints(s.id) }
                            }
                    )
                }

                Spacer(Modifier.height(12.dp))
                Button(
                    enabled = selectedSession != null && selectedPoints.size >= 2 && !makingVideo,
                    onClick = {
                        val s = selectedSession ?: return@Button
                        scope.launch {
                            makingVideo = true
                            try {
                                videoMsg = "지도 준비 중..."
                                val bg = makeMapBackground(context, selectedPoints, s.id)
                                videoMsg = "영상 만드는 중..."
                                val dir = context.getExternalFilesDir(Environment.DIRECTORY_MOVIES)!!
                                videoFile = makeRouteVideo(context, bg, s, File(dir, "drive_${s.id}.mp4"))
                                videoMsg = ""
                            } catch (e: Exception) {
                                videoMsg = "실패: ${e.message}"
                                Log.e("VIDEO", "영상 만들기 실패", e)
                            } finally {
                                makingVideo = false
                            }
                        }
                    }
                ) {
                    Text(if (makingVideo) "만드는 중..." else "추억 영상 만들기", fontSize = 22.sp)
                }
                if (videoMsg.isNotEmpty()) Text(videoMsg, fontSize = 18.sp)
            }
        }

        //기록버튼
        Button(
            onClick = {
                if (!isRecording) {
                    startTime = System.currentTimeMillis()
                    videoFile = null
                } else {
                    val points = recorded.toList()
                    if (points.isNotEmpty()) {
                        scope.launch { saveDrive(dao, startTime, points) }
                    }
                }
                isRecording = !isRecording
            },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(32.dp)
                .size(width = 220.dp, height = 88.dp)
        ) {
            Text(if (isRecording) "기록 종료" else "기록 시작", fontSize = 28.sp)
        }
    }

}

//드라이브 1회를 DB에 저장
suspend fun saveDrive(
    dao: DriveDao,
    startTime: Long,
    points: List<LocationPoint>
) {
    //총 거리 계산
    var distance = 0.0
    val result = FloatArray(1)
    for (i in 1 until points.size) {
        Location.distanceBetween(
            points[i - 1].lat, points[i - 1].lng,
            points[i].lat, points[i].lng, result
        )
        distance += result[0]
    }

    val endTime = System.currentTimeMillis()
    val sessionId = dao.insertSession(
        DriveSession(
            startTime = startTime,
            endTime = endTime,
            distanceMeters = distance,
            pointCount = points.size
        )
    )

    dao.insertPoints(points.mapIndexed { i, p ->
        RoutePoint(sessionId = sessionId, seq = i, latitude = p.lat, longitude = p.lng, time = startTime + i * 1000L)
    })
}