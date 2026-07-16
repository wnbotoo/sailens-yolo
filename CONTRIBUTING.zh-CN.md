[English](CONTRIBUTING.md) | **简体中文**

# 向 Sailens YOLO Edition 贡献

本仓库是 Sailens 的 **YOLO Edition**，许可证为 **AGPL-3.0**。

上游 Core Edition：

```text
https://github.com/wnbotoo/sailens-android
```

请先阅读：

```text
docs/repository-license-strategy.zh-CN.md
YOLO_EDITION_NOTICE.zh-CN.md
```

## 决定在哪贡献之前，先读这段

多数人来到这里是因为**这个版本是真能跑的那个**——它带权重，上游不带。但这不构成在这里开发的理由。

**想改行为，去上游改。** 你不需要在本仓库工作才能得到一个能跑的 app：把你自己的权重丢进上游的
`data/src/main/assets/`（那个路径已被 gitignore），它就跑起来了。本地构建和测试**不是分发**，
因此不产生任何 AGPL 义务。详见
[`docs/repository-license-strategy.zh-CN.md`](docs/repository-license-strategy.zh-CN.md)
的「Development workflow」一节——那节存在的原因就是这个问题总被反复重新论证。

**而且这里几乎没什么可改的。** 本 Edition 的全部增量是两个模型文件加上这些治理文档，源码是上游的、
一字未改。你在这里能编辑的任何东西，都是躺在下游 checkout 里的上游代码。

## 本仓库接受的贡献

只接受 YOLO Edition 相关贡献：

```text
1. 从 sailens-android 同步上游代码
2. 更新 YOLO 模型
3. 更新 YOLO 专属配置
4. 更新 YOLO 专属后处理
5. 修复 YOLO Edition 专属构建问题
6. 更新 AGPL-3.0 notice
7. 更新模型来源、版本和 license 文档
```

## 本仓库不接受的贡献

通用功能不要首发在本仓库。以下改动应提交到 Core Edition：

```text
1. 通用 UI 功能
2. 通用 Camera pipeline
3. 通用 Perception pipeline
4. 通用 ModelRunner 抽象
5. 通用调度器、风险分析、Scene fusion、Event engine、Speech engine
6. 与 YOLO 无关的 bug fix
7. 文档或测试的通用改进
```

## 回迁规则

默认禁止：

```text
sailens-yolo -> sailens-android
```

**这条是承重规则，不是整洁癖。** 你在这里贡献的代码，是以 AGPL-3.0 授权给维护者的，而 AGPL-3.0
代码无法被重新授权为 Apache-2.0。一旦落在这里，它就**永远回不到 Core Edition**——两个仓库会分叉，
上游的 Apache-2.0 保证会被破坏。而那个保证正是上游存在的全部理由。

要把本仓库的任何内容挪到 Core Edition，必须先完成 license review 并确认：

```text
1. 不包含 AGPL-covered 内容
2. 不依赖 YOLO 模型或 Ultralytics 代码
3. 不是从 AGPL 源码复制、翻译或派生而来
4. 可明确以 Apache-2.0 授权
5. 属于真正的通用功能
```

默认建议：需要通用能力时，在 Core Edition 中重新实现。

> **给版权所有者的注记：** 这条规则约束的是**第三方**贡献。作者不受自己 outbound license 的约束。
> 见 [`docs/repository-license-strategy.zh-CN.md`](docs/repository-license-strategy.zh-CN.md) §7.5。

## 模型更新要求

更新或新增模型文件的 PR，必须记录全部十项：

```text
1. 模型名称
2. 模型版本
3. 下载来源
4. 上游项目地址
5. 代码 license
6. 权重 license
7. 训练数据集 license
8. 是否允许再分发
9. 是否允许商业使用
10. 导出格式和推理 runtime
```

同时必须：

- 保持 `TfliteModelMetadataReaderTest` 绿——它是本 Edition 的契约闸。
- **用真实场景人工验证类别通道顺序。** 没有任何测试能校验这一点：契约只校验通道**数量**。
  shape 正确但类别顺序不同的模型会毫无报错地运行，然后静默地把世界讲错给一个看不见的人听。
  **这是安全闸，不是文书工作。**
- 更新 `docs/yolo-models.zh-CN.md`（含文件的 sha256）。

## Pull Request Checklist

```text
- [ ] This change is specific to the AGPL-3.0 YOLO Edition.
- [ ] This change is compatible with AGPL-3.0 distribution.
- [ ] This change is not intended to be merged back into the Apache-2.0 Core Edition.
- [ ] If model files changed, their source, version, license, and redistribution terms are documented.
- [ ] If model files changed, class channel order was verified manually on a real scene.
- [ ] If this syncs from Core Edition, the upstream commit SHA is documented.
```
