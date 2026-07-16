package com.testgen.github;

import com.testgen.config.GitHubAppProperties;
import com.testgen.model.InstallationTokenResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.PrivateKey;
import java.security.Signature;
import java.time.Clock;
import java.time.Instant;
import java.util.Base64;

import static com.testgen.util.Constants.INSTALLATION_TOKEN_REFRESH_MARGIN;
import static com.testgen.util.Constants.JWT_HEADER_JSON;
import static com.testgen.util.Constants.JWT_IAT_BACKDATE;
import static com.testgen.util.Constants.JWT_SIGNATURE_ALGORITHM;
import static com.testgen.util.Constants.JWT_TTL;

@Component
public class GitHubAppAuthenticator {

    private static final Logger log = LoggerFactory.getLogger(GitHubAppAuthenticator.class);

    private final RestClient gitHubRestClient;
    private final GitHubAppProperties properties;
    private final Clock clock;

    private PrivateKey privateKey;
    private CachedToken cachedToken;

    @Autowired
    public GitHubAppAuthenticator(RestClient gitHubRestClient, GitHubAppProperties properties) {
        this(gitHubRestClient, properties, Clock.systemUTC());
    }

    GitHubAppAuthenticator(RestClient gitHubRestClient, GitHubAppProperties properties, Clock clock) {
        this.gitHubRestClient = gitHubRestClient;
        this.properties = properties;
        this.clock = clock;
    }

    public synchronized String getInstallationToken() {
        if (cachedToken != null && cachedToken.isValid(clock)) {
            return cachedToken.token();
        }

        InstallationTokenResponse response = requestInstallationToken();
        cachedToken = new CachedToken(response.token(), Instant.parse(response.expiresAt()));
        log.info("Fetched new GitHub App installation token, valid until {}", cachedToken.expiresAt());
        return cachedToken.token();
    }

    private InstallationTokenResponse requestInstallationToken() {
        String jwt = buildAppJwt();
        InstallationTokenResponse response = gitHubRestClient.post()
                .uri("/app/installations/{installationId}/access_tokens", properties.installationId())
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + jwt)
                .retrieve()
                .body(InstallationTokenResponse.class);

        if (response == null || response.token() == null) {
            throw new IllegalStateException("GitHub installation token response was empty");
        }
        return response;
    }

    private String buildAppJwt() {
        Instant now = Instant.now(clock);
        long iat = now.minus(JWT_IAT_BACKDATE).getEpochSecond();
        long exp = now.plus(JWT_TTL).getEpochSecond();

        String header = base64UrlEncode(JWT_HEADER_JSON.getBytes(StandardCharsets.UTF_8));
        String payload = base64UrlEncode(
                ("{\"iat\":" + iat + ",\"exp\":" + exp + ",\"iss\":\"" + properties.id() + "\"}")
                        .getBytes(StandardCharsets.UTF_8));

        String unsignedToken = header + "." + payload;
        String signature = base64UrlEncode(sign(unsignedToken.getBytes(StandardCharsets.UTF_8)));
        return unsignedToken + "." + signature;
    }

    private byte[] sign(byte[] data) {
        try {
            Signature signature = Signature.getInstance(JWT_SIGNATURE_ALGORITHM);
            signature.initSign(loadPrivateKey());
            signature.update(data);
            return signature.sign();
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException("Unable to sign GitHub App JWT", e);
        }
    }

    private PrivateKey loadPrivateKey() {
        if (privateKey == null) {
            privateKey = PemPrivateKeyReader.readRsaPkcs8(properties.privateKey());
        }
        return privateKey;
    }

    private static String base64UrlEncode(byte[] bytes) {
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private record CachedToken(String token, Instant expiresAt) {
        boolean isValid(Clock clock) {
            return Instant.now(clock).isBefore(expiresAt.minus(INSTALLATION_TOKEN_REFRESH_MARGIN));
        }
    }
}
