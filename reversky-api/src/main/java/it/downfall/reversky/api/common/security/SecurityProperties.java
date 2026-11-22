package it.downfall.reversky.api.common.security;

import java.time.Duration;
import java.util.List;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * Configurazione della sicurezza di Spring.
 * <p>
 * I valori vengono validati all'avvio, così se uno o più valori non esistono l'applicazione non si avvia e restituisce errore.
 */
@Validated
@ConfigurationProperties(prefix = "reversky.security")
public record SecurityProperties(
    @NotBlank
    @Size(min = 32)
    String jwtSecret,

    @NotNull
    Duration accessTokenTtl,

    @NotNull
    Duration refreshTokenTtl,

    @NotNull
    Duration passwordResetTokenTtl,
    
    @NotNull
    Duration emailVerificationTokenTtl,

    @NotNull
    List<String> allowedOrigins
) {}
