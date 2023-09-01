package com.example.dronepilot

import android.content.Context
import android.content.Intent
import android.content.pm.ActivityInfo
import android.graphics.Color
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.MenuItem
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.widget.Toolbar
import com.example.dronepilot.gestureFragment.CameraFragment
import com.o3dr.android.client.interfaces.DroneListener
import com.o3dr.android.client.interfaces.TowerListener
import com.o3dr.services.android.lib.drone.attribute.AttributeType
import com.o3dr.services.android.lib.drone.property.Altitude
import com.o3dr.services.android.lib.drone.property.Battery
import com.o3dr.services.android.lib.drone.property.Speed
import com.o3dr.services.android.lib.drone.property.State

class PhoneMovementsActivity : AppCompatActivity(), SensorEventListener, DroneListener, TowerListener {

    private lateinit var connectBtn: Button
    private lateinit var armBtn: Button
    private lateinit var stopBtn: Button
    private lateinit var resTextView: TextView

    private val handler: Handler = Handler(Looper.getMainLooper())
    private lateinit var droneClient:DroneClass

    private lateinit var gyroscope: Sensor
    private lateinit var sensorManager: SensorManager
    private val parametersUpdateHandler = Handler()

    private lateinit var batteryPercentageTextViewM: TextView
    private lateinit var velocityTextViewM: TextView
    private lateinit var highTextViewM: TextView


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_phone_movements)

        //Fija la orientacion a landscape
        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE

        //Inicializacion del giroscopio
        sensorManager = getSystemService(Context.SENSOR_SERVICE) as SensorManager
        gyroscope = sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE)

        //Registrar el listener del giroscopio
        sensorManager.registerListener(
            this,
            gyroscope,
            SensorManager.SENSOR_DELAY_NORMAL
        )

        //Inicializar el dron
        droneClient = DroneClass.getDroneInstance(this)

        //Inicializa el runnable que maneja los cambios de parametros
        parametersUpdateHandler.postDelayed(parametersUpdateRunnable, 0)

        val toolbar: Toolbar = findViewById(R.id.toolbarM)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayShowTitleEnabled(false)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        val imageView: ImageView = findViewById(R.id.drone_toolbarM)
        imageView.setOnClickListener {
            showImageDialog()
        }

        //Inicializacion de botones y TextView
        connectBtn = findViewById(R.id.connectBtn)
        connectBtn.setOnClickListener { connectDrone() }
        armBtn = findViewById(R.id.armBtn)
        armBtn.setOnClickListener { armDrone() }
        stopBtn = findViewById(R.id.stopBtn)
        stopBtn.setOnClickListener { stopDrone() }
        resTextView = findViewById(R.id.resTextView)
        batteryPercentageTextViewM = findViewById(R.id.batteryPercentageTxtM)
        velocityTextViewM = findViewById(R.id.velocityValueTxtM)
        highTextViewM = findViewById(R.id.altitudeValueTxtM)
    }

    /**
     * Runnable que muestra y actualiza los parámetros del dron
     */
    private val parametersUpdateRunnable = object : Runnable {
        override fun run() {
            //Comprueba si el dron esta conectado
            if (droneClient.drone.isConnected) {
                val vehicleBattery = droneClient.drone.getAttribute<Battery>(AttributeType.BATTERY)
                //Actualiza la información de la bateria
                batteryPercentageTextViewM.text = "${vehicleBattery.batteryRemain.toInt()}%"

                // Si el porcentaje es menor al 70, cambia el color de la letra a rojo
                if (droneClient.drone.isConnected && vehicleBattery.batteryRemain.toInt() < 70) {
                    batteryPercentageTextViewM.setTextColor(Color.RED)
                } else {
                    //Si no, la letra se visualiza en negro
                    batteryPercentageTextViewM.setTextColor(Color.BLACK)
                }

                //Actualiza la información de la velocidad
                val vehicleVelocity = droneClient.drone.getAttribute<Speed>(AttributeType.SPEED)
                velocityTextViewM.text = "${formatDecimalValue(vehicleVelocity.groundSpeed)} m/s"

                //Actualiza la información de la altitud
                val vehicleAltitude =
                    droneClient.drone.getAttribute<Altitude>(AttributeType.ALTITUDE)
                highTextViewM.text = "${formatDecimalValue(vehicleAltitude.altitude / 100)}m"
            }
            //Repite la ejecucion del runnable cada 1s.
            parametersUpdateHandler.postDelayed(this, 1000)
        }
    }

    /**
     * Función para mostrar un cuadro de dialogo con las instrucciones de uso
     */
    private fun showImageDialog() {
        val imageDialog = AlertDialog.Builder(this)
        val inflater = this.layoutInflater
        val view = inflater.inflate(R.layout.image_commands_gesture, null)
        val dialogImage: ImageView = view.findViewById(R.id.dialogImageView)

        dialogImage.setImageResource(R.drawable.com_mov)
        DroneClass.stopMoving()
        imageDialog.setView(view)
        imageDialog.setPositiveButton("Cerrar") { dialog, _ ->
            dialog.dismiss()
        }

        val dialog = imageDialog.create()
        dialog.show()
    }

    /**
     * Funcion que para volver atras cuando pulsas el boton atras
     */
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == android.R.id.home) {
            onBackPressed()
            return true
        }
        return super.onOptionsItemSelected(item)
    }

    /**
     * Funcion que formatea un valor decimal a un string
     */
    private fun formatDecimalValue(value: Double): String {
        return String.format("%.2f", value)
    }

    /**
     * Funcion que para el dron
     */
    private fun stopDrone() {
        DroneClass.moveInDirection("stop")
    }

    /**
     * Funcion que gestiona las acciones del dron: arm, takeoff, RTL
     */
    private fun armDrone() {
        DroneClass.arm()
    }

    /**
     * Funcion que gestiona la conexion/desconenxion del dron
     */
    private fun connectDrone() {
        DroneClass.connect(false)
    }

    /**
     * Funcion que maneja los eventos del dron
     */
    override fun onDroneEvent(event: String?, extras: Bundle?) {
        DroneClass.droneEvent(event, armBtn, connectBtn)
    }

    /**
     * Funcion que maneja la interrupcion del servicio de dron
     */
    override fun onDroneServiceInterrupted(errorMsg: String?) {
        Log.d("PhoneMovementsActivity", "Drone service interrumpted: $errorMsg")
    }

    /**
    * Funcion que maneja la accion cuando se conecta la torre
    */
    override fun onTowerConnected() {
        droneClient.controlTower.registerDrone(droneClient.drone, handler)
        droneClient.drone.registerDroneListener(this)
    }

    /**
     * Funcion que maneja la accion cuando se desconecta la torre
     */
    override fun onTowerDisconnected() {
        Log.d("PhoneMovementsActivity", "Tower disconnect")
    }

    /**
     * Función que se ejecuta al iniciar la activity
     */
    override fun onStart() {
        super.onStart()
        droneClient.controlTower.connect(this)
    }

    /**
     * Función que se ejecuta al destruir la activity
     */
    override fun onDestroy() {
        super.onDestroy()
        DroneClass.onDestroy()

        if (droneClient.drone.isConnected) {
            droneClient.drone.disconnect()
            DroneClass.updateConnectedButton(false,connectBtn)
        }

    }

    /**
     * Función que se ejecuta al detener la activity
     */
    override fun onStop() {
        super.onStop()
        if (droneClient.drone.isConnected) {
            droneClient.drone.disconnect()
            DroneClass.updateConnectedButton(false,connectBtn)
        }
        DroneClass.stopMoving()
        droneClient.controlTower.unregisterDrone(droneClient.drone)
        droneClient.controlTower.disconnect()

    }

    /**
     * Función que se ejecuta al pausar el fragment
     */
    override fun onResume() {
        super.onResume()
        parametersUpdateHandler.postDelayed(parametersUpdateRunnable, 0)
    }

    /**
     * Función que se ejecuta al pausar el fragment
     */
    override fun onPause() {
        super.onPause()
        parametersUpdateHandler.removeCallbacks(parametersUpdateRunnable)
        DroneClass.stopMoving()
    }

    /**
     * Funcion que detecta y procesa los cambios del giroscopio
     * @param event contiene informacion sobre el evento del sensor, como
     * su valor y el tipo de sensor
     */
    override fun onSensorChanged(event: SensorEvent?) {
        if (event == null || event.accuracy == SensorManager.SENSOR_STATUS_UNRELIABLE) return

        val sensorType = event.sensor.type

        //Confirmamos que el evento recibido es del giroscopio
        if (sensorType == Sensor.TYPE_GYROSCOPE) {
            val x = event.values[0] // Velocidad angular en el eje X
            val y = event.values[1] // Velocidad angular en el eje Y
            val vehicleState = droneClient.drone.getAttribute<State>(AttributeType.STATE)

            val isDroneFlying = vehicleState.isFlying
            //Si el dron esta volando, traducir los movimientos del dispositivo a comandos del dron
            if (isDroneFlying) {
                when {
                    x < -0.7 -> {
                        resTextView.text = "left"
                        DroneClass.moveInDirection("west")
                    }
                    x > 0.7 -> {
                        resTextView.text = "right"
                        DroneClass.moveInDirection("east")
                    }
                    y < -0.7 -> {
                        resTextView.text = "back"
                        DroneClass.moveInDirection("south")
                    }
                    y > 0.7 -> {
                        resTextView.text = "forward"
                        DroneClass.moveInDirection("north")
                    }
                    else -> resTextView.text = " "
                }
            }
        }
    }

    /**
     * Funcion que llama cuando cambia la precision del sensor (no se implementa en esta aplicacion)
     */
    override fun onAccuracyChanged(p0: Sensor?, p1: Int) {
    }

    /**
     * Funcion que se ctiva cuando e presiona el boton atras
     */
    override fun onBackPressed(){
        val intent = Intent(this, MainActivity::class.java)
        startActivity(intent)
        if (droneClient.drone.isConnected) {
            droneClient.drone.disconnect()
            DroneClass.updateConnectedButton(false,connectBtn)
        }
        droneClient.controlTower.unregisterDrone(droneClient.drone)
        droneClient.controlTower.disconnect()
        DroneClass.stopMoving()
        finish()
    }

}