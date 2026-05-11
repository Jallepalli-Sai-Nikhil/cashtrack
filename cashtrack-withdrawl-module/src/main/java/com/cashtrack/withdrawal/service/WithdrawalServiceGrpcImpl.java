package com.cashtrack.withdrawal.service;

import com.cashtrack.api.*;
import com.cashtrack.account.entity.Account;
import com.cashtrack.account.repository.AccountRepository;
import com.cashtrack.withdrawal.entity.WithdrawalTransaction;
import com.cashtrack.withdrawal.repository.WithdrawalRepository;
import io.grpc.stub.StreamObserver;
import net.devh.boot.grpc.server.service.GrpcService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@GrpcService
@PreAuthorize("hasRole('CUSTOMER')")
public class WithdrawalServiceGrpcImpl extends WithdrawalServiceGrpc.WithdrawalServiceImplBase {

    @Autowired
    private WithdrawalRepository withdrawalRepository;

    @Autowired
    private AccountRepository accountRepository;

    @Override
    @Transactional
    public void initiateWithdrawal(WithdrawalRequest request, StreamObserver<TransactionResponse> responseObserver) {
        Optional<Account> accountOpt = accountRepository.findById(request.getAccountId());
        
        if (accountOpt.isEmpty()) {
            responseObserver.onNext(TransactionResponse.newBuilder()
                    .setStatus("FAILED")
                    .setMessage("Account not found")
                    .build());
            responseObserver.onCompleted();
            return;
        }

        Account account = accountOpt.get();
        if (account.getBalance() < request.getAmount()) {
            responseObserver.onNext(TransactionResponse.newBuilder()
                    .setStatus("INSUFFICIENT_FUNDS")
                    .setMessage("Account balance is too low")
                    .build());
            responseObserver.onCompleted();
            return;
        }

        WithdrawalTransaction tx = new WithdrawalTransaction();
        tx.setAccountId(request.getAccountId());
        tx.setAtmId(request.getAtmId());
        tx.setAmount(request.getAmount());
        tx.setStatus("INITIATED");

        withdrawalRepository.save(tx);

        responseObserver.onNext(TransactionResponse.newBuilder()
                .setTransactionId(tx.getTransactionId())
                .setStatus(tx.getStatus())
                .setMessage("Withdrawal initiated. Please confirm.")
                .build());
        responseObserver.onCompleted();
    }

    @Override
    @Transactional
    public void confirmWithdrawal(TransactionIdRequest request, StreamObserver<TransactionResponse> responseObserver) {
        Optional<WithdrawalTransaction> txOpt = withdrawalRepository.findById(request.getTransactionId());
        
        if (txOpt.isPresent()) {
            WithdrawalTransaction tx = txOpt.get();
            if ("COMPLETED".equals(tx.getStatus())) {
                responseObserver.onNext(TransactionResponse.newBuilder()
                        .setStatus("ALREADY_COMPLETED")
                        .build());
                responseObserver.onCompleted();
                return;
            }

            Optional<Account> accountOpt = accountRepository.findById(tx.getAccountId());
            if (accountOpt.isPresent()) {
                Account account = accountOpt.get();
                account.setBalance(account.getBalance() - tx.getAmount());
                accountRepository.save(account);

                tx.setStatus("COMPLETED");
                withdrawalRepository.save(tx);

                responseObserver.onNext(TransactionResponse.newBuilder()
                        .setTransactionId(tx.getTransactionId())
                        .setStatus("SUCCESS")
                        .setMessage("Cash dispensed and account debited.")
                        .build());
            } else {
                responseObserver.onNext(TransactionResponse.newBuilder()
                        .setStatus("ACCOUNT_LOST")
                        .build());
            }
        } else {
            responseObserver.onNext(TransactionResponse.newBuilder()
                    .setStatus("NOT_FOUND")
                    .build());
        }
        responseObserver.onCompleted();
    }

    @Override
    @Transactional
    public void reverseWithdrawal(TransactionIdRequest request, StreamObserver<TransactionResponse> responseObserver) {
        Optional<WithdrawalTransaction> txOpt = withdrawalRepository.findById(request.getTransactionId());
        if (txOpt.isPresent()) {
            WithdrawalTransaction tx = txOpt.get();
            tx.setStatus("REVERSED");
            withdrawalRepository.save(tx);
            responseObserver.onNext(TransactionResponse.newBuilder()
                    .setStatus("REVERSED")
                    .build());
        } else {
            responseObserver.onNext(TransactionResponse.newBuilder()
                    .setStatus("NOT_FOUND")
                    .build());
        }
        responseObserver.onCompleted();
    }
}
