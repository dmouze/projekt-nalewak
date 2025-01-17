package com.kierman.projektnalewak.ui

import android.annotation.SuppressLint
import android.app.AlertDialog
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Bundle
import android.view.View
import android.view.Window
import android.view.WindowManager
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.databinding.DataBindingUtil
import com.google.firebase.FirebaseApp
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.analytics.ktx.analytics
import com.google.firebase.analytics.logEvent
import com.google.firebase.ktx.Firebase
import com.kierman.projektnalewak.R
import com.kierman.projektnalewak.databinding.ActivityTimerBinding
import com.kierman.projektnalewak.util.TimerService
import com.kierman.projektnalewak.viewmodel.NalewakViewModel
import org.koin.androidx.viewmodel.ext.android.viewModel

@Suppress("DEPRECATION")
class TimerActivity : AppCompatActivity() {

    private lateinit var firebaseAnalytics: FirebaseAnalytics
    private var recv: String = ""
    private var timerStarted = false
    private lateinit var serviceIntent: Intent
    private var time = 0.0
    private lateinit var binding: ActivityTimerBinding
    private val viewModel by viewModel<NalewakViewModel>()
    private var userId = ""
    private var imie = ""
    private var results = ArrayList<Double>()

    @SuppressLint("UnspecifiedRegisterReceiverFlag", "NotifyDataSetChanged")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        registerReceiver(connectionLostReceiver, IntentFilter("BLUETOOTH_CONNECTION_LOST"))
        window.setFlags(
            WindowManager.LayoutParams.FLAG_FULLSCREEN,
            WindowManager.LayoutParams.FLAG_FULLSCREEN
        )
        window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_FULLSCREEN
        actionBar?.hide()
        requestWindowFeature(Window.FEATURE_NO_TITLE)
        binding = DataBindingUtil.setContentView(
            this,
            R.layout.activity_timer
        )

        FirebaseApp.initializeApp(this)
        firebaseAnalytics = Firebase.analytics

        getValues()

        val reset = findViewById<ImageView>(R.id.resetButton)
        val changeUser = findViewById<TextView>(R.id.text_change)
        val cleaning = findViewById<TextView>(R.id.cleaning)

        cleaning.setOnClickListener {
            showCleaningDialog()
        }

        showResults(results, imie)

        changeUser.setOnClickListener {
            firebaseAnalytics.logEvent("change_user") {
                param("user_id", userId)
            }
            val intent = Intent(this, ChoosePlayerActivity::class.java)
            startActivity(intent)
            finish()
        }

        viewModel.putTxt.observe(this) { newReceivedData ->
            if (newReceivedData != null) {
                recv = newReceivedData
                viewModel.txtRead.set(recv)
                val currentTime = System.currentTimeMillis()

                if (recv == "a" && !timerStarted) {
                    startTimer()
                    firebaseAnalytics.logEvent("timer_started") {}
                } else if (recv != "a" && timerStarted) {
                    stopTimer()
                    firebaseAnalytics.logEvent("timer_stopped") {
                        param("elapsed_time", time)
                    }
                }
            }
        }

        reset.setOnClickListener {
            resetTimer()
            firebaseAnalytics.logEvent("timer_reset") {}
        }

        serviceIntent = Intent(applicationContext, TimerService::class.java)
        registerReceiver(updateTime, IntentFilter(TimerService.TIMER_UPDATED))

        binding.viewModel = viewModel
    }

    private fun startTimer() {
        serviceIntent.putExtra(TimerService.TIME_EXTRA, time)
        startService(serviceIntent)
        timerStarted = true
    }

    private fun stopTimer() {
        stopService(serviceIntent)
        timerStarted = false
    }

    private fun resetTimer() {
        stopTimer()
        time = 0.0
        binding.timeTV.text = "00:000"
    }

    private val updateTime: BroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            val newTime = intent?.getDoubleExtra(TimerService.TIME_EXTRA, 0.0) ?: 0.0
            time = newTime
            binding.timeTV.text = String.format("%02d:%03d", time.toInt(), ((time - time.toInt()) * 1000).toInt())
        }
    }

    private val connectionLostReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            stopTimer()
            Toast.makeText(context, "Połączenie z urządzeniem zostało przerwane.", Toast.LENGTH_SHORT).show()
        }
    }

    private fun getValues() {
        imie = intent.getStringExtra("user_name") ?: ""
        userId = intent.getStringExtra("user_id") ?: ""
        results = intent.getSerializableExtra("user_results") as? ArrayList<Double> ?: ArrayList()
    }

    private fun showCleaningDialog() {
        val alertDialogBuilder = AlertDialog.Builder(this)
        alertDialogBuilder.setTitle("Zarządzaj otwarciem zaworu")
        alertDialogBuilder.setMessage("Otwórz lub zamknij zawór")
        alertDialogBuilder.setPositiveButton("OTWÓRZ") { _, _ ->
            viewModel.onClickSendData("c")
        }
        alertDialogBuilder.setNegativeButton("ZAMKNIJ") { _, _ ->
            viewModel.onClickSendData("d")
        }
        val alertDialog = alertDialogBuilder.create()
        alertDialog.show()
    }

    private fun showResults(results: ArrayList<Double>, imie: String) {
        val currentUser = findViewById<TextView>(R.id.current_user)
        currentUser.text = imie
    }

    override fun onDestroy() {
        super.onDestroy()
        unregisterReceiver(connectionLostReceiver)
    }
}
