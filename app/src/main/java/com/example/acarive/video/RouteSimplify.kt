package com.example.acarive.video

import com.example.acarive.data.drive.RoutePoint
import com.example.acarive.location.distanceKm

// 영상에 쓸 경로 점 최대 개수 (1초마다 저장이라 3시간이면 ~1만 개)
const val VIDEO_MAX_POINTS = 400

// 경로를 거의 같은 거리 간격으로 골라 [maxPoints]개 이하로 줄인다. 처음·마지막 점은 항상 남긴다
fun simplifyRoute(points: List<RoutePoint>, maxPoints: Int = VIDEO_MAX_POINTS): List<RoutePoint> {
    require(maxPoints >= 2) { "maxPoints는 2 이상" }
    if (points.size <= maxPoints) return points

    val seg = DoubleArray(points.size)
    for (i in 1 until points.size) seg[i] = dist(points[i - 1], points[i])
    val total = seg.sum()
    // 한 자리에 서 있기만 한 기록
    if (total <= 0.0) return listOf(points.first(), points.last())

    // 가운데 점은 step마다 하나씩이라 maxPoints - 2개를 넘지 않는다
    val step = total / (maxPoints - 2)
    val kept = ArrayList<RoutePoint>(maxPoints)
    kept += points.first()
    var acc = 0.0
    for (i in 1 until points.size - 1) {
        acc += seg[i]
        if (acc >= step) {
            kept += points[i]
            acc = 0.0
        }
    }
    kept += points.last()
    return kept
}

private fun dist(a: RoutePoint, b: RoutePoint) =
    distanceKm(a.latitude, a.longitude, b.latitude, b.longitude)
