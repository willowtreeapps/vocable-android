#!/usr/bin/env python3
"""
gh_fetch.py — pull throughput-forecast data from a GitHub Projects v2 board in
one command.

GitHub-Projects analog of the Jira-based fetch.py in the original
throughput-forecast skill. Paginates the project's items, and for every
`Issue` item nests its status-change timeline in the SAME query (no N+1 —
confirmed live at ~1 rate-limit point per 50-item page), then reshapes the
result into the exact envelope forecast.py's file mode already reads:

    <out-dir>/done.json   {"issues":{"nodes":[...]}}
    <out-dir>/open.json   {"issues":{"nodes":[...]}}

Each node's `changelog.histories[]` is built from the item's
`ProjectV2ItemStatusChangedEvent` timeline — the direct GitHub analog of
Jira's changelog status-transition entries — so forecast.py's started→Done
Service Level Expectation (Q3) and aging-WIP (Q4) work unmodified.

Auth: shells out to `gh api graphql`, reusing whatever `gh auth login`
already set up on this machine — no token is read from or written to
settings.json.

Item routing:
  - DraftIssue and PullRequest project items are EXCLUDED by default (they
    aren't deliverable flow / are already reachable via an issue's linked
    PRs) — pass --include-drafts to include drafts. --include-prs is
    intentionally NOT implemented: live testing found that a PullRequest's
    `timelineItems.totalCount` does NOT respect the `itemTypes` filter the
    way it does for Issue (a real PR showed totalCount=17 with 0 matching
    nodes), so the truncated-changelog backfill logic can't be trusted for
    PRs without separate handling. Passing --include-prs exits with an
    error rather than silently producing wrong changelogs.
  - Issues that are themselves a PARENT of other sub-issues (subIssuesSummary
    .total > 0) are EXCLUDED by default too — pass --include-parents to keep
    them. Same reasoning as Jira's Epic exclusion: a parent issue stays open/
    in-progress for its entire sub-issue lifetime (confirmed live on #613:
    18 sub-issues, 10 complete, parent still OPEN), so counting it as its own
    throughput/open-queue item would badly distort both. It still works as a
    --breakdown-field parent grouping label for its children regardless of
    this flag — this only controls whether the parent ALSO shows up as its
    own line item in done.json/open.json.
  - An item whose current Status is in neither --done-status nor --statuses
    is dropped but counted (`skipped_status`) and logged to stderr — this
    board has 7 status columns (not Jira's typical 3), so a misconfigured
    --statuses list would otherwise silently drop real items.

Usage:
    gh_fetch.py --org willowtreeapps --project-number 50 \
        --done-status "Done" \
        --statuses "Pre Backlog" "Backlog" "Ready to select" "In progress" \
                   "Development Complete (In Review)" "Ready for Demo" \
        --out-dir /tmp/forecast

Exit codes: 0 ok | 2 config/gh-auth | 3 GraphQL/network error | 6 zero Done items.
"""

import argparse
import json
import os
import subprocess
import sys
import time
from datetime import date, datetime

ITEMS_QUERY = """
query($org: String!, $number: Int!, $cursor: String) {
  organization(login: $org) {
    projectV2(number: $number) {
      items(first: 50, after: $cursor) {
        pageInfo { hasNextPage endCursor }
        nodes {
          fieldValues(first: 20) {
            nodes {
              __typename
              ... on ProjectV2ItemFieldSingleSelectValue {
                name
                field { ... on ProjectV2FieldCommon { name } }
              }
            }
          }
          content {
            __typename
            ... on Issue {
              number
              title
              createdAt
              closedAt
              state
              url
              issueType { name }
              labels(first: 20) { nodes { name } }
              repository { name }
              parent { number title }
              subIssuesSummary { total }
              timelineItems(itemTypes: [PROJECT_V2_ITEM_STATUS_CHANGED_EVENT], first: 50) {
                totalCount
                pageInfo { hasNextPage endCursor }
                nodes {
                  ... on ProjectV2ItemStatusChangedEvent {
                    createdAt
                    previousStatus
                    status
                  }
                }
              }
            }
          }
        }
      }
    }
  }
}
"""

