# Test Skill

<references>

- `skills/common.md`

</references>

<test_triggers>

- Use this skill when local verification must be planned, discovered, executed, summarized, or classified.
- Use this skill after implementation changes when relevant tests or checks are discoverable and feasible.
- Use this skill before Commit Skill when verification affects commit readiness.
- Use this skill when a failing test result must be classified and routed back to implementation, environment setup, or user input.
- Do not use this skill as the primary skill for implementation, issue planning, commit generation, pull request creation, or code review.

</test_triggers>

<test_discovery>

- Test commands must be discovered from verified repository sources before execution.
- Authoritative sources may include package scripts, build files, task runner configuration, CI configuration, README files, contributor documentation, and existing test commands in repository history when relevant.
- Relevant tests should be selected based on changed files, affected behavior, repository conventions, and available test organization.
- Broad test suites may be used when targeted tests are unavailable and execution is feasible.
- Test commands must not be invented.
- Missing test commands must be reported as unavailable verification.

</test_discovery>

<test_execution>

- Execute relevant local tests or checks when they are discoverable, feasible, and within the requested scope.
- Prefer targeted tests before broad suites when targeted tests provide meaningful confidence.
- Use non-watch, non-interactive commands.
- Avoid destructive tests unless explicitly requested and approved.
- Avoid commands that require unavailable external services, credentials, paid resources, or network access unless the user explicitly approves the required setup.
- Do not install dependencies unless the user explicitly approves dependency installation or the repository workflow already provides them.
- Stop execution when a command is unsafe, interactive, destructive, or requires unapproved escalation.

</test_execution>

<failure_classification>

- Failed tests must be classified before routing the next step.
- Implementation Failure means the failure is likely caused by the current implementation or changed behavior.
- Test Failure means the test expectation or test setup appears inconsistent with verified requirements or repository behavior.
- Environment Failure means the failure depends on missing tools, dependencies, credentials, services, files, permissions, or platform conditions.
- Flaky or Non-Deterministic Failure means the result is inconsistent or timing-dependent and cannot be confidently attributed without more evidence.
- Scope Blocker means fixing or verifying the failure would exceed the requested work scope.
- Discovered Issue means testing revealed a verified defect, gap, or risk outside the current work scope that should be tracked separately.
- Unknown Failure means the failure cannot be classified from available evidence.
- Implementation Failure should route back to Implementation Skill with the failure summary.
- Discovered Issue should route to Issue Skill for related issue discovery or new issue creation when traceability is needed.
- Environment Failure, Scope Blocker, and Unknown Failure should be reported with the missing information or required user decision.

</failure_classification>

<loop_control>

- Implementation -> Test may repeat when failures are classified as Implementation Failure.
- The loop should stop when tests pass, failures are classified as non-implementation blockers, or user input is required.
- The loop should stop when the same failure repeats without new evidence.
- The loop should stop when fixing the failure would exceed requested scope.
- The loop should stop or pause when a discovered issue is outside the current work scope and must be tracked separately.
- The loop should stop when verification requires unapproved high-risk, destructive, interactive, or network-dependent action.
- Each loop should preserve the latest command, outcome, failure summary, and routing decision.

</loop_control>

<test_result_reporting>

- Test results must distinguish executed commands from unavailable or skipped verification.
- Test results must include command, outcome, relevant failure summary, classification, and recommended next skill when applicable.
- Passing results should recommend Commit Skill when commit readiness is the next workflow step.
- Failing implementation-related results should recommend Implementation Skill.
- Out-of-scope verified findings should recommend Issue Skill and should not be treated as current implementation failures.
- Unavailable verification should explain why the command could not be discovered or executed.
- Test summaries must not overstate coverage or confidence.

</test_result_reporting>

## Standard Output Format

### Test Result

- Passed
- Failed
- Unavailable
- Skipped

### Commands

- Commands executed or considered.

### Failure Classification

- Implementation Failure
- Test Failure
- Environment Failure
- Flaky or Non-Deterministic Failure
- Scope Blocker
- Discovered Issue
- Unknown Failure
- Not Applicable

### Summary

- What was verified.
- What failed or could not be verified.
- Relevant evidence.

### Recommended Next Skill

- Implementation
- Issue
- Commit
- Pull Request
- User Input Required
- None

## Success Criteria

- Relevant test or check commands are discovered from verified repository sources when available.
- Executed commands and outcomes are reported accurately.
- Failures are classified with supporting evidence.
- Implementation-related failures are routed back to Implementation Skill.
- Verified out-of-scope findings are routed to Issue Skill for related issue discovery or new issue creation.
- Passing or unavailable verification is clearly communicated for Commit and Pull Request readiness.
- Unsafe, destructive, interactive, or unapproved verification is not executed.

<trusted_sources>

- Repository test configuration is authoritative for available local test commands.
- Package scripts, build files, task runner configuration, CI configuration, README files, and contributor documentation are authoritative when they define verification workflows.
- Changed files and verified implementation scope are authoritative for selecting relevant tests.
- Command output is authoritative for executed local test results.
- CI and remote checks are authoritative for remote validation when local execution is unavailable or not requested.
- Repository documentation and governance resources are authoritative for verification expectations.
- Test decisions must prioritize verified information over assumptions.
- Conflicting validation signals must be reported and reconciled before readiness conclusions are made.

</trusted_sources>

<cli_policy>

- CLI verification should be used to discover repository test commands and relevant changed files.
- Local test execution must use commands discovered from verified repository sources or explicitly requested by the user.
- Test commands should be scoped to relevant verification whenever feasible.
- Commands requiring dependency installation, external services, credentials, network access, destructive state changes, or interactive watch mode should not run without explicit approval.
- CLI results must be treated as validation evidence, not as a substitute for judgment.

</cli_policy>

<allowed_cli_commands>

- `git status`
- `git status --short`
- `git diff`
- `git diff --staged`
- `git ls-files`
- `git log`
- `git show`
- `rg`
- `ls`
- `find`
- `cat`
- `pwd`
- Verified repository test commands discovered from package scripts, task runners, build files, CI configuration, README files, contributor documentation, or explicit user instruction.

</allowed_cli_commands>
