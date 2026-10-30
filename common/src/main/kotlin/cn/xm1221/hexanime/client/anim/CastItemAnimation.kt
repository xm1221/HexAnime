package cn.xm1221.hexanime.client.anim

import cn.xm1221.hexanime.casting.item.CastItemProfile
import cn.xm1221.hexanime.casting.item.CastItemState
import cn.xm1221.hexanime.casting.item.CastItems
import cn.xm1221.hexanime.casting.item.CastPulseKind
import cn.xm1221.hexanime.casting.item.DamagePose
import cn.xm1221.hexanime.casting.item.PulseStyle
import cn.xm1221.hexanime.casting.item.StaffCastMotions
import cn.xm1221.hexanime.casting.item.StaffMaterial
import cn.xm1221.hexanime.casting.item.VariantMotion
import cn.xm1221.hexanime.casting.item.VariantMotions
import cn.xm1221.hexanime.config.HexAnimeClientConfig
import cn.xm1221.hexanime.staff.Staffs
import dev.kosmx.playerAnim.api.TransformType
import dev.kosmx.playerAnim.api.firstPerson.FirstPersonMode
import dev.kosmx.playerAnim.api.firstPerson.FirstPersonConfiguration
import dev.kosmx.playerAnim.api.layered.IAnimation
import dev.kosmx.playerAnim.api.layered.ModifierLayer
import dev.kosmx.playerAnim.core.util.Vec3f
import net.minecraft.client.player.AbstractClientPlayer
import net.minecraft.util.Mth
import net.minecraft.world.InteractionHand
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.sin

