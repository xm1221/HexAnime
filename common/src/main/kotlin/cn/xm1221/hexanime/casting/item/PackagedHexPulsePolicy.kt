package cn.xm1221.hexanime.casting.item

/**
 * Maps Hex Casting 0.11.4 packaged-hex use results onto a pulse.
 *
 * `ItemPackagedHex.use` returns [broken] (`InteractionResult.CONSUME`) only when
 * `breakAfterDepletion() && getMedia == 0`. A cypher with media left is not broken.
 * Trinkets and artifacts never break on depletion (`breakAfterDepletion() == false`).
 */
object PackagedHexPulsePolicy {
    @JvmStatic
    fun kindForBroken(broken: Boolean): CastPulseKind =
        if (broken) CastPulseKind.BREAK else CastPulseKind.CAST
}
