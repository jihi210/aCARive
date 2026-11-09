package com.example.acarive.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density

/** Pleos 세로 앱 창의 폭. 토스트 최대 폭(792)과 좌우 여백(24)에서 역산한 값이다. */
private const val CAR_WINDOW_WIDTH_DP = 840f
private const val PHONE_MAX_WIDTH_DP = 600

private val LocalAppColors = staticCompositionLocalOf { DayColors }

object AppTheme {
    val colors: AppColors
        @Composable get() = LocalAppColors.current
}

/**
 * 시스템 다크 모드를 차량의 야간 모드로 사용한다.
 *
 * 모든 치수는 차량 디스플레이 기준이므로, 폰에서 개발·시연할 때는 840dp 창을 축소해서 보여준다.
 */
@Composable
fun ACARiveTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    val colors = if (darkTheme) NightColors else DayColors
    val materialColors = if (darkTheme) {
        darkColorScheme(primary = colors.primary, background = colors.background, surface = colors.surface)
    } else {
        lightColorScheme(primary = colors.primary, background = colors.background, surface = colors.surface)
    }

    val configuration = LocalConfiguration.current
    val density = LocalDensity.current
    val appDensity = if (configuration.smallestScreenWidthDp < PHONE_MAX_WIDTH_DP) {
        Density(
            density = density.density * configuration.screenWidthDp / CAR_WINDOW_WIDTH_DP,
            fontScale = density.fontScale,
        )
    } else {
        density
    }

    CompositionLocalProvider(LocalAppColors provides colors, LocalDensity provides appDensity) {
        MaterialTheme(colorScheme = materialColors, typography = Typography, content = content)
    }
}
