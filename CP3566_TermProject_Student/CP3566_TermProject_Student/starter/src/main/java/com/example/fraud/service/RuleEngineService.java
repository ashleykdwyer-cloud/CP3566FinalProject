package com.example.fraud.service;

import com.example.fraud.repo.AlertRepository;
import com.example.fraud.repo.AuditLogRepository;
import com.example.fraud.repo.CaseRepository;
import com.example.fraud.repo.RuleRepository;
import com.example.fraud.repo.TransactionRepository;
import com.example.fraud.repo.WatchlistRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;
import java.math.BigDecimal;

import com.example.fraud.model.Rule;
import com.example.fraud.model.Transaction;
import com.example.fraud.model.Watchlist;
import com.example.fraud.model.Alert;
import com.example.fraud.model.Case;
import com.example.fraud.model.AuditLog;

/**
 * TODO (student) — THE RULE ENGINE.   PROJECT_BRIEF.html §4.3
 *
 * Scan every transaction and OPEN A NEW CASE for each rule hit:
 *   R1  large single amount
 *   R2  velocity (too many transactions too fast on one account)
 *   R3  counterparty on the watchlist                              [R1–R3 required]
 *   R4  structuring (several transfers just under the limit)       [bonus]
 *
 * The fields and method signatures below are GIVEN — you fill in the bodies.
 *   - Read thresholds from the `rules` table via ruleRepo (do NOT hard-code them).
 *   - Opening a case = save an Alert, then a Case with status "NEW", then an audit_log row.
 *   - On the seeded data this must open EXACTLY 16 cases (5 R1 + 3 R2 + 5 R3 + 3 R4).
 *   - The given repositories return everything (findAll, etc.); you may also ADD query methods
 *     to them (e.g. findByAmountGreaterThanEqual) if you prefer to let the database filter.
 */
@Service
public class RuleEngineService {

    private final TransactionRepository transactionRepo;
    private final RuleRepository ruleRepo;
    private final WatchlistRepository watchlistRepo;
    private final AlertRepository alertRepo;
    private final CaseRepository caseRepo;
    private final AuditLogRepository auditLogRepo;

    public RuleEngineService(TransactionRepository transactionRepo, RuleRepository ruleRepo,
                             WatchlistRepository watchlistRepo, AlertRepository alertRepo,
                             CaseRepository caseRepo, AuditLogRepository auditLogRepo) {
        this.transactionRepo = transactionRepo;
        this.ruleRepo = ruleRepo;
        this.watchlistRepo = watchlistRepo;
        this.alertRepo = alertRepo;
        this.caseRepo = caseRepo;
        this.auditLogRepo = auditLogRepo;
    }

