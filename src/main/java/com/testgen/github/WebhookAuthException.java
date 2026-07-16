package com.testgen.github;

import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

public class WebhookAuthException extends ResponseStatusException {

    public WebhookAuthException() {
        super(HttpStatus.UNAUTHORIZED, "Invalid webhook signature");
    }
}
