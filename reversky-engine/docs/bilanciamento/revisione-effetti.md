# Revisione degli effetti

Revisione carta per carta degli effetti del *Catalogo completo 120 carte*, prima di qualsiasi simulazione.
Si aggiorna dopo ogni decisione, così il lavoro sopravvive a un cambio di sessione: **all'inizio di ogni sessione si rilegge questo file e si riprende da dove si ferma.**

- Fonte delle carte: [catalogo degli effetti](../effetti/catalogo-effetti.md). Regole: [decisioni aperte](../decisioni-aperte/decisioni-aperte.md).
- Possono cambiare effetti, ATK e DEF; **nomi e rarità restano** quelli del catalogo.
- Prima gli effetti; le statistiche solo come intervallo indicativo, i numeri esatti arrivano con la simulazione.
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
| Mutant | da fissare con le due carte |

### Ordine della revisione — deciso il 10 novembre

Per rarità, dalla più bassa alla più alta, tutte le fazioni insieme, così ogni carta si confronta con quelle della sua rarità e le carte semplici fissano la scala prima delle altre:

1. Uncommon (26), in ordine di fazione: Invitation, Wound, Archive, Dream, ognuna nell'ordine del catalogo;
2. Rare (16), Mythic (12), Legendary (8), con lo stesso ordine di fazione;
3. Mutant (2);
4. Spell (12), Hidden (8), Terreni (4), Oggetti (4);
5. Common (28): solo l'intervallo delle statistiche, perché non hanno effetto.

Prossima carta: **Wampus Cat** (Uncommon, Theory of the Invitation).

### Carte per passo — deciso il 10 novembre

**Una carta per passo**: dopo ogni carta si aspetta la scelta prima di passare alla successiva.

### Regole decise prima della revisione (10 novembre)

Valgono per tutte le carte; il dettaglio è nelle [decisioni aperte](../decisioni-aperte/decisioni-aperte.md).

| Regola | Decisione |
|---|---|
| "Primo round", "primo attacco", "una volta" (D-31, D-33) | una volta per entrata in campo; conta solo un attacco con danno > 0 |
| DEF temporanea (D-35) | scudo: assorbe i danni per prima, la scadenza non uccide |
| Durata delle Spell (D-12) | finché la creatura resta in campo |
| Hidden (D-15) | con una condizione: quando la condizione si verifica; senza: all'inizio del prossimo scontro del proprietario |
| Terreni (D-19) | un solo Terreno in gioco: quello pescato dopo sostituisce il precedente |
| Effetti secondari dei Terreni (D-18) | si rivedono ora, si implementano dopo il bonus base |
| Oggetti (D-47) | monouso, sulla prima creatura del proprietario che ne soddisfa la condizione |
| Breaking Point (D-16) | DEF 0 uccide |
| Mutant (D-20) | sopra Legendary negli effetti che confrontano le rarità |
| Burn (D-13) | **rimandato** |

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
| M-15 | Burn: rivelare la prossima carta del mazzo | D-13 (rimandato) |

Zone: mazzo, campo (una creatura per giocatore), zona Hidden, Terreno (uno solo, condiviso), Oggetto (uno per giocatore), scarti.
Durate possibili: un round, N round dall'entrata, lo scontro, la permanenza in campo, la partita.

Carte già segnalate da riprendere quando si arriva a loro: The Hollow Wyrm (con D-19 la condizione non si verifica mai), La Regina dei Sentieri Perduti ("sceglie" non è deterministico), Unseen Exit e Static Interference (condizione esatta, D-15), The Null Witness (Terreno "nemico", D-38), riga orfana dopo SCP-3000 nel catalogo.

## Carte decise

Nessuna carta ancora.

## Modifiche ai documenti da fare a fine revisione

Nessuna ancora.
