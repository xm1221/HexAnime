package cn.xm1221.hexanime.client.anim

import cn.xm1221.hexanime.casting.OvercastStrain
import cn.xm1221.hexanime.casting.MishapFlavor
import cn.xm1221.hexanime.casting.item.CastItemProfile
import cn.xm1221.hexanime.casting.item.CastItemState
import cn.xm1221.hexanime.casting.item.CastItems
import cn.xm1221.hexanime.casting.item.CastPulseKind
import cn.xm1221.hexanime.casting.item.DamagePose
import cn.xm1221.hexanime.casting.item.ItemCastMotion
import cn.xm1221.hexanime.casting.item.PulseStyle
import cn.xm1221.hexanime.casting.item.StaffCastMotions
import cn.xm1221.hexanime.casting.item.StaffMaterial
import cn.xm1221.hexanime.casting.item.VariantMotion
import cn.xm1221.hexanime.casting.item.VariantMotions
import cn.xm1221.hexanime.config.HexAnimeClientConfig
import cn.xm1221.hexanime.staff.StaffAimTiming
import cn.xm1221.hexanime.staff.Staffs
import dev.kosmx.playerAnim.api.TransformType
import dev.kosmx.playerAnim.api.firstPerson.FirstPersonMode
import dev.kosmx.playerAnim.api.firstPerson.FirstPersonConfiguration
import dev.kosmx.playerAnim.api.layered.IAnimation
import dev.kosmx.playerAnim.api.layered.ModifierLayer
import dev.kosmx.playerAnim.core.util.Vec3f
import net.minecraft.client.Minecraft
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
    private var overcastInitial = 0f
    private var overcastStartTick = -10_000f

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
            profile.animatesAsStaff &&
            isPulseActive(state) &&
            state.pulseKind == CastPulseKind.CAST
        ) {
            return
        }
        triggerPulseInternal(state, profile, variant, strain, kind)
    }

    /** Four-tick local dip when Hex accepts a drawn pattern. Not a networked flourish. */
    fun setOvercast(strain: Float) {
        overcastInitial = strain.coerceIn(0f, 1f)
        overcastStartTick = ageTicks
    }

    fun triggerMicroPulse(hand: InteractionHand) {
        hands.getValue(hand).microStartTick = ageTicks
    }

    /**
     * Backup finish when the stack clears without an in-progress cast flourish.
     * Uses the raw aim target, not the displayed spring.
     */
    fun triggerLocalStaffCast(hand: InteractionHand, profile: CastItemProfile, strain: Float) {
        if (!profile.animatesAsStaff || !profile.supportsPulse) return
        val state = hands.getValue(hand)
        val variant = StaffCastMotions.variantForCursor(state.targetX, state.targetY)
        triggerPulse(hand, profile, variant, strain, CastPulseKind.CAST)
    }

    override fun isActive(): Boolean {
        if (!HexAnimeClientConfig.config.enabled) return false
        val overcast = overcastNow(ageTicks)
        return hands.values.any { state ->
            CastItemActivity.shouldRun(
                enabled = true,
                hasAim = state.activeAim || state.weight > 0.01f,
                hasPulse = isPulseActive(state),
                hasMicroPulse = microActive(state),
                hasFatigue = fatigueActive(state),
                overcastStrain = if (state.profile?.animatesAsStaff == true) overcast else 0f,
            )
        }
    }

    override fun tick() {
        ageTicks += 1f
        observeHeldItems()
        for (state in hands.values) {
            if (state.remoteAim && state.activeAim &&
                ageTicks - state.lastRemoteAimTick > StaffAimTiming.REMOTE_TIMEOUT_TICKS
            ) {
                state.activeAim = false
            }
            state.prevX = state.shownX
            state.prevY = state.shownY
            state.prevWeight = state.weight

            val desiredWeight = if (state.activeAim) 1f else 0f
            state.weight = Mth.lerp(STAFF_FADE_PER_TICK, state.weight, desiredWeight)
            if (!state.activeAim && state.weight < 0.01f) state.weight = 0f
            val mishapNow = isPulseActive(state) && state.pulseKind == CastPulseKind.MISHAP
            if (state.mishapWasActive && !mishapNow) state.fatigueStartTick = ageTicks
            state.mishapWasActive = mishapNow
            stepAimSpring(state)
        }
    }

    /** Display follow only. [HandState.targetX] stays the raw sample used for flourishes and the network. */
    private fun stepAimSpring(state: HandState) {
        val profile = state.profile
        val material = state.profile?.staffMaterial ?: StaffMaterial.NONE
        val moving = state.activeAim ||
            abs(state.shownX) > 0.001f || abs(state.shownY) > 0.001f ||
            abs(state.velX) > 0.001f || abs(state.velY) > 0.001f
        if (profile?.animatesAsStaff != true || !moving) {
            if (!state.activeAim) {
                state.shownX = 0f
                state.shownY = 0f
                state.velX = 0f
                state.velY = 0f
            }
            return
        }
        val params = StaffSprings.forMaterial(material)
        val targetX = if (state.activeAim) state.targetX else 0f
        val targetY = if (state.activeAim) state.targetY else 0f
        val nextX = SpringDamper.step(
            position = state.shownX,
            velocity = state.velX,
            target = targetX,
            stiffness = params.stiffness,
            dampingRatio = params.dampingRatio,
            dt = SpringDamper.TICK_DT,
        )
        val nextY = SpringDamper.step(
            position = state.shownY,
            velocity = state.velY,
            target = targetY,
            stiffness = params.stiffness,
            dampingRatio = params.dampingRatio,
            dt = SpringDamper.TICK_DT,
        )
        state.shownX = nextX.position
        state.velX = nextX.velocity
        state.shownY = nextY.position
        state.velY = nextY.velocity
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
        val active = CastItemActivity.shouldRun(
            enabled = true,
            hasAim = state.activeAim || state.weight > 0.01f,
            hasPulse = isPulseActive(state),
            hasMicroPulse = microActive(state),
            hasFatigue = fatigueActive(state),
            overcastStrain = if (profile.animatesAsStaff) overcastNow(ageTicks) else 0f,
        )
        if (!active) return CastHandFrame.EMPTY
        var x = Mth.lerp(tickDelta, state.prevX, state.shownX)
        var y = Mth.lerp(tickDelta, state.prevY, state.shownY)
        if (profile.staffMaterial == StaffMaterial.QUENCHED) {
            val step = StaffDisplayTuning.quenchedStep()
            x = StaffDisplay.quantize(x, step)
            y = StaffDisplay.quantize(y, step)
        }
        val weight = Mth.lerp(tickDelta, state.prevWeight, state.weight)
        val aimYaw = if (profile.supportsAim) -x * STAFF_YAW_SPREAD else 0f
        val aimPitch = if (profile.supportsAim) y * STAFF_PITCH_SPREAD else 0f
        val phase = ageTicks + tickDelta
        val strain = if (!profile.animatesAsStaff) {
            state.strain
        } else {
            maxOf(state.strain, overcastNow(phase))
        }
        val damage = if (config.damageAnimations) {
            damageState(profile.damage, strain) * damageFeedbackNow(state, phase)
        } else {
            0f
        }
        val tremor = damageTremor(profile.damage, damage, phase, state.variant)
        val chaos = if (!profile.animatesAsStaff) {
            Tremor.NONE
        } else {
            StaffDisplay.chaos(strain, phase).let { Tremor(it.yaw, it.pitch, it.roll) }
        }
        val pulse = pulseMotion(hand, state, phase)
        val micro = microPitch(state, phase)
        val fatigue = FatigueCurve.envelope(phase - state.fatigueStartTick)
        val offhandEmpty = player.getItemInHand(hand.opposite()).isEmpty
        val ritual = if (!offhandEmpty &&
            profile.animatesAsStaff &&
            (weight > 0.05f || isPulseActive(state))
        ) {
            OffhandRitual.poseFor(player, hand)
        } else {
            null
        }
        val ritualWeight = if (ritual == null) {
            0f
        } else {
            maxOf(weight, if (isPulseActive(state)) 1f else 0f).coerceIn(0f, 1f)
        }
        var itemX = pulse.itemX
        var itemY = pulse.itemY
        var itemZ = pulse.itemZ
        if (profile.staffMaterial == StaffMaterial.QUENCHED) {
            itemZ += kotlin.math.sin(phase * 2.4f) * StaffDisplayTuning.quenchedTremor()
        }
        if (profile.staffMaterial == StaffMaterial.MINDSPLICE) {
            val drift = StaffDisplay.drift(phase, StaffDisplayTuning.mindspliceDrift() * weight.coerceIn(0f, 1f))
            itemX += drift.first
            itemY += drift.second
        }

        return CastHandFrame(
            profile = profile,
            motion = pulse.motion,
            aimWeight = if (profile.supportsAim) weight else 0f,
            aimYaw = aimYaw,
            aimPitch = aimPitch,
            aimRoll = 0f,
            bodyYawDelta = pulse.bodyYaw + (tremor.yaw + chaos.yaw) * 0.22f,
            bodyPitchDelta = pulse.bodyPitch + damage * profile.damage.pitchDroop * 0.32f +
                fatigue * FatigueCurve.BODY_DROOP,
            bodyRollDelta = pulse.bodyRoll + (tremor.roll + chaos.roll) * 0.18f,
            bodyXDelta = pulse.bodyX,
            bodyYDelta = pulse.bodyY,
            bodyZDelta = pulse.bodyZ,
            headYawDelta = pulse.headYaw - (tremor.yaw + chaos.yaw) * 0.32f,
            headPitchDelta = pulse.headPitch - (tremor.pitch + chaos.pitch) * 0.24f +
                fatigue * FatigueCurve.HEAD_DROOP,
            headRollDelta = pulse.headRoll - (tremor.roll + chaos.roll) * 0.42f,
            leftLegYawDelta = pulse.leftLegYaw,
            leftLegPitchDelta = pulse.leftLegPitch,
            leftLegRollDelta = pulse.leftLegRoll,
            rightLegYawDelta = pulse.rightLegYaw,
            rightLegPitchDelta = pulse.rightLegPitch,
            rightLegRollDelta = pulse.rightLegRoll,
            armYawDelta = pulse.armYaw + tremor.yaw + chaos.yaw,
            armPitchDelta = pulse.armPitch + tremor.pitch + chaos.pitch +
                damage * profile.damage.pitchDroop + fatigue * FatigueCurve.ARM_DROOP,
            armRollDelta = pulse.armRoll + tremor.roll + chaos.roll,
            itemYawDelta = pulse.itemYaw + (tremor.yaw + chaos.yaw) * 0.82f,
            itemPitchDelta = pulse.itemPitch + (tremor.pitch + chaos.pitch) * 0.82f + micro +
                fatigue * FatigueCurve.ITEM_DROOP,
            itemRollDelta = pulse.itemRoll + (tremor.roll + chaos.roll) * 1.35f,
            itemXDelta = itemX,
            itemYDelta = itemY,
            itemZDelta = itemZ,
            offhandYawDelta = if (offhandEmpty) 0f else pulse.offhandYaw + (ritual?.yaw ?: 0f) * ritualWeight,
            offhandPitchDelta = if (offhandEmpty) 0f else pulse.offhandPitch + (ritual?.pitch ?: 0f) * ritualWeight,
            offhandRollDelta = if (offhandEmpty) 0f else pulse.offhandRoll + (ritual?.roll ?: 0f) * ritualWeight,
            firstPersonYaw = pulse.firstPersonYaw,
            firstPersonPitch = pulse.firstPersonPitch + micro + fatigue * FatigueCurve.ITEM_DROOP,
            firstPersonRoll = pulse.firstPersonRoll,
            firstPersonX = pulse.firstPersonX,
            firstPersonY = pulse.firstPersonY,
            firstPersonZ = pulse.firstPersonZ,
            strain = strain,
            breakWave = pulse.breakWave,
        ).limitTagged()
    }

    private fun CastHandFrame.limitTagged(): CastHandFrame {
        if (profile?.staffMaterial != StaffMaterial.TAGGED) return this
        return copy(
            aimYaw = TaggedEnvelope.scaleRotation(aimYaw),
            aimPitch = TaggedEnvelope.scaleRotation(aimPitch),
            aimRoll = TaggedEnvelope.scaleRotation(aimRoll),
            bodyYawDelta = TaggedEnvelope.scaleRotation(bodyYawDelta),
            bodyPitchDelta = TaggedEnvelope.scaleRotation(bodyPitchDelta),
            bodyRollDelta = TaggedEnvelope.scaleRotation(bodyRollDelta),
            bodyXDelta = TaggedEnvelope.scalePosition(bodyXDelta),
            bodyYDelta = TaggedEnvelope.scalePosition(bodyYDelta),
            bodyZDelta = TaggedEnvelope.scalePosition(bodyZDelta),
            headYawDelta = TaggedEnvelope.scaleRotation(headYawDelta),
            headPitchDelta = TaggedEnvelope.scaleRotation(headPitchDelta),
            headRollDelta = TaggedEnvelope.scaleRotation(headRollDelta),
            leftLegYawDelta = TaggedEnvelope.scaleRotation(leftLegYawDelta),
            leftLegPitchDelta = TaggedEnvelope.scaleRotation(leftLegPitchDelta),
            leftLegRollDelta = TaggedEnvelope.scaleRotation(leftLegRollDelta),
            rightLegYawDelta = TaggedEnvelope.scaleRotation(rightLegYawDelta),
            rightLegPitchDelta = TaggedEnvelope.scaleRotation(rightLegPitchDelta),
            rightLegRollDelta = TaggedEnvelope.scaleRotation(rightLegRollDelta),
            armYawDelta = TaggedEnvelope.scaleRotation(armYawDelta),
            armPitchDelta = TaggedEnvelope.scaleRotation(armPitchDelta),
            armRollDelta = TaggedEnvelope.scaleRotation(armRollDelta),
            itemYawDelta = TaggedEnvelope.scaleRotation(itemYawDelta),
            itemPitchDelta = TaggedEnvelope.scaleRotation(itemPitchDelta),
            itemRollDelta = TaggedEnvelope.scaleRotation(itemRollDelta),
            itemXDelta = TaggedEnvelope.scalePosition(itemXDelta),
            itemYDelta = TaggedEnvelope.scalePosition(itemYDelta),
            itemZDelta = TaggedEnvelope.scalePosition(itemZDelta),
            firstPersonYaw = TaggedEnvelope.scaleRotation(firstPersonYaw),
            firstPersonPitch = TaggedEnvelope.scaleRotation(firstPersonPitch),
            firstPersonRoll = TaggedEnvelope.scaleRotation(firstPersonRoll),
            firstPersonX = TaggedEnvelope.scalePosition(firstPersonX),
            firstPersonY = TaggedEnvelope.scalePosition(firstPersonY),
            firstPersonZ = TaggedEnvelope.scalePosition(firstPersonZ),
        )
    }

    override fun get3DTransform(
        modelName: String,
        type: TransformType,
        tickDelta: Float,
        value0: Vec3f,
    ): Vec3f {
        val firstPerson = isLocalFirstPerson()
        val (rotScale, posScale) = localFirstPersonScales()
        if (modelName == BODY || modelName == HEAD) {
            val frames = hands.keys.map { frame(it, tickDelta) }
            if (type == TransformType.POSITION && modelName == BODY) {
                return Vec3f(
                    value0.x + frames.sumOf { it.bodyXDelta.toDouble() }.toFloat() * posScale,
                    value0.y + frames.sumOf { it.bodyYDelta.toDouble() }.toFloat() * posScale,
                    value0.z + frames.sumOf { it.bodyZDelta.toDouble() }.toFloat() * posScale,
                )
            }
            if (type != TransformType.ROTATION) return value0
            return if (modelName == HEAD) {
                Vec3f(
                    value0.x + frames.sumOf { it.headPitchDelta.toDouble() }.toFloat() * rotScale,
                    value0.y + frames.sumOf { it.headYawDelta.toDouble() }.toFloat() * rotScale,
                    value0.z + frames.sumOf { it.headRollDelta.toDouble() }.toFloat() * rotScale,
                )
            } else {
                Vec3f(
                    value0.x + frames.sumOf { it.bodyPitchDelta.toDouble() }.toFloat() * rotScale,
                    value0.y + frames.sumOf { it.bodyYawDelta.toDouble() }.toFloat() * rotScale,
                    value0.z + frames.sumOf { it.bodyRollDelta.toDouble() }.toFloat() * rotScale,
                )
            }
        }
        if (modelName == LEFT_LEG || modelName == RIGHT_LEG) {
            if (type != TransformType.ROTATION) return value0
            val frames = hands.keys.map { frame(it, tickDelta) }
            return if (modelName == LEFT_LEG) {
                Vec3f(
                    value0.x + frames.sumOf { it.leftLegPitchDelta.toDouble() }.toFloat() * rotScale,
                    value0.y + frames.sumOf { it.leftLegYawDelta.toDouble() }.toFloat() * rotScale,
                    value0.z + frames.sumOf { it.leftLegRollDelta.toDouble() }.toFloat() * rotScale,
                )
            } else {
                Vec3f(
                    value0.x + frames.sumOf { it.rightLegPitchDelta.toDouble() }.toFloat() * rotScale,
                    value0.y + frames.sumOf { it.rightLegYawDelta.toDouble() }.toFloat() * rotScale,
                    value0.z + frames.sumOf { it.rightLegRollDelta.toDouble() }.toFloat() * rotScale,
                )
            }
        }

        val bone = findBone(modelName) ?: return value0
        val frame = frame(bone.hand, tickDelta)
        val opposite = frame(bone.hand.opposite(), tickDelta)
        if (type == TransformType.POSITION) {
            return if (bone.item) {
                var itemZ = frame.itemZDelta
                if (!firstPerson && frame.profile?.animatesAsStaff == false) {
                    val motion = frame.profile.itemCastMotion
                        ?: frame.motion?.variant?.let(ItemCastMotion::forVariant)
                    if (motion == ItemCastMotion.CHEST_HOVER || motion == ItemCastMotion.CHEST_SPIN) {
                        itemZ = -itemZ
                    }
                }
                val itemScale = if (firstPerson) FIRST_PERSON_ITEM_POSITION_SCALE else posScale
                Vec3f(
                    value0.x + frame.itemXDelta * itemScale,
                    value0.y + frame.itemYDelta * itemScale,
                    value0.z + itemZ * itemScale,
                )
            } else {
                value0
            }
        }
        if (type != TransformType.ROTATION) return value0

        val x = scaledAim(value0.x, STAFF_BASE_PITCH + frame.aimPitch, frame.aimWeight, rotScale)
        val y = scaledAim(value0.y, frame.aimYaw, frame.aimWeight, rotScale)
        val z = scaledAim(value0.z, frame.aimRoll, frame.aimWeight, rotScale)

        return if (bone.item) {
            val itemScale = if (firstPerson) FIRST_PERSON_ITEM_ROTATION_SCALE else rotScale
            Vec3f(
                x + frame.itemPitchDelta * itemScale,
                y + frame.itemYawDelta * itemScale,
                z + frame.itemRollDelta * itemScale,
            )
        } else {
            Vec3f(
                x + (frame.armPitchDelta + opposite.offhandPitchDelta) * rotScale,
                y + (frame.armYawDelta + opposite.offhandYawDelta) * rotScale,
                z + (frame.armRollDelta + opposite.offhandRollDelta) * rotScale,
            )
        }
    }

    /** Local first person only. Other players keep the full pose on this client. */
    private fun localFirstPersonScales(): Pair<Float, Float> {
        if (isLocalFirstPerson()) {
            return FirstPersonEnvelope.ROTATION to FirstPersonEnvelope.TRANSLATION
        }
        return 1f to 1f
    }

    private fun isLocalFirstPerson(): Boolean {
        val minecraft = Minecraft.getInstance()
        val local = minecraft.player
        return local != null &&
            local.id == player.id &&
            minecraft.options.cameraType.isFirstPerson
    }

    private fun scaledAim(base: Float, target: Float, weight: Float, rotationScale: Float): Float {
        if (weight <= 0.001f) return base
        val blended = Mth.lerp(weight, base, target)
        return base + (blended - base) * rotationScale
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
            val profile = state.profile
            profile?.animatesAsStaff == true &&
                profile.supportsAim &&
                (state.activeAim || state.weight > 0.01f)
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
                isCastingContext(state) &&
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
                isCastingContext(state) &&
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
            state.damageFeedbackStartTick = -10_000f
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
        if (kind == CastPulseKind.DAMAGE) state.damageFeedbackStartTick = ageTicks
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
        if (kind == CastPulseKind.CAST) {
            state.castOrdinal += 1
            state.legMotionAllowed = LegMotionCadence.shouldAnimate(state.castOrdinal)
        } else {
            state.legMotionAllowed = kind == CastPulseKind.BREAK || kind == CastPulseKind.MISHAP
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

    private fun pulseMotion(hand: InteractionHand, state: HandState, phase: Float): PulseMotion {
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

        val legScale = if (state.legMotionAllowed) 1f else 0f
        return PulseMotion(
            motion = motion,
            bodyYaw = envelope * motion.bodyYaw + chaos,
            bodyPitch = bodyPitch,
            bodyRoll = envelope * motion.bodyRoll * rollScale + chaos * 0.8f,
            bodyX = envelope * motion.bodyX,
            bodyY = -envelope * motion.bodyY,
            bodyZ = envelope * motion.bodyZ,
            headYaw = -envelope * motion.headYaw + chaos * 1.4f,
            headPitch = -envelope * motion.headPitch - breakWave * 0.16f + mindspliceHead(state, envelope),
            headRoll = -envelope * motion.headRoll + chaos,
            leftLegYaw = envelope * motion.leftLegYaw * legScale,
            leftLegPitch = envelope * motion.leftLegPitch * legScale,
            leftLegRoll = envelope * motion.leftLegRoll * rollScale * legScale,
            rightLegYaw = envelope * motion.rightLegYaw * legScale,
            rightLegPitch = envelope * motion.rightLegPitch * legScale,
            rightLegRoll = envelope * motion.rightLegRoll * rollScale * legScale,
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
        ).withFloatingItem(hand, state, t)
    }

    private fun PulseMotion.withFloatingItem(
        hand: InteractionHand,
        state: HandState,
        t: Float,
    ): PulseMotion {
        if (state.pulseKind != CastPulseKind.CAST) return this
        val profile = state.pulseProfile ?: return this
        if (profile.animatesAsStaff) return this
        val motion = profile.itemCastMotion
            ?: ItemCastMotion.forVariant(state.pulseVariant)
        val inward = if (Staffs.armBone(player, hand) == Staffs.RIGHT_ARM) -1f else 1f
        val floating = ItemFloatMotions.pose(motion, t, inward) ?: return this
        val weight = floating.weight
        val (itemX, itemY, itemZ) = floating.firstPersonOffset()
        return copy(
            armYaw = lerp(armYaw, floating.armYaw, weight),
            armPitch = lerp(armPitch, floating.armPitch, weight),
            armRoll = lerp(armRoll, floating.armRoll, weight),
            itemYaw = lerp(itemYaw, floating.itemYaw, weight),
            itemPitch = lerp(itemPitch, floating.itemPitch, weight),
            itemRoll = lerp(itemRoll, floating.itemRoll, weight),
            itemX = lerp(this.itemX, itemX, weight),
            itemY = lerp(this.itemY, itemY, weight),
            itemZ = lerp(this.itemZ, itemZ, weight),
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
        val flavor = MishapFlavor.fromVariant(state.pulseVariant)
        val collapse = when (flavor) {
            MishapFlavor.RECOIL -> 0.48f
            MishapFlavor.MEDIA -> 0.68f
            MishapFlavor.SPATIAL -> 0.30f
            MishapFlavor.MIND -> 0.38f
        }
        val side = when (flavor) {
            MishapFlavor.RECOIL -> 0.10f
            MishapFlavor.MEDIA -> 0.05f
            MishapFlavor.SPATIAL -> 0.28f
            MishapFlavor.MIND -> 0.16f
        }
        val headYaw = when (flavor) {
            MishapFlavor.RECOIL -> 0.20f
            MishapFlavor.MEDIA -> 0.10f
            MishapFlavor.SPATIAL -> 0.34f
            MishapFlavor.MIND -> 0.62f
        }
        val headRoll = when (flavor) {
            MishapFlavor.RECOIL -> 0.06f
            MishapFlavor.MEDIA -> 0.05f
            MishapFlavor.SPATIAL -> 0.12f
            MishapFlavor.MIND -> 0.34f
        }
        val mindWave = if (flavor == MishapFlavor.MIND) {
            sin(t * PI * 9f).toFloat() * (1f - t)
        } else {
            0f
        }
        val spatialWave = if (flavor == MishapFlavor.SPATIAL) {
            sin(t * PI * 4f).toFloat() * (1f - t)
        } else {
            0f
        }
        val mediaSag = if (flavor == MishapFlavor.MEDIA) impact else 0f
        val armClutch = if (flavor == MishapFlavor.MIND) 0.26f * impact else 0f
        return PulseMotion(
            motion = motion,
            bodyYaw = mirror * side * 0.80f * impact + chaos * 0.35f + mindWave * 0.08f,
            bodyPitch = impact * collapse + abs(breakWave) * 0.08f + mediaSag * 0.08f,
            bodyRoll = mirror * side * 0.35f * impact + chaos * 0.22f + mindWave * 0.06f,
            bodyX = (-mirror * side + spatialWave * 0.10f) * impact,
            bodyY = (0.04f - mediaSag * 0.12f) * impact,
            bodyZ = 0.24f * impact + abs(breakWave) * 0.06f,
            headYaw = -mirror * headYaw * impact + chaos * 1.1f + mindWave * 0.42f,
            headPitch = (if (flavor == MishapFlavor.MEDIA) 0.38f else 0.20f) * impact -
                breakWave * 0.10f,
            headRoll = -mirror * headRoll * impact + chaos + mindWave * 0.30f,
            leftLegYaw = mirror * 0.18f * impact,
            leftLegPitch = (if (flavor == MishapFlavor.MEDIA) 0.40f else 0.26f) * impact +
                stagger * 0.12f,
            leftLegRoll = -mirror * 0.04f * impact,
            rightLegYaw = -mirror * 0.14f * impact,
            rightLegPitch = if (flavor == MishapFlavor.MEDIA) {
                -0.34f * impact - stagger * 0.08f
            } else {
                -0.20f * impact - stagger * 0.08f
            },
            rightLegRoll = mirror * 0.03f * impact,
            armYaw = mirror * (0.38f + side) * impact + mindWave * 0.16f,
            armPitch = (-0.52f - armClutch) * impact - abs(breakWave) * 0.08f,
            armRoll = mirror * (0.46f + armClutch) * impact + chaos,
            itemYaw = -mirror * (0.32f + headYaw * 0.28f) * impact,
            itemPitch = (0.44f + mediaSag * 0.12f) * impact - breakWave * 0.12f,
            itemRoll = mirror * (0.56f + headRoll) * impact + chaos * 1.2f + mindWave * 0.22f,
            itemX = (-mirror * side * 0.80f + spatialWave * 0.06f) * impact,
            itemY = -0.08f * impact,
            itemZ = 0.18f * impact,
            offhandYaw = -mirror * 0.44f * impact,
            offhandPitch = -0.62f * impact,
            offhandRoll = -mirror * 0.38f * impact - chaos,
            firstPersonYaw = spatialWave * 0.18f + mindWave * 0.08f,
            firstPersonPitch = (if (flavor == MishapFlavor.MEDIA) 0.20f else -0.12f) * impact,
            firstPersonRoll = mindWave * 0.16f,
            firstPersonX = spatialWave * 0.04f,
            firstPersonY = -mediaSag * 0.06f,
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
        return if (profile?.animatesAsStaff == true) {
            StaffCastMotions.byVariant(state.pulseVariant)
        } else {
            VariantMotions.byVariant(state.pulseVariant)
        }
    }

    private fun fatigueActive(state: HandState): Boolean =
        FatigueCurve.envelope(ageTicks - state.fatigueStartTick) > 0f

    private fun overcastNow(phase: Float): Float =
        OvercastStrain.remaining(overcastInitial, phase - overcastStartTick)

    private fun mindspliceHead(state: HandState, envelope: Float): Float {
        if (state.pulseKind != CastPulseKind.CAST) return 0f
        if (state.pulseProfile?.staffMaterial != StaffMaterial.MINDSPLICE) return 0f
        return StaffDisplayTuning.headPitch() * envelope.coerceAtLeast(0f)
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
            CastPulseKind.DAMAGE -> (base * 0.45f).coerceIn(8f, 12f)
            CastPulseKind.CAST -> if (state.pulseProfile?.animatesAsStaff == false) {
                if ((state.pulseProfile?.itemCastMotion
                        ?: ItemCastMotion.forVariant(state.pulseVariant)) == ItemCastMotion.HAND_THRUST
                ) {
                    base * PACKAGED_THRUST_DURATION_SCALE
                } else {
                    base + PACKAGED_CAST_EXTRA_TICKS
                }
            } else {
                base
            }
        }
    }

    private fun damageState(pose: DamagePose, strain: Float): Float {
        if (strain <= pose.threshold) return 0f
        return ((strain - pose.threshold) / (1f - pose.threshold)).coerceIn(0f, 1f)
    }

    private fun damageFeedbackNow(state: HandState, phase: Float): Float =
        DamageFeedback.envelope(phase - state.damageFeedbackStartTick)

    private fun isCastingContext(state: HandState): Boolean =
        state.activeAim || state.weight > 0.01f || isPulseActive(state) || player.isUsingItem

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
        var velX: Float = 0f,
        var velY: Float = 0f,
        var weight: Float = 0f,
        var prevX: Float = 0f,
        var prevY: Float = 0f,
        var prevWeight: Float = 0f,
        var pulseProfile: CastItemProfile? = null,
        var pulseVariant: Int = 0,
        var pulseKind: CastPulseKind = CastPulseKind.CAST,
        var pulseStartTick: Float = -10_000f,
        var castOrdinal: Int = 0,
        var legMotionAllowed: Boolean = false,
        var damageFeedbackStartTick: Float = -10_000f,
        var microStartTick: Float = -10_000f,
        var mishapWasActive: Boolean = false,
        var fatigueStartTick: Float = -10_000f,
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
        private const val WINDUP_WEIGHT = 0.24f
        private const val HOLD_WEIGHT = 0.82f
        private const val STAFF_FADE_PER_TICK = 0.22f
        private const val STAFF_YAW_SPREAD = 0.32f
        private const val STAFF_PITCH_SPREAD = 0.24f
        private const val MICRO_PULSE_TICKS = 4f
        private const val MICRO_PULSE_PITCH = -0.10f
        private const val PACKAGED_CAST_EXTRA_TICKS = 8f
        private const val PACKAGED_THRUST_DURATION_SCALE = 0.72f
        private const val FIRST_PERSON_ITEM_POSITION_SCALE = 0.75f
        private const val FIRST_PERSON_ITEM_ROTATION_SCALE = 0.85f
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
