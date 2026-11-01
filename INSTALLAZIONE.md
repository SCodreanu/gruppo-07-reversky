# Guida all'installazione

Cosa installare sul proprio PC (Windows) per lavorare su Reversky.

| Programma | Quando serve | Download |
|---|---|---|
| **Docker Desktop** | sempre: avvio del backend con Docker e test dell'API | [Installare Docker Desktop su Windows](https://docs.docker.com/desktop/setup/install/windows-install/) |
| **Java 25 — Eclipse Temurin** | per avviare i moduli senza Docker (Gradle, IDE, test) | [Download Temurin 25](https://adoptium.net/temurin/releases/?version=25) |

---

## 1. Docker Desktop

1. Apri la [pagina di installazione](https://docs.docker.com/desktop/setup/install/windows-install/) e scarica **Docker Desktop for Windows - x86_64**.
2. Avvia `Docker Desktop Installer.exe` e lascia selezionata l'opzione **Use WSL 2** (consigliata).
3. Se l'installer dice che WSL 2 manca o va aggiornato, apri **PowerShell come amministratore** ed esegui:
   ```powershell
   wsl --install
   ```
   oppure, se è già installato:
   ```powershell
   wsl --update
   ```
4. Riavvia il PC quando richiesto.
5. Apri **Docker Desktop** e aspetta che in basso a sinistra compaia lo stato *Engine running*.

Verifica, da un nuovo terminale:

```bash
docker --version
docker compose version
```

> Docker Desktop deve essere **aperto** ogni volta che usi il progetto: gli script e i test dell'API lo controllano.

---

## 2. Java 25 (Eclipse Temurin)

1. Apri la [pagina di download di Temurin 25](https://adoptium.net/temurin/releases/?version=25).
2. Scegli **Windows**, **x64**, **JDK** e scarica l'installer **`.msi`**.
3. Durante l'installazione, nella schermata delle funzionalità, attiva:
   - **Add to PATH**
   - **Set JAVA_HOME variable**
4. Concludi l'installazione e **chiudi e riapri** i terminali (e l'IDE), così leggono le nuove variabili.

Verifica:

```bash
java -version      # deve indicare 25 e "Temurin"
echo %JAVA_HOME%   # da cmd; in PowerShell: $env:JAVA_HOME
```

### Configurare l'IDE

- **IntelliJ IDEA:** *File → Project Structure → Project → SDK* → scegli il JDK 25 di Temurin (se non compare: *Add SDK → JDK* e indica la cartella di installazione, di solito `C:\Program Files\Eclipse Adoptium\jdk-25…`).
- **VS Code:** installa l'*Extension Pack for Java*; il JDK viene letto da `JAVA_HOME`.

Il progetto usa il **Gradle wrapper** (`gradlew` / `gradlew.bat`): Gradle **non** va installato a parte, lo scarica il wrapper al primo avvio.

---

## Problemi comuni

| Problema | Soluzione |
|---|---|
| Docker Desktop non parte e parla di *virtualization* | la virtualizzazione è disattivata nel BIOS/UEFI: attivala (Intel VT-x / AMD-V) e riavvia |
| Docker chiede di aggiornare WSL | `wsl --update` da PowerShell come amministratore |
| `java -version` mostra una versione diversa da 25 | un'altra Java viene prima nel `PATH`: in *Variabili d'ambiente* sposta in alto la cartella `bin` di Temurin 25, oppure disinstalla le versioni vecchie |
| `'java' non è riconosciuto come comando` | l'opzione *Add to PATH* non era attiva: reinstalla Temurin attivandola, poi riapri il terminale |
| Gradle dice *Toolchain … 25 not found* | `JAVA_HOME` non punta al JDK 25: correggilo e riapri terminale e IDE |

Quando tutto è installato, torna al [README](README.md#come-avviare-il-backend) per avviare il backend.
