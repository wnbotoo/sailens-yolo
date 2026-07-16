**English** | [简体中文](YOLO_EDITION_NOTICE.zh-CN.md)

# YOLO Edition Notice

This repository is the **Sailens YOLO Edition** and is distributed under **AGPL-3.0**.

> **Not affiliated with, endorsed by, or sponsored by Ultralytics.** "YOLO" is used descriptively
> to identify the models this edition integrates. Ultralytics trademarks, where they exist, belong
> to their owner; nothing here implies review, approval, or endorsement by them.

## Relationship to Core Edition

```text
Core Edition:
  Repository: https://github.com/wnbotoo/sailens-android
  License: Apache-2.0

YOLO Edition:
  Repository: https://github.com/wnbotoo/sailens-yolo
  License: AGPL-3.0
```

YOLO Edition is a downstream distribution of Core Edition. General feature development should happen in Core Edition first and then be synchronized downstream.

## License boundary

This repository may include YOLO-specific model files, configuration, postprocessing, and release wiring. Because YOLO Edition may include AGPL-covered YOLO models and/or integrations, this repository and its distributed app builds are released under AGPL-3.0.

Do not merge YOLO Edition code back into Core Edition unless a license review confirms that the change is free of AGPL-covered material and can be clearly licensed under Apache-2.0.

## Model notice requirements

Every YOLO model included in this repository or in a YOLO Edition release must document:

```text
1. Model name
2. Model version
3. Upstream project
4. Download source
5. Code license
6. Weights license
7. Training dataset license, if known
8. Redistribution terms
9. Commercial-use terms
10. Export format and runtime target
```

## Release source requirements

Every distributed YOLO Edition app build must provide:

```text
1. Complete corresponding source code
2. Build scripts and build instructions
3. AGPL-3.0 license file
4. Third-party dependency license notices
5. YOLO model source and license notices
6. A matching git tag for the released build
```

## Suggested app About screen text

```text
Sailens YOLO Edition
License: AGPL-3.0
Source code: https://github.com/wnbotoo/sailens-yolo

This edition includes YOLO model support. Model files and third-party dependencies may have their own license terms. See the repository notices for details.
```
