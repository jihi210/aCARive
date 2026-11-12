package com.example.acarive.ui.sheets

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.acarive.data.model.Route
import com.example.acarive.ui.components.AppIcon
import com.example.acarive.ui.components.AppIconImage
import com.example.acarive.ui.components.HorizontalLine
import com.example.acarive.ui.components.IconActionButton
import com.example.acarive.ui.components.PagePadding
import com.example.acarive.ui.components.PrimaryButton
import com.example.acarive.ui.components.RouteImage
import com.example.acarive.ui.components.StatGrid
import com.example.acarive.ui.components.StatItem
import com.example.acarive.ui.components.TagRow
import com.example.acarive.ui.theme.AppTheme
import com.example.acarive.ui.theme.AppType
import com.example.acarive.ui.theme.Black
import com.example.acarive.ui.theme.White

@Composable
fun RouteDetailSheet(
    route: Route,
    isFavorite: Boolean,
    isDrivingRestricted: Boolean,
    onToggleFavorite: () -> Unit,
    onStartDrive: () -> Unit,
    onDismiss: () -> Unit,
) {
    val colors = AppTheme.colors
    val imageButtonBackground = Black.copy(alpha = 0.6f)
    // TODO(backend): 코스 상세 API (경유지 목록, 실시간 도로 혼잡도). 지금은 목록 데이터를 그대로 쓴다.
    AppBottomSheet(onDismiss = onDismiss) { close ->
        Column(Modifier.verticalScroll(rememberScrollState())) {
            Box(Modifier.fillMaxWidth().height(360.dp)) {
                RouteImage(route, Modifier.fillMaxWidth().height(360.dp))
                IconActionButton(
                    icon = AppIcon.Close,
                    contentDescription = "닫기",
                    background = imageButtonBackground,
                    tint = White,
                    onClick = close,
                    modifier = Modifier.align(Alignment.TopStart).padding(24.dp),
                )
                IconActionButton(
                    icon = AppIcon.Heart,
                    contentDescription = "즐겨찾기",
                    background = imageButtonBackground,
                    tint = White,
                    filled = isFavorite,
                    onClick = onToggleFavorite,
                    modifier = Modifier.align(Alignment.TopEnd).padding(24.dp),
                )
            }
            Column(
                Modifier
                    .navigationBarsPadding()
                    .padding(PagePadding),
            ) {
                TagRow(route.tags)
                Spacer(Modifier.height(16.dp))
                Text(route.name, color = colors.text, style = AppType.TitleLarge)
                Spacer(Modifier.height(8.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    AppIconImage(AppIcon.Pin, colors.textSecondary, size = 32.dp)
                    Spacer(Modifier.width(8.dp))
                    Text(route.area, color = colors.textSecondary, style = AppType.BodySmall)
                }
                Spacer(Modifier.height(24.dp))
                HorizontalLine()
                StatGrid(
                    items = listOf(
                        StatItem("왕복 시간", route.duration),
                        StatItem("주행 거리", route.distance),
                        StatItem("경유지", if (route.stopCount > 0) "${route.stopCount}곳" else "-"),
                    ),
                )
                HorizontalLine()
                Spacer(Modifier.height(24.dp))
                // 긴 글은 주행 중에 숨긴다 (LIMIT_CONTENT).
                if (!isDrivingRestricted) {
                    Text(route.description, color = colors.textSecondary, style = AppType.BodySmall)
                    Spacer(Modifier.height(24.dp))
                }
                PrimaryButton(
                    text = "이 코스로 출발하기",
                    icon = AppIcon.Car,
                    onClick = {
                        close()
                        onStartDrive()
                    },
                )
            }
        }
    }
}
