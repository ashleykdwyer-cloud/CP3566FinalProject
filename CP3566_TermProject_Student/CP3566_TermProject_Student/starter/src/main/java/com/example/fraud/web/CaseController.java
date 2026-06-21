package com.example.fraud.web;

import com.example.fraud.model.AuditLog;
import com.example.fraud.model.Case;
import com.example.fraud.model.User;
import com.example.fraud.repo.AuditLogRepository;
import com.example.fraud.repo.CaseRepository;
import com.example.fraud.service.CaseService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/cases")
public class CaseController {

    private final CaseRepository caseRepo;
    private final CaseService caseService;
    private final AuditLogRepository auditLogRepo;
    private final AuthSupport auth;

    public CaseController(CaseRepository caseRepo,
                          CaseService caseService,
                          AuditLogRepository auditLogRepo,
                          AuthSupport auth) {
        this.caseRepo = caseRepo;
        this.caseService = caseService;
        this.auditLogRepo = auditLogRepo;
        this.auth = auth;
    }

    @GetMapping
    public List<Case> listCases(@RequestParam(required = false) String status) {
        if (status == null || status.isBlank()) {
            return caseRepo.findAll();
        }

        return caseRepo.findByStatus(status);
    }

    @GetMapping("/{id}")
    public Case getCase(@PathVariable Long id) {
        return caseRepo.findById(id)
                .orElseThrow(() -> new ResponseStatusException(
                                HttpStatus.NOT_FOUND,
                                "Case not found"));
    }

    @PostMapping("/{id}/pickup")
    public Case pickup(@PathVariable Long id,
                       @RequestHeader("X-Username") String username,
                       @RequestHeader("X-Password") String password) {

        User user = auth.requireLogin(username, password);
        auth.requireRole(user, "ANALYST");

        return caseService.apply(id, "pickup", user.getUsername(), user.getRole());
    }

    @PostMapping("/{id}/escalate")
    public Case escalate(@PathVariable Long id,
                         @RequestHeader("X-Username") String username,
                         @RequestHeader("X-Password") String password) {
        User user = auth.requireLogin(username, password);
        auth.requireRole(user, "ANALYST");

        return caseService.apply(id, "escalate", user.getUsername(), user.getRole());
    }

    @PostMapping("/{id}/send-back")
    public Case sendBack(@PathVariable Long id,
                         @RequestHeader("X-Username") String username,
                         @RequestHeader("X-Password") String password) {
        User user = auth.requireLogin(username, password);
        auth.requireRole(user, "INVESTIGATOR");

        return caseService.apply(id, "send-back", user.getUsername(), user.getRole());
    }

    @PostMapping("/{id}/close-false")
    public Case closeFalse(@PathVariable Long id,
                           @RequestHeader("X-Username") String username,
                           @RequestHeader("X-Password") String password) {
        User user = auth.requireLogin(username, password);
        auth.requireRole(user, "ANALYST", "INVESTIGATOR");

        return caseService.apply(id, "close-false", user.getUsername(), user.getRole());
    }

    @PostMapping("/{id}/close-fraud")
    public Case closeFraud(@PathVariable Long id,
                           @RequestHeader("X-Username") String username,
                           @RequestHeader("X-Password") String password) {
        User user = auth.requireLogin(username, password);
        auth.requireRole(user, "INVESTIGATOR");

        return caseService.apply(id, "close-fraud", user.getUsername(), user.getRole());
    }

    @PostMapping("/{id}/notes")
    @ResponseStatus(HttpStatus.CREATED)
    public Map<String, String> addNote(
            @PathVariable Long id,
            @RequestHeader("X-Username") String username,
            @RequestHeader("X-Password") String password,
            @RequestBody Map<String, String> body) {

        User user = auth.requireLogin(username, password);

        Case c = caseRepo.findById(id)
                .orElseThrow(() ->
                        new ResponseStatusException(
                                HttpStatus.NOT_FOUND,
                                "Case not found"));

        String note = body.getOrDefault("note", "");

        AuditLog log = new AuditLog(
                user.getUsername(),
                "ADD_NOTE",
                "case",
                c.getId(),
                note,
                LocalDateTime.now()
        );

        auditLogRepo.save(log);

        return Map.of("message", "note added");
    }
}