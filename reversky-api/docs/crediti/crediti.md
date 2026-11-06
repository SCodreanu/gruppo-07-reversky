# Crediti virtuali

## Cosa sono

Ogni utente ha un **saldo di crediti** che usa per comprare pacchetti.
In fase 1 i crediti **non si comprano con denaro reale**: si ottengono giocando. L'acquisto con moneta reale arriverà in fase 2 (circa mese 13) ed è **fuori dal perimetro di sviluppo**.

## Dove sono salvati

Nella tabella `inventories`: **un inventario per utente** (chiave primaria `user_id`), creato alla registrazione.

```
inventories
user_id | credits
   7    |   350
```

Ogni movimento resta nello storico `credit_transactions`, così si può ricostruire il saldo e i DSG hanno i dati reali su crediti guadagnati e spesi:

```
credit_transactions
id | user_id | amount | reason        | created_at
 1 |    7    |   500  | SIGNUP        | 2026-11-04 10:00
 2 |    7    |  -200  | PACK_PURCHASE | 2026-11-04 10:05
 3 |    7    |    30  | BATTLE_WIN    | 2026-11-04 10:20
```

- Il database impedisce un saldo negativo (`chk_inventories_credits_nonnegative`).
- Anche un movimento da 0 crediti scrive la sua riga: un pacchetto gratuito risulta come `PACK_PURCHASE` con `amount` 0.
- Nello stesso inventario ci sono anche i pacchetti non ancora aperti (`pack_inventory`, vedi [pacchetti](../pacchetti/pacchetti.md)).

## Come si ottengono in fase 1

| Motivo | Quando | Valore |
|---|---|---|
| Benvenuto | alla registrazione (valore iniziale dell'inventario) | 500 **[DA CONFERMARE]** |
| Vittoria | partita classificata vinta | 30 **[DA CONFERMARE]** |
| Pareggio | partita classificata pareggiata | 10 **[DA CONFERMARE]** |
| Correzione admin | dal backoffice, registrata in `audit_logs` | variabile |

E si spendono così:

| Motivo | Quando |
|---|---|
| Acquisto pacchetto | `POST /packs/{id}/purchase` |

I valori vengono dal modello economico dei Digital Strategist e stanno nel **rule set attivo** (`rule_sets.signup_bonus`, `win_reward`, `draw_reward`),
modificabile dal [backoffice](../backoffice/backoffice.md#regole-di-gioco--rule_sets): **non** sono nel codice né in `application.properties`.

Le partite amichevoli (`FRIENDLY`) **non danno crediti**: altrimenti due amici, o una persona con due account, potrebbero scambiarsi vittorie senza limiti.

Il benvenuto si legge dal rule set attivo. Vittoria e pareggio si leggono dal rule set della partita (`battles.rule_set_id`): così un retry paga secondo le regole con cui la partita è stata preparata, anche se nel frattempo ne è stato attivato un altro.

> Un bonus giornaliero richiederebbe una colonna per ricordare l'ultimo ritiro (es. `inventories.last_daily_bonus_at`), che oggi lo schema non ha.

## Regole

1. **Il saldo non può mai andare sotto zero.** Il service lo controlla prima, il database lo garantisce comunque.
2. Ogni modifica del saldo passa **solo** da `InventoryService` (metodi `addCredits` e `spendCredits`), che scrive anche la riga di `credit_transactions` nella stessa transazione. Le righe dello storico non si modificano e non si cancellano.
3. L'operazione che usa i crediti (es. comprare un pacchetto) e la modifica del saldo stanno nella **stessa transazione**.
4. Per evitare doppie spese con richieste contemporanee, si **blocca la riga dell'inventario** prima di modificarla. Se un'operazione tocca due utenti (uno scambio, un pareggio in classificata), le due righe si bloccano in ordine di `user_id` crescente, per evitare deadlock:

```java
@Lock(LockModeType.PESSIMISTIC_WRITE)
@Query("select i from Inventory i where i.user.id = :userId")
Optional<Inventory> findByUserIdForUpdate(Integer userId);
```

```java
/**
 * Scala i crediti dall'inventario dell'utente.
 *
 * @throws InsufficientCreditsException se il saldo non basta
 */
@Transactional
public Inventory spendCredits(Integer userId, int amount, CreditReason reason) {
	Inventory inventory = inventoryRepository.findByUserIdForUpdate(userId)
			.orElseThrow(InventoryNotFoundException::new);
	if (inventory.getCredits() < amount) {
		throw new InsufficientCreditsException(amount, inventory.getCredits());
	}
	inventory.removeCredits(amount);
	creditTransactionRepository.save(new CreditTransaction(inventory.getUser(), -amount, reason));
	return inventory;
}
```

## Endpoint

| Metodo | URL | Descrizione |
|---|---|---|
| `GET` | `/api/v1/inventory` | crediti e pacchetti non aperti |

## Preparazione alla fase 2

Non si sviluppa niente di pagamento. Il **tasso di conversione crediti/euro** è definito dai DSG nel modello economico (serve per le proiezioni a 12, 24 e 48 mesi) e, quando servirà, starà nel rule set (backoffice) come gli altri valori economici, senza logica collegata.
