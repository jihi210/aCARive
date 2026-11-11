package com.example.acarive.collector

import com.example.acarive.data.course.CourseCatalog
import com.example.acarive.data.course.DriveCourse
import com.example.acarive.data.tour.TourCommonDetail
import com.example.acarive.data.tour.TourContentType
import com.example.acarive.data.tour.TourJson
import com.example.acarive.data.tour.TourListItem
import com.example.acarive.data.tour.parseTourTime
import com.example.acarive.data.tour.toCommonDetail
import com.example.acarive.data.tour.toDriveCourse
import com.example.acarive.data.tour.toScenicSpot
import kotlinx.serialization.Serializable
import java.io.File

private const val MAX_FAILURES = 3
private const val DAY_MS = 24L * 60 * 60 * 1000

// 목록 수집 진행 상황. 페이지 중간에 한도에 걸려도 다음 페이지부터 이어간다.
@Serializable
data class ListingProgress(
    val items: List<TourListItem> = emptyList(),
    val nextPage: Int = 1,
    val done: Boolean = false,
)

// 한 번의 수집 주기(한 달) 진행 상태. state.json 으로 저장된다.
@Serializable
data class CollectorState(
    val cycleStartedAt: Long,
    val courseListing: ListingProgress = ListingProgress(),
    val spotListing: ListingProgress = ListingProgress(),
    // contentid → 완성된 코스
    val collected: Map<String, DriveCourse> = emptyMap(),
    // 지난 주기에 완성한 코스. 수정일이 같으면 상세를 다시 부르지 않고 재사용한다.
    val previous: Map<String, DriveCourse> = emptyMap(),
    // 경유지 contentid → 상세. 여러 코스가 같은 경유지를 쓰므로 한 번만 부른다.
    val stopDetails: Map<String, TourCommonDetail> = emptyMap(),
    // contentid → 실패 횟수. MAX_FAILURES 번 실패하면 이번 주기엔 건너뛴다.
    val failures: Map<String, Int> = emptyMap(),
    val completedAt: Long? = null,
)

data class RunResult(
    val state: CollectorState,
    val finished: Boolean,
    // 멈춘 이유 (한도 초과 등). 끝까지 돌았으면 null
    val stopReason: String?,
    val reused: Int,
    val fetched: Int,
    val failed: Int,
)

