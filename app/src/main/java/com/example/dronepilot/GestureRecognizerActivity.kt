package com.example.dronepilot

import android.content.Intent
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

        val toolbar: Toolbar = findViewById(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayShowTitleEnabled(false)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        val imageView: ImageView = findViewById(R.id.drone_toolbar)
        imageView.setOnClickListener {
            showImageDialog()
        }

        batteryPercentageTextView = findViewById(R.id.batteryPercentageTxt)
        velocityTextView = findViewById(R.id.velocityValueTxt)
        highTextView = findViewById(R.id.altitudeValueTxt)
    }

    private fun getCurrentFragment(): Fragment? {
        val navHostFragment = supportFragmentManager.findFragmentById(R.id.fragmentContainerView) as NavHostFragment?
        return navHostFragment?.childFragmentManager?.fragments?.get(0)
    }

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

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == android.R.id.home) {
            onBackPressed()
            return true
        }
        return super.onOptionsItemSelected(item)
    }

    override fun onBatteryPercentageChanged(percentage: Int) {
        batteryPercentageTextView.text = "$percentage%"
    }

    override fun onVelocityChanged(velocity: Double) {
        velocityTextView.text = "${formatDecimalValue(velocity)}m/s"
    }

    override fun onAltitudeChanged(altitude: Double) {
        highTextView.text = "${formatDecimalValue(altitude)}m"
    }

    private fun formatDecimalValue(value: Double): String {
        return String.format("%.2f", value)
    }
}
