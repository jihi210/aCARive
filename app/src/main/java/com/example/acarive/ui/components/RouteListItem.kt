package com.example.acarive.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.acarive.data.model.Route
import com.example.acarive.ui.theme.AppTheme
import com.example.acarive.ui.theme.AppType
import com.example.acarive.ui.theme.bold

/** 코스 탭과 저장 탭에서 함께 쓰는 가로형 코스 카드 */
@Composable
fun RouteListItem(
    route: Route,
    isFavorite: Boolean,
    onClick: () -> Unit,
    onToggleFavorite: () -> Unit,
    modifier: Modifier = Modifier,
    showTags: Boolean = true,
) {
    val colors = AppTheme.colors
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(CardShape)
            .background(colors.surface)
            .tapClickable(onClick = onClick)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        RouteImage(
            route,
            Modifier
                .size(width = 208.dp, height = 168.dp)
                .clip(ChipShape),
        )
        Spacer(Modifier.width(24.dp))
        Column(Modifier.weight(1f)) {
            if (showTags) {
                TagRow(route.tags)
                Spacer(Modifier.height(12.dp))
            }
            Text(
                route.name,
                color = colors.text,
                style = AppType.BodyMedium.bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(Modifier.height(4.dp))
            Text(
                "${route.area} · ${route.duration}",
                color = colors.textSecondary,
                style = AppType.LabelMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        IconActionButton(
            icon = AppIcon.Heart,
            contentDescription = "즐겨찾기",
            background = Color.Transparent,
            tint = colors.text,
            filled = isFavorite,
            onClick = onToggleFavorite,
            modifier = Modifier.padding(start = 8.dp),
        )
    }
}
