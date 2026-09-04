# DACN v1.0 — Architecture & Demo Diagrams

Các sơ đồ dưới đây là **conceptual diagrams** phục vụ báo cáo/demo. Chúng mô tả quan hệ nghiệp vụ chính, không thay thế physical schema của MySQL.

## 1. Conceptual ERD

```mermaid
erDiagram
    ROLE ||--o{ USER_ACCOUNT : grants
    USER_ACCOUNT ||--o| EMPLOYEE : owns
    VENUE ||--o{ EVENT : hosts
    EVENT ||--o{ WORK_SHIFT : contains

    EMPLOYEE ||--o{ SHIFT_REGISTRATION : submits
    WORK_SHIFT ||--o{ SHIFT_REGISTRATION : receives

    EMPLOYEE ||--o{ SHIFT_ASSIGNMENT : works
    WORK_SHIFT ||--o{ SHIFT_ASSIGNMENT : staffs
    SHIFT_REGISTRATION o|--o| SHIFT_ASSIGNMENT : source

    WORK_SHIFT ||--o{ SHIFT_AREA : defines
    SHIFT_AREA ||--o{ SHIFT_TABLE : contains
    SHIFT_AREA o|--o{ SHIFT_ASSIGNMENT : places
    SHIFT_ASSIGNMENT }o--o{ SHIFT_TABLE : covers

    SHIFT_ASSIGNMENT ||--o| ATTENDANCE : records
    WORK_SHIFT ||--o{ ATTENDANCE_CHECK_SESSION : opens
    ATTENDANCE ||--o{ ATTENDANCE_CHECK_EVENT : audits

    EMPLOYEE ||--|| EMPLOYEE_REPUTATION : aggregates
    EMPLOYEE ||--o{ REPUTATION_EVENT : history
    SHIFT_ASSIGNMENT ||--o| EMPLOYEE_EVALUATION : evaluated

    SHIFT_ASSIGNMENT ||--o{ REPLACEMENT_REQUEST : original
    REPLACEMENT_REQUEST ||--o{ REPLACEMENT_INVITATION : offers
    REPLACEMENT_REQUEST o|--o| SHIFT_ASSIGNMENT : replacement

    REPLACEMENT_REQUEST ||--o{ AI_RECOMMENDATION_RUN : analyzes
```

## 2. Use-case view

```mermaid
flowchart LR
    A[ADMIN]
    C[COORDINATOR]
    E[EMPLOYEE]

    subgraph System[Wedding Staff Management]
        U1[Quản lý nhân viên / venue / event / shift]
        U2[Điều phối nhiều ca / nhiều venue]
        U3[Duyệt đăng ký / phân công]
        U4[Replacement workflow]
        U5[Deterministic + AI recommendation]
        U6[Tạo QR/OTP attendance session]
        U7[Self check-in / check-out]
        U8[Confirm attendance]
        U9[Reputation / evaluation]
        U10[Payroll / reports / dashboard]
        U11[Area / table placement]
    end

    A --> U1
    A --> U2
    A --> U3
    A --> U4
    A --> U5
    A --> U6
    A --> U8
    A --> U9
    A --> U10
    A --> U11

    C --> U2
    C --> U3
    C --> U4
    C --> U5
    C --> U6
    C --> U8
    C --> U9
    C --> U10
    C --> U11

    E --> U4
    E --> U7
    E --> U10
```

## 3. Service-level class diagram

