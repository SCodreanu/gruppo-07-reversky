package it.downfall.reversky.api.common.mail;

import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;

import it.downfall.reversky.api.common.security.SecurityProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Invia le email tramite SMTP. In sviluppo il server è Mailpit (http://localhost:8025),
 * che le cattura senza spedirle davvero.
 * <p>
 * Se l'invio fallisce l'errore viene solo scritto nel log: registrazione e reset non vengono
 * annullati, l'utente può chiedere un nuovo link.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class SmtpEmailSender implements EmailSender {

	private final JavaMailSender mailSender;
	private final EmailProperties emailProperties;
	private final SecurityProperties securityProperties;

	@Override
	public void sendEmailVerification(String to, String rawToken) {
		String link = buildLink(emailProperties.verifyEmailUrl(), rawToken);
		String text = """
			Hi,

			to complete your Reversky registration, please confirm your email address by opening this link:

			%s

			If you did not sign up, you can ignore this email.
			""".formatted(link);

		send(to, "Confirm your email address", text);
	}

	@Override
	public void sendPasswordReset(String to, String rawToken) {
		String link = buildLink(emailProperties.resetPasswordUrl(), rawToken);
		long minutes = securityProperties.passwordResetTokenTtl().toMinutes();

		String text = """
			Hi,

			you asked to reset the password of your Reversky account. Open this link:

			%s

			The link expires in %d minutes.
			If it wasn't you, you can ignore this email: your password will not change.
			""".formatted(link, minutes);

		send(to, "Reset your password", text);
	}

	private void send(String to, String subject, String text) {
		SimpleMailMessage message = new SimpleMailMessage();

		message.setFrom(emailProperties.from());
		message.setTo(to);
		message.setSubject(subject);
		message.setText(text);

		try {
			mailSender.send(message);
			log.info("Email \"{}\" sent to {}", subject, maskEmail(to));
		} catch (MailException e) {
			log.error("Failed to send email \"{}\" to {}: {}", subject, maskEmail(to), e.getClass().getSimpleName());
		}
	}

	// Aggiunge ?token=... all'indirizzo della pagina del frontend
	private String buildLink(String pageUrl, String rawToken) {
		return UriComponentsBuilder
			.fromUriString(pageUrl)
			.queryParam("token", rawToken)
			.toUriString();
	}

	// "mario@example.com" -> "m***@example.com": l'email è un dato personale (CodingRequirements §10)
	private String maskEmail(String email) {
		int at = email.indexOf('@');
		if (at <= 1) {
			return "***";
		}

		return email.charAt(0) + "***" + email.substring(at);
	}
}
