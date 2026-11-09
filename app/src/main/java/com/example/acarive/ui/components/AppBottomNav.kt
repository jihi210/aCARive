package com.example.acarive.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.acarive.ui.navigation.AppTab
import com.example.acarive.ui.theme.AppTheme
import com.example.acarive.ui.theme.AppType

/** 세로 창의 하단 탭 */
@Composable
fun AppBottomNav(activeTab: AppTab, onSelect: (AppTab) -> Unit, modifier: Modifier = Modifier) {
    Column(modifier.fillMaxWidth().background(AppTheme.colors.surface)) {
        HorizontalLine()
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .height(120.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            AppTab.entries.forEach { tab ->
                NavItem(tab, active = tab == activeTab, onClick = { onSelect(tab) }, Modifier.weight(1f))
            }
        }
    }
}

/** 가로 창의 측면 탭. 세로 공간을 콘텐츠에 양보한다. */
@Composable
fun AppNavRail(activeTab: AppTab, onSelect: (AppTab) -> Unit, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxHeight()
            .width(160.dp)
            .background(AppTheme.colors.surface)
            .navigationBarsPadding(),
        verticalArrangement = Arrangement.SpaceEvenly,
    ) {
        AppTab.entries.forEach { tab ->
            NavItem(tab, active = tab == activeTab, onClick = { onSelect(tab) }, Modifier.fillMaxWidth())
        }
    }
}

@Composable
private fun NavItem(tab: AppTab, active: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val color = if (active) AppTheme.colors.text else AppTheme.colors.textSecondary
    Column(
        modifier = modifier
            .clip(CardShape)
            .tapClickable(role = Role.Tab, onClick = onClick)
            .height(112.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        AppIconImage(
            icon = tab.icon,
            tint = color,
            filled = active && tab.icon == AppIcon.Heart,
            strokeWidth = if (active) 2.6f else 2f,
        )
        Spacer(Modifier.height(4.dp))
        Text(
            tab.label,
            color = color,
            style = AppType.LabelSmall.copy(fontWeight = if (active) FontWeight.Bold else FontWeight.Normal),
        )
    }
}