TIMELINE_BACKFILL_QUERY = """
query($owner: String!, $repo: String!, $number: Int!, $after: String) {
  repository(owner: $owner, name: $repo) {
    issue(number: $number) {
      timelineItems(itemTypes: [PROJECT_V2_ITEM_STATUS_CHANGED_EVENT], first: 100, after: $after) {
        totalCount
        pageInfo { hasNextPage endCursor }
        nodes {
          ... on ProjectV2ItemStatusChangedEvent {
            createdAt
            previousStatus
            status
          }
        }
      }
    }
  }
}
"""


def gh_graphql(query, retries=3, **variables):
    cmd = ["gh", "api", "graphql", "-f", f"query={query}"]
    for k, v in variables.items():
        if v is None:
            cmd += ["-F", f"{k}=null"]
        elif isinstance(v, bool):
            cmd += ["-F", f"{k}={'true' if v else 'false'}"]
        elif isinstance(v, int):
            cmd += ["-F", f"{k}={v}"]
        else:
            cmd += ["-f", f"{k}={v}"]
    for attempt in range(retries + 1):
        result = subprocess.run(cmd, capture_output=True, text=True)
        if result.returncode != 0:
            if attempt < retries:
                time.sleep(2 ** attempt)
                continue
            sys.exit(f"ERROR(3): gh api graphql failed: {result.stderr[:800]}")
        try:
            data = json.loads(result.stdout)
        except json.JSONDecodeError as e:
            sys.exit(f"ERROR(3): non-JSON response: {e}\n{result.stdout[:500]}")
        if "errors" in data:
            sys.exit(f"ERROR(3): GraphQL errors: {json.dumps(data['errors'])[:800]}")
        return data["data"]
    sys.exit("ERROR(3): exhausted retries")


def check_gh_auth():
    result = subprocess.run(["gh", "auth", "status"], capture_output=True, text=True)
    if result.returncode != 0:
        sys.exit(f"ERROR(2): gh is not authenticated — run `gh auth login`.\n{result.stderr}")


def fetch_all_items(org, project_number):
    """Paginate the project's items, nested timelineItems included per Issue."""
    nodes, cursor, pages = [], None, 0
    while True:
        data = gh_graphql(ITEMS_QUERY, org=org, number=project_number, cursor=cursor)
        items = data["organization"]["projectV2"]["items"]
        nodes.extend(items["nodes"])
        pages += 1
        if not items["pageInfo"]["hasNextPage"]:
            print(f"#   project items: {len(nodes)} nodes, {pages} page(s)", file=sys.stderr)
            return nodes
        cursor = items["pageInfo"]["endCursor"]


def backfill_truncated_timelines(owner, repo, issue_nodes):
    """For any Issue whose nested timelineItems was truncated (>50 status
    changes — rare), re-page it directly via the issue's own timelineItems.
    Mirrors the original fetch.py's backfill_truncated() pattern."""
    n_backfilled = 0
    for node in issue_nodes:
        content = node["content"]
        tl = content["timelineItems"]
        if not tl["pageInfo"]["hasNextPage"]:
            continue
        n_backfilled += 1
        all_nodes = list(tl["nodes"])
        cursor = tl["pageInfo"]["endCursor"]
        while True:
            data = gh_graphql(TIMELINE_BACKFILL_QUERY, owner=owner, repo=repo,
                               number=content["number"], after=cursor)
            page = data["repository"]["issue"]["timelineItems"]
            all_nodes.extend(page["nodes"])
            if not page["pageInfo"]["hasNextPage"]:
                break
            cursor = page["pageInfo"]["endCursor"]
        content["timelineItems"] = {"totalCount": len(all_nodes), "nodes": all_nodes,
                                     "pageInfo": {"hasNextPage": False}}
    return n_backfilled


