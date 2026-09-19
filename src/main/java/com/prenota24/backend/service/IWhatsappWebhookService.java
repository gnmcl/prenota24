package com.prenota24.backend.service;

public interface IWhatsappWebhookService {
    String verifyChallenge(String mode, String token, String challenge);
    void receiveSigned(String rawBody, String signature);
}
