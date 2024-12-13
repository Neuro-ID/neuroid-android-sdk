package com.neuroid.tracker.models

interface IntegrationHealthProtocol {
    // methods NID class relies on
    fun startIntegrationHealthCheck()
    fun captureIntegrationHealthEvent(event: NIDEventModel)
    fun saveIntegrationHealthEvents()

    // public methods User can access
    fun printIntegrationHealthInstruction()
    fun setVerifyIntegrationHealth(verify: Boolean)
}