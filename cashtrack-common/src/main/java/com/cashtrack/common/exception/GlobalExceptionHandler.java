package com.cashtrack.common.exception;

import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import net.devh.boot.grpc.server.advice.GrpcAdvice;
import net.devh.boot.grpc.server.advice.GrpcExceptionHandler;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Component;

@Component
@GrpcAdvice
public class GlobalExceptionHandler {

    @GrpcExceptionHandler(AccessDeniedException.class)
    public Status handleAccessDeniedException(AccessDeniedException e) {
        return Status.PERMISSION_DENIED.withDescription("Access denied: " + e.getMessage()).withCause(e);
    }

    @GrpcExceptionHandler(IllegalArgumentException.class)
    public Status handleIllegalArgumentException(IllegalArgumentException e) {
        return Status.INVALID_ARGUMENT.withDescription(e.getMessage()).withCause(e);
    }

    @GrpcExceptionHandler(Exception.class)
    public Status handleException(Exception e) {
        return Status.INTERNAL.withDescription("An unexpected error occurred: " + e.getMessage()).withCause(e);
    }
}
