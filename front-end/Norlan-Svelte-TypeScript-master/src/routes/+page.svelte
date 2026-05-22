<script lang="ts">
  import { fade } from "svelte/transition";
  import { Lock, Shield, ClipboardCheck, Settings } from "lucide-svelte";

  /*
Componente Home Page del portale NorLan.
Funge da punto di ingresso principale, presentando l'identità del brand, una panoramica
dei servizi core (compliance normativa) e call-to-action strategiche verso le pagine interne.
*/
  class ServicePreview {
    icon: typeof Lock;
    title: string;
    description: string;

    constructor(icon: typeof Lock, title: string, description: string) {
      this.icon = icon;
      this.title = title;
      this.description = description;
    }
  }

  // Modello dati per l'anteprima dei servizi mostrati nella griglia della Home Page
  const services: ServicePreview[] = [
    new ServicePreview(
      Lock,
      "Protezione Dati Personali",
      "Consulenza completa sulla protezione dei dati personali, GDPR compliance e servizio DPO esterno certificato."
    ),
    new ServicePreview(
      Shield,
      "Sicurezza sul Lavoro",
      "Servizio RSPP esterno e consulenza completa sulla sicurezza nei luoghi di lavoro secondo le normative vigenti."
    ),
    new ServicePreview(
      ClipboardCheck,
      "Autocontrollo Igienico",
      "Consulenza in materia di HACCP, verifiche di laboratorio e gestione della documentazione per le imprese alimentari."
    ),
    new ServicePreview(
      Settings,
      "Sistemi di Gestione",
      "Implementazione e gestione di sistemi certificati ISO 9001, 14001, 45001 e modelli organizzativi 231."
    )
  ];
</script>

<div class="min-h-screen bg-white" in:fade out:fade>
  <!-- Sezione Hero: Impatto visivo iniziale con immagine di sfondo, titolo e call-to-action principale -->
  <section class="h-screen flex items-center justify-center relative bg-cover bg-center">
    <div class="absolute inset-0 bg-[url('/NorLan.jpg')] bg-cover bg-center blur-sm blur"></div>

    <div class="absolute inset-0 bg-black/40"></div>

    <div class="relative text-center text-white slide-in-bottom">
      <h1 class="text-5xl md:text-7xl font-bold">NorLan</h1>
      <p class="text-xl md:text-2xl mt-4">Consulenza integrata sulle normative speciali</p>
      <a href="/Servizi" class="mt-6 inline-block bg-white text-[#1B4B6B] px-8 py-3 rounded-lg font-semibold pulse">Scopri di più</a>
    </div>
  </section>

  <!-- Sezione Identitaria: Breve introduzione alla mission NorLan con link al profilo professionale -->
  <section class="py-20 bg-gray-50 text-center">
    <h2 class="text-3xl md:text-4xl font-bold text-[#1B4B6B]">Chi Siamo</h2>
    <p class="text-gray-600 text-lg max-w-3xl mx-auto mt-4">
      NorLan nasce dall'idea di fornire, ad aziende ed enti pubblici e privati, consulenza "integrata" sulle normative speciali, nell'ottica della "compliance" aziendale.
    </p>
    <a href="/Chi-Siamo" class="mt-8 inline-block bg-[#1B4B6B] text-white px-8 py-3 rounded-lg hover:bg-[#1B4B6B]/90 typing">Scopri la nostra storia</a>
  </section>

  <div class="h-0.5 w-full bg-gradient-to-r from-[#1B4B6B] to-[#2C7DA0]"></div>

  <!-- Griglia dinamica dei servizi: mostra le 4 macro-aree di consulenza (Privacy, Sicurezza, HACCP, Sistemi) -->
  <section class="py-20 text-center">
    <h2 class="text-3xl md:text-4xl font-bold text-[#1B4B6B]">I Nostri Servizi</h2>
    <div class="grid grid-cols-1 md:grid-cols-2 gap-8 max-w-5xl mx-auto mt-8">
      {#each services as service (service.title)}
        <div class="bg-white p-8 rounded-xl ombraHome hover:shadow-xl transition-shadow text-center float">
          <svelte:component this={service.icon} class="w-12 h-12 mx-auto text-[#1B4B6B] spin" />
          <h3 class="text-xl font-bold text-[#1B4B6B] mt-4">{service.title}</h3>
          <p class="text-gray-600 mt-2">{service.description}</p>
        </div>
      {/each}
    </div>
    <a href="/Servizi" class="mt-8 inline-block bg-[#1B4B6B] text-white px-8 py-3 rounded-lg hover:bg-[#1B4B6B]/90 typing">Tutti i Servizi</a>
  </section>

  <div class="h-0.5 w-full bg-gradient-to-r from-[#1B4B6B] to-[#2C7DA0]"></div>

  <section class="py-20 bg-gray-50 text-center">
    <h2 class="text-3xl font-bold text-[#1B4B6B]">Hai bisogno di una consulenza?</h2>
    <p class="text-gray-600 mt-4 max-w-2xl mx-auto">Contattaci per una consulenza personalizzata sulle tue esigenze specifiche.</p>
    <a href="/Contatti" class="mt-6 inline-block bg-[#1B4B6B] text-white px-8 py-3 rounded-lg hover:bg-[#1B4B6B]/90 typing">Contattaci</a>
  </section>
</div>