<script>
  import { onMount } from 'svelte';
  import { fade } from 'svelte/transition';
  import { Shield, BadgeCheck, Users, Medal, GraduationCap, Calendar, Clock } from 'lucide-svelte';

  const certifications = [
    {
      icon: Shield,
      title: "Centro Territoriale ANFOS",
      category: "Sicurezza sul Lavoro",
      link: "https://www.anfos.org/centri-di-formazione-anfos/?c=01942",
      desc: "Centro Territoriale ANFOS (N. 09142)"
    },
    {
      icon: BadgeCheck,
      title: "DPO Certificato KHC",
      category: "Privacy",
      link: "https://registri.khc.it/figure-professionali/risultati-registro-figure-professionali/?wpv_view_count=11825&wpv-wpcf-figura1=Data+Protection+Officer+UNI+11697%3A2017&wpv-wpcf-registro=&wpv-wpcf-nominativo1=barbano&wpv-wpcf-provincia1=&wpv-wpcf-nazione1=",
      desc: "N.Reg. DPO2352"
    },
    {
      icon: Users,
      title: "Socio AIAS",
      category: "Associazioni",
      link: "https://www.aias-sicurezza.it/barbano-di-maggio-rosa/sffb36050",
      desc: "Associazione Italiana Addetti alla Sicurezza"
    },
    {
      icon: Shield,
      title: "RSPP",
      category: "Sicurezza sul Lavoro",
      desc: "RSPP abilitato per tutti i macrosettori ATECO"
    },
    {
      icon: Medal,
      title: "Componente CTS AIAS",
      category: "Associazioni",
      desc: "Componente del Comitato Tecnico Scientifico"
    },
    {
      icon: Shield,
      title: "Socio ANPD",
      category: "Privacy",
      desc: "Associazione Nazionale per la Protezione dei Dati"
    },
    {
      icon: BadgeCheck,
      title: "Operatore abilitato su MePA",
      category: "Qualifiche",
      desc: "Servizi di Supporto Specialistico"
    }
  ];

  const education = [
    {
      icon: GraduationCap,
      institution: "Luiss Guido Carli University",
      title: "Laurea in Giurisprudenza, Indirizzo Giuridico - Economico",
      period: "1992 - 1997"
    },
    {
      icon: GraduationCap,
      institution: "ALMA LABORIS Business School",
      title: "MASTER ALTA FORMAZIONE SISTEMI QUALITA' AMBIENTE E SICUREZZA",
      period: "2011"
    },
    {
      icon: GraduationCap,
      institution: "IL CORRIERE DELLA PRIVACY - FEDERPRIVACY - WOLTERS KLUWER",
      title: "MASTER PRIVACY OFFICER & Consulente Privacy",
      duration: "48 ore",
      period: "2017"
    },
    {
      icon: GraduationCap,
      institution: "Luiss Business School",
      title: "Executive Programme in Compliance Management",
      duration: "140 ore",
      period: "2018",
      details: "Corporate Governance, Sistemi dei Controlli, Compliance, D.Lgs. n. 231/01, Trasparenza ed Anti corruzione, Sicurezza e Salute sui Luoghi di Lavoro, Tutela dell'Ambiente, Antiriciclaggio, Data Protection Officer & Privacy Consultant"
    },
    {
      icon: GraduationCap,
      institution: "Altalex Formazione",
      title: "Master GDPR: La Tutela Sostanziale e Processuale dei Diritti degli Interessati",
      duration: "20 ore",
      period: "2019"
    },
    {
      icon: GraduationCap,
      institution: "Alta Scuola di Formazione per la Protezione dei Dati",
      title: "Cybersecurity Specialist",
      duration: "24 ore",
      period: "2020"
    },
    {
      icon: GraduationCap,
      institution: "Alta Scuola di Formazione per la Protezione dei Dati",
      title: "Cybersecurity Manager",
      duration: "24 ore",
      period: "2020"
    },
    {
      icon: GraduationCap,
      institution: "Accademia Istituto Italiano Privacy (IIP)",
      title: "Corso Maestro della Protezione dei Dati & Data Protection Designer",
      period: "ott 2020 - lug 2021"
    },
    {
      icon: GraduationCap,
      institution: "Luiss Business School",
      title: "EXECUTIVE PROGRAMME IN SECURITY RISK MANAGEMENT",
      period: "nov 2021 - apr 2022"
    },
    {
      icon: GraduationCap,
      institution: "Croce Rossa Italiana - Corpo Militare Volontario",
      title: "Corso di 2° Livello in Psicologia delle Emergenze e Gestione dello Stress in Operazioni",
      period: "mar 2024 - mar 2025",
      details: "Presso il Comando CC 'Cacciatori di Puglia' di Amendola (FG)"
    }
  ];
  let visible = false;

  onMount(() => {
    visible = true;
  });

  const springParams = {
    stiffness: 0.3,
    damping: 0.7
  };
</script>

<style>
  @keyframes float {
    0%, 100% {
      transform: translateY(0);
    }
    50% {
      transform: translateY(-10px);
    }
  }

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

  .float {
    animation: float 3s infinite;
  }
</style>

