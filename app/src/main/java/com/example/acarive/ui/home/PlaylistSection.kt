package com.example.acarive.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.acarive.data.model.NowPlaying
import com.example.acarive.ui.components.AppIcon
import com.example.acarive.ui.components.AppIconImage
import com.example.acarive.ui.components.CardShape
import com.example.acarive.ui.components.ChipShape
import com.example.acarive.ui.components.IconActionButton
import com.example.acarive.ui.components.PagePadding
import com.example.acarive.ui.components.TagChip
import com.example.acarive.ui.theme.AppTheme
import com.example.acarive.ui.theme.AppType
import com.example.acarive.ui.theme.HyundaiNavy
import com.example.acarive.ui.theme.SkyBlue
import com.example.acarive.ui.theme.White
import com.example.acarive.ui.theme.bold

// 카드 내용은 DriveMusic.card (Spotify + 추천 엔진). 날씨는 아직 Mock
// TODO(backend): 가이드에 따라 첫 화면에 미리 정의된 플레이리스트·최근 재생·즐겨찾기를 함께 노출 + 플레이리스트 상세 화면
@Composable
internal fun PlaylistSection(
    nowPlaying: NowPlaying,
    isPlaying: Boolean,
    onTogglePlaying: () -> Unit,
) {
    val colors = AppTheme.colors
    Column(
        Modifier
            .padding(horizontal = PagePadding)
            .fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(CardShape)
                .background(colors.emphasis)
                .padding(24.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(128.dp)
                    .background(SkyBlue, ChipShape),
                contentAlignment = Alignment.Center,
            ) {
                AppIconImage(AppIcon.Music, HyundaiNavy, size = 64.dp)
            }
            Spacer(Modifier.width(24.dp))
            Column(Modifier.weight(1f)) {
                Text(nowPlaying.mixName, color = colors.onEmphasisAccent, style = AppType.LabelSmall.bold)
                Text(
                    nowPlaying.title,
                    color = colors.onEmphasis,
                    style = AppType.TitleSmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    nowPlaying.subtitle,
                    color = colors.onEmphasisSecondary,
                    style = AppType.LabelSmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Spacer(Modifier.width(16.dp))
            IconActionButton(
                icon = if (isPlaying) AppIcon.Pause else AppIcon.Play,
                contentDescription = if (isPlaying) "일시정지" else "재생",
                background = White,
                tint = HyundaiNavy,
                filled = !isPlaying,
                buttonSize = 96.dp,
                shape = RoundedCornerShape(48.dp),
                onClick = onTogglePlaying,
            )
        }
        Spacer(Modifier.height(16.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            nowPlaying.contextTags.forEach { TagChip(it) }
        }
    }
}
