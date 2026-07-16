package com.testgen.github;

import com.testgen.config.GitHubAppProperties;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import java.nio.charset.StandardCharsets;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.Base64;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class GitHubAppAuthenticatorTest {

    private static final String BASE_URL = "http://localhost";
    private static final String APP_ID = "app-123";
    private static final String INSTALLATION_ID = "installation-456";
    private static final String TOKEN_URI = BASE_URL + "/app/installations/" + INSTALLATION_ID + "/access_tokens";
    private static final String TEST_PRIVATE_KEY_PEM_BASE64 = generateTestPkcs8PemBase64();

    @Test
    void jwtClaimsReflectAppIdAndClockWindow() {
        Instant now = Instant.parse("2026-07-16T12:00:00Z");
        RestClient.Builder builder = RestClient.builder().baseUrl(BASE_URL);
        MockRestServiceServer mockServer = MockRestServiceServer.bindTo(builder).build();
        GitHubAppAuthenticator authenticator = newAuthenticator(builder, Clock.fixed(now, ZoneOffset.UTC));

        String[] capturedJwt = new String[1];
        mockServer.expect(requestTo(TOKEN_URI))
                .andExpect(method(HttpMethod.POST))
                .andExpect(request -> {
                    String auth = request.getHeaders().getFirst("Authorization");
                    assertThat(auth).startsWith("Bearer ");
                    capturedJwt[0] = auth.substring("Bearer ".length());
                })
                .andRespond(withSuccess(
                        "{\"token\":\"ghs_first\",\"expires_at\":\"2026-07-16T13:00:00Z\"}",
                        MediaType.APPLICATION_JSON));

        authenticator.getInstallationToken();

        String[] parts = capturedJwt[0].split("\\.");
        String payloadJson = new String(Base64.getUrlDecoder().decode(parts[1]), StandardCharsets.UTF_8);

        assertThat(payloadJson).contains("\"iss\":\"" + APP_ID + "\"");
        assertThat(payloadJson).contains("\"iat\":" + now.minusSeconds(60).getEpochSecond());
        assertThat(payloadJson).contains("\"exp\":" + now.plusSeconds(9 * 60).getEpochSecond());
        mockServer.verify();
    }

    @Test
    void reusesCachedTokenWithinTtl() {
        Instant now = Instant.parse("2026-07-16T12:00:00Z");
        RestClient.Builder builder = RestClient.builder().baseUrl(BASE_URL);
        MockRestServiceServer mockServer = MockRestServiceServer.bindTo(builder).build();
        GitHubAppAuthenticator authenticator = newAuthenticator(builder, Clock.fixed(now, ZoneOffset.UTC));

        mockServer.expect(requestTo(TOKEN_URI))
                .andRespond(withSuccess(
                        "{\"token\":\"ghs_cached\",\"expires_at\":\"2026-07-16T13:00:00Z\"}",
                        MediaType.APPLICATION_JSON));

        String first = authenticator.getInstallationToken();
        String second = authenticator.getInstallationToken();

        assertThat(first).isEqualTo("ghs_cached");
        assertThat(second).isEqualTo("ghs_cached");
        mockServer.verify();
    }

    @Test
    void fetchesNewTokenOnceCachedTokenIsStale() {
        MutableClock clock = new MutableClock(Instant.parse("2026-07-16T12:00:00Z"));
        RestClient.Builder builder = RestClient.builder().baseUrl(BASE_URL);
        MockRestServiceServer mockServer = MockRestServiceServer.bindTo(builder).build();
        GitHubAppAuthenticator authenticator = newAuthenticator(builder, clock);

        mockServer.expect(requestTo(TOKEN_URI))
                .andRespond(withSuccess(
                        "{\"token\":\"ghs_first\",\"expires_at\":\"2026-07-16T12:02:00Z\"}",
                        MediaType.APPLICATION_JSON));
        mockServer.expect(requestTo(TOKEN_URI))
                .andRespond(withSuccess(
                        "{\"token\":\"ghs_second\",\"expires_at\":\"2026-07-16T13:05:00Z\"}",
                        MediaType.APPLICATION_JSON));

        String first = authenticator.getInstallationToken();
        clock.advanceTo(Instant.parse("2026-07-16T12:05:00Z"));
        String second = authenticator.getInstallationToken();

        assertThat(first).isEqualTo("ghs_first");
        assertThat(second).isEqualTo("ghs_second");
        mockServer.verify();
    }

    private GitHubAppAuthenticator newAuthenticator(RestClient.Builder builder, Clock clock) {
        GitHubAppProperties properties = new GitHubAppProperties(APP_ID, TEST_PRIVATE_KEY_PEM_BASE64, INSTALLATION_ID);
        return new GitHubAppAuthenticator(builder.build(), properties, clock);
    }

    private static String generateTestPkcs8PemBase64() {
        try {
            KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance("RSA");
            keyPairGenerator.initialize(2048);
            KeyPair keyPair = keyPairGenerator.generateKeyPair();
            byte[] pkcs8Der = keyPair.getPrivate().getEncoded();

            String base64Der = Base64.getMimeEncoder(64, "\n".getBytes(StandardCharsets.UTF_8))
                    .encodeToString(pkcs8Der);
            String pem = "-----BEGIN PRIVATE KEY-----\n" + base64Der + "\n-----END PRIVATE KEY-----\n";
            return Base64.getEncoder().encodeToString(pem.getBytes(StandardCharsets.UTF_8));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }

    private static final class MutableClock extends Clock {
        private Instant instant;

        private MutableClock(Instant instant) {
            this.instant = instant;
        }

        void advanceTo(Instant instant) {
            this.instant = instant;
        }

        @Override
        public ZoneId getZone() {
            return ZoneOffset.UTC;
        }

        @Override
        public Clock withZone(ZoneId zone) {
            return this;
        }

        @Override
        public Instant instant() {
            return instant;
        }
    }
}