// 전국 여행코스 + 풍경 지점을 모아 CourseCatalog 를 만든다.
// 1) 코스 목록 → 2) 풍경 지점 목록 → 3) 코스마다 상세·소개·경유지·경유지 상세
class CourseCollector(
    private val source: TourSource,
    private val config: CollectorConfig,
    private val now: () -> Long = System::currentTimeMillis,
    private val log: (String) -> Unit = ::println,
) {

    // 이전 상태를 이어서 실행한다. [onProgress] 는 진행할 때마다 상태를 저장하는 데 쓴다.
    fun run(initial: CollectorState?, forceNewCycle: Boolean = false, onProgress: (CollectorState) -> Unit = {}): RunResult {
        var state = startOrResume(initial, forceNewCycle)
        if (state.completedAt != null) {
            return RunResult(state, finished = true, stopReason = null, reused = 0, fetched = 0, failed = 0)
        }
        var reused = 0
        var fetched = 0
        var failed = 0
        fun save(next: CollectorState) {
            state = next
            onProgress(next)
        }

        try {
            save(state.copy(courseListing = listAll("코스", state.courseListing, courseListParams()) { save(state.copy(courseListing = it)) }))
            if (config.collectSpots) {
                save(state.copy(spotListing = listAll("풍경 지점", state.spotListing, spotListParams()) { save(state.copy(spotListing = it)) }))
            }

            val spotIndex = state.spotListing.items.associateBy { it.contentid }
            val pending = state.courseListing.items
                .distinctBy { it.contentid }
                .filter { it.contentid !in state.collected && (state.failures[it.contentid] ?: 0) < MAX_FAILURES }
            log("코스 상세: 남은 ${pending.size}개 / 전체 ${state.courseListing.items.size}개")

            for ((index, item) in pending.withIndex()) {
                val id = item.contentid
                val prev = state.previous[id]
                val modifiedAt = parseTourTime(item.modifiedtime)
                if (prev != null && modifiedAt != null && prev.sourceModifiedAt == modifiedAt) {
                    save(state.copy(collected = state.collected + (id to prev)))
                    reused++
                    continue
                }
                try {
                    val (course, newStops) = fetchCourse(id, state.stopDetails, spotIndex)
                    save(
                        state.copy(
                            collected = state.collected + (id to course),
                            stopDetails = state.stopDetails + newStops,
                            failures = state.failures - id,
                        ),
                    )
                    fetched++
                    if (fetched % 20 == 0) log("  ${index + 1}/${pending.size} 진행 중 (${course.name})")
                } catch (e: TourApiException) {
                    failed++
                    log("  건너뜀 $id ${item.title.orEmpty()}: ${e.message}")
                    save(state.copy(failures = state.failures + (id to (state.failures[id] ?: 0) + 1)))
                }
            }
            save(state.copy(completedAt = now()))
            return RunResult(state, finished = true, stopReason = null, reused = reused, fetched = fetched, failed = failed)
        } catch (e: QuotaExceededException) {
            return RunResult(state, finished = false, stopReason = e.message, reused = reused, fetched = fetched, failed = failed)
        }
    }

    // 지금까지 모은 것으로 카탈로그를 만든다. 수집 도중이어도 완성된 코스만큼은 쓸 수 있다.
    fun buildCatalog(state: CollectorState): CourseCatalog = CourseCatalog(
        collectedAt = state.cycleStartedAt,
        courses = state.collected.values.sortedBy { it.id },
        spots = state.spotListing.items
            .distinctBy { it.contentid }
            .mapNotNull { it.toScenicSpot(state.cycleStartedAt) }
            .sortedBy { it.id },
    )

    private fun startOrResume(initial: CollectorState?, force: Boolean): CollectorState {
        val time = now()
        if (initial == null) {
            log("새 수집을 시작해요")
            return CollectorState(cycleStartedAt = time)
        }
        val due = time - initial.cycleStartedAt >= config.refreshDays * DAY_MS
        if (force || (initial.completedAt != null && due)) {
            log("새 수집 주기를 시작해요. 지난 주기 코스 ${initial.collected.size}개는 바뀌지 않았으면 재사용해요")
            return CollectorState(cycleStartedAt = time, previous = initial.previous + initial.collected)
        }
        if (initial.completedAt != null) {
            val nextAt = initial.cycleStartedAt + config.refreshDays * DAY_MS
            log("이번 주기 수집은 이미 끝났어요. 다음 갱신까지 약 ${(nextAt - time) / DAY_MS + 1}일 남았어요 (지금 다시 하려면 --fresh)")
        } else {
            log("지난 수집을 이어서 해요 (완료 ${initial.collected.size}개)")
        }
        return initial
    }

    private fun courseListParams(): Map<String, String> =
        mapOf("contentTypeId" to TourContentType.TravelCourse.id.toString(), "arrange" to "C") + config.courseListParams

    private fun spotListParams(): Map<String, String> =
        mapOf("contentTypeId" to TourContentType.TouristSpot.id.toString(), "arrange" to "C") + config.spotListParams

    private fun listAll(
        label: String,
        start: ListingProgress,
        params: Map<String, String>,
        onPage: (ListingProgress) -> Unit,
    ): ListingProgress {
        var progress = start
        while (!progress.done) {
            val body = source.list(params, progress.nextPage, config.pageSize)
            val items = progress.items + body.items
            progress = ListingProgress(
                items = items,
                nextPage = progress.nextPage + 1,
                done = !body.hasNextPage || body.items.isEmpty(),
            )
            log("$label 목록 ${body.pageNo}페이지: 누적 ${items.size} / ${body.totalCount}개")
            onPage(progress)
        }
        return progress
    }

    // 코스 하나를 완성한다. 새로 부른 경유지 상세도 함께 돌려준다.
    private fun fetchCourse(
        id: String,
        cachedStops: Map<String, TourCommonDetail>,
        spotIndex: Map<String, TourListItem>,
    ): Pair<DriveCourse, Map<String, TourCommonDetail>> {
        val common = source.detailCommon(id) ?: throw TourApiException("공통 정보가 비어 있어요")
        val intro = source.courseIntro(id)
        val stops = source.courseStops(id)

        val newStops = mutableMapOf<String, TourCommonDetail>()
        val stopDetails = stops.mapNotNull { it.subcontentid?.trim()?.takeIf(String::isNotEmpty) }
            .distinct()
            .mapNotNull { stopId ->
                // 이미 받은 경유지 → 관광지 목록에 있던 지점 → 새로 호출 순으로 찾는다.
                val detail = cachedStops[stopId]
                    ?: spotIndex[stopId]?.toCommonDetail()
                    ?: try {
                        source.detailCommon(stopId)?.also { newStops[stopId] = it }
                    } catch (e: TourApiException) {
                        log("    경유지 $stopId 상세 실패: ${e.message}")
                        null
                    }
                detail?.let { stopId to it }
            }
            .toMap()

        return toDriveCourse(common, intro, stops, stopDetails, collectedAt = now()) to newStops
    }
}

object StateStore {
    fun load(file: File): CollectorState? =
        if (file.exists()) TourJson.decodeFromString(CollectorState.serializer(), file.readText()) else null

    // 쓰는 도중에 꺼져도 이전 파일이 깨지지 않게 임시 파일에 쓴 뒤 바꾼다.
    fun save(file: File, state: CollectorState) {
        writeAtomically(file, TourJson.encodeToString(CollectorState.serializer(), state))
    }

    fun saveCatalog(file: File, catalog: CourseCatalog) {
        writeAtomically(file, TourJson.encodeToString(CourseCatalog.serializer(), catalog))
    }

    private fun writeAtomically(file: File, text: String) {
        file.absoluteFile.parentFile?.mkdirs()
        val tmp = File(file.absoluteFile.parentFile, "${file.name}.tmp")
        tmp.writeText(text)
        if (!tmp.renameTo(file)) {
            file.delete()
            tmp.renameTo(file)
        }
    }
}
