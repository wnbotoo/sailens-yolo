[English](repository-license-strategy.md) | **简体中文**

# Repository License Strategy: YOLO Edition

> 本文档定义 `sailens-yolo` 的 AGPL-3.0 发行规则。本文档不是法律意见；正式发布前，应由熟悉开源许可证的律师或合规顾问审阅最终仓库结构、构建产物、依赖、模型权重和发布流程。

## 1. 仓库定位

```text
Repository: https://github.com/wnbotoo/sailens-yolo
Edition: YOLO Edition
License: AGPL-3.0
Upstream Core Edition: https://github.com/wnbotoo/sailens-android
Upstream License: Apache-2.0
```

`Sailens YOLO Edition` 是 `Sailens Core Edition` 的下游发行版。

Core Edition 保持 Apache-2.0 边界，YOLO Edition 可以加入 YOLO 模型、YOLO 专属配置、YOLO 后处理和 YOLO 发行能力，因此本仓库按 AGPL-3.0 发布。

## 2. 同步方向

允许：

```text
sailens-android  ->  sailens-yolo
Apache-2.0 Core  ->  AGPL-3.0 YOLO Edition
```

默认禁止：

```text
sailens-yolo  ->  sailens-android
AGPL-3.0      ->  Apache-2.0
```

本仓库中的代码、模型、配置或构建逻辑不得合并回 `sailens-android`，除非经过 license review，并确认相关内容不包含 AGPL-covered material，且可以明确以 Apache-2.0 授权。

## 3. 本仓库允许的内容

```text
1. 从 sailens-android 同步来的通用代码
2. YOLO 模型权重
3. YOLO 专属模型配置
4. YOLO 专属后处理逻辑
5. YOLO Edition 构建配置
6. YOLO 模型来源、版本、license 和 notice
7. AGPL-3.0 源码提供说明
8. 第三方依赖 license notice
```

## 4. 本仓库不做的事情

```text
1. 不首发通用产品功能
2. 不作为 Core Edition 的上游
3. 不把 AGPL 专属实现回迁到 Core Edition
4. 不隐藏 YOLO 模型来源或许可证
5. 不发布无法对应到源码 tag 的构建产物
```

通用功能开发应提交到 `sailens-android`。

## 5. 发布要求

每次发布 YOLO Edition App 前，应确认：

```text
1. `LICENSE` 是 AGPL-3.0。
2. 发布 tag 已创建。
3. 完整源码可获取。
4. 构建脚本和构建说明可获取。
5. YOLO 模型来源、版本、下载地址和 license 已记录。
6. 第三方依赖 license notice 已记录。
7. App 内关于页面显示 YOLO Edition、AGPL-3.0、源码链接和模型 license。
```

## 6. 实际目录结构

> 本节原先推荐 `yolo-edition/` 目录或 `app/src/yolo/` source set。**都不需要了**，因为 Core Edition
> 最终采用了 bring-your-own-model 设计：它自己不带权重，只解析两个固定资产路径，并从 TFLite metadata
> 自动读取 shape/layout/dtype/量化。所以本 Edition 的全部增量就是两个二进制：

```text
data/src/main/assets/sem.tflite      Core 的 sem 槽位
data/src/main/assets/det.tflite      Core 的 det 槽位
```

Core Edition 的 `.gitignore` 忽略 `data/src/main/assets/*.tflite`；本仓库用 `git add -f` 加入一次，
之后即永久 tracked，**两仓在权重上永不冲突，且 Core 不需要为本仓库添加任何东西**（满足 knowledge
boundary）。

**代码增量为零。**不要为了"集中放置"去新建 YOLO 专属目录或 source set —— 没有 YOLO 专属代码可放。
如果哪天真出现了（例如换了个输出布局不同的模型，需要新解码分支），**那个分支应该进 Core Edition**：
支持一种张量布局是格式事实，不是 AGPL 衍生物，Core 收得下。只有确实无法与 Core 解耦的东西才留在这里。

## 7. 回迁规则

默认不要从本仓库回迁代码到 Core Edition。

如果确实需要回迁，必须确认：

```text
1. 不包含 YOLO 模型权重。
2. 不包含 Ultralytics AGPL 代码。
3. 不包含从 AGPL 代码复制、翻译或派生的实现。
4. 不依赖 YOLO Edition-only source set、目录或配置。
5. 可明确以 Apache-2.0 授权。
6. 回迁内容属于通用功能，而不是 YOLO Edition 专属功能。
```

默认建议：需要通用能力时，在 Core Edition 中重新实现。

**这条规则的适用对象是第三方贡献者，不是版权所有者。**见下一节。

## 7.5 Development workflow（读这节，别自己推演）

> 本节不是法律意见。但它回答一个每隔几周就会被重新论证一遍的问题：
> **"开发该在哪做？A 没模型没法调试，B 又回不到 A。"**
> 答案是：**这个两难大部分是幻觉。**

### 结论：只在 Core Edition 上开发，权重放本地工作区

```text
日常开发   只在 sailens-android。把权重丢进它的 data/src/main/assets/（已 gitignore），
           真机调试照常。权重永远进不了 A 的 git。
本仓库     只在发版时 git merge upstream/main，零代码改动。
第三方 PR  一律去 Core Edition。本仓库只收模型更新。
```

**别搞两个 checkout 来回搬代码**——那才会真的把自己绕进"这段算不算 AGPL"的泥潭。

### 为什么这样不触发 AGPL

**AGPL 的义务全部挂在 conveying（分发）上。** GPL-3.0 §2 明确允许你不受任何条件地运行和修改
**你不分发**的作品；AGPL 额外加的 §13 触发条件是"用户通过网络与修改版交互"——本地真机调试一个
Android app 不沾边。

所以：**在 Core Edition 的工作区里放 AGPL 权重做开发和调试，零 AGPL 义务，因为你没有分发。**
Core Edition 的 `.gitignore` 保证那些权重进不了提交。开发在 A、调试有模型、A 仓库干净，
三件事同时成立。

唯一真正的约束是**分发**：带权重的构建产物不能就那样发出去，发布走本仓库（AGPL）。

### 为什么 owner 不受"AGPL 回不到 A"约束

那条规则的完整版是「**第三方的**代码贡献必须去 A」：第三方在本仓库提 PR，本仓库 LICENSE 是
AGPL-3.0，所以他授予维护者的是 AGPL 版权许可，没法把它挪进 Apache-2.0 的 Core Edition。

**但版权所有者不受自己 outbound license 的约束。** 你写的代码你持有版权；它不会因为你是在一个恰好
躺着 AGPL 权重的目录里写的，就变成 AGPL。outbound license 是你**授予别人**的，不是套在自己脖子上的。

### 而且本仓库几乎没有可开发的东西

本 Edition 的增量 = 2 个权重 + 治理文档，**零代码**，两仓源码 100% 相同。所以"在 B 上开发新
feature"这个场景基本不存在——你在这里能改的任何代码都是 Core Edition 的代码。真正 B-only 的
工作只有"换个权重文件"。

## 8. 长期原则

```text
1. Core Edition 是主线。
2. YOLO Edition 是下游 AGPL 发行版。
3. 通用开发进 Core Edition。
4. YOLO 专属开发留在 YOLO Edition。
5. A -> B 单向同步。
6. B -> A 默认禁止。
7. 每个模型都必须记录 code license、weights license、dataset license 和 redistribution terms。
```
