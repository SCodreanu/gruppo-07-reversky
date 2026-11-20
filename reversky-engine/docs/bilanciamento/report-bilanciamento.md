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
| 30 PV, bonus D-04 (+2/−1 ATK) | 40.000 | Wound 51,5% | Invitation 48,4% | 0,0% | 36,6 | 49,9% | 4 |
| 30 PV, bonus di prova (+1 DEF) | 40.000 | Archive 55,5% | Invitation 44,6% | 0,0% | 44,4 | 49,7% | 7 |
| 25 PV, bonus D-04 (+2/−1 ATK) | 40.000 | Wound 52,7% | Invitation 47,2% | 0,0% | 33,1 | 49,8% | 6 |
| 25 PV, bonus di prova (+1 DEF) | 40.000 | Archive 54,6% | Invitation 42,5% | 0,0% | 41,3 | 49,6% | 7 |

## 30 PV, bonus D-04 (+2/−1 ATK)

### Fazioni

Contro le altre fazioni, senza le partite speculari.

| Fazione | Partite | Vittorie | Pareggi | Sconfitte | Punteggio |
|---|---|---|---|---|---|
| Wound | 15.000 | 7.713 | 25 | 7.262 | 51,5% |
| Invitation | 15.000 | 7.254 | 25 | 7.721 | 48,4% |
| Archive | 15.000 | 7.664 | 22 | 7.314 | 51,2% |
| Dream | 15.000 | 7.322 | 22 | 7.656 | 48,9% |

### Scontri diretti

Punteggio della fazione della riga contro quella della colonna, nei due posti. Ogni fazione batte quella indicata tra parentesi: atteso 55-65% contro di lei, 45-55% negli scontri neutri.

| | Wound | Invitation | Archive | Dream |
|---|---|---|---|---|
| Wound (Dream) | — | 54,7% | 0,0% | 99,8% |
| Invitation (Archive) | 45,4% | — | 100,0% | 0,0% |
| Archive (Wound) | 100,0% | 0,0% | — | 53,5% |
| Dream (Invitation) | 0,2% | 100,0% | 46,5% | — |

### Partite

- Primo giocatore (sfidante): 49,9%.
- Finite per punti vita (`LIFE_POINTS_ZERO` e `BOTH_LIFE_POINTS_ZERO`): 53,6%. D-01 cercava circa metà.
- Durata in round: media 36,6, minimo 8, mediana 37, 90° percentile 48, massimo 75.

| Motivo di fine | Partite | Quota |
|---|---|---|
| `BOTH_LIFE_POINTS_ZERO` | 4 | 0,0% |
| `LIFE_POINTS_ZERO` | 21.452 | 53,6% |
| `BOTH_DECKS_EMPTY` | 1.577 | 3,9% |
| `NO_CREATURES_LEFT` | 16.965 | 42,4% |
| `ROUND_LIMIT` | 2 | 0,0% |

### Rarità e tipi

Punteggio del lato che ha una carta del gruppo nel mazzo e del lato che l'ha giocata.

| Gruppo | Carte | Partite giocata | Nel mazzo | Da giocata |
|---|---|---|---|---|
| Common | 28 | 334.925 | 49,9% | 46,9% |
| Uncommon | 26 | 307.115 | 50,0% | 46,7% |
| Rare | 16 | 148.991 | 50,1% | 46,3% |
| Mythic | 12 | 112.185 | 50,1% | 45,6% |
| Legendary | 8 | 87.681 | 50,4% | 46,0% |
| Mutant | 2 | 43.723 | 50,7% | 46,4% |
| Spell | 12 | 336.011 | 50,0% | 46,5% |
| Hidden | 8 | 115.239 | 50,0% | 46,7% |
| Terreno | 4 | 66.158 | 50,0% | 47,1% |
| Oggetto | 4 | 65.858 | 50,0% | 45,7% |

### Avvisi

- Scontro di vantaggio Wound contro Dream: 99,8% (atteso 55-65%).
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
| Wound | 15.000 | 7.196 | 46 | 7.758 | 48,1% |
| Invitation | 15.000 | 6.667 | 38 | 8.295 | 44,6% |
| Archive | 15.000 | 8.302 | 42 | 6.656 | 55,5% |
| Dream | 15.000 | 7.750 | 44 | 7.206 | 51,8% |

### Scontri diretti

Punteggio della fazione della riga contro quella della colonna, nei due posti. Ogni fazione batte quella indicata tra parentesi: atteso 55-65% contro di lei, 45-55% negli scontri neutri.

