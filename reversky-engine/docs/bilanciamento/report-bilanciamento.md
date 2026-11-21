# Report di bilanciamento

Generato da `BalanceSimulationTest` con `gradlew balanceTest` (`ENG-28`): non si modifica a mano, si rigenera. Gli stessi seed danno lo stesso report.

## Come è fatto

- Catalogo: `catalogo-finale.csv`, 120 carte, con il registro di tutti gli effetti dell'engine.
- Mazzi: di una sola fazione, nuovi a ogni partita. 20 creature della fazione (le Mutant valgono per entrambe le loro fazioni), il Terreno e l'Oggetto della fazione, 6 Spell, 2 Hidden. Ogni carta si sceglie a caso tra quelle ancora ammesse dai limiti dell'API: copie Common 3, Uncommon 2 (anche Spell e Hidden), Rare 2, Mythic, Legendary e Mutant 1; massimo 2 Legendary, 2 Mythic, 1 Mutant, 5 Rare contando Terreno e Oggetto; al massimo una Hidden `DAMAGE`.
- Partite: 16 accoppiamenti ordinati di fazioni (speculari compresi) × 2.500 = 40.000 per configurazione. La partita i dell'accoppiamento p ha seed 20261120 + p × 2.500 + i, uguale in tutte le configurazioni: cambiano solo le regole.
- Punteggio: vittoria 1, pareggio ½, sconfitta 0. Le fazioni e gli scontri diretti non contano le partite speculari; il primo giocatore (lo sfidante, che pesca per primo), la durata e le carte le contano tutte.
- Carte: "nel mazzo" se è nel mazzo di quel lato, "giocata" se è entrata in gioco (creatura in campo, Spell risolta, Hidden attivata, Terreno o Oggetto messo). Il gruppo è la rarità per le creature e il tipo per le altre carte; la sua media è pesata sulle partite.
- Il punteggio da giocata è sotto il 50% in quasi tutti i gruppi: chi perde gioca più carte, perché perde più creature. Il confronto dentro un gruppo resta valido (la banda di 10 punti usa il punteggio da giocata), il livello assoluto no: la scala delle rarità usa il punteggio nel mazzo, che non ha questo spostamento (deciso il 20 novembre).
- Avvisi: scontro di vantaggio fuori da 55-65%, scontro neutro e fazione fuori da 45-55%, ROUND_LIMIT sopra il 5%, primo giocatore fuori da 45-55%, carta a più di 10 punti dalla media del gruppo (con almeno 500 partite giocata), scala delle rarità delle creature non crescente.
- Dati di ogni carta: `report-carte.csv`.

## Sintesi

| Configurazione | Partite | Fazione migliore | Fazione peggiore | ROUND_LIMIT | Durata media | Primo giocatore | Avvisi |
|---|---|---|---|---|---|---|---|
| 30 PV, bonus D-04 (+2/−1 ATK) | 40.000 | Archive 51,0% | Dream 49,0% | 0,0% | 31,7 | 50,0% | 4 |
| 30 PV, bonus di prova (+1 DEF) | 40.000 | Invitation 51,7% | Dream 48,1% | 0,0% | 37,8 | 49,4% | 4 |
| 25 PV, bonus D-04 (+2/−1 ATK) | 40.000 | Archive 51,5% | Dream 48,5% | 0,0% | 28,4 | 49,7% | 4 |
| 25 PV, bonus di prova (+1 DEF) | 40.000 | Wound 52,3% | Dream 46,8% | 0,0% | 34,9 | 49,4% | 5 |

## 30 PV, bonus D-04 (+2/−1 ATK)

### Fazioni

Contro le altre fazioni, senza le partite speculari.

| Fazione | Partite | Vittorie | Pareggi | Sconfitte | Punteggio |
|---|---|---|---|---|---|
| Wound | 15.000 | 7.507 | 29 | 7.464 | 50,1% |
| Invitation | 15.000 | 7.461 | 29 | 7.510 | 49,8% |
| Archive | 15.000 | 7.646 | 21 | 7.333 | 51,0% |
| Dream | 15.000 | 7.336 | 21 | 7.643 | 49,0% |

### Scontri diretti

Punteggio della fazione della riga contro quella della colonna, nei due posti. Ogni fazione batte quella indicata tra parentesi: atteso 55-65% contro di lei, 45-55% negli scontri neutri.

