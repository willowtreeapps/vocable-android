#!/usr/bin/env python3
"""
forecast.py — throughput-based delivery forecast.

Forecasts delivery WITHOUT story-point estimation, using only the team's
historical weekly throughput (items reaching a Done status) via a probabilistic
(Monte Carlo) simulation. Replaces sprint commitment / velocity for a Kanban
team. Forecasts the team as a whole and, optionally, broken down by component.

Four questions, one toolkit:
  1. "How long to clear N items?"   -> weeks-to-complete distribution  (Monte Carlo)
  2. "How many done by <date>?"      -> items-completed distribution     (Monte Carlo)
  3. "What can we promise per item?" -> cycle-time Service Level Expectation (SLE)
  4. "What should we pull first?"    -> aging work-in-progress vs the SLE

Q1/Q2 are the stakeholder-facing FORECAST. Q3/Q4 are the team-facing FLOW
metrics that make a team behave like Kanban day-to-day: Q3 replaces the story-
point estimate with a measured per-item commitment, Q4 is the daily-standup
"finish-before-you-start" artifact.

DATA SOURCE:
  The PRIMARY path is FILE MODE: run fetch.py once (one REST pass — Done + open
  queue + all changelogs inline) to write done.json / open.json, then pass those
  to --done/--open/--changelog here. The engine itself never touches the network
  in file mode; it's pure math over two JSON files, so it's tracker-agnostic (any
  tracker can produce the same JSON envelope — see the SETUP-GUIDE).

  PURE-MATH mode (numbers passed in as flags) and LIVE mode (--live, direct REST
  with --jira-base/--project) remain as fallbacks:

  --throughput "37,41,28,27,31,55,20"   weekly item counts, oldest->newest
  --items N                              backlog size to clear
  --cycle-times "3,5,8,2,14,..."         per-item cycle time in DAYS (created->Done)
  --wip-item "ABC-1234:In Progress:12"   one in-flight item (key:status:age_days)

Usage:
    # FILE MODE (preferred): ingest fetch.py's output directly — one command.
    forecast.py --done done.json --open open.json --by-date 2026-07-21 \
        --project ABC --jira-base https://you.atlassian.net
    #   add --changelog done.json open.json -> Q3/Q4 switch to started→Done
    #   (active time from the changelog)

    # PURE-MATH: numbers passed in
    forecast.py --throughput "37,41,28,27,31,55,20" --items 60
    forecast.py --throughput "..." --items 60 \
        --cycle-times "3,5,8,2,14,6,21,4" \
        --wip-item "ABC-1234:In Progress:18" --wip-item "ABC-1250:Code Review:5"

    # LIVE (direct REST)
    forecast.py --live --jira-base https://you.atlassian.net --project ABC \
        [--weeks 10] [--done-status "Done"]

Defaults: --by-date = 3 weeks out | --trials 10000 | --seed 42

Exit codes: 0 ok | 2 config/token | 3 curl/network | 4 non-JSON | 5 API error
"""

import argparse
import json
import math
import os
import random
import subprocess
import sys
import tempfile
from collections import Counter, defaultdict
from datetime import datetime, date, timedelta

SETTINGS_PATH = os.path.expanduser("~/.claude/settings.json")
JIRA_BASE = None   # set from --jira-base in main() (live mode / report links)


def backlog_jql(project, statuses):
    """Live-mode open-queue JQL, built from --project + --wip-status."""
    status_in = ",".join(f'"{s}"' for s in statuses)
    return (f"project = {project} AND statusCategory != Done "
            f"AND status in ({status_in})")


def exclude_epics(jql):
    """Insert `AND issuetype != Epic` before any trailing ORDER BY. Epics are
    typically placeholders/containers, not deliverable flow — excluded by
    default (override with --include-epics)."""
    marker = " ORDER BY "
    idx = jql.upper().rfind(marker)
    clause = " AND issuetype != Epic"
    return jql + clause if idx == -1 else jql[:idx] + clause + jql[idx:]


def _is_epic(node):
    """True if a raw issue node is an Epic (defensive file-mode guard, so a stale
    done.json/open.json that predates the JQL exclusion can't skew the math)."""
    it = (node.get("fields", {}) or {}).get("issuetype") or {}
    return (it.get("name") or "").strip().lower() == "epic"


# --------------------------------------------------------------------------- #
# Atlassian REST plumbing (live mode only; file mode never hits the network)
# --------------------------------------------------------------------------- #
def load_token():
    try:
        with open(SETTINGS_PATH) as f:
            cfg = json.load(f)
    except FileNotFoundError:
        sys.exit(f"ERROR: settings file not found at {SETTINGS_PATH}")
    except json.JSONDecodeError as e:
        sys.exit(f"ERROR: settings file is not valid JSON: {e}")
    try:
        auth = cfg["mcpServers"]["atlassian"]["headers"]["Authorization"]
    except KeyError:
        sys.exit("ERROR: mcpServers.atlassian.headers.Authorization not found")
    if not auth.startswith("Basic "):
        sys.exit("ERROR: Authorization header is not Basic auth")
    return auth


