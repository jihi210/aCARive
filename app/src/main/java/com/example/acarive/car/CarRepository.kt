package com.example.acarive.car

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import java.io.IOException

private val Context.carStore by preferencesDataStore(name = "car")

private val SELECTED_CAR = stringPreferencesKey("selected_car_id")

// 추억 영상에 등장할 차. 로그인이 없어서 ProfileRepository 처럼 기기(DataStore)에 저장한다
class CarRepository(context: Context) {

    private val store = context.applicationContext.carStore

    // 저장된 값이 없으면 CarModel.DEFAULT
    val selectedCar: Flow<CarModel> = store.data
        .catch { e -> if (e is IOException) emit(emptyPreferences()) else throw e }
        .map { CarModel.fromId(it[SELECTED_CAR]) }
        .distinctUntilChanged()

    suspend fun setSelectedCar(car: CarModel) {
        store.edit { it[SELECTED_CAR] = car.id }
    }
}
