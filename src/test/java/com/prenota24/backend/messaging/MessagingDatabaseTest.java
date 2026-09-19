package com.prenota24.backend.messaging;

import static org.assertj.core.api.Assertions.*;

import com.prenota24.backend.common.EntityNotFoundException;
import com.prenota24.backend.common.MessageSendBlockedException;
import com.prenota24.backend.common.MessagingConflictException;
import com.prenota24.backend.domain.MessageKind;
import com.prenota24.backend.dto.SendMessageRequest;
import com.prenota24.backend.service.IConversationService;
import com.prenota24.backend.service.IWhatsappWebhookService;
import com.prenota24.backend.service.impl.MessageDispatchService;
import com.prenota24.backend.service.impl.WhatsappDispatchScheduler;
import java.nio.charset.StandardCharsets;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Instant;
import java.util.HexFormat;
import java.util.UUID;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.PageRequest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.core.env.Environment;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

/** Runs only against an explicitly supplied disposable PostgreSQL database. No test transaction:
 * service transaction boundaries must work exactly as they do when invoked by HTTP controllers. */
@EnabledIfEnvironmentVariable(named = "MESSAGING_DB_TEST", matches = "true")
@ActiveProfiles("test")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, properties = {
        "whatsapp.enabled=true", "whatsapp.pilot-studio-id=10000000-0000-0000-0000-000000000001",
        "whatsapp.phone-number-id=test-phone", "whatsapp.access-token=unused-test-token",
        "whatsapp.app-secret=test-secret", "whatsapp.verify-token=test-verify",
        "whatsapp.template-name=test_template", "whatsapp.template-language=it"
})
@MockitoBean(types = {WhatsappGateway.class, WhatsappDispatchScheduler.class})
class MessagingDatabaseTest {
    private static final UUID STUDIO = UUID.fromString("10000000-0000-0000-0000-000000000001");
    private final JdbcTemplate jdbc;
    private final IConversationService conversations;
    private final IWhatsappWebhookService webhook;
    private final MessageDispatchService dispatch;
    private final Environment environment;
    private UUID user;
    private UUID client;
    private UUID appointment;

    @Autowired
    MessagingDatabaseTest(JdbcTemplate jdbc, IConversationService conversations,
                          IWhatsappWebhookService webhook, MessageDispatchService dispatch, Environment environment) {
        this.jdbc = jdbc;
        this.conversations = conversations;
        this.webhook = webhook;
        this.dispatch = dispatch;
        this.environment = environment;
    }

    @BeforeEach
    void seed() {
        assertThat(jdbc.queryForObject("SELECT current_database()", String.class))
                .as("Use a dedicated disposable messaging_v1_* database")
                .startsWith("messaging_v1_");
        jdbc.update("DELETE FROM studio WHERE id = ?", STUDIO);
        jdbc.update("INSERT INTO studio(id, name) VALUES (?, 'Messaging test')", STUDIO);
        user = UUID.randomUUID();
        client = UUID.randomUUID();
        appointment = UUID.randomUUID();
        var professional = UUID.randomUUID();
        jdbc.update("INSERT INTO app_user(id, studio_id, email, password_hash, role) VALUES (?, ?, ?, 'unused', 'ADMIN')",
                user, STUDIO, user + "@example.test");
        jdbc.update("INSERT INTO client(id, studio_id, first_name, last_name, phone) VALUES (?, ?, 'Test', 'Patient', '+39 333 111 2222')",
                client, STUDIO);
        jdbc.update("INSERT INTO professional(id, studio_id, first_name, last_name) VALUES (?, ?, 'Test', 'Professional')",
                professional, STUDIO);
        jdbc.update("INSERT INTO appointment(id, studio_id, professional_id, client_id, start_datetime, end_datetime) VALUES (?, ?, ?, ?, now(), now() + interval '1 hour')",
                appointment, STUDIO, professional, client);
    }

