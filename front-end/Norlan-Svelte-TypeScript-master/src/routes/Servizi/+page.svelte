<script lang="ts">
  import { ChevronRight } from 'lucide-svelte';
  import { fade } from 'svelte/transition';

  interface ServiceContent {
    title: string;
    content: string[];
  }

  interface Service {
    title: string;
    content: ServiceContent[];
  }

  const services: Service[] = [
    {
      title: "CONSULENZA LEGALE IN MATERIA DI PROTEZIONE DEI DATI PERSONALI (Regolamento UE 2016/679 e D.Lgs. n. 196/2003)",
      content: [
        {
          title: "CONSULENZA",
          content: [
            "Mappatura del trattamento dei dati",
            "Verifica delle basi giuridiche dei vari trattamenti",
            "Redazione della documentazione (informative, consensi, procedure di accountability, analisi dei rischi, valutazione di impatto sulla protezione dei dati, registro dei trattamenti, framework nazionale per la gestione ed il monitoraggio della cybersecurity, nomine ecc.)",
            "Supporto nella gestione delle NOTIFICHE DI VIOLAZIONI",
            "Supporto nella gestione delle richieste di esercizio dei diritti da parte degli interessati",
            "Ulteriori adempimenti specifici in base agli specifici trattamenti",
          ]
        },
        {
          title: "INCARICO DI RESPONSABILE DELLA PROTEZIONE DEI DATI/DATA PROTECTION OFFICER (RPD/DPO)",
          content: [
            "Offriamo specifica consulenza per l’assunzione dell’incarico di RPD/DPO (ove previsto). I nostri consulenti sono certificati ai sensi della norma UNI 11697 (ENTE DI CERTIFICAZIONE KHC)"
          ]
        },
        {
          title: "INCARICO DI PRIVACY SPECIALIST",
          content: [
            "Offriamo consulenza per l’affidamento dell’incarico di PRIVACY SPECIALIST all’interno del TEAM di protezione dei dati aziendale"
          ]
        },
        {
          title: "SERVIZI IN MATERIA DI CYBERSECURITY",
          content: [
            "Grazie alla collaborazione con aziende esperte, vengono forniti anche specifici Servizi in materia di CYBERSECURITY, con particolare riferimento a: 1. Cybersecurity Management, 2. Penetration Testing, 3. Interventi tecnici atti ad eliminare eventuali vulnerabilità e a proteggere gli asset aziendali, 4. Formazione del personale."
          ]
        },
        {
          title: "VIDEOSORVEGLIANZA E VIDEOREGISTRAZIONE",
          content: [
            "Offriamo consulenza per la gestione delle pratiche di VIDEOSORVEGLIANZA E VIDEOREGISTRAZIONE (Statuto dei Lavoratori) sia nei rapporti con le rappresentanze sindacali aziendali (supporto nella redazione dell’ACCORDO SINDACALE e nella gestione delle relative riunioni con le RSA aziendali) che nella presentazione di ISTANZA alla Direzione Territoriale del Lavoro (gestione pratica, supporto nella redazione della relazione tecnica, redazione planimetrie illustrative dell’impianto ecc.)"
          ]
        }
      ]
    },
    {
      title: "CONSULENZA LEGALE IN MATERIA DI SICUREZZA E SALUTE SUI LUOGHI DI LAVORO ed in materia di SECURITY RISK MANAGEMENT",
      content: [
        {
          title: "CONSULENZA",
          content: [
            'Valutazione dei rischi e redazione del relativo documento (DVR)',
            'Redazione della documentazione collegata al DVR: verbali riunioni, designazioni, consegna DPI, check list di verifica, monitoraggio e controllo ecc.',
            'Supporto nel rapporto con i RLS',
            'Supporto nella scelta dei DPI',
            'Supporto nella scelta della segnaletica di sicurezza e nella valutazione di conformità di luoghi di lavoro ed attrezzature',
            'Progettazione di specifici OPUSCOLI DI INFORMAZIONE in materia di sicurezza e salute sui luoghi di lavoro',
            'Redazione, ove previsto, del DOCUMENTO UNICO DI VALUTAZIONE DEI RISCHI DA INTERFERENZE (DUVRI), in caso di presenza di più aziende nello stesso luogo di lavoro e gestione di tutte le attività di coordinamento per conto dell’azienda committente',
            '(cantieri) REDAZIONE PIANI OPERATIVI DI SICUREZZA (POS)',
            'Redazione di PIANI DI EMERGENZA e delle relative PLANIMETRIE DI EMERGENZA e supporto nell’effettuazione e gestione delle prove di evacuazione',
            'Valutazioni specifiche (rischio biologico e redazione dell’eventuale documentazione aggiuntiva, rischio derivante dall’utilizzo di particolari attrezzature di lavoro e redazione, ove necessario, di specifiche procedure di utilizzo e manuali di manutenzione, rischio legionellosi e predisposizione della documentazione specifica, compresi i registri di controllo, rischio chimico, rischio cancerogeno ecc.)'
          ]
        },
        {
          title: "INCARICO DI RSPP ESTERNO",
          content: [
            "Offriamo specifica consulenza per l’assunzione (ove richiesto) dell’incarico di RSPP ESTERNO",
            "I nostri consulenti sono abilitati per tutti i MACROSETTORI ATECO",
          ]
        },
        {
          title: "SPECIFICHE RILEVAZIONI STRUMENTALI",
          content: [
            "Qualora necessario, per le rilevazioni strumentali, ci avvaliamo di tecnici e Laboratori autorizzati"
          ]
        }
      ]
    },
    {
      title: "CONSULENZA IN MATERIA DI AUTOCONTROLLO IGIENICO",
      content: [
        {
          title: "CONSULENZA",
          content: [
            "Consulenza in materia di NOTIFICA AI FINI DELLA REGISTRAZIONE IMPRESE ALIMENTARI",
            "Redazione del MANUALE AZIENDALE DI AUTOCONTROLLO IGIENICO e del MANUALE DELLA RINTRACCIABILITA’",
            "Redazione ed aggiornamento periodico della documentazione collegata ai manuali",
            "Redazione e fornitura periodica dei registri di MONITORAGGIO",
            "Verifiche periodiche in azienda (audit)",
            "Redazione, ove previsto, della documentazione semplificata",
            "Redazione, ove richiesto, del layout aziendale",
            "Redazione di specifiche procedure ed istruzioni operative",
            "Progettazione di cartellonistica personalizzata",
          ]
        },
        {
          title: "VERIFICHE DI LABORATORIO",
          content: [
            "Per la predisposizione e gestione del PIANO DELLE VERIFICHE ANALITICHE DI LABORATORIO (ove previsto) ci rivolgiamo a Laboratori accreditati per l’esecuzione dei prelievi.",
            "Il Piano delle verifiche prevede, in base alle esigenze ed alle dimensioni aziendali, la campionatura di: Acqua (anche potabilità); Materia prima; Prodotto finito; Superfici (tamponi); Operatori (tamponi); Aria"
          ]
        }
      ]
    },
    {
      title: "CONSULENZA IN MATERIA DI SISTEMI DI GESTIONE",
      content: [
        {
          title: "CONSULENZA",
          content: [
            "I nostri consulenti possono supportare le Aziende Pubbliche e Private che intendano progettare e implementare sistemi di gestione. In particolare, possiamo supportarvi per le seguenti certificazioni:"
          ]
        },
        {
          title: "SISTEMI DI GESTIONE PER LA QUALITA'",
          content: [
            "Certificazione del SISTEMA DI GESTIONE PER LA QUALITA’ (UNI EN ISO 9001:2015) come requisito indispensabile di riferibilità, affidabilità e credibilità dell’azienda nel mercato e spesso richiesto per la partecipazione a bandi di gara pubblici."
          ]
        },
        {
          title: "SISTEMI DI GESTIONE AMBIENTALI",
          content: [
            "Certificazione del SISTEMA DI GESTIONE AMBIENTALE (UNI EN ISO 14001:2015) come elemento distintivo legato all’impegno aziendale di salvaguardare l’ambiente oltre che come strumento di minimizzazione dei costi di una eventuale non conformità."
          ]
        },
        {
          title: "SISTEMI DI GESTIONE SALUTE E SICUREZZA SUI LUOGHI DI LAVORO",
          content: [
            "Certificazione del SISTEMA DI GESTIONE DELLA SICUREZZA E SALUTE SUL LAVORO (UNI EN ISO 45001:2018) come strumento di gestione di tutte le problematiche legate alla sicurezza e salute sui luoghi di lavoro e di diffusione di una cultura aziendale della sicurezza e come valido elemento distintivo sul mercato, spesso richiesto come requisito per la partecipazione a bandi di gara pubblici. Tale certificazione, inoltre, ha efficacia esimente della responsabilità amministrativa delle persone giuridiche, delle società e delle associazioni (art. 30 del D.Lgs. n. 81/2008 e s.m.i.) e consente di poter partecipare alle richieste di sconti sui premi assicurativi INAIL."
          ]
        },
        {
          title: "SISTEMI INTEGRATI",
          content: [
            "I tre sistemi possono essere anche INTEGRATI. La nostra azienda inoltre fornisce supporto nel contatto e nella scelta dell’ENTE DI CERTIFICAZIONE e fornisce assistenza sia durante la fase di prima implementazione che nelle successive fasi di mantenimento della certificazione."
          ]
        },
        {
          title: "SISTEMI DI GESTIONE PER LA SICUREZZA DELLE INFORMAZIONI",
          content: [
            "Certificazione del SISTEMA DI GESTIONE PER LA SICUREZZA DELLE INFORMAZIONI (ISO IEC 27701:2019) come strumento di accountability ai sensi del Regolamento UE 2016/679"
          ]
        },
        {
          title: "SISTEMI DI GESTIONE ANTICORRUZIONE",
          content: [
            "Certificazione del SISTEMA DI GESTIONE ANTICORRUZIONE ai sensi della norma ISO 37001:2016"
          ]
        },
        {
          title: "MODELLI ORGANIZZATIVI 231",
          content: [
            "Progettazione ed implementazione di modelli organizzativi ai sensi del D.Lgs. 231/2001",
            "Tali modelli consentono alle aziende di ottenere l’esimente della responsabilità amministrativa qualora vengano commessi i reati specificamente elencati nella normativa (c.d. REATI PRESUPPOSTO)"
          ]
        }
      ]
    }
  ];

  let activeService: number | null = null;

  function setActiveService(index: number) {
    activeService = index;
  }
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

  @keyframes pulse {
    0%, 100% {
      border-color: #1B4B6B;
      box-shadow: 0 0 10px #1B4B6B;
    }
    50% {
      border-color: #1B4B6B;
      box-shadow: 0 0 30px #1B4B6B;
    }
  }

  .pulse-border {
    animation: pulse 1.5s infinite;
  }
