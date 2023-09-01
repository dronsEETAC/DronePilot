package com.example.dronepilot

import android.content.Intent
import android.graphics.Color
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.MenuItem
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.widget.Toolbar
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.NavHostFragment
import com.example.dronepilot.gestureFragment.CameraFragment
import com.example.dronepilot.gestureFragment.PermissionsFragment

class GestureRecognizerActivity : AppCompatActivity(), DroneParametersStatusListener{

    private lateinit var batteryPercentageTextView: TextView
    private lateinit var velocityTextView: TextView
    private lateinit var highTextView: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_gesture_recognizer)

        //Inicializacion de la toolbar
        val toolbar: Toolbar = findViewById(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayShowTitleEnabled(false)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        //Configura el click en el icono de la toolbar
        val imageView: ImageView = findViewById(R.id.drone_toolbar)
        imageView.setOnClickListener {
            showImageDialog()
        }

        //Inicializacion de los textView
        batteryPercentageTextView = findViewById(R.id.batteryPercentageTxt)
        velocityTextView = findViewById(R.id.velocityValueTxt)
        highTextView = findViewById(R.id.altitudeValueTxt)
    }

    /**
     * Funcion que devuelve el fragmento que se esta mostrando en la activity
     */
    private fun getCurrentFragment(): Fragment? {
        val navHostFragment = supportFragmentManager.findFragmentById(R.id.fragmentContainerView) as NavHostFragment?
        return navHostFragment?.childFragmentManager?.fragments?.get(0)
    }

    /**
     * Funcion para mostrar un dialogo con la informacion de los gestos
     */
    private fun showImageDialog() {
        val imageDialog = AlertDialog.Builder(this)
        val inflater = this.layoutInflater
        val view = inflater.inflate(R.layout.image_commands_gesture, null)
        val dialogImage: ImageView = view.findViewById(R.id.dialogImageView)

        dialogImage.setImageResource(R.drawable.gesture_commands)
        val currentFragment = getCurrentFragment()
        if (currentFragment is CameraFragment) {
            currentFragment.stop()
        }
        imageDialog.setView(view)
        imageDialog.setPositiveButton("Cerrar") { dialog, _ ->
            dialog.dismiss()
        }

        val dialog = imageDialog.create()
        dialog.show()
    }

    /**
     * Funcion para manejar la pulsacion del boton de atras
     */
    override fun onBackPressed() {
        val currentFragment = getCurrentFragment()

        if (currentFragment is CameraFragment) {
            currentFragment.onBackPressed()
            return
        }else if (currentFragment is PermissionsFragment){
            currentFragment.onBackPressed()
        }else{
            val intent = Intent(this, MainActivity::class.java)
            startActivity(intent)
        }
        finish()
    }

    /**
     * Funcion que se ejecuta al pulsar el item atras de la barra de herramientas
     */
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == android.R.id.home) {
            onBackPressed()
            return true
        }
        return super.onOptionsItemSelected(item)
    }

    /**
     * Funcion que actualiza el valor del textView del porcentaje de batería
     */
    override fun onBatteryPercentageChanged(percentage: Int, isConnected: Boolean) {
        batteryPercentageTextView.text = "$percentage%"

        // Si el porcentaje es menor al 70, cambia el color de la letra a rojo
        if (isConnected && percentage < 70) {
            batteryPercentageTextView.setTextColor(Color.RED)
        } else {
            //Si no, la letra se visualiza en negro
            batteryPercentageTextView.setTextColor(Color.BLACK)
        }
    }

    /**
     * Funcion que actualiza el valor del textView de la velocidad
     */
    override fun onVelocityChanged(velocity: Double) {
        velocityTextView.text = "${formatDecimalValue(velocity)}m/s"
    }

    /**
     * Funcion que actualiza el valor del textView de la altitud
     */
    override fun onAltitudeChanged(altitude: Double) {
        highTextView.text = "${formatDecimalValue(altitude)}m"
    }

    /**
     * Funcion para formatear un valor decimal a 2 decimales
     */
    private fun formatDecimalValue(value: Double): String {
        return String.format("%.2f", value)
    }
}
