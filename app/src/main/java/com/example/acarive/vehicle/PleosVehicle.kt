package com.example.acarive.vehicle

import ai.pleos.playground.vehicle.Vehicle
import ai.pleos.playground.vehicle.constant.PleosPermission
import ai.pleos.playground.vehicle.constant.control.VehicleGear
import ai.pleos.playground.vehicle.listener.EvBatteryStateOfChargeListener
import ai.pleos.playground.vehicle.listener.FuelPercentListener
import ai.pleos.playground.vehicle.listener.GearSelectionListener
import ai.pleos.playground.vehicle.listener.RangeRemainingListener
import android.content.Context
import android.content.Intent
import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

private const val TAG = "PleosVehicle"

// 차량에서 받은 값. 못 받은 값은 null (EV 면 연료가 null, 내연기관이면 배터리가 null)
data class VehicleData(
    val vin: String? = null,
    val gear: VehicleGear? = null,
    val evBatteryPercent: Int? = null,
    val fuelPercent: Int? = null,
    // 미터 단위로 온다고 보고 있다 (AAOS RANGE_REMAINING 과 같은 단위). 에뮬레이터에서 확인 필요
    val rangeMeters: Float? = null,
    val updatedAt: Long? = null,
)

// Pleos Vehicle SDK 래퍼. 차량 정보·기어·배터리/연료·주행 가능 거리를 구독한다.
// 속도는 이 SDK 에 없어서 위치 변화로 따로 계산한다 (PleosLocation.speedKmh).
class PleosVehicle(context: Context) {
    private val vehicle = Vehicle(context.applicationContext)
    private var started = false

    private val _data = MutableStateFlow(VehicleData())
    val data: StateFlow<VehicleData> = _data.asStateFlow()

    private val gearListener = object : GearSelectionListener {
        override fun onSelected(gear: VehicleGear?) = set { copy(gear = gear) }
        override fun onFailed(e: Exception) = logError(e)
    }
    private val socListener = object : EvBatteryStateOfChargeListener {
        override fun onEvBatteryStateOfChargeUpdated(soc: Int?) { soc?.let { set { copy(evBatteryPercent = it) } } }
        override fun onFailed(e: Exception) = logError(e)
    }
    private val fuelListener = object : FuelPercentListener {
        override fun onFuelPercentUpdated(fuel: Int?) { fuel?.let { set { copy(fuelPercent = it) } } }
        override fun onFailed(e: Exception) = logError(e)
    }
    private val rangeListener = object : RangeRemainingListener {
        override fun onRangeRemainingChanged(range: Float?) { range?.let { set { copy(rangeMeters = it) } } }
        override fun onFailed(e: Exception) = logError(e)
    }

    fun start() {
        if (started) return
        try {
            vehicle.initialize()
            started = true
        } catch (e: Exception) {
            Log.e(TAG, "Vehicle SDK 초기화 실패", e)
            return
        }
        // 지금 값 한 번 + 바뀔 때마다
        vehicle.getCarInfo().getVINInfo({ vin -> set { copy(vin = vin) } }, ::logError)
        vehicle.getGear().getCurrentGearSelectionState({ gear -> set { copy(gear = gear) } }, ::logError)
        vehicle.getEvBattery().getEvBatteryStateOfCharge({ soc -> set { copy(evBatteryPercent = soc) } }, ::logError)
        vehicle.getFuel().getFuelPercent({ fuel -> set { copy(fuelPercent = fuel) } }, ::logError)
        vehicle.getOdometer().getRangeRemainingDistance({ range -> set { copy(rangeMeters = range) } }, ::logError)

        runCatching {
            vehicle.getGear().registerGearSelection(gearListener)
            vehicle.getEvBattery().registerEvBatteryStateOfCharge(socListener)
            vehicle.getFuel().registerFuelPercent(fuelListener)
            vehicle.getOdometer().registerRangeRemainingListener(rangeListener)
        }.onFailure { logError(it) }
    }

    fun stop() {
        if (!started) return
        runCatching {
            vehicle.getGear().unregisterGearSelection(gearListener)
            vehicle.getEvBattery().unregisterEvBatteryStateOfCharge(socListener)
            vehicle.getFuel().unregisterFuelPercent(fuelListener)
            vehicle.getOdometer().unregisterRangeRemainingListener(rangeListener)
            vehicle.release()
        }.onFailure { logError(it) }
        started = false
    }

    private fun set(change: VehicleData.() -> VehicleData) {
        _data.update { it.change().copy(updatedAt = System.currentTimeMillis()) }
    }

    // 차량마다 없는 값이 있어서(EV/내연기관) 실패는 로그만 남긴다
    private fun logError(e: Throwable) {
        Log.w(TAG, "차량 데이터 요청 실패: ${e.message}")
    }

    companion object {
        // 읽기 권한만 쓴다. 제어(CONTROL_*) 는 안 씀
        val PERMISSIONS = arrayOf(
            PleosPermission.CAR_INFO,
            PleosPermission.CAR_DRIVE,
            PleosPermission.CAR_ENERGY,
        )

        private const val SERVICE_ACTION = "ai.pleos.playground.vehicle.VEHICLE_START"

        // Pleos Connect(차량/전용 에뮬레이터)에만 차량 서비스가 있다. 일반 폰·에뮬레이터는 false
        fun isAvailable(context: Context): Boolean {
            val intent = Intent(SERVICE_ACTION)
            val pm = context.packageManager
            return pm.queryIntentServices(intent, 0).isNotEmpty() || pm.queryIntentActivities(intent, 0).isNotEmpty()
        }
    }
}

// 화면에 보여줄 기어 글자
val VehicleGear.label: String
    get() = when (this) {
        VehicleGear.GEAR_PARK -> "P"
        VehicleGear.GEAR_REVERSE -> "R"
        VehicleGear.GEAR_NEUTRAL -> "N"
        VehicleGear.GEAR_UNKNOWN -> "-"
        else -> "D"
    }
