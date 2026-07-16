[English](YOLO_EDITION_NOTICE.md) | **简体中文**

# YOLO Edition 声明

本仓库是 **Sailens YOLO Edition**，按 **AGPL-3.0** 分发。

> **与 Ultralytics 无隶属、未获其背书或赞助。** "YOLO" 在此为描述性使用，用于标识本 Edition
> 集成的模型。Ultralytics 的商标（若存在）归其所有者；此处任何内容都不暗示他们的审阅、批准或背书。

## 与 Core Edition 的关系

```text
Core Edition:
  Repository: https://github.com/wnbotoo/sailens-android
  License: Apache-2.0

YOLO Edition:
  Repository: https://github.com/wnbotoo/sailens-yolo
  License: AGPL-3.0
```

YOLO Edition 是 Core Edition 的下游发行版。通用功能开发应先在 Core Edition 进行，然后向下游同步。

## 许可边界

本仓库可以包含 YOLO 专属的模型文件、配置、后处理和发布接线。因为本 Edition 可能包含
AGPL-covered 的 YOLO 模型和/或集成，所以本仓库及其分发的 app 构建物按 AGPL-3.0 发布。

不要把 YOLO Edition 的代码合回 Core Edition，除非 license review 确认该内容不含 AGPL-covered
材料且可明确以 Apache-2.0 授权。

> 这条约束的是**第三方**贡献。版权所有者不受自己 outbound license 的约束——见
> [`docs/repository-license-strategy.zh-CN.md`](docs/repository-license-strategy.zh-CN.md) §7.5。

## 模型声明要求

本仓库或任一 YOLO Edition 发布中包含的每个 YOLO 模型，都必须记录：

```text
1. 模型名称
2. 模型版本
3. 上游项目
4. 下载来源
5. 代码 license
6. 权重 license
7. 训练数据集 license（若已知）
8. 再分发条款
9. 商业使用条款
10. 导出格式与运行时目标
```

当前打包模型的完整记录见 [`docs/yolo-models.zh-CN.md`](docs/yolo-models.zh-CN.md)。

## 发布源码要求

每一个分发的 YOLO Edition app 构建物都必须提供：

```text
1. 完整对应源码
2. 构建脚本与构建说明
3. AGPL-3.0 license 文件
4. 第三方依赖 license 声明
5. YOLO 模型来源与 license 声明
6. 与发布构建对应的 git tag
```

## App 关于页建议文案

```text
Sailens YOLO Edition
License: AGPL-3.0
Source code: https://github.com/wnbotoo/sailens-yolo

本版本包含 YOLO 模型支持。模型文件与第三方依赖可能各有其许可条款，详见仓库中的声明。
```

> 实现说明：关于页的许可证与源码链接来自 `BuildConfig.APP_LICENSE` / `BuildConfig.APP_SOURCE_URL`
> （在 `app/build.gradle.kts` 中设置），因此本 Edition 无需改动任何 UI 代码即可正确声明 AGPL-3.0
> 和本仓库地址。
