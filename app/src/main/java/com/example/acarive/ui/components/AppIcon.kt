package com.example.acarive.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.addPathNodes
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

private fun circle(cx: Float, cy: Float, r: Float): String =
    "M${cx - r} ${cy}a$r $r 0 1 0 ${2 * r} 0a$r $r 0 1 0 ${-2 * r} 0"

/** 시안의 24x24 라인 아이콘. SVG path 를 그대로 옮겼다. */
enum class AppIcon(vararg val paths: String) {
    Home("M3 10.8 12 3l9 7.8", "M5 9.5V21h14V9.5M9 21v-7h6v7"),
    Route(circle(6f, 19f, 2f), circle(18f, 5f, 2f), "M7.8 18c5.5-1 2.2-9 7.8-11.8"),
    History("M3 12a9 9 0 1 0 3-6.7L3 8", "M3 3v5h5M12 7v5l3 2"),
    Heart("M20.8 4.6a5.5 5.5 0 0 0-7.8 0L12 5.7l-1.1-1.1a5.5 5.5 0 0 0-7.8 7.8l1.1 1.1L12 21l7.8-7.5 1.1-1.1a5.5 5.5 0 0 0-.1-7.8Z"),
    User(circle(12f, 8f, 4f), "M4 21a8 8 0 0 1 16 0"),
    Bell("M18 8a6 6 0 0 0-12 0c0 7-3 7-3 9h18c0-2-3-2-3-9", "M10 21h4"),
    Cloud("M7 18h10a4 4 0 0 0 .7-7.9A6 6 0 0 0 6.3 8.4 4.8 4.8 0 0 0 7 18Z", "m8 21-1 2m5-2-1 2m5-2-1 2"),
    Play("m8 5 11 7-11 7Z"),
    Pause("M9 5v14M15 5v14"),
    Chevron("m9 18 6-6-6-6"),
    Clock(circle(12f, 12f, 9f), "M12 7v5l3 2"),
    Pin("M20 10c0 5-8 11-8 11S4 15 4 10a8 8 0 1 1 16 0Z", circle(12f, 10f, 2.5f)),
    Sparkle("m12 3 1.2 4.1L17 9l-3.8 1.9L12 15l-1.2-4.1L7 9l3.8-1.9Z", "m19 15 .6 2.1L22 18l-2.4.9L19 21l-.6-2.1L16 18l2.4-.9Z"),
    Close("m6 6 12 12M18 6 6 18"),
    Car("m5 16-1 2v2h2v-2h12v2h2v-2l-1-2-2-7H7Z", "M6 14h12M8 18h.01M16 18h.01"),
    Music("M9 18V5l10-2v13", circle(6f, 18f, 3f), circle(16f, 16f, 3f)),
    Share(circle(18f, 5f, 2f), circle(6f, 12f, 2f), circle(18f, 19f, 2f), "m8 11 8-5M8 13l8 5"),
    Check("m5 12 4 4L19 6"),
    Download("M12 4v11", "m7 10 5 5 5-5", "M5 20h14"),
}

private fun AppIcon.toImageVector(filled: Boolean, strokeWidth: Float): ImageVector {
    val builder = ImageVector.Builder(
        name = name,
        defaultWidth = 24.dp,
        defaultHeight = 24.dp,
        viewportWidth = 24f,
        viewportHeight = 24f,
    )
    paths.forEach { path ->
        builder.addPath(
            pathData = addPathNodes(path),
            fill = if (filled) SolidColor(Color.Black) else null,
            stroke = SolidColor(Color.Black),
            strokeLineWidth = strokeWidth,
            strokeLineCap = StrokeCap.Round,
            strokeLineJoin = StrokeJoin.Round,
        )
    }
    return builder.build()
}

@Composable
fun AppIconImage(
    icon: AppIcon,
    tint: Color,
    modifier: Modifier = Modifier,
    size: Dp = 48.dp,
    filled: Boolean = false,
    strokeWidth: Float = 2f,
    contentDescription: String? = null,
) {
    val vector = remember(icon, filled, strokeWidth) { icon.toImageVector(filled, strokeWidth) }
    Image(
        imageVector = vector,
        contentDescription = contentDescription,
        colorFilter = ColorFilter.tint(tint),
        modifier = modifier.size(size),
    )
}
