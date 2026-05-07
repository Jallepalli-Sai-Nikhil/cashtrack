package com.cashtrack.auth.service;

import com.cashtrack.api.*;
import com.cashtrack.auth.entity.User;
import com.cashtrack.auth.repository.UserRepository;
import io.grpc.stub.StreamObserver;
import net.devh.boot.grpc.server.service.GrpcService;
import org.springframework.beans.factory.annotation.Autowired;
import java.util.Optional;

@GrpcService
public class AuthServiceGrpcImpl extends AuthServiceGrpc.AuthServiceImplBase {

    @Autowired
    private UserRepository userRepository;

    @Override
    public void login(LoginRequest request, StreamObserver<AuthResponse> responseObserver) {
        Optional<User> userOpt = userRepository.findByUsername(request.getUsername());
        
        if (userOpt.isPresent() && userOpt.get().getPassword().equals(request.getPassword())) {
            responseObserver.onNext(AuthResponse.newBuilder()
                    .setToken("generated-jwt-token-for-" + userOpt.get().getUsername())
                    .setStatus("SUCCESS")
                    .build());
        } else {
            responseObserver.onNext(AuthResponse.newBuilder()
                    .setStatus("FAILED")
                    .build());
        }
        responseObserver.onCompleted();
    }

    @Override
    public void validateToken(TokenRequest request, StreamObserver<AuthResponse> responseObserver) {
        // Simple validation logic for prototype
        boolean isValid = request.getToken().startsWith("generated-jwt-token-");
        responseObserver.onNext(AuthResponse.newBuilder()
                .setStatus(isValid ? "VALID" : "INVALID")
                .build());
        responseObserver.onCompleted();
    }
}