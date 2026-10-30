# HexAnime WP0-WP5 交接文档

## 审查状态

- 审查日期：2026-10-30
- 审查基线：`646d08d`
- 当前提交：`4abd703580a1b21ce3430b097ecf6b5d3ff4ef4f`
- 提交标题：`WP5: add mishap fatigue, offhand ritual, and a tagged envelope`
- 分支：`main`
- 审查开始时工作区：干净，且与 `origin/main` 同步
- 当前工作区变更：只有本交接文档，且尚未纳入 Git 跟踪
- 本次审查没有修改业务源码，只新增本交接文档
- 没有执行游戏内人工验收，也没有启动 Fabric/Forge 客户端

本次审查覆盖的提交：

```text
582171b final add
8b76bc8 WP0: pin hand PoseStack ordinals and lock packaged pulse results
c8df49a WP1: close the staff cast flourish and add a cast camera hit
9af1db3 WP2: split packaged families and rewind artifact re-entry
f6d19d1 WP3: spring-damp staff aim and attenuate first person
890e98b WP4: texture the ornate staves and raise strain on real overcast
41517b5 WP1: expose the pattern client hook to the mixin
97e2763 WP3: give the wood spring a visible overshoot
4abd703 WP5: add mishap fatigue, offhand ritual, and a tagged envelope
```

## 版本与依赖

- Minecraft：`1.20.1`
- Java：`17`
- Hex Casting：`0.11.4`
- Kotlin：`2.2.21`
- Fabric Language Kotlin：`1.13.7+kotlin.2.2.21`
- Kotlin for Forge：`4.12.0`
- Gradle：`8.14`
- Architectury Loom：`1.9.436`
- Fabric Loader：`0.19.2`
- Fabric API：`0.92.8+1.20.1`
- Inline：`1.20.1-1.2.2`
- playerAnimator：`1.0.2-rc1+1.20`
- 模组版本：`0.1.0+1.20.1-SNAPSHOT`

Hex Casting 已从 `0.11.3` 更新到 `0.11.4`，仓库内没有再发现 `0.11.3` 的有效配置引用。

## 主架构

```text
CursorSampler
  -> CastAnimationClient / StaffAimRelay
  -> CastItemAnimation
  -> playerAnimator 动画层
  -> 第三方视角 / 第一人称渲染
```

核心职责：

- `CursorSampler`：打开 Hex 施法 GUI 时采样鼠标位置；本地动画直接更新，网络同步受限频和服务端配置控制。
- `StaffAimRelay`：服务端验证玩家是否真的持有支持瞄准的物品，缓存最后光标，并向跟踪玩家广播。
- `CastItemProfiles` / `CastItems`：把物品解析成动画 profile、Variant、媒质/耐久 strain。
- `CastItemAnimation`：每个客户端玩家一层 playerAnimator 动画，管理双手、法杖瞄准、施法脉冲、损坏、Mishap、疲劳和副手动作。
- `StaffCastRelay`：法杖施法成功后的收尾 flourish，来源是 Hex 的 `AttemptSpell`。
- `PackagedHexUseMixin`：封装咒术实际使用或耗尽时广播 `CAST` / `BREAK`。
- `MishapSideEffectMixin`：Hex Mishap 时广播全身事故动画。
- `OvercastStrainRelay`：捕获 Overcast 伤害并广播临时 strain。
- `CameraShakeClient`：本地相机晃动。
- `AnimationPreferences`：记录每个客户端是否接收远程瞄准和远程事件。

## 事件流

### 法杖瞄准

1. Hex 施法 GUI 打开。
2. `CursorSampler` 读取当前手持物品并解析 `CastItemState`。
3. 本地客户端直接调用 `CastItemAnimation.setAim(...)`。
4. 如果开启 `syncToServer`，发送 `MsgStaffAimC2S`。
5. `StaffAimRelay` 校验 hand、物品 profile、数值有限性和速率。
6. 通过的样本发送给同一维度、跟踪范围内的其他玩家。
7. 远程客户端通过 `MsgStaffAimS2C` 更新动画；若 30 tick 没有新样本则关闭远程瞄准。

### 法杖施法收尾

1. Hex `OperatorSideEffect.AttemptSpell.performEffect` 完成。
2. `AttemptSpellMixin` 只处理有施法音的 spell。
3. `StaffCastRelay` 检查施法手确实是非封装法杖。
4. 按玩家 10 tick debounce。
5. 读取最后一次原始光标样本，从 `StaffCastMotions` 选择四个空间方向动作之一。
6. `CastPulseRelay` 发送 `CAST` 给施法者本人和符合条件的观察者。

### 封装咒术

