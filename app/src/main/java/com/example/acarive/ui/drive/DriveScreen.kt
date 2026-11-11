package com.example.acarive.ui.drive

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.acarive.ui.components.AppIcon
import com.example.acarive.ui.components.AppIconImage
import com.example.acarive.ui.components.CardShape
import com.example.acarive.ui.components.ContentMaxWidth
import com.example.acarive.ui.components.tapClickable
import com.example.acarive.ui.theme.ACARiveTheme
import com.example.acarive.ui.theme.AppTheme
import com.example.acarive.ui.theme.AppType
import com.example.acarive.ui.theme.bold

/**
 * 코스 출발 후 표시하는 주행 전용 화면.
 *
 * 지도 SDK가 실제 경로를 전달하기 전에도 레이아웃을 검증할 수 있도록 [DriveMap]
 * 은 경로 형태의 플레이스홀더를 그린다. 실제 지도 연결 시 이 컴포넌트만 교체한다.
 */
@Composable
fun DriveScreen(
    routeName: String,
    remainingDistance: String,
    remainingTime: String,
    speedKph: Int,
    onEndDrive: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(AppTheme.colors.background),
    ) {
        DriveMap(
            routeName = routeName,
            modifier = Modifier.fillMaxSize(),
        )

        GuidanceBanner(
            distanceToTurn = "214 m",
            instruction = "우회전",
            nextDistance = "90 m",
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(horizontal = 32.dp, vertical = 24.dp)
                .widthIn(max = ContentMaxWidth)
                .fillMaxWidth(),
        )

        DriveBottomPanel(
            speedKph = speedKph,
            remainingDistance = remainingDistance,
            remainingTime = remainingTime,
            onEndDrive = onEndDrive,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .widthIn(max = ContentMaxWidth)
                .fillMaxWidth(),
        )
    }
}

