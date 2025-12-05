package com.example.garuda.domain.classifier

/**
 * Interface for Emergency Intent Analyzers
 * Implementations can be on-device, cloud-based, or hybrid
 */
interface EmergencyIntentAnalyzer {
    
    /**
     * Analyze the emergency intent of transcribed speech
     * @param input The analysis input containing text and optional context
     * @return EmergencyResult with classification details
     */
    suspend fun analyzeEmergencyIntent(input: AnalysisInput): EmergencyResult
    
    /**
     * Simplified version that takes just the text
     */
    suspend fun analyzeEmergencyIntent(text: String): EmergencyResult {
        return analyzeEmergencyIntent(AnalysisInput(transcribedText = text))
    }
    
    /**
     * Check if the analyzer is available (e.g., network for cloud)
     */
    fun isAvailable(): Boolean
    
    /**
     * Get the source type of this analyzer
     */
    fun getSource(): AnalysisSource
}

/**
 * Thresholds for classification decisions
 */
object ClassificationThresholds {
    const val IMMEDIATE_TRIGGER = 0.75f    // Trigger SOS immediately
    const val CONFIRMATION_REQUIRED = 0.50f // Show confirmation dialog
    const val CLOUD_VALIDATION = 0.40f      // Uncertain, validate with cloud
    const val IGNORE = 0.40f                // Below this, ignore
}
