package com.example.acarive.ui.saved

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.acarive.data.SampleData
import com.example.acarive.data.model.Route
import com.example.acarive.ui.components.AppIcon
import com.example.acarive.ui.components.AppIconImage
import com.example.acarive.ui.components.PageHeader
import com.example.acarive.ui.components.PagePadding
import com.example.acarive.ui.components.RouteListItem
import com.example.acarive.ui.theme.ACARiveTheme
import com.example.acarive.ui.theme.AppTheme
import com.example.acarive.ui.theme.AppType
import com.example.acarive.ui.theme.bold

@Composable
fun SavedScreen(
    routes: List<Route>,
    favoriteRouteIds: Set<String>,
    onSelectRoute: (String) -> Unit,
    onToggleFavorite: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    // TODO(backend): 추천 코스 목록에서 거르는 대신 즐겨찾기 목록 API로 조회 (추천에 없는 코스도 저장될 수 있음)
    val saved = routes.filter { it.id in favoriteRouteIds }

    LazyVerticalGrid(
        columns = GridCells.Adaptive(minSize = 680.dp),
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(PagePadding),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        item(span = { GridItemSpan(maxLineSpan) }) {
            PageHeader(title = "저장한 코스", modifier = Modifier.padding(bottom = 0.dp))
        }
        if (saved.isEmpty()) {
            item(span = { GridItemSpan(maxLineSpan) }) { EmptySaved() }
        } else {
            items(saved, key = { it.id }) { route ->
                RouteListItem(
                    route = route,
                    isFavorite = true,
                    showTags = false,
                    onClick = { onSelectRoute(route.id) },
                    onToggleFavorite = { onToggleFavorite(route.id) },
                )
            }
        }
    }
}

@Composable
private fun EmptySaved() {
    val colors = AppTheme.colors
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 96.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        AppIconImage(AppIcon.Heart, colors.textSecondary, size = 72.dp)
        Spacer(Modifier.height(16.dp))
        Text("저장한 코스가 없어요", color = colors.text, style = AppType.BodyMedium.bold)
        Text("코스의 하트를 눌러보세요", color = colors.textSecondary, style = AppType.LabelMedium)
    }
}

@Preview(showBackground = true, widthDp = 840, heightDp = 800)
@Composable
private fun SavedScreenPreview() {
    ACARiveTheme {
        SavedScreen(SampleData.routes, SampleData.favoriteRouteIds, {}, {})
    }
}

@Preview(showBackground = true, name = "Empty", widthDp = 840, heightDp = 800)
@Composable
private fun SavedScreenEmptyPreview() {
    ACARiveTheme {
        SavedScreen(SampleData.routes, emptySet(), {}, {})
    }
}
