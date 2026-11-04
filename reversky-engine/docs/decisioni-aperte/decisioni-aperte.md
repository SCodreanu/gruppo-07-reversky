# Decisioni aperte del motore

Tutti i punti **[DA CONFERMARE]** dei documenti dell'engine in un solo posto, più le **incoerenze** trovate confrontando
il brief, la **proposta al cliente** del 2 novembre, il documento di lavoro (`docs/idee`, con il "Regolamento di gioco v1.0")
e i documenti di `docs/`.

Come si usa:

- I DSG (regolamento) e gli SWD (dettagli tecnici) decidono; chi decide è nella colonna **Decide**.
- Finché un punto è aperto, il codice implementa la **Proposta** e lo segnala con un commento `// [DA CONFERMARE] D-xx`.
- Quando un punto si chiude: aggiorna lo **Stato**, il documento di riferimento e il regolamento per i giocatori.
- **Blocca** indica le issue della [roadmap](../roadmap/roadmap.md) che non si possono chiudere senza la decisione.

Già decisi il 4 novembre: **D-04** (malus −1), **D-21** (meccaniche approvate), **D-22** (due fazioni), **D-44** (gioco in inglese), **D-45** (valori di gioco nel backoffice), **D-46** (numeri degli effetti nel codice). Il catalogo delle carte è rimandato: l'engine usa dati di prova.

Obiettivo: portare i punti `Alta` ai DSG il **9 novembre** e chiuderli entro il **16 novembre**, la milestone
*Regole approvate* della roadmap presentata al cliente.

## Regole base

| ID | Tema | Problema | Proposta | Decide | Priorità | Blocca | Stato |
|---|---|---|---|---|---|---|---|
| D-01 | Punti vita iniziali | valore iniziale del rule set `1.0` (si cambia dal backoffice, D-45) | 30; si rivede dopo `ENG-28` | DSG | Alta | — | aperto |
| D-02 | Limite di round | valore iniziale del rule set `1.0` (si cambia dal backoffice, D-45); non era scritto se conta i round **totali** o per scontro | 100 round **totali** della partita | DSG | Alta | `ENG-07` | aperto |
| D-03 | Danno al giocatore | **Incoerenza.** Brief: "differenza tra il proprio attacco e la difesa residua della creatura sconfitta". `regole-base`: DEF negativa (A 8 vs B DEF 5 → **3**). Bozza del regolamento: 8 − (−3) = **11**. La proposta al cliente mostra l'esempio (B a DEF −3) ma non dice quanti PV si perdono | DEF negativa (**3**). È la lettura del brief con "difesa residua" = DEF *prima* del colpo (8 − 5 = 3) e resta valida anche quando la morte è causata da un effetto. Il valore 11 conta il danno due volte | DSG + SWD | Alta | `ENG-06` | aperto |
| D-04 | Malus di fazione | la proposta al cliente presenta +2 ATK contro la fazione che si batte e −1 ATK contro quella che ti batte; lo schema accettava solo bonus ≥ 0 | **Deciso:** il −1 resta. Nello schema `faction_bonus.atk_bonus` può essere negativo, `def_bonus` resta ≥ 0; l'attacco effettivo non scende sotto 0. Esempi aggiornati (cambia solo l'`08`: PV 30–27) | DSG + lead SWD | Alta | — | **deciso 4 nov** |
| D-05 | Fine partita (condizioni 3–5) | proposte del team | come in `regole-base` | DSG | Alta | `ENG-07` | aperto |
| D-06 | Mazzo con sole carte non creatura rimaste | il giocatore deve pescare, nel mazzo ci sono solo Spell/Hidden/Terreni | le carte non vengono risolte; il giocatore perde per `NO_CREATURES_LEFT` | SWD | Media | `ENG-19` | aperto |
| D-07 | Evento `CARD_DRAWN` | `log-partita` lo prevede, ma l'esempio di sequenza non lo usa per le creature | `CARD_DRAWN` solo per Spell/Hidden/Terreni; per le creature basta `CREATURE_ENTERED` | SWD | Bassa | `ENG-08` | aperto |
| D-08 | Morti fuori dal round | SCP-173 e The Gnawing Witness (danno all'entrata), The Null Witness (elimina all'entrata), SCP-001 (inizio round), Nure-onna, The Unseen Matriarch e SCP-106 (dopo i danni), The Silent Shepherd e SCP-2317 (alla morte) possono uccidere fuori dal passo 4 | dopo **ogni** fase si controllano le morti con le stesse regole del passo 7; una creatura morta prima del passo 4 **non attacca** | SWD + DSG | Alta | `ENG-22` | aperto |

