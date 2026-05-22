<script lang="ts">
  import { fade } from 'svelte/transition';
  import { Shield, BookOpen, Award, Clock, Users } from 'lucide-svelte';
  import DocenteCard from '$lib/Components/Features/Formazione/DocenteCard.svelte';
  import CorsoCard from '$lib/Components/Features/Formazione/CorsoCard.svelte';
  /*
  Componente dedicato alla gestione dell'offerta formativa.
  Gestisce l'esposizione dinamica del catalogo corsi e del corpo docenti,
  implementando la navigazione tra le sezioni tramite stato reattivo e selezione condizionale.
  */

  // Interfacce di tipizzazione per la strutturazione rigorosa dei dati formativi e delle schede docente
  interface Course {
    title: string;
    reference: string;
    content: { level: string; description: string; risk: string; }[];
  }

  interface DocenteCardData {
    nome: string;
    qualifica: string;
    titoli: string[];
  }

  let showDocents = $state(false);
  let courseDetails = $state<Course | null>(null);

  // Repository locale dei corsi erogati (con riferimenti normativi) e del team di docenti accreditati
  let courses: Course[] = [
    {
      title: 'Datore di lavoro/RSPP (iniziale)',
      reference: 'ACCORDO STATO REGIONI 21/12/2011 Rep. 223/CSR',
      content: [
        { level: 'Rischio basso (16 ore)', description: 'COMMERCIO INGROSSO E DETTAGLIO, ATTIVITÀ ARTIGIANALI, HOTEL RISTORANTI, ASSICURAZIONI, IMMOBILIARI, INFORMATICA, ASSOCIAZIONI RICREATIVE, CULTURALI E SPORTIVE, SERVIZI DOMESTICI, ORGANIZZAZIONI EXTRATERRITORIALI', risk: 'low' },
        { level: 'Rischio medio (32 ore)', description: 'AGRICOLTURA, PESCA, MAGAZZINAGGIO, TRASPORTI, COMUNICAZIONE, ASSISTENZA SOCIALE NON RESIDENZIALE, PUBBLICA AMMINISTRAZIONE, ISTRUZIONE', risk: 'medium' },
        { level: 'Rischio elevato (48 ore)', description: 'ESTRAZIONI MINERARIE, COSTRUZIONI, INDUSTRIE (ALIMENTARI, TESSILI, ABBIGLIAMENTO, CONCIARIE, LEGNO, CARTA, EDITORIA, STAMPA), MINERALI, PRODUZIONE METALLI, FABBRICAZIONE MACCHINE, AUTOVEICOLI, MOBILI, ENERGIA ELETTRICA, GAS, ACQUA, SMALTIMENTO RIFIUTI, RAFFINERIE, CHIMICA, GOMMA, PLASTICA, ASSISTENZA RESIDENZIALE, SANITÀ', risk: 'high' }
      ]
    },
    {
      title: 'Datore di lavoro/RSPP (aggiornamento quinquennale)',
      reference: 'ACCORDO STATO REGIONI 21/12/2011 Rep. 223/CSR',
      content: [
        { level: 'Rischio basso (6 ore)', description: 'Attività a rischio basso', risk: 'low' },
        { level: 'Rischio medio (10 ore)', description: 'Attività a rischio medio', risk: 'medium' },
        { level: 'Rischio elevato (14 ore)', description: 'Attività a rischio elevato', risk: 'high' }
      ]
    },
    {
      title: 'RLS (Rappresentante dei lavoratori per la sicurezza)',
      reference: 'D.LGS. 81/2008 E S.M.I., ARTICOLO 37, COMMA 11',
      content: [
        { level: 'Corso iniziale (32 ore)', description: '', risk: 'none' },
        { level: 'Aggiornamento annuale (4 ore)', description: 'Per aziende 15-50 dipendenti', risk: 'none' },
        { level: 'Aggiornamento annuale (8 ore)', description: 'Per aziende >50 dipendenti', risk: 'none' }
      ]
    },
    {
      title: 'Addetti antincendio (iniziale)',
      reference: 'DM 2/9/2021',
      content: [
        { level: 'LIVELLO 1 (4 ore)', description: 'QUELLE NON RIENTRANTI NEL LIVELLO 2 O 3', risk: 'low' },
        { level: 'LIVELLO 2 (8 ore)', description: 'LUOGHI DI LAVORO (ALLEGATO I AL DPR 151/2011 CON ESCLUSIONE DELLE ATTIVITÀ DI LIVELLO 3). I CANTIERI TEMPORANEI E MOBILI OVE SI DETENGONO ED IMPIEGANO SOSTANZE INFIAMMABILI E SI FA USO DI FIAMME LIBERE, ESCLUSI QUELLI INTERAMENTE ALL\'APERTO.', risk: 'medium' },
        { level: 'LIVELLO 3 (16 ore)', description: 'STABILIMENTI DI "SOGLIA INFERIORE" E DI "SOGLIA SUPERIORE", FABBRICHE E DEPOSITI DI ESPLOSIVI, CENTRALI TERMOELETTRICHE, IMPIANTI DI ESTRAZIONE, IMPIANTI E LABORATORI NUCLEARI, DEPOSITI AL CHIUSO DI MATERIALI COMBUSTIBILI, ATTIVITÀ COMMERCIALI ED ESPOSITIVE CON SUPERFICI APERTA AL PUBBLICO S>10.000 M2, AEROSTAZIONI, STAZIONI FERROVIARIE, STAZIONI MARITTIME, METROPOLITANE, ALBERGHI>200 POSTI LETTO, STRUTTURE SANITARIE, CASE DI RIPOSO, SCUOLE E UFFICI CON >1.000 PERSONE', risk: 'high' }
      ]
    },
    {
      title: 'Addetti antincendio (aggiornamento quinquennale)',
      reference: 'DM 2/9/2021',
      content: [
        { level: 'LIVELLO 1 (2 ore)', description: 'Attività vedi sopra', risk: 'low' },
        { level: 'LIVELLO 2 (5 ore)', description: 'Attività vedi sopra', risk: 'medium' },
        { level: 'LIVELLO 3 (8 ore)', description: 'Attività vedi sopra', risk: 'high' }
      ]
    },
    {
      title: 'Addetti al primo soccorso (iniziale)',
      reference: 'D.LGS. 81/2008 E DM 388/2003',
      content: [
        { level: 'Aziende di gruppo B e C (12 ore)', description: 'Aziende che non rientrano nel gruppo A', risk: 'medium' },
        { level: 'Aziende di gruppo A (16 ore)', description: 'Attività industriali soggette all\'obbligo di dichiarazione o notifica', risk: 'high' }
      ]
    },
    {
      title: 'Addetti al primo soccorso (aggiornamento triennale)',
      reference: 'D.LGS. 81/2008 E DM 388/2003',
      content: [
        { level: 'Aziende di gruppo B e C (4 ore)', description: 'Attività vedi sopra', risk: 'medium' },
        { level: 'Aziende di gruppo A (6 ore)', description: 'Attività vedi sopra', risk: 'high' }
      ]
    },
    {
      title: 'Preposti (iniziale)',
      reference: 'ACCORDO STATO REGIONI 21/12/2011 Rep. 223/CSR',
      content: [
        { level: 'Corso iniziale (8 ore)', description: '', risk: 'none' }
      ]
    },
    {
      title: 'Preposti (aggiornamento biennale)',
      reference: 'ACCORDO STATO REGIONI 21/12/2011 Rep. 223/CSR',
      content: [
        { level: 'Corso di aggiornamento (6 ore)', description: '', risk: 'none' }
      ]
    },
    {
      title: 'ASPP (addetti al servizio di prevenzione e protezione)',
      reference: 'ACCORDO STATO REGIONI 7/7/2016',
      content: [
        { level: 'Modulo A (28 ore)', description: '', risk: 'none' },
        { level: 'Modulo B (comune) (48 ore)', description: '', risk: 'none' },
        { level: 'Moduli B di specializzazione', description: 'B-SP1 (Agricoltura e pesca), B-SP2 (Estrazioni, cave e costruzioni), B-SP3 (Sanità residenziale), B-SP4 (Chimico e petrolchimico)', risk: 'none' }
      ]
    },
    {
      title: 'ASPP (aggiornamento quinquennale)',
      reference: 'ACCORDO STATO REGIONI 7/7/2016',
      content: [
        { level: 'Corso di aggiornamento (20 ore)', description: '', risk: 'none' }
      ]
    },
    {
      title: 'Formazione generale e specifica lavoratori (iniziale)',
      reference: 'ACCORDO STATO REGIONI 21/12/2011 Rep. 223/CSR',
      content: [
        { level: 'Rischio basso (4+4 ore)', description: 'Commercio, artigianato, hotel, ristoranti, assicurazioni, immobiliari, informatica, associazioni ricreative, culturali e sportive, servizi domestici, organizzazioni extraterritoriali', risk: 'low' },
        { level: 'Rischio medio (4+8 ore)', description: 'Agricoltura, pesca, magazzinaggio, trasporti, comunicazione, assistenza sociale non residenziale, pubblica amministrazione, iSTRUZIONE', risk: 'medium' },
        { level: 'Rischio elevato (4+12 ore)', description: 'Estrazioni minerarie, costruzioni, industrie alimentari, tessili, abbigliamento, conciarie, legno, carta, editoria, stampa, minerali, produzione metalli, fabbricazione macchine, autoveicoli, mobili, energia elettrica, gas, acqua, smaltimento rifiuti, raffinerie, chimica, gomma, plastica, assistenza residenziale, sanità', risk: 'high' }
      ]
    },
    {
      title: 'Formazione generale e specifica lavoratori (aggiornamento quinquennale)',
      reference: 'ACCORDO STATO REGIONI 21/12/2011 Rep. 223/CSR',
      content: [
        { level: 'Corso di aggiornamento (6 ore)', description: 'Unico per tutte le attività', risk: 'none' }
      ]
    },
    {
      title: 'Attrezzature',
      reference: 'ACCORDO STATO REGIONI 22/2/2012',
      content: [
        { level: 'Piattaforme di lavoro elevabili (8-10 ore)', description: '', risk: 'none' },
        { level: 'Carrelli elevatori semoventi con conducente a bordo (12-16 ore)', description: '', risk: 'none' },
        { level: 'Trattori agricoli e forestali', description: '', risk: 'none' },
        { level: 'Escavatori, pale caricatrici frontali, terne e autoribaltabili a cingoli', description: '', risk: 'none' }
      ]
    },
    {
      title: 'Protezione dei dati personali',
      reference: '',
      content: [
        { level: 'Livello base (2 ore)', description: 'Consigliato per trattamenti semplici', risk: 'none' },
        { level: 'Livello medio (4 ore)', description: 'Consigliato per trattamenti di media complessità', risk: 'none' },
        { level: 'Livello avanzato (6 ore)', description: 'Consigliato per trattamenti complessi', risk: 'none' }
      ]
    },
    {
      title: 'Autocontrollo Igienico',
      reference: '',
      content: [
        { level: 'Personale (4 ore)', description: '', risk: 'none' },
        { level: 'Alimentarista (4 ore)', description: '', risk: 'none' },
        { level: 'Personale Alimentarista (4 ore)', description: '', risk: 'none' }
      ]
    },
    {
      title: 'Corso Safety&Security per personale volontariato',
      reference:'',
      content: [
        {level: 'Corso Safety&Security specifico per personale associazioni di volontariato da impiegare nelle manifestazioni (20 ore)', description: '', risk: 'none'}
      ]
    },
    {
      title: 'Stress Lavoro Correlato',
      reference: 'articolo 37 del D.Lgs. N. 81/2008 e s.m.i.',
      content:[
        {level: 'Corso di formazione specifica sullo stress da lavoro correlato (4 ore)', description:'', risk: 'none'}
      ]
    }
  ];

  let docenti: DocenteCardData[] = [
    {
      nome: "Dott.ssa Rosa BARBANO DI MAGGIO",
      qualifica: "DPO ed RSPP",
      titoli: [
        "Laurea in Giurisprudenza (L.U.I.S.S. \"Guido Carli\" – ROMA)",
        "Responsabile del Servizio di Prevenzione e Protezione",
        "Data Protection Officer certificato",
        "Security Manager"
      ]
    },
    {
      nome: "Dott. Giuseppe CENTRA",
      qualifica: "Psicologo e Psicoterapeuta",
      titoli: [
        "Laurea Magistrale in Psicologia Clinica e della Salute",
        "Responsabile Sanitario Associazione \"Comunità Sulla strada di Emmaus\" (Foggia)"
      ]
    },
    {
      nome: "Dott. Diego PALLADINO",
      qualifica: "Dirigente Medico presso CSS",
      titoli: [
        "Specializzazione in Radiologia Diagnostica"
      ]
    },
    {
      nome: "Dott. Donato CASSANO",
      qualifica: "Ispettore Superiore S.u.p.s. della Polizia di Stato in quiescenza",
      titoli: [
        "Laurea in Giurisprudenza",
        "Istruttore T.F.R., BLS-D e PBLS-D (Adulto e Pediatrico)",
        "Formatore Corsi per Autista Soccorritore ed addetti al trasporto e soccorso di infermi e feriti"
      ]
    },
    {
      nome: "O.S.S. Tommaso SCARPIELLO",
      qualifica: "Operatore Socio Sanitario",
      titoli: [
        "Istruttore BLSD e PBLSD",
        "Soccorritore Addetto ai Mezzi di Trasporto e Soccorso"
      ]
    },
    {
      nome: "P.T.C. Michele SVENTURATO",
      qualifica: "Perito Tecnico Commerciale",
      titoli: [
        "Docente ed Istruttore antincendio qualificato ai sensi del D.M. 2/9/2021",
        "Formatore/Istruttore Dipartimento di Protezione Civile",
        "Formatore/Istruttore Scuola di Protezione Civile Foggia"
      ]
    }
  ];

  // Handler per la selezione del corso da menu a tendina, aggiorna il dettaglio reattivo visualizzato
  function selectCourse(event: Event) {
    const target = event.target as HTMLSelectElement;
    const selectedTitle = target.value;
    courseDetails = courses.find(course => course.title === selectedTitle) || null;
  }
