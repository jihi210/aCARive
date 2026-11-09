package com.example.acarive.data.drive

import android.location.Location
import com.example.acarive.data.model.DriveHistoryItem
import com.example.acarive.data.model.DriveSummary
import com.example.acarive.location.LocationPoint
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

private const val RECENT_DRIVE_COUNT = 5
private val DateFormatter = DateTimeFormatter.ofPattern("MM. dd", Locale.KOREA)

/** 저장된 드라이브로 올해 누적 수치와 최근 기록을 만든다. [sessions]는 최신순이어야 한다. */
fun List<DriveSession>.toDriveSummary(
    year: Int,
    zone: ZoneId = ZoneId.systemDefault(),
): DriveSummary {
    val thisYear = filter { Instant.ofEpochMilli(it.startTime).atZone(zone).year == year }
    return DriveSummary(
        year = year,
        totalKm = thisYear.sumOf { it.distanceMeters } / 1_000,
        driveCount = thisYear.size,
        totalDuration = formatDuration(thisYear.sumOf { it.endTime - it.startTime }),
        recentDrives = take(RECENT_DRIVE_COUNT).map { session ->
            DriveHistoryItem(
                sessionId = session.id,
                date = Instant.ofEpochMilli(session.startTime).atZone(zone).format(DateFormatter),
                distance = formatDistance(session.distanceMeters),
                duration = formatDuration(session.endTime - session.startTime),
            )
        },
    )
}

fun formatDistance(meters: Double): String =
    if (meters < 1_000) {
        String.format(Locale.US, "%.0f m", meters)
    } else {
        String.format(Locale.US, "%,.1f km", meters / 1_000)
    }

fun formatDuration(millis: Long): String {
    val minutes = millis / 60_000
    return when {
        millis <= 0 -> "0분"
        minutes < 1 -> "1분 미만"
        minutes < 60 -> "${minutes}분"
        minutes % 60 == 0L -> "${minutes / 60}시간"
        else -> "${minutes / 60}시간 ${minutes % 60}분"
    }
}

/** 기록한 위치들을 드라이브 한 건으로 저장한다. */
suspend fun saveDrive(dao: DriveDao, startTime: Long, points: List<LocationPoint>) {
    // 점 사이 거리를 모두 더해 총 거리 계산
    var distance = 0.0
    val result = FloatArray(1)
    for (i in 1 until points.size) {
        Location.distanceBetween(
            points[i - 1].lat, points[i - 1].lng,
            points[i].lat, points[i].lng, result,
        )
        distance += result[0]
    }

    val sessionId = dao.insertSession(
        DriveSession(
            startTime = startTime,
            endTime = System.currentTimeMillis(),
            distanceMeters = distance,
            pointCount = points.size,
        ),
    )
    // 좌표 저장
    dao.insertPoints(
        points.mapIndexed { i, p ->
            RoutePoint(sessionId = sessionId, seq = i, latitude = p.lat, longitude = p.lng, time = startTime + i * 1000L)
        },
    )
}