class CastItemAnimation(
    private val player: AbstractClientPlayer,
) : IAnimation {
    private val hands = InteractionHand.values().associateWith { HandState() }
    private var ageTicks = 0f

    fun setAim(
        active: Boolean,
        normX: Float,
        normY: Float,
        hand: InteractionHand,
        state: CastItemState?,
    ) {
        setAim(active, normX, normY, hand, state?.profile, state?.variant ?: 0, state?.strain ?: 0f)
    }

    fun setAim(
        active: Boolean,
        normX: Float,
        normY: Float,
        hand: InteractionHand,
        profile: CastItemProfile?,
        variant: Int,
        strain: Float,
        remote: Boolean = false,
    ) {
        val state = hands.getValue(hand)
        if (profile != null) {
            state.profile = profile
            state.variant = variant.coerceAtLeast(0)
            state.strain = strain.coerceIn(0f, 1f)
        }
        state.activeAim = active && profile?.supportsAim == true
        state.remoteAim = remote
        if (state.activeAim && remote) state.lastRemoteAimTick = ageTicks
        if (state.activeAim) {
            state.targetX = if (normX.isFinite()) normX.coerceIn(-1.2f, 1.2f) else 0f
            state.targetY = if (normY.isFinite()) normY.coerceIn(-1.2f, 1.2f) else 0f
        }
    }

    fun triggerPulse(
        hand: InteractionHand,
        profile: CastItemProfile?,
        variant: Int,
        strain: Float,
        kind: CastPulseKind,
    ) {
        if (profile == null) return
        if (kind == CastPulseKind.CAST && !profile.supportsPulse) return
        val state = hands.getValue(hand)
        if (isPulseActive(state) && kind.priority < state.pulseKind.priority) return
        if (kind == CastPulseKind.BREAK && !profile.pulseStyle.allowBreak) return
        if (kind == CastPulseKind.CAST &&
            profile.staffMaterial != StaffMaterial.NONE &&
            isPulseActive(state) &&
            state.pulseKind == CastPulseKind.CAST
        ) {
            return
        }
        triggerPulseInternal(state, profile, variant, strain, kind)
    }

    /** Four-tick local dip when Hex accepts a drawn pattern. Not a networked flourish. */
    fun triggerMicroPulse(hand: InteractionHand) {
        hands.getValue(hand).microStartTick = ageTicks
    }

    /**
     * Backup finish when the stack clears without an in-progress cast flourish.
     * Uses the raw aim target, not the displayed spring.
     */
    fun triggerLocalStaffCast(hand: InteractionHand, profile: CastItemProfile, strain: Float) {
        if (profile.staffMaterial == StaffMaterial.NONE || !profile.supportsPulse) return
        val state = hands.getValue(hand)
        val variant = StaffCastMotions.variantForCursor(state.targetX, state.targetY)
        triggerPulse(hand, profile, variant, strain, CastPulseKind.CAST)
    }

    override fun isActive(): Boolean {
        if (!HexAnimeClientConfig.config.enabled) return false
        return hands.values.any { state ->
            state.activeAim ||
                state.weight > 0.01f ||
                isPulseActive(state) ||
                microActive(state) ||
                hasDamageMotion(state)
        }
    }

    override fun tick() {
        ageTicks += 1f
        observeHeldItems()
        for (state in hands.values) {
            if (state.remoteAim && state.activeAim &&
                ageTicks - state.lastRemoteAimTick > REMOTE_AIM_TIMEOUT_TICKS
            ) {
                state.activeAim = false
            }
            state.prevX = state.shownX
            state.prevY = state.shownY
            state.prevWeight = state.weight

            val desiredWeight = if (state.activeAim) 1f else 0f
            state.weight = Mth.lerp(STAFF_FADE_PER_TICK, state.weight, desiredWeight)
            if (!state.activeAim && state.weight < 0.01f) state.weight = 0f
            if (state.activeAim) {
                state.shownX = Mth.lerp(STAFF_AIM_PER_TICK, state.shownX, state.targetX)
                state.shownY = Mth.lerp(STAFF_AIM_PER_TICK, state.shownY, state.targetY)
            }
        }
    }

    /** Called from the client tick as well, so low-media feedback can activate an idle layer. */
    fun observeHeldItems() {
        for ((hand, state) in hands) {
            refreshHeldItem(hand, state)
        }
    }

    override fun setupAnim(tickDelta: Float) {}

    fun frame(hand: InteractionHand, tickDelta: Float): CastHandFrame {
        val config = HexAnimeClientConfig.config
        if (!config.enabled) return CastHandFrame.EMPTY

        val state = hands.getValue(hand)
        val profile = state.profile ?: return CastHandFrame.EMPTY
        val x = Mth.lerp(tickDelta, state.prevX, state.shownX)
        val y = Mth.lerp(tickDelta, state.prevY, state.shownY)
        val weight = Mth.lerp(tickDelta, state.prevWeight, state.weight)
        val aimYaw = if (profile.supportsAim) -x * STAFF_YAW_SPREAD else 0f
        val aimPitch = if (profile.supportsAim) y * STAFF_PITCH_SPREAD else 0f
        val phase = ageTicks + tickDelta
        val damage = if (config.damageAnimations) damageState(profile.damage, state.strain) else 0f
        val tremor = damageTremor(profile.damage, damage, phase, state.variant)
        val pulse = pulseMotion(state, phase)
        val micro = microPitch(state, phase)

        return CastHandFrame(
            profile = profile,
            motion = pulse.motion,
            aimWeight = if (profile.supportsAim) weight else 0f,
            aimYaw = aimYaw,
            aimPitch = aimPitch,
            aimRoll = 0f,
            bodyYawDelta = pulse.bodyYaw + tremor.yaw * 0.22f,
            bodyPitchDelta = pulse.bodyPitch + damage * profile.damage.pitchDroop * 0.32f,
            bodyRollDelta = pulse.bodyRoll + tremor.roll * 0.18f,
            bodyXDelta = pulse.bodyX,
            bodyYDelta = pulse.bodyY,
            bodyZDelta = pulse.bodyZ,
            headYawDelta = pulse.headYaw - tremor.yaw * 0.32f,
            headPitchDelta = pulse.headPitch - tremor.pitch * 0.24f,
            headRollDelta = pulse.headRoll - tremor.roll * 0.42f,
            leftLegYawDelta = pulse.leftLegYaw,
            leftLegPitchDelta = pulse.leftLegPitch,
            leftLegRollDelta = pulse.leftLegRoll,
            rightLegYawDelta = pulse.rightLegYaw,
            rightLegPitchDelta = pulse.rightLegPitch,
            rightLegRollDelta = pulse.rightLegRoll,
            armYawDelta = pulse.armYaw + tremor.yaw,
            armPitchDelta = pulse.armPitch + tremor.pitch + damage * profile.damage.pitchDroop,
            armRollDelta = pulse.armRoll + tremor.roll,
            itemYawDelta = pulse.itemYaw + tremor.yaw * 0.82f,
            itemPitchDelta = pulse.itemPitch + tremor.pitch * 0.82f + micro,
            itemRollDelta = pulse.itemRoll + tremor.roll * 1.35f,
            itemXDelta = pulse.itemX,
            itemYDelta = pulse.itemY,
            itemZDelta = pulse.itemZ,
            offhandYawDelta = pulse.offhandYaw,
            offhandPitchDelta = pulse.offhandPitch,
            offhandRollDelta = pulse.offhandRoll,
            firstPersonYaw = pulse.firstPersonYaw,
            firstPersonPitch = pulse.firstPersonPitch + micro,
            firstPersonRoll = pulse.firstPersonRoll,
            firstPersonX = pulse.firstPersonX,
            firstPersonY = pulse.firstPersonY,
            firstPersonZ = pulse.firstPersonZ,
            strain = state.strain,
            breakWave = pulse.breakWave,
        )
    }

    override fun get3DTransform(
        modelName: String,
        type: TransformType,
        tickDelta: Float,
        value0: Vec3f,
    ): Vec3f {
        if (modelName == BODY || modelName == HEAD) {
            val frames = hands.keys.map { frame(it, tickDelta) }
            if (type == TransformType.POSITION && modelName == BODY) {
                return Vec3f(
                    value0.x + frames.sumOf { it.bodyXDelta.toDouble() }.toFloat(),
                    value0.y + frames.sumOf { it.bodyYDelta.toDouble() }.toFloat(),
                    value0.z + frames.sumOf { it.bodyZDelta.toDouble() }.toFloat(),
                )
            }
            if (type != TransformType.ROTATION) return value0
            return if (modelName == HEAD) {
                Vec3f(
                    value0.x + frames.sumOf { it.headPitchDelta.toDouble() }.toFloat(),
                    value0.y + frames.sumOf { it.headYawDelta.toDouble() }.toFloat(),
                    value0.z + frames.sumOf { it.headRollDelta.toDouble() }.toFloat(),
                )
            } else {
                Vec3f(
                    value0.x + frames.sumOf { it.bodyPitchDelta.toDouble() }.toFloat(),
                    value0.y + frames.sumOf { it.bodyYawDelta.toDouble() }.toFloat(),
                    value0.z + frames.sumOf { it.bodyRollDelta.toDouble() }.toFloat(),
                )
            }
        }
        if (modelName == LEFT_LEG || modelName == RIGHT_LEG) {
            if (type != TransformType.ROTATION) return value0
            val frames = hands.keys.map { frame(it, tickDelta) }
            return if (modelName == LEFT_LEG) {
                Vec3f(
                    value0.x + frames.sumOf { it.leftLegPitchDelta.toDouble() }.toFloat(),
                    value0.y + frames.sumOf { it.leftLegYawDelta.toDouble() }.toFloat(),
                    value0.z + frames.sumOf { it.leftLegRollDelta.toDouble() }.toFloat(),
                )
            } else {
                Vec3f(
                    value0.x + frames.sumOf { it.rightLegPitchDelta.toDouble() }.toFloat(),
                    value0.y + frames.sumOf { it.rightLegYawDelta.toDouble() }.toFloat(),
                    value0.z + frames.sumOf { it.rightLegRollDelta.toDouble() }.toFloat(),
                )
            }
        }

        val bone = findBone(modelName) ?: return value0
        val frame = frame(bone.hand, tickDelta)
        val opposite = frame(bone.hand.opposite(), tickDelta)
        if (type == TransformType.POSITION) {
            return if (bone.item) {
                Vec3f(
                    value0.x + frame.itemXDelta,
                    value0.y + frame.itemYDelta,
                    value0.z + frame.itemZDelta,
                )
            } else {
                value0
            }
        }
        if (type != TransformType.ROTATION) return value0

        val x = if (frame.aimWeight > 0.001f) {
            Mth.lerp(frame.aimWeight, value0.x, STAFF_BASE_PITCH + frame.aimPitch)
        } else {
            value0.x
        }
        val y = if (frame.aimWeight > 0.001f) {
            Mth.lerp(frame.aimWeight, value0.y, frame.aimYaw)
        } else {
            value0.y
        }
        val z = if (frame.aimWeight > 0.001f) {
            Mth.lerp(frame.aimWeight, value0.z, frame.aimRoll)
        } else {
            value0.z
        }

        return if (bone.item) {
            Vec3f(
                x + frame.itemPitchDelta,
                y + frame.itemYawDelta,
                z + frame.itemRollDelta,
            )
        } else {
            Vec3f(
                x + frame.armPitchDelta + opposite.offhandPitchDelta,
                y + frame.armYawDelta + opposite.offhandYawDelta,
                z + frame.armRollDelta + opposite.offhandRollDelta,
            )
        }
    }

    override fun getFirstPersonMode(tickDelta: Float): FirstPersonMode {
        if (!HexAnimeClientConfig.config.enabled) return FirstPersonMode.DISABLED
        val mishapVisible = hands.values.any { state ->
            isPulseActive(state) && state.pulseKind == CastPulseKind.MISHAP
        }
        if (mishapVisible) {
            return if (HexAnimeClientConfig.config.firstPersonAnimations) {
                FirstPersonMode.THIRD_PERSON_MODEL
            } else {
                FirstPersonMode.DISABLED
            }
        }
        val staffAimVisible = hands.values.any { state ->
            state.profile?.supportsAim == true && (state.activeAim || state.weight > 0.01f)
        }
        return when {
            staffAimVisible -> FirstPersonMode.NONE
            HexAnimeClientConfig.config.firstPersonAnimations -> FirstPersonMode.THIRD_PERSON_MODEL
            else -> FirstPersonMode.DISABLED
        }
    }

    override fun getFirstPersonConfiguration(tickDelta: Float): FirstPersonConfiguration =
        FirstPersonConfiguration(true, true, true, true)

    fun isMishapActive(hand: InteractionHand): Boolean {
        val state = hands.getValue(hand)
        return isPulseActive(state) && state.pulseKind == CastPulseKind.MISHAP
    }

    private fun refreshHeldItem(hand: InteractionHand, state: HandState) {
        val snapshot = CastItems.snapshot(player.getItemInHand(hand))
        if (snapshot != null) {
            val damageJump = state.hasSample && snapshot.damageValue > state.lastDamageValue
            val mediaDrop = state.hasSample &&
                state.lastMediaFullness >= 0f &&
                snapshot.mediaFullness >= 0f &&
                state.lastMediaFullness - snapshot.mediaFullness >= 0.06f

            state.profile = snapshot.profile
            state.variant = snapshot.variant
            state.strain = snapshot.strain
            state.packagedHex = snapshot.packagedHex
            state.lastDamageValue = snapshot.damageValue
            state.lastMediaFullness = snapshot.mediaFullness
            state.hasSample = true

            if (HexAnimeClientConfig.config.damageAnimations &&
                (damageJump || mediaDrop) &&
                !isPulseActive(state)
            ) {
                triggerPulseInternal(state, snapshot.profile, snapshot.variant, snapshot.strain, CastPulseKind.DAMAGE)
            }
        } else {
            val previousProfile = state.profile
            if (state.hasSample &&
                !state.packagedHex &&
                previousProfile?.pulseStyle?.allowBreak != false &&
                state.strain >= 0.92f &&
                previousProfile != null &&
                !isPulseActive(state)
            ) {
                triggerPulseInternal(
                    state,
                    previousProfile,
                    state.variant,
                    state.strain,
                    CastPulseKind.BREAK,
                )
            }
            state.hasSample = false
            state.lastDamageValue = -1
            state.lastMediaFullness = -1f
            if (!state.activeAim && !isPulseActive(state) && state.weight <= 0.01f) {
                state.strain = 0f
            }
        }
    }

    private fun triggerPulseInternal(
        state: HandState,
        profile: CastItemProfile,
        variant: Int,
        strain: Float,
        kind: CastPulseKind,
    ) {
        val style = profile.pulseStyle
        if (style.reentry &&
            kind == CastPulseKind.CAST &&
            state.pulseKind == CastPulseKind.CAST &&
            state.pulseProfile?.id == profile.id &&
            isPulseActive(state)
        ) {
            val duration = pulseDurationTicks(state)
            state.pulseProfile = profile
            state.pulseVariant = variant.coerceAtLeast(0)
            state.strain = strain.coerceIn(0f, 1f)
            state.pulseKind = kind
            state.pulseStartTick = style.reentryStartTick(ageTicks, duration)
            return
        }
        state.pulseProfile = profile
        state.pulseVariant = variant.coerceAtLeast(0)
        state.strain = strain.coerceIn(0f, 1f)
        state.pulseKind = kind
        state.pulseStartTick = ageTicks
    }

    private fun findBone(modelName: String): BoneRef? {
        for (hand in InteractionHand.values()) {
            val arm = Staffs.armBone(player, hand)
            if (modelName == arm) return BoneRef(hand, item = false)
            if (modelName == Staffs.itemBone(arm)) return BoneRef(hand, item = true)
        }
        return null
    }

    private fun isPulseActive(state: HandState): Boolean {
        if (state.pulseProfile == null) return false
        val duration = pulseDurationTicks(state)
        return ageTicks - state.pulseStartTick in 0f..duration
    }

    private fun hasDamageMotion(state: HandState): Boolean {
        if (!HexAnimeClientConfig.config.damageAnimations) return false
        val profile = state.profile ?: return false
        return damageState(profile.damage, state.strain) > 0f
    }

    private fun pulseMotion(state: HandState, phase: Float): PulseMotion {
        if (!isPulseActive(state)) return PulseMotion.NONE
        val motion = motionFor(state)
        val elapsed = phase - state.pulseStartTick
        val t = (elapsed / pulseDurationTicks(state)).coerceIn(0f, 1f)
        val envelope = pulseEnvelope(state, state.pulseKind, motion, t)
        val chaos = if (state.pulseKind == CastPulseKind.MISHAP) {
            sin(t * PI * 7f).toFloat() * (1f - t) * 0.18f
        } else {
            0f
        }
        val breakWave = when (state.pulseKind) {
            CastPulseKind.BREAK -> sin(t * PI * 4f).toFloat() * (1f - t) * 0.58f
            CastPulseKind.MISHAP -> sin(t * PI * 6f).toFloat() * (1f - t) * 0.42f
            else -> 0f
        }
        if (state.pulseKind == CastPulseKind.MISHAP) {
            return mishapPulseMotion(state, motion, envelope, t, chaos, breakWave)
        }
        val rollScale = when (state.pulseKind) {
            CastPulseKind.CAST -> 0.18f
            CastPulseKind.DAMAGE -> 0.24f
            CastPulseKind.BREAK -> 0.58f
            CastPulseKind.MISHAP -> 0.82f
        }
        val bodyPitch = if (state.pulseKind == CastPulseKind.MISHAP) {
            abs(envelope) * (0.24f + abs(motion.bodyPitch) * 0.55f) + abs(breakWave) * 0.08f
        } else {
            envelope * motion.bodyPitch - breakWave * 0.12f
        }

        return PulseMotion(
            motion = motion,
            bodyYaw = envelope * motion.bodyYaw + chaos,
            bodyPitch = bodyPitch,
            bodyRoll = envelope * motion.bodyRoll * rollScale + chaos * 0.8f,
            bodyX = envelope * motion.bodyX,
            bodyY = -envelope * motion.bodyY,
            bodyZ = envelope * motion.bodyZ,
            headYaw = -envelope * motion.headYaw + chaos * 1.4f,
            headPitch = -envelope * motion.headPitch - breakWave * 0.16f,
            headRoll = -envelope * motion.headRoll + chaos,
            leftLegYaw = envelope * motion.leftLegYaw,
            leftLegPitch = envelope * motion.leftLegPitch,
            leftLegRoll = envelope * motion.leftLegRoll * rollScale,
            rightLegYaw = envelope * motion.rightLegYaw,
            rightLegPitch = envelope * motion.rightLegPitch,
            rightLegRoll = envelope * motion.rightLegRoll * rollScale,
            armYaw = envelope * motion.armYaw,
            armPitch = envelope * motion.armPitch - breakWave * 0.10f,
            armRoll = envelope * motion.armRoll + chaos,
            itemYaw = envelope * motion.itemYaw,
            itemPitch = envelope * motion.itemPitch - breakWave * 0.19f,
            itemRoll = envelope * motion.itemRoll + chaos * 1.5f,
            itemX = envelope * motion.itemX,
            itemY = -envelope * motion.itemY,
            itemZ = envelope * motion.itemZ,
            offhandYaw = envelope * motion.offhandYaw,
            offhandPitch = envelope * motion.offhandPitch + breakWave * 0.08f,
            offhandRoll = envelope * motion.offhandRoll - chaos,
            firstPersonYaw = envelope * motion.firstPersonYaw,
            firstPersonPitch = envelope * motion.firstPersonPitch - breakWave * 0.14f,
            firstPersonRoll = envelope * motion.firstPersonRoll + chaos * 1.2f,
            firstPersonX = envelope * motion.firstPersonX,
            firstPersonY = envelope * motion.firstPersonY,
            firstPersonZ = envelope * motion.firstPersonZ,
            breakWave = breakWave,
        )
    }

    private fun mishapPulseMotion(
        state: HandState,
        motion: VariantMotion,
        envelope: Float,
        t: Float,
        chaos: Float,
        breakWave: Float,
    ): PulseMotion {
        val impact = abs(envelope)
        val mirror = if (state.pulseVariant % 2 == 0) 1f else -1f
        val stagger = sin(t * PI * 3f).toFloat() * (1f - t)
        return PulseMotion(
            motion = motion,
            bodyYaw = mirror * 0.08f * impact + chaos * 0.35f,
            bodyPitch = impact * 0.48f + abs(breakWave) * 0.08f,
            bodyRoll = mirror * 0.035f * impact + chaos * 0.22f,
            bodyX = -mirror * 0.10f * impact,
            bodyY = 0.04f * impact,
            bodyZ = 0.24f * impact + abs(breakWave) * 0.06f,
            headYaw = -mirror * 0.20f * impact + chaos * 1.1f,
            headPitch = 0.20f * impact - breakWave * 0.10f,
            headRoll = -mirror * 0.06f * impact + chaos,
            leftLegYaw = mirror * 0.18f * impact,
            leftLegPitch = 0.26f * impact + stagger * 0.12f,
            leftLegRoll = -mirror * 0.04f * impact,
            rightLegYaw = -mirror * 0.14f * impact,
            rightLegPitch = -0.20f * impact - stagger * 0.08f,
            rightLegRoll = mirror * 0.03f * impact,
            armYaw = mirror * 0.38f * impact,
            armPitch = -0.52f * impact - abs(breakWave) * 0.08f,
            armRoll = mirror * 0.46f * impact + chaos,
            itemYaw = -mirror * 0.32f * impact,
            itemPitch = 0.44f * impact - breakWave * 0.12f,
            itemRoll = mirror * 0.56f * impact + chaos * 1.2f,
            itemX = -mirror * 0.08f * impact,
            itemY = -0.08f * impact,
            itemZ = 0.18f * impact,
            offhandYaw = -mirror * 0.44f * impact,
            offhandPitch = -0.62f * impact,
            offhandRoll = -mirror * 0.38f * impact - chaos,
            firstPersonYaw = 0f,
            firstPersonPitch = 0f,
            firstPersonRoll = 0f,
            firstPersonX = 0f,
            firstPersonY = 0f,
            firstPersonZ = 0f,
            breakWave = breakWave,
        )
    }

    private fun pulseEnvelope(state: HandState, kind: CastPulseKind, motion: VariantMotion, t: Float): Float {
        val anticipation = motion.anticipation.coerceIn(0.05f, 0.45f)
        val release = (anticipation + 0.12f).coerceAtMost(0.58f)
        val hold = (release + 0.16f).coerceAtMost(0.78f)
        val core = when {
            t < anticipation -> {
                val k = smoothStep(t / anticipation)
                lerp(0f, -WINDUP_WEIGHT, k)
            }
            t < release -> {
                val k = smoothStep((t - anticipation) / (release - anticipation))
                lerp(-WINDUP_WEIGHT, 1f, k)
            }
            t < hold -> {
                val k = smoothStep((t - release) / (hold - release))
                lerp(1f, HOLD_WEIGHT, k)
            }
            else -> {
                val k = smoothStep((t - hold) / (1f - hold))
                lerp(HOLD_WEIGHT, 0f, k)
            }
        }
        val beat = maxOf(motion.secondaryBeat, stateStyle(state).secondaryBeat)
        val secondary = if (beat > 0f && t in 0.42f..0.78f) {
            val k = (t - 0.42f) / 0.36f
            sin(PI * k).toFloat() * beat * 0.18f
        } else {
            0f
        }
        val style = stateStyle(state)
        val kindScale = when (kind) {
            CastPulseKind.CAST -> style.castScale
            CastPulseKind.BREAK -> motion.breakScale() * style.breakScale
            CastPulseKind.DAMAGE -> 0.58f
            CastPulseKind.MISHAP -> 1.85f
        }
        return (core + secondary) * kindScale
    }

    private fun smoothStep(value: Float): Float {
        val t = value.coerceIn(0f, 1f)
        return t * t * (3f - 2f * t)
    }

    private fun lerp(from: Float, to: Float, delta: Float): Float =
        from + (to - from) * delta

    private fun VariantMotion.breakScale(): Float = when (variant) {
        4, 7 -> 1.7f
        6 -> 1.6f
        else -> 1.45f
    }

    private fun stateStyle(state: HandState): PulseStyle =
        state.pulseProfile?.pulseStyle ?: PulseStyle.DEFAULT

    private fun motionFor(state: HandState): VariantMotion {
        val profile = state.pulseProfile
        return if (profile != null && profile.staffMaterial != StaffMaterial.NONE) {
            StaffCastMotions.byVariant(state.pulseVariant)
        } else {
            VariantMotions.byVariant(state.pulseVariant)
        }
    }

    private fun microActive(state: HandState): Boolean =
        ageTicks - state.microStartTick in 0f..MICRO_PULSE_TICKS

    private fun microPitch(state: HandState, phase: Float): Float {
        val elapsed = phase - state.microStartTick
        if (elapsed < 0f || elapsed > MICRO_PULSE_TICKS) return 0f
        return sin((elapsed / MICRO_PULSE_TICKS) * PI.toFloat()) * MICRO_PULSE_PITCH
    }

    private fun pulseDurationTicks(state: HandState): Float {
        val base = motionFor(state).durationTicks * stateStyle(state).durationScale
        return when (state.pulseKind) {
            CastPulseKind.MISHAP -> (base + 12f).coerceAtLeast(28f)
            CastPulseKind.BREAK -> base + 4f
            CastPulseKind.DAMAGE -> (base * 0.65f).coerceAtLeast(10f)
            CastPulseKind.CAST -> base
        }
    }

    private fun damageState(pose: DamagePose, strain: Float): Float {
        if (strain <= pose.threshold) return 0f
        return ((strain - pose.threshold) / (1f - pose.threshold)).coerceIn(0f, 1f)
    }

    private fun damageTremor(
        pose: DamagePose,
        damage: Float,
        phase: Float,
        variant: Int,
    ): Tremor {
        if (damage <= 0f) return Tremor.NONE
        val variantPhase = variant * 0.63f
        val wave = phase * pose.frequency * 0.35f + variantPhase
        return Tremor(
            yaw = sin(wave * 1.13f) * pose.tremorYaw * damage,
            pitch = sin(wave * 1.47f + 0.8f) * pose.tremorPitch * damage,
            roll = sin(wave * 0.91f + 1.7f) * pose.tremorRoll * damage,
        )
    }

    private fun InteractionHand.opposite(): InteractionHand =
        if (this == InteractionHand.MAIN_HAND) InteractionHand.OFF_HAND else InteractionHand.MAIN_HAND

    private data class HandState(
        var activeAim: Boolean = false,
        var remoteAim: Boolean = false,
        var lastRemoteAimTick: Float = -10_000f,
        var profile: CastItemProfile? = null,
        var variant: Int = 0,
        var strain: Float = 0f,
        var packagedHex: Boolean = false,
        var hasSample: Boolean = false,
        var lastDamageValue: Int = -1,
        var lastMediaFullness: Float = -1f,
        var targetX: Float = 0f,
        var targetY: Float = 0f,
        var shownX: Float = 0f,
        var shownY: Float = 0f,
        var weight: Float = 0f,
        var prevX: Float = 0f,
        var prevY: Float = 0f,
        var prevWeight: Float = 0f,
        var pulseProfile: CastItemProfile? = null,
        var pulseVariant: Int = 0,
        var pulseKind: CastPulseKind = CastPulseKind.CAST,
        var pulseStartTick: Float = -10_000f,
        var microStartTick: Float = -10_000f,
    )

    private data class BoneRef(val hand: InteractionHand, val item: Boolean)

    private data class Tremor(val yaw: Float, val pitch: Float, val roll: Float) {
        companion object {
            val NONE = Tremor(0f, 0f, 0f)
        }
    }

    private data class PulseMotion(
        val motion: VariantMotion?,
        val bodyYaw: Float,
        val bodyPitch: Float,
        val bodyRoll: Float,
        val bodyX: Float,
        val bodyY: Float,
        val bodyZ: Float,
        val headYaw: Float,
        val headPitch: Float,
        val headRoll: Float,
        val leftLegYaw: Float,
        val leftLegPitch: Float,
        val leftLegRoll: Float,
        val rightLegYaw: Float,
        val rightLegPitch: Float,
        val rightLegRoll: Float,
        val armYaw: Float,
        val armPitch: Float,
        val armRoll: Float,
        val itemYaw: Float,
        val itemPitch: Float,
        val itemRoll: Float,
        val itemX: Float,
        val itemY: Float,
        val itemZ: Float,
        val offhandYaw: Float,
        val offhandPitch: Float,
        val offhandRoll: Float,
        val firstPersonYaw: Float,
        val firstPersonPitch: Float,
        val firstPersonRoll: Float,
        val firstPersonX: Float,
        val firstPersonY: Float,
        val firstPersonZ: Float,
        val breakWave: Float,
    ) {
        companion object {
            val NONE = PulseMotion(
                motion = null,
                bodyYaw = 0f,
                bodyPitch = 0f,
                bodyRoll = 0f,
                bodyX = 0f,
                bodyY = 0f,
                bodyZ = 0f,
                headYaw = 0f,
                headPitch = 0f,
                headRoll = 0f,
                leftLegYaw = 0f,
                leftLegPitch = 0f,
                leftLegRoll = 0f,
                rightLegYaw = 0f,
                rightLegPitch = 0f,
                rightLegRoll = 0f,
                armYaw = 0f,
                armPitch = 0f,
                armRoll = 0f,
                itemYaw = 0f,
                itemPitch = 0f,
                itemRoll = 0f,
                itemX = 0f,
                itemY = 0f,
                itemZ = 0f,
                offhandYaw = 0f,
                offhandPitch = 0f,
                offhandRoll = 0f,
                firstPersonYaw = 0f,
                firstPersonPitch = 0f,
                firstPersonRoll = 0f,
                firstPersonX = 0f,
                firstPersonY = 0f,
                firstPersonZ = 0f,
                breakWave = 0f,
            )
        }
    }

    companion object {
        private const val STAFF_BASE_PITCH = -0.28f
        private const val BODY = "body"
        private const val HEAD = "head"
        private const val LEFT_LEG = "leftLeg"
        private const val RIGHT_LEG = "rightLeg"
        private const val REMOTE_AIM_TIMEOUT_TICKS = 30f
        private const val WINDUP_WEIGHT = 0.24f
        private const val HOLD_WEIGHT = 0.82f
        private const val STAFF_AIM_PER_TICK = 0.45f
        private const val STAFF_FADE_PER_TICK = 0.22f
        private const val STAFF_YAW_SPREAD = 0.32f
        private const val STAFF_PITCH_SPREAD = 0.24f
        private const val MICRO_PULSE_TICKS = 4f
        private const val MICRO_PULSE_PITCH = -0.10f
    }
}

