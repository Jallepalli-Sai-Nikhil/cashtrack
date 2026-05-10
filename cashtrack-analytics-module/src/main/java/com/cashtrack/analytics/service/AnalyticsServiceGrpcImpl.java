package com.cashtrack.analytics.service;

import com.cashtrack.api.*;
import io.grpc.stub.StreamObserver;
import net.devh.boot.grpc.server.service.GrpcService;
import org.springframework.security.access.prepost.PreAuthorize;

@GrpcService
@PreAuthorize("hasAnyRole('BANK_ADMIN', 'AUDITOR')")
public class AnalyticsServiceGrpcImpl extends AnalyticsServiceGrpc.AnalyticsServiceImplBase {
    @Override
    public void getTransactionAnalytics(AnalyticsRequest request, StreamObserver<AnalyticsResponse> responseObserver) {
        responseObserver.onNext(AnalyticsResponse.newBuilder().setData("{}").build());
        responseObserver.onCompleted();
    }
}