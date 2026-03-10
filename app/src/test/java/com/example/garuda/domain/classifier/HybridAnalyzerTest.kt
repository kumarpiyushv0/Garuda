package com.example.garuda.domain.classifier

import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

/**
 * Unit tests for [HybridAnalyzer].
 * Tests the orchestration logic that decides when to use cloud vs on-device analysis.
 */
class HybridAnalyzerTest {

    private lateinit var onDeviceAnalyzer: OnDeviceAnalyzer
    private lateinit var geminiAnalyzer: GeminiAnalyzer
    private lateinit var hybridAnalyzer: HybridAnalyzer

    @Before
    fun setup() {
        onDeviceAnalyzer = mockk()
        geminiAnalyzer = mockk()
        hybridAnalyzer = HybridAnalyzer(onDeviceAnalyzer, geminiAnalyzer)
    }

    private fun createResult(
        confidence: Float,
        isLegit: Boolean = confidence >= 0.50f,
        source: AnalysisSource = AnalysisSource.ON_DEVICE,
        urgencyLevel: UrgencyLevel = UrgencyLevel.UNKNOWN,
        reason: String = "test reason"
    ) = EmergencyResult(
        isLegit = isLegit,
        confidence = confidence,
        reason = reason,
        emotionIndicators = emptyList(),
        urgencyLevel = urgencyLevel,
        source = source
    )

    private val testInput = AnalysisInput(transcribedText = "garuda help")

    // ==================== High Confidence On-Device → Skip Cloud ====================

    @Test
    fun `high on-device confidence skips cloud and returns HYBRID source`() = runTest {
        coEvery { onDeviceAnalyzer.analyzeEmergencyIntent(testInput) } returns
            createResult(confidence = 0.85f, isLegit = true)

        val result = hybridAnalyzer.analyzeEmergencyIntent(testInput)

        assertEquals(AnalysisSource.HYBRID, result.source)
        assertEquals(0.85f, result.confidence, 0.001f)
        assertTrue(result.isLegit)
    }

    @Test
    fun `exactly 0_75 confidence skips cloud`() = runTest {
        coEvery { onDeviceAnalyzer.analyzeEmergencyIntent(testInput) } returns
            createResult(confidence = 0.75f, isLegit = true)

        val result = hybridAnalyzer.analyzeEmergencyIntent(testInput)

        assertEquals(0.75f, result.confidence, 0.001f)
        assertEquals(AnalysisSource.HYBRID, result.source)
    }

    // ==================== Low Confidence On-Device → Skip Cloud ====================

    @Test
    fun `low on-device confidence skips cloud and returns HYBRID source`() = runTest {
        coEvery { onDeviceAnalyzer.analyzeEmergencyIntent(testInput) } returns
            createResult(confidence = 0.15f, isLegit = false)

        val result = hybridAnalyzer.analyzeEmergencyIntent(testInput)

        assertEquals(AnalysisSource.HYBRID, result.source)
        assertEquals(0.15f, result.confidence, 0.001f)
        assertFalse(result.isLegit)
    }

    @Test
    fun `exactly 0_25 confidence skips cloud`() = runTest {
        coEvery { onDeviceAnalyzer.analyzeEmergencyIntent(testInput) } returns
            createResult(confidence = 0.25f, isLegit = false)

        val result = hybridAnalyzer.analyzeEmergencyIntent(testInput)

        assertEquals(0.25f, result.confidence, 0.001f)
        assertEquals(AnalysisSource.HYBRID, result.source)
    }

    // ==================== Uncertain Range → Cloud Validation ====================

    @Test
    fun `uncertain on-device and cloud available combines results`() = runTest {
        coEvery { onDeviceAnalyzer.analyzeEmergencyIntent(testInput) } returns
            createResult(confidence = 0.50f)
        every { geminiAnalyzer.isAvailable() } returns true
        coEvery { geminiAnalyzer.analyzeEmergencyIntent(testInput) } returns
            createResult(confidence = 0.80f, source = AnalysisSource.CLOUD_GEMINI)

        val result = hybridAnalyzer.analyzeEmergencyIntent(testInput)

        assertEquals(AnalysisSource.HYBRID, result.source)
        // Combined: 0.50 * 0.4 + 0.80 * 0.6 = 0.20 + 0.48 = 0.68
        assertEquals(0.68f, result.confidence, 0.01f)
    }

