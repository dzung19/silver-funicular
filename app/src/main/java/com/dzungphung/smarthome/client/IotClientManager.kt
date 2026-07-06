package com.dzungphung.smarthome.client

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.IBinder
import android.os.RemoteException
import android.util.Log
import com.dzungphung.smarthome.IIotCallback
import com.dzungphung.smarthome.IIotHub
import com.dzungphung.smarthome.core.CoreIotService
import com.dzungphung.smarthome.proto.DeviceCommand
import com.dzungphung.smarthome.proto.DeviceState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Connection states for the IPC Service link.
 */
enum class ConnectionState {
    DISCONNECTED,
    CONNECTING,
    CONNECTED
}

/**
 * Client Connection Manager running in the Main UI Process.
 * Manages binding to [CoreIotService], exposes telemetry as [StateFlow],
 * and handles process death recovery and thread safety.
 */
class IotClientManager(private val context: Context) {

    companion object {
        private const val TAG = "IotClientManager"
        private const val DEFAULT_DEVICE_ID = "device-ble-01"
    }

    private var iotHub: IIotHub? = null
    private var serviceBinder: IBinder? = null

    private val _connectionState = MutableStateFlow(ConnectionState.DISCONNECTED)
    val connectionState: StateFlow<ConnectionState> = _connectionState.asStateFlow()

    private val _deviceState = MutableStateFlow<DeviceState?>(null)
    val deviceState: StateFlow<DeviceState?> = _deviceState.asStateFlow()

    /**
     * AIDL Callback implementation receiving Protobuf byte arrays from the service.
     */
    private val callback = object : IIotCallback.Stub() {
        override fun onDeviceStateChanged(protobufPayload: ByteArray?) {
            if (protobufPayload == null) return
            try {
                val parsedState = DeviceState.parseFrom(protobufPayload)
                Log.d(TAG, "Received state update via IPC: temp=${parsedState.temperature}°C, isOn=${parsedState.isOn}")
                _deviceState.value = parsedState
            } catch (e: Exception) {
                Log.e(TAG, "Failed to parse DeviceState protobuf payload", e)
            }
        }
    }

    /**
     * DeathRecipient to monitor if the isolated :iot_core service process dies unexpectedly.
     */
    private val deathRecipient = IBinder.DeathRecipient {
        Log.w(TAG, "CoreIotService process died unexpectedly!")
        handleServiceDisconnected()
    }

    private val serviceConnection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            Log.d(TAG, "Connected to CoreIotService Binder interface")
            serviceBinder = service
            iotHub = IIotHub.Stub.asInterface(service)

            try {
                // Link death recipient to be notified if service process crashes
                service?.linkToDeath(deathRecipient, 0)

                // Register callback to start receiving telemetry
                iotHub?.registerCallback(callback)
                _connectionState.value = ConnectionState.CONNECTED
            } catch (e: RemoteException) {
                Log.e(TAG, "Error registering callback after service connection", e)
                handleServiceDisconnected()
            }
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            Log.w(TAG, "Service disconnected: $name")
            handleServiceDisconnected()
        }

        override fun onBindingDied(name: ComponentName?) {
            Log.e(TAG, "Binding died for component: $name")
            handleServiceDisconnected()
        }
    }

    /**
     * Initiates binding to the Core Service.
     */
    fun connect() {
        if (_connectionState.value != ConnectionState.DISCONNECTED) return

        _connectionState.value = ConnectionState.CONNECTING
        val intent = Intent(context, CoreIotService::class.java)
        val bound = context.bindService(intent, serviceConnection, Context.BIND_AUTO_CREATE)

        if (!bound) {
            Log.e(TAG, "Failed to bind to CoreIotService")
            _connectionState.value = ConnectionState.DISCONNECTED
        }
    }

    /**
     * Sends a toggle command to the Core Service via AIDL.
     */
    fun sendToggleCommand(turnOn: Boolean) {
        val hub = iotHub
        if (hub == null || _connectionState.value != ConnectionState.CONNECTED) {
            Log.w(TAG, "Cannot send command: Service is not connected")
            return
        }

        try {
            val command = DeviceCommand.newBuilder()
                .setId(DEFAULT_DEVICE_ID)
                .setTurnOn(turnOn)
                .build()

            Log.d(TAG, "Sending command via IPC: turnOn=$turnOn")
            hub.sendCommand(command.toByteArray())
        } catch (e: RemoteException) {
            Log.e(TAG, "RemoteException sending command to CoreIotService", e)
            handleServiceDisconnected()
        }
    }

    /**
     * Unbinds from the Core Service and cleans up resources.
     */
    fun disconnect() {
        if (_connectionState.value == ConnectionState.DISCONNECTED) return

        try {
            iotHub?.unregisterCallback(callback)
            serviceBinder?.unlinkToDeath(deathRecipient, 0)
            context.unbindService(serviceConnection)
        } catch (e: Exception) {
            Log.e(TAG, "Error during disconnect", e)
        } finally {
            handleServiceDisconnected()
        }
    }

    private fun handleServiceDisconnected() {
        iotHub = null
        serviceBinder = null
        _deviceState.value = null
        _connectionState.value = ConnectionState.DISCONNECTED
    }
}
