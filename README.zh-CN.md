# HexAnime

Minecraft 1.20.1 玩家动画模组，基于 Architectury，支持 Fabric 与 Forge。作者 xm1221。

仓库：https://github.com/xm1221/HexAnime

## 功能

- 打开 Hex Casting 绘制界面时，法杖保持近身竖直姿态，光标只让手腕和手臂做小幅倾斜；第一人称和第三人称都有对应动作。
- 封装咒术物品真正释放时播放一次性施法动作，包括 Artifact、Trinket、Cypher 和 Ancient Cypher。
- 八个 Hex 视觉 Variant 各自选择不同的全身动作；物品种类只用于判断它是不是封装咒术。
- 媒质耗尽或物品耐久较低时，手臂和物品会持续颤抖、下垂并触发事件；Cypher 最后一次耗尽有更强的断裂反馈。
- Hex Mishap 会触发统一的全身反冲和更强烈的第一人称晃动。
- 部分 Variant 使用不死图腾式爆点：短暂压缩、整体上弹、双臂张开、定格后回落。
- 封装咒术一次性施法使用分阶段关键帧，其第一人称直接渲染玩家模型的手臂和物品；法杖瞄准保留原来的原版手部第一人称路径。
- 法杖瞄准恢复直接归一化映射和固定平滑，不使用后期加入的径向或死区处理。
- 客户端可分别关闭本地动画、上传自身状态、接收连续瞄准或接收远程事件；服务端也可按同步类别全局关闭。

playerAnimator **没有**打进本模组，请向原作者 KosmX 下载。

## 动画结构

- `CastItemProfiles`：物品到动画档案的有序注册表，按优先级匹配，可继续注册第三方物品；第三方法杖可只设置 `animatesAsStaff`，不必伪造内置杖体材质。
- 法杖瞄准固定使用原始的 yaw/pitch 比例，封装咒术不使用光标瞄准。
- `PulseStyle` / `VariantMotion`：封装咒术释放与耗尽的复用时长，以及对应的全身关键帧。
- `DamagePose`：低媒质或低耐久时的颤抖、下垂参数。
- `CastItemAnimation`：每个玩家一层 playerAnimator 动画，同时管理主手和副手。

## 依赖

以下都是硬依赖。缺少时游戏会拒绝加载。

- Minecraft 1.20.1，Java 17
- Hex Casting `>= 0.11.4`
- playerAnimator `>= 1.0.2-rc1+1.20`
- Architectury API `>= 9.2.14`
- Cloth Config `>= 11.1.106`
- Fabric：Loader `>= 0.16.9`、Fabric API `>= 0.92.8+1.20.1`、Fabric Language Kotlin `>= 1.13.7+kotlin.2.2.21`
- Forge：Forge `>= 47`、Kotlin for Forge `>= 4.12.0`

Hex Casting 自身还需要 PAUCAL、Patchouli、Inline `>= 1.20.1-1.2.2`，以及 Fabric 的 Cardinal Components 或 Forge 的 Caelus。Mod Menu 是可选的 Fabric 配置入口。

## 构建

```bash
./gradlew build
```

需要 Java 17。项目模板来自 MieHexTemplate，许可证为 MIT。
