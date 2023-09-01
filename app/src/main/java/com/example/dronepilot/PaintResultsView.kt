package com.example.dronepilot

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.util.AttributeSet
import android.view.View
import com.google.mediapipe.tasks.vision.gesturerecognizer.GestureRecognizerResult
import com.google.mediapipe.tasks.vision.handlandmarker.HandLandmark
import kotlin.math.max

class PaintResultsView(context: Context?, attributeSet: AttributeSet?) : View(context, attributeSet) {

    // Resultados obtenidos del reconocedor de gestos
    private var results: GestureRecognizerResult? = null

    // Pinceles para dibujar las líneas y puntos
    private var linePaint = Paint()
    private var pointPaint = Paint()

    // Factores para el escalado y dimensiones de la imagen
    private var scaleFactor: Float = 1f
    private var imageWidth: Int = 1
    private var imageHeight: Int = 1

    init {
        initPaints()
    }

    /**
     * Funcion que realiza la configuracion inicial de las lineas y puntos
     */
    private fun initPaints() {
        // Configuración del pincel para dibujar líneas
        linePaint.color = Color.BLUE
        linePaint.strokeWidth = LANDMARK_STROKE_WIDTH //Ancho de la linea dibujada
        linePaint.style = Paint.Style.STROKE //Solo se dibuja el contorno de las lineas

        // Configuración del pincel para dibujar puntos
        pointPaint.color = Color.YELLOW
        pointPaint.strokeWidth = LANDMARK_STROKE_WIDTH
        pointPaint.style = Paint.Style.FILL //Se rellenan los puntos
    }

    /**
     * Funcion que dibuja las lineas y puntos en el canvas
     */
    override fun draw(canvas: Canvas) {
        super.draw(canvas)
        results?.let { gestureRecognizerResult ->
            val lines = mutableListOf<Float>()
            val points = mutableListOf<Float>()

            for (landmarks in gestureRecognizerResult.landmarks()) {
                for (i in landmarkConnections.indices step 2) {
                    val startX = landmarks[landmarkConnections[i]].x() * imageWidth * scaleFactor
                    val startY = landmarks[landmarkConnections[i]].y() * imageHeight * scaleFactor
                    val endX = landmarks[landmarkConnections[i + 1]].x() * imageWidth * scaleFactor
                    val endY = landmarks[landmarkConnections[i + 1]].y() * imageHeight * scaleFactor
                    lines.add(startX)
                    lines.add(startY)
                    lines.add(endX)
                    lines.add(endY)
                    points.add(startX)
                    points.add(startY)
                }
                // Dibuja las líneas y puntos en el canvas
                canvas.drawLines(lines.toFloatArray(), linePaint)
                canvas.drawPoints(points.toFloatArray(), pointPaint)
            }
        }
    }

    /**
     * Establece los resultados para visualización y recalcula las dimensiones y el factor de escala
     */
    fun setResults( gestureRecognizerResult: GestureRecognizerResult, imageHeight: Int, imageWidth: Int) {
        results = gestureRecognizerResult

        this.imageHeight = imageHeight
        this.imageWidth = imageWidth

        //Escalar las coordenadas entre el view que se dibuja y el canvas de la imagen original
        scaleFactor = max(width * 1f / imageWidth, height * 1f / imageHeight)

        invalidate() //Actualizar la vista para que se redibuje
    }

    companion object {
        private const val LANDMARK_STROKE_WIDTH = 8F //8F = 8 pixeles

        //Lista de conexiones entre landmarks para visualizar las marcas de la mano.
        private val landmarkConnections = listOf(
            HandLandmark.WRIST,
            HandLandmark.THUMB_CMC,
            HandLandmark.THUMB_CMC,
            HandLandmark.THUMB_MCP,
            HandLandmark.THUMB_MCP,
            HandLandmark.THUMB_IP,
            HandLandmark.THUMB_IP,
            HandLandmark.THUMB_TIP,
            HandLandmark.WRIST,
            HandLandmark.INDEX_FINGER_MCP,
            HandLandmark.INDEX_FINGER_MCP,
            HandLandmark.INDEX_FINGER_PIP,
            HandLandmark.INDEX_FINGER_PIP,
            HandLandmark.INDEX_FINGER_DIP,
            HandLandmark.INDEX_FINGER_DIP,
            HandLandmark.INDEX_FINGER_TIP,
            HandLandmark.INDEX_FINGER_MCP,
            HandLandmark.MIDDLE_FINGER_MCP,
            HandLandmark.MIDDLE_FINGER_MCP,
            HandLandmark.MIDDLE_FINGER_PIP,
            HandLandmark.MIDDLE_FINGER_PIP,
            HandLandmark.MIDDLE_FINGER_DIP,
            HandLandmark.MIDDLE_FINGER_DIP,
            HandLandmark.MIDDLE_FINGER_TIP,
            HandLandmark.MIDDLE_FINGER_MCP,
            HandLandmark.RING_FINGER_MCP,
            HandLandmark.RING_FINGER_MCP,
            HandLandmark.RING_FINGER_PIP,
            HandLandmark.RING_FINGER_PIP,
            HandLandmark.RING_FINGER_DIP,
            HandLandmark.RING_FINGER_DIP,
            HandLandmark.RING_FINGER_TIP,
            HandLandmark.RING_FINGER_MCP,
            HandLandmark.PINKY_MCP,
            HandLandmark.WRIST,
            HandLandmark.PINKY_MCP,
            HandLandmark.PINKY_MCP,
            HandLandmark.PINKY_PIP,
            HandLandmark.PINKY_PIP,
            HandLandmark.PINKY_DIP,
            HandLandmark.PINKY_DIP,
            HandLandmark.PINKY_TIP
        )
    }
}
