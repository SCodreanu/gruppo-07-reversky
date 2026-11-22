package it.downfall.reversky.api.common.mail;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import jakarta.validation.constraints.NotBlank;

/**
 * Configurazione delle email letta da {@code reversky.mail.*}.
 * <p>
 * Si chiama EmailProperties e non MailProperties per non confondersi con la classe di Spring Boot
 * che legge {@code spring.mail.*} (host, porta, timeout).
 *
 * @param from             mittente delle email
 * @param verifyEmailUrl   pagina del frontend per la verifica dell'email, riceve {@code ?token=...}
 * @param resetPasswordUrl pagina del frontend per il reset della password, riceve {@code ?token=...}
 */
@Validated
@ConfigurationProperties(prefix = "reversky.mail")
public record EmailProperties(

	@NotBlank
	String from,

	@NotBlank
	String verifyEmailUrl,

	@NotBlank
	String resetPasswordUrl
) {}
