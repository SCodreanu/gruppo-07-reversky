# Catalogo degli effetti

Elenco di **tutti** gli effetti delle carte, presi dal documento di lavoro (`docs/idee`, versione aggiornata) e dalla
proposta al cliente del 2 novembre, tradotti in termini del motore: codice, momenti dell'interfaccia `CreatureEffect`
(vedi [effetti](effetti.md)), stato da tenere e domande aperte. È il backlog delle issue `ENG-19`, `ENG-20`, `ENG-21`, `ENG-23`, `ENG-25`, `ENG-26`.

> **Il catalogo è ancora da definire** (D-24): carte, statistiche e testi possono cambiare. Finché non è confermato, l'engine si sviluppa e si testa con **dati di prova**.
> Ogni effetto si implementa quando la sua carta è confermata; la struttura (momenti, stato, pezzi di motore) vale comunque.

- **Carta:** nel gioco tutto è in inglese (D-44). Dove la proposta o le carte illustrate non fissano ancora il nome, quello qui è un nome di lavoro tradotto.
- **Codice:** proposta; diventa definitivo quando l'effetto è registrato in `EffectRegistry` e il backoffice lo assegna a una carta (`catalog_cards.effect_code`).
- **Lotto:** `P1` = testo chiaro, si implementa subito; `P2` = serve una decisione o un pezzo di motore in più (vedi colonna *Domande*).
- **Chi:** divisione tra i due sviluppatori — A = Theory of the Invitation + Theory of the Archive, B = Theory of the Wound + Theory of the Dream.
- I riferimenti `D-xx` rimandano alle [decisioni aperte](../decisioni-aperte/decisioni-aperte.md).
- Quando un effetto è fatto, spostalo nella tabella *Effetti implementati* di [effetti](effetti.md) con il nome del test.

Abbreviazioni: *avv.* = creatura avversaria, *scontro* = serie di round tra le stesse due creature.

## Theory of the Invitation — ispirata ai Monti Appalachi (A)

| Carta | Rarità | Codice | Effetto | Momenti | Stato | Lotto | Domande |
|---|---|---|---|---|---|---|---|
| Hidebehind | Uncommon | `HIDEBEHIND` | il primo attacco subito fa 0 danni | `modifyIncomingDamage` | `dodgeUsed` (per scontro) | P1 | D-31, D-33. Esempio `09` |
| The Silent Shepherd | Uncommon | `SILENT_SHEPHERD` | quando muore, l'avv. perde 1 DEF | `onDeath` | — | P1 | può uccidere: D-08 |
| Spearfinger | Rare | `SPEARFINGER` | nel 1° round dello scontro +2 danni | `modifyOutgoingAttack` | round dello scontro | P1 | D-31 |
| Bell Witch | Rare | `BELL_WITCH` | dal 2° round, a fine round se è viva, avv. −1 DEF | `onRoundEnd` | round dello scontro | P1 | può uccidere a fine round: D-08 |
| The Rain Preacher | Rare | `RAIN_PREACHER` | annulla l'effetto dell'avv. | `onEnterPlay` (`NULLIFY`) | — | P1 | durata: D-32 |
| Mothman | Mythic | `MOTHMAN` | se sta per perdere la sfida di ≤ 3 punti ribalta il risultato, 1 volta | — | — | P2 | non c'è un momento per la partita intera: D-36 |
| Bigfoot | Mythic | `BIGFOOT` | +2 ATK contro rarità superiore, +1 contro le altre | `modifyOutgoingAttack` | — | P1 | "potenza" = ATK? Confronto solo con `rarityOrder` |
| Wendigo | Legendary | `WENDIGO` | *Hunger and Frost:* fine round avv. −1 DEF; quando sconfigge una creatura recupera 3 DEF; la prossima avv. entra con −2 DEF | `onRoundEnd`, `onKill` | — | P2 | recupero oltre la DEF iniziale? Serve `heal()` e D-34 |

Senza effetto (per i test base): **Ash Raven** (Common, 5/4 nella proposta — "ripete le ultime parole che ha sentito" è solo testo di sapore).

## Theory of the Archive — ispirata agli SCP (A)

