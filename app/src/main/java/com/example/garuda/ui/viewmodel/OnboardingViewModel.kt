package com.example.garuda.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.garuda.domain.repository.AppPreferencesRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class OnboardingViewModel @Inject constructor(
    private val appPreferencesRepository: AppPreferencesRepository
) : ViewModel() {

    val isOnboardingCompleted: StateFlow<Boolean> = appPreferencesRepository.onboardingCompleted
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = false
        )

    fun completeOnboarding() {
        viewModelScope.launch {
            appPreferencesRepository.saveOnboardingState(true)
        }
    }
}
