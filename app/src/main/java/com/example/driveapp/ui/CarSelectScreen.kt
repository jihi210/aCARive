package com.example.driveapp.ui

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
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.driveapp.car.CarModel
import com.example.driveapp.car.TopDownCar

private val Accent = Color(0xFF3B6FE0)

// 영상 속에서 달릴 차를 고르는 화면 (정차 중에만 열림)
// 위: 고른 차의 큰 사진 + 영상에 나올 탑뷰 모습 / 아래: 차 5대 카드 / 맨 아래: 큰 버튼 2개
@Composable
fun CarSelectScreen(
    initial: CarModel,
    onConfirm: (CarModel) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    var picked by remember { mutableStateOf(initial) }

    Column(
        modifier = modifier
            .background(Color(0xFFF7F8FA))
            // 빈 곳을 눌러도 뒤 화면(기록 버튼 등)이 눌리지 않게 터치를 여기서 받아 버림
            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) {}
            .padding(horizontal = 64.dp, vertical = 40.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("영상 속에서 달릴 차를 골라 주세요", fontSize = 36.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(28.dp))

        // 고른 차 크게 보기
        Row(
            modifier = Modifier.fillMaxWidth().weight(1f),
            horizontalArrangement = Arrangement.spacedBy(40.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Image(
                painter = painterResource(picked.photoRes),
                contentDescription = picked.displayName,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .weight(1.6f)
                    .height(420.dp)
                    .clip(RoundedCornerShape(24.dp))
            )
            Column(
                modifier = Modifier.weight(1f),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(picked.displayName, fontSize = 40.sp, fontWeight = FontWeight.Bold)
                Text(picked.category, fontSize = 24.sp, color = Color.Gray)
                Spacer(Modifier.height(24.dp))
                TopDownPreview(car = picked, modifier = Modifier.size(280.dp))
                Text("영상에서는 이렇게 위에서 본 모습으로 달려요", fontSize = 20.sp, color = Color.Gray)
            }
        }

        Spacer(Modifier.height(28.dp))

        // 차 5대 카드
        Row(horizontalArrangement = Arrangement.spacedBy(20.dp)) {
            CarModel.entries.forEach { car ->
                CarCard(car = car, selected = car == picked, onClick = { picked = car })
            }
        }

        Spacer(Modifier.height(32.dp))

        // 큰 버튼 (차량 터치 기준에 맞춰 높이 88dp)
        Row(horizontalArrangement = Arrangement.spacedBy(24.dp)) {
            OutlinedButton(onClick = onDismiss, modifier = Modifier.size(width = 240.dp, height = 88.dp)) {
                Text("취소", fontSize = 26.sp)
            }
            Button(onClick = { onConfirm(picked) }, modifier = Modifier.size(width = 420.dp, height = 88.dp)) {
                Text("이 차로 영상 만들기", fontSize = 26.sp)
            }
        }
    }
}

@Composable
private fun CarCard(car: CarModel, selected: Boolean, onClick: () -> Unit) {
    Column(
        modifier = Modifier
            .width(260.dp)
            .clip(RoundedCornerShape(20.dp))
            .background(if (selected) Color(0xFFE8EFFD) else Color.White)
            .border(
                width = if (selected) 4.dp else 1.dp,
                color = if (selected) Accent else Color(0xFFDDE1E6),
                shape = RoundedCornerShape(20.dp)
            )
            .clickable(onClick = onClick)
            .padding(12.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Image(
            painter = painterResource(car.photoRes),
            contentDescription = car.displayName,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxWidth().height(140.dp).clip(RoundedCornerShape(12.dp))
        )
        Spacer(Modifier.height(10.dp))
        Text(
            car.displayName,
            fontSize = 22.sp,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
            color = if (selected) Accent else Color.Unspecified
        )
    }
}

// 탑뷰 차를 천천히 한 바퀴씩 돌려 보여 주는 미리보기
@Composable
private fun TopDownPreview(car: CarModel, modifier: Modifier = Modifier) {
    val spin = rememberInfiniteTransition(label = "spin")
    val angle by spin.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(tween(8000, easing = LinearEasing), RepeatMode.Restart),
        label = "angle"
    )
    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            drawCircle(Color(0xFFE9ECF1))
            drawIntoCanvas { c ->
                TopDownCar.draw(
                    c.nativeCanvas, car,
                    cx = center.x, cy = center.y,
                    lengthPx = size.minDimension * 0.75f,
                    headingDeg = angle
                )
            }
        }
    }
}
