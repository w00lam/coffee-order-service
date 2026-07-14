# AGENTS.md

<purpose>

- This file defines how the agent selects and uses project skills.
- The agent acts as a router and executor.
- Detailed rules must remain in `skills/*.md`.
- This file must not duplicate skill rules.

</purpose>

<available_skills>

- `skills/common.md`
- `skills/requirements.md`
- `skills/decision.md`
- `skills/domain.md`
- `skills/specification.md`
- `skills/repository.md`
- `skills/issue.md`
- `skills/implementation.md`
- `skills/test.md`
- `skills/commit.md`
- `skills/pr.md`
- `skills/review.md`

</available_skills>

<agent_role>

- Select the correct skill for the user request.
- Load only the skill needed for the current task.
- Always apply `skills/common.md`.
- Use CLI verification when repository state, GitHub state, or file contents affect the task.
- Prefer verified information over assumptions.
- Keep actions scoped to the user request.
- Avoid destructive actions unless explicitly requested.
- Do not merge pull requests unless explicitly requested and repository governance allows it.

</agent_role>

<skill_routing>

- Use `skills/repository.md` for repository initialization, branch strategy, label strategy, template setup, artifact organization, and repository readiness checks.
- Use `skills/requirements.md` for requirements gathering, requirements clarification, requirements validation, scope definition, constraint identification, assumption identification, and requirement readiness evaluation.
- Use `skills/decision.md` for decision identification, interactive decision facilitation, option comparison, trade-off analysis, decision recording, rejected alternatives, open decision tracking, and downstream impact guidance.
- Use `skills/domain.md` for domain identification, domain decomposition, domain boundary analysis, domain dependency analysis, domain grouping, domain complexity analysis, ownership analysis, and domain allocation guidance.
- Use `skills/specification.md` for API specification generation, data model specification, ERD generation, flow generation, sequence diagram generation, state definition, business rule definition, validation rule definition, and specification artifact generation.
- Use `skills/issue.md` for issue planning, issue creation, issue decomposition, label selection, priority assignment, and working branch name definition.
- Use `skills/implementation.md` for implementing verified issue, requirement, specification, review, or test-failure driven changes in repository files.
- Use `skills/test.md` for local verification planning, test command discovery, test execution, test failure classification, and validation result reporting.
- Use `skills/commit.md` for change analysis, commit scope validation, commit message generation, and commit readiness checks.
- Use `skills/pr.md` for pull request creation, pull request update, change summary generation, test validation summary, and review readiness validation.
- Use `skills/review.md` for pull request review, file review, diff review, code block review, line review, review comment generation, review summary generation, review response generation, review verification, resolve conversation decisions, and approve or request changes decisions.

</skill_routing>

<execution_rules>

- Read `skills/common.md` first.
- Load exactly one task-specific skill by default.
- Load additional skills only when the user request explicitly spans multiple workflows or when the common workflow requires a handoff.
- Do not load unrelated skills.
- Multi-skill execution must be justified by the requested work.
- Skill loading should minimize unnecessary context consumption.
- Prefer the smallest valid skill set required to complete the task.
- Use trusted sources defined in the selected skill.
- Use allowed CLI commands defined in the selected skill.
- Verify repository state before making repository, issue, implementation, test, commit, pull request, or review decisions.
- Clearly report when required information is missing.
- Avoid inventing labels, branches, issues, commits, pull requests, review state, test commands, or validation results.
- Preserve user and teammate changes.

</execution_rules>

<workflow_rules>

- When implementation work should be traceable to a GitHub Issue, verify whether a related issue already exists before implementation begins.
- If no related issue exists and the work is issue-worthy, use Issue Skill to create a new issue with verified labels before Implementation Skill.
- Implementation changes should be tested before commit when relevant tests are discoverable and feasible.
- If Test Skill reports implementation-related failures, return to Implementation Skill with the failure summary.
- If Implementation or Test discovers a verified issue outside the current work scope, use Issue Skill to find or create a separate issue instead of expanding the current implementation scope.
- If the discovered issue blocks the current work, record the new or existing issue as a blocker before continuing or stopping.
- Repeat Implementation -> Test until tests pass, failures are classified as non-implementation blockers, or user input is required.
- Stop the loop when the same failure repeats without new evidence, the fix would exceed requested scope, required information is missing, or execution requires unapproved high-risk action.
- Commit Skill should consume the latest available Test Skill result when commit readiness depends on verification.
- Review Skill evaluates submitted validation evidence and remote checks; it does not run local tests.

</workflow_rules>

<github_action_rules>

- GitHub CLI may be used only when needed and allowed by the selected skill.
- The agent may create or modify issues, labels, branch-related metadata, commits, pull requests, pull request reviews, and review comments.
- GitHub write actions must be scoped to the requested work.
- GitHub state should be verified before write operations when verification is possible.
- Non-destructive actions may proceed when explicitly requested by the user.
- Routine repository workflow actions should not require additional confirmation when the user's intent is clear.
- Routine repository workflow actions include creating issues, creating pull requests, creating review comments, creating reviews, updating issue metadata, and updating pull request metadata.
- Broad repository changes require additional verification before execution.
- Actions affecting multiple GitHub resources require additional verification before execution.
- Repository-wide changes must be verified before execution.
- Higher-risk actions include repository-wide modifications, bulk modifications across multiple resources, resource deletion, and destructive repository operations.
- Preview requirements should apply primarily to destructive, repository-wide, or high-impact actions.
- The agent should preview intended GitHub write actions before execution when the action is destructive, repository-wide, high-impact, or broadly affects multiple resources.
- Safety mechanisms should not unnecessarily block normal issue, pull request, review, commit, or label workflows.
- The agent must not merge pull requests unless explicitly requested.
- The agent must not delete branches unless explicitly requested.
- The agent must not delete labels unless explicitly requested.
- The agent must not delete issues unless explicitly requested.
- Destructive actions require explicit user intent.
- Destructive actions should require confirmation before execution when confirmation has not already been provided.
- The agent must not perform destructive repository actions unless explicitly requested.

</github_action_rules>

<output_rules>

- Report what skill was used.
- Report what was verified.
- Report what action was taken.
- Report what could not be verified.
- Report what follow-up action is needed, if any.
- Keep output concise and task-focused.

</output_rules>

<language_policy>

- Follow the language policy defined in `skills/common.md`.
- Human-facing artifact language must follow the service audience rules in `skills/common.md`.
- Skill and agent documents must be written in English.
- Commit messages should use English.
- Issue titles, issue descriptions, pull request titles, pull request descriptions, review comments, and review responses should follow the service audience language rules unless the repository defines another convention.

</language_policy>

<constraints>

- Do not duplicate the full contents of any skill file.
- Do not define detailed issue, commit, pull request, review, implementation, test, or decision rules in `AGENTS.md`.
- Do not assume unavailable repository or GitHub state.
- Do not perform merge automation by default.

</constraints>
