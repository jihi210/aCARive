package com.example.acarive.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.example.acarive.data.model.DriveSummary
import com.example.acarive.ui.components.AppIcon
import com.example.acarive.ui.components.AppIconImage
import com.example.acarive.ui.components.CardShape
import com.example.acarive.ui.components.PagePadding
import com.example.acarive.ui.components.tapClickable
import com.example.acarive.ui.theme.AppTheme
import com.example.acarive.ui.theme.AppType
import com.example.acarive.ui.theme.Black
import com.example.acarive.ui.theme.Copper
import com.example.acarive.ui.theme.bold
import java.util.Locale

private const val EARTH_CIRCUMFERENCE_KM = 40_075.0

@Composable
internal fun DriveStoryBanner(summary: DriveSummary, onClick: () -> Unit) {
    val colors = AppTheme.colors
    val laps = summary.totalKm / EARTH_CIRCUMFERENCE_KM
    val distance = String.format(Locale.US, "%,.1f", summary.totalKm)
    // 0.1바퀴(약 4,000km)가 안 되면 바퀴 수가 0.0으로 보이므로 거리로 표시한다.
    val headline = if (laps >= 0.1) {
        "올해 지구를 ${String.format(Locale.US, "%.1f", laps)}바퀴 달렸어요"
    } else {
        "올해 $distance km 달렸어요"
    }
    Row(
        modifier = Modifier
            .padding(horizontal = PagePadding)
            .fillMaxWidth()
            .clip(CardShape)
            .background(colors.surface)
            .tapClickable(role = Role.Button, onClick = onClick)
            .padding(horizontal = 32.dp, vertical = 24.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier
                .size(80.dp)
                .background(Copper, CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            AppIconImage(AppIcon.Car, Black)
        }
        Spacer(Modifier.width(24.dp))
        Column(Modifier.weight(1f)) {
            Text(headline, color = colors.text, style = AppType.BodySmall.bold)
            Text(
                "$distance km · ${summary.driveCount}번의 드라이브",
                color = colors.textSecondary,
                style = AppType.LabelMedium,
            )
        }
        AppIconImage(AppIcon.Chevron, colors.textSecondary, size = 40.dp)
    }
}
