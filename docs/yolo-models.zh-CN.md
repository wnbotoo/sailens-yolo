[English](yolo-models.md) | **简体中文**

# 本 Edition 打包的模型：来源与许可

> 本文档满足 `YOLO_EDITION_NOTICE.md` 要求的模型记录项。**不是法律意见**；发布前应由熟悉开源
> 许可证的顾问复核。
>
> 模型的**技术契约**（shape / 布局 / 类别顺序 / 性能红线）见上游的 [`models.zh-CN.md`](models.zh-CN.md)。
> 本文只讲**这两个权重是什么、从哪来、什么许可**。

## 打包内容

```text
data/src/main/assets/sem.tflite      语义可行走区域分割
data/src/main/assets/det.tflite      障碍物检测
```

上游 `.gitignore` 忽略 `data/src/main/assets/*.tflite`；这里用 `git add -f` 纳入，走 Git LFS
（`.gitattributes` 的 `*.tflite filter=lfs`）。

## sem.tflite

| 项 | 值 |
|---|---|
| 模型名 | `yolo26n-sem` |
| 原始文件名 | `yolo26n-sem_float16.tflite` |
| sha256 | `69e240a9b7ba81b83cef1ffc0bac77698a47a37544c7517259b9accd599ab4f5` |
| 上游项目 | Ultralytics YOLO26 |
| 下载来源 | ⚠️ **待补精确 release URL** — 来自 `ultralytics/yolo-flutter-app` releases 的 canonical Android 资产（v0.3.5 release notes 提到 "full YOLO26 semantic segmentation support"；Android 语义资产可追到 v0.2.0 canonical assets） |
| 模型版本 / release tag | ⚠️ **待确认** |
| 代码 license | AGPL-3.0（Ultralytics） |
| 权重 license | AGPL-3.0（按 Ultralytics 的主张；权重是否适用 copyleft 在业界有争议，本仓库按其主张从严处理） |
| 训练数据集 | Cityscapes（19 类 trainId） |
| 数据集 license | 🔴 **非商用**。约束跟着权重走，与代码许可无关 |
| 可否再分发 | 可，按 AGPL-3.0（须提供完整对应源码） |
| 商业使用 | 🔴 **不可**（受 Cityscapes 数据集约束）。免费 app 有"非商用"论点；**任何商业化（含卖硬件）前必须换用可商用数据训练的 sem 模型** |
| 导出格式 | onnx2tf float16 export，NHWC |
| I/O | FLOAT32 `[1,640,640,3]` → `Identity` FLOAT32 `[1,640,640,19]` 稠密分数 |
| 运行目标 | LiteRT，GPU |

## det.tflite

| 项 | 值 |
|---|---|
| 模型名 | `yolo26n` |
| 原始文件名 | `yolo26n_float16.tflite` |
| sha256 | `5950fac5e1a92adb17ad907b3925943c5c9dd29d59b0cbc1391efb4eb72740cf` |
| 上游项目 | Ultralytics YOLO26 |
| 下载来源 | ⚠️ **待补精确 release URL** |
| 模型版本 / release tag | ⚠️ **待确认** |
| 代码 license | AGPL-3.0（Ultralytics） |
| 权重 license | AGPL-3.0（同上） |
| 训练数据集 | COCO（80 类） |
| 数据集 license | 标注为 CC BY 4.0；图像各自有其来源条款。⚠️ **发布前复核** |
| 可否再分发 | 可，按 AGPL-3.0 |
| 商业使用 | 比 sem 宽松（COCO 无非商用条款），但**仍受 AGPL 权重约束** |
| 导出格式 | onnx2tf float16 export，NHWC |
| I/O | FLOAT32 `[1,640,640,3]` → `Identity` FLOAT32 `[1,84,8400]`（RAW_TRANSPOSED） |
| 运行目标 | LiteRT，GPU |

## 🔴 最长的那根杆

**sem 的 Cityscapes 非商用约束是本项目商业化路上最硬的一堵墙，且换仓库解决不了**——它跟着**权重**
走，跟代码许可无关。免费分发有"非商用"论点；**卖硬件是明确商用，届时 sem 必须换成用可商用数据
训练的模型**。这与 A/B 分仓无关，别把"A 是 Apache 了"误当成这个问题也解决了。

## 换模型时必须做的事

1. 跑 `py scripts/inspect_tflite.py <path>` 对照 [`models.zh-CN.md`](models.zh-CN.md) 的契约。
2. **人工验证类别顺序的语义**——`TfliteModelMetadataReaderTest` 只能校验 shape/dtype，
   **校验不了类别顺序**。顺序错不会报错，会静默把"人行道"当"马路"讲给一个看不见的人听。
   用一张已知场景实测 argmax 结果再谈精度。
3. 更新本文档的全部 10 项（含 sha256）。
4. `./gradlew :data:testDebugUnitTest` 必须绿（`TfliteModelMetadataReaderTest` 是契约闸）。
5. 真机复核上游 [`models.zh-CN.md`](models.zh-CN.md) 的两条性能红线：
   `sem: postprocessBackend = "native_score"` 且 `outputReadTimeMs ≈ 0`；
   `det: postprocessBackend = "native_bbox_nms_float_handle"`。
   **上游没有权重、验不了这两条，只能在这里验。**
