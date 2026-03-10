package com.example.garuda.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.garuda.domain.usecase.emergency.TriggerEmergencyUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SosViewModel @Inject constructor(
    private val triggerEmergencyUseCase: TriggerEmergencyUseCase
) : ViewModel() {

    fun onSosClicked() {
        viewModelScope.launch {
            triggerEmergencyUseCase()
            // Could add state update here (e.g. "SOS Sent!")
        }
    }
}
