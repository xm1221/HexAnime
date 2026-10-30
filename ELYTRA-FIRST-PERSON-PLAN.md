# 鞘翅第一人称动画适配计划

## 状态

- 记录日期：2026-10-30
- 当前状态：仅计划，尚未实现
- 适用范围：HexAnime 客户端第一人称动画
- 目标版本：Minecraft 1.20.1、Fabric / Forge

## 目标

玩家穿着鞘翅并处于滑翔状态时，继续保留 HexAnime 的第一人称动画，不直接关闭：

- 法杖瞄准、收招和第一人称弹簧保持可用。
- 封装咒术物品的脱手、旋转、前后飞行和回位继续可用。
- 进入滑翔和退出滑翔时平滑过渡，不出现瞬间跳姿势。
- 避免法杖、物品或手臂穿进镜头。
- 地面、站立和普通下落时保持现有行为。

本功能只影响本地客户端表现，不修改服务端配置、网络协议或其他玩家的远程动画。

## 当前代码入口

- `common/src/main/kotlin/cn/xm1221/hexanime/client/anim/StaffFirstPerson.kt`
  - 法杖第一人称的固定俯仰、抬升和 PoseStack 变换。
- `common/src/main/kotlin/cn/xm1221/hexanime/client/anim/CastItemAnimation.kt`
  - 第一人称模式选择、动画活动判断和第一人称物品变换。
- `common/src/main/java/cn/xm1221/hexanime/mixin/client/ItemInHandRendererMixin.java`
  - 在 `renderArmWithItem` 中执行法杖第一人称 push/pop。
- `common/src/main/kotlin/cn/xm1221/hexanime/client/anim/FirstPersonEnvelope.kt`
  - 第一人称旋转和位移的整体缩小比例。
- `common/src/main/kotlin/cn/xm1221/hexanime/config/HexAnimeClientConfig.kt`
  - 第一人称动画开关。

## 坐标约定

### 第一人称相机/手部局部空间

原版右手第一人称基准：

```text
X = +0.56   右
Y = -0.52   下
Z = -0.72   前
```

左手基准：

```text
X = -0.56
```

因此局部方向和对应关系：

| 字段 | 正方向 | 负方向 |
| --- | --- | --- |
| `X` | 向右 | 向左 |
| `Y` | 向上 | 向下 |
| `Z` | 向后、靠近玩家 | 向前、远离玩家 |
| `itemYaw` | 绕竖直 Y 轴 | 反向旋转 |
| `itemPitch` | 绕水平 X 轴 | 反向翻转 |
| `itemRoll` | 物品平面内旋转 | 反向平面旋转 |

playerAnimator 在应用原版手部变换后执行：

```text
translate(itemX / 16, itemY / 16, itemZ / 16)
rotate Z(itemRoll)
rotate Y(itemYaw)
rotate X(itemPitch)
```

因此 `itemX/itemY/itemZ` 的编写值需要通过 `firstPersonOffset()` 转换为模型像素。

### 第三人称注意项

第三人称的手持物品渲染路径与第一人称不同。胸前组的局部 Z 方向不能直接沿用第一人称值，需要在第三人称路径中单独处理。

## 分阶段实施方案

### 阶段 1：滑翔状态和过渡

新增纯逻辑模块，例如：

```text
common/src/main/kotlin/cn/xm1221/hexanime/client/anim/ElytraFirstPersonPose.kt
```

输入：

- `fallFlying`
- 滑翔 tick
- `deltaMovement`
- 相机俯仰
- 是否第一人称

输出：

- `elytraWeight`
- 第一人称俯仰修正
- 前方偏移
- 垂直偏移
- 侧倾修正

建议：

- 进入滑翔平滑约 6 tick。
- 退出滑翔平滑约 8 tick。
- 低速或刚起飞的短时间使用安全权重，避免抖动。
- 所有角度和位移设置上下限。

