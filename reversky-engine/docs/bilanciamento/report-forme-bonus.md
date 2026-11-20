# Report delle forme del bonus di fazione

Generato da `BalanceSimulationTest` con `gradlew balanceTest` (`ENG-28`): non si modifica a mano, si rigenera. Gli stessi seed danno lo stesso report.

## Come è fatto

- Forme del bonus di fazione da provare per D-04, con gli stessi mazzi e seed di `report-bilanciamento.md`: cambia solo il bonus. +1 ATK contro +1 DEF: chi batte l'avversaria ha +1 ATK in ogni round dello scontro, chi è battuto ha +1 DEF una volta per scontro contro chi lo batte. La variante per coppia dà +2 DEF invece di +1 a Wound contro Archive e a Invitation contro Dream.
- Catalogo: `catalogo-finale.csv`, 120 carte, con il registro di tutti gli effetti dell'engine.
- Mazzi: di una sola fazione, nuovi a ogni partita. 20 creature della fazione (le Mutant valgono per entrambe le loro fazioni), il Terreno e l'Oggetto della fazione, 6 Spell, 2 Hidden. Ogni carta si sceglie a caso tra quelle ancora ammesse dai limiti dell'API: copie Common 3, Uncommon 2 (anche Spell e Hidden), Rare 2, Mythic, Legendary e Mutant 1; massimo 2 Legendary, 2 Mythic, 1 Mutant, 5 Rare contando Terreno e Oggetto; al massimo una Hidden `DAMAGE`.
- Partite: 16 accoppiamenti ordinati di fazioni (speculari compresi) × 2.500 = 40.000 per configurazione. La partita i dell'accoppiamento p ha seed 20261120 + p × 2.500 + i, uguale in tutte le configurazioni: cambiano solo le regole.
- Punteggio: vittoria 1, pareggio ½, sconfitta 0. Le fazioni e gli scontri diretti non contano le partite speculari; il primo giocatore (lo sfidante, che pesca per primo), la durata e le carte le contano tutte.
- Carte: "nel mazzo" se è nel mazzo di quel lato, "giocata" se è entrata in gioco (creatura in campo, Spell risolta, Hidden attivata, Terreno o Oggetto messo). Il gruppo è la rarità per le creature e il tipo per le altre carte; la sua media è pesata sulle partite.
- Il punteggio da giocata è sotto il 50% in quasi tutti i gruppi: chi perde gioca più carte, perché perde più creature. Il confronto dentro un gruppo resta valido (la banda di 10 punti usa il punteggio da giocata), il livello assoluto no: la scala delle rarità usa il punteggio nel mazzo, che non ha questo spostamento (deciso il 20 novembre).
- Avvisi: scontro di vantaggio fuori da 55-65%, scontro neutro e fazione fuori da 45-55%, ROUND_LIMIT sopra il 5%, primo giocatore fuori da 45-55%, carta a più di 10 punti dalla media del gruppo (con almeno 500 partite giocata), scala delle rarità delle creature non crescente.
- Dati di ogni carta: `report-forme-bonus-carte.csv`.

## Sintesi

| Configurazione | Partite | Fazione migliore | Fazione peggiore | ROUND_LIMIT | Durata media | Primo giocatore | Avvisi |
|---|---|---|---|---|---|---|---|
| 30 PV, +1 ATK contro +1 DEF | 40.000 | Archive 56,1% | Invitation 42,5% | 0,0% | 42,5 | 49,6% | 6 |
| 25 PV, +1 ATK contro +1 DEF | 40.000 | Archive 54,7% | Invitation 41,3% | 0,0% | 39,3 | 49,8% | 5 |
| 30 PV, +1 ATK contro +1 DEF, +2 DEF a Wound contro Archive e a Invitation contro Dream | 40.000 | Wound 60,6% | Archive 42,4% | 0,0% | 43,2 | 49,5% | 7 |
| 25 PV, +1 ATK contro +1 DEF, +2 DEF a Wound contro Archive e a Invitation contro Dream | 40.000 | Wound 62,0% | Archive 43,2% | 0,0% | 40,0 | 49,5% | 7 |

## 30 PV, +1 ATK contro +1 DEF

### Fazioni

Contro le altre fazioni, senza le partite speculari.

| Fazione | Partite | Vittorie | Pareggi | Sconfitte | Punteggio |
|---|---|---|---|---|---|
| Wound | 15.000 | 7.009 | 68 | 7.923 | 47,0% |
| Invitation | 15.000 | 6.344 | 50 | 8.606 | 42,5% |
| Archive | 15.000 | 8.380 | 55 | 6.565 | 56,1% |
| Dream | 15.000 | 8.155 | 51 | 6.794 | 54,5% |

### Scontri diretti