<main class="min-h-screen pt-20 bg-white" in:fade out:fade>
  <section class="relative h-[70vh] flex items-center justify-center">
    <div class="absolute inset-0 w-full h-full blur-sm">
      <img
        src="/NorLan-ChiSiamo.jpg"
        alt="Background"
        class="w-full h-full object-cover"
      />
    </div>
    <div class="absolute inset-0 bg-gradient-to-b from-black/30 to-black/50"></div>
    <div class="container mx-auto px-4 relative z-10">
      <div class="max-w-3xl mx-auto text-center slide-in-bottom">
        {#if visible}
          <div class="mb-8">
            <h1 class="text-4xl md:text-5xl font-bold mb-4 text-white">
              Dott.ssa Rosa<br/>BARBANO-DI MAGGIO
            </h1>
            <div class="w-16 h-0.5 bg-white mx-auto mb-4"></div>
            <p class="text-base md:text-lg text-white/90 mb-8">
              Sistemi integrati di compliance per l'efficienza aziendale
            </p>
            <div class="w-16 h-0.5 bg-white mx-auto mb-4"></div>
            <p class="text-xl text-white/90 max-w-2xl mx-auto font-medium bg-black/30 p-6 rounded-lg backdrop-blur-sm">
              La Dott.ssa Rosa Barbano Di Maggio ha conseguito la Laurea in Giurisprudenza presso la L.U.I.S.S. "Guido
              Carli" ed ha acquisito le abilitazioni e certificazioni necessarie per poter fornire consulenza e
              supporto ad enti ed aziende pubbliche e private.
            </p>
          </div>
        {/if}
      </div>
    </div>
  </section>

  <section class="py-16 bg-white">
    <div class="container mx-auto px-4">
      <h2 class="text-3xl font-bold text-[#1B4B6B] mb-12 text-center">
        Certificazioni e Qualifiche
      </h2>

      <div class="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6 max-w-7xl mx-auto">
        {#each certifications as cert, idx}
          <div class="bg-white rounded-xl p-6 relative overflow-hidden group ombraChiSiamo float">
            <div class="absolute top-0 right-0 bg-[#1B4B6B]/5 w-32 h-32 rounded-full -translate-y-16 translate-x-16 group-hover:scale-150 transition-transform duration-500"></div>

            <div class="absolute top-2 right-2 px-3 py-1 text-sm bg-[#1B4B6B]/10 text-[#1B4B6B] rounded-full">
              {cert.category}
            </div>

            <div class="relative z-10">
              <div class="text-[#1B4B6B] mb-4 transform group-hover:scale-110 transition-transform">
                <svelte:component this={cert.icon} size={48} />
              </div>
              <h3 class="text-xl font-bold text-[#1B4B6B] mb-2 group-hover:text-[#1B4B6B]/80 transition-colors">
                {cert.title}
              </h3>
              <p class="text-gray-600 mb-4 text-sm">
                {cert.desc}
              </p>
              {#if cert.link}
                <a
                  href={cert.link}
                  target="_blank"
                  rel="noopener noreferrer"
                  class="inline-flex items-center justify-center space-x-2 bg-[#1B4B6B] text-white px-4 py-2 rounded-lg hover:bg-[#1B4B6B]/90 transition-all mt-4 w-full"
                >
                  <span class="font-medium typing">Verifica Certificazione</span>
                  <BadgeCheck size={20} />
                </a>
              {/if}
            </div>
          </div>
        {/each}
      </div>
    </div>
  </section>

  <div class="h-0.5 w-full bg-gradient-to-r from-[#1B4B6B] to-[#2C7DA0]"></div>

  <section class="py-16 bg-gray-50">
    <div class="container mx-auto px-4">
      <h2 class="text-3xl font-bold text-[#1B4B6B] mb-12 text-center">
        Formazione
      </h2>

      <div class="max-w-5xl mx-auto">
        <div class="relative">
          <div class="absolute top-0 bottom-0 left-8 w-0.5 bg-[#1B4B6B]/20"></div>

          {#each education as item, idx}
            <div class="relative mb-8 ml-20">
              <div class="absolute -left-18 mt-0">
                <div class="bg-white p-2 rounded-full shadow-md">
                  <svelte:component this={item.icon} size={32} />
                </div>
              </div>
              <div class="bg-white rounded-xl p-6 ombraChiSiamo hover:shadow-xl float transition-all">
                <h3 class="text-xl font-bold text-[#1B4B6B] mb-2">
                  {item.institution}
                </h3>
                <p class="text-gray-800 font-medium mb-3">
                  {item.title}
                </p>
                <div class="flex items-center flex-wrap gap-4 text-sm text-gray-600">
                  <div class="flex items-center space-x-2">
                    <Calendar size={16} />
                    <span>{item.period}</span>
                  </div>
                  {#if item.duration}
                    <div class="flex items-center space-x-2">
                      <Clock size={16} />
                      <span>{item.duration}</span>
                    </div>
                  {/if}
                </div>
                {#if item.details}
                  <p class="mt-3 text-sm text-gray-600">
                    {item.details}
                  </p>
                {/if}
              </div>
            </div>
          {/each}
        </div>
      </div>
    </div>
  </section>
</main>
