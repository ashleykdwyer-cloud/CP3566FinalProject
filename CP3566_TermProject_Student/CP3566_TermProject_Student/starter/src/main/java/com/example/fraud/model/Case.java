package com.example.fraud.model;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDateTime;

/**
 * A case a person works through the state machine
 * (NEW -> REVIEWING -> ESCALATED -> CLOSED_FALSE | CLOSED_FRAUD). Maps to table {@code cases}
 * ("cases", not "case", because CASE is a reserved SQL word).
 */
@Entity
@Table(name = "cases")
public class Case {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private Long alertId;
    private String status;        // NEW | REVIEWING | ESCALATED | CLOSED_FALSE | CLOSED_FRAUD
    private String assignedTo;    // nullable until someone picks it up
    private LocalDateTime openedAt;

    public Case() { }
    public Case(Long alertId, String status, String assignedTo, LocalDateTime openedAt) {
        this.alertId = alertId; this.status = status;
        this.assignedTo = assignedTo; this.openedAt = openedAt;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getAlertId() { return alertId; }
    public void setAlertId(Long alertId) { this.alertId = alertId; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public String getAssignedTo() { return assignedTo; }
    public void setAssignedTo(String assignedTo) { this.assignedTo = assignedTo; }
    public LocalDateTime getOpenedAt() { return openedAt; }
    public void setOpenedAt(LocalDateTime openedAt) { this.openedAt = openedAt; }
}
