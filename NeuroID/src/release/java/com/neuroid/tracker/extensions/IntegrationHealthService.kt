package com.neuroid.tracker.extensions

import com.neuroid.tracker.NeuroID
import com.neuroid.tracker.NeuroIDPublic
import com.neuroid.tracker.models.IntegrationHealthProtocol
import com.neuroid.tracker.models.NIDEventModel
import com.neuroid.tracker.utils.NIDLogWrapper

class IntegrationHealthService(
    private val logger:NIDLogWrapper,
    private val neuroID: NeuroID,
):IntegrationHealthProtocol {
    override fun startIntegrationHealthCheck() {
    }

    override fun captureIntegrationHealthEvent(event: NIDEventModel) {
    }

    override fun saveIntegrationHealthEvents() {
    }

    override fun printIntegrationHealthInstruction() {
        logger.i(msg="No Debug Module Found")
    }

    override fun setVerifyIntegrationHealth(verify: Boolean) {
        logger.i(msg="No Debug Module Found")
    }
}

// Public Extensions
fun NeuroIDPublic.printIntegrationHealthInstruction() {
    this.printIntegrationHealthInstruction()
}

fun NeuroIDPublic.setVerifyIntegrationHealth(verify: Boolean) {
    this.setVerifyIntegrationHealth(verify)
}