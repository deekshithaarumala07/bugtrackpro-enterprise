# Security audit summary

## Fixes in this release

- Removed the hardcoded datasource password fallback; database credentials are environment-configured.
- Removed the fixed development JWT secret. Development uses a random temporary process key; production mode requires a 32-byte-or-longer configured secret.
- JWT requests re-read current account status and role, so disabling an account or changing its role takes effect without waiting for the token's original role claim to expire.
- `JwtAuthFilter` now reads the account through `UserRepository`, breaking the startup bean cycle through `UserService` and `SecurityConfig`; a filter regression test covers active-account role loading.
- CORS origins come from `CORS_ORIGINS`; credentialed cross-origin cookies are disabled because this app uses bearer tokens.
- Project lists and reads check manager/member access. Added project membership, and constrained issue, sprint, dashboard, analytics, overdue, duplicate, and CSV responses to accessible projects/issues.
- Developer assignment checks active account, role, and project membership. Issue status transitions and role permissions are checked on the server.
- Admin user listing returns summary DTOs; developer choice lists exclude inactive users. The last active admin cannot be demoted/disabled, and users cannot disable/demote themselves.
- Attachment downloads require issue access; storage uses random names outside the web root, path containment checks, an allow-list, size limits, and basic signature/text checks.
- Comments/history require issue access; edit/delete is limited to author or project manager/admin. Notifications are delivered only to issue reporter/assignee and read operations are owner-scoped.
- Editing or deleting a comment requires access to its parent issue before author/manager permissions are considered.
- CSV export prefixes spreadsheet formula-like values before quoting and escaping cells.
- Error responses do not expose unexpected exception details. Duplicate registration returns conflict.

## Remaining risks and work before production

- Access tokens remain in browser local storage and last 24 hours. There is no refresh-token rotation, server-side revocation, or logout invalidation; XSS could expose a token.
- Login throttling now expires counters and bounds tracked identities (10 attempts per address/account per minute by default); it remains process-local, does not coordinate across API replicas, and its source address should be reviewed behind a production proxy.
- `spring.jpa.hibernate.ddl-auto=update` is still active. A reviewed Flyway/Liquibase migration and upgrade rehearsal are required before production.
- Authorization policy is materially improved but lacks a full integration suite for every endpoint and role. Review/export, inactive-user, existing-member migration, and all IDOR cases need dedicated API tests.
- Attachment validation is allow-list and signature based; it does not use antivirus or sandbox scanning. Enforce HTTPS and appropriate reverse-proxy limits in production.
- No dependency vulnerability scan or penetration test was performed.
- Some UI actions still have incomplete error feedback; sprint editing/replanning, project archive/delete/update, work logs, password reset, refresh flow, server-side pagination, and OpenAPI are not implemented.
- Full web startup after the bean-cycle fix, DB schema initialization, and file persistence were not verified here because MySQL and Docker were unavailable. The previous user-provided startup log did confirm MySQL/JPA connection before the bean cycle was raised.
