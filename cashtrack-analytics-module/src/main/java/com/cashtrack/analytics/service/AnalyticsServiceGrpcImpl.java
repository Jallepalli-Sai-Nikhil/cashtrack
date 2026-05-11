package com.cashtrack.analytics.service;

import com.cashtrack.api.*;
import com.cashtrack.analytics.entity.AnalyticsSnapshot;
import com.cashtrack.analytics.repository.AnalyticsSnapshotRepository;
import io.grpc.stub.StreamObserver;
import net.devh.boot.grpc.server.service.GrpcService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;

import java.util.List;

@GrpcService
public class AnalyticsServiceGrpcImpl extends AnalyticsServiceGrpc.AnalyticsServiceImplBase {

    @Autowired
    private AnalyticsSnapshotRepository analyticsSnapshotRepository;

    @Override
    @PreAuthorize("hasRole('AUDITOR') or hasRole('BANK_ADMIN')")
    public void getTransactionAnalytics(AnalyticsRequest request, StreamObserver<AnalyticsResponse> responseObserver) {
        // Query logic to aggregate transaction data
        List<AnalyticsSnapshot> snapshots = analyticsSnapshotRepository.findAll();
        
        StringBuilder data = new StringBuilder("Transaction Analytics Report:\n");
        snapshots.forEach(s -> data.append(s.getSnapshotDate())
                                  .append(" - ")
                                  .append(s.getMetricName())
                                  .append(": ")
                                  .append(s.getMetricValue())
                                  .append("\n"));

        responseObserver.onNext(AnalyticsResponse.newBuilder()
                .setData(data.length() > 30 ? data.toString() : "No data available for the selected range.")
                .build());
        responseObserver.onCompleted();
    }

    @Override
    @PreAuthorize("hasRole('BANK_ADMIN')")
    public void getATMUsageStats(AnalyticsATMIdRequest request, StreamObserver<AnalyticsResponse> responseObserver) {
        responseObserver.onNext(AnalyticsResponse.newBuilder()
                .setData("Usage Stats for ATM " + request.getAtmId() + ": 85% Utilization, 120 Transactions/Day avg.")
                .build());
        responseObserver.onCompleted();
    }
}
