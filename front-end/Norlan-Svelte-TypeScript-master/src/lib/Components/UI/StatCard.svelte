<script lang="ts">
    /*
Componente "Card Statistica" per le dashboard.
Renderizza una card cliccabile se riceve un link (href),
altrimenti mostra un blocco informativo statico.
*/

    import type { ComponentType } from 'svelte';
    import { ArrowRight } from 'lucide-svelte';

    // Definisce la struttura dei dati in ingresso e permette l'override degli stili
    interface Props {
        titolo: string;
        valore: string | number;
        icona: ComponentType;
        href?: string;
        bgIcona?: string;
        testoIcona?: string;
        hoverBgIcona?: string;
    }

    let {
        titolo,
        valore,
        icona: Icona,
        href,
        bgIcona = 'bg-[#1B4B6B]/10',
        testoIcona = 'text-[#1B4B6B]',
        hoverBgIcona = 'group-hover:bg-[#1B4B6B]'
    }: Props = $props();
</script>

{#if href}
    <!-- Card interattiva con effetti hover e navigazione (usata se href è definito) -->
    <a {href} class="bg-white p-6 rounded-3xl shadow-sm border border-gray-100 flex flex-col justify-between group hover:shadow-xl hover:border-[#1B4B6B]/30 hover:-translate-y-1 transition-all cursor-pointer">
        <div class="flex justify-between items-start mb-4">
            <div class="p-4 {bgIcona} {testoIcona} rounded-2xl {hoverBgIcona} group-hover:text-white transition-colors">
                <Icona size={24} />
            </div>
            <ArrowRight size={20} class="text-gray-300 group-hover:text-[#1B4B6B] transition-colors" />
        </div>
        <div>
            <h2 class="text-4xl font-black text-[#1B4B6B]">{valore}</h2>
            <p class="text-[10px] font-black text-gray-400 uppercase tracking-widest mt-1">{titolo}</p>
        </div>
    </a>
{:else}
    <!-- Card statica per la semplice visualizzazione dei dati (usata come fallback) -->
    <div class="bg-white p-8 rounded-3xl border border-gray-100 shadow-sm flex items-center gap-6 hover:shadow-xl transition-all group">
        <div class="w-16 h-16 rounded-2xl flex items-center justify-center transition-colors {bgIcona} {testoIcona} {hoverBgIcona} group-hover:text-white">
            <Icona size={28} />
        </div>
        <div>
            <p class="text-[10px] font-black text-gray-400 uppercase tracking-widest mb-1">{titolo}</p>
            <p class="text-4xl font-black text-[#1B4B6B] leading-none">{valore}</p>
        </div>
    </div>
{/if}