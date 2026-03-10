package com.example.garuda.domain.manager

/**
 * Abstracts Android system interactions during an emergency (like SMS and Foreground Services)
 * away from the pure Kotlin domain layer.
 */
interface SystemEmergencyManager {
    /**
     * Starts the foreground service responsible for continuous background tasks
     * like tracking location and recording audio during an emergency.
     */
    fun startEmergencyService()

    /**
     * Sends an SMS message to a specific phone number.
     * @param phoneNumber The destination number
     * @param message The text content
     * @return Result indicating success or failure
     */
    fun sendSms(phoneNumber: String, message: String): Result<Unit>
}
