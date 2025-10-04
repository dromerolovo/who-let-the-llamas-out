package com.llamas.data

data class LlamaDto(
    val id : Int,
    val bearing : Double,
    val currentPosition : SimpleCoordinate,
    val movementPerSecond : SimpleCoordinate
)
