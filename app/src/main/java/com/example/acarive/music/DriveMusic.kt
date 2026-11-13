package com.example.acarive.music

import android.content.Context
import android.util.Log
import com.acarive.scenic.context.ContextAggregator
import com.acarive.scenic.context.KmaWeatherSource
import com.acarive.scenic.context.MockWeatherSource
import com.acarive.scenic.context.SpeedSource
import com.acarive.scenic.context.WeatherInfo
import com.acarive.scenic.context.WeatherSource
import com.acarive.scenic.data.TrackRepository
import com.acarive.scenic.domain.DrivingContext
import com.acarive.scenic.domain.GenreGroup
import com.acarive.scenic.domain.Recommendation
import com.acarive.scenic.domain.Recommender
import com.acarive.scenic.domain.SpeedBand
import com.acarive.scenic.domain.UserPreference
import com.acarive.scenic.domain.Weather
import com.acarive.scenic.player.SpotifyPlayer
import com.acarive.scenic.session.DriveSession
import com.example.acarive.BuildConfig
import com.example.acarive.data.model.NowPlaying
import com.example.acarive.location.LocationPoint
import com.spotify.android.appremote.api.SpotifyAppRemote
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import com.acarive.scenic.player.NowPlaying as PlayerState
import kotlinx.coroutines.withTimeoutOrNull

private const val TAG = "DriveMusic"
private const val REDIRECT_URI = "acarive://spotify-auth"
private const val MIX_NAME = "드라이브 추천"

// TODO(music): 음악 취향 설정 화면이 생기면 거기서 받기. 지금은 팀원 테스트(DriveViewModel)와 같은 기본값
private val DEFAULT_GENRES = setOf(GenreGroup.CHILL, GenreGroup.POP)

enum class SpotifyStatus { NotInstalled, Disconnected, Connecting, Connected }

// 주행 음악. Spotify 연결을 관리하고, 추천 엔진(scenic DriveSession)이 속도·날씨·시간대에 맞는 곡을 골라 튼다.
// 소리는 Spotify 앱이 내고 이 앱은 원격 조종만 한다.
class DriveMusic(
    private val context: Context,
    private val scope: CoroutineScope,
    speedKmh: StateFlow<Int?>,
    // 현재 위치 (날씨 조회용). 아직 못 받았으면 null 이고 날씨는 서울시청 기준으로 조회한다
    location: () -> LocationPoint?,
    // 사용자가 끊지 않았는데 연결이 끊겼을 때 (Spotify 앱 종료 등)
    private val onConnectionLost: () -> Unit,
) {
    private val player = SpotifyPlayer(context, BuildConfig.SPOTIFY_CLIENT_ID, REDIRECT_URI)

    // 위치로 계산한 속도 (AppViewModel). 못 받으면 null 이고 추천은 속도 없이 한다
    private val speedSource = object : SpeedSource {
        override suspend fun currentSpeedKmh(): Double? = speedKmh.value?.toDouble()
    }

    // 기상청 단기예보. KMA_SERVICE_KEY(local.properties)가 없으면 홈 날씨(18° 가벼운 비)와 맞춘 Mock 으로 대신한다
    private val weatherSource: WeatherSource =
        if (BuildConfig.KMA_SERVICE_KEY.isBlank()) {
            Log.w(TAG, "local.properties 에 KMA_SERVICE_KEY 가 없어 Mock 날씨를 쓴다")
            MockWeatherSource(WeatherInfo(Weather.RAIN, 18.0))
        } else {
            KmaWeatherSource(BuildConfig.KMA_SERVICE_KEY, location)
        }

    private val tracks by lazy { TrackRepository(context).load() }

    private var sessionScope: CoroutineScope? = null
    private val recommendation = MutableStateFlow<Recommendation?>(null)
    private val drivingContext = MutableStateFlow(DrivingContext())

    private val _status = MutableStateFlow(if (isInstalled()) SpotifyStatus.Disconnected else SpotifyStatus.NotInstalled)
    val status: StateFlow<SpotifyStatus> = _status.asStateFlow()

    val isPlaying: StateFlow<Boolean> = player.nowPlaying
        .map { it != null && !it.isPaused }
        .stateIn(scope, SharingStarted.Eagerly, false)

    // 홈 플레이리스트 카드
    val card: StateFlow<NowPlaying> = combine(status, player.nowPlaying, recommendation, drivingContext, ::toCard)
        .stateIn(scope, SharingStarted.Eagerly, toCard(status.value, null, null, DrivingContext()))

    init {
        scope.launch {
            player.connected.collect { connected ->
                if (!connected && _status.value == SpotifyStatus.Connected) {
                    stopSession()
                    _status.value = SpotifyStatus.Disconnected
                    onConnectionLost()
                }
            }
        }
    }

    //spotify 테스트용
    var lastError: String? = null
        private set
    fun setActivity(activity: Context?) { player.activityContext = activity }
    fun isInstalled(): Boolean = SpotifyAppRemote.isSpotifyInstalled(context)

    // 연결되면 true. 앱 시작 때 자동 재연결은 showAuthView = false 로 조용히 시도한다
    suspend fun connect(showAuthView: Boolean): Boolean {
        if (!isInstalled()) {
            _status.value = SpotifyStatus.NotInstalled
            return false
        }
        when (_status.value) {
            SpotifyStatus.Connected -> return true
            SpotifyStatus.Connecting -> return false
            else -> Unit
        }
        if (BuildConfig.SPOTIFY_CLIENT_ID.isBlank()) {
            Log.w(TAG, "local.properties 에 SPOTIFY_CLIENT_ID 가 없어 연결하지 않는다")
            _status.value = SpotifyStatus.Disconnected
            return false
        }
        _status.value = SpotifyStatus.Connecting
        val result = withTimeoutOrNull(30_000L) { player.connect(showAuthView) }
            ?: Result.failure(Exception("시간 초과(30초): 권한 화면이 안 뜨거나 Spotify 앱이 응답하지 않아요"))
        if (result.isFailure) player.release() // 늦게 붙는 연결이 남지 않게 정리
        lastError = result.exceptionOrNull()?.let { "${it::class.java.simpleName}: ${it.message}" }
        result.exceptionOrNull()?.let { Log.w(TAG, "Spotify 연결 실패", it) }
        _status.value = if (result.isSuccess) SpotifyStatus.Connected else SpotifyStatus.Disconnected
        return result.isSuccess
    }

    fun disconnect() {
        stopSession()
        // 상태를 먼저 바꿔서 연결 끊김 안내가 뜨지 않게 한다
        _status.value = if (isInstalled()) SpotifyStatus.Disconnected else SpotifyStatus.NotInstalled
        player.release()
    }

    // 재생 버튼. 처음 누르면 추천을 시작해 첫 곡을 틀고, 그 뒤로는 일시정지/다시 재생
    fun togglePlay() {
        Log.d(TAG, "startSession 호출")
        if (_status.value != SpotifyStatus.Connected) return
        val now = player.nowPlaying.value
        when {
            sessionScope == null -> startSession()
            now == null -> Unit // 첫 곡을 고르는 중
            now.isPaused -> player.resume()
            else -> player.pause()
        }
    }

    private fun startSession() {
        val parent = scope.coroutineContext[Job]
        val s = CoroutineScope(scope.coroutineContext + SupervisorJob(parent)).also { sessionScope = it }
        val aggregator = ContextAggregator(speedSource, weatherSource)
        val session = DriveSession(
            recommender = Recommender(tracks),
            aggregator = aggregator,
            player = player,
            preference = UserPreference(DEFAULT_GENRES),
            scope = s,
        )
        aggregator.start(s)
        session.start()
        s.launch { session.current.collect { recommendation.value = it } }
        s.launch { aggregator.context.collect { drivingContext.value = it } }
    }

    private fun stopSession() {
        sessionScope?.cancel()
        sessionScope = null
        recommendation.value = null
        drivingContext.value = DrivingContext()
    }

    fun release() {
        stopSession()
        player.release()
    }
}

