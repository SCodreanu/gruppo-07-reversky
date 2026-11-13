# Revisione degli effetti

Revisione carta per carta degli effetti del *Catalogo completo 120 carte*, prima di qualsiasi simulazione.
Si aggiorna dopo ogni decisione, così il lavoro sopravvive a un cambio di sessione: **all'inizio di ogni sessione si rilegge questo file e si riprende da dove si ferma.**

- Fonte delle carte: [catalogo degli effetti](../effetti/catalogo-effetti.md). Regole: [decisioni aperte](../decisioni-aperte/decisioni-aperte.md).
- Si discutono **solo gli effetti**: ATK e DEF non fanno parte di questa revisione e si decidono con la simulazione (deciso il 10 novembre). **Nomi e rarità restano** quelli del catalogo.
- Le Common e Uncommon che condividono lo stesso effetto vanno bene (D-37); si segnalano solo i doppioni dalle Rare in su e le carte strettamente peggiori di un'altra della stessa rarità.

## Metodo

### Unità di valore — decisa il 10 novembre

L'unità è il **punto statistica** (PS).

| Cosa | Valore |
|---|---|
| +1 DEF permanente | 1 PS |
| 1 danno alla DEF avversaria (permanente) | 1 PS |
| 1 danno prevenuto | 1 PS |
| +1 ATK, oppure −1 ATK avversario, per un round | 1 PS |
| +1 ATK per tutta la permanenza in campo | R PS, con **R = 2,5** (round medi in cui una creatura attacca; si ricalibra con la simulazione) |

- Valore di un effetto = intensità × probabilità che scatti × numero di attivazioni.
- Si scontano le parti che non servono: ATK sotto 0, danno oltre la morte quando muoiono entrambe, effetti che scattano su una creatura già morta.
- Perché funziona: il danno si conserva. Ogni punto di ATK che va a segno toglie DEF avversaria oppure diventa danno al giocatore (la DEF negativa), quindi DEF e danno singolo si confrontano 1 a 1 e l'ATK è danno ripetuto per i round in cui la creatura resta viva.

Riferimenti nella stessa unità:

| Riferimento | Valore |
|---|---|
| Bonus di fazione +2 ATK (scontro favorevole) | circa +5 PS |
| Malus di fazione −1 ATK (scontro sfavorevole) | circa −2,5 PS |
| Media del bonus di fazione su tutti gli scontri | circa +0,6 PS per creatura |

Budget di partenza per l'effetto, da confermare carta per carta:

| Rarità | Budget dell'effetto |
|---|---|
| Common | 0 |
| Uncommon | 2–3 PS |
| Rare | 4–5 PS |
| Mythic | 6–8 PS |
| Legendary | 9–12 PS |
| Mutant | 8–10 PS (deciso l'11 novembre: budget Legendary meno il valore della seconda fazione, circa 1–2 PS) |
| Spell | 4–5 PS ciascuna, qualunque rarità (deciso l'11 novembre: ogni mazzo ne gioca esattamente 6 scelte liberamente, quindi devono valere più o meno uguale) |
| Hidden | 4–5 PS ciascuna, come le Spell (deciso l'11 novembre: ogni mazzo ne gioca esattamente 2, quindi devono valere più o meno uguale) |
| Terreno | circa 3,5 PS per ogni creatura della sua fazione in campo (deciso l'11 novembre: i quattro Terreni si pareggiano tra loro; si cambiano solo gli effetti secondari) |
| Oggetto | 4–5 PS ciascuno, come Spell e Hidden (deciso l'11 novembre: carta monouso in uno slot fisso; i quattro Oggetti devono valere più o meno uguale) |

### Ordine della revisione — deciso il 10 novembre

Per rarità, dalla più bassa alla più alta, tutte le fazioni insieme, così ogni carta si confronta con quelle della sua rarità e le carte semplici fissano la scala prima delle altre:

1. Uncommon (26), in ordine di fazione: Invitation, Wound, Archive, Dream, ognuna nell'ordine del catalogo;
2. Rare (16), Mythic (12), Legendary (8), con lo stesso ordine di fazione;
3. Mutant (2);
4. Spell (12), Hidden (8), Terreni (4), Oggetti (4).

Le Common (28) non hanno effetto e restano fuori dalla revisione.

**Revisione completata l'11 novembre:** le 64 creature con effetto (26 Uncommon, 16 Rare, 12 Mythic, 8 Legendary, 2 Mutant; le 28 Common non hanno effetto), 12 Spell, 8 Hidden, 4 Terreni e 4 Oggetti: 92 carte. Modifiche ai documenti applicate l'11 novembre; prossimo passo: la simulazione (prima carta da controllare: La Regina dei Sentieri Perduti).

Da riprendere nelle Rare:
- Baku: risolto il 10 novembre (*Divoratore di incubi*).
- The Unseen Matriarch: risolta il 10 novembre (*Velo spezzato*).
- The Rootbound Doppelganger: risolta l'11 novembre (*Imboscata a ogni vittima*).

### Carte per passo — deciso il 10 novembre

**Una carta per passo**: dopo ogni carta si aspetta la scelta prima di passare alla successiva.

### Regole decise prima della revisione (10 novembre)

Valgono per tutte le carte; il dettaglio è nelle [decisioni aperte](../decisioni-aperte/decisioni-aperte.md).

| Regola | Decisione |
|---|---|
| "Primo round", "primo attacco", "una volta" (D-31, D-33) | una volta per entrata in campo; conta solo un attacco con danno > 0 |
| DEF temporanea (D-35) | scudo: assorbe i danni per prima, la scadenza non uccide |
| Durata delle Spell (D-12) | finché la creatura resta in campo |
| Hidden (D-15) | con una condizione: quando la condizione si verifica; senza: all'inizio del prossimo scontro del proprietario. **Proposte adottate l'11 novembre:** una Hidden pescata nella pescata iniziale non si attiva nel primo scontro ("non nel primo turno"); "scontro corrente" dura finché una delle due creature muore |
| Terreni (D-19) | un solo Terreno in gioco: quello pescato dopo sostituisce il precedente |
| Effetti secondari dei Terreni (D-18) | si rivedono ora, si implementano dopo il bonus base |
| Oggetti (D-47) | monouso, sulla prima creatura del proprietario che ne soddisfa la condizione |
| Breaking Point (D-16) | DEF 0 uccide |
| Mutant (D-20) | sopra Legendary negli effetti che confrontano le rarità |
| Burn (D-13) | **deciso l'11 novembre:** se la carta rivelata è una creatura entra con un bonus fisso; altrimenti non si scarta nulla |
| Effetto annullato (regola della revisione, 10 novembre) | mentre è annullato non scatta e i suoi bonus ATK sono sospesi; la DEF già guadagnata resta |
| Danno da effetto (regola della revisione, 10 novembre) | "infligge X danni DEF" e "−X DEF" da un effetto sono la stessa cosa: perdita permanente di DEF, **non** un attacco. Le prevenzioni "dal primo attacco" e Unseen Exit non la riducono; la "penalità DEF" di Hollow Clearing, Remnants of Reality e Fascicolo Sigillato 0 sì. "Attacco" = solo il danno del passo 4 |

### Meccaniche del motore

Le schede delle carte rimandano a questi codici. Momenti dell'interfaccia e passi del round sono in [effetti](../effetti/effetti.md#ordine-di-risoluzione-di-un-round): passo 0 entrata (bonus di fazione, Terreno, Oggetto, Hidden senza condizione, `onEnterPlay`), 1 `onRoundStart`, 2 attacco (`modifyOutgoingAttack`), 3 danno ricevuto (`modifyIncomingDamage`), 4 danni simultanei, 5 `afterDamage`, 6 `onRoundEnd`, 7 morti (`onDeath`, `onKill`).

| Codice | Meccanica | Decisione |
|---|---|---|
| M-01 | Contatori per entrata: round dall'entrata, attivazioni usate ("primo", "una volta", "per N round") | D-31, D-33 |
| M-02 | Controllo delle morti dopo ogni fase; una creatura morta prima del passo 4 non attacca | D-08 |
| M-03 | Modificatori in attesa nel `PlayerState`, sommati sulla prossima creatura che entra | D-34 |
| M-04 | `StatModifier` con origine, lato, durata in round, sospendibile; anche sulla creatura avversaria | D-35 |
| M-05 | DEF temporanea come scudo: assorbe i danni per prima, la scadenza non uccide | D-35 |
| M-06 | Soglia "metà DEF": DEF ≤ 50% della DEF all'entrata (bonus compresi) | — |
| M-07 | Contatore delle creature perse nel `PlayerState` | — |
| M-08 | Cura (`heal()`), con un tetto da fissare per carta | — |
| M-09 | Sopravvivenza a un danno letale: passo prima del controllo delle morti | D-36 |
| M-10 | Annullamento di un effetto (`NULLIFY`, con durata) e di un effetto futuro | D-30 |
| M-11 | Zona Hidden: carte coperte; attivazione a inizio scontro o alla condizione; azioni sulle Hidden avversarie | D-15 |
| M-12 | Terreno: uno solo in gioco, sostituito dal successivo; bonus a tutte le creature della sua fazione | D-17, D-19 |
| M-13 | Oggetto: accanto al mazzo, monouso sulla prima creatura del proprietario che ne soddisfa la condizione | D-47 |
| M-14 | Confronto di rarità tramite `rarityOrder` (Mutant sopra Legendary) | D-20 |
| M-15 | Burn: rivelare la prossima carta del mazzo | D-13 (deciso l'11 novembre) |

Zone: mazzo, campo (una creatura per giocatore), zona Hidden, Terreno (uno solo, condiviso), Oggetto (uno per giocatore), scarti.
Durate possibili: un round, N round dall'entrata, lo scontro, la permanenza in campo, la partita.

Carte già segnalate da riprendere quando si arriva a loro: The Hollow Wyrm (con D-19 la condizione non si verifica mai), La Regina dei Sentieri Perduti ("sceglie" non è deterministico), Unseen Exit e Static Interference (condizione esatta, D-15), The Null Witness (Terreno "nemico", D-38), riga orfana dopo SCP-3000 nel catalogo.

## Carte decise

### Wampus Cat — Theory of the Invitation, Uncommon — **riscritta** (10 novembre)

- **Effetto originale:** +1 ATK nel primo round (circa 1,2 PS, sotto il budget e dominato da Kappa, +2 nel primo round).
- **Effetto deciso — *Sguardo della follia*:** all'entrata la creatura avversaria in campo ha **−1 ATK finché resta in campo**.
- **Motore:** `onEnterPlay` (passo 0); modificatore sull'avversaria (M-04, origine `EFFECT`, durata: permanenza in campo dell'avversaria); l'attacco non scende sotto 0. Colpisce solo la creatura in campo al momento dell'entrata e resta anche se Wampus Cat muore prima.
- **Interazioni:** si annulla se un effetto `NULLIFY` entra nello stesso momento (M-10); Remnants of Reality e Fascicolo Sigillato 0 possono cancellare il malus.
- **Valore:** circa 2,2 PS (l'avversaria resta in campo in media circa 2,5 round). Nel budget Uncommon, sotto Yuki-onna (Rare, circa 3 PS).
- **Motivo:** porta la carta nel budget, la distingue da Kappa e dalle quattro Uncommon con "−1 ATK nel primo round", e racconta la leggenda cherokee (chi vede il Wampus Cat impazzisce).

### Hidebehind — Theory of the Invitation, Uncommon — **modificata** (10 novembre)

- **Effetto originale:** previene 2 danni dal primo attacco ricevuto (circa 1,7 PS, poco sotto il budget).
- **Effetto deciso:** previene 2 danni dal **primo attacco di ogni creatura avversaria** che affronta.
- **Motore:** `modifyIncomingDamage` (passo 3). Stato: l'ultima avversaria contro cui ha già prevenuto; quando l'avversaria cambia, lo scudo torna disponibile. Non serve un nuovo momento dell'interfaccia. Conta solo un attacco con danno > 0 (D-33).
- **Interazioni:** si somma a SCP-500 e Badge di Uscita Negata sullo stesso attacco (sottrazioni, minimo 0); Spearfinger la annulla. Unseen Exit va controllata dopo tutte le riduzioni.
- **Valore:** circa 2,3 PS (circa 1,7 nel primo scontro più circa 0,6 per il 35% di casi in cui arriva a un secondo scontro).
- **Motivo:** porta la carta nel budget, la distingue da Sala d'Attesa Vivente e Nurikabe e segue la leggenda (si nasconde di nuovo dietro ogni vittima). L'esempio `09` resta valido (una sola avversaria).

### Il Pastore Muto — Theory of the Invitation, Uncommon — **riscritta** (10 novembre)

- **Effetto originale:** quando muore, 1 danno all'avversaria (circa 1,4 PS, sotto il budget e dominato da The Threadbound Doll, 2 danni alla morte).
- **Effetto deciso — *Custode del gregge*:** quando muore, la **prossima creatura del proprietario entra con +2 DEF**.
- **Motore:** `onDeath` (passo 7 o controllo delle morti dopo qualsiasi fase, M-02); aggiunge +2 DEF ai modificatori in attesa del proprietario (M-03, D-34), come una Spell `SHIELD`. DEF permanente, non temporanea.
- **Interazioni:** funziona anche quando muoiono entrambe; non scatta se il suo effetto è annullato al momento della morte (M-10). Si somma alle Spell `SHIELD` in attesa.
- **Valore:** circa 1,9 PS (2 PS, quasi mai sprecato; solo se la partita finisce con la sua morte).
- **Motivo:** affidabile, nessun pezzo di motore nuovo, distinto da The Threadbound Doll e legato alla carta (il custode passa la guardia al prossimo del gregge).

### Cervo Bianco — Theory of the Invitation, Uncommon — **modificata, con il suo gruppo speculare** (10 novembre)

- **Effetto originale:** all'entrata −1 DEF all'avversaria (circa 1,5 PS, sotto il budget e dominato da SCP-173, The Gnawing Witness e The Nameless Caller).
- **Gruppo speculare:** lo stesso effetto ce l'ha una Uncommon per fazione: Cervo Bianco (Invitation), Cartellino Sbiadito (Wound), Il Collezionista di Nomi (Archive), Rokurokubi (Dream). La decisione vale **per tutte e quattro**; quando si arriva alle altre tre basta una conferma.
- **Effetto deciso:** all'entrata **−1 DEF alla creatura avversaria in campo e −1 DEF alla prossima creatura avversaria che entra**.
- **Motore:** `onEnterPlay` (passo 0); la prima parte è danno da effetto (può uccidere prima dell'attacco, D-08); la seconda è un modificatore in attesa nel `PlayerState` avversario (M-03, D-34), come la parte di Wendigo.
- **Interazioni:** Hollow Clearing ("prima penalità DEF −1") e Il Custode delle Porte Incrociate possono annullare la prima parte; Remnants of Reality e Fascicolo Sigillato 0 possono restituire la DEF.
- **Valore:** circa 2,4 PS (circa 1,5 per la prima parte più circa 0,9 per la seconda, che non va quasi mai sprecata).
- **Motivo:** porta il gruppo nel budget, toglie la dominanza delle due carte con 2 danni all'entrata e tiene uguali le quattro carte speculari.

### The Nameless Caller — Theory of the Invitation, Uncommon — **riscritta** (10 novembre)

- **Effetto originale:** all'entrata −1 DEF all'avversaria, −2 se un nemico è già morto (circa 2,6 PS, nel budget ma sempre peggiore di SCP-173 e The Gnawing Witness, 2 danni all'entrata).
- **Effetto deciso — *Voci dei dispersi*:** all'entrata **1 danno alla creatura avversaria per ogni creatura avversaria già morta in questa partita, massimo 3**.
- **Motore:** `onEnterPlay` (passo 0); danno da effetto, può uccidere prima dell'attacco (D-08). Il conteggio legge il contatore delle creature perse nel `PlayerState` avversario (M-07): conta ogni creatura avversaria morta nella partita, per qualsiasi causa.
- **Interazioni:** Hollow Clearing (prima penalità DEF −1) e Il Custode delle Porte Incrociate possono ridurla o annullarla; Remnants of Reality e Fascicolo Sigillato 0 possono restituire la DEF.
- **Valore:** circa 2,75 PS (debole a inizio partita, più forte della coppia con 2 danni dalla terza morte avversaria). Non si sovrappone a The Drowned Memory (Rare), che conta le proprie creature e dà DEF.
- **Motivo:** tiene l'idea della carta (più dispersi, più forte il richiamo), toglie la dominanza a parità di valore.

### Il Raccoglitore di Orme — Theory of the Invitation, Uncommon — **modificata, con The Index Maw** (10 novembre)

- **Effetto originale:** all'entrata +1 DEF fino alla fine dello scontro (circa 0,9 PS, molto sotto il budget e dominato da L'Ombra del Torii).
- **Gruppo speculare:** The Index Maw (Archive, Uncommon) ha lo stesso effetto; la decisione vale per entrambe, quando si arriva a The Index Maw basta una conferma.
- **Effetto deciso:** all'entrata **+2 DEF finché resta in campo**.
- **Motore:** `onEnterPlay` (passo 0); DEF permanente (origine `EFFECT`), non temporanea.
- **Interazioni:** Il Custode delle Porte Incrociate può annullarla (è un effetto di entrata); non è un modificatore temporaneo, quindi The Null Witness e SCP-3999 non la tolgono.
- **Valore:** circa 1,8 PS. È lo stesso effetto della Spell SCP-109, accettabile per una Uncommon.
- **Motivo:** semplice e affidabile, tiene uguali le due carte speculari.

### Howlers — Theory of the Wound, Uncommon — **modificata, con il suo gruppo speculare** (10 novembre)

- **Effetto originale:** −1 ATK nemico nel primo round (circa 0,9 PS, molto sotto il budget e dominato da Wampus Cat, −1 ATK finché l'avversaria resta in campo).
- **Gruppo speculare:** Howlers (Wound), SCP-049 (Archive), Noppera-bō e The Masked Bloom (Dream) hanno lo stesso effetto; la decisione vale per tutte e quattro, alle altre tre basta una conferma.
- **Effetto deciso:** all'entrata la creatura avversaria ha **−2 ATK nel primo round e −1 ATK nel secondo**.
- **Motore:** `onEnterPlay` (passo 0); modificatore sull'avversaria con durata contata dai round dopo l'entrata (M-01, M-04, origine `EFFECT`); l'attacco non scende sotto 0.
- **Interazioni:** Remnants of Reality e Fascicolo Sigillato 0 possono annullare il malus; Il Custode delle Porte Incrociate lo annulla (è un effetto di entrata).
- **Valore:** circa 2,3 PS (circa 1,7 nel primo round, circa 0,6 nel secondo, che arriva in circa il 60% dei casi). Sotto Yuki-onna (Rare, −2 per due round, circa 3 PS).
- **Motivo:** porta il gruppo nel budget; è concentrato all'inizio, mentre Wampus Cat è costante, quindi né dominato né copia.

### Wretches — Theory of the Wound, Uncommon — **modificata** (10 novembre)

- **Effetto originale:** la prima volta che viene ferito, +1 ATK nel round seguente (circa 0,6 PS, la Uncommon più debole).
- **Effetto deciso — *Furia del ferito*:** la prima volta che viene ferito, **+2 ATK finché resta in campo**.
- **Motore:** `afterDamage` (passo 5), una volta per entrata (M-01); modificatore `EFFECT` permanente (M-04). "Ferito" = una perdita reale di DEF maggiore di 0, da attacco o da effetto, dopo l'eventuale scudo di DEF temporanea. Se viene ferito all'entrata da un effetto avversario, il bonus vale già nel primo round.
- **Interazioni:** si annulla con gli effetti `NULLIFY`; L'Addetto alle Uscite, Deathmoths, Kitsune e Static Interference possono togliere o ridurre il bonus ATK.
- **Valore:** circa 2,2 PS (quasi sempre ferito nel primo round, poi in media circa 1,3 round di attacco in più).
- **Motivo:** tiene l'idea della carta e la porta nel budget. Non copia Shuten-dōji (Legendary, +2 a ogni danno), perché scatta una volta sola. Conseguenza: SCP-096 (+2 ATK a metà DEF) diventa dominata.

### Cartellino Sbiadito — Theory of the Wound, Uncommon — **confermata come Cervo Bianco** (10 novembre)

- **Effetto originale:** all'entrata −1 DEF all'avversaria (uguale al vecchio Cervo Bianco).
- **Effetto deciso:** quello del gruppo di Cervo Bianco: all'entrata **−1 DEF alla creatura avversaria in campo e −1 DEF alla prossima creatura avversaria che entra** (circa 2,4 PS). Motore e interazioni come Cervo Bianco.

### Sala d'Attesa Vivente — Theory of the Wound, Uncommon — **modificata come Hidebehind, con Nurikabe** (10 novembre)

- **Effetto originale:** previene 2 danni dal primo attacco (uguale al vecchio Hidebehind; dopo la modifica di Hidebehind era dominata).
- **Gruppo speculare:** Hidebehind (Invitation), Sala d'Attesa Vivente (Wound), Nurikabe (Dream). La decisione vale anche per **Nurikabe**, già decisa: non serve riproporla.
- **Effetto deciso:** previene 2 danni dal **primo attacco di ogni creatura avversaria** che affronta (circa 2,3 PS). Motore e interazioni come Hidebehind.
- **Motivo:** rimette in pari il gruppo speculare; per la Sala si legge come una stanza che cambia a ogni nuovo visitatore.

### The Gnawing Witness — Theory of the Wound, Uncommon — **mantenuta, con SCP-173** (10 novembre)

- **Effetto:** all'entrata **2 danni alla creatura avversaria** (invariato).
- **Gruppo speculare:** SCP-173 (Archive, Uncommon) ha lo stesso effetto e resta uguale: già decisa, non serve riproporla.
- **Motore:** `onEnterPlay` (passo 0); danno da effetto, può uccidere prima dell'attacco (D-08).
- **Interazioni:** Hollow Clearing lo riduce a 1 contro una creatura della Theory of the Invitation; Il Custode delle Porte Incrociate lo annulla; Remnants of Reality e Fascicolo Sigillato 0 possono restituire la DEF.
- **Valore:** circa 2,8 PS, la Uncommon più forte finora, in cima al budget.
- **Motivo:** nel budget, chiara e deterministica; la coppia speculare è voluta.

### L'Addetto alle Uscite — Theory of the Wound, Uncommon — **riscritta** (10 novembre)

- **Effetto originale:** annulla 1 punto del primo bonus ATK avversario (circa 1,1 PS, non deterministico: quale bonus, quali fonti, quanto dura).
- **Effetto deciso — *Indicazioni cancellate*:** all'entrata la creatura avversaria **perde fino a 2 punti dei suoi bonus ATK finché resta in campo**, compresi quelli che riceve dopo; **se non ne ha, ha −1 ATK nel primo round**.
- **Motore:** `onEnterPlay` (passo 0). "Bonus ATK" = qualsiasi modificatore ATK positivo, di qualsiasi origine (fazione, Terreno, Spell, Hidden, effetto). Sospende fino a 2 punti in totale con la sospensione per origine (M-04, D-35), nell'ordine in cui i bonus sono stati applicati; se all'entrata non ce n'è nessuno, applica il malus di un round (M-01).
- **Interazioni:** si annulla con gli effetti `NULLIFY` e con Il Custode delle Porte Incrociate; contro una creatura della Theory of the Archive toglie il suo +2 di fazione contro la Theory of the Wound.
- **Valore:** circa 2,1 PS.
- **Motivo:** tiene l'idea della carta, la rende deterministica e la porta nel budget. È della stessa famiglia di Kitsune (Legendary), Static Interference (Hidden) e Deathmoths (Mythic), ma molto più piccola: non è un doppione.

### SCP-096 (Shy Guy) — Theory of the Archive, Uncommon — **riscritta** (10 novembre)

- **Effetto originale:** a metà DEF +2 ATK, senza durata scritta (circa 1,1 PS, dominato da Wretches).
- **Effetto deciso — *Il volto visto*:** la **prima creatura avversaria che lo ferisce** viene marchiata; **SCP-096 ha +3 ATK contro quella creatura**.
- **Motore:** `afterDamage` (passo 5) per marchiare, una volta per entrata (M-01): stato = identità della creatura marchiata. `modifyOutgoingAttack` (passo 2) aggiunge +3 solo se l'avversaria in campo è quella marchiata. "Ferito" come per Wretches (perdita reale di DEF maggiore di 0, da attacco o da effetto). Se viene ferito all'entrata da un effetto della creatura avversaria, il bonus vale già nel primo round.
- **Interazioni:** si annulla con gli effetti `NULLIFY`; L'Addetto alle Uscite, Deathmoths, Kitsune e Static Interference possono togliere o ridurre il bonus ATK.
- **Valore:** circa 2,0 PS (marchia quasi sempre la creatura che affronta nel primo round, poi +3 per il resto dello scontro; contro le avversarie successive niente).
- **Motivo:** è la leggenda di SCP-096 (insegue solo chi ha visto il suo volto), nel budget e con una meccanica unica; Wretches (+2 contro tutte) e SCP-096 non si dominano a vicenda.

### SCP-049 (Plague Doctor) — Theory of the Archive, Uncommon — **confermata come Howlers** (10 novembre)

- **Effetto originale:** −1 ATK nemico nel primo round (uguale al vecchio Howlers).
- **Effetto deciso:** quello del gruppo di Howlers: all'entrata la creatura avversaria ha **−2 ATK nel primo round e −1 ATK nel secondo** (circa 2,3 PS). Motore e interazioni come Howlers.

### Il Collezionista di Nomi — Theory of the Archive, Uncommon — **confermata come Cervo Bianco** (10 novembre)

- **Effetto originale:** all'entrata −1 DEF all'avversaria (uguale al vecchio Cervo Bianco).
- **Effetto deciso:** quello del gruppo di Cervo Bianco: all'entrata **−1 DEF alla creatura avversaria in campo e −1 DEF alla prossima creatura avversaria che entra** (circa 2,4 PS). Motore e interazioni come Cervo Bianco.

### SCP-053 (The Young Girl) — Theory of the Archive, Uncommon — **modificata** (10 novembre)

- **Effetto originale:** nel primo round il nemico ha +1 ATK e −2 DEF (circa 1,9 PS; il "−2 DEF nel primo round" contraddiceva la regola del danno da effetto; dominato da SCP-173).
- **Effetto deciso — *Paranoia*:** all'entrata **−3 DEF alla creatura avversaria**, che ha **+2 ATK nel primo round**.
- **Motore:** `onEnterPlay` (passo 0): −3 DEF è danno da effetto (permanente, può uccidere prima dell'attacco, D-08); +2 ATK è un modificatore sull'avversaria di un round (M-01, M-04).
- **Interazioni:** Hollow Clearing riduce il danno di 1 contro una creatura della Theory of the Invitation; Il Custode delle Porte Incrociate annulla tutto l'effetto; Remnants of Reality e Fascicolo Sigillato 0 possono restituire la DEF. Se l'avversaria muore all'entrata (circa 30% dei casi), il +2 ATK non conta.
- **Valore:** circa 2,5 PS.
- **Motivo:** tiene lo scambio della carta (colpo più forte, rischio più alto), è coerente con la regola del danno da effetto, nel budget e non più dominato da SCP-173.

### SCP-2006 (Too Spooky) — Theory of the Archive, Uncommon — **riscritta** (10 novembre)

- **Effetto originale:** all'entrata +1 alla statistica base più bassa (in realtà un aumento di statistica nascosto, deciso dalle statistiche della carta; parità non definita).
- **Effetto deciso — *Imitazione*:** all'entrata, se l'**ATK base della creatura avversaria è più alto del suo, prende quell'ATK base finché resta in campo**.
- **Motore:** `onEnterPlay` (passo 0); confronta l'ATK base delle due carte (`CardDefinition.atk`) e sostituisce l'ATK base di SCP-2006; bonus di fazione e modificatori si sommano dopo. Resta anche contro le avversarie successive.
- **Interazioni:** Il Custode delle Porte Incrociate la annulla (effetto di entrata); i malus all'ATK dell'avversaria non contano, perché il confronto è sui valori base.
- **Valore:** circa 2,1 PS (l'ATK avversaria è più alta in circa metà dei casi, di circa 2 punti).
- **Motivo:** un effetto vero al posto di un aumento di statistica, nel budget e legato alla carta (diventa il mostro che ha davanti). Non si sovrappone a SCP-3999 (Legendary, scambia ATK e DEF avversarie).

### The Index Maw — Theory of the Archive, Uncommon — **confermata come Il Raccoglitore di Orme** (10 novembre)

- **Effetto originale:** all'entrata +1 DEF fino alla fine del combattimento (uguale al vecchio Il Raccoglitore di Orme).
- **Effetto deciso:** all'entrata **+2 DEF finché resta in campo** (circa 1,8 PS). Motore e interazioni come Il Raccoglitore di Orme.

### Kappa — Theory of the Dream, Uncommon — **mantenuta** (10 novembre)

- **Effetto:** **+2 ATK nel primo round** (invariato).
- **Motore:** `modifyOutgoingAttack` (passo 2), nel primo round dopo l'entrata (M-01, D-31).
- **Interazioni:** L'Addetto alle Uscite, Static Interference, Kitsune e Deathmoths possono togliere il bonus; il gruppo di Hidebehind (−2 dal primo attacco) lo assorbe esattamente; gli effetti `NULLIFY` che entrano insieme lo annullano.
- **Valore:** circa 2,3 PS, nel budget. Non dominato da Wretches (+2 dal secondo round) né lo domina.
- **Motivo:** nel budget, chiaro e non dominato. Da riprendere con Il Doppio di Servizio (Rare, +3 nel primo round), che usa lo stesso meccanismo.

### Noppera-bō — Theory of the Dream, Uncommon — **confermata come Howlers** (10 novembre)

- **Effetto originale:** −1 ATK al nemico nel primo round (uguale al vecchio Howlers).
- **Effetto deciso:** quello del gruppo di Howlers: all'entrata la creatura avversaria ha **−2 ATK nel primo round e −1 ATK nel secondo** (circa 2,3 PS). Motore e interazioni come Howlers.

### Rokurokubi — Theory of the Dream, Uncommon — **confermata come Cervo Bianco** (10 novembre)

- **Effetto originale:** all'entrata 1 danno DEF al nemico (con la regola del danno da effetto, uguale al vecchio Cervo Bianco).
- **Effetto deciso:** quello del gruppo di Cervo Bianco: all'entrata **−1 DEF alla creatura avversaria in campo e −1 DEF alla prossima creatura avversaria che entra** (circa 2,4 PS). Motore e interazioni come Cervo Bianco. Il gruppo speculare è completo.

### The Threadbound Doll — Theory of the Dream, Uncommon — **mantenuta** (10 novembre)

- **Effetto:** quando muore, **2 danni alla creatura avversaria** (invariato).
- **Motore:** `onDeath` (passo 7 o controllo delle morti, M-02); danno da effetto, non un attacco. Se uccide la sopravvissuta muoiono entrambe e nessun giocatore subisce danni.
- **Interazioni:** il gruppo di Hidebehind e Unseen Exit non la riducono; Hollow Clearing la riduce di 1 contro una creatura della Theory of the Invitation; The Undying Blossom e Mothman possono sopravvivere se è letale; un effetto `NULLIFY` attivo al momento della morte la annulla.
- **Valore:** circa 2,5 PS, nel budget; nessuna Uncommon la domina (SCP-2317, Mythic, ne è la versione grande).
- **Motivo:** nel budget, chiara e non dominata.

### L'Ombra del Torii — Theory of the Dream, Uncommon — **modificata** (10 novembre)

- **Effetto originale:** +2 DEF con il Terreno Yokai, altrimenti +1, senza durata scritta (circa 1,1 PS, dominato da Il Raccoglitore di Orme e The Index Maw).
- **Effetto deciso:** all'entrata **+3 DEF con il Terreno della Theory of the Dream in gioco, altrimenti +2**, finché resta in campo.
- **Motore:** `onEnterPlay` (passo 0); controlla se in gioco c'è Devouring Abyss (con D-19 un solo Terreno, di chiunque); il Terreno si controlla solo all'entrata. DEF permanente (origine `EFFECT`).
- **Interazioni:** Il Custode delle Porte Incrociate la annulla (effetto di entrata).
- **Valore:** circa 2,05 PS.
- **Motivo:** porta la carta nel budget e la lega al suo Terreno. **Scelta consapevole:** domina Il Raccoglitore di Orme e The Index Maw (+2 DEF sempre); accettato il 10 novembre.

### The Masked Bloom — Theory of the Dream, Uncommon — **confermata come Howlers** (10 novembre)

- **Effetto originale:** all'entrata −1 ATK alla creatura avversaria nel primo round (uguale al vecchio Howlers).
- **Effetto deciso:** quello del gruppo di Howlers: all'entrata la creatura avversaria ha **−2 ATK nel primo round e −1 ATK nel secondo** (circa 2,3 PS). Motore e interazioni come Howlers. Il gruppo speculare è completo.

**Fine delle Uncommon (26 su 26).**

### Spearfinger — Theory of the Invitation, Rare — **modificata** (10 novembre)

- **Effetto originale:** il primo attacco fa +2 danni (circa 2,3 PS: l'effetto di Kappa, Uncommon, su una Rare; dominato da Il Doppio di Servizio, Rare, +3 nel primo round).
- **Effetto deciso:** il **primo attacco contro ogni nuova creatura avversaria** fa **+2**.
- **Motore:** `modifyOutgoingAttack` (passo 2); il +2 è un **bonus ATK** (origine `EFFECT`), quindi L'Addetto alle Uscite, Kitsune, Deathmoths e Static Interference possono toglierlo. Stato: l'ultima avversaria contro cui ha già usato il bonus; si riattiva quando l'avversaria cambia (come Hidebehind).
- **Interazioni:** il gruppo di Hidebehind (−2 dal primo attacco) lo assorbe esattamente.
- **Valore:** circa 3,1 PS, **sotto il budget Rare** (4–5 PS): scelta consapevole del 10 novembre. Nessuna Rare lo domina né è dominata (Il Doppio di Servizio fa di più nel primo round, Spearfinger si ripete).
- **Motivo:** distribuisce il colpo su ogni avversaria e lo distingue da Kappa e da Il Doppio di Servizio.

### Bell Witch — Theory of the Invitation, Rare — **modificata** (10 novembre)

- **Effetto originale:** dal secondo round 2 danni, una volta (circa 2,1 PS; momento non definito).
- **Effetto deciso — *Infestazione che resta*:** all'**inizio del secondo round** 2 danni alla creatura avversaria, una volta; **quando muore, la prossima creatura avversaria entra con −2 DEF**.
- **Motore:** `onRoundStart` del secondo round dopo l'entrata (M-01): danno da effetto prima degli attacchi, se uccide l'avversaria quella non attacca (D-08). `onDeath`: −2 DEF nei modificatori in attesa del `PlayerState` avversario (M-03, D-34).
- **Interazioni:** Hollow Clearing riduce di 1 i danni contro una creatura della Theory of the Invitation; un effetto `NULLIFY` attivo in quel momento li annulla; le prevenzioni dagli attacchi non contano.
- **Valore:** circa 3,9 PS (circa 2,1 dalla prima parte, che scatta in circa il 60% delle entrate; circa 1,8 dalla seconda, quasi mai sprecata).
- **Motivo:** vicina al budget Rare, usa meccanismi già previsti e non copia SCP-001 (Legendary). Somiglia solo in parte a un pezzo di Wendigo (la prossima avversaria con −2 DEF, ma dopo il primo KO).

### Il Predicatore sotto la Pioggia — Theory of the Invitation, Rare — **riscritta** (10 novembre)

- **Effetto originale:** disattiva l'abilità del nemico fino alla fine del secondo round (circa 1,5 PS; quasi uguale a SCP-2521, Rare, "per due round").
- **Effetto deciso — *Sermone che cancella*:** all'entrata **annulla l'effetto della creatura avversaria finché resta in campo**; **se non ha un effetto, ha invece −2 ATK finché resta in campo**.
- **Motore:** `onEnterPlay` con priorità `NULLIFY` (D-30, M-10); se l'avversaria non ha `effectCode`, modificatore −2 ATK sull'avversaria (M-04, origine `EFFECT`). Se entrano insieme, blocca anche l'effetto di entrata avversario.
- **Regola per gli effetti annullati** (proposta, nessuna obiezione il 10 novembre): mentre è annullato, l'effetto non scatta e i suoi bonus ATK sono sospesi; la DEF già guadagnata resta.
- **Valore:** circa 3,3 PS (circa 1,5 contro le creature con effetto, circa 1,7 di −2 ATK contro le Common).
- **Motivo:** fa sempre qualcosa (cancella l'abilità o, se manca, la forza), si avvicina al budget Rare e risolve il doppione con SCP-2521.

### The Bloodless Hound — Theory of the Invitation, Rare — **riscritta** (10 novembre)

- **Effetto originale:** dopo aver eliminato una creatura recupera 2 DEF, al massimo fino alla DEF base, una volta (circa 1,0 PS).
- **Effetto deciso — *Drenaggio senza sangue*:** dopo ogni suo attacco che infligge danni **recupera metà dei danni inflitti** (per difetto), senza superare la DEF all'entrata; **dopo aver eliminato una creatura recupera altri 2**.
- **Motore:** `afterDamage` (passo 5) per il recupero dopo l'attacco, `onKill` (passo 7) per i 2 in più; `heal()` (M-08) con tetto = **DEF all'entrata, bonus compresi** (lo stesso riferimento delle soglie a metà DEF, M-06). "Danni inflitti" = il suo attacco dopo le prevenzioni.
- **Interazioni:** si annulla con gli effetti `NULLIFY`; le prevenzioni avversarie riducono anche il recupero.
- **Valore:** circa 3,6 PS.
- **Motivo:** un meccanismo unico (assorbe vita) che lavora a ogni round e non solo alle uccisioni; metà e non tutto, per non renderla quasi invulnerabile contro creature più deboli.

**Fine delle Rare di Theory of the Invitation.**

### Skin-Stealers — Theory of the Wound, Rare — **modificata** (10 novembre)

- **Effetto originale:** DEF pari al 25% della DEF base avversaria, massimo 3, per un round (circa 1,3 PS, sotto diverse Uncommon).
- **Effetto deciso — *Pelle rubata*:** all'entrata ottiene **DEF pari alla metà della DEF base avversaria** (per difetto, **massimo 4**) **finché resta in campo**.
- **Motore:** `onEnterPlay` (passo 0); "DEF base" = il valore DEF della carta avversaria (`CardDefinition.def`); DEF permanente, origine `EFFECT` (non temporanea, quindi The Null Witness e SCP-3999 non la tolgono).
- **Interazioni:** Il Custode delle Porte Incrociate la annulla (effetto di entrata).
- **Valore:** circa 3,3 PS, **sotto il budget Rare** di circa 1 PS (scelta consapevole del 10 novembre).
- **Motivo:** tiene l'idea della carta (prende la pelle della vittima), più grande e permanente; cresce contro le avversarie più resistenti.

### Il Doppio di Servizio — Theory of the Wound, Rare — **modificata** (10 novembre)

- **Effetto originale:** +3 ATK nel primo round (circa 3,5 PS; lo stesso meccanismo di Kappa, Uncommon, con un punto in più).
- **Effetto deciso — *Doppio turno*:** **+3 ATK nei primi due round** dopo l'entrata.
- **Motore:** `modifyOutgoingAttack` (passo 2), nei round 1 e 2 dopo l'entrata (M-01); bonus ATK (origine `EFFECT`).
- **Interazioni:** L'Addetto alle Uscite ne toglie 2 punti; Kitsune, Deathmoths e Static Interference possono toglierlo; il gruppo di Hidebehind ne assorbe 2 sul primo attacco; gli effetti `NULLIFY` che entrano insieme lo annullano.
- **Valore:** circa 5,0 PS, in cima al budget Rare.
- **Motivo:** nel budget e chiaramente sopra Kappa; con Spearfinger (+2 contro ogni nuova avversaria) nessuna delle due domina l'altra.

### Il Direttore dell'Edificio — Theory of the Wound, Rare — **modificata** (10 novembre)

- **Effetto originale:** riduce la DEF base avversaria del 20%, massimo 3 (in pratica 1 punto: circa 1,5 PS).
- **Effetto deciso — *Demolizione*:** all'entrata, danni alla creatura avversaria pari al **40% della sua DEF base** (per difetto, **massimo 4**).
- **Motore:** `onEnterPlay` (passo 0); "DEF base" = il valore DEF della carta avversaria; danno da effetto (permanente, può uccidere prima dell'attacco, D-08). La DEF all'entrata usata per le soglie a metà DEF non cambia.
- **Interazioni:** Hollow Clearing lo riduce di 1 contro una creatura della Theory of the Invitation; Il Custode delle Porte Incrociate lo annulla; Remnants of Reality e Fascicolo Sigillato 0 possono restituire la DEF.
- **Valore:** circa 3,6 PS (2–3 danni con DEF tipiche, uccide all'entrata in circa il 25% dei casi).
- **Motivo:** tiene l'idea della percentuale (più forte contro le avversarie più resistenti) senza rispecchiare Skin-Stealers (metà della DEF base, stessa fazione).

### L'Operatore della Stanza 0 — Theory of the Wound, Rare — **riscritta** (10 novembre)

- **Effetto originale:** 2 danni e ritarda di un round la prima Hidden nemica (circa 3,3 PS: in pratica The Gnawing Witness più poco; "ritardare" non definito con la nuova D-15).
- **Effetto deciso — *Linee incrociate*:** all'entrata, **la prossima Hidden avversaria che si attiva funziona come se fosse tua**.
- **Motore:** `onEnterPlay` (passo 0) mette uno stato sulla zona Hidden avversaria (M-11): la prossima Hidden che l'avversario attiva, in qualsiasi momento della partita e anche dopo la morte dell'Operatore, si risolve con i lati scambiati. Una `DAMAGE` colpisce la creatura del suo proprietario; una `SURVIVAL` aiuta la tua creatura; una con condizione (Unseen Exit) la controlla sulla tua creatura. Poi viene scartata come sempre.
- **Interazioni:** Il Custode delle Porte Incrociate annulla l'effetto (è di entrata); se l'avversario non attiva più Hidden, non succede nulla.
- **Valore:** circa 4,1 PS (l'avversario ne attiva una in circa il 60% delle partite, con uno scarto grande).
- **Motivo:** unica, nel budget Rare e legata alla carta (un centralino che incrocia le linee). Non copia La Regina dei Sentieri Perduti (Legendary), che annulla la prossima Hidden invece di girarla.

**Fine delle Rare di Theory of the Wound.**

### SCP-106 (Old Man) — Theory of the Archive, Rare — **modificata** (10 novembre)

- **Effetto originale:** riflette 1 danno per i primi due attacchi subiti (circa 2,0 PS).
- **Effetto deciso — *Riflesso più forte*:** **riflette 2 danni** all'attaccante per **ciascuno dei primi due attacchi subiti**.
- **Motore:** `afterDamage` (passo 5); contano solo gli attacchi con danno maggiore di 0 dopo l'entrata (D-33, M-01); il danno riflesso è danno da effetto e può uccidere (D-08; se muoiono entrambe, nessun danno ai giocatori).
- **Interazioni:** Hollow Clearing lo riduce di 1 contro una creatura della Theory of the Invitation; si annulla con gli effetti `NULLIFY`; il danno da effetto non conta come attacco subito.
- **Valore:** circa 4,1 PS.
- **Motivo:** stesso meccanismo, doppio; nel budget Rare e semplice.

### SCP-682 (Hard-to-Destroy Reptile) — Theory of the Archive, Rare — **riscritta** (10 novembre)

- **Effetto originale:** dopo il primo attacco, il successivo fa 2 danni in meno (circa 1,0 PS).
- **Effetto deciso — *Adattamento*:** ogni attacco subito **riduce di 2 i danni di tutti gli attacchi successivi**, cumulativo, **massimo −4**.
- **Motore:** `modifyIncomingDamage` (passo 3); stato = numero di attacchi con danno maggiore di 0 subiti dopo l'entrata (M-01, D-33); vale finché resta in campo, anche contro le avversarie successive. Si somma alle altre prevenzioni (sottrazioni, minimo 0).
- **Interazioni:** si annulla con gli effetti `NULLIFY` (con la regola degli effetti annullati la riduzione è sospesa); il danno da effetto non è un attacco, quindi non viene ridotto e non fa crescere l'adattamento.
- **Valore:** circa 3,5 PS.
- **Motivo:** trasforma in meccanica la descrizione della carta ("capace di adattamento") e la avvicina al budget Rare.

### SCP-2521 — Theory of the Archive, Rare — **riscritta** (10 novembre)

- **Effetto originale:** disattiva l'effetto della creatura nemica per due round (circa 1,5 PS; la versione più debole tra Il Predicatore sotto la Pioggia e Baku).
- **Effetto deciso — *Abilità rubata*:** all'entrata **annulla l'effetto della creatura avversaria finché resta in campo**; **se quella creatura è Rare o inferiore, SCP-2521 usa quell'effetto come proprio** finché resta in campo.
- **Motore:** `onEnterPlay` con priorità `NULLIFY` (D-30, M-10). Se la rarità avversaria ha `rarityOrder` ≤ 3 (Rare o inferiore), crea una **nuova istanza** dell'effetto dal suo `effectCode` (`EffectRegistry.create`) e la collega a SCP-2521: contatori nuovi, e la parte di entrata scatta subito come se SCP-2521 fosse appena entrata. L'effetto rubato resta a SCP-2521 anche dopo la morte della vittima. Contro Mythic, Legendary e Mutant annulla soltanto.
- **Regole per l'effetto rubato:** "la sua fazione" e "creature alleate" si leggono dal lato di SCP-2521; un effetto `NULLIFY` successivo annulla anche l'effetto rubato (regola degli effetti annullati).
- **Valore:** circa 3,5 PS.
- **Motivo:** immediata, visibile e fedele alla carta (sottrae le descrizioni). Il limite a Rare evita di dare a una Rare gli effetti Mythic e Legendary e i loro casi limite. Si sovrappone a Il Predicatore sotto la Pioggia nell'annullamento, ma usa l'effetto rubato al posto del −2 ATK.

### The Drowned Memory — Theory of the Archive, Rare — **modificata** (10 novembre)

- **Effetto originale:** +1 DEF per creatura alleata eliminata, massimo +3 (circa 2,2 PS, vicino a una Uncommon).
- **Effetto deciso — *Memoria profonda*:** all'entrata **+1 DEF per ogni creatura alleata già eliminata (massimo +4)** e **+1 ATK se sono almeno 3**, finché resta in campo.
- **Motore:** `onEnterPlay` (passo 0); legge il contatore delle creature perse nel `PlayerState` del proprietario (M-07); DEF e ATK permanenti (origine `EFFECT`). Si calcola solo all'entrata.
- **Interazioni:** Il Custode delle Porte Incrociate la annulla (effetto di entrata); L'Addetto alle Uscite e gli altri annullatori di bonus ATK possono togliere il +1 ATK.
- **Valore:** circa 3,9 PS.
- **Motivo:** tiene l'identità della carta e aggiunge forza a metà e fine partita, vicino al budget Rare.

**Fine delle Rare di Theory of the Archive.**

### Yuki-onna — Theory of the Dream, Rare — **riscritta** (10 novembre)

- **Effetto originale:** −2 ATK nemico per due round (circa 2,8 PS, appena sopra il gruppo di Howlers).
- **Effetto deciso — *Congelata*:** all'entrata la creatura avversaria **non attacca nel primo round** (il suo attacco è 0).
- **Motore:** `onEnterPlay` (passo 0); modificatore sull'avversaria per il primo round dopo l'entrata che porta il suo attacco a 0 (M-01, M-04), dopo tutti gli altri modificatori.
- **Interazioni:** Remnants of Reality e Fascicolo Sigillato 0 possono annullarlo (è una penalità ATK); Il Custode delle Porte Incrociate lo annulla (effetto di entrata). Gli effetti dell'avversaria che non sono attacchi (danni all'entrata, alla morte, a inizio round) restano.
- **Valore:** circa 4,4 PS, più alto contro le avversarie con molto ATK.
- **Motivo:** meccanica unica, nel budget Rare e fedele alla leggenda (congela i viandanti).

### Jorōgumo — Theory of the Dream, Rare — **modificata** (10 novembre)

- **Effetto originale:** previene 3 danni dal primo attacco (circa 2,4 PS, appena sopra il gruppo di Hidebehind).
- **Effetto deciso — *Doppia illusione*:** previene **3 danni da ciascuno dei primi due attacchi** subiti.
- **Motore:** `modifyIncomingDamage` (passo 3); stato = attacchi con danno maggiore di 0 già prevenuti dopo l'entrata (M-01, D-33). Si somma alle altre prevenzioni (sottrazioni, minimo 0).
- **Interazioni:** si annulla con gli effetti `NULLIFY`; il danno da effetto non è un attacco, quindi non viene ridotto.
- **Valore:** circa 4,0 PS.
- **Motivo:** semplice, nel budget Rare, chiaramente sopra il gruppo di Hidebehind. Non usa "il primo attacco fa 0", che sarebbe uguale a *Congelata* di Yuki-onna. Da confrontare con The Rootbound Doppelganger (Mutant, previene 3 dal primo attacco e −2 ATK per due round).

### Nure-onna — Theory of the Dream, Rare — **modificata** (10 novembre)

- **Effetto originale:** quando la DEF nemica scende a metà, 3 danni, una volta (circa 2,9 PS; quasi uguale a The Unseen Matriarch, Mythic, D-37).
- **Effetto deciso — *Ogni vittima*:** **una volta per ogni creatura avversaria**, quando la sua DEF scende a metà o meno, **3 danni**.
- **Motore:** `afterDamage` (passo 5) e controllo dopo ogni fase in cui la DEF avversaria cambia (anche per danni da effetto); soglia = DEF ≤ 50% della DEF all'entrata dell'avversaria (M-06), con l'avversaria ancora viva. Stato: l'ultima avversaria colpita (come Hidebehind). Danno da effetto: spesso uccide, e l'avversaria morta non attacca nel round seguente (D-08).
- **Interazioni:** Hollow Clearing lo riduce di 1 contro una creatura della Theory of the Invitation; si annulla con gli effetti `NULLIFY`.
- **Valore:** circa 3,9 PS.
- **Motivo:** si ripete contro ogni nuova avversaria, vicino al budget Rare. Resta lo stesso meccanismo di The Unseen Matriarch: da differenziare dal lato delle Mythic.

### The Face Collector — Theory of the Dream, Rare — **modificata** (10 novembre)

- **Effetto originale:** ruba 2 ATK al nemico fino alla fine dello scontro (circa 6 PS: **sopra** il budget Rare, a livello Mythic).
- **Effetto deciso — *Forza presa in prestito*:** ruba **2 ATK nel primo round e 1 ATK nel secondo**: la creatura avversaria ha il malus, The Face Collector guadagna lo stesso bonus.
- **Motore:** `onEnterPlay` (passo 0); due modificatori con durata contata dai round dopo l'entrata (M-01, M-04): −X sull'avversaria, +X su di sé. Se l'avversaria ha meno ATK di quanto dovrebbe perdere, ruba solo quello che c'è (minimo 0) e guadagna lo stesso.
- **Interazioni:** Remnants of Reality e Fascicolo Sigillato 0 possono togliere il malus all'avversaria; L'Addetto alle Uscite, Kitsune, Deathmoths e Static Interference possono togliere il bonus; Il Custode delle Porte Incrociate annulla tutto (effetto di entrata).
- **Valore:** circa 4,4 PS, nel budget Rare.
- **Motivo:** la modifica più piccola che riporta la carta nel budget, tenendo l'idea del furto.

**Fine delle Rare (16 su 16).**

### Mothman — Theory of the Invitation, Mythic — **riscritta** (10 novembre)

- **Effetto originale:** una volta sopravvive con 1 DEF a uno scontro perso per massimo 3 DEF e infligge 2 danni (circa 2,2 PS; sopravvivere con 1 DEF vale poco con la regola del danno; doppione della sopravvivenza di The Undying Blossom, Mythic).
- **Effetto deciso — *Disastro annunciato*:** all'entrata, **e ogni volta che una creatura avversaria entra mentre Mothman è in campo**, quella creatura subisce **3 danni**.
- **Motore:** `onEnterPlay` (passo 0) per la creatura in campo; per le successive, un controllo all'entrata delle creature avversarie finché Mothman è vivo (passo 0 del loro scontro, dopo i bonus e prima degli effetti di entrata). Danno da effetto: può uccidere prima dell'attacco (D-08).
- **Interazioni:** Hollow Clearing lo riduce di 1 contro una creatura della Theory of the Invitation; Il Custode delle Porte Incrociate annulla la parte all'entrata di Mothman; gli effetti `NULLIFY` lo sospendono.
- **Valore:** circa 6 PS, nel budget Mythic.
- **Motivo:** effetto da Mythic, unico, fedele alla leggenda (ogni arrivo incontra il disastro annunciato); lascia la sopravvivenza a The Undying Blossom e risolve il doppione.

### Bigfoot — Theory of the Invitation, Mythic — **modificata** (10 novembre)

- **Effetto originale:** +2 ATK contro Rare o superiori, +1 DEF contro Common e Uncommon (circa 1,8 PS; il +1 DEF non diceva se valeva a ogni nuova avversaria).
- **Effetto deciso — *Ammazzagiganti*:** contro **Rare o superiori** +4 ATK e **2 danni in meno da ogni loro attacco**; contro **Common e Uncommon** +2 ATK.
- **Motore:** `modifyOutgoingAttack` (passo 2) e `modifyIncomingDamage` (passo 3), controllati sul `rarityOrder` dell'avversaria in campo a ogni attacco (Rare = 3 o più; Mutant sopra Legendary, D-20). Nessuna parte legata all'entrata, quindi vale contro ogni avversaria.
- **Interazioni:** gli annullatori di bonus ATK (L'Addetto alle Uscite, Kitsune, Deathmoths, Static Interference) possono togliere il bonus; la riduzione si somma alle altre prevenzioni; il danno da effetto non è un attacco.
- **Valore:** circa 5,8 PS, vicino al budget Mythic.
- **Motivo:** una sola identità chiara (brilla contro le creature grandi). Condivide il +4 con Hounds (Mythic) ma con un'altra condizione.

### The Thorned Matriarch — Theory of the Invitation, Mythic — **riscritta** (10 novembre)

- **Effetto originale:** a metà DEF sacrifica 3 DEF per infliggere 5 danni, se sopravvive al sacrificio, una volta (circa 1,4 PS: finestra troppo stretta).
- **Effetto deciso — *Offerta di rovi*:** all'**inizio di ogni round**, se la sua **DEF è più alta di quella avversaria**, **sacrifica 2 DEF e infligge 4 danni** alla creatura avversaria.
- **Motore:** `onRoundStart` (passo 1); il sacrificio è una perdita della propria DEF (la condizione garantisce che resti viva); i 4 danni sono danno da effetto prima degli attacchi e possono uccidere (D-08). Il confronto usa le DEF attuali.
- **Interazioni:** Hollow Clearing riduce di 1 i danni contro una creatura della Theory of the Invitation; si sospende con gli effetti `NULLIFY`.
- **Valore:** circa 6,5 PS (con la DEF alta di una Mythic scatta spesso 2 volte o più).
- **Motivo:** tiene il sacrificio, ma attivo finché è la più forte; unica e nel budget Mythic.

**Fine delle Mythic di Theory of the Invitation.**

### Hounds — Theory of the Wound, Mythic — **modificata** (10 novembre)

- **Effetto originale:** quando la DEF nemica è a metà, +4 ATK fino alla fine del combattimento (circa 1,8 PS: il +4 arrivava quando l'avversaria moriva comunque).
- **Effetto deciso — *Odore del sangue*:** **+4 ATK contro ogni creatura avversaria già ferita** (DEF più bassa della sua DEF all'entrata).
- **Motore:** `modifyOutgoingAttack` (passo 2), controllato a ogni attacco sull'avversaria in campo (DEF attuale < DEF all'entrata, M-06); nessuno stato.
- **Interazioni:** gli annullatori di bonus ATK possono toglierlo; gli effetti `NULLIFY` lo sospendono.
- **Valore:** circa 7 PS (entra quasi sempre contro una sopravvissuta ferita; dopo il primo round ogni avversaria è ferita).
- **Motivo:** nel budget Mythic, semplice, fedele alla descrizione (attratti dalle prede indebolite); esce dalla famiglia "metà DEF" di Nure-onna e The Unseen Matriarch.

### Deathmoths — Theory of the Wound, Mythic — **modificata** (10 novembre)

- **Effetto originale:** −2 ATK nemico per due round e annulla 1 ulteriore bonus ATK (circa 3,6 PS; pezzi già presenti su carte più economiche).
- **Effetto deciso — *Divoratrici di luce*:** all'entrata la creatura avversaria ha **−2 ATK finché resta in campo** e **perde fino a 2 punti dei suoi bonus ATK** (anche quelli ricevuti dopo).
- **Motore:** `onEnterPlay` (passo 0); malus sull'avversaria (M-04, origine `EFFECT`) e sospensione dei bonus ATK per origine (D-35), con la stessa regola di L'Addetto alle Uscite (qualsiasi origine, nell'ordine di applicazione, per tutta la permanenza dell'avversaria).
- **Interazioni:** Remnants of Reality e Fascicolo Sigillato 0 possono togliere il malus; Il Custode delle Porte Incrociate annulla l'effetto (di entrata). Il malus resta anche se Deathmoths muore prima.
- **Valore:** circa 5,8 PS, vicino al budget Mythic.
- **Motivo:** l'originale reso permanente. Contiene una versione più grande di Wampus Cat e lo stesso annullamento di L'Addetto alle Uscite: stesse famiglie su rarità diverse.

### Il Custode delle Porte Incrociate — Theory of the Wound, Mythic — **riscritta** (10 novembre)

- **Effetto originale:** annulla il prossimo effetto di ingresso nemico e ottiene +2 DEF (circa 3 PS; "prossimo" non definito).
- **Effetto deciso — *Inviolabile*:** **gli effetti delle creature avversarie non lo colpiscono** (niente danni da effetto, niente malus, niente annullamenti; i danni degli attacchi sì); ha **+2 ATK contro creature che hanno un effetto**.
- **Motore:** filtro sul bersaglio: ogni effetto con origine `EFFECT` di una creatura avversaria che lo prenderebbe di mira viene ignorato (danni, modificatori, `NULLIFY`); `modifyOutgoingAttack` +2 se l'avversaria ha un `effectCode`. Spell, Hidden, Terreni e Oggetti avversari lo colpiscono normalmente.
- **Interazioni:** Mothman, Yuki-onna (*Congelata* riguarda l'attacco avversario, quindi resta), il gruppo di Cervo Bianco, Deathmoths, Il Predicatore sotto la Pioggia e SCP-2521 non lo toccano; gli annullatori di bonus ATK sono effetti, quindi non tolgono il suo +2.
- **Valore:** circa 5 PS, sotto il budget Mythic perché dipende dal mazzo avversario (scelta consapevole del 10 novembre).
- **Motivo:** unica e legata alla carta (una serratura che nessuno apre).

**Fine delle Mythic di Theory of the Wound.**

### SCP-2317 (A Door in the Woods) — Theory of the Archive, Mythic — **mantenuta** (10 novembre)

- **Effetto:** quando muore, **4 danni alla creatura avversaria** e **+1 ATK alla prossima creatura alleata** (invariato).
- **Motore:** `onDeath` (passo 7 o controllo delle morti, M-02); danno da effetto (se uccide la sopravvissuta muoiono entrambe e nessun giocatore subisce danni); +1 ATK nei modificatori in attesa del proprietario (M-03, D-34), **finché la prossima creatura resta in campo** (come le Spell, D-12).
- **Interazioni:** Hollow Clearing riduce il danno di 1 contro una creatura della Theory of the Invitation; Il Custode delle Porte Incrociate è immune al danno; The Undying Blossom può sopravvivere; un effetto `NULLIFY` attivo alla morte annulla tutto.
- **Valore:** circa 6 PS, nel budget Mythic.
- **Motivo:** nel budget e chiara; è la versione grande di The Threadbound Doll e Il Pastore Muto.

### SCP-3000 (Anantashesha) — Theory of the Archive, Mythic — **riscritta** (10 novembre)

- **Effetto originale:** +3 ATK contro Rare o superiori (circa 1,8 PS; strettamente dominato da Bigfoot, *Ammazzagiganti*).
- **Effetto deciso — *Pelle abissale*:** **+2 ATK** finché resta in campo; contro **Rare o superiori** subisce **metà dei danni** dai loro attacchi (per difetto).
- **Motore:** `modifyOutgoingAttack` (passo 2) per il +2; `modifyIncomingDamage` (passo 3) dimezza l'attacco se l'avversaria ha `rarityOrder` ≥ 3 (Mutant sopra Legendary, D-20). Il dimezzamento si applica dopo le altre prevenzioni (proposta: prima le sottrazioni, poi la metà).
- **Interazioni:** gli annullatori di bonus ATK possono togliere il +2; il danno da effetto non è un attacco, quindi non viene dimezzato.
- **Valore:** circa 5,9 PS, vicino al budget Mythic. Né lei né Bigfoot dominano l'altra.
- **Riga orfana del catalogo:** dopo SCP-3000 il catalogo ha "previene 2 danni dal primo attacco — Nessun effetto", probabilmente un resto di questa carta; non si sa con certezza. Da segnalare a chi cura il catalogo.

### The Null Witness — Theory of the Archive, Mythic — **modificata** (10 novembre)

- **Effetto originale:** rimuove il Terreno nemico e i modificatori temporanei, infligge 4 DEF e si autoelimina (effetto da circa 5–6 PS, ma la carta si elimina da sola: una Spell che occupa uno dei 20 posti creatura).
- **Effetto deciso — *Protocollo di cancellazione*:** all'entrata **4 danni** alla creatura avversaria, che **perde i suoi modificatori temporanei**; **il Terreno in gioco viene rimosso se l'ha pescato l'avversario**. **Resta in campo.**
- **Motore:** `onEnterPlay` (passo 0). "Modificatori temporanei" = i modificatori dell'avversaria che hanno una durata (round, scontro); restano quelli permanenti, come le Spell (D-12). Il Terreno rimosso va negli scarti e i suoi bonus si tolgono (D-19, regola dello scudo per la DEF). Danno da effetto: può uccidere prima dell'attacco (D-08).
- **Interazioni:** Hollow Clearing riduce il danno di 1 contro una creatura della Theory of the Invitation; Il Custode delle Porte Incrociate è immune; se il Terreno in gioco l'ha pescato il proprietario di The Null Witness, resta.
- **Valore:** circa 6,5 PS, nel budget Mythic.
- **Motivo:** tiene tutta l'identità della carta (cancella Terreno e modificatori) senza autoeliminazione. **Chiude D-38.**

### Baku — Theory of the Dream, Mythic — **modificata** (10 novembre)

- **Effetto originale:** annulla l'effetto nemico per due round e +3 DEF (circa 4,2 PS; sotto Il Predicatore sotto la Pioggia e SCP-2521, Rare, che annullano finché l'avversaria resta in campo).
- **Effetto deciso — *Divoratore di incubi*:** finché Baku è in campo, **ogni volta che l'effetto di una creatura avversaria sta per attivarsi, Baku lo divora**: l'attivazione non avviene e **Baku guadagna +2 DEF**.
- **Motore:** nuovo momento "prima che un effetto avversario si attivi" (M-10), controllato prima di ogni attivazione che produrrebbe un evento `EFFECT_TRIGGERED`; vale anche per gli effetti di entrata delle nuove avversarie (Baku risolve per primo, priorità `NULLIFY`). DEF permanente, origine `EFFECT`.
- **Dettagli proposti:** un effetto continuo (bonus ATK finché resta in campo, prevenzioni) viene divorato alla prima applicazione e resta spento per quella creatura (regola degli effetti annullati); **+2 DEF al massimo una volta per creatura avversaria**, così un effetto che si ripete ogni round non dà DEF all'infinito.
- **Interazioni:** Il Custode delle Porte Incrociate è immune (*Inviolabile*); gli effetti di Spell, Hidden, Terreni e Oggetti non sono effetti di creatura e non si divorano. Due Baku uno contro l'altro: risolve per primo quello del giocatore che risolve per primo nell'ordine deterministico del round, e l'altro viene divorato.
- **Valore:** circa 5,7 PS, vicino al budget Mythic.
- **Motivo:** meccanica unica e reattiva, diversa dagli annullatori Rare (che spengono una creatura; Baku mangia ogni attivazione e cresce), fedele alla descrizione.

### The Unseen Matriarch — Theory of the Dream, Mythic — **modificata** (10 novembre)

- **Effetto originale:** quando la DEF nemica scende a metà, 4 danni e −1 ATK fino a fine scontro, una volta (circa 3 PS; sotto Nure-onna, *Ogni vittima*, Rare, con lo stesso meccanismo, D-37).
- **Effetto deciso — *Velo spezzato*:** la soglia passa **su di lei**: la prima volta che la **sua** DEF scende a metà o meno, la creatura avversaria subisce **danni pari alla DEF che ha perso (massimo 8)** e **−1 ATK finché resta in campo**.
- **Motore:** controllo della soglia dopo ogni fase in cui cambia la sua DEF (M-06: metà = 50% o meno della DEF all'entrata, bonus compresi). "DEF persa" = DEF all'entrata meno DEF attuale al momento del controllo; conta solo la perdita reale (la DEF temporanea che fa da scudo non conta, come per "ferita"). Si attiva solo se è ancora viva (DEF > 0). Una volta per entrata in campo (D-33), stato `used`. Danno da effetto: può uccidere prima dell'attacco (D-08).
- **Interazioni:** Hollow Clearing riduce il danno di 1 contro una creatura della Theory of the Invitation; si annulla con Il Predicatore sotto la Pioggia e SCP-2521 e viene divorato da Baku; Il Custode delle Porte Incrociate è immune.
- **Valore:** circa 6 PS, nel budget Mythic.
- **Motivo:** tiene l'idea della soglia a metà DEF ma guarda il proprio lato, quindi non ripete più Nure-onna; più viene colpita, più forte risponde (il velo che riflette).

### The Undying Blossom — Theory of the Dream, Mythic — **modificata** (10 novembre)

- **Effetto originale:** una volta sopravvive a un danno letale con 4 DEF, perdendo i bonus temporanei (circa 4,8 PS, sotto il budget Mythic).
- **Effetto deciso — *Maledizione cremisi*:** una volta, quando subirebbe un danno letale, **resta con 4 DEF** e perde i suoi bonus temporanei; **la creatura avversaria subisce 3 danni**.
- **Motore:** `afterDamage` prima del controllo delle morti (D-36), stato `used`, una volta per entrata in campo (D-33). Letale = DEF ≤ 0 (D-16), da qualunque fonte: attacco, effetto di creatura, Spell, Hidden, Terreno, Oggetto. "Bonus temporanei" = i modificatori con una durata (D-35). I 3 danni vanno sempre alla creatura avversaria in campo (ce n'è una sola), sono danno da effetto e possono ucciderla subito (D-08).
- **Morte doppia:** se nello stesso passo morirebbero entrambe, lei resta con 4 DEF e l'avversaria muore; l'avversario subisce i danni della DEF negativa della sua creatura, lei non ne causa.
- **Interazioni:** Il Predicatore sotto la Pioggia e SCP-2521 la annullano; Baku divora l'attivazione (lei muore, Baku +2 DEF); Il Custode delle Porte Incrociate non subisce i 3 danni, ma lei sopravvive lo stesso (l'effetto su di sé non lo tocca); Hollow Clearing riduce i 3 danni di 1 contro una creatura della Theory of the Invitation.
- **Valore:** circa 7 PS, nel budget Mythic.
- **Motivo:** la vendetta dell'onryō che rifiorisce; nessun'altra carta unisce sopravvivenza e vendetta. Chiude le domande del catalogo (danni degli effetti, morte doppia).

### Wendigo — Theory of the Invitation, Legendary — **modificata** (10 novembre)

- **Effetto originale:** a fine round −1 DEF all'avversaria; alla prima eliminazione recupera 3 DEF e la prossima avversaria entra con −2 DEF (circa 8,2 PS, appena sotto il budget Legendary).
- **Effetto deciso — *Insaziabile*:** a fine round la creatura avversaria perde **1 DEF più 1 per ogni nemica che Wendigo ha eliminato**, massimo 3; **ogni eliminazione** fa recuperare **3 DEF**, fino alla DEF all'entrata.
- **Motore:** `onRoundEnd` (passo 6) per il drenaggio, danno da effetto che può uccidere (D-08); `onKill` per il contatore delle eliminazioni e il recupero con `heal()` (tetto: DEF all'entrata, come The Bloodless Hound). Il contatore vale per entrata in campo (D-33). Conta anche un'eliminazione fatta dal drenaggio stesso.
- **Interazioni:** Hollow Clearing riduce il drenaggio di 1 contro una creatura della Theory of the Invitation; Il Custode delle Porte Incrociate è immune; Baku divora le attivazioni (+2 DEF una volta); Il Predicatore sotto la Pioggia e SCP-2521 la annullano.
- **Valore:** circa 9,5 PS, nel budget Legendary.
- **Motivo:** il Wendigo cresce a ogni pasto e non è mai sazio; effetto che aumenta durante la partita, unico nel catalogo. Toglie il modificatore in attesa (D-34), quindi il motore è più semplice. Domanda del catalogo risolta (recupero con tetto alla DEF all'entrata).

### La Regina dei Sentieri Perduti — Theory of the Invitation, Legendary — **modificata** (11 novembre)

- **Effetto originale:** sceglie tra −4 DEF all'avversaria e annullare la prossima Hidden nemica; alla prima eliminazione applica l'altro (circa 7 PS con qualunque regola fissa; **non deterministico**).
- **Effetto deciso — *Sentieri deviati*:** all'entrata e a **ogni eliminazione** guarda le **prossime tre creature** nel mazzo avversario e manda **quella di rarità più alta** in fondo al mazzo (a parità, quella che verrebbe pescata prima); all'entrata infligge anche **−2 DEF** alla creatura avversaria.
- **Motore:** `onEnterPlay` e `onKill`. Nuovo pezzo di motore: modifica dell'ordine del mazzo avversario (`PlayerState`), deterministica perché l'ordine del mazzo viene dal seed. Si contano solo le creature; Spell, Hidden, Terreni e Oggetti restano al loro posto. Rarità confrontata con `rarityOrder` (Mutant sopra Legendary, D-20). Con meno di due creature nel mazzo avversario non succede nulla. Nuovo evento di log (proposta: `DECK_REORDERED`, con la carta spostata). Il −2 DEF è danno da effetto (D-08).
- **Interazioni:** Il Custode delle Porte Incrociate è immune al −2 DEF, ma non protegge il mazzo del suo proprietario (lo spostamento non lo bersaglia); Baku divora le attivazioni; Il Predicatore sotto la Pioggia e SCP-2521 la annullano (niente spostamenti alle eliminazioni); Hollow Clearing riduce il −2 di 1 contro una creatura della Theory of the Invitation.
- **Valore:** circa 9 PS, **stimato**: dipende dal mazzo avversario. È la prima carta da controllare con la simulazione.
- **Motivo:** "riordina le strade" alla lettera, unica nel catalogo e deterministica. Toglie la parte sulle Hidden e la domanda D-15 collegata.

### Smiler — Theory of the Wound, Legendary — **modificata** (11 novembre)

- **Effetto originale:** +2 ATK per ogni round sopravvissuto, dal round seguente, massimo +6 (circa 9 PS, ma **dominato quasi sempre da Shuten-dōji**, Legendary, +2 ATK dopo ogni danno subito, massimo +8).
- **Effetto deciso — *Sorriso nel buio*:** per ogni round in cui sopravvive, Smiler ottiene **+1 ATK (massimo +3)** e la creatura avversaria **−1 ATK (massimo −3)**.
- **Motore:** `onRoundEnd` dopo il controllo delle morti (passo 7): "sopravvive" = è viva alla fine del round. Il +1 ATK di Smiler resta contro le nuove avversarie (è un bonus ATK, quindi Deathmoths, L'Addetto alle Uscite e gli annullatori possono toglierlo). Il −1 ATK è un modificatore sulla creatura avversaria, origine `EFFECT`, che dura finché quella creatura resta in campo (anche se Smiler muore prima); il contatore del −3 vale per ogni avversaria, quindi una nuova avversaria parte da 0.
- **Interazioni:** Il Custode delle Porte Incrociate è immune al −1 ATK (Smiler cresce lo stesso); Baku divora le attivazioni; Il Predicatore sotto la Pioggia e SCP-2521 la annullano.
- **Valore:** circa 9 PS (stessa oscillazione totale dell'originale, metà su di sé e metà sull'avversaria), nel budget Legendary.
- **Motivo:** toglie la dominanza di Shuten-dōji (Smiler fa paura, Shuten si arrabbia) e segue lo Smiler delle Backrooms, che si nutre della paura nel buio. Domanda del catalogo risolta (il bonus resta contro la prossima avversaria).

### The Hollow Wyrm — Theory of the Wound, Legendary — **modificata** (11 novembre)

- **Effetto originale:** se entrambi i giocatori hanno un Terreno, annulla quello avversario per due round e infligge 2 danni (0 PS: con D-19 in gioco c'è un solo Terreno, quindi la condizione non si verifica mai).
- **Effetto deciso — *Divoratore di Crepe*:** all'entrata **inghiotte il Terreno in gioco, chiunque l'abbia pescato**: il Terreno viene scartato e il Wyrm ottiene **+3 ATK e +3 DEF finché resta in campo**. Un Terreno pescato mentre è in campo viene inghiottito allo stesso modo. Se all'entrata non c'è nessun Terreno, infligge **4 danni** alla creatura avversaria.
- **Motore:** `onEnterPlay` e controllo all'arrivo di un Terreno mentre il Wyrm è in campo (stesso pezzo di motore di Mothman, esteso ai Terreni). Il Terreno inghiottito va negli scarti e i suoi bonus si tolgono con le regole di D-19 (regola dello scudo per la DEF); un Terreno inghiottito all'arrivo non si piazza e non dà bonus. Ogni Terreno inghiottito dà +3/+3 (al massimo 2 per partita: un Terreno per mazzo). I 4 danni sono danno da effetto (D-08). Evento di log: proposta `TERRAIN_REMOVED` con la causa.
- **"Le Crepe":** nel catalogo ogni Terreno è una Crepa (Eco, Radura, Occhi, Cavità), quindi "inghiotte le Crepe" = mangia i Terreni.
- **Interazioni:** può inghiottire anche Echo Rift, il Terreno della sua fazione (deterministico, accettato). The Null Witness toglie solo il Terreno pescato dall'avversario e non guadagna nulla. Il Custode delle Porte Incrociate è immune ai 4 danni; se il Wyrm viene annullato (Il Predicatore sotto la Pioggia, SCP-2521) il bonus ATK è sospeso, la DEF resta e i nuovi Terreni non vengono inghiottiti; Baku divora le attivazioni.
- **Valore:** circa 9,5 PS, nel budget Legendary.
- **Motivo:** la carta ora funziona, segue la descrizione alla lettera e dà al Terreno una vera carta che lo contrasta.

### SCP-001 (When Day Breaks) — Theory of the Archive, Legendary — **modificata** (11 novembre)

- **Effetto originale:** all'entrata 3 danni, poi 2 danni a inizio round per al massimo 3 round (circa 7,5 PS, appena sotto il budget Legendary).
- **Effetto deciso — *Alba piena*:** all'entrata **3 danni**, poi **3 danni** a inizio round per i **3 round seguenti**, alla creatura avversaria in campo in quel momento.
- **Motore:** `onEnterPlay` (passo 0) e `onRoundStart` (passo 1) nei tre round dopo quello di entrata (SCP-001 entra al passo 0 del suo primo round). I round si contano dall'entrata di SCP-001, per entrata in campo (D-33), stato "round rimanenti". Se l'avversaria muore prima, i danni dei round rimanenti colpiscono la nuova avversaria. Danno da effetto: può uccidere prima dell'attacco (D-08).
- **Interazioni:** Hollow Clearing riduce ogni colpo di 1 contro una creatura della Theory of the Invitation; Il Custode delle Porte Incrociate è immune; Baku divora le attivazioni; Il Predicatore sotto la Pioggia e SCP-2521 la annullano (i round rimanenti si perdono).
- **Valore:** circa 9,5 PS, nel budget Legendary.
- **Motivo:** tiene l'identità dell'originale (danni a inizio round per più round, unica nel catalogo) e la porta nel budget. Domanda del catalogo risolta (i round rimanenti passano alla nuova avversaria).

### SCP-3999 (I Am At The Center Of Everything That Happens To Me) — Theory of the Archive, Legendary — **modificata** (11 novembre)

- **Effetto originale:** all'entrata azzera i bonus temporanei dell'avversaria e ne scambia ATK e DEF base per due round, minimo 1 (circa 4 PS con molta varianza: lo scambio aiuta o danneggia a seconda delle statistiche dell'avversaria; domande aperte su quale DEF scambiare e su come tornare indietro).
- **Effetto deciso — *Riscrittura della realtà*:** **all'entrata e ogni due round** (nel suo 3°, 5°, … round in campo) la creatura avversaria **perde tutti i bonus ATK e DEF, da qualunque fonte** (effetti, Spell, Oggetti, Terreno) e torna ai valori base; i danni subiti restano. Ogni volta SCP-3999 ottiene **+1 ATK** (massimo +3).
- **Motore:** `onEnterPlay` (passo 0) e `onRoundStart` (passo 1) nei round dispari dopo l'entrata, contati per entrata in campo (D-33). Si tolgono tutti gli `StatModifier` positivi dell'avversaria con qualunque origine (D-35), anche quelli permanenti delle Spell (D-12); i bonus DEF si tolgono con la regola dello scudo. I bonus del Terreno tornano solo se il Terreno viene ripescato o se la creatura rientra (una creatura già in campo non li riceve di nuovo). Il bonus di fazione non è un modificatore e resta. Il +1 ATK è un bonus ATK di SCP-3999 (rimovibile da Deathmoths e simili).
- **Interazioni:** Il Custode delle Porte Incrociate è immune; Baku divora le attivazioni; Il Predicatore sotto la Pioggia e SCP-2521 la annullano. È la versione ciclica e completa della famiglia "togli i bonus" (Deathmoths, L'Addetto alle Uscite, The Null Witness, Static Interference), che ne tolgono solo una parte.
- **Valore:** circa 9 PS, nel budget Legendary.
- **Motivo:** tiene l'idea della realtà riscritta a cicli, toglie lo scambio inaffidabile e risolve le due domande del catalogo.

### Shuten-dōji — Theory of the Dream, Legendary — **confermata** (11 novembre)

- **Effetto:** dopo ogni danno DEF subito **+2 ATK cumulativi, massimo +8**.
- **Motore:** `afterDamage`, stato `bonus`. Conta ogni perdita reale di DEF sopra 0 dopo gli scudi (stessa definizione di "ferita"), **anche dai danni degli effetti**. Il bonus vale dal primo attacco successivo: un danno subito al passo 4 conta dall'attacco del round seguente, un danno da effetto di entrata prima del passo 2 conta già nell'attacco di quel round.
- **Dettagli:** il bonus resta contro le nuove avversarie finché Shuten-dōji è in campo; è un bonus ATK, quindi Deathmoths, L'Addetto alle Uscite e SCP-3999 possono toglierlo, e gli annullatori lo sospendono.
- **Valore:** circa 9,5 PS, nel budget Legendary.
- **Motivo:** chiara, deterministica e nel budget; dopo *Sorriso nel buio* di Smiler non domina più nessuna carta. Wretches (*Furia del ferito*, Uncommon, un solo +2) ne è la versione piccola, non una copia. Domanda del catalogo risolta (contano anche i danni degli effetti).

### Kitsune — Theory of the Dream, Legendary — **modificata** (11 novembre)

- **Effetto originale:** sigilla fino a 3 punti di bonus ATK nemici per tre round, +2 DEF se sigilla (circa 3,3 PS, livello Uncommon; la più debole della famiglia "togli i bonus").
- **Effetto deciso — *Mille forme*:** Kitsune **prende la forma dell'avversaria**: finché sono entrambe in campo **copia l'effetto della creatura avversaria** (l'avversaria tiene il suo); se copia qualcosa, **+3 DEF**. Sigilla inoltre **fino a 3 punti di bonus ATK** dell'avversaria finché resta in campo.
- **Motore:** stesso pezzo di motore di SCP-2521 (nuova istanza dell'effetto dal registro collegata a un'altra creatura), ma senza annullare l'originale e contro ogni rarità. La copia si prende all'entrata di Kitsune e all'entrata di ogni nuova avversaria (stesso controllo di Mothman); parte come se Kitsune fosse appena entrata (il suo `onEnterPlay` si esegue subito) e ha contatori nuovi. Quando l'avversaria esce, la copia si toglie; i risultati permanenti restano (DEF già guadagnata). Il +3 DEF è permanente, origine `EFFECT`, una volta per copia. Il sigillo non tocca il bonus di fazione (non è un modificatore, D-35).
- **Casi limite:** non copia Il Custode delle Porte Incrociate (immune) né un'altra Kitsune (niente copia, niente DEF, nessun ciclo); contro una creatura senza effetto non copia nulla e non prende DEF. Gli effetti copiati che parlano di "sé" si riferiscono a Kitsune (es. copiando Smiler cresce Kitsune).
- **Interazioni:** se Kitsune viene annullata (Il Predicatore sotto la Pioggia, SCP-2521) la copia si spegne con lei; Baku divora le attivazioni della copia come quelle di ogni effetto avversario.
- **Valore:** circa 9,5 PS, nel budget Legendary; dipende un po' dall'effetto dell'avversaria.
- **Motivo:** la kitsune è una mutaforma; esce dalla famiglia "togli i bonus" tenendone un po' (il sigillo) e risolve la domanda del catalogo (il bonus di fazione non si sigilla).

### The Rootbound Doppelganger — Theory of the Invitation + Theory of the Dream, Mutant — **modificata** (11 novembre)

- **Effetto originale:** previene 3 danni dal primo attacco avversario e −2 ATK all'avversaria per i due round seguenti, una volta per entrata (circa 7 PS, sotto il budget Mutant).
- **Effetto deciso — *Imboscata a ogni vittima*:** **contro ogni creatura avversaria** previene **3 danni dal suo primo attacco** e le dà **−2 ATK per i suoi due round seguenti**.
- **Motore:** `modifyIncomingDamage` (passo 3) e `onRoundEnd`; stato "ultima avversaria" (come Hidebehind): quando entra una nuova avversaria, scudo e malus ripartono. Il −2 ATK è un modificatore sull'avversaria con durata di due round (D-35).
- **Interazioni:** Il Custode delle Porte Incrociate è immune al −2 ATK (lo scudo vale lo stesso, perché agisce su di sé); Baku divora le attivazioni; Il Predicatore sotto la Pioggia e SCP-2521 la annullano.
- **Confronto con Jorōgumo** (Rare, previene 3 dai primi due attacchi): nessuna sovrapposizione da correggere, la Mutant resta sopra come deve.
- **Valore:** circa 9 PS, nel budget Mutant (8–10).
- **Motivo:** si legge come la versione grande dei due genitori (Hidebehind si nasconde dietro ogni vittima, Noppera-bō toglie ATK). Lo stesso schema vale per The Stitched Corridor.

### The Stitched Corridor — Theory of the Wound + Theory of the Archive, Mutant — **modificata** (11 novembre)

- **Effetto originale:** prima del primo round 2 danni; il primo attacco ricevuto fa 2 danni in meno, una volta per entrata (circa 4 PS; più debole di uno dei suoi genitori, Sala d'Attesa Vivente, che protegge contro ogni avversaria).
- **Effetto deciso — *Battito per ogni vittima*:** **contro ogni creatura avversaria**: **2 danni** quando entra (o quando entra il Corridor) e il suo **primo attacco fa 3 danni in meno**.
- **Motore:** `onEnterPlay` (passo 0) e controllo all'entrata delle creature avversarie mentre è in campo (stesso pezzo di Mothman) per i 2 danni, danno da effetto (D-08); `modifyIncomingDamage` (passo 3) per lo scudo, stato "ultima avversaria" (come Hidebehind e The Rootbound Doppelganger).
- **Interazioni:** Hollow Clearing riduce i 2 danni di 1 contro una creatura della Theory of the Invitation; Il Custode delle Porte Incrociate è immune ai danni (lo scudo vale lo stesso); Baku divora le attivazioni; Il Predicatore sotto la Pioggia e SCP-2521 la annullano. Somiglia in parte a Mothman (Mythic, 3 danni a ogni nemica che entra), ma con numeri più bassi e unita allo scudo.
- **Valore:** circa 8,5 PS, nel budget Mutant (8–10).
- **Motivo:** stesso schema di The Rootbound Doppelganger: la versione grande dei due genitori, ognuno applicato a ogni avversaria. Le due Mutant si leggono come fusioni.

### Morso del Rovescio — Spell `DAMAGE` — **confermata** (11 novembre)

- **Effetto:** la creatura avversaria **−3 ATK**.
- **Motore:** si pesca solo dopo la morte della propria creatura, quindi colpisce l'avversaria sopravvissuta; senza bersaglio (morte doppia) colpisce la prossima che entra (D-11). Dura finché quella creatura resta in campo (D-12). Modificatore con origine Spell: non è un bonus, quindi la famiglia "togli i bonus" non lo tocca.
- **Valore:** circa 5,5 PS (circa due round ancora contro una sopravvissuta, tutta la permanenza contro una nuova avversaria; si sconta l'ATK sotto 0), poco sopra il budget delle Spell.
- **Motivo:** vicina al budget e la più semplice del mazzo; le alternative (durata fissa, inversione dei bonus) aggiungevano un'eccezione a D-12 o un'altra carta alla famiglia "togli i bonus".

### Frattura della Pelle — Spell `DAMAGE` — **modificata** (11 novembre)

- **Effetto originale:** la creatura avversaria −3 DEF (3 PS, sotto il budget delle Spell).
- **Effetto deciso — *Frattura profonda*:** la creatura avversaria **−5 DEF**.
- **Motore:** colpisce subito l'avversaria sopravvissuta, o la prossima che entra (D-11); danno da effetto, può uccidere subito (D-08).
- **Interazioni:** Watching Rift riduce di 1 la prima Spell Danno ricevuta da una creatura della Theory of the Archive; Hollow Clearing riduce di 1 ogni penalità DEF di una creatura della Theory of the Invitation. Simile alla Hidden Breaking Point (−5 DEF se l'avversaria ha almeno 5 DEF), ma è un altro tipo di carta e senza condizione.
- **Valore:** 5 PS, nel budget.
- **Motivo:** semplice e nel budget, senza nuove meccaniche; le Spell restano lo strato semplice del gioco.

### Dissoluzione — Spell `DAMAGE` — **confermata** (11 novembre)

- **Effetto:** la creatura avversaria **−2 ATK e −2 DEF**.
- **Motore:** colpisce subito l'avversaria sopravvissuta, o la prossima che entra (D-11); il −2 DEF è danno da effetto (D-08), il −2 ATK dura finché quella creatura resta in campo (D-12).
- **Valore:** circa 5,5 PS (circa 3,5 l'ATK, 2 la DEF), allo stesso livello di Morso del Rovescio.
- **Motivo:** semplice e allo stesso livello di Morso del Rovescio; tra le tre Spell `DAMAGE` nessuna domina le altre (Morso tutta ATK, Frattura tutta DEF, Dissoluzione un misto).

### Velo degli Ancorati — Spell `SHIELD` — **modificata** (11 novembre)

- **Effetto originale:** +3 DEF (3 PS, sotto il budget; **identica** a SCP-458, e SCP-109 con +2 DEF è peggiore di entrambe).
- **Effetto deciso — *Velo spesso*:** **+5 DEF**.
- **Motore:** modificatore in attesa sulla prossima creatura del proprietario (D-10, D-34), si somma alle altre `SHIELD` in attesa, dura finché quella creatura resta in campo (D-12). DEF permanente.
- **Valore:** 5 PS, nel budget.
- **Motivo:** speculare a Frattura della Pelle (−5 DEF), semplice. Velo resta lo scudo DEF "puro"; SCP-458 e SCP-109 prendono effetti diversi, legati alla loro storia SCP (cibo e acqua infiniti), quando si arriva a loro.

### Patto del Registro — Spell `SHIELD` — **confermata** (11 novembre)

- **Effetto:** **+2 DEF e +1 ATK**.
- **Motore:** modificatore in attesa sulla prossima creatura del proprietario (D-10, D-34), dura finché resta in campo (D-12). Il +1 ATK è un bonus (la famiglia "togli i bonus" può toglierlo); la DEF è permanente. Echo Rift dà +1 ATK in più alle creature della Theory of the Wound con una Spell che aumenta l'ATK.
- **Valore:** 4,5 PS, a metà del budget.
- **Motivo:** nel budget e semplice; sta tra Velo degli Ancorati (tutta DEF) e Armatura della Pioggia (soprattutto ATK), senza dominanze.

### Armatura della Pioggia — Spell `SHIELD` — **modificata** (11 novembre)

- **Effetto originale:** +1 DEF e +2 ATK (6 PS, un punto sopra il budget; la Spell più forte fuori dalle Burn).
- **Effetto deciso — *Armatura da guerra*:** **+2 ATK**.
- **Motore:** modificatore in attesa sulla prossima creatura del proprietario (D-34), dura finché resta in campo (D-12). È un bonus ATK (la famiglia "togli i bonus" può toglierlo); Echo Rift dà +1 ATK in più alle creature della Theory of the Wound.
- **Valore:** 5 PS, nel budget.
- **Motivo:** le tre `SHIELD` semplici diventano un trio pulito (Velo tutta DEF, Armatura tutta ATK, Patto il misto), speculare alle tre `DAMAGE` (Frattura, Morso, Dissoluzione).

### SCP-500 · Pillola di Emergenza — Spell `SHIELD` — **confermata** (11 novembre)

- **Effetto:** la prossima creatura del proprietario **subisce 4 danni in meno dal primo danno**.
- **Motore:** modificatore in attesa (D-34); `modifyIncomingDamage`, una volta per entrata, sul primo danno maggiore di 0 (D-33). Le riduzioni si sottraggono prima di un eventuale dimezzamento (regola di SCP-3000).
- **Lettura decisa:** vale per il **primo danno di qualunque tipo** (attacco o effetto), come dice il testo della carta ("primo danno subito"); la riga del catalogo degli effetti, che dice "primo attacco", va corretta. Così ferma anche un danno all'entrata (Mothman, SCP-001, The Stitched Corridor).
- **Valore:** circa 4 PS (fino a 4 punti, meno se il primo colpo è sotto 4), in fondo al budget.
- **Motivo:** nel budget e semplice; è l'unica Spell che fa da scudo per un colpo, quindi non si sovrappone a Velo degli Ancorati (DEF permanente).

### SCP-109 · Acqua Infinita — Spell `SHIELD` — **modificata** (11 novembre)

- **Effetto originale:** +2 DEF alla prossima creatura (2 PS; strettamente peggiore di Velo degli Ancorati e SCP-458).
- **Effetto deciso — *Borraccia*:** **+2 DEF**; inoltre, **una volta**, quando la creatura scende a metà DEF o meno, **recupera 3 DEF** (fino alla DEF all'entrata).
- **Motore:** modificatore in attesa (D-34) con un effetto collegato alla creatura finché resta in campo (D-12). Soglia M-06: metà = 50% o meno della DEF all'entrata, bonus compresi (anche il +2 della Spell), controllata dopo ogni fase in cui cambia la DEF, solo se la creatura è ancora viva. `heal()` con tetto alla DEF all'entrata (come The Bloodless Hound). Una volta per entrata in campo (D-33), stato `used`.
- **Interazioni:** non è un effetto di creatura, quindi Baku non lo divora e gli annullatori di creatura non lo spengono; SCP-3999 toglie il +2 DEF come ogni bonus (regola dello scudo), ma non la cura già avvenuta.
- **Valore:** circa 4,4 PS, nel budget.
- **Motivo:** la borraccia che non si svuota: tiene il +2 DEF e aggiunge un sorso d'emergenza. Non è più un Velo più debole.

### SCP-458 · Pizza Senza Fine — Spell `SHIELD` — **modificata** (11 novembre)

- **Effetto originale:** +3 DEF alla prossima creatura (3 PS; uguale al vecchio Velo, ora strettamente peggiore di Velo con +5).
- **Effetto deciso — *Fette da dividere*:** **+2 DEF alla prossima creatura e +2 DEF a quella dopo**.
- **Motore:** due modificatori in attesa (D-34): il primo va sulla prossima creatura del proprietario, il secondo resta in attesa e va sulla creatura successiva (come Il Pastore Muto). Ognuno dura finché la sua creatura resta in campo (D-12). Se in attesa ci sono altre `SHIELD`, si sommano solo sulla prima creatura; la seconda fetta aspetta comunque la creatura dopo.
- **Valore:** 4 PS, nel budget.
- **Motivo:** cibo infinito, abbastanza per due; l'unica Spell che arriva a due creature, senza sovrapporsi alla cura di SCP-109 né al Velo.

### D-13 — Regola delle Burn — **decisa** (11 novembre)

- **Problema:** il catalogo somma ATK e DEF della creatura rivelata alla "creatura attiva", ma una Spell si pesca solo dopo la morte della propria creatura; e la carta rivelata è proprio la prossima creatura del proprietario, che quindi entrerebbe con ATK e DEF doppie (circa 13–20 PS). Scartare una carta non creatura costava una carta al proprietario.
- **Regola decisa — *Nessuna carta persa*:** la Burn rivela la prossima carta del mazzo del proprietario. Se è una creatura, **entra con un bonus fisso**, diverso per ogni Burn. Se non è una creatura, **non si scarta nulla** e la carta resta al suo posto. Con il mazzo vuoto non succede nulla.
- **Motore:** il bonus è un modificatore in attesa sulla prossima creatura (D-34), che è la carta rivelata; dura finché resta in campo (D-12). Probabilità che la carta rivelata sia una creatura: circa 2/3 (20 creature su 30 carte, varia durante la partita). Nuovo evento di log (proposta: `BURN_REVEALED`, con la carta rivelata).
- **Motivo:** semplice, deterministica, nessuna carta persa e valore calcolabile; la variante "brucia la carta" dipendeva dal costo di perdere una creatura, valutabile solo con la simulazione.

### Richiamo della Crepa — Spell `BURN` — **modificata** (11 novembre)

- **Effetto originale:** Burn base (somma ATK e DEF della creatura rivelata; con la regola di D-13 serviva un bonus fisso).
- **Effetto deciso:** se la carta rivelata è una creatura, entra con **+2 ATK e +2 DEF**; altrimenti non succede nulla. (Prima decisa a +2 ATK +1 DEF, alzata lo stesso giorno per togliere la dominanza di Fusione del Rovescio.)
- **Motore:** M-15 (D-13); modificatore in attesa sulla creatura rivelata (D-34), dura finché resta in campo (D-12). Il +2 ATK è un bonus (la famiglia "togli i bonus" può toglierlo); Echo Rift dà +1 ATK in più alle creature della Theory of the Wound.
- **Valore:** circa 4,7 PS (2/3 × 7), nel budget.
- **Motivo:** la Burn "tutto o niente": dà più delle altre due quando esce una creatura, mentre Fusione del Rovescio ed Eco dei Dimenticati danno qualcosa anche quando non esce. Nessuna delle tre domina le altre.

### Fusione del Rovescio — Spell `BURN` — **modificata** (11 novembre)

- **Effetto originale:** Burn base e +1 ATK se la carta rivelata è una creatura (con la base decisa sarebbe +3 ATK +1 DEF, circa 5,7 PS, sopra il budget).
- **Effetto deciso — *Consolazione*:** se la carta rivelata è una creatura, entra con **+2 ATK e +1 DEF**; altrimenti **la prossima creatura ottiene comunque +1 ATK**.
- **Motore:** M-15 (D-13); modificatore in attesa sulla prossima creatura (D-34), dura finché resta in campo (D-12). La carta non creatura resta al suo posto.
- **Valore:** circa 4,8 PS (4 + 1/3 × 2,5), nel budget.
- **Motivo:** stessa famiglia di Richiamo della Crepa, più sicura; il "+1 ATK" del catalogo passa al caso in cui la carta non è una creatura.
- **Dominanza risolta:** con Richiamo a +2 ATK +1 DEF Fusione era strettamente migliore; Richiamo è stata alzata a +2 ATK +2 DEF (11 novembre), quindi è migliore quando esce una creatura e Fusione quando non esce.

### Eco dei Dimenticati — Spell `BURN` — **modificata** (11 novembre)

- **Effetto originale:** Burn base e +1 DEF se la carta rivelata è una creatura.
- **Effetto deciso — *Consolazione*:** se la carta rivelata è una creatura, entra con **+2 ATK e +1 DEF**; altrimenti **la prossima creatura ottiene comunque +2 DEF**.
- **Motore:** M-15 (D-13); modificatore in attesa sulla prossima creatura (D-34), dura finché resta in campo (D-12). La carta non creatura resta al suo posto.
- **Valore:** circa 4,7 PS (4 + 1/3 × 2), nel budget.
- **Motivo:** la gemella DEF di Fusione del Rovescio; chiude la famiglia delle Burn senza dominanze (Richiamo +2/+2 o niente; Fusione +2/+1 o +1 ATK; Eco +2/+1 o +2 DEF).

### Last Sanctuary — Hidden `SURVIVAL` — **modificata** (11 novembre)

- **Effetto originale:** la propria creatura +4 DEF nello scontro corrente (circa 3,8 PS, appena sotto il budget).
- **Effetto deciso:** **+5 DEF nello scontro corrente**.
- **Motore:** senza condizione: si attiva all'inizio del prossimo scontro del proprietario (D-15), cioè quando entra la sua prossima creatura; pescata nella pescata iniziale non si attiva nel primo scontro. DEF temporanea che fa da scudo fino alla fine dello scontro (D-35), cioè finché una delle due creature muore. Insieme ad altre Hidden: prima le `DAMAGE`, poi le `SURVIVAL`.
- **Valore:** circa 4,7 PS (una parte dello scudo va persa se lo scontro finisce prima), nel budget.
- **Motivo:** il cambiamento più piccolo che la porta nel budget; le Hidden, come le Spell, restano semplici.

### Survival Instinct — Hidden `SURVIVAL` — **modificata** (11 novembre)

- **Effetto originale:** +2 ATK e +2 DEF nello scontro corrente (circa 5,9 PS, un punto sopra il budget).
- **Effetto deciso:** **+2 ATK e +1 DEF nello scontro corrente**.
- **Motore:** senza condizione: si attiva all'inizio del prossimo scontro del proprietario (D-15). La DEF fa da scudo fino alla fine dello scontro (D-35); il +2 ATK dura fino alla fine dello scontro ed è un bonus (la famiglia "togli i bonus" può toglierlo).
- **Valore:** circa 5 PS (circa 4 l'ATK, 1 lo scudo), in cima al budget.
- **Motivo:** il taglio più piccolo; fa coppia con Last Sanctuary (tutta DEF) dal lato dell'ATK.

### Remnants of Reality — Hidden `SURVIVAL` — **modificata** (11 novembre)

- **Effetto originale:** annulla fino a 3 punti di penalità ATK/DEF senza superare i valori originali (circa 1 PS: senza condizione si attivava all'inizio dello scontro, quando la creatura nuova non ha quasi mai penalità).
- **Effetto deciso — *Fino a 5*:** **diventa una Hidden con condizione**: resta coperta finché la propria creatura riceve una penalità, poi **annulla fino a 5 punti** di penalità, senza superare i valori di prima.
- **Penalità:** qualunque riduzione di ATK da una carta avversaria e la DEF persa per un effetto, una Spell o una Hidden avversaria; il danno di un attacco **non** è una penalità.
- **Motore:** controllo della condizione dopo ogni applicazione di un modificatore negativo o di un danno da effetto sulla propria creatura (M-11, D-15). Annulla prima la penalità che l'ha attivata, poi quelle già presenti sulla creatura, nell'ordine in cui sono state applicate, fino a 5 punti; i punti avanzati si perdono. Sospensione per origine (D-35, M-04); la DEF tolta da un danno da effetto si restituisce con `heal()`, con tetto alla DEF di prima. Si attiva una volta, poi va negli scarti.
- **Valore:** circa 4,5 PS (si attiva quasi sempre prima o poi), nel budget.
- **Motivo:** con la condizione la carta funziona; il numero più alto la porta nel budget.

### Unseen Exit — Hidden `SURVIVAL` — **confermata** (11 novembre)

- **Effetto:** quando un **attacco** avversario ucciderebbe la propria creatura, **previene 3 danni**.
- **Motore:** Hidden con condizione (D-15): resta coperta finché la condizione si verifica. Controllo in `modifyIncomingDamage` (passo 3), dopo tutte le altre riduzioni; "ucciderebbe" = DEF ≤ 0 dopo il danno (D-16). Poi va negli scarti.
- **Domande risolte:** vale **solo contro gli attacchi**, come dice il testo (i danni letali di ogni tipo sono già di The Undying Blossom); se morirebbero entrambe e i 3 punti la salvano, sopravvive e l'altra muore, quindi l'avversario subisce danni.
- **Valore:** circa 4,5 PS (meno danni al giocatore dalla DEF negativa, circa 2,5; la creatura salvata se l'attacco era letale di 3 o meno, circa 2), nel budget.
- **Motivo:** nel budget e chiara; le risposte sopra chiudono le domande del catalogo.

### Hostile Presence — Hidden `DAMAGE` — **modificata** (11 novembre)

- **Effetto originale:** avversaria −4 ATK "prima del calcolo dello scontro" (durata non scritta: circa 6,5 PS per tutto lo scontro, circa 3,7 PS per un round).
- **Effetto deciso — *−5 al primo round*:** la creatura avversaria **−5 ATK nel primo round** dello scontro.
- **Durata decisa:** "prima del calcolo dello scontro" = **solo il primo round** dello scontro in cui la Hidden si attiva (vale anche per Echo Strike).
- **Motore:** senza condizione: si attiva all'inizio del prossimo scontro del proprietario (D-15) e colpisce la creatura avversaria in campo in quel momento; prima le `DAMAGE`, poi le `SURVIVAL`. Modificatore di un round sull'avversaria (D-35); si sconta l'ATK sotto 0.
- **Interazioni:** Il Custode delle Porte Incrociate non è immune (la sua immunità vale solo contro gli effetti di creatura). Somiglia a *Congelata* di Yuki-onna (attacco 0 nel primo round), ma su una Hidden.
- **Valore:** circa 4,4 PS, nel budget.
- **Motivo:** una Hidden `DAMAGE` che colpisce una volta prima dello scontro, come dice il testo; le penalità ATK costanti restano alle creature.

### Echo Strike — Hidden `DAMAGE` — **confermata** (11 novembre)

- **Effetto:** la creatura avversaria **−2 ATK e −2 DEF** prima del calcolo dello scontro.
- **Motore:** senza condizione: si attiva all'inizio del prossimo scontro del proprietario (D-15) contro la creatura avversaria in campo. Il −2 ATK dura **solo il primo round** (regola decisa con Hostile Presence); il −2 DEF è danno da effetto, permanente (D-08).
- **Valore:** circa 4 PS (circa 2 l'ATK, 2 la DEF), in fondo al budget.
- **Motivo:** nel budget e semplice; è la Hidden `DAMAGE` mista, tra Hostile Presence (tutta ATK) e Breaking Point (tutta DEF).

### Breaking Point — Hidden `DAMAGE` — **modificata** (11 novembre)

- **Effetto originale:** −5 DEF se la DEF avversaria è almeno 5, altrimenti −2 DEF fino al minimo di 0 (circa 3,8 PS, appena sotto il budget).
- **Effetto deciso — *Spezzata*:** se la creatura avversaria ha **5 DEF o meno, si spezza: la sua DEF scende a 0** e muore, senza danni al giocatore; altrimenti **−3 DEF**.
- **Motore:** senza condizione: si attiva all'inizio del prossimo scontro del proprietario (D-15) contro la creatura avversaria in campo. Danno da effetto (D-08); DEF 0 uccide (D-16). La DEF portata a 0 non diventa negativa, quindi l'avversario non subisce danni da quella morte. La soglia guarda la DEF attuale (dopo i danni già subiti e gli scudi).
- **Interazioni:** The Undying Blossom sopravvive con 4 DEF (danno letale); Unseen Exit non scatta (non è un attacco); Watching Rift riduce di 1 la prima Spell Danno, non le Hidden.
- **Valore:** circa 4,8 PS, nel budget.
- **Motivo:** un vero punto di rottura che finisce le creature indebolite; deterministica e diversa dalle Spell.

### Static Interference — Hidden `DAMAGE` — **modificata** (11 novembre)

- **Effetto originale:** annulla fino a 3 punti di un bonus ATK nemico; se non c'è nessun bonus, 2 danni DEF (condizione e durata non scritte: se aspettasse un bonus, il ramo "nessun bonus" non si verificherebbe mai).
- **Effetto deciso — *Tutto lo scontro*:** **Hidden senza condizione**: all'inizio del prossimo scontro del proprietario **sospende fino a 3 punti di bonus ATK** della creatura avversaria **per tutto lo scontro**; se non ne ha, le infligge **3 danni**.
- **Motore:** si attiva all'inizio del prossimo scontro del proprietario (D-15); prima le `DAMAGE`, poi le `SURVIVAL`. "Bonus ATK" = qualunque modificatore ATK positivo, di qualunque origine (non il bonus di fazione, che non è un modificatore); sospensione per origine (D-35, M-04), nell'ordine in cui i bonus sono stati applicati, fino alla fine dello scontro. I 3 danni sono danno da effetto (D-08).
- **Valore:** circa 4 PS (bonus presente circa una volta su due), nel budget.
- **Motivo:** la più vicina al testo della carta; risolve la domanda del catalogo (non aspetta una condizione, così entrambi i rami hanno senso). È la versione Hidden della famiglia "togli i bonus".

### Echo Rift — Terreno, Theory of the Wound — **modificato** (11 novembre)

- **Effetto originale:** creature Backrooms +1 ATK; con una Spell che aumenta l'ATK ricevono +1 ATK in più, una volta per scontro (circa 2,8 PS per creatura; durata del +1 non scritta, e dipende da poche Spell).
- **Effetto deciso — *Eco delle ferite*:** creature Backrooms **+1 ATK**; la prima volta che ognuna viene **ferita** in uno scontro, ottiene **+1 ATK per il resto di quello scontro**.
- **Motore:** bonus passivo a tutte le creature Backrooms in campo, anche dell'avversario (D-17), finché il Terreno resta (D-19); effetto secondario rivisto ora, implementato dopo (D-18). "Ferita" = perdita reale di DEF sopra 0 dopo gli scudi; una volta per creatura per scontro. Entrambi i +1 sono bonus ATK (la famiglia "togli i bonus" può toglierli).
- **Interazioni:** con Wretches (*Furia del ferito*, +2 ATK alla prima ferita) arriva a +3: sinergia voluta dentro la fazione. Se The Hollow Wyrm inghiotte il Terreno, i bonus si tolgono.
- **Valore:** circa 3,5 PS per creatura Backrooms, sull'obiettivo.
- **Motivo:** non dipende più dalle Spell, arriva all'obiettivo e segue la Theory of the Wound.

### Hollow Clearing — Terreno, Theory of the Invitation — **modificato** (11 novembre)

- **Effetto originale:** creature degli Appalachi +2 DEF; la prima penalità DEF di ogni scontro ridotta di 1 (circa 2,6 PS per creatura).
- **Effetto deciso — *Ogni penalità*:** creature degli Appalachi **+2 DEF**; **ogni** penalità DEF che ricevono è **ridotta di 1**.
- **Motore:** bonus passivo a tutte le creature degli Appalachi in campo, anche dell'avversario (D-17); il +2 DEF si toglie con la regola dello scudo se il Terreno esce (D-19). "Penalità DEF" = DEF persa per un effetto, una Spell o una Hidden, non per un attacco (stessa definizione di Remnants of Reality). Secondario rivisto ora, implementato dopo (D-18).
- **Coerenza:** le schede già scritte dicevano "Hollow Clearing riduce di 1 contro una creatura della Theory of the Invitation" senza il limite "prima per scontro"; con questa decisione sono tutte corrette.
- **Valore:** circa 3,5 PS per creatura degli Appalachi, sull'obiettivo.
- **Motivo:** il cambiamento più piccolo che arriva all'obiettivo e tiene giuste le schede precedenti.

### Watching Rift — Terreno, Theory of the Archive — **confermato** (11 novembre)

- **Effetto:** creature SCP **+1 ATK e +1 DEF**; la **prima Spell Danno** ricevuta da ognuna in uno scontro è **ridotta di 1 punto complessivo**.
- **Motore:** bonus passivo a tutte le creature SCP in campo, anche dell'avversario (D-17), finché il Terreno resta (D-19). Secondario rivisto ora, implementato dopo (D-18): una volta per creatura per scontro. **"1 punto complessivo"** su una Spell che colpisce ATK e DEF (Dissoluzione): si toglie prima dalla parte DEF, poi dall'ATK.
- **Valore:** circa 3,7 PS per creatura SCP (3,5 le statistiche, circa 0,2 il secondario), sull'obiettivo.
- **Motivo:** già sull'obiettivo; la regola "prima la DEF" risolve l'unica lacuna.

### Devouring Abyss — Terreno, Theory of the Dream — **confermato** (11 novembre)

- **Effetto:** creature Yokai **+1 ATK**; al **primo confronto** infliggono **1 danno DEF**.
- **Motore:** bonus passivo a tutte le creature Yokai in campo, anche dell'avversario (D-17), finché il Terreno resta (D-19). Secondario rivisto ora, implementato dopo (D-18): "primo confronto" = **all'inizio di ogni scontro** (passo 0), una volta per creatura per scontro, contro la creatura avversaria. Danno da effetto (D-08).
- **Interazioni:** Hollow Clearing lo porta a 0 contro una creatura degli Appalachi; Il Custode delle Porte Incrociate non è immune (un Terreno non è un effetto di creatura).
- **Valore:** circa 3,5 PS per creatura Yokai (2,5 l'ATK, circa 1 il danno), sull'obiettivo.
- **Motivo:** già sull'obiettivo; le due regole sopra fissano momento e tipo del danno.

### Badge di Uscita Negata — Oggetto, Theory of the Wound — **modificato** (11 novembre)

- **Effetto originale:** previene 2 danni dal primo attacco, poi si consuma (2 PS; con un numero più alto diventava una copia di SCP-500).
- **Effetto deciso — *Nessuna uscita*:** la prima volta che la creatura del proprietario **morirebbe, per qualunque danno, resta con 1 DEF**; poi il badge si consuma.
- **Motore:** monouso (D-47): resta accanto al mazzo e scatta sulla prima creatura del proprietario che subisce un danno letale (DEF ≤ 0, D-16), prima del controllo delle morti (D-36, stesso pezzo di The Undying Blossom). Vale contro attacchi, effetti, Spell, Hidden e Terreni. Se morirebbero entrambe, la creatura del proprietario resta con 1 DEF e l'altra muore. Ordine rispetto al Terreno: D-48.
- **Ordine con altre protezioni:** prima le prevenzioni del passo 3 (Unseen Exit, scudi); poi, se il danno è ancora letale, l'effetto della creatura stessa (The Undying Blossom resta con 4 DEF e il badge non si consuma); solo dopo il badge.
- **Valore:** circa 4,8 PS (il danno oltre la morte evitato e la creatura che attacca ancora), nel budget.
- **Motivo:** l'uscita è negata, quindi la creatura non può andarsene; un compito diverso da SCP-500 (Spell) e da Unseen Exit (Hidden, 3 punti contro un attacco, non sempre salva).

### Bussola dei Sentieri Falsi — Oggetto, Theory of the Invitation — **modificato** (11 novembre)

- **Effetto originale:** +2 DEF per il primo round, poi si consuma (circa 1,8 PS).
- **Effetto deciso — *Strada lunga*:** uno **scudo di 2 DEF all'inizio di ognuno dei primi tre round** dello scontro (si rinnova ogni round), poi si consuma.
- **Motore:** monouso (D-47): resta accanto al mazzo e scatta all'inizio del prossimo scontro della creatura del proprietario. DEF temporanea che fa da scudo (D-35): all'inizio di ognuno dei primi tre round lo scudo torna a 2 (non si somma a quello rimasto). Se lo scontro finisce prima, i round restanti si perdono e l'Oggetto è comunque consumato. Ordine rispetto al Terreno: D-48.
- **Valore:** circa 5 PS (circa 1,9 + 1,7 + 1,4), in cima al budget.
- **Motivo:** resta l'Oggetto "scudo", allungato sullo scontro; non si sovrappone a Last Sanctuary (uno scudo da 5) né a SCP-500 (un colpo). I quattro Oggetti restano distinti: il Badge salva dalla morte, la Bussola fa da scudo, il Fascicolo e il Campanello hanno compiti propri.

### Fascicolo Sigillato 0 — Oggetto, Theory of the Archive — **modificato** (11 novembre)

- **Effetto originale:** annulla fino a 2 punti della prima penalità ATK/DEF, poi si consuma (circa 1,8 PS; una copia più piccola della Hidden Remnants of Reality).
- **Effetto deciso — *Censurato*:** **il primo effetto avversario, Spell o Hidden che colpirebbe la creatura del proprietario viene annullato del tutto**, poi il Fascicolo si consuma.
- **"Colpirebbe":** qualunque cosa che un effetto di creatura, una Spell o una Hidden avversaria applicherebbe alla creatura del proprietario: danno da effetto, penalità ATK o DEF, annullamento o furto dell'effetto (Il Predicatore sotto la Pioggia, SCP-2521, Baku), rimozione o sospensione dei bonus (Deathmoths, SCP-3999, Static Interference). **Non** contano gli attacchi, i Terreni (passivi per tutti) né ciò che non cambia la creatura (la copia di Kitsune).
- **Motore:** monouso (D-47): resta accanto al mazzo e protegge la creatura del proprietario in campo (o la prossima, se non ce n'è). Filtro sul bersaglio come quello di Il Custode delle Porte Incrociate, ma per una sola applicazione e anche contro Spell e Hidden. Se un effetto colpisce in più parti nello stesso momento (es. Dissoluzione −2 ATK e −2 DEF), si annulla tutta l'applicazione. Ordine rispetto al Terreno: D-48.
- **Valore:** circa 4 PS (il valore medio della prima applicazione avversaria annullata), nel budget.
- **Motivo:** il fascicolo è sigillato e non entra niente; un compito proprio, diverso da Remnants of Reality (che restituisce solo punti di penalità).

### Campanello del Torii — Oggetto, Theory of the Dream — **modificato** (11 novembre)

- **Effetto originale:** alla prima Hidden nemica attivata concede +2 DEF, poi si consuma (circa 1,8 PS; tipo di DEF non scritto).
- **Effetto deciso — *Campana d'allarme*:** quando la prima Hidden nemica **starebbe per attivarsi, viene annullata** (scartata senza effetto), poi il Campanello si consuma.
- **Motore:** monouso (D-47): resta accanto al mazzo finché l'avversario attiva una Hidden (D-15), sia `SURVIVAL` sia `DAMAGE`. Si controlla nel momento in cui la Hidden si attiverebbe, prima del suo effetto. "Nemica" = una Hidden che lavora per l'avversario (anche una presa con *Linee incrociate* di L'Operatore della Stanza 0). Ordine rispetto al Terreno: D-48.
- **Valore:** circa 4 PS (il valore medio di una Hidden avversaria, circa 4,5, per una probabilità di circa il 90%), nel budget.
- **Motivo:** la campana suona prima dell'anomalia e la ferma; l'unica carta del catalogo che contrasta le Hidden (La Regina dei Sentieri Perduti non lo fa più).

## Modifiche ai documenti da fare a fine revisione

**Applicate l'11 novembre** in catalogo degli effetti, effetti, carte, log della partita, decisioni aperte, test, CodingRequirements, roadmap e nelle decisioni dell'API. L'elenco resta come traccia.

- [catalogo degli effetti](../effetti/catalogo-effetti.md): riga di Wampus Cat → effetto *Sguardo della follia*, momento `onEnterPlay`, nessuno stato, lotto P1; togliere Wampus Cat dalla classe base "+ATK nel primo round".
- [catalogo degli effetti](../effetti/catalogo-effetti.md): riga di Hidebehind → "primo attacco di ogni creatura avversaria", stato "ultima avversaria"; resta nella classe base "primo attacco ridotto" con la variante per avversaria.
- [effetti](../effetti/effetti.md) (esempio Hidebehind), [test](../test/test.md) (test di Hidebehind: aggiungere il caso "nuova avversaria → scudo di nuovo") e [CodingRequirements](../../CodingRequirements.md) (Javadoc di esempio): testo dell'effetto aggiornato.
- [catalogo degli effetti](../effetti/catalogo-effetti.md): riga di Il Pastore Muto → *Custode del gregge*, `onDeath`, modificatore in attesa (D-34); togliere la carta dalla classe base "danni alla morte" e da D-08; aggiungerla a D-34 in [decisioni aperte](../decisioni-aperte/decisioni-aperte.md).
- [effetti](../effetti/effetti.md) e [decisioni aperte](../decisioni-aperte/decisioni-aperte.md): scrivere la regola "danno da effetto = −DEF, non è un attacco".
- [catalogo degli effetti](../effetti/catalogo-effetti.md): righe di Cervo Bianco, Cartellino Sbiadito, Il Collezionista di Nomi e Rokurokubi → "−1 DEF all'avversaria e −1 DEF alla prossima che entra" (dopo la conferma delle altre tre); aggiungerle a D-34 in [decisioni aperte](../decisioni-aperte/decisioni-aperte.md).
- [catalogo degli effetti](../effetti/catalogo-effetti.md): riga di The Nameless Caller → *Voci dei dispersi*, `onEnterPlay`, contatore delle creature perse (M-07), domanda risolta.
- [catalogo degli effetti](../effetti/catalogo-effetti.md): righe di Il Raccoglitore di Orme e The Index Maw → "+2 DEF finché resta in campo", lotto P1; toglierle dalla riga "DEF temporanea come scudo" dei pezzi di motore.
- [catalogo degli effetti](../effetti/catalogo-effetti.md): righe di Howlers, SCP-049, Noppera-bō e The Masked Bloom → "−2 ATK all'avversaria nel primo round, −1 nel secondo" (dopo la conferma delle altre tre); aggiornare la classe base "avv. −ATK per N round".
- [catalogo degli effetti](../effetti/catalogo-effetti.md): righe di Sala d'Attesa Vivente e Nurikabe → come Hidebehind ("primo attacco di ogni creatura avversaria").
- [catalogo degli effetti](../effetti/catalogo-effetti.md): riga di L'Addetto alle Uscite → *Indicazioni cancellate*, lotto P2 (D-35), domanda risolta.
- [catalogo degli effetti](../effetti/catalogo-effetti.md): riga di SCP-096 → *Il volto visto*, `afterDamage` e `modifyOutgoingAttack`, stato "creatura marchiata"; in [effetti](../effetti/effetti.md) sostituire l'esempio "Effetto con soglia: SCP-096" (usare Hounds o The Unseen Matriarch come esempio di soglia a metà DEF).
- [catalogo degli effetti](../effetti/catalogo-effetti.md): riga di SCP-053 → *Paranoia*, lotto P1, domanda risolta.
- [catalogo degli effetti](../effetti/catalogo-effetti.md): riga di SCP-2006 → *Imitazione*, `onEnterPlay`, domanda della parità risolta.
- [catalogo degli effetti](../effetti/catalogo-effetti.md): riga di L'Ombra del Torii → "+3 DEF con il Terreno della Theory of the Dream, altrimenti +2, finché resta in campo"; domanda risolta (Terreno di chiunque, controllato all'entrata).
- [catalogo degli effetti](../effetti/catalogo-effetti.md): riga di Spearfinger → "+2 al primo attacco contro ogni nuova avversaria", bonus ATK, stato "ultima avversaria".
- [catalogo degli effetti](../effetti/catalogo-effetti.md): riga di Bell Witch → *Infestazione che resta*, `onRoundStart` del secondo round e `onDeath`; aggiungerla a D-34 in [decisioni aperte](../decisioni-aperte/decisioni-aperte.md).
- [catalogo degli effetti](../effetti/catalogo-effetti.md): riga di Il Predicatore sotto la Pioggia → *Sermone che cancella*; [effetti](../effetti/effetti.md): scrivere la regola degli effetti annullati e aggiornare l'elenco degli effetti `NULLIFY`.
- [catalogo degli effetti](../effetti/catalogo-effetti.md): riga di The Bloodless Hound → *Drenaggio senza sangue*, `afterDamage` e `onKill`, `heal()` con tetto alla DEF all'entrata (domanda risolta).
- [catalogo degli effetti](../effetti/catalogo-effetti.md): riga di Skin-Stealers → *Pelle rubata*, lotto P1; toglierla dalla riga "DEF temporanea come scudo"; in [CodingRequirements](../../CodingRequirements.md) aggiornare l'esempio di arrotondamento di Skin-Stealers (metà, massimo 4).
- [catalogo degli effetti](../effetti/catalogo-effetti.md): riga di Il Doppio di Servizio → *Doppio turno*, +3 ATK nei primi due round; toglierla dalla classe base "+ATK nel primo round".
- [catalogo degli effetti](../effetti/catalogo-effetti.md): riga di Il Direttore dell'Edificio → *Demolizione*, 40% della DEF base, massimo 4 (domanda risolta); in [CodingRequirements](../../CodingRequirements.md) l'esempio "20% di 13" va aggiornato.
- [catalogo degli effetti](../effetti/catalogo-effetti.md): riga di L'Operatore della Stanza 0 → *Linee incrociate*, lotto P2 (M-11, D-15); togliere la carta da D-08 (non fa più danni all'entrata).
- [catalogo degli effetti](../effetti/catalogo-effetti.md): riga di SCP-106 → riflette 2 danni per i primi due attacchi subiti.
- [catalogo degli effetti](../effetti/catalogo-effetti.md): riga di SCP-682 → *Adattamento*, stato "attacchi subiti".
- [catalogo degli effetti](../effetti/catalogo-effetti.md): riga di SCP-2521 → *Abilità rubata*, lotto P2; aggiungere ai pezzi di motore "effetto rubato: nuova istanza dal registro collegata a un'altra creatura"; in [effetti](../effetti/effetti.md) aggiornare l'elenco degli effetti `NULLIFY`.
- [catalogo degli effetti](../effetti/catalogo-effetti.md): riga di The Drowned Memory → *Memoria profonda* (+1 DEF per alleata eliminata fino a +4, +1 ATK con almeno 3), domanda risolta.
- [catalogo degli effetti](../effetti/catalogo-effetti.md): riga di Yuki-onna → *Congelata* (attacco avversario 0 nel primo round); toglierla dalla classe base "avv. −ATK per N round".
- [catalogo degli effetti](../effetti/catalogo-effetti.md): riga di Jorōgumo → *Doppia illusione*, 3 danni prevenuti dai primi due attacchi.
- [catalogo degli effetti](../effetti/catalogo-effetti.md): riga di Nure-onna → *Ogni vittima*, stato "ultima avversaria colpita", soglia controllata dopo ogni fase in cui cambia la DEF avversaria.
- [catalogo degli effetti](../effetti/catalogo-effetti.md): riga di The Face Collector → *Forza presa in prestito* (2 nel primo round, 1 nel secondo), domanda risolta.
- [catalogo degli effetti](../effetti/catalogo-effetti.md): riga di Mothman → *Disastro annunciato*, lotto P1; aggiungere ai pezzi di motore "controllo all'entrata delle creature avversarie mentre una creatura è in campo" (serve a Mothman); togliere Mothman da M-09 e chiudere la domanda in [effetti](../effetti/effetti.md) e D-36.
- [catalogo degli effetti](../effetti/catalogo-effetti.md): riga di Bigfoot → *Ammazzagiganti*, lotto P1 (domanda risolta); in [carte](../carte/carte.md) aggiornare l'esempio di Bigfoot nella sezione Rarità.
- [catalogo degli effetti](../effetti/catalogo-effetti.md): riga di The Thorned Matriarch → *Offerta di rovi*, `onRoundStart`; aggiornare D-08 (danni a inizio round).
- [catalogo degli effetti](../effetti/catalogo-effetti.md): riga di Hounds → *Odore del sangue*, `modifyOutgoingAttack`; togliere Hounds dalla classe base "soglia metà DEF avversaria".
- [catalogo degli effetti](../effetti/catalogo-effetti.md): riga di Deathmoths → *Divoratrici di luce*, lotto P2 (D-35), domanda risolta.
- [catalogo degli effetti](../effetti/catalogo-effetti.md): riga di Il Custode delle Porte Incrociate → *Inviolabile*, lotto P2; aggiungere ai pezzi di motore "immunità agli effetti delle creature avversarie (filtro sul bersaglio)"; togliere il Custode dagli effetti `NULLIFY` in [effetti](../effetti/effetti.md) e dalle "interazioni" delle carte con effetto di entrata nel catalogo.
- [catalogo degli effetti](../effetti/catalogo-effetti.md): riga di SCP-3000 → *Pelle abissale*; ordine "prima le sottrazioni, poi la metà" per le prevenzioni; in [carte](../carte/carte.md) aggiornare l'esempio di SCP-3000 nella sezione Rarità.
- [catalogo degli effetti](../effetti/catalogo-effetti.md): riga di The Null Witness → *Protocollo di cancellazione*, senza autoeliminazione; [decisioni aperte](../decisioni-aperte/decisioni-aperte.md): chiudere D-38.
- [catalogo degli effetti](../effetti/catalogo-effetti.md): riga di Baku → *Divoratore di incubi*, lotto P2 (M-10); aggiungere ai pezzi di motore "momento prima dell'attivazione di un effetto avversario"; in [effetti](../effetti/effetti.md) aggiornare l'elenco degli effetti `NULLIFY`.
- [catalogo degli effetti](../effetti/catalogo-effetti.md): riga di The Unseen Matriarch → *Velo spezzato*, soglia sulla propria DEF; togliere la carta dalla classe base "soglia metà DEF avversaria" (resta solo Nure-onna) e chiudere la parte di D-37 su Nure-onna in [decisioni aperte](../decisioni-aperte/decisioni-aperte.md).
- [catalogo degli effetti](../effetti/catalogo-effetti.md): riga di The Undying Blossom → *Maledizione cremisi*, domande risolte (vale contro ogni danno letale; morte doppia: lei resta); aggiungere The Undying Blossom alla riga "danni dopo i danni" di D-08.
- [catalogo degli effetti](../effetti/catalogo-effetti.md): riga di Wendigo → *Insaziabile*, `onRoundEnd` e `onKill`, stato "eliminazioni", `heal()` con tetto (domanda risolta); togliere Wendigo da D-34 in [decisioni aperte](../decisioni-aperte/decisioni-aperte.md).
- [catalogo degli effetti](../effetti/catalogo-effetti.md): riga di La Regina dei Sentieri Perduti → *Sentieri deviati*, `onEnterPlay` e `onKill`, lotto P2; aggiungere ai pezzi di motore "modifica dell'ordine del mazzo avversario"; [log della partita](../log-partita/log-partita.md): nuovo evento `DECK_REORDERED`; togliere la nota "non deterministico".
- [catalogo degli effetti](../effetti/catalogo-effetti.md): riga di Smiler → *Sorriso nel buio*, `onRoundEnd`, stati "round sopravvissuti" e "malus sull'avversaria", domanda risolta; il lotto P1 resta (aggiornare la riga di `ENG-23` nella [roadmap](../roadmap/roadmap.md) solo se cambia il lotto).
- [catalogo degli effetti](../effetti/catalogo-effetti.md): riga di The Hollow Wyrm → *Divoratore di Crepe*, `onEnterPlay` e arrivo di un Terreno, lotto P2; togliere la nota "non può attivarsi"; [carte](../carte/carte.md): sezione Terreni, un Terreno può essere inghiottito; [log della partita](../log-partita/log-partita.md): evento `TERRAIN_REMOVED` con la causa (serve anche a The Null Witness).
- [catalogo degli effetti](../effetti/catalogo-effetti.md): riga di SCP-001 → *Alba piena* (3 all'entrata, poi 3 a inizio round per 3 round), domanda risolta (i round rimanenti passano alla nuova avversaria).
- [catalogo degli effetti](../effetti/catalogo-effetti.md): riga di SCP-3999 → *Riscrittura della realtà*, `onEnterPlay` e `onRoundStart`, lotto P2 (D-35); domande risolte (niente più scambio di ATK e DEF); aggiornare D-35 in [decisioni aperte](../decisioni-aperte/decisioni-aperte.md).
- [catalogo degli effetti](../effetti/catalogo-effetti.md): riga di Shuten-dōji → domanda risolta (contano anche i danni degli effetti; il bonus resta contro le nuove avversarie).
- [catalogo degli effetti](../effetti/catalogo-effetti.md): riga di Kitsune → *Mille forme*, lotto P2 (stesso pezzo di SCP-2521, D-35); domanda risolta (il bonus di fazione non si sigilla).
- [catalogo degli effetti](../effetti/catalogo-effetti.md): riga di The Rootbound Doppelganger → *Imboscata a ogni vittima*, stato "ultima avversaria"; resta nella classe base "primo attacco ridotto" con la variante per avversaria; aggiornare l'esempio `12` in [examples](../../examples/README.md) se usa più di un'avversaria.
- [catalogo degli effetti](../effetti/catalogo-effetti.md): riga di The Stitched Corridor → *Battito per ogni vittima*, stato "ultima avversaria", controllo all'entrata delle avversarie (pezzo di Mothman); resta nelle classi base "primo attacco ridotto" e "danni all'entrata" con la variante per avversaria.
- [catalogo degli effetti](../effetti/catalogo-effetti.md): riga di Frattura della Pelle → avv. −5 DEF.
- [catalogo degli effetti](../effetti/catalogo-effetti.md): riga di Velo degli Ancorati → +5 DEF.
- [catalogo degli effetti](../effetti/catalogo-effetti.md): riga di Armatura della Pioggia → +2 ATK (senza DEF).
- [catalogo degli effetti](../effetti/catalogo-effetti.md): riga di SCP-500 → "il primo danno di qualunque tipo" invece di "il primo attacco".
- [catalogo degli effetti](../effetti/catalogo-effetti.md): riga di SCP-109 → *Borraccia*, lotto P2 (soglia M-06, `heal()`); aggiungere ai pezzi di motore "Spell che collega un effetto alla creatura che la riceve".
- [catalogo degli effetti](../effetti/catalogo-effetti.md): riga di SCP-458 → *Fette da dividere*, due modificatori in attesa in fila; aggiungerla a D-34 in [decisioni aperte](../decisioni-aperte/decisioni-aperte.md).
- [decisioni aperte](../decisioni-aperte/decisioni-aperte.md): chiudere **D-13** con la regola *Nessuna carta persa*; [carte](../carte/carte.md): sezione Burn riscritta (niente "creatura attiva", niente scarto); [catalogo degli effetti](../effetti/catalogo-effetti.md): "Regola Burn" e righe delle tre Burn con i bonus decisi; [log della partita](../log-partita/log-partita.md): evento `BURN_REVEALED`.
- [catalogo degli effetti](../effetti/catalogo-effetti.md): riga di Last Sanctuary → +5 DEF nello scontro corrente; sezione Hidden: scrivere le due regole di D-15 adottate l'11 novembre ("non nel primo turno", durata di "scontro corrente") e aggiornarle anche in [carte](../carte/carte.md) e [decisioni aperte](../decisioni-aperte/decisioni-aperte.md).
- [catalogo degli effetti](../effetti/catalogo-effetti.md): riga di Survival Instinct → +2 ATK, +1 DEF nello scontro corrente.
- [catalogo degli effetti](../effetti/catalogo-effetti.md): riga di Remnants of Reality → Hidden con condizione ("quando la propria creatura riceve una penalità"), fino a 5 punti, definizione di penalità; aggiungerla all'elenco delle Hidden con condizione in [carte](../carte/carte.md) e D-15.
- [catalogo degli effetti](../effetti/catalogo-effetti.md): riga di Unseen Exit → condizione esatta (solo attacchi, dopo le altre riduzioni), domande risolte.
- [catalogo degli effetti](../effetti/catalogo-effetti.md): riga di Hostile Presence → avv. −5 ATK nel primo round dello scontro; scrivere la durata "solo il primo round" per le Hidden `DAMAGE` "prima del calcolo dello scontro".
- [catalogo degli effetti](../effetti/catalogo-effetti.md): riga di Breaking Point → *Spezzata* (DEF ≤ 5: DEF a 0, nessun danno al giocatore; altrimenti −3 DEF).
- [catalogo degli effetti](../effetti/catalogo-effetti.md): riga di Static Interference → senza condizione (inizio del prossimo scontro), fino a 3 punti sospesi per tutto lo scontro, altrimenti 3 danni; toglierla dall'elenco delle Hidden con condizione in [carte](../carte/carte.md) e D-15 (al suo posto entra Remnants of Reality).
- [catalogo degli effetti](../effetti/catalogo-effetti.md) e [carte](../carte/carte.md): Echo Rift → secondario *Eco delle ferite* (+1 ATK alla prima ferita in ogni scontro); obiettivo dei Terreni circa 3,5 PS per creatura della fazione.
- [catalogo degli effetti](../effetti/catalogo-effetti.md) e [carte](../carte/carte.md): Hollow Clearing → secondario *Ogni penalità* (ogni penalità DEF ridotta di 1), con la definizione di penalità DEF.
- [catalogo degli effetti](../effetti/catalogo-effetti.md) e [carte](../carte/carte.md): Watching Rift → "1 punto complessivo" tolto prima dalla DEF, poi dall'ATK; una volta per creatura per scontro.
- [catalogo degli effetti](../effetti/catalogo-effetti.md) e [carte](../carte/carte.md): Devouring Abyss → "primo confronto" = inizio di ogni scontro, danno da effetto.
- [catalogo degli effetti](../effetti/catalogo-effetti.md): riga di Badge di Uscita Negata → *Nessuna uscita* (resta con 1 DEF al primo danno letale), lotto P2 (D-36, D-47); aggiungerlo alla riga "sopravvivenza a un danno letale" dei pezzi di motore.
- [catalogo degli effetti](../effetti/catalogo-effetti.md): riga di Bussola dei Sentieri Falsi → *Strada lunga* (scudo di 2 DEF rinnovato nei primi tre round dello scontro), lotto P2 (D-35, D-47).
- [catalogo degli effetti](../effetti/catalogo-effetti.md): riga di Fascicolo Sigillato 0 → *Censurato* (annulla la prima applicazione avversaria non di attacco), lotto P2; il pezzo di motore "filtro sul bersaglio" serve anche agli Oggetti.
- [catalogo degli effetti](../effetti/catalogo-effetti.md): riga di Campanello del Torii → *Campana d'allarme* (annulla la prima Hidden nemica), lotto P2 (M-11, D-15, D-47).
- [catalogo degli effetti](../effetti/catalogo-effetti.md): riga di Wretches → *Furia del ferito*, `afterDamage`, stato `used`, con la definizione di "ferito".