| | Wound | Invitation | Archive | Dream |
|---|---|---|---|---|
| Wound (Dream) | — | 54,7% | 9,8% | 79,9% |
| Invitation (Archive) | 45,3% | — | 78,2% | 10,2% |
| Archive (Wound) | 90,2% | 21,8% | — | 54,5% |
| Dream (Invitation) | 20,1% | 89,8% | 45,5% | — |

### Partite

- Primo giocatore (sfidante): 49,7%.
- Finite per punti vita (`LIFE_POINTS_ZERO` e `BOTH_LIFE_POINTS_ZERO`): 28,8%. D-01 cercava circa metà.
- Durata in round: media 44,4, minimo 14, mediana 44, 90° percentile 52, massimo 75.

| Motivo di fine | Partite | Quota |
|---|---|---|
| `BOTH_LIFE_POINTS_ZERO` | 9 | 0,0% |
| `LIFE_POINTS_ZERO` | 11.525 | 28,8% |
| `BOTH_DECKS_EMPTY` | 2.426 | 6,1% |
| `NO_CREATURES_LEFT` | 26.037 | 65,1% |
| `ROUND_LIMIT` | 3 | 0,0% |

### Rarità e tipi

Punteggio del lato che ha una carta del gruppo nel mazzo e del lato che l'ha giocata.

| Gruppo | Carte | Partite giocata | Nel mazzo | Da giocata |
|---|---|---|---|---|
| Common | 28 | 363.243 | 49,8% | 48,9% |
| Uncommon | 26 | 334.693 | 50,2% | 49,2% |
| Rare | 16 | 164.689 | 50,0% | 48,7% |
| Mythic | 12 | 126.603 | 50,2% | 48,5% |
| Legendary | 8 | 98.758 | 50,6% | 48,9% |
| Mutant | 2 | 49.209 | 50,1% | 48,4% |
| Spell | 12 | 373.672 | 50,0% | 48,8% |
| Hidden | 8 | 130.184 | 50,0% | 49,3% |
| Terreno | 4 | 75.171 | 50,0% | 50,0% |
| Oggetto | 4 | 74.139 | 50,0% | 48,3% |

### Avvisi

- Scontro di vantaggio Wound contro Dream: 79,9% (atteso 55-65%).
- Scontro di vantaggio Invitation contro Archive: 78,2% (atteso 55-65%).
- Scontro di vantaggio Archive contro Wound: 90,2% (atteso 55-65%).
- Scontro di vantaggio Dream contro Invitation: 89,8% (atteso 55-65%).
- Fazione Invitation: 44,6% contro le altre (atteso 45-55%).
- Fazione Archive: 55,5% contro le altre (atteso 45-55%).
- Scala delle rarità (nel mazzo): Rare (50,0%) non supera Uncommon (50,2%).

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
| Wound | 15.000 | 7.897 | 24 | 7.079 | 52,7% |
| Invitation | 15.000 | 7.073 | 23 | 7.904 | 47,2% |
| Archive | 15.000 | 7.664 | 23 | 7.313 | 51,2% |
| Dream | 15.000 | 7.320 | 22 | 7.658 | 48,9% |

### Scontri diretti

Punteggio della fazione della riga contro quella della colonna, nei due posti. Ogni fazione batte quella indicata tra parentesi: atteso 55-65% contro di lei, 45-55% negli scontri neutri.

| | Wound | Invitation | Archive | Dream |
|---|---|---|---|---|
| Wound (Dream) | — | 58,2% | 0,1% | 99,8% |
| Invitation (Archive) | 41,8% | — | 99,9% | 0,0% |
| Archive (Wound) | 99,9% | 0,1% | — | 53,5% |
| Dream (Invitation) | 0,2% | 100,0% | 46,5% | — |

### Partite

- Primo giocatore (sfidante): 49,8%.
- Finite per punti vita (`LIFE_POINTS_ZERO` e `BOTH_LIFE_POINTS_ZERO`): 72,9%. D-01 cercava circa metà.
- Durata in round: media 33,1, minimo 6, mediana 33, 90° percentile 46, massimo 75.

| Motivo di fine | Partite | Quota |
|---|---|---|
| `BOTH_LIFE_POINTS_ZERO` | 27 | 0,1% |
| `LIFE_POINTS_ZERO` | 29.142 | 72,9% |
| `BOTH_DECKS_EMPTY` | 1.041 | 2,6% |
| `NO_CREATURES_LEFT` | 9.788 | 24,5% |
| `ROUND_LIMIT` | 2 | 0,0% |

### Rarità e tipi

Punteggio del lato che ha una carta del gruppo nel mazzo e del lato che l'ha giocata.

