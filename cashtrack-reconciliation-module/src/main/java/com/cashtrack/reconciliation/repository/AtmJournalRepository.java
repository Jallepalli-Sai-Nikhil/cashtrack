package com.cashtrack.reconciliation.repository;

import com.cashtrack.reconciliation.entity.AtmJournal;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface AtmJournalRepository extends JpaRepository<AtmJournal, String> {
    List<AtmJournal> findByEventTimestampBetween(LocalDateTime start, LocalDateTime end);
}
