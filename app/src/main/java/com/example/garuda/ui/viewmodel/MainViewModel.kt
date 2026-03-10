package com.example.garuda.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.garuda.domain.repository.AppPreferencesRepository
import com.example.garuda.ui.navigation.Screen
import com.google.firebase.auth.FirebaseAuth
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class MainViewModel @Inject constructor(
    private val appPreferencesRepository: AppPreferencesRepository,
    private val auth: FirebaseAuth
) : ViewModel() {

    private val _startDestination = MutableStateFlow<String?>(null)
    val startDestination: StateFlow<String?> = _startDestination.asStateFlow()

    init {
        viewModelScope.launch {
            val isOnboardingCompleted = appPreferencesRepository.onboardingCompleted.first()
            val currentUser = auth.currentUser

            if (!isOnboardingCompleted) {
                _startDestination.value = Screen.Onboarding.route
            } else if (currentUser == null) {
                _startDestination.value = Screen.Login.route
            } else {
                _startDestination.value = Screen.Home.route
            }
        }
    }
}
