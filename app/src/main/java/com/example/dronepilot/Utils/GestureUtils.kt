package com.example.dronepilot.Utils

import com.example.dronepilot.GestureRecognizerClass
import com.google.mediapipe.tasks.vision.gesturerecognizer.GestureRecognizerResult

class GestureUtils {
    //Clase que define los resultados del reconocimiento
    data class ResultClass(
        val results: List<GestureRecognizerResult>,
        val inferenceTime: Long,
        val inputImageHeight: Int,
        val inputImageWidth: Int,
    )

    //Error que manjea los resultados y errores del reconocimiento
    interface GestureRecognizerListener {
        fun onError(error: String, errorCode: Int = GestureRecognizerClass.OTHER_ERROR)
        fun onResults(resultBundle: ResultClass)
    }
}