package com.example.driveapp.ui

import android.location.Location
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.driveapp.data.DriveDatabase
import com.example.driveapp.data.DriveSession
import com.example.driveapp.data.RoutePoint
import com.example.driveapp.location.LocationPoint
import com.example.driveapp.location.mockRoute
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import androidx.compose.foundation.clickable
@Composable
fun RecordScreen(modifier: Modifier = Modifier,
                 liveLocation: LocationPoint?=null,//Pleos에서 받은 위치
                 useMock: Boolean=true
                 ) {
    // 기록 중인지 아닌지 기억하는 상태값
    var isRecording by remember { mutableStateOf(false) }

    //지금 들어온 위치
    var current by remember { mutableStateOf<LocationPoint?>(null) }

    //이번 기록에서 쌓인 위치 목록
    var recorded=remember { mutableStateListOf<LocationPoint>() }
    // 반복문 안에서도 항상 최신 위치를 읽게 해 줌
    val latestLive by rememberUpdatedState(liveLocation)

    //DB연결
    val dao= DriveDatabase.get(LocalContext.current).driveDao()
    val scope= rememberCoroutineScope()//저장 작업용
    val sessions by dao.getSessions().collectAsState(initial = emptyList())
    var startTime by remember { mutableStateOf(0L) }

    var selectedPoints by remember {mutableStateOf<List<RoutePoint>>(emptyList())}
    LaunchedEffect(isRecording) {
        if(isRecording){
            recorded.clear()
            var i=0
            while(true){
                val p=if (useMock) mockRoute[i%mockRoute.size]else latestLive
                if(p!=null) {
                    current = p
                    recorded.add(p)
                }
                i++
                delay(1000)
            }
        }
    }

    Column(
        modifier = modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(if (isRecording) "기록 중..." else "대기 중", fontSize = 40.sp)
        Spacer(Modifier.height(32.dp))

        Button(
            onClick = {
                if(!isRecording){
                    startTime = System.currentTimeMillis()
                }else{
                    val points=recorded.toList()
                    if(points.isNotEmpty()){
                        scope.launch { saveDrive(dao,startTime,points) }
                    }
                }
                isRecording = !isRecording
            },
            modifier = Modifier.size(width = 400.dp, height = 120.dp)
        ) {
            Text(if (isRecording) "기록 종료" else "기록 시작", fontSize = 36.sp)
        }

        Spacer(Modifier.height(32.dp))
        Text(if (useMock) "모드: MOCK" else "모드: 실제 위치", fontSize = 24.sp)
        Text("위도 ${current?.lat ?: "-"} / 경도 ${current?.lng ?: "-"}", fontSize = 28.sp)
        Text("기록된 점: ${recorded.size}개", fontSize = 28.sp)

        Spacer(Modifier.height(24.dp))
        Text("저장된 드라이브: ${sessions.size}개", fontSize = 28.sp)
        sessions.take(3).forEach { s ->
            Text("#${s.id}  점 ${s.pointCount}개  ${"%.0f".format(s.distanceMeters)}m  ${(s.endTime - s.startTime) / 1000}초", fontSize = 22.sp,
                modifier= Modifier.clickable{
                    scope.launch { selectedPoints=dao.getPoints(s.id) }
                }
            )
        }
        Spacer(Modifier.height(16.dp))
        RouteCanvas(
            points = selectedPoints,
            modifier = Modifier.size(width = 500.dp, height = 280.dp)
        )
    }
}
suspend fun saveDrive(
    dao: com.example.driveapp.data.DriveDao,
    startTime: Long,
    points: List<LocationPoint>
) {
    // 점 사이 거리를 모두 더해 총 거리 계산
    var distance = 0.0
    val result = FloatArray(1)
    for (i in 1 until points.size) {//0<->1, 1<->2...
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
    // 좌표 저장
    dao.insertPoints(points.mapIndexed { i, p ->
        RoutePoint(sessionId = sessionId, seq = i, latitude = p.lat, longitude = p.lng, time = startTime + i * 1000L)
    })
}