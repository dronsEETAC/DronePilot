package com.example.dronepilot

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Button

class MainActivity : AppCompatActivity() {

    private lateinit var gestureBtn: Button
    private lateinit var movementsBtn: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        gestureBtn = findViewById(R.id.gestureBtn)
        gestureBtn.setOnClickListener { openGestureActivity() }

        movementsBtn = findViewById(R.id.movementsBtn)
        movementsBtn.setOnClickListener { openMovementsActivity() }
    }

    private fun openMovementsActivity() {
        val intent = Intent(this, PhoneMovementsActivity::class.java)
        startActivity(intent)
        finish()
    }

    private fun openGestureActivity() {
       val intent = Intent(this, GestureRecognizerActivity::class.java)
       startActivity(intent)
        finish()
    }
}