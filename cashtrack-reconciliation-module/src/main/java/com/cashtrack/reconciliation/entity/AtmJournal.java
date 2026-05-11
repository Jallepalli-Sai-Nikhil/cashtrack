package com.cashtrack.reconciliation.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

@Entity
@Table(name = "atm_journals")
@Data
public class AtmJournal {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @Column(name = "atm_id")
    private String atmId;

    @Column(name = "transaction_id")
    private String transactionId;

    @Column(nullable = false)
    private Double amount;

    @Column(name = "event_timestamp")
    private LocalDateTime eventTimestamp;

    @Column(name = "recorded_at")
    private LocalDateTime recordedAt = LocalDateTime.now();
}