Punteggio della fazione della riga contro quella della colonna, nei due posti. Ogni fazione batte quella indicata tra parentesi: atteso 55-65% contro di lei, 45-55% negli scontri neutri.

| | Wound | Invitation | Archive | Dream |
|---|---|---|---|---|
| Wound (Dream) | — | 55,3% | 28,4% | 57,2% |
| Invitation (Archive) | 44,8% | — | 58,2% | 24,5% |
| Archive (Wound) | 71,6% | 41,8% | — | 54,7% |
| Dream (Invitation) | 42,8% | 75,5% | 45,3% | — |

### Partite

- Primo giocatore (sfidante): 49,6%.
- Finite per punti vita (`LIFE_POINTS_ZERO` e `BOTH_LIFE_POINTS_ZERO`): 32,3%. D-01 cercava circa metà.
- Durata in round: media 42,5, minimo 12, mediana 43, 90° percentile 50, massimo 75.

| Motivo di fine | Partite | Quota |
|---|---|---|
| `BOTH_LIFE_POINTS_ZERO` | 20 | 0,1% |
| `LIFE_POINTS_ZERO` | 12.887 | 32,2% |
| `BOTH_DECKS_EMPTY` | 2.858 | 7,1% |
| `NO_CREATURES_LEFT` | 24.234 | 60,6% |
| `ROUND_LIMIT` | 1 | 0,0% |

### Rarità e tipi

Punteggio del lato che ha una carta del gruppo nel mazzo e del lato che l'ha giocata.

| Gruppo | Carte | Partite giocata | Nel mazzo | Da giocata |
|---|---|---|---|---|
| Common | 28 | 363.155 | 49,7% | 49,1% |
| Uncommon | 26 | 334.591 | 50,3% | 49,6% |
| Rare | 16 | 164.687 | 50,0% | 49,1% |
| Mythic | 12 | 126.498 | 50,2% | 49,0% |
| Legendary | 8 | 98.833 | 50,6% | 49,4% |
| Mutant | 2 | 49.262 | 50,3% | 49,1% |
| Spell | 12 | 373.716 | 50,0% | 49,3% |
| Hidden | 8 | 130.058 | 50,0% | 50,0% |
| Terreno | 4 | 75.178 | 50,0% | 50,7% |
| Oggetto | 4 | 74.132 | 50,0% | 48,9% |

### Avvisi

- Scontro neutro Wound contro Invitation: 55,3% (atteso 45-55%).
- Scontro di vantaggio Archive contro Wound: 71,6% (atteso 55-65%).
- Scontro di vantaggio Dream contro Invitation: 75,5% (atteso 55-65%).
- Fazione Invitation: 42,5% contro le altre (atteso 45-55%).
- Fazione Archive: 56,1% contro le altre (atteso 45-55%).
- Scala delle rarità (nel mazzo): Rare (50,0%) non supera Uncommon (50,3%).

### Carte fuori banda

Più di 10 punti dalla media del gruppo, con almeno 500 partite giocata.

Nessuna.

### Campione basso

Meno di 500 partite giocata: non si valutano.

Nessuna.

## 25 PV, +1 ATK contro +1 DEF

### Fazioni

Contro le altre fazioni, senza le partite speculari.

| Fazione | Partite | Vittorie | Pareggi | Sconfitte | Punteggio |
|---|---|---|---|---|---|
| Wound | 15.000 | 7.537 | 57 | 7.406 | 50,4% |
| Invitation | 15.000 | 6.177 | 48 | 8.775 | 41,3% |
| Archive | 15.000 | 8.186 | 47 | 6.767 | 54,7% |
| Dream | 15.000 | 8.000 | 48 | 6.952 | 53,5% |

### Scontri diretti

Punteggio della fazione della riga contro quella della colonna, nei due posti. Ogni fazione batte quella indicata tra parentesi: atteso 55-65% contro di lei, 45-55% negli scontri neutri.

| | Wound | Invitation | Archive | Dream |
|---|---|---|---|---|
| Wound (Dream) | — | 58,0% | 32,1% | 61,2% |
| Invitation (Archive) | 42,0% | — | 58,0% | 24,0% |
| Archive (Wound) | 67,9% | 42,0% | — | 54,3% |
| Dream (Invitation) | 38,8% | 76,0% | 45,7% | — |

### Partite

- Primo giocatore (sfidante): 49,8%.
- Finite per punti vita (`LIFE_POINTS_ZERO` e `BOTH_LIFE_POINTS_ZERO`): 57,5%. D-01 cercava circa metà.
- Durata in round: media 39,3, minimo 10, mediana 40, 90° percentile 48, massimo 75.

