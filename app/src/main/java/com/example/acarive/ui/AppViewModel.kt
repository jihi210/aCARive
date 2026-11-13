package com.example.acarive.ui

import android.app.Application
import android.content.pm.PackageManager
import android.util.Log
import androidx.compose.runtime.snapshotFlow
import androidx.core.content.ContextCompat
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.acarive.data.SampleData
import com.example.acarive.data.course.CourseCatalog
import com.example.acarive.data.course.CourseCatalogRepository
import com.example.acarive.data.course.CourseRecommender
import com.example.acarive.data.course.heroFor
import com.example.acarive.data.course.toDriveSuggestion
import com.example.acarive.data.course.toRoute
import com.example.acarive.data.course.CourseProgress
import com.example.acarive.data.course.keyStops
import com.example.acarive.data.favorite.FavoriteRepository
import com.example.acarive.data.model.VehicleInfo
import com.example.acarive.data.model.Weather
import com.example.acarive.data.model.WeatherCondition
import com.example.acarive.BuildConfig
import com.acarive.scenic.context.KmaWeatherSource
import com.acarive.scenic.context.MockWeatherSource
import com.acarive.scenic.context.WeatherInfo
import com.acarive.scenic.context.WeatherSource
import com.acarive.scenic.domain.Weather as MusicWeather
import com.example.acarive.data.profile.ProfileRepository
import com.example.acarive.location.DemoDrive
import com.example.acarive.location.LocationPoint
import com.example.acarive.music.DriveMusic
import com.example.acarive.music.SpotifyStatus
import com.example.acarive.navi.CarNavigation
import com.example.acarive.navi.CarNavigationListener
import com.example.acarive.navi.NaviStatus
import com.example.acarive.navi.NaviStop
import ai.pleos.playground.navi.constants.NaviErrorCode
import com.example.acarive.location.PleosLocation
import com.example.acarive.location.mockRoute
import com.example.acarive.ui.record.USE_MOCK_LOCATION
import com.example.acarive.vehicle.DrivingRestriction
import com.example.acarive.vehicle.PleosVehicle
import com.example.acarive.vehicle.VehicleData
import com.example.acarive.vehicle.label
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.IOException
import kotlin.math.roundToInt

private const val TOAST_DURATION_MS = 2_200L
private const val TAG = "AppViewModel"

// 위치를 못 받았을 때 추천 기준 지점
private val DEFAULT_ORIGIN = LocationPoint(37.5665, 126.9780)
private const val DEFAULT_ORIGIN_LABEL = "서울시청"
// 이만큼 움직였을 때만 추천을 다시 계산한다
private const val RECOMMEND_MOVE_KM = 5.0
// 홈 날씨를 다시 받는 간격 (기상청 초단기실황은 1시간마다 갱신)
private const val WEATHER_INTERVAL_MS = 20 * 60_000L

// 주행 중 추천: 30분마다 주변 코스 2개. 주변에 없으면 잠깐 뒤 다시 본다
private const val SUGGEST_INTERVAL_MS = 30 * 60_000L
private const val SUGGEST_RETRY_MS = 2 * 60_000L
private const val SUGGEST_COUNT = 2
// 데모 주행 중에는 시연할 수 있게 훨씬 자주 추천한다
private const val DEMO_SUGGEST_INTERVAL_MS = 20_000L
private const val DEMO_SUGGEST_RETRY_MS = 10_000L
// 데모 자유 주행(기록만)은 가장 가까운 코스 출발점으로 달린다. 이보다 가까운 곳은 이미 도착한 걸로 본다
private const val DEMO_MIN_TARGET_KM = 2.0

// true 면 Pleos 차량 서비스 대신 목업 차량을 연결한다. 일반 폰·에뮬레이터에서 화면 확인할 때만 켠다.
const val USE_MOCK_VEHICLE = false

class AppViewModel(application: Application) : AndroidViewModel(application) {

