package com.cashtrack.forecast.service;

import com.cashtrack.api.*;
import io.grpc.stub.StreamObserver;
import net.devh.boot.grpc.server.service.GrpcService;

@GrpcService
public class ForecastServiceGrpcImpl extends ForecastServiceGrpc.ForecastServiceImplBase {
    @Override
    public void forecastCashDemand(ForecastRequest request, StreamObserver<ForecastResponse> responseObserver) {
        responseObserver.onNext(ForecastResponse.newBuilder().setForecastData("{}").build());
        responseObserver.onCompleted();
    }
}