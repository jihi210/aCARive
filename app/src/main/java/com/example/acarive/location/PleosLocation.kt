package com.example.acarive.location

import ai.pleos.playground.location.fused.FusedLocation
import ai.pleos.playground.location.fused.listener.FusedLocationListener
import ai.pleos.playground.location.fused.model.LocationData
import android.content.Context
import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import kotlin.math.asin
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.math.sqrt

class PleosLocation(context: Context){
    private val fusedLocation= FusedLocation(context)
    private var started=false

    // 가장 최근에 받은 위치
    var latest by mutableStateOf<LocationPoint?>(null)
        private set

    // 위치 변화로 계산한 속도(km/h). Vehicle SDK 에 속도 API 가 없어서 여기서 구한다.
    var speedKmh by mutableStateOf<Int?>(null)
        private set
    private var previous: LocationData? = null

    private val listener= object : FusedLocationListener{
        override fun onChanged(location: LocationData?) {
            Log.d("FLS", "위치 수신: $location")
            if(location!=null){
                latest= LocationPoint(location.latitude,location.longitude)
                updateSpeed(location)
            }
        }

        override fun onError(exception: Exception) {
            Log.e("FLS", "위치 에러", exception)
        }
    }

    private fun updateSpeed(location: LocationData) {
        val prev = previous
        previous = location
        if (prev == null) return
        val seconds = (location.timestampMillis - prev.timestampMillis) / 1000.0
        // 너무 짧은 간격은 오차가 커서 건너뛴다
        if (seconds < 0.5) return
        val km = distanceKm(prev.latitude, prev.longitude, location.latitude, location.longitude)
        speedKmh = (km / seconds * 3600).roundToInt()
    }

    fun start() {
        if (started) return
        try {
            if (fusedLocation.isSupported()) {
                fusedLocation.initialize()
                fusedLocation.registerFusedLocationCallback(listener)
                started = true
                Log.d("FLS", "등록 완료")
            } else {
                Log.d("FLS", "이 기기는 Fused Location 미지원")
            }
        } catch (e: SecurityException) {
            Log.e("FLS", "권한 없음: 위치를 시작하지 못함", e)
        }
    }

    fun stop(){
        if(!started){
            return
        }
        fusedLocation.unregisterFusedLocationCallback(listener)
        fusedLocation.release()
        started=false
        previous = null
        speedKmh = null
    }

    companion object {
        const val PERMISSION = "pleos.car.permission.FUSED_LOCATION"
    }
}

// 하버사인 직선거리(km)
internal fun distanceKm(lat1: Double, lng1: Double, lat2: Double, lng2: Double): Double {
    val dLat = Math.toRadians(lat2 - lat1)
    val dLng = Math.toRadians(lng2 - lng1)
    val h = sin(dLat / 2).let { it * it } +
        cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) * sin(dLng / 2).let { it * it }
    return 2 * 6371.0 * asin(sqrt(h))
}
