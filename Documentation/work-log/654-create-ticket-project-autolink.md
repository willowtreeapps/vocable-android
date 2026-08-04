# #654 — create-ticket: auto-add issues to Project #50 and link sub-issue parents

## What

Updated `.claude/skills/create-ticket/SKILL.md` so that after `gh issue create`,
the new issue is also:
1. Added to [Project #50](https://github.com/orgs/willowtreeapps/projects/50)
   with Status set to **"Pre Backlog"**.
2. Moved to the top of its Status column (`updateProjectV2ItemPosition`,
   `afterId` omitted) instead of landing wherever the API defaults it.
3. If it has a `Part of #<parent>` line, linked to that parent via GitHub's
   native sub-issue relationship (`addSubIssue`), not just the text reference.
4. Given a working branch via `createLinkedBranch` (issue's native
   "Development" link) instead of a plain `git checkout -b`, so the branch
   shows up under the issue's Development section from the start of work,
   not only once a PR eventually exists.

## Why

Tickets created via `/create-ticket` previously existed only as GitHub issues
until someone manually added them to the board — invisible to triage until
that happened. Sub-issues also weren't linked via GitHub's native
parent/sub-issue relationship, so the project board's "Parent issue"/
"Sub-issues progress" fields stayed empty even when the ticket body said
`Part of #N` in prose.

## Key decisions

**Project field mutation vs. native sub-issue relationship — these are two
different mechanisms, not one.** Setting Status is a project-field mutation
(`updateProjectV2ItemFieldValue`, scoped to one project). Linking a sub-issue
to its parent is a **repo-level relationship** (`addSubIssue`, via `Issue.parent`/
`Issue.subIssues`) that the project's "Parent issue"/"Sub-issues progress"
fields merely *reflect* — there is no project-field mutation for parent
linkage; the project fields are read-only projections of the repo-level
relationship. `SKILL.md` documents both, in the right place for each.

**Column position is a side effect of a project-wide order, not a per-column
one.** `updateProjectV2ItemPosition` orders items on a single flat list for
the whole project; there's no separate "position within this Status group"
concept in the API. Moving an item to the front of that flat list
(`afterId` omitted) is sufficient to make it first within its own Status
group too, since nothing at all precedes it — confirmed live (see below) that
this doesn't reorder other items relative to each other, it just shifts the
moved item ahead of everything.

**Requires the `project` OAuth scope**, not just `read:project` — the write
mutations (`addProjectV2ItemById`, `updateProjectV2ItemFieldValue`) fail with
`INSUFFICIENT_SCOPES` on a read-only token. Documented the `gh auth refresh -s
project` fix inline so it isn't a surprise mid-task.

**IDs are hardcoded in the doc (project/field/option IDs), with an escape
hatch.** These don't change often, but `SKILL.md` includes the introspection
query to re-derive them if the project's fields/options are ever edited,
rather than leaving future-Claude to guess or break silently.

**`createLinkedBranch` needs repo write access, distinct from the `project`
scope issue above.** Tested live against issue #654 with the current token
(which already has `project`/`repo`/`read:org` scopes) and got `FORBIDDEN:
rhyslutsky does not have the correct permissions to execute
CreateLinkedBranch` — a different failure mode than `INSUFFICIENT_SCOPES`,
confirming it's a repo-collaborator-access gap, not a token-scope gap (the
same gap already blocking `git push`/PR creation on #653). `SKILL.md` calls
this out explicitly so the two failure modes aren't confused with each other.
Once write access exists, this step should just work — nothing else about it
needs to change.

## Verification

- Confirmed `Issue.parent`/`Issue.subIssues`/`Mutation.addSubIssue` exist via
  GraphQL introspection (this repo's schema does support native sub-issues).
- Confirmed the **read side is already real, not hypothetical**: issue #630
  already carries a live `parent` link to #613 in this repo — so the
  underlying relationship is genuinely in use here, not a feature nobody's
  touched.
- Did **not** fabricate a test parent/child link on real issues just to
  exercise `addSubIssue` — the read-side confirmation above gives high
  confidence in the mutation shape (matches the introspected input fields
  exactly: `issueId`, `subIssueId`) without polluting real issue timelines.
  The next real sub-issue created via this skill is the first live exercise
  of the write path — check its result once that happens.
- **Did** live-verify the project-add + Status-set steps for real, by
  dogfooding them against issue #654 itself (this ticket): added to
  Project #50, Status confirmed as "Pre Backlog" via a follow-up query.

## Links

- Issue: #654
- Project: https://github.com/orgs/willowtreeapps/projects/50/views/1
