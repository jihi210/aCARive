package com.example.driveapp.car

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF

// 위에서 내려다본 차를 코드로 그리는 도구
// 영상(RouteOverlay)과 차 선택 화면 미리보기에서 같은 그림을 씀
object TopDownCar {

    private val shadow = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.argb(70, 0, 0, 0) }
    private val body = Paint(Paint.ANTI_ALIAS_FLAG)
    private val outline = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        color = Color.argb(200, 30, 34, 40)
    }
    private val glass = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.parseColor("#1F2A36") }
    private val roof = Paint(Paint.ANTI_ALIAS_FLAG)
    private val rack = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        color = Color.parseColor("#2A2A2A")
        strokeCap = Paint.Cap.ROUND
    }
    private val headLight = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.parseColor("#FFF6C8") }
    private val tailLight = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.parseColor("#E5342B") }
    private val mirror = Paint(Paint.ANTI_ALIAS_FLAG)

    /**
     * (cx, cy)를 중심으로 차를 그림.
     * @param lengthPx 제네시스 G80(lengthRatio 1.0)일 때의 차 길이(px)
     * @param headingDeg 차 앞이 향하는 방향. 0 = 화면 위쪽, 시계 방향으로 증가
     */
    fun draw(canvas: Canvas, car: CarModel, cx: Float, cy: Float, lengthPx: Float, headingDeg: Float) {
        val l = lengthPx * car.lengthRatio
        val w = lengthPx * car.widthRatio
        val r = w * car.cornerRatio

        canvas.save()
        canvas.translate(cx, cy)
        canvas.rotate(headingDeg)

        // 차 모양은 '앞이 위(-y)'를 기준으로 그림
        val top = -l / 2
        val left = -w / 2

        // 그림자
        canvas.drawRoundRect(RectF(left + w * 0.06f, top + l * 0.04f, -left + w * 0.10f, -top + l * 0.05f), r, r, shadow)

        // 사이드 미러
        mirror.color = car.bodyColor
        val mirrorY = top + l * 0.33f
        canvas.drawRoundRect(RectF(left - w * 0.13f, mirrorY, left + w * 0.02f, mirrorY + l * 0.06f), w * 0.05f, w * 0.05f, mirror)
        canvas.drawRoundRect(RectF(-left - w * 0.02f, mirrorY, -left + w * 0.13f, mirrorY + l * 0.06f), w * 0.05f, w * 0.05f, mirror)

        // 차체
        body.color = car.bodyColor
        val bodyRect = RectF(left, top, -left, -top)
        canvas.drawRoundRect(bodyRect, r, r, body)

        // 앞 유리 · 지붕 · 뒷 유리
        val inset = w * 0.12f
        val windTop = top + l * 0.28f
        val roofTop = top + l * 0.40f
        val roofBottom = top + l * 0.78f
        val rearBottom = top + l * 0.88f
        canvas.drawRoundRect(RectF(left + inset, windTop, -left - inset, roofTop + w * 0.08f), w * 0.10f, w * 0.10f, glass)
        canvas.drawRoundRect(RectF(left + inset, roofBottom - w * 0.06f, -left - inset, rearBottom), w * 0.10f, w * 0.10f, glass)
        roof.color = car.roofColor
        canvas.drawRoundRect(RectF(left + inset, roofTop, -left - inset, roofBottom), w * 0.08f, w * 0.08f, roof)

        // SUV 지붕 가로대
        if (car.roofRack) {
            rack.strokeWidth = w * 0.06f
            val rx = w * 0.30f
            canvas.drawLine(-rx, roofTop + w * 0.05f, -rx, roofBottom - w * 0.05f, rack)
            canvas.drawLine(rx, roofTop + w * 0.05f, rx, roofBottom - w * 0.05f, rack)
        }

        // 헤드램프(앞) · 리어램프(뒤)
        // 둥근 모서리 밖으로 삐져나오지 않게 안쪽에 배치
        val lampW = w * 0.22f
        val lampH = l * 0.03f
        val lampX = w * 0.16f
        val headY = top + l * 0.03f
        val tailY = -top - l * 0.03f - lampH
        canvas.drawRoundRect(RectF(left + lampX, headY, left + lampX + lampW, headY + lampH), lampH, lampH, headLight)
        canvas.drawRoundRect(RectF(-left - lampX - lampW, headY, -left - lampX, headY + lampH), lampH, lampH, headLight)
        canvas.drawRoundRect(RectF(left + lampX, tailY, left + lampX + lampW, tailY + lampH), lampH, lampH, tailLight)
        canvas.drawRoundRect(RectF(-left - lampX - lampW, tailY, -left - lampX, tailY + lampH), lampH, lampH, tailLight)

        // 테두리 (흰 차가 밝은 지도에서도 보이게)
        outline.strokeWidth = (w * 0.04f).coerceAtLeast(1.5f)
        canvas.drawRoundRect(bodyRect, r, r, outline)

        canvas.restore()
    }
}
