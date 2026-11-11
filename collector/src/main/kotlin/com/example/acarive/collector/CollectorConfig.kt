package com.example.acarive.collector

import java.io.File
import java.util.Properties

// 수집 설정. collector/collector.properties 에서 읽는다 (예시: collector.properties.example)
data class CollectorConfig(
    // data.go.kr 에서 받은 인증키. Encoding 키(% 포함)와 Decoding 키 둘 다 된다.
    val serviceKey: String,
    val mobileOs: String = "ETC",
    val mobileApp: String = "aCARive",
    // 한 번 실행에서 쓸 최대 호출 수. 개발계정 하루 한도(1,000)보다 조금 작게 둔다.
    val callLimit: Int = 950,
    // 호출 사이 대기(ms). 너무 빠르면 서버가 막는다.
    val requestDelayMs: Long = 150,
    val pageSize: Int = 100,
    // 지난 수집을 시작한 지 이만큼 지나면 새로 수집한다.
    val refreshDays: Int = 30,
    // 관광지(풍경 지점) 목록도 모을지
    val collectSpots: Boolean = true,
    // 코스 목록(areaBasedList2) 호출에 덧붙일 파라미터. 예: lDongRegnCd=51, arrange=C
    val courseListParams: Map<String, String> = emptyMap(),
    // 풍경 지점 목록 호출에 덧붙일 파라미터. 기본은 자연(cat1=A01) 관광지
    val spotListParams: Map<String, String> = mapOf("cat1" to "A01"),
    // 진행 상태 파일. 한도에 걸려 멈추면 다음 실행 때 여기서 이어간다.
    val stateFile: File = File("collector/data/state.json"),
    // 완성된 카탈로그. 앱 assets 에 바로 쓴다.
    val catalogFile: File = File("app/src/main/assets/course_catalog.json"),
) {
    companion object {
        private const val COURSE_PARAM_PREFIX = "courseList."
        private const val SPOT_PARAM_PREFIX = "spotList."

        // 환경변수 TOUR_API_KEY 가 있으면 파일의 serviceKey 보다 우선한다.
        fun load(file: File, env: Map<String, String> = System.getenv()): CollectorConfig {
            val props = Properties()
            if (file.exists()) file.reader(Charsets.UTF_8).use { props.load(it) }
            fun get(key: String): String? = props.getProperty(key)?.trim()?.takeIf { it.isNotEmpty() }

            val key = env["TOUR_API_KEY"]?.trim()?.takeIf { it.isNotEmpty() } ?: get("serviceKey")
                ?: error("인증키가 없어요. ${file.path} 의 serviceKey 나 환경변수 TOUR_API_KEY 를 설정해 주세요.")

            fun params(prefix: String): Map<String, String> = props.stringPropertyNames()
                .filter { it.startsWith(prefix) }
                .associate { it.removePrefix(prefix) to props.getProperty(it).trim() }
                .filterValues { it.isNotEmpty() }

            val defaults = CollectorConfig(serviceKey = key)
            val spotParams = params(SPOT_PARAM_PREFIX)
            return defaults.copy(
                mobileOs = get("mobileOs") ?: defaults.mobileOs,
                mobileApp = get("mobileApp") ?: defaults.mobileApp,
                callLimit = get("callLimit")?.toInt() ?: defaults.callLimit,
                requestDelayMs = get("requestDelayMs")?.toLong() ?: defaults.requestDelayMs,
                pageSize = get("pageSize")?.toInt() ?: defaults.pageSize,
                refreshDays = get("refreshDays")?.toInt() ?: defaults.refreshDays,
                collectSpots = get("collectSpots")?.toBooleanStrict() ?: defaults.collectSpots,
                courseListParams = params(COURSE_PARAM_PREFIX),
                spotListParams = spotParams.ifEmpty { defaults.spotListParams },
                stateFile = get("stateFile")?.let(::File) ?: defaults.stateFile,
                catalogFile = get("catalogFile")?.let(::File) ?: defaults.catalogFile,
            )
        }
    }
}