    @Test
    void signedInboundCommitsDeduplicatesAndReadCursorNeverRegresses() throws Exception {
        var conversation = conversations.getOrCreateForAppointment(appointment, STUDIO, user);
        String payload = incoming(UUID.randomUUID().toString(), Instant.now().minusSeconds(5));
        receive(payload);
        receive(payload);
        var messages = conversations.listMessages(conversation.id(), STUDIO, PageRequest.of(0, 50));
        assertThat(messages.getTotalElements()).isEqualTo(1);
        var first = messages.getContent().getFirst();
        conversations.markRead(conversation.id(), first.id(), STUDIO, user);
        assertThat(conversations.get(conversation.id(), STUDIO, user).unreadCount()).isZero();
        receive(incoming(UUID.randomUUID().toString(), Instant.now().minusSeconds(60)));
        assertThat(conversations.get(conversation.id(), STUDIO, user).unreadCount()).isEqualTo(1);
        var latest = conversations.listMessages(conversation.id(), STUDIO, PageRequest.of(0, 50)).getContent().getFirst();
        conversations.markRead(conversation.id(), latest.id(), STUDIO, user);
        conversations.markRead(conversation.id(), first.id(), STUDIO, user);
        assertThat(conversations.get(conversation.id(), STUDIO, user).unreadCount()).isZero();
    }

    @Test
    void tenantIsolationConsentAndIdempotencyAreEnforced() throws Exception {
        var conversation = conversations.getOrCreateForAppointment(appointment, STUDIO, user);
        assertThat(conversations.getOrCreateForAppointment(appointment, STUDIO, user).id()).isEqualTo(conversation.id());
        assertThatThrownBy(() -> conversations.get(conversation.id(), UUID.randomUUID(), user))
                .isInstanceOf(EntityNotFoundException.class);
        assertThatThrownBy(() -> conversations.getOrCreateForAppointment(appointment, UUID.randomUUID(), user))
                .isInstanceOf(EntityNotFoundException.class);
        var request = new SendMessageRequest("Test reply", appointment, UUID.randomUUID(), MessageKind.TEXT);
        assertThatThrownBy(() -> conversations.enqueue(conversation.id(), request, STUDIO, user))
                .isInstanceOf(MessageSendBlockedException.class);
        conversations.updateConsent(conversation.id(), true, STUDIO, user);
        assertThatThrownBy(() -> conversations.enqueue(conversation.id(), request, STUDIO, user))
                .isInstanceOf(MessageSendBlockedException.class);
        receive(incoming(UUID.randomUUID().toString(), Instant.now().minusSeconds(1)));
        var message = conversations.enqueue(conversation.id(), request, STUDIO, user);
        assertThat(conversations.enqueue(conversation.id(), request, STUDIO, user).id()).isEqualTo(message.id());
        assertThatThrownBy(() -> conversations.enqueue(conversation.id(),
                new SendMessageRequest("Changed", appointment, request.requestId(), MessageKind.TEXT), STUDIO, user))
                .isInstanceOf(MessagingConflictException.class);
        assertThat(jdbc.queryForObject("SELECT sender_user_id FROM conversation_message WHERE id = ?", UUID.class, message.id())).isEqualTo(user);
        jdbc.update("UPDATE client SET phone = '+39 333 999 8888' WHERE id = ?", client);
        assertThat(conversations.get(conversation.id(), STUDIO, user).whatsappOptIn()).isFalse();
        assertThat(dispatch.claim(message.id())).isEmpty();
        assertThat(jdbc.queryForObject("SELECT error_message FROM conversation_message WHERE id = ?", String.class, message.id())).isEqualTo("CLIENT_PHONE_CHANGED");
    }

