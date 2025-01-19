package com.kierman.projektnalewak.ui

import android.app.Activity
import android.bluetooth.BluetoothAdapter
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.View
import android.view.Window
import android.view.WindowManager
import android.widget.ImageView
import android.widget.Toast
import androidx.activity.result.ActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.databinding.DataBindingUtil
import com.kierman.projektnalewak.R
import com.kierman.projektnalewak.databinding.ActivityConnectBinding
import com.kierman.projektnalewak.util.PERMISSIONS
import com.kierman.projektnalewak.util.REQUEST_ALL_PERMISSION
import com.kierman.projektnalewak.util.Util
import com.kierman.projektnalewak.viewmodel.NalewakViewModel
import org.koin.androidx.viewmodel.ext.android.viewModel

@Suppress("DEPRECATION")
class ConnectActivity : AppCompatActivity() {
    private lateinit var binding: ActivityConnectBinding

    private val viewModel by viewModel<NalewakViewModel>()

    var mBluetoothAdapter: BluetoothAdapter? = null
    private var recv: String = ""


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_FULLSCREEN
        requestWindowFeature(Window.FEATURE_NO_TITLE)
        window.setFlags(
            WindowManager.LayoutParams.FLAG_FULLSCREEN,
            WindowManager.LayoutParams.FLAG_FULLSCREEN
        )
        actionBar?.hide()

        binding = DataBindingUtil.setContentView(
            this,
            R.layout.activity_connect
        )

        binding.viewModel = viewModel
        binding.activity = this

        val arrow = findViewById<ImageView>(R.id.arrow)
        arrow.visibility = View.INVISIBLE
        arrow.setOnClickListener {
            val intent = Intent(this, ChoosePlayerActivity::class.java)
            startActivity(intent)
        }

        val lista = findViewById<ImageView>(R.id.listimg)


        lista.setOnClickListener {
            val intent = Intent(this, RankingActivity::class.java)
            startActivity(intent)
        }


        if (!hasPermissions(this, PERMISSIONS)) {
            requestPermissions(PERMISSIONS, REQUEST_ALL_PERMISSION)
        }

        initObserving()


    }

    private val startForResult =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result: ActivityResult ->
            if (result.resultCode == Activity.RESULT_OK) {
                viewModel.onClickConnect(this@ConnectActivity)
            }
        }

    private fun sendConnectionLostBroadcast() {
        val intent = Intent("BLUETOOTH_CONNECTION_LOST")
        sendBroadcast(intent)
    }


    private fun initObserving() {
        // Obserwowanie postępu
        viewModel.inProgress.observe(this) {
            if (it.getContentIfNotHandled() == true) {
                viewModel.inProgressView.set(true)
            } else {
                viewModel.inProgressView.set(false)
            }
        }

        // Obserwowanie stanu postępu
        viewModel.progressState.observe(this) {
            viewModel.txtProgress.set(it)
        }

        // Żądanie włączenia Bluetooth
        viewModel.requestBleOn.observe(this) {
            val enableBtIntent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
            startForResult.launch(enableBtIntent)
        }

        // Zdarzenie połączenia/rozłączenia Bluetooth
        viewModel.connected.observe(this) {
            if (it != null) {
                if (it) {
                    viewModel.setInProgress(false)
                    viewModel.btnConnected.set(true)
                    Util.showNotification("Urządzenie zostało połączone.")
                    val intent = Intent(this, ChoosePlayerActivity::class.java)
                    startActivity(intent)
                    val arrow = findViewById<ImageView>(R.id.arrow)
                    arrow.visibility = View.VISIBLE
                } else {
                    sendConnectionLostBroadcast()
                    viewModel.setInProgress(false)
                    viewModel.btnConnected.set(false)
                    Util.showNotification("Połączenie z urządzeniem zostało przerwane.")
                }
            }
        }

        // Błąd połączenia Bluetooth
        viewModel.connectError.observe(this) {
            Util.showNotification("Błąd połączenia. Proszę sprawdzić urządzenie.")
            viewModel.setInProgress(false)
        }

        // Odebranie danych
        viewModel.putTxt.observe(this) {
            if (it != null) {
                recv += it
//                sv_read_data.fullScroll(View.FOCUS_DOWN)
                viewModel.txtRead.set(recv)
            }
        }
    }

    private fun hasPermissions(context: Context?, permissions: Array<String>): Boolean {
        for (permission in permissions) {
            if (context?.let { ActivityCompat.checkSelfPermission(it, permission) }
                != PackageManager.PERMISSION_GRANTED
            ) {
                return false
            }
        }
        return true
    }

    // Sprawdzanie uprawnień
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String?>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        when (requestCode) {
            REQUEST_ALL_PERMISSION -> {
                // Jeśli żądanie zostało anulowane, wynikowe tablice są puste.
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    Toast.makeText(this, "Uprawnienia przyznane!", Toast.LENGTH_SHORT).show()
                } else {
                    requestPermissions(permissions, REQUEST_ALL_PERMISSION)
                    Toast.makeText(this, "Uprawnienia muszą zostać przyznane", Toast.LENGTH_SHORT)
                        .show()
                }
            }
        }
    }

    override fun onPause() {
        super.onPause()
        viewModel.unregisterReceiver()
    }

    @Deprecated("Deprecated in Java")
    override fun onBackPressed() {
        super.onBackPressed()
        viewModel.setInProgress(false)
    }
}
