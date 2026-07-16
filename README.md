**English** | [简体中文](README.zh-CN.md)

# Sailens YOLO Edition

**Sailens** — *Smart AI LENS*.

This repository is the **AGPL-3.0 YOLO Edition** of Sailens: the Apache-2.0 Core Edition plus
YOLO26 model weights, distributed under AGPL-3.0 because those weights are AGPL-covered.

> **Not affiliated with, endorsed by, or sponsored by Ultralytics.** "YOLO" is used descriptively
> to identify the models this edition integrates.

```text
Core Edition:
  https://github.com/wnbotoo/sailens-android
  License: Apache-2.0

YOLO Edition:
  https://github.com/wnbotoo/sailens-yolo
  License: AGPL-3.0
```

## What Sailens is

Android navigation assistance for blind and low-vision users. A camera-fed perception pipeline
turns the scene ahead into speech and haptics: where you can walk, where the road edge is, and what
is in the way.

Architecture, model contract, NPU wiring, and trace tooling all live upstream and are synced here
unchanged — see [`AGENTS.md`](AGENTS.md) and [`docs/`](docs/).

## What this edition adds

Exactly two things, and deliberately nothing else:

```text
data/src/main/assets/sem.tflite     yolo26n-sem, semantic segmentation
data/src/main/assets/det.tflite     yolo26n, obstacle detection
```

Provenance, licenses, and dataset terms for both: [`docs/yolo-models.md`](docs/yolo-models.md).

The Core Edition ships **no weights** — it resolves these two asset paths and reads shape, layout,
dtype and quantization from model metadata at load time. So this edition needs **zero code
changes**: it is upstream's tree, plus two binaries, plus these governance docs.

## Purpose

The Core Edition keeps the application, perception pipeline, and model runner abstraction under
Apache-2.0, so it can be embedded in closed products. This edition exists so the app can actually
ship with working models today, and it carries the AGPL obligation those weights bring.

## Development model

```text
sailens-android  ->  sailens-yolo
Apache-2.0 Core  ->  AGPL-3.0 YOLO Edition
```

**General development happens in the Core Edition first.** This repository should only receive:

```text
1. Downstream sync from sailens-android
2. YOLO model updates
3. YOLO-specific model configuration
4. YOLO-specific postprocessing or build wiring
5. YOLO Edition release maintenance
6. AGPL and third-party license notices
```

Do not merge code from this repository back into `sailens-android` unless a license review confirms
the change is free of AGPL-covered material and can be clearly licensed as Apache-2.0.

> **If you are the copyright holder, read "Development workflow" in
> [`docs/repository-license-strategy.md`](docs/repository-license-strategy.md) first.** The rule
> above governs *third-party* contributions. It does not mean you must develop here to get a
> runnable app — you don't, and doing so is the harder path.

## Build

Same as upstream; the weights are already in place.

```bash
./gradlew build
./gradlew :app:assembleDebug
```

## Syncing from upstream

One-time setup per clone (a merge driver cannot be committed, so `.gitattributes` alone is not
enough):

```bash
git remote add upstream git@github.com:wnbotoo/sailens-android.git
git config merge.ours.driver true
```

Then every sync is:

```bash
git fetch upstream
git merge upstream/main
```

Record the upstream commit SHA in the merge commit message; the PR checklist asks for it.

### Why merge, not rebase

**Merge remembers.** Once a conflict is resolved, the merge commit becomes a common ancestor and the
same hunks never conflict again. Rebase replays this edition's commits onto upstream's head from
scratch every time, so **you would re-resolve the same conflicts on every single sync, forever** —
it makes conflicts worse, not better.

Rebase also rewrites published history, which means force-pushing a repository whose entire job is
being a stable, auditable source for AGPL compliance. And a merge commit structurally records "this
edition's state X + upstream's state Y", which is exactly the audit trail a release needs.

### Why syncs are usually quiet

Almost nothing here overlaps upstream. The weights, `docs/yolo-models.md`,
`docs/repository-license-strategy.md`, `CONTRIBUTING*.md`, `YOLO_EDITION_NOTICE*.md`, `.github/`,
and `TfliteModelMetadataReaderTest` have no upstream counterpart, so they cannot conflict.

Of the files that do overlap:

- `README.md`, `README.zh-CN.md`, `LICENSE`, `NOTICE` — resolved automatically by the `merge=ours`
  rules in `.gitattributes`. See that file for the trade-off.
- `AGENTS.md` — this edition's delta is a **pure prefix** above a marker; upstream's text below is
  verbatim, so upstream's edits merge cleanly. Keep it that way: never edit upstream's text here.
- `app/build.gradle.kts`, `app/src/main/res/values/strings.xml` — a couple of lines each; they merge
  cleanly unless upstream edits the same lines.

### 🔴 Upstream's `main` must be append-only

**Never rewrite upstream's published history** (no squash-and-force-push, no rebase of `main`).

This edition's ancestry is what makes syncing work. If upstream force-pushes, the commit this
edition was built on becomes an orphan, the merge base collapses to the initial commit, and git
compares against an empty tree — **every file in the repository conflicts at once**. Resolving that
once does not fix it; the broken base persists and every later sync repeats it.

If it happens anyway, the fix is to re-parent this edition onto upstream's new commit — a one-time
repair, not a workflow:

```bash
git tag backup-before-reparent main          # always, first
git fetch upstream
git checkout --detach <upstream-new-commit>
git read-tree -u --reset main                # keep this edition's tree exactly
git diff backup-before-reparent --stat       # MUST be empty: content is unchanged
git commit -m "yolo edition: ..."            # same content, correct parent
git branch -f main HEAD && git checkout main
git push --force-with-lease origin main
```

Verify `git merge-base main upstream/main` afterwards: it must be upstream's tip, not the initial
commit.

## Docs

This edition's own documents:

| | | |
|---|---|---|
| [`docs/yolo-models.md`](docs/yolo-models.md) | [中文](docs/yolo-models.zh-CN.md) | Provenance and licenses of the bundled weights |
| [`docs/repository-license-strategy.md`](docs/repository-license-strategy.md) | [中文](docs/repository-license-strategy.zh-CN.md) | AGPL distribution rules, sync direction, **development workflow** |
| [`YOLO_EDITION_NOTICE.md`](YOLO_EDITION_NOTICE.md) | [中文](YOLO_EDITION_NOTICE.zh-CN.md) | Edition notice and model record requirements |
| [`CONTRIBUTING.md`](CONTRIBUTING.md) | [中文](CONTRIBUTING.zh-CN.md) | What this repo accepts, and where else to go |

Everything under `docs/` other than the two above is upstream's, synced unchanged — model contract,
perception profiles, NPU wiring, trace metrics. Each has an English and a Chinese version.

`LICENSE` and `NOTICE` are not translated: only their English text is legally operative, and an
unofficial translation would create ambiguity about which version controls.

## Required release notices

Every YOLO Edition release must provide:

```text
1. Complete corresponding source code
2. Build instructions and scripts
3. AGPL-3.0 license file
4. YOLO model source, version, and license notice
5. Third-party dependency license notice
6. Git tag matching the released build
```

See [`docs/repository-license-strategy.md`](docs/repository-license-strategy.md),
[`YOLO_EDITION_NOTICE.md`](YOLO_EDITION_NOTICE.md), [`CONTRIBUTING.md`](CONTRIBUTING.md).

## License

AGPL-3.0 — see [LICENSE](LICENSE).

Upstream code is Apache-2.0 by The Sailens Authors; this edition's *combined* distribution is
AGPL-3.0. See [NOTICE](NOTICE).
