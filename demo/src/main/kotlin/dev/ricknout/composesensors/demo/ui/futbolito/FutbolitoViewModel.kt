package dev.ricknout.composesensors.demo.ui.futbolito

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class FutbolitoViewModel : ViewModel() {

    private val _scoreTop    = MutableStateFlow(0)
    val scoreTop: StateFlow<Int> = _scoreTop.asStateFlow()

    private val _scoreBottom = MutableStateFlow(0)
    val scoreBottom: StateFlow<Int> = _scoreBottom.asStateFlow()


    fun onGoalTop() {
        _scoreTop.value++
    }


    fun onGoalBottom() {
        _scoreBottom.value++
    }

    fun reset() {
        _scoreTop.value    = 0
        _scoreBottom.value = 0
    }
}
