package com.kierman.projektnalewak.viewmodel

import android.Manifest
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothSocket
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.lifecycle.MutableLiveData
import com.kierman.projektnalewak.MyApplication
import com.kierman.projektnalewak.util.Event
import com.kierman.projektnalewak.util.SPP_UUID
import com.kierman.projektnalewak.util.Util
import java.io.*
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.*

class Repository {

    var connected: MutableLiveData<Boolean?> = MutableLiveData(null)
    var progressState: MutableLiveData<String> = MutableLiveData("")
    val putTxt: MutableLiveData<String> = MutableLiveData("")

    val inProgress = MutableLiveData<Event<Boolean>>()
    val connectError = MutableLiveData<Event<Boolean>>()

    private var mBluetoothAdapter: BluetoothAdapter? = BluetoothAdapter.getDefaultAdapter()
    private var mBluetoothStateReceiver: BroadcastReceiver? = null
    var targetDevice: BluetoothDevice? = null
    private var socket: BluetoothSocket? = null
    private var mOutputStream: OutputStream? = null
    private var mInputStream: InputStream? = null

    var foundDevice: Boolean = false

    companion object {
        private const val REQUEST_BLUETOOTH_PERMISSIONS = 101
    }

    fun isBluetoothSupport(): Boolean {
        return if (mBluetoothAdapter == null) {
            Util.showNotification("Urządzenie nie obsługuje Bluetooth.")
            false
        } else {
            true
        }
    }

    fun isBluetoothEnabled(): Boolean {
        return if (mBluetoothAdapter?.isEnabled == false) {
            Util.showNotification("Proszę włączyć Bluetooth.")
            false
        } else {
            true
        }
    }

    fun scanDevice(activity: AppCompatActivity) {
        progressState.postValue("Skanowanie urządzeń...")

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) { // Android 12+
            if (ActivityCompat.checkSelfPermission(activity, Manifest.permission.BLUETOOTH_SCAN) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(
                    activity,
                    arrayOf(Manifest.permission.BLUETOOTH_SCAN, Manifest.permission.BLUETOOTH_CONNECT),
                    REQUEST_BLUETOOTH_PERMISSIONS
                )
                return
            }
        }

        registerBluetoothReceiver()
        mBluetoothAdapter?.startDiscovery()
    }

    fun sendByteData(data: ByteArray) {
        Thread {
            try {
                mOutputStream?.write(data)
                mOutputStream?.flush()
            } catch (e: IOException) {
                e.printStackTrace()
            }
        }.start()
    }

    fun disconnect() {
        try {
            socket?.close()
            connected.postValue(false)
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }

    fun unregisterReceiver() {
        if (mBluetoothStateReceiver != null) {
            MyApplication.applicationContext().unregisterReceiver(mBluetoothStateReceiver)
            mBluetoothStateReceiver = null
        }
    }

    private fun registerBluetoothReceiver() {
        val stateFilter = IntentFilter().apply {
            addAction(BluetoothAdapter.ACTION_STATE_CHANGED)
            addAction(BluetoothAdapter.ACTION_CONNECTION_STATE_CHANGED)
            addAction(BluetoothDevice.ACTION_ACL_CONNECTED)
            addAction(BluetoothDevice.ACTION_ACL_DISCONNECTED)
            addAction(BluetoothDevice.ACTION_BOND_STATE_CHANGED)
            addAction(BluetoothDevice.ACTION_FOUND)
            addAction(BluetoothAdapter.ACTION_DISCOVERY_STARTED)
            addAction(BluetoothAdapter.ACTION_DISCOVERY_FINISHED)
            addAction(BluetoothDevice.ACTION_PAIRING_REQUEST)
        }

        mBluetoothStateReceiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent) {
                when (intent.action) {
                    BluetoothAdapter.ACTION_STATE_CHANGED -> {
                        when (intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, BluetoothAdapter.ERROR)) {
                            BluetoothAdapter.STATE_OFF -> Log.d("Bluetooth", "Wyłączony")
                            BluetoothAdapter.STATE_ON -> Log.d("Bluetooth", "Włączony")
                        }
                    }
                    BluetoothDevice.ACTION_FOUND -> {
                        val device: BluetoothDevice? = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE)
                        if (device != null && !foundDevice) {
                            if (ActivityCompat.checkSelfPermission(context!!, Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED) {
                                val deviceName = device.name ?: "Unknown"
                                if (deviceName.startsWith("Luf")) {
                                    targetDevice = device
                                    foundDevice = true
                                    connectToTargetedDevice(device)
                                }
                            }
                        }
                    }
                    BluetoothDevice.ACTION_ACL_DISCONNECTED -> connected.postValue(false)
                }
            }
        }
        MyApplication.applicationContext().registerReceiver(mBluetoothStateReceiver, stateFilter)
    }

    fun connectToTargetedDevice(device: BluetoothDevice) {
        progressState.postValue("Łączenie z ${device.name}...")

        val thread = Thread {
            val uuid = UUID.fromString(SPP_UUID)
            try {
                if (ActivityCompat.checkSelfPermission(MyApplication.applicationContext(), Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
                    return@Thread
                }
                socket = device.createRfcommSocketToServiceRecord(uuid)
                socket?.connect()
                connected.postValue(true)
                mOutputStream = socket?.outputStream
                mInputStream = socket?.inputStream
            } catch (e: Exception) {
                connectError.postValue(Event(true))
                socket?.close()
            }
        }
        thread.start()
    }
}