| Carta | Rarità | Codice | Effetto | Momenti | Stato | Lotto | Domande |
|---|---|---|---|---|---|---|---|
| SCP-173 | Uncommon | `SCP_173` | all'entrata 2 danni all'avv., prima del 1° round | `onEnterPlay` | — | P1 | può uccidere prima del round: D-08 |
| SCP-096 | Uncommon | `SCP_096` | DEF ≤ 50% dell'iniziale → +3 ATK per il resto dello scontro | `afterDamage` | `enraged` | P1 | DEF iniziale comprende i bonus? (proposta: DEF all'entrata, bonus compresi) |
| SCP-106 (Old Man) | Rare | `SCP_106` | *Dimensional Corrosion:* ogni volta che l'avv. gli fa danno, l'avv. subisce 1 danno | `afterDamage` | — | P1 | il danno di ritorno conta per il danno al giocatore: D-08 |
| SCP-682 | Rare | `SCP_682` | dopo il 1° attacco subito, −2 a ogni danno successivo dello scontro (minimo 0) | `modifyIncomingDamage` | `hitsTaken` (per scontro) | P1 | D-31 |
| SCP-2521 | Rare | `SCP_2521` | se l'avv. ha un effetto, lo annulla per lo scontro | `onEnterPlay` (`NULLIFY`) | — | P1 | D-32 |
| The Drowned Memory | Rare | `DROWNED_MEMORY` | *Echoes of the Forgotten:* all'entrata +1 DEF per ogni propria creatura già eliminata nella partita, max +4 | `onEnterPlay` | — | P1 | serve il contatore delle creature perse nel `PlayerState`; D-24 |
| The Null Witness | Rare | `NULL_WITNESS` | due versioni proposte: *Absolute Erasure* (elimina l'avv., tutti i Terreni e gli effetti temporanei, poi se stesso, nessun danno ai giocatori) oppure *Selective Erasure* (rimuove il Terreno avversario, annulla effetti e modificatori nemici, −5/−5 all'avv., poi elimina se stesso) | `onEnterPlay` | — | P2 | versione da scegliere, ATK/DEF da definire, i Terreni "non si distruggono": D-38 |
| SCP-2317 | Mythic | `SCP_2317` | quando muore, 4 danni all'avv. | `onDeath` | — | P1 | possono morire entrambe (già in `effetti.md`) |
| SCP-3000 | Mythic | `SCP_3000` | contro Rare, Mythic, Legendary: +3 ATK e metà danni subiti (per difetto) | `modifyOutgoingAttack`, `modifyIncomingDamage` | — | P1 | Mutant compresa? D-20. Soglia: `rarityOrder ≥ 3` |
| SCP-001 | Legendary | `SCP_001` | all'entrata il giocatore +3 PV; a inizio round avv. perde metà della DEF residua (per difetto) | `onEnterPlay`, `onRoundStart` | — | P2 | PV oltre il valore iniziale? Con DEF 1 l'avv. perde 0 (già in `effetti.md`); D-08 |

## Theory of the Wound — ispirata alle Backrooms (B)

| Carta | Rarità | Codice | Effetto | Momenti | Stato | Lotto | Domande |
|---|---|---|---|---|---|---|---|
| Howlers | Uncommon | `HOWLERS` | all'entrata avv. −2 ATK nei primi 2 round dello scontro | `onEnterPlay`, `onRoundStart` | round dello scontro | P1 | — |
| Living Waiting Room | Uncommon | `LIVING_WAITING_ROOM` | *Endless Corridor:* il primo attacco nemico fa metà danni (per difetto) | `modifyIncomingDamage` | `used` | P1 | uguale a Nurikabe: D-37 |
| The Gnawing Witness | Uncommon | `GNAWING_WITNESS` | *Teeth in the Walls:* all'entrata avv. −2 DEF, una volta per ingresso | `onEnterPlay` | — | P1 | uguale a SCP-173: D-37; può uccidere prima del round: D-08 |
| Skin-Stealers | Rare | `SKIN_STEALERS` | all'entrata aggiunge alla propria DEF il 25% di ATK e DEF iniziali dell'avv. "per 1 turno" | `onEnterPlay`, `onRoundEnd` | round dello scontro | P2 | turno = round o scontro? Cosa succede alla DEF quando finisce? |
| The Service Double | Rare | `SERVICE_DOUBLE` | all'entrata raddoppia l'ATK base | `onEnterPlay` | — | P1 | molto forte (Rare con ATK fino a 2× il limite): segnalare ai DSG per il bilanciamento |
| The Building Director | Rare | `BUILDING_DIRECTOR` | all'entrata avv. −20% della DEF iniziale (per difetto) | `onEnterPlay` | — | P1 | cambia anche la DEF iniziale usata dalle soglie del 50%? (proposta: no) |
| Hounds | Mythic | `HOUNDS` | DEF avv. ≤ 50% dell'iniziale → +4 ATK per il resto dello scontro | `afterDamage` | `triggered` | P1 | come SCP-096, ma guarda l'avversario |
| Deathmoths | Mythic | `DEATHMOTHS` | avv. −2 ATK solo nel 1° round | `onEnterPlay`, `onRoundStart` | round dello scontro | P1 | — |
| Smiler | Legendary | `SMILER` | ogni round in cui sopravvive +2 ATK cumulativo, max +6 | `onRoundEnd` | `bonus` | P1 | il bonus resta anche contro la prossima avv.? (proposta: sì) |
| The Hollow Wyrm | Legendary | `HOLLOW_WYRM` | *Rift Devourer:* all'entrata, se entrambi hanno un Terreno, il Terreno avversario non dà bonus per i primi 2 round dello scontro (non viene distrutto) | `onEnterPlay`, `onRoundStart` | round dello scontro | P2 | serve sospendere i modificatori di un Terreno: D-35; seconda Legendary della fazione: D-24 |

