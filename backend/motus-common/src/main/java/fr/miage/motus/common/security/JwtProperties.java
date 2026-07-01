package fr.miage.motus.common.security;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "motus.jwt")
public record JwtProperties(String secret, long expirationMs) {}
