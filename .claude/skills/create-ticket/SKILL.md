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

## Creating and linking the branch

**Sub-issue tickets whose PR will target a parent integration branch** (e.g.
`feature/voice-selection` — the normal case per `CLAUDE.md`'s sub-issue
convention) **must use `createLinkedBranch` to show up under the issue's
"Development" section.** `Closes #N` does not link or auto-close when the
PR's base isn't the repo's default branch (`closingIssuesReferences` comes
back empty, `willCloseTarget: false`) — confirmed on #653/#654. There is also
no mutation to attach an already-existing branch/PR after the fact —
`createLinkedBranch` only creates a new ref, it can't adopt one. So call it
**first**, before pushing any branch with that name; if a plain branch is
pushed first, the only fix is deleting and recreating both the branch and PR.

1. Get the issue's node ID (same query as "Adding it to the board" step 1),
   the repository's node ID, and the base commit to branch from (the tip of
   the parent's integration branch, e.g. `feature/voice-selection`, or `main`
   for a standalone issue with no parent):
   ```bash
   git rev-parse origin/<base-branch>
   gh api repos/willowtreeapps/vocable-android --jq '.node_id'
   ```
2. Create + link the branch in one call (name follows the usual
   `feature/<issue-number>/<short-description>` convention) — needs
   **write access to the repo** (fails with `FORBIDDEN: does not have the
   correct permissions` if missing, a distinct failure from the
   `project`-scope `INSUFFICIENT_SCOPES` case below):
   ```bash
   gh api graphql -f query='
   mutation($issueId: ID!, $oid: GitObjectID!, $name: String!, $repositoryId: ID!) {
     createLinkedBranch(input: {issueId: $issueId, oid: $oid, name: $name, repositoryId: $repositoryId}) {
       linkedBranch { ref { name } }
     }
   }' -f issueId="<issue node id>" -f oid="<base commit SHA>" \
      -f name="feature/<issue-number>/<short-description>" -f repositoryId="<repo node id>"
   ```
3. Fetch and check out the branch it just created, instead of creating a new
   local one:
   ```bash
   git fetch origin feature/<issue-number>/<short-description>
   git checkout feature/<issue-number>/<short-description>
   ```

Standalone issues targeting `main` don't have this problem — `Closes #N`
works normally there — but every sub-issue in this repo's workflow targets a
parent branch, so treat `createLinkedBranch`-first as the default.

## Adding it to the board

Every issue this skill creates also gets added to [Project #50](https://github.com/orgs/willowtreeapps/projects/50/views/1) with Status **"Pre Backlog"** — an issue not on the board is invisible to triage/prioritization.

The `gh` token needs the `project` scope for this (not just `read:project`) — if a mutation below fails with `INSUFFICIENT_SCOPES`, run `gh auth refresh -s project` (an interactive device-flow approval) before retrying.

1. **Get the issue's node ID** (from the `gh issue create` output URL, or query it):
   ```bash
   gh api graphql -f query='
   query { repository(owner: "willowtreeapps", name: "vocable-android") {
     issue(number: <N>) { id }
   } }'
   ```
2. **Add it to the project** (project node ID `PVT_kwDOAAiCcM4AiZ3r` — Project #50 on `willowtreeapps`):
   ```bash
   gh api graphql -f query='
   mutation($projectId: ID!, $contentId: ID!) {
     addProjectV2ItemById(input: {projectId: $projectId, contentId: $contentId}) {
       item { id }
     }
   }' -f projectId="PVT_kwDOAAiCcM4AiZ3r" -f contentId="<issue node id>"
   ```
   Note the returned `item.id` — that's the **project item ID**, different from the issue's node ID, and what the next steps need.
3. **Look up the Status field ID and "Pre Backlog" option ID** — derive these live rather than hardcoding them; they're GitHub GraphQL object IDs (not secrets), but they're project config that can be renumbered if the board's fields/options ever change:
   ```bash
   gh api graphql -f query='
   query { organization(login: "willowtreeapps") { projectV2(number: 50) {
     fields(first: 20) { nodes {
       ... on ProjectV2SingleSelectField { id name options { id name } }
     } }
   } } }' --jq '.data.organization.projectV2.fields.nodes[] | select(.name=="Status")'
   ```
   Take the field's `id` and the `id` of the `"Pre Backlog"` option from the output.
4. **Set Status to "Pre Backlog"**:
   ```bash
   gh api graphql -f query='
   mutation($projectId: ID!, $itemId: ID!, $fieldId: ID!, $optionId: String!) {
     updateProjectV2ItemFieldValue(input: {
       projectId: $projectId, itemId: $itemId, fieldId: $fieldId,
       value: { singleSelectOptionId: $optionId }
     }) { projectV2Item { id } }
   }' -f projectId="PVT_kwDOAAiCcM4AiZ3r" -f itemId="<project item id from step 2>" \
      -f fieldId="<field id from step 3>" -f optionId="<option id from step 3>"
   ```
5. **Move it to the top of its column** — new tickets should sort first in
   the board's grouped-by-Status view. Omitting `afterId` on
   `updateProjectV2ItemPosition` moves an item to the front of the whole
   project-wide list, which also puts it first within its own Status group,
   without disturbing other items' relative order.
   ```bash
   gh api graphql -f query='
   mutation($projectId: ID!, $itemId: ID!) {
     updateProjectV2ItemPosition(input: {projectId: $projectId, itemId: $itemId}) {
       clientMutationId
     }
   }' -f projectId="PVT_kwDOAAiCcM4AiZ3r" -f itemId="<project item id from step 2>"
   ```

## Linking sub-issues to their parent

If the ticket has a `Part of #<parent>` line, also link it via GitHub's **native sub-issue relationship** (not just the text reference) — this is what populates the parent issue's "Sub-issues progress" and this issue's "Parent issue" fields on the project board automatically; there's no separate project-field mutation for it.

```bash
gh api graphql -f query='
mutation($issueId: ID!, $subIssueId: ID!) {
  addSubIssue(input: {issueId: $issueId, subIssueId: $subIssueId}) {
    issue { number }
    subIssue { number }
  }
}' -f issueId="<parent issue node id>" -f subIssueId="<new issue node id>"
```

Get each node ID the same way as step 1 above (`repository(...).issue(number: N).id`). Skip this step entirely for standalone issues (no `Part of #<parent>` line).

After creating and linking, this is the point where the "Starting new work" workflow in `CLAUDE.md` continues: branch as `feature/<issue-number>/<short-description>`, PR against the right integration branch using `.github/PULL_REQUEST_TEMPLATE.md`, and a `Documentation/work-log/<issue-number>-<slug>.md` entry once the work is done.
