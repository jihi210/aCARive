package com.example.acarive.ui.record

import android.app.Application
import android.os.Environment
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.acarive.car.CarModel
import com.example.acarive.car.CarRepository
import com.example.acarive.data.drive.DriveDatabase
import com.example.acarive.data.drive.RoutePoint
import com.example.acarive.data.drive.saveDrive
import com.example.acarive.data.drive.toDriveSummary
import com.example.acarive.data.model.DriveSummary
import com.example.acarive.location.DemoDrive
import com.example.acarive.location.LocationPoint
import com.example.acarive.location.PleosLocation
import com.example.acarive.location.mockRoute
import com.example.acarive.video.makeMapBackground
import com.example.acarive.video.makeRouteVideo
import com.example.acarive.video.saveVideoToDownloads
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.File
import java.time.Year

/** true 면 실제 위치 대신 [mockRoute]를 기록한다. Pleos 위치를 받을 수 없는 기기에서 테스트할 때만 켠다. */
const val USE_MOCK_LOCATION = false

private const val SAMPLE_INTERVAL_MS = 1_000L
private const val TAG = "DriveRecord"

data class DriveRecordUiState(
    val isRecording: Boolean = false,
    val recordedCount: Int = 0,
    val summary: DriveSummary = DriveSummary(Year.now().value, 0.0, 0, "0분", emptyList()),
    val expandedSessionId: Long? = null,
    val expandedPoints: List<RoutePoint> = emptyList(),
    val video: DriveVideoState = DriveVideoState.Idle,
    /** 추억 영상에 등장할 차 (마지막으로 고른 차를 기억한다) */
    val selectedCar: CarModel = CarModel.DEFAULT,
)

/** 펼친 드라이브의 추억 영상 상태 */
sealed interface DriveVideoState {
    data object Idle : DriveVideoState
    data class Making(val message: String) : DriveVideoState
    data class Ready(val file: File) : DriveVideoState
    data class Failed(val message: String) : DriveVideoState
}

/** 기록 상태를 화면 밖에 두어 탭을 옮기거나 화면이 회전해도 기록이 이어지게 한다. */
class DriveRecordViewModel(application: Application) : AndroidViewModel(application) {
    private val dao = DriveDatabase.get(application).driveDao()
    private val carRepo = CarRepository(application)
    private var location: PleosLocation? = null
    private val recorded = mutableListOf<LocationPoint>()
    private var recordingJob: Job? = null
    private var startTime = 0L
    private var videoJob: Job? = null
    private var downloadJob: Job? = null

