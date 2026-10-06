# folio — Personal Finance Manager

A college-project personal finance application built primarily in Java with Spring Boot, Spring MVC, Spring Data JPA, Thymeleaf, and MySQL support.

## Run in Replit

The included `Finance Management App` workflow starts the application on port 8080. The default `demo` profile uses a local H2 database in MySQL compatibility mode so the app runs immediately without external database setup. Demo data is created automatically on first launch.

## Demo account

- Email: `viewer@demo.com`
- Password: `viewer123`

You can also create a separate account from the sign-up page.

## Use MySQL

1. Create a MySQL database named `finance_manager`.
2. Set `SPRING_PROFILES_ACTIVE=mysql`.
3. Set these environment variables in Replit Secrets:
   - `MYSQL_URL` — JDBC URL (for example `jdbc:mysql://host:3306/finance_manager?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC`)
   - `MYSQL_USER` — database username
   - `MYSQL_PASSWORD` — database password
4. Restart the `Finance Management App` workflow.

JPA creates and updates the tables on startup for this project (`spring.jpa.hibernate.ddl-auto=update`). For a production application, replace this with versioned migrations and stronger operational security.

## Run locally

Install Java 17+ and Maven 3.8+, then run from this directory:

```bash
mvn spring-boot:run
```

For the MySQL profile:

```bash
SPRING_PROFILES_ACTIVE=mysql MYSQL_URL='jdbc:mysql://localhost:3306/finance_manager' MYSQL_USER='root' MYSQL_PASSWORD='your-password' mvn spring-boot:run
```

## Features

- Session-based sign in, sign up, and sign out; BCrypt password hashing.
- Personal dashboard with lifetime and current-month totals, savings, remaining budget, and recent transactions.
- Transaction create, edit, delete, search, and type filtering.
- Monthly/category budgets with Java-calculated spending, remaining amount, usage percentage, and status.
- Date-range reports with income, expenses, savings, and category-level expense totals.
- Editable profile, currency/notification preferences, and password changes.
- Per-user ownership checks on transaction and budget reads/updates/deletes.
- Responsive Thymeleaf interface with minimal JavaScript.

## Project structure

- `src/main/java/edu/college/finance/entity` — JPA entities and transaction type
- `src/main/java/edu/college/finance/repository` — Spring Data repositories
- `src/main/java/edu/college/finance/service` — financial calculations, CRUD, and demo data
- `src/main/java/edu/college/finance/web` — MVC controllers, auth guard, and shared model
- `src/main/resources/templates` — Thymeleaf pages
- `src/main/resources/static/css` — interface styling
- `src/main/resources/application-mysql.properties` — MySQL profile configuration
