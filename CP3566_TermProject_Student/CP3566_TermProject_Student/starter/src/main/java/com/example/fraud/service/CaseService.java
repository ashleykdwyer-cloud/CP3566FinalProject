package com.example.fraud.service;

import com.example.fraud.model.AuditLog;
import com.example.fraud.model.Case;
import com.example.fraud.repo.AuditLogRepository;
import com.example.fraud.repo.CaseRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;

/**
 * TODO (student) — THE CASE WORKFLOW (state machine).   PROJECT_BRIEF.html §4.4
 *
 * Legal moves (anything else must be rejected):
 *   pickup:      NEW       -> REVIEWING      (ANALYST)
 *   escalate:    REVIEWING -> ESCALATED      (ANALYST)
 *   send-back:   ESCALATED -> REVIEWING      (INVESTIGATOR)
 *   close-false: REVIEWING -> CLOSED_FALSE   (ANALYST)
 *   close-false: ESCALATED -> CLOSED_FALSE   (INVESTIGATOR)
 *   close-fraud: ESCALATED -> CLOSED_FRAUD   (INVESTIGATOR)
 *
 * The fields and the method signature below are GIVEN — you fill in the body.
 */
@Service
public class CaseService {

    private final CaseRepository caseRepo;
    private final AuditLogRepository auditLogRepo;

    public CaseService(CaseRepository caseRepo, AuditLogRepository auditLogRepo) {
        this.caseRepo = caseRepo;
        this.auditLogRepo = auditLogRepo;
    }

    /**
     * Apply an action to a case, enforcing the state machine and the role rules. Throw so your
     * controller can map it to the right HTTP status:
     * case not found -> 404,  illegal move from the current state -> 409,  wrong role -> 403.
     *
     * @param caseId        the case to act on
     * @param action        one of: pickup, escalate, send-back, close-false, close-fraud
     * @param actorUsername who is doing it (use for assignedTo on pickup, and for the audit log)
     * @param actorRole     ANALYST | INVESTIGATOR | ADMIN
     * @return the updated case
     */
    public Case apply(Long caseId, String action, String actorUsername, String actorRole) {
        // 1) load case (404 if not found)
        Case theCase = caseRepo.findById(caseId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Case not found: " + caseId));

        String current = theCase.getStatus();
        if (current == null) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Case has no status: " + caseId);
        }

        // 2) validate action legal from current status -> determine nextStatus (or throw 409)
        String nextStatus;
        switch (action) {
            case "pickup":
                // NEW -> REVIEWING (ANALYST)
                if (!"NEW".equals(current)) {
                    throw new ResponseStatusException(HttpStatus.CONFLICT,
                            "Illegal action 'pickup' from status: " + current);
                }
                nextStatus = "REVIEWING";
                // role check later
                break;

            case "escalate":
                // REVIEWING -> ESCALATED (ANALYST)
                if (!"REVIEWING".equals(current)) {
                    throw new ResponseStatusException(HttpStatus.CONFLICT,
                            "Illegal action 'escalate' from status: " + current);
                }
                nextStatus = "ESCALATED";
                break;

            case "send-back":
                // ESCALATED -> REVIEWING (INVESTIGATOR)
                if (!"ESCALATED".equals(current)) {
                    throw new ResponseStatusException(HttpStatus.CONFLICT,
                            "Illegal action 'send-back' from status: " + current);
                }
                nextStatus = "REVIEWING";
                break;

            case "close-false":
                // REVIEWING -> CLOSED_FALSE (ANALYST)
                // or ESCALATED -> CLOSED_FALSE (INVESTIGATOR)
                if ("REVIEWING".equals(current) || "ESCALATED".equals(current)) {
                    nextStatus = "CLOSED_FALSE";
                } else {
                    throw new ResponseStatusException(HttpStatus.CONFLICT,
                            "Illegal action 'close-false' from status: " + current);
                }
                break;

            case "close-fraud":
                // ESCALATED -> CLOSED_FRAUD (INVESTIGATOR)
                if (!"ESCALATED".equals(current)) {
                    throw new ResponseStatusException(HttpStatus.CONFLICT,
                            "Illegal action 'close-fraud' from status: " + current);
                }
                nextStatus = "CLOSED_FRAUD";
                break;

            default:
                throw new ResponseStatusException(HttpStatus.CONFLICT, "Unknown action: " + action);
        }
        // 3) enforce role rules (throw 403 if not allowed). ADMIN bypasses restrictions.
        String role = actorRole == null ? "" : actorRole.trim();
        boolean allowed;
        switch (action) {
            case "pickup":
            case "escalate":
                // ANALYST required (or ADMIN)
                allowed = "ANALYST".equals(role) || "ADMIN".equals(role);
                break;

            case "send-back":
            case "close-fraud":
                // INVESTIGATOR required (or ADMIN)
                allowed = "INVESTIGATOR".equals(role) || "ADMIN".equals(role);
                break;

            case "close-false":
                // If coming from REVIEWING => ANALYST; if from ESCALATED => INVESTIGATOR; ADMIN allowed
                if ("REVIEWING".equals(current)) {
                    allowed = "ANALYST".equals(role) || "ADMIN".equals(role);
                } else { // ESCALATED
                    allowed = "INVESTIGATOR".equals(role) || "ADMIN".equals(role);
                }
                break;

            default:
                allowed = false; // should not happen
        }

        if (!allowed) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "Role '" + role + "' not allowed to perform action '" + action + "' from status '" + current + "'");
        }
        // 4) apply transition: set status, assignedTo on pickup, save
        theCase.setStatus(nextStatus);
        if ("pickup".equals(action)) {
            theCase.setAssignedTo(actorUsername);
        }

        Case saved = caseRepo.save(theCase);

        // 5) audit log
        String detail = "status: " + current + " -> " + nextStatus;
        AuditLog log = new AuditLog(actorUsername, action, "case", saved.getId(), detail, LocalDateTime.now());
        auditLogRepo.save(log);

        return saved;
    }
}
        // TODO (student):
        //   1. load the case from caseRepo (not found -> throw 404)
        //   2. is `action` legal from the case's current status? if not -> throw 409
        //   3. is `actorRole` allowed to make that move? if not -> throw 403
        //   4. set the new status (and assignedTo on pickup), then save the case
        //   5. write one audit_log row with auditLogRepo, then return the updated case


