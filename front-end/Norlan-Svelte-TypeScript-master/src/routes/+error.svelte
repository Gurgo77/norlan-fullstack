<script lang="ts">
  import { fade } from "svelte/transition";
  import { Search } from "lucide-svelte";
  import { page } from '$app/stores';
</script>

<div class="min-h-screen bg-white flex flex-col" in:fade out:fade>
  <section class="h-64 flex items-center justify-center relative">
    <div class="absolute inset-0 bg-[url('/NorLan.jpg')] bg-cover bg-center blur-sm"></div>

    <div class="absolute inset-0 bg-black/40"></div>

    <div class="relative text-center text-white">
      <h1 class="text-4xl md:text-6xl font-bold">{$page.status}</h1>
      <p class="text-xl md:text-2xl mt-2">{$page.error?.message || 'Si è verificato un errore'}</p>
    </div>
  </section>

  <section class="flex-1 py-12 bg-gray-50 flex flex-col items-center justify-center text-center px-4">
    <div class="max-w-2xl mx-auto">
      <Search class="w-16 h-16 mx-auto text-[#1B4B6B] mb-6" />
      <h2 class="text-2xl md:text-3xl font-bold text-[#1B4B6B]">Qualcosa è andato storto nell'applicativo</h2>
      <p class="text-gray-600 text-lg mt-4">
        Controlla il terminale di sviluppo per maggiori dettagli tecnici sull'errore.
      </p>
      <div class="mt-8">
        <a href="/" class="inline-block bg-[#1B4B6B] text-white px-8 py-3 rounded-lg hover:bg-[#1B4B6B]/90 mr-4">
          Torna alla Home
        </a>
      </div>
    </div>
  </section>

  <div class="h-0.5 w-full bg-gradient-to-r from-[#1B4B6B] to-[#2C7DA0]"></div>

  <section class="py-8 bg-white text-center">
    <p class="text-gray-600">NorLan - Consulenza integrata sulle normative speciali</p>
  </section>
</div>