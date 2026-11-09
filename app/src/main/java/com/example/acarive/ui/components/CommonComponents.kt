package com.example.acarive.ui.components

import android.view.SoundEffectConstants
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.example.acarive.data.model.Route
import com.example.acarive.ui.theme.AppTheme
import com.example.acarive.ui.theme.AppType
import com.example.acarive.ui.theme.bold

/** 세로 앱 창 폭. 가로 창에서도 한 줄 콘텐츠는 이 폭을 넘지 않는다. */
val ContentMaxWidth = 840.dp
val PagePadding = 32.dp
val CardShape = RoundedCornerShape(24.dp)
val ButtonShape = RoundedCornerShape(30.dp)
val ChipShape = RoundedCornerShape(12.dp)

/** 가이드의 터치 피드백 요구사항에 맞춰 진동과 클릭음을 함께 주는 clickable */
fun Modifier.tapClickable(role: Role? = null, onClick: () -> Unit): Modifier = composed {
    val haptic = LocalHapticFeedback.current
    val view = LocalView.current
    clickable(role = role) {
        haptic.performHapticFeedback(HapticFeedbackType.ContextClick)
        view.playSoundEffect(SoundEffectConstants.CLICK)
        onClick()
    }
}

@Composable
fun TagChip(text: String, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .heightIn(min = 44.dp)
            .background(AppTheme.colors.surfaceVariant, ChipShape)
            .padding(horizontal = 16.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(text, color = AppTheme.colors.text, style = AppType.LabelSmall)
    }
}

@Composable
fun TagRow(tags: List<String>, modifier: Modifier = Modifier) {
    Row(modifier, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        tags.forEach { TagChip(it) }
    }
}

@Composable
fun PageHeader(title: String, modifier: Modifier = Modifier) {
    Text(
        title,
        modifier = modifier.padding(bottom = 32.dp),
        color = AppTheme.colors.text,
        style = AppType.TitleLarge,
    )
}

/** 탭 공통 레이아웃: 전체 스크롤, 가로 창에서는 가운데 정렬 */
@Composable
fun SubPage(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    Box(modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
        Column(
            modifier = Modifier
                .widthIn(max = ContentMaxWidth)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(PagePadding),
            content = content,
        )
    }
}

/** 가이드의 아이콘 버튼 (터치 영역 80, 아이콘 48) */
@Composable
fun IconActionButton(
    icon: AppIcon,
    contentDescription: String,
    background: Color,
    tint: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    filled: Boolean = false,
    buttonSize: Dp = 80.dp,
    shape: RoundedCornerShape = CardShape,
) {
    Box(
        modifier = modifier
            .size(buttonSize)
            .clip(shape)
            .background(background)
            .tapClickable(role = Role.Button, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        AppIconImage(icon, tint, filled = filled, contentDescription = contentDescription)
    }
}

/** 가이드의 Basic Button Large (높이 100) */
@Composable
fun PrimaryButton(
    text: String,
    icon: AppIcon,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = AppTheme.colors
    Row(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 100.dp)
            .clip(ButtonShape)
            .background(colors.primary)
            .tapClickable(role = Role.Button, onClick = onClick),
        horizontalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterHorizontally),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        AppIconImage(icon, colors.onPrimary)
        Text(text, color = colors.onPrimary, style = AppType.BodyMedium.bold)
    }
}

@Composable
fun RouteImage(route: Route, modifier: Modifier = Modifier, contentDescription: String? = route.name) {
    AsyncImage(
        model = route.imageUrl,
        contentDescription = contentDescription,
        contentScale = ContentScale.Crop,
        modifier = modifier.background(AppTheme.colors.surfaceVariant),
    )
}

@Composable
fun HorizontalLine(modifier: Modifier = Modifier) {
    Box(modifier.fillMaxWidth().height(2.dp).background(AppTheme.colors.line))
}

data class StatItem(val label: String, val value: String)

/** 세로 구분선이 있는 3열 수치 표 (주행 통계, 코스 상세, 차량 데이터) */
@Composable
fun StatGrid(items: List<StatItem>, modifier: Modifier = Modifier) {
    val colors = AppTheme.colors
    Row(modifier.fillMaxWidth().height(IntrinsicSize.Min)) {
        items.forEachIndexed { index, item ->
            if (index > 0) {
                Box(Modifier.width(2.dp).fillMaxHeight().background(colors.line))
            }
            Column(
                modifier = Modifier.weight(1f).padding(vertical = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(item.label, color = colors.textSecondary, style = AppType.LabelMedium, textAlign = TextAlign.Center)
                Spacer(Modifier.height(8.dp))
                Text(item.value, color = colors.text, style = AppType.BodyMedium.bold, textAlign = TextAlign.Center)
            }
        }
    }
}
