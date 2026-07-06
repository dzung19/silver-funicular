package com.dzungphung.smarthome.client

import com.dzungphung.smarthome.proto.DeviceState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class IotClientManagerUnitTest {

    @Test
    fun `device state protobuf payload correctly deserializes into model`() {
        val expectedState = DeviceState.newBuilder()
            .setId("device-ble-01")
            .setTemperature(27)
            .setIsOn(true)
            .build()

        val payload = expectedState.toByteArray()

        val parsed = DeviceState.parseFrom(payload)

        assertNotNull(parsed)
        assertEquals("device-ble-01", parsed.id)
        assertEquals(27, parsed.temperature)
        assertTrue(parsed.isOn)
    }
}
