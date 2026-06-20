# CP3566 Term Project — Starter (Spring Boot + Spring Data JPA + H2)

A **barely-working** starting point for the **Fraud Case-Management** project. It is a Spring Boot
application — the same stack as the in-class `jpa-h2` sample. On first start it does exactly three
things, and **only** three:

1. **Connects** to the database (the H2 datasource in `application.properties`).
2. **Declares + creates** the 8 tables (Hibernate builds them from the `@Entity` classes).
3. **Loads** the data into `accounts` + `transactions`: the class dataset **`fraud-data.xlsx`**
   (~1,000,000 transactions) ships in this folder and is **parsed and loaded automatically**. (If
   that file is ever removed, the app falls back to generating ~50,000 rows so it still runs.) Either
   way the data is ordinary traffic plus a small, *known* set of **planted fraud** so every rule
   (R1–R4) has hits.

That is the whole starter. **It does not run any rule, raise any alert, or open any case** — the
`alerts`, `cases`, and `audit_log` tables are created but left **empty**. Turning the planted fraud
into the **16 cases** is *your* job. That is the rule engine + case workflow you build on top of
this data layer (see `PROJECT_BRIEF.html` §4.3–§4.8).

* Spring Boot 3.5.14 · Java 21 · Maven (the `./mvnw` wrapper — nothing to install)
* Dependencies: **Spring Web** · **Spring Data JPA** · **H2 Database** (runtime) · **PostgreSQL** (runtime) · **Apache POI** (reads the Excel)
* Database: **file-backed H2** (`jdbc:h2:file:./data`) — the data survives a restart.

## Run it (no database, no Maven to install)

**Before you start (Windows / macOS / Linux):** you need **JDK 21**. If IntelliJ says the project
SDK is missing or wrong, set it via *File ▸ Project Structure ▸ Project ▸ SDK ▸ Add SDK ▸ Download
JDK ▸ version 21*. The **first run needs internet** (IntelliJ downloads the libraries once, then it
works offline). IntelliJ **Community or Ultimate** both work — you're only running a `main` method.

Open the **`starter/`** folder (the one that contains `pom.xml`) in IntelliJ IDEA, let it import the
Maven project, then click the green ▶ next to `main()` in `FraudApplication.java`. Or, from a
terminal in this folder:

```bash
./mvnw spring-boot:run
```

On Windows: `mvnw.cmd spring-boot:run`. The first run downloads Spring Boot once, then prints the
SQL Hibernate runs (it **creates the 8 tables**) and the generation summary below. The app then
keeps running (Tomcat) — press `Ctrl-C` to stop.

Expected output (timings vary by machine):

```
Resources: JVM max heap = 512 MB  |  app.transaction-count = 50,000 (kept light for laptops).

Loading your dataset from .../starter/fraud-data.xlsx ...

================  DATASET LOADED FROM EXCEL  ================
  transactions: 1,000,040   loaded in ~20s

Through the JPA layer: accountRepository.count() = 5,000, transactionRepository.count() = 1,000,040
alerts = 0, cases = 0  ->  build the rule engine + case workflow to open the 16 cases (see PROJECT_BRIEF.html).
```

## Your dataset — `fraud-data.xlsx`

