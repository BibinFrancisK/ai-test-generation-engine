package com.testgen.github;

import com.testgen.config.GitHubWebhookProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.util.HexFormat;

import static com.testgen.util.Constants.HMAC_ALGORITHM;
import static com.testgen.util.Constants.SIGNATURE_PREFIX;

@Component
public class WebhookSignatureValidator {

    private static final Logger log = LoggerFactory.getLogger(WebhookSignatureValidator.class);

    private final GitHubWebhookProperties properties;

    public WebhookSignatureValidator(GitHubWebhookProperties properties) {
        this.properties = properties;
    }

    public void validate(byte[] rawBody, String signatureHeader) {
        if (signatureHeader == null || !signatureHeader.startsWith(SIGNATURE_PREFIX)) {
            log.warn("Webhook rejected: signature header missing or malformed");
            throw new WebhookAuthException();
        }

        String expectedSignature = computeSignature(rawBody);
        boolean matches = MessageDigest.isEqual(
                expectedSignature.getBytes(StandardCharsets.US_ASCII),
                signatureHeader.getBytes(StandardCharsets.US_ASCII));

        if (!matches) {
            log.warn("Webhook rejected: signature does not match");
            throw new WebhookAuthException();
        }
    }

    private String computeSignature(byte[] rawBody) {
        try {
            Mac mac = Mac.getInstance(HMAC_ALGORITHM);
            mac.init(new SecretKeySpec(properties.secret().getBytes(StandardCharsets.UTF_8), HMAC_ALGORITHM));
            return SIGNATURE_PREFIX + HexFormat.of().formatHex(mac.doFinal(rawBody));
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException("Unable to compute webhook signature", e);
        }
    }
}