    // TODO(backend): 초기 상태를 SampleData 대신 API 응답으로 채우기
    //  - 즐겨찾기 목록(지금은 기기에만 저장), 연결된 차량
    //  - 추천: assets 카탈로그 + 위치·취향으로 앱에서 정렬 (CourseRecommender). 타 사용자 기록은 아직
    //  - 날씨: 현재 위치 기반 날씨 API
    //  - 로딩/에러 상태도 AppUiState 에 추가 필요
    private val _uiState = MutableStateFlow(AppUiState())
    val uiState: StateFlow<AppUiState> = _uiState.asStateFlow()

    private var toastJob: Job? = null

    private val courseRepository = CourseCatalogRepository(application)
    private val profileRepository = ProfileRepository(application)
    private val favoriteRepository = FavoriteRepository(application)
    private var catalog: CourseCatalog? = null

    // 추천 기준 위치. 실제 위치를 받기 전에는 기본 지점
    private var origin: LocationPoint = if (USE_MOCK_LOCATION) mockRoute.first() else DEFAULT_ORIGIN
    private var hasRealOrigin = USE_MOCK_LOCATION
    // 가장 최근 위치. 주행 중 추천에 쓴다
    private var currentLocation: LocationPoint? = null
    private var location: PleosLocation? = null
    // Pleos 에서 받은 실제 위치·속도. 데모가 켜져 있으면 대신 DemoDrive 값을 쓴다 (watchLocation)
    private val realLocation = MutableStateFlow<LocationPoint?>(null)
    private val realSpeed = MutableStateFlow<Int?>(null)
    // 위치 변화로 계산한 속도. 차량 카드·음악 추천에 쓴다
    private val speedKmh = MutableStateFlow<Int?>(null)

    // 기록 중이거나 코스 안내 중인지 (setDriving)
    private var isDriving = false
    private var suggestJob: Job? = null
    // 지금 안내 중인 코스. 추천에서 뺀다
    private var drivingRouteId: String? = null
    // 이번 주행에서 이미 보여준 코스는 다시 추천하지 않는다
    private val suggestedIds = mutableSetOf<String>()

    private val drivingRestriction = DrivingRestriction(application)

    // 코스 길안내는 차량 내비가 한다. 차량 내비가 없는 기기면 null
    private val carNavigation: CarNavigation? =
        if (CarNavigation.isAvailable(application)) CarNavigation(application, NaviEvents()) else null

    // 기상청 단기예보. 홈 날씨와 음악 추천이 같이 쓴다.
    // KMA_SERVICE_KEY(local.properties)가 없으면 18° 비로 고정된 Mock
    private val weatherSource: WeatherSource =
        if (BuildConfig.KMA_SERVICE_KEY.isBlank()) {
            Log.w(TAG, "local.properties 에 KMA_SERVICE_KEY 가 없어 Mock 날씨를 쓴다")
            MockWeatherSource(WeatherInfo(MusicWeather.RAIN, 18.0))
        } else {
            KmaWeatherSource(BuildConfig.KMA_SERVICE_KEY, location = { currentLocation ?: origin })
        }
    private var weatherJob: Job? = null

    private val music = DriveMusic(application, viewModelScope, speedKmh, weatherSource) {
        showToast("Spotify 연결이 끊겼어요")
    }

    private var vehicle: PleosVehicle? = null
    private var vehicleJob: Job? = null

    init {
        watchDrivingRestriction()
        loadProfile()
        loadFavorites()
        watchMusic()
        loadCourses()
        watchLocation()
        startLocation()
        refreshWeather()
    }

    // 위치 권한이 있으면 위치를 받기 시작한다. 권한을 받은 뒤 다시 불러도 된다.
    fun startLocation() {
        if (USE_MOCK_LOCATION || location != null) return
        val app = getApplication<Application>()
        val granted = ContextCompat.checkSelfPermission(app, PleosLocation.PERMISSION) ==
            PackageManager.PERMISSION_GRANTED
        if (!granted) return
        val source = PleosLocation(app).also { location = it }
        source.start()
        viewModelScope.launch {
            snapshotFlow { source.latest }.collect { realLocation.value = it }
        }
        viewModelScope.launch {
            snapshotFlow { source.speedKmh }.collect { realSpeed.value = it }
        }
    }