The class dataset, **`fraud-data.xlsx`** (~1,000,000 transactions, ~35 MB), **ships in this folder**
and loads automatically on the first run — every student gets the same data. The parser is
`seed/ExcelDataLoader.java` (**given — you don't edit it**); it **streams** the file, so even a
million rows load with low memory.

For reference, the Excel has **one row per transaction**, with a header row naming these columns:

| account_no | holder_name | amount | currency | counterparty | country | occurred_at |
|---|---|---|---|---|---|---|
| ACCT-001000 | Alice Tremblay | 45.20 | CAD | Maple Grocers | CA | 2025-09-01 08:14 |
| ACCT-001000 | Alice Tremblay | 15000.00 | USD | Offshore Holdings | KY | 2025-09-06 22:01 |

`occurred_at` can be an Excel date cell or text like `2025-09-01 08:14`.

To reload after the data changes: stop the app, delete `data.mv.db`, and run again (the loader only
runs when the database is empty).

## The planted fraud (what your rule engine must find)

The ~1,000,000 transactions are ordinary ($5–$2,000, everyday counterparties, spread out in time)
so none trips a rule by accident. Mixed in is a fixed, deterministic set of fraud:

| Rule | Pattern planted | Hits | Threshold (in the `rules` table) |
|------|-----------------|------|----------------------------------|
| **R1** Large amount | 5 over-limit transfers | 5 | amount ≥ $10,000 |
| **R2** Velocity | 3 accounts, 6 transfers in 25 min each | 3 | ≥ 5 within 60 min |
| **R3** Watchlist | 5 transfers to watchlisted names | 5 | counterparty on watchlist |
| **R4** Structuring *(bonus)* | 3 accounts, 4 transfers just under $10,000 | 3 | ≥ 3 of $9,000–$9,999 within 24h |

When you build the rule engine correctly it should open **exactly 16 cases**, all `NEW`, regardless
of dataset size — a clean fixture you can test against. The data is deterministic (fixed seed `42`).

## See the rows in the database

With the app running, open **http://localhost:8080/h2-console** and connect:

| Field | Value |
|-------|-------|
| JDBC URL | `jdbc:h2:file:./data`  ← change it from the default `jdbc:h2:~/test` |
| User Name | `SA` |
| Password | *(leave empty)* |

Click **Connect**, then run `SELECT * FROM TRANSACTIONS LIMIT 20;` or
`SELECT COUNT(*) FROM ACCOUNTS;`. The `ALERTS` and `CASES` tables exist but are empty until you
build the rule engine.

## The files you were given

```
pom.xml                                     four dependencies (Web, Data JPA, H2, PostgreSQL)
mvnw, mvnw.cmd, .mvn/                         the Maven wrapper — no install needed
docker-compose.yml                          a PostgreSQL server, for the "going further" step
src/main/java/com/example/fraud/
    FraudApplication.java                    the launcher (@SpringBootApplication)
    model/      Account, User, Watchlist,    the 8 @Entity classes — these declare the schema
                Rule, Transaction, Alert,    (Hibernate creates the tables from them)
                Case, AuditLog
    repo/       8 × ...Repository            empty JpaRepository interfaces — CRUD for free
    seed/DataSeeder.java                     connects, creates the tables, loads the data
                                             (Excel if present, else generated — you don't edit it)
    seed/ExcelDataLoader.java                streams + loads fraud-data.xlsx (given — you don't edit it)
fraud-data.xlsx                              the class dataset (~1,000,000 rows, ~35 MB) — loaded automatically
src/main/resources/
    application.properties                   file-backed H2 + show-sql + the H2 console
    application-postgres.properties          the "postgres" profile (config, not code)
```

## Your job — build the rest (this is what's graded)

The **data layer is done**: the `@Entity` classes and `JpaRepository` interfaces are the only code
that touches the database, and Spring Data writes their SQL. You add the layers above them (build
in the order in `PROJECT_BRIEF.html` §3, explained in `CP3566_Concepts_Explained.html`):

- **Rule engine** (`@Service`) — scan the transactions and **open a `NEW` case for every hit** of
  R1–R3 (R4 = bonus). Read the thresholds from the `rules` table; do not hard-code them.
- **Case workflow** (`@Service`) — the state machine NEW → REVIEWING → ESCALATED →
  CLOSED_FALSE / CLOSED_FRAUD. Reject illegal moves (409) and wrong roles (403).
- **REST endpoints** (`@RestController`) — the exact contract in `PROJECT_BRIEF.html` §4.5,
  returning JSON and the right status codes.
- **Security** — login (username + password) first (401 if not, or bad password), then role
  (403 if not allowed): `ANALYST`, `INVESTIGATOR`, `ADMIN`. The users are seeded with
  **BCrypt-hashed** passwords; verify the password with a `PasswordEncoder` (`.matches(...)`).
  **Demo logins:** `analyst1`/`analyst123` · `investig1`/`investig123` · `admin1`/`admin123`.
- **A screen + the audit log** — list and act on cases; every action writes one `audit_log` line.

## Resources — it won't hang your laptop

Even with the full million-row dataset, the load is kept safe:

- **Loaded once, then cached** — the first run loads `fraud-data.xlsx` (~20 s); after that the app
  sees the data and skips the load, so restarts are fast.
- **Streamed, not slurped** — the loader reads the Excel one row at a time and inserts in batches,
  so memory stays low and flat no matter the file size (a million rows load fine under the cap).
- **Capped heap** — `./mvnw spring-boot:run` runs with `-Xmx512m` (set in `pom.xml`); the first line
  the app prints shows the heap it's using.

To reload the data from scratch, **stop the app and delete `data.mv.db`**, then run again. (If you
remove `fraud-data.xlsx` entirely, the app generates a small ~50,000-row sample instead.)

## Going further — PostgreSQL (config, not a rewrite)

The same `@Entity` classes and repositories work against a real server database:

```bash
docker compose up -d
./mvnw spring-boot:run -Dspring-boot.run.profiles=postgres
```

None of the Java changes — only the configuration.
