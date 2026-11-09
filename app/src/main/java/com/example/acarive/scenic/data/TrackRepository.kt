package com.acarive.scenic.data

import android.content.Context
import com.acarive.scenic.domain.GenreGroup
import com.acarive.scenic.domain.MoodTag
import com.acarive.scenic.domain.Track

object TrackCsvParser {
    fun parse(rawText: String): List<Track> {
        val rows = parseCsv(rawText.removePrefix("\uFEFF"))
        val headerIdx = rows.indexOfFirst { r -> r.any { it.trim() == "곡명" } }
        if (headerIdx < 0) return emptyList()
        val header = rows[headerIdx].map { it.trim() }
        fun col(name: String) = header.indexOf(name)

        return rows.drop(headerIdx + 1).mapNotNull { r ->
            fun cell(name: String) = r.getOrNull(col(name))?.trim().orEmpty()
            if (cell("번호") == "예시") return@mapNotNull null
            val title = cell("곡명")
            val uri = cell("Spotify URI")
            if (title.isBlank() || uri.isBlank()) return@mapNotNull null
            val genre = GenreGroup.fromLabel(cell("장르 그룹")) ?: return@mapNotNull null
            val energy = cell("energy").toIntOrNull()?.takeIf { it in 1..5 } ?: return@mapNotNull null
            val tags = listOf("태그1", "태그2", "태그3").mapNotNull { MoodTag.fromLabel(cell(it)) }.distinct()
            Track(title, cell("아티스트"), uri, genre, tags, energy)
        }
    }

    private fun parseCsv(text: String): List<List<String>> {
        val rows = mutableListOf<List<String>>()
        var row = mutableListOf<String>()
        val sb = StringBuilder()
        var inQuotes = false
        var i = 0
        while (i < text.length) {
            val c = text[i]
            when {
                inQuotes && c == '"' && text.getOrNull(i + 1) == '"' -> { sb.append('"'); i++ }
                c == '"' -> inQuotes = !inQuotes
                !inQuotes && c == ',' -> { row.add(sb.toString()); sb.clear() }
                !inQuotes && c == '\n' -> { row.add(sb.toString()); sb.clear(); rows.add(row); row = mutableListOf() }
                !inQuotes && c == '\r' -> Unit
                else -> sb.append(c)
            }
            i++
        }
        if (sb.isNotEmpty() || row.isNotEmpty()) { row.add(sb.toString()); rows.add(row) }
        return rows
    }
}

class TrackRepository(private val context: Context) {
    fun load(assetName: String = "tracks.csv"): List<Track> =
        context.assets.open(assetName).bufferedReader(Charsets.UTF_8).use { TrackCsvParser.parse(it.readText()) }
}
