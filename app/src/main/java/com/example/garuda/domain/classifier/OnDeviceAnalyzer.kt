package com.example.garuda.domain.classifier

import android.util.Log
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

/**
 * On-Device Emergency Intent Analyzer
 * Uses weighted keyword matching, sentiment analysis, and pattern detection
 * Works completely offline with no external dependencies
 */
@Singleton
class OnDeviceAnalyzer @Inject constructor() : EmergencyIntentAnalyzer {

    companion object {
        private const val TAG = "OnDeviceAnalyzer"
    }

    // ==================== KEYWORD DICTIONARIES ====================

    // Trigger phrases that must be present (with base weights)
    private val triggerPhrases = mapOf(
        "garuda help" to 0.25f,
        "hey garuda" to 0.20f,
        "garuda emergency" to 0.30f,
        "garuda sos" to 0.30f,
        "help me garuda" to 0.25f,
        "garuda save me" to 0.30f,
        "garuda please" to 0.22f
    )

    // Urgency indicators (add to confidence)
    private val urgencyIndicators = mapOf(
        "please" to 0.08f,
        "now" to 0.10f,
        "quick" to 0.10f,
        "quickly" to 0.10f,
        "hurry" to 0.12f,
        "fast" to 0.08f,
        "immediately" to 0.12f,
        "urgent" to 0.12f,
        "right now" to 0.12f,
        "asap" to 0.10f
    )

    // Distress/fear indicators (add significant confidence)
    private val distressIndicators = mapOf(
        "scared" to 0.15f,
        "afraid" to 0.15f,
        "fear" to 0.12f,
        "terrified" to 0.18f,
        "panic" to 0.15f,
        "danger" to 0.18f,
        "dangerous" to 0.18f,
        "threatening" to 0.20f,
        "threat" to 0.18f,
        "attack" to 0.22f,
        "attacking" to 0.22f,
        "following" to 0.15f,
        "chasing" to 0.18f,
        "stalking" to 0.20f,
        "stalker" to 0.20f,
        "stranger" to 0.12f,
        "creepy" to 0.10f,
        "trapped" to 0.18f,
        "alone" to 0.08f,
        "dark" to 0.05f,
        "hurt" to 0.15f,
        "hurting" to 0.15f,
        "pain" to 0.12f
    )

    // Critical emergency indicators (very high weight)
    private val criticalIndicators = mapOf(
        "rape" to 0.30f,
        "kidnap" to 0.30f,
        "kill" to 0.28f,
        "killing" to 0.28f,
        "murder" to 0.30f,
        "weapon" to 0.25f,
        "gun" to 0.28f,
        "knife" to 0.25f,
        "die" to 0.22f,
        "dying" to 0.25f,
        "911" to 0.25f,
        "police" to 0.15f,
        "ambulance" to 0.15f
    )

    // False positive indicators (subtract from confidence)
    private val falsePositiveIndicators = mapOf(
        "just kidding" to -0.35f,
        "joking" to -0.30f,
        "joke" to -0.25f,
        "test" to -0.25f,
        "testing" to -0.25f,
        "practice" to -0.20f,
        "practicing" to -0.20f,
        "pretend" to -0.25f,
        "pretending" to -0.25f,
        "acting" to -0.25f,
        "movie" to -0.20f,
        "game" to -0.20f,
        "story" to -0.18f,
        "storytelling" to -0.20f,
        "nevermind" to -0.30f,
        "never mind" to -0.30f,
        "cancel" to -0.25f,
        "stop" to -0.15f,
        "don't" to -0.12f,
        "dont" to -0.12f,
        "not really" to -0.25f,
        "false alarm" to -0.35f,
        "accident" to -0.18f,
        "accidentally" to -0.20f,
        "oops" to -0.18f,
        "sorry" to -0.10f,
        "wrong" to -0.12f
    )

    // Repetition patterns indicating genuine distress
    private val repetitionPatterns = mapOf(
        "help help" to 0.12f,
        "please please" to 0.10f,
        "no no no" to 0.08f,
        "stop stop" to 0.08f
    )

    // ==================== ANALYSIS LOGIC ====================

