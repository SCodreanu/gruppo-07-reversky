# Report del valore degli effetti

Generato da `EffectAblationSimulationTest` con `gradlew effectTest` (`ENG-42`, fase 2): non si modifica a mano, si rigenera. Gli stessi seed danno lo stesso report.

## Come è fatto

- Configurazione: 30 PV, +1 ATK contro +1 DEF, 20.000 partite di base (16 accoppiamenti × 1.250), seed 20261120 + accoppiamento × 1.250 + partita; mazzi casuali come in `report-bilanciamento.md`.
- Partite appaiate: per ogni carta si rigiocano, con gli stessi seed e mazzi, le partite in cui i due mazzi ne hanno un numero diverso di copie, con la carta senza effetto. Il valore per copia è la pendenza del guadagno sulla differenza di copie, cambiata di segno.
- Senza effetto: una creatura perde il codice; Spell, Hidden, Terreni e Oggetti hanno un effetto che non fa niente, quindi il valore è quello di tutta la carta (un Terreno perde anche il bonus base).
- Giudizio: le creature contro il prezzo del loro effetto (`ps_effetto`), le altre carte contro la media in V del loro tipo; troppo forte o troppo debole se lo scarto supera 2,0 punti di V e il 50% del riferimento.
- Dati di ogni carta: `report-effetti-carte.csv`.

## Taratura

