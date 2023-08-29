package com.example.dronepilot

import android.content.Context
import android.os.Bundle
import android.os.Handler
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.Toast
import com.MAVLink.enums.MAV_CMD.MAV_CMD_CONDITION_YAW
import com.o3dr.android.client.ControlTower
import com.o3dr.android.client.Drone
import com.o3dr.android.client.apis.ControlApi
import com.o3dr.android.client.apis.ExperimentalApi
import com.o3dr.android.client.apis.VehicleApi
import com.o3dr.android.client.interfaces.LinkListener
import com.o3dr.services.android.lib.drone.attribute.AttributeEvent
import com.o3dr.services.android.lib.drone.attribute.AttributeType
import com.o3dr.services.android.lib.drone.connection.ConnectionParameter
import com.o3dr.services.android.lib.drone.property.Parameter
import com.o3dr.services.android.lib.drone.property.State
import com.o3dr.services.android.lib.drone.property.VehicleMode
import com.o3dr.services.android.lib.gcs.link.LinkConnectionStatus
import com.o3dr.services.android.lib.mavlink.MavlinkMessageWrapper
import com.o3dr.services.android.lib.model.AbstractCommandListener
import com.o3dr.services.android.lib.model.SimpleCommandListener
import kotlinx.coroutines.*

class DroneClass private constructor(context: Context){
    lateinit var drone: Drone
    lateinit var controlTower : ControlTower
    var context:Context = context