    @Test
    fun `uncertain on-device and cloud unavailable falls back to on-device`() = runTest {
        coEvery { onDeviceAnalyzer.analyzeEmergencyIntent(testInput) } returns
            createResult(confidence = 0.50f, reason = "Possible emergency")
        every { geminiAnalyzer.isAvailable() } returns false

        val result = hybridAnalyzer.analyzeEmergencyIntent(testInput)

        assertEquals(AnalysisSource.HYBRID, result.source)
        assertEquals(0.50f, result.confidence, 0.001f)
        assertTrue(result.reason.contains("Cloud validation unavailable"))
    }

    // ==================== Cloud Failure Fallback ====================

    @Test
    fun `cloud failure falls back to on-device result`() = runTest {
        coEvery { onDeviceAnalyzer.analyzeEmergencyIntent(testInput) } returns
            createResult(confidence = 0.50f)
        every { geminiAnalyzer.isAvailable() } returns true
        coEvery { geminiAnalyzer.analyzeEmergencyIntent(testInput) } returns
            createResult(confidence = 0f, reason = "Network unavailable", source = AnalysisSource.CLOUD_GEMINI)

        val result = hybridAnalyzer.analyzeEmergencyIntent(testInput)

        assertEquals(AnalysisSource.HYBRID, result.source)
        assertEquals(0.50f, result.confidence, 0.001f)
    }

    // ==================== Result Combination Logic ====================

    @Test
    fun `combined result uses 40-60 weighting`() = runTest {
        coEvery { onDeviceAnalyzer.analyzeEmergencyIntent(testInput) } returns
            createResult(confidence = 0.40f)
        every { geminiAnalyzer.isAvailable() } returns true
        coEvery { geminiAnalyzer.analyzeEmergencyIntent(testInput) } returns
            createResult(confidence = 0.60f, source = AnalysisSource.CLOUD_GEMINI)

        val result = hybridAnalyzer.analyzeEmergencyIntent(testInput)

        // Combined: 0.40 * 0.4 + 0.60 * 0.6 = 0.16 + 0.36 = 0.52
        assertEquals(0.52f, result.confidence, 0.01f)
    }

    @Test
    fun `combined result uses higher urgency level`() = runTest {
        coEvery { onDeviceAnalyzer.analyzeEmergencyIntent(testInput) } returns
            createResult(confidence = 0.50f, urgencyLevel = UrgencyLevel.HIGH)
        every { geminiAnalyzer.isAvailable() } returns true
        coEvery { geminiAnalyzer.analyzeEmergencyIntent(testInput) } returns
            createResult(
                confidence = 0.60f,
                urgencyLevel = UrgencyLevel.LOW,
                source = AnalysisSource.CLOUD_GEMINI
            )

        val result = hybridAnalyzer.analyzeEmergencyIntent(testInput)

        // HIGH has lower ordinal than LOW, so HIGH should be selected
        assertEquals(UrgencyLevel.HIGH, result.urgencyLevel)
    }

    @Test
    fun `combined result merges emotion indicators from both analyzers`() = runTest {
        coEvery { onDeviceAnalyzer.analyzeEmergencyIntent(testInput) } returns
            EmergencyResult(
                isLegit = true,
                confidence = 0.50f,
                reason = "on-device",
                emotionIndicators = listOf("distress:scared"),
                urgencyLevel = UrgencyLevel.MEDIUM,
                source = AnalysisSource.ON_DEVICE
            )
        every { geminiAnalyzer.isAvailable() } returns true
        coEvery { geminiAnalyzer.analyzeEmergencyIntent(testInput) } returns
            EmergencyResult(
                isLegit = true,
                confidence = 0.70f,
                reason = "cloud",
                emotionIndicators = listOf("urgency:hurry"),
                urgencyLevel = UrgencyLevel.HIGH,
                source = AnalysisSource.CLOUD_GEMINI
            )

        val result = hybridAnalyzer.analyzeEmergencyIntent(testInput)

        assertTrue(result.emotionIndicators.contains("distress:scared"))
        assertTrue(result.emotionIndicators.contains("urgency:hurry"))
    }

    // ==================== isAvailable / getSource ====================

    @Test
    fun `hybrid analyzer is always available`() {
        assertTrue(hybridAnalyzer.isAvailable())
    }

    @Test
    fun `hybrid analyzer source is HYBRID`() {
        assertEquals(AnalysisSource.HYBRID, hybridAnalyzer.getSource())
    }
}
