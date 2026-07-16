package com.testgen.api;

import com.testgen.github.GitHubWebhookHandler;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class WebhookController {

    private final GitHubWebhookHandler webhookHandler;

    public WebhookController(GitHubWebhookHandler webhookHandler) {
        this.webhookHandler = webhookHandler;
    }

    @PostMapping("/webhook/github")
    public ResponseEntity<Void> receiveWebhook(
            @RequestBody byte[] rawBody,
            @RequestHeader(value = "X-Hub-Signature-256", required = false) String signature,
            @RequestHeader(value = "X-GitHub-Event", required = false) String eventType,
            @RequestHeader(value = "X-GitHub-Delivery", required = false) String deliveryId) {
        webhookHandler.handle(rawBody, signature, eventType, deliveryId);
        return ResponseEntity.ok().build();
    }
}
