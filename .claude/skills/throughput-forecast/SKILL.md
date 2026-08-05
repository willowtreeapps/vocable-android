---
name: throughput-forecast
description: >-
  Forecast delivery from a team's historical weekly throughput — no story points,
  no velocity. Answers "how long to clear this backlog?" and "how many items done
  by a date?" at 50/85/95% confidence, for the team as a whole and (optionally)
  broken down by component. Also reports the cycle-time Service Level Expectation
  (the per-item estimate replacement) and an aging work-in-progress list. Use when
  the user asks to forecast delivery, predict a completion date, estimate how much
  fits before a date, replace velocity/sprint commitment for a Kanban team, or asks
  "when will this be done?" / "can we finish by a date?". Triggers on: "delivery
  forecast", "throughput forecast", "Monte Carlo", "probabilistic forecast", "when
  will the backlog be done", "cycle time", "SLE", "service level expectation",
  "aging WIP", "what should we pull first". This is the GitHub Projects v2 port of
  WillowTree's Jira-based throughput-forecast skill — same forecast engine,
  different data source.
---

# Throughput Forecasting (GitHub Projects v2)

Project-agnostic, throughput-based delivery forecasting for a Kanban workflow,
sourced from a **GitHub Projects v2** board instead of Jira. Replaces sprint
commitment / velocity with a probabilistic forecast computed by simulation
(Monte Carlo) from the team's *actual* weekly throughput — the count of items
reaching a Done status per week. No estimation, no story points.

It answers four questions. Q1/Q2 are the stakeholder-facing **forecast** (each at
50 / 85 / 95% confidence); Q3/Q4 are the team-facing **flow metrics**:

1. **How long** to clear a backlog of N items?  *(Monte Carlo)*
2. **How many** items done by a target date?    *(Monte Carlo)*
3. **What can we promise per item?** — the cycle-time **Service Level Expectation
   (SLE)**, the measured per-item commitment that replaces the story-point estimate.
4. **What should we pull first?** — **aging work-in-progress**: in-flight items
   already older than the SLE, i.e. the "finish-before-you-start" standup list.

Q1/Q2 also break down **by component** if `config.yaml`'s `breakdown_field` is set
(`labels` or `issuetype`). Q3/Q4 render only when their data is available.

**Setup / porting:** the forecast engine (`forecast.py`/`forecast-html.py`) is
copied byte-for-byte from the original Jira-based skill (`willowtreeapps/
dq-documentation:skills/throughput-forecast/scripts/`) and needed **zero
changes** for this port. It always runs here in **file mode** — pure math, no
network calls — reading the two JSON files `gh_fetch.py` produces. The copy
also still carries `forecast.py`'s Jira `--live` path (`load_token()`,
`run_curl()`, direct REST against `--jira-base`) from the source skill; this
skill never passes `--live`, so that path is dead code here, not something in
use — left in place rather than trimmed so this file stays a straight diff
against the upstream source for future fixes. Only the gather step differs:
`gh_fetch.py` pulls from the GitHub Projects v2 GraphQL API instead of Jira
REST. **Project values** (org, project number, repo, statuses, breakdown
axis) live in `config.yaml` — read it first and pass them as flags.

## First — orient the user (before pulling data)

Open with a quick 2–3 sentence blurb. Adapt, don't paste verbatim:

> **throughput-forecast** predicts delivery from your team's *actual* weekly
> throughput — no story points, no velocity. Ask it "how long to clear the
> backlog?", "how many items ship by a date?", "what's our cycle-time SLE?" —
> and it flags aging work-in-progress to pull before starting anything new.
> This runs against GitHub Project #50, not Jira.

## Then — confirm the throughput window (ASK before pulling)

Ask which trailing window to forecast from — **10 weeks** (quick) or **26 weeks**
(~2 quarters — steadier, smooths noise). Default 26 unless they want the quick
read. Carry the chosen `WEEKS` into `forecast.py --weeks WEEKS`.

## Data pull — one GraphQL pass (~seconds; trivial rate-limit cost)

Read `config.yaml` for `github.org`, `github.project_number`, `github.repo`,
`backlog.statuses`, `throughput.done_statuses`, `breakdown_field`, and
`cycle.*`. Then:

```bash
python3 .claude/scripts/gh_fetch.py \
  --org <github.org> --project-number <github.project_number> --repo <github.repo> \
  --done-status <throughput.done_statuses…> \
  --statuses <backlog.statuses…> \
  --breakdown-field <breakdown_field> \
  --out-dir /tmp/forecast

python3 .claude/scripts/forecast.py \
  --done /tmp/forecast/done.json --open /tmp/forecast/open.json \
  --changelog /tmp/forecast/done.json /tmp/forecast/open.json \
  --project "Vocable-Android" \
  --components <components…> \
  --wip-status <cycle.wip_statuses…> \
  --reset-status <cycle.reset_statuses…> --start-status <cycle.start_status> \
  --weeks <WEEKS> --by-date <YYYY-MM-DD> --seed 42
```

