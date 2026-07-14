# Common Rules

<github_rules>

- GitHub resources must be managed consistently with repository governance.
- GitHub metadata must reflect verified repository state and work state.
- GitHub decisions must preserve traceability between requirements, implementation, review, and release artifacts.
- GitHub resource changes must avoid conflicting ownership, duplicate meaning, and stale status.

</github_rules>

<label_rules>

- Classification metadata must be based on verified information.
- Classification metadata must remain consistent with repository governance.
- Classification metadata must not encode temporary notes or unverified assumptions.
- Duplicate classification meaning must be avoided.

</label_rules>

<development_rules>

- Tests must be added or updated when behavior changes or risk justifies verification.
- Existing tests must be run when they are relevant and feasible.
- Implementation changes should be verified before commit when relevant verification is discoverable and feasible.
- Code must favor maintainability over cleverness.
- Code must be readable without relying on hidden context.
- Code quality must be evaluated against correctness, clarity, cohesion, and long-term change cost.
- Exception handling must preserve useful failure information and avoid masking defects.
- Implementations must prefer simple solutions that satisfy verified requirements.
- Changes must remain consistent with existing project structure, style, and conventions.
- Unrelated changes must be avoided.
- User or teammate changes must be preserved.

</development_rules>

<documentation_rules>

- Documentation must reference artifacts accurately and consistently.
- Documentation must be updated when behavior, interfaces, requirements, or operational expectations change.
- Documentation must distinguish verified facts from unresolved decisions.
- Requirement traceability must be maintained from source request to implementation and review artifacts.
- Documentation must avoid stale, redundant, or conflicting guidance.
- Documentation must use terminology consistently across related artifacts.

</documentation_rules>

## Skill Contract

All skills should follow a consistent structure.

Required sections:

- references
- triggers
- success_criteria
- trusted_sources
- cli_policy
- allowed_cli_commands

Optional sections:

- template_usage
- output
- boundaries
- validation
- readiness

Guidelines:

- Skills define behavior, process, validation, and safety rules.
- Skills should not define reusable deliverable structures when a template exists.
- Templates are the canonical source for deliverable formatting.
- Skills should reference templates rather than duplicate template content.
- Success Criteria should be objective and verifiable.
- Trusted Sources should identify authoritative information sources.
- CLI policies should minimize assumptions and prioritize verification.
- New skills should remain focused on a single primary responsibility.
- Cross-skill responsibilities should be explicit rather than implied.

## Template Registry

Deliverable-producing skills should use the following templates when available:

- requirements -> `templates/requirements.md`
- decision -> `templates/decision.md`
- specification -> `templates/specification.md`
- issue -> `templates/issue.md`
- pr -> `templates/pr.md`

Guidelines:

- Canonical template paths are repository-relative and use `templates/<template-name>.md`.
- Templates are the canonical source of deliverable formatting.
- Skills define behavior, process, validation, and safety rules.
- Skills should reference templates rather than duplicate template structure.
- Missing template information should be marked as unavailable, unresolved, or open rather than invented.
- Template usage must preserve traceability to verified sources.
- New templates should be registered here when introduced.
- New deliverable-producing skills should reference an appropriate template when one exists.

Domain, Implementation, Test, and Review outputs are intentionally skill-owned.

No reusable deliverable template is currently defined.

These skills primarily provide analysis, implementation guidance, validation results, design decisions, review findings, and workflow guidance rather than reusable deliverable artifacts.

If future reusable deliverables emerge, template adoption may be reconsidered.

## Artifact Lifecycle

Deliverables should preserve the existing workflow order:

```text
Requirements
->
Decision
->
Domain
->
Specification
->
Issue
->
Implementation
->
Test
->
Commit
->
Pull Request
->
Review
```

- Requirements, Decision, Domain, and Specification produce repository artifacts.
- Issue and Pull Request produce GitHub artifacts.
- Implementation, Test, Commit, and Repository primarily modify or verify repository state.
- Review produces review findings rather than reusable repository documents.
- Artifact flow must preserve traceability between repository artifacts and GitHub artifacts.

