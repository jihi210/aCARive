package com.example.acarive.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.snapping.SnapPosition
import androidx.compose.foundation.gestures.snapping.rememberSnapFlingBehavior
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.acarive.data.model.Route
import com.example.acarive.ui.components.AppIcon
import com.example.acarive.ui.components.CardShape
import com.example.acarive.ui.components.IconActionButton
import com.example.acarive.ui.components.PagePadding
import com.example.acarive.ui.components.RouteImage
import com.example.acarive.ui.components.TagRow
import com.example.acarive.ui.components.tapClickable
import com.example.acarive.ui.theme.AppTheme
import com.example.acarive.ui.theme.AppType
import com.example.acarive.ui.theme.Black
import com.example.acarive.ui.theme.White
import com.example.acarive.ui.theme.bold

@Composable
internal fun RouteCarouselSection(
    routes: List<Route>,
    favoriteRouteIds: Set<String>,
    onSelectRoute: (String) -> Unit,
    onToggleFavorite: (String) -> Unit,
    onOpenAll: () -> Unit,
) {
    Column(Modifier.fillMaxWidth()) {
        SectionHeading(
            title = "2–3시간 코스",
            modifier = Modifier.padding(horizontal = PagePadding),
        ) {
            Box(
                Modifier
                    .clip(CardShape)
                    .tapClickable(role = Role.Button, onClick = onOpenAll)
                    .heightIn(min = 80.dp)
                    .padding(horizontal = 16.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text("전체보기", color = AppTheme.colors.text, style = AppType.BodySmall)
            }
        }
        Spacer(Modifier.height(8.dp))
        val listState = rememberLazyListState()
        LazyRow(
            state = listState,
            flingBehavior = rememberSnapFlingBehavior(listState, SnapPosition.Start),
            contentPadding = PaddingValues(horizontal = PagePadding),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            items(routes, key = { it.id }) { route ->
                RouteCard(
                    route = route,
                    isFavorite = route.id in favoriteRouteIds,
                    onClick = { onSelectRoute(route.id) },
                    onToggleFavorite = { onToggleFavorite(route.id) },
                )
            }
        }
    }
}

@Composable
private fun RouteCard(
    route: Route,
    isFavorite: Boolean,
    onClick: () -> Unit,
    onToggleFavorite: () -> Unit,
) {
    val colors = AppTheme.colors
    Column(
        Modifier
            .width(560.dp)
            .clip(CardShape)
            .background(colors.surface)
            .tapClickable(onClick = onClick),
    ) {
        Box(Modifier.fillMaxWidth().height(280.dp)) {
            RouteImage(route, Modifier.fillMaxSize())
            IconActionButton(
                icon = AppIcon.Heart,
                contentDescription = "즐겨찾기",
                background = Black.copy(alpha = 0.6f),
                tint = White,
                filled = isFavorite,
                onClick = onToggleFavorite,
                modifier = Modifier.align(Alignment.TopEnd).padding(16.dp),
            )
        }
        Column(Modifier.padding(24.dp)) {
            TagRow(route.tags)
            Spacer(Modifier.height(12.dp))
            Text(
                route.name,
                color = colors.text,
                style = AppType.BodyMedium.bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                "${route.area} · ${route.duration} · ${route.distance}",
                color = colors.textSecondary,
                style = AppType.LabelMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}
