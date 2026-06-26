# PROMPTS.md — AI prompt log (mandatory)

**CP3566 Term Project · Fraud Case-Management**

> Rename this file to `PROMPTS.md` and commit it to your GitHub repository.

## Why this file exists

**GitHub Copilot (and Copilot Chat) is the only AI tool you may use on this project.**
This file is your honest record of the significant prompts you gave it.

- It is **mandatory**: a missing or fabricated log makes the **20 work marks zero** (see the brief, §6 and §7).
- Using **any other AI tool** — ChatGPT, Claude, Gemini, Cursor, etc. — is an **automatic zero on the whole project**.
- Together with your **5–10 commits**, this log is how we see the work is yours and grew over time.

## What to log

Log every **significant** prompt — the ones that produced code you kept, or that shaped a design decision.
You do **not** need to log tiny autocompletes (a variable name, an `import`). Aim for honesty, not volume.

For each one, note what you asked, what Copilot gave you, and **what you did with it** (kept / changed / rejected) —
because you must be able to **explain every line in your demo**.

## Format — copy one block per prompt

```
### Entry N — <short title>
- When: <date>
- Working on: <which part, e.g. CaseService state machine>
- Prompt: <what you typed to Copilot / Copilot Chat>
- Copilot suggested: <one-line summary of what it produced>
- What I did: kept / changed / rejected — <why, in one line>
```

---

## Example (delete this once you start your own)

### Entry 1 — Velocity rule sliding window
- **When:** 2026-06-03
- **Working on:** `RuleEngineService` — the R2 velocity check
- **Prompt:** "write a method that counts how many transactions for one account fall within a 60-minute window"
- **Copilot suggested:** a nested loop comparing every pair of transactions (O(n²))
- **What I did:** *changed it* — used a two-pointer sliding window instead, because the nested loop was too slow on the seeded data. I understand and can explain both versions.

---

## My log

### Entry 1 — Rule Map
- **When:** 2026-06-20
- **Working on:** RuleEngineService.java
- **Prompt:** For public int scanAndOpenCases, load all rules from ruleRepo.findAll() into a map where the rule code is the key
- **Copilot suggested:**Implemented loading rules into a Map keyed by rule code in `scanAndOpenCases()`:
- Added imports: `HashMap`, `Map`, `Rule`
- Implemented: `Map<String, Rule> rules = new HashMap<>();` with for-each loop to populate map
- Added comments showing how to use the map for rule lookups
- **What I did:** Kept it - I used the map so each rule could be looked up by the code.

### Entry 2 — R1 large amount
- **When:** 2026-06-20
- **Working on:** RuleEngineService.java scanAndOpenCases
- **Prompt:** Next in scanAndOpenCases, add the R1 large amount check. Loop through all transactions and call openCase when the transaction amount is greater than or equal to the R1 threshold.
- **Copilot suggested:** Implemented R1 rule check in `scanAndOpenCases()`:
- Added imports: `Transaction`, `BigDecimal`
- Fetched R1 rule from map
- Looped through all transactions
- Called `openCase()` for amounts >= R1.getThresholdAmount()
- Incremented `opened` counter for each hit
- Returns total count
- **What I did:** Changed it - I changed it to use the threshold from the rules table instead of hard-coded data

### Entry 3 — R3 Watchlist
- **When:** 2026-06-21
- **Working on:** RuleEngineService.java scanAndOpenCases
- **Prompt:** Next, add the R3 watchlist check. Compare each transaction counterparty to the watchlist names and ignore case.
- **Copilot suggested:** Implemented R3 watchlist check in `scanAndOpenCases()`:
- Added imports: `Watchlist`, `Set`, `HashSet`
- Built a case-insensitive set of watchlist names for O(1) lookup
- Looped through transactions and checked if counterparty (case-insensitive) is in the watchlist set
- Called `openCase()` for each match
- Incremented `opened` counter
- **What I did:** Kept it - the set made the comparison simple and case-insensitive.

