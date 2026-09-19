package com.prenota24.backend.controller;

import com.prenota24.backend.service.IWhatsappWebhookService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/webhooks/whatsapp")
@RequiredArgsConstructor
public class WhatsappWebhookController {
    private final IWhatsappWebhookService webhookService;

    @GetMapping
    public ResponseEntity<String> verify(@RequestParam(name = "hub.mode", required = false) String mode,
                                         @RequestParam(name = "hub.verify_token", required = false) String token,
                                         @RequestParam(name = "hub.challenge", required = false) String challenge) {
        return ResponseEntity.ok(webhookService.verifyChallenge(mode, token, challenge));
    }

    @PostMapping
    public ResponseEntity<Void> receive(@RequestBody String rawBody,
                                        @RequestHeader(name = "X-Hub-Signature-256", required = false) String signature) {
        webhookService.receiveSigned(rawBody, signature);
        return ResponseEntity.ok().build();
    }
}
