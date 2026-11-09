package com.example.driveapp.ui

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import com.example.driveapp.data.RoutePoint
import com.example.driveapp.location.LocationPoint
import kotlin.math.cos
import kotlin.math.min

@Composable
fun RouteCanvas(points: List<LocationPoint>, modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        if (points.size < 2) return@Canvas   // 점이 2개 미만이면 선을 못 그림

        //위도,경도 범위 구하기
        val minLat = points.minOf { it.lat }
        val maxLat = points.maxOf { it.lat }
        val minLng = points.minOf { it.lng }
        val maxLng = points.maxOf { it.lng }

        //경도 1도의 실제 길이는 위도에 따라 줄어들어서 보정
        val lngFactor = cos(Math.toRadians((minLat + maxLat) / 2))
        val latRange = (maxLat - minLat).coerceAtLeast(1e-6)
        val lngRange = ((maxLng - minLng) * lngFactor).coerceAtLeast(1e-6)

        val pad = 40f
        val w = size.width - pad * 2
        val h = size.height - pad * 2
        val scale = min(w / lngRange, h / latRange)
        val offsetX = pad + (w - lngRange * scale) / 2
        val offsetY = pad + (h - latRange * scale) / 2

        fun toScreen(p: LocationPoint) = Offset(
            x = (offsetX + (p.lng - minLng) * lngFactor * scale).toFloat(),
            y = (offsetY + (maxLat - p.lat) * scale).toFloat()
        )

        // 점들을 순서대로 이어 선 만들기
        val path = Path().apply {
            val first = toScreen(points.first())
            moveTo(first.x, first.y)
            points.drop(1).forEach { p ->
                val o = toScreen(p)
                lineTo(o.x, o.y)
            }
        }

        drawPath(
            path = path,
            color = Color(0xFF3B6FE0),
            style = Stroke(width = 8f, cap = StrokeCap.Round, join = StrokeJoin.Round)
        )
        drawCircle(Color(0xFF2E9E5B), radius = 14f, center = toScreen(points.first()))
        drawCircle(Color(0xFFD64545), radius = 14f, center = toScreen(points.last()))
    }
}