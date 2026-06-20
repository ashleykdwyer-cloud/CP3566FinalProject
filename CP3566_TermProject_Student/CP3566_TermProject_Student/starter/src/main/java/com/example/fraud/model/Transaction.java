package com.example.fraud.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * A single money transfer — the rules screen these. {@code accountId} is a plain foreign-key
 * column (kept simple, no JPA relationship). Maps to table {@code transactions}.
 * The index supports the velocity/structuring scan, which orders by (account, time).
 */
@Entity
@Table(name = "transactions",
       indexes = @Index(name = "idx_txn_account_time", columnList = "account_id, occurred_at"))
public class Transaction {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private Long accountId;
    @Column(precision = 14, scale = 2)
    private BigDecimal amount;
    private String currency;
    private String counterparty;
    private String country;
    private LocalDateTime occurredAt;

    public Transaction() { }
    public Transaction(Long accountId, BigDecimal amount, String currency,
                       String counterparty, String country, LocalDateTime occurredAt) {
        this.accountId = accountId; this.amount = amount; this.currency = currency;
        this.counterparty = counterparty; this.country = country; this.occurredAt = occurredAt;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getAccountId() { return accountId; }
    public void setAccountId(Long accountId) { this.accountId = accountId; }
    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }
    public String getCurrency() { return currency; }
    public void setCurrency(String currency) { this.currency = currency; }
    public String getCounterparty() { return counterparty; }
    public void setCounterparty(String counterparty) { this.counterparty = counterparty; }
    public String getCountry() { return country; }
    public void setCountry(String country) { this.country = country; }
    public LocalDateTime getOccurredAt() { return occurredAt; }
    public void setOccurredAt(LocalDateTime occurredAt) { this.occurredAt = occurredAt; }
}
