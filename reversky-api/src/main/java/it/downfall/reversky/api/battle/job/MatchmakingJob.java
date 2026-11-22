package it.downfall.reversky.api.battle.job;

import java.time.Clock;
import java.util.concurrent.TimeUnit;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import it.downfall.reversky.api.battle.service.MatchmakingService;
import it.downfall.reversky.api.battle.service.QueueJoinedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Fa partire l'abbinamento della coda classificata (docs/sfide, "Abbinamento", API-49): ogni 2 secondi e subito dopo
 * ogni ingresso in coda.
 * <p>
 * Si spegne con {@code reversky.matchmaking.enabled=false}: i test lo fanno
 * ({@code src/test/resources/config/application.properties}) e chiamano {@link MatchmakingService} da soli, così un
 * test della coda non viene abbinato mentre gira. Tutti e due i tentativi usano il {@link TaskScheduler} di Spring.
 */
@Slf4j
@Component
@ConditionalOnProperty(name = "reversky.matchmaking.enabled", havingValue = "true", matchIfMissing = true)
@RequiredArgsConstructor
public class MatchmakingJob {

	private final MatchmakingService matchmakingService;
	private final TaskScheduler taskScheduler;
	private final Clock clock;

	/**
	 * Ogni 2 secondi dalla fine del giro precedente: con un ritardo fisso due giri programmati non si sovrappongono,
	 * anche quando l'engine risponde lentamente. Il tentativo dopo un ingresso in coda può invece correre insieme a un
	 * giro programmato (o a un'altra istanza dell'API): lì protegge {@code SKIP LOCKED} in {@link MatchmakingService}.
	 */
	@Scheduled(fixedDelay = 2, timeUnit = TimeUnit.SECONDS)
	public void matchQueue() {
		int created = matchmakingService.matchAll();
		if (created > 0) {
			log.info("Matchmaking created {} ranked battles", created);
		}
	}

	/**
	 * Dopo il commit dell'ingresso in coda prova subito un abbinamento, in un altro thread: {@code POST
	 * /matchmaking/queue} risponde senza aspettarlo. Se la transazione dell'ingresso fallisce non parte niente.
	 */
	@TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
	public void onQueueJoined(QueueJoinedEvent event) {
		log.debug("User {} joined the ranked queue: matchmaking scheduled now", event.userId());
		taskScheduler.schedule(this::matchQueue, clock.instant());
	}
}
