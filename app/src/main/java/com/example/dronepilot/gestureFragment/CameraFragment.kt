package com.example.dronepilot.gestureFragment

import android.content.Context
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
import com.example.dronepilot.*
import com.example.dronepilot.DroneClass.Companion.moveInDirection
import com.example.dronepilot.R
import com.example.dronepilot.Utils.GestureUtils
import com.example.dronepilot.databinding.FragmentCameraBinding
import com.google.mediapipe.tasks.components.containers.Category
import com.o3dr.android.client.interfaces.DroneListener
import com.o3dr.android.client.interfaces.TowerListener
import com.o3dr.services.android.lib.drone.attribute.AttributeType
import com.o3dr.services.android.lib.drone.property.Altitude
import com.o3dr.services.android.lib.drone.property.Battery
import com.o3dr.services.android.lib.drone.property.Speed
import com.o3dr.services.android.lib.drone.property.State
import timber.log.Timber
import timber.log.Timber.DebugTree
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit


class CameraFragment : Fragment(), GestureUtils.GestureRecognizerListener, DroneListener, TowerListener {

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
    private val parametersUpdateHandler = Handler()
    private var droneParametersStatusListener: DroneParametersStatusListener? = null

    companion object {
        private const val TAG = "Hand gesture recognizer"
    }

