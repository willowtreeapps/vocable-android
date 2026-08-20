# CI: validate the Play Store release pipeline end-to-end

**Issue:** #626 (detached from #613 — general CI/release-health work, not feature-specific)

## What was needed

`ps-release.yml` (the tag-triggered production release pipeline) had zero runs in Actions
history, and the repo had only one tag (`v1.3.0`) — it hadn't been exercised since at least
Jan 2024. The ticket called for validating four things before trusting it for a real release:
`PLAY_STORE_CREDENTIALS` still valid, the signing key matching what's published, `versionCode`
generation ahead of the last published build, and a successful dry-run release to an
internal/alpha track.

## What was found

The ticket assumed `ps-release.yml` was the only untested pipeline. There's a second one,
`pre-release-upload.yml`, called from `build.yml` on every push to `main` — it uploads to
Firebase App Distribution (group `internal`) **and** Play Store `track: alpha` (the Play
Developer API's name for what the Play Console UI calls closed testing, i.e. exactly the
"internal/alpha track" the ticket asks about). It had actually run twice already (both real
merges to `main`) and failed identically both times, which turned out to be first real evidence
of the pipeline's actual health — better than a synthetic dry run, since it was already
exercising the real path.

Both workflows turned out to be broken for three independent, stacked reasons. Fixing one just
exposed the next:

1. **APK vs. App Bundle mismatch.** Both workflows built and uploaded a raw `.apk` via
   `assembleRelease`. Play already has a `.aab` published at versionCode 31 (from the original
   manual `v1.3.0` publish) — once an app has a bundle on Play, it rejects APK uploads outright:
   `Cannot replace a bundle of version code 31 with an APK.`
2. **A silently-swallowed script failure.** `get-next-version-code.sh` queries the Play
   Developer API for the highest versionCode across all tracks and prints `max + 1`. The step
   that called it, `echo "value=$(./scripts/get-next-version-code.sh ...)" >> "$GITHUB_OUTPUT"`,
   always exits 0 because `echo` succeeds regardless of what the substituted command did — a
   classic bash pitfall. When the script failed to create a Play Console edit, the step still
   reported success with an empty value. `build.gradle.kts`'s `versionCode = versionCodeEnv + 30`
   (`versionCodeEnv` defaulting to 1 via `?.toIntOrNull() ?: 1`) then landed on exactly 31 — the
   already-used code — making the failure look like a versionCode logic bug rather than an
   upstream auth failure.
3. **Two wrong ways to get an Android-Publisher-scoped OAuth token**, in sequence:
   - `google-github-actions/auth`'s `access_token_scopes` input only affects the action's own
     `access_token` *output*, which only exists when `token_format: access_token` is set.
     Without it, `gcloud auth print-access-token` mints its own token from the ADC credentials
     file, ignoring `access_token_scopes` entirely — and `print-access-token` has no `--scopes`
     flag to override it. The resulting token was scoped to the generic `cloud-platform` default,
     which the Android Publisher API's `edits.insert` rejects.
   - Setting `token_format: access_token` (to actually generate a scoped output) hit a second,
     real wall on a live run: generating that output routes through the IAM Service Account
     Credentials API (`iamcredentials.googleapis.com`), which is disabled on this GCP project —
     `IAM Service Account Credentials API has not been used in project ... or it is disabled.`
     That's a Google Cloud Console change, not something fixable from a workflow file.

## What changed

- `ps-release.yml` and `pre-release-upload.yml`: build `bundleRelease` alongside
  `assembleRelease` and upload the `.aab` to Play Store instead of the `.apk`. The `.apk` build
  is left in place — `pre-release-upload.yml`'s Firebase distribution step and
  `ps-release.yml`'s GitHub release attachment both already worked against it.
