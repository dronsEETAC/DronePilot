package com.example.dronepilot

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.MenuItem
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.widget.Toolbar
import com.example.dronepilot.gestureFragment.CameraFragment
import com.example.dronepilot.gestureFragment.PermissionsFragment

class GestureRecognizerActivity : AppCompatActivity(), DroneParametersStatusListener{

    private lateinit var batteryPercentageTextView: TextView
    private lateinit var velocityTextView: TextView
    private lateinit var highTextView: TextView


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_gesture_recognizer)

        val toolbar: Toolbar = findViewById(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayShowTitleEnabled(false)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        val imageView: ImageView = findViewById(R.id.drone_toolbar)
        imageView.setOnClickListener {
            showMessageBox()
        }

        batteryPercentageTextView = findViewById(R.id.batteryPercentageTxt)
        velocityTextView = findViewById(R.id.velocityValueTxt)
        highTextView = findViewById(R.id.highValueTxt)
    }

    private fun showMessageBox() {
        val alertDialogBuilder = AlertDialog.Builder(this)
        alertDialogBuilder.setTitle("Mensaje")
        alertDialogBuilder.setMessage("Has hecho clic en la imagen del Toolbar.")
        alertDialogBuilder.setPositiveButton("Aceptar") { dialog, _ ->
            dialog.dismiss()
        }

        val alertDialog = alertDialogBuilder.create()
        alertDialog.show()
    }

    override fun onBackPressed() {
        val currentFragment = supportFragmentManager.findFragmentById(R.id.cameraFragment)

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

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == android.R.id.home) {
            onBackPressed()
            return true
        }
        return super.onOptionsItemSelected(item)
    }

    override fun onBatteryPercentageChanged(percentage: Int) {
        updateBatteryPercentage(percentage)
    }

    override fun onVelocityChanged(velocity: Double) {
        updateVelocity(velocity)
    }

    override fun onHighChanged(high: Double) {
        updateHigh(high)
    }

    private fun updateBatteryPercentage(percentage: Int) {
        batteryPercentageTextView.text = "$percentage%"
    }

    private fun updateVelocity(velocity: Double) {
        velocityTextView.text = "${formatDecimalValue(velocity)}m/s"
    }

    private fun updateHigh(high: Double) {
        highTextView.text = "${formatDecimalValue(high)}m"
    }

    private fun formatDecimalValue(value: Double): String {
        return String.format("%.2f", value)
    }
}
