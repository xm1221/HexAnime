# HexAnime

Minecraft 1.20.1 玩家动画模组（Architectury，Fabric + Forge，Mojmap），作者 xm1221。

> 仓库：https://github.com/xm1221/HexAnime

## 这个模组做什么

手持 **Hex Casting** 的法杖，并打开咒术绘制界面（`at.petrak.hexcasting.client.gui.GuiSpellcasting`）画图案时，持杖的那只手臂会指向鼠标光标。其他玩家也能看到。

- 法杖判定：Hex 的 `ItemStaff`，或物品标签 `hexcasting:staves`。
- 主手默认用右臂。副手用另一只手臂；左撇子（主手在左侧）会换成左臂。
- 客户端大约每 50 毫秒（约 20 Hz）采样一次光标，相对绘制网格中心（GUI 中心）归一化到 ±1，只有变化时才发送。
- 服务器会确认玩家确实拿着法杖、限制频率、把数值夹在范围内，再广播给正在追踪该玩家的其他人。
- 动画用 KosmX **playerAnimator** 的一层（优先级 1000），每帧插值，松开或关掉界面后淡出。
- **第一人称手臂仍是原版渲染**（不盖住绘制网格）。自己按 F5 看第三人称时，手臂会跟着光标。

playerAnimator **没有**打进本模组，请向原作者下载。

版本 `0.0.1`（`0.0.1+1.20.1`）。

## 需要自行安装的依赖

下面这些都是硬依赖（或 Hex Casting 自己的硬依赖）。缺任何一个，游戏都会拒绝加载。

### HexAnime 直接要求

| 依赖 | 说明 |
| --- | --- |
| Minecraft 1.20.1 | Java 17 |
| Hex Casting | `>= 0.11.3`，模组 ID `hexcasting` |
| playerAnimator | KosmX，`>= 1.0.2-rc1+1.20`。Fabric 模组 ID 是 `player-animator`，Forge 模组 ID 是 `playeranimator`。请从原作者处下载，不要使用重新打包的副本 |
| Architectury API | `>= 9.2.14` |
| Cloth Config | `>= 11.1.106` |
| Fabric API | 仅 Fabric，`>= 0.87.0+1.20.1` |
| Fabric Language Kotlin | 仅 Fabric，`>= 1.10.18+kotlin.1.9.22` |
| Kotlin for Forge | 仅 Forge，`>= 4.10.0` |
| Forge | 仅 Forge，`>= 47` |

### Hex Casting 还要求你安装

**Fabric**

- PAUCAL `>= 0.6.0-pre < 0.7.0`
- Patchouli `>= 1.20.1-80`
- Cardinal Components API `~5.2.1`（base、entity、item、block）
- Inline `>= 1.20.1-1.0.1`
- Cloth Config（上面已经列出）

**Forge**

- PAUCAL `[0.6.0, 0.7.0)`
- Patchouli `[1.20.1-80,)`
- Caelus `[3.1.0+1.20,)`
- Inline `[1.20.1-1.0.1,)`
- Kotlin for Forge（上面已经列出）

Mod Menu 是可选的，用来打开配置界面。

## 构建

```bash
./gradlew build
```

需要 Java 17。模板来自 [xm1221/MieHexTemplate](https://github.com/xm1221/MieHexTemplate)（Mojmap，`use_mielib=no`）。

## English

HexAnime plays a player animation while you draw a Hex Casting pattern. Hold a staff (`ItemStaff` or the `hexcasting:staves` tag) with the spellcasting GUI open, and that arm points at the cursor. Other players see it. The main hand uses the right arm; the offhand and left-handed layouts use the other arm.

The first-person hand stays on the vanilla renderer. Third person, including your own body, uses a playerAnimator layer (priority 1000) that interpolates each frame and fades out when the GUI closes.

playerAnimator is **not** bundled. Download it from KosmX. Fabric mod id: `player-animator`. Forge mod id: `playeranimator`. Version `1.0.2-rc1+1.20` or newer for 1.20.

Also install Hex Casting `0.11.3+` and the dependencies in the table above (Architectury API, Cloth Config, Fabric API or Kotlin for Forge, and Hex Casting's own dependencies: PAUCAL, Patchouli, Cardinal Components or Caelus, Inline).

## License

MIT © xm1221
