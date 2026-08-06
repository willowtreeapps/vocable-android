# #666 — Remove vendored throughput-forecast files; install globally instead

## What

Removed everything the throughput-forecast GitHub-Projects port (#653, #664)
had vendored into this repo:

- `.claude/scripts/{gh_fetch.py,forecast.py,forecast-html.py}`
- `.claude/skills/throughput-forecast/{SKILL.md,config.yaml}`
- `.claude/settings.json` (existed solely for this skill's Bash allowlist —
  removed entirely rather than left with an empty `permissions.allow`)

`create-ticket` (#654/#656) is untouched — separate concern, explicitly out
of scope. `.claude/settings.local.json` is unrelated pre-existing config, not
touched either.

## Why

A reviewer on PR #655 flagged that `gh_fetch.py` belonged in the shared
`dq-documentation` skill repo, not vendored per-app — that skill's
`forecast.py`/`forecast-html.py`/`config.yaml` are already written to be
reused by any team. The original plan (tracked in this issue) was to move
`gh_fetch.py` there while `vocable-android` kept its own project-specific
config. On reflection, that still leaves an unnecessary footprint in this
repo — no part of the skill needs to be **read from** vocable-android's own
git history, since Claude Code skills are discovered from `.claude/skills/`
per *session*, not per repo, and can just as easily be installed **globally**
at `~/.claude/skills/`, available for any project's GitHub board without
committing anything into any one app's repo. That's the direction actually
taken: PR #665 (the parent-breakdown work) was closed unmerged rather than
revised, and the whole skill was installed at
`~/.claude/skills/github-throughput-forecast/` instead — see that skill's
own `SKILL.md` for the generalized, non-repo-specific version of the setup.

## Key decisions

**Nothing here required reverting merged history.** #655/#656 are merged
into `feature/voice-selection`, and other branches have since branched off
it — reverting those merge commits would risk rippling into unrelated work.
Instead, this is a normal forward-moving delete commit, same as any other
cleanup — safe regardless of what else has branched off `feature/voice-selection`
since.

**#665 was closed, not merged-then-reverted.** It never merged, so its
parent-breakdown/zero-series fixes exist only in the global skill install
now (carried over when copying the files there), not lost — just relocated
before ever landing in this repo.

**`run_daily_forecast.py` (from #664) was never part of this cleanup** — it
was never merged (only ever sat on #664/#665's now-closed branch), so there
was nothing to remove here. It's being relocated to a personal,
non-repo-committed location as a separate step, pointed at the new global
skill scripts.

## Links

- Issue: #666
- Superseded: PR #655 (merged, files now removed), PR #665 (closed unmerged)
- Replacement: `~/.claude/skills/github-throughput-forecast/` (local, global install — not in any repo)