- `scripts/get-next-version-code.sh`: mints its own OAuth token via the standard OAuth2
  JWT-bearer flow (`google-auth`'s `service_account.Credentials.from_service_account_info(...).refresh()`),
  the same mechanism `r0adkll/upload-google-play` already uses internally — which is why that
  step never hit the scope problem. This needs only the raw service-account JSON
  (`PLAY_STORE_CREDENTIALS`, passed through as `$SERVICE_ACCOUNT_JSON`) and has no IAM
  Credentials API dependency at all. The `google-github-actions/auth` step became entirely
  unused once this landed and was removed from both workflows.
- The "Compute next versionCode" step now assigns to a variable before writing to
  `$GITHUB_OUTPUT` (`value=$(...); echo "value=$value" ...`) instead of embedding the
  substitution directly in the `echo` call, so a real failure aborts the step instead of
  silently defaulting.
- `get-next-version-code.sh` now prints the actual Play API error response body on edit-creation
  failure, instead of a generic message — both real failures during this work required pulling
  raw workflow logs to diagnose; this puts the cause directly in the log next time.
- `pre-release-upload.yml` gained a `workflow_dispatch` trigger with a `skip_device_tests`
  input, so the Play/Firebase upload steps can be dry-run from any branch without a push to
  `main`. Without the input, `device-tests.yml` fails immediately on a standalone dispatch: it
  downloads a debug/androidTest APK artifact from *the same run*, which normally exists because
  `build.yml`'s own `build` job produces it before calling into this workflow — a standalone
  dispatch has no such job.

## Verification

Real, live dry run on `feature/626/fix-gcloud-token-scope` via the new `workflow_dispatch`
input: [run 32376505584](https://github.com/willowtreeapps/vocable-android/actions/runs/32376505584).
Every step passed, including a real committed Play Store release:

- `Compute next versionCode` → `483` (no collision with the stale 31)
- `Deploy to Firebase` → `uploaded new release pre-release(483) (483) successfully!`
- `Upload to Play Store` → `Validating tracks: 'alpha'` → `Uploading
  app/build/outputs/bundle/release/app-release.aab` → `Successfully uploaded 1 artifacts` →
  `Successfully committed 17531760968116200413`

This is the first time either release pipeline has completed successfully. Since Play accepted
and committed the signed bundle, it also confirms the signing key used by CI matches what Play
expects for this app (ticket AC #2) — a rejected signature would have failed the commit, not
succeeded.

The token-minting approach itself was also verified locally end-to-end (before the branch's
final state was confirmed on a real run) using a throwaway fake service-account key: the
JWT-bearer request reached Google's real `oauth2.googleapis.com` token endpoint and received a
structured `invalid_grant: Invalid grant: account not found` rejection — proof the JWT
signing/encoding path itself is correct, independent of whether the specific key is valid.

## Key decisions, and why

**Why mint the token via JWT-bearer instead of just enabling the IAM Credentials API.** Enabling
`iamcredentials.googleapis.com` in the Google Cloud Console would also have fixed
`token_format: access_token`, and is arguably the more "standard" GitHub Actions pattern. It was
rejected here because it's an infrastructure change outside the repo, gated on someone with GCP
project access, and adds a permanent dependency on an API that has no other reason to be enabled
for this project. Minting the token directly from the existing service-account key needs nothing
new — it's the same mechanism `r0adkll/upload-google-play` was already relying on successfully.

**Why `skip_device_tests` rather than always running device tests on a manual dispatch.**
Building a debug+androidTest APK inside `pre-release-upload.yml` just to satisfy
`device-tests.yml`'s artifact-download step would have duplicated `build.yml`'s own `build` job.
Skipping the job on a flagged manual dispatch is simpler and matches what a dry run actually
needs to validate — the Play/Firebase upload path, not instrumentation coverage that's already
gated on every PR via `build.yml`.

**Left open, deliberately not decided here:** `PLAY_STORE_CREDENTIALS` is confirmed
*functionally* valid — it authenticated and completed a real upload during the dry run — but
it's the original key from 2023-10-12, never rotated. Whether to rotate it on a schedule is a
team call, not a code fix, so the ticket's checklist leaves that box open pending a decision.

## Pointers

- Issue: #626 · PRs: #684 (AAB fix), #685 (auth scope attempt + silent-failure fix, later found
  incomplete), #686 (`token_format: access_token`, later found to need the disabled IAM API),
  #687 (working JWT-bearer fix + `workflow_dispatch`/`skip_device_tests`)
- Note on PR sequencing: #686 merged to `main` one commit before the JWT-bearer fix and the
  `workflow_dispatch` addition were pushed to the same branch (auto-merge, not an intentional
  early merge) — #687 exists specifically to bring `main` in sync with the branch state that was
  actually validated by the successful dry run.
