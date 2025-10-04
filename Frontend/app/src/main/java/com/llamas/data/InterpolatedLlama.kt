package com.llamas.data

data class InterpolatedLlama(
    val id : Int,
    val currentPosition : SimpleCoordinate,
    val movementPerSecond : SimpleCoordinate,
    val bearing : Double,
    val lastUpdatedTime : Long
)
