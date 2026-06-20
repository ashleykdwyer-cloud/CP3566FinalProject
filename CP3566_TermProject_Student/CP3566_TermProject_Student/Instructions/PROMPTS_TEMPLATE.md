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

### Entry 1 —
- **When:**
- **Working on:**
- **Prompt:**
- **Copilot suggested:**
- **What I did:**

### Entry 2 —
- **When:**
- **Working on:**
- **Prompt:**
- **Copilot suggested:**
- **What I did:**

### Entry 3 —
- **When:**
- **Working on:**
- **Prompt:**
- **Copilot suggested:**
- **What I did:**

*(add more entries as you go)*
