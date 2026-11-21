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
| 30 PV, +1 ATK contro +1 DEF | 40.000 | Invitation 51,2% | Dream 48,7% | 0,0% | 36,7 | 49,2% | 0 |
| 25 PV, +1 ATK contro +1 DEF | 40.000 | Wound 52,6% | Dream 47,9% | 0,0% | 33,8 | 49,4% | 0 |
| 30 PV, +1 ATK contro +1 DEF, +2 DEF a Wound contro Archive e a Invitation contro Dream | 40.000 | Wound 64,3% | Archive 35,8% | 0,0% | 37,2 | 49,4% | 7 |
| 25 PV, +1 ATK contro +1 DEF, +2 DEF a Wound contro Archive e a Invitation contro Dream | 40.000 | Wound 64,9% | Dream 36,8% | 0,0% | 34,3 | 49,7% | 7 |

## 30 PV, +1 ATK contro +1 DEF

### Fazioni

Contro le altre fazioni, senza le partite speculari.

| Fazione | Partite | Vittorie | Pareggi | Sconfitte | Punteggio |
|---|---|---|---|---|---|
| Wound | 15.000 | 7.479 | 81 | 7.440 | 50,1% |
| Invitation | 15.000 | 7.643 | 84 | 7.273 | 51,2% |
| Archive | 15.000 | 7.448 | 89 | 7.463 | 50,0% |
| Dream | 15.000 | 7.261 | 84 | 7.655 | 48,7% |

### Scontri diretti

Punteggio della fazione della riga contro quella della colonna, nei due posti. Ogni fazione batte quella indicata tra parentesi: atteso 55-65% contro di lei, 45-55% negli scontri neutri.

| | Wound | Invitation | Archive | Dream |
|---|---|---|---|---|
| Wound (Dream) | — | 51,0% | 41,5% | 57,9% |
| Invitation (Archive) | 49,0% | — | 61,8% | 42,9% |
| Archive (Wound) | 58,5% | 38,2% | — | 53,2% |
| Dream (Invitation) | 42,1% | 57,1% | 46,9% | — |

### Partite

- Primo giocatore (sfidante): 49,2%.
- Finite per punti vita (`LIFE_POINTS_ZERO` e `BOTH_LIFE_POINTS_ZERO`): 34,6%. D-01 cercava circa metà.
- Durata in round: media 36,7, minimo 11, mediana 37, 90° percentile 43, massimo 75.

| Motivo di fine | Partite | Quota |
|---|---|---|
| `BOTH_LIFE_POINTS_ZERO` | 18 | 0,0% |
| `LIFE_POINTS_ZERO` | 13.834 | 34,6% |
| `BOTH_DECKS_EMPTY` | 3.455 | 8,6% |
| `NO_CREATURES_LEFT` | 22.690 | 56,7% |
| `ROUND_LIMIT` | 3 | 0,0% |

### Rarità e tipi

Punteggio del lato che ha una carta del gruppo nel mazzo e del lato che l'ha giocata.

| Gruppo | Carte | Partite giocata | Nel mazzo | Da giocata |
|---|---|---|---|---|
| Common | 28 | 361.303 | 49,8% | 49,3% |
| Uncommon | 26 | 333.195 | 50,1% | 49,5% |
| Rare | 16 | 163.649 | 50,1% | 49,4% |
| Mythic | 12 | 125.719 | 50,2% | 49,3% |
| Legendary | 8 | 98.109 | 50,8% | 49,9% |
| Mutant | 2 | 48.840 | 50,5% | 49,6% |
| Spell | 12 | 371.224 | 50,0% | 49,5% |
| Hidden | 8 | 129.094 | 50,0% | 50,3% |
| Terreno | 4 | 74.211 | 50,0% | 50,6% |
| Oggetto | 4 | 73.581 | 50,0% | 49,1% |

### Avvisi