## Theory of the Dream — ispirata agli Yokai (B)

| Carta | Rarità | Codice | Effetto | Momenti | Stato | Lotto | Domande |
|---|---|---|---|---|---|---|---|
| Kappa | Uncommon | `KAPPA` | +3 ATK nei primi 2 round | `modifyOutgoingAttack` | round dello scontro | P1 | — |
| Nurikabe | Uncommon | `NURIKABE` | il primo attacco subito fa il 50% (per difetto) | `modifyIncomingDamage` | `used` | P1 | D-33, D-37 |
| Yuki-onna | Rare | `YUKI_ONNA` | all'entrata avv. −3 ATK nei primi 2 round | `onEnterPlay`, `onRoundStart` | round dello scontro | P1 | — |
| Jorōgumo | Rare | `JOROGUMO` | il primo attacco dell'avv. fa 0 danni | `modifyIncomingDamage` | `used` | P1 | uguale a Hidebehind: D-37 |
| Nure-onna | Rare | `NURE_ONNA` | DEF avv. ≤ 50% dell'iniziale → subito 3 danni extra, una volta per scontro | `afterDamage` | `triggered` (per scontro) | P1 | D-08, D-31 |
| Baku | Mythic | `BAKU` | se l'avv. ha un effetto lo annulla per lo scontro; +3 DEF una volta | `onEnterPlay` (`NULLIFY`) | — | P1 | il +3 DEF vale anche se l'avv. non ha effetti? (proposta: sì, "inoltre"); D-32 |
| Kitsune | Mythic | `KITSUNE` | *Fox Seal:* all'entrata il bonus di un effetto nemico che aumenta l'ATK scende di 2 (min 0) per tutto lo scontro | `onEnterPlay` | — | P2 | serve sapere da dove arriva un bonus ATK: D-35 |
| The Unseen Matriarch | Mythic | `UNSEEN_MATRIARCH` | *The Forbidden Face:* DEF avv. ≤ 50% dell'iniziale → subito 4 danni extra, una volta per scontro | `afterDamage` | `triggered` (per scontro) | P1 | stessa meccanica di Nure-onna (classe base comune); terza Mythic della fazione: D-24 |
| Shuten-dōji | Legendary | `SHUTEN_DOJI` | ogni volta che subisce danni +2 ATK cumulativo, max +8, fino alla morte | `afterDamage` | `bonus` | P1 | il testo dice anche "per il round successivo": vale fino alla morte (proposta) |

## Carte con due fazioni o fuori dalle rarità normali