    /**
     * Scan all transactions and open one NEW case per rule hit.
     * @return the number of cases opened (should be 16 on the seeded data)
     */
    public int scanAndOpenCases() {
        // 1. load the rules (thresholds) from ruleRepo into a Map keyed by rule code
        Map<String, Rule> rules = new HashMap<>();

        for (Rule r : ruleRepo.findAll()) {
            if (r != null && r.getCode() != null) {
                rules.put(r.getCode(), r);
            }
        }

        List<Transaction> transactions = transactionRepo.findAll();
        int opened = 0;

        // R1: large single amount
        Rule r1 = rules.get("R1");
        if (r1 != null && r1.isEnabled() && r1.getThresholdAmount() != null) {
            BigDecimal threshold = r1.getThresholdAmount();
            for (Transaction t : transactions) {
                if (t != null && t.getAmount() != null && t.getAmount().compareTo(threshold) >= 0) {
                    String detail = "amount " + t.getAmount().toPlainString() + " >= " + threshold.toPlainString();
                    LocalDateTime when = t.getOccurredAt() != null ? t.getOccurredAt() : LocalDateTime.now();
                    openCase(t.getId(), "R1", detail, when);
                    opened++;
                }
            }
        }

        // R3: counterparty on the watchlist (case-insensitive match)
        Rule r3 = rules.get("R3");
        if (r3 != null && r3.isEnabled()) {
            // build a set of lower-cased watchlist names for quick lookup
            Set<String> watchNames = new HashSet<>();
            for (Watchlist w : watchlistRepo.findAll()) {
                if (w != null && w.getName() != null) watchNames.add(w.getName().trim().toLowerCase());
            }
            if (!watchNames.isEmpty()) {
                for (Transaction t : transactions) {
                    if (t != null && t.getCounterparty() != null &&
                            watchNames.contains(t.getCounterparty().trim().toLowerCase())) {
                        String detail = "counterparty " + t.getCounterparty() + " on watchlist";
                        LocalDateTime when = t.getOccurredAt() != null ? t.getOccurredAt() : LocalDateTime.now();
                        openCase(t.getId(), "R3", detail, when);
                        opened++;
                    }
                }
            }
        }

        // R2: velocity (minCount transactions within windowMinutes for one account)
        Rule r2 = rules.get("R2");
        if (r2 != null && r2.isEnabled() && r2.getMinCount() > 0) {
            int windowMinutes = r2.getWindowMinutes();
            int minCount = r2.getMinCount();

            // Group transactions by accountId
            Map<Long, List<Transaction>> byAccount = new HashMap<>();
            for (Transaction t : transactions) {
                if (t != null && t.getAccountId() != null && t.getOccurredAt() != null) {
                    byAccount.computeIfAbsent(t.getAccountId(), k -> new ArrayList<>()).add(t);
                }
            }

            // Check each account for velocity violations
            for (Map.Entry<Long, List<Transaction>> entry : byAccount.entrySet()) {
                List<Transaction> txns = entry.getValue();
                // Sort by time
                txns.sort((t1, t2) -> t1.getOccurredAt().compareTo(t2.getOccurredAt()));

                // Check if any sliding window contains >= minCount txns
                boolean flagged = false;
                for (int i = 0; i < txns.size() && !flagged; i++) {
                    LocalDateTime start = txns.get(i).getOccurredAt();
                    LocalDateTime end = start.plusMinutes(windowMinutes);
                    int count = 0;
                    for (Transaction t : txns) {
                        if (!t.getOccurredAt().isBefore(start) && !t.getOccurredAt().isAfter(end)) {
                            count++;
                        }
                    }
                    if (count >= minCount) {
                        // open a case for this account using the first transaction's time
                        String detail = count + " transactions in " + windowMinutes + " minute window";
                        openCase(txns.get(i).getId(), "R2", detail, start);
                        opened++;
                        flagged = true;
                    }
                }
            }
        }

        // R4: structuring (minCount transfers just under the limit within windowMinutes per account)
        Rule r4 = rules.get("R4");

        if (r4 != null
                && r4.isEnabled()
                && r4.getThresholdAmount() != null
                && r4.getMinCount() > 0) {

            int windowMinutes = r4.getWindowMinutes();
            int minCount = r4.getMinCount();

            // Use the rules table threshold instead of hard-coding 9999/10000.
            BigDecimal upper = r4.getThresholdAmount();
            BigDecimal lower = upper.subtract(new BigDecimal("1000.00"));

            Map<Long, List<Transaction>> byAccount = new HashMap<>();

            for (Transaction t : transactions) {
                if (t != null
                        && t.getAccountId() != null
                        && t.getOccurredAt() != null
                        && t.getAmount() != null
                        && t.getAmount().compareTo(lower) >= 0
                        && t.getAmount().compareTo(upper) < 0) {

                    byAccount.computeIfAbsent(t.getAccountId(), k -> new ArrayList<>()).add(t);
                }
            }

            for (Map.Entry<Long, List<Transaction>> entry : byAccount.entrySet()) {
                List<Transaction> txns = entry.getValue();
                txns.sort((t1, t2) -> t1.getOccurredAt().compareTo(t2.getOccurredAt()));

                boolean flagged = false;

                for (int i = 0; i < txns.size() && !flagged; i++) {
                    LocalDateTime start = txns.get(i).getOccurredAt();
                    LocalDateTime end = start.plusMinutes(windowMinutes);

                    int count = 0;

                    for (Transaction t : txns) {
                        if (!t.getOccurredAt().isBefore(start)
                                && !t.getOccurredAt().isAfter(end)) {
                            count++;
                        }
                    }

                    if (count >= minCount) {
                        String detail = count
                                + " transfers just under "
                                + upper.toPlainString()
                                + " in "
                                + windowMinutes
                                + " minute window";

                        openCase(txns.get(i).getId(), "R4", detail, start);
                        opened++;
                        flagged = true;
                    }
                }
            }
        }

        System.out.println("Cases opened: " + opened);
        return opened;
    }

    /**
     * Open one case for a transaction that tripped a rule.
     * @param transactionId the offending transaction's id
     * @param ruleCode      "R1".."R4"
     * @param detail        a short, human-readable reason
     * @param when          the timestamp to stamp the alert and case with
     */
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
}
