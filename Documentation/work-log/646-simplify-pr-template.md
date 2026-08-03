# Simplify .github/PULL_REQUEST_TEMPLATE.md

**Issue:** #646 (standalone process change, no parent)

## What was needed

The original PR template (`## What` / `## Why it's safe` / `## Out of scope / follow-up`, all free-text prose) was designed for the heavier sub-issue PRs in the #613 breakdown, but added too much friction for smaller, everyday PRs — too dense to fill out quickly.

## What changed

Replaced it with a lighter, checkbox-driven template: `## Summary` (bullets), `## Ticket` (GitHub issue reference — this repo tracks work in GitHub Issues, not Jira, so the ticket-link wording was adapted accordingly), `## Type of Change`, `## Testing`, and `## Checklist` (tests pass locally via `./gradlew testDebug`, no secrets committed, `CLAUDE.md` updated if a new pattern was introduced). No "Author" section, per explicit direction.

`CLAUDE.md`'s "Starting new work" section (step 3) updated to reference the new template shape instead of the old one.

## Pointers

- Issue: #646
