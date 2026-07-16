package com.testgen.github;

import com.testgen.config.GitHubWebhookProperties;
import org.junit.jupiter.api.Test;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.util.HexFormat;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class WebhookSignatureValidatorTest {

    private static final String SECRET = "test-webhook-secret";
    private static final byte[] BODY = "{\"action\":\"opened\"}".getBytes(StandardCharsets.UTF_8);

    private final WebhookSignatureValidator validator =
            new WebhookSignatureValidator(new GitHubWebhookProperties(SECRET));

    @Test
    void doesNotThrowWhenSignatureMatches() {
        String validSignature = sign(BODY, SECRET);

        assertThatCode(() -> validator.validate(BODY, validSignature)).doesNotThrowAnyException();
    }

    @Test
    void throwsWebhookAuthExceptionWhenSignatureDoesNotMatch() {
        String signedWithWrongSecret = sign(BODY, "a-different-secret");

        assertThatThrownBy(() -> validator.validate(BODY, signedWithWrongSecret))
                .isInstanceOf(WebhookAuthException.class);
    }

    @Test
    void throwsWebhookAuthExceptionWhenBodyIsTampered() {
        String validSignature = sign(BODY, SECRET);
        byte[] tamperedBody = "{\"action\":\"closed\"}".getBytes(StandardCharsets.UTF_8);

        assertThatThrownBy(() -> validator.validate(tamperedBody, validSignature))
                .isInstanceOf(WebhookAuthException.class);
    }

    @Test
    void throwsWebhookAuthExceptionWhenSignatureHeaderIsMissing() {
        assertThatThrownBy(() -> validator.validate(BODY, null))
                .isInstanceOf(WebhookAuthException.class);
    }

    @Test
    void throwsWebhookAuthExceptionWhenSignatureHeaderIsMalformed() {
        assertThatThrownBy(() -> validator.validate(BODY, "not-a-valid-signature"))
                .isInstanceOf(WebhookAuthException.class);
    }

    private String sign(byte[] body, String secret) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            return "sha256=" + HexFormat.of().formatHex(mac.doFinal(body));
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException(e);
        }
    }
}
