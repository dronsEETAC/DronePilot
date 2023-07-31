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

    var movementJob: Job? = null

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
        southBtn.setOnClickListener { goSur() }

        eastBtn = findViewById(R.id.eastBtn)
        eastBtn.setOnClickListener { goEast() }

        westBtn = findViewById(R.id.westBtn)
        westBtn.setOnClickListener { goWest() }

        rtlBtn = findViewById(R.id.RTLBtn)
        rtlBtn.setOnClickListener { goNorth() }
        resTextView = findViewById(R.id.resTextView)
    }

    private fun stopDrone() {
        movementJob?.cancel()
        movementJob = null

        DroneClass.moveDrone("north", 0f)
    }

    private fun armDrone() {
        movementJob?.cancel()
        movementJob = null
        DroneClass.arm()
    }

    private fun connectDrone() {
        DroneClass.connect()
    }

    fun moveInDirection(direction: String, velocity: Float) = GlobalScope.launch {
        while (isActive) {
            DroneClass.moveDrone(direction, velocity)
            delay(100)
        }
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
        movementJob?.cancel()
        movementJob = null
        //DroneClass.moveDrone("north", 5f)
        if (movementJob == null || movementJob?.isCancelled == true) {
            movementJob = moveInDirection("north", 5f)
        }
    }

    private fun goSur(){
        movementJob?.cancel()
        movementJob = null
        //DroneClass.moveDrone("south", 5f)
        if (movementJob == null || movementJob?.isCancelled == true) {
            movementJob = moveInDirection("south", 5f)
        }
    }

    private fun goEast(){
        movementJob?.cancel()
        movementJob = null
        //DroneClass.moveDrone("east", 5f)
        if (movementJob == null || movementJob?.isCancelled == true) {
            movementJob = moveInDirection("east", 5f)
        }
    }
    private fun goWest(){
        movementJob?.cancel()
        movementJob = null
        //DroneClass.moveDrone("west", 5f)
        if (movementJob == null || movementJob?.isCancelled == true) {
            movementJob = moveInDirection("west", 5f)
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

            if (x < -0.5) {
                resTextView.text = "Inclinación hacia la izquierda"
                //DroneClass.west()
                Log.d("Giroscopio", "izquierda")
            } else if (x > 0.5) {
                resTextView.text = "Inclinación hacia la derecha"
                //DroneClass.east()
                Log.d("Giroscopio", "derecha")
            } else if (y < -0.5) {
                resTextView.text = "Inclinación hacia delante"
                Log.d("Giroscopio", "delante")
            } else if (y > 0.5) {
                resTextView.text = "Inclinación hacia atras"
                Log.d("Giroscopio", "atrás")

            }
            else{
                resTextView.text = " "
            }
        }
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