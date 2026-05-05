<script lang="ts">
  import { fade } from 'svelte/transition';
  </script>
  
  <style>
    @keyframes slideInFromBottom {
      from {
        transform: translateY(100%);
        opacity: 0;
      }
      to {
        transform: translateY(0);
        opacity: 1;
      }
    }
  
    .slide-in-bottom {
      animation: slideInFromBottom 0.8s ease-out forwards;
    }
  </style>
  
  <div class="min-h-screen pt-20 bg-white" in:fade out:fade>
    <section class="relative h-[70vh] flex items-center justify-center overflow-hidden">
      <div class="absolute inset-0 w-full h-full blur-sm">
        <img
          src="/NorLan.jpg"
          alt="Background"
          class="w-full h-full object-cover"
        />
      </div>
      <div class="absolute inset-0 bg-gradient-to-b from-black/30 to-black/50"></div>
      <div class="container mx-auto px-4 relative z-10 text-center slide-in-bottom">
        <h1 class="text-4xl md:text-5xl font-bold text-white">
          Informativa sulla Privacy
        </h1>
        <p class="text-xl text-white/90 mt-4 max-w-3xl mx-auto">
          La tua privacy è importante per noi
        </p>
      </div>
    </section>
  
    <section class="py-20 bg-gray-50 text-center px-4">
      <div class="max-w-4xl mx-auto text-left">
        <h2 class="text-2xl md:text-3xl font-bold text-[#1B4B6B] mb-4">Informativa sulla Privacy</h2>
        <p class="text-gray-600 mb-4">
          NORLAN, nella persona della Dott.ssa Rosa BARBANO DI MAGGIO, tratta i dati personali da Lei forniti mediante compilazione del FORM “CONTATTI” presente sul sito internet www.norlan.it.
        </p>
        <p class="text-gray-600 mb-4">
          In particolare, vengono richiesti, in applicazione del principio di minimizzazione, solo i dati necessari per dare riscontro alla Sua richiesta, ovvero: Nome e Cognome, indirizzo di posta elettronica ed eventuali dati ulteriori da Lei inseriti nella sezione “messaggio”.
        </p>
  
        <div class="mt-8">
          <h3 class="text-xl font-bold text-[#1B4B6B] mb-2">Finalità e Base Giuridica del Trattamento</h3>
          <p class="text-gray-600 mb-4">
            NORLAN, nella persona della Dott.ssa Rosa BARBANO DI MAGGIO, tratta i dati acquisiti mediante compilazione del FORM “CONTATTI” presente sul sito internet www.norlan.it al solo scopo di dare riscontro alla Sua richiesta. In particolare, il trattamento consiste nelle seguenti attività:
          </p>
          <ul class="list-disc list-inside text-gray-600 mb-4">
            <li>Raccolta (acquisizione dal FORM con reindirizzamento su mail dedicata)</li>
            <li>Utilizzo per la sola finalità di dare riscontro</li>
            <li>Cancellazione ad esaurimento della finalità del trattamento</li>
            <li>Cancellazione dei dati particolari all’atto dell’acquisizione</li>
          </ul>
          <p class="text-gray-600 mb-4">
            Le basi giuridiche del trattamento dei dati “comuni” sono:
          </p>
          <ul class="list-disc list-inside text-gray-600 mb-4">
            <li>L’articolo 6, paragrafo 1, lettera b) del Regolamento UE 2016/679 per i trattamenti di cui alle precedenti lettere a) e b).</li>
          </ul>
        </div>
  
        <div class="mt-8">
          <h3 class="text-xl font-bold text-[#1B4B6B] mb-2">Tipologia e Fonte dei Dati Personali Trattati</h3>
          <p class="text-gray-600 mb-4">
            I dati personali oggetto di trattamento sono quelli da Lei forniti e sono dati anagrafici e di contatto.
          </p>
        </div>
  
        <div class="mt-8">
          <h3 class="text-xl font-bold text-[#1B4B6B] mb-2">Principi del Trattamento</h3>
          <p class="text-gray-600 mb-4">
            Il trattamento sarà improntato ai principi di CORRETTEZZA, LICEITÀ, TRASPARENZA, TUTELA DELLA RISERVATEZZA, TUTELA DEI DIRITTI DELL'INTERESSATO.
          </p>
        </div>
  
        <div class="mt-8">
          <h3 class="text-xl font-bold text-[#1B4B6B] mb-2">Modalità e Luogo del Trattamento</h3>
          <p class="text-gray-600 mb-4">
            Il trattamento dei dati viene effettuato presso la sede legale del Titolare del Trattamento. I dati possono essere trattati, per conto della Società sia da soggetti interni (autorizzati al trattamento dei dati ai sensi dell’articolo 29 del Regolamento UE 2016/679 e specificamente informati ed addestrati) che esterni (formalmente designati come Responsabili Esterni del Trattamento ai sensi dell’articolo 28 del Regolamento UE 2016/679). Il trattamento può essere effettuato sia con modalità cartacee che elettroniche nel rispetto delle misure di sicurezza predisposte da NORLAN.
          </p>
        </div>
  
        <div class="mt-8">
          <h3 class="text-xl font-bold text-[#1B4B6B] mb-2">Natura Obbligatoria o Facoltativa del Conferimento</h3>
          <p class="text-gray-600 mb-4">
            Il conferimento dei dati è facoltativo: in mancanza il Titolare del Trattamento non potrà dare riscontro alla richiesta inserita nel FORM.
          </p>
        </div>
  
        <div class="mt-8">
          <h3 class="text-xl font-bold text-[#1B4B6B] mb-2">Comunicazione dei Dati</h3>
          <p class="text-gray-600 mb-4">
            I dati trattati non saranno oggetto di divulgazione e di diffusione a terzi.
          </p>
        </div>
  
        <div class="mt-8">
          <h3 class="text-xl font-bold text-[#1B4B6B] mb-2">Trasferimento dei Dati verso un Paese Terzo e/o un’Organizzazione Internazionale e Garanzie</h3>
          <p class="text-gray-600 mb-4">
            I dati di natura personale forniti non saranno trasferiti all’estero in Paesi extra SEE. In ogni caso, qualora detto trasferimento dovesse rendersi necessario per riscontrare alla sua richiesta, il trasferimento sarà effettuato sulla base delle disposizioni normative vigenti.
          </p>
        </div>
  
        <div class="mt-8">
          <h3 class="text-xl font-bold text-[#1B4B6B] mb-2">Conservazione dei Dati</h3>
          <p class="text-gray-600 mb-4">
            I dati conferiti verranno conservati fino ad esaurimento della finalità del trattamento, in particolare a conclusione dell’interlocuzione necessaria a dare riscontro alla Sua richiesta.
          </p>
        </div>
  
        <div class="mt-8">
          <h3 class="text-xl font-bold text-[#1B4B6B] mb-2">Processi Automatizzati</h3>
          <p class="text-gray-600 mb-4">
            Il Titolare del trattamento non intende utilizzare processi decisionali automatizzati, ivi compresa la profilazione di cui all’articolo 22, paragrafi 1 e 4, del Regolamento Europeo.
          </p>
        </div>
  
        <div class="mt-8">
          <h3 class="text-xl font-bold text-[#1B4B6B] mb-2">Diritti degli Interessati</h3>
          <p class="text-gray-600 mb-4">
            Nella Sua qualità di INTERESSATO, ha i Diritti di cui all'articolo 15 e seguenti del Regolamento UE 2016/679, precisamente i diritti di:
          </p>
          <ul class="list-disc list-inside text-gray-600 mb-4">
            <li>Accedere ai Suoi dati personali (articolo 15 del Regolamento UE n. 2016/679)</li>
            <li>Ottenere la conferma dell'esistenza o meno di dati personali che La riguardano, anche se non ancora registrati e la loro comunicazione in forma intelligibile</li>
            <li>Ottenere l'indicazione: dell'origine dei dati personali, delle finalità e modalità del trattamento, della logica applicata in caso di trattamento effettuato con l'ausilio di strumenti elettronici, degli estremi identificativi del Titolare e dei Responsabili, dei soggetti o delle categorie di soggetti ai quali i dati personali possono essere comunicati o che possono venirne a conoscenza in qualità di rappresentante designato nel territorio dello Stato, di responsabili o incaricati</li>
            <li>Ottenere: aggiornamento, rettifica ovvero (se vi ha interesse) integrazione dei dati (articolo 16 del Regolamento UE n. 2016/679), cancellazione, trasformazione in forma anonima o blocco dei dati trattati in violazione di legge, compresi quelli di cui non è necessaria la conservazione in relazione agli scopi per i quali i dati sono stati raccolti e successivamente trattati, c.d. "Diritto all'Oblio" (articolo 17 del Regolamento UE n. 2016/679)</li>
            <li>Chiedere la limitazione dei trattamento dei Suoi dati personali (articolo 18 del Regolamento UE n. 2016/679)</li>
            <li>Attestazione che le operazioni di cui alle lettere a) e b) sono state portate a conoscenza, anche per quanto riguarda il loro contenuto, di coloro ai quali i dati sono stati comunicati o diffusi, eccettuato il caso in cui tale adempimento si rivela impossibile o comporta un impiego di mezzi manifestatamente sproporzionato rispetto al diritto tutelato</li>
            <li>Opporsi, in tutto o in parte, per motivi legittimi al trattamento dei dati personali che La riguardano, ancorché pertinenti allo scopo della raccolta (articolo 21 del Regolamento UE n. 2016/679)</li>
            <li>Di "portabilità" dei Suoi dati personali (articolo 20 del Regolamento UE n. 2016/679), purché rientranti nell'ambito dei dati per i quali è consentita la portabilità</li>
            <li>Ha altresì il diritto di proporre Reclamo al Garante per la Protezione dei Dati Personali italiano.</li>
          </ul>
        </div>
  
        <div class="mt-8">
          <h3 class="text-xl font-bold text-[#1B4B6B] mb-2">Titolare del Trattamento</h3>
          <p class="text-gray-600 mb-4">
            Dott.ssa Rosa BARBANO DI MAGGIO
          </p>
          <p class="text-gray-600 mb-4">
            e-mail: barbanorosa@libero.it - PEC: barbanodimaggiorosa@pec.it
          </p>
          <p class="text-gray-600 mb-4">
            San Giovanni Rotondo, 18/03/2025
          </p>
          <p class="text-gray-600 mb-4">
            Il Titolare del Trattamento<br>
            Dott.ssa Rosa BARBANO DI MAGGIO
          </p>
        </div>
      </div>
    </section>
  </div>
  