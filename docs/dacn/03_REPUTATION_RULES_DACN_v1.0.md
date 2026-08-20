# DACN Reputation & Evaluation Rules v1.0

## Purpose

Commit 3 introduces a deterministic, auditable employee reputation module. It is a business scoring system, not the AI module. Recommendation and AI commits may later consume the reputation score and its supporting history as input signals.

## Baseline

- New reputation profile starts at `80/100`.
- Existing employees receive the same baseline when `V007__employee_reputation_and_evaluation.sql` is applied.
- DACS attendance confirmed before module activation is not scored retroactively.
- Score is always clamped to `[0, 100]`.
- The event ledger stores the actual applied delta so `scoreBefore + scoreDelta = scoreAfter` always holds.

## Attendance rules

Only `CONFIRMED` attendance affects reputation.

| Result | Rule |
|---|---:|
| `PRESENT` | `+2` |
| `LATE` 1-15 minutes | `-1` |
| `LATE` 16-30 minutes | `-2` |
| `LATE` >30 minutes | `-4` |
| `EARLY_LEAVE` 1-15 minutes | `-1` |
| `EARLY_LEAVE` 16-30 minutes | `-2` |
| `EARLY_LEAVE` >30 minutes | `-4` |
| `LATE_AND_EARLY_LEAVE` | sum of both penalties |
| `ABSENT` | `-10` |

A non-absent confirmed attendance increments `completedShiftCount`. Late and early leave counters are independently incremented when their minute values are positive. An absent attendance increments `absentCount` and does not increment completed shifts.

## Evaluation rules

Only `ADMIN` and `COORDINATOR` may create evaluations in Commit 3. The target assignment must be `COMPLETED`, and one assignment may be evaluated only once.

| Rating | Reputation delta |
|---:|---:|
| 5 | `+5` |
| 4 | `+2` |
| 3 | `0` |
| 2 | `-3` |
| 1 | `-6` |

`evaluationCount` and `ratingSum` are stored in the aggregate. Average rating is derived as `ratingSum / evaluationCount`.

## Audit model

`EmployeeReputation` is the current aggregate used for efficient reads.

`ReputationEvent` is an append-only business ledger. It records:

- event type;
- source type and source id;
- score before;
- actual applied score delta;
- score after;
- reason;
- actor when applicable;
- occurrence time.

The database unique constraint on `(source_type, source_id)` prevents the same attendance, evaluation, or baseline source from being scored twice.

## Transaction boundaries

Attendance confirmation, assignment completion/absence state, and reputation update execute inside the same Spring transaction. If reputation processing fails, the confirmation transaction rolls back instead of leaving partial state.

Employee creation and baseline reputation initialization also execute inside the same employee-creation transaction.

## Scope intentionally deferred

- late-cancellation reputation events: Commit 4 after replacement/cancellation workflow exists;
- recommendation usage: Commit 5;
- AI usage: Commit 6;
- leader-originated evaluations: optional enhancement after core DACN scope is stable.