Nessuno.

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
| Wound | 15.000 | 7.857 | 73 | 7.070 | 52,6% |
| Invitation | 15.000 | 7.401 | 76 | 7.523 | 49,6% |
| Archive | 15.000 | 7.444 | 89 | 7.467 | 49,9% |
| Dream | 15.000 | 7.132 | 94 | 7.774 | 47,9% |

### Scontri diretti

Punteggio della fazione della riga contro quella della colonna, nei due posti. Ogni fazione batte quella indicata tra parentesi: atteso 55-65% contro di lei, 45-55% negli scontri neutri.

| | Wound | Invitation | Archive | Dream |
|---|---|---|---|---|
| Wound (Dream) | — | 53,5% | 42,9% | 61,4% |
| Invitation (Archive) | 46,5% | — | 62,1% | 40,2% |
| Archive (Wound) | 57,1% | 37,9% | — | 54,8% |
| Dream (Invitation) | 38,6% | 59,8% | 45,2% | — |

### Partite

- Primo giocatore (sfidante): 49,4%.
- Finite per punti vita (`LIFE_POINTS_ZERO` e `BOTH_LIFE_POINTS_ZERO`): 57,6%. D-01 cercava circa metà.
- Durata in round: media 33,8, minimo 7, mediana 35, 90° percentile 41, massimo 75.

| Motivo di fine | Partite | Quota |
|---|---|---|
| `BOTH_LIFE_POINTS_ZERO` | 57 | 0,1% |
| `LIFE_POINTS_ZERO` | 22.975 | 57,4% |
| `BOTH_DECKS_EMPTY` | 2.274 | 5,7% |
| `NO_CREATURES_LEFT` | 14.691 | 36,7% |
| `ROUND_LIMIT` | 3 | 0,0% |

### Rarità e tipi

Punteggio del lato che ha una carta del gruppo nel mazzo e del lato che l'ha giocata.

| Gruppo | Carte | Partite giocata | Nel mazzo | Da giocata |
|---|---|---|---|---|
| Common | 28 | 344.181 | 49,9% | 49,3% |
| Uncommon | 26 | 316.266 | 50,0% | 49,3% |
| Rare | 16 | 153.588 | 50,1% | 49,2% |
| Mythic | 12 | 116.381 | 50,2% | 49,2% |
| Legendary | 8 | 90.638 | 50,6% | 49,8% |
| Mutant | 2 | 45.162 | 50,5% | 49,7% |
| Spell | 12 | 347.824 | 50,0% | 50,0% |
| Hidden | 8 | 119.555 | 50,0% | 51,0% |
| Terreno | 4 | 68.752 | 50,0% | 51,5% |
| Oggetto | 4 | 68.274 | 50,0% | 49,5% |

### Avvisi

Nessuno.

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
| Wound | 15.000 | 9.610 | 64 | 5.326 | 64,3% |
| Invitation | 15.000 | 9.490 | 66 | 5.444 | 63,5% |
| Archive | 15.000 | 5.334 | 72 | 9.594 | 35,8% |
| Dream | 15.000 | 5.432 | 66 | 9.502 | 36,4% |

### Scontri diretti

Punteggio della fazione della riga contro quella della colonna, nei due posti. Ogni fazione batte quella indicata tra parentesi: atteso 55-65% contro di lei, 45-55% negli scontri neutri.

| | Wound | Invitation | Archive | Dream |
|---|---|---|---|---|
| Wound (Dream) | — | 51,0% | 84,0% | 57,9% |
| Invitation (Archive) | 49,0% | — | 61,8% | 79,7% |
| Archive (Wound) | 16,0% | 38,2% | — | 53,2% |
| Dream (Invitation) | 42,1% | 20,3% | 46,9% | — |

### Partite

- Primo giocatore (sfidante): 49,4%.
- Finite per punti vita (`LIFE_POINTS_ZERO` e `BOTH_LIFE_POINTS_ZERO`): 33,6%. D-01 cercava circa metà.
- Durata in round: media 37,2, minimo 8, mediana 38, 90° percentile 43, massimo 75.