    // 데모가 켜져 있으면 실제 위치 대신 데모 위치·속도를 쓴다
    private fun watchLocation() {
        viewModelScope.launch {
            combine(DemoDrive.state, realLocation) { demo, real -> if (demo.isActive) demo.position else real }
                .filterNotNull()
                .distinctUntilChanged()
                .collect(::onLocation)
        }
        viewModelScope.launch {
            combine(DemoDrive.state, realSpeed) { demo, real -> if (demo.isActive) demo.speedKmh else real }
                .distinctUntilChanged()
                .collect { speed ->
                    speedKmh.value = speed
                    _uiState.update { it.copy(speedKmh = speed) }
                }
        }
        viewModelScope.launch {
            DemoDrive.state.map { it.start }.distinctUntilChanged().collect { start ->
                _uiState.update { it.copy(demoStart = start) }
            }
        }
    }

    // 데모: 지도에서 찍은 곳을 현재 위치로 쓴다. 주행 중이면 거기서부터 다시 달린다
    fun setDemoStart(point: LocationPoint) {
        DemoDrive.setStart(point)
        updateDemoPath()
        showToast("데모 위치를 정했어요")
    }

    // 데모를 끄고 실제 위치로 돌아간다. 실제 위치가 없으면 기본 지점으로 추천한다
    fun clearDemoStart() {
        DemoDrive.clear()
        if (realLocation.value == null && !USE_MOCK_LOCATION) {
            origin = DEFAULT_ORIGIN
            hasRealOrigin = false
            currentLocation = null
            recommend()
            refreshWeather()
        }
        showToast("데모를 껐어요")
    }

    // 데모 주행 경로. 코스 안내 중이면 그 코스 경유지를, 기록만 하면 가장 가까운 코스 출발점으로 달린다.
    // 출발점에 가까워지면 주행 중 추천 카드가 뜨고, 고르면 그 코스로 바뀐다
    private fun updateDemoPath() {
        if (!DemoDrive.isActive) return
        val here = DemoDrive.position
        val courses = catalog?.courses
        if (!isDriving || here == null || courses == null) {
            DemoDrive.stopDriving()
            return
        }
        val path = drivingRouteId?.let { id -> courses.find { it.id == id }?.waypoints }
            ?: courses.mapNotNull { it.start }
                .filter { CourseRecommender.distanceKm(here, it) >= DEMO_MIN_TARGET_KM }
                .minByOrNull { CourseRecommender.distanceKm(here, it) }
                ?.let(::listOf)
        if (path.isNullOrEmpty()) DemoDrive.stopDriving() else DemoDrive.driveAlong(path)
    }

    private fun onLocation(point: LocationPoint) {
        currentLocation = point
        updateProgress(point)
        if (hasRealOrigin && CourseRecommender.distanceKm(origin, point) < RECOMMEND_MOVE_KM) return
        origin = point
        hasRealOrigin = true
        recommend()
        refreshWeather()
    }

    // 홈 날씨. 지금 위치로 바로 받고 그 뒤로는 주기적으로 다시 받는다. 실패하면 직전 날씨를 그대로 둔다
    private fun refreshWeather() {
        weatherJob?.cancel()
        weatherJob = viewModelScope.launch {
            while (true) {
                weatherSource.current()?.let { info -> _uiState.update { it.copy(weather = info.toHomeWeather()) } }
                delay(WEATHER_INTERVAL_MS)
            }
        }
    }

    private fun recommend() {
        val catalog = catalog ?: return
        val preferred = CourseRecommender.themesFromPreferences(_uiState.value.profile.preferences)
        val recs = CourseRecommender.recommend(catalog.courses, origin, preferred)
        val originLabel = if (hasRealOrigin) null else DEFAULT_ORIGIN_LABEL
        _uiState.update {
            it.copy(routes = recs.map { r -> r.toRoute() }, hero = recs.firstOrNull()?.let { r -> heroFor(r, originLabel) })
        }
    }