    override suspend fun analyzeEmergencyIntent(input: AnalysisInput): EmergencyResult {
        val text = input.transcribedText
        val lowerText = text.lowercase(Locale.getDefault())
        
        var confidence = 0f
        val emotionIndicators = mutableListOf<String>()
        val reasons = mutableListOf<String>()

        // Step 1: Check for trigger phrase (required)
        var hasTriggerPhrase = false
        for ((phrase, weight) in triggerPhrases) {
            if (lowerText.contains(phrase)) {
                confidence += weight
                hasTriggerPhrase = true
                reasons.add("Trigger phrase detected: '$phrase'")
                break
            }
        }

        if (!hasTriggerPhrase) {
            return EmergencyResult(
                isLegit = false,
                confidence = 0f,
                reason = "No trigger phrase detected",
                emotionIndicators = emptyList(),
                urgencyLevel = UrgencyLevel.LOW,
                source = AnalysisSource.ON_DEVICE
            )
        }

        // Step 2: Analyze urgency indicators
        for ((word, weight) in urgencyIndicators) {
            if (lowerText.contains(word)) {
                confidence += weight
                emotionIndicators.add("urgency:$word")
            }
        }

        // Step 3: Analyze distress indicators
        for ((word, weight) in distressIndicators) {
            if (lowerText.contains(word)) {
                confidence += weight
                emotionIndicators.add("distress:$word")
            }
        }

        // Step 4: Check critical indicators
        var hasCritical = false
        for ((word, weight) in criticalIndicators) {
            if (lowerText.contains(word)) {
                confidence += weight
                emotionIndicators.add("critical:$word")
                hasCritical = true
            }
        }

        // Step 5: Check for false positive indicators
        for ((phrase, weight) in falsePositiveIndicators) {
            if (lowerText.contains(phrase)) {
                confidence += weight  // weight is negative
                emotionIndicators.add("false_positive:$phrase")
                reasons.add("False positive indicator: '$phrase'")
            }
        }

        // Step 6: Check repetition patterns
        for ((pattern, weight) in repetitionPatterns) {
            if (lowerText.contains(pattern)) {
                confidence += weight
                emotionIndicators.add("repetition:$pattern")
            }
        }

        // Step 7: Analyze exclamation marks (indicates urgency/panic)
        val exclamationCount = text.count { it == '!' }
        if (exclamationCount > 0) {
            val exclamationBonus = minOf(exclamationCount * 0.03f, 0.12f)
            confidence += exclamationBonus
            emotionIndicators.add("exclamation:$exclamationCount")
        }

        // Step 8: Incorporate audio tone indicators if available
        input.audioToneIndicators?.let { audio ->
            when (audio.detectedEmotion.lowercase()) {
                "fear", "panic" -> {
                    confidence += 0.15f
                    emotionIndicators.add("audio:${audio.detectedEmotion}")
                }
                "stress", "anxious" -> {
                    confidence += 0.10f
                    emotionIndicators.add("audio:${audio.detectedEmotion}")
                }
                "calm" -> {
                    confidence -= 0.05f
                    emotionIndicators.add("audio:calm")
                }
            }

            if (audio.speechRate == SpeechRate.VERY_FAST) {
                confidence += 0.08f
                emotionIndicators.add("speech:very_fast")
            }

            if (audio.hasBreathiness || audio.hasTrembling) {
                confidence += 0.10f
                emotionIndicators.add("voice:distressed")
            }

            confidence += audio.voiceStressLevel * 0.15f
        }

        // Step 9: Context analysis
        input.previousContext?.let { context ->
            val lowerContext = context.lowercase()
            // Check if previous context suggests it's not real
            if (lowerContext.contains("let's pretend") || 
                lowerContext.contains("in the movie") ||
                lowerContext.contains("in the story")) {
                confidence -= 0.20f
                reasons.add("Context suggests non-emergency")
            }
        }

        // Clamp confidence to valid range
        confidence = confidence.coerceIn(0f, 1f)

        // Determine urgency level
        val urgencyLevel = when {
            hasCritical || confidence >= 0.85f -> UrgencyLevel.CRITICAL
            confidence >= 0.70f -> UrgencyLevel.HIGH
            confidence >= 0.50f -> UrgencyLevel.MEDIUM
            confidence >= 0.30f -> UrgencyLevel.LOW
            else -> UrgencyLevel.UNKNOWN
        }

        // Build final reason
        val finalReason = buildString {
            if (confidence >= 0.75f) {
                append("High confidence emergency detected. ")
            } else if (confidence >= 0.50f) {
                append("Possible emergency detected. ")
            } else if (confidence >= 0.30f) {
                append("Low confidence - may be false positive. ")
            } else {
                append("Likely not an emergency. ")
            }
            
            if (emotionIndicators.any { it.startsWith("distress:") || it.startsWith("critical:") }) {
                append("Distress indicators present. ")
            }
            if (emotionIndicators.any { it.startsWith("audio:") }) {
                append("Voice analysis indicates stress. ")
            }
            if (emotionIndicators.any { it.startsWith("false_positive:") }) {
                append("False positive signals detected. ")
            }
        }

        Log.d(TAG, "Analysis complete: confidence=$confidence, urgency=$urgencyLevel")
        Log.d(TAG, "Indicators: $emotionIndicators")

        return EmergencyResult(
            isLegit = confidence >= ClassificationThresholds.CONFIRMATION_REQUIRED,
            confidence = confidence,
            reason = finalReason.trim(),
            emotionIndicators = emotionIndicators,
            urgencyLevel = urgencyLevel,
            source = AnalysisSource.ON_DEVICE
        )
    }

    override fun isAvailable(): Boolean = true  // Always available

    override fun getSource(): AnalysisSource = AnalysisSource.ON_DEVICE
}
