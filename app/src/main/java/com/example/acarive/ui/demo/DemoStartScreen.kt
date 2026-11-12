package com.example.acarive.ui.demo

import android.content.Context
import android.graphics.Color as AndroidColor
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.example.acarive.location.LocationPoint
import com.example.acarive.ui.components.AppIcon
import com.example.acarive.ui.components.AppIconImage
import com.example.acarive.ui.components.ButtonShape
import com.example.acarive.ui.components.CardShape
import com.example.acarive.ui.components.ContentMaxWidth
import com.example.acarive.ui.components.IconActionButton
import com.example.acarive.ui.components.PrimaryButton
import com.example.acarive.ui.components.tapClickable
import com.example.acarive.ui.theme.ActiveBlue
import com.example.acarive.ui.theme.AlertRed
import com.example.acarive.ui.theme.AppTheme
import com.example.acarive.ui.theme.AppType
import com.example.acarive.ui.theme.bold
import org.maplibre.android.MapLibre
import org.maplibre.android.camera.CameraPosition
import org.maplibre.android.camera.CameraUpdateFactory
import org.maplibre.android.geometry.LatLng
import org.maplibre.android.maps.MapLibreMap
import org.maplibre.android.maps.MapLibreMapOptions
import org.maplibre.android.maps.MapView
import org.maplibre.android.style.layers.CircleLayer
import org.maplibre.android.style.layers.PropertyFactory.circleColor
import org.maplibre.android.style.layers.PropertyFactory.circleRadius
import org.maplibre.android.style.layers.PropertyFactory.circleStrokeColor
import org.maplibre.android.style.layers.PropertyFactory.circleStrokeWidth
import org.maplibre.android.style.sources.GeoJsonSource
import org.maplibre.geojson.Feature
import org.maplibre.geojson.FeatureCollection
import org.maplibre.geojson.Point

private const val STYLE_URL = "https://tiles.openfreemap.org/styles/positron"
// 데모 위치가 없으면 코스 출발점이 다 보이게 남한 전체부터
private val KOREA_CENTER = LocationPoint(36.3, 127.8)
private const val KOREA_ZOOM = 6.0
private const val START_ZOOM = 10.0

/**
 * 데모 주행 출발 위치 고르기. 지도를 움직이거나 찍으면 가운데 핀 위치가 현재 위치가 된다.
 * 파란 점은 코스 출발점이라 근처를 찍으면 주행 중 추천까지 바로 시연할 수 있다.
 */
@Composable
fun DemoStartScreen(
    // 지금 데모 위치. null 이면 데모 꺼짐
    demoStart: LocationPoint?,
    courseStarts: List<LocationPoint>,
    onPick: (LocationPoint) -> Unit,
    onClearDemo: () -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = AppTheme.colors
    // 지도가 준비되면 채워진다. 버튼을 누를 때 가운데 좌표를 읽는다
    var map by remember { mutableStateOf<MapLibreMap?>(null) }
    val mapView = rememberMapView(demoStart, courseStarts) { map = it }

    Box(modifier.fillMaxSize().background(colors.background)) {
        AndroidView(factory = { mapView }, modifier = Modifier.fillMaxSize())

        // 가운데 핀. 아이콘(24) 안에서 핀 끝이 y=21 이라 72dp 기준 27dp 올려야 끝이 가운데에 온다
        AppIconImage(
            icon = AppIcon.Pin,
            tint = AlertRed,
            size = 72.dp,
            strokeWidth = 2.5f,
            contentDescription = "선택 위치",
            modifier = Modifier.align(Alignment.Center).offset(y = (-27).dp),
        )

        Row(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(24.dp)
                .widthIn(max = ContentMaxWidth)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            IconActionButton(
                icon = AppIcon.Close,
                contentDescription = "닫기",
                background = colors.surface,
                tint = colors.text,
                onClick = onClose,
            )
            Column(
                Modifier
                    .weight(1f)
                    .clip(CardShape)
                    .background(colors.surface)
                    .padding(horizontal = 24.dp, vertical = 16.dp),
            ) {
                Text("데모 주행 위치", color = colors.text, style = AppType.BodyMedium.bold)
                Text(
                    "지도를 찍어 현재 위치를 정하세요. 파란 점은 코스 출발점이에요",
                    color = colors.textSecondary,
                    style = AppType.LabelMedium,
                )
            }
        }

        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .padding(24.dp)
                .widthIn(max = ContentMaxWidth)
                .fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            PrimaryButton(
                text = "여기를 현재 위치로",
                icon = AppIcon.Pin,
                onClick = {
                    val target = map?.cameraPosition?.target ?: return@PrimaryButton
                    onPick(LocationPoint(target.latitude, target.longitude))
                    onClose()
                },
            )
            if (demoStart != null) {
                Box(
                    Modifier
                        .fillMaxWidth()
                        .heightIn(min = 100.dp)
                        .clip(ButtonShape)
                        .background(colors.surface)
                        .tapClickable(role = Role.Button) {
                            onClearDemo()
                            onClose()
                        },
                    contentAlignment = Alignment.Center,
                ) {
                    Text("데모 끄고 실제 위치 쓰기", color = colors.text, style = AppType.BodyMedium.bold)
                }
            }
        }
    }
}