</script>

<style>
  .float {
    animation: float 3s infinite ease-in-out;
  }

  @keyframes float {
    0%, 100% { transform: translateY(0); }
    50% { transform: translateY(-10px); }
  }

  .custom-select {
    appearance: none;
    background-color: white;
    border: 2px solid #1B4B6B;
    border-radius: 5px;
    padding: 10px;
    font-size: 1rem;
    color: #1B4B6B;
    cursor: pointer;
    transition: border-color 0.3s ease;
  }

  .pulse-border {
    animation: pulse 1.5s infinite;
  }

  @keyframes pulse {
    0%, 100% { border-color: #1B4B6B; box-shadow: 0 0 10px #1B4B6B; }
    50% { border-color: #1B4B6B; box-shadow: 0 0 30px #1B4B6B; }
  }

  .tab-button {
    background-color: #f3f4f6;
    color: #1B4B6B;
    padding: 10px 20px;
    border: 2px solid #1B4B6B;
    border-radius: 5px;
    font-weight: 600;
    transition: all 0.3s ease;
  }

  .tab-button.active {
    background-color: #1B4B6B;
    color: white;
  }
</style>

<div class="min-h-screen bg-white" in:fade>
  <section class="relative h-[70vh] flex items-center justify-center overflow-hidden">
    <div class="absolute inset-0 w-full h-full blur-sm">
      <img src="/NorLan.jpg" alt="Background" class="w-full h-full object-cover" />
    </div>
    <div class="absolute inset-0 bg-gradient-to-b from-black/30 to-black/50"></div>
    <div class="container mx-auto px-4 relative z-10 text-center text-white">
      <h1 class="text-4xl md:text-5xl font-bold">Corsi di Formazione</h1>
      <p class="text-lg md:text-xl mt-4">Sicurezza e salute sui luoghi di lavoro</p>
    </div>
  </section>

  <section class="py-10 bg-gray-50 text-center">
    <div class="max-w-4xl mx-auto px-4">
      <h2 class="text-2xl md:text-3xl font-bold text-[#1B4B6B]">La nostra offerta formativa</h2>
      <p class="text-gray-600 text-lg mt-4">
        NorLan offre un'ampia gamma di corsi di formazione specifici per la sicurezza e salute nei luoghi di lavoro.
      </p>
    </div>
  </section>

  <section class="py-8 bg-white">
    <div class="max-w-5xl mx-auto px-4">
      <div class="grid grid-cols-2 md:grid-cols-5 gap-6">
        <div class="text-center float">
          <Shield class="w-12 h-12 mx-auto text-[#1B4B6B]" />
          <h3 class="mt-2 font-semibold text-[#1B4B6B]">Conformità normativa</h3>
        </div>
        <div class="text-center float">
          <BookOpen class="w-12 h-12 mx-auto text-[#1B4B6B]" />
          <h3 class="mt-2 font-semibold text-[#1B4B6B]">Docenti qualificati</h3>
        </div>
        <div class="text-center float">
          <Award class="w-12 h-12 mx-auto text-[#1B4B6B]" />
          <h3 class="mt-2 font-semibold text-[#1B4B6B]">Attestati riconosciuti</h3>
        </div>
        <div class="text-center float">
          <Clock class="w-12 h-12 mx-auto text-[#1B4B6B]" />
          <h3 class="mt-2 font-semibold text-[#1B4B6B]">Aggiornamenti periodici</h3>
        </div>
        <div class="text-center float">
          <Users class="w-12 h-12 mx-auto text-[#1B4B6B]" />
          <h3 class="mt-2 font-semibold text-[#1B4B6B]">Team di esperti</h3>
        </div>
      </div>
    </div>
  </section>

  <section class="py-6 bg-gray-50">
    <div class="max-w-6xl mx-auto px-4 text-center">
      <div class="flex flex-wrap justify-center gap-4">
        <button class="tab-button {!showDocents ? 'active' : ''}" onclick={() => showDocents = false}>
          Catalogo Corsi
        </button>
        <button class="tab-button {showDocents ? 'active' : ''}" onclick={() => showDocents = true}>
          I Nostri Docenti
        </button>
      </div>
    </div>
  </section>

  <!-- Sezione dinamica di selezione catalogo: permette all'utente di filtrare i dettagli tecnici per tipologia di corso -->
  {#if !showDocents}
    <section class="py-12 bg-gray-50">
      <div class="max-w-6xl mx-auto px-4">
        <h2 class="text-2xl md:text-3xl font-bold text-[#1B4B6B] text-center mb-8">Catalogo Corsi</h2>
        <div class="mb-6 text-center">
          <select id="course-select" onchange={selectCourse} class="custom-select pulse-border">
            <option value="">Seleziona un corso</option>
            {#each courses as course (course.title)}
              <option value={course.title}>{course.title}</option>
            {/each}
          </select>
        </div>

        {#if courseDetails}
          <div in:fade>
            <CorsoCard corso={courseDetails} />
          </div>
        {/if}
      </div>
    </section>
    <!-- Visualizzazione a griglia del team docente, richiamando il componente dedicato DocenteCard per ogni profilo -->
  {:else}
    <section class="py-12 bg-gray-50" in:fade>
      <div class="max-w-6xl mx-auto px-4">
        <h2 class="text-2xl md:text-3xl font-bold text-[#1B4B6B] text-center mb-8">Il Nostro Team di Docenti</h2>
        <div class="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-8">
          {#each docenti as docente (docente.nome)}
            <DocenteCard {docente} />
          {/each}
        </div>
      </div>
    </section>
  {/if}

  <section class="py-12 bg-[#1B4B6B] text-white text-center">
    <div class="max-w-4xl mx-auto px-4">
      <h2 class="text-2xl md:text-3xl font-bold">Hai bisogno di informazioni?</h2>
      <div class="mt-6">
        <a href="/Contatti" class="inline-block bg-white text-[#1B4B6B] px-8 py-3 rounded-lg font-semibold hover:bg-gray-100 transition-colors">
          Contattaci
        </a>
      </div>
    </div>
  </section>
</div>