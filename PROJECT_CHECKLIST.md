# Release checklist

This checklist records current code and verification status. A feature existing in the source does not mean its entire workflow has passed acceptance testing.

## Implemented in this update

- [x] Temporary random JWT key for local development; production secret validation.
- [x] Environment-driven database configuration with no default database password.
- [x] Environment-driven CORS configuration; no credentialed CORS cookies.
- [x] Current account active/role check on each bearer-token request.
- [x] Scoped project reads and project membership APIs.
- [x] Scoped issue, sprint, dashboard, analytics, overdue, duplicate, and CSV access.
- [x] Role-checked issue assignment and server-side workflow transitions.
- [x] Comments, comment history, private attachment upload/download APIs.
- [x] Safe explicit admin bootstrap when no active ADMIN exists.
- [x] Unit tests for JWT policy, access policy, and status workflow.
- [x] Manual API, security, and acceptance documentation.

## Present in the supplied application, with incomplete scope or verification

- [~] React dashboards, bug list/details, Kanban, reports, sprint view, user management, security view.
- [~] Issue CRUD and search; list operations are not paginated server-side.
- [~] Notifications and optional assignment email.
- [~] Admin account role/status changes; no password reset or invitation flow.
- [~] Project create/read/member management; project edit/archive/delete are absent.
- [~] Sprint create/start-complete and issue assignment; editing, capacity, and burndown are absent.
- [~] Attachments support allow-listed types and 10 MB cap; malware scanning is absent.
- [~] AI triage and duplicates are deterministic local heuristics, not external AI.

## Not implemented or not verified

- [ ] Refresh token rotation/revocation or server-side logout invalidation.
- [ ] Versioned Flyway/Liquibase production migrations.
- [ ] Complete integration/E2E/browser suite and every role/endpoint negative case.
- [ ] Full live MySQL schema/startup/data-restart verification.
- [ ] OpenAPI/Swagger generation.
- [ ] Work logs, milestones, password reset, and broad audit coverage.
- [ ] Docker startup and production deployment verification.
- [ ] Dependency vulnerability scan and penetration test.
- [ ] Company purchase acceptance.
