package com.savorsight.sensor

data class HeadPose(
    val deltaYawDeg: Float = 0f,
    val deltaPitchDeg: Float = 0f,
    val isCalibrated: Boolean = false,
)
