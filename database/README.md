# Database

MySQL 8.4 database name: `bugtracker`.

The application uses Hibernate `ddl-auto=update` for local development. On startup it updates entity tables and creates a bootstrap administrator only when explicitly configured and no active administrator exists. It does not seed sample users, projects, or bugs.

Connection defaults:
- Host: `localhost`
- Port: `3306`
- Database: `bugtracker`
- Username: configured with `DB_USERNAME` (default `bugtracker`)
- Password: configured with `DB_PASSWORD`; there is no default application password.

For production, replace `ddl-auto=update` with versioned migrations (Flyway/Liquibase), set `APP_ENV=production`, and configure a unique 32-byte-or-longer JWT secret.