## Workflow Automation

Implementation changes should enter Test before Commit when relevant tests or checks are discoverable and feasible.

If Test reports implementation-related failures, return to Implementation with the failure summary and rerun relevant verification after changes are made.

If Implementation or Test discovers a verified issue outside the current work scope, route it to Issue instead of expanding the active implementation.

Discovered out-of-scope issues should reuse an existing related issue when one covers the finding, or create a new issue when issue creation is required for traceability and no verified related issue exists.

If a discovered issue blocks the current work, the blocking relationship should be recorded before continuing or stopping.

Repeat Implementation -> Test until tests pass, failures are classified as non-implementation blockers, or user input is required.

Stop the loop when the same failure repeats without new evidence, the required fix exceeds the requested scope, required information is missing, or verification requires unapproved high-risk action.

Commit readiness should consume the latest available Test result when verification affects confidence in the committed work.

Review evaluates submitted validation evidence and remote checks; Review does not run local tests.

## Artifact Locations

Long-lived documentation artifacts should use the repository's `docs/` directory as the current documentation source of truth. Version history should be represented by repository tags, commits, and dated decision records rather than by duplicating stale document files. The documentation tree is:

```text
docs/
|- README.md
|- Requirements.md
|- Versions.md
|- decisions/
|  |- README.md
|  `- <decision-slug>.md
|- Domain.md
`- Specification.md
```

Artifact mapping:

- Documentation navigation -> `docs/README.md`
- Version index -> `docs/Versions.md`
- Requirements -> `docs/Requirements.md`
- Decision index -> `docs/decisions/README.md`
- Decision record -> `docs/decisions/<decision-slug>.md`
- Domain -> `docs/Domain.md`
- Specification -> `docs/Specification.md`
- Issue -> GitHub Issue
- Pull Request -> GitHub Pull Request
- Review -> GitHub Review / Review Findings

Guidelines:

- Edit and version long-lived documentation artifacts under `docs/`.
- Treat files under `docs/` as the latest maintained documentation, not as immutable historical snapshots.
- Use `Versions.md` to point readers to tagged versions, commits, or other verified version anchors when older behavior must be found.
- Keep `docs/README.md` focused on discoverable navigation to current documentation and important indexes.
- Keep `docs/decisions/README.md` as a concise decision index and store each decision in a separate `docs/decisions/<decision-slug>.md` file.
- Use descriptive kebab-case decision slugs and preserve stable file paths after downstream artifacts reference them.
- Do not duplicate full decision content in the decision index.
- Do not encode versioned documentation by copying full pages such as `Requirements-v1.md` unless repository governance explicitly requires a separate historical artifact.
- Do not create a parallel Wiki tree for requirements, domain, or specification artifacts unless repository governance changes explicitly.
- Traceability between repository artifacts and GitHub artifacts must be preserved.

## Issue Workflow

GitHub Issues should communicate workflow state through verified labels, linked pull requests, and issue comments rather than title prefixes or duplicated planning artifacts.

Guidelines:

- `ready` means the issue has enough verified scope, context, and acceptance expectations to begin implementation.
- `blocked` means progress depends on unresolved information, an external dependency, a prerequisite issue, or an explicit decision.
- `in progress` means active work is underway on a verified branch, pull request, or assigned implementation effort.
- `done` means the issue outcome has been completed, reviewed as required, and no remaining acceptance expectations are open.
- Status labels must reflect current verified work state and should be updated when evidence changes.
- If status label names differ by repository, use the verified repository label taxonomy rather than inventing new labels.
- Blocked issues should identify the blocker in the issue body or a comment and should link the blocking issue, decision, artifact, or pull request when available.

## Pull Request Issue Links

Pull requests should link issues according to whether the submitted change fully completes the issue outcome.

Guidelines:

