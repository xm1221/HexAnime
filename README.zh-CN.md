# HexAnime

Minecraft 1.20.1 玩家动画模组（Architectury，Fabric + Forge）。

> **最终命名：** HexAnime（anime = animation）。仓库：https://github.com/xm1221/HexAnime

## 用途

首个计划功能（**尚未实现**）：手持法杖、在 Hex Casting 施法 GUI 画咒时，手臂与法杖跟随光标，并同步给其他玩家；无 Hex Casting 时退化为跟随准星。

## 状态

仅骨架（`0.0.1+1.20.1`）。运行时 Hex Casting 为可选兼容；已 JiJ 嵌入 KosmX playerAnimator `1.0.2-rc1+1.20`（`include modImplementation`，不 shadow）。

## 构建

```bash
./gradlew build
```

需要 Java 17。模板：MieHexTemplate（Mojmap，`use_mielib=no`）。

## 许可证

MIT © xm1221
