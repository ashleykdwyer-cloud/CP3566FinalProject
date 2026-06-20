package com.example.fraud.model;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDateTime;

/** One permanent line per action: who did what, to which entity, when. Maps to table {@code audit_log}. */
@Entity
@Table(name = "audit_log")
public class AuditLog {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String actor;
    private String action;
    private String entity;
    private Long entityId;
    private String detail;
    private LocalDateTime atTime;

    public AuditLog() { }
    public AuditLog(String actor, String action, String entity,
                    Long entityId, String detail, LocalDateTime atTime) {
        this.actor = actor; this.action = action; this.entity = entity;
        this.entityId = entityId; this.detail = detail; this.atTime = atTime;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getActor() { return actor; }
    public void setActor(String actor) { this.actor = actor; }
    public String getAction() { return action; }
    public void setAction(String action) { this.action = action; }
    public String getEntity() { return entity; }
    public void setEntity(String entity) { this.entity = entity; }
    public Long getEntityId() { return entityId; }
    public void setEntityId(Long entityId) { this.entityId = entityId; }
    public String getDetail() { return detail; }
    public void setDetail(String detail) { this.detail = detail; }
    public LocalDateTime getAtTime() { return atTime; }
    public void setAtTime(LocalDateTime atTime) { this.atTime = atTime; }
}