    companion object{
        private var droneInstance : DroneClass? = null
        //private const val serverIP: String = "172.20.10.3" //Internet movil
        //private const val serverIP: String = "192.168.0.128" //Casa
        private const val serverIP: String = "192.168.0.18" //Piso
        private const val usbBaudRate : Int = 57600
        private const val serverPort : Int = 5763
        val handler = Handler()
        var currentDirection: String? = null
        //var currentVelocity: Float = 0f
        private const val TAKEOFF_ALTITUDE = 7.0
        private const val VEL_DRONE = 5f

        /**
         * Funcion que proporciona una instancia de la clase DroneClass
         * @param context Contexto de la aplicación o actividad actual
         */
        fun getDroneInstance(context: Context): DroneClass {
            //Crea una nueva instancia de DroneClass
            droneInstance = DroneClass(context)
            //Inicializa drone utilizando el contexto proporcionado
            droneInstance!!.drone = Drone(context)
            //Inicializa controlTower con el mismo contexto
            droneInstance!!.controlTower = ControlTower(context)
            //Devuelve la instancia
            return droneInstance!!
        }

/*
        /** Funcion para conectarse al dron mediante TCP */
        fun connect() {
            //Comprueba si el dron esta conectado
            if (droneInstance!!.drone.isConnected) {
                //En caso afirmativo, se desconecta
                droneInstance!!.drone.disconnect()
            } else {
                //Se define el parametro de conexion de tipo TCP pasandole como parametro la IP y el puerto donde
                //esta ejecutandose el simulador
                val connectionParams = ConnectionParameter.newTcpConnection(serverIP,serverPort, null)
                //Se realiza la conexión al dron
                droneInstance!!.drone.connect(connectionParams,  object : LinkListener {
                    //Se escucha el canal y notifica al usario de los cambios producidos
                    override fun onLinkStateUpdated(connectionStatus: LinkConnectionStatus) {
                        alertUser("connectionStatusChanged: $connectionStatus")
                    }

                })
            }
        }
        /** Funcion para conectarse al dron mediante USB */
        fun connectUSB() {
            //Comprueba si el dron esta conectado
            if (droneInstance!!.drone.isConnected) {
                droneInstance!!.drone.disconnect() //En caso afirmativo, desconecta el dron
            } else {
                //Se define el parametro de conexion de tipo USB y se pasa como parametro la velocidad de transmision
                val connectionParams = ConnectionParameter.newUsbConnection(usbBaudRate,null)
                //Se realiza la conexión al dron
                droneInstance!!.drone.connect(connectionParams, object : LinkListener {
                    override fun onLinkStateUpdated(connectionStatus: LinkConnectionStatus) {
                        //Se informa al usuario de los cambios que se producen en el canal
                        alertUser("connectionStatusChanged: $connectionStatus")
                    }
                })
            }
        }*/

        /**
         * Conecta el dron con la aplicacion según el tipo de conexión requerido
         * @param isUSBConnection true si se quiere conectar mediante USB
         * false si se quiere conectar mediante TCP
         */
        fun connect(isUSBConnection: Boolean = false) {
            //Se asegura de que droneInstance no sea nulo
            droneInstance?.let { instance ->
                if (instance.drone.isConnected) {
                    instance.drone.disconnect()
                } else {
                    val connectionParams = if (isUSBConnection) {
                        ConnectionParameter.newUsbConnection(usbBaudRate, null)
                    } else {
                        ConnectionParameter.newTcpConnection(serverIP, serverPort, null)
                    }
                    instance.drone.connect(connectionParams, object : LinkListener {
                        override fun onLinkStateUpdated(connectionStatus: LinkConnectionStatus) {
                            Log.d("DRONE_CONNECTION", "connectionStatusChanged: $connectionStatus")
                        }
                    })
                }
                //Se maneja la excepcion en caso de ser nulo y se avisa al usuario
            } ?: alertUser("Drone instance is not initialized.")
        }

        /**
         * Realiza los comandos basicos: arm, take off, RTL segun el estado del dron
         */
        fun arm() {
            val vehicleState = droneInstance?.drone?.getAttribute<State>(AttributeType.STATE) //Obtiene el estado del dron
            when {
                vehicleState?.isFlying == true -> handleFlyingState()
                vehicleState?.isArmed == true -> handleArmedState()
                vehicleState?.isConnected == false-> alertUser("Connect to a drone first")
                vehicleState?.isConnected == true && !vehicleState.isArmed -> handleConnectedState()
                else -> Log.e("DRONE_ERROR", "Unhandled drone state.")
            }
        }

        /** RTL */
        private fun handleFlyingState(){
            VehicleApi.getApi(droneInstance?.drone).setVehicleMode(VehicleMode.COPTER_RTL, object : AbstractCommandListener() {
                override fun onSuccess() {
                    Log.d("DRONE_STATUS", "Returning to launch position.")
                }
                override fun onError(executionError: Int) {
                    Log.e("DRONE_ERROR", "Error returning to launch: $executionError")
                }
                override fun onTimeout() {
                    Log.w("DRONE_TIMEOUT", "Operation timed out. Check connection.")
                }
            })
        }

        /** Take off */
        private fun handleArmedState(){
            ControlApi.getApi(droneInstance?.drone).takeoff(TAKEOFF_ALTITUDE, object : AbstractCommandListener() {
                override fun onSuccess() {
                    Log.d("DRONE_STATUS", "Taking off...")
                }
                override fun onError(i: Int) {
                    Log.e("DRONE_ERROR", "Unable to take off. Error number: $i.")
                }
                override fun onTimeout() {
                    Log.w("DRONE_TIMEOUT", "Take off timed out. Check connection.")
                }
            })
        }

        /** Arm */
        private fun handleConnectedState(){
            //Habiltar el control manual antes de armar
            ControlApi.getApi(droneInstance?.drone).enableManualControl(true
            ) { isEnabled ->
                if (isEnabled) {
                    VehicleApi.getApi(droneInstance?.drone)
                        .arm(true, false, object : SimpleCommandListener() {
                            override fun onError(executionError: Int) {
                                Log.e("DRONE_ERROR", "Error arming the drone: $executionError.")
                            }
                            override fun onTimeout() {
                                Log.w("DRONE_TIMEOUT","Arming operation timed out. Check connection.")
                            }
                            override fun onSuccess() {
                                Log.d("DRONE_STATUS", "Drone is armed.")
                            }
                        })
                }else{
                    Log.e("DRONE_ERROR", "Unable to enable manual control for arming.")
                }
            }
        }

        /**
         * Define un objeto Runnable que contiene la lógica para mover el dron en una dirección
         */
        private val runnableMoveDirection = object : Runnable {
            override fun run() {
                // Comprueba si se ha establecido una dirección
                if (currentDirection != null) {
                    // Mueve el dron en la dirección especificada
                    moveDrone(currentDirection!!)
                    // Vuelve a ejecutar Runnable tras 1 segundo
                    handler.postDelayed(this, 1000)
                }
            }
        }

        /**
         * Establece la dirección en la que se desea mover el dron y comienza el movimiento
         * @param direction: dirección en la que se desea mover el dron
         */
        fun moveInDirection(direction: String) {
            // Establece la dirección actual a la especificada
            currentDirection = direction
            // Planifica la ejecución del Runnable para iniciar el movimiento del dron
            handler.post(runnableMoveDirection)
        }

        fun stopMoving() {
            handler.removeCallbacks(runnableMoveDirection)
            currentDirection = null
        }

        fun onDestroy() {
            handler.removeCallbacksAndMessages(null) // Limpia todos los mensajes y Runnables
        }

        /**
         * Mueve el dron según los puntos cardinales especificados.
         * @param direction La dirección cardinal en la que se desea mover el dron.
         * Valores posibles: "stop", "north", "south", "east", "west", "northWest", "northEast", "southEast", "southWest".
         */
        fun moveDrone(direction: String) {
            //Definir el tipo de mensaje necesario para mover el dron
            val msg = com.MAVLink.common.msg_set_position_target_local_ned()
            //Establecer los valores iniciales para el mensaje
            msg.time_boot_ms = 0
            msg.target_system = 0
            msg.target_component = 0
            msg.coordinate_frame = 1
            msg.type_mask = 0b0000111111000111

            //Evaluar la direccion especificada para establecer la velocidad necesaria en cada eje
            when (direction) {
                "stop" -> {
                    msg.vx = 0f
                    msg.vy = 0f
                }
                "north" -> {
                    msg.vx = VEL_DRONE
                    msg.vy = 0f
                }
                "south" -> {
                    msg.vx = - VEL_DRONE
                    msg.vy = 0f
                }
                "east" -> {
                    msg.vx = 0f
                    msg.vy = VEL_DRONE
                }
                "west" -> {
                    msg.vx = 0f
                    msg.vy = - VEL_DRONE
                }
                "northWest" -> {
                    msg.vx = VEL_DRONE
                    msg.vy = - VEL_DRONE
                }
                "northEast" -> {
                    msg.vx = VEL_DRONE
                    msg.vy = VEL_DRONE
                }
                "southEast" -> {
                    msg.vx = - VEL_DRONE
                    msg.vy = VEL_DRONE
                }
                "southWest" -> {
                    msg.vx = - VEL_DRONE
                    msg.vy = - VEL_DRONE
                }
            }
            //Definir los valores adicionales del mensaje
            msg.vz = 0f
            msg.x = 0f
            msg.y = 0f
            msg.z = 0f
            msg.afx = 0f
            msg.afy = 0f
            msg.afz = 0f
            msg.yaw = 0f
            msg.yaw_rate = 0f

            //Envolver el mensaje para su envío
            val mavMsg = MavlinkMessageWrapper(msg)
            //Comprobar si el dron esta volando
            if (droneInstance?.drone?.getAttribute<State>(AttributeType.STATE)?.isFlying == true) {
                //Si es asi, enviar el comando al dron
                ExperimentalApi.getApi(droneInstance?.drone).sendMavlinkMessage(mavMsg)
                Log.d("DRONE_MOVEMENT", "Command sent. Direction: $direction at velocity: $VEL_DRONE m/s")
            }else{
                Log.e("DRONE_MOVEMENT_ERROR", "Attempted to move the drone while not flying. Direction: $direction at velocity: $VEL_DRONE m/s")
            }
        }

        fun alertUser(message: String?) {
            Toast.makeText(droneInstance!!.context?.applicationContext, message, Toast.LENGTH_LONG).show()
        }

        /**
         * Funcion para actualizar el texto del boton de conexió¡on
         * @param isConnected Un booleano que indica si el dispositivo está conectado o no
         * @param connectBtn El botón que se desea actualizar
         */
        fun updateConnectedButton(isConnected: Boolean, connectBtn: Button) {
            // Comprueba si el dispositivo esta conectado
            if (isConnected) {
                // Cambia el texto del botón a "Disconnect" para indicar que al pulsarlo se desconectaría el dispositivo
                connectBtn.text = "Disconnect"
            } else {
                // Cambia el texto del botón a "Connect" para indicar que al pulsarlo se conectaría el dispositivo
                connectBtn.text = "Connect"
            }
        }


        private fun updateArmButton(armBtn: Button) {
            val vehicleState = droneInstance?.drone?.getAttribute<State>(AttributeType.STATE)
            if (droneInstance?.drone?.isConnected == false) {
                armBtn.visibility = View.INVISIBLE
            } else {
                armBtn.visibility = View.VISIBLE
            }

            if (vehicleState?.isFlying == true) {
                // RTL
                armBtn.text = "RTL"
            } else if (vehicleState?.isArmed  == true) {
                // Take off
                armBtn.text = "TAKE OFF"
            } else if (vehicleState?.isConnected == true && !vehicleState.isArmed) {
                // Connected but not Armed
                armBtn.text = "ARM"
            }
        }

        /**
         * Reacciona y gestiona diferentes eventos relacionados con el estado del dron
         * @param event evento del dron con el que se va a trabajar
         * @param armBtn Boton para realizar los comandos básicos del dron
         * @param connectBtn Boton para realizar la conexion y desconexion del dron
         */
        fun droneEvent(event: String?, armBtn: Button, connectBtn: Button){
            when (event) {
                //Si el dron se conecta o se desconecta se actualiza el nombre del boton connect
                AttributeEvent.STATE_CONNECTED, AttributeEvent.STATE_DISCONNECTED -> {
                    val isConnected = droneInstance?.drone?.isConnected ?: false
                    updateConnectedButton(isConnected, connectBtn)
                }
                //Si el dron se produce un cambio de estado, si se arma o si se cambia el modo de vehiculo,
                // se actualiza el nombre del boton arm
                AttributeEvent.STATE_UPDATED, AttributeEvent.STATE_ARMING, AttributeEvent.STATE_VEHICLE_MODE -> {
                    updateArmButton(armBtn)
                }
            }
        }
    }
}