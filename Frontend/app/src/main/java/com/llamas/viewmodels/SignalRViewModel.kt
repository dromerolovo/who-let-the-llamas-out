package com.llamas.viewmodels

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.llamas.data.LlamaDto
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import com.microsoft.signalr.HubConnection
import com.microsoft.signalr.HubConnectionBuilder

class SignalRViewModel : ViewModel() {
    private val _isConnected = MutableStateFlow(false)
    val isConnected: StateFlow<Boolean> = _isConnected.asStateFlow()

    private val _llamasUpdates = MutableStateFlow<List<LlamaDto>>(emptyList())
    val llamasUpdates: StateFlow<List<LlamaDto>> = _llamasUpdates.asStateFlow()

    private lateinit var hubConnection: HubConnection
    private val SIGNAL_URL = "http://10.0.2.2:5265/llamas-hub"

    init {
        setupSignalR()
    }

    private fun setupSignalR() {
        hubConnection = HubConnectionBuilder.create(SIGNAL_URL).build()


        hubConnection.on("TestMessage", { message ->
            Log.d("SignalR", "Received test message: $message")
        }, String::class.java)

        hubConnection.on("LlamasPositions", { llamas ->
            Log.d("SignalR", "Received Llamas: ${llamas.contentToString()}")
            Log.d("SignalR", "Array size: ${llamas.size}")
            _llamasUpdates.value = llamas.toList()
        }, Array<LlamaDto>::class.java)

        hubConnection.onClosed { exception ->
            Log.d("SignalR", "Connection closed: ${exception?.message}")
            _isConnected.value = false
        }

        viewModelScope.launch {
            try {
                withContext(Dispatchers.IO) {
                    Log.d("Connection", "SignalR Connecting to: $SIGNAL_URL")
                    hubConnection.start().blockingAwait()
                    _isConnected.value = true
                    Log.d("Connection", "SignalR Connected successfully!")
                }
            } catch (e: Exception) {
                Log.e("Connection", "Connection Failed: ${e.message}")
                Log.e("Connection", "Stack trace: ", e)
                _isConnected.value = false
            }
        }
    }
}