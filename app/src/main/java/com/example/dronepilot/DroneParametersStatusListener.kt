package com.example.dronepilot

interface DroneParametersStatusListener {
    fun onBatteryPercentageChanged(percentage: Int)

    fun onVelocityChanged(velocity: Double)

    fun onHighChanged(high: Double)
}