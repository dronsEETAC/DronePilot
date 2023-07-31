package com.example.dronepilot

import android.content.Context
import android.content.Intent
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import com.o3dr.android.client.interfaces.DroneListener
import com.o3dr.android.client.interfaces.TowerListener
import com.o3dr.services.android.lib.drone.attribute.AttributeType
import com.o3dr.services.android.lib.drone.property.State
import kotlinx.coroutines.*

class PhoneMovementsActivity : AppCompatActivity(), SensorEventListener, DroneListener, TowerListener {

    private lateinit var connectBtn: Button
    private lateinit var armBtn: Button
    private lateinit var rtlBtn: Button
    private lateinit var stopBtn: Button
    private lateinit var eastBtn: Button
    private lateinit var westBtn: Button
    private lateinit var southBtn: Button

    private lateinit var resTextView: TextView

    private val handler: Handler = Handler(Looper.getMainLooper())
    private lateinit var droneClient:DroneClass

    private lateinit var gyroscope: Sensor
    private lateinit var sensorManager: SensorManager

    private var isDroneFlying = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_phone_movements)

        sensorManager = getSystemService(Context.SENSOR_SERVICE) as SensorManager
        gyroscope = sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE)

        sensorManager.registerListener(
            this,
            gyroscope,
            SensorManager.SENSOR_DELAY_NORMAL
        )
        droneClient = DroneClass.getDroneInstance(this)

        connectBtn = findViewById(R.id.connectBtn)
        connectBtn.setOnClickListener { connectDrone() }

        armBtn = findViewById(R.id.armBtn)
        armBtn.setOnClickListener { armDrone() }

        stopBtn = findViewById(R.id.stopBtn)
        stopBtn.setOnClickListener { stopDrone() }

        southBtn = findViewById(R.id.southBtn)
        southBtn.setOnClickListener { goSouthEast() }

        eastBtn = findViewById(R.id.eastBtn)
        eastBtn.setOnClickListener { goSouthWest() }

        westBtn = findViewById(R.id.westBtn)
        westBtn.setOnClickListener { goNorthWest() }

        rtlBtn = findViewById(R.id.RTLBtn)
        rtlBtn.setOnClickListener { goNorthEast() }
        resTextView = findViewById(R.id.resTextView)
    }

    private fun stopDrone() {
        DroneClass.movementJob?.cancel()
        DroneClass.movementJob = null
        if (DroneClass.movementJob == null || DroneClass.movementJob?.isCancelled == true) {
            DroneClass.movementJob = DroneClass.moveInDirection("stop", 0f)
        }
    }

    private fun armDrone() {
        DroneClass.arm()
    }

    private fun connectDrone() {
        DroneClass.connect()
    }

    override fun onDroneEvent(event: String?, extras: Bundle?) {
        DroneClass.droneEvent(event,extras, armBtn, connectBtn)
    }

    override fun onDroneServiceInterrupted(errorMsg: String?) {
        Toast.makeText(this@PhoneMovementsActivity, "Drone service interrumpted: $errorMsg", Toast.LENGTH_LONG)
    }

    override fun onTowerConnected() {
        droneClient.controlTower.registerDrone(droneClient.drone, handler)
        droneClient.drone.registerDroneListener(this)
    }

    override fun onTowerDisconnected() {
        Toast.makeText(this@PhoneMovementsActivity, "Tower disconnect", Toast.LENGTH_LONG)
    }

    override fun onStart() {
        super.onStart()
        droneClient.controlTower.connect(this)
    }

    override fun onStop() {
        super.onStop()
        if (droneClient.drone.isConnected) {
            droneClient.drone.disconnect()
            DroneClass.updateConnectedButton(false,connectBtn)
        }
        droneClient.controlTower.unregisterDrone(droneClient.drone)
        droneClient.controlTower.disconnect()
    }

    private fun goNorth(){
        DroneClass.movementJob?.cancel()
        DroneClass.movementJob = null
        if (DroneClass.movementJob == null || DroneClass.movementJob?.isCancelled == true) {
            DroneClass.movementJob = DroneClass.moveInDirection("north", 5f)
        }
    }

    private fun goSur(){
        DroneClass.movementJob?.cancel()
        DroneClass.movementJob = null
        if (DroneClass.movementJob == null || DroneClass.movementJob?.isCancelled == true) {
            DroneClass.movementJob = DroneClass.moveInDirection("south", 5f)
        }
    }

    private fun goEast(){
        DroneClass.movementJob?.cancel()
        DroneClass.movementJob = null
        if (DroneClass.movementJob == null || DroneClass.movementJob?.isCancelled == true) {
            DroneClass.movementJob = DroneClass.moveInDirection("east", 5f)
        }
    }
    private fun goWest(){
        DroneClass.movementJob?.cancel()
        DroneClass.movementJob = null
        if (DroneClass.movementJob == null || DroneClass.movementJob?.isCancelled == true) {
            DroneClass.movementJob = DroneClass.moveInDirection("west", 5f)
        }
    }

    private fun goSouthWest(){
        DroneClass.movementJob?.cancel()
        DroneClass.movementJob = null
        if (DroneClass.movementJob == null || DroneClass.movementJob?.isCancelled == true) {
            DroneClass.movementJob = DroneClass.moveInDirection("southWest", 5f)
        }
    }

    private fun goSouthEast(){
        DroneClass.movementJob?.cancel()
        DroneClass.movementJob = null
        if (DroneClass.movementJob == null || DroneClass.movementJob?.isCancelled == true) {
            DroneClass.movementJob = DroneClass.moveInDirection("southEast", 5f)
        }
    }

    private fun goNorthWest(){
        DroneClass.movementJob?.cancel()
        DroneClass.movementJob = null
        if (DroneClass.movementJob == null || DroneClass.movementJob?.isCancelled == true) {
            DroneClass.movementJob = DroneClass.moveInDirection("northWest", 5f)
        }
    }

    private fun goNorthEast(){
        DroneClass.movementJob?.cancel()
        DroneClass.movementJob = null
        if (DroneClass.movementJob == null || DroneClass.movementJob?.isCancelled == true) {
            DroneClass.movementJob = DroneClass.moveInDirection("northEast", 5f)
        }
    }

    override fun onSensorChanged(event: SensorEvent?) {
        if (event == null) return
        if (event.accuracy == SensorManager.SENSOR_STATUS_UNRELIABLE) return

        val sensorType = event.sensor.type

        if (sensorType == Sensor.TYPE_GYROSCOPE) {
            val x = event.values[0] // Velocidad angular en el eje X
            val y = event.values[1] // Velocidad angular en el eje Y
            val z = event.values[2] // Velocidad angular en el eje Z
            val vehicleState = droneClient.drone.getAttribute<State>(AttributeType.STATE)

            isDroneFlying = vehicleState.isFlying

            if (isDroneFlying) {
                when {
                    x < -0.7 -> moveDrone("left")
                    x > 0.7 -> moveDrone("right")
                    y < -0.7 -> moveDrone("back")
                    y > 0.7 -> moveDrone("forward")
                    x > 0.6 && y < -0.6 -> moveDrone("southEast")
                    x < -0.6 && y < -0.6 -> moveDrone("southWest")
                    x > 0.6 && y > 0.6 -> moveDrone("northEast")
                    x < -0.6 && y > 0.6 -> moveDrone("northWest")
                    else -> resTextView.text = " "
                }
            }
        }
    }

    private fun moveDrone(direction: String) {
        DroneClass.movementJob?.cancel()
        DroneClass.movementJob = DroneClass.moveInDirection(direction, 5f)
        resTextView.text = "Inclinación hacia $direction"
    }

    override fun onAccuracyChanged(p0: Sensor?, p1: Int) {

    }

    override fun onBackPressed(){
        val intent = Intent(this, MainActivity::class.java)
        startActivity(intent)
        if (droneClient.drone.isConnected) {
            droneClient.drone.disconnect()
            DroneClass.updateConnectedButton(false,connectBtn)
        }
        droneClient.controlTower.unregisterDrone(droneClient.drone)
        droneClient.controlTower.disconnect()
        finish()
    }

}