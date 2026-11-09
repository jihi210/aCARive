package com.example.acarive.ui.record

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.acarive.data.model.DriveHistoryItem
import com.example.acarive.data.model.DriveSummary
import com.example.acarive.ui.components.AppIcon
import com.example.acarive.ui.components.AppIconImage
import com.example.acarive.ui.components.CardShape
import com.example.acarive.ui.components.HorizontalLine
import com.example.acarive.ui.components.PageHeader
import com.example.acarive.ui.components.PrimaryButton
import com.example.acarive.ui.components.StatGrid
import com.example.acarive.ui.components.StatItem
import com.example.acarive.ui.components.SubPage
import com.example.acarive.ui.components.tapClickable
import com.example.acarive.ui.theme.ACARiveTheme
import com.example.acarive.ui.theme.AppTheme
import com.example.acarive.ui.theme.AppType
import com.example.acarive.ui.theme.bold
import java.util.Locale

@Composable
fun RecordScreen(
    state: DriveRecordUiState,
    savedRouteCount: Int,
    onStartRecording: () -> Unit,
    onStopRecording: () -> Unit,
    onToggleDrive: (Long) -> Unit,
    onMakeVideo: () -> Unit,
    isDrivingRestricted: Boolean,
    modifier: Modifier = Modifier,
) {
    val colors = AppTheme.colors
    val summary = state.summary
    SubPage(modifier) {
        PageHeader(title = "나의 주행 기록")
        RecordControl(
            isRecording = state.isRecording,
            recordedCount = state.recordedCount,
            onStart = onStartRecording,
            onStop = onStopRecording,
        )
        Spacer(Modifier.height(16.dp))
        RecordHero(summary)
        Spacer(Modifier.height(16.dp))
        StatGrid(
            items = listOf(
                StatItem("드라이브", "${summary.driveCount}회"),
                StatItem("주행 시간", summary.totalDuration),
                StatItem("저장 코스", "${savedRouteCount}개"),
            ),
            modifier = Modifier.clip(CardShape).background(colors.surface),
        )
        Spacer(Modifier.height(32.dp))
        Text("최근 드라이브", color = colors.text, style = AppType.TitleSmall)
        Spacer(Modifier.height(16.dp))
        Column(Modifier.clip(CardShape).background(colors.surface)) {
            if (summary.recentDrives.isEmpty()) {
                Text(
                    "아직 기록이 없어요",
                    color = colors.textSecondary,
                    style = AppType.BodySmall,
                    modifier = Modifier.fillMaxWidth().padding(32.dp),
                )
            }
            summary.recentDrives.forEachIndexed { index, drive ->
                if (index > 0) HorizontalLine()
                val isExpanded = drive.sessionId == state.expandedSessionId
                HistoryRow(drive, isExpanded, onClick = { onToggleDrive(drive.sessionId) })
                if (isExpanded) {
                    DriveRoute(state)
                    DriveVideo(state, isDrivingRestricted, onMakeVideo)
                }
            }
        }
    }
}

@Composable
private fun RecordControl(
    isRecording: Boolean,
    recordedCount: Int,
    onStart: () -> Unit,
    onStop: () -> Unit,
) {
    val colors = AppTheme.colors
    val detail = when {
        !isRecording -> "경로를 기록하려면 위치 정보가 필요해요"
        recordedCount == 0 -> "위치를 받는 중이에요"
        else -> "위치 ${recordedCount}개 기록"
    }
    Column(
        Modifier
            .fillMaxWidth()
            .clip(CardShape)
            .background(colors.surface)
            .padding(32.dp),
    ) {
        Text(if (isRecording) "기록 중" else "드라이브 기록", color = colors.text, style = AppType.BodyMedium.bold)
        Text(detail, color = colors.textSecondary, style = AppType.LabelMedium)
        if (USE_MOCK_LOCATION) {
            Text("모의 위치 사용 중", color = colors.textSecondary, style = AppType.LabelMedium)
        }
        Spacer(Modifier.height(24.dp))
        PrimaryButton(
            text = if (isRecording) "기록 종료" else "기록 시작",
            icon = if (isRecording) AppIcon.Check else AppIcon.Play,
            onClick = if (isRecording) onStop else onStart,
        )
    }
}

