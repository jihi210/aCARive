package com.example.acarive.ui.sheets

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.example.acarive.data.course.CourseTheme
import com.example.acarive.ui.components.AppIcon
import com.example.acarive.ui.components.AppIconImage
import com.example.acarive.ui.components.ButtonShape
import com.example.acarive.ui.components.IconActionButton
import com.example.acarive.ui.components.PagePadding
import com.example.acarive.ui.components.PrimaryButton
import com.example.acarive.ui.components.tapClickable
import com.example.acarive.ui.theme.AppTheme
import com.example.acarive.ui.theme.AppType

// 취향(명시적 프로필) 다시 설정하기. 코스 추천이 쓰는 테마 이름을 그대로 저장한다.
@Composable
fun PreferenceSheet(
    current: List<String>,
    isDrivingRestricted: Boolean,
    onSave: (List<String>) -> Unit,
    onDismiss: () -> Unit,
) {
    val colors = AppTheme.colors
    // 예전 문구("한적한 길")도 테마로 맞춰서 처음 선택 상태를 만든다
    var selected by rememberSaveable {
        mutableStateOf(
            CourseTheme.entries.filter { theme -> current.any { it.contains(theme.label) || theme.label.contains(it) } }
                .map { it.label },
        )
    }
    AppBottomSheet(onDismiss = onDismiss) { close ->
        Column(
            Modifier
                .verticalScroll(rememberScrollState())
                .navigationBarsPadding()
                .padding(PagePadding),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    "어떤 드라이브를 좋아하세요?",
                    color = colors.text,
                    style = AppType.TitleLarge,
                    modifier = Modifier.weight(1f),
                )
                IconActionButton(
                    icon = AppIcon.Close,
                    contentDescription = "닫기",
                    background = colors.surfaceVariant,
                    tint = colors.text,
                    onClick = close,
                )
            }

            // 설정 작업이라 주행 중에는 막는다 (NO_SETUP)
            if (isDrivingRestricted) {
                Text(
                    "취향 설정은 주차 후에 할 수 있어요.",
                    color = colors.text,
                    style = AppType.BodyMedium,
                    modifier = Modifier.padding(vertical = 48.dp),
                )
                return@Column
            }

            Spacer(Modifier.height(8.dp))
            Text(
                "고른 취향에 맞는 코스를 먼저 추천해요. 여러 개 고를 수 있어요.",
                color = colors.textSecondary,
                style = AppType.BodySmall,
            )
            Spacer(Modifier.height(32.dp))
            ThemeChips(
                selected = selected,
                onToggle = { label ->
                    // 테마 정의 순서를 지킨다
                    val next = if (label in selected) selected - label else selected + label
                    selected = CourseTheme.entries.map { it.label }.filter { it in next }
                },
            )
            Spacer(Modifier.height(40.dp))
            PrimaryButton(
                text = if (selected.isEmpty()) "취향 없이 저장하기" else "${selected.size}개 저장하기",
                icon = AppIcon.Check,
                onClick = {
                    onSave(selected)
                    close()
                },
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ThemeChips(selected: List<String>, onToggle: (String) -> Unit) {
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        CourseTheme.entries.forEach { theme ->
            ThemeChip(theme.label, isSelected = theme.label in selected, onClick = { onToggle(theme.label) })
        }
    }
}

@Composable
private fun ThemeChip(label: String, isSelected: Boolean, onClick: () -> Unit) {
    val colors = AppTheme.colors
    val content = if (isSelected) colors.onPrimary else colors.text
    Row(
        modifier = Modifier
            .heightIn(min = 80.dp)
            .clip(ButtonShape)
            .background(if (isSelected) colors.primary else colors.surfaceVariant)
            .semantics { this.selected = isSelected }
            .tapClickable(role = Role.Checkbox, onClick = onClick)
            .padding(horizontal = 28.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (isSelected) {
            AppIconImage(AppIcon.Check, content, size = 32.dp)
            Spacer(Modifier.width(8.dp))
        }
        Text(label, color = content, style = AppType.BodySmall)
    }
}