def run_curl(method, url, auth_header, body=None):
    fd, config_path = tempfile.mkstemp(prefix="atlassian-", suffix=".curlrc")
    try:
        os.fchmod(fd, 0o600)
        with os.fdopen(fd, "w") as f:
            f.write(f'header = "Authorization: {auth_header}"\n')
            f.write('header = "Accept: application/json"\n')
            if body is not None:
                f.write('header = "Content-Type: application/json"\n')
        cmd = ["curl", "-sS", "--fail-with-body", "--max-time", "30",
               "--config", config_path, "-X", method, url]
        if body is not None:
            cmd += ["-d", body]
        result = subprocess.run(cmd, capture_output=True, text=True)
        if result.returncode != 0:
            print(f"ERROR: curl exited {result.returncode}", file=sys.stderr)
            print(result.stderr, file=sys.stderr)
            sys.exit(3)
        return result.stdout
    finally:
        try:
            os.unlink(config_path)
        except OSError:
            pass


def parse_response(raw):
    try:
        data = json.loads(raw)
    except json.JSONDecodeError:
        print("ERROR: API did not return JSON. First 500 chars:", file=sys.stderr)
        print(raw[:500], file=sys.stderr)
        sys.exit(4)
    if isinstance(data, dict) and (data.get("errorMessages") or data.get("errors")):
        print("ERROR: Atlassian returned a structured error:", file=sys.stderr)
        print(json.dumps(data, indent=2), file=sys.stderr)
        sys.exit(5)
    return data


def jql_all(jql, auth, fields):
    """Paginate /search/jql via nextPageToken; return every issue node."""
    issues, token = [], None
    while True:
        payload = {"jql": jql, "fields": fields, "maxResults": 100}
        if token:
            payload["nextPageToken"] = token
        data = parse_response(
            run_curl("POST", f"{JIRA_BASE}/rest/api/3/search/jql",
                     auth, body=json.dumps(payload)))
        issues.extend(data.get("issues", []))
        token = data.get("nextPageToken")
        if not token:
            return issues


def jql_count(jql, auth):
    """Exact-ish backlog size via the approximate-count endpoint."""
    data = parse_response(
        run_curl("POST", f"{JIRA_BASE}/rest/api/3/search/approximate-count",
                 auth, body=json.dumps({"jql": jql})))
    return int(data.get("count", 0))


# --------------------------------------------------------------------------- #
# Throughput
# --------------------------------------------------------------------------- #
def weekly_throughput(auth, weeks, done_statuses, project, include_epics=False):
    """Items resolved per complete ISO week over the lookback window.

    Drops the current (partial) week so it doesn't bias the sample low.
    """
    lookback_days = (weeks + 1) * 7
    status_list = ",".join(f'"{s}"' for s in done_statuses)
    jql = (f'project = {project} AND status in ({status_list}) '
           f'AND resolutiondate >= -{lookback_days}d ORDER BY resolutiondate ASC')
    if not include_epics:
        jql = exclude_epics(jql)
    issues = jql_all(jql, auth, ["resolutiondate"])

    this_week = date.today().isocalendar()[:2]   # (iso_year, iso_week)
    buckets = Counter()
    for it in issues:
        rd = it["fields"].get("resolutiondate")
        if not rd:
            continue
        d = datetime.strptime(rd[:10], "%Y-%m-%d").date()
        key = d.isocalendar()[:2]
        if key == this_week:
            continue   # exclude the in-progress week
        buckets[key] += 1

    # Keep only the most recent `weeks` complete weeks, oldest -> newest.
    ordered = sorted(buckets.items())[-weeks:]
    return [count for _, count in ordered], ordered


# --------------------------------------------------------------------------- #
# Monte Carlo
# --------------------------------------------------------------------------- #
def sim_weeks_to_finish(backlog, sample, trials, rng):
    """Distribution of #weeks to clear `backlog` items."""
    out = []
    for _ in range(trials):
        remaining, w = backlog, 0
        while remaining > 0:
            remaining -= rng.choice(sample)
            w += 1
            if w > 520:           # 10yr safety valve (throughput all-zero etc.)
                break
        out.append(w)
    out.sort()
    return out


def sim_items_by_date(n_weeks, sample, trials, rng):
    """Distribution of #items completed over `n_weeks`."""
    out = [sum(rng.choice(sample) for _ in range(n_weeks)) for _ in range(trials)]
    out.sort()
    return out


def pct(sorted_list, p):
    if not sorted_list:
        return 0
    idx = min(int(round(p / 100 * (len(sorted_list) - 1))), len(sorted_list) - 1)
    return sorted_list[idx]