| Motivo di fine | Partite | Quota |
|---|---|---|
| `BOTH_LIFE_POINTS_ZERO` | 27 | 0,1% |
| `LIFE_POINTS_ZERO` | 13.418 | 33,5% |
| `BOTH_DECKS_EMPTY` | 3.123 | 7,8% |
| `NO_CREATURES_LEFT` | 23.429 | 58,6% |
| `ROUND_LIMIT` | 3 | 0,0% |

### Rarità e tipi

Punteggio del lato che ha una carta del gruppo nel mazzo e del lato che l'ha giocata.

| Gruppo | Carte | Partite giocata | Nel mazzo | Da giocata |
|---|---|---|---|---|
| Common | 28 | 361.196 | 50,0% | 49,3% |
| Uncommon | 26 | 333.069 | 49,5% | 48,8% |
| Rare | 16 | 163.693 | 50,2% | 49,3% |
| Mythic | 12 | 125.833 | 50,3% | 49,2% |
| Legendary | 8 | 98.120 | 50,9% | 49,9% |
| Mutant | 2 | 48.777 | 50,7% | 49,6% |
| Spell | 12 | 370.955 | 50,0% | 49,3% |
| Hidden | 8 | 129.157 | 50,0% | 50,5% |
| Terreno | 4 | 74.097 | 50,0% | 50,3% |
| Oggetto | 4 | 73.525 | 50,0% | 48,9% |

### Avvisi

- Scontro di vantaggio Archive contro Wound: 16,0% (atteso 55-65%).
- Scontro di vantaggio Dream contro Invitation: 20,3% (atteso 55-65%).
- Fazione Wound: 64,3% contro le altre (atteso 45-55%).
- Fazione Invitation: 63,5% contro le altre (atteso 45-55%).
- Fazione Archive: 35,8% contro le altre (atteso 45-55%).
- Fazione Dream: 36,4% contro le altre (atteso 45-55%).
- Scala delle rarità (nel mazzo): Uncommon (49,5%) non supera Common (50,0%).

### Carte fuori banda

Più di 10 punti dalla media del gruppo, con almeno 500 partite giocata.

