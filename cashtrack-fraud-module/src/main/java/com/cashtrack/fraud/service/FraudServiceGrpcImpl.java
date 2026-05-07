package com.cashtrack.fraud.service;

import com.cashtrack.api.*;
import com.cashtrack.fraud.entity.FraudAlert;
import com.cashtrack.fraud.repository.FraudRepository;
import io.grpc.stub.StreamObserver;
import net.devh.boot.grpc.server.service.GrpcService;
import org.springframework.beans.factory.annotation.Autowired;
import java.time.LocalDateTime;

@GrpcService
public class FraudServiceGrpcImpl extends FraudServiceGrpc.FraudServiceImplBase {

    @Autowired
    private FraudRepository fraudRepository;

    @Override
    public void detectFraudulentTransaction(FraudTransactionIdRequest request, StreamObserver<FraudResponse> responseObserver) {
        // Mock logic: Transactions over 10000 are flagged
        String status = "SAFE";
        // In a real scenario, we'd fetch the transaction and check its amount
        
        responseObserver.onNext(FraudResponse.newBuilder()
                .setStatus(status)
                .build());
        responseObserver.onCompleted();
    }

    @Override
    public void flagSuspiciousActivity(SuspiciousActivityRequest request, StreamObserver<FraudResponse> responseObserver) {
        FraudAlert alert = new FraudAlert();
        alert.setAccountId(request.getAccountId());
        alert.setAlertType("MANUAL_FLAG");
        alert.setSeverity("HIGH");
        alert.setTimestamp(LocalDateTime.now());
        fraudRepository.save(alert);

        responseObserver.onNext(FraudResponse.newBuilder()
                .setStatus("FLAGGED")
                .build());
        responseObserver.onCompleted();
    }
}