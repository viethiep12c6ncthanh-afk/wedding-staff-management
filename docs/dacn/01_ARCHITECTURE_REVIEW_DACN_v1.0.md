# DACN Architecture Review v1.0

## 1. Baseline

Project: **Wedding Staff Management System**  
DACN topic: **Xây dựng hệ thống quản lý ca làm và điều phối nhân sự phục vụ tiệc cưới, sự kiện đa địa điểm tích hợp AI**.

Baseline DACS is immutable:

- Git tag: `dacs-v1.0.0`
- Baseline commit: `e0f09a2`
- DACN branch: `develop/dacn`
- Backend baseline: 15 tests, 0 failures, 0 errors
- Frontend baseline: production build successful

DACN must extend the DACS baseline without rewriting completed DACS business rules unless a reproducible bug is found.

## 2. Current architecture

### Backend

Technology:

- Java 21
- Spring Boot 3.5.4
- Spring Security + JWT
- Spring Data JPA / Hibernate
- Bean Validation
- MySQL
- Maven

Layering currently follows:

```text
Controller
    -> Service (@Transactional)
        -> Repository (Spring Data JPA)
            -> MySQL
```

Main domain entities:

- `UserAccount`
- `Role`
- `Employee`
- `Venue`
- `Event`
- `WorkShift`
- `ShiftRegistration`
- `ShiftAssignment`
- `Attendance`

Important services:

- `EmployeeService`
- `VenueService`
- `EventService`
- `ShiftService`
- `RegistrationService`
- `AssignmentService`
- `AttendanceService`
- `DashboardService`
- `PayrollReportService`

### Frontend

Technology:

- React 19
- Vite
- Axios
- React Router

Frontend structure:

```text
src/api
src/components/common
src/layouts
src/pages
src/styles
src/utils
```

JWT is stored in local storage and attached by the Axios interceptor. A `401` clears the session and redirects to login. Routes are protected by authentication and role checks.

## 3. DACS rules that DACN must preserve

### Account roles

```text
ADMIN
COORDINATOR
EMPLOYEE
```

`LEADER` is not an account role. It is a per-shift `ShiftRole`:

```text
LEADER
STAFF
```

### Assignment sources

```text
REGISTRATION
DIRECT
```

Direct assignment keeps:

```text
registrationId = null
assignmentSource = DIRECT
```

### Schedule overlap

Hard conflict rule:

```text
newStart < existingEnd AND newEnd > existingStart
```

Intervals are `[start, end)`, so adjacent shifts are allowed.

### Registration cancellation

An employee may directly cancel their own registration only when at least 24 hours remain before shift start.

### Attendance

Process state:

```text
DRAFT
CONFIRMED
```

Result:

```text
PRESENT
LATE
EARLY_LEAVE
LATE_AND_EARLY_LEAVE
ABSENT
```

A confirmed attendance record is immutable through the current DACS workflow.

### Payroll

At attendance confirmation the system stores:

- `basePaySnapshot`
- `payableAmount`

Reports use these stored values instead of recalculating from the current shift pay.

## 4. Architecture extension points for DACN

### 4.1 Multi-shift / multi-venue coordination

Existing model already supports multiple venues:

```text
Venue <- Event <- WorkShift
```

Therefore DACN Commit 2 should not redesign this relationship. It should add a coordination/query layer that combines:

- shift date/time
- venue
- `requiredStaff`
- active assignment count
- staffing shortage
- overlap conflicts
- transition warnings between consecutive shifts

Recommended new service boundary:

```text
CoordinationController
    -> CoordinationService
        -> WorkShiftRepository
        -> ShiftAssignmentRepository
```

Do not overload `DashboardService` with all coordination logic.

Hard overlap remains a blocking business rule. Insufficient transition time between different venues should initially be a **warning**, not a hard rejection.

A practical first version can use configurable minimum buffers instead of Maps/GPS distance calculations.

### 4.2 Reputation and evaluation

Current source already provides reliable signals:

- confirmed attendance result
- late minutes
- early-leave minutes
- absence
- completed assignments
- cancellation timestamps/reasons
- historical venue through `assignment -> shift -> event -> venue`
- historical leader/staff role

Recommended design:

```text
Employee
  -> EmployeeReputation (current aggregate)
  -> ReputationEvent (immutable history/ledger)
```

Do not store only one unexplained `reputationScore` field.

The free-text `Employee.experienceLevel` may be displayed but should not be the main recommendation signal. Historical completed shifts and venue/role history are more reliable and already derivable from the database.

### 4.3 Cancellation and replacement

Current DACS behavior must remain:

- employee self-cancel is blocked inside 24 hours;
- Admin/Coordinator can cancel an active assignment before the shift is in progress/completed.

DACN should not weaken the 24-hour DACS rule. To support real-world late cancellation, introduce a separate **cancellation/replacement request workflow**. The request can be recorded even when direct self-cancellation is no longer allowed, and Coordinator remains the final decision maker.

Recommended model boundary:

```text
ReplacementRequest
ReplacementInvitation
```

This preserves original assignment history instead of deleting/replacing records in place.

### 4.4 Candidate filtering and recommendation