| Carta | Gruppo | Partite giocata | Punteggio | Media del gruppo | Scarto |
|---|---|---|---|---|---|
| Fuoco fatuo | Common | 12.917 | 59,7% | 49,3% | +10,4 punti |
| Corvo di Cenere | Common | 12.868 | 59,4% | 49,3% | +10,0 punti |
| Le Falene della Miniera | Common | 12.960 | 59,8% | 49,3% | +10,5 punti |
| Wampus Cat | Uncommon | 12.907 | 59,7% | 48,8% | +10,9 punti |
| Hidebehind | Uncommon | 12.778 | 59,9% | 48,8% | +11,1 punti |
| Il Pastore Muto | Uncommon | 13.019 | 60,4% | 48,8% | +11,6 punti |
| Cervo Bianco | Uncommon | 12.657 | 59,7% | 48,8% | +10,9 punti |
| The Nameless Caller | Uncommon | 12.922 | 60,2% | 48,8% | +11,4 punti |
| Il Raccoglitore di Orme | Uncommon | 12.848 | 60,2% | 48,8% | +11,4 punti |
| Spearfinger | Rare | 10.057 | 59,5% | 49,3% | +10,2 punti |
| Il Predicatore sotto la Pioggia | Rare | 10.072 | 59,4% | 49,3% | +10,1 punti |
| The Bloodless Hound | Rare | 10.048 | 59,8% | 49,3% | +10,5 punti |
| The Thorned Matriarch | Mythic | 10.309 | 60,3% | 49,2% | +11,1 punti |
| Wendigo | Legendary | 12.134 | 60,3% | 49,9% | +10,4 punti |
| SCP-1762 | Common | 12.863 | 38,0% | 49,3% | -11,3 punti |
| Pupazzo da Archivio | Common | 12.781 | 38,3% | 49,3% | -11,0 punti |
| Tazza Eterna | Common | 12.751 | 39,1% | 49,3% | -10,2 punti |
| Il Timbro di Nessuno | Common | 12.714 | 36,6% | 49,3% | -12,7 punti |
| SCP-173 | Uncommon | 12.777 | 38,2% | 48,8% | -10,6 punti |
| SCP-053 | Uncommon | 12.784 | 38,6% | 48,8% | -10,2 punti |
| The Index Maw | Uncommon | 12.633 | 37,7% | 48,8% | -11,1 punti |
| SCP-682 | Rare | 10.290 | 39,0% | 49,3% | -10,2 punti |
| SCP-2521 | Rare | 10.229 | 38,7% | 49,3% | -10,6 punti |
| The Drowned Memory | Rare | 10.295 | 38,3% | 49,3% | -11,0 punti |
| SCP-2317 | Mythic | 10.494 | 37,7% | 49,2% | -11,4 punti |
| SCP-3000 | Mythic | 10.609 | 39,0% | 49,2% | -10,2 punti |
| Dullers | Common | 12.957 | 59,6% | 49,3% | +10,3 punti |
| Crawlers | Common | 13.116 | 59,8% | 49,3% | +10,4 punti |
| Cercatore di Moquette | Common | 13.073 | 59,3% | 49,3% | +10,0 punti |
| Ronzio Giallo | Common | 13.159 | 59,3% | 49,3% | +10,0 punti |
| L'Ascensore Senza Pulsanti | Common | 13.043 | 60,0% | 49,3% | +10,6 punti |
| L'Inquilino del Soffitto | Common | 13.097 | 60,3% | 49,3% | +10,9 punti |
| Howlers | Uncommon | 12.864 | 59,7% | 48,8% | +10,9 punti |
| Wretches | Uncommon | 12.814 | 59,5% | 48,8% | +10,7 punti |
| Cartellino Sbiadito | Uncommon | 12.993 | 59,5% | 48,8% | +10,7 punti |
| Sala d'Attesa Vivente | Uncommon | 13.005 | 60,8% | 48,8% | +12,0 punti |
| The Gnawing Witness | Uncommon | 12.801 | 59,2% | 48,8% | +10,4 punti |
| L'Addetto alle Uscite | Uncommon | 12.906 | 59,7% | 48,8% | +10,9 punti |
| Il Doppio di Servizio | Rare | 10.267 | 60,1% | 49,3% | +10,9 punti |
| Il Direttore dell'Edificio | Rare | 10.146 | 59,5% | 49,3% | +10,3 punti |
| L'Operatore della Stanza 0 | Rare | 10.185 | 60,8% | 49,3% | +11,6 punti |
| Hounds | Mythic | 10.525 | 59,3% | 49,2% | +10,1 punti |
| Deathmoths | Mythic | 10.411 | 60,0% | 49,2% | +10,8 punti |
| Il Custode delle Porte Incrociate | Mythic | 10.472 | 59,2% | 49,2% | +10,0 punti |
| Smiler | Legendary | 12.389 | 60,1% | 49,9% | +10,2 punti |
| The Hollow Wyrm | Legendary | 12.288 | 59,9% | 49,9% | +10,0 punti |
| Chochin-obake | Common | 12.753 | 39,1% | 49,3% | -10,2 punti |
| Tanuki del Tempio | Common | 12.825 | 39,3% | 49,3% | -10,0 punti |
| The Bone Lantern | Common | 12.731 | 37,7% | 49,3% | -11,7 punti |
| Rokurokubi | Uncommon | 12.844 | 37,9% | 48,8% | -10,9 punti |
| Nure-onna | Rare | 10.423 | 38,1% | 49,3% | -11,2 punti |
| The Unseen Matriarch | Mythic | 10.759 | 38,7% | 49,2% | -10,5 punti |
| Shuten-dōji | Legendary | 12.452 | 39,8% | 49,9% | -10,1 punti |
| Kitsune | Legendary | 12.294 | 39,8% | 49,9% | -10,2 punti |
| Echo Rift | Terreno | 18.361 | 60,6% | 50,3% | +10,3 punti |
| Hollow Clearing | Terreno | 18.175 | 61,3% | 50,3% | +10,9 punti |
| Watching Rift | Terreno | 18.761 | 39,7% | 50,3% | -10,6 punti |
| Devouring Abyss | Terreno | 18.800 | 40,3% | 50,3% | -10,1 punti |
| Badge di Uscita Negata | Oggetto | 18.355 | 59,3% | 48,9% | +10,4 punti |
| Bussola dei Sentieri Falsi | Oggetto | 17.755 | 59,3% | 48,9% | +10,4 punti |
| Fascicolo Sigillato 0 | Oggetto | 18.696 | 38,5% | 48,9% | -10,4 punti |

