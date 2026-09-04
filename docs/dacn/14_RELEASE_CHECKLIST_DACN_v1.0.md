# DACN v1.0 — Final Release Checklist

Không tạo tag release cho tới khi toàn bộ mục bắt buộc PASS.

## A. Repository hygiene

```powershell
cd "D:\DoAn\wedding-staff-management"

git status --short
git diff --check
git branch --show-current
git rev-parse --short HEAD
```

Trước Commit 14 expected:

```text
branch = develop/dacn
HEAD   = c815a21
status = clean
```

Sau khi apply patch, chỉ các file Commit 14 được phép thay đổi.

Kiểm tra không track build/secrets:

```powershell
git ls-files |
    Select-String -Pattern '(^|/)(node_modules|target|dist)/|(^|/)\.env($|\.)'
```

Expected: không có output.

## B. Database release verification

Các migration là manual one-time scripts.

- Database mới: apply V002 -> V012 theo đúng thứ tự, V004_1 ngay sau V004.
- Database hiện tại đã migrate: **không chạy lại** migration.
- Không restore DB chỉ để chạy release checklist.

Final schema verification nên chạy backend với:

```text
JPA_DDL_AUTO=validate
```

Nếu `validate` fail, xử lý schema mismatch trước release; không đổi lại `update` để che lỗi.

## C. Backend final regression

```powershell
cd "D:\DoAn\wedding-staff-management\backend"
mvn clean test
```

Gate:

```text
Tests run: 94
Failures: 0
Errors: 0
BUILD SUCCESS
```

Nếu số tests tăng sau Commit 14 thì lấy output Maven thực tế làm chuẩn, nhưng failures/errors phải bằng 0.

## D. Frontend final build

```powershell
cd "D:\DoAn\wedding-staff-management\frontend"
npm run build
```

Gate:

```text
vite build PASS
```

Không commit `dist/`.

## E. Runtime smoke

Tối thiểu:

1. Admin login.
2. Dashboard manager API/UI.
3. Replacement request -> review -> candidate list.
4. Deterministic recommendation.
5. AI recommendation hoặc deterministic fallback.
6. QR/OTP attendance DRAFT.
7. Manager confirm attendance.
8. Payroll report.
9. Employee personal pages.
10. Area/Table placement.

Security:

```text
No JWT              -> 401
Invalid JWT         -> 401
ADMIN manager API   -> 200
ADMIN employee-only -> 403
One-sided GPS       -> 400
localhost:5173 CORS -> 200
evil.example CORS   -> 403
```

## F. Documentation review

- README không còn mô tả repo là DACS-only.
- Migration policy ghi rõ manual one-time.
- AI ghi rõ coordinator quyết định cuối.
- Không gọi deterministic scoring là AI.
- Không gọi 94 backend tests là browser E2E.
- Known limitations được ghi rõ.
- Diagram được mô tả là conceptual khi không phải physical schema.
- Không có password/token/API key thật trong docs/Postman.

## G. Stage Commit 14

Stage đúng release files:

```powershell
cd "D:\DoAn\wedding-staff-management"

git add `
"README.md" `
"docs/dacn/02_ROADMAP_DACN_v1.0.md" `
"docs/dacn/14_DIAGRAMS_DACN_v1.0.md" `
"docs/dacn/14_TEST_REPORT_DACN_v1.0.md" `
"docs/dacn/14_RELEASE_DEMO_DACN_v1.0.md" `
"docs/dacn/14_RELEASE_CHECKLIST_DACN_v1.0.md" `
"docs/postman/Wedding_Staff_Management_DACN_v1.postman_collection.json"

git diff --cached --check
git diff --cached --stat
git status --short
```

Expected: 7 staged files, không còn `??`.

## H. Commit

```powershell
git commit -m "release(dacn): finalize documentation demo and release"
git status --short
git rev-parse --short HEAD
```

Working tree phải sạch.

## I. Tag

Kiểm tra tag chưa tồn tại:

```powershell
git tag --list "dacn-v1.0.0"
git tag --list "checkpoint-after-dacn-release"
```

Nếu không có output:

```powershell
git tag dacn-v1.0.0
git tag checkpoint-after-dacn-release
```

Xác minh:

```powershell
git log -1 --oneline --decorate
git tag --points-at HEAD
git status --short
```

Expected ở HEAD:

```text
dacn-v1.0.0
checkpoint-after-dacn-release
```

## J. Release evidence cần lưu

Ghi lại:

- final commit hash;
- `mvn clean test` summary;
- `npm run build` summary;
- runtime smoke result;
- screenshots demo quan trọng;
- release tags.

Không cần commit DB dump hoặc secret để làm bằng chứng.
