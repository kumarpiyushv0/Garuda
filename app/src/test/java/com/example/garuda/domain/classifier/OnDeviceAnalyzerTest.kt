package com.example.garuda.domain.classifier

import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

/**
 * Unit tests for [OnDeviceAnalyzer].
 * Tests keyword-based emergency classification logic.
 */
class OnDeviceAnalyzerTest {

    private lateinit var analyzer: OnDeviceAnalyzer

    @Before
    fun setup() {
        analyzer = OnDeviceAnalyzer()
    }

    // ==================== Trigger Phrase Detection ====================

    @Test
    fun `no trigger phrase returns zero confidence`() = runTest {
        val result = analyzer.analyzeEmergencyIntent("hello world")

        assertFalse(result.isLegit)
        assertEquals(0f, result.confidence, 0.001f)
        assertEquals(UrgencyLevel.LOW, result.urgencyLevel)
        assertEquals(AnalysisSource.ON_DEVICE, result.source)
    }

    @Test
    fun `garuda help triggers detection`() = runTest {
        val result = analyzer.analyzeEmergencyIntent("garuda help")

        assertTrue(result.confidence > 0f)
        assertEquals(AnalysisSource.ON_DEVICE, result.source)
    }

    @Test
    fun `hey garuda triggers detection`() = runTest {
        val result = analyzer.analyzeEmergencyIntent("hey garuda please help me")

        assertTrue(result.confidence > 0f)
    }

    @Test
    fun `garuda emergency triggers detection with higher base weight`() = runTest {
        val result = analyzer.analyzeEmergencyIntent("garuda emergency")

        assertTrue(result.confidence >= 0.30f)
    }

    // ==================== Urgency Indicators ====================

    @Test
    fun `urgency words boost confidence`() = runTest {
        val withoutUrgency = analyzer.analyzeEmergencyIntent("garuda help")
        val withUrgency = analyzer.analyzeEmergencyIntent("garuda help now please hurry")

        assertTrue(withUrgency.confidence > withoutUrgency.confidence)
    }

    // ==================== Distress Indicators ====================

    @Test
    fun `distress words boost confidence`() = runTest {
        val withoutDistress = analyzer.analyzeEmergencyIntent("garuda help")
        val withDistress = analyzer.analyzeEmergencyIntent("garuda help I am scared someone is following me")

        assertTrue(withDistress.confidence > withoutDistress.confidence)
        assertTrue(withDistress.emotionIndicators.any { it.startsWith("distress:") })
    }

    // ==================== Critical Indicators ====================

    @Test
    fun `critical words result in CRITICAL urgency level`() = runTest {
        val result = analyzer.analyzeEmergencyIntent("garuda help someone has a knife and is attacking me")

        assertEquals(UrgencyLevel.CRITICAL, result.urgencyLevel)
        assertTrue(result.confidence >= 0.75f)
        assertTrue(result.isLegit)
    }

    @Test
    fun `critical words significantly boost confidence`() = runTest {
        val withoutCritical = analyzer.analyzeEmergencyIntent("garuda help")
        val withCritical = analyzer.analyzeEmergencyIntent("garuda help someone is trying to kidnap me")

        assertTrue(withCritical.confidence > withoutCritical.confidence + 0.2f)
    }

    // ==================== False Positive Indicators ====================

    @Test
    fun `false positive indicators reduce confidence`() = runTest {
        val genuine = analyzer.analyzeEmergencyIntent("garuda help I am in danger")
        val falsePositive = analyzer.analyzeEmergencyIntent("garuda help just kidding I am in danger")

        assertTrue(falsePositive.confidence < genuine.confidence)
    }

    @Test
    fun `testing keyword reduces confidence`() = runTest {
        val result = analyzer.analyzeEmergencyIntent("garuda help testing the app")

        assertTrue(result.emotionIndicators.any { it.startsWith("false_positive:") })
    }

    @Test
    fun `false alarm returns very low confidence`() = runTest {
        val result = analyzer.analyzeEmergencyIntent("garuda help false alarm sorry")

        assertTrue(result.confidence < 0.3f)
    }

