# Company acceptance report

This report distinguishes code inspection and automated unit/build results from live system verification. The database and Docker runtime were not available in the build environment.

| Acceptance area | Result | Evidence / reason |
|---|---|---|
| Source audit and dependency/config review | PASS (reviewed) | Existing Spring Boot 3.5.6 / Java 21 / React 18 / Vite stack retained |
| Backend clean package and unit tests | PASS | `mvn -q clean package`; Surefire reports 10 unit tests, 0 failures/errors |
| Frontend dependency install and production build | PASS | `npm ci --ignore-scripts`; final `npm run build` (Vite 6.0.5, 1,647 modules) |
| Kanban presentation regression from user screenshot | PASS (source/build only) | Added lane/card styling, responsive horizontal board layout, empty/loading states, and role/workflow-filtered keyboard status controls; browser visual recheck remains outstanding |
| Release ZIP integrity and clean-copy builds | PASS | `unzip -tq`; extracted archive to a separate temporary directory; clean Maven package, `npm ci --ignore-scripts`, and Vite build all succeeded |
| Project/issue access policy and workflow unit coverage | PASS (unit scope only) | Tests cover unrelated-user denial, member/manager access, role-based status actions, valid/invalid status transitions |
| JWT signing, verification, production secret rule | PASS (unit scope only) | Tests cover configured secret, development generated secret, production missing/short secret rejection |
| JWT filter account lookup and role authority | PASS (unit scope only) | Regression test confirms filter loads the active account from `UserRepository`; it no longer depends on `UserService`, removing the `JwtAuthFilter → UserService → SecurityConfig → JwtAuthFilter` cycle from the pasted startup log |
| Docker Compose validation/startup | NOT VERIFIED | `docker compose config` could not run because the Docker executable is unavailable |
| Packaged backend startup after bean-cycle fix | NOT VERIFIED | The supplied log showed MySQL/JPA initialized and then exposed the circular dependency; the release filter now depends on `UserRepository`. This environment has no reachable MySQL instance to repeat full startup after the change. |
| MySQL clean schema initialization and existing DB upgrade | NOT VERIFIED | MySQL server unavailable; Hibernate `ddl-auto=update` remains in use |
| HTTP health, register/login, role/API authorization | NOT VERIFIED end-to-end | Requires a running database and application; the JWT secret startup error and reported filter/service bean cycle are addressed in source but HTTP behavior still needs a live rerun |
| Browser workflows / attachment upload/download | NOT VERIFIED end-to-end | No running app/database/browser automation suite |
| Frontend component/E2E tests | NOT VERIFIED | No frontend test framework or tests in the supplied project |
| Dependency vulnerability scan | NOT VERIFIED | No dependency scanner report was generated |
| Production suitability / purchase approval | FAIL / REQUIRES REMEDIATION | Migration, refresh/revocation, full negative authorization suite, and live acceptance remain outstanding |

## Railway/GitHub preparation check — 2026-10-11

| Check | Result | Evidence / limitation |
|---|---|---|
| Backend unit suite after deployment-readiness changes | PASS | `mvn -q -DargLine=-javaagent:<local Byte Buddy agent> clean test`; 10 tests, 0 failures/errors. This host's default Mockito self-attachment failed once, so the existing agent was supplied explicitly. |
| Backend deployable package | PASS | `mvn -q -DskipTests package`; produced the jar used by the existing backend Dockerfile. |
| Frontend production build after Railway API configuration | PASS | `npm run build`; Vite 6.0.5 build completed. |
| Login counter, CSV formula, comment access, Railway service config | PASS (source review) | Bounded/expiring sign-in attempts, CSV formula neutralization, parent-issue access checks, and a Railway deployment guide were added. No new endpoint-level integration tests were added for these changes. |
| Public repository contents scan | PASS (local scan) | No real `.env`, private-key file, or common token signature found. Ignore rules exclude local env files, build output, dependencies, uploads, and Java targets while keeping `.env.example` files. |
| Docker image and live Railway deployment | NOT VERIFIED | Docker and Railway CLI are unavailable in this environment; a live database, Railway account configuration, and app smoke test are still needed. |

## Manual verification still required

1. Configure a disposable MySQL database and unique local secrets using `.env.example`.
2. Start with `docker compose up --build` (or use `bash backend/run-local.sh` after installing local MySQL).
3. Call `GET http://localhost:8080/api/health`, bootstrap an admin, register non-admin accounts, and verify login.
4. Exercise project membership, issue create/assign/status, comments, attachments, notifications, and CSV with separate roles.
5. Attempt cross-project reads/downloads and forbidden issue transitions and confirm 403/404 behavior.
6. Restart services and verify existing database rows and attachment files persist.