private fun toCard(
    status: SpotifyStatus,
    now: PlayerState?,
    rec: Recommendation?,
    ctx: DrivingContext,
): NowPlaying = when {
    status == SpotifyStatus.NotInstalled ->
        NowPlaying("Spotify", "Spotify 앱이 필요해요", "설치하면 주행에 맞는 음악을 틀어드려요", emptyList())
    status == SpotifyStatus.Connecting ->
        NowPlaying("Spotify", "Spotify에 연결하는 중이에요", "잠시만 기다려 주세요", emptyList())
    status == SpotifyStatus.Disconnected ->
        NowPlaying("Spotify", "Spotify를 연결해 주세요", "재생을 누르면 연결하고 곡을 골라드려요", emptyList())
    now == null ->
        NowPlaying(MIX_NAME, "지금 상황에 맞는 곡을 골라드릴게요", "재생을 눌러 시작하세요", ctx.tags())
    else -> NowPlaying(
        mixName = MIX_NAME,
        title = now.title.ifBlank { rec?.scored?.track?.title.orEmpty() },
        // 추천 이유는 추천 엔진이 고른 곡일 때만 있다 (사용자가 Spotify 에서 직접 바꾸면 null)
        subtitle = listOfNotNull(now.artist.ifBlank { null }, rec?.reason).joinToString(" · "),
        contextTags = ctx.tags(),
    )
}

// 추천에 쓴 주행 환경을 태그로
private fun DrivingContext.tags(): List<String> = listOfNotNull(
    weather?.label,
    timeSlot?.label,
    when (speedBand) {
        SpeedBand.JAM -> "느린 흐름"
        SpeedBand.CITY -> "시내 주행"
        SpeedBand.ROAD -> "국도 주행"
        SpeedBand.HIGHWAY -> "고속 주행"
        null -> null
    },
).map { "# $it" }