data class CastHandFrame(
    val profile: CastItemProfile?,
    val motion: VariantMotion?,
    val aimWeight: Float,
    val aimYaw: Float,
    val aimPitch: Float,
    val aimRoll: Float,
    val bodyYawDelta: Float,
    val bodyPitchDelta: Float,
    val bodyRollDelta: Float,
    val bodyXDelta: Float,
    val bodyYDelta: Float,
    val bodyZDelta: Float,
    val headYawDelta: Float,
    val headPitchDelta: Float,
    val headRollDelta: Float,
    val leftLegYawDelta: Float,
    val leftLegPitchDelta: Float,
    val leftLegRollDelta: Float,
    val rightLegYawDelta: Float,
    val rightLegPitchDelta: Float,
    val rightLegRollDelta: Float,
    val armYawDelta: Float,
    val armPitchDelta: Float,
    val armRollDelta: Float,
    val itemYawDelta: Float,
    val itemPitchDelta: Float,
    val itemRollDelta: Float,
    val itemXDelta: Float,
    val itemYDelta: Float,
    val itemZDelta: Float,
    val offhandYawDelta: Float,
    val offhandPitchDelta: Float,
    val offhandRollDelta: Float,
    val firstPersonYaw: Float,
    val firstPersonPitch: Float,
    val firstPersonRoll: Float,
    val firstPersonX: Float,
    val firstPersonY: Float,
    val firstPersonZ: Float,
    val strain: Float,
    val breakWave: Float,
) {
    val hasMotion: Boolean
        get() = aimWeight > 0.001f ||
            abs(armYawDelta) + abs(armPitchDelta) + abs(armRollDelta) +
            abs(itemYawDelta) + abs(itemPitchDelta) + abs(itemRollDelta) +
            abs(bodyYawDelta) + abs(bodyPitchDelta) + abs(bodyRollDelta) +
            abs(bodyXDelta) + abs(bodyYDelta) + abs(bodyZDelta) +
            abs(leftLegYawDelta) + abs(leftLegPitchDelta) + abs(leftLegRollDelta) +
            abs(rightLegYawDelta) + abs(rightLegPitchDelta) + abs(rightLegRollDelta) >
            0.001f

    companion object {
        val EMPTY = CastHandFrame(
            profile = null,
            motion = null,
            aimWeight = 0f,
            aimYaw = 0f,
            aimPitch = 0f,
            aimRoll = 0f,
            bodyYawDelta = 0f,
            bodyPitchDelta = 0f,
            bodyRollDelta = 0f,
            bodyXDelta = 0f,
            bodyYDelta = 0f,
            bodyZDelta = 0f,
            headYawDelta = 0f,
            headPitchDelta = 0f,
            headRollDelta = 0f,
            leftLegYawDelta = 0f,
            leftLegPitchDelta = 0f,
            leftLegRollDelta = 0f,
            rightLegYawDelta = 0f,
            rightLegPitchDelta = 0f,
            rightLegRollDelta = 0f,
            armYawDelta = 0f,
            armPitchDelta = 0f,
            armRollDelta = 0f,
            itemYawDelta = 0f,
            itemPitchDelta = 0f,
            itemRollDelta = 0f,
            itemXDelta = 0f,
            itemYDelta = 0f,
            itemZDelta = 0f,
            offhandYawDelta = 0f,
            offhandPitchDelta = 0f,
            offhandRollDelta = 0f,
            firstPersonYaw = 0f,
            firstPersonPitch = 0f,
            firstPersonRoll = 0f,
            firstPersonX = 0f,
            firstPersonY = 0f,
            firstPersonZ = 0f,
            strain = 0f,
            breakWave = 0f,
        )
    }
}

class CastItemLayer(val animation: CastItemAnimation) : ModifierLayer<CastItemAnimation>(animation) {
    constructor(player: AbstractClientPlayer) : this(CastItemAnimation(player))
}

object CastAnimationTuning {
    const val LAYER_PRIORITY = 1000
}