| | Wound | Invitation | Archive | Dream |
|---|---|---|---|---|
| Wound (Dream) | — | 50,5% | 0,0% | 99,9% |
| Invitation (Archive) | 49,5% | — | 100,0% | 0,0% |
| Archive (Wound) | 100,0% | 0,0% | — | 53,2% |
| Dream (Invitation) | 0,1% | 100,0% | 46,9% | — |

### Partite

- Primo giocatore (sfidante): 50,0%.
- Finite per punti vita (`LIFE_POINTS_ZERO` e `BOTH_LIFE_POINTS_ZERO`): 56,4%. D-01 cercava circa metà.
- Durata in round: media 31,7, minimo 7, mediana 32, 90° percentile 41, massimo 75.

| Motivo di fine | Partite | Quota |
|---|---|---|
| `BOTH_LIFE_POINTS_ZERO` | 10 | 0,0% |
| `LIFE_POINTS_ZERO` | 22.548 | 56,4% |
| `BOTH_DECKS_EMPTY` | 1.701 | 4,3% |
| `NO_CREATURES_LEFT` | 15.740 | 39,4% |
| `ROUND_LIMIT` | 1 | 0,0% |

### Rarità e tipi

Punteggio del lato che ha una carta del gruppo nel mazzo e del lato che l'ha giocata.

| Gruppo | Carte | Partite giocata | Nel mazzo | Da giocata |
|---|---|---|---|---|
| Common | 28 | 333.445 | 49,9% | 47,3% |
| Uncommon | 26 | 305.826 | 50,0% | 47,2% |
| Rare | 16 | 147.957 | 50,2% | 46,9% |
| Mythic | 12 | 111.474 | 50,2% | 46,2% |
| Legendary | 8 | 86.883 | 50,3% | 46,5% |
| Mutant | 2 | 43.255 | 50,4% | 46,7% |
| Spell | 12 | 333.803 | 50,0% | 47,1% |
| Hidden | 8 | 114.140 | 50,0% | 47,4% |
| Terreno | 4 | 65.388 | 50,0% | 47,6% |
| Oggetto | 4 | 65.375 | 50,0% | 46,4% |

### Avvisi

- Scontro di vantaggio Wound contro Dream: 99,9% (atteso 55-65%).
- Scontro di vantaggio Invitation contro Archive: 100,0% (atteso 55-65%).
- Scontro di vantaggio Archive contro Wound: 100,0% (atteso 55-65%).
- Scontro di vantaggio Dream contro Invitation: 100,0% (atteso 55-65%).

### Carte fuori banda

Più di 10 punti dalla media del gruppo, con almeno 500 partite giocata.

Nessuna.

### Campione basso

Meno di 500 partite giocata: non si valutano.

Nessuna.

## 30 PV, bonus di prova (+1 DEF)

### Fazioni

Contro le altre fazioni, senza le partite speculari.

| Fazione | Partite | Vittorie | Pareggi | Sconfitte | Punteggio |
|---|---|---|---|---|---|
| Wound | 15.000 | 7.435 | 55 | 7.510 | 49,8% |
| Invitation | 15.000 | 7.739 | 47 | 7.214 | 51,7% |
| Archive | 15.000 | 7.544 | 43 | 7.413 | 50,4% |
| Dream | 15.000 | 7.181 | 57 | 7.762 | 48,1% |

### Scontri diretti

Punteggio della fazione della riga contro quella della colonna, nei due posti. Ogni fazione batte quella indicata tra parentesi: atteso 55-65% contro di lei, 45-55% negli scontri neutri.

| | Wound | Invitation | Archive | Dream |
|---|---|---|---|---|
| Wound (Dream) | — | 50,7% | 12,3% | 86,2% |
| Invitation (Archive) | 49,3% | — | 89,0% | 17,0% |
| Archive (Wound) | 87,7% | 11,0% | — | 52,7% |
| Dream (Invitation) | 13,8% | 83,0% | 47,3% | — |

### Partite

- Primo giocatore (sfidante): 49,4%.
- Finite per punti vita (`LIFE_POINTS_ZERO` e `BOTH_LIFE_POINTS_ZERO`): 33,4%. D-01 cercava circa metà.
- Durata in round: media 37,8, minimo 11, mediana 38, 90° percentile 44, massimo 75.

| Motivo di fine | Partite | Quota |
|---|---|---|
| `BOTH_LIFE_POINTS_ZERO` | 17 | 0,0% |
| `LIFE_POINTS_ZERO` | 13.332 | 33,3% |
| `BOTH_DECKS_EMPTY` | 2.656 | 6,6% |
| `NO_CREATURES_LEFT` | 23.993 | 60,0% |
| `ROUND_LIMIT` | 2 | 0,0% |

