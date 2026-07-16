[English](README.md) | **简体中文**

# Sailens YOLO Edition

**Sailens** —— *Smart AI LENS*。

本仓库是 Sailens 的 **AGPL-3.0 YOLO Edition**：Apache-2.0 的 Core Edition 加上 YOLO26 模型权重。
因为那些权重受 AGPL 覆盖，所以本仓库按 AGPL-3.0 分发。

> **与 Ultralytics 无隶属、未获其背书或赞助。** "YOLO" 在此为描述性使用，用于标识本 Edition
> 集成的模型。

```text
Core Edition:
  https://github.com/wnbotoo/sailens-android
  License: Apache-2.0

YOLO Edition:
  https://github.com/wnbotoo/sailens-yolo
  License: AGPL-3.0
```

## Sailens 是什么

面向盲人与低视力用户的 Android 导航辅助。摄像头驱动的感知管线把前方场景变成语音和触觉反馈：
哪里能走、路沿在哪、什么东西挡着。

架构、模型契约、NPU 接线、trace 工具全部在上游，同步到这里时一字未改——见
[`AGENTS.md`](AGENTS.md) 和 [`docs/`](docs/)。

## 本 Edition 加了什么

只有两样，刻意不多加：

```text
data/src/main/assets/sem.tflite     yolo26n-sem，语义分割
data/src/main/assets/det.tflite     yolo26n，障碍物检测
```

两者的来源、许可证和数据集条款：[`docs/yolo-models.zh-CN.md`](docs/yolo-models.zh-CN.md)。

Core Edition **不带任何权重**——它解析这两个资产路径，并在加载时从模型 metadata 读取 shape、
layout、dtype 和量化参数。所以本 Edition **零代码改动**：它就是上游的树，加两个二进制，加这些
治理文档。

## 为什么这么分

Core Edition 把应用、感知管线和模型运行器抽象保持在 Apache-2.0 之下，以便嵌入闭源产品。
本 Edition 的存在是为了让 app 今天就能带着可用模型出货，并承担随之而来的 AGPL 义务。

## 开发模型

```text
sailens-android  ->  sailens-yolo
Apache-2.0 Core  ->  AGPL-3.0 YOLO Edition
```

**通用开发一律先在 Core Edition 做。** 本仓库只应接收：

```text
1. 从 sailens-android 同步上游代码
2. 更新 YOLO 模型
3. 更新 YOLO 专属配置
4. 更新 YOLO 专属后处理或构建接线
5. YOLO Edition 发布维护
6. 更新 AGPL 与第三方 license notice
```

不要把本仓库的代码合回 `sailens-android`，除非 license review 确认该改动不含 AGPL-covered 内容
且可明确以 Apache-2.0 授权。

> **如果你是版权所有者，先读
> [`docs/repository-license-strategy.zh-CN.md`](docs/repository-license-strategy.zh-CN.md)
> 的「Development workflow」一节。** 上面那条规则约束的是**第三方**贡献。它不意味着你必须在这里
> 开发才能得到能跑的 app——你不必，而且那样反而是更难的路。

## 构建

与上游相同；权重已经就位。

```bash
./gradlew build
./gradlew :app:assembleDebug
```

## 从上游同步

每个 clone 一次性设置（merge driver 无法提交进仓库，所以光有 `.gitattributes` 不够）：

```bash
git remote add upstream git@github.com:wnbotoo/sailens-android.git
git config merge.ours.driver true
```

之后每次同步就是：

```bash
git fetch upstream
git merge upstream/main
```

在 merge commit 信息里记录上游的 commit SHA；PR checklist 会要。

### 为什么用 merge 而不是 rebase

**merge 会记住。** 冲突解过一次之后，那个 merge commit 成为共同祖先，同样的 hunk 不会再冲突。
rebase 每次都把本 Edition 的提交在上游 HEAD 上从头重放，所以**你要在每一次同步里把同样的冲突
重新解一遍，永远**——它让冲突变多，不是变少。

rebase 还会重写已发布的历史，也就是要 force-push 一个"全部职责就是为 AGPL 合规提供稳定可审计
源码"的仓库。而 merge commit 结构性地记录了「本 Edition 的状态 X + 上游的状态 Y」，那正是发布
需要的审计线索。

