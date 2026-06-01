package com.nicholasbergesen.gunsout.feature.body

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class InBodyScanGateTest {

    @Test
    fun `blocks duplicate frames while handling one frame`() {
        val gate = InBodyScanGate()

        assertTrue(gate.tryStart())
        assertFalse(gate.tryStart())
    }

    @Test
    fun `keeps scanning after rejected frame but stops after accepted frame`() {
        val gate = InBodyScanGate()

        assertTrue(gate.tryStart())
        gate.finish(acceptedResult = false)
        assertTrue(gate.tryStart())
        gate.finish(acceptedResult = true)
        assertFalse(gate.tryStart())
    }
}
