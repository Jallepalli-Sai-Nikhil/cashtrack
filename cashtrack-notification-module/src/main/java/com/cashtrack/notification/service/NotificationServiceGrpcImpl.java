package com.cashtrack.notification.service;

import com.cashtrack.api.*;
import com.cashtrack.notification.entity.Notification;
import com.cashtrack.notification.repository.NotificationRepository;
import io.grpc.stub.StreamObserver;
import net.devh.boot.grpc.server.service.GrpcService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import java.time.LocalDateTime;

@GrpcService
@PreAuthorize("hasAnyRole('BANK_ADMIN', 'CUSTOMER')")
public class NotificationServiceGrpcImpl extends NotificationServiceGrpc.NotificationServiceImplBase {

    @Autowired
    private NotificationRepository notificationRepository;

    @Override
    public void sendTransactionAlert(TransactionAlertRequest request, StreamObserver<NotificationResponse> responseObserver) {
        Notification notification = new Notification();
        notification.setAccountId(request.getAccountId());
        notification.setMessage(request.getTransactionDetails());
        notification.setType("TRANSACTION");
        notification.setChannel("APP_PUSH");
        notification.setSentAt(LocalDateTime.now());
        notificationRepository.save(notification);

        responseObserver.onNext(NotificationResponse.newBuilder()
                .setStatus("SENT")
                .build());
        responseObserver.onCompleted();
    }

    @Override
    public void getNotifications(NotificationAccountIdRequest request, StreamObserver<NotificationListResponse> responseObserver) {
        var notifications = notificationRepository.findByAccountId(request.getAccountId());
        StringBuilder sb = new StringBuilder();
        notifications.forEach(n -> sb.append(n.getMessage()).append(" | "));
        
        responseObserver.onNext(NotificationListResponse.newBuilder()
                .setNotifications(sb.toString())
                .build());
        responseObserver.onCompleted();
    }
}