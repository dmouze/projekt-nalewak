@file:Suppress("DEPRECATION")

package com.kierman.projektnalewak.ui

import android.annotation.SuppressLint
import android.app.AlertDialog
import android.app.Dialog
import android.os.Bundle
import android.util.Log
import android.view.View
import android.view.Window
import android.view.WindowManager
import android.widget.ListView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.firebase.database.*
import com.kierman.projektnalewak.util.UserModel
import com.kierman.projektnalewak.databinding.ActivityRankingBinding
import com.kierman.projektnalewak.util.ResultsAdapter
import com.kierman.projektnalewak.util.UserListAdapter
import com.kierman.projektnalewak.R


class RankingActivity : AppCompatActivity(), UserListAdapter.ItemClickListener,
    UserListAdapter.ItemLongClickListener {


    private lateinit var binding: ActivityRankingBinding
    private lateinit var adapter: UserListAdapter
    private lateinit var databaseReference: DatabaseReference

    @SuppressLint("NotifyDataSetChanged")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityRankingBinding.inflate(layoutInflater)
        requestWindowFeature(Window.FEATURE_NO_TITLE)
        window.setFlags(
            WindowManager.LayoutParams.FLAG_FULLSCREEN,
            WindowManager.LayoutParams.FLAG_FULLSCREEN
        )
        window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_FULLSCREEN
        actionBar?.hide()
        setContentView(binding.root)

        val firebaseDatabase = FirebaseDatabase.getInstance()
        databaseReference =
            firebaseDatabase.reference.child("menele") // Zmień na odpowiednią ścieżkę w swojej bazie danych

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
                    val name = userSnapshot.child("imie")
                        .getValue(String::class.java) // Pobierz imię użytkownika
                    val czasMap = userSnapshot.child("czas")
                        .getValue(object : GenericTypeIndicator<HashMap<String, Double>>() {})
                    val timeList = ArrayList(czasMap?.values ?: emptyList())
                    val user = UserModel(
                        id,
                        name,
                        timeList
                    ) // Tworzenie UserModel z ID, imieniem i czasami
                    userList.add(user)
                }
                adapter.notifyDataSetChanged()
            }

            override fun onCancelled(error: DatabaseError) {
            }
        })
    }

    override fun onItemClick(user: UserModel) {
        val results = user.time
        val id = user.id
        val imie = user.name
        showResultsDialog(results, imie, id)
    }
    override fun onItemLongClick(user: UserModel) {
        val id = user.id
        val imie = user.name
        showDeleteUserDialog(imie, id)
    }
    @SuppressLint("InflateParams", "SetTextI18n")
    private fun showResultsDialog(results: List<Double>?, imie: String?, id: String?) {
        val dialog = Dialog(this)
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
        dialog.setContentView(R.layout.custom_result_list)

        val window = dialog.window
        val attributes = window?.attributes

        attributes?.width = WindowManager.LayoutParams.MATCH_PARENT
        attributes?.height = WindowManager.LayoutParams.WRAP_CONTENT
        window?.decorView?.systemUiVisibility = View.SYSTEM_UI_FLAG_FULLSCREEN

        // Ustawienie tła dla okna dialogowego
        window?.setBackgroundDrawableResource(R.drawable.round_background_white)

        attributes?.y = resources.getDimensionPixelSize(R.dimen.bottom_margin)

        val titleTextView = dialog.findViewById<TextView>(R.id.resultTitleTextView)
        titleTextView.text = "Wyniki osoby: $imie"

        val listView = dialog.findViewById<ListView>(R.id.resultListView)

        // Wyszukaj najlepszy wynik (najkrótszy czas)
        val bestResult = results
            ?.filter { it > 0 } // Usuń nulle z listy wyników
            ?.minByOrNull { it }

        val otherResults = results
            ?.filter { it > 0 } // Usuń nulle z listy wyników
            ?.filter { it != bestResult } // Usuń najlepszy wynik
            ?.map { getTimeStringFromDouble(it) } // Konwertuj wynik na "ss:SSS"
            ?.toMutableList()
            ?.asReversed()

        val bestResultWithEmoji = bestResult?.let { "🥇${getTimeStringFromDouble(it)}" } ?: ""
        val sortedResults = mutableListOf<String>()
        if (bestResult != null) {
            sortedResults.add(bestResultWithEmoji)
        }
        if (otherResults != null) {
            sortedResults.addAll(otherResults)
        }

        val adapter = ResultsAdapter(sortedResults)
        listView.adapter = adapter

        fun removeValueFromFirebase(id: String, value: Double) {
            val reference = databaseReference

            // Tworzenie referencji do konkretnego użytkownika na podstawie ID
            val userReference = reference.child(id)

            userReference.addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(dataSnapshot: DataSnapshot) {
                    val userData = dataSnapshot.getValue(object : GenericTypeIndicator<Map<String, Any>>() {})

                    if (userData != null) {
                        // Pobieranie mapy czasów użytkownika
                        val czasMap = userData["czas"] as? Map<String, Any>

                        if (czasMap != null) {
                            for ((czasKey, czasValue) in czasMap) {
                                if (czasValue is Double && czasValue == value) {
                                    userReference.child("czas").child(czasKey).removeValue()
                                }
                            }
                        }
                    }
                }

                override fun onCancelled(databaseError: DatabaseError) {
                    // Obsługa błędów
                    println("Error: ${databaseError.message}")
                }
            })
        }


        listView.setOnItemLongClickListener { _, _, position, _ ->
            val selectedResult = sortedResults[position]

            val resultValue = selectedResult.replace(":", ".")

            // Dostosuj format wartości do tego samego, co w bazie danych
            val formatedValue = String.format("%.3f", resultValue.toDouble())

            val alertDialogBuilder = AlertDialog.Builder(this)
            alertDialogBuilder.setTitle("Potwierdź usunięcie wyniku")
            alertDialogBuilder.setMessage("Czy na pewno chcesz usunąć wynik: $selectedResult?")
            alertDialogBuilder.setPositiveButton("Tak") { _, _ ->
                // Usuń wynik z Firebase
                Log.d("odczyt", id.toString() + formatedValue)

                if (id != null) {
                    removeValueFromFirebase(id, formatedValue.replace(",", ".").toDouble())
                }

                // Po usunięciu odśwież widok
                adapter.notifyDataSetChanged()
                dialog.dismiss()
            }
            alertDialogBuilder.setNegativeButton("Anuluj") { _, _ ->
                // Nic nie rób, po prostu zamknij dialog potwierdzający
            }

            val alertDialog = alertDialogBuilder.create()
            alertDialog.show()

            true
        }

        dialog.show()
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
            // Nic nie rób, po prostu zamknij dialog potwierdzający
        }

        val alertDialog = alertDialogBuilder.create()
        alertDialog.show()
    }

    private fun removeUserFromFirebase(id: String) {
        val reference = databaseReference

        // Usuń użytkownika z bazy danych
        reference.child(id).removeValue()
    }


    private fun getTimeStringFromDouble(time: Double): String {
        val seconds = time.toInt()
        val milliseconds = ((time - seconds) * 1000).toInt()
        return String.format("%02d:%03d", seconds, milliseconds)
    }


}
