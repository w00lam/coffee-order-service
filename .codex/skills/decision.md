# Decision Skill

<references>

- `skills/common.md`
- `skills/requirements.md`

</references>

<decision_triggers>

- Use this skill when verified requirements expose meaningful choices, trade-offs, unresolved decisions, or competing downstream directions.
- Use this skill after requirements validation and before domain analysis when downstream work depends on selected direction.
- Use this skill when architectural, product, workflow, repository, artifact, or integration choices must be recorded before domain, specification, issue, or implementation work proceeds.
- Use this skill when prior decisions must be reviewed for current applicability, superseded status, or downstream impact.
- Do not use this skill as the primary skill for requirements gathering, domain modeling, specification generation, issue creation, implementation, test execution, commit generation, pull request creation, or code review.

</decision_triggers>

<decision_identification>

- Decision analysis must identify choices that materially affect downstream domain, specification, issue, implementation, test, pull request, or review work.
- Decisions must be based on verified requirements, constraints, repository governance, existing artifacts, user intent, and relevant repository state.
- Trivial preferences should not be recorded as decisions unless they affect traceability, governance, scope, risk, or downstream work.
- Missing information that prevents a decision must be recorded as an open decision rather than guessed.
- Decision analysis must distinguish accepted decisions, rejected alternatives, open decisions, assumptions, and constraints.

</decision_identification>

<option_analysis>

- Options must be compared against verified requirements and constraints.
- Option analysis should identify benefits, costs, risks, reversibility, downstream impact, and traceability.
- Rejected alternatives must include the reason they were not selected.
- Accepted decisions must include the rationale for selection.
- Decision confidence must reflect available evidence rather than preference alone.
- Decision analysis must not invent unavailable requirements, constraints, repository state, labels, issues, branches, commits, pull requests, or validation results.

</option_analysis>

<downstream_impact>

- Decisions must identify affected downstream skills and artifacts when relevant.
- Domain impact should identify affected responsibility boundaries, ownership, dependencies, or grouping.
- Specification impact should identify affected interfaces, data models, flows, states, business rules, validation rules, or documented behavior.
- Issue impact should identify whether new or updated issue planning is needed.
- Implementation impact should identify constraints or boundaries that implementation must follow without creating implementation plans.
- Test impact should identify verification expectations or risks without executing tests.
- Open decisions that block downstream work must be reported before handoff.

</downstream_impact>

<decision_lifecycle>

- Decisions may be Proposed, Accepted, Superseded, Rejected, or Open.
- Accepted decisions represent the current chosen direction.
- Rejected decisions represent considered alternatives that should not guide downstream work.
- Superseded decisions represent prior accepted decisions replaced by newer verified decisions.
- Open decisions represent unresolved choices that need more information or stakeholder input.
- Decision updates must preserve prior rationale and traceability instead of silently rewriting history.

</decision_lifecycle>

<artifact_management>

- Decision artifacts should be generated in repository-defined locations.
- When no more specific documentation location is defined, each decision should be written to a separate `docs/decisions/<decision-slug>.md` file.
- The decision index at `docs/decisions/README.md` should be created or updated with a concise link to each decision record.
- Full decision content must remain in the individual decision record rather than being duplicated in the index.
- Decision records must preserve traceability to requirements, constraints, source requests, repository artifacts, and downstream artifacts when relevant.
- Decision records should avoid duplicating full requirements, domain definitions, specifications, issue plans, implementation plans, or review findings.
- Decision records should be concise enough to support future maintenance and review.

</artifact_management>

## Output

Use `templates/decision.md`.

Populate all applicable sections.

Do not omit mandatory sections defined by the template.

Document only verified decision context, options, rationale, and impact.

Place unresolved decisions under Open Decisions.

Place unverified but necessary working assumptions under Assumptions.

The template is the canonical output format for decision deliverables.

## Success Criteria

- Material decisions are identified from verified requirements, constraints, and repository context.
- Accepted decisions include rationale and traceability.
- Rejected alternatives include reasons for rejection.
- Open decisions and missing information are explicit.
- Downstream impact is identified for domain, specification, issue, implementation, test, pull request, or review work when relevant.
- Decision status is clear and current.
- Decision artifacts are written to the repository-defined decision location when artifact generation is requested.

<trusted_sources>

- Verified requirements are authoritative for requested outcomes, constraints, acceptance expectations, and decision drivers.
- User requests are authoritative for current intent when they are unambiguous and current.
- Repository governance resources are authoritative for artifact locations, ownership boundaries, workflow expectations, and source-of-truth rules.
- Existing decision artifacts are authoritative when they are current and consistent with verified requirements and repository state.
- Existing repository artifacts and documentation are authoritative when they affect decision context or downstream impact.
- Git history is authoritative for historical decision provenance when relevant.
- Decision work must prioritize verified information over assumptions.
- Conflicting sources must be reconciled before decisions are accepted.
- Unverified information must be recorded as an assumption or open decision rather than treated as a decision.

</trusted_sources>

<cli_policy>

- CLI verification may be used when repository artifacts, existing decisions, documentation, or governance resources affect decision analysis.
- Existing decision artifacts should be inspected before creating or updating decisions when they may overlap.
- Repository files should be inspected when they provide current context needed to evaluate options or downstream impact.
- Git history should be inspected only when historical decisions, provenance, or prior context affect the decision.
- CLI usage must be scoped to decision identification, option analysis, decision recording, and downstream impact validation.

</cli_policy>

<allowed_cli_commands>

- `git ls-files`
- `git show`
- `git log`
- `rg`
- `ls`
- `find`
- `cat`
- `pwd`

</allowed_cli_commands>
