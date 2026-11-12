package com.example.acarive.ui.sheets

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.acarive.car.CarModel
import com.example.acarive.car.TopDownCar
import com.example.acarive.ui.components.AppIcon
import com.example.acarive.ui.components.AppIconImage
import com.example.acarive.ui.components.CardShape
import com.example.acarive.ui.components.IconActionButton
import com.example.acarive.ui.components.PagePadding
import com.example.acarive.ui.components.PrimaryButton
import com.example.acarive.ui.components.tapClickable
import com.example.acarive.ui.theme.AppTheme
import com.example.acarive.ui.theme.AppType
import com.example.acarive.ui.theme.bold

/**
 * 추억 영상에 등장할 차를 사진으로 고르는 시트.
 * 사진을 둘러보는 설정 작업이라 주행 중에는 열지 않는다 (RecordScreen 에서 막음).
 */
@Composable
fun CarSelectSheet(
    current: CarModel,
    onConfirm: (CarModel) -> Unit,
    onDismiss: () -> Unit,
) {
    val colors = AppTheme.colors
    var picked by rememberSaveable { mutableStateOf(current) }
    AppBottomSheet(onDismiss = onDismiss) { close ->
        Column(
            Modifier
                .verticalScroll(rememberScrollState())
                .navigationBarsPadding()
                .padding(PagePadding),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    "영상 속에서 달릴 차",
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
            Spacer(Modifier.height(8.dp))
            Text(
                "고른 차가 위에서 본 모습으로 경로를 따라 달려요.",
                color = colors.textSecondary,
                style = AppType.BodySmall,
            )

            Spacer(Modifier.height(24.dp))
            CarHero(picked)

            Spacer(Modifier.height(24.dp))
            Row(
                Modifier.horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                CarModel.entries.forEach { car ->
                    CarCard(car, isSelected = car == picked, onClick = { picked = car })
                }
            }

            Spacer(Modifier.height(32.dp))
            PrimaryButton(
                text = "이 차로 영상 만들기",
                icon = AppIcon.Sparkle,
                onClick = {
                    onConfirm(picked)
                    close()
                },
            )
        }
    }
}

/** 고른 차의 큰 사진과, 영상에 실제로 나오는 탑뷰 모습 */
@Composable
private fun CarHero(car: CarModel) {
    val colors = AppTheme.colors
    Column(
        Modifier
            .fillMaxWidth()
            .clip(CardShape)
            .background(colors.surfaceVariant),
    ) {
        Image(
            painter = painterResource(car.photoRes),
            contentDescription = car.displayName,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxWidth().aspectRatio(16f / 9f),
        )
        Row(
            Modifier.fillMaxWidth().padding(24.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(Modifier.weight(1f)) {
                Text(car.displayName, color = colors.text, style = AppType.HeadlineSmall)
                Text(car.category, color = colors.textSecondary, style = AppType.LabelMedium)
                Spacer(Modifier.height(8.dp))
                Text("영상에서는 이렇게 보여요", color = colors.textSecondary, style = AppType.LabelMedium)
            }
            TopDownPreview(car, Modifier.size(160.dp))
        }
    }
}

/** 영상과 같은 코드로 그린 탑뷰 차. 천천히 돌려서 방향이 바뀌는 모습을 보여준다 */
@Composable
private fun TopDownPreview(car: CarModel, modifier: Modifier = Modifier) {
    val turn = rememberInfiniteTransition(label = "carTurn")
    val heading by turn.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(tween(8_000, easing = LinearEasing), RepeatMode.Restart),
        label = "heading",
    )
    Canvas(modifier.background(AppTheme.colors.surface, CircleShape)) {
        drawIntoCanvas {
            TopDownCar.draw(
                it.nativeCanvas, car,
                cx = size.width / 2, cy = size.height / 2,
                lengthPx = size.minDimension * 0.7f,
                headingDeg = heading,
            )
        }
    }
}

@Composable
private fun CarCard(car: CarModel, isSelected: Boolean, onClick: () -> Unit) {
    val colors = AppTheme.colors
    Column(
        Modifier
            .width(240.dp)
            .clip(CardShape)
            .background(colors.surface)
            .border(if (isSelected) 4.dp else 2.dp, if (isSelected) colors.primary else colors.line, CardShape)
            .semantics { selected = isSelected }
            .tapClickable(role = Role.RadioButton, onClick = onClick),
    ) {
        Box {
            Image(
                painter = painterResource(car.photoRes),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxWidth().height(136.dp).background(colors.surfaceVariant),
            )
            if (isSelected) {
                Box(
                    Modifier
                        .align(Alignment.TopEnd)
                        .padding(12.dp)
                        .size(48.dp)
                        .background(colors.primary, CircleShape),
                    contentAlignment = Alignment.Center,
                ) {
                    AppIconImage(AppIcon.Check, colors.onPrimary, size = 32.dp)
                }
            }
        }
        Column(Modifier.padding(horizontal = 20.dp, vertical = 16.dp)) {
            Text(
                car.displayName,
                color = colors.text,
                style = AppType.BodySmall.bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                car.category,
                color = colors.textSecondary,
                style = AppType.LabelMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}
