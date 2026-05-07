package com.cashtrack.reconciliation.service;

import com.cashtrack.api.*;
import com.cashtrack.reconciliation.entity.ReconciliationLog;
import com.cashtrack.reconciliation.repository.ReconciliationRepository;
import io.grpc.stub.StreamObserver;
import net.devh.boot.grpc.server.service.GrpcService;
import org.springframework.beans.factory.annotation.Autowired;
import java.time.LocalDate;

@GrpcService
public class ReconciliationServiceGrpcImpl extends ReconciliationServiceGrpc.ReconciliationServiceImplBase {

    @Autowired
    private ReconciliationRepository reconciliationRepository;

    @Override
    public void reconcileTransactions(ReconcileRequest request, StreamObserver<ReconcileResponse> responseObserver) {
        ReconciliationLog log = new ReconciliationLog();
        log.setReconciliationDate(LocalDate.parse(request.getDate()));
        log.setTotalTransactions(100); // Mock data
        log.setMatchedCount(98);
        log.setMismatchedCount(2);
        log.setStatus("DISCREPANCY_FOUND");
        reconciliationRepository.save(log);

        responseObserver.onNext(ReconcileResponse.newBuilder()
                .setStatus(log.getStatus())
                .build());
        responseObserver.onCompleted();
    }
}