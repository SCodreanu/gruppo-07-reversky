package it.downfall.reversky.api.notification.exception;

import it.downfall.reversky.api.common.exception.BusinessException;

/**
 * La notifica non esiste o è di un altro utente (docs/notifiche): nei due casi la risposta è la stessa.
 */
public class NotificationNotFoundException extends BusinessException {

	public NotificationNotFoundException(Integer notificationId) {
		super(Category.NOT_FOUND, "NOTIFICATION_NOT_FOUND", "Notification not found",
			"Notification " + notificationId + " does not exist");
	}
}
