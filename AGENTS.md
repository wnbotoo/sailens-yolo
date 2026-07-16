<!--
  YOLO EDITION PREFIX — everything below the marker is upstream's AGENTS.md, verbatim.
  Keep it that way: edit upstream's text in upstream, never here, or every sync conflicts.
  Corrections that apply to this edition go in this prefix block only.
-->

# AGENTS.md — Sailens YOLO Edition

```text
Repository: wnbotoo/sailens-yolo      Edition: YOLO Edition   License: AGPL-3.0
Upstream:   wnbotoo/sailens-android   Edition: Core Edition   License: Apache-2.0
```

**Everything after the marker below is upstream's guide, synced unchanged. It is accurate for the
code.** These rules are what differs here, and they override the upstream text where they disagree:

- **Upstream's guide says "this repository is Apache-2.0" and "no model weights are committed
  here". Both are true *of upstream*, not of this repository.** Here the license is AGPL-3.0, and
  `data/src/main/assets/{sem,det}.tflite` are tracked deliberately via `git add -f` — the weights are
  the entire point of this edition. Do not "fix" either by deleting weights or by proposing that
  upstream commit them.
- **Nearly everything belongs upstream.** Allowed direction is `sailens-android -> sailens-yolo`.
  Do not start general features here: UI, camera, perception, scheduler, risk, fusion, events,
  speech, model-runner abstraction, non-YOLO bug fixes. Make those in `sailens-android` first, then
  sync. This edition's entire delta is two weights plus governance docs — zero code.
- **`sailens-yolo -> sailens-android` is forbidden by default.** A third party's contribution here
  is licensed to the maintainer under AGPL-3.0 and cannot be relicensed Apache-2.0. That rule is
  load-bearing, not tidiness. (The copyright holder is not bound by their own outbound license —
  see "Development workflow" in `docs/repository-license-strategy.md`.)
- **Never run `git push upstream`.** It would put this edition's AGPL history into the Apache-2.0
  repository. A correctly set up clone has upstream's push URL disabled, but that is local config
  and a fresh clone does not have it — check `git remote -v` before trusting it, and see
  "One-time setup, per clone" in `README.md`.
- **Knowledge boundary:** upstream must not reference this repository, this edition, or any YOLO
  Edition release path. Never open a PR against `sailens-android` that links here or adds build
  logic aware of this repo. Upstream changes must make sense without knowing this repo exists.
- **Every model change must update `docs/yolo-models.md`** with source, version, licenses, dataset
  terms, and redistribution/commercial terms, and must keep `TfliteModelMetadataReaderTest` green —
  that test is this edition's contract guard.
- **Never rewrite published history, here or upstream.** This edition's ancestry is what makes
  `git merge upstream/main` work. See "Syncing from upstream" in `README.md`.
- Keep `LICENSE` (AGPL-3.0), `NOTICE`, `YOLO_EDITION_NOTICE.md`,
  `docs/repository-license-strategy.md`, `CONTRIBUTING.md`, and `.github/pull_request_template.md`
  aligned.

---
<!-- UPSTREAM AGENTS.md BEGINS HERE — DO NOT EDIT BELOW THIS LINE -->

# AGENTS.md

Sailens is Android navigation assistance for blind and low-vision users: a camera-fed perception
pipeline that turns the scene ahead into speech and haptics. This file is the repo's agent guide;
`README.md` is the human entry point.

## License and provenance guardrails
- **This repository is Apache-2.0.** Every contribution must be licensable under Apache-2.0.
- Do not copy, translate, or derive code from copyleft-licensed (GPL/AGPL) sources into this repo.
  Supporting a published *tensor layout* or a public *dataset class order* is a format fact and is
  fine; porting someone else's implementation is not.
- **No model weights are committed here** — the app is bring-your-own-model (`docs/models.md`).
  Never add a `.tflite` to a commit; `data/src/main/assets/*.tflite` is git-ignored on purpose.
  Weights carry their own licenses and dataset terms independent of this code license.