@Composable
private fun GuidanceBanner(
    distanceToTurn: String,
    instruction: String,
    nextDistance: String,
    modifier: Modifier = Modifier,
) {
    val colors = AppTheme.colors
    Row(
        modifier = modifier
            .clip(CardShape)
            .background(colors.emphasis)
            .padding(horizontal = 32.dp, vertical = 24.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(24.dp),
    ) {
        Box(
            modifier = Modifier
                .heightIn(min = 80.dp)
                .clip(CardShape)
                .background(colors.onEmphasisAccent)
                .padding(horizontal = 20.dp),
            contentAlignment = Alignment.Center,
        ) {
            AppIconImage(
                icon = AppIcon.Chevron,
                tint = colors.onEmphasis,
                size = 48.dp,
                contentDescription = "오른쪽 방향",
            )
        }
        Column(Modifier.weight(1f)) {
            Text(
                text = "$distanceToTurn 앞 $instruction",
                color = colors.onEmphasis,
                style = AppType.TitleMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = "이후 $nextDistance 직진",
                color = colors.onEmphasisSecondary,
                style = AppType.BodySmall,
                maxLines = 1,
            )
        }
    }
}

@Composable
private fun DriveMap(
    routeName: String,
    modifier: Modifier = Modifier,
) {
    val colors = AppTheme.colors
    Box(
        modifier = modifier
            .background(colors.surfaceVariant)
            .semantics { contentDescription = "$routeName 주행 경로 지도" },
    ) {
        Canvas(Modifier.fillMaxSize()) {
            val roadStroke = size.minDimension * 0.075f
            val routeStroke = size.minDimension * 0.035f
            val route = Path().apply {
                moveTo(size.width * 0.63f, size.height * 0.02f)
                cubicTo(
                    size.width * 0.84f,
                    size.height * 0.18f,
                    size.width * 0.38f,
                    size.height * 0.29f,
                    size.width * 0.56f,
                    size.height * 0.43f,
                )
                cubicTo(
                    size.width * 0.76f,
                    size.height * 0.58f,
                    size.width * 0.23f,
                    size.height * 0.67f,
                    size.width * 0.46f,
                    size.height * 0.88f,
                )
            }

            drawPath(
                path = route,
                color = colors.surface,
                style = Stroke(width = roadStroke, cap = StrokeCap.Round),
            )
            drawPath(
                path = route,
                color = colors.emphasis,
                style = Stroke(width = routeStroke, cap = StrokeCap.Round),
            )

            drawLine(
                color = colors.line,
                start = Offset(size.width * 0.04f, size.height * 0.28f),
                end = Offset(size.width * 0.42f, size.height * 0.18f),
                strokeWidth = routeStroke,
                cap = StrokeCap.Round,
            )
            drawLine(
                color = colors.line,
                start = Offset(size.width * 0.66f, size.height * 0.69f),
                end = Offset(size.width * 0.96f, size.height * 0.58f),
                strokeWidth = routeStroke,
                cap = StrokeCap.Round,
            )
        }

        Box(
            modifier = Modifier
                .align(Alignment.Center)
                .clip(CardShape)
                .background(colors.primary)
                .padding(horizontal = 20.dp, vertical = 12.dp),
        ) {
            Text("현재 위치", color = colors.onPrimary, style = AppType.LabelSmall.bold)
        }
    }
}

@Composable
private fun DriveBottomPanel(
    speedKph: Int,
    remainingDistance: String,
    remainingTime: String,
    onEndDrive: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = AppTheme.colors
    Column(
        modifier = modifier
            .navigationBarsPadding()
            .padding(24.dp)
            .clip(CardShape)
            .background(colors.surface)
            .padding(24.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            DriveMetric(
                label = "현재 속도",
                value = speedKph.toString(),
                unit = "km/h",
                modifier = Modifier.weight(1f),
            )
            DriveMetric(
                label = "잔여 거리",
                value = remainingDistance,
                modifier = Modifier.weight(1f),
            )
            DriveMetric(
                label = "남은 시간",
                value = remainingTime,
                modifier = Modifier.weight(1f),
            )
        }
        Spacer(Modifier.height(20.dp))
        EndDriveButton(onClick = onEndDrive)
    }
}

@Composable
private fun DriveMetric(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
    unit: String? = null,
) {
    val colors = AppTheme.colors
    Column(
        modifier = modifier
            .heightIn(min = 112.dp)
            .clip(CardShape)
            .background(colors.surfaceVariant)
            .padding(horizontal = 12.dp, vertical = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(
            text = label,
            color = colors.textSecondary,
            style = AppType.LabelSmall,
            textAlign = TextAlign.Center,
        )
        Row(
            modifier = Modifier.wrapContentWidth(),
            verticalAlignment = Alignment.Bottom,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Text(
                text = value,
                color = colors.text,
                style = AppType.TitleSmall,
                textAlign = TextAlign.Center,
                maxLines = 1,
            )
            if (unit != null) {
                Text(text = unit, color = colors.textSecondary, style = AppType.LabelSmall)
            }
        }
    }
}

@Composable
private fun EndDriveButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = AppTheme.colors
    Row(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 100.dp)
            .clip(CardShape)
            .background(colors.primary)
            .tapClickable(role = Role.Button, onClick = onClick),
        horizontalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterHorizontally),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        AppIconImage(
            icon = AppIcon.Close,
            tint = colors.onPrimary,
            contentDescription = null,
        )
        Text("주행 종료", color = colors.onPrimary, style = AppType.BodyMedium.bold)
    }
}

@Preview(showBackground = true, widthDp = 840, heightDp = 1200)
@Composable
private fun DriveScreenPreview() {
    ACARiveTheme {
        DriveScreen(
            routeName = "북한강 물빛 드라이브",
            remainingDistance = "86 km",
            remainingTime = "2시간 20분",
            speedKph = 0,
            onEndDrive = {},
        )
    }
}
