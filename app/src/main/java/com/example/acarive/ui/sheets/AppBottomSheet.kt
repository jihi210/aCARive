package com.example.acarive.ui.sheets

import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import com.example.acarive.ui.components.ContentMaxWidth
import com.example.acarive.ui.theme.AppTheme
import com.example.acarive.ui.theme.Black
import kotlinx.coroutines.launch

/** [content] 는 닫기 애니메이션을 실행하는 close 콜백을 받는다. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun AppBottomSheet(
    onDismiss: () -> Unit,
    content: @Composable ColumnScope.(close: () -> Unit) -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scope = rememberCoroutineScope()
    // 시트는 별도 창이라 테마에서 지정한 밀도를 다시 넘겨줘야 한다.
    val density = LocalDensity.current
    val close: () -> Unit = {
        scope.launch { sheetState.hide() }.invokeOnCompletion { onDismiss() }
    }
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        sheetMaxWidth = ContentMaxWidth,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
        containerColor = AppTheme.colors.surface,
        scrimColor = Black.copy(alpha = 0.6f),
        dragHandle = null,
    ) {
        CompositionLocalProvider(LocalDensity provides density) {
            content(close)
        }
    }
}
