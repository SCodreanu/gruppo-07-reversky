package it.downfall.reversky.api.battle.service;

/**
 * Un utente è entrato nella coda classificata (API-49). Lo pubblica {@link MatchmakingQueueService#join}; dopo il
 * commit {@code battle/job/MatchmakingJob} fa partire subito un abbinamento, senza aspettare il giro dei 2 secondi.
 * Con l'abbinamento spento ({@code reversky.matchmaking.enabled=false}) nessuno lo ascolta.
 *
 * @param userId l'utente appena entrato in coda
 */
public record QueueJoinedEvent(Integer userId) {
}
