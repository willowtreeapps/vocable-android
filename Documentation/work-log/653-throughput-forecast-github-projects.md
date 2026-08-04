# #653 — Port throughput-forecast to GitHub Projects v2

## What

Added a GitHub Projects v2 port of WillowTree's Jira-based `throughput-forecast`
skill, so it forecasts delivery (Monte Carlo, cycle-time SLE, aging WIP) from
[Project #50](https://github.com/orgs/willowtreeapps/projects/50), the board
this repo actually uses, instead of requiring Jira.

New files (`.claude/scripts/`, `.claude/skills/throughput-forecast/`):
- `gh_fetch.py` — the new gather step. Pulls the project's items via GraphQL,
  nesting each Issue's status-change timeline (`ProjectV2ItemStatusChangedEvent`)
  in the same paginated query, and writes `done.json`/`open.json` in the exact
  envelope the forecast engine already reads.
- `forecast.py`, `forecast-html.py` — copied **byte-for-byte, unmodified** from
  the source Jira skill (`willowtreeapps/dq-documentation:skills/
  throughput-forecast/scripts/`). Both are pure math with zero network calls,
  so the entire port is "swap the gather step" — confirmed by tracing every
  field `forecast.py` reads against what GitHub's GraphQL API can supply.
- `config.yaml`, `SKILL.md` — GitHub-flavored re-skin of the originals.
- `.claude/settings.json` — allowlists the three scripts.

## Why

This repo tracks work in GitHub Projects, not Jira, so the original skill was
unusable here as-is. Its own `SETUP-GUIDE.md` documents an explicit "adapting
to another tracker" path (produce the same two-file JSON envelope; the engine
doesn't care where the data came from), and live testing confirmed GitHub
Projects v2 exposes a direct analog of Jira's changelog via
`timelineItems(itemTypes: [PROJECT_V2_ITEM_STATUS_CHANGED_EVENT])` — so this
was a faithful port, not a redesign.

## Key decisions (not obvious from the code alone)

**1. `DraftIssue`/`PullRequest` project items are excluded by default.**
GitHub Projects v2 items are a union of `Issue`/`DraftIssue`/`PullRequest`
(320 items on this board split 227/50/43). Neither Draft nor PR items are
deliverable flow in the Jira-Epic sense, so both are excluded by default
(`--include-drafts` opts drafts back in). **`--include-prs` is intentionally
not implemented** — live testing found a real PR whose
`timelineItems.totalCount` was 17 while the itemTypes-filtered `nodes` came
back empty. `totalCount` does not respect the `itemTypes` filter for
`PullRequest` the way it reliably does for `Issue`, so the same
"truncated → backfill" logic used for issues can't be trusted for PRs without
separate handling. Passing `--include-prs` exits with an error rather than
silently producing wrong changelogs.

**2. `resolutiondate` derivation has a bulk-import trap — found and fixed
during verification, not anticipated in the design.** The naive rule ("last
status-transition to Done, else `closedAt`") looked right until the first
real run against Project #50 showed **141 of 169 Done items sharing the exact
same transition timestamp** (`2024-05-28T18:04:24Z`). Spot-checking several of
those issues live confirmed why: each one's *entire* timeline was a single
`previousStatus:"" → "Done"` entry at that instant — the moment those
already-closed historical issues were bulk-added to the project — while their
real `closedAt` dates were genuinely spread out (some over a year earlier).
Treating that shared import instant as 141 real "completions" would have
fabricated a single fake mega-throughput week and badly biased the forecast.

`gh_fetch.py`'s `is_bulk_import_artifact()` now detects this specific
signature — a timeline whose *entire* history is one empty-origin entry
landing directly on a done status — and falls back to `closedAt` for exactly
those items, while a real multi-step history that happens to end at Done is
untouched. After the fix: 141 correctly reclassified as import artifacts, only
2 genuine (non-artifact) disagreements remain, and 2 items with no `closedAt`
at all were dropped rather than guessed. This is logged to stderr every run
(`resolutiondate bulk-import artifacts...` / `...disagreements (>1 day,
non-artifact)`) so it stays visible rather than silently "handled."

This is exactly the class of issue the skill's own outlier detection exists
to catch downstream (a robust median/MAD z-score on weekly throughput) — but
that detection assumes real weeks, not a single fabricated one; catching the
import artifact at the *gather* step, before it becomes a fake week, was the
right layer to fix it at.

**3. `.claude/settings.json` did not exist on this branch.** It exists,
unmerged, on a separate `chore/claude-code-setup` branch with the same
`permissions.allow` shape. This PR adds an independent one here scoped only
to the three new scripts — **flagging a likely merge collision** when both
branches eventually land; whoever merges second should reconcile the two
`permissions.allow` lists rather than let one silently overwrite the other.

## Verification (against the real Project #50 data, not a fixture)

- `gh_fetch.py` run end-to-end: 320 total items → 167 done / 58 open / 50
  drafts / 43 PRs / 2 skipped (no status match), reconciling exactly to 320.
- Rate-limit cost: ~7 points for the full pull (well inside the 5,000/hr
  budget) — the nested single-query-per-page design needed no N+1 fallback.
- Issue #618 spot-checked by hand against its live timeline (`Ready to
  select` → `In progress` → `Ready for Demo`) — `open.json`'s entry matched
  exactly (status + 3-entry changelog).
- `forecast.py`/`forecast-html.py` ran **unmodified** against the output:
  produced Q1 (27/33/37 weeks to clear the 58-item open queue at 50/85/95%),
  Q2 (3/3/5 items by the +3-week horizon), Q3 (cycle-time SLE: 1/5/7 days at
  50/85/95%, started→Done basis), and Q4 (6 items in flight, 1 over the SLE).
  The outlier detector correctly flagged two anomalous weeks in the real
  `closedAt` history (unrelated to the import-artifact fix above — these are
  genuine historical bulk-close weeks from 2020, exactly what that detector
  is designed to surface).

## Links

- Issue: #653
- Project: https://github.com/orgs/willowtreeapps/projects/50/views/1
- Source skill (private): `willowtreeapps/dq-documentation:skills/throughput-forecast/`
