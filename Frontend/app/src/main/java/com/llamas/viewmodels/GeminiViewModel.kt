package com.llamas.viewmodels

import android.annotation.SuppressLint
import android.provider.Settings.Global.getString
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.ai.client.generativeai.GenerativeModel
import com.llamas.R
import com.llamas.data.SimpleCoordinate
import com.mapbox.geojson.Point
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlin.random.Random

class GeminiViewModel(private val geminiToken: String) : ViewModel() {

    private val _aiResponse = MutableStateFlow<String?>("Hello Stranger")
    val aiResponse: StateFlow<String?> = _aiResponse.asStateFlow()

    private val generativeModel = GenerativeModel(
        modelName = "gemini-2.5-flash-lite",
        apiKey = geminiToken
    )

    fun generateContent(coordinates: Point) {
        val prompt = buildPromptForCapture(coordinates)
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val response = generativeModel.generateContent(prompt)
                _aiResponse.value = response.text
            } catch (e: Exception) {
                _aiResponse.value = "Error: ${e.message}"
            }
        }
    }

    fun generateContent(coordinates: List<SimpleCoordinate>) {
        val prompt = buildPromptForHint(coordinates)
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val response = generativeModel.generateContent(prompt)
                _aiResponse.value = response.text
            } catch (e: Exception) {
                _aiResponse.value = "Error: ${e.message}"
            }
        }
    }

    @SuppressLint("DefaultLocale")
    private fun buildPromptForHint(otherLlamas: List<SimpleCoordinate>) : String {
        val random = Random.nextInt(1, otherLlamas.size);
        val selected = otherLlamas[random]

        val lat = selected.y
        val long = selected.x

        val point = String.format("Latitude: %f, Longitude: %f", lat, long)

        val prompt = """
            Make your response two lines long
            You are very sarcastic and cynical, in the middle of an existential crisis, but you still have a lot of humor. 
            You are a llama wandering around Vancouver for some reason—it doesn’t make sense, and you know it. 
            People might try to capture you, but of course, you don’t care. 
            If you are receiving this message, it means that someone is asking for a hint about where are the other llamas. The other llama is located at ${point}. 
            Make your response two lines long. Your language shouldn’t be complex and should be rated M (mature).
        """.trimIndent()

        return prompt
    }

    @SuppressLint("DefaultLocale")
    private fun buildPromptForCapture(coordinates : Point) : String {

        val lat = coordinates.latitude()
        val long = coordinates.longitude()

        val point = String.format("Latitude: %f, Longitude: %f", lat, long)

        val prompt = """
            Make your response two lines long
            You are very sarcastic and cynical, in the middle of an existential crisis, but you still have a lot of humor. 
            You are a llama wandering around Vancouver for some reason—it doesn’t make sense, and you know it. 
            People might try to capture you, but of course, you don’t care. 
            If you are receiving this message, it means that you will be captured. Your current location is ${point}. 
            Make your response two lines long. Your language shouldn’t be complex and should be rated M (mature).
        """.trimIndent()

        return prompt
    }


}