### 为什么同步通常很安静

这里几乎没有东西与上游重叠。权重、`docs/yolo-models.md`、`docs/repository-license-strategy.md`、
`CONTRIBUTING*.md`、`YOLO_EDITION_NOTICE*.md`、`.github/`、`TfliteModelMetadataReaderTest`
在上游都没有对应文件，所以不可能冲突。

真正重叠的那几个：

- `README.md`、`README.zh-CN.md`、`LICENSE`、`NOTICE` —— 由 `.gitattributes` 里的 `merge=ours`
  规则自动解决。取舍见那个文件。
- `AGENTS.md` —— 本 Edition 的增量是标记行之上的**纯前缀**，标记行以下是上游原文，
  所以上游的修改能干净合入。**保持这样：永远不要在这里编辑上游的正文。**
- `app/build.gradle.kts`、`app/src/main/res/values/strings.xml` —— 各自只有几行；
  除非上游改到同样的行，否则能干净合入。

### 🔴 上游的 `main` 必须是 append-only

**永远不要重写上游已发布的历史**（不要 squash 后 force-push，不要 rebase `main`）。

本 Edition 的 ancestry 正是同步能工作的前提。上游一旦 force-push，本 Edition 所基于的那个提交
就变成孤儿，merge base 退化到初始提交，git 拿**空树**当基准比——**整个仓库的每个文件同时冲突**。
解决一次并不能修好它：坏掉的 base 还在，之后每次同步都会再来一遍。

万一真发生了，正确的修法是把本 Edition 重新挂到上游的新提交上——**一次性修复，不是工作流**：

```bash
git tag backup-before-reparent main          # 永远先做这步
git fetch upstream
git checkout --detach <上游的新提交>
git read-tree -u --reset main                # 原样保留本 Edition 的树
git diff backup-before-reparent --stat       # 必须为空：内容不变
git commit -m "yolo edition: ..."            # 同样的内容，正确的父提交
git branch -f main HEAD && git checkout main
git push --force-with-lease origin main
```

之后务必验证 `git merge-base main upstream/main`：它必须是上游的 tip，不能是初始提交。

## 文档

本 Edition 自有的文档：

| | | |
|---|---|---|
| [`docs/yolo-models.zh-CN.md`](docs/yolo-models.zh-CN.md) | [English](docs/yolo-models.md) | 打包权重的来源与许可 |
| [`docs/repository-license-strategy.zh-CN.md`](docs/repository-license-strategy.zh-CN.md) | [English](docs/repository-license-strategy.md) | AGPL 分发规则、同步方向、**开发工作流** |
| [`YOLO_EDITION_NOTICE.zh-CN.md`](YOLO_EDITION_NOTICE.zh-CN.md) | [English](YOLO_EDITION_NOTICE.md) | Edition 声明与模型记录要求 |
| [`CONTRIBUTING.zh-CN.md`](CONTRIBUTING.zh-CN.md) | [English](CONTRIBUTING.md) | 本仓库接受什么，以及别的该去哪 |

`docs/` 下除上面两份之外的全部文档都来自上游、同步时未改——模型契约、感知挡位、NPU 接线、
trace 指标。每份都有中英两版。

`LICENSE` 和 `NOTICE` 不做翻译：只有英文原文具有法律效力，非官方译本反而会制造「以哪份为准」的歧义。

## 发布必须提供的声明

每次 YOLO Edition 发布必须提供：

```text
1. 完整对应源码
2. 构建说明与脚本
3. AGPL-3.0 license 文件
4. YOLO 模型来源、版本与 license 声明
5. 第三方依赖 license 声明
6. 与发布构建对应的 git tag
```

见 [`docs/repository-license-strategy.zh-CN.md`](docs/repository-license-strategy.zh-CN.md)、
[`YOLO_EDITION_NOTICE.zh-CN.md`](YOLO_EDITION_NOTICE.zh-CN.md)、
[`CONTRIBUTING.zh-CN.md`](CONTRIBUTING.zh-CN.md)。

## 许可证

AGPL-3.0 —— 见 [LICENSE](LICENSE)。

上游代码是 The Sailens Authors 的 Apache-2.0；本 Edition 的**组合**分发物是 AGPL-3.0。
见 [NOTICE](NOTICE)。