+1 DEF su 28 creature senza effetto: 1 punto di V vale 1,45 punti per copia (da una creatura all'altra varia di 2,31 punti).

## Creature

| Carta | Fazione | Rarità | Partite | Valore per copia | Valore in V | PS | Riferimento | Scarto | Giudizio |
|---|---|---|---|---|---|---|---|---|---|
| The Null Witness | Archive | Mythic | 4.740 | 9,22 punti | 6,4 ± 0,4 | 5,2 | 5,2 | +1,2 | ok |
| Rokurokubi | Dream | Uncommon | 5.730 | 3,11 punti | 2,1 ± 0,2 | 1,2 | 1,2 | +0,9 | ok |
| Nure-onna | Dream | Rare | 4.856 | 1,89 punti | 1,3 ± 0,2 | 0,6 | 0,6 | +0,7 | ok |
| Kappa | Dream | Uncommon | 5.766 | 4,05 punti | 2,8 ± 0,2 | 2,1 | 2,1 | +0,7 | ok |
| Hidebehind | Invitation | Uncommon | 5.882 | 4,65 punti | 3,2 ± 0,2 | 2,6 | 2,6 | +0,6 | ok |
| The Index Maw | Archive | Uncommon | 5.763 | 3,55 punti | 2,5 ± 0,2 | 1,9 | 1,9 | +0,6 | ok |
| The Hollow Wyrm | Wound | Legendary | 5.529 | 9,79 punti | 6,8 ± 0,4 | 6,3 | 6,3 | +0,5 | ok |
| SCP-106 | Archive | Rare | 4.805 | 4,97 punti | 3,4 ± 0,3 | 3,1 | 3,1 | +0,3 | ok |
| The Gnawing Witness | Wound | Uncommon | 5.982 | 2,21 punti | 1,5 ± 0,2 | 1,3 | 1,3 | +0,2 | ok |
| Cartellino Sbiadito | Wound | Uncommon | 6.004 | 2,48 punti | 1,7 ± 0,2 | 1,5 | 1,5 | +0,2 | ok |
| Skin-Stealers | Wound | Rare | 4.934 | 4,86 punti | 3,4 ± 0,3 | 3,3 | 3,3 | +0,1 | ok |
| SCP-173 | Archive | Uncommon | 5.769 | 2,40 punti | 1,7 ± 0,2 | 1,6 | 1,6 | +0,1 | ok |
| The Threadbound Doll | Dream | Uncommon | 5.799 | 2,66 punti | 1,8 ± 0,2 | 1,8 | 1,8 | +0,0 | ok |
| Yuki-onna | Dream | Rare | 4.776 | 6,54 punti | 4,5 ± 0,3 | 4,5 | 4,5 | +0,0 | ok |
| Howlers | Wound | Uncommon | 5.950 | 3,20 punti | 2,2 ± 0,2 | 2,2 | 2,2 | +0,0 | ok |
| L'Addetto alle Uscite | Wound | Uncommon | 5.928 | 1,81 punti | 1,2 ± 0,2 | 1,5 | 1,5 | -0,3 | ok |
| The Face Collector | Dream | Rare | 4.730 | 6,68 punti | 4,6 ± 0,3 | 4,9 | 4,9 | -0,3 | ok |
| The Bloodless Hound | Invitation | Rare | 4.932 | 4,42 punti | 3,1 ± 0,2 | 3,4 | 3,4 | -0,3 | ok |
| Il Raccoglitore di Orme | Invitation | Uncommon | 5.994 | 2,23 punti | 1,5 ± 0,2 | 1,9 | 1,9 | -0,4 | ok |
| Hounds | Wound | Mythic | 4.882 | 2,35 punti | 1,6 ± 0,2 | 2,0 | 2,0 | -0,4 | ok |
| Il Collezionista di Nomi | Archive | Uncommon | 5.744 | 3,05 punti | 2,1 ± 0,2 | 2,6 | 2,6 | -0,5 | ok |
| The Masked Bloom | Dream | Uncommon | 5.821 | 2,79 punti | 1,9 ± 0,2 | 2,5 | 2,5 | -0,6 | ok |
| L'Ombra del Torii | Dream | Uncommon | 5.797 | 2,07 punti | 1,4 ± 0,2 | 2,0 | 2,0 | -0,6 | ok |
| Il Direttore dell'Edificio | Wound | Rare | 4.831 | 3,73 punti | 2,6 ± 0,3 | 3,2 | 3,2 | -0,6 | ok |
| Baku | Dream | Mythic | 4.812 | 5,31 punti | 3,7 ± 0,3 | 4,3 | 4,3 | -0,6 | ok |
| Il Pastore Muto | Invitation | Uncommon | 5.976 | 2,66 punti | 1,8 ± 0,2 | 2,5 | 2,5 | -0,7 | ok |
| SCP-2006 | Archive | Uncommon | 5.847 | 1,64 punti | 1,1 ± 0,1 | 1,8 | 1,8 | -0,7 | ok |
| SCP-682 | Archive | Rare | 4.691 | 4,10 punti | 2,8 ± 0,2 | 3,5 | 3,5 | -0,7 | ok |
| SCP-049 | Archive | Uncommon | 5.704 | 2,77 punti | 1,9 ± 0,2 | 2,6 | 2,6 | -0,7 | ok |
| Noppera-bō | Dream | Uncommon | 5.843 | 2,72 punti | 1,9 ± 0,2 | 2,6 | 2,6 | -0,7 | ok |
| Cervo Bianco | Invitation | Uncommon | 6.000 | 2,37 punti | 1,6 ± 0,2 | 2,4 | 2,4 | -0,8 | ok |
| The Drowned Memory | Archive | Rare | 4.817 | 6,70 punti | 4,6 ± 0,3 | 5,4 | 5,4 | -0,8 | ok |
| Nurikabe | Dream | Uncommon | 5.770 | 3,10 punti | 2,1 ± 0,2 | 3,0 | 3,0 | -0,9 | ok |
| SCP-001 | Archive | Legendary | 5.415 | 15,24 punti | 10,5 ± 0,4 | 11,4 | 11,4 | -0,9 | ok |
| La Regina dei Sentieri Perduti | Invitation | Legendary | 5.540 | -1,34 punti | -0,9 ± 0,4 | 0,0 | 0,0 | -0,9 | ok |
| Spearfinger | Invitation | Rare | 4.929 | 3,14 punti | 2,2 ± 0,2 | 3,1 | 3,1 | -0,9 | ok |
| Il Predicatore sotto la Pioggia | Invitation | Rare | 4.886 | 3,70 punti | 2,6 ± 0,2 | 3,5 | 3,5 | -0,9 | ok |
| Il Doppio di Servizio | Wound | Rare | 4.898 | 5,83 punti | 4,0 ± 0,3 | 5,0 | 5,0 | -1,0 | ok |
| L'Operatore della Stanza 0 | Wound | Rare | 4.859 | 6,17 punti | 4,3 ± 0,3 | 5,3 | 5,3 | -1,0 | ok |
| Wampus Cat | Invitation | Uncommon | 5.983 | 1,83 punti | 1,3 ± 0,2 | 2,3 | 2,3 | -1,0 | ok |
| SCP-2317 | Archive | Mythic | 4.837 | 5,15 punti | 3,6 ± 0,3 | 4,6 | 4,6 | -1,0 | ok |
| The Stitched Corridor | Wound/Archive | Mutant | 8.818 | 9,20 punti | 6,4 ± 0,3 | 7,4 | 7,4 | -1,0 | ok |
| Il Custode delle Porte Incrociate | Wound | Mythic | 4.943 | 4,54 punti | 3,1 ± 0,3 | 4,3 | 4,3 | -1,2 | ok |
| Jorōgumo | Dream | Rare | 4.735 | 5,98 punti | 4,1 ± 0,3 | 5,3 | 5,3 | -1,2 | ok |
| The Thorned Matriarch | Invitation | Mythic | 4.893 | 7,69 punti | 5,3 ± 0,4 | 6,5 | 6,5 | -1,2 | ok |
| Kitsune | Dream | Legendary | 5.507 | 8,59 punti | 5,9 ± 0,3 | 7,2 | 7,2 | -1,3 | ok |
| SCP-3000 | Archive | Mythic | 4.913 | 7,57 punti | 5,2 ± 0,3 | 6,5 | 6,5 | -1,3 | ok |
| Sala d'Attesa Vivente | Wound | Uncommon | 5.912 | 3,02 punti | 2,1 ± 0,2 | 3,6 | 3,6 | -1,5 | ok |
| SCP-053 | Archive | Uncommon | 5.845 | 2,06 punti | 1,4 ± 0,2 | 3,0 | 3,0 | -1,6 | ok |
| The Nameless Caller | Invitation | Uncommon | 5.959 | 2,91 punti | 2,0 ± 0,2 | 3,6 | 3,6 | -1,6 | ok |
| SCP-096 | Archive | Uncommon | 5.821 | 1,25 punti | 0,9 ± 0,1 | 2,5 | 2,5 | -1,6 | ok |
| Deathmoths | Wound | Mythic | 4.876 | 4,81 punti | 3,3 ± 0,3 | 5,0 | 5,0 | -1,7 | ok |
| Mothman | Invitation | Mythic | 4.946 | 5,70 punti | 3,9 ± 0,3 | 5,8 | 5,8 | -1,9 | ok |
| The Undying Blossom | Dream | Mythic | 4.748 | 8,85 punti | 6,1 ± 0,4 | 8,0 | 8,0 | -1,9 | ok |
| Wretches | Wound | Uncommon | 5.942 | 2,46 punti | 1,7 ± 0,2 | 3,6 | 3,6 | -1,9 | ok |
| Bell Witch | Invitation | Rare | 4.857 | 3,56 punti | 2,5 ± 0,3 | 4,5 | 4,5 | -2,0 | ok |
| The Unseen Matriarch | Dream | Mythic | 4.819 | 3,36 punti | 2,3 ± 0,3 | 4,5 | 4,5 | -2,2 | ok |
| SCP-2521 | Archive | Rare | 4.857 | 3,36 punti | 2,3 ± 0,2 | 4,5 | 4,5 | -2,2 | ok |
| Bigfoot | Invitation | Mythic | 4.888 | 6,33 punti | 4,4 ± 0,3 | 6,6 | 6,6 | -2,2 | ok |
| Wendigo | Invitation | Legendary | 5.579 | 7,19 punti | 5,0 ± 0,3 | 7,4 | 7,4 | -2,4 | ok |
| The Rootbound Doppelganger | Invitation/Dream | Mutant | 8.892 | 7,15 punti | 4,9 ± 0,3 | 7,9 | 7,9 | -3,0 | ok |
| SCP-3999 | Archive | Legendary | 5.377 | 8,02 punti | 5,5 ± 0,3 | 8,6 | 8,6 | -3,1 | ok |
| Shuten-dōji | Dream | Legendary | 5.506 | 8,55 punti | 5,9 ± 0,3 | 10,1 | 10,1 | -4,2 | ok |
| Smiler | Wound | Legendary | 5.633 | 7,75 punti | 5,4 ± 0,3 | 10,1 | 10,1 | -4,7 | ok |

## Spell

| Carta | Fazione | Rarità | Partite | Valore per copia | Valore in V | PS | Riferimento | Scarto | Giudizio |
|---|---|---|---|---|---|---|---|---|---|
| Richiamo della Crepa |  | Uncommon | 10.833 | 7,13 punti | 4,9 ± 0,2 | 4,9 | 4,5 | +0,4 | ok |
| Patto del Registro |  | Uncommon | 10.852 | 7,02 punti | 4,9 ± 0,2 | 4,7 | 4,5 | +0,3 | ok |
| Velo degli Ancorati |  | Uncommon | 10.818 | 7,02 punti | 4,9 ± 0,2 | 5,1 | 4,5 | +0,3 | ok |
| Frattura della Pelle |  | Uncommon | 10.932 | 6,93 punti | 4,8 ± 0,2 | 4,9 | 4,5 | +0,3 | ok |
| Morso del Rovescio |  | Uncommon | 10.840 | 6,76 punti | 4,7 ± 0,2 | 4,9 | 4,5 | +0,1 | ok |
| SCP-109 |  | Uncommon | 10.821 | 6,69 punti | 4,6 ± 0,2 | 4,8 | 4,5 | +0,1 | ok |
| Fusione del Rovescio |  | Uncommon | 10.838 | 6,46 punti | 4,5 ± 0,2 | 4,5 | 4,5 | -0,1 | ok |
| Armatura della Pioggia |  | Uncommon | 10.850 | 6,38 punti | 4,4 ± 0,2 | 4,2 | 4,5 | -0,1 | ok |
| Dissoluzione |  | Uncommon | 10.793 | 6,35 punti | 4,4 ± 0,2 | 4,7 | 4,5 | -0,1 | ok |
| SCP-458 |  | Uncommon | 10.921 | 6,19 punti | 4,3 ± 0,2 | 4,0 | 4,5 | -0,3 | ok |
| Eco dei Dimenticati |  | Uncommon | 10.847 | 5,93 punti | 4,1 ± 0,2 | 4,5 | 4,5 | -0,4 | ok |
| SCP-500 |  | Uncommon | 10.721 | 5,81 punti | 4,0 ± 0,2 | 3,5 | 4,5 | -0,5 | ok |

## Hidden

| Carta | Fazione | Rarità | Partite | Valore per copia | Valore in V | PS | Riferimento | Scarto | Giudizio |
|---|---|---|---|---|---|---|---|---|---|
| Unseen Exit |  | Uncommon | 8.594 | 6,62 punti | 4,6 ± 0,3 | 3,9 | 3,3 | +1,2 | ok |
| Last Sanctuary |  | Uncommon | 8.387 | 5,96 punti | 4,1 ± 0,3 | 3,7 | 3,3 | +0,8 | ok |
| Hostile Presence |  | Uncommon | 6.042 | 5,61 punti | 3,9 ± 0,3 | 3,2 | 3,3 | +0,5 | ok |
| Static Interference |  | Uncommon | 6.139 | 4,82 punti | 3,3 ± 0,3 | 3,5 | 3,3 | -0,0 | ok |
| Survival Instinct |  | Uncommon | 8.527 | 4,41 punti | 3,1 ± 0,2 | 3,2 | 3,3 | -0,3 | ok |
| Echo Strike |  | Uncommon | 6.087 | 3,90 punti | 2,7 ± 0,3 | 3,1 | 3,3 | -0,6 | ok |
| Breaking Point |  | Uncommon | 6.132 | 3,70 punti | 2,6 ± 0,3 | 3,8 | 3,3 | -0,8 | ok |
| Remnants of Reality |  | Uncommon | 8.554 | 3,65 punti | 2,5 ± 0,2 | 2,5 | 3,3 | -0,8 | ok |

## Terreni

| Carta | Fazione | Rarità | Partite | Valore per copia | Valore in V | PS | Riferimento | Scarto | Giudizio |
|---|---|---|---|---|---|---|---|---|---|
| Devouring Abyss | Dream | Rare | 7.500 | 23,85 punti | 16,5 ± 0,4 | 5,3 | 14,0 | +2,5 | ok |
| Hollow Clearing | Invitation | Rare | 7.500 | 19,97 punti | 13,8 ± 0,4 | 3,8 | 14,0 | -0,2 | ok |
| Watching Rift | Archive | Rare | 7.500 | 19,34 punti | 13,4 ± 0,3 | 4,9 | 14,0 | -0,7 | ok |
| Echo Rift | Wound | Rare | 7.500 | 17,97 punti | 12,4 ± 0,4 | 4,2 | 14,0 | -1,6 | ok |

## Oggetti

| Carta | Fazione | Rarità | Partite | Valore per copia | Valore in V | PS | Riferimento | Scarto | Giudizio |
|---|---|---|---|---|---|---|---|---|---|
| Badge di Uscita Negata | Wound | Rare | 7.500 | 7,14 punti | 4,9 ± 0,3 | 5,3 | 3,3 | +1,7 | ok |
| Bussola dei Sentieri Falsi | Invitation | Rare | 7.500 | 4,32 punti | 3,0 ± 0,2 | 3,3 | 3,3 | -0,3 | ok |
| Fascicolo Sigillato 0 | Archive | Rare | 7.500 | 3,95 punti | 2,7 ± 0,2 | 2,9 | 3,3 | -0,5 | ok |
| Campanello del Torii | Dream | Rare | 7.500 | 3,41 punti | 2,4 ± 0,2 | 2,7 | 3,3 | -0,9 | ok |

## Avvisi

Nessuno.