## Big picture
- Modules: `:app`, `:camera`, `:data`, `:domain`, `:presentation`, `:ux` (`settings.gradle.kts`).
- Direction: outer modules depend inward on `:domain` interfaces; keep Android/platform APIs out of `:domain`.
- `:app` hosts Koin + root Compose (`MainApplication.kt`, `MainActivity.kt`, `app/App.kt`) and runtime wiring.
- Runtime profile lives in `app/SailensRuntimeProfile.kt`; Koin bindings live in `app/DomainBindingsModule.kt`, `app/DiModule.kt`, and each feature module's `*Module.kt`.
- `:camera` owns CameraX capture + frame stream (`CameraViewModel.kt`, `ImageFrameAnalyzer.kt`).
- `:data` owns ML/depth/log/trace implementations (`data/di/DataModule.kt`).
- `:domain` owns perception/analysis/decision/trace use cases (`domain/src/main/java/com/sailens/domain/usecase`).
- `:presentation` owns UI state, overlay rendering, trace replay UI, TTS, and haptics (`SceneAnalysisViewModel.kt`, `device/*`).

## Runtime flow to preserve
- `ImageAnalysis` outputs `YUV_420_888` frames -> `ImageFrameAnalyzer` -> `SharedFlow<ImageFrame>` with `DROP_OLDEST`.
- `StartSceneAnalysisUseCase` initializes `PerceptionRepository`; in `DEFAULT` profile it also initializes the realtime obstacle (detection) provider.
- `StartSceneAnalysisUseCase` starts a trace session, maps each frame to `PerceptionResult`, `SceneResult`, and `FrameTrace`, then records runtime backend fields.
- `ProcessFrameUseCase` runs semantic segmentation, can reuse cached semantic analysis between scheduled runs, then runs obstacle detection extraction/tracking (det only; no instance-segmentation refinement).
- The perception profile decides which models run: `BASIC` = sem only (obstacles from the semantic mask), `DEFAULT` = sem + det (`DETECTION_MODEL`). There is no seg/refinement provider.
- `AnalyzeSceneUseCase` runs `ObstacleOcclusionAnalyzer` (carves corridor-overlapping tracked det-box ground-contact bands out of the sem passable mask — det∩sem cross-validation) then computes connectivity + road safety + ground transition + scene elements.
- `DecideEventsUseCase` order is fixed: `EventGenerator -> EventConflictResolver -> EventMerger -> CooldownManager`.
- `SceneAnalysisViewModel` consumes with `collectLatest`, updates masks/overlays/debug state, and triggers speech/haptics only when UI state enables them.

## Project-specific conventions
- Check `SailensRuntimeProfile.kt`, `DomainBindingsModule.kt`, and `data/di/DataModule.kt` first; DI is explicit constructor injection via Koin.
- Constructor defaults in `PerceptionConfig` are conservative (`BASIC`, obstacle provider type `NONE`), but the app's runtime tiers override them to sem + realtime det mode.
- Runtime tiers are `standard` (sem/det on GPU) and `ultra` (future VLM on NPU, realtime vision models stay on GPU).
- Treat `SailensRuntimeProfile.kt` as the source of truth for runtime backend targets and cadence; physical model files are resolved by `ModelCatalog` / `ModelSourceResolver` from `(ModelType, actual accelerator)`.
- Runtime hardware label comes from `DeviceHardwareProfileProvider` (`Build.SOC_MANUFACTURER` + `Build.SOC_MODEL`, with board/model fallback), not a hardcoded SoC string.
- ML backend reporting is carried by `MlRuntimeInfo` through outputs, scene debug info, trace JSON, trace replay, and UI.
- Accelerator selection is explicit by default; do not hide initialization failures with backend fallback while debugging model/backend compatibility. The app trace label reports the requested/active LiteRT accelerator, not proof that every op stayed on that backend.
- Obstacle filtering uses a perspective-aware navigation corridor (`navigationCorridorFarWidth` -> `navigationCorridorCenterWidth` from `navigationCorridorHorizonY` to the bottom of frame); obstacle speech uses typed keys for `person` / `bicycle` / `vehicle` / `static`, with slightly later center-person gates, earlier side-person gates that allow medium-urgency side persons, and forward vehicle priority preserved before multi-zone merge.
- User-facing prompt policy defaults away from brittle lane/surface/intersection fallbacks: road warning, road exit, ground-change speech, and traffic-light/road-ratio intersection fallback are off unless explicitly enabled; daily prompts should prefer typed obstacles, high-certainty blocked, path-complex, and low-priority `event_traffic_light` from stable traffic-light evidence. Traffic-light evidence is gated by semantic pixel ratio, road context, and debounce; if intersection prompts are enabled from reliable evidence, keep broad "possible intersection" wording.
- If you add event categories/keys, update domain event generation/merge logic and presentation string resources.
- `SceneEvent.messageKey` must stay aligned with `presentation/src/main/res/values/strings.xml` and `values-zh/strings.xml`.
- `BinaryMask` is `BitSet`-based and used in hot loops; avoid allocation-heavy patterns in analysis code.

