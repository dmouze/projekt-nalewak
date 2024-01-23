package com.kierman.projektnalewak.viewmodel

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothSocket
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.util.Log
import androidx.lifecycle.MutableLiveData
import com.kierman.projektnalewak.MyApplication
import com.kierman.projektnalewak.util.Event
import com.kierman.projektnalewak.util.SPP_UUID
import com.kierman.projektnalewak.util.Util
import java.io.*
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.*

@Suppress("ASSIGNED_BUT_NEVER_ACCESSED_VARIABLE")
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



    fun isBluetoothSupport(): Boolean {
        return if (mBluetoothAdapter == null) {
            Util.showNotification("Urządzenie nie obsługuje Bluetooth.")
            false
        } else {
            true
        }
    }

    fun isBluetoothEnabled(): Boolean {
        return if (!mBluetoothAdapter!!.isEnabled) {
            // Urządzenie obsługuje Bluetooth, ale jest wyłączone
            // Wymagane jest aktywowanie Bluetooth za zgodą użytkownika
            Util.showNotification("Proszę włączyć Bluetooth.")
            false
        } else {
            true
        }
    }

    fun scanDevice() {
        progressState.postValue("Skanowanie urządzeń...")

        registerBluetoothReceiver()

        val bluetoothAdapter = mBluetoothAdapter
        foundDevice = false
        bluetoothAdapter?.startDiscovery() // Rozpoczęcie skanowania urządzeń Bluetooth
    }


    private fun registerBluetoothReceiver() {
        val stateFilter = IntentFilter()
        stateFilter.addAction(BluetoothAdapter.ACTION_STATE_CHANGED) // BluetoothAdapter.ACTION_STATE_CHANGED: zmiana stanu Bluetooth
        stateFilter.addAction(BluetoothAdapter.ACTION_CONNECTION_STATE_CHANGED)
        stateFilter.addAction(BluetoothDevice.ACTION_ACL_CONNECTED) // Połączenie nawiązane
        stateFilter.addAction(BluetoothDevice.ACTION_ACL_DISCONNECTED) // Połączenie przerwane
        stateFilter.addAction(BluetoothDevice.ACTION_BOND_STATE_CHANGED)
        stateFilter.addAction(BluetoothDevice.ACTION_FOUND) // Urządzenie znalezione
        stateFilter.addAction(BluetoothAdapter.ACTION_DISCOVERY_STARTED) // Rozpoczęcie skanowania urządzeń
        stateFilter.addAction(BluetoothAdapter.ACTION_DISCOVERY_FINISHED) // Zakończenie skanowania urządzeń
        stateFilter.addAction(BluetoothDevice.ACTION_PAIRING_REQUEST)
        mBluetoothStateReceiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent) {
                val action = intent.action // Pobranie akcji
                if (action != null) {
                    Log.d("Bluetooth action", action)
                }
                val device =
                    intent.getParcelableExtra<BluetoothDevice>(BluetoothDevice.EXTRA_DEVICE)
                var name: String? = null
                if (device != null) {
                    name = device.name // Pobranie nazwy urządzenia z wiadomości broadcast
                }
                when (action) {
                    BluetoothAdapter.ACTION_STATE_CHANGED -> {
                        val state = intent.getIntExtra(
                            BluetoothAdapter.EXTRA_STATE,
                            BluetoothAdapter.ERROR
                        )
                        when (state) {
                            BluetoothAdapter.STATE_OFF -> {
                            }

                            BluetoothAdapter.STATE_TURNING_OFF -> {
                            }

                            BluetoothAdapter.STATE_ON -> {
                            }

                            BluetoothAdapter.STATE_TURNING_ON -> {
                            }
                        }
                    }

                    BluetoothDevice.ACTION_ACL_CONNECTED -> {

                    }

                    BluetoothDevice.ACTION_BOND_STATE_CHANGED -> {
                    }

                    BluetoothDevice.ACTION_ACL_DISCONNECTED -> {
                        connected.postValue(false)
                    }

                    BluetoothAdapter.ACTION_DISCOVERY_STARTED -> {
                    }

                    BluetoothDevice.ACTION_FOUND -> {
                        if (!foundDevice) {
                            val deviceName = device!!.name
                            val deviceAddress = device.address

                            // Szukanie urządzeń o nazwie zaczynającej się od "RNM"
                            if (deviceName != null && deviceName.length > 4) {
                                if (deviceName.substring(0, 3) == "Luf") {
                                    // Filtruj urządzenie docelowe i użyj connectToTargetedDevice()
                                    targetDevice = device
                                    foundDevice = true
                                    connectToTargetedDevice(targetDevice)
                                }
                            }
                        }
                    }
                }
            }
        }
        MyApplication.applicationContext().registerReceiver(
            mBluetoothStateReceiver,
            stateFilter
        )
    }

    @ExperimentalUnsignedTypes
    private fun connectToTargetedDevice(targetedDevice: BluetoothDevice?) {
        progressState.postValue("Łączenie z ${targetDevice?.name}...")

        val thread = Thread {
            val uuid = UUID.fromString(SPP_UUID)
            try {
                // Utworzenie gniazda BluetoothSocket
                socket = targetedDevice?.createRfcommSocketToServiceRecord(uuid)

                socket?.connect()

               //Po nawiązaniu połączenia

                connected.postValue(true)
                mOutputStream = socket?.outputStream
                mInputStream = socket?.inputStream
                // Nasłuchiwanie na dane
                beginListenForData()

            } catch (e: Exception) {
                // Błąd podczas nawiązywania połączenia Bluetooth
                e.printStackTrace()
                connectError.postValue(Event(true))
                try {
                    socket?.close()
                } catch (e: IOException) {
                    e.printStackTrace()
                }
            }
        }

        // Rozpoczęcie wątku do nawiązania połączenia
        thread.start()
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

     // Wysyłanie danych za pomocą Bluetooth

    fun sendByteData(data: ByteArray) {
        Thread {
            try {
                mOutputStream?.write(data) // Wysłanie danych
            } catch (e: Exception) {
                // Błąd podczas wysyłania danych
                e.printStackTrace()
            }
        }.start()
    }


     // Konwersja
     // @ByteToUint: byte[] -> uint
     // @byteArrayToHex: byte[] -> hex string

    private val mByteBuffer: ByteBuffer = ByteBuffer.allocateDirect(8)
    // byte -> uint
    fun byteToUnit(data: ByteArray?, offset: Int, endian: ByteOrder): Long {
        synchronized(mByteBuffer) {
            mByteBuffer.clear()
            mByteBuffer.order(endian)
            mByteBuffer.limit(8)
            if (endian === ByteOrder.LITTLE_ENDIAN) {
                mByteBuffer.put(data, offset, 4)
                mByteBuffer.putInt(0)
            } else {
                mByteBuffer.putInt(0)
                mByteBuffer.put(data, offset, 4)
            }
            mByteBuffer.position(0)
            return mByteBuffer.long
        }
    }

    fun byteArrayToHex(a: ByteArray): String {
        val sb = StringBuilder()
        for (b in a) sb.append(String.format("%02x ", b /*&0xff*/))
        return sb.toString()
    }


     // Nasłuchiwanie na dane Bluetooth

    @ExperimentalUnsignedTypes
    fun beginListenForData() {
        val mWorkerThread = Thread {
            while (!Thread.currentThread().isInterrupted) {
                try {
                    val bytesAvailable = mInputStream?.available()
                    if (bytesAvailable != null) {
                        if (bytesAvailable > 0) { // Dane odebrane
                            val packetBytes = ByteArray(bytesAvailable)
                            mInputStream?.read(packetBytes)

                            /**
                             * Obsługa bufora
                             */
                            val s = String(packetBytes,Charsets.UTF_8)
                            putTxt.postValue(s)

                            /**
                             * Obsługa pojedynczego bajtu
                             */
                            for (i in 0 until bytesAvailable) {
                                val b = packetBytes[i]
                                Log.d("inputData", String.format("%02x", b))
                            }
                        }
                    }
                } catch (e: UnsupportedEncodingException) {
                    e.printStackTrace()
                } catch (e: IOException) {
                    e.printStackTrace()
                }
            }
        }

        // Rozpoczęcie wątku nasłuchującego na dane
        mWorkerThread.start()
    }
}
