package com.example.dronepilot

interface DroneParametersStatusListener {
    /**
     * Funcion que se llama cuando cambia el porcentaje de batería del dron
     */
    fun onBatteryPercentageChanged(percentage: Int, isConnected: Boolean)

    /**
     * Funcion que se llama cuando la velocidad del dron
     */
    fun onVelocityChanged(velocity: Double)

    /**
     * Funcion que se llama cuando la altitud del dron
     */
    fun onAltitudeChanged(high: Double)
}