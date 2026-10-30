package cn.xm1221.hexanime.casting

import at.petrak.hexcasting.api.casting.mishaps.Mishap

enum class MishapFlavor(val variant: Int, val commandName: String) {
    RECOIL(0, "recoil"),
    MEDIA(1, "media"),
    SPATIAL(2, "spatial"),
    MIND(3, "mind");

    companion object {
        fun fromVariant(variant: Int): MishapFlavor =
            entries.firstOrNull { it.variant == variant } ?: RECOIL

        fun fromCommandName(name: String): MishapFlavor? =
            entries.firstOrNull { it.commandName == name.lowercase() }
    }
}

/** Stable categories chosen from Hex Casting mishap classes, not localized display text. */
object MishapMotions {
    private val media = setOf(
        "MishapNotEnoughMedia",
        "MishapBadItem",
        "MishapBadOffhandItem",
        "MishapLackingHotbarItem",
    )
    private val spatial = setOf(
        "MishapBadBlock",
        "MishapBadLocation",
        "MishapLocationInWrongDimension",
        "MishapEntityTooFarAway",
        "MishapBadCaster",
        "MishapBadEntity",
        "MishapImmuneEntity",
        "MishapNoAkashicRecord",
        "MishapNoSpellCircle",
    )
    private val mind = setOf(
        "MishapBadBrainsweep",
        "MishapAlreadyBrainswept",
        "MishapUnenlightened",
        "MishapOthersName",
        "MishapInternalException",
        "MishapEvalTooMuch",
        "MishapStackSize",
        "MishapNotEnoughArgs",
        "MishapDivideByZero",
    )

    fun flavorFor(simpleClassName: String): MishapFlavor = when (simpleClassName) {
        in media -> MishapFlavor.MEDIA
        in spatial -> MishapFlavor.SPATIAL
        in mind -> MishapFlavor.MIND
        else -> MishapFlavor.RECOIL
    }

    @JvmStatic
    fun variantFor(mishap: Mishap): Int = flavorFor(mishap.javaClass.simpleName).variant
}
