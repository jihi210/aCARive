package com.example.acarive.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.example.acarive.data.model.HeroRecommendation
import com.example.acarive.data.model.Weather
import com.example.acarive.ui.components.AppIcon
import com.example.acarive.ui.components.AppIconImage
import com.example.acarive.ui.components.ButtonShape
import com.example.acarive.ui.components.CardShape
import com.example.acarive.ui.components.ChipShape
import com.example.acarive.ui.components.PagePadding
import com.example.acarive.ui.components.RouteImage
import com.example.acarive.ui.components.tapClickable
import com.example.acarive.ui.theme.AppType
import com.example.acarive.ui.theme.Black
import com.example.acarive.ui.theme.HyundaiNavy
import com.example.acarive.ui.theme.White
import com.example.acarive.ui.theme.bold

// 사진 위 글자의 대비 4.5:1 을 보장하기 위해 글자가 놓이는 아래쪽을 충분히 어둡게 한다.
private val HeroShade = Brush.verticalGradient(
    0.0f to Black.copy(alpha = 0.1f),
    0.35f to Black.copy(alpha = 0.3f),
    0.6f to Black.copy(alpha = 0.75f),
    1.0f to Black.copy(alpha = 0.9f),
)

// TODO(backend): 히어로 추천은 현재 위치/날씨/교통/취향을 반영한 추천 API 응답으로 교체 (문구 포함)
@Composable
internal fun HeroCard(
    hero: HeroRecommendation,
    weather: Weather,
    onOpenRoute: () -> Unit,
) {
    Box(
        Modifier
            .padding(horizontal = PagePadding)
            .fillMaxWidth()
            .height(560.dp)
            .clip(CardShape),
    ) {
        RouteImage(
            route = hero.route,
            contentDescription = hero.route.name,
            modifier = Modifier.fillMaxSize(),
        )
        Box(Modifier.fillMaxSize().background(HeroShade))

        Row(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(24.dp)
                .background(Black.copy(alpha = 0.6f), ChipShape)
                .padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            AppIconImage(AppIcon.Cloud, White, size = 40.dp)
            Text("${weather.temperature}° ${weather.description}", color = White, style = AppType.LabelMedium)
        }

        Column(
            Modifier
                .align(Alignment.BottomStart)
                .fillMaxWidth()
                .padding(32.dp),
        ) {
            Text(hero.headline, color = White, style = AppType.TitleLarge)
            Spacer(Modifier.height(8.dp))
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                AppIconImage(AppIcon.Pin, White, size = 32.dp)
                Text(hero.etaLabel, color = White, style = AppType.LabelMedium)
            }
            Spacer(Modifier.height(24.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 100.dp)
                    .clip(ButtonShape)
                    .background(White)
                    .tapClickable(role = Role.Button, onClick = onOpenRoute),
                horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text("코스 보기", color = HyundaiNavy, style = AppType.BodyMedium.bold)
                AppIconImage(AppIcon.Chevron, HyundaiNavy, size = 40.dp)
            }
        }
    }
}
