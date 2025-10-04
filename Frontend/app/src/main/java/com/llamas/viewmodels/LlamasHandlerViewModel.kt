package com.llamas.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.llamas.data.InterpolatedLlama
import com.llamas.data.LlamaDto
import com.llamas.data.SimpleCoordinate
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class LlamasHandlerViewModel : ViewModel() {
    private val _llamasStates = MutableStateFlow<Map<Int, InterpolatedLlama>>(emptyMap())
    private val _interpolatedLlamas = MutableStateFlow<Map<Int, InterpolatedLlama>>(emptyMap())

    val interpolatedLlamas: StateFlow<Map<Int, InterpolatedLlama>> = _interpolatedLlamas.asStateFlow()

    init {
        startInterpolation()
    }

    fun updateLlamas(updates: List<LlamaDto>) {
        val currentTime = System.currentTimeMillis()
        val currentStates = _llamasStates.value.toMutableMap()

        updates.forEach { update ->
            val obj = currentStates[update.id]
            currentStates[update.id] = InterpolatedLlama(
                id = update.id,
                currentPosition = update.currentPosition,
                bearing = update.bearing,
                movementPerSecond = update.movementPerSecond,
                lastUpdatedTime = currentTime
            )
        }

        _llamasStates.value = currentStates
    }

    private fun startInterpolation() {
        viewModelScope.launch {
            while(true) {
                val currentTime = System.currentTimeMillis()
                val states = _llamasStates.value

                val interpolated = states.mapValues { (_, llm)  ->
                    val elapsedSeconds = (currentTime - llm.lastUpdatedTime) / 1000.0
                    val clampedElapsed = elapsedSeconds.coerceIn(0.0, 1.0)

                    val newLng = llm.currentPosition.x + (llm.movementPerSecond.x * clampedElapsed)
                    val newLat = llm.currentPosition.y + (llm.movementPerSecond.y * clampedElapsed)
                    val newPosition = SimpleCoordinate(newLng, newLat)

                    llm.copy(currentPosition = newPosition)
                }


                _interpolatedLlamas.value = interpolated

                delay(1)

            }
        }
    }
}