1. `ItemPackagedHex.use` 在服务端执行。
2. 正常返回 `SUCCESS` 时广播 `CAST`。
3. 可耗尽物品返回 `CONSUME` 时广播 `BREAK`。
4. `PackagedHexUseAnimationMixin` 把原版 `UseAnim.BLOCK` 改为 `NONE`。
5. 客户端 `triggerPulse` 用 profile 和 Variant 选择动作。
6. Artifact 的 `PulseStyle.reentry` 允许连续施法时回卷到动作约 22% 进度，而不是完全重新开始。

### Mishap / Overcast / 相机

1. `DoMishap.performEffect` HEAD 时广播 `MISHAP`。
2. 客户端播放强化后的 Mishap 全身动作，并在动作结束后启动 10 tick 疲劳曲线。
3. Mishap 期间第一人称使用 playerAnimator 的 `THIRD_PERSON_MODEL`。
4. `OvercastStrainRelay` 监听 Overcast 伤害，发送 `MsgOvercastStrainS2C`。
5. 客户端把 Overcast strain 与物品自身的媒质/耐久 strain 取最大值。
6. `CameraShakeClient` 只在施法者本地接收 `CAST`、`BREAK`、`MISHAP` 时触发。

## 已确认问题

### P1：空闲法杖的 Overcast 不能可靠激活动画层

证据：

- `setOvercast` 只写入 `overcastInitial` 和 `overcastStartTick`。
- `isActive()` 没有检查 `overcastNow(...)`。
- `hasDamageMotion()` 只看 `state.strain`，而普通法杖通常没有耐久/媒质 strain。
- `frame()` 确实计算了 `overcastNow(...)`，但只有动画层已经运行时才会稳定参与渲染。

相关位置：

- `common/src/main/kotlin/cn/xm1221/hexanime/client/anim/CastItemAnimation.kt:96`
- `common/src/main/kotlin/cn/xm1221/hexanime/client/anim/CastItemAnimation.kt:116`
- `common/src/main/kotlin/cn/xm1221/hexanime/client/anim/CastItemAnimation.kt:217`
- `common/src/main/kotlin/cn/xm1221/hexanime/client/anim/CastItemAnimation.kt:567`

影响：

- 玩家空闲持杖时受到 Overcast 伤害，粒子可能由 `StaffTipParticles` 单独显示，但法杖/身体的 Overcast 颤抖不一定启动。
- 如果玩家正在瞄准或施法，动画层已经 active，问题不明显。

建议：

- 把当前有效 Overcast strain 纳入 `isActive()` 和/或 `hasDamageMotion()`。
- 增加一个纯逻辑测试，覆盖“空闲法杖 + 高 Overcast strain”。

### P2：Overcast 使用 `LIVING_HURT`，它发生在伤害最终结算之前

Architectury 9.2.14 的 `EntityEvent.LIVING_HURT` 文档明确说明它发生在实体受伤前，等价于 Forge 的 `LivingAttackEvent`，不能确认伤害最终是否被取消或修改。

相关位置：

- `common/src/main/kotlin/cn/xm1221/hexanime/casting/OvercastStrainRelay.kt:12`

影响：

- 其他模组、无敌帧、免疫或其他监听器取消/缩小伤害时，HexAnime 仍可能播放 Overcast 反馈。
- `amount` 是受伤前参数，不一定是最终生命损失。

建议：

- 优先寻找 Hex 自身真正执行 Overcast 的路径，或在平台提供的“伤害后”事件中广播。
- 如果必须沿用当前事件，至少把“预结算 strain”作为产品定义写清楚，并验证取消伤害场景。

### P2：服务端允许远程瞄准刷新间隔大于客户端 30 tick 超时

证据：

- `CastItemAnimation.REMOTE_AIM_TIMEOUT_TICKS = 30f`
- 服务端 `refreshTicks()` 允许 `5..100`
- `StaffAimRelay.refresh()` 只有在刷新 tick 才补发 active 状态

相关位置：

- `common/src/main/kotlin/cn/xm1221/hexanime/client/anim/CastItemAnimation.kt:132`
- `common/src/main/kotlin/cn/xm1221/hexanime/client/anim/CastItemAnimation.kt:935`
- `common/src/main/kotlin/cn/xm1221/hexanime/config/HexAnimeServerConfig.kt:78`
- `common/src/main/kotlin/cn/xm1221/hexanime/staff/StaffAimRelay.kt:105`

影响：

- 光标静止且 `refreshIntervalTicks > 30` 时，远程观察者会先超时关闭瞄准，再等下一次刷新恢复，产生周期闪烁。

建议：

