# DACN — Final Acceptance Summary

## 1. Project

**Hệ thống quản lý ca làm và điều phối nhân sự phục vụ tiệc cưới, sự kiện đa địa điểm tích hợp AI**

Technology: Java 21, Spring Boot, Spring Security + JWT, JPA/Hibernate, MySQL, React, Vite, Maven.

## 2. Acceptance baseline

- Branch: `main`
- HEAD before C10 documentation: `7e0699f`
- Historical release tag `dacn-v1.0.0` remains at its original release commit.

## 3. Acceptance status

| Checkpoint | Scope | Result |
|---|---|---|
| C0 | Baseline / cold start | PASS |
| C1 | Authentication / roles | PASS |
| C2 | Master data CRUD | PASS + CLEAN |
| C3 | Shift / registration / assignment / coordination | PASS + CLEAN |
| C4 | Reputation / replacement | PASS + CLEAN |
| C5 | Recommendation / AI fallback | PASS + CLEAN |
| C6 | Area / table + QR / OTP / GPS attendance | PASS + CLEAN |
| C7 | Payroll + dashboard + reports | PASS + CLEAN |
| C8 | Security / negative tests / configuration consistency | PASS |
| C9 | UI / UX final audit | PASS |

## 4. Automated backend regression

```text
Tests run: 94
Failures: 0
Errors: 0
Skipped: 0
BUILD SUCCESS
```

## 5. Frontend verification

- Production build: PASS
- ADMIN UI audit: PASS
- COORDINATOR UI audit: PASS
- EMPLOYEE UI audit: PASS
- Responsive mobile audit: PASS
- Final source/UI text audit: PASS
- No remaining reproducible red Console errors in accepted flows.

## 6. Security and hardening verified

- Missing, invalid and tampered JWT requests are rejected.
- Role-restricted endpoints enforce authorization.
- Invalid DTOs and invalid date ranges return client errors.
- Unsupported HTTP methods return HTTP 405 instead of HTTP 500.
- Runtime `.env` is not tracked.
- Hibernate DDL fallback is `validate`.
- Example JWT secret is valid Base64 and is not a real secret.

## 7. AI behavior

Recommendation flow:

```text
hard constraints
-> deterministic scoring
-> optional AI reranking / explanation
-> coordinator decision
```

AI cannot bypass hard constraints or autonomously assign employees.
When AI is disabled or unavailable, deterministic recommendation remains the designed fallback.

## 8. UI corrections during final acceptance

- Removed internal development wording from Dashboard and Reports.
- Replacement UI no longer requests candidates for shifts that have already started.
- Final frontend production build and `git diff --check` passed.

## 9. Intentional limitations

- No WebSocket realtime dashboard.
- No shift swap.
- No 2D/3D floorplan drag-and-drop.
- No Maps routing.
- No Excel/PDF export.
- No Kubernetes deployment.
- No autonomous AI staffing decision.
- No full browser E2E automation suite.

## 10. Documentation policy

Files `01...14` under `docs/dacn` are retained as implementation history and may reference historical development commits.

This document represents the accepted system state after checkpoints C0 through C9.
Final release tagging is performed only after C10 repository and regression verification passes.
