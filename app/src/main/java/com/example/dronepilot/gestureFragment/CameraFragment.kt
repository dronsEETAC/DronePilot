package com.example.dronepilot.gestureFragment

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.example.dronepilot.DroneClass
import com.example.dronepilot.GestureRecognizerClass
import com.example.dronepilot.MainActivity
import com.example.dronepilot.R
import com.example.dronepilot.databinding.FragmentCameraBinding
import com.google.mediapipe.tasks.components.containers.Category
import com.o3dr.android.client.interfaces.DroneListener
import com.o3dr.android.client.interfaces.TowerListener
import timber.log.Timber
import timber.log.Timber.DebugTree
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.*


class CameraFragment : Fragment(), GestureRecognizerClass.GestureRecognizerListener, DroneListener, TowerListener {

    private var _fragmentCameraBinding: FragmentCameraBinding? = null
    private val fragmentCameraBinding get() = _fragmentCameraBinding!!
    private lateinit var gestureRecognizerClass: GestureRecognizerClass
    private var preview: Preview? = null //Muestra una vista previa en tiempo real de lo que ve la camara
    private var imageAnalyzer: ImageAnalysis? = null //Analiza un flujo de imagenes en tiempo real
    private var camera: Camera? = null
    private var cameraProvider: ProcessCameraProvider? = null
    private lateinit var backgroundExecutor: ExecutorService
    private lateinit var resultText : TextView
    private val handler: Handler = Handler(Looper.getMainLooper())

    private lateinit var connectGesBtn : Button
    private lateinit var armGesBtn : Button

    private lateinit var droneClient : DroneClass

    private lateinit var headingBtn : Button
    //var movementJob: Job? = null

    companion object {
        private const val TAG = "Hand gesture recognizer"
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,savedInstanceState: Bundle?): View {
        _fragmentCameraBinding = FragmentCameraBinding.inflate(inflater, container, false)
        return fragmentCameraBinding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        resultText = view.findViewById(R.id.resultLabel)

        Timber.plant(DebugTree())

        droneClient = DroneClass.getDroneInstance(requireContext())

        connectGesBtn = view.findViewById(R.id.connect_ges_btn)
        connectGesBtn.setOnClickListener { connect() }

        armGesBtn = view.findViewById(R.id.arm_ges_btn)
        armGesBtn.setOnClickListener { arm() }

        backgroundExecutor = Executors.newSingleThreadExecutor()

        //Asegura que el metodo setUpCamera se ejecuta en el hilo principal
        fragmentCameraBinding.cameraPreview.post { startCamera() }

        backgroundExecutor.execute {
            gestureRecognizerClass = GestureRecognizerClass(
                context = requireContext(),
                gestureRecognizerListener = this
            )
        }
    }

    private fun startCamera(){
        //Inicia una operacion asincrona para obtener el ProcessCameraProvider al que se le añade un listener
        //se le asigna el resultado del ProcessCameraProvider a cameraProvider y se configuran los usos de camera
        val cameraProviderFuture = ProcessCameraProvider.getInstance(requireContext())
        cameraProviderFuture.addListener(
            {
                // CameraProvider -> administra el ciclo de vida de las camaras
                // Vincula los casos de uso de la camara al ciclo de vida de una actividad/fragmento
                cameraProvider = cameraProviderFuture.get()

                // Set the camera use cases ->
                setCameraUseCases()
            }, ContextCompat.getMainExecutor(requireContext())
        )
    }

