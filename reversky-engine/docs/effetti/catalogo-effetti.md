# Catalogo degli effetti

Elenco di **tutti** gli effetti delle 120 carte, tradotti in termini del motore: codice, momenti dell'interfaccia `CreatureEffect`
(vedi [effetti](effetti.md)), stato da tenere e domande aperte. È il backlog delle issue `ENG-19`, `ENG-20`, `ENG-21`, `ENG-23`, `ENG-25`, `ENG-26`, `ENG-33`.

**Fonte:** il *Catalogo completo 120 carte* (bozza di game design del 9 novembre), che **sostituisce** il documento di lavoro (`docs/idee`) e la proposta al cliente del 2 novembre.
La struttura (quante carte per tipo, fazione e rarità) è decisa (D-24); i testi degli effetti sono in **revisione** e possono ancora cambiare, le statistiche ATK/DEF non sono ancora definite.
Finché la revisione non è chiusa, l'engine si sviluppa e si testa con **dati di prova** ([`examples/`](../../examples/README.md)).

- **Carta:** nome come nel catalogo; tra parentesi il nome inglese di lavoro usato finora, dove esiste. Il gioco è in inglese (D-44): i nomi definitivi li danno i DSG.
- **Effetto:** testo del catalogo, riassunto senza cambiarne il senso.
- **Codice:** proposta; diventa definitivo quando l'effetto è registrato in `EffectRegistry` e il backoffice lo assegna a una carta (`catalog_cards.effect_code`). I numeri stanno nella classe dell'effetto (D-46).
- **Lotto:** `P1` = testo chiaro, si implementa appena la carta è confermata; `P2` = serve una decisione o un pezzo di motore in più (vedi colonna *Domande*).
- **Chi:** divisione tra i due sviluppatori — A = Theory of the Invitation + Theory of the Archive, B = Theory of the Wound + Theory of the Dream.
- I riferimenti `D-xx` rimandano alle [decisioni aperte](../decisioni-aperte/decisioni-aperte.md).
- Quando un effetto è fatto, spostalo nella tabella *Effetti implementati* di [effetti](effetti.md) con il nome del test.

