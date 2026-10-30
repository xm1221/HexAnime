package cn.xm1221.hexanime.client

import cn.xm1221.hexanime.casting.MishapFlavor
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class MishapScreenEffectPolicyTest {
    @Test
    fun recoilNeverGetsAnExtraScreenLayer() {
        assertFalse(MishapScreenEffectPolicy.supports(MishapFlavor.RECOIL))
        assertFalse(
            MishapScreenEffectPolicy.shouldTrigger(
                MishapFlavor.RECOIL,
                force = true,
                millisSinceLast = Long.MAX_VALUE,
            ),
        )
    }

    @Test
    fun realGameplayTriggersRespectTheCooldown() {
        assertTrue(
            MishapScreenEffectPolicy.shouldTrigger(
                MishapFlavor.SPATIAL,
                force = false,
                millisSinceLast = MishapScreenEffectPolicy.COOLDOWN_MILLIS,
            ),
        )
        assertFalse(
            MishapScreenEffectPolicy.shouldTrigger(
                MishapFlavor.MIND,
                force = false,
                millisSinceLast = MishapScreenEffectPolicy.COOLDOWN_MILLIS - 1,
            ),
        )
    }

    @Test
    fun administratorProbeBypassesTheCooldown() {
        assertTrue(
            MishapScreenEffectPolicy.shouldTrigger(
                MishapFlavor.MEDIA,
                force = true,
                millisSinceLast = 0,
            ),
        )
    }

    @Test
    fun screenEnvelopeFadesInAndOut() {
        assertEquals(0f, MishapScreenEffectPolicy.envelope(0f))
        assertEquals(0f, MishapScreenEffectPolicy.envelope(1f))
        assertTrue(MishapScreenEffectPolicy.envelope(0.5f) > 0.9f)
    }
}
