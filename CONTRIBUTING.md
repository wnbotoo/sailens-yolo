**English** | [简体中文](CONTRIBUTING.zh-CN.md)

# Contributing to Sailens YOLO Edition

This repository is the **YOLO Edition** of Sailens, licensed under **AGPL-3.0**.

Upstream Core Edition:

```text
https://github.com/wnbotoo/sailens-android
```

Read these first:

```text
docs/repository-license-strategy.md
YOLO_EDITION_NOTICE.md
```

## Read this before deciding where to contribute

Most people arrive here because this is the edition that actually runs — it ships weights, upstream
does not. That is not a reason to develop here.

**If you want to change behaviour, do it upstream.** You do not need to work in this repository to
get a running app: drop your own weights into upstream's `data/src/main/assets/` (which is
git-ignored) and it runs. Building and testing locally is not distribution, so it carries no AGPL
obligation. See "Development workflow" in
[`docs/repository-license-strategy.md`](docs/repository-license-strategy.md) — that section exists
because this question keeps getting re-litigated.

**And there is very little here to change.** This edition's entire delta is two model files plus
these governance docs. The source is upstream's, unmodified. Anything you would edit here is
upstream's code sitting in a downstream checkout.

## What this repository accepts

Only YOLO Edition-specific contributions:

```text
1. Downstream sync from sailens-android
2. YOLO model updates
3. YOLO-specific model configuration
4. YOLO-specific postprocessing
5. Fixes for YOLO Edition-only build problems
6. AGPL-3.0 notice updates
7. Model provenance, version, and license documentation
```

## What this repository does not accept

Do not land general features here first. These belong in the Core Edition:

```text
1. General UI features
2. General camera pipeline
3. General perception pipeline
4. General ModelRunner abstraction
5. General scheduler, risk analysis, scene fusion, event engine, speech engine
6. Bug fixes unrelated to YOLO
7. General documentation or test improvements
```

## Upstreaming rule

Forbidden by default:

```text
sailens-yolo -> sailens-android
```

**This is load-bearing, not tidiness.** Code you contribute here is licensed to the maintainer
under AGPL-3.0, and AGPL-3.0 code cannot be relicensed as Apache-2.0. If it lands here, it can
never reach the Core Edition — the two repositories would diverge and upstream's Apache-2.0
guarantee would be compromised. That guarantee is upstream's whole reason to exist.

To move anything from here to the Core Edition, a license review must confirm:

```text
1. It contains no AGPL-covered material
2. It does not depend on YOLO models or Ultralytics code
3. It is not copied, translated, or derived from AGPL source
4. It can be clearly licensed under Apache-2.0
5. It is genuinely general functionality
```

Default recommendation: when general capability is needed, implement it in the Core Edition
instead.

> **Note for the copyright holder:** this rule binds *third-party* contributions. An author is not
> bound by their own outbound license. See
> [`docs/repository-license-strategy.md`](docs/repository-license-strategy.md) §7.5.

## Model update requirements

A PR that updates or adds a model file must document all ten:

```text
1. Model name
2. Model version
3. Download source
4. Upstream project URL
5. Code license
6. Weights license
7. Training dataset license
8. Whether redistribution is permitted
9. Whether commercial use is permitted
10. Export format and inference runtime
```

It must also:

- Keep `TfliteModelMetadataReaderTest` green — it is this edition's contract guard.
- **Manually verify class channel order on a real scene.** No test can check this: the contract is
  validated by channel *count* only. A model with the right shape and a different class order runs
  with no error and quietly mislabels the world for someone who cannot see it. Treat it as a safety
  gate, not paperwork.
- Update `docs/yolo-models.md`, including the file's sha256.

## Pull Request Checklist

```text
- [ ] This change is specific to the AGPL-3.0 YOLO Edition.
- [ ] This change is compatible with AGPL-3.0 distribution.
- [ ] This change is not intended to be merged back into the Apache-2.0 Core Edition.
- [ ] If model files changed, their source, version, license, and redistribution terms are documented.
- [ ] If model files changed, class channel order was verified manually on a real scene.
- [ ] If this syncs from Core Edition, the upstream commit SHA is documented.
```
