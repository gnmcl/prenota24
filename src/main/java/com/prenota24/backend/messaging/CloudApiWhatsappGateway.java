package com.prenota24.backend.messaging;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.prenota24.backend.config.WhatsappProperties;
import com.prenota24.backend.domain.MessageKind;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import org.springframework.stereotype.Component;

@Component
public class CloudApiWhatsappGateway implements WhatsappGateway {
    private final WhatsappProperties properties;
    private final ObjectMapper objectMapper;
    private final HttpClient httpClient;

    public CloudApiWhatsappGateway(WhatsappProperties properties, ObjectMapper objectMapper) {
        this.properties = properties;
        this.objectMapper = objectMapper;
        this.httpClient = HttpClient.newBuilder().connectTimeout(properties.connectTimeout()).build();
    }

    @Override
    public SendResult send(DispatchCommand command) {
        try {
            String body = objectMapper.writeValueAsString(payload(command));
            var request = HttpRequest.newBuilder()
                    .uri(URI.create("https://graph.facebook.com/" + properties.apiVersion() + "/"
                            + properties.phoneNumberId() + "/messages"))
                    .timeout(properties.requestTimeout())
                    .header("Authorization", "Bearer " + properties.accessToken())
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(body))
                    .build();
            var response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() == 429 || response.statusCode() >= 500) {
                return SendResult.unknown("Cloud API HTTP " + response.statusCode() + "; esito non verificabile");
            }
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                return SendResult.failed("Cloud API HTTP " + response.statusCode());
            }
            var id = objectMapper.readTree(response.body()).path("messages").path(0).path("id").asText(null);
            return id == null || id.isBlank()
                    ? SendResult.unknown("Cloud API ha risposto senza message id")
                    : SendResult.accepted(id);
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            return SendResult.unknown("Invio interrotto; esito non verificabile");
        } catch (IOException | RuntimeException ex) {
            return SendResult.unknown("Errore di trasporto; esito non verificabile: " + ex.getClass().getSimpleName());
        }
    }

    private ObjectNode payload(DispatchCommand command) {
        var root = objectMapper.createObjectNode();
        root.put("messaging_product", "whatsapp");
        root.put("recipient_type", "individual");
        root.put("to", command.recipientPhone());
        root.put("biz_opaque_callback_data", command.messageId().toString());
        if (command.kind() == MessageKind.TEXT) {
            root.put("type", "text");
            root.putObject("text").put("preview_url", false).put("body", command.text());
        } else {
            root.put("type", "template");
            var template = root.putObject("template");
            template.put("name", properties.templateName());
            template.putObject("language").put("code", properties.templateLanguage());
        }
        return root;
    }

}
