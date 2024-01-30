package com.kierman.projektnalewak.ui

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.os.Bundle
import android.view.View
import android.view.Window
import android.view.WindowManager
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.databinding.DataBindingUtil
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.kierman.projektnalewak.R
import com.kierman.projektnalewak.databinding.ActivityCreateBinding

class CreateActivity : AppCompatActivity() {
    private lateinit var binding: ActivityCreateBinding
    private lateinit var imie: EditText
    private lateinit var przycisk: Button

    private val databaseReference: DatabaseReference = FirebaseDatabase.getInstance().getReference("wyniki")
    private lateinit var fusedLocationClient: FusedLocationProviderClient


    companion object {
        private const val LOCATION_PERMISSION_REQUEST_CODE = 123
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_FULLSCREEN
        actionBar?.hide()
        requestWindowFeature(Window.FEATURE_NO_TITLE)
        window.setFlags(
            WindowManager.LayoutParams.FLAG_FULLSCREEN,
            WindowManager.LayoutParams.FLAG_FULLSCREEN)

        binding = DataBindingUtil.setContentView(
            this,
            R.layout.activity_create
        )

        imie = findViewById(R.id.user_name)
        przycisk = findViewById(R.id.btn_add_user)

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        przycisk.setOnClickListener{


            // Sprawdź uprawnienia lokalizacji
            if (checkLocationPermission()) {
                // Jeśli uprawnienia są dostępne, pobierz lokalizację
                getLastLocation()
            } else {
                // Jeśli brak uprawnień, poproś użytkownika o nie
                requestLocationPermission()
            }
        }
    }

    private fun checkLocationPermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
    }

    private fun requestLocationPermission() {
        ActivityCompat.requestPermissions(
            this,
            arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
            LOCATION_PERMISSION_REQUEST_CODE
        )
    }

    private fun getLastLocation() {
        if (checkLocationPermission()) {
            try {
                fusedLocationClient.lastLocation
                    .addOnSuccessListener { location: Location? ->
                        if (location != null) {
                            // Lokalizacja została pomyślnie pobrana
                            saveUserWithLocation(imie.text.toString().trim(), ArrayList(), location)
                        } else {
                            // Nie udało się pobrać lokalizacji
                            Toast.makeText(this, "Nie można pobrać lokalizacji", Toast.LENGTH_SHORT).show()
                        }
                    }
                    .addOnFailureListener { e ->
                        // Wystąpił błąd podczas pobierania lokalizacji
                        Toast.makeText(this, "Błąd: ${e.message}", Toast.LENGTH_SHORT).show()
                    }
            } catch (e: SecurityException) {
                // Użytkownik może odrzucić prośbę o uprawnienia
                Toast.makeText(this, "Brak uprawnień do lokalizacji", Toast.LENGTH_SHORT).show()
            }
        } else {
            // Brak uprawnień do lokalizacji
            Toast.makeText(this, "Brak uprawnień do lokalizacji", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // Uprawnienia zostały przyznane, spróbuj ponownie pobrać lokalizację
                getLastLocation()
            } else {
                // Uprawnienia zostały odrzucone
                Toast.makeText(this, "Brak uprawnień do lokalizacji", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun saveUserWithLocation(sImie: String, sCzas: ArrayList<Double>, location: Location) {
        val userId = databaseReference.push().key

        if (userId != null) {
            val userMap = hashMapOf(
                "id" to userId,
                "imie" to sImie,
                "czas" to sCzas,
                "latitude" to location.latitude,
                "longitude" to location.longitude
            )

            databaseReference.child(userId).setValue(userMap)
                .addOnSuccessListener {
                    Toast.makeText(this, "Użytkownik dodany!", Toast.LENGTH_SHORT).show()
                    imie.text.clear()
                }
                .addOnFailureListener {
                    Toast.makeText(this, "Wystąpił problem...", Toast.LENGTH_SHORT).show()
                }
        }

        val intent = Intent(this, ChoosePlayerActivity::class.java)
        startActivity(intent)
        finish()
    }
}
