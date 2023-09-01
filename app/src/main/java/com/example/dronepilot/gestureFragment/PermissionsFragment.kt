package com.example.dronepilot.gestureFragment

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.Navigation
import com.example.dronepilot.MainActivity
import com.example.dronepilot.R


class PermissionsFragment : Fragment() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        //Se verifica si la aplicación ya tiene permiso para acceder a la cámara
        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
            //Si ya tiene el permiso, navega directamente al fragmento de la cámara
            navigateToCameraFragment()
        } else {
            // Si no tiene el permiso, lo solicita al usuario
            requestPermissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    /**
     * Maneja la respuesta del usuario a la solicitud de permisos
     */
    private val requestPermissionLauncher = registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted: Boolean ->
        if (isGranted) {
            // Si el usuario concede el permiso, navega al fragmento de la cámara
            navigateToCameraFragment()
        } else {
            //Si el usuario niega el permiso, muestra una notificacion informativa
            Toast.makeText(context,"Permission request denied", Toast.LENGTH_LONG).show()
        }
    }

    /**
     * Maneja la navegacion hacia el fragmento de la camara
     */
    private fun navigateToCameraFragment() {
        // Navega a CameraFragment usando el Navigation Component
        lifecycleScope.launchWhenStarted { //Ejecuta una tarea asincrona en segundo plano, vinculada al ciclo de vida de una actividad
            Navigation.findNavController(requireActivity(), R.id.fragmentContainerView).navigate(R.id.action_permissionsFragment_to_cameraFragment)
        }
    }

    /**
     * Funcion que define el coportamiento del boton de retroceso
     */
    fun onBackPressed(){
        // Crea una nueva intención para iniciar la actividad principal
        val intent = Intent(requireContext(), MainActivity::class.java)
        // Inicia la actividad principal
        startActivity(intent)
    }
}