package com.testgen.github;

import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.KeyFactory;
import java.security.PrivateKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.util.Base64;

final class PemPrivateKeyReader {

    private static final String PKCS8_HEADER = "-----BEGIN PRIVATE KEY-----";
    private static final String PKCS8_FOOTER = "-----END PRIVATE KEY-----";

    private PemPrivateKeyReader() {}

    static PrivateKey readRsaPkcs8(String base64EncodedPem) {
        String pem = new String(Base64.getDecoder().decode(base64EncodedPem), StandardCharsets.UTF_8);

        if (!pem.contains(PKCS8_HEADER)) {
            throw new IllegalStateException(
                    "GitHub App private key must be in PKCS#8 PEM format ('BEGIN PRIVATE KEY'). "
                            + "Convert a PKCS#1 key first: openssl pkcs8 -topk8 -inform PEM -outform PEM -nocrypt "
                            + "-in original-key.pem -out pkcs8-key.pem");
        }

        String base64Der = pem
                .replace(PKCS8_HEADER, "")
                .replace(PKCS8_FOOTER, "")
                .replaceAll("\\s", "");

        byte[] derBytes = Base64.getDecoder().decode(base64Der);

        try {
            KeyFactory keyFactory = KeyFactory.getInstance("RSA");
            return keyFactory.generatePrivate(new PKCS8EncodedKeySpec(derBytes));
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException("Unable to load GitHub App private key", e);
        }
    }
}
