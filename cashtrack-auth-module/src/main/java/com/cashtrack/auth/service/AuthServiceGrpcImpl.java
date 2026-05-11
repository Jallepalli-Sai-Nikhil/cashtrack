package com.cashtrack.auth.service;

import com.cashtrack.api.*;
import com.cashtrack.auth.entity.Role;
import com.cashtrack.auth.entity.User;
import com.cashtrack.auth.repository.UserRepository;
import com.cashtrack.common.security.JwtTokenProvider;
import io.grpc.stub.StreamObserver;
import net.devh.boot.grpc.server.service.GrpcService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@GrpcService
public class AuthServiceGrpcImpl extends AuthServiceGrpc.AuthServiceImplBase {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Override
    public void register(RegisterRequest request, StreamObserver<AuthResponse> responseObserver) {
        if (userRepository.findByUsername(request.getUsername()).isPresent()) {
            responseObserver.onNext(AuthResponse.newBuilder()
                    .setStatus("USER_ALREADY_EXISTS")
                    .build());
            responseObserver.onCompleted();
            return;
        }

        User user = new User();
        user.setUsername(request.getUsername());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        
        Set<Role> roles = request.getRolesList().stream()
                .map(roleStr -> {
                    try {
                        return Role.valueOf(roleStr.toUpperCase());
                    } catch (IllegalArgumentException e) {
                        return null;
                    }
                })
                .filter(java.util.Objects::nonNull)
                .collect(Collectors.toSet());
        
        if (roles.isEmpty()) {
            roles = Set.of(Role.CUSTOMER); // Default role
        }
        
        user.setRoles(roles);
        userRepository.save(user);

        // Generate token immediately after registration
        List<String> roleNames = roles.stream().map(Enum::name).collect(Collectors.toList());
        String token = jwtTokenProvider.generateToken(user.getUsername(), roleNames);

        responseObserver.onNext(AuthResponse.newBuilder()
                .setToken(token)
                .setStatus("SUCCESS")
                .build());
        responseObserver.onCompleted();
    }

    @Override
    public void login(LoginRequest request, StreamObserver<AuthResponse> responseObserver) {
        System.out.println("Login attempt for user: " + request.getUsername());
        Optional<User> userOpt = userRepository.findByUsername(request.getUsername());

        if (userOpt.isPresent()) {
            User user = userOpt.get();
            boolean passwordMatch = passwordEncoder.matches(request.getPassword(), user.getPassword());
            System.out.println("User found. Password match: " + passwordMatch);

            if (passwordMatch) {
                List<String> roles = user.getRoles().stream()
                        .map(Enum::name)
                        .collect(Collectors.toList());

                String token = jwtTokenProvider.generateToken(user.getUsername(), roles);
                System.out.println("Token generated: " + (token != null && !token.isEmpty()));

                responseObserver.onNext(AuthResponse.newBuilder()
                        .setToken(token)
                        .setStatus("SUCCESS")
                        .build());
            } else {
                responseObserver.onNext(AuthResponse.newBuilder()
                        .setStatus("INVALID_CREDENTIALS")
                        .build());
            }
        } else {
            System.out.println("User not found: " + request.getUsername());
            responseObserver.onNext(AuthResponse.newBuilder()
                    .setStatus("USER_NOT_FOUND")
                    .build());
        }
        responseObserver.onCompleted();
    }

    @Override
    public void validateToken(TokenRequest request, StreamObserver<AuthResponse> responseObserver) {
        String token = request.getToken();
        
        // 1. Check Request Body first
        if (token == null || token.isEmpty()) {
            // 2. Fallback: Check Metadata (Headers) if body is empty
            token = com.cashtrack.common.security.JwtSecurityInterceptor.AUTHORIZATION_CONTEXT_KEY.get();
            System.out.println("Token empty in body, checking context/metadata: " + (token != null));
        }

        // Clean up the token (strip Bearer)
        if (token != null && token.startsWith("Bearer ")) {
            token = token.substring(7);
        }
        
        System.out.println("Final token for validation: " + (token != null && token.length() > 10 ? token.substring(0, 10) + "..." : "EMPTY"));
        
        boolean isValid = jwtTokenProvider.validateToken(token);
        System.out.println("Validation result: " + isValid);
        
        responseObserver.onNext(AuthResponse.newBuilder()
                .setStatus(isValid ? "VALID" : "INVALID")
                .setToken(token != null ? token : "")
                .build());
        responseObserver.onCompleted();
    }

    @Override
    public void refreshToken(TokenRequest request, StreamObserver<AuthResponse> responseObserver) {
        String token = request.getToken();
        if (token != null && token.startsWith("Bearer ")) {
            token = token.substring(7);
        }

        if (token != null && jwtTokenProvider.validateToken(token)) {
            String username = jwtTokenProvider.getUsernameFromToken(token);
            List<String> roles = jwtTokenProvider.getRolesFromToken(token);
            String newToken = jwtTokenProvider.generateToken(username, roles);

            responseObserver.onNext(AuthResponse.newBuilder()
                    .setToken(newToken)
                    .setStatus("SUCCESS")
                    .build());
        } else {
            responseObserver.onNext(AuthResponse.newBuilder()
                    .setStatus("INVALID_TOKEN")
                    .build());
        }
        responseObserver.onCompleted();
    }

    @Override
    public void logout(TokenRequest request, StreamObserver<AuthResponse> responseObserver) {
        // In a stateless JWT setup, logout is usually handled on the client side by deleting the token.
        responseObserver.onNext(AuthResponse.newBuilder()
                .setStatus("SUCCESS")
                .build());
        responseObserver.onCompleted();
    }
}
