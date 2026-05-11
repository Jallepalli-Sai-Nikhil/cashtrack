package com.cashtrack.withdrawal.service;

import com.cashtrack.api.*;
import com.cashtrack.account.entity.Account;
import com.cashtrack.account.repository.AccountRepository;
import com.cashtrack.withdrawal.entity.WithdrawalTransaction;
import com.cashtrack.withdrawal.repository.WithdrawalRepository;
import io.grpc.stub.StreamObserver;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class WithdrawalServiceTest {

    @Mock
    private WithdrawalRepository withdrawalRepository;

    @Mock
    private AccountRepository accountRepository;

    @Mock
    private StreamObserver<TransactionResponse> responseObserver;

    @InjectMocks
    private WithdrawalServiceGrpcImpl withdrawalService;

    private Account testAccount;
    private WithdrawalTransaction testTx;

    @BeforeEach
    void setUp() {
        testAccount = new Account();
        testAccount.setAccountId("ACC-123");
        testAccount.setBalance(1000.0);

        testTx = new WithdrawalTransaction();
        testTx.setTransactionId("TX-456");
        testTx.setAccountId("ACC-123");
        testTx.setAmount(200.0);
        testTx.setStatus("INITIATED");
    }

    @Test
    void initiateWithdrawal_Success() {
        when(accountRepository.findById("ACC-123")).thenReturn(Optional.of(testAccount));
        when(withdrawalRepository.save(any())).thenReturn(testTx);

        WithdrawalRequest request = WithdrawalRequest.newBuilder()
                .setAccountId("ACC-123")
                .setAmount(200.0)
                .setAtmId("ATM-001")
                .build();

        withdrawalService.initiateWithdrawal(request, responseObserver);

        verify(withdrawalRepository).save(any());
        verify(responseObserver).onNext(argThat(r -> r.getStatus().equals("INITIATED")));
        verify(responseObserver).onCompleted();
    }

    @Test
    void initiateWithdrawal_InsufficientFunds() {
        testAccount.setBalance(100.0);
        when(accountRepository.findById("ACC-123")).thenReturn(Optional.of(testAccount));

        WithdrawalRequest request = WithdrawalRequest.newBuilder()
                .setAccountId("ACC-123")
                .setAmount(200.0)
                .build();

        withdrawalService.initiateWithdrawal(request, responseObserver);

        verify(responseObserver).onNext(argThat(r -> r.getStatus().equals("INSUFFICIENT_FUNDS")));
        verify(withdrawalRepository, never()).save(any());
    }

    @Test
    void confirmWithdrawal_Success() {
        when(withdrawalRepository.findById("TX-456")).thenReturn(Optional.of(testTx));
        when(accountRepository.findById("ACC-123")).thenReturn(Optional.of(testAccount));

        TransactionIdRequest request = TransactionIdRequest.newBuilder()
                .setTransactionId("TX-456")
                .build();

        withdrawalService.confirmWithdrawal(request, responseObserver);

        verify(accountRepository).save(argThat(a -> a.getBalance() == 800.0));
        verify(withdrawalRepository).save(argThat(t -> t.getStatus().equals("COMPLETED")));
        verify(responseObserver).onNext(argThat(r -> r.getStatus().equals("SUCCESS")));
    }
}
