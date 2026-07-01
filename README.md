[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](LICENSE)

<img src="doc/img/SigilloAteneoTestoColori.svg" width="250px;" alt="Insubria Logo">

# 🎬 CineMax

**Progetto universitario per l'esame di Laboratorio Interdisciplinare B – Università degli Studi dell'Insubria (2026)**

Sistema di gestione per piccoli cinema monosala da 200 posti. Permette la gestione del palinsesto cinematografico e delle prenotazioni tramite 
interfaccia grafica JavaFX, scritto in Java 25 con architettura distribuita Client/Server RMI e persistenza su database PostgreSQL.

Manuale Tecnico, Manuale Utente e JavaDoc disponibili in `/doc`.

---

## 👥 Autori

- **Alt Niccolò Jacopo 762605 VA** - ruolo
- **Gerti Alessia 762405 VA** - ruolo
- **Soldo Mateo 760762 VA** - ruolo
- **Vignati Davide 761134 VA** - ruolo

---

## 📦 Dipendenze

Il progetto utilizza **Maven** per la compilazione. Le dipendenze principali sono:
- **JavaFX 25.0.1** - interfaccia grafica client
- **PostgreSQL JDBC 42.7.5** - accesso al database
- **Java RMI** - comunicazione distribuita client/server

---

## ⚙️ Requisiti

- **Java 25** o superiore
- **Maven 3.8+**
- **PostgreSQL 17+**

---

## 🗄️ Setup Dabatase

1. Scarica, se non la possiedi, un’istanza **PostgreSQL** dal sito ufficiale https://www.postgresql.org/.
2. Configura le credenziali di accesso.
3. Esegui il jar del DBcreator che crea e popola il database automaticamente:

```bash
java -jar bin/DBcreator.jar
```

4. Il programma chiederà host, porta, username e password — premi invio per usare i valori di default.

---

## 🔨 Build

```bash
mvn clean package
```

I JAR eseguibili vengono generati nelle rispettive cartelle `target/` di ogni modulo. (ha senso lasciarlo?)

---

## ▶️ Avvio

**1. Avvia il Server**

```bash
java -jar server/target/serverCM.jar
```

Il server chiederà le credenziali PostgreSQL. Una volta connesso al database, avvia il registry RMI sulla porta **1099**.

**2. Avvia il Client**

```bash
java -jar client/target/App.jar
```

Il client chiederà l'host e la porta del server RMI. Assicurarsi che il server sia già avviato prima di lanciare il client.

---

## 📁 Struttura del Progetto

è ingombrante non so se metterla

---

## 📌 Note Tecniche

- **Architettura**: Client/Server distribuita tramite Java RMI
- **Database**: PostgreSQL, schema in Terza Forma Normale (3NF)
- **Password**: cifrate con SHA-256 (UTF-8) lato client prima dell'invio
- **Concorrenza**: gestita con `SELECT FOR UPDATE` in `creaPrenotazione` per evitare race condition tra client concorrenti
- **Formato data/ora**: `yyyy-MM-dd HH:mm:ss`
- **Capienza sala**: 200 posti fissi
- **Sovrapposizione proiezioni**: il sistema impedisce l'aggiunta di proiezioni che si sovrappongono temporalmente
  
---

## 🔐 Credenziali di Test

Il sistema include utenti di test predefiniti:

| Ruolo | Username | Password |
|-------|----------|----------|
| Proiezionista | `proiezionista1` | `1234` |
| Proiezionista | `proiezionista2` | `1234` |
| Bigliettaio | `bigliettaio1` | `1234` |
| Bigliettaio | `bigliettaio2` | `1234` |
| Bigliettaio | `bigliettaio3` | `1234` |
| Bigliettaio | `bigliettaio4` | `1234` |
| Bigliettaio | `bigliettaio5` | `1234` |
| Cliente (Test) | `test` | `test` |

**Nota:** gli utenti ospiti (Guest) non richiedono autenticazione.

---
## 📚 Documentazione

- **Manuale Utente** (`/doc/Manuale_Utente.pdf`): guida per l'utilizzo dell'applicazione
- **Manuale Tecnico** (`/doc/Manuale_Tecnico.pdf`): architettura, scelte progettuali, diagrammi UML e documentazione DB (ER, schema relazionale)
- **JavaDoc API** (`/doc/javadoc/index.html`): documentazione completa delle classi e metodi

---

## 📄 Licenza

Questo progetto è rilasciato sotto licenza **MIT**. Vedi il file `LICENSE` per i dettagli.
