@file:Suppress("DEPRECATION")

package com.kierman.projektnalewak.ui

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.view.Window
import android.view.WindowManager
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.firebase.database.*
import com.kierman.projektnalewak.util.UserModel
import com.kierman.projektnalewak.databinding.ActivityChoosePlayerBinding
import com.kierman.projektnalewak.util.UserListAdapter
import com.kierman.projektnalewak.R

class ChoosePlayerActivity : AppCompatActivity(), UserListAdapter.ItemClickListener, UserListAdapter.ItemLongClickListener {

    private lateinit var binding: ActivityChoosePlayerBinding
    private lateinit var adapter: UserListAdapter
    private lateinit var databaseReference: DatabaseReference

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityChoosePlayerBinding.inflate(layoutInflater)
        requestWindowFeature(Window.FEATURE_NO_TITLE)
        window.setFlags(
            WindowManager.LayoutParams.FLAG_FULLSCREEN,
            WindowManager.LayoutParams.FLAG_FULLSCREEN
        )
        window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_FULLSCREEN
        actionBar?.hide()
        setContentView(binding.root)

        val firebaseDatabase = FirebaseDatabase.getInstance()
        databaseReference = firebaseDatabase.reference.child("menele") // Zmień na odpowiednią ścieżkę w swojej bazie danych

        val newUser = findViewById<TextView>(R.id.add_new)

        newUser.setOnClickListener {
            val intent = Intent(this, CreateActivity::class.java)
            startActivity(intent)
            finish()
        }

        val userList = mutableListOf<UserModel>()
        adapter = UserListAdapter(userList, this,this)

        binding.userRecyclerView.layoutManager = LinearLayoutManager(this)
        binding.userRecyclerView.adapter = adapter

        databaseReference.addValueEventListener(object : ValueEventListener {
            @SuppressLint("NotifyDataSetChanged")
            override fun onDataChange(snapshot: DataSnapshot) {
                userList.clear()
                for (userSnapshot in snapshot.children) {
                    val id = userSnapshot.key // Pobierz ID użytkownika
                    val name = userSnapshot.child("imie").getValue(String::class.java) // Pobierz imię użytkownika
                    val czasMap = userSnapshot.child("czas").getValue(object : GenericTypeIndicator<HashMap<String, Double>>() {})
                    val timeList = ArrayList(czasMap?.values ?: emptyList())
                    val user = UserModel(id, name, timeList) // Tworzenie UserModel z ID, imieniem i czasami
                    userList.add(user)
                }
                adapter.notifyDataSetChanged()
            }

            override fun onCancelled(error: DatabaseError) {
            }
        })
    }

    override fun onItemClick(user: UserModel) {
        val id = user.id
        val results = user.time
        val imie = user.name
        val resultArray: ArrayList<Double> = ArrayList(results)
        val intent = Intent(this, TimerActivity::class.java)
        intent.putExtra("user_name", imie)
        intent.putExtra("user_id", id)
        intent.putExtra("user_results", resultArray)
        startActivity(intent)
        finish()
    }

    override fun onItemLongClick(user: UserModel) {
        val id = user.id
        val imie = user.name
        showDeleteUserDialog(imie, id)
    }

    private fun showDeleteUserDialog(imie: String?, id: String?) {
        val alertDialogBuilder = AlertDialog.Builder(this)
        alertDialogBuilder.setTitle("Potwierdź usunięcie użytkownika")
        alertDialogBuilder.setMessage("Czy na pewno chcesz usunąć użytkownika: $imie razem z wszystkimi wynikami?")
        alertDialogBuilder.setPositiveButton("Tak") { _, _ ->
            // Usuń użytkownika z Firebase
            if (id != null) {
                removeUserFromFirebase(id)
            }
        }
        alertDialogBuilder.setNegativeButton("Anuluj") { _, _ ->
        }

        val alertDialog = alertDialogBuilder.create()
        alertDialog.show()
    }

    private fun removeUserFromFirebase(id: String) {
        val reference = databaseReference

        reference.child(id).removeValue()
    }
}