    private fun setCameraUseCases() {
        val cameraProvider = cameraProvider?: throw IllegalStateException("Camera initialization failed.")

        val cameraSelector = CameraSelector.Builder().requireLensFacing(CameraSelector.LENS_FACING_FRONT).build() //Se selecciona la camara frontal

        //Ratio 4:3 porque es el que usan los modelos
        preview = Preview.Builder().setTargetAspectRatio(AspectRatio.RATIO_4_3)
            .setTargetRotation(fragmentCameraBinding.cameraPreview.display.rotation) //Segun como este orientado el dispositivo
            .build()

        // Analisis de la imagen usando RGBA 8888 que es el que usa el modelo. Unicamente se procesa la ultima imagen, descartando las demas
        imageAnalyzer =
            ImageAnalysis.Builder().setTargetAspectRatio(AspectRatio.RATIO_4_3)
                .setTargetRotation(fragmentCameraBinding.cameraPreview.display.rotation)
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .setOutputImageFormat(ImageAnalysis.OUTPUT_IMAGE_FORMAT_RGBA_8888)
                .build()
                // The analyzer can then be assigned to the instance
                .also {
                    it.setAnalyzer(backgroundExecutor) { image ->
                        imageGestureAnalysis(image)
                    }
                }

        // Must unbind the use-cases before rebinding them
        cameraProvider.unbindAll() //Liberar los casos y poder configurar nuevos usos de la camara

        try {
            // Metodo que crea una instancia de la camara y la vincula a un ciclo de vida especifico,
            // se la vincula a la instancia actual del fragmento
            camera = cameraProvider.bindToLifecycle(this, cameraSelector, preview, imageAnalyzer)

            // Mostrar en pantalla la vista previa de la camara
            preview?.setSurfaceProvider(fragmentCameraBinding.cameraPreview.surfaceProvider)
        } catch (exc: Exception) {
            Log.e(TAG, "Use case binding failed", exc)
        }
    }

    private fun imageGestureAnalysis(imageProxy: ImageProxy) {
        gestureRecognizerClass.prepareData(imageProxy = imageProxy)
    }

    override fun onResults(resultBundle: GestureRecognizerClass.ResultClass) {
        activity?.runOnUiThread {
            if (_fragmentCameraBinding != null) {
                val gestureCategories = resultBundle.results.first().gestures()
                if (gestureCategories.isNotEmpty()) {
                    resultAnalysis(gestureCategories.first())
                    Log.d("RESULT: ", gestureCategories.first().toString())
                } else {
                    //Log.d("RESULT: ", "gestureRecognizerResult empty")
                }
                fragmentCameraBinding.paintResults.setResults(
                    resultBundle.results.first(),
                    resultBundle.inputImageHeight,
                    resultBundle.inputImageWidth
                )

                fragmentCameraBinding.paintResults.invalidate() //Fuerza a dibujar
                }
        }
    }

    private fun resultAnalysis(result: MutableList<Category>){
        resultText.text = result[0].categoryName()
        when (result[0].categoryName()) {
            "Closed_Fist" -> {
            }
            "Open_Palm" -> {//STOP
                DroneClass.movementJob?.cancel()
                DroneClass.movementJob = null
                if (DroneClass.movementJob == null || DroneClass.movementJob?.isCancelled == true) {
                    DroneClass.movementJob = DroneClass.moveInDirection("stop", 0f)
                }
                resultText.text = "${result[0].categoryName()} -> STOP"
            }
            "Thumb_Up" -> {
                //DroneClass.returnToLaunch()
                resultText.text = result[0].categoryName() + " -> RTL"
            }
            "Thumb_Down" -> {
                DroneClass.movementJob?.cancel()
                DroneClass.movementJob = null
                if (DroneClass.movementJob == null || DroneClass.movementJob?.isCancelled == true) {
                    DroneClass.movementJob = DroneClass.moveInDirection("south", 5f)
                }
                resultText.text = result[0].categoryName() + " -> SOUTH"
            }
            "Pointing_Up" -> {
                DroneClass.movementJob?.cancel()
                DroneClass.movementJob = null
                if (DroneClass.movementJob == null || DroneClass.movementJob?.isCancelled == true) {
                    DroneClass.movementJob = DroneClass.moveInDirection("north", 5f)
                }
                resultText.text = result[0].categoryName() + " -> NORTH"
            }
            "Victory" ->{
                DroneClass.movementJob?.cancel()
                DroneClass.movementJob = null
                if (DroneClass.movementJob == null || DroneClass.movementJob?.isCancelled == true) {
                    DroneClass.movementJob = DroneClass.moveInDirection("west", 5f)
                }
                resultText.text = result[0].categoryName() + " -> WEST"
            }
            "ILoveYou" -> {
                DroneClass.movementJob?.cancel()
                DroneClass.movementJob = null
                if (DroneClass.movementJob == null || DroneClass.movementJob?.isCancelled == true) {
                    DroneClass.movementJob = DroneClass.moveInDirection("east", 5f)
                }
                resultText.text = result[0].categoryName() + " -> EAST"
            }
            else -> resultText.text = " "
        }
    }
/*
    fun moveInDirection(direction: String, velocity: Float) = GlobalScope.launch {
        while (isActive) {
            DroneClass.moveDrone(direction, velocity)
            delay(100)
        }
    }*/

