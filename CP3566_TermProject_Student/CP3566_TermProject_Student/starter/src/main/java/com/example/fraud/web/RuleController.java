package com.example.fraud.web;

import com.example.fraud.model.Rule;
import com.example.fraud.repo.RuleRepository;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@RestController
@RequestMapping("/api/rules")
public class RuleController {

    private final RuleRepository ruleRepo;

    public RuleController(RuleRepository ruleRepo) {
        this.ruleRepo = ruleRepo;
    }

    @GetMapping
    public List<Rule> getRules() {
        return ruleRepo.findAll();
    }

    @PutMapping("/{code}")
    public Rule updateRule(@PathVariable String code,
                           @RequestBody Rule updated) {

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
