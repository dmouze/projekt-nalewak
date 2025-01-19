package com.kierman.projektnalewak.viewmodel

import androidx.appcompat.app.AppCompatActivity
import androidx.databinding.ObservableBoolean
import androidx.databinding.ObservableField
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.kierman.projektnalewak.util.Event
import com.kierman.projektnalewak.util.Util
import java.nio.charset.Charset

class NalewakViewModel(private val repository: Repository): ViewModel() {

    val connected: LiveData<Boolean?>
        get() = repository.connected
    val progressState: LiveData<String>
        get() = repository.progressState
    var btnConnected = ObservableBoolean(false)

    var inProgressView = ObservableBoolean(false)
    var txtProgress: ObservableField<String> = ObservableField("")

    private val _requestBleOn = MutableLiveData<Event<Boolean>>()
    val requestBleOn: LiveData<Event<Boolean>>
        get() = _requestBleOn

    val inProgress: LiveData<Event<Boolean>>
        get() = repository.inProgress

    val connectError: LiveData<Event<Boolean>>
        get() = repository.connectError

    val txtRead: ObservableField<String> = ObservableField("")
    val putTxt: LiveData<String>
        get() = repository.putTxt


    fun setInProgress(en: Boolean){
        repository.inProgress.value = Event(en)
    }

    fun onClickConnect(activity: AppCompatActivity) {
        if (connected.value == false || connected.value == null) {
            if (repository.isBluetoothSupport()) {
                if (repository.isBluetoothEnabled()) {
                    setInProgress(true)
                    repository.scanDevice(activity) // 🔹 Przekazanie activity
                } else {
                    _requestBleOn.value = Event(true)
                }
            } else {
                Util.showNotification("Bluetooth nie jest obsługiwany.")
            }
        } else {
            repository.disconnect()
        }
    }



    fun unregisterReceiver(){
        repository.unregisterReceiver()
    }

    fun onClickSendData(sendTxt: String){
        val byteArr = sendTxt.toByteArray(Charset.defaultCharset())
        repository.sendByteData(byteArr)
    }
}
