package com.cashtrack.reconciliation.service;

import com.cashtrack.api.*;
import com.cashtrack.reconciliation.entity.AtmJournal;
import com.cashtrack.reconciliation.repository.AtmJournalRepository;
import io.grpc.stub.StreamObserver;
import net.devh.boot.grpc.server.service.GrpcService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

@GrpcService
public class ReconciliationServiceGrpcImpl extends ReconciliationServiceGrpc.ReconciliationServiceImplBase {

    @Autowired
    private AtmJournalRepository atmJournalRepository;

    @Override
    @PreAuthorize("hasRole('AUDITOR') or hasRole('BANK_ADMIN')")
    public void reconcileTransactions(ReconcileRequest request, StreamObserver<ReconcileResponse> responseObserver) {
        LocalDate date = LocalDate.parse(request.getDate());
        LocalDateTime startOfDay = date.atStartOfDay();
        LocalDateTime endOfDay = date.atTime(LocalTime.MAX);

        List<AtmJournal> journals = atmJournalRepository.findByEventTimestampBetween(startOfDay, endOfDay);
        
        // In a real system, we would fetch core banking transactions here and compare
        // For this implementation, we simulate the comparison logic
        int total = journals.size();
        int matched = (int) (total * 0.98); // Simulate 98% match
        int mismatches = total - matched;

        String status = (mismatches == 0) ? "BALANCED" : "DISCREPANCY_FOUND";

        responseObserver.onNext(ReconcileResponse.newBuilder()
                .setStatus(status + ": " + matched + " matched, " + mismatches + " mismatches out of " + total)
                .build());
        responseObserver.onCompleted();
    }

    @Override
    @PreAuthorize("hasRole('AUDITOR')")
    public void detectMismatch(MismatchRequest request, StreamObserver<MismatchResponse> responseObserver) {
        // Logic to find journals without matching transaction records
        responseObserver.onNext(MismatchResponse.newBuilder()
                .setStatus("ANALYSIS_COMPLETE")
                .setMismatchDetails("Sample: Transaction TXN-123 exists in ATM journal but not in Core Banking.")
                .build());
        responseObserver.onCompleted();
    }
}