| Motivo di fine | Partite | Quota |
|---|---|---|
| `BOTH_LIFE_POINTS_ZERO` | 39 | 0,1% |
| `LIFE_POINTS_ZERO` | 22.980 | 57,5% |
| `BOTH_DECKS_EMPTY` | 1.816 | 4,5% |
| `NO_CREATURES_LEFT` | 15.164 | 37,9% |
| `ROUND_LIMIT` | 1 | 0,0% |

### Rarità e tipi

Punteggio del lato che ha una carta del gruppo nel mazzo e del lato che l'ha giocata.

| Gruppo | Carte | Partite giocata | Nel mazzo | Da giocata |
|---|---|---|---|---|
| Common | 28 | 346.827 | 49,8% | 49,1% |
| Uncommon | 26 | 318.436 | 50,2% | 49,3% |
| Rare | 16 | 155.065 | 50,0% | 48,9% |
| Mythic | 12 | 117.526 | 50,2% | 48,9% |
| Legendary | 8 | 91.681 | 50,5% | 49,2% |
| Mutant | 2 | 45.728 | 50,3% | 49,1% |
| Spell | 12 | 351.165 | 50,0% | 49,7% |
| Hidden | 8 | 120.821 | 50,0% | 50,5% |
| Terreno | 4 | 69.993 | 50,0% | 51,9% |
| Oggetto | 4 | 68.803 | 50,0% | 49,2% |

### Avvisi

- Scontro neutro Wound contro Invitation: 58,0% (atteso 45-55%).
- Scontro di vantaggio Archive contro Wound: 67,9% (atteso 55-65%).
- Scontro di vantaggio Dream contro Invitation: 76,0% (atteso 55-65%).
- Fazione Invitation: 41,3% contro le altre (atteso 45-55%).
- Scala delle rarità (nel mazzo): Rare (50,0%) non supera Uncommon (50,2%).

### Carte fuori banda

Più di 10 punti dalla media del gruppo, con almeno 500 partite giocata.

Nessuna.

### Campione basso

Meno di 500 partite giocata: non si valutano.

Nessuna.

## 30 PV, +1 ATK contro +1 DEF, +2 DEF a Wound contro Archive e a Invitation contro Dream

### Fazioni

Contro le altre fazioni, senza le partite speculari.

| Fazione | Partite | Vittorie | Pareggi | Sconfitte | Punteggio |
|---|---|---|---|---|---|
| Wound | 15.000 | 9.059 | 69 | 5.872 | 60,6% |
| Invitation | 15.000 | 8.037 | 58 | 6.905 | 53,8% |
| Archive | 15.000 | 6.329 | 56 | 8.615 | 42,4% |
| Dream | 15.000 | 6.454 | 59 | 8.487 | 43,2% |

### Scontri diretti

Punteggio della fazione della riga contro quella della colonna, nei due posti. Ogni fazione batte quella indicata tra parentesi: atteso 55-65% contro di lei, 45-55% negli scontri neutri.

| | Wound | Invitation | Archive | Dream |
|---|---|---|---|---|
| Wound (Dream) | — | 55,3% | 69,4% | 57,2% |
| Invitation (Archive) | 44,8% | — | 58,2% | 58,4% |
| Archive (Wound) | 30,6% | 41,8% | — | 54,7% |
| Dream (Invitation) | 42,8% | 41,6% | 45,3% | — |

### Partite

- Primo giocatore (sfidante): 49,5%.
- Finite per punti vita (`LIFE_POINTS_ZERO` e `BOTH_LIFE_POINTS_ZERO`): 30,9%. D-01 cercava circa metà.
- Durata in round: media 43,2, minimo 13, mediana 44, 90° percentile 50, massimo 75.

| Motivo di fine | Partite | Quota |
|---|---|---|
| `BOTH_LIFE_POINTS_ZERO` | 15 | 0,0% |
| `LIFE_POINTS_ZERO` | 12.326 | 30,8% |
| `BOTH_DECKS_EMPTY` | 2.910 | 7,3% |
| `NO_CREATURES_LEFT` | 24.748 | 61,9% |
| `ROUND_LIMIT` | 1 | 0,0% |

### Rarità e tipi

Punteggio del lato che ha una carta del gruppo nel mazzo e del lato che l'ha giocata.

| Gruppo | Carte | Partite giocata | Nel mazzo | Da giocata |
|---|---|---|---|---|
| Common | 28 | 363.619 | 49,9% | 49,2% |
| Uncommon | 26 | 335.393 | 49,8% | 49,0% |
| Rare | 16 | 165.174 | 50,1% | 49,2% |
| Mythic | 12 | 127.002 | 50,3% | 49,1% |
| Legendary | 8 | 99.268 | 50,8% | 49,8% |
| Mutant | 2 | 49.416 | 50,6% | 49,4% |
| Spell | 12 | 374.329 | 50,0% | 49,2% |
| Hidden | 8 | 130.466 | 50,0% | 50,4% |
| Terreno | 4 | 75.280 | 50,0% | 50,6% |
| Oggetto | 4 | 74.297 | 50,0% | 48,8% |

