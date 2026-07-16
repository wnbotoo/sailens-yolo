**English** | [简体中文](repository-license-strategy.zh-CN.md)

# Repository license strategy: YOLO Edition

> This document defines `sailens-yolo`'s AGPL-3.0 distribution rules. **It is not legal advice.**
> Before any formal release, the final repository structure, build artifacts, dependencies, model
> weights, and release process should be reviewed by a lawyer or compliance advisor familiar with
> open-source licensing.

## 1. What this repository is

```text
Repository: https://github.com/wnbotoo/sailens-yolo
Edition: YOLO Edition
License: AGPL-3.0
Upstream Core Edition: https://github.com/wnbotoo/sailens-android
Upstream License: Apache-2.0
```

`Sailens YOLO Edition` is a downstream distribution of `Sailens Core Edition`.

The Core Edition keeps its Apache-2.0 boundary. The YOLO Edition may add YOLO models, YOLO-specific
configuration, YOLO postprocessing, and YOLO release capability, and is therefore released under
AGPL-3.0.

## 2. Sync direction

Allowed:

```text
sailens-android  ->  sailens-yolo
Apache-2.0 Core  ->  AGPL-3.0 YOLO Edition
```

Forbidden by default:

```text
sailens-yolo  ->  sailens-android
AGPL-3.0      ->  Apache-2.0
```

Code, models, configuration, or build logic in this repository must not be merged back into
`sailens-android` unless a license review confirms the content contains no AGPL-covered material and
can be clearly licensed under Apache-2.0.

## 3. What this repository may contain

```text
1. General code synced from sailens-android
2. YOLO model weights
3. YOLO-specific model configuration
4. YOLO-specific postprocessing
5. YOLO Edition build configuration
6. YOLO model provenance, version, license, and notices
7. AGPL-3.0 source availability statements
8. Third-party dependency license notices
```

## 4. What this repository does not do

```text
1. Land general product features first
2. Act as upstream of the Core Edition
3. Backport AGPL-specific implementations into the Core Edition
4. Hide YOLO model provenance or licensing
5. Publish build artifacts that cannot be mapped to a source tag
```

General feature development goes to `sailens-android`.

## 5. Release requirements

Before releasing a YOLO Edition app build, confirm:

```text
1. `LICENSE` is AGPL-3.0.
2. The release tag is created.
3. Complete corresponding source is obtainable.
4. Build scripts and instructions are obtainable.
5. YOLO model provenance, version, download URL, and license are recorded.
6. Third-party dependency license notices are recorded.
7. The app's About screen shows YOLO Edition, AGPL-3.0, the source link, and model licenses.
```

## 6. Actual directory structure

> This section previously recommended a `yolo-edition/` directory or an `app/src/yolo/` source set.
> **Neither is needed**, because the Core Edition ended up bring-your-own-model: it ships no
> weights, resolves two fixed asset paths, and reads shape/layout/dtype/quantization from TFLite
> metadata automatically. So this edition's entire delta is two binaries:

```text
data/src/main/assets/sem.tflite      the Core's sem slot
data/src/main/assets/det.tflite      the Core's det slot
```

The Core Edition's `.gitignore` ignores `data/src/main/assets/*.tflite`; this repository adds them
once with `git add -f`, after which they stay tracked. **The two repositories can never conflict
over weights, and the Core does not need to add anything for this repository's benefit** (satisfying
the knowledge boundary).

**The code delta is zero.** Do not create YOLO-specific directories or source sets for the sake of
"keeping things together" — there is no YOLO-specific code to keep. If some day there genuinely is
(say a model with a different output layout needing a new decode branch), **that branch belongs in
the Core Edition**: supporting a tensor layout is a format fact, not an AGPL derivative, and the
Core can take it. Only things that genuinely cannot be decoupled from the Core stay here.

## 7. Upstreaming rule

By default, do not move code from this repository into the Core Edition.

If you must, confirm:

```text
1. It contains no YOLO model weights.
2. It contains no Ultralytics AGPL code.
3. It is not copied, translated, or derived from AGPL source.
4. It does not depend on YOLO Edition-only source sets, directories, or configuration.
5. It can be clearly licensed under Apache-2.0.
6. It is general functionality, not YOLO Edition-specific.
```

Default recommendation: when general capability is needed, implement it in the Core Edition.

**This rule applies to third-party contributors, not to the copyright holder.** See the next
section.

## 7.5 Development workflow (read this; do not re-derive it)

> This is not legal advice. But it answers a question that gets re-litigated every few weeks:
> **"Where should development happen? The Core has no models so I cannot debug, and this repo can't
> go back upstream."**
> The answer: **that dilemma is mostly an illusion.**

### Conclusion: develop only in the Core Edition, with weights in your local working copy

```text
Daily development   Only in sailens-android. Drop weights into its data/src/main/assets/
                    (git-ignored) and debug on device as normal. The weights can never
                    reach A's git.
This repository     Only `git merge upstream/main` at release time. Zero code changes.
Third-party PRs     Always to the Core Edition. This repo takes model updates only.
```

**Do not maintain two checkouts and shuttle code between them** — that is what actually drags you
into "is this snippet AGPL now?".

### Why this does not trigger AGPL

**Every AGPL obligation hangs on conveying (distribution).** GPL-3.0 §2 explicitly permits running
and modifying a covered work **you do not convey**, without conditions. AGPL's additional §13
triggers on "users interacting with it remotely through a network" — debugging an Android app
locally does not come close.

So: **putting AGPL weights in the Core Edition's working copy to develop and debug carries zero AGPL
obligation, because you are not distributing.** The Core Edition's `.gitignore` guarantees those
weights never enter a commit. Development in A, debugging with models, and a clean A repository are
all true at the same time.

The only real constraint is **distribution**: you cannot ship a build with those weights as-is;
release goes through this repository (AGPL).

### Why the copyright holder is not bound by the upstreaming rule

The complete version of that rule is: "**third-party** code contributions must go to A". A third
party opens a PR here; this repository's LICENSE is AGPL-3.0; therefore their copyright grant to the
maintainer is AGPL, and it cannot be moved into the Apache-2.0 Core Edition.

**But a copyright holder is not bound by their own outbound license.** You hold copyright in code
you write; it does not become AGPL because you wrote it in a directory that happened to contain AGPL
weights. An outbound license is a grant **you make to others**, not a collar on yourself.

### And there is almost nothing here to develop

This edition's delta is two weights plus governance docs — **zero code** — and the two repositories'
sources are 100% identical. So "developing a new feature in B" is a scenario that essentially does
not exist: any code you could change here is the Core Edition's code. The only genuinely B-only work
is swapping a weights file.

## 8. Long-term principles

```text
1. The Core Edition is the mainline.
2. The YOLO Edition is a downstream AGPL distribution.
3. General development goes to the Core Edition.
4. YOLO-specific work stays in the YOLO Edition.
5. A -> B one-way sync.
6. B -> A forbidden by default (for third parties; see §7.5).
7. Every model must record code license, weights license, dataset license, and redistribution terms.
```
