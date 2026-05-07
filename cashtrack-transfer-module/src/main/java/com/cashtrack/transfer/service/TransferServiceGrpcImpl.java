package com.cashtrack.transfer.service;

import com.cashtrack.api.*;
import com.cashtrack.transfer.entity.TransferTransaction;
import com.cashtrack.transfer.repository.TransferRepository;
import io.grpc.stub.StreamObserver;
import net.devh.boot.grpc.server.service.GrpcService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@GrpcService
public class TransferServiceGrpcImpl extends TransferServiceGrpc.TransferServiceImplBase {

    @Autowired
    private TransferRepository transferRepository;

    @Override
    @Transactional
    public void initiateTransfer(TransferRequest request, StreamObserver<TransactionResponse> responseObserver) {
        TransferTransaction tx = new TransferTransaction();
        tx.setSourceAccountId(request.getSourceAccountId());
        tx.setTargetAccountId(request.getTargetAccountId());
        tx.setAmount(request.getAmount());
        tx.setStatus("INITIATED");

        transferRepository.save(tx);

        responseObserver.onNext(TransactionResponse.newBuilder()
                .setTransactionId(tx.getTransactionId())
                .setStatus(tx.getStatus())
                .setMessage("Transfer initiated")
                .build());
        responseObserver.onCompleted();
    }

    @Override
    @Transactional
    public void validateTransfer(TransactionIdRequest request, StreamObserver<TransactionResponse> responseObserver) {
        updateStatus(request.getTransactionId(), "VALIDATED", responseObserver);
    }

    @Override
    @Transactional
    public void executeTransfer(TransactionIdRequest request, StreamObserver<TransactionResponse> responseObserver) {
        updateStatus(request.getTransactionId(), "EXECUTED", responseObserver);
    }

    @Override
    @Transactional
    public void confirmTransfer(TransactionIdRequest request, StreamObserver<TransactionResponse> responseObserver) {
        updateStatus(request.getTransactionId(), "COMPLETED", responseObserver);
    }

    @Override
    @Transactional
    public void rollbackTransfer(TransactionIdRequest request, StreamObserver<TransactionResponse> responseObserver) {
        updateStatus(request.getTransactionId(), "ROLLBACKED", responseObserver);
    }

    private void updateStatus(String txId, String status, StreamObserver<TransactionResponse> observer) {
        Optional<TransferTransaction> txOpt = transferRepository.findById(txId);
        
        if (txOpt.isPresent()) {
            TransferTransaction tx = txOpt.get();
            tx.setStatus(status);
            transferRepository.save(tx);
            
            observer.onNext(TransactionResponse.newBuilder()
                    .setTransactionId(tx.getTransactionId())
                    .setStatus(tx.getStatus())
                    .setMessage("Status updated to " + status)
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
