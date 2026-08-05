# #664 — throughput-forecast: breakdown by parent, exclude parent-container issues, daily Voice Selection report

## What

Extended the GitHub-Projects throughput-forecast skill (built in #653) so it
can forecast a single feature's delivery, not just team-wide throughput:

- `gh_fetch.py`: new `--breakdown-field parent` groups items by their native
  GitHub sub-issue parent (e.g. `"#613 Feature: Voice Selection — Hybrid…"`),
  and (this issue's own addition) issues that are themselves a parent of
  other sub-issues are now **excluded from done/open by default**
  (`--include-parents` to opt in) — the same reasoning as the Jira Epic
  exclusion, applied to GitHub's structural equivalent.
- `forecast.py`/`forecast-html.py`: handle a `--components` series with zero
  throughput weeks gracefully (renders "no throughput data in window"
  instead of a `ZeroDivisionError`/crash) — needed because a freshly-linked
  parent group can easily have no Done items yet.
- `run_daily_forecast.py` (new): a scheduled-task driver that fetches
  Project #50, forecasts scoped to Voice Selection (`#613`) only via an
  allowlist, and writes a dated + "latest" HTML report straight into the
  Drive-synced `Metrics` folder.
- `SKILL.md`/`config.yaml` updated to document all of the above.

## Why

The team wants delivery visibility scoped to Voice Selection specifically,
not just team-wide throughput. This board has no Epic issue type, so there's
no built-in "feature" grouping — but the native GitHub sub-issue parent link
(added in #654) gives us exactly that, once two gaps are closed: the parent
issue itself needs to be excluded (it sits open for its whole sub-issue
lifetime and would badly distort its own group — confirmed live: #613 has 18
sub-issues, 10 complete, and is still `OPEN`), and the forecast engine needs
to tolerate a component series that has too little (or zero) history without
crashing.

## Key decisions

**This breaks byte-for-byte parity between `forecast.py` and the upstream
Jira skill, established as a deliberate goal in #653 — worth being explicit
about, since a reviewer on #655 specifically praised keeping it unmodified.**
The zero-length-series guards (`if s["sample"]:` branches in the Q1/Q2/JSON
sections, `rsplit(":", 2)` instead of `split(":")` for component names that
may contain colons) are genuine behavioral fixes needed to support
`--breakdown-field parent` safely, not cosmetic changes — a parent-scoped
series realistically can have zero Done items in a 10-26 week window, and
component/parent titles routinely contain colons (`"Day 2: Capture…"` style),
which the original `spec.split(":")` would mis-parse. Both are real bugs
`--breakdown-field parent` exposes that `labels`/`issuetype` mostly wouldn't.
This trades the "clean upstream diff" property (defended in #653/#655) for
correctness under the new breakdown axis — a straight upstream sync is no
longer possible without re-reconciling these fixes, and that's the right
call here, but should stay visible rather than get silently reintroduced as
an implicit assumption next time someone touches these files.

**Parent-container exclusion uses `subIssuesSummary.total > 0`, not issue
type or title conventions.** This is a structural check (does this issue
have sub-issues linked to it), not a naming/labeling heuristic — confirmed
against #613 live before writing the fix, and it composes correctly with
`--breakdown-field parent`: an item can simultaneously be *excluded* from
done/open (because it has children) and still *used* as the grouping label
for those same children, since the exclusion check and the grouping lookup
(`content.parent`) look at different relationships (this item's own
children vs. this item's own parent).

**`run_daily_forecast.py` hardcodes its own repo/Drive paths and a narrow
`ALLOWED_PARENT_NUMBERS = {613}` allowlist** rather than reading
`config.yaml` — this is a single-purpose scheduled-task script for this one
feature's reporting cadence, not a generic reusable skill entry point (that
role stays with `gh_fetch.py`/`forecast.py` driven through `SKILL.md`). The
allowlist is deliberate scope-narrowing (per direct instruction), not a
placeholder to widen later — revisit once voice-selection merges to `main`,
per its own docstring.

## Verification

- Confirmed live: #613 has `subIssuesSummary.total = 18`, `completed = 10`,
  `state = OPEN` — the exact "sits open for its whole lifetime" case this
  exclusion targets.
- Ran `gh_fetch.py --breakdown-field parent` against the real Project #50:
  322 total items, 168 done / 55 open / 50 drafts / 43 PRs / **4 parent
  issues excluded** / 2 skipped — sums to 322. Confirmed #613 is absent from
  both `done.json` and `open.json`, while its sub-issues correctly carry the
  `"#613 Feature: Voice Selection…"` grouping label.
  Discovered 3 parent groups on the board in total (`#613`, `#615` "Day 1
  SPIKE…", `#627` "Reset App Settings"); `run_daily_forecast.py`'s allowlist
  correctly narrows this to just `#613`.
- Ran the full `run_daily_forecast.py` end-to-end: `gh_fetch.py` →
  `forecast.py` → `forecast-html.py`, writing both a dated and "latest" HTML
  report into the Drive-synced Metrics folder. Terminal summary showed the
  `#613` series's own throughput distinct from TEAM (`tp [0,0,0,0,0,0,0,0,0,2]
  open 5` vs. team's `[1,2,3,15,3,1,1,1,1,4] open 55`) — confirming the
  breakdown is real, not just a relabeled copy of the team line.

## Links

- Issue: #664
- Prior work: #653 (GitHub Projects port), #654 (create-ticket project auto-add/sub-issue linking)
- Project: https://github.com/orgs/willowtreeapps/projects/50/views/1
