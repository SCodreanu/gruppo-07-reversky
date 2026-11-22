package it.downfall.reversky.api.battle.job;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.scheduling.TaskScheduler;

import it.downfall.reversky.api.battle.service.MatchmakingService;
import it.downfall.reversky.api.battle.service.QueueJoinedEvent;

/**
 * I due modi in cui parte l'abbinamento (API-49): il giro programmato e il tentativo subito dopo un ingresso in coda,
 * che va sul {@link TaskScheduler} invece di girare nel thread della richiesta. Che l'evento arrivi solo dopo il commit
 * lo garantisce {@code @TransactionalEventListener}; che l'ingresso lo pubblichi è in {@code MatchmakingControllerIT}.
 */
class MatchmakingJobTest {

	private static final Instant NOW = Instant.parse("2026-11-22T10:00:00Z");

	private final MatchmakingService matchmakingService = mock(MatchmakingService.class);
	private final TaskScheduler taskScheduler = mock(TaskScheduler.class);
	private final MatchmakingJob job = new MatchmakingJob(matchmakingService, taskScheduler,
		Clock.fixed(NOW, ZoneOffset.UTC));

	@Test
	@DisplayName("The scheduled run tries to match the whole queue")
	void matchQueue_matchesAll() {
		// Act
		job.matchQueue();

		// Assert
		verify(matchmakingService).matchAll();
	}

	@Test
	@DisplayName("Joining the queue schedules one run right now on the task scheduler, not in the request thread")
	void onQueueJoined_schedulesRunNow() {
		// Act
		job.onQueueJoined(new QueueJoinedEvent(7));

		// Assert: niente abbinamento nel thread di chi è entrato, un giro programmato per adesso
		ArgumentCaptor<Runnable> run = ArgumentCaptor.forClass(Runnable.class);
		verify(taskScheduler).schedule(run.capture(), eq(NOW));
		verify(matchmakingService, never()).matchAll();

		run.getValue().run();
		verify(matchmakingService).matchAll();
	}
}
