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

class PaintResultsView(context: Context?, attributeSet: AttributeSet?) : View(context, attributeSet) { //OverlayView extiende la clase View y este es el constructor de esa clase
    private var results: GestureRecognizerResult? = null
    private var linePaint = Paint()
    private var pointPaint = Paint()

    private var scaleFactor: Float = 1f
    private var imageWidth: Int = 1
    private var imageHeight: Int = 1

    init {
        initPaints()
    }

    fun clear() {
        results = null
        linePaint.reset()
        pointPaint.reset()
        invalidate()
        initPaints()
    }

    private fun initPaints() {
        linePaint.color = Color.BLUE
        linePaint.strokeWidth = LANDMARK_STROKE_WIDTH //Ancho de la linea dibujada
        linePaint.style = Paint.Style.STROKE //Solo se dibuja el contorno de las lineas

        pointPaint.color = Color.YELLOW
        pointPaint.strokeWidth = LANDMARK_STROKE_WIDTH
        pointPaint.style = Paint.Style.FILL //Se rellenan los puntos
    }

    override fun draw(canvas: Canvas) {
        super.draw(canvas)
        results?.let { gestureRecognizerResult ->
            val lines = mutableListOf<Float>()
            val points = mutableListOf<Float>()

            for (landmarks in gestureRecognizerResult.landmarks()) {
                for (i in landmarkConnections.indices step 2) { //landmarkConnections.indices significa que devuelve un rango de indices que comienzan en cero y aumentan en incremento de dos hasta la longitud de la lista landmarkConnections
                    val startX = landmarks[landmarkConnections[i]].x() * imageWidth * scaleFactor //Actualiza la posicion x
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
                canvas.drawLines(lines.toFloatArray(), linePaint)
                canvas.drawPoints(points.toFloatArray(), pointPaint)
            }
        }
    }

    fun setResults( gestureRecognizerResult: GestureRecognizerResult, imageHeight: Int, imageWidth: Int) {
        results = gestureRecognizerResult

        this.imageHeight = imageHeight
        this.imageWidth = imageWidth

        scaleFactor = max(width * 1f / imageWidth, height * 1f / imageHeight) //escalar las coordenadas entre el view que se dibuja y el canvas de la imagen original

        invalidate() //Actualizar la vista actual
    }

    companion object {
        private const val LANDMARK_STROKE_WIDTH = 8F //8F = 8 pixeles

        // This list defines the lines that are drawn when visualizing the hand landmark detection
        // results. These lines connect:
        // landmarkConnections[2*n] and landmarkConnections[2*n+1]
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