    private val _uiState = MutableStateFlow(DriveRecordUiState())
    val uiState: StateFlow<DriveRecordUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            dao.getSessions().collect { sessions ->
                _uiState.update { it.copy(summary = sessions.toDriveSummary(Year.now().value)) }
            }
        }
        viewModelScope.launch {
            carRepo.selectedCar.collect { car -> _uiState.update { it.copy(selectedCar = car) } }
        }
    }

    /** 위치 권한을 받은 뒤에 호출한다. */
    fun startRecording() {
        if (recordingJob != null) return
        // 데모 주행이면 Pleos 위치 없이 시뮬레이션 위치만 기록한다
        if (!USE_MOCK_LOCATION && !DemoDrive.isActive) {
            val source = location ?: PleosLocation(getApplication()).also { location = it }
            source.start()
        }
        recorded.clear()
        startTime = System.currentTimeMillis()
        _uiState.update { it.copy(isRecording = true, recordedCount = 0) }
        recordingJob = viewModelScope.launch {
            var i = 0
            while (true) {
                val point = when {
                    USE_MOCK_LOCATION -> mockRoute[i % mockRoute.size]
                    DemoDrive.isActive -> DemoDrive.position
                    else -> location?.latest
                }
                if (point != null) {
                    recorded.add(point)
                    _uiState.update { it.copy(recordedCount = recorded.size) }
                }
                i++
                delay(SAMPLE_INTERVAL_MS)
            }
        }
    }

    /** 기록을 끝내고 저장한다. 받은 위치가 없으면 저장하지 않으며, [onFinished]로 저장 여부를 알린다. */
    fun stopRecording(onFinished: (saved: Boolean) -> Unit) {
        recordingJob?.cancel() ?: return
        recordingJob = null
        _uiState.update { it.copy(isRecording = false) }
        val points = recorded.toList()
        if (points.isEmpty()) {
            onFinished(false)
            return
        }
        viewModelScope.launch {
            saveDrive(dao, startTime, points)
            onFinished(true)
        }
    }

    /** 최근 드라이브를 펼쳐 경로를 보거나 다시 접는다. */
    fun toggleExpanded(sessionId: Long) {
        videoJob?.cancel()
        videoJob = null
        if (_uiState.value.expandedSessionId == sessionId) {
            _uiState.update {
                it.copy(expandedSessionId = null, expandedPoints = emptyList(), video = DriveVideoState.Idle)
            }
            return
        }
        // 이미 만든 영상이 있으면 바로 보여준다.
        val existing = videoFile(sessionId).takeIf { it.exists() && it.length() > 0 }
        val video = existing?.let { DriveVideoState.Ready(it) } ?: DriveVideoState.Idle
        _uiState.update { it.copy(expandedSessionId = sessionId, expandedPoints = emptyList(), video = video) }
        viewModelScope.launch {
            val points = dao.getPoints(sessionId)
            _uiState.update {
                if (it.expandedSessionId == sessionId) it.copy(expandedPoints = points) else it
            }
        }
    }

    /**
     * 펼친 드라이브의 경로로 지도 배경 위를 [car]가 달리는 영상을 만든다.
     * 고른 차는 저장해 두고 다음 영상의 기본값으로 쓴다. 같은 드라이브를 다른 차로 만들면 영상을 덮어쓴다.
     */
    fun makeVideo(car: CarModel = _uiState.value.selectedCar) {
        val state = _uiState.value
        val sessionId = state.expandedSessionId ?: return
        if (state.expandedPoints.size < 2 || videoJob != null) return
        val points = state.expandedPoints
        _uiState.update { it.copy(selectedCar = car) }
        videoJob = viewModelScope.launch {
            val app = getApplication<Application>()
            try {
                carRepo.setSelectedCar(car)
                val session = dao.getSession(sessionId) ?: error("드라이브를 찾을 수 없어요")
                setVideo(sessionId, DriveVideoState.Making("지도를 준비하고 있어요"))
                val bg = makeMapBackground(app, points, sessionId)
                setVideo(sessionId, DriveVideoState.Making("영상을 만들고 있어요"))
                val file = makeRouteVideo(app, bg, session, videoFile(sessionId), car)
                setVideo(sessionId, DriveVideoState.Ready(file))
            } catch (e: kotlinx.coroutines.CancellationException) {
                videoFile(sessionId).delete() // 만들다 만 파일을 다음에 완성본으로 착각하지 않게
                throw e
            } catch (e: Exception) {
                videoFile(sessionId).delete()
                Log.e(TAG, "영상 만들기 실패", e)
                setVideo(sessionId, DriveVideoState.Failed("영상을 만들지 못했어요"))
            } finally {
                videoJob = null
            }
        }
    }

    /** 펼친 드라이브의 영상을 기기 다운로드 폴더에 저장한다. 저장 중에 또 누르면 무시한다. */
    fun downloadVideo(onFinished: (saved: Boolean) -> Unit) {
        val state = _uiState.value
        val sessionId = state.expandedSessionId ?: return
        val video = state.video as? DriveVideoState.Ready ?: return
        if (downloadJob != null) return
        downloadJob = viewModelScope.launch {
            try {
                onFinished(saveVideoToDownloads(getApplication(), video.file, "aCARive_drive_$sessionId.mp4"))
            } finally {
                downloadJob = null
            }
        }
    }

    private fun setVideo(sessionId: Long, video: DriveVideoState) {
        _uiState.update { if (it.expandedSessionId == sessionId) it.copy(video = video) else it }
    }

    private fun videoFile(sessionId: Long): File {
        val app = getApplication<Application>()
        val dir = app.getExternalFilesDir(Environment.DIRECTORY_MOVIES) ?: app.filesDir
        return File(dir, "drive_$sessionId.mp4")
    }

    override fun onCleared() {
        location?.stop()
    }
}
