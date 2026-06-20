package com.example.fraud.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;

/**
 * A fraud rule and its thresholds. The id is the rule {@code code} ("R1".."R4"), assigned by
 * hand (no @GeneratedValue) — just like the manually-assigned id in the jpa-h2 sample.
 * Thresholds live in this table so an admin can change them without editing code.
 * Maps to table {@code rules}.
 */
@Entity
@Table(name = "rules")
public class Rule {
    @Id
    private String code;                 // R1 .. R4
    private String name;
    @Column(precision = 14, scale = 2)
    private BigDecimal thresholdAmount;  // nullable: R2/R3 do not use it
    private int windowMinutes;
    private int minCount;
    private boolean enabled;

    public Rule() { }
    public Rule(String code, String name, BigDecimal thresholdAmount,
                int windowMinutes, int minCount, boolean enabled) {
        this.code = code; this.name = name; this.thresholdAmount = thresholdAmount;
        this.windowMinutes = windowMinutes; this.minCount = minCount; this.enabled = enabled;
    }

    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public BigDecimal getThresholdAmount() { return thresholdAmount; }
    public void setThresholdAmount(BigDecimal thresholdAmount) { this.thresholdAmount = thresholdAmount; }
    public int getWindowMinutes() { return windowMinutes; }
    public void setWindowMinutes(int windowMinutes) { this.windowMinutes = windowMinutes; }
    public int getMinCount() { return minCount; }
    public void setMinCount(int minCount) { this.minCount = minCount; }
    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean enabled) { this.enabled = enabled; }
}
