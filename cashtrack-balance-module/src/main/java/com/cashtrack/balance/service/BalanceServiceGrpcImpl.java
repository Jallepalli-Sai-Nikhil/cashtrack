package com.cashtrack.balance.service;

import com.cashtrack.api.*;
import com.cashtrack.balance.repository.TransactionHistoryRepository;
import io.grpc.stub.StreamObserver;
import net.devh.boot.grpc.server.service.GrpcService;
import org.springframework.beans.factory.annotation.Autowired;

@GrpcService
public class BalanceServiceGrpcImpl extends BalanceServiceGrpc.BalanceServiceImplBase {

    @Autowired
    private TransactionHistoryRepository historyRepository;

    @Override
    public void getBalance(BalanceRequest request, StreamObserver<BalanceResponse> responseObserver) {
        // In a real app, calculate balance from history or fetch from account module
        responseObserver.onNext(BalanceResponse.newBuilder()
                .setAccountId(request.getAccountId())
                .setBalance(1500.50)
                .build());
        responseObserver.onCompleted();
    }

    @Override
    public void getMiniStatement(StatementRequest request, StreamObserver<StatementResponse> responseObserver) {
        var history = historyRepository.findByAccountIdOrderByTimestampDesc(request.getAccountId());
        StringBuilder sb = new StringBuilder("Mini Statement: ");
        history.stream().limit(5).forEach(r -> sb.append(r.getType()).append(": ").append(r.getAmount()).append(" | "));
        
        responseObserver.onNext(StatementResponse.newBuilder()
                .setAccountId(request.getAccountId())
                .setStatementDetails(sb.toString())
                .build());
        responseObserver.onCompleted();
    }
}