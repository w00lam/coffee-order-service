# Implementation Skill

<references>

- `skills/common.md`

</references>

<implementation_triggers>

- Use this skill when verified requirements, issues, specifications, review feedback, or test failures must be implemented in repository files.
- Use this skill when code, documentation, configuration, templates, or repository artifacts must be changed to satisfy verified work scope.
- Use this skill when Test Skill reports implementation-related failures that require repository changes.
- Use this skill when review feedback is accepted and requires implementation changes.
- Do not use this skill as the primary skill for requirements gathering, domain analysis, specification generation, issue planning, test execution, commit generation, pull request creation, or code review.

</implementation_triggers>

<implementation_inputs>

- Implementation must consume verified requirements, issues, specifications, review feedback, test failure summaries, repository artifacts, and repository state when they affect the requested change.
- Implementation scope must be based on verified user intent and authoritative repository context.
- Missing or conflicting input information must be reported before implementation proceeds when it prevents a safe change.
- Test failure inputs must identify the failing command, failure summary, affected behavior, and failure classification when available.
- Implementation must not invent requirements, issue scope, API behavior, labels, branches, commits, pull requests, review state, test commands, or validation results.

</implementation_inputs>

<repository_area_inspection>

- Before implementation, inspect relevant documentation artifacts first when they exist.
- Documentation artifacts are the primary planning source for implementation scope, accepted decisions, domain boundaries, specifications, and traceability.
- Relevant documentation artifacts may include `docs/Requirements.md`, `docs/decisions/README.md`, `docs/decisions/*.md`, `docs/Domain.md`, and `docs/Specification.md`.
- Source code directories should be inspected after documentation context is understood and only when behavior, interfaces, data flow, or application logic may change.
- Test directories should be inspected after documentation context is understood and only when behavior changes require new or updated verification.
- Configuration files and directories should be inspected after documentation context is understood and only when runtime behavior, tooling, build, dependency, environment, lint, format, or test execution may be affected.
- Script and task runner definitions should be inspected after documentation context is understood and only when existing automation, commands, generation, or verification workflows may be affected.
- Templates and governance files should be inspected after documentation context is understood and only when the change affects issue, pull request, review, commit, test, implementation, artifact, label, branch, or repository workflow.
- Existing references should be searched before changing names, contracts, public APIs, configuration keys, file locations, or documented behavior.
- Repository areas and documentation artifacts that do not exist should not be invented; missing relevant documentation artifacts should be reported when they affect implementation confidence.
- Inspection must remain scoped to the requested work and should not become a repository-wide audit unless the request requires it.

</repository_area_inspection>

<scope_control>

- Implementation changes must remain scoped to the requested work.
- Related changes may be included only when they are necessary to complete or verify the requested work.
- Unrelated cleanup, formatting, refactoring, or documentation changes should be avoided unless required by the work.
- Newly discovered defects, gaps, or risks outside the verified work scope must not be silently absorbed into the current implementation.
- Out-of-scope findings should be routed to Issue Skill when they are issue-worthy and need traceability.
- If an out-of-scope finding blocks the requested implementation, report the blocker and related issue status before continuing.
- Existing user and teammate changes must be preserved.
- Modified files must be inspected before editing when their contents affect the implementation.
- Ownership boundaries and repository conventions must be respected.
- Destructive operations must not be performed unless explicitly requested.

</scope_control>

<implementation_execution>

- Implementations must prefer simple solutions that satisfy verified requirements.
- Existing project structure, patterns, helper APIs, and conventions should be followed.
- Behavior changes must include or preserve appropriate verification paths when feasible.
- Tests should be added or updated when behavior changes or risk justifies verification.
- Specification-driven development should be used when specifications, decisions, requirements, or documentation artifacts define the intended behavior.
- Test-driven development should be preferred when behavior can be specified through focused tests before implementation.
- For learning-oriented implementation work, tests should be treated as executable specification that the user can read before or alongside the implementation.
- When the workflow is educational and tests are added or changed, implementation output should summarize the purpose of the tests in plain language.
- Documentation and artifacts must be updated when implementation changes affect documented behavior, interfaces, requirements, or operational expectations.
- Error handling must preserve useful failure information and avoid masking defects.
- Implementation should leave the repository in a state that can be verified by Test Skill when relevant tests are discoverable and feasible.

