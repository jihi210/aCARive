package com.example.acarive.collector

import java.io.File
import kotlin.system.exitProcess

private const val DEFAULT_CONFIG = "collector/collector.properties"
private const val SAVE_INTERVAL_MS = 5_000L

// 실행: ./gradlew :collector:run                         (이어서 수집)
//      ./gradlew :collector:run --args="--fresh"         (새 주기로 처음부터)
//      ./gradlew :collector:run --args="--config 경로"   (다른 설정 파일)
fun main(args: Array<String>) {
    val fresh = "--fresh" in args
    val configPath = args.indexOf("--config").takeIf { it >= 0 }?.let { args.getOrNull(it + 1) } ?: DEFAULT_CONFIG

    val config = try {
        CollectorConfig.load(File(configPath))
    } catch (e: IllegalStateException) {
        System.err.println(e.message)
        exitProcess(1)
    }
    println("설정: ${File(configPath).absolutePath}")
    println("호출 한도 ${config.callLimit}번 · 코스 파라미터 ${config.courseListParams} · 풍경 지점 파라미터 ${config.spotListParams}")

    val client = TourApiClient(config)
    val collector = CourseCollector(client, config)

    var lastSaved = 0L
    var latest: CollectorState? = null
    val result = try {
        collector.run(StateStore.load(config.stateFile), forceNewCycle = fresh) { state ->
            latest = state
            // 매번 쓰면 느려서 몇 초에 한 번만 저장한다. 끝날 때 한 번 더 저장한다.
            if (System.currentTimeMillis() - lastSaved >= SAVE_INTERVAL_MS) {
                StateStore.save(config.stateFile, state)
                lastSaved = System.currentTimeMillis()
            }
        }
    } catch (e: FatalApiException) {
        latest?.let { StateStore.save(config.stateFile, it) }
        System.err.println("수집을 멈췄어요: ${e.message}")
        exitProcess(2)
    }

    StateStore.save(config.stateFile, result.state)
    val catalog = collector.buildCatalog(result.state)
    StateStore.saveCatalog(config.catalogFile, catalog)

    println()
    println("이번 실행: API ${client.callsMade}번 호출 · 새로 수집 ${result.fetched} · 재사용 ${result.reused} · 실패 ${result.failed}")
    println("카탈로그: 코스 ${catalog.courses.size}개 · 풍경 지점 ${catalog.spots.size}개 → ${config.catalogFile.absolutePath}")
    if (result.finished) {
        println("이번 주기 수집 완료!")
    } else {
        val remaining = result.state.courseListing.items.distinctBy { it.contentid }.size - result.state.collected.size
        println("중간에 멈췄어요: ${result.stopReason}")
        println("남은 코스 약 ${remaining}개. 내일 같은 명령으로 다시 실행하면 이어서 수집해요.")
    }
}
