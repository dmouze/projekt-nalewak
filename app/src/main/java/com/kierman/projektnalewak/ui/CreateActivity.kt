@file:Suppress("DEPRECATION")

package com.kierman.projektnalewak.ui

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.view.Window
import android.view.WindowManager
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.databinding.DataBindingUtil
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.kierman.projektnalewak.R
import com.kierman.projektnalewak.databinding.ActivityCreateBinding

class CreateActivity : AppCompatActivity() {
    private lateinit var binding: ActivityCreateBinding

    private lateinit var imie: EditText
    private lateinit var przycisk: Button

    private val databaseReference: DatabaseReference = FirebaseDatabase.getInstance().getReference("wyniki")

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

        imie = findViewById(R.id.imie_menela)
        przycisk = findViewById(R.id.btn_add_menel)

        przycisk.setOnClickListener{
            val sImie = imie.text.toString().trim()
            val sCzas = ArrayList<Double>()

            val userId = databaseReference.push().key

            if (userId != null) {
                val userMap = hashMapOf(
                    "id" to userId,
                    "imie" to sImie,
                    "czas" to sCzas
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

}
