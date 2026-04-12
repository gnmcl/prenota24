package com.prenota24.backend.service;

import com.prenota24.backend.domain.Notification;
import com.prenota24.backend.domain.NotificationChannel;

public interface NotificationSender {
    NotificationChannel channel();
    void send(Notification notification);
}