- 将刷新间隔上限限制为 20，或至少不超过远程 timeout。
- 或者让 timeout 根据服务端同步配置动态计算，并使用安全余量。

### P2：`firstPersonAnimations = false` 仍会走法杖第一人称自定义路径

证据：

- 法杖瞄准时 `getFirstPersonMode()` 直接返回 `NONE`，没有检查 `firstPersonAnimations`。
- `StaffFirstPerson.pushAim()` 只检查物品、profile、aim weight 和 Mishap，没有检查该开关。
- `NONE` 的语义是“playerAnimator 不接管第一人称”，因此原版手部路径仍会运行，HexAnime 仍可修改手部 PoseStack。

相关位置：

- `common/src/main/kotlin/cn/xm1221/hexanime/client/anim/CastItemAnimation.kt:444`
- `common/src/main/kotlin/cn/xm1221/hexanime/client/anim/StaffFirstPerson.kt:26`

说明：

- 如果产品定义是“该开关只控制封装咒术的全模型第一人称，不控制法杖瞄准”，当前行为是正确的。
- 如果开关名称按字面表示所有第一人称动画，这是配置契约缺陷。

建议：

- 在交接或配置 tooltip 中明确语义。
- 如果开关应覆盖法杖，则在 `getFirstPersonMode()` 或 `pushAim()` 中统一拦截。

### P2：第三方“法杖”扩展仍被 `StaffMaterial` 硬编码限制

证据：

- 只要 `staffMaterial == NONE`，以下路径就拒绝或绕过法杖逻辑：
  - `CastPulseRelay` 的 staff pulse 分类
  - `StaffCastRelay` 的 AttemptSpell flourish
  - `StaffPatternClient` 的本地微脉冲/收尾
  - `CastItemAnimation` 的 Overcast、chaos、副手 ritual

相关位置：

- `common/src/main/kotlin/cn/xm1221/hexanime/casting/CastPulseRelay.kt:48`
- `common/src/main/kotlin/cn/xm1221/hexanime/casting/StaffCastRelay.kt:34`
- `common/src/main/kotlin/cn/xm1221/hexanime/client/StaffPatternClient.kt:32`
- `common/src/main/kotlin/cn/xm1221/hexanime/client/anim/CastItemAnimation.kt:109`

影响：

- `CastItemProfiles.register(...)` 看起来是公开扩展点，但第三方注册一个 `supportsAim = true`、`staffMaterial = NONE` 的自定义法杖时，瞄准可能工作，施法收尾、Overcast 和仪式副手动作不会工作。

建议：

- 在 profile 中增加“animatesAsStaff”或明确的 family capability，而不是把 `StaffMaterial.NONE` 同时当作“非法杖”和“无材质”。
- 至少给第三方法杖保留 `StaffMaterial.TAGGED` 之外的配置入口。

### P3：Mishap 抑制会在未发送 Mishap 的情况下仍生效

证据：

- `MISHAP` 在配置检查和手持物品检查之前就写入 `recentMishapUntil`。
- 因此 `mishapPulse = false`、没有可识别物品或 Mishap 被其他条件丢弃时，仍可能抑制随后的 `CAST` / `BREAK`。
- 抑制键只按玩家 UUID，不区分手。

相关位置：

- `common/src/main/kotlin/cn/xm1221/hexanime/casting/CastPulseRelay.kt:32`

影响：

- 误吞施法动作或断裂动作，尤其是在双持和服务端关闭 Mishap 动画时。

建议：

- 只有 Mishap 事件真正通过校验并准备发送后才写入抑制窗口。
- 如果保留全局抑制，至少明确它只针对同一只手和同一施法序列。

### P3：文档和生成配置仍有删除后的残留引用

证据：

- `doc/hexdoc.toml` 仍指向已删除的 `registry/HexAnimeActions.kt`。
- `README.zh-CN.md` 仍提到当前源码中不存在的 `PulsePose`。

相关位置：

- `doc/hexdoc.toml:36`
- `README.zh-CN.md:25`

影响：

- hexdoc 或文档生成会失败，或者继续展示过期架构。

建议：

- 删除对应 pattern stub 配置，或重新生成有效的 hexdoc 配置。
- 把 `PulsePose` 更新为当前实际使用的 `PulseStyle` / `VariantMotion` 术语。

### P3：格式检查有文件末尾空行

`git diff --check 646d08d..HEAD` 报告：

```text
common/src/main/kotlin/cn/xm1221/hexanime/client/anim/StaffDisplay.kt:61: new blank line at EOF.
```

这是格式问题，不影响构建。本次没有修改。

## 已核对但未发现问题的点