`gh_fetch.py` shells out to `gh api graphql` (reusing the machine's existing `gh
auth login` — no token in `settings.json`), nests each Issue's status-change
timeline in the same paginated query (confirmed ~1 rate-limit point per 50-item
page — no per-item N+1 calls), and back-fills any item with >50 status changes
the same way the Jira version back-fills truncated changelogs. Passing the same
files to `--changelog` makes **started→Done the every-run default** (the honest,
active-time SLE), exactly as in the Jira version — `forecast.py` doesn't know or
care that the data came from GitHub instead of Jira.

A sanity summary (item counts by bucket, timelines backfilled, resolutiondate
fallback/disagreement counts) prints to **gh_fetch.py's stderr** — read it before
quoting numbers; the done+open+excluded+skipped counts should sum to the
project's total item count. A second summary (weeks kept/dropped, cycle-time
percentiles, excluded counts) prints from `forecast.py` itself. If `gh_fetch.py`
exits `ERROR(6)` (zero Done items), the `--org`/`--project-number`/`--done-status`
is wrong — fix it, don't hand-assemble numbers.

| `gh_fetch.py` flag | Meaning |
|------|---------|
| `--org` / `--project-number` | which GitHub Projects v2 board |
| `--repo` | repo name (for the timeline-backfill query and issue keys) |
| `--done-status …` | status name(s) counted as delivered |
| `--statuses …` | open-queue (active/backlog) status names |
| `--breakdown-field none\|labels\|issuetype\|parent` | what feeds forecast.py's `--components` axis |
| `--include-drafts` | include DraftIssue project items (default: excluded) |
| `--include-prs` | **not implemented** — see the script's docstring; a real PR showed `timelineItems.totalCount` not respecting the itemTypes filter, so the truncated-changelog backfill can't be trusted for PRs yet |
| `--include-parents` | include issues that are themselves a parent of other sub-issues as their own line item (default: excluded — see below) |
| `--since YYYY-MM-DD` | only include items created on/after this date |

`forecast.py`'s own flags are unchanged from the Jira version — see its
`--help` or the original skill's docs for the full table (`--weeks`,
`--by-date`, `--outliers`, `--cycle-max-days`, `--json-out`, etc.).

## DraftIssue, PullRequest, and parent issues are excluded (default) — the Epic-exclusion analog

The Jira version excludes `issuetype = Epic` by default (placeholders, not
deliverable flow). GitHub Projects v2 has no Epic concept, but has three
analogous "not deliverable flow" categories: **DraftIssue** items (notes, not
real issues — excluded by default, `--include-drafts` to opt in), **PullRequest**
items (already reachable via an issue's linked PRs — always excluded;
`--include-prs` is not yet implemented, see above), and **parent issues** —
an issue that is itself the parent of other sub-issues (confirmed live on
#613: 18 sub-issues, 10 complete, parent still `OPEN`). A parent stays
open/in-progress for its *entire* sub-issue lifetime, so counting it as its
own throughput/open-queue item would badly distort both — same reasoning as
the Epic exclusion, just GitHub's structural equivalent. Excluded by default;
`--include-parents` to opt in. A parent still works as a `--breakdown-field
parent` grouping label for its children regardless of this flag — the flag
only controls whether the parent *also* shows up as its own line item.

## Breaking down by parent (the Epic-analog grouping)

`--breakdown-field parent` groups Q1/Q2 by each item's native GitHub
sub-issue parent (e.g. `"#613 Feature: Voice Selection — Hybrid…"`) instead
of by label or issue type — the closest thing this board has to a Jira
Epic/feature grouping, since it has no Epic issue type. Items with no parent
link don't get a component and only show up in the TEAM line. If a project
has multiple unrelated parents on the board, you'll likely want to filter
`forecast.py --components` down to just the ones relevant to the current
ask (see `run_daily_forecast.py`'s `ALLOWED_PARENT_NUMBERS` pattern for an
example that scopes a scheduled report to one feature) rather than passing
every discovered parent through — a component series with very few or zero
Done items in the window renders as "no throughput data in window" rather
than crashing (both `forecast.py` and `forecast-html.py` handle a
zero-length sample series gracefully for exactly this reason).

## Cycle-time basis — started→Done (Q3/Q4)

Same as the Jira version: a created→Done cycle time bakes in backlog wait and
inflates the SLE. `--changelog` turns on **started→Done** (active time): start
= first entry to `--start-status`, reset on any return to a `--reset-status`.
The status-change timeline `gh_fetch.py` pulls (`ProjectV2ItemStatusChangedEvent`)
is a direct analog of Jira's changelog, so this works identically.

## HTML report (optional)

After the terminal summary, ask whether they want just the terminal read or a
shareable **HTML report**. If yes, re-run with `--json-out` and render:

```bash
python3 .claude/scripts/forecast.py … --json-out /tmp/forecast.json
python3 .claude/scripts/forecast-html.py /tmp/forecast.json ~/Desktop/forecast-<date>.html
```

Same `forecast-html.py` as the Jira version, unmodified — it only ever
consumes `forecast.py --json-out`'s output.

## After running

- Lead with the **85% confidence** lines. Quote 85% to stakeholders, keep 50%
  for the team.
- Quote the **cycle-time 85% SLE** as the per-item commitment.
- Call out any **over-SLE** items by key — the "pull before you start new
  work" tickets for the next standup.
- The forecast is valid only while team size & flow are stable — re-run after
  a reorg or a long holiday.
- **Component forecasts are independent** — the TEAM line is not the sum of
  the component lines (variances don't add).