</style>

<div class="min-h-screen bg-gray-100" in:fade out:fade>
  <section class="relative h-[70vh] flex items-center justify-center overflow-hidden">
    <div class="absolute inset-0 w-full h-full blur-sm">
      <img
        src="/NorLan.jpg"
        alt="Background"
        class="w-full h-full object-cover"
      />
    </div>
    <div class="absolute inset-0 bg-gradient-to-b from-black/30 to-black/50"></div>
    <div class="container mx-auto px-4 relative z-10 text-center">
      <h1 class="text-4xl md:text-5xl font-bold text-white slide-in-bottom">
        I Nostri Servizi
      </h1>
    </div>
  </section>

  <section class="py-16">
    <div class="container mx-auto px-4">
      <div class="flex flex-col lg:flex-row gap-8 max-w-7xl mx-auto">
        <div class="lg:w-1/3 slide-in-bottom">
          <div class="sticky top-24 space-y-2">
            {#each services as service, index}
              <button
                on:click={() => setActiveService(index)}
                class={`
                  w-full text-left p-6 rounded-xl transition-all duration-300
                  border-2 border-gray-300 pulse-border
                  ${activeService === index
                    ? 'bg-[#1B4B6B] text-white border-[#1B4B6B] shadow-lg '
                    : 'bg-white text-gray-700 hover:bg-gray-50'}
                `}
              >
                {service.title}
              </button>
            {/each}
          </div>
        </div>

        <div class="lg:w-2/3">
          {#if activeService !== null}
            <div class="space-y-6">
              <div class="bg-white rounded-xl shadow-lg p-6 border-2 border-[#1B4B6B]">
                <h2 class="text-xl md:text-2xl font-bold text-[#1B4B6B]">
                  {services[activeService].title}
                </h2>
              </div>

              {#each services[activeService].content as section}
                <div class="bg-white rounded-xl shadow-lg border-2 border-gray-200">
                  <div class="bg-[#1B4B6B] px-6 py-4">
                    <h3 class="text-lg font-semibold text-white">{section.title}</h3>
                  </div>
                  <div class="p-6 space-y-4">
                    {#each section.content as item}
                      <div class="flex items-start gap-4">
                        <ChevronRight class="w-5 h-5 text-[#1B4B6B] flex-shrink-0 mt-1" />
                        <p class="text-gray-700 leading-relaxed">{item}</p>
                      </div>
                    {/each}
                  </div>
                </div>
              {/each}
            </div>
          {:else}
            <div class="bg-white rounded-xl shadow-lg p-6 text-gray-700">
              Selezionare un servizio per vederne i dettagli.
            </div>
          {/if}
        </div>
      </div>
    </div>
  </section>
</div>
