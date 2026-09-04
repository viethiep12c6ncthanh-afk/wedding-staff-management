# DACN Commit 10 — Multi-rule Payroll

## 1. Scope

Commit 10 extends the existing attendance-confirmation payroll snapshot without changing QR/OTP attendance ownership or recalculating historical payroll.

Rules in `DACN_MULTI_RULE_V1`:

- `BASE_SHIFT_PAY`: snapshot `WorkShift.payAmount`.
- `LEADER_ALLOWANCE`: 10% of base pay when `ShiftAssignment.shiftRole = LEADER`.
- `LATE_DEDUCTION`: prorated by `lateMinutes / scheduledMinutes`.
- `EARLY_LEAVE_DEDUCTION`: prorated by `earlyLeaveMinutes / scheduledMinutes`.
- `OVERTIME`: prorated by overtime minutes with multiplier `1.5`.
- `ABSENT`: payable amount is always 0.
- Final payable is never negative.

Money is rounded to 2 decimals with `HALF_UP`.

## 2. Formula

```text
scheduledMinutes = shift.endAt - shift.startAt
overtimeMinutes  = max(0, attendance.checkOutAt - shift.endAt)

leaderAllowance    = LEADER ? basePay * 10% : 0
lateDeduction      = basePay * lateMinutes / scheduledMinutes
earlyLeaveDeduction = basePay * earlyLeaveMinutes / scheduledMinutes
overtimePay        = basePay * overtimeMinutes / scheduledMinutes * 1.5

payable = max(
    0,
    basePay
    + leaderAllowance
    + overtimePay
    - lateDeduction
    - earlyLeaveDeduction
)
```

## 3. Snapshot policy

`AttendanceService.confirm()` remains the only payroll settlement point. Confirmed attendance remains immutable.

New snapshot columns:

```text
payroll_policy_version
leader_allowance_snapshot
late_deduction_snapshot
early_leave_deduction_snapshot
overtime_minutes_snapshot
overtime_pay_snapshot
```

Existing columns remain authoritative:

```text
base_pay_snapshot
payable_amount
```

V012 backfills existing confirmed records with:

```text
payroll_policy_version = DACS_FLAT_V1
all new adjustment snapshots = 0
```

It deliberately does not recompute existing `base_pay_snapshot` or `payable_amount`.

## 4. Reporting

Existing endpoints remain unchanged:

```text
GET /api/reports/payroll
GET /api/reports/payroll/mine
```

Reports aggregate stored snapshots only and expose:

- total base pay
- total leader allowance
- total overtime pay
- total late deduction
- total early-leave deduction
- total payable

The same breakdown is aggregated per employee.

## 5. Compatibility

- QR/OTP/GPS continues to create/update DRAFT attendance only.
- Reputation is still applied exactly when attendance is confirmed.
- `LEADER` remains a per-shift assignment role, not an account role.
- No historical confirmed attendance is recalculated.
- No Flyway dependency is introduced; V012 is a manual one-time migration.

## 6. Acceptance checklist

1. Back up the database before V012.
2. Apply `V012__multi_rule_payroll.sql` exactly once.
3. Verify all old confirmed rows are `DACS_FLAT_V1` and keep the same `base_pay_snapshot` / `payable_amount`.
4. Run `mvn clean test`.
5. Run frontend `npm run build`.
6. Runtime smoke:
   - STAFF present
   - LEADER present
   - late
   - early leave
   - overtime
   - absent
   - employee payroll report
   - manager payroll report
7. Review `git diff --check` and staged diff before commit.
