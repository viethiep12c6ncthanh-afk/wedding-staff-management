# PROJECT_HANDOFF_DACN.md

## Project

**Xây dựng hệ thống quản lý ca làm và điều phối nhân sự phục vụ tiệc cưới, sự kiện đa địa điểm tích hợp AI**

## Baseline

- DACS release tag: `dacs-v1.0.0`
- Baseline commit: `e0f09a2`
- DACN branch: `develop/dacn`
- Do not rewrite DACS migrations or completed DACS behavior unless a reproducible bug is found.

## Technology

Backend:

- Java 21
- Spring Boot 3.5.4
- Spring Security + JWT
- Spring Data JPA / Hibernate
- Maven
- REST/JSON

Frontend:

- React 19
- Vite
- Axios
- React Router

Database:

- MySQL

## DACS completed scope

- JWT login
- `ADMIN`, `COORDINATOR`, `EMPLOYEE`
- employee/account management
- venue management
- event lifecycle
- shift lifecycle
- employee registration
- approve/reject registration
- direct assignment
- registration-based assignment
- schedule overlap check
- basic attendance
- fixed shift pay snapshot
- dashboard
- payroll report
- API error handling
- backend automated tests
- React frontend workflows

## Critical business rules

- `LEADER` is a shift role, not an account role.
- Assignment source is `REGISTRATION` or `DIRECT`.
- Direct assignment has `registrationId = null`.
- Overlap rule: `newStart < existingEnd AND newEnd > existingStart`.
- Time intervals are `[start, end)`.
- Employee direct self-cancel requires at least 24 hours before shift start.
- Attendance is `DRAFT` or `CONFIRMED`.
- Confirmed attendance is not edited by the DACS workflow.
- Payroll uses stored `basePaySnapshot` and `payableAmount`.
- Physical Employee deletion is not used.

## Current schema history

Historical one-time SQL scripts:

```text
V002__user_employee_refactor.sql
V003__venue_event_shift_refactor.sql
V004__registration_assignment_refactor.sql
V004_1__allow_reassignment_after_cancellation.sql
V005__attendance_basic_pay_refactor.sql
V006__employee_management.sql
```

Important: the current project does **not** have Flyway configured. These are manual versioned SQL scripts. New DACN database changes begin with `V007__...sql`; old scripts are never edited or rerun on the existing database.

## Baseline verification

At DACN start:

```text
Backend: 15 tests, 0 failures, 0 errors
Frontend: production build successful
Git working tree: clean
```

## Official DACN milestone roadmap

1. Architecture baseline/review
2. Multi-shift/multi-venue coordination
3. Employee reputation and evaluation
4. Cancellation/replacement workflow
5. Deterministic candidate filtering/scoring
6. AI-assisted recommendation
7. QR attendance backend
8. QR attendance frontend
9. Advanced dashboard/reporting
10. Operational audit/decision history
11. Integration/security/regression hardening
12. Final documentation/demo/release

## AI boundary

Deterministic rules first:

```text
hard eligibility
  -> deterministic scoring
  -> valid/top candidates
```

AI second:

```text
valid candidates
  -> AI reranking/explanation
  -> Coordinator decision
```

AI must never bypass hard rules. If AI is unavailable or returns invalid output, the deterministic ranking remains usable.

## Important extension decisions

- Multi-venue is already supported by the current `Venue <- Event <- WorkShift` model; DACN adds coordination/query logic instead of redesigning the domain relation.
- Transition time between different venues is initially a warning using configurable buffers; GPS/Maps is optional.
- Reputation uses an aggregate plus immutable history/events.
- Late cancellation is modeled through a separate request/replacement workflow so the DACS 24-hour direct self-cancel rule remains intact.
- QR self check-in uses dedicated endpoints/service methods; do not broadly relax manual attendance authorization.
- New complex frontend workflows should use dedicated pages/components rather than continuously enlarging existing DACS CRUD pages.

## Development workflow for every milestone

```text
checkpoint before
-> inspect current source
-> design schema/API
-> new SQL script if needed
-> backend
-> automated tests
-> API smoke test
-> frontend
-> UI test
-> production build
-> git diff/status review
-> commit
-> checkpoint after
```

Never commit:

- `.env`
- real passwords
- real JWT secrets
- database backups
- `node_modules`
- `target`
- `dist`

## Prompt for a new chat

> Continue DACN from DACS baseline `dacs-v1.0.0` on branch `develop/dacn`. Read the current source and `docs/PROJECT_HANDOFF_DACN.md` before changing code. Preserve DACS business rules and historical SQL scripts. Follow the 12-milestone roadmap in `docs/dacn/02_ROADMAP_DACN_v1.0.md`. Do not call deterministic scoring AI; AI is a separate reranking/explanation layer with fallback and cannot bypass hard constraints.
