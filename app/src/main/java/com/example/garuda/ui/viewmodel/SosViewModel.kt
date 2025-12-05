package com.example.garuda.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.garuda.domain.manager.SosManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SosViewModel @Inject constructor(
    private val sosManager: SosManager
) : ViewModel() {

    fun onSosClicked() {
        viewModelScope.launch {
            sosManager.triggerSos()
            // Could add state update here (e.g. "SOS Sent!")
        }
    }
}
