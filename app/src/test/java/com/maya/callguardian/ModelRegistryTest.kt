package com.maya.callguardian

import com.maya.callguardian.ai.model_registry.ModelRegistry
import com.maya.callguardian.domain.models.ModelStatus
import com.maya.callguardian.domain.models.VoiceAuthenticityStatus
import com.maya.callguardian.intelligence.voice_authenticity.DefaultVoiceAuthenticityEngine
import org.junit.Assert.*
import org.junit.Test

class ModelRegistryTest {

    @Test
    fun testModelRegistryReportsHonestStatus() {
        val registry = ModelRegistry()
        val models = registry.getRegisteredModels()

        assertTrue(models.isNotEmpty())
        val gemma = models.find { it.id == "gemma_2b" }
        assertNotNull(gemma)
        assertEquals(ModelStatus.FALLBACK_ACTIVE, gemma!!.status)
        assertTrue(gemma.fallbackStrategy.contains("Heuristic"))

        val voice = models.find { it.id == "edge_spoof" }
        assertNotNull(voice)
        assertEquals(ModelStatus.NOT_INSTALLED, voice!!.status)
    }

    @Test
    fun testVoiceAuthenticityDoesNotFabricateRandomScores() {
        val engine = DefaultVoiceAuthenticityEngine()
        val result = engine.analyzeAudioChunk(ByteArray(100), 16000)

        // Must report UNAVAILABLE and not invent fake deepfake percentages
        assertEquals(VoiceAuthenticityStatus.UNAVAILABLE, result.status)
        assertEquals(0.0f, result.syntheticVoiceIndicatorScore, 0.001f)
        assertFalse(result.isModelAvailable)
        assertTrue(result.supportingEvidence.contains("unavailable", ignoreCase = true))
    }
}