    /**
     * Inicializacion del fragmento
     */
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,savedInstanceState: Bundle?): View {
        _fragmentCameraBinding = FragmentCameraBinding.inflate(inflater, container, false)
        return fragmentCameraBinding.root
    }

    /**
     * Configuración una vez que la vista ha sido creada
     */
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        resultText = view.findViewById(R.id.resultLabel)

        Timber.plant(DebugTree())

        //Inicializa la instancia del dron
        droneClient = DroneClass.getDroneInstance(requireContext())

        //Inicializa los botones
        connectGesBtn = view.findViewById(R.id.connect_ges_btn)
        connectGesBtn.setOnClickListener { connect() }
        armGesBtn = view.findViewById(R.id.arm_ges_btn)
        armGesBtn.setOnClickListener { arm() }

        //Inicialilza las tareas en segundo plano
        backgroundExecutor = Executors.newSingleThreadExecutor()

        //Asegura que el metodo setUpCamera se ejecuta en el hilo principal
        fragmentCameraBinding.cameraPreview.post { startCamera() }

        //Inicializa el runnable que maneja los cambios de parametros
        parametersUpdateHandler.postDelayed(parametersUpdateRunnable, 0)

        //Inicializa el gestureRecognizer en segundo plano
        backgroundExecutor.execute {
            gestureRecognizerClass = GestureRecognizerClass(
                context = requireContext(),
                gestureRecognizerListener = this
            )
        }
    }

    /**
     * Se invoca cuando un fragmento se asocia por primera vez con un contexto
     * @param context Contexto al que se asocia el fragmento
     */
    override fun onAttach(context: Context) {
        super.onAttach(context)
        try {
            //Intenta convertir el contexto en la interfaz DroneParametersStatusListener
            droneParametersStatusListener = context as DroneParametersStatusListener
        } catch (e: ClassCastException) {
            throw ClassCastException("$context must implement BatteryStatusListener")
        }
    }

    /**
     * Funcion para inicializar la camara
     */
    private fun startCamera(){
        //Inicia una operacion asincrona para obtener el ProcessCameraProvider al que se le añade un listener
        //se le asigna el resultado del ProcessCameraProvider a cameraProvider y se configuran los usos de camera
        val cameraProviderFuture = ProcessCameraProvider.getInstance(requireContext())
        cameraProviderFuture.addListener(
            {
                // CameraProvider -> administra el ciclo de vida de las camaras
                // Vincula los casos de uso de la camara al ciclo de vida de una actividad/fragmento
                cameraProvider = cameraProviderFuture.get()

                // Set the camera use cases
                setCameraUseCases()
            }, ContextCompat.getMainExecutor(requireContext())
        )
    }

    /**
     * Configura y vincula los casos de uso de la cámara utilizando CameraX
     * Esta función establece una vista previa de la cámara y un analizador de imágenes para procesar
     * las imágenes en tiempo real. La vista previa se muestra en la UI y las imágenes son analizadas
     * en el formato RGBA 8888. Además, se asegura de vincular la cámara al ciclo de vida del fragmento actual
     */
    private fun setCameraUseCases() {
        // Obtiene el cameraProvider. Si no está inicializado, lanza una excepción.
        val cameraProvider = cameraProvider?: throw IllegalStateException("Camera initialization failed.")

        //Selecciona la camara frontal mediante un cameraSelector
        val cameraSelector = CameraSelector.Builder().requireLensFacing(CameraSelector.LENS_FACING_FRONT).build()

        // Configura un objeto Preview para mostrar una vista previa de la cámara en una relación de aspecto 4:3
        // Además, ajusta la rotación de la vista previa según la orientación actual del dispositivo
        preview = Preview.Builder().setTargetAspectRatio(AspectRatio.RATIO_4_3)
            .setTargetRotation(fragmentCameraBinding.cameraPreview.display.rotation)
            .build()

        // Analisis de imagenes de la camara. Procesa las imagenes en formato RGBA 8888
        // Unicamente se procesa la ultima imagen, descartando las anteriores
        imageAnalyzer =
            ImageAnalysis.Builder().setTargetAspectRatio(AspectRatio.RATIO_4_3)
                .setTargetRotation(fragmentCameraBinding.cameraPreview.display.rotation)
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .setOutputImageFormat(ImageAnalysis.OUTPUT_IMAGE_FORMAT_RGBA_8888)
                .build()
                .also {
                    it.setAnalyzer(backgroundExecutor) { image ->
                        //Analiza la imagen llamando al metodo prepareData de la clase GestureRecognizerClass
                        gestureRecognizerClass.prepareData(image)
                    }
                }

        //Liberar los use-cases y poder configurar nuevos usos de la camara
        cameraProvider.unbindAll()

        try {
            // Metodo que crea una instancia de la camara y la vincula al ciclo de vida del fragmento actual
            // También vincula la cámara a los use-cases configurados: vista previa y análisis de imagen
            camera = cameraProvider.bindToLifecycle(this, cameraSelector, preview, imageAnalyzer)

            // Mostrar en pantalla la vista previa de la camara
            preview?.setSurfaceProvider(fragmentCameraBinding.cameraPreview.surfaceProvider)
        } catch (exc: Exception) {
            Log.e(TAG, "Use case binding failed", exc)
        }
    }

    /**
     * Maneja los resultados obtenidos del reconocimiento de gestos.
     * @param resultBundle Una clase que contiene resultados del reconocimiento de gestos
     */
    override fun onResults(resultBundle: GestureUtils.ResultClass) {
        //Se asegura que se ejecute en el hilo principal
        activity?.runOnUiThread {
            //Verifica que el binding (enlace con la vista) no sea nulo
            if (_fragmentCameraBinding != null) {
                //Obtiene la categorias del primer gesto detectado
                val gestureCategories = resultBundle.results.first().gestures()
                //Si hay una mano detectada
                if (gestureCategories.isNotEmpty()) {
                    //Analiza el primer gesto detectado
                    resultAnalysis(gestureCategories.first())
                    Log.d("RESULT: ", gestureCategories.first().toString())
                } else {
                    Log.d("RESULT: ", "gestureRecognizerResult empty")
                }
                //Pasa los resultados a la clase PaintResultsView para pintar el esqueleto de la mano
                fragmentCameraBinding.paintResults.setResults(
                    resultBundle.results.first(),
                    resultBundle.inputImageHeight,
                    resultBundle.inputImageWidth
                )
                //Fuerza un nuevo dibujo
                fragmentCameraBinding.paintResults.invalidate()
                }
        }
    }

    /**
     * Analiza los resultados obtenidos tras reconocer un gesto y ejecuta acciones en el dron
     * basadas en los gestos reconocidos.
     * @param result Lista de categorías que representan gestos reconocidos.
     */
    private fun resultAnalysis(result: MutableList<Category>) {
        // Muestra el nombre del último gesto en resultText
        resultText.text = result[0].categoryName()

        // Obtiene el estado actual del dron
        val vehicleState = droneClient.drone.getAttribute<State>(AttributeType.STATE)

        //Verifica si el dron esta volando
        if (vehicleState.isFlying) {
            // Analiza el nombre del gesto y ejecuta acciones en el dron
            when (result[0].categoryName()) {
                "chill" -> { // Mueve el dron hacia el Sudeste (SE)
                    moveInDirection("southEast")
                    resultText.text = "${result[0].categoryName()} -> SouthEast"
                }
                "open_palm" -> { // Detiene el dron
                    moveInDirection("stop")
                    resultText.text = "${result[0].categoryName()} -> Stop"
                }
                "thumb_up" -> { // Norte (N)
                    moveInDirection("north")
                    resultText.text = result[0].categoryName() + " -> North"
                }
                "thumb_down" -> { // Sur (S)
                    moveInDirection("south")
                    resultText.text = result[0].categoryName() + " -> South"
                }
                "left_L" -> { // Oeste (W)
                    moveInDirection("west")
                    resultText.text = result[0].categoryName() + " -> West"
                }
                "victory" -> { // Noroeste (NO)
                    moveInDirection("northWest")
                    resultText.text = result[0].categoryName() + " -> NorthWest"
                }
                "rockstar" -> { // Noreste (NE)
                    moveInDirection("northEast")
                    resultText.text = result[0].categoryName() + " -> NorthEast"
                }
                "three" -> { // Este (E)
                    moveInDirection("east")
                    resultText.text = result[0].categoryName() + " -> East"
                }
                "perfect" -> { // Suroeste
                    moveInDirection("southWest")
                    resultText.text = result[0].categoryName() + " -> SouthWest"
                }
                else -> resultText.text = result[0].categoryName()
            }
        }
    }

    /**
     * Funcion para obtener el porcentaje de bateria del dron
     */
    private fun batteryPercentage() : Int {
        return if (droneClient.drone.isConnected) {
            val vehicleBattery = droneClient.drone.getAttribute<Battery>(AttributeType.BATTERY)
            (vehicleBattery.batteryRemain).toInt()
        } else{
            //Si el dron no esta conectado devuelve 0
            0
        }
    }

    /**
     * Funcion para obtener la velocidad del dron
     */
    private fun velocity() : Double {
        return if (droneClient.drone.isConnected) {
            val vehicleVelocity = droneClient.drone.getAttribute<Speed>(AttributeType.SPEED)
            vehicleVelocity.groundSpeed
        } else{
            0.0
        }
    }

    /**
     * Funcion para obtener la altitud del dron
     */
    private fun altitude() : Double {
        return if (droneClient.drone.isConnected) {
            val vehicleAltitude = droneClient.drone.getAttribute<Altitude>(AttributeType.ALTITUDE)
            vehicleAltitude.altitude / 100
        } else{
            0.0
        }
    }

    /**
     * Runnable para actualizar los parámetros del dron cada 1s
     */
    private val parametersUpdateRunnable = object : Runnable {
        override fun run() {
            val batteryPercentage = batteryPercentage()
            //Obtiene y notifica el porcentaje de bateria
            droneParametersStatusListener?.onBatteryPercentageChanged(batteryPercentage, droneClient.drone.isConnected)
            //Obtiene y notifica la velocidad
            val velocity = velocity()
            droneParametersStatusListener?.onVelocityChanged(velocity)
            //Obtiene y notifica la altitud
            val altitude = altitude()
            droneParametersStatusListener?.onAltitudeChanged(altitude)
            //Programa la ejecución del runnable en 1s.
            parametersUpdateHandler.postDelayed(this, 1000)
        }
    }

    /**
     * Función que se ejecuta cuando ocurre un error
     */
    override fun onError(error: String, errorCode: Int) {
        activity?.runOnUiThread {
            Toast.makeText(requireContext(), error, Toast.LENGTH_SHORT).show()
        }
    }

    /**
     * Función que se ejecuta al reanudar el fragment
     */
    override fun onResume() {
        super.onResume()
        //Comienza a ejecutar el runnable de actualización de los parametros del dron
        parametersUpdateHandler.postDelayed(parametersUpdateRunnable, 0)
        backgroundExecutor.execute {
            if (gestureRecognizerClass.isClosed()) {
                gestureRecognizerClass.gestureRecognizer()
            }
        }
    }

    /**
     * Función que se ejecuta al pausar el fragment
     */
    override fun onPause() {
        super.onPause()
        // Elimina las llamadas del Runnable de actualización de parámetros
        parametersUpdateHandler.removeCallbacks(parametersUpdateRunnable)

        if (this::gestureRecognizerClass.isInitialized) {
            backgroundExecutor.execute { gestureRecognizerClass.clearGestureRecognizer() }
        }
        DroneClass.stopMoving()
    }

    /**
     * Función que se ejecuta al destruir la vista
     */
    override fun onDestroyView() {
        super.onDestroyView()
        DroneClass.onDestroy()
        _fragmentCameraBinding = null

        if (droneClient.drone.isConnected) {
            droneClient.drone.disconnect()
            DroneClass.updateConnectedButton(false,connectGesBtn)
        }

        //Cierra el ejecutor en segundo plano
        backgroundExecutor.shutdown()
        backgroundExecutor.awaitTermination(Long.MAX_VALUE, TimeUnit.NANOSECONDS)
    }

    /**
     * Función que se ejecuta al iniciar el fragment
     */
    override fun onStart() {
        super.onStart()
        droneClient.controlTower.connect(this)
    }

    /**
     * Función que se ejecuta al detener el fragment
     */
    override fun onStop() {
        super.onStop()
        if (droneClient.drone.isConnected) {
            droneClient.drone.disconnect()
            DroneClass.updateConnectedButton(false,connectGesBtn)
        }
        DroneClass.stopMoving()
        droneClient.controlTower.unregisterDrone(droneClient.drone)
        droneClient.controlTower.disconnect()
    }

    /**
     * Función para manejar los eventos del dron
     */
    override fun onDroneEvent(event: String?, extras: Bundle?) {
        DroneClass.droneEvent(event,armGesBtn, connectGesBtn)
    }

    /**
     * Función para manejar interrupciones del servicio del dron
     */
    override fun onDroneServiceInterrupted(errorMsg: String?) {
        Log.d("DroneServiceInterrupted CameraFragment", "$errorMsg")
    }

    /**
     * Función que se ejecuta cuando se conecta la aplicación a la torre
     */
    override fun onTowerConnected() {
        droneClient.controlTower.registerDrone(droneClient.drone, handler)
        droneClient.drone.registerDroneListener(this)
    }

    /**
     * Función que se ejecuta cuando se desconecta la aplicación a la torre
     */
    override fun onTowerDisconnected() {
        droneClient.controlTower.unregisterDrone(droneClient.drone)
        droneClient.drone.unregisterDroneListener(this)
    }

    /**
     * Función para conectar el dron
     */
    private fun connect() {
        DroneClass.connect(false)
    }

    /**
     * Función para detener el dron
     */
     fun stop() {
         DroneClass.stopMoving()
     }

    /**
     * Función para cambiar el estado del dron
     */
    private fun arm(){
        DroneClass.arm()
    }

    /**
     * Función que se ejecuta al pulsar el botón atras.
     */
     fun onBackPressed(){
        val intent = Intent(requireContext(), MainActivity::class.java)
        startActivity(intent)
        if (droneClient.drone.isConnected) {
            droneClient.drone.disconnect()
            DroneClass.updateConnectedButton(false,connectGesBtn)
        }
        droneClient.controlTower.unregisterDrone(droneClient.drone)
        droneClient.controlTower.disconnect()
        DroneClass.stopMoving()
    }

}