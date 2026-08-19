# DACN Roadmap v1.0

## Goal

Develop DACN from the immutable DACS baseline into a strong software-engineering student project with one clear AI use case, end-to-end workflows, testability, explainability and portfolio value.

The roadmap has **12 milestone commits**. Small `fix:` commits may exist between milestones when needed.

## Milestone 1 — Architecture baseline

Suggested commit:

```text
docs(dacn): establish architecture review and roadmap
```

Scope:

- audit DACS source
- record extension points
- record migration policy
- freeze DACS/DACN boundary
- record 12-commit roadmap

No DACS business behavior changes.

## Milestone 2 — Multi-shift / multi-venue coordination

Suggested commit:

```text
feat(coordination): add multi-shift multi-venue coordination
```

Scope:

- daily/date-range coordination view
- venue filtering
- required vs active assigned staff
- under-staffed indication
- hard schedule overlap remains unchanged
- transition/buffer warning between consecutive shifts
- API + UI + tests

Prefer configuration-based transition buffers before GPS/Maps complexity.

## Milestone 3 — Employee reputation and evaluation

Suggested commit:

```text
feat(reputation): add employee reputation and evaluation history
```

Scope:

- reputation aggregate
- immutable reputation event/history
- attendance-derived rules
- absence/late/early-leave signals
- completed-shift signals
- evaluation input where appropriate
- API + UI + tests

## Milestone 4 — Cancellation and replacement workflow

Suggested commit:

```text
feat(replacement): add cancellation and replacement workflow
```

Scope:

- cancellation/replacement request
- reason and timestamps
- detect staffing shortage
- replacement invitation
- accept/decline/expire
- Coordinator confirmation
- preserve original assignment history
- tests for state transitions

Do not weaken the DACS 24-hour self-cancel rule.

## Milestone 5 — Deterministic candidate recommendation

Suggested commit:

```text
feat(recommendation): add candidate filtering and deterministic scoring
```

Scope:

- ACTIVE employee filter
- overlap hard rule
- transition warning/penalty
- reputation
- attendance history
- completed-shift history
- venue experience
- recent workload
- prior LEADER experience
- explainable deterministic score
- API + tests + UI

This milestone is not called AI.

## Milestone 6 — AI-assisted recommendation

Suggested commit:

```text
feat(ai): add ai-assisted staffing recommendation
```

Scope:

- provider-independent AI client interface
- structured candidate input
- structured model output
- reranking/recommendation explanation
- deterministic fallback
- invalid-output handling
- recommendation metadata/audit
- Coordinator remains final decision maker

AI cannot bypass hard business rules.

## Milestone 7 — Secure QR attendance backend

Suggested commit:

```text
feat(qr-attendance): add secure qr attendance backend
```

Scope:

- QR session/token for shift
- expiry/time window
- employee ownership/assignment validation
- check-in
- check-out
- duplicate/reuse prevention
- integration with DACS attendance confirmation
- backend tests

## Milestone 8 — QR attendance frontend

Suggested commit:

```text
feat(qr-attendance-ui): add qr attendance user interface
```

Scope:

- Coordinator/Leader QR display where permitted
- employee check-in/check-out screen
- mobile-friendly flow
- expired/invalid/not-assigned/already-used states
- UI smoke test

## Milestone 9 — Advanced dashboard and reports

Suggested commit:

```text
feat(reporting): add advanced workforce dashboard and reports
```

Scope:

- staffing shortage
- per-venue/per-shift summaries
- attendance trends
- late/absence rates
- reputation summaries
- replacement statistics
- employee performance/workload
- recommendation outcomes
- filters by date/venue where useful

## Milestone 10 — Operational audit and decision history

Suggested commit:

```text
feat(audit): add operational audit and decision history
```

Scope is intentionally selective:

- replacement lifecycle
- reputation changes
- recommendation/AI decisions
- QR attendance events
- important confirmation actions

Do not build an unnecessarily generic audit platform.

## Milestone 11 — Integration, security and regression hardening

Suggested commit:

```text
test(hardening): strengthen integration security and regression coverage
```

Scope:

- preserve all DACS regression behavior
- add DACN service/integration tests
- authorization tests
- concurrency/state-transition tests where critical
- database constraint/index review
- API error cases
- frontend loading/error/empty states
- deterministic AI fallback tests
- verify schema using `ddl-auto=validate` in a controlled verification environment

## Milestone 12 — Final documentation and release

Suggested commit:

```text
release(dacn): finalize documentation demo and release
```

Scope:

- final README
- ERD
- Use Case
- Class Diagram
- Sequence Diagrams
- Activity Diagrams
- AI architecture explanation
- API/Postman collection
- test report
- demo dataset
- final end-to-end demo scenario
- backend test
- frontend production build
- release tag

## Optional stretch goals

Only after the mandatory roadmap is stable:

- GPS validation
- OTP fallback
- real-time notification
- shift swap
- Excel/PDF export
- AI incident summary
- deployment enhancements

These must not delay the mandatory workflow.

## End-to-end demo target

```text
Multi-venue dashboard
  -> under-staffed shift
  -> cancellation/replacement request
  -> deterministic candidate filtering
  -> AI-assisted recommendation
  -> Coordinator selects candidate
  -> replacement accepted/confirmed
  -> QR check-in/check-out
  -> attendance confirmed
  -> reputation updated
  -> dashboard/report updated
```

This scenario is the primary integration target for the DACN.
