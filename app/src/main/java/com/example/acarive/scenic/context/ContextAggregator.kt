package com.acarive.scenic.context

import com.acarive.scenic.domain.DrivingContext
import com.acarive.scenic.domain.SpeedBand
import com.acarive.scenic.domain.TempBand
import com.acarive.scenic.domain.TimeSlot
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.time.LocalDateTime

data class AggregatorConfig(
    val speedIntervalMs: Long = 10_000,
    val speedWindowMs: Long = 180_000,
    val envIntervalMs: Long = 20 * 60_000L,
    val highwayKmh: Double = 80.0,
    val bandConfirmCount: Int = 2,
) {
    companion object {
        fun demo() = AggregatorConfig(speedIntervalMs = 1_000, speedWindowMs = 18_000, envIntervalMs = 60_000)
    }
}

class ContextAggregator(
    private val speedSource: SpeedSource,
    private val weatherSource: WeatherSource,
    private val cfg: AggregatorConfig = AggregatorConfig(),
    private val now: () -> LocalDateTime = { LocalDateTime.now() },
) {
    private val _context = MutableStateFlow(DrivingContext())
    val context: StateFlow<DrivingContext> = _context.asStateFlow()

    private val samples = ArrayDeque<Double>()
    private val maxSamples = (cfg.speedWindowMs / cfg.speedIntervalMs).toInt().coerceAtLeast(1)
    private var pendingBand: SpeedBand? = null
    private var pendingCount = 0

    fun start(scope: CoroutineScope): List<Job> = listOf(
        scope.launch { while (isActive) { sampleSpeed(); delay(cfg.speedIntervalMs) } },
        scope.launch { while (isActive) { refreshEnvironment(); delay(cfg.envIntervalMs) } },
    )

    private suspend fun sampleSpeed() {
        val kmh = try { speedSource.currentSpeedKmh() } catch (e: Exception) { null } ?: return
        samples.addLast(kmh)
        while (samples.size > maxSamples) samples.removeFirst()

        val band = SpeedBand.fromKmh(samples.average(), cfg.highwayKmh)
        val current = _context.value.speedBand
        if (current == null) { setBand(band); return }
        if (band == current) { pendingBand = null; pendingCount = 0; return }
        if (band == pendingBand) pendingCount++ else { pendingBand = band; pendingCount = 1 }
        if (pendingCount >= cfg.bandConfirmCount) setBand(band)
    }

    private fun setBand(band: SpeedBand) {
        pendingBand = null; pendingCount = 0
        _context.update { it.copy(speedBand = band) }
    }

    private suspend fun refreshEnvironment() {
        val info = try { weatherSource.current() } catch (e: Exception) { null }
        val hour = now().hour
        _context.update {
            it.copy(
                timeSlot = TimeSlot.fromHour(hour),
                // 날씨 조회에 실패하면 직전 값을 유지한다.
                weather = info?.weather ?: it.weather,
                tempBand = info?.let { i -> TempBand.fromCelsius(i.tempC) } ?: it.tempBand,
            )
        }
    }
}
