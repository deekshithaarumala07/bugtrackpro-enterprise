# Deploy BugTrackPro on Railway

This repository is an isolated monorepo. Deploy the API and web client as separate Railway services and use a Railway MySQL service. Railway supports a separate root directory for each service; this lets the existing Dockerfiles build independently.

## 1. Create the services

Create an empty Railway project and add three services from the project canvas:

| Service name | Source / root directory | Build |
|---|---|---|
| `mysql` | Railway MySQL template | Railway-managed MySQL |
| `backend` | This GitHub repository, root directory `/backend` | Dockerfile at `/backend/Dockerfile` |
| `frontend` | This GitHub repository, root directory `/frontend` | Dockerfile at `/frontend/Dockerfile` |

Set both application services to deploy from the same branch. The MySQL service should remain private; the API connects through Railway's private network.

## 2. Configure the backend

Add these variables to the `backend` service. The database entries are Railway reference variables; set the MySQL service name to `mysql` as shown above.

```text
APP_ENV=production
PORT=8080
DB_URL=jdbc:mysql://${{mysql.MYSQLHOST}}:${{mysql.MYSQLPORT}}/${{mysql.MYSQLDATABASE}}?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC
DB_USERNAME=${{mysql.MYSQLUSER}}
DB_PASSWORD=${{mysql.MYSQLPASSWORD}}
JWT_SECRET=<a unique random value containing at least 32 bytes>
APP_UPLOAD_DIR=/app/uploads
BOOTSTRAP_ADMIN_NAME=<initial administrator's display name>
BOOTSTRAP_ADMIN_EMAIL=<initial administrator's email>
BOOTSTRAP_ADMIN_PASSWORD=<unique password with at least 12 characters>
```

Set the backend health check path to `/api/health`. Attach a Railway volume to this service at `/app/uploads`; attachments are otherwise lost when a container is replaced. After the first successful start creates the administrator, remove the three `BOOTSTRAP_ADMIN_*` variables from Railway and deploy the change. Bootstrap only creates an admin when no active admin exists, but removing the password from service configuration is still good secret hygiene.

Generate a Railway public domain for the backend after it deploys. The browser app calls this public HTTPS API domain.

## 3. Configure the frontend

Generate a public domain for `frontend`, then add this variable:

```text
VITE_API_URL=https://${{backend.RAILWAY_PUBLIC_DOMAIN}}/api
```

`VITE_API_URL` is consumed during the Docker build. The frontend Dockerfile declares it as a build argument, so Railway rebuilds the static app with the backend address when the variable changes.

## 4. Allow the frontend origin

After both domains exist, set this variable on `backend`:

```text
CORS_ORIGINS=https://${{frontend.RAILWAY_PUBLIC_DOMAIN}}
```

Railway reference variables update automatically when the referenced service domain changes. Redeploy both services after adding or changing these values. Open the frontend domain and check that `/api/health` is healthy and that sign-in/API requests succeed.

## 5. First sign-in and data persistence

Use the initial administrator credentials from the one-time bootstrap configuration. Create a project and test issue creation, comments, attachments, and CSV export. Confirm the database service and backend upload volume are attached in the same Railway project/environment.

## Deployment limitations

- The application still uses Hibernate `ddl-auto=update`; it does not include a versioned schema migration/rollback process. Back up the database before upgrades and do not treat this demo deployment as production approved until migrations and live acceptance checks are complete.
- Login throttling uses bounded process-local state. It is suitable for a single demo API instance, but it does not coordinate limits across replicas or survive restarts.
- Uploaded files live on the backend volume. A single attached volume is not a shared multi-region attachment store.
- The repository has unit tests and a production frontend build, but no DB-backed end-to-end test suite. Complete the manual checks in [ACCEPTANCE_REPORT.md](ACCEPTANCE_REPORT.md) before relying on it for real operational data.
