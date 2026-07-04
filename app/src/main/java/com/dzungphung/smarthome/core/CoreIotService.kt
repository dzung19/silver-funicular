package com.dzungphung.smarthome.core

import android.app.Service
import android.content.Intent
import android.os.IBinder
import android.os.RemoteCallbackList
import android.os.RemoteException
import android.util.Log
import com.dzungphung.smarthome.IIotCallback
import com.dzungphung.smarthome.IIotHub
import com.dzungphung.smarthome.proto.DeviceCommand
import com.dzungphung.smarthome.proto.DeviceState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

/**
 * Service executing in the `:iot_core` isolated process.
 * Hosts the AIDL Stub, communicates with [FakeBleRepository], and broadcasts
 * protobuf-encoded telemetry to all connected clients using [RemoteCallbackList].
 */
class CoreIotService : Service() {

    companion object {
        private const val TAG = "CoreIotService"
    }

    private val serviceScope = CoroutineScope(Dispatchers.Default + SupervisorJob())
    private val repository = FakeBleRepository()

    /**
     * RemoteCallbackList safely handles client callbacks across process boundaries
     * and automatically cleans up when client processes die.
     */
    private val callbackList = RemoteCallbackList<IIotCallback>()

    private val binder = object : IIotHub.Stub() {
        override fun sendCommand(protobufPayload: ByteArray?) {
            if (protobufPayload == null) return
            try {
                val command = DeviceCommand.parseFrom(protobufPayload)
                Log.d(TAG, "Received command for device [${command.id}]: turnOn = ${command.turnOn}")
                repository.setDeviceOnState(command.turnOn)
            } catch (e: Exception) {
                Log.e(TAG, "Failed to parse DeviceCommand payload", e)
            }
        }

        override fun registerCallback(callback: IIotCallback?) {
            if (callback != null) {
                val registered = callbackList.register(callback)
                Log.d(TAG, "Registered client callback: $registered")
            }
        }

        override fun unregisterCallback(callback: IIotCallback?) {
            if (callback != null) {
                val unregistered = callbackList.unregister(callback)
                Log.d(TAG, "Unregistered client callback: $unregistered")
            }
        }
    }

    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "CoreIotService created in process: ${android.os.Process.myPid()}")

        // Start collecting telemetry from FakeBleRepository
        serviceScope.launch {
            repository.deviceStateFlow.collect { deviceState ->
                broadcastStateToClients(deviceState)
            }
        }
    }

    private fun broadcastStateToClients(deviceState: DeviceState) {
        val payload = deviceState.toByteArray()
        val count = callbackList.beginBroadcast()
        Log.d(TAG, "Broadcasting state update (temp: ${deviceState.temperature}°C, isOn: ${deviceState.isOn}) to $count client(s)")

        for (i in 0 until count) {
            try {
                callbackList.getBroadcastItem(i).onDeviceStateChanged(payload)
            } catch (e: RemoteException) {
                Log.e(TAG, "Error invoking callback on client index $i", e)
            }
        }
        callbackList.finishBroadcast()
    }

    override fun onBind(intent: Intent?): IBinder {
        Log.d(TAG, "Client binding to CoreIotService")
        return binder
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.d(TAG, "CoreIotService destroying")
        callbackList.kill()
        serviceScope.cancel()
    }
}
