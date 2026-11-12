package com.example.driveapp.car

import android.content.Context

// 사용자가 고른 차를 기기에 기억해 두는 곳 (앱을 껐다 켜도 유지)
// 차 하나의 id만 저장하면 돼서 DB 대신 SharedPreferences를 씀
class CarRepository(context: Context) {

    private val prefs = context.applicationContext
        .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    // 마지막으로 고른 차 (처음이면 기본 차)
    fun getSelectedCar(): CarModel = CarModel.fromId(prefs.getString(KEY_CAR, null))

    fun setSelectedCar(car: CarModel) {
        prefs.edit().putString(KEY_CAR, car.id).apply()
    }

    private companion object {
        const val PREFS_NAME = "car_prefs"
        const val KEY_CAR = "selected_car_id"
    }
}