```mermaid
classDiagram
    class CoordinationService
    class ReputationService
    class ReplacementService
    class CandidateRecommendationService
    class AiRecommendationService
    class AiRecommendationClient
    class OllamaRecommendationClient
    class OpenAiRecommendationClient
    class QrAttendanceService
    class AttendanceService
    class ShiftPlacementService
    class PayrollCalculator
    class PayrollReportService
    class OperationsAnalyticsService
    class WorkforceAnalyticsService
    class AiAnalyticsService

    ReplacementService --> CandidateRecommendationService
    AiRecommendationService --> CandidateRecommendationService
    AiRecommendationService --> AiRecommendationClient
    AiRecommendationClient <|.. OllamaRecommendationClient
    AiRecommendationClient <|.. OpenAiRecommendationClient

    QrAttendanceService --> AttendanceService
    AttendanceService --> PayrollCalculator
    AttendanceService --> ReputationService

    PayrollReportService --> AttendanceService : reads snapshots
    OperationsAnalyticsService --> CoordinationService
```

## 4. Replacement + AI sequence

```mermaid
sequenceDiagram
    actor Employee
    actor Coordinator
    participant API
    participant Replacement
    participant Deterministic
    participant AI
    participant LLM

    Employee->>API: Create replacement request
    API->>Replacement: validate own active assignment
    Replacement-->>Employee: PENDING

    Coordinator->>API: Approve request
    API->>Replacement: lock + re-check state
    Replacement-->>Coordinator: OPEN

    Coordinator->>API: Get candidates
    API->>Deterministic: hard constraints + score
    Deterministic-->>Coordinator: valid ranked candidates

    Coordinator->>API: AI recommendation
    API->>AI: valid deterministic candidates only
    AI->>LLM: structured work context
    alt valid AI output
        LLM-->>AI: reranked IDs + explanation
        AI-->>Coordinator: AI_ASSISTED
    else disabled/error/invalid
        AI-->>Coordinator: DETERMINISTIC_FALLBACK
    end

    Coordinator->>API: Invite selected candidate
    Employee->>API: Accept invitation
    API->>Replacement: lock + eligibility + overlap + capacity
    Replacement-->>Employee: replacement assignment
```

## 5. QR / OTP attendance sequence

```mermaid
sequenceDiagram
    actor Manager
    actor Employee
    participant QR as QrAttendanceService
    participant Attendance as AttendanceService
    participant DB

    Manager->>QR: Create CHECK_IN session
    QR->>DB: store token hash + OTP hash + optional GPS policy
    QR-->>Manager: raw QR token + OTP once

    Employee->>QR: check-in(QR or OTP, optional GPS)
    QR->>QR: validate session/action/window/GPS/ownership
    QR->>Attendance: selfCheckIn
    Attendance->>DB: create/update DRAFT attendance
    QR->>DB: immutable check event
    QR-->>Employee: DRAFT result

    Manager->>Attendance: CONFIRM
    Attendance->>Attendance: payroll snapshot + result
    Attendance->>DB: CONFIRMED immutable attendance
    Attendance->>DB: reputation update
```

## 6. Payroll activity

```mermaid
flowchart TD
    A[Attendance DRAFT] --> B{Manager confirms?}
    B -- No --> A
    B -- Yes --> C{ABSENT?}
    C -- Yes --> D[payable = 0]
    C -- No --> E[Snapshot base shift pay]
    E --> F[+ LEADER allowance]
    F --> G[- late deduction]
    G --> H[- early-leave deduction]
    H --> I[+ overtime pay]
    I --> J[max 0 and HALF_UP]
    D --> K[Persist CONFIRMED snapshots]
    J --> K
    K --> L[Attendance immutable]
    K --> M[Reports aggregate snapshots]
```

## 7. End-to-end demo activity

```mermaid
flowchart TD
    A[Dashboard detects understaffed shift]
    --> B[Employee requests replacement]
    --> C[Coordinator approves]
    --> D[Deterministic candidate filtering]
    --> E[Optional AI rerank/explanation]
    --> F[Coordinator sends invitation]
    --> G[Candidate accepts]
    --> H[Area/Table placement]
    --> I[QR/OTP check-in]
    --> J[QR/OTP check-out]
    --> K[Manager confirms attendance]
    --> L[Reputation + payroll snapshot]
    --> M[Dashboard/report updated]
```