### 阶段 2：法杖第一人称

调整 `StaffFirstPerson.kt`：

- 滑翔时降低固定上抬量。
- 将法杖向镜头前方移动，避免贴近头部。
- 根据相机俯仰和滑翔速度做少量跟随，不直接复制完整相机旋转。
- 保留绘制咒术时的光标瞄准、弹簧和 Mishap 抑制。
- 继续使用成对的 `pushPose()` / `popPose()`。

验收重点：

- 打开咒术绘制界面时法杖仍能跟随光标。
- 俯冲、拉升和转弯时不穿模。
- 退出滑翔后恢复原来的高度和俯仰。

### 阶段 3：封装施法物品第一人称

调整 `CastItemAnimation.kt`：

- 保留 playerAnimator 的 `THIRD_PERSON_MODEL` 第一人称路径。
- 滑翔时只显示必要的施法手臂和物品，减少身体模型进入镜头。
- 对脱手、胸前悬浮和旋转动作增加鞘翅状态下的位移补偿。
- 滑翔时禁用腿部动作，避免滑翔姿势和腿部动画叠加。
- 保持物品的当前多圈旋转和回位逻辑。

第三人称如果出现方向相反，只在第三人称分支处理胸前组 Z 方向，不修改第一人称公共值。

### 阶段 4：冲突和配置

- Mishap、疲劳、相机晃动和屏幕花纹保持独立。
- 滑翔时可降低身体偏移对第一人称的影响。
- 不增加新的服务端同步字段。
- 客户端 `enabled` 或 `firstPersonAnimations` 关闭时不执行新增逻辑。

## 测试计划

### 自动化测试

新增 `ElytraFirstPersonPoseTest`，至少覆盖：

- 地面状态 `elytraWeight = 0`。
- 滑翔状态权重能够达到 1。
- 进入和退出不会瞬间跳变。
- 极低速度时没有 NaN、无限值或高频抖动。
- 俯仰、位移和侧倾都在设定范围内。
- 第一人称和第三人称使用不同的 Z 方向策略。

构建验证：

```powershell
$env:JAVA_HOME='C:\Program Files\Java\jdk-17'
.\gradlew.bat :common:test build --console=plain
```

### 实机验收

1. 地面持杖打开绘制界面，确认外观与当前版本一致。
2. 起跳并使用鞘翅，观察进入滑翔时是否平滑。
3. 滑翔中打开咒术绘制界面，确认法杖仍能跟随光标。
4. 俯冲、拉升、左右转弯，确认法杖不穿镜头、不遮挡准星。
5. 滑翔中使用封装咒术物品，确认手前飞行、胸前悬浮、旋转和回位正常。
6. 滑翔中触发法杖收招和 Mishap，确认腿部动作不叠加。
7. 关闭鞘翅滑翔，确认退出时平滑恢复。
8. 第三人称观察本地玩家，确认不会因为第一人称补偿出现反向飞行。
9. Fabric 和 Forge 各启动客户端验证一次。

## 风险和待确认

- playerAnimator 的第一人称物品偏移与第三人称手持物品层不是同一套渲染坐标。
- 鞘翅滑翔时相机俯仰不等于身体俯仰，不能直接把相机角度全量用于物品。
- 第一人称物品太靠近镜头时可能被近裁剪面裁掉，需要加入最大位移限制。
- `THIRD_PERSON_MODEL` 显示手臂时可能仍包含躯干，需要动态调整 `FirstPersonConfiguration`。
- 是否需要鞘翅专属配置开关或强度滑条，应在实机验收后决定。

## 完成标准

- 地面表现无回归。
- 滑翔进入和退出平滑。
- 法杖和封装物品第一人称动画均可用。
- 不出现明显穿模、反向飞行、抖动或 NaN。
- `common:test build` 通过。
- Fabric 和 Forge 构建通过。
- 游戏内视觉验收完成。