Reuse the existing overlap query in `ShiftAssignmentRepository` as the hard schedule-conflict rule.

Recommended pipeline:

```text
Employee pool
  -> hard eligibility rules
  -> deterministic scoring
  -> candidate list
```

Useful features that can be derived from current data:

- ACTIVE employment status
- schedule conflict
- transition warning
- attendance history
- reputation
- completed-shift count
- recent workload
- prior work at the venue
- prior LEADER experience

This layer is **not AI** and must remain deterministic and testable.

### 4.5 AI-assisted recommendation

AI is a separate layer after deterministic filtering/scoring:

```text
Hard rules
  -> valid candidates
  -> deterministic score
  -> AI recommendation/reranking/explanation
  -> Coordinator decision
```

AI must never bypass hard constraints.

The AI integration should use an abstraction so the business service is not directly tied to one provider:

```text
AiRecommendationClient (interface)
  -> provider implementation
```

Required fallback:

```text
AI unavailable/invalid output
  -> deterministic ranking remains usable
```

Store enough recommendation metadata to explain and audit the decision.

### 4.6 QR attendance

Do not loosen the existing manual attendance authorization just to support self check-in.

Current manual flow allows Admin/Coordinator or the LEADER of the shift to record attendance. DACN QR should use separate endpoints/service methods that validate:

- authenticated employee owns the assignment;
- assignment is active;
- QR session belongs to the correct shift;
- QR/session is valid in the allowed time window;
- duplicate check-in/check-out is rejected;
- resulting attendance still follows DACS confirmation rules.

Recommended boundary:

```text
QrAttendanceController
  -> QrAttendanceService
      -> AttendanceService/domain rules where appropriate
```

### 4.7 Advanced dashboard/reporting

Current `DashboardService` is simple count aggregation. DACN can extend reporting with dedicated query DTOs/services for:

- staffing shortage by shift/venue
- attendance trends
- late/absence rates
- reputation distribution
- replacement activity
- employee workload/performance
- recommendation outcomes

Avoid turning one dashboard method into a large all-purpose query service.

## 5. Important findings / risks

### RISK-01 — SQL migration files are not Flyway migrations at runtime

The repository contains versioned SQL files (`V002` ... `V006`), but `pom.xml` currently has no Flyway dependency and these files are under `database/migrations`, not Spring Boot's Flyway classpath location.

Therefore they are currently **manual one-time versioned SQL scripts**, not automatically executed migrations.

DACN rule:

- do not edit DACS scripts;
- new database changes start from `V007__...sql`;
- apply each new script explicitly and record it in the commit/test checklist;
- do not pretend Flyway is managing schema history unless Flyway is intentionally introduced later.

For safety, do not introduce Flyway in Commit 1. Reassess migration automation during hardening after the DACN schema has stabilized.

### RISK-02 — `ddl-auto=update` remains enabled by default in dev

During development this is convenient but it means Hibernate and SQL scripts can both mutate schema.

Policy for DACN:

- SQL scripts remain the intended versioned schema record;
- after schema stabilizes, hardening should switch verification environments to `ddl-auto=validate`;
- never rely only on Hibernate auto-update for a release database.

### RISK-03 — Existing tests are mainly unit tests with mocks

The DACS baseline has useful service/validation/error tests, but no full database/API integration suite.

DACN must add tests for critical cross-entity workflows, especially:

- coordination counts and conflicts
- reputation ledger idempotency
- replacement concurrency/state transitions
- QR duplicate/expiry rules
- recommendation hard-rule filtering
- AI fallback
- authorization

### RISK-04 — Some frontend pages are already large

Several DACS pages are 400–600+ lines. DACN should not keep appending complex workflows into those files.

New DACN functionality should prefer dedicated pages/components and reusable UI pieces rather than making existing CRUD pages monolithic.

### RISK-05 — Late cancellation requires a new workflow, not weakening DACS

The current employee self-cancel method intentionally rejects cancellation inside 24 hours. Reputation rules cannot simply penalize a late self-cancellation event that the system never records.

DACN replacement should therefore record a separate request/incident, then let Coordinator resolve it. This creates usable reputation history without breaking the DACS rule.

## 6. Architecture decision for Commit 1

Commit 1 is documentation/architecture only. It does not modify DACS business behavior.

Accepted decisions:

1. DACS `dacs-v1.0.0` stays immutable.
2. DACN development continues on `develop/dacn`.
3. Multi-venue coordination gets a dedicated coordination layer.
4. Reputation uses current aggregate + immutable event history.
5. Late cancellation/replacement uses a request workflow; DACS 24-hour self-cancel rule remains.
6. Recommendation is split into deterministic filtering/scoring and a separate AI layer.
7. AI always has deterministic fallback and cannot bypass hard rules.
8. QR self check-in gets dedicated endpoints/service; manual attendance authorization is not weakened.
9. New schema scripts begin at `V007` and remain explicit/manual until migration automation is deliberately adopted.
10. Advanced dashboard/reporting uses focused query services/DTOs.
11. Regression baseline remains 15 backend tests + successful frontend production build.
12. Each later milestone must pass build/test/API/UI checks before commit.