@Composable
private fun RecordHero(summary: DriveSummary) {
    val colors = AppTheme.colors
    Column(
        Modifier
            .fillMaxWidth()
            .clip(CardShape)
            .background(colors.emphasis)
            .padding(32.dp),
    ) {
        Text("${summary.year}년", color = colors.onEmphasisAccent, style = AppType.LabelMedium.bold)
        Text(
            text = buildAnnotatedString {
                append(String.format(Locale.US, "%,.1f", summary.totalKm))
                withStyle(SpanStyle(color = colors.onEmphasisSecondary, fontSize = AppType.TitleSmall.fontSize)) {
                    append(" km")
                }
            },
            color = colors.onEmphasis,
            style = AppType.HeadlineMedium,
        )
    }
}

// TODO(backend): 세션에 코스 이름과 날씨를 함께 저장해 이 행에 표시 (지금 DB에는 없는 값)
@Composable
private fun HistoryRow(drive: DriveHistoryItem, isExpanded: Boolean, onClick: () -> Unit) {
    val colors = AppTheme.colors
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .tapClickable(onClick = onClick)
            .padding(horizontal = 32.dp, vertical = 24.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(drive.date, color = colors.textSecondary, style = AppType.LabelMedium)
        Spacer(Modifier.width(24.dp))
        Column(Modifier.weight(1f)) {
            Text(drive.distance, color = colors.text, style = AppType.BodySmall.bold)
            Text(drive.duration, color = colors.textSecondary, style = AppType.LabelMedium)
        }
        AppIconImage(
            AppIcon.Chevron,
            colors.textSecondary,
            size = 40.dp,
            modifier = Modifier.rotate(if (isExpanded) 90f else 0f),
        )
    }
}

@Composable
private fun DriveRoute(state: DriveRecordUiState) {
    if (state.expandedPoints.size >= 2) {
        RouteCanvas(
            points = state.expandedPoints,
            modifier = Modifier.fillMaxWidth().height(280.dp).padding(bottom = 24.dp),
        )
    } else {
        Text(
            "경로를 그리기엔 위치가 부족해요",
            color = AppTheme.colors.textSecondary,
            style = AppType.LabelMedium,
            modifier = Modifier.padding(start = 32.dp, end = 32.dp, bottom = 24.dp),
        )
    }
}

/** 펼친 드라이브의 추억 영상. 주행 중에는 재생하지 않는다 (NO_VIDEO). */
@Composable
private fun DriveVideo(state: DriveRecordUiState, isDrivingRestricted: Boolean, onMakeVideo: () -> Unit) {
    if (state.expandedPoints.size < 2) return
    val colors = AppTheme.colors
    Column(Modifier.fillMaxWidth().padding(start = 32.dp, end = 32.dp, bottom = 32.dp)) {
        when (val video = state.video) {
            is DriveVideoState.Ready -> if (isDrivingRestricted) {
                Text("주행 중에는 영상을 볼 수 없어요", color = colors.textSecondary, style = AppType.LabelMedium)
            } else {
                VideoPlayer(
                    file = video.file,
                    modifier = Modifier.fillMaxWidth().aspectRatio(16f / 9f).clip(CardShape),
                )
            }
            is DriveVideoState.Making ->
                Text(video.message, color = colors.textSecondary, style = AppType.LabelMedium)
            DriveVideoState.Idle, is DriveVideoState.Failed -> {
                if (video is DriveVideoState.Failed) {
                    Text(video.message, color = colors.textSecondary, style = AppType.LabelMedium)
                    Spacer(Modifier.height(16.dp))
                }
                PrimaryButton(text = "추억 영상 만들기", icon = AppIcon.Sparkle, onClick = onMakeVideo)
            }
        }
    }
}

@Preview(showBackground = true, widthDp = 840, heightDp = 1200)
@Composable
private fun RecordScreenPreview() {
    ACARiveTheme {
        RecordScreen(
            state = DriveRecordUiState(
                summary = DriveSummary(
                    year = 2026,
                    totalKm = 210.4,
                    driveCount = 2,
                    totalDuration = "3시간 20분",
                    recentDrives = listOf(DriveHistoryItem(1, "05. 18", "86.0 km", "1시간 30분")),
                ),
            ),
            savedRouteCount = 3,
            onStartRecording = {},
            onStopRecording = {},
            onToggleDrive = {},
            onMakeVideo = {},
            isDrivingRestricted = false,
        )
    }
}
