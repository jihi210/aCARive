package com.acarive.scenic.domain

enum class MoodTag(val label: String) {
    SENTIMENTAL("감성"), LONELY("쓸쓸"), DREAMY("몽환"), WARM("따뜻"), REFRESHING("청량"),
    LIVELY("경쾌"), SLEEK("세련"), EASY("편안"), GRAND("웅장"), POWERFUL("힘있음");

    companion object {
        fun fromLabel(s: String): MoodTag? = values().firstOrNull { it.label == s.trim() }
    }
}

enum class GenreGroup(val label: String) {
    DYNAMIC("역동적"), CHILL("chill"), LYRICAL("서정적"), POP("팝");

    companion object {
        fun fromLabel(s: String): GenreGroup? =
            values().firstOrNull { it.label.equals(s.trim(), ignoreCase = true) }
    }
}

enum class Weather(val label: String) { SUNNY("맑음"), CLOUDY("흐림"), RAIN("비"), SNOW("눈") }

enum class TempBand {
    FREEZING, COLD, MILD, WARM_DAY, HOT; // 0 미만 / 0~9 / 10~19 / 20~27 / 28~

    companion object {
        fun fromCelsius(c: Double): TempBand = when {
            c < 0 -> FREEZING
            c < 10 -> COLD
            c < 20 -> MILD
            c < 28 -> WARM_DAY
            else -> HOT
        }
    }
}

enum class TimeSlot(val label: String) {
    MORNING("아침"), DAY("낮"), EVENING("저녁"), NIGHT("밤");

    companion object {
        fun fromHour(h: Int): TimeSlot = when (h) {
            in 5..9 -> MORNING
            in 10..16 -> DAY
            in 17..19 -> EVENING
            else -> NIGHT
        }
    }
}

enum class SpeedBand {
    JAM, CITY, ROAD, HIGHWAY; // 0~30 / 30~60 / 60~80 / 80~

    companion object {
        fun fromKmh(kmh: Double, highwayKmh: Double = 80.0): SpeedBand = when {
            kmh < 30 -> JAM
            kmh < 60 -> CITY
            kmh < highwayKmh -> ROAD
            else -> HIGHWAY
        }
    }
}

data class Track(
    val title: String,
    val artist: String,
    val uri: String,
    val genre: GenreGroup,
    val tags: List<MoodTag>,
    val energy: Int,
) {
    val id: String get() = uri
}

data class DrivingContext(
    val weather: Weather? = null,
    val tempBand: TempBand? = null,
    val timeSlot: TimeSlot? = null,
    val speedBand: SpeedBand? = null,
)

/** 사용자가 고른 장르 그룹. 비어 있으면 모든 장르를 같은 취향으로 본다. */
data class UserPreference(val genres: Set<GenreGroup> = emptySet()) {
    companion object {
        /** 취향을 입력하지 않았을 때 쓰는 기본값. */
        val DEFAULT = UserPreference(setOf(GenreGroup.CHILL, GenreGroup.POP))

        /**
         * 화면에서 선택한 장르 이름 목록으로 만든다.
         * 알 수 없는 이름은 무시하고, 인식된 장르가 하나도 없으면 DEFAULT를 쓴다.
         */
        fun fromLabels(labels: Collection<String>?): UserPreference {
            val genres = labels.orEmpty().mapNotNull { GenreGroup.fromLabel(it) }.toSet()
            return if (genres.isEmpty()) DEFAULT else UserPreference(genres)
        }
    }
}
