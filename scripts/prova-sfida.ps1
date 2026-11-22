<#
.SYNOPSIS
	Prova completa di API ed engine sullo stack di Docker Compose (API-54, #124).

.DESCRIPTION
	Con lo stack avviato con i dati di prova (profilo dev, vedi reversky-api/docs/sfide, "Prova sullo stack completo"):
	  1. login di alice e bob e mazzi con Spell, Hidden e Terreni;
	  2. alice sfida bob, bob accetta: partita completata con il log round per round e le carte speciali;
	  3. engine spento: l'accettazione di un secondo invito risponde 503 ENGINE_UNAVAILABLE con battleId,
	     riacceso l'engine il retry completa la partita e un secondo retry non richiama l'engine;
	  4. uno scambio tra alice e bob: proposta, controproposta, accettazione.
	Lo script ferma e riavvia da solo il container dell'engine. Esce con codice 1 se un controllo fallisce.

.EXAMPLE
	$env:SPRING_PROFILES_ACTIVE = "dev"; docker compose up --build -d
	powershell -ExecutionPolicy Bypass -File scripts\prova-sfida.ps1
#>
param(
	[string]$BaseUrl = "http://localhost:8080/api/v1",
	[switch]$SkipTrade
)

# Niente ErrorActionPreference = "Stop": con PowerShell 5.1 trasformerebbe in errore l'avanzamento che docker compose
# scrive su stderr. Gli errori veri li segnalano Assert-Check e i throw.
$composeFile = Join-Path (Split-Path -Parent $PSScriptRoot) "compose.yaml"
$password = "Reversky2026"
$script:failures = 0

# HttpClient al posto di Invoke-RestMethod: non lancia eccezioni sui 4xx/5xx (servono il 503 e il suo corpo)
# e legge la risposta in UTF-8 anche con PowerShell 5.1, che altrimenti la decodifica come Latin-1.
Add-Type -AssemblyName System.Net.Http
$client = New-Object System.Net.Http.HttpClient
$client.Timeout = [TimeSpan]::FromSeconds(60)

function Invoke-Api {
	param([string]$Method, [string]$Path, [string]$Token, $Body)
	$request = New-Object System.Net.Http.HttpRequestMessage ([System.Net.Http.HttpMethod]::new($Method)), "$BaseUrl$Path"
	if ($Token) {
		$request.Headers.Authorization = New-Object System.Net.Http.Headers.AuthenticationHeaderValue "Bearer", $Token
	}
	if ($null -ne $Body) {
		$json = $Body | ConvertTo-Json -Depth 10 -Compress
		$request.Content = New-Object System.Net.Http.StringContent $json, ([Text.Encoding]::UTF8), "application/json"
	}
	$response = $client.SendAsync($request).GetAwaiter().GetResult()
	$text = [Text.Encoding]::UTF8.GetString($response.Content.ReadAsByteArrayAsync().GetAwaiter().GetResult())
	$parsed = $null
	if ($text) {
		$parsed = $text | ConvertFrom-Json
	}
	[pscustomobject]@{ Status = [int]$response.StatusCode; Body = $parsed }
}

function Assert-Check {
	param([bool]$Condition, [string]$Message)
	if ($Condition) {
		Write-Host "  OK      $Message" -ForegroundColor Green
	} else {
		Write-Host "  ERRORE  $Message" -ForegroundColor Red
		$script:failures++
	}
}

function Write-Step {
	param([string]$Title)
	Write-Host ""
	Write-Host $Title -ForegroundColor Cyan
}

function Get-Token {
	param([string]$Email)
	$login = Invoke-Api POST "/auth/login" -Body @{ email = $Email; password = $password }
	if ($login.Status -ne 200) {
		throw "Login di $Email fallito ($($login.Status)): lo stack e' partito con SPRING_PROFILES_ACTIVE=dev?"
	}
	$login.Body.accessToken
}

function Get-EngineLogCount {
	# Il log del container resta tra stop e start: si contano le righe prima e dopo un'azione.
	# "Simulazione completata" = una chiamata a POST /api/v1/simulations, "Started ..." = un avvio dell'engine.
	param([string]$Pattern)
	@(docker compose -f $composeFile logs engine 2>$null | Select-String $Pattern).Count
}

function Wait-EngineStart {
	param([int]$StartsBefore)
	for ($i = 0; $i -lt 60; $i++) {
		if ((Get-EngineLogCount "Started ReverskyEngineApplication") -gt $StartsBefore) {
			return
		}
		Start-Sleep -Seconds 2
	}
	throw "L'engine non e' ripartito entro 2 minuti"
}

function Get-EventCounts {
	param($Log)
	$counts = @{}
	foreach ($group in ($Log | Group-Object type)) {
		$counts[$group.Name] = $group.Count
	}
	$counts
}

function Get-FirstDeck {
	param([string]$Token)
	$deck = @((Invoke-Api GET "/decks" -Token $Token).Body)[0]
	(Invoke-Api GET "/decks/$($deck.id)" -Token $Token).Body
}

function Get-CopiesOf {
	param([string]$Token, [int]$FactionId, [int[]]$CardIds)
	$content = (Invoke-Api GET "/collection?factionId=$FactionId&type=CREATURE&size=100" -Token $Token).Body.content
	$copies = @{}
	foreach ($row in $content) {
		if ($CardIds -contains $row.card.id) {
			$copies[$row.card.id] = $row
		}
	}
	$copies
}

# ---- 1. Login e mazzi ----
Write-Step "1. Login di alice e bob, mazzi con le carte speciali"
$alice = Get-Token "alice@reversky.dev"
$bob = Get-Token "bob@reversky.dev"
$bobId = (Invoke-Api GET "/users/me" -Token $bob).Body.id
$aliceDeck = Get-FirstDeck $alice
$bobDeck = Get-FirstDeck $bob
foreach ($deck in @($aliceDeck, $bobDeck)) {
	$types = ($deck.cards | Group-Object { $_.card.type } | ForEach-Object {
		"$($_.Name)=$(($_.Group | Measure-Object quantity -Sum).Sum)"
	}) -join ", "
	Write-Host "  $($deck.name) (id $($deck.id)): $types"
	foreach ($type in "SPELL", "HIDDEN", "TERRAIN") {
		Assert-Check (@($deck.cards | Where-Object { $_.card.type -eq $type }).Count -gt 0) "$($deck.name) contiene $type"
	}
}

# ---- 2. Sfida completa ----
Write-Step "2. alice sfida bob, bob accetta: partita simulata dall'engine"
$invite = Invoke-Api POST "/battle-invites" -Token $alice -Body @{ opponentId = $bobId; deckId = $aliceDeck.id }
Assert-Check ($invite.Status -eq 201) "invito creato ($($invite.Status))"
$accept = Invoke-Api POST "/battle-invites/$($invite.Body.id)/accept" -Token $bob -Body @{ deckId = $bobDeck.id }
Assert-Check ($accept.Status -eq 200) "accettazione $($accept.Status)"
$battle = (Invoke-Api GET "/battles/$($accept.Body.id)" -Token $alice).Body
$events = Get-EventCounts $battle.log
Write-Host "  partita $($battle.id): $($battle.result), $($battle.endReason), $($battle.totalRounds) round, PV alice $($battle.me.finalLifePoints) - bob $($battle.opponent.finalLifePoints), seed $($battle.seed)"
Write-Host "  log: $($battle.log.Count) eventi, SPELL_RESOLVED=$($events['SPELL_RESOLVED']) HIDDEN_SET=$($events['HIDDEN_SET']) TERRAIN_PLACED=$($events['TERRAIN_PLACED']) ITEM_PLACED=$($events['ITEM_PLACED'])"
Assert-Check ($battle.completed -eq $true) "partita completata"
Assert-Check ($battle.log[0].type -eq "MATCH_STARTED" -and $battle.log[-1].type -eq "MATCH_ENDED") "log da MATCH_STARTED a MATCH_ENDED"
Assert-Check (($battle.log | Measure-Object round -Maximum).Maximum -eq $battle.totalRounds) "log round per round fino al round $($battle.totalRounds)"
foreach ($type in "SPELL_RESOLVED", "HIDDEN_SET", "TERRAIN_PLACED") {
	Assert-Check ($events[$type] -gt 0) "il log contiene $type"
}
$bobView = (Invoke-Api GET "/battles/$($battle.id)" -Token $bob).Body
$mirror = @{ WIN = "LOSS"; LOSS = "WIN"; DRAW = "DRAW" }
Assert-Check ($bobView.mySide -eq "OPPONENT" -and $bobView.outcome -eq $mirror[$battle.outcome]) "esito per alice $($battle.outcome), per bob $($bobView.outcome)"

# ---- 3. Engine spento e retry ----
Write-Step "3. Engine spento: 503 con battleId, poi retry dopo il riavvio"
docker compose -f $composeFile stop engine 2>$null | Out-Null
Write-Host "  engine fermato (l'accettazione puo' metterci fino a 10 secondi, il timeout del client)"
$invite = Invoke-Api POST "/battle-invites" -Token $alice -Body @{ opponentId = $bobId; deckId = $aliceDeck.id }
$accept = Invoke-Api POST "/battle-invites/$($invite.Body.id)/accept" -Token $bob -Body @{ deckId = $bobDeck.id }
$pendingId = $accept.Body.battleId
Assert-Check ($accept.Status -eq 503 -and $accept.Body.code -eq "ENGINE_UNAVAILABLE") "accettazione $($accept.Status) $($accept.Body.code)"
Assert-Check ([bool]$pendingId) "la risposta porta battleId $pendingId"
$pending = (Invoke-Api GET "/battles/$pendingId" -Token $alice).Body
Assert-Check ($pending.completed -eq $false -and $null -eq $pending.result) "la partita resta senza risultato (completed: false)"

$startsBefore = Get-EngineLogCount "Started ReverskyEngineApplication"
docker compose -f $composeFile start engine 2>$null | Out-Null
Wait-EngineStart $startsBefore
Write-Host "  engine riavviato"
$retry = Invoke-Api POST "/battles/$pendingId/retry" -Token $alice
Assert-Check ($retry.Status -eq 200 -and $retry.Body.completed -eq $true) "retry $($retry.Status): $($retry.Body.result), $($retry.Body.totalRounds) round, $($retry.Body.log.Count) eventi"
$simulationsBefore = Get-EngineLogCount "Simulazione completata"
$again = Invoke-Api POST "/battles/$pendingId/retry" -Token $alice
Assert-Check ($again.Status -eq 200 -and $again.Body.id -eq $pendingId -and $again.Body.totalRounds -eq $retry.Body.totalRounds) "secondo retry $($again.Status): stessa partita"
Assert-Check ((Get-EngineLogCount "Simulazione completata") -eq $simulationsBefore) "il secondo retry non richiama l'engine"

# ---- 4. Scambio ----
if (-not $SkipTrade) {
	Write-Step "4. Scambio: alice propone, bob contropropone, alice accetta"
	# Creature di una fazione che non c'e' in nessuno dei due mazzi, cosi' i mazzi restano validi.
	# alice cede 2 copie di $given e bob 1 di $asked: si scelgono tra le carte con abbastanza copie libere,
	# cosi' lo script si puo' rilanciare piu' volte sullo stesso database.
	$deckFactions = @($aliceDeck.cards + $bobDeck.cards | ForEach-Object { $_.card.faction.id } | Where-Object { $_ })
	$factionId = @((Invoke-Api GET "/factions").Body | Where-Object { $deckFactions -notcontains $_.id })[0].id
	$aliceFree = (Invoke-Api GET "/collection?factionId=$factionId&type=CREATURE&size=100" -Token $alice).Body.content |
		Where-Object { $_.usedInDecks -eq 0 -and $_.quantity - $_.reservedQuantity -ge 2 } | Sort-Object { $_.card.id }
	$bobFree = (Invoke-Api GET "/collection?factionId=$factionId&type=CREATURE&size=100" -Token $bob).Body.content |
		Where-Object { $_.usedInDecks -eq 0 -and $_.quantity - $_.reservedQuantity -ge 1 } | Sort-Object { $_.card.id }
	$given = @($aliceFree)[0].card.id
	$asked = @($bobFree | Where-Object { $_.card.id -ne $given -and $aliceFree.card.id -contains $_.card.id })[0].card.id
	if (-not $given -or -not $asked) {
		throw "Non ci sono piu' creature libere da scambiare: cancellare il volume del database (docker compose down -v)"
	}
	$aliceBefore = Get-CopiesOf $alice $factionId @($given, $asked)
	$bobBefore = Get-CopiesOf $bob $factionId @($given, $asked)
	Write-Host "  carte $given e $asked; prima: alice $($aliceBefore[$given].quantity)+$($aliceBefore[$asked].quantity), bob $($bobBefore[$given].quantity)+$($bobBefore[$asked].quantity)"

	$trade = Invoke-Api POST "/trades" -Token $alice -Body @{
		recipientId = $bobId; give = @(@{ cardId = $given; quantity = 1 }); receive = @(@{ cardId = $asked; quantity = 1 })
	}
	Assert-Check ($trade.Status -eq 201) "proposta $($trade.Status) $($trade.Body.status)"
	Assert-Check ((Get-CopiesOf $alice $factionId @($given))[$given].reservedQuantity -eq 1) "la copia offerta da alice e' riservata"
	$counter = Invoke-Api POST "/trades/$($trade.Body.id)/counter" -Token $bob -Body @{
		give = @(@{ cardId = $asked; quantity = 1 }); receive = @(@{ cardId = $given; quantity = 2 })
	}
	Assert-Check ($counter.Status -eq 200) "controproposta $($counter.Status) $($counter.Body.status)"
	$wrongTurn = Invoke-Api POST "/trades/$($trade.Body.id)/accept" -Token $bob
	Assert-Check ($wrongTurn.Status -eq 409) "bob non puo' accettare la sua controproposta ($($wrongTurn.Status) $($wrongTurn.Body.code))"
	$accepted = Invoke-Api POST "/trades/$($trade.Body.id)/accept" -Token $alice
	Assert-Check ($accepted.Status -eq 200 -and $accepted.Body.status -eq "ACCEPTED") "accettazione $($accepted.Status) $($accepted.Body.status)"

	$aliceAfter = Get-CopiesOf $alice $factionId @($given, $asked)
	$bobAfter = Get-CopiesOf $bob $factionId @($given, $asked)
	Write-Host "  dopo: alice $($aliceAfter[$given].quantity)+$($aliceAfter[$asked].quantity), bob $($bobAfter[$given].quantity)+$($bobAfter[$asked].quantity)"
	Assert-Check ($aliceAfter[$given].quantity -eq $aliceBefore[$given].quantity - 2 -and $aliceAfter[$asked].quantity -eq $aliceBefore[$asked].quantity + 1) "alice: -2 copie di $given, +1 di $asked"
	Assert-Check ($bobAfter[$given].quantity -eq $bobBefore[$given].quantity + 2 -and $bobAfter[$asked].quantity -eq $bobBefore[$asked].quantity - 1) "bob: +2 copie di $given, -1 di $asked"
	Assert-Check ($aliceAfter[$given].reservedQuantity -eq 0 -and $bobAfter[$asked].reservedQuantity -eq 0) "nessuna copia resta riservata"
	Assert-Check ((Get-FirstDeck $alice).violations.Count -eq 0 -and (Get-FirstDeck $bob).violations.Count -eq 0) "i due mazzi restano validi"
}

Write-Host ""
if ($script:failures -gt 0) {
	Write-Host "$($script:failures) controlli falliti" -ForegroundColor Red
	exit 1
}
Write-Host "Tutti i controlli superati" -ForegroundColor Green