### Campione basso

Meno di 500 partite giocata: non si valutano.

Nessuna.

## 25 PV, +1 ATK contro +1 DEF, +2 DEF a Wound contro Archive e a Invitation contro Dream

### Fazioni

Contro le altre fazioni, senza le partite speculari.

| Fazione | Partite | Vittorie | Pareggi | Sconfitte | Punteggio |
|---|---|---|---|---|---|
| Wound | 15.000 | 9.699 | 59 | 5.242 | 64,9% |
| Invitation | 15.000 | 9.070 | 60 | 5.870 | 60,7% |
| Archive | 15.000 | 5.616 | 75 | 9.309 | 37,7% |
| Dream | 15.000 | 5.479 | 78 | 9.443 | 36,8% |

### Scontri diretti

Punteggio della fazione della riga contro quella della colonna, nei due posti. Ogni fazione batte quella indicata tra parentesi: atteso 55-65% contro di lei, 45-55% negli scontri neutri.

| | Wound | Invitation | Archive | Dream |
|---|---|---|---|---|
| Wound (Dream) | — | 53,5% | 79,6% | 61,4% |
| Invitation (Archive) | 46,5% | — | 62,1% | 73,4% |
| Archive (Wound) | 20,4% | 37,9% | — | 54,8% |
| Dream (Invitation) | 38,6% | 26,6% | 45,2% | — |

### Partite

- Primo giocatore (sfidante): 49,7%.
- Finite per punti vita (`LIFE_POINTS_ZERO` e `BOTH_LIFE_POINTS_ZERO`): 56,6%. D-01 cercava circa metà.
- Durata in round: media 34,3, minimo 6, mediana 36, 90° percentile 42, massimo 75.

| Motivo di fine | Partite | Quota |
|---|---|---|
| `BOTH_LIFE_POINTS_ZERO` | 50 | 0,1% |
| `LIFE_POINTS_ZERO` | 22.582 | 56,5% |
| `BOTH_DECKS_EMPTY` | 2.063 | 5,2% |
| `NO_CREATURES_LEFT` | 15.302 | 38,3% |
| `ROUND_LIMIT` | 3 | 0,0% |

### Rarità e tipi

Punteggio del lato che ha una carta del gruppo nel mazzo e del lato che l'ha giocata.

| Gruppo | Carte | Partite giocata | Nel mazzo | Da giocata |
|---|---|---|---|---|
| Common | 28 | 344.405 | 50,1% | 49,3% |
| Uncommon | 26 | 316.700 | 49,5% | 48,7% |
| Rare | 16 | 153.901 | 50,1% | 49,1% |
| Mythic | 12 | 116.692 | 50,3% | 49,1% |
| Legendary | 8 | 90.880 | 50,7% | 49,9% |
| Mutant | 2 | 45.187 | 50,8% | 49,7% |
| Spell | 12 | 348.188 | 50,0% | 49,7% |
| Hidden | 8 | 119.813 | 50,0% | 51,0% |
| Terreno | 4 | 68.872 | 50,0% | 51,2% |
| Oggetto | 4 | 68.275 | 50,0% | 49,3% |

### Avvisi

