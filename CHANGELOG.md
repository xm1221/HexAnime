# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/).

## 0.1.0

### Added

- Profile-driven cast animations for artifacts, trinkets, cyphers and ancient cyphers.
- Variant-aware phase and amplitude, plus media/durability damage feedback.
- Actual item-use and depletion pulses synchronized by the server.
- Full-body mishap reactions and first-person recoil.
- Three-axis Variant motions, one distinct motion for each of the eight Hex variants.
- Totem-like pop and upward body jolt on selected variants.
- Multi-stage anticipation, release, hold and recovery keyframes for packaged Hex casts.
- First-person rendering now uses playerAnimator's third-person model mode.
- Real client and server animation configuration.
- Client subscription packets so servers can skip animation traffic per player.
- Chinese localization for configuration text.

### Changed

- Reverted staff aiming to the original direct normalized cursor mapping and fixed smoothing.
- Changed staff casting to a near-vertical hold with only a small cursor-driven tilt.
- Fixed first-person horizontal cursor direction.
- Suppressed the vanilla packaged-Hex use animation.
- Reworked the animation layer to manage both hands and reusable item profiles.
- Updated to Hex Casting 0.11.4, Kotlin 2.2.21, Gradle 8.14 and Loom 1.9.

### Removed

- Dummy congratulate spells, Patchouli sample entries, action datagen and template placeholder options.

## 0.0.1

### Added

- Initial version.