    @Test
    void callbackBeforeSendCompletionCannotBeDowngraded() throws Exception {
        var conversation = conversations.getOrCreateForAppointment(appointment, STUDIO, user);
        conversations.updateConsent(conversation.id(), true, STUDIO, user);
        var message = conversations.enqueue(conversation.id(),
                new SendMessageRequest(null, null, UUID.randomUUID(), MessageKind.TEMPLATE), STUDIO, user);
        assertThat(dispatch.claim(message.id())).isPresent();
        assertThat(dispatch.claim(message.id())).isEmpty();
        String providerId = "wamid." + UUID.randomUUID();
        receive("""
                {"entry":[{"changes":[{"value":{"metadata":{"phone_number_id":"test-phone"},
                "statuses":[{"id":"%s","status":"read","recipient_id":"393331112222","biz_opaque_callback_data":"%s"}]}}]}]}
                """.formatted(providerId, message.id()));
        dispatch.complete(message.id(), WhatsappGateway.SendResult.accepted(providerId));
        assertThat(jdbc.queryForObject("SELECT status FROM conversation_message WHERE id = ?", String.class, message.id())).isEqualTo("READ");
    }

    @Test
    void httpRejectsInvalidSignaturesAndPreservesPageContractAndRoleGuard() throws Exception {
        try (var http = HttpClient.newHttpClient()) {
            String base = "http://127.0.0.1:" + environment.getRequiredProperty("local.server.port");
            var invalidSignature = http.send(HttpRequest.newBuilder(URI.create(base + "/api/webhooks/whatsapp"))
                    .header("Content-Type", "application/json").POST(HttpRequest.BodyPublishers.ofString("{}"))
                    .build(), HttpResponse.BodyHandlers.ofString());
            assertThat(invalidSignature.statusCode()).isEqualTo(401);
            var anonymous = http.send(HttpRequest.newBuilder(URI.create(base + "/api/conversations")).build(), HttpResponse.BodyHandlers.ofString());
            assertThat(anonymous.statusCode()).isEqualTo(401);
            for (String role : new String[]{"ADMIN", "PROFESSIONAL"}) {
                String jwt = io.jsonwebtoken.Jwts.builder().subject(user.toString()).claim("role", role)
                        .signWith(io.jsonwebtoken.security.Keys.hmacShaKeyFor(environment.getRequiredProperty("jwt.secret").getBytes(StandardCharsets.UTF_8)))
                        .compact();
                var response = http.send(HttpRequest.newBuilder(URI.create(base + "/api/conversations"))
                        .header("Authorization", "Bearer " + jwt).build(), HttpResponse.BodyHandlers.ofString());
                assertThat(response.statusCode()).isEqualTo(role.equals("ADMIN") ? 200 : 403);
                if (role.equals("ADMIN")) {
                    var body = new com.fasterxml.jackson.databind.ObjectMapper().readTree(response.body());
                    assertThat(body.path("content").isArray()).isTrue();
                    assertThat(body.path("page").has("totalPages")).isTrue();
                }
            }
        }
    }

    private String incoming(String id, Instant timestamp) {
        return """
                {"entry":[{"changes":[{"value":{"metadata":{"phone_number_id":"test-phone"},
                "messages":[{"id":"%s","from":"393331112222","timestamp":"%s","type":"text","text":{"body":"Test inbound"}}]}}]}]}
                """.formatted(id, timestamp.getEpochSecond());
    }

    @Test
    void invalidClientPhoneCannotCaptureAnIncomingMessage() throws Exception {
        jdbc.update("UPDATE client SET phone = '+39 invalid 333 111 2222' WHERE id = ?", client);
        String providerId = UUID.randomUUID().toString();
        receive(incoming(providerId, Instant.now().minusSeconds(1)));
        assertThat(jdbc.queryForObject("SELECT count(*) FROM conversation WHERE studio_id = ?", Long.class, STUDIO)).isZero();
        assertThat(jdbc.queryForObject("SELECT reason FROM whatsapp_inbound_quarantine WHERE provider_message_id = ?", String.class, providerId)).isEqualTo("UNMATCHED_PHONE");
    }

    private void receive(String payload) throws Exception {
        var mac = Mac.getInstance("HmacSHA256");
        mac.init(new SecretKeySpec("test-secret".getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
        webhook.receiveSigned(payload, "sha256=" + HexFormat.of().formatHex(mac.doFinal(payload.getBytes(StandardCharsets.UTF_8))));
    }
}
