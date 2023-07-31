package com.example.dronepilot

import android.content.Context
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.Toast
import com.MAVLink.Messages.MAVLinkMessage
import com.MAVLink.enums.MAV_FRAME
import com.o3dr.android.client.ControlTower
import com.o3dr.android.client.Drone
import com.o3dr.android.client.apis.ControlApi
import com.o3dr.android.client.apis.ExperimentalApi
import com.o3dr.android.client.apis.VehicleApi
import com.o3dr.android.client.interfaces.LinkListener
import com.o3dr.services.android.lib.coordinate.LatLong
import com.o3dr.services.android.lib.coordinate.LatLongAlt
import com.o3dr.services.android.lib.drone.action.ExperimentalActions
import com.o3dr.services.android.lib.drone.attribute.AttributeEvent
import com.o3dr.services.android.lib.drone.attribute.AttributeType
import com.o3dr.services.android.lib.drone.connection.ConnectionParameter
import com.o3dr.services.android.lib.drone.property.Altitude
import com.o3dr.services.android.lib.drone.property.Gps
import com.o3dr.services.android.lib.drone.property.State
import com.o3dr.services.android.lib.drone.property.VehicleMode
import com.o3dr.services.android.lib.gcs.link.LinkConnectionStatus
import com.o3dr.services.android.lib.mavlink.MavlinkMessageWrapper
import com.o3dr.services.android.lib.model.AbstractCommandListener
import com.o3dr.services.android.lib.model.SimpleCommandListener
import com.o3dr.services.android.lib.model.action.Action
import kotlinx.coroutines.*
import org.droidplanner.services.android.impl.utils.CommonApiUtils.sendMavlinkMessage
import kotlin.math.cos
import kotlin.math.sin

class DroneClass private constructor(private var context: Context?){
    lateinit var drone: Drone
    lateinit var controlTower : ControlTower

