package com.cashtrack.session.service;

import com.cashtrack.api.*;
import com.cashtrack.session.entity.AtmSession;
import com.cashtrack.session.repository.SessionRepository;
import io.grpc.stub.StreamObserver;
import net.devh.boot.grpc.server.service.GrpcService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

@GrpcService
public class SessionServiceGrpcImpl extends SessionServiceGrpc.SessionServiceImplBase {

    @Autowired
    private SessionRepository sessionRepository;

    @Override
    @Transactional
    public void initiateSession(InitiateSessionRequest request, StreamObserver<SessionResponse> responseObserver) {
        AtmSession session = new AtmSession();
        session.setAtmId(request.getAtmId());
        session.setCardNumber(request.getCardNumber());
        session.setStatus("INITIATED");

        sessionRepository.save(session);

        responseObserver.onNext(SessionResponse.newBuilder()
                .setSessionId(session.getSessionId())
                .setStatus(session.getStatus())
                .setToken("")
                .build());
        responseObserver.onCompleted();
    }

    @Override
    @Transactional
    public void validatePIN(ValidatePINRequest request, StreamObserver<SessionResponse> responseObserver) {
        Optional<AtmSession> sessionOpt = sessionRepository.findById(request.getSessionId());

        if (sessionOpt.isPresent()) {
            AtmSession session = sessionOpt.get();
            // Validate pin logic here (mocked as true if not empty)
            if (!request.getPin().isEmpty() && session.getExpiresAt().isAfter(LocalDateTime.now())) {
                session.setStatus("AUTHENTICATED");
                session.setToken(UUID.randomUUID().toString()); // Mock JWT token
                sessionRepository.save(session);

                responseObserver.onNext(SessionResponse.newBuilder()
                        .setSessionId(session.getSessionId())
                        .setStatus(session.getStatus())
                        .setToken(session.getToken())
                        .build());
            } else {
                session.setStatus("TERMINATED");
                sessionRepository.save(session);

                responseObserver.onNext(SessionResponse.newBuilder()
                        .setSessionId(session.getSessionId())
                        .setStatus("FAILED")
                        .setToken("")
                        .build());
            }
        } else {
            responseObserver.onNext(SessionResponse.newBuilder()
                    .setSessionId(request.getSessionId())
                    .setStatus("NOT_FOUND")
                    .setToken("")
                    .build());
        }
        responseObserver.onCompleted();
    }

    @Override
    @Transactional
    public void authenticateCard(AuthCardRequest request, StreamObserver<SessionResponse> responseObserver) {
        responseObserver.onNext(SessionResponse.newBuilder()
                .setSessionId("")
                .setStatus("CARD_AUTHENTICATED")
                .setToken("")
                .build());
        responseObserver.onCompleted();
    }

    @Override
    @Transactional
    public void refreshSession(SessionIdRequest request, StreamObserver<SessionResponse> responseObserver) {
        Optional<AtmSession> sessionOpt = sessionRepository.findById(request.getSessionId());

        if (sessionOpt.isPresent()) {
            AtmSession session = sessionOpt.get();
            if (session.getStatus().equals("AUTHENTICATED")) {
                session.setExpiresAt(LocalDateTime.now().plusMinutes(5));
                sessionRepository.save(session);

                responseObserver.onNext(SessionResponse.newBuilder()
                        .setSessionId(session.getSessionId())
                        .setStatus("REFRESHED")
                        .setToken(session.getToken())
                        .build());
                responseObserver.onCompleted();
                return;
            }
        }
        
        responseObserver.onNext(SessionResponse.newBuilder()
                .setSessionId(request.getSessionId())
                .setStatus("FAILED")
                .setToken("")
                .build());
        responseObserver.onCompleted();
    }

    @Override
    @Transactional
    public void terminateSession(SessionIdRequest request, StreamObserver<SessionResponse> responseObserver) {
        Optional<AtmSession> sessionOpt = sessionRepository.findById(request.getSessionId());

        if (sessionOpt.isPresent()) {
            AtmSession session = sessionOpt.get();
            session.setStatus("TERMINATED");
            session.setToken("");
            sessionRepository.save(session);

            responseObserver.onNext(SessionResponse.newBuilder()
                    .setSessionId(session.getSessionId())
                    .setStatus("TERMINATED")
                    .setToken("")
                    .build());
        } else {
            responseObserver.onNext(SessionResponse.newBuilder()
                    .setSessionId(request.getSessionId())
                    .setStatus("NOT_FOUND")
                    .setToken("")
                    .build());
        }
        responseObserver.onCompleted();
    }
}
