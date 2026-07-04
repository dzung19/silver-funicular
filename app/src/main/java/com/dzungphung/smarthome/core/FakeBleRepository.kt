package com.dzungphung.smarthome.core

import com.dzungphung.smarthome.proto.DeviceState
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flow
import kotlin.random.Random

/**
 * Repository simulating a BLE Hardware Device emitting temperature telemetry.
 */
class FakeBleRepository {

    private val deviceId = "device-ble-01"
    private val _isOnState = MutableStateFlow(true)

    /**
     * Flow that periodically emits a simulated [DeviceState] every 3 seconds
     * with randomized temperature readings between 20°C and 30°C.
     */
    val deviceStateFlow: Flow<DeviceState> = flow {
        while (true) {
            val randomTemp = Random.nextInt(20, 31)
            val currentState = DeviceState.newBuilder()
                .setId(deviceId)
                .setTemperature(randomTemp)
                .setIsOn(_isOnState.value)
                .build()

            emit(currentState)
            delay(3000)
        }
    }

    fun setDeviceOnState(isOn: Boolean) {
        _isOnState.value = isOn
    }
}