- Scontro di vantaggio Archive contro Wound: 20,4% (atteso 55-65%).
- Scontro di vantaggio Dream contro Invitation: 26,6% (atteso 55-65%).
- Fazione Wound: 64,9% contro le altre (atteso 45-55%).
- Fazione Invitation: 60,7% contro le altre (atteso 45-55%).
- Fazione Archive: 37,7% contro le altre (atteso 45-55%).
- Fazione Dream: 36,8% contro le altre (atteso 45-55%).
- Scala delle rarità (nel mazzo): Uncommon (49,5%) non supera Common (50,1%).

### Carte fuori banda

Più di 10 punti dalla media del gruppo, con almeno 500 partite giocata.

| Carta | Gruppo | Partite giocata | Punteggio | Media del gruppo | Scarto |
|---|---|---|---|---|---|
| Il Timbro di Nessuno | Common | 12.133 | 38,4% | 49,3% | -10,9 punti |
| SCP-2317 | Mythic | 9.721 | 38,8% | 49,1% | -10,3 punti |
| Dullers | Common | 12.282 | 59,9% | 49,3% | +10,7 punti |
| Crawlers | Common | 12.435 | 60,0% | 49,3% | +10,8 punti |
| Faceling | Common | 12.433 | 59,7% | 49,3% | +10,4 punti |
| Cercatore di Moquette | Common | 12.391 | 59,7% | 49,3% | +10,4 punti |
| Ronzio Giallo | Common | 12.465 | 59,8% | 49,3% | +10,5 punti |
| L'Ascensore Senza Pulsanti | Common | 12.397 | 60,2% | 49,3% | +10,9 punti |
| L'Inquilino del Soffitto | Common | 12.487 | 60,7% | 49,3% | +11,4 punti |
| Howlers | Uncommon | 12.198 | 60,0% | 48,7% | +11,3 punti |
| Wretches | Uncommon | 12.153 | 59,9% | 48,7% | +11,1 punti |
| Cartellino Sbiadito | Uncommon | 12.272 | 59,7% | 48,7% | +11,0 punti |
| Sala d'Attesa Vivente | Uncommon | 12.302 | 60,7% | 48,7% | +12,0 punti |
| The Gnawing Witness | Uncommon | 12.099 | 59,3% | 48,7% | +10,6 punti |
| L'Addetto alle Uscite | Uncommon | 12.198 | 60,0% | 48,7% | +11,3 punti |
| Il Doppio di Servizio | Rare | 9.618 | 60,5% | 49,1% | +11,3 punti |
| Il Direttore dell'Edificio | Rare | 9.525 | 59,5% | 49,1% | +10,4 punti |
| L'Operatore della Stanza 0 | Rare | 9.516 | 60,7% | 49,1% | +11,5 punti |
| Hounds | Mythic | 9.704 | 60,2% | 49,1% | +11,1 punti |
| Deathmoths | Mythic | 9.662 | 59,5% | 49,1% | +10,4 punti |
| Il Custode delle Porte Incrociate | Mythic | 9.654 | 59,1% | 49,1% | +10,0 punti |
| Smiler | Legendary | 11.512 | 60,7% | 49,9% | +10,8 punti |
| The Hollow Wyrm | Legendary | 11.254 | 60,4% | 49,9% | +10,5 punti |
| The Bone Lantern | Common | 12.201 | 38,5% | 49,3% | -10,8 punti |
| Rokurokubi | Uncommon | 12.306 | 38,1% | 48,7% | -10,6 punti |
| Nure-onna | Rare | 9.914 | 38,5% | 49,1% | -10,7 punti |
| The Unseen Matriarch | Mythic | 10.122 | 38,8% | 49,1% | -10,3 punti |
| Echo Rift | Terreno | 17.022 | 62,1% | 51,2% | +10,9 punti |
| Badge di Uscita Negata | Oggetto | 17.052 | 59,8% | 49,3% | +10,6 punti |

### Campione basso

Meno di 500 partite giocata: non si valutano.

Nessuna.

