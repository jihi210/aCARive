package com.example.acarive.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.acarive.ui.theme.AppTheme
import com.example.acarive.ui.theme.AppType

/** 가이드의 Toast: 최대 폭 792, 메시지 최대 폭 712. 여백 24는 호출부에서 준다. */
@Composable
fun AppToast(message: String, modifier: Modifier = Modifier) {
    val colors = AppTheme.colors
    Row(
        modifier = modifier
            .widthIn(max = 792.dp)
            .background(colors.primary, CardShape)
            .padding(horizontal = 32.dp, vertical = 24.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        AppIconImage(AppIcon.Check, colors.onPrimary, size = 32.dp)
        Spacer(Modifier.width(16.dp))
        Text(message, color = colors.onPrimary, style = AppType.BodySmall, modifier = Modifier.widthIn(max = 712.dp))
    }
}
