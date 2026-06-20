package com.example.fraud.model;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import java.time.LocalDateTime;

/** Raised when a transaction trips a rule; each alert opens one case. Maps to table {@code alerts}. */
@Entity
@Table(name = "alerts",
       indexes = @Index(name = "idx_alert_rule", columnList = "rule_code"))
public class Alert {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private Long transactionId;
    private String ruleCode;
    private String detail;
    private LocalDateTime createdAt;

    public Alert() { }
    public Alert(Long transactionId, String ruleCode, String detail, LocalDateTime createdAt) {
        this.transactionId = transactionId; this.ruleCode = ruleCode;
        this.detail = detail; this.createdAt = createdAt;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getTransactionId() { return transactionId; }
    public void setTransactionId(Long transactionId) { this.transactionId = transactionId; }
    public String getRuleCode() { return ruleCode; }
    public void setRuleCode(String ruleCode) { this.ruleCode = ruleCode; }
    public String getDetail() { return detail; }
    public void setDetail(String detail) { this.detail = detail; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
