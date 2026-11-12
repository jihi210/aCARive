package com.example.acarive.ui

import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.acarive.location.PleosLocation
import com.example.acarive.ui.components.AppBottomNav
import com.example.acarive.ui.components.AppNavRail
import com.example.acarive.ui.components.AppToast
import com.example.acarive.ui.components.ContentMaxWidth
import com.example.acarive.ui.drive.DriveScreen
import com.example.acarive.ui.drive.DriveSuggestionCard
import com.example.acarive.ui.home.HomeScreen
import com.example.acarive.ui.my.MyScreen
import com.example.acarive.ui.navigation.AppTab
import com.example.acarive.ui.record.DriveRecordViewModel
import com.example.acarive.ui.record.RecordScreen
import com.example.acarive.ui.record.USE_MOCK_LOCATION
import com.example.acarive.ui.route.RouteScreen
import com.example.acarive.ui.saved.SavedScreen
import com.example.acarive.ui.sheets.RouteDetailSheet
import com.example.acarive.ui.sheets.VehicleConnectSheet
import com.example.acarive.ui.theme.AppTheme
import com.example.acarive.vehicle.PleosVehicle

/** 이 폭 이상이면 가로 창(앱 영역 2칸)으로 보고 측면 탭 + 2단 레이아웃을 쓴다. */
val WideWindowWidth = 1200.dp

@Composable
fun ACARiveApp(
    viewModel: AppViewModel = viewModel(),
    recordViewModel: DriveRecordViewModel = viewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val recordState by recordViewModel.uiState.collectAsStateWithLifecycle()
    var activeTab by rememberSaveable { mutableStateOf(AppTab.Home) }
    var selectedRouteId by rememberSaveable { mutableStateOf<String?>(null) }
    var activeDriveRouteId by rememberSaveable { mutableStateOf<String?>(null) }
    var showVehicleSheet by rememberSaveable { mutableStateOf(false) }

    val selectRoute: (String) -> Unit = { selectedRouteId = it }
    val openVehicleSheet = { showVehicleSheet = true }

    rememberStartupLocationRequest(onGranted = viewModel::startLocation)

    // 차량 읽기 권한을 받은 뒤 연결한다. 일부를 거절해도 받을 수 있는 값은 받는다.
    val vehiclePermissions = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions(),
    ) { viewModel.connectVehicle() }
    val connectVehicle: () -> Unit = {
        if (USE_MOCK_VEHICLE) viewModel.connectVehicle() else vehiclePermissions.launch(PleosVehicle.PERMISSIONS)
    }

    val startRecording = rememberRecordingStarter(
        onStart = {
            viewModel.startLocation()
            recordViewModel.startRecording()
            viewModel.showToast("드라이브 기록을 시작했어요")
        },
        onDenied = { viewModel.showToast("위치 권한이 있어야 경로를 기록할 수 있어요") },
    )
    // TODO(backend): 코스에서 출발한 경우 route.id 를 세션에 함께 저장하고, 기록을 서버에 업로드 (기록 영상 생성의 원본 데이터)
    val requestRecording = {
        when {
            recordState.isRecording -> viewModel.showToast("이미 기록 중이에요")
            !state.isDriveMode -> viewModel.showToast("드라이브 모드를 켜면 기록할 수 있어요")
            else -> startRecording()
        }
    }
    val finishRecording = {
        recordViewModel.stopRecording { saved ->
            viewModel.showToast(if (saved) "드라이브를 저장했어요" else "받은 위치가 없어 저장하지 않았어요")
        }
    }

    // 코스 안내 없이 기록만 하는 주행이면 주변 코스를 주기적으로 추천한다
    val isFreeDriving = recordState.isRecording && activeDriveRouteId == null
    LaunchedEffect(isFreeDriving) { viewModel.setFreeDriving(isFreeDriving) }

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .background(AppTheme.colors.background)
            .statusBarsPadding(),
    ) {
        val isWide = maxWidth >= WideWindowWidth
        val content: @Composable (Modifier) -> Unit = { modifier ->
            Box(modifier) {
                val activeDriveRoute = state.routes.firstOrNull { it.id == activeDriveRouteId }
                if (activeDriveRoute != null) {
                    DriveScreen(
                        routeName = activeDriveRoute.name,
                        remainingDistance = activeDriveRoute.distance,
                        remainingTime = activeDriveRoute.duration,
                        speedKph = 0,
                        onEndDrive = {
                            if (recordState.isRecording) finishRecording()
                            activeDriveRouteId = null
                        },
                    )
                } else when (activeTab) {
                    AppTab.Home -> HomeScreen(
                        state = state,
                        driveSummary = recordState.summary,
                        isWide = isWide,
                        onToggleDriveMode = {
                            // 드라이브 모드를 끄면 "기록하지 않아요" 안내대로 진행 중인 기록도 끝낸다.
                            if (state.isDriveMode && recordState.isRecording) finishRecording()
                            viewModel.toggleDriveMode()
                        },
                        onOpenVehicle = openVehicleSheet,
                        onSelectRoute = selectRoute,
                        onTogglePlaying = viewModel::togglePlaying,
                        onToggleFavorite = viewModel::toggleFavorite,
                        onOpenAllRoutes = { activeTab = AppTab.Route },
                        onOpenRecord = { activeTab = AppTab.Record },
                    )
                    AppTab.Route -> RouteScreen(
                        routes = state.routes,
                        favoriteRouteIds = state.favoriteRouteIds,
                        onSelectRoute = selectRoute,
                        onToggleFavorite = viewModel::toggleFavorite,
                    )
                    AppTab.Record -> RecordScreen(
                        state = recordState,
                        savedRouteCount = state.favoriteRouteIds.size,
                        onStartRecording = requestRecording,
                        onStopRecording = finishRecording,
                        onToggleDrive = recordViewModel::toggleExpanded,
                        onMakeVideo = recordViewModel::makeVideo,
                        isDrivingRestricted = state.isDrivingRestricted,
                    )
                    AppTab.Saved -> SavedScreen(
                        routes = state.routes,
                        favoriteRouteIds = state.favoriteRouteIds,
                        onSelectRoute = selectRoute,
                        onToggleFavorite = viewModel::toggleFavorite,
                    )
                    AppTab.My -> MyScreen(
                        profile = state.profile,
                        vehicle = state.vehicle,
                        isDrivingRestricted = state.isDrivingRestricted,
                        onOpenVehicle = openVehicleSheet,
                        onToggleRecommendWhileDriving = viewModel::toggleRecommendWhileDriving,
                        onRestricted = viewModel::showParkingRequired,
                    )
                }

                if (activeDriveRoute == null && state.driveSuggestions.isNotEmpty()) {
                    DriveSuggestionCard(
                        suggestions = state.driveSuggestions,
                        onAccept = { route ->
                            viewModel.dismissDriveSuggestions()
                            activeDriveRouteId = route.id
                        },
                        onDismiss = viewModel::dismissDriveSuggestions,
                        modifier = Modifier
                            .align(Alignment.TopCenter)
                            .padding(24.dp)
                            .widthIn(max = ContentMaxWidth)
                            .fillMaxWidth(),
                    )
                }

                ToastHost(
                    message = state.toastMessage,
                    modifier = Modifier
                        .align(if (activeDriveRoute != null) Alignment.Center else Alignment.BottomCenter)
                        .padding(24.dp),
                )
            }
        }

        if (activeDriveRouteId != null) {
            content(Modifier.fillMaxSize())
        } else if (isWide) {
            Row(Modifier.fillMaxSize()) {
                AppNavRail(activeTab = activeTab, onSelect = { activeTab = it })
                content(Modifier.weight(1f).fillMaxSize())
            }
        } else {
            Column(Modifier.fillMaxSize()) {
                content(Modifier.weight(1f).fillMaxWidth())
                AppBottomNav(activeTab = activeTab, onSelect = { activeTab = it })
            }
        }
    }

    state.routes.firstOrNull { it.id == selectedRouteId }?.let { route ->
        RouteDetailSheet(
            route = route,
            isFavorite = route.id in state.favoriteRouteIds,
            isDrivingRestricted = state.isDrivingRestricted,
            onToggleFavorite = { viewModel.toggleFavorite(route.id) },
            onStartDrive = {
                activeDriveRouteId = route.id
                requestRecording()
            },
            onDismiss = { selectedRouteId = null },
        )
    }

    if (showVehicleSheet) {
        VehicleConnectSheet(
            vehicle = state.vehicle,
            isDrivingRestricted = state.isDrivingRestricted,
            onConnect = connectVehicle,
            onDisconnect = viewModel::disconnectVehicle,
            onDismiss = { showVehicleSheet = false },
        )
    }
}

