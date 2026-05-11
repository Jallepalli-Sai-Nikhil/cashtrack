package com.cashtrack.deposit.service;

import com.cashtrack.api.*;
import com.cashtrack.account.entity.Account;
import com.cashtrack.account.repository.AccountRepository;
import com.cashtrack.deposit.entity.DepositTransaction;
import com.cashtrack.deposit.repository.DepositRepository;
import io.grpc.stub.StreamObserver;
import net.devh.boot.grpc.server.service.GrpcService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@GrpcService
@PreAuthorize("hasRole('CUSTOMER')")
public class DepositServiceGrpcImpl extends DepositServiceGrpc.DepositServiceImplBase {

    @Autowired
    private DepositRepository depositRepository;

    @Autowired
    private AccountRepository accountRepository;

    @Override
    @Transactional
    public void initiateDeposit(DepositRequest request, StreamObserver<TransactionResponse> responseObserver) {
        Optional<Account> accountOpt = accountRepository.findById(request.getAccountId());
        
        if (accountOpt.isEmpty()) {
            responseObserver.onNext(TransactionResponse.newBuilder()
                    .setStatus("FAILED")
                    .setMessage("Account not found")
                    .build());
            responseObserver.onCompleted();
            return;
        }

        DepositTransaction tx = new DepositTransaction();
        tx.setAccountId(request.getAccountId());
        tx.setAtmId(request.getAtmId());
        tx.setAmount(request.getAmount());
        tx.setStatus("INITIATED");

        depositRepository.save(tx);

        responseObserver.onNext(TransactionResponse.newBuilder()
                .setTransactionId(tx.getTransactionId())
                .setStatus(tx.getStatus())
                .setMessage("Deposit initiated. Please insert cash.")
                .build());
        responseObserver.onCompleted();
    }

    @Override
    @Transactional
    public void acceptCash(TransactionIdRequest request, StreamObserver<TransactionResponse> responseObserver) {
        updateStatus(request.getTransactionId(), "CASH_ACCEPTED", responseObserver);
    }

    @Override
    @Transactional
    public void confirmDeposit(TransactionIdRequest request, StreamObserver<TransactionResponse> responseObserver) {
        Optional<DepositTransaction> txOpt = depositRepository.findById(request.getTransactionId());
        
        if (txOpt.isPresent()) {
            DepositTransaction tx = txOpt.get();
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
                account.setBalance(account.getBalance() + tx.getAmount());
                accountRepository.save(account);

                tx.setStatus("COMPLETED");
                depositRepository.save(tx);

                responseObserver.onNext(TransactionResponse.newBuilder()
                        .setTransactionId(tx.getTransactionId())
                        .setStatus("SUCCESS")
                        .setMessage("Deposit completed. Account credited.")
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

    private void updateStatus(String txId, String status, StreamObserver<TransactionResponse> observer) {
        Optional<DepositTransaction> txOpt = depositRepository.findById(txId);
        if (txOpt.isPresent()) {
            DepositTransaction tx = txOpt.get();
            tx.setStatus(status);
            depositRepository.save(tx);
            observer.onNext(TransactionResponse.newBuilder()
                    .setTransactionId(tx.getTransactionId())
                    .setStatus(tx.getStatus())
                    .build());
        } else {
            observer.onNext(TransactionResponse.newBuilder().setStatus("NOT_FOUND").build());
        }
        observer.onCompleted();
    }
}