// 화면 생명주기에 맞춰 MapView 를 돌린다
@Composable
private fun rememberMapView(
    initial: LocationPoint?,
    courseStarts: List<LocationPoint>,
    onReady: (MapLibreMap) -> Unit,
): MapView {
    val context = LocalContext.current
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    val mapView = remember { createMapView(context, initial, courseStarts, onReady) }
    DisposableEffect(lifecycle, mapView) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_START -> mapView.onStart()
                Lifecycle.Event.ON_RESUME -> mapView.onResume()
                Lifecycle.Event.ON_PAUSE -> mapView.onPause()
                Lifecycle.Event.ON_STOP -> mapView.onStop()
                else -> Unit
            }
        }
        lifecycle.addObserver(observer)
        onDispose {
            lifecycle.removeObserver(observer)
            // 화면이 먼저 닫히면 직접 내려준다
            if (lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)) mapView.onPause()
            if (lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED)) mapView.onStop()
            mapView.onDestroy()
        }
    }
    return mapView
}

private fun createMapView(
    context: Context,
    initial: LocationPoint?,
    courseStarts: List<LocationPoint>,
    onReady: (MapLibreMap) -> Unit,
): MapView {
    MapLibre.getInstance(context)
    val center = initial ?: KOREA_CENTER
    val camera = CameraPosition.Builder()
        .target(LatLng(center.lat, center.lng))
        .zoom(if (initial != null) START_ZOOM else KOREA_ZOOM)
        .build()
    val options = MapLibreMapOptions.createFromAttributes(context)
        .camera(camera)
        // Compose 안에서 다른 UI 와 겹쳐 그리기 쉽도록 TextureView
        .textureMode(true)
    return MapView(context, options).apply {
        onCreate(null)
        getMapAsync { map ->
            onReady(map)
            map.setStyle(STYLE_URL) { style ->
                val features = courseStarts.map { Feature.fromGeometry(Point.fromLngLat(it.lng, it.lat)) }
                style.addSource(GeoJsonSource("course-starts", FeatureCollection.fromFeatures(features)))
                style.addLayer(
                    CircleLayer("course-starts", "course-starts").withProperties(
                        circleRadius(7f),
                        circleColor(ActiveBlue.toArgb()),
                        circleStrokeColor(AndroidColor.WHITE),
                        circleStrokeWidth(2f),
                    ),
                )
            }
            // 찍은 곳으로 지도를 옮겨 가운데 핀에 맞춘다
            map.addOnMapClickListener { latLng ->
                map.animateCamera(CameraUpdateFactory.newLatLng(latLng))
                true
            }
        }
    }
}
