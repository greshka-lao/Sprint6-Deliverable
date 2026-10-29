package com.fdmgroup.sprint4Deliverable.Model;

import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

@Entity
@Table(name = "admin_security_events")
public class AdminSecurityEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "admin_user_id")
    private AdminUser adminUser;

    @Column(nullable = false, length = 40)
    private String eventType;

    @Column(nullable = false)
    private Instant occurredAt;

    @Column(nullable = false, length = 40)
    private String outcome;

    @Column(length = 255)
    private String subject;

    public AdminSecurityEvent() {
    }

    public AdminSecurityEvent(AdminUser adminUser, String eventType, String outcome, String subject) {
        this.adminUser = adminUser;
        this.eventType = eventType;
        this.outcome = outcome;
        this.subject = subject;
    }

    @PrePersist
    public void onCreate() {
        if (occurredAt == null) {
            occurredAt = Instant.now();
        }
    }

    public Long getId() {
        return id;
    }

    public AdminUser getAdminUser() {
        return adminUser;
    }

    public void setAdminUser(AdminUser adminUser) {
        this.adminUser = adminUser;
    }

    public String getEventType() {
        return eventType;
    }

    public void setEventType(String eventType) {
        this.eventType = eventType;
    }

    public Instant getOccurredAt() {
        return occurredAt;
    }

    public String getOutcome() {
        return outcome;
    }

    public void setOutcome(String outcome) {
        this.outcome = outcome;
    }

    public String getSubject() {
        return subject;
    }

    public void setSubject(String subject) {
        this.subject = subject;
    }
}