// 홈 추천에 현재 위치를 쓰려고 앱을 처음 열 때 한 번 위치 권한을 묻는다. 거절하면 서울시청 기준으로 추천.
@Composable
private fun rememberStartupLocationRequest(onGranted: () -> Unit) {
    val context = LocalContext.current
    var asked by rememberSaveable { mutableStateOf(false) }
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) onGranted()
    }
    LaunchedEffect(Unit) {
        if (asked || USE_MOCK_LOCATION) return@LaunchedEffect
        asked = true
        val hasPermission = ContextCompat.checkSelfPermission(context, PleosLocation.PERMISSION) ==
            PackageManager.PERMISSION_GRANTED
        if (!hasPermission) launcher.launch(PleosLocation.PERMISSION)
    }
}

/** 위치 권한을 확인(필요하면 요청)한 뒤 기록을 시작하는 동작을 돌려준다. */
@Composable
private fun rememberRecordingStarter(onStart: () -> Unit, onDenied: () -> Unit): () -> Unit {
    val context = LocalContext.current
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) onStart() else onDenied()
    }
    return {
        val hasPermission = ContextCompat.checkSelfPermission(context, PleosLocation.PERMISSION) ==
            PackageManager.PERMISSION_GRANTED
        if (USE_MOCK_LOCATION || hasPermission) onStart() else launcher.launch(PleosLocation.PERMISSION)
    }
}

@Composable
private fun ToastHost(message: String?, modifier: Modifier = Modifier) {
    // 사라지는 애니메이션 동안에도 마지막 문구를 유지한다.
    var lastMessage by remember { mutableStateOf("") }
    if (message != null) lastMessage = message

    AnimatedVisibility(
        visible = message != null,
        enter = fadeIn(),
        exit = fadeOut(),
        modifier = modifier,
    ) {
        AppToast(lastMessage)
    }
}
