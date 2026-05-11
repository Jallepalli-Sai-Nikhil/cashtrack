package com.cashtrack.analytics.repository;

import com.cashtrack.analytics.entity.AnalyticsSnapshot;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.time.LocalDate;
import java.util.List;

@Repository
public interface AnalyticsSnapshotRepository extends JpaRepository<AnalyticsSnapshot, String> {
    List<AnalyticsSnapshot> findBySnapshotDateBetween(LocalDate start, LocalDate end);
}
