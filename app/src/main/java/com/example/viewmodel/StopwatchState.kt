package com.example.viewmodel

import kotlinx.coroutines.flow.MutableStateFlow

object StopwatchState {
    val stopwatchSeconds = MutableStateFlow(0L)
    val isStopwatchRunning = MutableStateFlow(false)

    val lunchStopwatchSeconds = MutableStateFlow(0L)
    val isLunchStopwatchRunning = MutableStateFlow(false)
}
