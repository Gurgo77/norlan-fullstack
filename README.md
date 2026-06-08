Norlan Client Portal - Full-Stack Application

Benvenuto nel Norlan Client Portal, una piattaforma enterprise full-stack progettata per la gestione anagrafica, il monitoraggio della formazione aziendale, la gestione della compliance/sicurezza sul lavoro (DPI) e la comunicazione in tempo reale tramite chat integrata.

L'applicazione segue un'architettura disaccoppiata composta da:
- Backend: Spring Boot (Java) - REST API Stateless securizzate tramite JWT e canali WebSocket STOMP.
- Frontend: SvelteKit (TypeScript) - Interfaccia reattiva moderna integrata con TailwindCSS.
- Database: MySQL.

---

Prerequisiti di Sistema

Prima di procedere con l'installazione e l'avvio locale, assicurarsi che il sistema soddisfi i seguenti requisiti:

- Java Development Kit (JDK): Versione 17 o superiore.
- Node.js: Versione 18.x o superiore (consigliata LTS) e **npm** (incluso in Node).
- MySQL Server: Versione 8.0 o superiore.

---

1. Configurazione della Base Dati (MySQL)

Il sistema è configurato per operare in modalità Production-Ready (`spring.jpa.hibernate.ddl-auto=validate`). Ciò significa che Hibernate verificherà solo lo schema ma non modificherà né creerà le tabelle in autonomia all'avvio. La struttura deve essere pre-popolata tramite il dump SQL fornito ("script.sql").

-Step per l'inizializzazione:

  1.  Accedi alla tua istanza locale di MySQL ed esegui il comando per creare il database dedicato:
      'CREATE DATABASE norlan_db CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;'
  2.  Importa il file di dump fornito (`script.sql`) per generare tutte le tabelle e i relativi vincoli.
  3.  Utenza Amministratore Preconfigurata: Il dump SQL include già le istruzioni necessarie per inserire il primo utente amministratore globale in         modo sicuro (tramite hashing crittografico forte BCrypt).
        -Email: `admin@norlan.it`,
        -Password di default: `ciao`.

---

2. Configurazione ed Esecuzione del Backend (Spring Boot)

-Configurazione delle Proprietà
Controlla o modifica il file di configurazione centrale situato in `src/main/resources/application.properties`:

- Connessione DB: Verifica che username (`root`) e password siano corretti per la tua istanza locale.
- Upload Limits: La gestione dei file blocca caricamenti superiori a 10MB.
- File System: All'avvio verrà creata automaticamente una directory `./uploads` nella root del server per memorizzare i file del portale in sicurezza.

-Comando di Avvio
Posizionati nella directory principale del backend ed esegui:
'./mvnw spring-boot:run'.

3. Configurazione ed Esecuzione del Frontend (SvelteKit)
Il frontend è un'applicazione single-page scritta in TypeScript. Comunica con il backend tramite chiamate HTTP (Axios) e si connette ai canali WebSocket per la messaggistica istantanea.

Passi per l'avvio:
Apri una nuova sessione del terminale e spostati all'interno della cartella del frontend:
'cd Norlan-Svelte-TypeScript-master'.

Installa tutti i pacchetti e le dipendenze di sviluppo:
'npm install'.

Avvia il server locale in modalità di sviluppo:
'npm run dev'.

Il frontend sarà accessibile dal tuo browser all'indirizzo: http://localhost:5173.

Nota CORS: Il backend integra già una policy CORS che abilita esplicitamente l'origine http://localhost:5173 a trasmettere credenziali di autenticazione.

4. Accesso al Sistema e Sicurezza (RBAC)
Il portale applica un controllo basato sui Ruoli (Role-Based Access Control) e gestisce le sessioni in modalità Stateless tramite token crittografici JWT.

Credenziali Primo Accesso:
Vai all'indirizzo http://localhost:5173/login e utilizza le credenziali caricate tramite lo script SQL:
'Email: admin@norlan.it',
'Password: ciao'.

Panoramica dei Permessi:

-Amministrazione Globale: Riservato al ruolo ADMIN. Consente la creazione ed eliminazione totale di record aziendali e profili dei lavoratori.
-Gestione Lavoratori e Azienda: Accessibile ad ADMIN ed AZIENDA. I dipendenti possono effettuare solo aggiornamenti controllati sul proprio profilo.
-Docenti e Corsi: I docenti possono gestire la formazione e consultare le anagrafiche dei propri iscritti, mentre i dipendenti possono consultare il materiale didattico e rilasciare feedback.
