# DACN Commit 7 — QR / OTP / GPS Attendance Backend

## 1. Scope

Commit 7 adds backend self-attendance while preserving the DACS attendance lifecycle.

```text
manual attendance (existing) ─────────────┐
                                          ├─ Attendance DRAFT ──> manager CONFIRMED
QR / OTP self-attendance (new) ───────────┘
```

The new flow does **not** let an employee confirm attendance, calculate final payroll, or bypass assignment ownership.

## 2. Existing rules preserved

- Attendance remains one record per assignment.
- Only `ASSIGNED` / `CONFIRMED` assignments are attendable.
- A `CONFIRMED` attendance is immutable.
- Final confirmation remains ADMIN / COORDINATOR only.
- Payroll snapshot and reputation updates still happen only through the existing `AttendanceService.confirm(...)`.
- Manual attendance authorization for ADMIN / COORDINATOR / shift LEADER is unchanged.

## 3. Session model

ADMIN / COORDINATOR creates a short-lived attendance session for one shift and one action:

```text
CHECK_IN
CHECK_OUT
```

A session has:

- random 256-bit QR token;
- six-digit OTP fallback;
- `validFrom` / `expiresAt`;
- optional GPS center + radius;
- creator and optional revocation metadata.

Only the raw QR token and OTP returned by the creation API are shown to the manager. The database stores:

- SHA-256 of the random QR token;
- BCrypt hash of the OTP.

Creating a new active session for the same shift/action revokes the previous active session.

Default session lifetime: 10 minutes. Request may choose 1–30 minutes.

## 4. Action windows

Backend also applies shift-relative windows independently of token expiry:

- CHECK_IN: from 120 minutes before shift start until shift end.
- CHECK_OUT: from shift start until 240 minutes after shift end.

This means a valid token cannot be reused outside the attendance window.

## 5. GPS

GPS is supplemental, not continuous tracking.

A session may be created:

- without coordinates: no GPS check;
- with `latitude + longitude`: GPS becomes mandatory for that session.

When coordinates are configured, radius defaults to 150 m and may be 20–1000 m.

The backend calculates Haversine distance at the moment of check-in/check-out. It stores only the submitted point and computed distance in the immutable check event.

`Venue` currently has no latitude/longitude fields, so Commit 7 intentionally stores the location policy on the short-lived attendance session instead of changing the venue CRUD model. Commit 8 can let a coordinator populate the session coordinates from the browser/device UI.

## 6. API

### Create session — ADMIN / COORDINATOR

```http
POST /api/qr-attendance/sessions
```

Example:

```json
{
  "shiftId": 12,
  "action": "CHECK_IN",
  "validMinutes": 10,
  "latitude": 10.7769,
  "longitude": 106.7009,
  "radiusMeters": 150
}
```

Response contains the one-time display values `qrToken` and `otp`.

### Revoke session — ADMIN / COORDINATOR

```http
POST /api/qr-attendance/sessions/{id}/revoke
```

### Self check-in — EMPLOYEE

```http
POST /api/qr-attendance/check-in
```

QR:

```json
{
  "shiftId": 12,
  "qrToken": "<token>",
  "latitude": 10.7769,
  "longitude": 106.7009
}
```

OTP fallback:

```json
{
  "shiftId": 12,
  "otp": "123456",
  "latitude": 10.7769,
  "longitude": 106.7009
}
```

Exactly one of `qrToken` / `otp` is accepted.

### Self check-out — EMPLOYEE

```http
POST /api/qr-attendance/check-out
```

Same credential rules as check-in, but requires a CHECK_OUT session.

## 7. Hard validation order

```text
authenticated employee
  -> exactly one credential
  -> valid session
  -> correct shift
  -> correct CHECK_IN/CHECK_OUT action
  -> not revoked / not expired
  -> inside shift action window
  -> GPS radius if configured
  -> employee owns active assignment
  -> Attendance still DRAFT
  -> duplicate check-in/check-out rejected
  -> write immutable check event
```

## 8. Concurrency and duplicate protection

- Shift assignment lookup uses `PESSIMISTIC_WRITE`.
- Attendance lookup by assignment uses `PESSIMISTIC_WRITE`.
- Existing `uk_attendance_assignment` still guarantees one Attendance per assignment.
- `attendance_check_events` adds a unique constraint on `(attendance_id, action)`.
- New QR/OTP session creation locks active sessions before revoking/replacing them.

## 9. Tables

`V010__qr_otp_gps_attendance.sql` adds:

```text
attendance_check_sessions
attendance_check_events
```

It does not alter the existing `attendances` table.

## 10. Security boundaries

QR/OTP self-attendance is evidence collection only.

It does not:

- create assignments;
- assign another employee;
- confirm attendance;
- mark payroll final;
- update reputation directly;
- track employee GPS continuously.

The coordinator remains the final authority through the existing attendance confirmation workflow.

## 11. Acceptance criteria

Backend acceptance for Commit 7:

- migration V010 applies successfully;
- all pre-existing tests still pass;
- new QR attendance unit tests pass;
- manager can create CHECK_IN and CHECK_OUT sessions;
- employee can check in using QR;
- employee can check out using OTP fallback;
- wrong employee cannot use a session for an assignment they do not own;
- expired/revoked/wrong-action sessions fail;
- duplicate check-in/check-out fails;
- GPS outside radius fails when GPS is configured;
- resulting Attendance remains `DRAFT`;
- existing ADMIN/COORDINATOR confirm still works afterward.