### Avvisi

- Scontro neutro Wound contro Invitation: 55,3% (atteso 45-55%).
- Scontro di vantaggio Archive contro Wound: 30,6% (atteso 55-65%).
- Scontro di vantaggio Dream contro Invitation: 41,6% (atteso 55-65%).
- Fazione Wound: 60,6% contro le altre (atteso 45-55%).
- Fazione Archive: 42,4% contro le altre (atteso 45-55%).
- Fazione Dream: 43,2% contro le altre (atteso 45-55%).
- Scala delle rarità (nel mazzo): Uncommon (49,8%) non supera Common (49,9%).

### Carte fuori banda

Più di 10 punti dalla media del gruppo, con almeno 500 partite giocata.

Nessuna.

### Campione basso

Meno di 500 partite giocata: non si valutano.

Nessuna.

## 25 PV, +1 ATK contro +1 DEF, +2 DEF a Wound contro Archive e a Invitation contro Dream

### Fazioni

Contro le altre fazioni, senza le partite speculari.

| Fazione | Partite | Vittorie | Pareggi | Sconfitte | Punteggio |
|---|---|---|---|---|---|
| Wound | 15.000 | 9.269 | 61 | 5.670 | 62,0% |
| Invitation | 15.000 | 7.586 | 58 | 7.356 | 50,8% |
| Archive | 15.000 | 6.450 | 51 | 8.499 | 43,2% |
| Dream | 15.000 | 6.581 | 58 | 8.361 | 44,1% |

### Scontri diretti

Punteggio della fazione della riga contro quella della colonna, nei due posti. Ogni fazione batte quella indicata tra parentesi: atteso 55-65% contro di lei, 45-55% negli scontri neutri.

| | Wound | Invitation | Archive | Dream |
|---|---|---|---|---|
| Wound (Dream) | — | 58,0% | 66,8% | 61,2% |
| Invitation (Archive) | 42,0% | — | 58,0% | 52,3% |
| Archive (Wound) | 33,2% | 42,0% | — | 54,3% |
| Dream (Invitation) | 38,8% | 47,7% | 45,7% | — |

### Partite

- Primo giocatore (sfidante): 49,5%.
- Finite per punti vita (`LIFE_POINTS_ZERO` e `BOTH_LIFE_POINTS_ZERO`): 55,9%. D-01 cercava circa metà.
- Durata in round: media 40,0, minimo 9, mediana 41, 90° percentile 49, massimo 75.

| Motivo di fine | Partite | Quota |
|---|---|---|
| `BOTH_LIFE_POINTS_ZERO` | 42 | 0,1% |
| `LIFE_POINTS_ZERO` | 22.326 | 55,8% |
| `BOTH_DECKS_EMPTY` | 1.879 | 4,7% |
| `NO_CREATURES_LEFT` | 15.752 | 39,4% |
| `ROUND_LIMIT` | 1 | 0,0% |

### Rarità e tipi

Punteggio del lato che ha una carta del gruppo nel mazzo e del lato che l'ha giocata.

| Gruppo | Carte | Partite giocata | Nel mazzo | Da giocata |
|---|---|---|---|---|
| Common | 28 | 347.840 | 49,9% | 49,2% |
| Uncommon | 26 | 319.760 | 49,7% | 48,9% |
| Rare | 16 | 155.911 | 50,1% | 49,0% |
| Mythic | 12 | 118.278 | 50,2% | 49,0% |
| Legendary | 8 | 92.403 | 50,7% | 49,6% |
| Mutant | 2 | 45.883 | 50,6% | 49,5% |
| Spell | 12 | 352.585 | 50,0% | 49,7% |
| Hidden | 8 | 121.510 | 50,0% | 50,9% |
| Terreno | 4 | 70.318 | 50,0% | 51,8% |
| Oggetto | 4 | 69.173 | 50,0% | 49,2% |

### Avvisi

- Scontro neutro Wound contro Invitation: 58,0% (atteso 45-55%).
- Scontro di vantaggio Archive contro Wound: 33,2% (atteso 55-65%).
- Scontro di vantaggio Dream contro Invitation: 47,7% (atteso 55-65%).
- Fazione Wound: 62,0% contro le altre (atteso 45-55%).
- Fazione Archive: 43,2% contro le altre (atteso 45-55%).
- Fazione Dream: 44,1% contro le altre (atteso 45-55%).
- Scala delle rarità (nel mazzo): Uncommon (49,7%) non supera Common (49,9%).

### Carte fuori banda

Più di 10 punti dalla media del gruppo, con almeno 500 partite giocata.

Nessuna.

### Campione basso

Meno di 500 partite giocata: non si valutano.

Nessuna.

