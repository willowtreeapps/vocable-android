# 690 — Staged rollout for the production release pipeline

## Why

`ps-release.yml` (triggered by pushing a semver git tag) uploads directly to the Play Store `production` track at 100% rollout, with no ramp. This workflow has never actually completed a production upload — the repo's only tag is `v1.3.0` from 2020, predating the current pipeline entirely.

Vocable's users are AAC super-users who depend on the app daily to communicate; if a release ships a bug, they can't talk until it's fixed. With the team down to a single engineer as of the 2026-08-24 layoff, there's no one to catch a bad rollout quickly if it goes to everyone at once on the pipeline's first real run.

## What changed

Added `status: inProgress` and `userFraction: 0.2` to the `r0adkll/upload-google-play` step in `ps-release.yml`. The release now ramps to 20% of production users instead of 100%, and stays there until someone manually completes the rollout in Play Console.

## Key decision

Completing the rollout to 100% is a deliberate manual step, not automated here — the point is a human checkpoint (Crashlytics, install/crash trends) between the initial ramp and full release, not just a slower default percentage.

## Links

Part of #690.
