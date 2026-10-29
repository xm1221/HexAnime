package cn.xm1221.hexamine.net

/**
 * TODO: client → server packet carrying the local staff aim / cursor sample.
 *
 * Planned (not implemented):
 * - Sent from the client when staff-follow is active.
 * - Server validates (player exists, rate limit, holding staff / casting context) then fans out [StaffAimS2C] to trackers.
 * - Architectury networking preferred; keep payload free of Hex Casting types.
 */
object StaffAimC2S {
    // stub
}
