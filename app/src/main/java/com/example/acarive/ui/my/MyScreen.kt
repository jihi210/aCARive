package com.example.acarive.ui.my

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.acarive.data.SampleData
import com.example.acarive.data.model.UserProfile
import com.example.acarive.data.model.VehicleInfo
import com.example.acarive.ui.components.AppIcon
import com.example.acarive.ui.components.AppIconImage
import com.example.acarive.ui.components.CardShape
import com.example.acarive.ui.components.HorizontalLine
import com.example.acarive.ui.components.PageHeader
import com.example.acarive.ui.components.SubPage
import com.example.acarive.ui.components.TagChip
import com.example.acarive.ui.components.tapClickable
import com.example.acarive.ui.theme.ACARiveTheme
import com.example.acarive.ui.theme.AppTheme
import com.example.acarive.ui.theme.AppType
import com.example.acarive.ui.theme.bold

@Composable
fun MyScreen(
    profile: UserProfile,
    vehicle: VehicleInfo?,
    isDrivingRestricted: Boolean,
    onOpenVehicle: () -> Unit,
    onToggleRecommendWhileDriving: () -> Unit,
    onRestricted: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = AppTheme.colors
    // 설정 작업은 주행 중에 막는다 (NO_SETUP). 막힌 기능을 누르면 안내만 띄운다.
    val whenParked: (() -> Unit) -> () -> Unit = { action ->
        { if (isDrivingRestricted) onRestricted() else action() }
    }

    SubPage(modifier) {
        PageHeader(title = "내 드라이브")
        VehicleCard(vehicle, onOpenVehicle)
        Spacer(Modifier.height(32.dp))
        Text("선호하는 드라이브", color = colors.text, style = AppType.TitleSmall)
        Spacer(Modifier.height(16.dp))
        PreferenceCard(
            preferences = profile.preferences,
            // TODO(backend): 취향(명시적 프로필) 설문 화면 + 저장 API (시안에 화면 없음)
            onReset = whenParked {},
        )
        Spacer(Modifier.height(32.dp))
        Column(Modifier.clip(CardShape).background(colors.surface)) {
            SettingRow(
                icon = AppIcon.Music,
                label = "Spotify 연결",
                value = if (profile.spotifyConnected) "연결됨" else "연결 안 됨",
                // TODO(backend): Spotify OAuth 연결/해제. 토큰은 Keystore 에 보관
                onClick = whenParked {},
            )
            HorizontalLine()
            SettingRow(
                icon = AppIcon.Bell,
                label = "주행 중 코스 추천",
                value = if (profile.recommendWhileDriving) "켜짐" else "꺼짐",
                onClick = whenParked(onToggleRecommendWhileDriving),
            )
            HorizontalLine()
            SettingRow(
                icon = AppIcon.Share,
                label = "기록 공유 설정",
                value = null,
                // TODO(backend): 기록 공유 설정 조회/저장 (시안에 화면 없음)
                onClick = whenParked {},
            )
        }
    }
}

@Composable
private fun VehicleCard(vehicle: VehicleInfo?, onClick: () -> Unit) {
    val colors = AppTheme.colors
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(CardShape)
            .background(colors.emphasis)
            .tapClickable(role = Role.Button, onClick = onClick)
            .padding(horizontal = 32.dp, vertical = 24.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        AppIconImage(if (vehicle != null) AppIcon.Check else AppIcon.Car, colors.onEmphasisAccent, size = 64.dp)
        Spacer(Modifier.width(24.dp))
        Column(Modifier.weight(1f)) {
            Text(
                if (vehicle != null) "${vehicle.name} 연결됨" else "내 차량 연결하기",
                color = colors.onEmphasis,
                style = AppType.BodyMedium.bold,
            )
            Text(
                if (vehicle != null) "실시간 데이터 수신 중" else "더 정확한 추천을 받아보세요",
                color = colors.onEmphasisSecondary,
                style = AppType.LabelMedium,
            )
        }
        AppIconImage(AppIcon.Chevron, colors.onEmphasisSecondary, size = 40.dp)
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun PreferenceCard(preferences: List<String>, onReset: () -> Unit) {
    val colors = AppTheme.colors
    Column(
        Modifier
            .fillMaxWidth()
            .clip(CardShape)
            .background(colors.surface)
            .padding(24.dp),
    ) {
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            preferences.forEach { TagChip(it) }
        }
        if (preferences.isEmpty()) {
            Text("아직 고른 취향이 없어요", color = colors.textSecondary, style = AppType.BodySmall)
        }
        Spacer(Modifier.height(8.dp))
        Box(
            Modifier
                .clip(CardShape)
                .tapClickable(role = Role.Button, onClick = onReset)
                .heightIn(min = 80.dp),
            contentAlignment = Alignment.CenterStart,
        ) {
            Text("취향 다시 설정하기", color = colors.text, style = AppType.BodySmall)
        }
    }
}

@Composable
private fun SettingRow(
    icon: AppIcon,
    label: String,
    value: String?,
    onClick: () -> Unit,
) {
    val colors = AppTheme.colors
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 104.dp)
            .tapClickable(role = Role.Button, onClick = onClick)
            .padding(horizontal = 32.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        AppIconImage(icon, colors.text)
        Spacer(Modifier.width(24.dp))
        Text(label, color = colors.text, style = AppType.BodySmall, modifier = Modifier.weight(1f))
        if (value != null) {
            Text(value, color = colors.textSecondary, style = AppType.LabelMedium)
        } else {
            AppIconImage(AppIcon.Chevron, colors.textSecondary, size = 40.dp)
        }
    }
}

@Preview(showBackground = true, widthDp = 840, heightDp = 1400)
@Composable
private fun MyScreenPreview() {
    ACARiveTheme {
        MyScreen(SampleData.profile, vehicle = null, isDrivingRestricted = false, onOpenVehicle = {}, onToggleRecommendWhileDriving = {}, onRestricted = {})
    }
}
