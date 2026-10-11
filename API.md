# BugTrackPro API reference

Base URL: `http://localhost:8080/api`. Except registration, login, and health, routes require `Authorization: Bearer <access-token>`. JSON errors have `{ "error": "...", "timestamp": "..." }` shape. DTOs are returned rather than password-bearing entities.

## Authentication and status

| Method | Route | Access | Purpose |
|---|---|---|---|
| GET | `/health` | Public | Backend process status |
| POST | `/auth/register` | Public | Register a TESTER |
| POST | `/auth/login` | Public | Sign in and receive an access token |

Registration passwords require at least 12 characters, an uppercase letter, and a number. Logout currently clears the client token; there is no server-side token revocation or refresh-token endpoint.

## Projects and membership

| Method | Route | Access | Purpose |
|---|---|---|---|
| GET | `/projects` | Signed in | Projects the user can access |
| GET | `/projects/{id}` | Project access | Project details and member summaries |
| POST | `/projects` | ADMIN, MANAGER | Create a project; managers become its owner |
| POST | `/projects/{id}/members/{userId}` | Project owner, ADMIN | Add an active user |
| DELETE | `/projects/{id}/members/{userId}` | Project owner, ADMIN | Remove a member |

## Issues, discussion, files, and history

| Method | Route | Access | Purpose |
|---|---|---|---|
| GET | `/bugs`, `/bugs/{id}`, `/bugs/mine` | Issue access | Read scoped issues |
| POST | `/bugs` | Project access | Create an issue; reporter comes from token |
| PATCH | `/bugs/{id}` | Issue policy | Update permitted issue metadata |
| PATCH | `/bugs/{id}/status` | Issue policy + workflow | Change issue status |
| PATCH | `/bugs/{id}/assign` | Project owner, ADMIN | Assign to an active project developer |
| GET, POST | `/bugs/{id}/comments` | Issue access | Read/add comments |
| PATCH, DELETE | `/comments/{id}` | Author or project owner, ADMIN | Edit/delete a comment |
| GET | `/bugs/{id}/history` | Issue access | Read change history |
| GET, POST | `/bugs/{id}/attachments` | Issue access | List/upload validated attachments |
| GET | `/bugs/attachments/{id}/download` | Issue access | Download an attachment |

Issue transitions are checked server-side: OPEN → IN_PROGRESS or REOPENED; IN_PROGRESS → OPEN or RESOLVED; RESOLVED → IN_PROGRESS, CLOSED, or REOPENED; CLOSED → REOPENED; REOPENED → IN_PROGRESS or RESOLVED. Developers cannot close issues; testers can reopen issues they reported.

## Other routes

| Route | Access | Purpose |
|---|---|---|
| GET `/users` | ADMIN | User management list |
| GET `/users/developers?projectId={id}` | Project owner, ADMIN | Active developer members eligible for assignment in this project |
| GET `/users/managers` | ADMIN | Active manager choices for project ownership |
| GET `/users/project-candidates?projectId={id}` | Project owner, ADMIN | Active non-admin users eligible for project membership |
| PATCH `/users/{id}/role`, `/users/{id}/toggle` | ADMIN | Change role or account status; last active admin/self-deactivation protections apply |
| GET `/dashboard/summary` | Signed in | Counts scoped to accessible issues/projects (global for ADMIN) |
| GET `/features/sprints` | Signed in | Sprints for accessible projects |
| POST `/features/sprints`, PATCH `/features/sprints/{id}/toggle` | Project owner, ADMIN | Create/start/complete a sprint |
| PATCH `/features/bugs/{id}/sprint` | Project owner, ADMIN | Add/remove issue from a sprint in the same project |
| GET `/features/notifications` | Signed in | Current user's notifications and unread count |
| PATCH `/features/notifications/{id}/read`, `/features/notifications/read-all` | Signed in | Mark current user's notifications read |
| GET `/features/analytics`, `/features/overdue`, `/features/reports/bugs.csv` | Signed in | Scoped analytics, overdue issues, CSV |
| GET `/features/ai/suggest` | Signed in | Deterministic local triage suggestions; not an external AI model |
| GET `/features/ai/duplicates` | Signed in | Similar issues within accessible project data |
| GET `/features/security` | ADMIN | Security audit summary |

There is no generated OpenAPI/Swagger UI in this release. Pagination and server-side filter parameters are not implemented for the main list endpoints.
