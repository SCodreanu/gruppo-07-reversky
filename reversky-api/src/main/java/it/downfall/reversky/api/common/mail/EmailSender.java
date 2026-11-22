package it.downfall.reversky.api.common.mail;

/**
 * Invio delle email transazionali.
 * L'implementazione cambia senza toccare i service che la usano.
 */
public interface EmailSender {

	/**
	 * Invia il link per verificare l'indirizzo email.
	 *
	 * @param to indirizzo del destinatario
	 * @param rawToken token in chiaro da mettere nel link (mai salvarlo né loggarlo)
	 */
	void sendEmailVerification(String to, String rawToken);

	/**
	 * Invia il link per ripristinare la password.
	 *
	 * @param to indirizzo del destinatario
	 * @param rawToken token in chiaro da mettere nel link (mai salvarlo né loggarlo)
	 */
	void sendPasswordReset(String to, String rawToken);

}
