# HexAnime

Minecraft 1.20.1 玩家动画模组（Architectury，Fabric + Forge，Mojmap）。

> 仓库：https://github.com/xm1221/HexAnime

## 这个模组做什么

手持 **Hex Casting** 的法杖，并打开咒术绘制界面（`GuiSpellcasting`）画图案时，持杖的那只手臂会指向鼠标光标，其他玩家也能看到。

- 法杖判定：Hex 的 `ItemStaff`，或物品标签 `hexcasting:staves`。
- 主手默认用右臂。副手用另一只手臂；左撇子会换成左臂。
- 光标相对绘制网格中心采样，大约 20 Hz，只有变化时才发给服务器。
- 服务器确认玩家拿着法杖、限制频率、夹紧数值，再广播给附近能看见该玩家的人。
- 使用 KosmX **playerAnimator**（优先级 1000），每帧插值，关掉界面后淡出。
- **第一人称手臂仍是原版渲染。** 自己在第三人称（F5）时能看到手臂跟随。

playerAnimator **没有**打进本模组，请向原作者 KosmX 下载。

版本 `0.0.1`（`0.0.1+1.20.1`）。

## 需要自行安装的依赖

### HexAnime 直接要求

- Minecraft 1.20.1，Java 17
- Hex Casting `>= 0.11.3`（模组 ID `hexcasting`）
- playerAnimator `>= 1.0.2-rc1+1.20`（Fabric 模组 ID `player-animator`，Forge 模组 ID `playeranimator`）。从原作者处下载，不要使用重新打包的副本
- Architectury API `>= 9.2.14`
- Cloth Config `>= 11.1.106`
- Fabric：Fabric API `>= 0.87.0+1.20.1`、Fabric Language Kotlin `>= 1.10.18+kotlin.1.9.22`
- Forge：Forge `>= 47`、Kotlin for Forge `>= 4.10.0`

### Hex Casting 还要求

**Fabric：** PAUCAL（`>= 0.6.0-pre` 且 `< 0.7.0`）、Patchouli（`>= 1.20.1-80`）、Cardinal Components API（`~5.2.1`，含 base / entity / item / block）、Inline（`>= 1.20.1-1.0.1`）、Cloth Config。

**Forge：** PAUCAL（`[0.6.0, 0.7.0)`）、Patchouli（`[1.20.1-80,)`）、Caelus（`[3.1.0+1.20,)`）、Inline（`[1.20.1-1.0.1,)`）、Kotlin for Forge。

Mod Menu 可选，用来打开配置界面。

## 构建

```bash
./gradlew build
```

需要 Java 17。模板：MieHexTemplate（Mojmap，`use_mielib=no`）。

## 许可证

MIT © xm1221
