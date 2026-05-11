package com.cashtrack.forecast.service;

import com.cashtrack.api.*;
import io.grpc.stub.StreamObserver;
import net.devh.boot.grpc.server.service.GrpcService;
import org.springframework.security.access.prepost.PreAuthorize;

@GrpcService
public class ForecastServiceGrpcImpl extends ForecastServiceGrpc.ForecastServiceImplBase {

    @Override
    @PreAuthorize("hasRole('BANK_ADMIN')")
    public void forecastCashDemand(ForecastRequest request, StreamObserver<ForecastResponse> responseObserver) {
        // Weighted Moving Average logic simulation
        double baseDemand = 10000.0;
        double seasonalFactor = 1.15; // 15% increase for weekend/holiday
        double forecast = baseDemand * seasonalFactor;

        responseObserver.onNext(ForecastResponse.newBuilder()
                .setForecastData("Predicted Demand for ATM " + request.getAtmId() + ": $" + String.format("%.2f", forecast) + " over the next 24 hours.")
                .build());
        responseObserver.onCompleted();
    }

    @Override
    @PreAuthorize("hasRole('BANK_ADMIN')")
    public void recommendCashLoading(RecommendCashRequest request, StreamObserver<ForecastResponse> responseObserver) {
        responseObserver.onNext(ForecastResponse.newBuilder()
                .setForecastData("Recommendation for ATM " + request.getAtmId() + ": Load $15,000 on Tuesday morning to avoid outage.")
                .build());
        responseObserver.onCompleted();
    }
}
