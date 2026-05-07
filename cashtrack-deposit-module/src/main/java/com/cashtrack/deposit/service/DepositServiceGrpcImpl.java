package com.cashtrack.deposit.service;

import com.cashtrack.api.*;
import com.cashtrack.deposit.entity.DepositTransaction;
import com.cashtrack.deposit.entity.TransactionState;
import com.cashtrack.deposit.repository.DepositRepository;
import io.grpc.stub.StreamObserver;
import net.devh.boot.grpc.server.service.GrpcService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@GrpcService
public class DepositServiceGrpcImpl extends DepositServiceGrpc.DepositServiceImplBase {

    @Autowired
    private DepositRepository depositRepository;

    @Override
    @Transactional
    public void initiateDeposit(DepositRequest request, StreamObserver<TransactionResponse> responseObserver) {
        DepositTransaction transaction = new DepositTransaction();
        transaction.setAccountId(request.getAccountId());
        transaction.setAtmId(request.getAtmId());
        transaction.setAmount(request.getAmount());
        
        TransactionState state = new TransactionState.Initiated();
        transaction.setStatus(state.getClass().getSimpleName());

        depositRepository.save(transaction);

        responseObserver.onNext(TransactionResponse.newBuilder()
                .setTransactionId(transaction.getTransactionId())
                .setStatus(transaction.getStatus())
                .setMessage("Deposit initiated successfully")
                .build());
        responseObserver.onCompleted();
    }

    @Override
    @Transactional
    public void acceptCash(TransactionIdRequest request, StreamObserver<TransactionResponse> responseObserver) {
        updateTransactionState(request.getTransactionId(), new TransactionState.Processing(), responseObserver);
    }

    @Override
    @Transactional
    public void validateDeposit(TransactionIdRequest request, StreamObserver<TransactionResponse> responseObserver) {
        updateTransactionState(request.getTransactionId(), new TransactionState.Authorized(), responseObserver);
    }

    @Override
    @Transactional
    public void confirmDeposit(TransactionIdRequest request, StreamObserver<TransactionResponse> responseObserver) {
        // Here we would typically update the account balance using the account module
        updateTransactionState(request.getTransactionId(), new TransactionState.Completed(), responseObserver);
    }

    @Override
    @Transactional
    public void reconcileDeposit(TransactionIdRequest request, StreamObserver<TransactionResponse> responseObserver) {
        updateTransactionState(request.getTransactionId(), new TransactionState.Reversed(), responseObserver);
    }

    private void updateTransactionState(String txId, TransactionState newState, StreamObserver<TransactionResponse> observer) {
        Optional<DepositTransaction> txOpt = depositRepository.findById(txId);
        
        if (txOpt.isPresent()) {
            DepositTransaction tx = txOpt.get();
            
            // Demonstrating Pattern Matching for Switch (Java 21+)
            String message = switch (newState) {
                case TransactionState.Initiated() -> "Deposit has been initiated";
                case TransactionState.Processing() -> "Cash is being counted and processed";
                case TransactionState.Authorized() -> "Deposit has been authorized by the system";
                case TransactionState.Completed() -> "Deposit successfully posted to account";
                case TransactionState.Failed() -> "Deposit failed during processing";
                case TransactionState.Reversed() -> "Deposit has been reversed and cash returned";
            };

            tx.setStatus(newState.getClass().getSimpleName());
            depositRepository.save(tx);
            
            observer.onNext(TransactionResponse.newBuilder()
                    .setTransactionId(tx.getTransactionId())
                    .setStatus(tx.getStatus())
                    .setMessage(message)
                    .build());
        } else {
            observer.onNext(TransactionResponse.newBuilder()
                    .setTransactionId(txId)
                    .setStatus("NOT_FOUND")
                    .setMessage("Transaction not found")
                    .build());
        }
        observer.onCompleted();
    }
}
