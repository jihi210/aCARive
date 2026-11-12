package com.example.acarive.ui.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.acarive.data.SampleData
import com.example.acarive.data.model.DriveSummary
import com.example.acarive.ui.AppUiState
import com.example.acarive.ui.components.PagePadding
import com.example.acarive.ui.theme.ACARiveTheme
import com.example.acarive.ui.theme.AppTheme
import com.example.acarive.ui.theme.AppType

/**
 * 가이드에 따라 자주 쓰는 기능을 첫 화면 상단에 둔다:
 * 드라이브 모드 -> 플레이리스트 -> 추천 코스 순서. 가로 창은 왼쪽에 조작, 오른쪽에 코스를 놓는다.
 */
@Composable
fun HomeScreen(
    state: AppUiState,
    driveSummary: DriveSummary,
    isWide: Boolean,
    onToggleDriveMode: () -> Unit,
    onOpenVehicle: () -> Unit,
    onSelectRoute: (String) -> Unit,
    onTogglePlaying: () -> Unit,
    onToggleFavorite: (String) -> Unit,
    onOpenAllRoutes: () -> Unit,
    onOpenRecord: () -> Unit,
    onOpenNotifications: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val controls: @Composable () -> Unit = {
        HomeTopBar(onOpenNotifications)
        DriveStatusRow(isDriveMode = state.isDriveMode, onToggle = onToggleDriveMode)
        PlaylistSection(
            nowPlaying = state.nowPlaying,
            isPlaying = state.isPlaying,
            onTogglePlaying = onTogglePlaying,
        )
        VehicleStrip(vehicle = state.vehicle, onClick = onOpenVehicle)
    }
    val recommendations: @Composable () -> Unit = {
        state.hero?.let { hero ->
            HeroCard(
                hero = hero,
                weather = state.weather,
                onOpenRoute = { onSelectRoute(hero.route.id) },
            )
        }
        RouteCarouselSection(
            // 기획: 메인에는 5개만. 나머지는 전체보기(코스 탭)
            routes = state.routes.take(HOME_ROUTE_COUNT),
            favoriteRouteIds = state.favoriteRouteIds,
            onSelectRoute = onSelectRoute,
            onToggleFavorite = onToggleFavorite,
            onOpenAll = onOpenAllRoutes,
        )
    }

    if (isWide) {
        Row(modifier.fillMaxSize()) {
            HomeColumn(Modifier.weight(1f)) {
                controls()
                DriveStoryBanner(summary = driveSummary, onClick = onOpenRecord)
            }
            HomeColumn(Modifier.weight(1f)) { recommendations() }
        }
    } else {
        HomeColumn(modifier.fillMaxSize()) {
            controls()
            recommendations()
            DriveStoryBanner(summary = driveSummary, onClick = onOpenRecord)
        }
    }
}

@Composable
private fun HomeColumn(modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    Column(
        modifier = modifier
            .verticalScroll(rememberScrollState())
            .padding(vertical = PagePadding),
        verticalArrangement = Arrangement.spacedBy(24.dp),
    ) {
        content()
    }
}

@Composable
internal fun SectionHeading(
    title: String,
    modifier: Modifier = Modifier,
    action: @Composable () -> Unit = {},
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            title,
            color = AppTheme.colors.text,
            style = AppType.TitleSmall,
            modifier = Modifier.weight(1f),
        )
        action()
    }
}

private const val HOME_ROUTE_COUNT = 5

private val PreviewDriveSummary = DriveSummary(2026, 12_480.0, 42, "186시간", emptyList())

private val PreviewState = AppUiState(
    routes = SampleData.routes,
    hero = SampleData.hero,
    favoriteRouteIds = SampleData.favoriteRouteIds,
)

@Preview(showBackground = true, widthDp = 840, heightDp = 2200)
@Composable
private fun HomeScreenPreview() {
    ACARiveTheme {
        HomeScreen(PreviewState, PreviewDriveSummary, isWide = false, {}, {}, {}, {}, {}, {}, {}, {})
    }
}

@Preview(showBackground = true, widthDp = 1680, heightDp = 1100)
@Composable
private fun HomeScreenWidePreview() {
    ACARiveTheme(darkTheme = true) {
        HomeScreen(PreviewState, PreviewDriveSummary, isWide = true, {}, {}, {}, {}, {}, {}, {}, {})
    }
}