def _median(vals):
    s = sorted(vals)
    n = len(s)
    if n == 0:
        return 0.0
    return float(s[n // 2]) if n % 2 else (s[n // 2 - 1] + s[n // 2]) / 2


def detect_outliers(sample, thresh=3.5):
    """Flag weeks that deviate from the series by a ROBUST modified z-score
    (median + MAD; Iglewicz–Hoaglin). Robust to the outlier itself — unlike a
    mean/σ z-score, which a spike inflates enough to hide. Needs >=4 weeks.

    Returns [(index, value, mz), ...] for each flagged week (high OR low)."""
    n = len(sample)
    if n < 4:
        return []
    med = _median(sample)
    mad = _median([abs(x - med) for x in sample])
    if mad > 0:
        scale = 0.6745 / mad
    else:
        # >half the weeks identical → MAD is 0; fall back to mean-abs-deviation
        # (the Iglewicz–Hoaglin recommendation) so we don't divide by zero.
        mean_ad = sum(abs(x - med) for x in sample) / n
        if mean_ad == 0:
            return []                       # every week identical → nothing to flag
        scale = 0.7979 / mean_ad            # 1 / 1.253314
    return [(i, x, scale * (x - med)) for i, x in enumerate(sample)
            if abs(scale * (x - med)) > thresh]


def apply_outlier_policy(sample, flagged, policy):
    """Return (sim_sample, note) per policy. keep: unchanged. winsorize: cap each
    flagged week at the nearest in-band week (preserves sample size). drop: remove
    flagged weeks, but keep >=3 so the simulation still has a sample."""
    if not flagged or policy == "keep":
        return list(sample), None
    fidx = {i for i, _, _ in flagged}
    kept = [x for i, x in enumerate(sample) if i not in fidx]
    if policy == "drop":
        if len(kept) < 3:
            return list(sample), "too few weeks left to drop — kept as-is"
        return kept, f"dropped {len(fidx)} outlier week(s) from the sim sample"
    if policy == "winsorize":
        hi, lo = (max(kept), min(kept)) if kept else (max(sample), min(sample))
        med = _median(sample)
        adj = [(hi if x > med else lo) if i in fidx else x
               for i, x in enumerate(sample)]
        return adj, f"winsorized {len(fidx)} outlier week(s) to [{lo}, {hi}]"
    return list(sample), None


# --------------------------------------------------------------------------- #
# File mode — ingest fetch.py's result files directly (all the ISO-week
# bucketing, per-component split, and the started→Done math when --changelog
# files are supplied). This is what makes the whole forecast run a SINGLE
# allow-listed command: no intermediate flag string, no `$(cat …)`, no heredoc —
# so it never prompts.
# --------------------------------------------------------------------------- #
def _day(s):
    """Date portion of a Jira timestamp like 2026-04-14T13:53:10.217-0400."""
    return date.fromisoformat(s[:10])


def _load_nodes(paths):
    nodes = []
    for path in paths:
        try:
            with open(path) as f:
                data = json.load(f)
        except FileNotFoundError:
            sys.exit(f"ERROR: file not found: {path}")
        except json.JSONDecodeError as e:
            sys.exit(f"ERROR: {path} is not valid JSON: {e}")
        nodes.extend((data.get("issues", {}) or {}).get("nodes", []) or [])
    return nodes


def _comp_names(node, wanted):
    cs = node.get("fields", {}).get("components") or []
    return [c["name"] for c in cs if c.get("name") in wanted]


def _status_transitions(changelog):
    out = []
    for h in (changelog or {}).get("histories", []) or []:
        ts = h.get("created", "")
        for it in h.get("items", []) or []:
            if it.get("field") == "status":
                out.append((ts, it.get("toString")))
    out.sort(key=lambda x: x[0])
    return out


def _started_day(node, start_status, reset_statuses):
    """Date the final active stint began (first In Progress, reset on bounce to
    Ready-for-Dev/Cancelled), or (None, reason) if it can't be determined."""
    cl = node.get("changelog") or {}
    total = cl.get("total")
    hist = cl.get("histories", []) or []
    truncated = isinstance(total, int) and total > len(hist)
    start = None
    for ts, to in _status_transitions(cl):
        if to in reset_statuses:
            start = None                     # bounced to backlog → reset the clock
        elif to == start_status and start is None:
            start = ts                       # first In Progress of the current stint
    if start is None:
        return None, ("truncated" if truncated else "nostart")
    if truncated:
        return None, "truncated"
    return _day(start), None


def synthesize_from_files(args):
    """Populate args.throughput/items/component/cycle_times/wip_item/cycle_basis
    from fetch.py's result files, exactly as if the numeric flags had been passed
    — then the existing pure-math path runs unchanged. Sanity summary → stderr."""
    if not args.open_files:
        sys.exit("ERROR: --done requires --open (both fetch.py files are needed)")
    today = (datetime.strptime(args.today, "%Y-%m-%d").date()
             if args.today else date.today())
    # throughput window: an explicit [--since, --until] (e.g. two calendar quarters)
    # or the trailing -Nd fallback. --today is the aging-WIP now-clock only.
    win_lo = win_hi = None
    if args.since:
        since = datetime.strptime(args.since, "%Y-%m-%d").date()
        until = (datetime.strptime(args.until, "%Y-%m-%d").date()
                 if args.until else today)
        win_lo, win_hi = since, until
        drop = {since.isocalendar()[:2], until.isocalendar()[:2]}
        if since <= today <= until:              # window includes the live week
            drop.add(today.isocalendar()[:2])
    else:
        cutoff = date.fromordinal(today.toordinal() - args.lookback_days)
        drop = {today.isocalendar()[:2], cutoff.isocalendar()[:2]}
    comps = args.components
    compset = set(comps)

    # throughput + created→Done cycle times (Done query)
    team_wk, comp_wk, cycle_days = Counter(), defaultdict(Counter), []
    epics_skipped = 0
    for n in _load_nodes(args.done):
        if not args.include_epics and _is_epic(n):
            epics_skipped += 1
            continue
        rd = n.get("fields", {}).get("resolutiondate")
        if not rd:
            continue
        d = _day(rd)
        if win_lo is not None and not (win_lo <= d <= win_hi):
            continue                             # outside the explicit window → ignore
        wk = d.isocalendar()[:2]
        team_wk[wk] += 1
        for c in _comp_names(n, compset):
            comp_wk[c][wk] += 1
        created = n.get("fields", {}).get("created")
        if created:
            cycle_days.append(max(0, (d - _day(created)).days))
    complete = sorted(w for w in team_wk if w not in drop)
    if args.weeks is not None:                   # keep the most recent N complete weeks
        complete = complete[-args.weeks:]
    team_series = [team_wk[w] for w in complete]
    comp_series = {c: [comp_wk[c][w] for w in complete] for c in comps}

    # open queue + created→now aging WIP (open query)
    team_open, comp_open, wip = 0, Counter(), []
    wipset = set(args.wip_status)
    for n in _load_nodes(args.open_files):
        if not args.include_epics and _is_epic(n):
            epics_skipped += 1
            continue
        team_open += 1
        for c in _comp_names(n, compset):
            comp_open[c] += 1
        status = n.get("fields", {}).get("status", {}).get("name", "")
        if status in wipset:
            created = n.get("fields", {}).get("created")
            if created:
                wip.append({"key": n.get("key", "?"), "status": status,
                            "age": max(0, (today - _day(created)).days)})

    cycle_days.sort()
    raw_n, dropped = len(cycle_days), 0
    if args.cycle_max_days is not None:
        kept = [d for d in cycle_days if d <= args.cycle_max_days]
        dropped = raw_n - len(kept)
        cycle_days = kept

    # started→Done basis (Q3/Q4) when changelog files are supplied
    basis, excluded, started_used = "created", [], 0
    if args.changelog:
        basis = "started"
        reset = set(args.reset_status)
        started, started_ages, seen = [], {}, set()
        for n in _load_nodes(args.changelog):
            if not args.include_epics and _is_epic(n):
                continue
            key = n.get("key")
            if key in seen:
                continue
            seen.add(key)
            f = n.get("fields", {})
            sd, reason = _started_day(n, args.start_status, reset)
            if sd is None:
                excluded.append(f"{key}:{reason}")
                continue
            rd = f.get("resolutiondate")
            if rd:
                started.append(max(0, (_day(rd) - sd).days))     # completed → cycle time
            else:
                started_ages[key] = max(0, (today - sd).days)    # in-flight → age
        started.sort()
        if args.cycle_max_days is not None:
            started = [d for d in started if d <= args.cycle_max_days]
        cycle_days = started
        for w in wip:
            if w["key"] in started_ages:
                w["age"] = started_ages[w["key"]]
                started_used += 1

    wip.sort(key=lambda x: x["age"], reverse=True)

    # hand off to the existing pure-math path
    args.throughput = ",".join(map(str, team_series))
    args.items = team_open
    args.component = [f"{c}:{','.join(map(str, comp_series[c]))}:{comp_open[c]}"
                      for c in comps]
    args.cycle_times = ",".join(map(str, cycle_days)) if cycle_days else None
    args.wip_item = [f"{w['key']}:{w['status']}:{w['age']}" for w in wip]
    args.cycle_basis = basis

    def note(s): print(s, file=sys.stderr)
    window_desc = (f"{args.since}→{args.until or today.isoformat()}" if args.since
                   else f"last {args.lookback_days}d")
    trim = f" (trimmed to last {args.weeks} wks)" if args.weeks else ""
    note("# forecast (file mode) summary")
    note(f"#   now-clock      : {today.isoformat()}   window: {window_desc}{trim}")
    if args.include_epics:
        note("#   epics          : INCLUDED (--include-epics)")
    else:
        note(f"#   epics          : excluded (placeholders); {epics_skipped} "
             "filtered from the input files")
    note(f"#   complete weeks : {len(complete)} kept  "
         f"({', '.join(f'{y}-W{w:02d}' for (y, w) in complete)})")
    note(f"#   dropped(partial): {', '.join(sorted(f'{y}-W{w:02d}' for (y, w) in drop))}")
    note(f"#   team throughput: {team_series}  (open queue {team_open})")
    for c in comps:
        note(f"#   {c:<8}: tp {comp_series[c]}  open {comp_open[c]}")
    if cycle_days:
        lbl = "started→Done" if basis == "started" else "created→Done"
        cap = (f"  [capped ≤{args.cycle_max_days:.0f}d, dropped {dropped} of {raw_n}]"
               if (basis == "created" and args.cycle_max_days is not None) else "")
        note(f"#   cycle times    : N={len(cycle_days)}{cap}  ({lbl})")
        note(f"#     percentiles  : p50 {pct(cycle_days,50):.0f}d  p75 "
             f"{pct(cycle_days,75):.0f}d  p85 {pct(cycle_days,85):.0f}d  p95 "
             f"{pct(cycle_days,95):.0f}d  max {cycle_days[-1]:.0f}d")
    if args.changelog:
        note(f"#   started ages   : {started_used}/{len(wip)} in-flight items "
             f"matched a changelog (rest kept created→now age)")
        if excluded:
            reasons = Counter(e.rsplit(":", 1)[1] for e in excluded)
            summary = ", ".join(f"{r} ×{c}" for r, c in reasons.most_common())
            note(f"#   changelog excluded: {len(excluded)} ({summary}); "
                 f"first 10: {' '.join(excluded[:10])}")
    note(f"#   aging WIP      : {len(wip)} in flight "
         f"(statuses: {', '.join(args.wip_status)})")


# --------------------------------------------------------------------------- #
# Report
# --------------------------------------------------------------------------- #
def bar(n, scale):
    return "█" * max(0, min(40, int(round(n / scale)))) if scale else ""


def main():
    p = argparse.ArgumentParser(description="Monte Carlo delivery forecast from throughput")
    p.add_argument("--project", default=None,
                   help="project key/label for the report title & ticket links "
                        "(required for --live)")
    p.add_argument("--jira-base", default=None,
                   help="Atlassian site, e.g. https://you.atlassian.net — required "
                        "for --live; in file mode it only sets the report's ticket links")
    p.add_argument("--throughput", default=None,
                   help='pure-math mode: comma weekly counts oldest->newest, '
                        'e.g. "37,41,28,27,31,55,20"')
    p.add_argument("--items", type=int, default=None, help="backlog size to clear")
    p.add_argument("--component", action="append", default=[],
                   help='per-component series, repeatable. Format '
                        '"Name:t1,t2,...:items"  e.g. "Web:6,6,6,8,9,27,6:233"')
    p.add_argument("--live", action="store_true",
                   help="pull data from REST (needs a REST-scoped token)")
    p.add_argument("--weeks", type=int, default=None,
                   help="[file mode] keep only the most recent N COMPLETE weeks of "
                        "throughput (e.g. 10 quick, 26 steadier); omit to keep all "
                        "complete weeks in the window.  [live] REST lookback weeks "
                        "(defaults to 10 when omitted).")
    p.add_argument("--backlog-jql", default=None,
                   help="[live] full override; else built from --project + --wip-status")
    p.add_argument("--include-epics", action="store_true",
                   help="include Epic issues (default: exclude — Epics are usually "
                        "placeholders, not deliverable flow). Applies to live JQL "
                        "and to the file-mode defensive filter.")
    p.add_argument("--cycle-times", default=None,
                   help='pure-math: comma per-item cycle times in DAYS '
                        '(created->Done) for completed items, e.g. "3,5,8,2,14". '
                        'Enables the cycle-time SLE (Q3).')
    p.add_argument("--cycle-file", default=None,
                   help="read --cycle-times from a file instead (comma- or "
                        "whitespace-separated days). Avoids a long shell string.")
    p.add_argument("--wip-item", action="append", default=[],
                   help='one in-flight item, repeatable. "KEY:STATUS:AGE_DAYS" '
                        'e.g. "ABC-1234:In Progress:12". Enables aging WIP (Q4).')
    p.add_argument("--wip-file", default=None,
                   help='read WIP items from a file, one "KEY:STATUS:AGE_DAYS" per '
                        "line. Merged with any --wip-item flags.")
    p.add_argument("--cycle-basis", choices=["created", "started"], default="created",
                   help='what the cycle times / WIP ages measure: "created" '
                        '(created→Done, includes backlog wait) or "started" '
                        '(started→Done active time). Sets Q3/Q4 wording only.')
    p.add_argument("--outliers", choices=["keep", "winsorize", "drop"], default="keep",
                   help="how to handle throughput weeks flagged as statistical "
                        "outliers (robust median/MAD): keep (flag only — default), "
                        "winsorize (cap to the nearest in-band week), or drop "
                        "(remove from the sim sample). Applied per series.")
    p.add_argument("--outlier-threshold", type=float, default=3.5,
                   help="modified z-score above which a week is flagged an outlier "
                        "(default 3.5, the Iglewicz–Hoaglin convention)")
    p.add_argument("--by-date", default=None, help="YYYY-MM-DD (default: +3 weeks)")
    p.add_argument("--trials", type=int, default=10000)
    p.add_argument("--done-status", default="Done")
    p.add_argument("--seed", type=int, default=42)
    p.add_argument("--json-out", default=None,
                   help="also write the computed forecast (all of Q1–Q4) as "
                        "structured JSON to this path — feeds forecast-html.py "
                        "for the branded HTML report. The terminal report still prints.")
    # ---- file mode: ingest fetch.py's result files directly (one-command run) ----
    p.add_argument("--done", nargs="+", default=None,
                   help="fetch.py's done.json for the Done/throughput query. "
                        "Enables file mode: throughput/items/cycle-times/WIP are "
                        "computed in-process (no hand-off, no prompts).")
    p.add_argument("--open", nargs="+", default=None, dest="open_files",
                   help="fetch.py's open.json for the open-queue query (file mode)")
    p.add_argument("--changelog", nargs="+", default=None,
                   help="files carrying inline changelogs (pass the same "
                        "done.json/open.json). When given, Q3/Q4 switch to "
                        "started→Done (active time); --cycle-basis is set to started.")
    p.add_argument("--components", nargs="+", default=[],
                   help="[file mode] components to break out (default: none — team "
                        "line only; pass e.g. --components web api to split by team)")
    p.add_argument("--lookback-days", type=int, default=77,
                   help="[file mode] trailing -Nd throughput window (default 77); "
                        "ignored if --since is given")
    p.add_argument("--since", default=None,
                   help="[file mode] throughput window START (YYYY-MM-DD), e.g. a "
                        "quarter start. When set, the window is [--since, --until] "
                        "and --lookback-days is ignored; the Done JQL should match.")
    p.add_argument("--until", default=None,
                   help="[file mode] throughput window END (YYYY-MM-DD), inclusive; "
                        "default --today")
    p.add_argument("--today", default=None,
                   help="[file mode] reference NOW date YYYY-MM-DD (default today); "
                        "sets the aging-WIP age clock (window is set by "
                        "--since/--until or --lookback-days)")
    p.add_argument("--wip-status", nargs="+",
                   default=["In Progress", "In Review"],
                   help="[file mode] in-flight statuses for Q4 — set to your active "
                        "columns (default is a generic placeholder)")
    p.add_argument("--cycle-max-days", type=float, default=None,
                   help="[file mode] drop Done items whose cycle time exceeds N days "
                        "from the SLE sample (trims the backlog-aging tail)")
    p.add_argument("--start-status", default="In Progress",
                   help="[changelog] status whose first entry starts the clock")
    p.add_argument("--reset-status", nargs="+", default=["To Do", "Cancelled"],
                   help="[changelog] statuses that reset the clock on return to "
                        "backlog — set to yours (default is a generic placeholder)")
    args = p.parse_args()

    if args.done:
        synthesize_from_files(args)   # populates the pure-math args from raw files

    # ---- file-based inputs (so the Q3/Q4 data never has to be a shell string) --
    # An explicit --cycle-file / --wip-file overrides/augments whatever file mode
    # (or the pure-math flags) produced — handy for a manually-computed SLE sample.
    if args.cycle_file:
        try:
            raw = open(args.cycle_file).read()
        except OSError as e:
            sys.exit(f"ERROR: --cycle-file: {e}")
        vals = [x for x in raw.replace("\n", ",").replace(" ", ",").split(",") if x.strip()]
        args.cycle_times = ",".join(vals)
    if args.wip_file:
        try:
            lines = open(args.wip_file).read().splitlines()
        except OSError as e:
            sys.exit(f"ERROR: --wip-file: {e}")
        args.wip_item = args.wip_item + [ln.strip() for ln in lines if ln.strip()]

    rng = random.Random(args.seed)
    done_statuses = [s.strip() for s in args.done_status.split(",")]

    if args.throughput and not args.live:
        # ---- PURE-MATH MODE (supported) ----
        try:
            sample = [int(x) for x in args.throughput.split(",") if x.strip() != ""]
        except ValueError:
            sys.exit("ERROR: --throughput must be comma-separated integers")
        # pure-math mode has no calendar context; label neutrally oldest->newest
        ordered = [(f"wk{i+1}", c) for i, c in enumerate(sample)]
        if args.items is None:
            sys.exit("ERROR: --items is required in pure-math mode")
        backlog = args.items
    else:
        # ---- LIVE MODE (direct REST) ----
        global JIRA_BASE
        if not args.jira_base or not args.project:
            sys.exit("ERROR: --live requires --jira-base and --project")
        JIRA_BASE = args.jira_base.rstrip("/")
        auth = load_token()
        sample, ordered = weekly_throughput(auth, args.weeks or 10, done_statuses,
                                            args.project, include_epics=args.include_epics)
        if not sample or sum(sample) == 0:
            sys.exit("ERROR: no completed items found via REST — check the "
                     "Authorization value in settings.json and that --project / "
                     "--done-status match your board, or use pure-math mode "
                     "(--throughput \"...\" --items N).")
        bjql = args.backlog_jql or backlog_jql(args.project, args.wip_status)
        if not args.include_epics:
            bjql = exclude_epics(bjql)
        backlog = args.items if args.items is not None else jql_count(bjql, auth)

    if args.by_date:
        target = datetime.strptime(args.by_date, "%Y-%m-%d").date()
    else:
        target = date.today() + timedelta(weeks=3)
    today = date.today()
    n_weeks = max(1, math.ceil((target - today).days / 7))

    # ----- assemble series: TEAM first, then each --component ----------------
    series = [{"name": "TEAM (all)", "sample": sample, "backlog": backlog}]
    for spec in args.component:
        try:
            name, tp, items = spec.split(":")
            cs = [int(x) for x in tp.split(",") if x.strip() != ""]
            series.append({"name": name.strip(), "sample": cs, "backlog": int(items)})
        except ValueError:
            sys.exit(f'ERROR: --component must be "Name:t1,t2,...:items"  (got: {spec})')

    # ----- outlier detection + optional handling (per series) ----------------
    # Flag on the observed history; the chosen policy decides what the sim uses.
    for s in series:
        raw = list(s["sample"])
        s["flagged_raw"] = detect_outliers(raw, args.outlier_threshold)
        adj, onote = apply_outlier_policy(raw, s["flagged_raw"], args.outliers)
        s["sample_raw"] = raw
        s["sample"] = adj
        s["outlier_note"] = onote
        # indices still flagged in the DISPLAYED (sim) sample — for chart marks
        s["outliers_display"] = detect_outliers(adj, args.outlier_threshold)

    # ----- simulate every series --------------------------------------------
    for s in series:
        s["mean"] = sum(s["sample"]) / len(s["sample"])
        s["weeks"] = sim_weeks_to_finish(s["backlog"], s["sample"], args.trials, rng)
        s["items"] = sim_items_by_date(n_weeks, s["sample"], args.trials, rng)

    # ----- cycle-time SLE (Q3) ----------------------------------------------
    cycle_sample, sle = None, {}
    if args.cycle_times:
        try:
            cycle_sample = sorted(
                float(x) for x in args.cycle_times.split(",") if x.strip() != "")
        except ValueError:
            sys.exit("ERROR: --cycle-times must be comma-separated numbers (days)")
        if not cycle_sample:
            sys.exit("ERROR: --cycle-times had no values")
        sle = {q: pct(cycle_sample, q) for q in (50, 85, 95)}

    # ----- aging WIP (Q4) ---------------------------------------------------
    wip = []
    for spec in args.wip_item:
        try:
            head, age = spec.rsplit(":", 1)
            key, status = head.split(":", 1)
            wip.append({"key": key.strip(), "status": status.strip(),
                        "age": float(age)})
        except ValueError:
            sys.exit(f'ERROR: --wip-item must be "KEY:STATUS:AGE_DAYS" (got: {spec})')
    wip.sort(key=lambda x: x["age"], reverse=True)

    if args.cycle_basis == "started":
        basis_label = "started → Done (active time)"
        basis_clock = "days of work starting"
        age_label = "active days since work started"
    else:
        basis_label = "created → Done"
        basis_clock = "days of being created"
        age_label = "days since created"

    wk = len(sample)
    breakdown = len(series) > 1
    NW = max(len(s["name"]) for s in series)

    # ----- render ------------------------------------------------------------
    L = []
    _title = f"{args.project} FORECAST" if args.project else "DELIVERY FORECAST"
    _head = f"┌─ {_title} "
    L.append(_head + "─" * max(3, 63 - len(_head)))
    L.append(f"│ Run: {today.isoformat()}   Trials: {args.trials:,}   "
             f"Done = {'/'.join(done_statuses)}   Sample: {wk} complete weeks")
    L.append("└" + "─" * 62)
    epics_line = ("⚠ Epics INCLUDED (--include-epics)" if args.include_epics
                  else "Epics excluded from all figures (placeholders, not deliverable flow)")
    L.append(epics_line)
    L.append("")

    # Throughput table
    L.append(f"THROUGHPUT  (items reaching Done per week, oldest → newest)")
    L.append(f"  {'series':<{NW}}   weekly counts{' '*max(0, 3*wk-13)}   mean   min  max")
    for s in series:
        counts = " ".join(f"{c:>2}" for c in s["sample"])
        L.append(f"  {s['name']:<{NW}}   {counts}   {s['mean']:>4.1f}   "
                 f"{min(s['sample']):>3}  {max(s['sample']):>3}")
    if any(s["flagged_raw"] for s in series):
        L.append("")
        L.append(f"  OUTLIER WEEKS  (robust median/MAD flag, z>{args.outlier_threshold:g}"
                 f"; policy: {args.outliers})")
        for s in series:
            if not s["flagged_raw"]:
                continue
            marks = ", ".join(f"w{i+1}={v:g} (z {mz:+.1f})"
                              for i, v, mz in s["flagged_raw"])
            extra = f"  → {s['outlier_note']}" if s["outlier_note"] else ""
            L.append(f"  ! {s['name']:<{NW}}  {marks}{extra}")
        if args.outliers == "keep":
            L.append("    policy=keep: flagged only, still in the sample — "
                     "re-run with --outliers drop|winsorize to adapt.")
        L.append("    (w-index is the observed history; a genuine big week is not "
                 "an artifact — confirm before dropping.)")
    L.append("")
    L.append("─" * 63)

    # Q1 — how long to clear each backlog
    L.append("Q1.  HOW LONG to clear the current open queue?")
    L.append("     (open = Ready for Dev + In Progress + Code Review + QA Ready)")
    L.append("")
    L.append(f"  {'series':<{NW}}   queue   50%      85%      95%      85% date")
    L.append(f"  {'-'*NW}   -----   ----     ----     ----     ----------")
    for s in series:
        w50, w85, w95 = pct(s["weeks"], 50), pct(s["weeks"], 85), pct(s["weeks"], 95)
        d85 = today + timedelta(weeks=w85)
        L.append(f"  {s['name']:<{NW}}   {s['backlog']:>5}   "
                 f"{w50:>2}w      {w85:>2}w      {w95:>2}w      {d85.isoformat()}")
    L.append("")
    L.append("─" * 63)

    # Q2 — how many done by the horizon
    L.append(f"Q2.  HOW MANY done by {target.isoformat()} ({n_weeks} weeks out)?")
    L.append("     (at least N items, by confidence)")
    L.append("")
    L.append(f"  {'series':<{NW}}    95%     85%     50%")
    L.append(f"  {'-'*NW}    ---     ---     ---")
    for s in series:
        L.append(f"  {s['name']:<{NW}}   {pct(s['items'],5):>4}    "
                 f"{pct(s['items'],15):>4}    {pct(s['items'],50):>4}")
    L.append("")
    L.append("─" * 63)

    # Q3 — cycle-time Service Level Expectation
    if cycle_sample:
        L.append("Q3.  CYCLE TIME  — Service Level Expectation (the estimate replacement)")
        L.append(f"     (per-item {basis_label}, calendar days;  N = {len(cycle_sample)} items)")
        L.append("")
        L.append(f"  50% typical    within {sle[50]:>4.0f} days")
        L.append(f"  85% SLE        within {sle[85]:>4.0f} days   ← the team's per-item commitment")
        L.append(f"  95% worst      within {sle[95]:>4.0f} days")
        L.append("")
        L.append(f'  Say to stakeholders: "we don\'t estimate — 85% of items reach Done')
        L.append(f'  within {sle[85]:.0f} {basis_clock}." No story points required.')
        L.append("")
        L.append("─" * 63)

    # Q4 — aging work in progress
    if wip:
        L.append("Q4.  AGING WORK IN PROGRESS  — pull these before starting new work")
        if sle:
            L.append(f"     (age = {age_label}; flagged against the "
                     f"{sle[85]:.0f}-day 85% SLE)")
        else:
            L.append(f"     (age = {age_label}; pass --cycle-times to flag vs the SLE)")
        L.append("")
        breach = [w for w in wip if sle and w["age"] >= sle[85]]
        warn   = [w for w in wip if sle and sle[50] <= w["age"] < sle[85]]
        healthy = [w for w in wip if not sle or w["age"] < sle[50]]
        KW = max((len(w["key"]) for w in wip), default=8)
        SW = max((len(w["status"]) for w in wip), default=11)
        for w in breach:
            L.append(f"  🔴 {w['key']:<{KW}}  {w['status']:<{SW}}  {w['age']:>4.0f}d"
                     f"   OVER SLE — pull now")
        for w in warn:
            L.append(f"  🟡 {w['key']:<{KW}}  {w['status']:<{SW}}  {w['age']:>4.0f}d"
                     f"   aging — watch")
        if not sle:
            for w in wip:
                L.append(f"  •  {w['key']:<{KW}}  {w['status']:<{SW}}  {w['age']:>4.0f}d")
        elif healthy:
            L.append(f"  🟢 {len(healthy)} item(s) within the {sle[50]:.0f}-day typical line")
        L.append("")
        if sle:
            L.append(f"  {len(breach)} over SLE · {len(warn)} aging · {len(healthy)} healthy"
                     f"  (of {len(wip)} in flight)")
        L.append("")
        L.append("─" * 63)

    L.append("Read 85% as the commit-safe line. Quote it to stakeholders; keep 50% internal.")
    if breakdown:
        L.append("NOTE: component forecasts are independent — the TEAM line is NOT the sum of")
        L.append("      component lines (variances don't add). Trust TEAM for whole-team")
        L.append("      promises; use component lines to spot bottlenecks & lopsided queues.")
    L.append(f"Valid only while team & flow match the last {wk} weeks — re-run after reorg/holiday.")
    print("\n".join(L))

    # ----- structured JSON dump (feeds forecast-html.py) ------------------
    if args.json_out:
        window_desc = (f"{args.since} → {args.until or today.isoformat()}"
                       if args.done and args.since
                       else f"last {args.lookback_days}d" if args.done
                       else "throughput supplied directly")
        series_out = []
        for s in series:
            series_out.append({
                "name": s["name"],
                "sample": s["sample"],
                "sample_raw": s["sample_raw"],
                "mean": round(s["mean"], 1),
                "min": min(s["sample"]),
                "max": max(s["sample"]),
                "backlog": s["backlog"],
                "outliers": [{"wk": i + 1, "value": v, "z": round(mz, 1)}
                             for i, v, mz in s["outliers_display"]],
                "outlier_note": s["outlier_note"],
                "q1": {"w50": pct(s["weeks"], 50), "w85": pct(s["weeks"], 85),
                       "w95": pct(s["weeks"], 95),
                       "date85": (today + timedelta(weeks=pct(s["weeks"], 85))).isoformat()},
                "q2": {"c95": pct(s["items"], 5), "c85": pct(s["items"], 15),
                       "c50": pct(s["items"], 50)},
            })
        wip_out = []
        for w in wip:
            band = ("breach" if sle and w["age"] >= sle[85]
                    else "warn" if sle and w["age"] >= sle[50]
                    else "healthy" if sle else "none")
            wip_out.append({"key": w["key"], "status": w["status"],
                            "age": round(w["age"]), "band": band})
        payload = {
            "meta": {
                "project": args.project,      # None when no --project given
                "jira_browse_base": (args.jira_base.rstrip("/") + "/browse"
                                     if args.jira_base else None),
                "today": today.isoformat(),
                "by_date": target.isoformat(),
                "n_weeks": n_weeks,
                "trials": args.trials,
                "done_statuses": done_statuses,
                "sample_weeks": wk,
                "window": window_desc,
                "basis": basis_label,
                "basis_clock": basis_clock,
                "age_label": age_label,
                "outlier_policy": args.outliers,
                "outlier_threshold": args.outlier_threshold,
                "epics_excluded": not args.include_epics,
            },
            "series": series_out,
            "cycle": ({"n": len(cycle_sample),
                       "p50": sle[50], "p85": sle[85], "p95": sle[95],
                       "max": cycle_sample[-1],
                       "hist": _histogram(cycle_sample)} if cycle_sample else None),
            "wip": wip_out,
            "wip_bands": {
                "breach": sum(1 for w in wip_out if w["band"] == "breach"),
                "warn": sum(1 for w in wip_out if w["band"] == "warn"),
                "healthy": sum(1 for w in wip_out if w["band"] == "healthy"),
                "total": len(wip_out),
            },
        }
        with open(args.json_out, "w") as f:
            json.dump(payload, f, indent=2)
        print(f"# wrote forecast JSON → {args.json_out}", file=sys.stderr)


def _histogram(sorted_days, bins=(0, 7, 14, 30, 60, 90, 180, 365, 100000)):
    """Bucket cycle-time days into labeled bins for the distribution chart."""
    labels = ["0–7", "8–14", "15–30", "31–60", "61–90", "91–180", "181–365", "365+"]
    counts = [0] * len(labels)
    for d in sorted_days:
        for i in range(len(labels)):
            if bins[i] <= d < bins[i + 1]:
                counts[i] += 1
                break
    return [{"label": labels[i], "count": counts[i]} for i in range(len(labels))]


if __name__ == "__main__":
    main()
