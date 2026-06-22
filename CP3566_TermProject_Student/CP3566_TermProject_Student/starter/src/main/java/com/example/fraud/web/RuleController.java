package com.example.fraud.web;

import com.example.fraud.model.Rule;
import com.example.fraud.model.User;
import com.example.fraud.service.RuleEngineService;
import com.example.fraud.repo.RuleRepository;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/rules")
public class RuleController {

    private final RuleRepository ruleRepo;
    private final RuleEngineService ruleEngineService;
    private final AuthSupport auth;

    public RuleController(RuleRepository ruleRepo,
                          RuleEngineService ruleEngineService,
                          AuthSupport auth) {
        this.ruleRepo = ruleRepo;
        this.ruleEngineService = ruleEngineService;
        this.auth = auth;
    }

    @GetMapping
    public List<Rule> getRules() {
        return ruleRepo.findAll();
    }

    @PostMapping("/scan")
    public Map<String, Object> scanRules() {
        int opened = ruleEngineService.scanAndOpenCases();

        return Map.of(
                "message", "scan complete",
                "casesOpened", opened
        );
    }

    @PutMapping("/{code}")
    public Rule updateRule(@PathVariable String code,
                           @RequestHeader("X-Username") String username,
                           @RequestHeader("X-Password") String password,
                           @RequestBody Rule updated) {

        User user = auth.requireLogin(username, password);
        auth.requireRole(user, "ADMIN");

        Rule rule = ruleRepo.findById(code)
                .orElseThrow(() ->
                        new ResponseStatusException(
                                HttpStatus.NOT_FOUND,
                                "Rule not found"));

        rule.setThresholdAmount(updated.getThresholdAmount());
        rule.setWindowMinutes(updated.getWindowMinutes());
        rule.setMinCount(updated.getMinCount());
        rule.setEnabled(updated.isEnabled());

        return ruleRepo.save(rule);
    }
}
