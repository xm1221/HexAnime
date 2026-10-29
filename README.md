# HexAnime

[![powered by hexdoc](https://img.shields.io/endpoint?url=https://hexxy.media/api/v0/badge/hexdoc?label=1)](https://github.com/hexdoc-dev/hexdoc)

Player animation mod for Minecraft 1.20.1 (Fabric + Forge via Architectury).

> **HexAnime**（anime = animation）· https://github.com/xm1221/HexAnime

## Purpose / 用途

A **player animation** addon. First planned feature (not implemented yet):

While holding a staff and drawing in Hex Casting's spellcasting GUI, the player's arm + staff follows the cursor, synced to other players. If Hex Casting is absent, fall back to following the crosshair while holding a staff-like item.

这是一个**玩家动画**模组（HexAnime = Hex + Anime/animation）。首个计划功能（尚未实现）：

手持法杖并在咒术绘制（Hex Casting）施法界面画咒时，玩家的手臂与法杖跟随光标，并同步给其他玩家。若未安装 Hex Casting，则退化为手持类法杖物品时手臂跟随准星。

## Status

Skeleton only (`0.0.1+1.20.1`). Stubs under `cn.xm1221.hexanime.client` / `.net`. Hex Casting is an **optional** compat dependency at runtime; KosmX **playerAnimator** `1.0.2-rc1+1.20` is JiJ'd (`include modImplementation`, never shadowed).

## Build

```bash
./gradlew build
```

Java 17 required. Template: [xm1221/MieHexTemplate](https://github.com/xm1221/MieHexTemplate) (Mojmap, `use_mielib=no`).

## License

MIT © xm1221