    // 기기에 저장된 취향·설정을 계속 받는다. 취향이 바뀌면 추천도 다시 계산한다.
    private fun loadProfile() {
        viewModelScope.launch {
            profileRepository.profile.collect { profile ->
                val preferencesChanged = profile.preferences != _uiState.value.profile.preferences
                _uiState.update { it.copy(profile = profile) }
                if (preferencesChanged) recommend()
            }
        }
    }

    // 기기에 저장된 즐겨찾기를 계속 받는다. toggleFavorite 로 저장하면 여기로 다시 들어온다
    private fun loadFavorites() {
        viewModelScope.launch {
            favoriteRepository.routeIds.collect { ids ->
                _uiState.update { it.copy(favoriteRouteIds = ids) }
            }
        }
    }

    private fun loadCourses() {
        viewModelScope.launch {
            try {
                val loaded = courseRepository.load()
                catalog = loaded
                _uiState.update { it.copy(courseStarts = loaded.courses.mapNotNull { c -> c.start }) }
                recommend()
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "코스 카탈로그 읽기 실패", e)
                showToast("코스를 불러오지 못했어요")
            }
        }
    }

    // 차량이 주는 주행 중 UX 제한을 화면 상태로. 차량이 아니면 항상 false
    private fun watchDrivingRestriction() {
        drivingRestriction.start()
        viewModelScope.launch {
            drivingRestriction.restricted.collect { restricted ->
                _uiState.update { it.copy(isDrivingRestricted = restricted) }
            }
        }
    }

    /** 주행 중 막힌 기능을 눌렀을 때 안내 */
    fun showParkingRequired() {
        showToast("주차 후 이용할 수 있어요")
    }

    fun toggleDriveMode() {
        // TODO(backend): 드라이브/일반 주행 구분을 서버에 저장
        _uiState.update { it.copy(isDriveMode = !it.isDriveMode) }
    }

    // Spotify 상태와 홈 카드를 화면 상태로. 연결해 둔 적이 있으면 앱을 켤 때 조용히 다시 연결한다
    private fun watchMusic() {
        viewModelScope.launch {
            combine(music.status, music.card, music.isPlaying) { status, card, playing -> Triple(status, card, playing) }
                .collect { (status, card, playing) ->
                    _uiState.update { it.copy(spotify = status, nowPlaying = card, isPlaying = playing) }
                }
        }
        viewModelScope.launch {
            if (profileRepository.profile.first().spotifyConnected) music.connect(showAuthView = false)
        }
    }

    // 홈 카드 재생 버튼. 연결 전이면 연결부터 하고 바로 추천 곡을 튼다.
    // 오디오 포커스는 앱에서 따로 처리하지 않는다. 소리는 Spotify 앱이 내고(App Remote 는 원격 조종만),
    // 길안내 음성이 나오면 차량 오디오(CarAudioService)가 음악 볼륨을 자동으로 낮춘다.
    // 이 앱이 포커스를 요청하면 오히려 Spotify 재생을 끊게 된다.
    fun togglePlaying() {
        viewModelScope.launch {
            if (music.status.value != SpotifyStatus.Connected && !connectSpotify()) return@launch
            music.togglePlay()
        }
    }

    private val _spotifyAuthRequests = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    val spotifyAuthRequests: SharedFlow<Unit> = _spotifyAuthRequests.asSharedFlow()
    private var spotifyAuthRequested = false

    fun onSpotifyAuthResult(success: Boolean) {
        if (!success) {
            showToast("Spotify 승인이 취소됐어요")
            return
        }
        viewModelScope.launch { if (connectSpotify()) showToast("Spotify가 연결됐어요") }
    }

    // 마이 화면 "Spotify 연결". 연결돼 있으면 해제한다
    fun toggleSpotify() {
        if (music.status.value == SpotifyStatus.Connected) {
            music.disconnect()
            saveProfile("Spotify 연결을 해제했어요") { setSpotifyConnected(false) }
            return
        }
        viewModelScope.launch {
            if (connectSpotify()) showToast("Spotify가 연결됐어요")
        }
    }

    // 실패하면 안내를 띄우고 false. 성공하면 다음에 앱을 켤 때 자동으로 연결하도록 저장한다
    private suspend fun connectSpotify(): Boolean {
        if (music.status.value == SpotifyStatus.Connecting) return false
        if (!music.isInstalled()) {
            showToast("Spotify 앱을 먼저 설치해 주세요")
            return false
        }
        if (!music.connect(showAuthView = false)) {
            val needsAuth = music.lastError?.startsWith("UserNotAuthorizedException") == true
            if (needsAuth && !spotifyAuthRequested) {
                spotifyAuthRequested = true
                _spotifyAuthRequests.tryEmit(Unit)
                return false
            }
            showToast("Spotify에 연결하지 못했어요")
            return false
        }
        spotifyAuthRequested = false
        try {
            profileRepository.setSpotifyConnected(true)
        } catch (e: IOException) {
            Log.e(TAG, "설정 저장 실패", e)
        }
        return true
    }

    // TODO(backend): 서버가 생기면 즐겨찾기 추가/삭제 API 도 호출
    fun toggleFavorite(routeId: String) {
        viewModelScope.launch {
            try {
                favoriteRepository.toggle(routeId)
            } catch (e: IOException) {
                Log.e(TAG, "즐겨찾기 저장 실패", e)
                showToast("즐겨찾기를 저장하지 못했어요")
            }
        }
    }

    fun toggleRecommendWhileDriving() {
        val enabled = !_uiState.value.profile.recommendWhileDriving
        if (!enabled) dismissDriveSuggestions()
        saveProfile(if (enabled) "주행 중 코스 추천을 켰어요" else "주행 중 코스 추천을 껐어요") {
            setRecommendWhileDriving(enabled)
        }
    }

    // 저장하면 loadProfile 이 받아서 홈 추천도 다시 계산한다
    fun savePreferences(preferences: List<String>) {
        saveProfile("취향을 저장했어요") { setPreferences(preferences) }
    }

    private fun saveProfile(doneMessage: String, save: suspend ProfileRepository.() -> Unit) {
        viewModelScope.launch {
            try {
                profileRepository.save()
                showToast(doneMessage)
            } catch (e: IOException) {
                Log.e(TAG, "설정 저장 실패", e)
                showToast("설정을 저장하지 못했어요")
            }
        }
    }

    // 기록 중이거나 코스 안내 중이면 주행 중으로 보고 주변 코스를 추천한다.
    // 화면에서 기록/코스 상태가 바뀔 때마다 부른다. 코스만 바뀌면 타이머는 그대로 둔다.
    fun setDriving(active: Boolean, currentRouteId: String?) {
        val routeChanged = currentRouteId != drivingRouteId
        val changed = active != isDriving || routeChanged
        drivingRouteId = currentRouteId
        isDriving = active
        if (routeChanged) {
            if (currentRouteId != null) startCourse(currentRouteId) else endCourse()
        }
        if (changed) updateDemoPath()
        if (active == (suggestJob != null)) return
        suggestJob?.cancel()
        suggestJob = null
        dismissDriveSuggestions()
        if (!active) return
        suggestedIds.clear()
        suggestJob = viewModelScope.launch {
            var found = true
            while (true) {
                delay(suggestDelay(found))
                found = suggestNearby()
            }
        }
    }

    // 직전에 추천을 띄웠으면 긴 간격, 주변에 없었으면 짧게 다시 본다
    private fun suggestDelay(lastFound: Boolean): Long = when {
        DemoDrive.isActive -> if (lastFound) DEMO_SUGGEST_INTERVAL_MS else DEMO_SUGGEST_RETRY_MS
        lastFound -> SUGGEST_INTERVAL_MS
        else -> SUGGEST_RETRY_MS
    }

    // 주변에 코스가 있으면 카드를 띄우고 true. 설정이 꺼졌거나 주변에 없으면 false
    private fun suggestNearby(): Boolean {
        val state = _uiState.value
        if (!state.profile.recommendWhileDriving) return false
        val catalog = catalog ?: return false
        val here = currentLocation ?: origin.takeIf { hasRealOrigin } ?: return false
        val preferred = CourseRecommender.themesFromPreferences(state.profile.preferences)
        val exclude = suggestedIds + listOfNotNull(drivingRouteId)
        val recs = CourseRecommender.nearby(catalog.courses, here, preferred, exclude, SUGGEST_COUNT)
        if (recs.isEmpty()) return false
        suggestedIds += recs.map { it.course.id }
        // 답하지 않은 이전 카드는 새 추천으로 바꾼다
        _uiState.update { it.copy(driveSuggestions = recs.map { r -> r.toDriveSuggestion() }) }
        return true
    }

    // "괜찮아요" 또는 코스를 골랐을 때
    fun dismissDriveSuggestions() {
        _uiState.update { it.copy(driveSuggestions = emptyList()) }
    }

    // 현대차만 지원. 로그인 없이 Pleos Connect 차량 서비스에서 바로 데이터를 받는다.
    // 차량 권한(PleosVehicle.PERMISSIONS)은 화면에서 먼저 요청한다.
    fun connectVehicle() {
        if (USE_MOCK_VEHICLE) {
            _uiState.update { it.copy(vehicle = SampleData.connectedVehicle) }
            showToast("목업 차량이 연결됐어요")
            return
        }
        val app = getApplication<Application>()
        if (!PleosVehicle.isAvailable(app)) {
            showToast("이 기기에서는 차량 데이터를 받을 수 없어요")
            return
        }
        if (vehicle != null) return
        val source = PleosVehicle(app).also { vehicle = it }
        source.start()
        vehicleJob = viewModelScope.launch {
            combine(source.data, speedKmh) { data, speed -> data.toVehicleInfo(speed) }
                .collect { info -> _uiState.update { it.copy(vehicle = info) } }
        }
        showToast("현대 차량이 연결됐어요")
    }

    fun disconnectVehicle() {
        stopVehicle()
        _uiState.update { it.copy(vehicle = null) }
        showToast("차량 연결을 해제했어요")
    }

    private fun stopVehicle() {
        vehicleJob?.cancel()
        vehicleJob = null
        vehicle?.stop()
        vehicle = null
    }

    // 코스를 차량 내비로 보내고 진행 상황을 계산하기 시작한다. 안내 중에 코스를 바꿔도 여기로 온다
    private fun startCourse(routeId: String) {
        val stops = catalog?.courses?.find { it.id == routeId }?.keyStops().orEmpty()
        if (stops.isEmpty()) {
            _uiState.update { it.copy(driveProgress = null, naviStatus = NaviStatus.Failed) }
            return
        }
        val progress = CourseProgress.start(stops, currentLocation ?: origin.takeIf { hasRealOrigin })
        val nav = carNavigation
        _uiState.update {
            it.copy(driveProgress = progress, naviStatus = if (nav == null) NaviStatus.Unavailable else NaviStatus.Sending)
        }
        nav ?: return
        viewModelScope.launch {
            val sent = nav.sendRoute(stops.map { NaviStop(it.name, it.location) })
            // 그 사이 주행을 끝냈거나 코스를 바꿨으면 무시
            if (drivingRouteId != routeId) return@launch
            // 내비 오류(RouteFailed)가 먼저 왔으면 덮어쓰지 않는다
            _uiState.update {
                if (it.naviStatus != NaviStatus.Sending) it
                else it.copy(naviStatus = if (sent) NaviStatus.Sent else NaviStatus.Failed)
            }
            showToast(if (sent) "차량 내비로 코스를 보냈어요" else "차량 내비로 보내지 못했어요")
        }
    }

    private fun endCourse() {
        carNavigation?.cancelRoute()
        _uiState.update { it.copy(driveProgress = null, naviStatus = NaviStatus.Idle) }
    }

    private fun updateProgress(here: LocationPoint) {
        val progress = _uiState.value.driveProgress ?: return
        val updated = CourseProgress.update(progress.stops, progress.nextIndex, here)
        _uiState.update { it.copy(driveProgress = updated) }
        if (!progress.isFinished && updated.isFinished) showToast("코스를 다 달렸어요")
    }

    // 주행 화면 "내비 보기"
    fun openCarNavigation() {
        val nav = carNavigation ?: return showToast("이 기기에는 차량 내비가 없어요")
        viewModelScope.launch {
            if (!nav.showNavigation()) showToast("차량 내비를 열지 못했어요")
        }
    }

    // 내비 콜백은 다른 스레드로 올 수 있어서 메인에서 처리한다
    private inner class NaviEvents : CarNavigationListener {
        override fun onRouteFailed(code: NaviErrorCode) {
            viewModelScope.launch(Dispatchers.Main) {
                if (drivingRouteId == null) return@launch
                _uiState.update { it.copy(naviStatus = NaviStatus.RouteFailed) }
                showToast(
                    when (code) {
                        NaviErrorCode.CAUSE_SEARCH_ROUTE_OVER_DISTANCE -> "코스가 너무 멀어 내비가 경로를 찾지 못했어요"
                        NaviErrorCode.CAUSE_SEARCH_ROUTE_TOO_SHORT_DISTANCE -> "출발점이 너무 가까워 내비가 경로를 찾지 못했어요"
                        else -> "차량 내비가 경로를 찾지 못했어요"
                    },
                )
            }
        }

        // 내비 기준으로 목적지에 닿으면 앱 진행도 끝낸다
        override fun onArrived() {
            viewModelScope.launch(Dispatchers.Main) {
                val progress = _uiState.value.driveProgress ?: return@launch
                if (progress.isFinished) return@launch
                _uiState.update { it.copy(driveProgress = progress.copy(nextIndex = progress.stops.size, remainingKm = 0.0, remainingMinutes = 0)) }
                showToast("코스를 다 달렸어요")
            }
        }
    }

    override fun onCleared() {
        carNavigation?.release()
        music.release()
        drivingRestriction.stop()
        location?.stop()
        stopVehicle()
    }

    fun showToast(message: String) {
        toastJob?.cancel()
        _uiState.update { it.copy(toastMessage = message) }
        toastJob = viewModelScope.launch {
            delay(TOAST_DURATION_MS)
            _uiState.update { it.copy(toastMessage = null) }
        }
    }
}

// 기상청 날씨 -> 홈 카드 문구
private fun WeatherInfo.toHomeWeather(): Weather = Weather(
    temperature = tempC.roundToInt(),
    description = weather.label,
    condition = when (weather) {
        MusicWeather.SUNNY -> WeatherCondition.Sunny
        MusicWeather.CLOUDY -> WeatherCondition.Cloudy
        MusicWeather.RAIN -> WeatherCondition.Rain
        MusicWeather.SNOW -> WeatherCondition.Snow
    },
)

// 차량 SDK 값 -> 화면용. 배터리 값이 오면 EV 로 본다.
private fun VehicleData.toVehicleInfo(speedKmh: Int?): VehicleInfo = VehicleInfo(
    // 차종 이름은 SDK 에서 주지 않는다
    name = "내 현대차",
    statusLabel = "실시간 수신 중",
    vinSuffix = vin?.takeLast(4),
    gear = gear?.label,
    speedKmh = speedKmh,
    energyLabel = if (evBatteryPercent != null) "배터리" else "연료",
    energyPercent = evBatteryPercent ?: fuelPercent,
    rangeKm = rangeMeters?.let { (it / 1000).roundToInt() },
)
