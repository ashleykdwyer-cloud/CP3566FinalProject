package com.example.fraud;

/**
 * ============================================================================
 *   CP3566 TERM PROJECT  —  WHAT YOU NEED TO BUILD   (read this first)
 * ============================================================================
 *
 * GIVEN to you (you normally do NOT edit these):
 *   - com.example.fraud.model   the 8 @Entity classes  -> Hibernate creates the tables
 *   - com.example.fraud.repo    the 8 JpaRepository interfaces (CRUD for free)
 *   - com.example.fraud.seed.DataSeeder   connects, creates the tables, and loads the data into
 *                                         accounts + transactions (your instructor's fraud-data.xlsx
 *                                         if present, else a generated sample with planted fraud)
 *   - com.example.fraud.seed.ExcelDataLoader  parses the supplied fraud-data.xlsx (given)
 *
 * So when you run the starter, the database is populated BUT the alerts, cases and
 * audit_log tables are EMPTY. Turning the planted fraud into the 16 cases — and
 * everything around it — is YOURS to build. That is what is graded.
 *
 * BUILD IN THIS ORDER (each step works before the next). Full spec: PROJECT_BRIEF.html.
 *
 *   1. RULE ENGINE      (§4.3)  ->  com.example.fraud.service.RuleEngineService   [signatures given — fill the bodies]
 *        Scan the transactions; open a NEW case for every hit of R1, R2, R3
 *        (R4 = bonus). Read thresholds from the `rules` table — do NOT hard-code them.
 *        Correct on the seeded data = exactly 16 cases, all status NEW.
 *
 *   2. CASE WORKFLOW    (§4.4)  ->  com.example.fraud.service.CaseService          [signatures given — fill the bodies]
 *        State machine NEW -> REVIEWING -> ESCALATED -> CLOSED_FALSE / CLOSED_FRAUD.
 *        Illegal move -> 409.  Legal move by the wrong role -> 403.
 *
 *   3. REST ENDPOINTS   (§4.5)  ->  com.example.fraud.web (add @RestController classes)
 *        Build the exact contract in the brief; return JSON + the right status codes.
 *
 *   4. SECURITY         (§4.6)  ->  in your controllers / a filter
 *        Login first (401 if not, or bad password), then role (403 if not allowed).
 *        Users are seeded with BCrypt-HASHED passwords — verify with a PasswordEncoder.
 *        Demo logins: analyst1/analyst123, investig1/investig123, admin1/admin123.
 *
 *   5. AUDIT LOG        (§4.8)  ->  write one audit_log row per action (who / what / when).
 *
 *   6. A SCREEN         (§4.7)  ->  a simple page (Thymeleaf, or static HTML + JS that
 *                                   calls your REST API) to list and act on cases.
 *
 * Start with RuleEngineService. This class is documentation only — delete it whenever
 * you like.
 */
public final class StudentTasks {
    private StudentTasks() { }
}
