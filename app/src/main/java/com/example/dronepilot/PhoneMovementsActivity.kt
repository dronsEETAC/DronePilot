package com.example.dronepilot

import android.content.Context
import android.content.Intent
import android.content.pm.ActivityInfo
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
import java.util.concurrent.TimeUnit

class PhoneMovementsActivity : AppCompatActivity(), SensorEventListener, DroneListener, TowerListener {

    private lateinit var connectBtn: Button
    private lateinit var armBtn: Button
    private lateinit var stopBtn: Button
    private lateinit var resTextView: TextView

    private val handler: Handler = Handler(Looper.getMainLooper())
    private lateinit var droneClient:DroneClass

    private lateinit var gyroscope: Sensor
    private lateinit var sensorManager: SensorManager

    private var isDroneFlying = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_phone_movements)

        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE

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

    override fun onDestroy() {
        super.onDestroy()
        DroneClass.onDestroy()

        if (droneClient.drone.isConnected) {
            droneClient.drone.disconnect()
            DroneClass.updateConnectedButton(false,connectBtn)
        }

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

            //if (isDroneFlying) {
                when {
                    x < -0.7 -> {
                        resTextView.text = "left"
                        moveDrone("left")
                    }
                    x > 0.7 -> {
                        resTextView.text = "right"
                        moveDrone("right")
                    }
                    y < -0.7 -> {
                        resTextView.text = "back"
                        moveDrone("back")
                    }
                    y > 0.7 -> {
                        resTextView.text = "forward"
                        moveDrone("forward")
                    }
                    else -> resTextView.text = " "
                }
            //}
        }
    }

    private fun moveDrone(direction: String) {
        DroneClass.movementJob?.cancel()
        DroneClass.movementJob = DroneClass.moveInDirectionWithOutHeading(direction, 5f)
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