    // ==================== Repetition Patterns ====================

    @Test
    fun `repetition patterns boost confidence`() = runTest {
        val withoutRepetition = analyzer.analyzeEmergencyIntent("garuda help")
        val withRepetition = analyzer.analyzeEmergencyIntent("garuda help help help please please")

        assertTrue(withRepetition.confidence > withoutRepetition.confidence)
    }

    // ==================== Confidence Clamping ====================

    @Test
    fun `confidence is clamped between 0 and 1`() = runTest {
        // Very high confidence scenario
        val highResult = analyzer.analyzeEmergencyIntent(
            "garuda emergency help help please please now hurry immediately " +
            "scared terrified danger attacking stalking weapon gun knife kill die"
        )
        assertTrue(highResult.confidence <= 1f)

        // Very low confidence scenario (trigger + many false positives)
        val lowResult = analyzer.analyzeEmergencyIntent(
            "garuda help just kidding joking test practice pretend false alarm nevermind cancel"
        )
        assertTrue(lowResult.confidence >= 0f)
    }

    // ==================== Urgency Level Classification ====================

    @Test
    fun `high confidence without critical words returns HIGH urgency`() = runTest {
        val result = analyzer.analyzeEmergencyIntent(
            "garuda help now please hurry I am scared someone is following me in the dark"
        )

        assertTrue(result.confidence >= 0.50f)
        assertTrue(
            result.urgencyLevel == UrgencyLevel.HIGH ||
            result.urgencyLevel == UrgencyLevel.CRITICAL
        )
    }

    @Test
    fun `low confidence returns LOW or UNKNOWN urgency`() = runTest {
        val result = analyzer.analyzeEmergencyIntent("garuda help oops sorry wrong")

        assertTrue(
            result.urgencyLevel == UrgencyLevel.LOW ||
            result.urgencyLevel == UrgencyLevel.UNKNOWN
        )
    }

    // ==================== Exclamation Mark Handling ====================

    @Test
    fun `exclamation marks boost confidence`() = runTest {
        val without = analyzer.analyzeEmergencyIntent("garuda help")
        val with = analyzer.analyzeEmergencyIntent("garuda help!!!")

        assertTrue(with.confidence > without.confidence)
    }

    // ==================== Case Insensitivity ====================

    @Test
    fun `trigger phrases are case insensitive`() = runTest {
        val lower = analyzer.analyzeEmergencyIntent("garuda help")
        val upper = analyzer.analyzeEmergencyIntent("GARUDA HELP")
        val mixed = analyzer.analyzeEmergencyIntent("Garuda Help")

        assertEquals(lower.confidence, upper.confidence, 0.001f)
        assertEquals(lower.confidence, mixed.confidence, 0.001f)
    }

    // ==================== AnalysisInput Context ====================

    @Test
    fun `previous context suggesting pretend reduces confidence`() = runTest {
        val withoutContext = analyzer.analyzeEmergencyIntent(
            AnalysisInput(transcribedText = "garuda help I am scared")
        )
        val withPretendContext = analyzer.analyzeEmergencyIntent(
            AnalysisInput(
                transcribedText = "garuda help I am scared",
                previousContext = "let's pretend we're in a movie"
            )
        )

        assertTrue(withPretendContext.confidence < withoutContext.confidence)
    }

    // ==================== isLegit Threshold ====================

    @Test
    fun `isLegit true when confidence above CONFIRMATION_REQUIRED threshold`() = runTest {
        val result = analyzer.analyzeEmergencyIntent(
            "garuda help now please hurry I am scared someone is attacking me"
        )

        assertTrue(result.confidence >= ClassificationThresholds.CONFIRMATION_REQUIRED)
        assertTrue(result.isLegit)
    }

    @Test
    fun `isLegit false when confidence below CONFIRMATION_REQUIRED threshold`() = runTest {
        val result = analyzer.analyzeEmergencyIntent("garuda help just kidding test")

        assertFalse(result.isLegit)
    }
}