def current_status(project_item):
    for fv in project_item["fieldValues"]["nodes"]:
        if fv.get("__typename") == "ProjectV2ItemFieldSingleSelectValue":
            field = fv.get("field") or {}
            if field.get("name") == "Status":
                return fv.get("name")
    return None


def build_histories(timeline_nodes):
    return [{"created": n["createdAt"], "items": [{"field": "status", "toString": n["status"]}]}
            for n in timeline_nodes]


def components_for(content, breakdown_field):
    if breakdown_field == "labels":
        return [{"name": l["name"]} for l in content["labels"]["nodes"]]
    if breakdown_field == "issuetype":
        it = content.get("issueType")
        return [{"name": it["name"]}] if it else []
    if breakdown_field == "parent":
        # Group by the item's GitHub native sub-issue parent (the GH Projects
        # analog of a Jira Epic — this board has no Epic type, so a sub-issue's
        # linked parent is the "feature" a ticket belongs to). Standalone items
        # (no parent) get no component and only show up in the TEAM line.
        parent = content.get("parent")
        return [{"name": f"#{parent['number']} {parent['title']}"}] if parent else []
    return []


def day_from_iso(s):
    return date.fromisoformat(s[:10])


def is_bulk_import_artifact(timeline_nodes, done_statuses):
    """True if this item's ENTIRE timeline is a single previousStatus="" entry
    landing directly on a done-status — i.e. it was already closed before
    being added to the project and never actually flowed through the board.

    Discovered live on Project #50: 141 of 169 Done items shared the exact
    same transition timestamp (2024-05-28T18:04:24Z, the moment the project
    was bulk-populated), each with previousStatus="" -> "Done" as their ONLY
    timeline entry, while their real (spread-out, months/years earlier)
    closedAt dates showed the genuine history. Treating that shared instant
    as 141 real "completions" would fabricate a single fake mega-throughput
    week and badly bias the forecast. A REAL multi-step history that happens
    to end at Done is not affected by this check — only a lone, empty-origin
    entry is (the reliable "no real board journey" signature)."""
    return (len(timeline_nodes) == 1
            and timeline_nodes[0]["previousStatus"] == ""
            and timeline_nodes[0]["status"] in done_statuses)


def resolutiondate_for(content, timeline_nodes, done_statuses, stats):
    """Last status-transition into a done-status wins; fall back to closedAt
    if no such transition exists, OR if the only transition is a bulk-import
    artifact (see is_bulk_import_artifact). Flags (to stderr, at the end) any
    remaining case where the two disagree by more than a day — a genuine
    judgment call, not a solved fact (see PR description / work-log)."""
    done_set = set(done_statuses)
    if is_bulk_import_artifact(timeline_nodes, done_set):
        stats["resolutiondate_import_artifact"] += 1
        return content.get("closedAt")
    last_done_ts = None
    for n in timeline_nodes:
        if n["status"] in done_set:
            last_done_ts = n["createdAt"]
    if last_done_ts:
        if content.get("closedAt"):
            d1 = day_from_iso(last_done_ts)
            d2 = day_from_iso(content["closedAt"])
            if abs((d1 - d2).days) > 1:
                stats["resolutiondate_disagreements"] += 1
        return last_done_ts
    stats["resolutiondate_fallback_closedat"] += 1
    return content.get("closedAt")


