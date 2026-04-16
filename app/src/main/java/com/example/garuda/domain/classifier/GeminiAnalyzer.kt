package com.example.garuda.domain.classifier

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.util.Log
import com.example.garuda.BuildConfig
import com.google.ai.client.generativeai.GenerativeModel
import com.google.ai.client.generativeai.type.generationConfig
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import org.json.JSONObject
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Cloud-based Emergency Intent Analyzer using Google Gemini API
 * Provides more accurate classification using LLM understanding
 */
@Singleton
class GeminiAnalyzer @Inject constructor(
    @ApplicationContext private val context: Context
) : EmergencyIntentAnalyzer {

    companion object {
        private const val TAG = "GeminiAnalyzer"
        private const val TIMEOUT_MS = 10000L  // 10 second timeout
    }

    private val generativeModel by lazy {
        try {
            GenerativeModel(
                modelName = "gemini-3.1-flash",  // Stable model for SDK 0.7.0
                apiKey = BuildConfig.GEMINI_API_KEY,
                generationConfig = generationConfig {
                    temperature = 0.1f
                    maxOutputTokens = 256
                }
            )
        } catch (e: Exception) {
            Log.e(TAG, "Failed to initialize GenerativeModel", e)
            null
        }
    }

    private val systemPrompt = """
You are an emergency intent classifier for a women's safety app called "Garuda".
Your #1 ABSOLUTE PRIORITY is user safety. It is FAR better to trigger a false alarm than to miss a real emergency.
ALWAYS assume the person could be in genuine danger unless there is overwhelming proof otherwise.

Analyze for (even vague, partial, or indirect mentions count — be very generous):
1. Urgency indicators (please, now, hurry, help, quick, fast, come, somebody, anyone, SOS, emergency, 911, police)
2. Fear/panic indicators (scared, afraid, danger, worried, nervous, uncomfortable, uneasy, alone, crying, shaking, panicking, don't know what to do)
3. Threat indicators (following, attacking, stalking, chasing, creepy, suspicious, stranger, someone, man, guy, watching, looking, won't leave, blocking)
4. Critical indicators (rape, kidnap, weapon, gun, knife, hit, hurt, kill, assault, grab, touch, force, trap, locked, dragging)
5. Distress context (late at night, dark, alone, lost, stranded, no one around, cab, taxi, ride, alley, parking, deserted, unfamiliar place)
6. Implicit distress (whispering, speaking very quietly, rushed speech, heavy breathing, crying, trembling voice, background yelling or arguing)

FALSE POSITIVE: ONLY mark as false positive if the speaker UNMISTAKABLY and EXPLICITLY says one of these EXACT phrases: "just joking", "testing the app", "this is a test", "I'm pretending", "false alarm nevermind". Casual or ambiguous speech should NEVER be marked as false positive.

CRITICAL RULES:
- Even a SINGLE distress-related word should give at least 0.50 confidence
- Incomplete, garbled, whispered, or broken sentences likely mean real panic — give HIGH confidence
- Cultural and linguistic variations in expressing fear must be respected — be maximally generous
- If there is even a 20% chance the person might be in danger, classify as emergency
- Silence or very short phrases after activation could mean the person can't speak freely — treat with HIGH urgency
- Swearing or emotional outbursts combined with any context clue = emergency
- When uncertain, ALWAYS round confidence UP, not down

Return ONLY valid JSON (no markdown):
{"isLegit": true, "confidence": 0.85, "reason": "Brief explanation", "emotionIndicators": ["fear", "urgency"], "urgencyLevel": "HIGH"}

Confidence: >=0.40 = emergency, 0.20-0.39 = needs confirmation, <0.20 = likely false positive
""".trimIndent()

    override suspend fun analyzeEmergencyIntent(input: AnalysisInput): EmergencyResult {
        Log.d(TAG, "Starting Gemini analysis for: ${input.transcribedText}")
        
        if (!isAvailable()) {
            Log.w(TAG, "Network not available")
            return createFallbackResult("Network unavailable")
        }

        val model = generativeModel
        if (model == null) {
            Log.e(TAG, "GenerativeModel is null")
            return createFallbackResult("Model initialization failed")
        }

        return withContext(Dispatchers.IO) {
            try {
                val prompt = buildPrompt(input)
                Log.d(TAG, "Sending prompt to Gemini...")

                // Add timeout
                val result = withTimeoutOrNull(TIMEOUT_MS) {
                    val response = model.generateContent(prompt)
                    response.text
                }

                if (result == null) {
                    Log.w(TAG, "Gemini request timed out")
                    return@withContext createFallbackResult("Request timed out")
                }

                Log.d(TAG, "Gemini response: $result")
                parseGeminiResponse(result)
                
            } catch (e: Exception) {
                Log.e(TAG, "Gemini analysis failed: ${e.message}", e)
                createFallbackResult("Error: ${e.message}")
            }
        }
    }

    private fun buildPrompt(input: AnalysisInput): String {
        return buildString {
            append(systemPrompt)
            append("\n\n---\n\n")
            append("Analyze this input:\n\n")
            append("Transcribed text: \"${input.transcribedText}\"\n")
            
            input.previousContext?.let {
                append("Previous context: \"$it\"\n")
            }
            
            input.audioToneIndicators?.let { audio ->
                append("\nAudio analysis:\n")
                append("- Detected emotion: ${audio.detectedEmotion}\n")
                append("- Voice stress level: ${audio.voiceStressLevel}\n")
                append("- Speech rate: ${audio.speechRate}\n")
                append("- Has breathiness: ${audio.hasBreathiness}\n")
                append("- Has trembling: ${audio.hasTrembling}\n")
            }
            
            append("\nRespond with JSON only:")
        }
    }

    private fun parseGeminiResponse(responseText: String): EmergencyResult {
        try {
            // Extract JSON from response (handle markdown code blocks)
            val jsonString = responseText
                .replace("```json", "")
                .replace("```", "")
                .trim()
            
            val json = JSONObject(jsonString)
            
            val emotionIndicators = mutableListOf<String>()
            json.optJSONArray("emotionIndicators")?.let { array ->
                for (i in 0 until array.length()) {
                    emotionIndicators.add(array.getString(i))
                }
            }

            val urgencyLevel = when (json.optString("urgencyLevel", "UNKNOWN").uppercase()) {
                "CRITICAL" -> UrgencyLevel.CRITICAL
                "HIGH" -> UrgencyLevel.HIGH
                "MEDIUM" -> UrgencyLevel.MEDIUM
                "LOW" -> UrgencyLevel.LOW
                else -> UrgencyLevel.UNKNOWN
            }

            return EmergencyResult(
                isLegit = json.getBoolean("isLegit"),
                confidence = json.getDouble("confidence").toFloat(),
                reason = json.getString("reason"),
                emotionIndicators = emotionIndicators,
                urgencyLevel = urgencyLevel,
                source = AnalysisSource.CLOUD_GEMINI
            )
        } catch (e: Exception) {
            Log.e(TAG, "Failed to parse Gemini response: $responseText", e)
            throw e
        }
    }

    private fun createFallbackResult(reason: String): EmergencyResult {
        return EmergencyResult(
            isLegit = false,
            confidence = 0f,
            reason = reason,
            emotionIndicators = emptyList(),
            urgencyLevel = UrgencyLevel.UNKNOWN,
            source = AnalysisSource.CLOUD_GEMINI
        )
    }

    override fun isAvailable(): Boolean {
        val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val network = connectivityManager.activeNetwork ?: return false
        val capabilities = connectivityManager.getNetworkCapabilities(network) ?: return false
        
        return capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
               capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
    }

    override fun getSource(): AnalysisSource = AnalysisSource.CLOUD_GEMINI
}