| Gruppo | Carte | Partite giocata | Nel mazzo | Da giocata |
|---|---|---|---|---|
| Common | 28 | 314.067 | 50,0% | 46,7% |
| Uncommon | 26 | 286.625 | 50,0% | 46,4% |
| Rare | 16 | 137.503 | 50,0% | 46,1% |
| Mythic | 12 | 101.997 | 50,1% | 45,6% |
| Legendary | 8 | 79.633 | 50,3% | 45,9% |
| Mutant | 2 | 39.712 | 50,6% | 46,3% |
| Spell | 12 | 308.954 | 50,0% | 46,7% |
| Hidden | 8 | 104.512 | 50,0% | 47,3% |
| Terreno | 4 | 60.043 | 50,0% | 47,6% |
| Oggetto | 4 | 59.784 | 50,0% | 46,0% |

### Avvisi

- Scontro neutro Wound contro Invitation: 58,2% (atteso 45-55%).
- Scontro di vantaggio Wound contro Dream: 99,8% (atteso 55-65%).
- Scontro di vantaggio Invitation contro Archive: 99,9% (atteso 55-65%).
- Scontro di vantaggio Archive contro Wound: 99,9% (atteso 55-65%).
- Scontro di vantaggio Dream contro Invitation: 100,0% (atteso 55-65%).
- Scala delle rarità (nel mazzo): Uncommon (50,0%) non supera Common (50,0%).

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
| Wound | 15.000 | 7.517 | 47 | 7.436 | 50,3% |
| Invitation | 15.000 | 6.358 | 34 | 8.608 | 42,5% |
| Archive | 15.000 | 8.170 | 52 | 6.778 | 54,6% |
| Dream | 15.000 | 7.863 | 51 | 7.086 | 52,6% |

### Scontri diretti

Punteggio della fazione della riga contro quella della colonna, nei due posti. Ogni fazione batte quella indicata tra parentesi: atteso 55-65% contro di lei, 45-55% negli scontri neutri.

| | Wound | Invitation | Archive | Dream |
|---|---|---|---|---|
| Wound (Dream) | — | 58,1% | 15,5% | 77,3% |
| Invitation (Archive) | 41,9% | — | 74,3% | 11,3% |
| Archive (Wound) | 84,6% | 25,7% | — | 53,7% |
| Dream (Invitation) | 22,7% | 88,7% | 46,3% | — |

### Partite

- Primo giocatore (sfidante): 49,6%.
- Finite per punti vita (`LIFE_POINTS_ZERO` e `BOTH_LIFE_POINTS_ZERO`): 54,0%. D-01 cercava circa metà.
- Durata in round: media 41,3, minimo 10, mediana 42, 90° percentile 51, massimo 75.

| Motivo di fine | Partite | Quota |
|---|---|---|
| `BOTH_LIFE_POINTS_ZERO` | 42 | 0,1% |
| `LIFE_POINTS_ZERO` | 21.540 | 53,8% |
| `BOTH_DECKS_EMPTY` | 1.597 | 4,0% |
| `NO_CREATURES_LEFT` | 16.819 | 42,0% |
| `ROUND_LIMIT` | 2 | 0,0% |

### Rarità e tipi

Punteggio del lato che ha una carta del gruppo nel mazzo e del lato che l'ha giocata.

| Gruppo | Carte | Partite giocata | Nel mazzo | Da giocata |
|---|---|---|---|---|
| Common | 28 | 348.320 | 49,9% | 48,9% |
| Uncommon | 26 | 320.144 | 50,2% | 49,0% |
| Rare | 16 | 156.000 | 50,0% | 48,5% |
| Mythic | 12 | 118.539 | 50,1% | 48,4% |
| Legendary | 8 | 92.308 | 50,5% | 48,8% |
| Mutant | 2 | 45.961 | 50,2% | 48,5% |
| Spell | 12 | 353.352 | 50,0% | 49,2% |
| Hidden | 8 | 121.911 | 50,0% | 49,9% |
| Terreno | 4 | 70.556 | 50,0% | 51,0% |
| Oggetto | 4 | 69.368 | 50,0% | 48,6% |

### Avvisi

- Scontro neutro Wound contro Invitation: 58,1% (atteso 45-55%).
- Scontro di vantaggio Wound contro Dream: 77,3% (atteso 55-65%).
- Scontro di vantaggio Invitation contro Archive: 74,3% (atteso 55-65%).
- Scontro di vantaggio Archive contro Wound: 84,6% (atteso 55-65%).
- Scontro di vantaggio Dream contro Invitation: 88,7% (atteso 55-65%).
- Fazione Invitation: 42,5% contro le altre (atteso 45-55%).
- Scala delle rarità (nel mazzo): Rare (50,0%) non supera Uncommon (50,2%).

### Carte fuori banda

Più di 10 punti dalla media del gruppo, con almeno 500 partite giocata.

Nessuna.

### Campione basso

Meno di 500 partite giocata: non si valutano.

Nessuna.