    private fun stopDrone() {
        //movementJob?.cancel()
        //movementJob = null

        //DroneClass.moveDrone("north", 0f)
    }

    override fun onError(error: String, errorCode: Int) {
        activity?.runOnUiThread {
            Toast.makeText(requireContext(), error, Toast.LENGTH_SHORT).show()
        }
    }

    override fun onResume() {
        super.onResume()

        backgroundExecutor.execute {
            if (gestureRecognizerClass.isClosed()) {
                gestureRecognizerClass.gestureRecognizer()
            }
        }
    }

    override fun onPause() {
        super.onPause()

        if (this::gestureRecognizerClass.isInitialized) {
            backgroundExecutor.execute { gestureRecognizerClass.clearGestureRecognizer() }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _fragmentCameraBinding = null

        if (droneClient.drone.isConnected) {
            droneClient.drone.disconnect()
            DroneClass.updateConnectedButton(false,connectGesBtn)
        }

        backgroundExecutor.shutdown()
        backgroundExecutor.awaitTermination(Long.MAX_VALUE, TimeUnit.NANOSECONDS)
    }

    override fun onStart() {
        super.onStart()
        droneClient.controlTower.connect(this)
    }

    override fun onStop() {
        super.onStop()
        if (droneClient.drone.isConnected) {
            droneClient.drone.disconnect()
            DroneClass.updateConnectedButton(false,connectGesBtn)
        }
        droneClient.controlTower.unregisterDrone(droneClient.drone)
        droneClient.controlTower.disconnect()
    }

    override fun onDroneEvent(event: String?, extras: Bundle?) {
        DroneClass.droneEvent(event,extras,armGesBtn, connectGesBtn)
    }

    override fun onDroneServiceInterrupted(errorMsg: String?) {
        Log.d("DroneServiceInterrupted CameraFragment", "$errorMsg")
        Toast.makeText(requireContext(), "Drone service interrumpted: $errorMsg", Toast.LENGTH_LONG)

    }

    override fun onTowerConnected() {
        droneClient.controlTower.registerDrone(droneClient.drone, handler)
        droneClient.drone.registerDroneListener(this)
    }

    override fun onTowerDisconnected() {
        droneClient.controlTower.unregisterDrone(droneClient.drone)
        droneClient.drone.unregisterDroneListener(this)
    }

    private fun connect() {
        DroneClass.connect()
    }

    private fun arm(){
        DroneClass.arm()
    }

    fun onBackPressed(){
        val intent = Intent(requireContext(), MainActivity::class.java)
        startActivity(intent)
        if (droneClient.drone.isConnected) {
            droneClient.drone.disconnect()
            DroneClass.updateConnectedButton(false,connectGesBtn)
        }
        droneClient.controlTower.unregisterDrone(droneClient.drone)
        droneClient.controlTower.disconnect()
    }

}