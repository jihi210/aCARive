package com.example.acarive.location

import ai.pleos.playground.location.fused.FusedLocation
import ai.pleos.playground.location.fused.listener.FusedLocationListener
import ai.pleos.playground.location.fused.model.LocationData
import android.content.Context
import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

class PleosLocation(context: Context){
    private val fusedLocation= FusedLocation(context)
    private var started=false

    // 가장 최근에 받은 위치
    var latest by mutableStateOf<LocationPoint?>(null)
        private set

    private val listener= object : FusedLocationListener{
        override fun onChanged(location: LocationData?) {
            Log.d("FLS", "위치 수신: $location")
            if(location!=null){
                latest= LocationPoint(location.latitude,location.longitude)
            }
        }

        override fun onError(exception: Exception) {
            Log.e("FLS", "위치 에러", exception)
        }
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
    }

    companion object {
        const val PERMISSION = "pleos.car.permission.FUSED_LOCATION"
    }
}