    companion object{
        private var droneInstance : DroneClass? = null
        //private const val serverIP: String = "172.20.10.3" //Internet movil
        //private const val serverIP: String = "192.168.0.128" //Casa
        private const val serverIP: String = "192.168.0.18" //Piso
        private const val usbBaudRate : Int = 57600
        private const val serverPort : Int = 5763
        var movementJob: Job? = null

        fun getDroneInstance(context: Context): DroneClass {
            droneInstance = DroneClass(context.applicationContext)
            droneInstance!!.drone = Drone(context)
            droneInstance!!.controlTower = ControlTower(context)
            return droneInstance!!
        }

        fun connect() {
            if (droneInstance!!.drone.isConnected) {
                droneInstance!!.drone.disconnect()
            } else {
                val connectionParams = ConnectionParameter.newTcpConnection(serverIP,serverPort, null)
                droneInstance!!.drone.connect(connectionParams,  object : LinkListener {
                    override fun onLinkStateUpdated(connectionStatus: LinkConnectionStatus) {
                        alertUser("connectionStatusChanged: $connectionStatus")
                        Log.d(
                            "Connection",
                            "Connection code: ${connectionStatus.statusCode}; Connection status $connectionStatus"
                        )
                    }

                })
            }
        }

        fun connectUSB() {
            if (droneInstance!!.drone.isConnected) {
                droneInstance!!.drone.disconnect()
            } else {
                val connectionParams = ConnectionParameter.newUsbConnection(usbBaudRate,null)
                droneInstance!!.drone.connect(connectionParams, object : LinkListener {
                    override fun onLinkStateUpdated(connectionStatus: LinkConnectionStatus) {
                        alertUser("connectionStatusChanged: $connectionStatus")
                        Log.d(
                            "Connection",
                            "Connection code: ${connectionStatus.statusCode}; Connection status $connectionStatus"
                        )
                    }
                })
            }
        }

        fun arm() {
            val vehicleState = droneInstance!!.drone.getAttribute<State>(AttributeType.STATE)
            if (vehicleState.isFlying) {
                // RTL
                VehicleApi.getApi(droneInstance!!.drone).setVehicleMode(VehicleMode.COPTER_RTL, object : AbstractCommandListener() {
                    override fun onSuccess() {
                        alertUser("Vehicle mode change successful.")
                    }
                    override fun onError(executionError: Int) {
                        alertUser("Vehicle mode change failed: $executionError")
                    }
                    override fun onTimeout() {
                        alertUser("Vehicle mode change timed out.")
                    }
                })
            } else if (vehicleState.isArmed) {
                // Take off
                ControlApi.getApi(droneInstance!!.drone).takeoff(7.0, object : AbstractCommandListener() {
                    override fun onSuccess() {
                        alertUser("Taking off...")
                    }
                    override fun onError(i: Int) {
                        alertUser("Unable to take off: Error number $i.")
                    }
                    override fun onTimeout() {
                        alertUser("Take off time out.")
                    }
                })
            } else if (!vehicleState.isConnected) {
                // Need to connect first
                alertUser("Connect to a drone first")
            } else if (vehicleState.isConnected && !vehicleState.isArmed) {
                // Arm
                ControlApi.getApi(droneInstance!!.drone).enableManualControl(true
                ) { isEnabled ->
                    if (isEnabled) {
                        VehicleApi.getApi(droneInstance!!.drone)
                            .arm(true, false, object : SimpleCommandListener() {
                                override fun onError(executionError: Int) {
                                    alertUser("Unable to arm vehicle : Error number $executionError.")
                                }

                                override fun onTimeout() {
                                    alertUser("Arming operation timed out.")
                                }

                                override fun onSuccess() {
                                    alertUser("Armed")
                                }
                            })
                    }
                }
            }
        }

        fun moveDrone(direction: String, velocity: Float) {
            val msg = com.MAVLink.common.msg_set_position_target_local_ned()
            msg.time_boot_ms = 0
            msg.target_system = 0
            msg.target_component = 0
            msg.coordinate_frame = 1
            msg.type_mask = 0b0000111111000111

            when (direction) {
                "stop" -> {
                    msg.vx = 0f
                    msg.vy = 0f
                }
                "north" -> {
                    msg.vx = velocity
                    msg.vy = 0f
                }
                "south" -> {
                    msg.vx = - velocity
                    msg.vy = 0f
                }
                "east" -> {
                    msg.vx = 0f
                    msg.vy = velocity
                }
                "west" -> {
                    msg.vx = 0f
                    msg.vy = - velocity
                }
                "northWest" -> {
                    msg.vx = velocity
                    msg.vy = - velocity
                }
                "northEast" -> {
                    msg.vx = velocity
                    msg.vy = velocity
                }
                "southEast" -> {
                    msg.vx = - velocity
                    msg.vy = velocity
                }
                "southWest" -> {
                    msg.vx = - velocity
                    msg.vy = - velocity
                }
            }
            msg.vz = 0f
            msg.x = 0f
            msg.y = 0f
            msg.z = 0f
            msg.afx = 0f
            msg.afy = 0f
            msg.afz = 0f
            msg.yaw = 0f
            msg.yaw_rate = 0f

            val mavMsg = MavlinkMessageWrapper(msg)
            ExperimentalApi.getApi(droneInstance!!.drone).sendMavlinkMessage(mavMsg)
        }

        fun moveInDirection(direction: String, velocity: Float) = GlobalScope.launch {
            while (isActive) {
                moveDrone(direction, velocity)
                delay(100)
            }
        }

        fun alertUser(message: String?) {
            Toast.makeText(droneInstance!!.context?.applicationContext, message, Toast.LENGTH_LONG).show()
        }

        fun updateConnectedButton(isConnected: Boolean, connectBtn: Button) {
            if (isConnected) {
                connectBtn.text = "Disconnect"
            } else {
                connectBtn.text = "Connect"
            }
        }

        private fun updateArmButton(armBtn: Button) {
            val vehicleState = droneInstance!!.drone.getAttribute<State>(AttributeType.STATE)
            if (!droneInstance!!.drone.isConnected) {
                armBtn.visibility = View.INVISIBLE
            } else {
                armBtn.visibility = View.VISIBLE
            }

            if (vehicleState.isFlying) {
                // RTL
                armBtn.text = "RTL"
            } else if (vehicleState.isArmed) {
                // Take off
                armBtn.text = "TAKE OFF"
            } else if (vehicleState.isConnected && !vehicleState.isArmed) {
                // Connected but not Armed
                armBtn.text = "ARM"
            }
        }

        fun droneEvent(event: String?, extras: Bundle?, armBtn: Button, connectBtn: Button){
            when (event) {
                AttributeEvent.STATE_CONNECTED -> {
                    alertUser("Drone Connected")
                    updateConnectedButton(droneInstance!!.drone.isConnected, connectBtn)
                }
                AttributeEvent.STATE_DISCONNECTED -> {
                    alertUser("Drone Disconnected")
                    updateConnectedButton(droneInstance!!.drone.isConnected, connectBtn)
                }
                AttributeEvent.STATE_UPDATED -> {
                    updateArmButton(armBtn)
                }
                AttributeEvent.STATE_ARMING -> {
                    updateArmButton(armBtn)
                }
                AttributeEvent.STATE_VEHICLE_MODE -> {
                    updateArmButton(armBtn)
                }
                else -> {}
            }
        }
    }
}