Abbreviazioni: *avv.* = creatura avversaria; *scontro* = serie di round tra le stesse due creature (il catalogo dice anche "combattimento");
*metà DEF* = DEF ≤ 50% della DEF iniziale (quella all'entrata in campo, bonus compresi), come per SCP-096.

## Quadro numerico

| Tipo | Carte |
|---|---|
| Creature | 92: 22 Invitation, 22 Wound, 23 Archive, 23 Dream, 2 Mutant con due fazioni |
| Spell | 12 (3 `DAMAGE`, 6 `SHIELD`, 3 `BURN`), senza fazione |
| Hidden | 8 (4 `SURVIVAL`, 4 `DAMAGE`), senza fazione |
| Terreni | 4, uno per fazione |
| Oggetti | 4, uno per fazione |

Creature per rarità: Common 28 (7 per fazione), Uncommon 26 (6 per Invitation e Wound, 7 per Archive e Dream), Rare 16, Mythic 12, Legendary 8, Mutant 2.
Le **Common non hanno effetto**; Uncommon: modificatori lievi; Rare: abilità di potenza confrontabile; Mythic, Legendary e Mutant: abilità distintive.

I concept SCP-179, SCP-3507, SCP-1370, SCP-3812 e SCP-6004 sono di riserva e **non** fanno parte delle 120.

## Theory of the Invitation — Monti Appalachi (A)

Common senza effetto: Squonk, Fuoco fatuo (Will-o'-the-Wisp), Corvo di Cenere (Ash Raven), Cane Senz'Ombra, Spaventapasseri Zoppo, The Coal Crawler, Le Falene della Miniera.

| Carta | Rarità | Codice | Effetto | Momenti | Stato | Lotto | Domande |
|---|---|---|---|---|---|---|---|
| Wampus Cat | Uncommon | `WAMPUS_CAT` | +1 ATK nel primo round | `modifyOutgoingAttack` | round dall'entrata | P1 | primo round dopo l'entrata (D-31) |
| Hidebehind | Uncommon | `HIDEBEHIND` | previene 2 danni dal primo attacco ricevuto | `modifyIncomingDamage` | `used` | P1 | D-31, D-33. Esempio `09` |
| Il Pastore Muto (The Silent Shepherd) | Uncommon | `SILENT_SHEPHERD` | quando muore, 1 danno all'avv. | `onDeath` | — | P1 | può uccidere: D-08 |
| Cervo Bianco (White Stag) | Uncommon | `WHITE_STAG` | all'entrata avv. −1 DEF | `onEnterPlay` | — | P1 | D-08 |
| The Nameless Caller | Uncommon | `NAMELESS_CALLER` | all'entrata avv. −1 DEF, −2 se un nemico è già morto | `onEnterPlay` | — | P1 | "un nemico già morto": nella partita? Serve il contatore delle creature perse nel `PlayerState` |
| Il Raccoglitore di Orme | Uncommon | `FOOTPRINT_GATHERER` | all'entrata +1 DEF fino alla fine dello scontro | `onEnterPlay` | — | P1 | DEF temporanea: scudo, la scadenza non uccide (D-35) |
| Spearfinger | Rare | `SPEARFINGER` | il primo attacco fa +2 danni | `modifyOutgoingAttack` | `used` | P1 | D-31, D-33 |
| Bell Witch | Rare | `BELL_WITCH` | dal secondo round infligge 2 danni all'avv., una volta | `onRoundStart` | `used` | P1 | inizio o fine del round 2? D-08 |
| Il Predicatore sotto la Pioggia (The Rain Preacher) | Rare | `RAIN_PREACHER` | annulla l'effetto dell'avv. fino alla fine del secondo round | `onEnterPlay` (`NULLIFY`), `onRoundEnd` | round dello scontro | P1 | annulla anche quello che l'effetto ha già fatto all'entrata? |
| The Bloodless Hound | Rare | `BLOODLESS_HOUND` | dopo aver eliminato una creatura recupera 2 DEF, al massimo fino alla DEF base, una volta | `onKill` | `used` | P1 | serve `heal()`; DEF base = della carta o all'entrata? |
| Mothman | Mythic | `MOTHMAN` | una volta: se perde uno scontro di al massimo 3 DEF, sopravvive con DEF 1 e infligge 2 danni all'avv. | `afterDamage` (prima del controllo morti) | `used` | P2 | "perde di al massimo 3" = DEF finale tra 0 e −3 (D-36); se muoiono entrambe? I 2 danni possono uccidere: D-08 |
| Bigfoot | Mythic | `BIGFOOT` | +2 ATK contro Rare o superiori, +1 DEF contro Common e Uncommon | `onEnterPlay`, `modifyOutgoingAttack` | — | P2 | il +1 DEF dipende dall'avversaria: con D-31 (per entrata) si controlla solo contro la prima? Mutant sopra Legendary (D-20) |
| The Thorned Matriarch | Mythic | `THORNED_MATRIARCH` | a metà DEF sacrifica 3 DEF per infliggere 5 danni all'avv., se sopravvive al sacrificio, una volta | `afterDamage` | `used` | P1 | "se sopravvive": con DEF ≤ 3 non scatta? D-08 |
| Wendigo | Legendary | `WENDIGO` | a fine round avv. −1 DEF; al primo KO recupera 3 DEF e la prossima avv. entra con −2 DEF | `onRoundEnd`, `onKill` | `firstKill` | P2 | recupero oltre la DEF iniziale? Serve `heal()`; −2 alla prossima: D-34; D-08 |
| La Regina dei Sentieri Perduti | Legendary | `QUEEN_OF_LOST_PATHS` | **sceglie** tra avv. −4 DEF e annullare la prossima Hidden nemica; al primo KO applica l'altro effetto | `onEnterPlay`, `onKill` | `firstKill` | P2 | **non deterministico**: il giocatore non sceglie, serve una regola fissa. "Annulla la prossima Hidden" richiede di agire sulle Hidden coperte (D-15) |

## Theory of the Archive — SCP (A)

Common senza effetto: SCP-131 (Eye Pods), SCP-999 (Tickle Monster), SCP-1762 (Here Be Dragons), Pupazzo da Archivio (Archive Puppet), Tazza Eterna (Eternal Cup), Il Timbro di Nessuno, La Falena del Fascicolo Rosso.

| Carta | Rarità | Codice | Effetto | Momenti | Stato | Lotto | Domande |
|---|---|---|---|---|---|---|---|
| SCP-173 (The Sculpture) | Uncommon | `SCP_173` | all'entrata 2 danni all'avv. | `onEnterPlay` | — | P1 | D-08 |
| SCP-096 (Shy Guy) | Uncommon | `SCP_096` | a metà DEF ottiene +2 ATK | `afterDamage` | `enraged` | P1 | durata non scritta (proposta: finché resta in campo) |
| SCP-049 (Plague Doctor) | Uncommon | `SCP_049` | avv. −1 ATK nel primo round | `onEnterPlay` | round dello scontro | P1 | modificatore sull'avv. con durata (D-35) |
| Il Collezionista di Nomi | Uncommon | `NAME_COLLECTOR` | all'entrata avv. −1 DEF | `onEnterPlay` | — | P1 | D-08 |
| SCP-053 (The Young Girl) | Uncommon | `SCP_053` | nel primo round l'avv. ha +1 ATK e −2 DEF | `onEnterPlay` | round dello scontro | P2 | il −2 DEF "nel primo round" è un danno permanente o un modificatore che finisce? D-35 |
| SCP-2006 (Too Spooky) | Uncommon | `SCP_2006` | all'entrata +1 alla statistica base più bassa | `onEnterPlay` | — | P1 | con ATK = DEF quale sale? Serve una regola fissa |
| The Index Maw | Uncommon | `INDEX_MAW` | all'entrata +1 DEF fino alla fine dello scontro | `onEnterPlay` | — | P1 | come Il Raccoglitore di Orme (scudo, D-35) |
| SCP-106 (Old Man) | Rare | `SCP_106` | riflette 1 danno all'avv. per i primi due attacchi subiti | `afterDamage` | `hits` | P1 | un attacco da 0 conta? D-33; D-08 |
| SCP-682 (Hard-to-Destroy Reptile) | Rare | `SCP_682` | dopo il primo attacco subito, il secondo fa 2 danni in meno | `modifyIncomingDamage` | `hits` | P1 | vale solo per il secondo attacco (prima per tutti); D-33 |
| SCP-2521 | Rare | `SCP_2521` | annulla l'effetto dell'avv. per due round | `onEnterPlay` (`NULLIFY`), `onRoundEnd` | round dello scontro | P1 | — |
| The Drowned Memory | Rare | `DROWNED_MEMORY` | +1 DEF per ogni propria creatura eliminata, massimo +3 | `onEnterPlay` | — | P1 | solo all'entrata o anche quando muoiono creature dopo? Serve il contatore delle creature perse |
| SCP-2317 (A Door in the Woods) | Mythic | `SCP_2317` | quando muore, 4 danni all'avv. e +1 ATK alla prossima propria creatura | `onDeath` | — | P1 | possono morire entrambe (D-08); +1 ATK in attesa (D-34) |
| SCP-3000 (Anantashesha) | Mythic | `SCP_3000` | +3 ATK contro Rare o superiori | `modifyOutgoingAttack` | — | P1 | Mutant compresa (D-20). Nel catalogo, dopo questa carta, c'è una riga orfana ("previene 2 danni dal primo attacco — Nessun effetto"): resto del vecchio testo? |
| The Null Witness | Mythic | `NULL_WITNESS` | all'entrata toglie il Terreno avversario e i modificatori temporanei, infligge 4 danni all'avv. e si elimina | `onEnterPlay` | — | P2 | con D-19 c'è un solo Terreno in gioco e con D-17 vale per tutti: "Terreno nemico" va ridefinito (D-38). L'autoeliminazione causa danno al proprietario? D-08 |
| SCP-001 (When Day Breaks) | Legendary | `SCP_001` | all'entrata 3 danni all'avv., poi 2 danni a inizio round, per al massimo 3 round | `onEnterPlay`, `onRoundStart` | round dello scontro | P1 | se l'avv. muore prima, i round restanti passano alla prossima? D-31, D-08 |
| SCP-3999 (I Am At The Center Of Everything That Happens To Me) | Legendary | `SCP_3999` | all'entrata azzera i bonus temporanei dell'avv. e ne scambia ATK e DEF base per due round, minimo 1 | `onEnterPlay`, `onRoundEnd` | round dello scontro | P2 | scambio con la DEF già danneggiata: quale DEF? Come si torna indietro dopo 2 round? D-35 |

## Theory of the Wound — Backrooms (B)

Common senza effetto: Dullers, Crawlers, Faceling, Cercatore di Moquette, Ronzio Giallo (Yellow Hum), L'Ascensore Senza Pulsanti, L'Inquilino del Soffitto.

| Carta | Rarità | Codice | Effetto | Momenti | Stato | Lotto | Domande |
|---|---|---|---|---|---|---|---|
| Howlers | Uncommon | `HOWLERS` | avv. −1 ATK nel primo round | `onEnterPlay` | round dello scontro | P1 | — |
| Wretches | Uncommon | `WRETCHES` | la prima volta che subisce danni, +1 ATK nel round seguente | `afterDamage` | `used` | P1 | — |
| Cartellino Sbiadito (Faded Tag) | Uncommon | `FADED_TAG` | all'entrata avv. −1 DEF | `onEnterPlay` | — | P1 | D-08 |
| Sala d'Attesa Vivente (Living Waiting Room) | Uncommon | `LIVING_WAITING_ROOM` | previene 2 danni dal primo attacco | `modifyIncomingDamage` | `used` | P1 | D-33 |
| The Gnawing Witness | Uncommon | `GNAWING_WITNESS` | all'entrata 2 danni all'avv. | `onEnterPlay` | — | P1 | D-08 |
| L'Addetto alle Uscite | Uncommon | `EXIT_CLERK` | annulla 1 punto del primo bonus ATK dell'avv. | `onEnterPlay` | `used` | P2 | quali bonus contano (fazione, Terreno, Spell, effetto)? D-35 |
| Skin-Stealers | Rare | `SKIN_STEALERS` | ottiene DEF pari al 25% della DEF base dell'avv. (per difetto), massimo 3, per un round | `onEnterPlay`, `onRoundEnd` | round dall'entrata | P1 | DEF temporanea per un round: scudo (D-35) |
| Il Doppio di Servizio (The Service Double) | Rare | `SERVICE_DOUBLE` | +3 ATK nel primo round | `modifyOutgoingAttack` | round dello scontro | P1 | D-31 |
| Il Direttore dell'Edificio (The Building Director) | Rare | `BUILDING_DIRECTOR` | all'entrata avv. −20% della DEF base (per difetto), massimo 3 | `onEnterPlay` | — | P1 | cambia anche la DEF iniziale usata dalle soglie del 50%? (proposta: no) |
| L'Operatore della Stanza 0 | Rare | `ROOM_ZERO_OPERATOR` | all'entrata 2 danni all'avv. e ritarda di un round la prima Hidden avversaria | `onEnterPlay` | — | P2 | con D-15 una Hidden scatta a inizio scontro: cosa vuol dire ritardarla di un round? D-08 |
| Hounds | Mythic | `HOUNDS` | DEF avv. a metà → +4 ATK fino alla fine dello scontro | `afterDamage` | `triggered` | P1 | come SCP-096, ma guarda l'avversario |
| Deathmoths | Mythic | `DEATHMOTHS` | avv. −2 ATK per due round e annulla 1 punto di un altro bonus ATK | `onEnterPlay` | round dello scontro | P2 | quale bonus ATK? D-35 |
| Il Custode delle Porte Incrociate | Mythic | `CROSSED_DOORS_KEEPER` | annulla il prossimo effetto di entrata avversario e ottiene +2 DEF | `onEnterPlay` (`NULLIFY`) | `used` | P2 | la creatura avversaria in campo è già entrata: si intende la prossima? Serve uno stato nel `PlayerState` avversario |
| Smiler | Legendary | `SMILER` | +2 ATK per ogni round in cui sopravvive, dal round seguente, massimo +6 | `onRoundEnd` | `bonus` | P1 | il bonus resta contro la prossima avv.? (proposta: sì) |
| The Hollow Wyrm | Legendary | `HOLLOW_WYRM` | se entrambi i giocatori hanno un Terreno, annulla quello avversario per due round e infligge 2 danni all'avv. | `onEnterPlay`, `onRoundEnd` | round dello scontro | P2 | **non può attivarsi**: con D-19 in gioco c'è un solo Terreno, quindi "se entrambi hanno un Terreno" non si verifica mai. Da riscrivere nella revisione |

## Theory of the Dream — Yokai (B)

Common senza effetto: Kasa-obake, Chōchin-obake, Zashiki-warashi, Kodama, Tanuki del Tempio (Temple Tanuki), The Bone Lantern, La Sposa delle Campane.

| Carta | Rarità | Codice | Effetto | Momenti | Stato | Lotto | Domande |
|---|---|---|---|---|---|---|---|
| Kappa | Uncommon | `KAPPA` | +2 ATK nel primo round | `modifyOutgoingAttack` | round dello scontro | P1 | D-31 |
| Nurikabe | Uncommon | `NURIKABE` | previene 2 danni dal primo attacco | `modifyIncomingDamage` | `used` | P1 | D-33 |
| Noppera-bō | Uncommon | `NOPPERA_BO` | avv. −1 ATK nel primo round | `onEnterPlay` | round dello scontro | P1 | — |
| Rokurokubi | Uncommon | `ROKUROKUBI` | all'entrata 1 danno all'avv. | `onEnterPlay` | — | P1 | D-08 |
| The Threadbound Doll | Uncommon | `THREADBOUND_DOLL` | quando muore, 2 danni all'avv. | `onDeath` | — | P1 | D-08 |
| L'Ombra del Torii | Uncommon | `TORII_SHADOW` | all'entrata +2 DEF se c'è il Terreno della Theory of the Dream, altrimenti +1 | `onEnterPlay` | — | P1 | il Terreno di chiunque dei due (D-17)? |
| The Masked Bloom | Uncommon | `MASKED_BLOOM` | all'entrata avv. −1 ATK nel primo round | `onEnterPlay` | round dello scontro | P1 | — |
| Yuki-onna | Rare | `YUKI_ONNA` | avv. −2 ATK per due round | `onEnterPlay` | round dello scontro | P1 | — |
| Jorōgumo | Rare | `JOROGUMO` | previene 3 danni dal primo attacco | `modifyIncomingDamage` | `used` | P1 | D-33 |
| Nure-onna | Rare | `NURE_ONNA` | DEF avv. a metà → 3 danni all'avv., una volta | `afterDamage` | `triggered` | P1 | D-08, D-31 |
| The Face Collector | Rare | `FACE_COLLECTOR` | ruba 2 ATK all'avv. fino alla fine dello scontro | `onEnterPlay` | — | P1 | se l'avv. ha meno di 2 ATK ruba comunque 2? |
| Baku | Mythic | `BAKU` | annulla l'effetto dell'avv. per due round e ottiene +3 DEF | `onEnterPlay` (`NULLIFY`), `onRoundEnd` | round dello scontro | P1 | il +3 DEF vale anche se l'avv. non ha effetti? |
| The Unseen Matriarch | Mythic | `UNSEEN_MATRIARCH` | DEF avv. a metà → 4 danni all'avv. e −1 ATK fino alla fine dello scontro | `afterDamage` | `triggered` | P1 | stessa meccanica di Nure-onna (classe base comune) |
| The Undying Blossom | Mythic | `UNDYING_BLOSSOM` | una volta sopravvive a un danno letale con 4 DEF, perdendo i bonus temporanei | `afterDamage` (prima del controllo morti) | `used` | P2 | vale anche contro i danni degli effetti? Se muoiono entrambe? D-08, D-35 |
| Shuten-dōji | Legendary | `SHUTEN_DOJI` | dopo ogni danno subito +2 ATK cumulativi, massimo +8 | `afterDamage` | `bonus` | P1 | contano anche i danni degli effetti? |
| Kitsune | Legendary | `KITSUNE` | sigilla fino a 3 punti di bonus ATK dell'avv. per tre round; se sigilla qualcosa, +2 DEF | `onEnterPlay`, `onRoundEnd` | round dello scontro | P2 | quali bonus (fazione compresa)? D-35 |

## Mutant — due fazioni

Ognuna prende vantaggi e svantaggi di entrambe le fazioni (D-22, [regola](../fazioni/fazioni.md#carte-con-due-fazioni)).

| Carta | Fazioni | Codice | Effetto | Momenti | Stato | Lotto | Domande |
|---|---|---|---|---|---|---|---|
| The Rootbound Doppelganger | Invitation + Dream | `ROOTBOUND_DOPPELGANGER` | previene 3 danni dal primo attacco avversario e toglie 2 ATK all'avv. per i due round seguenti, una volta per entrata | `modifyIncomingDamage`, `onRoundEnd` | `used`, round | P1 | D-33; rarità per gli effetti che confrontano: D-20 |
| The Stitched Corridor | Wound + Archive | `STITCHED_CORRIDOR` | prima del primo round 2 danni all'avv.; il primo attacco ricevuto fa 2 danni in meno, una volta per entrata | `onEnterPlay`, `modifyIncomingDamage` | `used` | P1 | D-08, D-33 |

## Spell

Senza fazione. Le `DAMAGE` colpiscono subito la creatura avversaria in campo (senza bersaglio: la prossima che entra); le `SHIELD` restano in attesa e si sommano sulla prossima creatura del proprietario; poi vengono scartate (D-10, D-11, D-34).
Una Spell si pesca solo quando la propria creatura è morta: la "creatura attiva" del catalogo (SCP-109, SCP-458, Burn) è quindi sempre la **prossima** creatura del proprietario. I modificatori durano finché quella creatura resta in campo (D-12).

| Carta | Sottotipo | Codice | Effetto | Lotto |
|---|---|---|---|---|
| Morso del Rovescio (Bite of the Reverse) | `DAMAGE` | `BITE_OF_REVERSE` | avv. −3 ATK | P1 |
| Frattura della Pelle (Skin Fracture) | `DAMAGE` | `SKIN_FRACTURE` | avv. −3 DEF | P1 |
| Dissoluzione (Dissolution) | `DAMAGE` | `DISSOLUTION` | avv. −2 ATK, −2 DEF | P1 |
| Velo degli Ancorati (Veil of the Anchored) | `SHIELD` | `VEIL_OF_ANCHORED` | +3 DEF | P1 |
| Patto del Registro (Registry Pact) | `SHIELD` | `REGISTRY_PACT` | +2 DEF, +1 ATK | P1 |
| Armatura della Pioggia (Rain Armor) | `SHIELD` | `RAIN_ARMOR` | +1 DEF, +2 ATK | P1 |
| SCP-500 · Pillola di Emergenza | `SHIELD` | `SCP_500` | la prossima creatura subisce 4 danni in meno dal primo attacco | P1 (D-33) |
| SCP-109 · Acqua Infinita | `SHIELD` | `SCP_109` | +2 DEF | P1 |
| SCP-458 · Pizza Senza Fine | `SHIELD` | `SCP_458` | +3 DEF | P1 |
| Richiamo della Crepa (Call of the Rift) | `BURN` | `CRACK_CALL` | Burn | P1 (D-13) |
| Fusione del Rovescio (Reverse Fusion) | `BURN` | `REVERSE_FUSION` | Burn, +1 ATK se la carta rivelata è una creatura | P1 (D-13) |
| Eco dei Dimenticati (Echo of the Forgotten) | `BURN` | `FORGOTTEN_ECHO` | Burn, +1 DEF se la carta rivelata è una creatura | P1 (D-13) |

**Regola Burn (catalogo):** si rivela la prossima carta del mazzo. Se è una creatura, la sua ATK e la sua DEF si aggiungono **temporaneamente** alla creatura attiva; se è di un altro tipo viene scartata; con il mazzo vuoto non succede nulla.
Da decidere (D-13): la creatura rivelata resta nel mazzo o viene tolta? "Temporaneamente" fino a quando?

## Hidden

Senza fazione. Messe coperte quando pescate, poi scartate dopo l'attivazione (D-15, modificata il 10 novembre): quelle **con una condizione** (Unseen Exit, Static Interference) si attivano quando la condizione si verifica, le altre all'inizio del prossimo scontro del proprietario; insieme, prima le `DAMAGE` e poi le `SURVIVAL`.
Da fissare nella revisione: il "non nel primo turno" del catalogo e la condizione esatta di Static Interference (quando un bonus ATK avversario è presente? e se non c'è mai?). Le due Hidden con condizione sono `P2`: serve il controllo delle condizioni nei passi del round.

| Carta | Sottotipo | Codice | Effetto | Lotto |
|---|---|---|---|---|
| Last Sanctuary | `SURVIVAL` | `LAST_SANCTUARY` | propria creatura +4 DEF nello scontro corrente | P1 |
| Survival Instinct | `SURVIVAL` | `SURVIVAL_INSTINCT` | +2 ATK, +2 DEF nello scontro corrente | P1 |
| Remnants of Reality | `SURVIVAL` | `REMNANTS_OF_REALITY` | annulla fino a 3 punti di penalità ATK/DEF, senza superare i valori originali | P2 (D-35) |
| Unseen Exit | `SURVIVAL` | `UNSEEN_EXIT` | quando un attacco avversario ucciderebbe la creatura, previene 3 danni | P2 (D-15) |
| Hostile Presence | `DAMAGE` | `HOSTILE_PRESENCE` | avv. −4 ATK | P1 |
| Echo Strike | `DAMAGE` | `ECHO_STRIKE` | avv. −2 ATK, −2 DEF | P1 |
| Breaking Point | `DAMAGE` | `BREAKING_POINT` | avv. −5 DEF se ha almeno 5 DEF, altrimenti −2 DEF fino a un minimo di 0 | P1 (D-16) |
| Static Interference | `DAMAGE` | `STATIC_INTERFERENCE` | annulla fino a 3 punti di un bonus ATK avversario; se non ce ne sono, 2 danni all'avv. | P2 (D-35) |

"Nello scontro corrente" (Last Sanctuary, Survival Instinct): il bonus finisce con lo scontro; la DEF in più funziona da scudo e la scadenza non uccide (D-35).

## Terreni

Una Crepa per fazione. In gioco c'è un solo Terreno: quello pescato dopo, da chiunque, sostituisce il precedente (D-19). Il bonus vale per tutte le creature della fazione in campo, anche avversarie (D-17).
Bonus base `P1`; effetto secondario rivisto ora e implementato dopo, `P2` (D-18).

| Carta | Fazione | Codice | Bonus base | Effetto secondario |
|---|---|---|---|---|
| Echo Rift (Crepa Eco) | Theory of the Wound | `ECHO_RIFT` | +1 ATK | con una Spell che aumenta l'ATK, +1 ATK in più, una volta per scontro |
| Hollow Clearing (Crepa Radura) | Theory of the Invitation | `HOLLOW_CLEARING` | +2 DEF | la prima penalità alla DEF di ogni scontro −1 |
| Watching Rift (Crepa Occhi) | Theory of the Archive | `WATCHING_RIFT` | +1 ATK, +1 DEF | la prima Spell `DAMAGE` subita fa 1 punto in meno in totale |
| Devouring Abyss (Crepa Cavità) | Theory of the Dream | `DEVOURING_ABYSS` | +1 ATK | nel primo confronto, 1 danno all'avv. |

## Oggetti

Uno per fazione, rarità Rare, uno per mazzo. **Monouso** (D-47): l'Oggetto resta accanto al mazzo finché il suo effetto scatta una volta, sulla prima creatura del proprietario che ne soddisfa la condizione, poi si consuma. Ordine rispetto al Terreno: D-48.

| Carta | Fazione | Codice | Effetto | Lotto |
|---|---|---|---|---|
| Badge di Uscita Negata | Theory of the Wound | `DENIED_EXIT_BADGE` | previene 2 danni dal primo attacco, poi si consuma | P2 (D-33, D-47) |
| Bussola dei Sentieri Falsi | Theory of the Invitation | `FALSE_PATHS_COMPASS` | +2 DEF per il primo round, poi si consuma | P2 (D-47) |
| Fascicolo Sigillato 0 | Theory of the Archive | `SEALED_FILE_ZERO` | annulla fino a 2 punti della prima penalità ATK/DEF, poi si consuma | P2 (D-35, D-47) |
| Campanello del Torii | Theory of the Dream | `TORII_BELL` | quando si attiva la prima Hidden avversaria, +2 DEF, poi si consuma | P2 (D-15, D-47) |

Domanda per la revisione: la fazione dell'Oggetto conta (es. solo per creature della sua fazione)?

## Pezzi di motore richiesti dagli effetti

Prima degli effetti di Spell, Hidden, Terreni e Oggetti serve il registro (`ENG-22`); prima di quelli delle creature serve anche `CreatureEffect` (`ENG-23`); prima dei `P2`, questi pezzi in più:

| Pezzo | Serve a | Decisione |
|---|---|---|
| Numero massimo di attivazioni per effetto (niente `onCombatStart`); "primo round", "una volta", "per due round" si contano per creatura | quasi tutte le Uncommon e Rare, Bigfoot, SCP-001 | D-31 |
| Controllo delle morti dopo ogni fase | danni all'entrata (SCP-173, The Gnawing Witness, Cervo Bianco, The Nameless Caller, Cartellino Sbiadito, Il Collezionista di Nomi, Rokurokubi, L'Operatore della Stanza 0, The Stitched Corridor, The Null Witness, SCP-001), a inizio o fine round (Bell Witch, Wendigo, SCP-001), dopo i danni (SCP-106, Nure-onna, The Unseen Matriarch, The Thorned Matriarch, Mothman), alla morte (Il Pastore Muto, The Threadbound Doll, SCP-2317) | D-08 |
| Modificatori in attesa nel `PlayerState`, sommati sulla prossima creatura | Spell `SHIELD`, Spell `DAMAGE` senza bersaglio, Wendigo, SCP-2317 | D-34 |
| `StatModifier` con origine e durata in round, sospendibile | modificatori sull'avversario per N round (Howlers, SCP-049, Noppera-bō, The Masked Bloom, SCP-053, Yuki-onna, Deathmoths, The Rootbound Doppelganger), bonus "fino alla fine dello scontro" (Il Raccoglitore di Orme, The Index Maw, Skin-Stealers, Last Sanctuary, Survival Instinct), sospensioni e annullamenti (Kitsune, The Hollow Wyrm, The Null Witness, SCP-3999, Deathmoths, L'Addetto alle Uscite, Remnants of Reality, Static Interference, Fascicolo Sigillato 0, The Undying Blossom), effetti secondari dei Terreni | D-35 |
| Contatore delle creature perse nel `PlayerState` | The Drowned Memory, The Nameless Caller | — |
| `CreatureInPlay.heal()` | Wendigo, The Bloodless Hound | limiti da decidere |
| Sopravvivenza a un danno letale, prima del controllo morti | Mothman, The Undying Blossom, Unseen Exit | D-36 |
| Azioni sulle Hidden coperte dell'avversario | La Regina dei Sentieri Perduti, L'Operatore della Stanza 0, Campanello del Torii | D-15 |
| Controllo delle condizioni delle Hidden nei passi del round | Unseen Exit, Static Interference | D-15 |
| Sostituzione del Terreno in gioco, con rimozione dei suoi modificatori | tutti i Terreni, The Null Witness | D-19, D-35 |
| DEF temporanea come scudo (assorbe i danni per prima, la scadenza non uccide) | Il Raccoglitore di Orme, The Index Maw, Skin-Stealers, Last Sanctuary, Survival Instinct, Bussola dei Sentieri Falsi | D-35 |
| Annullamento di un effetto futuro | Il Custode delle Porte Incrociate | D-30 |
| Classi base per effetti uguali | "primo attacco ridotto di N" (Hidebehind, Sala d'Attesa Vivente, Nurikabe, Jorōgumo, The Rootbound Doppelganger, The Stitched Corridor, SCP-500, Badge di Uscita Negata); "danni all'entrata" (Cervo Bianco, Cartellino Sbiadito, Il Collezionista di Nomi, Rokurokubi, SCP-173, The Gnawing Witness); "+ATK nel primo round" (Wampus Cat, Kappa, Il Doppio di Servizio); "avv. −ATK per N round" (Howlers, SCP-049, Noppera-bō, The Masked Bloom, Yuki-onna); "soglia metà DEF avversaria" (Nure-onna, The Unseen Matriarch, Hounds); "danni alla morte" (Il Pastore Muto, The Threadbound Doll, SCP-2317) | D-37 |
