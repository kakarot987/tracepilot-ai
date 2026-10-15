# GitHub + ChatGPT Collaboration — TracePilot AI

> Repository: [kakarot987/tracepilot-ai](https://github.com/kakarot987/tracepilot-ai)  
> Project: Java-first, local-first, multi-agent incident investigation platform.  
> Scope of ChatGPT's write access by agreement: **Markdown files only**.

## Roles and ownership

| Area | Developer | ChatGPT |
| --- | --- | --- |
| Java, React, Docker, tests, infrastructure | Implement, run, commit, and own | Explain, review, and suggest changes in chat |
| System design | Make final technical decisions | Propose approaches and tradeoffs in Markdown |
| Documentation (`.md`) | Review and approve decisions | Create and update upon request |
| GitHub issues and pull requests | Create, prioritize, merge | Suggest descriptions/checklists; inspect when requested |
| Validation | Execute builds/tests and report results | Assess provided results; do not claim unrun tests passed |

ChatGPT must **not** modify non-Markdown files or independently create repository code. Use existing project code only as context when asked to review it.

## How to work together

1. **Developer implements:** Pick an unchecked task in [TODO.md](../TODO.md), write code and tests, and push changes to GitHub.
2. **Developer asks for review:** Share a commit SHA, pull request URL, branch name, or specific paths and describe the review goal. Example: `Review my incident analysis API on main for design and test gaps; do not edit code.`
3. **ChatGPT reviews:** Read the relevant repository state through the connected GitHub integration where available. Return prioritized findings, concrete suggestions, and references to files. Clearly distinguish observed facts from recommendations.
4. **Developer fixes and verifies:** Apply code changes and run tests locally. Share results if another review is needed.
5. **ChatGPT updates docs on request:** Update only `.md` files, such as `TODO.md`, `README.md`, `docs/ARCHITECTURE.md`, or `docs/EVALUATION.md`. Commit only those Markdown changes if GitHub write access is available and the request authorizes it.
6. **Progress is recorded:** Mark tasks complete only after their exit criteria are verified; add an entry to the progress log in [TODO.md](../TODO.md).

## Suggested prompts

- **Plan:** `Look at TODO.md in kakarot987/tracepilot-ai and break down Milestone 1 into actionable Markdown tasks. Only edit .md files.`
- **Review:** `Review the latest changes to kakarot987/tracepilot-ai for correctness, tool safety, and architecture. Do not modify code.`
- **Documentation:** `Read my current Java code in kakarot987/tracepilot-ai and update docs/ARCHITECTURE.md so it matches the implemented design. Markdown only.`
- **Track progress:** `Check what is implemented in kakarot987/tracepilot-ai, compare it with TODO.md, and update verified checkboxes and the progress log. Markdown only.`
- **Release:** `Review the docs and evaluation results for the v1 release. List remaining gaps and update TODO.md only.`

## Collaboration safeguards

- The GitHub connection provides access subject to its actual permissions; it is **not** automatic continuous monitoring or an autonomous background collaborator.
- When you want a fresh repository review or a documentation commit, ask explicitly in chat and specify a target branch or PR where relevant.
- ChatGPT must not mark features done from a plan alone; inspect files and test evidence where possible.
- Do not commit credentials, `.env` secrets, personal data, or live production logs. Use synthetic incidents and sanitized examples.
- Keep the project free-first: one compact local model, controlled agent steps, and optional cloud providers with quota limits.
- Never let an agent execute production write actions without explicit human approval.
- Avoid speculative or unverified claims in docs. Document limitations and unsuccessful evaluations.

## Documentation map

- [TODO.md](../TODO.md): canonical checklist and progress log.
- `README.md`: entry point, setup, demo, and portfolio narrative (to be added).
- `docs/ARCHITECTURE.md`: implemented components and data flows (later).
- `docs/DECISIONS.md`: important architectural decisions and alternatives (later).
- `docs/EVALUATION.md`: datasets, metrics, failures, and measured results (later).

## First collaboration checkpoint

- [x] Repository exists and is readable with the GitHub integration.
- [x] `TODO.md` exists on the default branch.
- [x] Collaboration workflow documented.
- [ ] Developer creates `README.md` MVP statement or requests a Markdown draft.
- [ ] Developer completes the first working Spring Boot + Ollama integration.
- [ ] ChatGPT reviews implementation and updates verified progress when asked.