### Rarità e tipi

Punteggio del lato che ha una carta del gruppo nel mazzo e del lato che l'ha giocata.

| Gruppo | Carte | Partite giocata | Nel mazzo | Da giocata |
|---|---|---|---|---|
| Common | 28 | 360.485 | 49,9% | 49,0% |
| Uncommon | 26 | 332.149 | 50,0% | 49,1% |
| Rare | 16 | 163.188 | 50,1% | 48,9% |
| Mythic | 12 | 125.084 | 50,2% | 48,7% |
| Legendary | 8 | 97.675 | 50,6% | 49,2% |
| Mutant | 2 | 48.569 | 50,4% | 49,0% |
| Spell | 12 | 369.864 | 50,0% | 49,0% |
| Hidden | 8 | 128.569 | 50,0% | 49,5% |
| Terreno | 4 | 73.837 | 50,0% | 49,8% |
| Oggetto | 4 | 73.283 | 50,0% | 48,5% |

### Avvisi

- Scontro di vantaggio Wound contro Dream: 86,2% (atteso 55-65%).
- Scontro di vantaggio Invitation contro Archive: 89,0% (atteso 55-65%).
- Scontro di vantaggio Archive contro Wound: 87,7% (atteso 55-65%).
- Scontro di vantaggio Dream contro Invitation: 83,0% (atteso 55-65%).

### Carte fuori banda

Più di 10 punti dalla media del gruppo, con almeno 500 partite giocata.

Nessuna.

### Campione basso

Meno di 500 partite giocata: non si valutano.

Nessuna.

## 25 PV, bonus D-04 (+2/−1 ATK)

### Fazioni

Contro le altre fazioni, senza le partite speculari.

| Fazione | Partite | Vittorie | Pareggi | Sconfitte | Punteggio |
|---|---|---|---|---|---|
| Wound | 15.000 | 7.658 | 24 | 7.318 | 51,1% |
| Invitation | 15.000 | 7.321 | 24 | 7.655 | 48,9% |
| Archive | 15.000 | 7.718 | 21 | 7.261 | 51,5% |
| Dream | 15.000 | 7.258 | 21 | 7.721 | 48,5% |

### Scontri diretti

Punteggio della fazione della riga contro quella della colonna, nei due posti. Ogni fazione batte quella indicata tra parentesi: atteso 55-65% contro di lei, 45-55% negli scontri neutri.

| | Wound | Invitation | Archive | Dream |
|---|---|---|---|---|
| Wound (Dream) | — | 53,4% | 0,1% | 99,9% |
| Invitation (Archive) | 46,6% | — | 100,0% | 0,0% |
| Archive (Wound) | 99,9% | 0,0% | — | 54,7% |
| Dream (Invitation) | 0,1% | 100,0% | 45,4% | — |

### Partite

- Primo giocatore (sfidante): 49,7%.
- Finite per punti vita (`LIFE_POINTS_ZERO` e `BOTH_LIFE_POINTS_ZERO`): 73,8%. D-01 cercava circa metà.
- Durata in round: media 28,4, minimo 3, mediana 29, 90° percentile 39, massimo 75.

| Motivo di fine | Partite | Quota |
|---|---|---|
| `BOTH_LIFE_POINTS_ZERO` | 22 | 0,1% |
| `LIFE_POINTS_ZERO` | 29.507 | 73,8% |
| `BOTH_DECKS_EMPTY` | 1.118 | 2,8% |
| `NO_CREATURES_LEFT` | 9.352 | 23,4% |
| `ROUND_LIMIT` | 1 | 0,0% |

### Rarità e tipi

Punteggio del lato che ha una carta del gruppo nel mazzo e del lato che l'ha giocata.

| Gruppo | Carte | Partite giocata | Nel mazzo | Da giocata |
|---|---|---|---|---|
| Common | 28 | 311.587 | 49,9% | 47,1% |
| Uncommon | 26 | 284.419 | 50,0% | 47,0% |
| Rare | 16 | 136.011 | 50,1% | 46,6% |
| Mythic | 12 | 100.785 | 50,2% | 46,2% |
| Legendary | 8 | 78.536 | 50,2% | 46,4% |
| Mutant | 2 | 39.220 | 50,4% | 46,7% |
| Spell | 12 | 305.818 | 50,0% | 47,4% |
| Hidden | 8 | 103.128 | 50,0% | 47,9% |
| Terreno | 4 | 59.048 | 50,0% | 48,0% |
| Oggetto | 4 | 59.258 | 50,0% | 46,6% |

