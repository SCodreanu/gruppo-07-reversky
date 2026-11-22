package it.downfall.reversky.api.battle.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import it.downfall.reversky.api.battle.entity.BattleInviteStatus;
import it.downfall.reversky.api.battle.repository.BattleInviteRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Gli inviti di sfida ancora in attesa, per le altre funzionalità: mazzi e amicizie (docs/sfide, docs/mazzi,
 * docs/amicizie).
 * <p>
 * Dipende <b>solo</b> da {@link BattleInviteRepository}, di proposito: {@code BattleInviteService} e
 * {@code BattleService} usano già {@code DeckService} e {@code FriendshipService}, quindi questi due non possono
 * usare quelli senza creare un ciclo di bean. Sostituisce le query native su {@code battle_invites} scritte prima che
 * esistesse {@code battle/}.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PendingInviteService {

	private final BattleInviteRepository battleInviteRepository;

	/**
	 * Il mazzo è quello dello sfidante in un invito {@code PENDING}? Un mazzo così non si elimina (docs/mazzi): resta
	 * bloccato finché l'invito non viene accettato, rifiutato o ritirato.
	 */
	@Transactional(readOnly = true)
	public boolean isDeckInPendingInvite(Integer deckId) {
		return battleInviteRepository.existsByChallengerDeckIdAndStatus(deckId, BattleInviteStatus.PENDING);
	}

	/**
	 * Elimina gli inviti {@code PENDING} tra due utenti, in entrambe le direzioni, dentro la transazione del chiamante:
	 * quando un'amicizia finisce, i suoi inviti in attesa spariscono con lei (docs/amicizie, regola 7).
	 */
	@Transactional
	public void deletePendingBetween(Integer userA, Integer userB) {
		int deleted = battleInviteRepository.deleteBetween(userA, userB, BattleInviteStatus.PENDING);
		if (deleted > 0) {
			log.info("Deleted {} pending battle invites between users {} and {}", deleted, userA, userB);
		}
	}
}
