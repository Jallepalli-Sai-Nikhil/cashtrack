package com.cashtrack.account.service;

import com.cashtrack.api.AccountResponse;
import com.cashtrack.api.CreateAccountRequest;
import com.cashtrack.account.repository.AccountRepository;
import io.grpc.stub.StreamObserver;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class AccountServiceTest {

    @Mock
    private AccountRepository accountRepository;

    @Mock
    private StreamObserver<AccountResponse> responseObserver;

    @InjectMocks
    private AccountServiceGrpcImpl accountService;

    @Test
    public void testCreateAccount() {
        CreateAccountRequest request = CreateAccountRequest.newBuilder()
                .setCustomerName("John Doe")
                .setKycDetails("KYC123")
                .setInitialDeposit(100.0)
                .build();

        // Mock save to simulate ID generation
        when(accountRepository.save(any())).thenAnswer(invocation -> {
            com.cashtrack.account.entity.Account account = invocation.getArgument(0);
            account.setAccountId("test-id");
            return account;
        });

        accountService.createAccount(request, responseObserver);

        verify(accountRepository, times(1)).save(any());
        verify(responseObserver, times(1)).onNext(argThat(response -> 
            response.getAccountId().equals("test-id") && response.getStatus().equals("SUCCESS")
        ));
        verify(responseObserver, times(1)).onCompleted();
    }
}