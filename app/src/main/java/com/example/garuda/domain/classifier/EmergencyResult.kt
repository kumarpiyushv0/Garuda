package com.example.garuda.domain.classifier

/**
 * Result of emergency intent analysis
 */
data class EmergencyResult(
    val isLegit: Boolean,
    val confidence: Float,  // 0.0 to 1.0
    val reason: String,
    val emotionIndicators: List<String> = emptyList(),
    val urgencyLevel: UrgencyLevel = UrgencyLevel.UNKNOWN,
    val source: AnalysisSource = AnalysisSource.ON_DEVICE
)

enum class UrgencyLevel {
    CRITICAL,   // Immediate danger
    HIGH,       // Urgent but not immediate
    MEDIUM,     // Possible emergency
    LOW,        // Unlikely emergency
    UNKNOWN
}

enum class AnalysisSource {
    ON_DEVICE,
    CLOUD_GEMINI,
    HYBRID
}

/**
 * Input context for analysis
 */
data class AnalysisInput(
    val transcribedText: String,
    val previousContext: String? = null,
    val audioToneIndicators: AudioToneIndicators? = null,
    val timestamp: Long = System.currentTimeMillis()
)

/**
 * Placeholder for audio-based tone analysis
 * Can be populated by an audio classifier if available
 */
data class AudioToneIndicators(
    val detectedEmotion: String = "unknown",  // "fear", "panic", "calm", "angry"
    val voiceStressLevel: Float = 0f,         // 0.0 to 1.0
    val speechRate: SpeechRate = SpeechRate.NORMAL,
    val hasBreathiness: Boolean = false,
    val hasTrembling: Boolean = false
)

enum class SpeechRate {
    VERY_SLOW,
    SLOW,
    NORMAL,
    FAST,
    VERY_FAST  // Indicates panic
}
