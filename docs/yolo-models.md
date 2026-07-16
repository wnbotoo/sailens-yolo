**English** | [简体中文](yolo-models.zh-CN.md)

# Bundled models: provenance and licenses

> This document satisfies the model record requirements in `YOLO_EDITION_NOTICE.md`. **It is not
> legal advice**; have someone familiar with open-source licensing review it before release.
>
> The models' **technical contract** (shape / layout / class order / performance red lines) is in
> upstream's [`models.md`](models.md). This document only covers **what these two weights are, where
> they came from, and under what license**.

## What is bundled

```text
data/src/main/assets/sem.tflite      semantic walkable-area segmentation
data/src/main/assets/det.tflite      obstacle detection
```

Upstream git-ignores `data/src/main/assets/*.tflite`; here they are tracked via `git add -f` and
stored in Git LFS (`.gitattributes` has `*.tflite filter=lfs`).

## sem.tflite

| Field | Value |
|---|---|
| Model name | `yolo26n-sem` |
| Original file name | `yolo26n-sem_float16.tflite` |
| sha256 | `69e240a9b7ba81b83cef1ffc0bac77698a47a37544c7517259b9accd599ab4f5` |
| Upstream project | Ultralytics YOLO26 |
| Download source | ⚠️ **exact release URL TBD** — from the canonical Android assets in `ultralytics/yolo-flutter-app` releases (v0.3.5 release notes mention "full YOLO26 semantic segmentation support"; the Android semantic assets trace back to v0.2.0 canonical assets) |
| Model version / release tag | ⚠️ **TBD** |
| Code license | AGPL-3.0 (Ultralytics) |
| Weights license | AGPL-3.0 (per Ultralytics' position; whether copyleft applies to weights is contested in the industry — this repository takes the strict reading of their claim) |
| Training dataset | Cityscapes (19 trainId classes) |
| Dataset license | 🔴 **NON-COMMERCIAL**. This constraint travels with the weights and is independent of any code license |
| Redistribution | Permitted, under AGPL-3.0 (complete corresponding source must be provided) |
| Commercial use | 🔴 **Not permitted** (Cityscapes dataset constraint). A free app has a "non-commercial" argument; **any commercialization, including selling hardware, requires replacing this with a sem model trained on commercially usable data** |
| Export format | onnx2tf float16 export, NHWC |
| I/O | FLOAT32 `[1,640,640,3]` → `Identity` FLOAT32 `[1,640,640,19]` dense scores |
| Runtime target | LiteRT, GPU |

## det.tflite

| Field | Value |
|---|---|
| Model name | `yolo26n` |
| Original file name | `yolo26n_float16.tflite` |
| sha256 | `5950fac5e1a92adb17ad907b3925943c5c9dd29d59b0cbc1391efb4eb72740cf` |
| Upstream project | Ultralytics YOLO26 |
| Download source | ⚠️ **exact release URL TBD** |
| Model version / release tag | ⚠️ **TBD** |
| Code license | AGPL-3.0 (Ultralytics) |
| Weights license | AGPL-3.0 (as above) |
| Training dataset | COCO (80 classes) |
| Dataset license | Annotations are CC BY 4.0; images carry their own source terms. ⚠️ **Review before release** |
| Redistribution | Permitted, under AGPL-3.0 |
| Commercial use | Looser than sem (COCO has no non-commercial clause), but **still bound by the AGPL weights** |
| Export format | onnx2tf float16 export, NHWC |
| I/O | FLOAT32 `[1,640,640,3]` → `Identity` FLOAT32 `[1,84,8400]` (RAW_TRANSPOSED) |
| Runtime target | LiteRT, GPU |

## 🔴 The longest pole

**sem's Cityscapes non-commercial constraint is the hardest wall on this project's path to
commercialization, and no amount of repository splitting solves it** — it travels with the
**weights**, not the code license. Free distribution has a "non-commercial" argument; **selling
hardware is unambiguously commercial, and at that point sem must be replaced with a model trained on
commercially usable data.** This is unrelated to the A/B split — do not mistake "the core is Apache
now" for this problem being solved too.

## Required steps when changing a model

1. Run `py scripts/inspect_tflite.py <path>` and check it against the contract in
   [`models.md`](models.md).
2. **Verify class channel order semantics by hand.** `TfliteModelMetadataReaderTest` can only check
   shape/dtype — **it cannot check class order**. A wrong order does not error; it will quietly call
   a sidewalk a road, out loud, to someone who cannot see it. Test the argmax output on a known
   scene before discussing accuracy.
3. Update all ten fields in this document (including the sha256).
4. `./gradlew :data:testDebugUnitTest` must be green (`TfliteModelMetadataReaderTest` is the
   contract guard).
5. Re-check upstream [`models.md`](models.md)'s two performance red lines on a device:
   `sem: postprocessBackend = "native_score"` and `outputReadTimeMs ≈ 0`;
   `det: postprocessBackend = "native_bbox_nms_float_handle"`.
   **Upstream has no weights and cannot verify these — this is the only place they can be checked.**
