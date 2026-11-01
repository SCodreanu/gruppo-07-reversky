# Crediti virtuali

## Cosa sono

Ogni utente ha un **saldo di crediti** che usa per comprare pacchetti.
In fase 1 i crediti **non si comprano con denaro reale**: si ottengono giocando. L'acquisto con moneta reale arriverà in fase 2 (circa mese 13) ed è **fuori dal perimetro di sviluppo**.

## Dove sono salvati

Nella tabella `inventories`: **un inventario per utente** (`user_id` univoco), creato alla registrazione.

```
inventories
id | user_id | credits
 1 |    7    |   350
```

- Il database impedisce un saldo negativo (`chk_inventories_credits_nonnegative`).
- Nello stesso inventario ci sono anche i pacchetti non ancora aperti (`pack_inventory`, vedi [pacchetti](../pacchetti/pacchetti.md)).

## Come si ottengono in fase 1

| Motivo | Quando | Valore |
|---|---|---|
| Benvenuto | alla registrazione (valore iniziale dell'inventario) | 500 **[DA CONFERMARE]** |
| Vittoria | sfida vinta | 30 **[DA CONFERMARE]** |
| Pareggio | sfida pareggiata | 10 **[DA CONFERMARE]** |
| Correzione admin | dal backoffice, registrata in `audit_logs` | variabile |

E si spendono così:

| Motivo | Quando |
|---|---|
| Acquisto pacchetto | `POST /packs/{id}/purchase` |

I valori vengono dal modello economico dei Digital Strategist e stanno in `application.properties`, **non nel codice**:

```properties
reversky.credits.signup-bonus=500
reversky.credits.win-reward=30
reversky.credits.draw-reward=10
```

> Un bonus giornaliero richiederebbe una colonna per ricordare l'ultimo ritiro (es. `inventories.last_daily_bonus_at`), che oggi lo schema non ha.

## Regole

1. **Il saldo non può mai andare sotto zero.** Il service lo controlla prima, il database lo garantisce comunque.
2. Ogni modifica del saldo passa **solo** da `InventoryService` (metodi `addCredits` e `spendCredits`).
3. L'operazione che usa i crediti (es. comprare un pacchetto) e la modifica del saldo stanno nella **stessa transazione**.
4. Per evitare doppie spese con richieste contemporanee, si **blocca la riga dell'inventario** prima di modificarla:

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
public Inventory spendCredits(Integer userId, int amount) {
	Inventory inventory = inventoryRepository.findByUserIdForUpdate(userId)
			.orElseThrow(InventoryNotFoundException::new);
	if (inventory.getCredits() < amount) {
		throw new InsufficientCreditsException(amount, inventory.getCredits());
	}
	inventory.removeCredits(amount);
	return inventory;
}
```

## Endpoint

| Metodo | URL | Descrizione |
|---|---|---|
| `GET` | `/api/v1/inventory` | crediti e pacchetti non aperti |

## Preparazione alla fase 2

Non si sviluppa niente di pagamento. Il **tasso di conversione crediti/euro** è definito dai DSG nel modello economico (serve per le proiezioni a 12, 24 e 48 mesi) e può stare in configurazione, senza logica collegata.
