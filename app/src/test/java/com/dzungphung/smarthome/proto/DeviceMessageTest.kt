package com.dzungphung.smarthome.proto

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Unit test for Protobuf payload serialization and deserialization.
 * Simulates IPC binary payload transfers between processes.
 */
class DeviceMessageTest {

    @Test
    fun `DeviceState serialization and deserialization retains all field values`() {
        val originalState = DeviceState.newBuilder()
            .setId("ble-device-99")
            .setTemperature(26)
            .setIsOn(true)
            .build()

        // Serialize to byte array (simulating IPC transfer)
        val byteArray = originalState.toByteArray()

        // Deserialize back from byte array
        val parsedState = DeviceState.parseFrom(byteArray)

        assertEquals("ble-device-99", parsedState.id)
        assertEquals(26, parsedState.temperature)
        assertTrue(parsedState.isOn)
    }

    @Test
    fun `DeviceCommand toggle command serializes and deserializes correctly`() {
        val commandOff = DeviceCommand.newBuilder()
            .setId("ble-device-99")
            .setTurnOn(false)
            .build()

        val byteArray = commandOff.toByteArray()
        val parsedCommand = DeviceCommand.parseFrom(byteArray)

        assertEquals("ble-device-99", parsedCommand.id)
        assertFalse(parsedCommand.turnOn)
    }
}
