# HexAnime

Player animation mod for Minecraft 1.20.1, built with Architectury for Fabric and Forge.

Repository: https://github.com/xm1221/HexAnime

## Features

- Staffs are held near vertical while casting; the cursor only applies a small wrist and arm tilt in first and third person.
- Packaged Hex items play a one-shot cast animation when actually used: artifacts, trinkets, cyphers and ancient cyphers.
- Each of the eight Hex visual variants selects a distinct full-body motion. Item class only decides whether an item is a packaged Hex.
- Low media or durability produces continuous tremor, droop and event pulses. A cypher's final depletion gets a stronger break reaction.
- Hex mishaps trigger a shared full-body recoil and a violent first-person response.
- Selected variants use a totem-like pop: brief compression, an upward body jolt, spread arms and a held silhouette before settling.
- Packaged-Hex casts use staged keyframes and the player-model first-person path; staff aiming keeps the original vanilla-hand path.
- A held cast keeps the first release burst, then switches to a variant-aware breathing and brace loop with a short release fade.
- Staff aiming intentionally uses the original direct normalized cursor mapping and fixed smoothing.
- Clients can independently disable local motion, sending their own state, receiving continuous aim, or receiving remote cast events. Servers can disable each sync family globally.

playerAnimator is **not** bundled. Download it from KosmX.

## Animation model

- `CastItemProfiles`: ordered item-to-profile registry with priority-based matching and third-party registration.
- Staff aiming uses the original fixed yaw/pitch ratios; packaged Hex items do not use cursor aim.
- `PulsePose`: reusable release and depletion motion for packaged Hex items.
- `DamagePose`: tremor and droop settings for low media or durability.
- `CastItemAnimation`: one playerAnimator layer per player, managing main and offhand state.

## Dependencies

- Minecraft 1.20.1, Java 17
- Hex Casting `>= 0.11.4`
- playerAnimator `>= 1.0.2-rc1+1.20`
- Architectury API `>= 9.2.14`
- Cloth Config `>= 11.1.106`
- Fabric Loader `>= 0.16.9`, Fabric API `>= 0.92.8+1.20.1` and Fabric Language Kotlin `>= 1.13.7+kotlin.2.2.21`
- Forge `>= 47` and Kotlin for Forge `>= 4.12.0`

Hex Casting also requires PAUCAL, Patchouli and Inline `>= 1.20.1-1.2.2`, plus Cardinal Components on Fabric or Caelus on Forge. Mod Menu is optional on Fabric.

## Build

```bash
./gradlew build
```

Open-source under MIT. Generated from MieHexTemplate.
