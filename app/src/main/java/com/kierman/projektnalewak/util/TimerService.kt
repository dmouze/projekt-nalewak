package com.kierman.projektnalewak.util

import android.app.Service
import android.content.Intent
import android.os.IBinder
import java.util.Timer
import java.util.TimerTask

class TimerService : Service() {

    override fun onBind(intent: Intent?): IBinder? = null

    private val timer = Timer()
    private var time = 0.0

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        intent?.let {
            time = it.getDoubleExtra(TIME_EXTRA, 0.0)
        }

        timer.scheduleAtFixedRate(TimeTask(), 0, 1) // Aktualizacja co 10 milisekund
        return START_NOT_STICKY
    }

    override fun onDestroy() {
        timer.cancel()
        super.onDestroy()
    }

    private inner class TimeTask : TimerTask() {
        private val startTimeMillis = System.currentTimeMillis()

        override fun run() {
            val intent = Intent(TIMER_UPDATED)
            val currentTimeMillis = System.currentTimeMillis()
            val elapsedTimeSeconds = (currentTimeMillis - startTimeMillis) / 1000.0
            intent.putExtra(TIME_EXTRA, elapsedTimeSeconds)
            sendBroadcast(intent)
        }
    }
    companion object {
        const val TIMER_UPDATED = "timerUpdated"
        const val TIME_EXTRA = "timeExtra"
    }
}
