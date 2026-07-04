package com.dzungphung.smarthome.core

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class FakeBleRepositoryTest {

    @Test
    fun `deviceStateFlow emits state with valid default values and temperature range`() = runTest {
        val repository = FakeBleRepository()

        val emittedState = repository.deviceStateFlow.first()

        assertEquals("device-ble-01", emittedState.id)
        assertTrue("Temperature should be >= 20", emittedState.temperature >= 20)
        assertTrue("Temperature should be <= 30", emittedState.temperature <= 30)
        assertTrue("Device should default to ON", emittedState.isOn)
    }

    @Test
    fun `setDeviceOnState updates isOn property in emitted flow`() = runTest {
        val repository = FakeBleRepository()

        repository.setDeviceOnState(false)

        val emittedState = repository.deviceStateFlow.first()
        assertFalse("Device state should reflect toggle to OFF", emittedState.isOn)
    }
}
