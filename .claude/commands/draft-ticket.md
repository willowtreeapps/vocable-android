---
description: Draft (or create) a Vocable Android ticket from a Figma design + repo context
---

You are helping draft a well-formed project ticket for the **vocable-android** repo. The user
will describe a feature/bug and usually give a Figma link or frame. Do this:

1. **Read the design.** If a Figma URL or node is provided, use the Figma MCP tools to read the
   frame(s) — capture the intended UI, states, and any copy. If no link is given, ask for one (or
   proceed from the text description and say so).
2. **Ground it in the code.** Search this repo for the relevant screen/domain/data code
   (`app/src/main/java/com/willowtree/vocable/{ui,domain,data}`) so the ticket names real files,
   components, and the likely touch-points. Note anything that collides with a "don't poke the
   bear" area from CLAUDE.md (head-tracking, accessibility palette, localization) and flag it.
3. **Check the board + existing issues.** Use the GitHub MCP tools to check the project board
   (org project 50) and open issues so you don't duplicate one; link related issues.
4. **Write the ticket** in this shape:
   - **Title** — concise, imperative.
   - **Context / problem** — what and why (link the Figma frame).
   - **Proposed approach** — grounded in the actual code; call out affected files/modules.
   - **Acceptance criteria** — checkbox list, testable.
   - **Test notes** — unit (`app/src/test`) vs on-device (`VocableTestRunner`) implications.
   - **Label** — `bug` or `enhancement`.
   - **Risks / guardrails** — any maintainer-review or accessibility considerations.
5. **Confirm before creating.** Show the draft and ask before creating it on GitHub. Only create
   the issue after the user says go — never open it silently.
