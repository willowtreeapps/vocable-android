#!/usr/bin/env python3
"""
run_daily_forecast.py — daily driver for the vocable-android throughput-forecast
skill. Fetches GitHub Project #50, breaks out by the Voice Selection feature
(native sub-issue parent #613 — the --breakdown-field parent axis, this
board's Epic-analog) forecasts, and writes a dated HTML report straight into
the Drive-synced Metrics folder (no separate upload step: that folder IS the
sync target on this machine).

Scoped to #613 only per direct instruction, for as long as feature/voice-
selection stays unmerged — other parents on the board (e.g. #627 Reset App
Settings) are deliberately excluded from the breakdown, not just currently
absent. Revisit ALLOWED_PARENT_NUMBERS once voice-selection merges to main.

Meant to be invoked by a scheduled task, once a day.
"""
import json
import subprocess
import sys
from datetime import date
from pathlib import Path

REPO_ROOT = Path("/Users/rhyslutsky/My Drive/Clients/Vocable/vocable-android")
SCRIPTS = REPO_ROOT / ".claude" / "scripts"
OUT_DIR = Path("/tmp/vocable_forecast")
DRIVE_DIR = Path("/Users/rhyslutsky/My Drive/Clients/Vocable/Metrics")
LATEST_NAME = "vocable-android-throughput-forecast-latest.html"

# Only break out these parent issue numbers (matched by "#<N> " prefix on the
# discovered component name, so a title edit doesn't silently drop it). Any
# other parent found on the board is excluded from --components — deliberate
# scope-narrowing to Voice Selection, not a stale filter to widen later.
ALLOWED_PARENT_NUMBERS = {613}

ORG = "willowtreeapps"
PROJECT_NUMBER = 50
REPO = "vocable-android"
DONE_STATUSES = ["Done"]
STATUSES = ["Pre Backlog", "Backlog", "Ready to select", "In progress",
            "Development Complete (In Review)", "Ready for Demo"]
WIP_STATUSES = ["In progress", "Development Complete (In Review)", "Ready for Demo"]
RESET_STATUSES = ["Backlog", "Pre Backlog"]
START_STATUS = "In progress"
WEEKS = 10  # trailing complete weeks kept for the throughput sample


def run(cmd):
    print("+", " ".join(cmd), file=sys.stderr)
    r = subprocess.run(cmd, capture_output=True, text=True)
    sys.stderr.write(r.stderr)
    if r.returncode != 0:
        sys.exit(f"ERROR: command failed ({r.returncode}): {' '.join(cmd)}")
    return r.stdout


def discover_parents(done_path, open_path):
    names = set()
    for p in (done_path, open_path):
        data = json.loads(p.read_text())
        for n in data["issues"]["nodes"]:
            for c in n.get("fields", {}).get("components", []):
                names.add(c["name"])
    allowed = {n for n in names
               if n.startswith("#") and int(n.split(" ", 1)[0][1:]) in ALLOWED_PARENT_NUMBERS}
    dropped = names - allowed
    if dropped:
        print(f"# excluded parent(s) outside ALLOWED_PARENT_NUMBERS: {sorted(dropped)}",
              file=sys.stderr)
    return sorted(allowed)


def main():
    OUT_DIR.mkdir(parents=True, exist_ok=True)
    DRIVE_DIR.mkdir(parents=True, exist_ok=True)
    today = date.today().isoformat()

    run(["python3", str(SCRIPTS / "gh_fetch.py"),
         "--org", ORG, "--project-number", str(PROJECT_NUMBER), "--repo", REPO,
         "--done-status", *DONE_STATUSES,
         "--statuses", *STATUSES,
         "--breakdown-field", "parent",
         "--out-dir", str(OUT_DIR)])

    done_path, open_path = OUT_DIR / "done.json", OUT_DIR / "open.json"
    parents = discover_parents(done_path, open_path)
    print(f"# discovered {len(parents)} parent(s): {parents}", file=sys.stderr)

    json_out = OUT_DIR / "forecast.json"
    forecast_cmd = ["python3", str(SCRIPTS / "forecast.py"),
                    "--done", str(done_path), "--open", str(open_path),
                    "--changelog", str(done_path), str(open_path),
                    "--project", "Vocable-Android",
                    "--wip-status", *WIP_STATUSES,
                    "--reset-status", *RESET_STATUSES, "--start-status", START_STATUS,
                    "--weeks", str(WEEKS), "--today", today,
                    "--json-out", str(json_out)]
    if parents:
        forecast_cmd += ["--components", *parents]
    run(forecast_cmd)

    dated_path = DRIVE_DIR / f"vocable-android-throughput-forecast-{today}.html"
    run(["python3", str(SCRIPTS / "forecast-html.py"), str(json_out), str(dated_path)])

    latest_path = DRIVE_DIR / LATEST_NAME
    latest_path.write_bytes(dated_path.read_bytes())
    print(f"# wrote {dated_path}")
    print(f"# wrote {latest_path}")


if __name__ == "__main__":
    main()