| Carta | Rarità | Nota per il motore | Domande |
|---|---|---|---|
| Ashen Kitsune | Mutant (14/16) | **due fazioni**: Theory of the Dream × Theory of the Invitation, con vantaggi e svantaggi di entrambe ([regola](../fazioni/fazioni.md#carte-con-due-fazioni), D-22). Effetto *Ashen Seal*: per ora c'è solo il testo di sapore | testo dell'effetto da definire |
| The Last Gift | Speciale, edizione di Natale (12/14) | effetto *Do Not Open*: per ora c'è solo il testo di sapore | quale `rarity`/`rarityOrder`? D-23 |

## Spell

Si applicano appena pescate e poi vengono scartate (D-10). Bersaglio e durata: D-11, D-12, D-13.

| Carta | Sottotipo | Codice | Effetto | Lotto |
|---|---|---|---|---|
| Bite of the Reverse | `DAMAGE` | `BITE_OF_REVERSE` | avv. −3 ATK | P1 |
| Skin Fracture | `DAMAGE` | `SKIN_FRACTURE` | avv. −3 DEF | P1 |
| Dissolution | `DAMAGE` | `DISSOLUTION` | avv. −2 ATK, −2 DEF | P1 |
| Veil of the Anchored | `SHIELD` | `VEIL_OF_ANCHORED` | prossima propria creatura +3 DEF | P1 |
| Registry Pact | `SHIELD` | `REGISTRY_PACT` | +2 DEF, +1 ATK | P1 |
| Rain Armor | `SHIELD` | `RAIN_ARMOR` | +1 DEF, +2 ATK | P1 |
| SCP-500 | `SHIELD` | `SCP_500` | prossima creatura immortale per il primo scontro | P2 (D-14) |
| Call of the Rift | `BURN` | `CRACK_CALL` | Burn ×1 | P1 (D-13) |
| Reverse Fusion | `BURN` | `REVERSE_FUSION` | Burn ×1, +1 ATK | P1 (D-13) |
| Echo of the Forgotten | `BURN` | `FORGOTTEN_ECHO` | Burn ×1, +1 DEF | P1 (D-13) |

Attenzione al nome: la Spell *Echo of the Forgotten* e l'effetto *Echoes of the Forgotten* di The Drowned Memory hanno lo stesso nome (D-37).

## Hidden

Messe coperte quando pescate, si attivano da sole (D-15), poi vengono scartate.

| Carta | Sottotipo | Codice | Effetto | Lotto |
|---|---|---|---|---|
| Last Sanctuary | `SURVIVAL` | `LAST_SANCTUARY` | propria creatura +4 DEF | P1 |
| Survival Instinct | `SURVIVAL` | `SURVIVAL_INSTINCT` | +2 ATK, +2 DEF | P1 |
| Remnants of Reality | `SURVIVAL` | `REMNANTS_OF_REALITY` | annulla fino a 3 punti di penalità causate dall'avversario | P2 (D-35) |
| Hostile Presence | `DAMAGE` | `HOSTILE_PRESENCE` | avv. −4 ATK | P1 |
| Echo Strike | `DAMAGE` | `ECHO_STRIKE` | avv. −2 ATK, −2 DEF | P1 |
| Breaking Point | `DAMAGE` | `BREAKING_POINT` | avv. −5 DEF se ha almeno 5 DEF, altrimenti fino a −2 | P1 (D-16) |

## Terreni

Una Crepa per fazione. Restano in gioco fino alla fine. Bonus base `P1`, effetto secondario `P2` (D-17, D-18).

| Carta | Fazione | Codice | Bonus base | Effetto secondario |
|---|---|---|---|---|
| Echo Rift (Crepa Eco) | Theory of the Wound | `ECHO_RIFT` | +1 ATK | +1 ATK extra quando una Spell dà ATK, una volta per scontro |
| Hollow Clearing (Crepa Radura) | Theory of the Invitation | `HOLLOW_CLEARING` | +2 DEF | la prima penalità alla DEF di ogni scontro −1 |
| Watching Rift (Crepa Occhio) | Theory of the Archive | `WATCHING_RIFT` | +1 ATK, +1 DEF | la prima Spell Danno di ogni scontro −1 punto |
| Devouring Abyss (Crepa Cavità) | Theory of the Dream | `DEVOURING_ABYSS` | +1 ATK | avv. −1 DEF la prima volta che lo affronta |

## Pezzi di motore richiesti dagli effetti

Prima degli effetti di Spell, Hidden e Terreni serve il registro (`ENG-22`); prima di quelli delle creature serve anche `CreatureEffect` (`ENG-23`); prima dei `P2`, questi pezzi in più:

| Pezzo | Serve a | Decisione |
|---|---|---|
| `onCombatStart(ctx)` nell'interfaccia e azzeramento dello stato "per scontro" | Hidebehind, Spearfinger, SCP-682, Nure-onna, The Unseen Matriarch, Hounds, Howlers… | D-31 |
| Controllo delle morti dopo ogni fase | SCP-173, The Gnawing Witness, The Null Witness, SCP-001, SCP-106, Nure-onna, Bell Witch, Wendigo, The Silent Shepherd, SCP-2317 | D-08 |
| Modificatori in attesa nel `PlayerState` | Spell Scudo, Spell Danno senza bersaglio, Wendigo | D-34 |
| `StatModifier` con origine, sospendibile | Kitsune, The Hollow Wyrm, The Null Witness, Remnants of Reality, Terreni secondari | D-35 |
| Contatore delle creature perse nel `PlayerState` | The Drowned Memory | — |
| `CreatureInPlay.heal()` e `PlayerState.heal()` | Wendigo, SCP-001 | limiti da decidere |
| Classi base per effetti uguali | "primo attacco ridotto" (Hidebehind, Jorōgumo, Nurikabe, Living Waiting Room); "danni extra sotto il 50%" (Nure-onna, The Unseen Matriarch); "danni all'entrata" (SCP-173, The Gnawing Witness) | D-37 |
