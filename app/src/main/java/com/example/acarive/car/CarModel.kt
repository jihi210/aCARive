package com.example.acarive.car

import androidx.annotation.DrawableRes
import com.example.acarive.R

// 영상에 등장시킬 수 있는 차 5종
// - photoRes: 차 선택 화면에 보여 줄 사진
// - bodyColor/roofColor: 영상 속 탑뷰 차 색
// - lengthRatio/widthRatio: 탑뷰 차 크기 비율 (제네시스 G80 길이를 1.0으로 기준)
// - roofRack: SUV 계열은 지붕 가로대 표시
// - cornerRatio: 모서리 둥글기 (작을수록 박스형)
enum class CarModel(
    val id: String,
    val displayName: String,
    val category: String,
    @DrawableRes val photoRes: Int,
    val bodyColor: Int,
    val roofColor: Int,
    val lengthRatio: Float,
    val widthRatio: Float,
    val roofRack: Boolean,
    val cornerRatio: Float
) {
    GENESIS_G80(
        id = "genesis_g80",
        displayName = "제네시스 G80",
        category = "대형 세단",
        photoRes = R.drawable.car_genesis_g80,
        bodyColor = 0xFF8C8478.toInt(),   // 무광 토프 그레이
        roofColor = 0xFF6E675D.toInt(),
        lengthRatio = 1.00f,
        widthRatio = 0.38f,
        roofRack = false,
        cornerRatio = 0.30f
    ),
    AVANTE(
        id = "avante",
        displayName = "더 뉴 아반떼",
        category = "준중형 세단",
        photoRes = R.drawable.car_avante,
        bodyColor = 0xFF8E97A3.toInt(),   // 실버 블루 그레이
        roofColor = 0xFF6F7884.toInt(),
        lengthRatio = 0.92f,
        widthRatio = 0.37f,
        roofRack = false,
        cornerRatio = 0.32f
    ),
    SANTA_FE(
        id = "santa_fe",
        displayName = "싼타페",
        category = "중형 SUV",
        photoRes = R.drawable.car_santafe,
        bodyColor = 0xFFF4F4F2.toInt(),   // 화이트
        roofColor = 0xFF2B2F36.toInt(),   // 검은 파노라마 루프
        lengthRatio = 0.95f,
        widthRatio = 0.42f,
        roofRack = true,
        cornerRatio = 0.22f
    ),
    CASPER(
        id = "casper",
        displayName = "캐스퍼",
        category = "경형 SUV",
        photoRes = R.drawable.car_casper,
        bodyColor = 0xFFD9472B.toInt(),   // 레드
        roofColor = 0xFFC23C22.toInt(),
        lengthRatio = 0.72f,
        widthRatio = 0.40f,
        roofRack = true,
        cornerRatio = 0.28f
    ),
    CONCEPT_SUV(
        id = "concept_suv",
        displayName = "현대 콘셉트 SUV",
        category = "박스형 전기 SUV 콘셉트",
        photoRes = R.drawable.car_concept_suv,
        bodyColor = 0xFFC9AE84.toInt(),   // 샴페인 골드
        roofColor = 0xFFB0956B.toInt(),
        lengthRatio = 0.86f,
        widthRatio = 0.44f,
        roofRack = true,
        cornerRatio = 0.12f
    );

    companion object {
        val DEFAULT = AVANTE

        fun fromId(id: String?): CarModel = entries.firstOrNull { it.id == id } ?: DEFAULT
    }
}