## Carte speciali e catalogo

| ID | Tema | Problema | Proposta | Decide | Priorità | Blocca | Stato |
|---|---|---|---|---|---|---|---|
| D-10 | Spell o Hidden pescate | **Incoerenza** nella bozza, §6: il punto 6 dice "verrà automaticamente scartata", il punto 7 "l'effetto verrà attivato". La proposta al cliente dice "attivati in automatico" | si applicano, poi si scartano | DSG | Alta | `ENG-19` | aperto |
| D-11 | Spell `DAMAGE` senza bersaglio | a inizio partita l'avversario può non avere creature in campo | si applica alla prossima creatura avversaria che entra | DSG | Media | `ENG-19` | aperto |
| D-12 | Durata dei modificatori delle Spell | non scritta | finché la creatura resta in campo | DSG | Media | `ENG-19` | aperto |
| D-13 | Burn | "somma ai tuoi valori attuali", ma quando si pesca una Spell la propria creatura è già morta: non ci sono valori attuali | la creatura rivelata viene tolta dal mazzo e le sue ATK e DEF si sommano alla **prossima creatura schierata** (più il bonus della Spell) | DSG | Media | `ENG-19` | aperto |
| D-14 | SCP-500 | Spell Scudo "immortale per il primo scontro": è nel documento ma non nel conteggio del catalogo (9 Spell: 3 Danno, 3 Scudo, 3 Burn) | tenerla fuori dal catalogo iniziale | DSG | Bassa | — | rimandato (catalogo) |
| D-15 | Attivazione delle Hidden | quando scattano? Echo Strike dice "prima di determinare l'esito dello scontro" | all'inizio del prossimo scontro del proprietario; prima le `DAMAGE`, poi le `SURVIVAL` | DSG | Alta | `ENG-20` | aperto |
| D-16 | Breaking Point | "senza scendere sotto zero": può uccidere una creatura con DEF ≤ 2? | DEF minima 0 → con DEF 0 la creatura muore | DSG | Bassa | `ENG-20` | aperto |
| D-17 | Terreno: a chi dà il bonus | solo alle creature del proprietario o a tutte quelle della fazione in campo? | solo al proprietario | DSG | Alta | `ENG-21` | aperto |
| D-18 | Terreno: effetti secondari | la bozza ha un secondo effetto per ogni Terreno (es. Echo Rift +1 ATK extra sulle Spell); `carte.md` riporta solo il bonus base | nella prima versione solo il bonus base; i secondari dopo il bilanciamento (lotto P2) | DSG | Media | `ENG-27` | aperto |
| D-19 | Due Terreni | "un nuovo Terreno sostituisce il precedente" contro "resta fino alla fine della partita" | con 1 Terreno per mazzo la sostituzione non può succedere: resta fino alla fine. Togliere la frase dalla bozza | DSG | Bassa | — | aperto |
| D-20 | Rarità Mutant | esiste una carta (Ashen Kitsune, 14/16), ma la proposta al cliente dice "ATK e DEF max: da definire"; mancano anche la posizione in `rarityOrder` e le regole per gli effetti che confrontano rarità | `rarityOrder` 6; per SCP-3000 e Bigfoot conta come superiore a Legendary | DSG | Media | — | rimandato (catalogo) |
| D-21 | Meccaniche aggiunte approvate? | la proposta al cliente chiedeva "Approvate le meccaniche aggiunte: Spell, Hidden, Terreni?" | **Deciso:** approvate dal cliente all'incontro del 2 novembre. Fanno parte del regolamento standard, come già in `AGENTS.md` | cliente | — | — | **deciso 2 nov** |
| D-22 | Carte con due fazioni | Ashen Kitsune (Mutant) riporta Theory of the Dream × Theory of the Invitation; `CardDefinition` aveva un solo `factionId` | **Deciso:** seconda fazione facoltativa (`catalog_cards.secondary_faction_id`, `secondaryFactionId` nel contratto), diversa dalla prima. La carta prende **vantaggi e svantaggi di entrambe**: si sommano i bonus di tutte le coppie di fazioni. Regola completa in [fazioni](../fazioni/fazioni.md#carte-con-due-fazioni), esempi `11`, `12`, `94` | lead SWD | Alta | `ENG-02`, `ENG-04` | **deciso 4 nov** |
| D-23 | Carte speciali (The Last Gift, edizione di Natale) | non è una delle sei rarità e non ha regole proprie | rarità `Special` fuori dalle bustine normali; per gli effetti che confrontano rarità conta come Legendary | DSG | Bassa | — | rimandato (catalogo) |
| D-24 | Nuove creature in `idee` | The Gnawing Witness, The Unseen Matriarch, The Hollow Wyrm, The Drowned Memory, The Null Witness non tornano con "15 creature per fazione" | **Rimandato:** il catalogo è ancora da definire. Fino ad allora l'engine si sviluppa e si testa con **dati di prova** ([`examples/`](../../examples/README.md), classi `TestCards`); gli effetti si implementano dal [catalogo](../effetti/catalogo-effetti.md) quando la carta è confermata | DSG | Media | — | rimandato (catalogo) |

## Effetti

Le domande sui singoli effetti sono nel [catalogo](../effetti/catalogo-effetti.md); qui ci sono quelle che cambiano il **motore**.

| ID | Tema | Problema | Proposta | Decide | Priorità | Blocca | Stato |
|---|---|---|---|---|---|---|---|
| D-30 | Ordine degli effetti contemporanei | `NULLIFY` prima, poi sfidante prima dell'avversario | come in `effetti.md` | DSG | Alta | `ENG-22` | aperto |
| D-31 | "Scontro" contro "entrata in campo" | `onEnterPlay` è descritto come "entra in campo (inizio dello scontro)", ma una creatura sopravvissuta inizia un **nuovo scontro senza rientrare**. Molti effetti dicono "per scontro" (Hidebehind, Spearfinger, SCP-682, Nure-onna, The Unseen Matriarch, Hounds) | aggiungere all'interfaccia `onCombatStart(ctx)`, chiamato a ogni nuovo scontro per entrambe le creature; gli effetti "per scontro" azzerano lì il proprio stato | SWD | Alta | `ENG-22` | aperto |
| D-32 | Durata degli annullamenti | The Rain Preacher: "per questa sfida"; SCP-2521 e Baku: "per l'intero scontro" | l'effetto annullato resta spento **finché la creatura annullata resta in campo** (stessa regola per tutti e tre) | DSG | Media | `ENG-25`, `ENG-26` | aperto |
| D-33 | "Primo attacco" | Hidebehind, Nurikabe, Jorōgumo, Living Waiting Room: il primo dello scontro o il primo della vita della creatura? Un attacco da 0 consuma l'effetto? | il primo attacco **con danno > 0** dello scontro | DSG | Media | `ENG-23` | aperto |
| D-34 | Effetti "sulla prossima creatura" | Wendigo (−2 DEF alla prossima avversaria), Spell Scudo, Spell Danno senza bersaglio | un unico meccanismo: modificatori in attesa nel `PlayerState`, applicati all'entrata | SWD | Alta | `ENG-19`, `ENG-25` | aperto |
| D-35 | Origine dei modificatori | Kitsune, The Hollow Wyrm, The Null Witness, Remnants of Reality, Echo Rift, Watching Rift e Hollow Clearing devono distinguere (e a volte sospendere) un modificatore da effetto, Spell, Hidden o Terreno | `StatModifier` con `source` (`FACTION`, `TERRAIN`, `SPELL`, `HIDDEN`, `EFFECT`), lato che l'ha causato e possibilità di sospenderlo per N round | SWD | Alta | `ENG-05` | aperto |
| D-36 | Mothman | "se sta per perdere la sfida… ribalta il risultato": è un effetto sull'intera partita, non sullo scontro. Non c'è un momento dell'interfaccia per farlo | riscrivere l'effetto in termini di scontro (es. "se muore con DEF tra 0 e −3, sopravvive con DEF 1, una volta") | DSG | Media | `ENG-25` | aperto |
| D-37 | Effetti e nomi duplicati | Jorōgumo = Hidebehind; Living Waiting Room = Nurikabe; The Gnawing Witness = SCP-173; The Unseen Matriarch ≈ Nure-onna (4 danni invece di 3). La Spell *Echo of the Forgotten* e l'effetto *Echoes of the Forgotten* (The Drowned Memory) hanno lo stesso nome | va bene per i DSG? Nel codice si usano classi base comuni; il nome doppio va cambiato | DSG | Bassa | — | rimandato (catalogo) |
| D-38 | The Null Witness | due versioni proposte; ATK e DEF da definire. *Absolute Erasure* elimina qualsiasi creatura avversaria (anche una Legendary) con una Rare. Entrambe le versioni rimuovono Terreni, ma il regolamento dice che il Terreno "non può essere distrutto" | versione *Selective Erasure*, con il Terreno avversario **sospeso** per lo scontro (come fa The Hollow Wyrm) invece che rimosso | DSG | Media | `ENG-25` | rimandato (catalogo) |

## Contratto e tecnica

| ID | Tema | Problema | Proposta | Decide | Priorità | Blocca | Stato |
|---|---|---|---|---|---|---|---|
| D-40 | Autenticazione engine in produzione | chiave condivisa nell'header | header `X-Engine-Key`, configurato da variabile d'ambiente; solo se si fa un deploy pubblico | SWD | Bassa | — | aperto |
| D-41 | Swagger nell'engine | `api-engine.md` lo richiede, ma springdoc non è in `build.gradle` | aggiungere `springdoc-openapi-starter-webmvc-ui:3.2.0`, la stessa versione dell'API | SWD (lead) | Alta | `ENG-15` | aperto |
| D-42 | Definizione di `creaturesDefeated` e `totalRounds` | non definiti nel contratto | `creaturesDefeated` = creature avversarie distrutte; `totalRounds` = round risolti (la preparazione è il round 0 e non conta) | SWD | Media | `ENG-12` | aperto |
| D-43 | Punti vita sotto zero nel risultato | il contratto non dice se `finalLifePoints` può essere negativo | nel risultato minimo 0; il log `PLAYER_DAMAGED` riporta il danno vero | SWD | Media | `ENG-11` | aperto |
| D-44 | Lingua dei nomi | il documento di lavoro e la proposta erano in italiano (*Teoria della Ferita*), le carte illustrate in inglese | **Deciso:** il gioco è **in inglese**: fazioni (*Theory of the Wound*…), carte, effetti, messaggi del log. Documenti del team e commenti nel codice restano in italiano; nei test le costanti `WOUND`, `INVITATION`, `ARCHIVE`, `DREAM` | team | — | — | **deciso 4 nov** |
| D-45 | Valori di gioco nel backoffice | punti vita e limite di round erano fissi nell'engine (`RuleConfig.defaults()`), composizione del mazzo e crediti in `application.properties` dell'API | **Deciso:** tutto ciò che non è una costante del regolamento sta nel database dell'API e si modifica dal backoffice: tabella `rule_sets` (versionata, una attiva), `rarities` (`max_per_deck`, `max_atk`, `max_def`), `faction_bonus`. L'engine riceve `rules` in ogni richiesta e non ha valori predefiniti; ogni partita salva `rule_set_id` e una copia di regole e bonus per il retry | lead SWD | Alta | `ENG-04`, `ENG-12` | **deciso 4 nov** |
| D-46 | Numeri degli effetti | i numeri dentro gli effetti (es. Kappa +3 ATK per 2 round) sono codice Java | **Deciso:** ogni effetto ha il suo tag `EFFECT_<CODICE>` e i suoi valori stanno nella classe dell'effetto; il backoffice sceglie l'effetto con il tag, non i numeri. Per cambiare un numero serve una modifica all'engine | lead SWD | — | — | **deciso 4 nov** |

## Fuori dall'engine (da segnalare al team)

- **Database:** la proposta al cliente indica PostgreSQL 18, ma il brief chiede **MySQL**. Farlo confermare esplicitamente dal cliente o dal docente prima della consegna.
- **Roadmap in `idee`:** la sezione "Roadmap di sviluppo" del documento di lavoro arriva al 25 gennaio 2027 ed è superata: la proposta al cliente usa le date del laboratorio (SAL 23 novembre, consegna 10 dicembre), come la [roadmap dell'engine](../roadmap/roadmap.md).
- **Fazioni nel database dell'API:** i dati iniziali di `factions` usano i nomi inglesi (*Theory of the Wound*…, D-44); lo schema è già aggiornato per il malus (D-04) e la seconda fazione (D-22).
