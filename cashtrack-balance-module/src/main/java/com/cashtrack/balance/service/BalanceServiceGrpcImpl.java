package com.cashtrack.balance.service;

import com.cashtrack.api.*;
import com.cashtrack.account.entity.Account;
import com.cashtrack.account.repository.AccountRepository;
import io.grpc.stub.StreamObserver;
import net.devh.boot.grpc.server.service.GrpcService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;

import java.util.Optional;

@GrpcService
@PreAuthorize("hasRole('CUSTOMER') or hasRole('BANK_ADMIN')")
public class BalanceServiceGrpcImpl extends BalanceServiceGrpc.BalanceServiceImplBase {

    @Autowired
    private AccountRepository accountRepository;

    @Override
    public void getBalance(BalanceRequest request, StreamObserver<BalanceResponse> responseObserver) {
        Optional<Account> accountOpt = accountRepository.findById(request.getAccountId());
        
        if (accountOpt.isPresent()) {
            responseObserver.onNext(BalanceResponse.newBuilder()
                    .setAccountId(accountOpt.get().getAccountId())
                    .setBalance(accountOpt.get().getBalance())
                    .build());
        } else {
            responseObserver.onNext(BalanceResponse.newBuilder()
                    .setAccountId(request.getAccountId())
                    .setBalance(-1.0)
                    .build());
        }
        responseObserver.onCompleted();
    }

    @Override
    public void getMiniStatement(StatementRequest request, StreamObserver<StatementResponse> responseObserver) {
        // Real logic would query Transaction table
        responseObserver.onNext(StatementResponse.newBuilder()
                .setAccountId(request.getAccountId())
                .setStatementDetails("Mini Statement: \n 2026-05-09: -100.00 (Withdrawal)\n 2026-05-08: +500.00 (Deposit)")
                .build());
        responseObserver.onCompleted();
    }
}