- Use `Closes #<issue-number>` only when merging the pull request should close the issue because all verified acceptance expectations for that issue are satisfied by the pull request.
- Use `Refs #<issue-number>` when the pull request contributes to the issue but leaves follow-up work, unresolved acceptance expectations, pending validation, or separate review work.
- Do not close broad tracking, epic, or parent issues from a partial implementation pull request.
- When a pull request uses `Refs`, document the remaining work or reason the issue stays open when that information is material to reviewers.
- If issue closure depends on non-code work, manual validation, post-merge operations, or stakeholder acceptance that is outside the pull request scope, use `Refs` unless repository governance explicitly allows closure at merge time.

<language_policy>

- Language standards must distinguish agent-facing content from human-facing collaboration content.
- Skill documents must use English.
- Agent documents must use English.
- Repository governance documents intended for agent parsing or automation must use English.
- Human-facing collaboration artifacts should use the language best suited to the target service audience.
- Commit messages should follow the language best suited to the target service audience unless the repository defines another convention.
- For Korea-focused services, commit messages, human-facing requirements, decisions, issues, pull requests, review comments, documentation artifacts, and review responses should be written primarily in Korean.
- For international or overseas-facing services, human-facing artifacts may use English or Korean-English mixed language when it improves team communication.
- When the target service audience is not defined, human-facing artifacts should default to the repository team's primary communication language.
- When both the target service audience and repository team's primary communication language are unknown, human-facing artifacts should use Korean by default while preserving technical terms in their canonical form.
- If audience uncertainty affects stakeholder communication, record the language choice as an assumption or open question in the relevant artifact.
- Commit messages should follow the applicable service audience language rule.
- Issue titles should follow the applicable service audience language rule when they are human-facing collaboration artifacts.
- Issue descriptions should follow the applicable service audience language rule when they are human-facing collaboration artifacts.
- Pull request titles should follow the applicable service audience language rule when they are human-facing collaboration artifacts.
- Pull request descriptions should follow the applicable service audience language rule when they are human-facing collaboration artifacts.
- Review comments should follow the applicable service audience language rule.
- Technical identifiers, API names, code symbols, product names, protocol names, external service names, and established repository terms should preserve their canonical English form.
- Do not translate terms when translation would reduce precision or conflict with established repository terminology.
- Human-facing collaboration artifacts should prioritize team communication effectiveness over unnecessary language standardization.
- Repository teams may define their primary communication language or audience-specific language convention without changing agent-facing language requirements.
- Language requirements must remain independent of any specific human language.

</language_policy>

<review_rules>

- Evaluation must prioritize correctness, security, reliability, maintainability, and verification.
- Findings must be based on observable evidence.
- Feedback must distinguish defects, risks, questions, and preferences.
- Criteria must be applied consistently across contributors and change types.
- Conclusions must remain objective, specific, and actionable.

</review_rules>

<repository_rules>

- Ownership boundaries and source-of-truth locations must remain explicit.
- Existing structure and conventions must be respected unless a verified need requires change.
- Metadata and governance resources must be updated only when the change requires it.
- Long-lived rules must remain separate from temporary task context.

</repository_rules>

<agent_rules>

- Skills must reference shared rules instead of duplicating reusable guidance.
- Context must be collected from authoritative local or remote sources before decisions are made.
- Artifacts must be verified before they are reported as complete.
- Decision making must prioritize verified requirements, repository conventions, user intent, and risk reduction.
- Agents must minimize assumptions when relevant information can be obtained.
- Agents must preserve existing user work and avoid unauthorized destructive actions.
- Agents must keep outputs scoped to the requested work.

</agent_rules>

<cli_rules>

- CLI usage must support verification of repository state, file contents, Git history, and GitHub resources.
- Repository state must be verified before making decisions that depend on current files, branches, commits, or status.
- Existing files must be inspected before generating modifications.
- Git history must be inspected when change scope, ownership, regression risk, or workflow consistency depends on it.
- GitHub resources should be verified before creation or modification.
- Agent decisions should be based on verified information whenever possible.
- Assumptions should be minimized when repository information can be obtained through CLI.
- CLI results must be treated as context for decisions, not as a substitute for judgment.
- CLI commands must be scoped to the information or verification needed.

</cli_rules>
