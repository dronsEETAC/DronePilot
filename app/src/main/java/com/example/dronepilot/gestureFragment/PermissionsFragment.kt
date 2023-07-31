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
import com.example.dronepilot.DroneClass
import com.example.dronepilot.MainActivity
import com.example.dronepilot.R


class PermissionsFragment : Fragment() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        //Se comprueba si el permiso para acceder a la camara ya ha sido otorgado o no
        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
            // Los permisos de cámara ya han sido aceptados.
            navigateToCameraFragment()
        } else {
            // Los permisos de cámara no han sido aceptados. Solicitar permisos.
            requestPermissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    private val requestPermissionLauncher = registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted: Boolean ->
        if (isGranted) {
            navigateToCameraFragment()
        } else {
            Toast.makeText(context,"Permission request denied", Toast.LENGTH_LONG).show()
        }
    }

    private fun navigateToCameraFragment() {
        lifecycleScope.launchWhenStarted { //Ejecuta una tarea asincrona en segundo plano, vinculada al ciclo de vida de una actividad
            Navigation.findNavController(requireActivity(), R.id.fragmentContainerView).navigate(R.id.action_permissionsFragment_to_cameraFragment)
        }
    }

    fun onBackPressed(){
        val intent = Intent(requireContext(), MainActivity::class.java)
        startActivity(intent)
    }



}