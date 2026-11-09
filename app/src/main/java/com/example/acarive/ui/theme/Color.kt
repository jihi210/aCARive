package com.example.acarive.ui.theme

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color

// 현대차그룹 CI 팔레트 (CLAUDE.md). 이미지를 제외한 모든 색은 이 값과 그 투명도로만 구성한다.
val HyundaiNavy = Color(0xFF002C5F)
val Sand = Color(0xFFE4DCD3)
val LightSand = Color(0xFFF6F3F2)
val Copper = Color(0xFFA36B4F)
val White = Color(0xFFFFFFFF)
val Black = Color(0xFF000000)
val ActiveBlue = Color(0xFF00AAD2)
val SkyBlue = Color(0xFFAACAE6)
val AlertRed = Color(0xFFE63312)

/**
 * 화면에서 쓰는 시맨틱 색. Pleos Connect 가이드의 주간/야간 대응과 대비 4.5:1 기준을 맞춘 조합이다.
 *
 * 팔레트 중 대비가 부족한 조합은 쓰지 않는다:
 * - [ActiveBlue]: 밝은 배경에서 2.7:1 -> 네이비(5.0:1)·검정 배경 위에서만 사용
 * - [Copper]: 흰 배경에서 4.4:1 -> 검정 글자/아이콘(4.8:1)의 배경으로만 사용
 * - [AlertRed]: 흰 배경에서 4.3:1 -> 글자에 쓰지 않고 알림 점으로만 사용
 */
@Immutable
data class AppColors(
    val background: Color,
    val surface: Color,
    val surfaceVariant: Color,
    val line: Color,
    val text: Color,
    val textSecondary: Color,
    val primary: Color,
    val onPrimary: Color,
    val emphasis: Color,
    val onEmphasis: Color,
    val onEmphasisSecondary: Color,
    val onEmphasisAccent: Color,
)

val DayColors = AppColors(
    background = LightSand,
    surface = White,
    surfaceVariant = Sand,
    line = Sand,
    text = HyundaiNavy,
    textSecondary = HyundaiNavy.copy(alpha = 0.75f),
    primary = HyundaiNavy,
    onPrimary = White,
    emphasis = HyundaiNavy,
    onEmphasis = White,
    onEmphasisSecondary = SkyBlue,
    onEmphasisAccent = ActiveBlue,
)

val NightColors = AppColors(
    background = Black,
    surface = HyundaiNavy,
    surfaceVariant = SkyBlue.copy(alpha = 0.18f),
    line = SkyBlue.copy(alpha = 0.3f),
    text = White,
    textSecondary = SkyBlue,
    primary = White,
    onPrimary = HyundaiNavy,
    emphasis = HyundaiNavy,
    onEmphasis = White,
    onEmphasisSecondary = SkyBlue,
    onEmphasisAccent = ActiveBlue,
)
