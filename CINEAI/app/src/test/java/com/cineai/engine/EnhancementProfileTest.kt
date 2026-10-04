package com.cineai.engine

import org.junit.Assert.assertEquals
import org.junit.Test

class EnhancementProfileTest {
    @Test fun jsonRoundTripPreservesAllControls() {
        val original=EnhancementProfile(exposure=.2f,brightness=-.1f,contrast=.3f,highlights=-.2f,shadows=.4f,whites=-.3f,blacks=.1f,temperature=.2f,tint=-.2f,saturation=.1f,vibrance=.2f,sharpness=.3f,texture=.4f,noiseReduction=.5f,vignette=.6f,grain=.7f,cinematicIntensity=.8f)
        val restored=EnhancementProfile.fromJson(original.json())
        assertEquals(original,restored)
    }
}
