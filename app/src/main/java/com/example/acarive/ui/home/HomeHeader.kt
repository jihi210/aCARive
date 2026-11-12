package com.example.acarive.ui.home

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.unit.dp
import com.example.acarive.data.model.VehicleInfo
import com.example.acarive.ui.components.AppIcon
import com.example.acarive.ui.components.AppIconImage
import com.example.acarive.ui.components.CardShape
import com.example.acarive.ui.components.IconActionButton
import com.example.acarive.ui.components.PagePadding
import com.example.acarive.ui.components.tapClickable
import com.example.acarive.ui.theme.AppTheme
import com.example.acarive.ui.theme.AppType
import com.example.acarive.ui.theme.bold

@Composable
internal fun HomeTopBar(onOpenNotifications: () -> Unit) {
    val colors = AppTheme.colors
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = PagePadding),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            "어디로 갈까요?",
            color = colors.text,
            style = AppType.TitleMedium,
            modifier = Modifier.weight(1f),
        )
        Spacer(Modifier.width(16.dp))
        IconActionButton(
            icon = AppIcon.Bell,
            contentDescription = "알림",
            background = colors.surface,
            tint = colors.text,
            // TODO(backend): 알림 목록 API + 알림 화면 (시안에 화면 없음). 미확인 알림이 생기면 빨간 점 표시
            onClick = onOpenNotifications,
        )
    }
}

@Composable
internal fun DriveStatusRow(isDriveMode: Boolean, onToggle: () -> Unit) {
    val colors = AppTheme.colors
    Row(
        modifier = Modifier
            .padding(horizontal = PagePadding)
            .fillMaxWidth()
            .clip(CardShape)
            .background(colors.surface)
            .padding(horizontal = 32.dp, vertical = 24.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(
                if (isDriveMode) "드라이브 모드" else "일반 주행 모드",
                color = colors.text,
                style = AppType.BodyMedium.bold,
            )
            Text(
                if (isDriveMode) "풍경과 음악을 기록해요" else "기록하지 않아요",
                color = colors.textSecondary,
                style = AppType.LabelMedium,
            )
        }
        Spacer(Modifier.width(16.dp))
        DriveModeSwitch(checked = isDriveMode, onToggle = onToggle)
    }
}

/** 드라이브 목적 주행을 직접 구분하는 슬라이드 버튼 (터치 영역 112x64) */
@Composable
private fun DriveModeSwitch(checked: Boolean, onToggle: () -> Unit) {
    val colors = AppTheme.colors
    val thumbOffset by animateDpAsState(if (checked) 48.dp else 0.dp, label = "thumbOffset")
    val trackColor by animateColorAsState(if (checked) colors.primary else colors.surfaceVariant, label = "trackColor")
    val thumbColor by animateColorAsState(if (checked) colors.onPrimary else colors.text, label = "thumbColor")
    Box(
        modifier = Modifier
            .size(width = 112.dp, height = 64.dp)
            .clip(RoundedCornerShape(32.dp))
            .background(trackColor)
            .tapClickable(role = Role.Switch, onClick = onToggle)
            .semantics {
                contentDescription = "드라이브 모드"
                stateDescription = if (checked) "켜짐" else "꺼짐"
            }
            .padding(8.dp),
    ) {
        Box(
            Modifier
                .offset(x = thumbOffset)
                .size(48.dp)
                .background(thumbColor, CircleShape),
        )
    }
}

@Composable
internal fun VehicleStrip(vehicle: VehicleInfo?, onClick: () -> Unit) {
    val colors = AppTheme.colors
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
        AppIconImage(
            icon = if (vehicle != null) AppIcon.Check else AppIcon.Car,
            tint = colors.text,
        )
        Spacer(Modifier.width(24.dp))
        Text(
            if (vehicle != null) "${vehicle.name} 연결됨" else "내 차량 연결하기",
            color = colors.text,
            style = AppType.BodySmall.bold,
            modifier = Modifier.weight(1f),
        )
        AppIconImage(AppIcon.Chevron, colors.textSecondary, size = 40.dp)
    }
}
