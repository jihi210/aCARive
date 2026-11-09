package com.example.acarive.ui.record

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import com.example.acarive.data.drive.RoutePoint
import com.example.acarive.ui.theme.AlertRed
import com.example.acarive.ui.theme.AppTheme
import kotlin.math.cos
import kotlin.math.min

@Composable
fun RouteCanvas(points: List<RoutePoint>, modifier: Modifier = Modifier) {
    val lineColor = AppTheme.colors.text
    val startFill = AppTheme.colors.surface
    Canvas(modifier = modifier) {
        if (points.size < 2) return@Canvas   // 점이 2개 미만이면 선을 못 그림

        //위도·경도 범위 구하기
        val minLat = points.minOf { it.latitude }
        val maxLat = points.maxOf { it.latitude }
        val minLng = points.minOf { it.longitude }
        val maxLng = points.maxOf { it.longitude }

        //경도 1도의 실제 길이는 위도에 따라 줄어들어서 보정
        val lngFactor = cos(Math.toRadians((minLat + maxLat) / 2))
        val latRange = (maxLat - minLat).coerceAtLeast(1e-6)
        val lngRange = ((maxLng - minLng) * lngFactor).coerceAtLeast(1e-6)

        //Canvas에 맞게 비율 정하기
        val pad = 32.dp.toPx()
        val w = size.width - pad * 2
        val h = size.height - pad * 2
        val scale = min(w / lngRange, h / latRange)
        val offsetX = pad + (w - lngRange * scale) / 2
        val offsetY = pad + (h - latRange * scale) / 2

        fun toScreen(p: RoutePoint) = Offset(
            x = (offsetX + (p.longitude - minLng) * lngFactor * scale).toFloat(),
            y = (offsetY + (maxLat - p.latitude) * scale).toFloat()
        )

        // 4) 점들을 순서대로 이어 선 만들기
        val path = Path().apply {
            moveTo(toScreen(points.first()).x, toScreen(points.first()).y)
            points.drop(1).forEach { p ->
                val o = toScreen(p)
                lineTo(o.x, o.y)
            }
        }

        drawPath(
            path = path,
            color = lineColor,
            style = Stroke(width = 6.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round)
        )
        // 출발은 속이 빈 원, 도착은 빨간 원. 색만으로 구분하지 않도록 모양을 다르게 한다.
        val radius = 12.dp.toPx()
        drawCircle(startFill, radius = radius, center = toScreen(points.first()))
        drawCircle(lineColor, radius = radius, center = toScreen(points.first()), style = Stroke(width = 4.dp.toPx()))
        drawCircle(AlertRed, radius = radius, center = toScreen(points.last()))
    }
}