def main():
    p = argparse.ArgumentParser(description="Pull throughput-forecast data from GitHub Projects v2")
    p.add_argument("--org", required=True, help="GitHub org that owns the project")
    p.add_argument("--project-number", type=int, required=True, help="Project number, e.g. 50")
    p.add_argument("--repo", default=None,
                   help="repo name, for the backfill query owner/repo (default: infer "
                        "from the first Issue item encountered)")
    p.add_argument("--out-dir", default="/tmp/forecast")
    p.add_argument("--done-status", nargs="+", default=["Done"],
                   help='status name(s) counted as delivered (default "Done")')
    p.add_argument("--statuses", nargs="+",
                   default=["Backlog", "In progress", "In review"],
                   help="open-queue (active/backlog) status names — set to your "
                        "board's committed columns (default is a generic placeholder)")
    p.add_argument("--breakdown-field", choices=["none", "labels", "issuetype", "parent"],
                   default="none",
                   help="what to use as the forecast's --components axis "
                        "(default none — team-only forecast). \"parent\" breaks "
                        "down by each item's native GitHub sub-issue parent — the "
                        "closest analog to a Jira Epic on a board with no Epic type; "
                        "items with no parent are counted in TEAM only.")
    p.add_argument("--include-drafts", action="store_true",
                   help="include DraftIssue project items (default: excluded — "
                        "not deliverable flow)")
    p.add_argument("--include-parents", action="store_true",
                   help="include issues that are themselves a parent of other "
                        "sub-issues (default: excluded — same reasoning as Jira's "
                        "Epic exclusion: a parent stays open/in-progress for its "
                        "entire sub-issue lifetime, so counting it as its own "
                        "throughput/open-queue item would badly distort both. It "
                        "still works as a --breakdown-field parent grouping label "
                        "for its children either way — this flag only affects "
                        "whether the parent ALSO appears as its own line item.)")
    p.add_argument("--include-prs", action="store_true",
                   help="NOT IMPLEMENTED — PullRequest.timelineItems.totalCount does "
                        "not respect the itemTypes filter in live testing (seen "
                        "totalCount=17 with 0 matching nodes for a real PR), so the "
                        "truncated-changelog backfill can't be trusted for PRs yet. "
                        "Passing this flag exits with an error.")
    p.add_argument("--since", default=None,
                   help="only include items created on/after this date (YYYY-MM-DD); "
                        "default: no filter (fetch everything)")
    args = p.parse_args()

    if args.include_prs:
        sys.exit("ERROR(2): --include-prs is not implemented — see the docstring for "
                  "why (PullRequest timelineItems totalCount is unreliable). Needs its "
                  "own verification before enabling; track as a follow-up.")

    check_gh_auth()
    os.makedirs(args.out_dir, exist_ok=True)
    t0 = time.time()

    done_statuses = args.done_status
    open_statuses = args.statuses
    since_cutoff = datetime.strptime(args.since, "%Y-%m-%d").date() if args.since else None

    all_nodes = fetch_all_items(args.org, args.project_number)

    issue_nodes = [n for n in all_nodes if n["content"] and n["content"]["__typename"] == "Issue"]
    n_backfilled = 0
    if issue_nodes:
        owner = args.org
        repo = args.repo or issue_nodes[0]["content"]["repository"]["name"]
        n_backfilled = backfill_truncated_timelines(owner, repo, issue_nodes)

    stats = {
        "total": len(all_nodes),
        "drafts_excluded": 0,
        "prs_excluded": 0,
        "parents_excluded": 0,
        "skipped_since_filter": 0,
        "skipped_status": 0,
        "done": 0,
        "open": 0,
        "resolutiondate_fallback_closedat": 0,
        "resolutiondate_disagreements": 0,
        "resolutiondate_import_artifact": 0,
    }
    done_nodes, open_nodes = [], []

    for node in all_nodes:
        content = node["content"]
        if content is None:
            stats["skipped_status"] += 1
            continue
        typename = content["__typename"]
        if typename == "DraftIssue":
            if not args.include_drafts:
                stats["drafts_excluded"] += 1
                continue
        elif typename == "PullRequest":
            stats["prs_excluded"] += 1
            continue
        elif typename != "Issue":
            stats["skipped_status"] += 1
            continue

        if typename == "Issue" and not args.include_parents:
            sub_total = (content.get("subIssuesSummary") or {}).get("total", 0)
            if sub_total > 0:
                stats["parents_excluded"] += 1
                continue

        created_at = content.get("createdAt")
        if since_cutoff and created_at and day_from_iso(created_at) < since_cutoff:
            stats["skipped_since_filter"] += 1
            continue

        status_name = current_status(node)
        timeline_nodes = (content.get("timelineItems") or {}).get("nodes", [])
        histories = build_histories(timeline_nodes)
        key = f"{content.get('repository', {}).get('name', args.repo or args.org)}#{content.get('number')}"

        base_fields = {
            "created": created_at,
            "components": components_for(content, args.breakdown_field),
            "issuetype": {"name": content["issueType"]["name"]} if content.get("issueType") else None,
        }
        changelog = {"total": len(timeline_nodes), "histories": histories}

        if status_name in done_statuses:
            rd = resolutiondate_for(content, timeline_nodes, done_statuses, stats)
            if not rd:
                # No transition into a done-status and no closedAt — can't place
                # it in time; drop rather than guess.
                stats["skipped_status"] += 1
                continue
            fields = dict(base_fields, resolutiondate=rd)
            done_nodes.append({"key": key, "fields": fields, "changelog": changelog})
            stats["done"] += 1
        elif status_name in open_statuses:
            fields = dict(base_fields, status={"name": status_name})
            open_nodes.append({"key": key, "fields": fields, "changelog": changelog})
            stats["open"] += 1
        else:
            stats["skipped_status"] += 1

    if not done_nodes:
        sys.exit("ERROR(6): 0 Done items found — check --org/--project-number/"
                 "--done-status against your board. Refusing to write files from "
                 "an empty pull.")

    done_path = os.path.join(args.out_dir, "done.json")
    open_path = os.path.join(args.out_dir, "open.json")
    with open(done_path, "w") as f:
        json.dump({"issues": {"nodes": done_nodes}}, f)
    with open(open_path, "w") as f:
        json.dump({"issues": {"nodes": open_nodes}}, f)

    print(f"#   total project items      : {stats['total']}", file=sys.stderr)
    print(f"#   done                     : {stats['done']}", file=sys.stderr)
    print(f"#   open                     : {stats['open']}", file=sys.stderr)
    print(f"#   drafts excluded          : {stats['drafts_excluded']} "
          f"({'included' if args.include_drafts else 'default: excluded'})", file=sys.stderr)
    print(f"#   PRs excluded             : {stats['prs_excluded']} (always excluded — see docstring)",
          file=sys.stderr)
    print(f"#   parent issues excluded   : {stats['parents_excluded']} "
          f"({'included' if args.include_parents else 'default: excluded — sit open for their whole sub-issue lifetime'})",
          file=sys.stderr)
    if args.since:
        print(f"#   skipped (before --since) : {stats['skipped_since_filter']}", file=sys.stderr)
    print(f"#   skipped (status not in --done-status/--statuses): {stats['skipped_status']}",
          file=sys.stderr)
    print(f"#   timelines backfilled     : {n_backfilled} truncated issue(s)", file=sys.stderr)
    print(f"#   resolutiondate fallback to closedAt (no Done transition found): "
          f"{stats['resolutiondate_fallback_closedat']}", file=sys.stderr)
    print(f"#   resolutiondate bulk-import artifacts (lone empty-origin transition, "
          f"used closedAt instead): {stats['resolutiondate_import_artifact']}", file=sys.stderr)
    print(f"#   resolutiondate/closedAt disagreements (>1 day, non-artifact): "
          f"{stats['resolutiondate_disagreements']}", file=sys.stderr)
    accounted = (stats["done"] + stats["open"] + stats["drafts_excluded"]
                 + stats["prs_excluded"] + stats["parents_excluded"]
                 + stats["skipped_since_filter"] + stats["skipped_status"])
    print(f"#   sanity: done+open+excluded+skipped = {accounted}  (total = {stats['total']})",
          file=sys.stderr)
    print(f"#   elapsed: {time.time() - t0:.1f}s", file=sys.stderr)
    print(done_path)
    print(open_path)


if __name__ == "__main__":
    main()
