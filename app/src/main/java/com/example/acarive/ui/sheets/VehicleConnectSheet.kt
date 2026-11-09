package com.example.acarive.ui.sheets

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
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
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import com.example.acarive.data.model.VehicleBrand
import com.example.acarive.data.model.VehicleInfo
import com.example.acarive.ui.components.AppIcon
import com.example.acarive.ui.components.AppIconImage
import com.example.acarive.ui.components.CardShape
import com.example.acarive.ui.components.HorizontalLine
import com.example.acarive.ui.components.IconActionButton
import com.example.acarive.ui.components.PagePadding
import com.example.acarive.ui.components.PrimaryButton
import com.example.acarive.ui.components.StatGrid
import com.example.acarive.ui.components.StatItem
import com.example.acarive.ui.components.tapClickable
import com.example.acarive.ui.theme.AppTheme
import com.example.acarive.ui.theme.AppType
import com.example.acarive.ui.theme.bold

private data class DataPermission(val title: String, val detail: String)

private val DataPermissions = listOf(
    DataPermission("주행 상태", "속도, 시동, 급가감속"),
    DataPermission("차량 위치", "현재 위치, 이동 경로"),
    DataPermission("차량 상태", "연료·배터리, 주행 가능 거리"),
)

@Composable
fun VehicleConnectSheet(
    vehicle: VehicleInfo?,
    selectedBrand: VehicleBrand,
    isDrivingRestricted: Boolean,
    onSelectBrand: (VehicleBrand) -> Unit,
    onConnect: () -> Unit,
    onDisconnect: () -> Unit,
    onDismiss: () -> Unit,
) {
    val colors = AppTheme.colors
    AppBottomSheet(onDismiss = onDismiss) { close ->
        Column(
            Modifier
                .verticalScroll(rememberScrollState())
                .navigationBarsPadding()
                .padding(PagePadding),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    if (vehicle != null) "연결된 차량" else "내 차량 연결하기",
                    color = colors.text,
                    style = AppType.TitleLarge,
                    modifier = Modifier.weight(1f),
                )
                IconActionButton(
                    icon = AppIcon.Close,
                    contentDescription = "닫기",
                    background = colors.surfaceVariant,
                    tint = colors.text,
                    onClick = close,
                )
            }

            when {
                vehicle != null -> ConnectedVehicleContent(
                    vehicle = vehicle,
                    isDrivingRestricted = isDrivingRestricted,
                    onDisconnect = {
                        onDisconnect()
                        close()
                    },
                )
                // 계정 인증과 동의는 설정 작업이므로 주행 중에 막는다 (NO_SETUP).
                isDrivingRestricted -> ParkingRequiredNotice()
                else -> ConnectFormContent(
                    selectedBrand = selectedBrand,
                    onSelectBrand = onSelectBrand,
                    onConnect = {
                        onConnect()
                        close()
                    },
                )
            }
        }
    }
}

@Composable
private fun ParkingRequiredNotice() {
    Text(
        "차량 연결은 주차 후에 할 수 있어요.",
        color = AppTheme.colors.text,
        style = AppType.BodyMedium,
        modifier = Modifier.padding(vertical = 48.dp),
    )
}

@Composable
private fun ConnectedVehicleContent(
    vehicle: VehicleInfo,
    isDrivingRestricted: Boolean,
    onDisconnect: () -> Unit,
) {
    val colors = AppTheme.colors
    // TODO(backend): 실시간 차량 데이터 구독 (속도, 배터리, 주행 가능 거리, 마지막 동기화 시각)
    Column(Modifier.fillMaxWidth()) {
        Spacer(Modifier.height(24.dp))
        Text(vehicle.name, color = colors.text, style = AppType.HeadlineSmall)
        Text(
            "${vehicle.plateNumber} · 마지막 동기화 ${vehicle.lastSynced}",
            color = colors.textSecondary,
            style = AppType.LabelMedium,
        )
        Spacer(Modifier.height(24.dp))
        StatGrid(
            items = listOf(
                StatItem("주행 속도", "${vehicle.speedKmh} km/h"),
                StatItem("배터리", "${vehicle.batteryPercent}%"),
                StatItem("주행 가능", "${vehicle.rangeKm} km"),
            ),
            modifier = Modifier.border(2.dp, colors.line, CardShape),
        )
        if (!isDrivingRestricted) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 24.dp)
                    .clip(CardShape)
                    .tapClickable(role = Role.Button, onClick = onDisconnect)
                    .heightIn(min = 80.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    "차량 연결 해제",
                    color = colors.textSecondary,
                    style = AppType.BodySmall.copy(textDecoration = TextDecoration.Underline),
                )
            }
        }
    }
}

@Composable
private fun ConnectFormContent(
    selectedBrand: VehicleBrand,
    onSelectBrand: (VehicleBrand) -> Unit,
    onConnect: () -> Unit,
) {
    val colors = AppTheme.colors
    Spacer(Modifier.height(16.dp))
    Text(
        "속도와 주행 상태를 받아 상황에 맞는 음악과 코스를 추천해요.",
        color = colors.textSecondary,
        style = AppType.BodySmall,
    )
    Spacer(Modifier.height(32.dp))
    Text("차량 브랜드", color = colors.text, style = AppType.BodyMedium.bold)
    Spacer(Modifier.height(16.dp))
    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        VehicleBrand.entries.forEach { brand ->
            BrandOption(
                brand = brand,
                selected = brand == selectedBrand,
                onClick = { onSelectBrand(brand) },
                modifier = Modifier.weight(1f),
            )
        }
    }
    Spacer(Modifier.height(32.dp))
    Text("데이터 이용 동의", color = colors.text, style = AppType.BodyMedium.bold)
    Spacer(Modifier.height(8.dp))
    DataPermissions.forEachIndexed { index, permission ->
        if (index > 0) HorizontalLine()
        PermissionRow(permission)
    }
    Spacer(Modifier.height(16.dp))
    Text(
        "데이터는 추천과 주행 기록에만 쓰이며 언제든 해제할 수 있어요.",
        color = colors.textSecondary,
        style = AppType.LabelMedium,
    )
    Spacer(Modifier.height(24.dp))
    PrimaryButton(
        text = "${selectedBrand.label} 계정으로 연결",
        icon = AppIcon.Car,
        onClick = onConnect,
    )
}

@Composable
private fun BrandOption(
    brand: VehicleBrand,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = AppTheme.colors
    val contentColor = if (selected) colors.onPrimary else colors.text
    Row(
        modifier = modifier
            .heightIn(min = 88.dp)
            .clip(CardShape)
            .background(if (selected) colors.primary else Color.Transparent)
            .border(2.dp, if (selected) colors.primary else colors.line, CardShape)
            .tapClickable(role = Role.RadioButton, onClick = onClick),
        horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (selected) AppIconImage(AppIcon.Check, contentColor, size = 32.dp)
        Text(brand.label, color = contentColor, style = if (selected) AppType.BodySmall.bold else AppType.BodySmall)
    }
}

@Composable
private fun PermissionRow(permission: DataPermission) {
    val colors = AppTheme.colors
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier
                .size(48.dp)
                .background(colors.surfaceVariant, CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            AppIconImage(AppIcon.Check, colors.text, size = 32.dp)
        }
        Spacer(Modifier.width(16.dp))
        Column {
            Text(permission.title, color = colors.text, style = AppType.BodySmall.bold)
            Text(permission.detail, color = colors.textSecondary, style = AppType.LabelMedium)
        }
    }
}
