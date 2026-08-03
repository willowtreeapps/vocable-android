---
name: create-ticket
description: Use when starting new work that needs a GitHub issue in this repo, or when asked to create/write/file a ticket. Enforces a tight, single-unit-of-work template so tickets don't grow into sprawling, hard-to-follow documents (see #613, #617, #622 for what this is meant to prevent).
---

# create-ticket

This repo's existing large tickets (#613, #617, #622) accumulated day-by-day plans, duplicate "resolved/unresolved decisions" tables, and inline strikethrough history until they became hard for anyone — human or Claude — to actually use. This skill exists to stop that pattern from recurring: every new ticket gets scoped to one unit of work and written in one fixed shape.

## Before drafting

Gather (ask the user via AskUserQuestion if not already clear from the conversation):
1. **The problem/need** — not the title restated, the actual "why."
2. **Parent issue**, if this is part of a larger ticket (`Part of #N`). Omit if standalone.
3. **Design/doc links** (Figma, Drive, etc.), if any — these get referenced as bare pointers, never pasted/embedded as content.

## Template — follow exactly, no extra sections

```
Part of #<parent>            <!-- omit this line entirely if standalone -->

## Why
2-4 sentences. The problem or need driving this ticket, not a restatement of the title.

## Scope
One sentence naming the single unit of work this issue covers.

## Acceptance Criteria
- [ ] Short, independently-verifiable checklist items

## Out of Scope
- Adjacent things this issue deliberately does NOT cover, with a pointer to where they ARE tracked (another issue number, or "not yet tracked")

## Links
- Design/doc links as bare URLs only — no embedded tables, screenshots-as-text, or copy-pasted doc content
```

## Scope-creep guard — check before creating

If the drafted issue would need **more than ~8 acceptance criteria**, or the "Why" section is actually describing **more than one unrelated concern**, stop before creating it. Say so explicitly and propose splitting into multiple issues instead (one per unit of work, each `Part of #<same parent>` if applicable). This check is the actual point of this skill — it's the direct fix for how #613 and #617 grew unmanageable. Don't skip it because splitting feels like extra work up front; it's cheaper than the alternative.

Do not add: day-by-day plans, "resolved decisions" / "decisions still needed" tables, embedded design mockup descriptions, or strikethrough-tracked history. If a decision needs to change after the ticket is created, edit the Acceptance Criteria directly or leave a comment — don't accumulate a decision log inside the issue body.

## Creating the issue

Once the body is drafted and passes the scope-creep guard, create it with `gh issue create -R willowtreeapps/vocable-android --title "<title>" --body "<drafted body>"`.

After creating, this is the point where the "Starting new work" workflow in `CLAUDE.md` continues: branch as `feature/<issue-number>/<short-description>`, PR against the right integration branch using `.github/PULL_REQUEST_TEMPLATE.md`, and a `Documentation/work-log/<issue-number>-<slug>.md` entry once the work is done.
