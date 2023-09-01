package com.example.dronepilot

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Matrix
import android.os.SystemClock
import android.util.Log
import androidx.camera.core.ImageProxy
import com.example.dronepilot.Utils.GestureUtils
import com.google.mediapipe.framework.image.BitmapImageBuilder
import com.google.mediapipe.framework.image.MPImage
import com.google.mediapipe.tasks.core.BaseOptions
import com.google.mediapipe.tasks.vision.core.RunningMode
import com.google.mediapipe.tasks.vision.gesturerecognizer.GestureRecognizer
import com.google.mediapipe.tasks.vision.gesturerecognizer.GestureRecognizerResult

class GestureRecognizerClass(val context: Context, val gestureRecognizerListener: GestureUtils.GestureRecognizerListener? = null) {

    private var minHandDetectionConfidence: Float = DEFAULT_HAND_DETECTION_CONFIDENCE
    private var minHandTrackingConfidence: Float = DEFAULT_HAND_TRACKING_CONFIDENCE
    private var minHandPresenceConfidence: Float = DEFAULT_HAND_PRESENCE_CONFIDENCE
    private var gestureRecognizer: GestureRecognizer? = null

    init{
        //Iniciar el reconocimiento de gestos
        gestureRecognizer()
    }

    /**
     * Funcion para inicializar el objeto gestureRecognizer
     */
    fun gestureRecognizer() {
        try {
            //Se pasa el modelo que debe utilizar como opcion
            val baseOptionBuilder = BaseOptions.builder().setModelAssetPath(MP_RECOGNIZER_TASK)
            val baseOptions = baseOptionBuilder.build()

            val optionsBuilder =
                GestureRecognizer.GestureRecognizerOptions.builder()
                    .setBaseOptions(baseOptions)
                    //Ajustar los parametros de configuración
                    .setMinHandDetectionConfidence(minHandDetectionConfidence)
                    .setMinTrackingConfidence(minHandTrackingConfidence)
                    .setMinHandPresenceConfidence(minHandPresenceConfidence)
                    //Definir listener para los resultados y errores
                    .setResultListener(this::returnLiveStreamResult)
                    .setErrorListener(this::returnLivestreamError)
                    //Se selecciona el modo de transmision en vivo
                    .setRunningMode(RunningMode.LIVE_STREAM)

            val options = optionsBuilder.build()
            //Construir el reconocedor a partir de las opciones proporcionadas
            gestureRecognizer = GestureRecognizer.createFromOptions(context, options)
        } catch (e: IllegalStateException) {
            gestureRecognizerListener?.onError("Gesture recognizer failed to initialize")
            Log.e(TAG,"IllegalStateException: Failed to load the task with error: " + e.message)
        } catch (e: RuntimeException) {
            gestureRecognizerListener?.onError("Gesture recognizer failed to initialize")
            Log.e( TAG,"RuntimeException: Failed to load the task with error: " + e.message)
        }
    }

    /**
     * Funcion para manejar los resultados del reconocimiento
     * @param result Contiene información sobre el gesto reconocido
     * @param input Representa la imagen original sobre la que se ha realizado el reconocimiento de gestos
     */
    private fun returnLiveStreamResult(result: GestureRecognizerResult, input: MPImage) {
        // Calcula el tiempo que ha transcurrido desde que la imagen fue capturada hasta que se obtiene el resultado
        val finishTimeMs = SystemClock.uptimeMillis()
        val inferenceTime = finishTimeMs - result.timestampMs()

        //Envia la información al listener de los resultados obtenidos
        gestureRecognizerListener?.onResults(
            GestureUtils.ResultClass(
                listOf(result),
                inferenceTime,
                input.height,
                input.width
            )
        )
    }

    /**
     * Funcion para manejar los errores del reconocimiento
     */
    private fun returnLivestreamError(error: RuntimeException) {
        gestureRecognizerListener?.onError(error.message ?: "An error has occurred")
    }

    /**
     * Funcion para preparar y convertir datos de imagen para el reconocimiento
     * @param imageProxy, representa un frame de imagen capturado por la cámara
     */
    fun prepareData(imageProxy: ImageProxy){
        val frameTime = SystemClock.uptimeMillis()

        // Copiar los bits RGB de la imagen a un buffer bitmap
        val bitmapBuffer = Bitmap.createBitmap(imageProxy.width, imageProxy.height, Bitmap.Config.ARGB_8888)
        imageProxy.use {
            bitmapBuffer.copyPixelsFromBuffer(imageProxy.planes[0].buffer)
        }
        imageProxy.close()

        val matrix = Matrix().apply {
            // Rotar la imagen obtenida de la cámara para que se alinee con la orientación actual de visualización
            postRotate(imageProxy.imageInfo.rotationDegrees.toFloat())

            // Invertir la imagen horizontalmente para que coincida con la orientación de la cámara frontal
            postScale(-1f, 1f, imageProxy.width.toFloat(), imageProxy.height.toFloat())
        }

        // Rotar el bitmap para coincidir con lo que espera el modelo
        val rotatedBitmap = Bitmap.createBitmap(
            bitmapBuffer,0,0,
            bitmapBuffer.width,
            bitmapBuffer.height,
            matrix,
            true
        )

        // Convertir el Bitmap en un objeto de tipo MPImage, compatible con MediaPipe
        val mpImage = BitmapImageBuilder(rotatedBitmap).build()

        //Llamar al metodo de reconocimiento asincrono que devolvera los datos
        //reconocidos en la funcion returnLiveStreamResult
        gestureRecognizer?.recognizeAsync(mpImage, frameTime)
    }

    /**
     * Funcion para borrar los recursos del reconocimiento
     */
    fun clearGestureRecognizer() {
        gestureRecognizer?.close()
        gestureRecognizer = null
    }

    /**
     * Funcion para verificar si se ha cerrado la funcionalidad del reconocimiento de gestos
     */
    fun isClosed(): Boolean {
        return gestureRecognizer == null
    }

    companion object {
        val TAG = "GestureRecognizerHelper"
        //Se indica la path del modelo que se desea utilizar
        private const val MP_RECOGNIZER_TASK = "31.task"
        const val DEFAULT_HAND_DETECTION_CONFIDENCE = 0.5F
        const val DEFAULT_HAND_TRACKING_CONFIDENCE = 0.5F
        const val DEFAULT_HAND_PRESENCE_CONFIDENCE = 0.5F
        const val OTHER_ERROR = 0
    }
}