- `MsgSyncConfigS2C` 的 `ServerConfig.encode()` / `decode()` 字段顺序一致，`staffPulse` 追加在末尾且两端顺序一致。
- `MsgCastPulseS2C` 的 encode/decode 顺序一致。
- `MsgNewSpellPatternMixin` 在 `handle` HEAD 把本地回调排入客户端任务队列；Hex 自己的 `handle` 随后也排入任务队列，因此先入队的 HexAnime 回调通常仍在 Hex 应用同一 packet 之前运行。这个注释不是明显的错误。
- 法杖 `CAST` 的本地备份和 server pulse 有同类型活动 pulse 去重，正常情况下不会因为 GUI 关闭再次完整重播。
- `AttemptSpellMixin` 对封装物品会由 `StaffCastRelay` 排除，封装咒术的动作主要由 `PackagedHexUseMixin` 发出。
- 最终 Fabric/Forge JAR 都包含新增 mixin、网络消息和 `en_us` / `zh_cn` 语言资源。
- Fabric JAR 的 `hexcasting >= 0.11.4`、`player-animator >= 1.0.2-rc1+1.20` 依赖元数据正确。
- Forge JAR 的 `hexcasting [0.11.4,)`、`playeranimator [1.0.2-rc1+1.20,)` 依赖元数据正确。

## 仍需游戏内人工确认

以下内容无法由构建或纯单元测试证明：

- Mishap 的 `bodyPitch` / `bodyZ` 符号是否确实表现为“向后倒”，而不是向前。
- 法杖是否保持竖直，位置是否足够高，肩膀高度是否始终自然。
- 第一人称左右方向是否与光标一致，尤其是左撇子和副手。
- 大幅动作是否穿模、超出屏幕、遮挡施法网格或被玩家模型自身遮挡。
- 连续施法时 Artifact reentry 是否自然，是否会形成明显重复起手。
- 法杖在空闲 Overcast 状态下是否还需要被 P1 的 `isActive` 修复后才能看到。
- `firstPersonAnimations = false` 的产品预期到底是否包含法杖瞄准路径。
- 双持、切换主副手、切换物品、死亡和换维度时，pulse/fatigue/remote timeout 是否残留。
- 原版 `BLOCK` 使用动画关闭后，封装物品在长按、冷却和连续使用时的视觉是否一致。
- 服务端关闭各类同步后，客户端本地动画和远程动画是否符合预期，并确认带宽确实下降。

## 构建与测试结果

使用的命令：

```powershell
$env:JAVA_HOME='C:\Program Files\Java\jdk-17'
.\gradlew.bat :common:test build --console=plain
```

实际结果：

- `:common:test`：通过
- 共 30 个测试，0 failures，0 errors，0 skipped
- `:fabric:build`：通过
- `:forge:build`：通过
- `common` production transform、Fabric remap、Forge remap：通过
- 构建时间约 55 秒

构建输出只有非阻断警告：

- Architectury Loom `1.9.436` 被标记为 outdated。
- Gradle 报告使用了会在 Gradle 9 失效的 deprecated features。

没有启动或验证以下运行时流程：

- Fabric client
- Forge client
- 专用服务器
- 双客户端远程同步
- 真实 Hex Mishap / Overcast / 连续施法

## 产物

本次构建生成了：

```text
fabric/build/libs/hexanime-fabric-0.1.0+1.20.1-SNAPSHOT.jar
forge/build/libs/hexanime-forge-0.1.0+1.20.1-SNAPSHOT.jar
```

这些是本地构建产物，不是部署动作。

## 建议的后续顺序

1. 先修空闲 Overcast 的动画层激活问题，并增加空闲法杖测试。
2. 再决定 Overcast 是否改用 Hex 内部路径或平台伤害后事件，避免预取消误报。
3. 统一远程瞄准刷新上限和 30 tick timeout。
4. 明确 `firstPersonAnimations` 的语义；如果开关应覆盖法杖，统一拦截自定义第一人称路径。
5. 把“法杖能力”从 `StaffMaterial.NONE` 中解耦，给第三方 profile 一个正式扩展点。
6. 把 Mishap 抑制窗口移到事件通过校验之后再记录，并区分手/施法序列。
7. 清理 `doc/hexdoc.toml`、README 中的过期术语和 `StaffDisplay.kt` 末尾空行。
8. 最后做一次 Fabric/Forge 双客户端人工验收，重点观察 Mishap 方向、法杖高度、第一人称方向、长按连续施法和远程超时。

## 交接边界

本次审查是只读审查。除本文件外没有应用任何源码、资源、构建脚本或版本修改。用户之前明确要求先检查、暂不修改；后续若要修复，应从上面的 P1/P2 项开始，而不是直接重做整套动画参数。
