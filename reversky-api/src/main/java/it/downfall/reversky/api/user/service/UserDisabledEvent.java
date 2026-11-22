package it.downfall.reversky.api.user.service;

/**
 * Un admin ha disattivato un account (API-66). Lo pubblica {@link UserAdminService#disable} dentro la sua transazione;
 * dopo il commit il canale delle notifiche chiude le connessioni aperte di quell'utente. Così {@code user/} non
 * dipende da {@code notification/}.
 *
 * @param userId l'utente disattivato
 */
public record UserDisabledEvent(Integer userId) {
}
