package com.example.acarive.ui.drive

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.acarive.data.model.DriveSuggestion
import com.example.acarive.data.model.Route
import com.example.acarive.ui.components.AppIcon
import com.example.acarive.ui.components.AppIconImage
import com.example.acarive.ui.components.CardShape
import com.example.acarive.ui.components.IconActionButton
import com.example.acarive.ui.components.tapClickable
import com.example.acarive.ui.theme.ACARiveTheme
import com.example.acarive.ui.theme.AppTheme
import com.example.acarive.ui.theme.AppType
import com.example.acarive.ui.theme.bold

// 주행 중 추천 카드. 코스를 누르면 출발(Y), X 를 누르면 닫기(N).
// 운전 중에 보는 화면이라 사진·긴 설명 없이 이름과 거리만 크게 보여준다.
@Composable
fun DriveSuggestionCard(
    suggestions: List<DriveSuggestion>,
    onAccept: (Route) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = AppTheme.colors
    Column(
        modifier = modifier
            .clip(CardShape)
            .background(colors.surface)
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            AppIconImage(AppIcon.Sparkle, colors.text, size = 40.dp)
            Spacer(Modifier.width(16.dp))
            Text(
                "근처에 이런 코스가 있어요",
                color = colors.text,
                style = AppType.TitleSmall,
                modifier = Modifier.weight(1f),
            )
            IconActionButton(
                icon = AppIcon.Close,
                contentDescription = "괜찮아요",
                background = Color.Transparent,
                tint = colors.textSecondary,
                onClick = onDismiss,
            )
        }
        suggestions.forEach { suggestion ->
            SuggestionRow(suggestion, onClick = { onAccept(suggestion.route) })
        }
    }
}

@Composable
private fun SuggestionRow(suggestion: DriveSuggestion, onClick: () -> Unit) {
    val colors = AppTheme.colors
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 112.dp)
            .clip(CardShape)
            .background(colors.surfaceVariant)
            .tapClickable(role = Role.Button, onClick = onClick)
            .padding(horizontal = 24.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(
                suggestion.route.name,
                color = colors.text,
                style = AppType.BodyMedium.bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                (listOf(suggestion.etaLabel) + suggestion.route.tags.take(1)).joinToString(" · "),
                color = colors.textSecondary,
                style = AppType.LabelMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        Spacer(Modifier.width(16.dp))
        Text("가볼게요", color = colors.text, style = AppType.BodySmall.bold)
        AppIconImage(AppIcon.Chevron, colors.text, size = 40.dp)
    }
}

@Preview(showBackground = true, widthDp = 840)
@Composable
private fun DriveSuggestionCardPreview() {
    fun route(id: String, name: String, tag: String) =
        Route(id, name, "경기 가평군", "2시간", "90 km", listOf(tag), null, null, "")
    ACARiveTheme {
        DriveSuggestionCard(
            suggestions = listOf(
                DriveSuggestion(route("a", "북한강 물빛 드라이브", "강변"), "지금 위치에서 10분"),
                DriveSuggestion(route("b", "청평호 노을길", "노을"), "지금 위치에서 20분"),
            ),
            onAccept = {},
            onDismiss = {},
        )
    }
}
