# Catalogo degli effetti

Elenco di **tutti** gli effetti delle 120 carte, tradotti in termini del motore: codice, momenti dell'interfaccia `CreatureEffect`
(vedi [effetti](effetti.md)), stato da tenere e note. È il backlog delle issue `ENG-19`, `ENG-20`, `ENG-21`, `ENG-23`, `ENG-25`, `ENG-26`, `ENG-33`.

**Fonte:** il *Catalogo completo 120 carte* (bozza di game design del 9 novembre), che **sostituisce** il documento di lavoro (`docs/idee`) e la proposta al cliente del 2 novembre,
con gli effetti decisi nella **revisione carta per carta del 10 e 11 novembre** ([revisione degli effetti](../bilanciamento/revisione-effetti.md)): lì ci sono, per ogni carta, l'effetto originale, il motivo del cambiamento, il valore e le interazioni.
La struttura (quante carte per tipo, fazione e rarità) è decisa (D-24). Gli **effetti sono decisi**; le statistiche ATK/DEF non sono ancora definite e si fissano con la simulazione.
Finché le statistiche mancano, l'engine si sviluppa e si testa con **dati di prova** ([`examples/`](../../examples/README.md)).

- **Carta:** nome come nel catalogo; tra parentesi il nome inglese di lavoro usato finora, dove esiste. Il gioco è in inglese (D-44): i nomi definitivi li danno i DSG.
- **Effetto:** l'effetto deciso nella revisione; in corsivo il nome di lavoro dell'effetto, se ne ha uno.
- **Codice:** proposta; diventa definitivo quando l'effetto è registrato in `EffectRegistry` e il backoffice lo assegna a una carta (`catalog_cards.effect_code`). I numeri stanno nella classe dell'effetto (D-46).
- **Lotto:** `P1` = si implementa con i pezzi di base (`CreatureEffect`, registro, modificatori); `P2` = serve un pezzo di motore in più (vedi [Pezzi di motore](#pezzi-di-motore-richiesti-dagli-effetti)).
- **Chi:** divisione tra i due sviluppatori — A = Theory of the Invitation + Theory of the Archive, B = Theory of the Wound + Theory of the Dream.
- I riferimenti `D-xx` rimandano alle [decisioni aperte](../decisioni-aperte/decisioni-aperte.md); i riferimenti `M-xx` all'elenco delle meccaniche nella [revisione](../bilanciamento/revisione-effetti.md).
- Quando un effetto è fatto, spostalo nella tabella *Effetti implementati* di [effetti](effetti.md) con il nome del test.

## Definizioni comuni

| Termine | Significato |
|---|---|
| *avv.* | la creatura avversaria in campo |
| *scontro* | serie di round tra le stesse due creature (il catalogo dice anche "combattimento"); finisce quando una delle due muore |
| *finché resta in campo* | dura per tutta la permanenza della creatura, anche negli scontri successivi |
| *danno da effetto* | DEF tolta da un effetto, una Spell, una Hidden, un Terreno o un Oggetto: è una perdita permanente di DEF, **non** un attacco (non attiva gli effetti "quando viene attaccata" e le prevenzioni degli attacchi). Può uccidere fuori dal passo 4 (D-08) |
| *ferita* | perdita reale di DEF sopra 0, dopo gli scudi |
| *metà DEF* | DEF ≤ 50% della DEF all'entrata in campo, bonus compresi (M-06); si controlla dopo ogni fase in cui la DEF cambia, solo se la creatura è ancora viva |
| *penalità* | riduzione di ATK da una carta avversaria, oppure DEF persa per un effetto, una Spell o una Hidden avversaria; il danno di un attacco non è una penalità |
| *bonus ATK* | qualunque modificatore ATK positivo, di qualunque origine; **non** il bonus di fazione, che non è un modificatore (D-35) |
| *"primo", "una volta"* | si contano **per entrata in campo** e con danno > 0 (D-31, D-33); *per avversaria* = di nuovo contro ogni nuova creatura avversaria (stato "ultima avversaria") |
| *annullato* | un effetto annullato non scatta e i suoi bonus ATK sono sospesi; la DEF già guadagnata resta |

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

Valore degli effetti (in punti statistica, PS; metodo nella [revisione](../bilanciamento/revisione-effetti.md#metodo)): Uncommon 2–3, Rare 4–5, Mythic 6–8, Legendary 9–12, Mutant 8–10; Spell, Hidden e Oggetti 4–5 ciascuno; Terreni circa 3,5 per creatura della fazione.

I concept SCP-179, SCP-3507, SCP-1370, SCP-3812 e SCP-6004 sono di riserva e **non** fanno parte delle 120.

## Theory of the Invitation — Monti Appalachi (A)

Common senza effetto: Squonk, Fuoco fatuo (Will-o'-the-Wisp), Corvo di Cenere (Ash Raven), Cane Senz'Ombra, Spaventapasseri Zoppo, The Coal Crawler, Le Falene della Miniera.

| Carta | Rarità | Codice | Effetto | Momenti | Stato | Lotto | Note |
|---|---|---|---|---|---|---|---|
| Wampus Cat | Uncommon | `WAMPUS_CAT` | *Sguardo della follia*: all'entrata avv. −1 ATK finché resta in campo | `onEnterPlay` | — | P1 | modificatore sull'avv. (D-35) |
| Hidebehind | Uncommon | `HIDEBEHIND` | previene 2 danni dal primo attacco di **ogni** creatura avversaria | `modifyIncomingDamage` | ultima avversaria | P1 | classe base "primo attacco ridotto per avversaria". Esempio `09` |
| Il Pastore Muto (The Silent Shepherd) | Uncommon | `SILENT_SHEPHERD` | *Custode del gregge*: quando muore, la prossima creatura del proprietario entra con +2 DEF | `onDeath` | — | P1 | modificatore in attesa (D-34); vale anche se muoiono entrambe |
| Cervo Bianco (White Stag) | Uncommon | `WHITE_STAG` | all'entrata avv. −1 DEF e −1 DEF alla prossima creatura avversaria che entra | `onEnterPlay` | — | P1 | danno da effetto (D-08); il secondo −1 è in attesa nel `PlayerState` avversario (D-34) |
| The Nameless Caller | Uncommon | `NAMELESS_CALLER` | *Voci dei dispersi*: all'entrata 1 danno all'avv. per ogni creatura avversaria già morta nella partita, massimo 3 | `onEnterPlay` | — | P1 | contatore delle creature perse (M-07) |
| Il Raccoglitore di Orme | Uncommon | `FOOTPRINT_GATHERER` | all'entrata +2 DEF finché resta in campo | `onEnterPlay` | — | P1 | DEF permanente |
| Spearfinger | Rare | `SPEARFINGER` | il primo attacco contro ogni nuova creatura avversaria fa +2 | `modifyOutgoingAttack` | ultima avversaria | P1 | è un bonus ATK |
| Bell Witch | Rare | `BELL_WITCH` | *Infestazione che resta*: all'inizio del secondo round 2 danni all'avv., una volta; quando muore, la prossima creatura avversaria entra con −2 DEF | `onRoundStart`, `onDeath` | `used` | P1 | D-08, D-34 |
| Il Predicatore sotto la Pioggia (The Rain Preacher) | Rare | `RAIN_PREACHER` | *Sermone che cancella*: all'entrata annulla l'effetto dell'avv. finché resta in campo; se non ha effetto, ha invece −2 ATK finché resta in campo | `onEnterPlay` (`NULLIFY`) | — | P2 | regola degli effetti annullati ([effetti](effetti.md#effetti-annullati)) |
| The Bloodless Hound | Rare | `BLOODLESS_HOUND` | *Drenaggio senza sangue*: dopo ogni suo attacco che infligge danni recupera metà dei danni inflitti (per difetto), senza superare la DEF all'entrata; dopo un'eliminazione recupera altri 2 | `afterDamage`, `onKill` | — | P2 | `heal()` con tetto alla DEF all'entrata |
| Mothman | Mythic | `MOTHMAN` | *Disastro annunciato*: all'entrata, e ogni volta che una creatura avversaria entra mentre Mothman è in campo, quella creatura subisce 3 danni | `onEnterPlay`, entrata avversaria | — | P1 | controllo all'entrata delle creature avversarie; D-08 |
| Bigfoot | Mythic | `BIGFOOT` | *Ammazzagiganti*: contro Rare o superiori +4 ATK e 2 danni in meno da ogni loro attacco; contro Common e Uncommon +2 ATK | `modifyOutgoingAttack`, `modifyIncomingDamage` | — | P1 | `rarityOrder`, Mutant sopra Legendary (D-20); si controlla contro ogni avversaria |
| The Thorned Matriarch | Mythic | `THORNED_MATRIARCH` | *Offerta di rovi*: all'inizio di ogni round, se la sua DEF è più alta di quella avversaria, sacrifica 2 DEF e infligge 4 danni all'avv. | `onRoundStart` | — | P1 | D-08 |
| Wendigo | Legendary | `WENDIGO` | *Insaziabile*: a fine round avv. −1 DEF più 1 per ogni nemica eliminata da Wendigo, massimo 3; ogni eliminazione fa recuperare 3 DEF, fino alla DEF all'entrata | `onRoundEnd`, `onKill` | eliminazioni | P2 | `heal()`; conta anche un'eliminazione fatta dal drenaggio; D-08 |
| La Regina dei Sentieri Perduti | Legendary | `QUEEN_OF_LOST_PATHS` | *Sentieri deviati*: all'entrata e a ogni eliminazione guarda le prossime tre creature del mazzo avversario e manda quella di rarità più alta in fondo al mazzo (a parità, quella che verrebbe pescata prima); all'entrata anche avv. −2 DEF | `onEnterPlay`, `onKill` | — | P2 | modifica dell'ordine del mazzo avversario; evento `DECK_REORDERED`. Valore stimato: prima carta da controllare con la simulazione |

## Theory of the Archive — SCP (A)

Common senza effetto: SCP-131 (Eye Pods), SCP-999 (Tickle Monster), SCP-1762 (Here Be Dragons), Pupazzo da Archivio (Archive Puppet), Tazza Eterna (Eternal Cup), Il Timbro di Nessuno, La Falena del Fascicolo Rosso.

| Carta | Rarità | Codice | Effetto | Momenti | Stato | Lotto | Note |
|---|---|---|---|---|---|---|---|
| SCP-173 (The Sculpture) | Uncommon | `SCP_173` | all'entrata 2 danni all'avv. | `onEnterPlay` | — | P1 | D-08 |
| SCP-096 (Shy Guy) | Uncommon | `SCP_096` | *Il volto visto*: la prima creatura avversaria che lo ferisce viene marchiata; +3 ATK contro quella creatura | `afterDamage`, `modifyOutgoingAttack` | creatura marchiata | P1 | — |
| SCP-049 (Plague Doctor) | Uncommon | `SCP_049` | all'entrata avv. −2 ATK nel primo round e −1 nel secondo | `onEnterPlay` | round dello scontro | P1 | modificatore sull'avv. con durata (D-35) |
| Il Collezionista di Nomi | Uncommon | `NAME_COLLECTOR` | all'entrata avv. −1 DEF e −1 DEF alla prossima creatura avversaria che entra | `onEnterPlay` | — | P1 | come Cervo Bianco (D-08, D-34) |
| SCP-053 (The Young Girl) | Uncommon | `SCP_053` | *Paranoia*: all'entrata avv. −3 DEF; l'avv. ha +2 ATK nel primo round | `onEnterPlay` | round dello scontro | P1 | il −3 è danno da effetto (D-08) |
| SCP-2006 (Too Spooky) | Uncommon | `SCP_2006` | *Imitazione*: all'entrata, se l'ATK base dell'avv. è più alto del suo, prende quell'ATK base finché resta in campo | `onEnterPlay` | — | P1 | confronto sui valori base (`CardDefinition.atk`); a parità non cambia nulla |
| The Index Maw | Uncommon | `INDEX_MAW` | all'entrata +2 DEF finché resta in campo | `onEnterPlay` | — | P1 | come Il Raccoglitore di Orme |
| SCP-106 (Old Man) | Rare | `SCP_106` | *Riflesso più forte*: riflette 2 danni all'attaccante per ciascuno dei primi due attacchi subiti | `afterDamage` | attacchi subiti | P1 | conta solo un attacco con danno > 0 (D-33); D-08 |
| SCP-682 (Hard-to-Destroy Reptile) | Rare | `SCP_682` | *Adattamento*: ogni attacco subito riduce di 2 i danni degli attacchi successivi, cumulativo, massimo −4 | `modifyIncomingDamage`, `afterDamage` | attacchi subiti | P1 | — |
| SCP-2521 | Rare | `SCP_2521` | *Abilità rubata*: all'entrata annulla l'effetto dell'avv. finché resta in campo; se l'avv. è Rare o inferiore, SCP-2521 usa quell'effetto come proprio finché resta in campo | `onEnterPlay` (`NULLIFY`) | effetto rubato | P2 | effetto rubato: nuova istanza dal registro collegata a un'altra creatura |
| The Drowned Memory | Rare | `DROWNED_MEMORY` | *Memoria profonda*: all'entrata +1 DEF per ogni creatura alleata già eliminata (massimo +4) e +1 ATK se sono almeno 3, finché resta in campo | `onEnterPlay` | — | P1 | contatore delle creature perse (M-07) |
| SCP-2317 (A Door in the Woods) | Mythic | `SCP_2317` | quando muore, 4 danni all'avv. e +1 ATK alla prossima propria creatura | `onDeath` | — | P1 | possono morire entrambe (D-08); +1 ATK in attesa (D-34), finché la prossima creatura resta in campo |
| SCP-3000 (Anantashesha) | Mythic | `SCP_3000` | *Pelle abissale*: +2 ATK finché resta in campo; contro Rare o superiori subisce metà dei danni dai loro attacchi (per difetto) | `modifyOutgoingAttack`, `modifyIncomingDamage` | — | P1 | prima le sottrazioni, poi la metà; il danno da effetto non si dimezza. Nel catalogo, dopo questa carta, c'è una riga orfana ("previene 2 danni dal primo attacco — Nessun effetto"): da segnalare a chi cura il catalogo |
| The Null Witness | Mythic | `NULL_WITNESS` | *Protocollo di cancellazione*: all'entrata 4 danni all'avv., che perde i suoi modificatori temporanei; il Terreno in gioco viene rimosso se l'ha pescato l'avversario. Resta in campo | `onEnterPlay` | — | P2 | rimozione del Terreno (D-19); evento `TERRAIN_REMOVED`; D-38 chiusa |
| SCP-001 (When Day Breaks) | Legendary | `SCP_001` | *Alba piena*: all'entrata 3 danni, poi 3 danni a inizio round per i 3 round seguenti, alla creatura avversaria in campo in quel momento | `onEnterPlay`, `onRoundStart` | round rimanenti | P1 | i round si contano dall'entrata di SCP-001: se l'avv. muore prima, i colpi restanti vanno alla nuova avversaria; D-08 |
| SCP-3999 (I Am At The Center Of Everything That Happens To Me) | Legendary | `SCP_3999` | *Riscrittura della realtà*: all'entrata e ogni due round (nel suo 3°, 5°, … round in campo) l'avv. perde tutti i bonus ATK e DEF, da qualunque fonte, e torna ai valori base (i danni restano); ogni volta SCP-3999 ottiene +1 ATK, massimo +3 | `onEnterPlay`, `onRoundStart` | round in campo | P2 | rimozione di tutti gli `StatModifier` positivi (D-35), DEF con la regola dello scudo; il bonus di fazione resta |

## Theory of the Wound — Backrooms (B)

Common senza effetto: Dullers, Crawlers, Faceling, Cercatore di Moquette, Ronzio Giallo (Yellow Hum), L'Ascensore Senza Pulsanti, L'Inquilino del Soffitto.

| Carta | Rarità | Codice | Effetto | Momenti | Stato | Lotto | Note |
|---|---|---|---|---|---|---|---|
| Howlers | Uncommon | `HOWLERS` | all'entrata avv. −2 ATK nel primo round e −1 nel secondo | `onEnterPlay` | round dello scontro | P1 | D-35 |
| Wretches | Uncommon | `WRETCHES` | *Furia del ferito*: la prima volta che viene ferito, +2 ATK finché resta in campo | `afterDamage` | `used` | P1 | — |
| Cartellino Sbiadito (Faded Tag) | Uncommon | `FADED_TAG` | all'entrata avv. −1 DEF e −1 DEF alla prossima creatura avversaria che entra | `onEnterPlay` | — | P1 | come Cervo Bianco (D-08, D-34) |
| Sala d'Attesa Vivente (Living Waiting Room) | Uncommon | `LIVING_WAITING_ROOM` | previene 2 danni dal primo attacco di ogni creatura avversaria | `modifyIncomingDamage` | ultima avversaria | P1 | come Hidebehind |
| The Gnawing Witness | Uncommon | `GNAWING_WITNESS` | all'entrata 2 danni all'avv. | `onEnterPlay` | — | P1 | come SCP-173; D-08 |
| L'Addetto alle Uscite | Uncommon | `EXIT_CLERK` | *Indicazioni cancellate*: all'entrata l'avv. perde fino a 2 punti dei suoi bonus ATK finché resta in campo, compresi quelli che riceve dopo; se non ne ha, −1 ATK nel primo round | `onEnterPlay` | — | P2 | sospensione per origine, nell'ordine in cui i bonus sono stati applicati (D-35) |
| Skin-Stealers | Rare | `SKIN_STEALERS` | *Pelle rubata*: all'entrata ottiene DEF pari alla metà della DEF base avversaria (per difetto, massimo 4) finché resta in campo | `onEnterPlay` | — | P1 | DEF base = `CardDefinition.def` dell'avv.; DEF permanente |
| Il Doppio di Servizio (The Service Double) | Rare | `SERVICE_DOUBLE` | *Doppio turno*: +3 ATK nei primi due round dopo l'entrata | `modifyOutgoingAttack` | round dall'entrata | P1 | D-31 |
| Il Direttore dell'Edificio (The Building Director) | Rare | `BUILDING_DIRECTOR` | *Demolizione*: all'entrata danni all'avv. pari al 40% della sua DEF base (per difetto, massimo 4) | `onEnterPlay` | — | P1 | danno da effetto (D-08); la DEF all'entrata usata dalle soglie non cambia |
| L'Operatore della Stanza 0 | Rare | `ROOM_ZERO_OPERATOR` | *Linee incrociate*: all'entrata, la prossima Hidden avversaria che si attiva funziona come se fosse del proprietario dell'Operatore | `onEnterPlay` | in attesa nel `PlayerState` avversario | P2 | azioni sulle Hidden avversarie (M-11, D-15) |
| Hounds | Mythic | `HOUNDS` | *Odore del sangue*: +4 ATK contro ogni creatura avversaria già ferita (DEF più bassa della sua DEF all'entrata) | `modifyOutgoingAttack` | — | P1 | — |
| Deathmoths | Mythic | `DEATHMOTHS` | *Divoratrici di luce*: all'entrata avv. −2 ATK finché resta in campo e perde fino a 2 punti dei suoi bonus ATK, anche quelli ricevuti dopo | `onEnterPlay` | — | P2 | come L'Addetto alle Uscite (D-35) |
| Il Custode delle Porte Incrociate | Mythic | `CROSSED_DOORS_KEEPER` | *Inviolabile*: gli effetti delle creature avversarie non lo colpiscono (niente danni da effetto, malus, annullamenti; gli attacchi sì); +2 ATK contro creature che hanno un effetto | filtro sul bersaglio, `modifyOutgoingAttack` | — | P2 | Spell, Hidden, Terreni e Oggetti avversari lo colpiscono normalmente; **non** è un effetto `NULLIFY` |
| Smiler | Legendary | `SMILER` | *Sorriso nel buio*: per ogni round in cui sopravvive, +1 ATK (massimo +3) e l'avv. −1 ATK (massimo −3) | `onRoundEnd` | round sopravvissuti, malus sull'avv. | P1 | il +1 resta contro le nuove avversarie; il malus resta sulla creatura che l'ha ricevuto e riparte da 0 contro una nuova avversaria |
| The Hollow Wyrm | Legendary | `HOLLOW_WYRM` | *Divoratore di Crepe*: all'entrata inghiotte il Terreno in gioco, chiunque l'abbia pescato (scartato) e ottiene +3 ATK e +3 DEF finché resta in campo; un Terreno pescato mentre è in campo viene inghiottito allo stesso modo; senza Terreno all'entrata, 4 danni all'avv. | `onEnterPlay`, arrivo di un Terreno | — | P2 | rimozione del Terreno (D-19, regola dello scudo); evento `TERRAIN_REMOVED` |

## Theory of the Dream — Yokai (B)

Common senza effetto: Kasa-obake, Chōchin-obake, Zashiki-warashi, Kodama, Tanuki del Tempio (Temple Tanuki), The Bone Lantern, La Sposa delle Campane.

| Carta | Rarità | Codice | Effetto | Momenti | Stato | Lotto | Note |
|---|---|---|---|---|---|---|---|
| Kappa | Uncommon | `KAPPA` | +2 ATK nel primo round | `modifyOutgoingAttack` | round dall'entrata | P1 | D-31 |
| Nurikabe | Uncommon | `NURIKABE` | previene 2 danni dal primo attacco di ogni creatura avversaria | `modifyIncomingDamage` | ultima avversaria | P1 | come Hidebehind |
| Noppera-bō | Uncommon | `NOPPERA_BO` | all'entrata avv. −2 ATK nel primo round e −1 nel secondo | `onEnterPlay` | round dello scontro | P1 | come Howlers |
| Rokurokubi | Uncommon | `ROKUROKUBI` | all'entrata avv. −1 DEF e −1 DEF alla prossima creatura avversaria che entra | `onEnterPlay` | — | P1 | come Cervo Bianco (D-08, D-34) |
| The Threadbound Doll | Uncommon | `THREADBOUND_DOLL` | quando muore, 2 danni all'avv. | `onDeath` | — | P1 | D-08 |
| L'Ombra del Torii | Uncommon | `TORII_SHADOW` | all'entrata +3 DEF se c'è il Terreno della Theory of the Dream, altrimenti +2, finché resta in campo | `onEnterPlay` | — | P1 | il Terreno di chiunque (D-17), controllato solo all'entrata |
| The Masked Bloom | Uncommon | `MASKED_BLOOM` | all'entrata avv. −2 ATK nel primo round e −1 nel secondo | `onEnterPlay` | round dello scontro | P1 | come Howlers |
| Yuki-onna | Rare | `YUKI_ONNA` | *Congelata*: all'entrata l'avv. non attacca nel primo round (il suo attacco è 0) | `onEnterPlay` | round dello scontro | P1 | — |
| Jorōgumo | Rare | `JOROGUMO` | *Doppia illusione*: previene 3 danni da ciascuno dei primi due attacchi subiti | `modifyIncomingDamage` | attacchi subiti | P1 | D-33 |
| Nure-onna | Rare | `NURE_ONNA` | *Ogni vittima*: una volta per ogni creatura avversaria, quando la sua DEF scende a metà o meno, 3 danni | `afterDamage` e controllo dopo ogni fase | ultima avversaria colpita | P1 | soglia M-06, anche per danni da effetto; D-08 |
| The Face Collector | Rare | `FACE_COLLECTOR` | *Forza presa in prestito*: ruba 2 ATK nel primo round e 1 ATK nel secondo (l'avv. ha il malus, The Face Collector lo stesso bonus) | `onEnterPlay` | round dello scontro | P1 | ruba sempre il valore intero; si sconta l'ATK sotto 0 |
| Baku | Mythic | `BAKU` | *Divoratore di incubi*: finché Baku è in campo, ogni volta che l'effetto di una creatura avversaria sta per attivarsi, Baku lo divora (non avviene) e ottiene +2 DEF | prima dell'attivazione di un effetto avversario (`NULLIFY`) | avversarie già divorate | P2 | un effetto continuo viene divorato alla prima applicazione e resta spento per quella creatura; +2 DEF al massimo una volta per creatura avversaria; non tocca Spell, Hidden, Terreni e Oggetti |
| The Unseen Matriarch | Mythic | `UNSEEN_MATRIARCH` | *Velo spezzato*: la prima volta che la **sua** DEF scende a metà o meno, l'avv. subisce danni pari alla DEF che ha perso (massimo 8) e −1 ATK finché resta in campo | `afterDamage` e controllo dopo ogni fase | `used` | P1 | DEF persa = DEF all'entrata meno DEF attuale (lo scudo non conta); solo se è ancora viva; D-08 |
| The Undying Blossom | Mythic | `UNDYING_BLOSSOM` | *Maledizione cremisi*: una volta, quando subirebbe un danno letale, resta con 4 DEF e perde i suoi bonus temporanei; l'avv. subisce 3 danni | `afterDamage` (prima del controllo morti) | `used` | P2 | vale contro ogni danno letale (attacco, effetto, Spell, Hidden, Terreno, Oggetto); morte doppia: lei resta, l'altra muore; D-36 |
| Shuten-dōji | Legendary | `SHUTEN_DOJI` | dopo ogni danno DEF subito +2 ATK cumulativi, massimo +8 | `afterDamage` | `bonus` | P1 | contano anche i danni degli effetti; il bonus resta contro le nuove avversarie |
| Kitsune | Legendary | `KITSUNE` | *Mille forme*: finché sono entrambe in campo copia l'effetto dell'avv. (l'avv. tiene il suo); se copia qualcosa, +3 DEF; sigilla inoltre fino a 3 punti di bonus ATK dell'avv. finché resta in campo | `onEnterPlay`, entrata avversaria | effetto copiato | P2 | stesso pezzo di SCP-2521 senza annullare l'originale; la copia parte come appena entrata; non copia Il Custode né un'altra Kitsune; il bonus di fazione non si sigilla (D-35) |

## Mutant — due fazioni

Ognuna prende vantaggi e svantaggi di entrambe le fazioni (D-22, [regola](../fazioni/fazioni.md#carte-con-due-fazioni)).

| Carta | Fazioni | Codice | Effetto | Momenti | Stato | Lotto | Note |
|---|---|---|---|---|---|---|---|
| The Rootbound Doppelganger | Invitation + Dream | `ROOTBOUND_DOPPELGANGER` | *Imboscata a ogni vittima*: contro ogni creatura avversaria previene 3 danni dal suo primo attacco e le dà −2 ATK per i suoi due round seguenti | `modifyIncomingDamage`, `onRoundEnd`, entrata avversaria | ultima avversaria | P1 | classe base "primo attacco ridotto per avversaria"; rarità per gli effetti che confrontano: D-20 |
| The Stitched Corridor | Wound + Archive | `STITCHED_CORRIDOR` | *Battito per ogni vittima*: contro ogni creatura avversaria, 2 danni quando entra (o quando entra il Corridor) e il suo primo attacco fa 3 danni in meno | `onEnterPlay`, entrata avversaria, `modifyIncomingDamage` | ultima avversaria | P1 | controllo all'entrata delle avversarie (come Mothman); D-08 |

## Spell

Senza fazione. Valore di ognuna circa 4–5 PS. Le `DAMAGE` colpiscono subito la creatura avversaria in campo (senza bersaglio: la prossima che entra); le `SHIELD` e le `BURN` restano in attesa e si sommano sulla prossima creatura del proprietario; poi vengono scartate (D-10, D-11, D-34).
Una Spell si pesca solo quando la propria creatura è morta: la "creatura attiva" del catalogo è quindi sempre la **prossima** creatura del proprietario. I modificatori durano finché quella creatura resta in campo (D-12).

| Carta | Sottotipo | Codice | Effetto | Lotto |
|---|---|---|---|---|
| Morso del Rovescio (Bite of the Reverse) | `DAMAGE` | `BITE_OF_REVERSE` | avv. −3 ATK | P1 |
| Frattura della Pelle (Skin Fracture) | `DAMAGE` | `SKIN_FRACTURE` | avv. −5 DEF | P1 |
| Dissoluzione (Dissolution) | `DAMAGE` | `DISSOLUTION` | avv. −2 ATK, −2 DEF | P1 |
| Velo degli Ancorati (Veil of the Anchored) | `SHIELD` | `VEIL_OF_ANCHORED` | +5 DEF | P1 |
| Patto del Registro (Registry Pact) | `SHIELD` | `REGISTRY_PACT` | +2 DEF, +1 ATK | P1 |
| Armatura della Pioggia (Rain Armor) | `SHIELD` | `RAIN_ARMOR` | +2 ATK | P1 |
| SCP-500 · Pillola di Emergenza | `SHIELD` | `SCP_500` | la prossima creatura subisce 4 danni in meno dal **primo danno di qualunque tipo** (attacco o effetto) | P1 (D-33) |
| SCP-109 · Acqua Infinita | `SHIELD` | `SCP_109` | *Borraccia*: +2 DEF; una volta, quando la creatura scende a metà DEF o meno, recupera 3 DEF (fino alla DEF all'entrata) | P2 (M-06, `heal()`) |
| SCP-458 · Pizza Senza Fine | `SHIELD` | `SCP_458` | *Fette da dividere*: +2 DEF alla prossima creatura e +2 DEF a quella dopo | P1 (D-34) |
| Richiamo della Crepa (Call of the Rift) | `BURN` | `CRACK_CALL` | se la carta rivelata è una creatura, entra con +2 ATK e +2 DEF; altrimenti nulla | P1 (D-13) |
| Fusione del Rovescio (Reverse Fusion) | `BURN` | `REVERSE_FUSION` | se la carta rivelata è una creatura, entra con +2 ATK e +1 DEF; altrimenti la prossima creatura ottiene comunque +1 ATK | P1 (D-13) |
| Eco dei Dimenticati (Echo of the Forgotten) | `BURN` | `FORGOTTEN_ECHO` | se la carta rivelata è una creatura, entra con +2 ATK e +1 DEF; altrimenti la prossima creatura ottiene comunque +2 DEF | P1 (D-13) |

**Regola Burn (D-13, decisa l'11 novembre, *Nessuna carta persa*):** la Burn rivela la prossima carta del mazzo del proprietario. Se è una creatura, entra con il bonus fisso della Burn (è proprio la prossima creatura che verrà pescata). Se non è una creatura, **non si scarta nulla** e la carta resta al suo posto; con il mazzo vuoto non succede nulla. Il bonus è un modificatore in attesa (D-34) e dura finché la creatura resta in campo (D-12). Evento di log: `BURN_REVEALED`.

Nessuna Spell domina un'altra: le `DAMAGE` e le prime tre `SHIELD` sono speculari (tutta DEF: Frattura −5 / Velo +5; tutta ATK: Morso −3 / Armatura +2; misto: Dissoluzione / Patto); tra le Burn, Richiamo dà di più quando esce una creatura, Fusione ed Eco danno qualcosa anche quando non esce.

## Hidden

Senza fazione. Valore di ognuna circa 4–5 PS. Messe coperte quando pescate, poi scartate dopo l'attivazione (D-15):

- quelle **con una condizione** (Remnants of Reality, Unseen Exit) restano coperte finché la condizione si verifica;
- le altre si attivano **all'inizio del prossimo scontro del proprietario**, contro la creatura avversaria in campo in quel momento;
- una Hidden pescata nella pescata iniziale **non si attiva nel primo scontro** ("non nel primo turno" del catalogo);
- se più Hidden si attivano insieme, prima le `DAMAGE` e poi le `SURVIVAL`.

"Nello scontro corrente": il bonus dura finché una delle due creature muore; la DEF in più funziona da scudo e la scadenza non uccide (D-35).
"Prima del calcolo dello scontro" (Hidden `DAMAGE`): il malus ATK vale **solo il primo round** dello scontro; il danno alla DEF è permanente.

| Carta | Sottotipo | Codice | Effetto | Lotto |
|---|---|---|---|---|
| Last Sanctuary | `SURVIVAL` | `LAST_SANCTUARY` | propria creatura +5 DEF nello scontro corrente | P1 |
| Survival Instinct | `SURVIVAL` | `SURVIVAL_INSTINCT` | +2 ATK, +1 DEF nello scontro corrente | P1 |
| Remnants of Reality | `SURVIVAL` | `REMNANTS_OF_REALITY` | **con condizione**: quando la propria creatura riceve una penalità, annulla fino a 5 punti di penalità (prima quella che l'ha attivata, poi quelle già presenti, in ordine), senza superare i valori di prima; i punti avanzati si perdono | P2 (D-15, D-35, `heal()`) |
| Unseen Exit | `SURVIVAL` | `UNSEEN_EXIT` | **con condizione**: quando un **attacco** avversario ucciderebbe la propria creatura, previene 3 danni (controllo al passo 3, dopo tutte le altre riduzioni) | P2 (D-15) |
| Hostile Presence | `DAMAGE` | `HOSTILE_PRESENCE` | avv. −5 ATK nel primo round dello scontro | P1 |
| Echo Strike | `DAMAGE` | `ECHO_STRIKE` | avv. −2 ATK nel primo round e −2 DEF | P1 |
| Breaking Point | `DAMAGE` | `BREAKING_POINT` | *Spezzata*: se l'avv. ha 5 DEF o meno, la sua DEF scende a 0 e muore, senza danni al giocatore; altrimenti −3 DEF | P1 (D-16) |
| Static Interference | `DAMAGE` | `STATIC_INTERFERENCE` | **senza condizione**: sospende fino a 3 punti di bonus ATK dell'avv. per tutto lo scontro; se non ne ha, 3 danni all'avv. | P2 (D-35) |

## Terreni

Una Crepa per fazione. In gioco c'è un solo Terreno: quello pescato dopo, da chiunque, sostituisce il precedente (D-19); The Null Witness e The Hollow Wyrm possono toglierlo. Il bonus vale per tutte le creature della fazione in campo, anche avversarie (D-17).
I quattro Terreni valgono circa **3,5 PS per creatura della loro fazione**. Bonus base `P1`; effetto secondario rivisto nella revisione e implementato dopo, `P2` (D-18).

| Carta | Fazione | Codice | Bonus base | Effetto secondario |
|---|---|---|---|---|
| Echo Rift (Crepa Eco) | Theory of the Wound | `ECHO_RIFT` | +1 ATK | *Eco delle ferite*: la prima volta che ogni creatura viene ferita in uno scontro, +1 ATK per il resto di quello scontro |
| Hollow Clearing (Crepa Radura) | Theory of the Invitation | `HOLLOW_CLEARING` | +2 DEF | *Ogni penalità*: ogni penalità DEF che ricevono è ridotta di 1 |
| Watching Rift (Crepa Occhi) | Theory of the Archive | `WATCHING_RIFT` | +1 ATK, +1 DEF | la prima Spell `DAMAGE` subita da ogni creatura in uno scontro fa 1 punto in meno in totale (si toglie prima dalla DEF, poi dall'ATK) |
| Devouring Abyss (Crepa Cavità) | Theory of the Dream | `DEVOURING_ABYSS` | +1 ATK | all'inizio di ogni scontro ogni creatura infligge 1 danno all'avv. (danno da effetto) |

## Oggetti

Uno per fazione, rarità Rare, uno per mazzo; valore di ognuno circa 4–5 PS. **Monouso** (D-47): l'Oggetto resta accanto al mazzo finché il suo effetto scatta una volta, sulla prima creatura del proprietario che ne soddisfa la condizione, poi si consuma. Quando scattano insieme all'entrata, prima il Terreno e poi l'Oggetto (D-48).
La fazione dell'Oggetto non limita il suo effetto: vale per qualunque creatura del proprietario.

| Carta | Fazione | Codice | Effetto | Lotto |
|---|---|---|---|---|
| Badge di Uscita Negata | Theory of the Wound | `DENIED_EXIT_BADGE` | *Nessuna uscita*: la prima volta che la creatura del proprietario morirebbe, per qualunque danno, resta con 1 DEF | P2 (D-36, D-47) |
| Bussola dei Sentieri Falsi | Theory of the Invitation | `FALSE_PATHS_COMPASS` | *Strada lunga*: all'inizio del prossimo scontro, uno scudo di 2 DEF all'inizio di ognuno dei primi tre round (si rinnova, non si somma) | P2 (D-35, D-47) |
| Fascicolo Sigillato 0 | Theory of the Archive | `SEALED_FILE_ZERO` | *Censurato*: il primo effetto di creatura, Spell o Hidden avversaria che colpirebbe la creatura del proprietario viene annullato del tutto (non gli attacchi, non i Terreni) | P2 (D-47) |
| Campanello del Torii | Theory of the Dream | `TORII_BELL` | *Campana d'allarme*: quando la prima Hidden nemica starebbe per attivarsi, viene annullata (scartata senza effetto) | P2 (M-11, D-15, D-47) |

**Ordine contro un danno letale:** prima le prevenzioni del passo 3 (scudi, Unseen Exit); poi, se il danno è ancora letale, l'effetto della creatura stessa (The Undying Blossom resta con 4 DEF); solo dopo il Badge di Uscita Negata.

## Pezzi di motore richiesti dagli effetti

Prima degli effetti di Spell, Hidden, Terreni e Oggetti serve il registro (`ENG-22`); prima di quelli delle creature serve anche `CreatureEffect` (`ENG-23`); prima dei `P2`, questi pezzi in più:

| Pezzo | Serve a | Decisione |
|---|---|---|
| Numero massimo di attivazioni per effetto (niente `onCombatStart`); "primo round", "una volta", "per due round" si contano per entrata in campo | quasi tutte le Uncommon e Rare, The Unseen Matriarch, The Undying Blossom, SCP-001 | D-31, D-33 |
| Stato "ultima avversaria": l'effetto riparte contro ogni nuova creatura avversaria | Hidebehind, Sala d'Attesa Vivente, Nurikabe, Spearfinger, Nure-onna, The Rootbound Doppelganger, The Stitched Corridor | D-33 |
| Controllo delle morti dopo ogni fase | danni all'entrata (SCP-173, The Gnawing Witness, Cervo Bianco, Cartellino Sbiadito, Il Collezionista di Nomi, Rokurokubi, The Nameless Caller, SCP-053, Il Direttore dell'Edificio, Mothman, The Null Witness, La Regina dei Sentieri Perduti, The Hollow Wyrm, SCP-001, The Stitched Corridor), a inizio o fine round (Bell Witch, The Thorned Matriarch, Wendigo, SCP-001), dopo i danni (SCP-106, Nure-onna, The Unseen Matriarch, The Undying Blossom), alla morte (The Threadbound Doll, SCP-2317), Spell e Hidden (Frattura della Pelle, Dissoluzione, Echo Strike, Breaking Point, Static Interference), Terreno (Devouring Abyss) | D-08 |
| Modificatori in attesa nel `PlayerState`, sommati sulla prossima creatura | Spell `SHIELD` e `BURN`, Spell `DAMAGE` senza bersaglio, Il Pastore Muto, Cervo Bianco e il suo gruppo, Bell Witch, SCP-2317, SCP-458 (due modificatori in fila) | D-34 |
| `StatModifier` con origine e durata in round, sospendibile | modificatori sull'avversario per N round (Howlers, SCP-049, Noppera-bō, The Masked Bloom, SCP-053, Yuki-onna, The Face Collector, The Rootbound Doppelganger, Hostile Presence, Echo Strike), bonus per N round (Kappa, Il Doppio di Servizio), bonus "nello scontro corrente" (Last Sanctuary, Survival Instinct, Echo Rift), sospensioni e rimozioni (L'Addetto alle Uscite, Deathmoths, Kitsune, Static Interference, SCP-3999, The Null Witness, The Undying Blossom, Remnants of Reality) | D-35 |
| Contatore delle creature perse nel `PlayerState` | The Drowned Memory, The Nameless Caller | M-07 |
| `CreatureInPlay.heal()` con tetto alla DEF all'entrata (o ai valori di prima) | The Bloodless Hound, Wendigo, SCP-109, Remnants of Reality | — |
| Soglia "metà DEF" controllata dopo ogni fase in cui cambia la DEF | Nure-onna, The Unseen Matriarch, SCP-109 | M-06 |
| Sopravvivenza a un danno letale, prima del controllo morti | The Undying Blossom, Badge di Uscita Negata (Unseen Exit agisce prima, come prevenzione al passo 3) | D-36 |
| Controllo all'entrata delle creature avversarie mentre una creatura è in campo | Mothman, The Stitched Corridor, The Rootbound Doppelganger, Kitsune; esteso all'arrivo di un Terreno per The Hollow Wyrm | — |
| Effetti annullati (`NULLIFY`) | Il Predicatore sotto la Pioggia, SCP-2521 | D-30 |
| Momento "prima dell'attivazione di un effetto avversario" | Baku | M-10 |
| Effetto rubato o copiato: nuova istanza dal registro collegata a un'altra creatura | SCP-2521 (ruba e annulla), Kitsune (copia senza annullare) | — |
| Filtro sul bersaglio | Il Custode delle Porte Incrociate (tutti gli effetti di creatura avversari), Fascicolo Sigillato 0 (una sola applicazione, anche Spell e Hidden) | — |
| Spell che collega un effetto alla creatura che la riceve | SCP-109 | — |
| Modifica dell'ordine del mazzo avversario | La Regina dei Sentieri Perduti (`DECK_REORDERED`) | — |
| Rivelazione della prossima carta del mazzo | Richiamo della Crepa, Fusione del Rovescio, Eco dei Dimenticati (`BURN_REVEALED`) | D-13, M-15 |
| Azioni sulle Hidden avversarie | L'Operatore della Stanza 0 (la prossima Hidden lavora per l'altro giocatore), Campanello del Torii (la annulla) | D-15, M-11 |
| Controllo delle condizioni delle Hidden nei passi del round | Remnants of Reality, Unseen Exit | D-15 |
| Sostituzione e rimozione del Terreno in gioco, con rimozione dei suoi modificatori | tutti i Terreni, The Null Witness, The Hollow Wyrm (`TERRAIN_REMOVED`) | D-19, D-35 |
| DEF temporanea come scudo (assorbe i danni per prima, la scadenza non uccide) | Last Sanctuary, Survival Instinct, Bussola dei Sentieri Falsi | D-35 |
| Classi base per effetti uguali | "primo attacco ridotto per avversaria" (Hidebehind, Sala d'Attesa Vivente, Nurikabe, The Rootbound Doppelganger, The Stitched Corridor); "danni all'entrata" (SCP-173, The Gnawing Witness); "−1 DEF ora e alla prossima" (Cervo Bianco, Cartellino Sbiadito, Il Collezionista di Nomi, Rokurokubi); "avv. −2/−1 ATK nei primi due round" (Howlers, SCP-049, Noppera-bō, The Masked Bloom); "+2 DEF finché resta in campo" (Il Raccoglitore di Orme, The Index Maw); "danni alla morte" (The Threadbound Doll, SCP-2317); "togli i bonus ATK" (L'Addetto alle Uscite, Deathmoths, Static Interference, il sigillo di Kitsune) | D-37 |