### Entry 4 — R2 Velocity
- **When:** 2026-06-21
- **Working on:** RuleEngineService.java scanAndOpenCases
- **Prompt:** Add the R2 velocity check. Group transactions by accountId and open one case if enough transactions happen within the rule time window.
- **Copilot suggested:** Implemented R2 velocity check in `scanAndOpenCases()`:
- Added imports: `List`, `ArrayList`
- Grouped transactions by accountId
- For each account, sorted transactions by time ascending
- Used sliding window approach: for each transaction start time, counted how many fall within [start, start + windowMinutes]
- Called `openCase()` once per flagged account (flagged = true ensures one case per account)
- Incremented `opened` counter
Algorithm handles:
- Null checks for transactions, account IDs, timestamps
- Chronological sorting
- Finding first window exceeding threshold
- Opening exactly one case per account
- **What I did:** Kept it - 


### Entry 5 — R4 Structuring
- **When:** 2026-06-21
- **Working on:** RuleEngineService.java scanAndOpenCases
- **Prompt:** Add the R4 structuring bonus check. Use transactions between 9000 and 9999 and open one case if enough happen within the rule time window.
- **Copilot suggested:** Implemented R4 structuring bonus check in `scanAndOpenCases()`:
- Filtered transactions with amounts in range [9000, 9999] using BigDecimal comparisons
- Used same sliding window logic as R2 but only for structuring amounts
- Groups by accountId
- Sorts by time
- Finds first window with >= minCount structuring transfers
- Opens one case per flagged account
- Increments `opened` counter

**Complete rule engine now includes:**
- ✅ R1: Large amounts
- ✅ R2: Velocity
- ✅ R3: Watchlist
- ✅ R4: Structuring bonus
- **What I did:** Changed it - I used the R4 threshold from the rules table and treated the range as just under that threshold.

### Entry 6 — Count opened cases
- **When:** 2026-06-23
- **Working on:** RuleEngineService.java scanAndOpenCases
- **Prompt:** Finish scanAndOpenCases so it counts every case opened and returns the total.
- **Copilot suggested:** scanAndOpenCases()` was already counting all cases and returning the total via the `opened` variable. The missing piece was implementing `openCase()` to actually persist the cases to the database.

Implemented `openCase()` method to complete the workflow:
1. Save Alert with transaction ID, rule code, detail, and timestamp
2. Save Case linked to the alert (status="NEW", unassigned)
3. Save AuditLog entry recording the system action

This ensures the counted cases are actually persisted and the full workflow completes.
- **What I did:** Kept it - I returned the total so the scan endpoint could show how many cases were created.

### Entry 7 — Open case persistence
- **When:** 2026-06-23
- **Working on:** RuleEngineService.java openCase
- **Prompt:** Finish the openCase() method so it saves an Alert, then a Case, then an AuditLog.
- **Copilot suggested:** Completed `openCase()` method:

```java
private void openCase(long transactionId, String ruleCode, String detail, LocalDateTime when) {
    // Save Alert
    Alert alert = new Alert(transactionId, ruleCode, detail, when);
    Alert savedAlert = alertRepo.save(alert);

    // Save Case linked to the Alert, status = "NEW", unassigned (null)
    Case c = new Case(savedAlert.getId(), "NEW", null, when);
    Case savedCase = caseRepo.save(c);

    // Record audit log: system opened a case for this rule
    AuditLog log = new AuditLog("system", "OPEN_CASE", "case", savedCase.getId(),
            "Rule " + ruleCode + ": " + detail, when);
    auditLogRepo.save(log);
}
```

Added imports: `Alert`, `Case`, `AuditLog`

**Method workflow:**
1. Create and save Alert (transactionId, ruleCode, detail, when) → get alertId
2. Create and save Case (alertId, "NEW", null, when)
3. Create and save AuditLog ("system", "OPEN_CASE", "case", caseId, formatted detail, when)

No compile errors; only style warnings (expected).
- **What I did:** Kept it - this match the requirement that each rule hit creates an alert, case and audit log row.