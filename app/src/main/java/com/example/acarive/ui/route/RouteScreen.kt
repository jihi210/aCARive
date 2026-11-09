package com.example.acarive.ui.route

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.acarive.data.SampleData
import com.example.acarive.data.model.Route
import com.example.acarive.ui.components.PageHeader
import com.example.acarive.ui.components.PagePadding
import com.example.acarive.ui.components.RouteListItem
import com.example.acarive.ui.components.tapClickable
import com.example.acarive.ui.theme.ACARiveTheme
import com.example.acarive.ui.theme.AppTheme
import com.example.acarive.ui.theme.AppType

private const val FILTER_ALL = "전체"

// TODO(backend): 필터 테마 목록을 서버 태그 기준으로 받아오고, 필터링도 코스 검색 API로 처리
private val RouteFilters = listOf(FILTER_ALL, "바다", "숲길", "한적한")

/** 세로 창은 1열, 가로 창은 2열로 배치된다. */
@Composable
fun RouteScreen(
    routes: List<Route>,
    favoriteRouteIds: Set<Int>,
    onSelectRoute: (Int) -> Unit,
    onToggleFavorite: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    var filter by rememberSaveable { mutableStateOf(FILTER_ALL) }
    val filtered = routes.filter { filter == FILTER_ALL || filter in it.tags }

    LazyVerticalGrid(
        columns = GridCells.Adaptive(minSize = 680.dp),
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(PagePadding),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        item(span = { GridItemSpan(maxLineSpan) }) {
            PageHeader(title = "오늘의 코스", modifier = Modifier.padding(bottom = 0.dp))
        }
        item(span = { GridItemSpan(maxLineSpan) }) {
            Row(
                modifier = Modifier.horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                RouteFilters.forEach { item ->
                    FilterChip(text = item, selected = item == filter, onClick = { filter = item })
                }
            }
        }
        items(filtered, key = { it.id }) { route ->
            RouteListItem(
                route = route,
                isFavorite = route.id in favoriteRouteIds,
                onClick = { onSelectRoute(route.id) },
                onToggleFavorite = { onToggleFavorite(route.id) },
            )
        }
    }
}

@Composable
private fun FilterChip(text: String, selected: Boolean, onClick: () -> Unit) {
    val colors = AppTheme.colors
    val shape = RoundedCornerShape(40.dp)
    Box(
        modifier = Modifier
            .heightIn(min = 80.dp)
            .clip(shape)
            .background(if (selected) colors.primary else Color.Transparent)
            .border(2.dp, if (selected) colors.primary else colors.line, shape)
            .tapClickable(role = Role.Tab, onClick = onClick)
            .padding(horizontal = 32.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(text, color = if (selected) colors.onPrimary else colors.text, style = AppType.BodySmall)
    }
}

@Preview(showBackground = true, widthDp = 840, heightDp = 1200)
@Composable
private fun RouteScreenPreview() {
    ACARiveTheme {
        RouteScreen(SampleData.routes, SampleData.favoriteRouteIds, {}, {})
    }
}
