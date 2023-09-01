package com.example.dronepilot

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Button
import androidx.appcompat.widget.Toolbar

class MainActivity : AppCompatActivity() {

    private lateinit var gestureBtn: Button
    private lateinit var movementsBtn: Button
    private lateinit var toolbar : Toolbar

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        //Vincular los botones con sus elementos de la vista y agregar un listener
        gestureBtn = findViewById(R.id.gestureBtn)
        gestureBtn.setOnClickListener { openGestureActivity() }

        movementsBtn = findViewById(R.id.movementsBtn)
        movementsBtn.setOnClickListener { openMovementsActivity() }

        //Inicializacion de la barra de herramientas
        toolbar = findViewById(R.id.mainToolBar)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayShowTitleEnabled(false) //Oculta el titulo de la aplicacion en la barra de herramientas
    }

    /**
     * Funcion para abrir la activity PhoneMovementsActivity
     */
    private fun openMovementsActivity() {
        val intent = Intent(this, PhoneMovementsActivity::class.java)
        startActivity(intent)
        finish()
    }

    /**
     * Funcion para abrir la activity GestureRecognizerActivity
     */
    private fun openGestureActivity() {
       val intent = Intent(this, GestureRecognizerActivity::class.java)
       startActivity(intent)
        finish()
    }
}