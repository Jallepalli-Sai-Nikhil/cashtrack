package com.cashtrack.transfer.service;

import com.cashtrack.api.*;
import com.cashtrack.account.entity.Account;
import com.cashtrack.account.repository.AccountRepository;
import com.cashtrack.transfer.entity.TransferTransaction;
import com.cashtrack.transfer.repository.TransferRepository;
import io.grpc.stub.StreamObserver;
import net.devh.boot.grpc.server.service.GrpcService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@GrpcService
@PreAuthorize("hasRole('CUSTOMER')")
public class TransferServiceGrpcImpl extends TransferServiceGrpc.TransferServiceImplBase {

    @Autowired
    private TransferRepository transferRepository;

    @Autowired
    private AccountRepository accountRepository;

    @Override
    @Transactional
    public void initiateTransfer(TransferRequest request, StreamObserver<TransactionResponse> responseObserver) {
        Optional<Account> sourceOpt = accountRepository.findById(request.getSourceAccountId());
        Optional<Account> targetOpt = accountRepository.findById(request.getTargetAccountId());

        if (sourceOpt.isEmpty() || targetOpt.isEmpty()) {
            responseObserver.onNext(TransactionResponse.newBuilder()
                    .setStatus("FAILED")
                    .setMessage("One or both accounts not found")
                    .build());
            responseObserver.onCompleted();
            return;
        }

        if (sourceOpt.get().getBalance() < request.getAmount()) {
            responseObserver.onNext(TransactionResponse.newBuilder()
                    .setStatus("INSUFFICIENT_FUNDS")
                    .build());
            responseObserver.onCompleted();
            return;
        }

        TransferTransaction tx = new TransferTransaction();
        tx.setSourceAccountId(request.getSourceAccountId());
        tx.setTargetAccountId(request.getTargetAccountId());
        tx.setAmount(request.getAmount());
        tx.setStatus("INITIATED");

        transferRepository.save(tx);

        responseObserver.onNext(TransactionResponse.newBuilder()
                .setTransactionId(tx.getTransactionId())
                .setStatus(tx.getStatus())
                .setMessage("Transfer initiated. Awaiting confirmation.")
                .build());
        responseObserver.onCompleted();
    }

    @Override
    @Transactional
    public void executeTransfer(TransactionIdRequest request, StreamObserver<TransactionResponse> responseObserver) {
        Optional<TransferTransaction> txOpt = transferRepository.findById(request.getTransactionId());
        
        if (txOpt.isPresent()) {
            TransferTransaction tx = txOpt.get();
            if ("COMPLETED".equals(tx.getStatus())) {
                responseObserver.onNext(TransactionResponse.newBuilder().setStatus("ALREADY_COMPLETED").build());
                responseObserver.onCompleted();
                return;
            }

            Optional<Account> sourceOpt = accountRepository.findById(tx.getSourceAccountId());
            Optional<Account> targetOpt = accountRepository.findById(tx.getTargetAccountId());

            if (sourceOpt.isPresent() && targetOpt.isPresent()) {
                Account source = sourceOpt.get();
                Account target = targetOpt.get();

                source.setBalance(source.getBalance() - tx.getAmount());
                target.setBalance(target.getBalance() + tx.getAmount());

                accountRepository.save(source);
                accountRepository.save(target);

                tx.setStatus("COMPLETED");
                transferRepository.save(tx);

                responseObserver.onNext(TransactionResponse.newBuilder()
                        .setTransactionId(tx.getTransactionId())
                        .setStatus("SUCCESS")
                        .setMessage("Transfer executed successfully.")
                        .build());
            } else {
                responseObserver.onNext(TransactionResponse.newBuilder().setStatus("ACCOUNT_LOST").build());
            }
        } else {
            responseObserver.onNext(TransactionResponse.newBuilder().setStatus("NOT_FOUND").build());
        }
        responseObserver.onCompleted();
    }

    @Override
    @Transactional
    public void rollbackTransfer(TransactionIdRequest request, StreamObserver<TransactionResponse> responseObserver) {
        Optional<TransferTransaction> txOpt = transferRepository.findById(request.getTransactionId());
        if (txOpt.isPresent()) {
            TransferTransaction tx = txOpt.get();
            tx.setStatus("ROLLED_BACK");
            transferRepository.save(tx);
            responseObserver.onNext(TransactionResponse.newBuilder().setStatus("ROLLED_BACK").build());
        } else {
            responseObserver.onNext(TransactionResponse.newBuilder().setStatus("NOT_FOUND").build());
        }
        responseObserver.onCompleted();
    }
}