### Avvisi

- Scontro di vantaggio Wound contro Dream: 99,9% (atteso 55-65%).
- Scontro di vantaggio Invitation contro Archive: 100,0% (atteso 55-65%).
- Scontro di vantaggio Archive contro Wound: 99,9% (atteso 55-65%).
- Scontro di vantaggio Dream contro Invitation: 100,0% (atteso 55-65%).

### Carte fuori banda

Più di 10 punti dalla media del gruppo, con almeno 500 partite giocata.

Nessuna.

### Campione basso

Meno di 500 partite giocata: non si valutano.

Nessuna.

## 25 PV, bonus di prova (+1 DEF)

### Fazioni

Contro le altre fazioni, senza le partite speculari.

| Fazione | Partite | Vittorie | Pareggi | Sconfitte | Punteggio |
|---|---|---|---|---|---|
| Wound | 15.000 | 7.828 | 48 | 7.124 | 52,3% |
| Invitation | 15.000 | 7.540 | 39 | 7.421 | 50,4% |
| Archive | 15.000 | 7.538 | 50 | 7.412 | 50,4% |
| Dream | 15.000 | 6.993 | 65 | 7.942 | 46,8% |

### Scontri diretti

Punteggio della fazione della riga contro quella della colonna, nei due posti. Ogni fazione batte quella indicata tra parentesi: atteso 55-65% contro di lei, 45-55% negli scontri neutri.

| | Wound | Invitation | Archive | Dream |
|---|---|---|---|---|
| Wound (Dream) | — | 54,0% | 18,0% | 85,0% |
| Invitation (Archive) | 46,0% | — | 85,1% | 20,1% |
| Archive (Wound) | 82,0% | 14,9% | — | 54,4% |
| Dream (Invitation) | 15,0% | 79,9% | 45,6% | — |

### Partite

- Primo giocatore (sfidante): 49,4%.
- Finite per punti vita (`LIFE_POINTS_ZERO` e `BOTH_LIFE_POINTS_ZERO`): 56,4%. D-01 cercava circa metà.
- Durata in round: media 34,9, minimo 7, mediana 36, 90° percentile 43, massimo 75.

| Motivo di fine | Partite | Quota |
|---|---|---|
| `BOTH_LIFE_POINTS_ZERO` | 47 | 0,1% |
| `LIFE_POINTS_ZERO` | 22.517 | 56,3% |
| `BOTH_DECKS_EMPTY` | 1.773 | 4,4% |
| `NO_CREATURES_LEFT` | 15.661 | 39,2% |
| `ROUND_LIMIT` | 2 | 0,0% |

### Rarità e tipi

Punteggio del lato che ha una carta del gruppo nel mazzo e del lato che l'ha giocata.

| Gruppo | Carte | Partite giocata | Nel mazzo | Da giocata |
|---|---|---|---|---|
| Common | 28 | 344.201 | 50,0% | 49,0% |
| Uncommon | 26 | 316.100 | 49,9% | 48,9% |
| Rare | 16 | 153.620 | 50,1% | 48,8% |
| Mythic | 12 | 116.228 | 50,3% | 48,7% |
| Legendary | 8 | 90.518 | 50,4% | 49,1% |
| Mutant | 2 | 45.078 | 50,4% | 49,0% |
| Spell | 12 | 347.296 | 50,0% | 49,5% |
| Hidden | 8 | 119.476 | 50,0% | 50,1% |
| Terreno | 4 | 68.650 | 50,0% | 50,6% |
| Oggetto | 4 | 68.208 | 50,0% | 48,8% |

### Avvisi

- Scontro di vantaggio Wound contro Dream: 85,0% (atteso 55-65%).
- Scontro di vantaggio Invitation contro Archive: 85,1% (atteso 55-65%).
- Scontro di vantaggio Archive contro Wound: 82,0% (atteso 55-65%).
- Scontro di vantaggio Dream contro Invitation: 79,9% (atteso 55-65%).
- Scala delle rarità (nel mazzo): Uncommon (49,9%) non supera Common (50,0%).

### Carte fuori banda

Più di 10 punti dalla media del gruppo, con almeno 500 partite giocata.

Nessuna.

### Campione basso

Meno di 500 partite giocata: non si valutano.

Nessuna.

