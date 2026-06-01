package com.nicholasbergesen.gunsout.feature.body

import java.util.concurrent.atomic.AtomicBoolean

class InBodyScanGate {
    private val busy = AtomicBoolean(false)
    private val accepted = AtomicBoolean(false)

    fun tryStart(): Boolean =
        !accepted.get() && busy.compareAndSet(false, true)

    fun finish(acceptedResult: Boolean) {
        if (acceptedResult) {
            accepted.set(true)
        }
        busy.set(false)
    }
}
