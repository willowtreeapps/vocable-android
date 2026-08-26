# #692 — Fix Snyk secret-scanner false positive in create-ticket SKILL.md

## What and why

Snyk's secret scanner (CWE-798, "Generic Secret Key") flagged two lines in
`.claude/skills/create-ticket/SKILL.md`: a hardcoded GitHub GraphQL Project
field ID (`PVTSSF_lADOAAiCcM4AiZ3rzga7WAg`) assigned to a shell variable named
`fieldId`. The value isn't a secret — GitHub node IDs grant no access on
their own — but it's shaped exactly like one to an entropy-based scanner
(long, opaque, sitting next to an `id`/`key`-named variable), so it kept
re-triggering alerts.

The skill file also had drifted into narrative/incident-log prose (a
blow-by-blow #653/#654 bug story, a "Practical takeaway" paragraph restating
the bullet above it, "Confirmed live and worth being precise about" framing)
— the same anti-pattern this skill's own scope-creep guard tells *tickets*
to avoid.

## What changed

- The "Adding it to the board" flow now derives the Status field ID and
  "Pre Backlog" option ID live via a `gh api graphql` query, instead of
  hardcoding them. This removes the flagged literal entirely (fixes the
  false positive at the source, not just cosmetically) and also removes the
  staleness risk the old version only warned about in a footnote.
- Condensed the "Creating and linking the branch" section's narrative into
  the operative facts (~6 lines instead of a multi-paragraph incident
  story), dropping the duplicate "Practical takeaway" restatement.
- No behavior change — same `gh`/GraphQL commands, same end result.

## Notes

- The Project ID (`PVT_kwDOAAiCcM4AiZ3r`) and the "Pre Backlog" option ID
  (`77e56e59`) were not flagged by Snyk and are left hardcoded; they're lower
  entropy / less scanner-shaped and this ticket's scope was the flagged
  literal specifically.
- Adding issues to Project #50 via the GraphQL API requires a `gh` token
  with `project` scope; the token used in this session only had classic
  repo scopes, so #692 was created but not auto-added to the board — add it
  manually if it wasn't already.
