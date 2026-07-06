package com.dzungphung.smarthome.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.dzungphung.smarthome.client.ConnectionState
import com.dzungphung.smarthome.client.IotClientManager
import com.dzungphung.smarthome.proto.DeviceState
import kotlinx.coroutines.flow.StateFlow

/**
 * ViewModel bridging the Jetpack Compose UI with [IotClientManager].
 */
class MainViewModel(private val clientManager: IotClientManager) : ViewModel() {

    val connectionState: StateFlow<ConnectionState> = clientManager.connectionState
    val deviceState: StateFlow<DeviceState?> = clientManager.deviceState

    fun connect() {
        clientManager.connect()
    }

    fun disconnect() {
        clientManager.disconnect()
    }

    fun togglePower(turnOn: Boolean) {
        clientManager.sendToggleCommand(turnOn)
    }

    override fun onCleared() {
        super.onCleared()
        clientManager.disconnect()
    }

    class Factory(private val clientManager: IotClientManager) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return MainViewModel(clientManager) as T
        }
    }
}
