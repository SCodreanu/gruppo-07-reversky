# Esempi di simulazione

Richieste pronte per `POST /api/v1/simulations`, ognuna con il **risultato atteso calcolato a mano**.
Servono a tre cose:

1. provare l'engine a mano (curl, PowerShell, file `.http`);
2. fare da **test di accettazione** automatici (`SimulationExamplesTest`, vedi [docs/test](../docs/test/test.md));
3. capire le regole con numeri concreti prima di scrivere il codice.

> **Dati di prova (mock).** Il catalogo delle carte è ancora da definire: nomi, `id` e statistiche qui sono inventati per i test.
> Alcuni valori (es. DEF 1000 nell'esempio 07) sono fuori dai limiti di rarità apposta: l'engine non valida i limiti, lo fa l'API.
> Come nel gioco, i nomi sono in inglese.

Fazioni usate in tutti gli esempi (vedi [fazioni](../docs/fazioni/fazioni.md)):

| `id` | Fazione | Ispirata a |
|---|---|---|
| 1 | Theory of the Wound | Backrooms |
| 2 | Theory of the Invitation | Monti Appalachi |
| 3 | Theory of the Archive | SCP |
| 4 | Theory of the Dream | Yokai |

Bonus di fazione (tranne l'esempio 05): **+2 ATK** contro la fazione che si batte, **−1 ATK** contro quella che ti batte.
Wound batte Dream, Invitation batte Archive, Archive batte Wound, Dream batte Invitation; Wound–Invitation e Archive–Dream sono neutre.
Regole: il blocco `rules` di ogni richiesta, uguale in tutti gli esempi (rule set `1.0`: 30 punti vita, limite 100 round).
Nel gioco vero l'API lo legge dal rule set attivo nel database (backoffice): l'engine non ha valori predefiniti.

## Elenco

| File | Cosa verifica | Risultato atteso |
|---|---|---|
| `01-base-esempio-regolamento` | l'esempio del regolamento: danno al giocatore = DEF negativa | `CHALLENGER_WIN`, `NO_CREATURES_LEFT`, 1 round, PV 30–27 |
| `02-scontro-su-piu-round` | nessuna muore → nuovo round con la DEF ridotta | `CHALLENGER_WIN`, `NO_CREATURES_LEFT`, 3 round, PV 30–28 |
| `03-morte-simultanea` | muoiono entrambe → nessun danno; mazzi finiti | `DRAW`, `BOTH_DECKS_EMPTY`, 2 round, PV 30–30 |
| `04-bonus-fazione-attacco` | +2 e −1 ATK tra Wound e Dream; DEF finale esattamente 0 → nessun danno | `CHALLENGER_WIN`, `NO_CREATURES_LEFT`, 1 round, PV 30–30 |
| `05-bonus-fazione-difesa` | `defBonus` applicato **una volta** a inizio scontro | `DRAW`, `BOTH_DECKS_EMPTY`, 3 round, PV 30–30 |
| `06-stallo-limite-round` | due creature con ATK 0 → niente loop infinito | `DRAW`, `ROUND_LIMIT`, 100 round, PV 30–30 |
| `07-limite-round-con-vincitore` | al limite di round vince chi ha più PV | `CHALLENGER_WIN`, `ROUND_LIMIT`, 100 round, PV 30–29 |
| `08-partita-completa-seed-42` | partita vera da 10 carte: mescolamento con seed, bonus e malus, morti simultanee | `CHALLENGER_WIN`, `NO_CREATURES_LEFT`, 14 round, PV 30–27 |
| `09-effetto-hidebehind` | primo effetto (da usare quando `HIDEBEHIND` è implementato) | `OPPONENT_WIN`, `NO_CREATURES_LEFT`, 2 round, PV 29–30 |
| `10-punti-vita-a-zero` | PV ≤ 0 finisce subito, e ha la precedenza su `NO_CREATURES_LEFT` | `CHALLENGER_WIN`, `LIFE_POINTS_ZERO`, 1 round, PV 30–0 |
| `11-doppia-fazione-vantaggio` | carta con due fazioni: prende il bonus della seconda fazione | `CHALLENGER_WIN`, `NO_CREATURES_LEFT`, 1 round, PV 30–28 |
| `12-doppia-fazione-svantaggio` | carta con due fazioni: subisce anche lo svantaggio della seconda fazione | `OPPONENT_WIN`, `NO_CREATURES_LEFT`, 2 round, PV 24–30 |
| `90-errore-effetto-sconosciuto` | `effectCode` scritto male (`HIDEBEHND`) | `400`, `UNKNOWN_EFFECT` |
| `91-errore-mazzo-senza-creature` | mazzo con sola Spell | `400`, `NO_CREATURES_IN_DECK` |
| `92-errore-fazione-sconosciuta` | `factionId` 9 non è tra le fazioni | `400`, `UNKNOWN_FACTION` |
| `93-errore-bonus-fazione-non-valido` | bonus di una fazione contro se stessa | `400`, `INVALID_FACTION_BONUS` |
| `94-errore-fazioni-uguali` | `secondaryFactionId` uguale a `factionId` | `400`, `INVALID_CARD_FACTIONS` |
| `95-errore-regole-non-valide` | `rules.maxRounds` 0 | `400`, `VALIDATION_FAILED` |

Tutti i valori sono anche in [`expected-results.csv`](expected-results.csv) (tutti tranne il 09) e
[`expected-results-effects.csv`](expected-results-effects.csv) (esempi con effetti): sono i file letti dal test automatico.

`creaturesDefeated` = creature **avversarie** distrutte da quel giocatore (in una morte simultanea contano per entrambi).

## Come sono stati calcolati

**01** — SCP-131 (ATK 8, DEF 6) contro Eternal Cup (ATK 4, DEF 5), entrambe Archive.
Round 1: Eternal Cup 5 − 8 = **−3** (muore), SCP-131 6 − 4 = 2 (vive). L'avversario perde **3** PV → 27.
L'avversario deve pescare ma non ha creature, lo sfidante ne ha una in campo → `NO_CREATURES_LEFT`.

**02** — Archive Puppet (3/10) contro SCP-999 (2/7), entrambe Archive. DEF dopo ogni round: 8/4 → 6/1 → 4/**−2**. L'avversario perde 2 PV.

**03** — 2× Dullers (5/5) contro 2× Faceling (5/5), tutte Wound. Ogni round muoiono entrambe (DEF 0): nessun danno.
Dopo il round 2 nessuno ha più creature → vince chi ha più PV, qui pari → `DRAW`.
Le carte sono copie identiche: il mescolamento non cambia il risultato.

**04** — Crawlers (Wound 4/5) contro Kodama (Dream 4/6). Wound batte Dream: Crawlers attacca con 4 + 2 = 6, Kodama con 4 − 1 = 3.
Round 1: Kodama 6 − 6 = **0** (muore, ma con DEF 0 il giocatore non perde PV); Crawlers 5 − 3 = 2.
*Controprova:* senza bonus né malus muoiono entrambe al round 2 e finisce `DRAW`.

**05** — Usa una sola riga di bonus di prova: Archive → Invitation `defBonus` 3 (nessun bonus ATK, nessun malus).
SCP-1762 (Archive 3/4) parte con DEF 4 + 3 = 7 contro Squonk (Invitation 3/9).
DEF dopo ogni round: 4/6 → 1/3 → −2/0 → muoiono entrambe al round 3.
*Controprove:* senza bonus vince l'avversario in 2 round; se il bonus DEF venisse applicato a ogni round SCP-1762 non morirebbe mai.

**06** — Kasa-obake (0/5) contro Zashiki-warashi (0/5), entrambe Dream: nessun danno, si arriva al round 100 a pari PV.

**07** — Shadowless Dog (3/1000) contro 2× Limping Scarecrow (0/200), tutte Invitation.
Il primo Scarecrow muore al round 67 (200 − 201 = −1 → l'avversario perde 1 PV); il secondo al round 100 ha ancora 101 DEF.
Limite raggiunto: 30 contro 29 → vince lo sfidante.

**08** — Mazzi da 10 creature senza effetti, seed `42`. Sfidante: Wound e Archive. Avversario: Invitation e Dream.
Si mescola **prima** il mazzo dello sfidante, poi quello dell'avversario, con `Collections.shuffle(mazzo, new Random(seed))`
(vedi [mescolamento](../docs/mescolamento/mescolamento.md)). Ordine dopo il mescolamento:

- Sfidante: Yellow Hum, Eternal Cup, Crawlers, Crawlers, Wretches, Faded Tag, SCP-049, SCP-131, Faceling, Dullers
- Avversario: Wampus Cat, Kodama, Kasa-obake, Noppera-bo, Ash Raven, Temple Tanuki, Squonk, White Stag, Ash Raven, Will-o'-the-Wisp

| Round | Sfidante (ATK effettivo → DEF dopo) | Avversario (ATK effettivo → DEF dopo) | Esito |
|---|---|---|---|
| 1 | Yellow Hum 6 → −4 | Wampus Cat 7 → −1 | muoiono entrambe |
| 2 | Eternal Cup 4 → 1 | Kodama 4 → 2 | — |
| 3 | Eternal Cup 4 → −3 | Kodama 4 → −2 | muoiono entrambe |
| 4 | Crawlers 6 (+2 vs Dream) → 1 | Kasa-obake 4 (−1 vs Wound) → −1 | avversario −1 PV (29) |
| 5 | Crawlers 6 → −4 | Noppera-bo 5 (−1) → 0 | muoiono entrambe |
| 6 | Crawlers 4 → 0 | Ash Raven 5 → 0 | muoiono entrambe |
| 7 | Wretches 8 (+2 vs Dream) → 4 | Temple Tanuki 2 (−1 vs Wound) → −2 | avversario −2 PV (27) |
| 8 | Wretches 6 → 1 | Squonk 3 → 1 | — |
| 9 | Wretches 6 → −2 | Squonk 3 → −5 | muoiono entrambe |
| 10 | Faded Tag 5 → 3 | White Stag 4 → 3 | — |
| 11 | Faded Tag 5 → −1 | White Stag 4 → −2 | muoiono entrambe |
| 12 | SCP-049 6 (−1 vs Invitation) → −2 | Ash Raven 7 (+2 vs Archive) → −2 | muoiono entrambe |
| 13 | SCP-131 1 (−1) → 0 | Will-o'-the-Wisp 8 (+2) → 2 | sfidante: DEF 0, nessun danno |
| 14 | Faceling 5 → −2 | Will-o'-the-Wisp 6 → −3 | muoiono entrambe |

Dopo il round 14 l'avversario non ha più creature, lo sfidante ha ancora Dullers → `CHALLENGER_WIN`, `NO_CREATURES_LEFT`.
Con lo stesso file e seed `43` il risultato cambia (PV finali 27–23): utile per il test "seed diverso → partita diversa".

**09** — Hidebehind (Invitation 4/6, `HIDEBEHIND`) contro Faceling (Wound 7/9), fazioni neutre.
Round 1: l'attacco di Faceling viene schivato (Hidebehind resta a 6), Faceling 9 − 4 = 5.
Round 2: Hidebehind 6 − 7 = −1 (muore), Faceling 1. Lo sfidante perde 1 PV.
*Controprova:* senza effetto Hidebehind muore già al round 1.
Finché `HIDEBEHIND` non è nell'`EffectRegistry` questa richiesta deve dare `400 UNKNOWN_EFFECT`.

**10** — Archive Puppet (40/50) contro Eternal Cup (1/5), entrambe Archive: Eternal Cup finisce a 5 − 40 = −35, l'avversario perde 35 PV.
L'avversario è anche senza creature, ma le condizioni si controllano in ordine: vince la 2 (`LIFE_POINTS_ZERO`).
`finalLifePoints` è 0 e non −5 (proposta D-43); nel log `PLAYER_DAMAGED` riporta comunque 35 danni.

**11** — Ashen Fox (Mutant, Dream **e** Invitation, 6/10) contro Archive Puppet (Archive 6/6).
Si sommano tutte le coppie di fazioni (vedi [fazioni](../docs/fazioni/fazioni.md#carte-con-due-fazioni)):

- Ashen Fox attacca con 6 + (Dream → Archive 0) + (Invitation → Archive **+2**) = 8;
- Archive Puppet attacca con 6 + (Archive → Dream 0) + (Archive → Invitation **−1**) = 5.

Round 1: Archive Puppet 6 − 8 = −2 (muore, l'avversario perde 2 PV); Ashen Fox 10 − 5 = 5.
*Controprova:* con la sola fazione Dream entrambe attaccano con 6: Archive Puppet finisce a 0 e nessuno perde PV.

**12** — Ashen Fox (Dream e Invitation, 6/10) contro Kodama (Dream 6/11).

- Ashen Fox attacca con 6 + (Dream → Dream 0) + (Invitation → Dream **−1**) = 5;
- Kodama attacca con 6 + (Dream → Dream 0) + (Dream → Invitation **+2**) = 8.

DEF dopo ogni round: 2/6 → **−6**/1. Ashen Fox muore, lo sfidante perde 6 PV.
*Controprova:* con la sola fazione Dream nessun bonus: muoiono entrambe al round 2 e finisce `DRAW`.

## Log minimo atteso (esempio 01)

Gli eventi devono comparire in quest'ordine (possono essercene altri, es. `CARD_DRAWN`, vedi [decisioni aperte](../docs/decisioni-aperte/decisioni-aperte.md)).
I `message` sono in inglese, perché li leggono i giocatori.

| Round | `type` | `side` | Dati da controllare |
|---|---|---|---|
| 0 | `MATCH_STARTED` | `null` | `startingLifePoints` 30 |
| 0 | `CREATURE_ENTERED` | `CHALLENGER` | SCP-131, ATK 8, DEF 6 |
| 0 | `CREATURE_ENTERED` | `OPPONENT` | Eternal Cup, ATK 4, DEF 5 |
| 1 | `ROUND_STARTED` | `null` | round 1 |
| 1 | `ATTACK_COMPUTED` | `CHALLENGER` | totale 8 |
| 1 | `ATTACK_COMPUTED` | `OPPONENT` | totale 4 |
| 1 | `DAMAGE_DEALT` | `CHALLENGER` | 8 danni a Eternal Cup, DEF −3 |
| 1 | `DAMAGE_DEALT` | `OPPONENT` | 4 danni a SCP-131, DEF 2 |
| 1 | `CREATURE_DIED` | `OPPONENT` | Eternal Cup |
| 1 | `PLAYER_DAMAGED` | `OPPONENT` | 3 danni, 27 PV rimasti |
| 1 | `MATCH_ENDED` | `null` | `CHALLENGER_WIN`, `NO_CREATURES_LEFT` |

Questo log è anche la base del **Verbale**, il replay della sfida proposto al cliente come documento del Registro.

## Come lanciarli

Avvia l'engine (`scripts\start-engine.bat`), poi dalla cartella `reversky-engine`:

```bat
:: cmd di Windows
curl -s -X POST http://localhost:8081/api/v1/simulations -H "Content-Type: application/json" --data-binary @examples\01-base-esempio-regolamento.json
```

```powershell
# PowerShell
Invoke-RestMethod -Method Post -Uri http://localhost:8081/api/v1/simulations `
  -ContentType 'application/json' -InFile examples\01-base-esempio-regolamento.json |
  Select-Object result, endReason, totalRounds
```

Oppure apri [`simulazioni.http`](simulazioni.http) in IntelliJ IDEA o in VS Code (estensione *REST Client*) e premi ▶ accanto alla richiesta.

## Aggiungere un esempio

1. Copia il file più simile e cambia solo quello che serve.
2. Calcola il risultato **a mano** e scrivi i passaggi in questo README.
3. Aggiungi una riga a `expected-results.csv` (o `expected-results-effects.csv` se usa un effetto).
4. Se l'esempio usa carte diverse tra loro nello stesso mazzo, il risultato dipende dal seed: scrivi anche l'ordine dopo il mescolamento.
