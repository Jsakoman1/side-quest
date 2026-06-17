package com.sidequest.sidequest.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.Instant;

@Getter
@Setter
@Entity
@Table(name = "quest")
public class Quest {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "creator_id", nullable = false)
    private AppUser creator;

    @Column(nullable = false)
    private String title;

    @Column(length = 2000)
    private String description;

    @Column(precision = 10, scale = 2)
    private BigDecimal awardAmount;

    @Column(name = "scheduled_at")
    private Instant scheduledAt;

    @Column(name = "term_fixed", nullable = false)
    private boolean termFixed = false;

    @Column(name = "pending_scheduled_at")
    private Instant pendingScheduledAt;

    @Column(name = "pending_term_fixed")
    private Boolean pendingTermFixed;

    @Column(name = "reopened_at")
    private Instant reopenedAt;

    @Enumerated(EnumType.STRING)
    @Column(name = "term_change_previous_status")
    private QuestStatus termChangePreviousStatus;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private QuestStatus status = QuestStatus.OPEN;

}
