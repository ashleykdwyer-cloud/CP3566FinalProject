package com.example.fraud.service;

import com.example.fraud.model.Case;
import com.example.fraud.repo.AuditLogRepository;
import com.example.fraud.repo.CaseRepository;
import org.springframework.stereotype.Service;

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
     *   case not found -> 404,  illegal move from the current state -> 409,  wrong role -> 403.
     *
     * @param caseId        the case to act on
     * @param action        one of: pickup, escalate, send-back, close-false, close-fraud
     * @param actorUsername who is doing it (use for assignedTo on pickup, and for the audit log)
     * @param actorRole     ANALYST | INVESTIGATOR | ADMIN
     * @return the updated case
     */
    public Case apply(Long caseId, String action, String actorUsername, String actorRole) {
        // TODO (student):
        //   1. load the case from caseRepo (not found -> throw 404)
        //   2. is `action` legal from the case's current status? if not -> throw 409
        //   3. is `actorRole` allowed to make that move? if not -> throw 403
        //   4. set the new status (and assignedTo on pickup), then save the case
        //   5. write one audit_log row with auditLogRepo, then return the updated case
        throw new UnsupportedOperationException("TODO: implement the case workflow (see PROJECT_BRIEF.html §4.4)");
    }
}
