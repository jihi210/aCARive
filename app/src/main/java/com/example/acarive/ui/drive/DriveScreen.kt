package com.example.acarive.ui.drive

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.acarive.data.course.DriveProgress
import com.example.acarive.data.course.KeyStop
import com.example.acarive.data.course.formatMinutes
import com.example.acarive.data.model.NowPlaying
import com.example.acarive.location.LocationPoint
import com.example.acarive.navi.NaviStatus
import com.example.acarive.ui.components.AppIcon
import com.example.acarive.ui.components.AppIconImage
import com.example.acarive.ui.components.CardShape
import com.example.acarive.ui.components.ContentMaxWidth
import com.example.acarive.ui.components.PagePadding
import com.example.acarive.ui.components.tapClickable
import com.example.acarive.ui.home.PlaylistSection
import com.example.acarive.ui.theme.ACARiveTheme
import com.example.acarive.ui.theme.AppTheme
import com.example.acarive.ui.theme.AppType
import com.example.acarive.ui.theme.bold
import java.util.Locale
import kotlin.math.max

/**
 * 코스 출발 후 표시하는 주행 화면. 길안내는 차량 내비가 하고(Send to Car),
 * 여기서는 코스 진행·속도·음악·주행 중 추천만 보여준다.
 */
@Composable
fun DriveScreen(
    routeName: String,
    progress: DriveProgress?,
    naviStatus: NaviStatus,
    speedKph: Int?,
    nowPlaying: NowPlaying,
    isPlaying: Boolean,
    onTogglePlaying: () -> Unit,
    onOpenNavigation: () -> Unit,
    onEndDrive: () -> Unit,
    modifier: Modifier = Modifier,
    // 주행 중 추천 카드. 코스 배너 바로 아래에 띄운다
    suggestion: (@Composable () -> Unit)? = null,
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(AppTheme.colors.background),
        contentAlignment = Alignment.TopCenter,
    ) {
        // 음악 카드는 자체 좌우 여백(PagePadding)이 있어서 나머지 항목에만 여백을 준다
        val itemPadding = Modifier.padding(horizontal = PagePadding)
        Column(
            modifier = Modifier
                .widthIn(max = ContentMaxWidth)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .navigationBarsPadding()
                .padding(vertical = 24.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp),
        ) {
            CourseBanner(routeName, progress, naviStatus, itemPadding.fillMaxWidth())
            suggestion?.let { Box(itemPadding) { it() } }
            DriveMetrics(speedKph, progress, itemPadding.fillMaxWidth())
            if (progress != null && progress.stops.isNotEmpty()) {
                StopList(progress, itemPadding.fillMaxWidth())
            }
            PlaylistSection(nowPlaying = nowPlaying, isPlaying = isPlaying, onTogglePlaying = onTogglePlaying)
            Row(itemPadding.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                DriveButton(
                    text = "내비 보기",
                    icon = AppIcon.Route,
                    primary = false,
                    onClick = onOpenNavigation,
                    modifier = Modifier.weight(1f),
                )
                DriveButton(
                    text = "주행 종료",
                    icon = AppIcon.Close,
                    primary = true,
                    onClick = onEndDrive,
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}

// 코스 이름 + 차량 내비 상태 + 다음 경유지
@Composable
private fun CourseBanner(
    routeName: String,
    progress: DriveProgress?,
    naviStatus: NaviStatus,
    modifier: Modifier = Modifier,
) {
    val colors = AppTheme.colors
    Column(
        modifier = modifier
            .clip(CardShape)
            .background(colors.emphasis)
            .padding(horizontal = 32.dp, vertical = 24.dp),
    ) {
        Text(
            text = naviStatus.label,
            color = colors.onEmphasisAccent,
            style = AppType.LabelMedium.bold,
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = routeName,
            color = colors.onEmphasis,
            style = AppType.TitleMedium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Spacer(Modifier.height(4.dp))
        Text(
            text = when {
                progress == null -> "코스 정보를 불러오지 못했어요"
                progress.isFinished -> "코스를 다 달렸어요"
                else -> "다음 경유지 · ${progress.nextStop?.name}"
            },
            color = colors.onEmphasisSecondary,
            style = AppType.BodySmall,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

private val NaviStatus.label: String
    get() = when (this) {
        NaviStatus.Idle, NaviStatus.Sending -> "차량 내비로 보내는 중"
        NaviStatus.Sent -> "차량 내비로 안내 중"
        NaviStatus.Unavailable -> "차량 내비 없음 · 코스 진행만 보여드려요"
        NaviStatus.Failed -> "차량 내비로 보내지 못했어요"
        NaviStatus.RouteFailed -> "차량 내비가 경로를 찾지 못했어요"
    }

@Composable
private fun DriveMetrics(speedKph: Int?, progress: DriveProgress?, modifier: Modifier = Modifier) {
    Row(modifier, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        DriveMetric(
            label = "현재 속도",
            value = speedKph?.toString() ?: "-",
            unit = "km/h",
            modifier = Modifier.weight(1f),
        )
        DriveMetric(
            label = "남은 거리",
            value = progress?.let { formatKm(it.remainingKm) } ?: "-",
            modifier = Modifier.weight(1f),
        )
        DriveMetric(
            label = "남은 시간",
            value = when {
                progress == null -> "-"
                progress.isFinished -> "도착"
                else -> formatMinutes(max(1, progress.remainingMinutes))
            },
            modifier = Modifier.weight(1f),
        )
    }
}

// 직선거리로 추정한 값이라 10km 넘으면 소수점은 뺀다
private fun formatKm(km: Double): String =
    if (km < 10) String.format(Locale.KOREA, "%.1f km", km) else String.format(Locale.KOREA, "%.0f km", km)

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
            .background(colors.surface)
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

// 차량 내비로 보낸 경유지. 지난 곳은 체크, 다음 곳은 강조
@Composable
private fun StopList(progress: DriveProgress, modifier: Modifier = Modifier) {
    val colors = AppTheme.colors
    Column(
        modifier = modifier
            .clip(CardShape)
            .background(colors.surface)
            .padding(horizontal = 32.dp, vertical = 16.dp),
    ) {
        progress.stops.forEachIndexed { i, stop ->
            val passed = i < progress.nextIndex
            val next = i == progress.nextIndex
            Row(
                modifier = Modifier.fillMaxWidth().heightIn(min = 72.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(if (passed) colors.primary else Color.Transparent)
                        .border(3.dp, if (passed || next) colors.primary else colors.line, CircleShape),
                    contentAlignment = Alignment.Center,
                ) {
                    if (passed) AppIconImage(AppIcon.Check, colors.onPrimary, size = 28.dp, strokeWidth = 3f)
                }
                Spacer(Modifier.width(24.dp))
                Text(
                    text = stop.name,
                    color = if (passed) colors.textSecondary else colors.text,
                    style = if (next) AppType.BodySmall.bold else AppType.BodySmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f),
                )
                if (i == progress.stops.lastIndex) {
                    Text("도착", color = colors.textSecondary, style = AppType.LabelMedium)
                }
            }
        }
    }
}

@Composable
private fun DriveButton(
    text: String,
    icon: AppIcon,
    primary: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = AppTheme.colors
    val content = if (primary) colors.onPrimary else colors.text
    Row(
        modifier = modifier
            .heightIn(min = 100.dp)
            .clip(CardShape)
            .background(if (primary) colors.primary else colors.surface)
            .tapClickable(role = Role.Button, onClick = onClick),
        horizontalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterHorizontally),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        AppIconImage(icon = icon, tint = content, contentDescription = null)
        Text(text, color = content, style = AppType.BodyMedium.bold)
    }
}

@Preview(showBackground = true, widthDp = 840, heightDp = 1400)
@Composable
private fun DriveScreenPreview() {
    val stops = listOf(
        KeyStop("두물머리", LocationPoint(37.53, 127.31)),
        KeyStop("세미원", LocationPoint(37.54, 127.32)),
        KeyStop("청평호반", LocationPoint(37.73, 127.42)),
    )
    ACARiveTheme {
        DriveScreen(
            routeName = "북한강 물빛 드라이브",
            progress = DriveProgress(stops, nextIndex = 1, remainingKm = 32.4, remainingMinutes = 40),
            naviStatus = NaviStatus.Sent,
            speedKph = 62,
            nowPlaying = NowPlaying("드라이브 추천", "곡 제목", "아티스트 · 비 오는 날 차분한 곡", listOf("# 비", "# 국도 주행")),
            isPlaying = true,
            onTogglePlaying = {},
            onOpenNavigation = {},
            onEndDrive = {},
        )
    }
}
