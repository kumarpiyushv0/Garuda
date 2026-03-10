package com.example.garuda.domain.classifier

import android.util.Log
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

import javax.inject.Named

/**
 * Hybrid Emergency Intent Analyzer
 * Combines on-device and cloud analysis for optimal accuracy
 * 
 * Strategy:
 * 1. Always run on-device analysis first (instant, works offline)
 * 2. If on-device is highly confident (>=0.75 or <=0.25), use that result
 * 3. If uncertain (0.25-0.75), try cloud validation if available
 * 4. Cloud result takes precedence when available and on-device is uncertain
 */
@Singleton
class HybridAnalyzer @Inject constructor(
    @Named("on_device") private val onDeviceAnalyzer: EmergencyIntentAnalyzer,
    @Named("cloud") private val geminiAnalyzer: EmergencyIntentAnalyzer
) : EmergencyIntentAnalyzer {

    companion object {
        private const val TAG = "HybridAnalyzer"
        
        // Thresholds for deciding when to use cloud
        private const val HIGH_CONFIDENCE_THRESHOLD = 0.75f
        private const val LOW_CONFIDENCE_THRESHOLD = 0.25f
    }

    override suspend fun analyzeEmergencyIntent(input: AnalysisInput): EmergencyResult {
        // Step 1: Always run on-device first
        val onDeviceResult = onDeviceAnalyzer.analyzeEmergencyIntent(input)
        Log.d(TAG, "On-device result: confidence=${onDeviceResult.confidence}, isLegit=${onDeviceResult.isLegit}")

        // Step 2: Check if on-device is confident enough
        if (onDeviceResult.confidence >= HIGH_CONFIDENCE_THRESHOLD) {
            Log.d(TAG, "High confidence on-device, using result directly")
            return onDeviceResult.copy(source = AnalysisSource.HYBRID)
        }

        if (onDeviceResult.confidence <= LOW_CONFIDENCE_THRESHOLD) {
            Log.d(TAG, "Low confidence on-device, likely false positive")
            return onDeviceResult.copy(source = AnalysisSource.HYBRID)
        }

        // Step 3: Uncertain range - try cloud validation
        if (!geminiAnalyzer.isAvailable()) {
            Log.d(TAG, "Cloud not available, using on-device result")
            return onDeviceResult.copy(
                source = AnalysisSource.HYBRID,
                reason = "${onDeviceResult.reason} (Cloud validation unavailable)"
            )
        }

        // Step 4: Get cloud result
        val cloudResult = geminiAnalyzer.analyzeEmergencyIntent(input)
        
        // If cloud failed, fall back to on-device
        if (cloudResult.confidence == 0f && cloudResult.reason.contains("unavailable")) {
            Log.d(TAG, "Cloud analysis failed, using on-device")
            return onDeviceResult.copy(source = AnalysisSource.HYBRID)
        }

        Log.d(TAG, "Cloud result: confidence=${cloudResult.confidence}, isLegit=${cloudResult.isLegit}")

        // Step 5: Combine results
        return combineResults(onDeviceResult, cloudResult)
    }

    private fun combineResults(
        onDevice: EmergencyResult,
        cloud: EmergencyResult
    ): EmergencyResult {
        // Weight cloud result more heavily when available (60/40 split)
        val combinedConfidence = (onDevice.confidence * 0.4f) + (cloud.confidence * 0.6f)
        
        // Combine emotion indicators
        val combinedIndicators = (onDevice.emotionIndicators + cloud.emotionIndicators).distinct()
        
        // Use higher urgency level (lower ordinal = higher urgency)
        val combinedUrgency = if (onDevice.urgencyLevel.ordinal <= cloud.urgencyLevel.ordinal) {
            onDevice.urgencyLevel
        } else {
            cloud.urgencyLevel
        }
        
        // Combine reasons
        val combinedReason = buildString {
            append("Hybrid analysis: ")
            append("On-device (${String.format(Locale.getDefault(), "%.0f", onDevice.confidence * 100)}%): ${onDevice.reason} | ")
            append("Cloud (${String.format(Locale.getDefault(), "%.0f", cloud.confidence * 100)}%): ${cloud.reason}")
        }

        return EmergencyResult(
            isLegit = combinedConfidence >= ClassificationThresholds.CONFIRMATION_REQUIRED,
            confidence = combinedConfidence,
            reason = combinedReason,
            emotionIndicators = combinedIndicators,
            urgencyLevel = combinedUrgency,
            source = AnalysisSource.HYBRID
        )
    }

    override fun isAvailable(): Boolean = true  // On-device is always available

    override fun getSource(): AnalysisSource = AnalysisSource.HYBRID
}
