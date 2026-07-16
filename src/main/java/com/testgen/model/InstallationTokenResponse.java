package com.testgen.model;

import com.fasterxml.jackson.annotation.JsonProperty;

public record InstallationTokenResponse(String token, @JsonProperty("expires_at") String expiresAt) {
}
