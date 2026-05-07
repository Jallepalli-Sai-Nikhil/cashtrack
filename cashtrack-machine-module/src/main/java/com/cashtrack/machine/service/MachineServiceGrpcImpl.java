package com.cashtrack.machine.service;

import com.cashtrack.api.*;
import com.cashtrack.machine.entity.ATMMachine;
import com.cashtrack.machine.repository.ATMRepository;
import io.grpc.stub.StreamObserver;
import net.devh.boot.grpc.server.service.GrpcService;
import org.springframework.beans.factory.annotation.Autowired;
import java.util.Optional;

@GrpcService
public class MachineServiceGrpcImpl extends MachineServiceGrpc.MachineServiceImplBase {

    @Autowired
    private ATMRepository atmRepository;

    @Override
    public void registerATM(RegisterATMRequest request, StreamObserver<ATMResponse> responseObserver) {
        ATMMachine atm = new ATMMachine();
        atm.setLocation(request.getLocation());
        atm.setCashBalance(0.0);
        atm.setStatus("ACTIVE");
        atmRepository.save(atm);

        responseObserver.onNext(ATMResponse.newBuilder()
                .setAtmId(atm.getId())
                .setStatus(atm.getStatus())
                .build());
        responseObserver.onCompleted();
    }

    @Override
    public void getATMCashStatus(ATMIdRequest request, StreamObserver<ATMCashResponse> responseObserver) {
        Optional<ATMMachine> atmOpt = atmRepository.findById(request.getAtmId());
        if (atmOpt.isPresent()) {
            responseObserver.onNext(ATMCashResponse.newBuilder()
                    .setAtmId(atmOpt.get().getId())
                    .setCashAvailable(atmOpt.get().getCashBalance())
                    .build());
        } else {
            responseObserver.onNext(ATMCashResponse.newBuilder()
                    .setAtmId(request.getAtmId())
                    .setCashAvailable(-1)
                    .build());
        }
        responseObserver.onCompleted();
    }
}