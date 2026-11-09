package com.example.acarive.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.example.acarive.R

@OptIn(ExperimentalTextApi::class)
private fun astaSans(weight: FontWeight) = Font(
    resId = R.font.asta_sans,
    weight = weight,
    variationSettings = FontVariation.Settings(FontVariation.weight(weight.weight)),
)

/** Pleos Connect 가이드 지정 서체 */
val AstaSans = FontFamily(
    astaSans(FontWeight.Normal),
    astaSans(FontWeight.Bold),
    astaSans(FontWeight.ExtraBold),
)

private fun scale(size: Int, lineHeight: Float = 1.5f) = TextStyle(
    fontFamily = AstaSans,
    fontWeight = FontWeight.Normal,
    fontSize = size.sp,
    lineHeight = (size * lineHeight).sp,
)

/** Pleos Connect UI 디자인 가이드의 타입 스케일. 가장 작은 글자는 LabelSmall(20)이다. */
object AppType {
    val HeadlineMedium = scale(56, 1.25f).bold
    val HeadlineSmall = scale(48, 1.25f).bold
    val TitleLarge = scale(40, 1.3f).bold
    val TitleMedium = scale(36, 1.3f).bold
    val TitleSmall = scale(32, 1.3f).bold
    val BodyMedium = scale(28)
    val BodySmall = scale(26)
    val LabelMedium = scale(24)
    val LabelSmall = scale(20)
}

val TextStyle.bold: TextStyle get() = copy(fontWeight = FontWeight.Bold)

val Typography = Typography(bodyLarge = AppType.BodySmall)