</implementation_execution>

<test_failure_handling>

- Test failure summaries from Test Skill must be treated as implementation inputs.
- Implementation-related failures should be addressed by modifying the smallest necessary repository scope.
- Environment failures, dependency failures, flaky failures, destructive test requirements, or unclear failures should be reported rather than treated as implementation defects.
- If the same failure repeats without new evidence, implementation should stop and report the blocker instead of looping indefinitely.
- If fixing a failure would exceed the requested scope, implementation should stop and report the scope boundary.
- If a failure reveals a separate issue outside the current work scope, implementation should route the finding to Issue Skill instead of broadening the current fix.
- After implementation-related failures are addressed, control should return to Test Skill for relevant verification when feasible.

</test_failure_handling>

<handoff>

- After changes are made, the next skill should usually be Test Skill when relevant tests or checks are discoverable and feasible.
- If no relevant test can be discovered or executed, the implementation output must identify what was changed and what remains unverified.
- Commit Skill should be used only after relevant verification is completed or unavailable verification is clearly reported.
- Review Skill should be used only after submitted work or review feedback must be evaluated.

</handoff>

## Success Criteria

- Requested changes are implemented within verified scope.
- Existing user and teammate work is preserved.
- Relevant repository conventions and ownership boundaries are respected.
- Behavior, documentation, configuration, or artifact changes are internally consistent.
- Test Skill can verify the change when relevant tests are discoverable and feasible.
- Learning-oriented changes expose the intended behavior through readable tests when feasible.
- Out-of-scope findings are reported or routed to Issue Skill rather than folded into the current implementation.
- Remaining uncertainty, unavailable verification, or scope blockers are reported.

<trusted_sources>

- Verified requirements are authoritative for requested outcomes, constraints, and acceptance expectations.
- Issues are authoritative for planned work scope and traceability when issue-linked work is implemented.
- Specifications are authoritative for intended behavior, interfaces, data models, flows, states, and business rules when relevant.
- Review feedback is authoritative for accepted requested changes when review response or verification work is requested.
- Test Skill results are authoritative for local test commands, outcomes, failure summaries, and failure classifications when available.
- Documentation artifacts are the primary planning source for implementation scope, accepted decisions, domain boundaries, specifications, and traceability when they exist.
- Repository files are authoritative for current implementation, documentation, configuration, templates, and artifacts.
- Repository documentation and governance resources are authoritative for conventions, ownership boundaries, and artifact expectations.
- Git state is authoritative for modified files, staged files, branch state, and working tree context when available.
- Implementation decisions must prioritize verified information over assumptions.
- Conflicting sources must be reconciled before changes are finalized.

</trusted_sources>

<cli_policy>

- CLI verification should be used when repository state, file contents, or existing behavior affect implementation.
- Relevant documentation artifacts should be inspected before repository files when they exist and affect implementation planning.
- Existing files must be inspected before editing when they are part of the requested change.
- Repository search should be used to find existing patterns, tests, configuration, and affected references.
- Git state should be checked when preserving user or teammate changes depends on current modifications.
- Test execution belongs to Test Skill and should not be performed as part of Implementation Skill unless the selected workflow explicitly loads Test Skill.
- CLI usage must be scoped to understanding and changing the requested implementation.

</cli_policy>

<allowed_cli_commands>

- `git status`
- `git status --short`
- `git diff`
- `git diff --staged`
- `git log`
- `git show`
- `git branch`
- `git rev-parse`
- `git ls-files`
- `rg`
- `ls`
- `find`
- `cat`
- `pwd`

</allowed_cli_commands>