## Integrations and assets
- CameraX (`camera-core/camera2/camera-lifecycle/camera-compose`) in `:camera`.
- LiteRT model execution with native YUV preprocessing (`native_yuv`), OpenCV fallback (`opencv_fallback`), and same-frame preprocessing cache hits (`shared_native_yuv` / `shared_quantized_native_yuv`).
- Semantic postprocess can use native fused score/stat extraction (`native_score`) or fallback argmax paths.
- Obstacle detection postprocess supports raw attribute-major tensors (`[1, 4+classCount, N]`) and end-to-end tensors (`[1, N, 6]`); layout is auto-resolved from the output tensor shape. The realtime path decodes straight from the output buffer handle (zero-copy) when available.
- No weights ship here. `ModelCatalog` resolves `sem` -> `data/src/main/assets/sem.tflite` and `det` -> `data/src/main/assets/det.tflite`; both are git-ignored, so a local working copy can hold weights that never reach a commit. Absent weights fail at init and surface as a start-analysis error — that is expected, not a bug to "fix" by committing a model.
- Shape, layout, dtype, and quantization are auto-resolved from the selected TFLite metadata and are not runtime profile fields, so swapping a conforming model needs no code change. To use separate GPU/NPU model files, update `ModelCatalog` / `ModelSourceResolver`, then set `acceleratorBackend` in `SailensRuntimeProfile.kt`. See `docs/models.md`.
- Class channel order is validated only by *count*, never by meaning: a wrong-order model runs silently and mislabels the scene for a user who cannot see it. Treat it as a safety property (`docs/models.md`).
- `FileLogService` writes JSONL logs under app internal `files/logs/`.
- `FileTraceService` writes trace JSONL sessions under app internal `files/traces/`; `FileTraceReplayService` reads them back.

## Developer workflows (pwsh)
```powershell
# from the repo root
.\gradlew.bat --no-daemon projects
.\gradlew.bat --no-daemon :app:assembleDebug
.\gradlew.bat --no-daemon test
.\gradlew.bat --no-daemon :domain:test
.\gradlew.bat --no-daemon :data:test
.\gradlew.bat --no-daemon :app:testDebugUnitTest
.\gradlew.bat --no-daemon :app:tasks
```

## Change guardrails
- **`main` is published and append-only. Never rewrite it** — no squash-and-force-push, no rebase of
  `main`, no amending a pushed commit. Fix mistakes with a new commit that reverts or corrects them.
  A force-push does not just inconvenience clones: anything built on a rewritten commit is orphaned
  from this history, and reconciling it costs far more than the tidy log was worth.
- Keep Android/platform APIs out of `:domain`.
- If you change frame resolution/format, update both camera use-case config and ML preprocessing assumptions.
- If you change model files, update `ModelCatalog` / `ModelSourceResolver`; if you change class counts, mask coefficient counts, resize filter, or backend targets, update `SailensRuntimeProfile.kt` first. Only change `SemanticModelConfig` or `ObstacleModelConfig` when the model contract/defaults change.
- If you change backend reporting fields, update `MlRuntimeInfo`, trace encoding/parsing/reporting, live debug UI, and trace replay UI together.
- Keep start/stop lifecycle symmetry: components with `reset()` should remain wired through `StopSceneAnalysisUseCase`.
- Present a plan and get agreement before starting a refactor.
