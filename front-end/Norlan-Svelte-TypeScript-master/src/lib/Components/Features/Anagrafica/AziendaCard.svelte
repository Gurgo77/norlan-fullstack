<script lang="ts">
    /**
     * Componente UI che rappresenta la scheda sintetica di un'Azienda cliente/registrata.
     * Mostra i dati anagrafici essenziali (Ragione Sociale, P.IVA, Sede) e le azioni rapide.
     * Utilizza le "Runes" di Svelte 5 ($props) per l'iniezione delle proprietà.
     */
    import {
        Trash2,
        ChevronRight,
        Building2,
        MapPin,
        FileDigit,
        Users,
        User
    } from 'lucide-svelte';

    interface Props {
        idUtente?: number | string;
        ragioneSociale: string;
        partitaIva: string;
        sedeLegale?: string;
        hasDipendenti?: boolean;
        canViewDetails?: boolean;
        canDelete?: boolean;
        onView?: () => void; // Callback per il routing alla pagina di dettaglio
        onDelete?: () => void; // Callback per chiamare l'API di eliminazione (gestita dal parent)
    }

    let {
        idUtente,
        ragioneSociale,
        partitaIva,
        sedeLegale,
        hasDipendenti = false,
        canViewDetails = true,
        canDelete = true,
        onView,
        onDelete
    }: Props = $props();
</script>

<div class="bg-white rounded-3xl border border-gray-100 shadow-sm hover:shadow-xl transition-all group relative flex flex-col h-full overflow-hidden">

    {#if canDelete}
        <button
                onclick={(e) => { e.stopPropagation(); onDelete?.(); }}
                class="absolute top-4 right-4 p-2 text-gray-300 hover:text-red-600 opacity-0 group-hover:opacity-100 transition-all z-10 hover:bg-red-50 rounded-lg"
                title="Rimuovi azienda"
        >
            <Trash2 size={18} />
        </button>
    {/if}

    <div
            role={canViewDetails ? "button" : "none"}
            tabindex={canViewDetails ? 0 : -1}
            onclick={() => canViewDetails && onView?.()}
            onkeydown={(e) => canViewDetails && e.key === 'Enter' && onView?.()}
            class="p-6 pb-4 flex-1 flex flex-col {canViewDetails ? 'cursor-pointer' : ''}"
    >
        <div class="flex items-center gap-4 mb-5">
            <div class="w-14 h-14 bg-[#1B4B6B]/10 text-[#1B4B6B] rounded-2xl flex items-center justify-center font-black text-lg group-hover:bg-[#1B4B6B] group-hover:text-white transition-all shrink-0">
                <Building2 size={24} />
            </div>
            <div class="pr-6 overflow-hidden">
                <h3 class="font-extrabold text-[#1B4B6B] text-lg uppercase leading-tight truncate" title={ragioneSociale}>
                    {ragioneSociale}
                </h3>
                <p class="text-[10px] font-bold uppercase text-gray-400 mt-0.5 truncate flex items-center gap-1">
                    {#if hasDipendenti}
                        <Users size={10} /> Con Personale
                    {:else}
                        <User size={10} /> Individuale
                    {/if}
                    {#if idUtente}
                        <span class="ml-1 opacity-50">| ID: #{idUtente}</span>
                    {/if}
                </p>
            </div>
        </div>

        {#if partitaIva}
            <div class="mb-5 flex items-center justify-between text-[10px] font-bold uppercase bg-gray-50 p-2.5 rounded-xl border border-gray-100">
                <span class="text-gray-400 flex items-center gap-1"><FileDigit size={12}/> P. Iva</span>
                <span class="text-[#1B4B6B] font-mono tracking-wider">{partitaIva}</span>
            </div>
        {/if}

        <div class="mt-auto pt-2">
            {#if sedeLegale}
                <div class="flex items-center gap-1.5 mb-1.5">
                    <MapPin size={12} class="text-[#1B4B6B] shrink-0" />
                    <span class="text-[9px] font-bold uppercase text-[#1B4B6B] truncate" title={sedeLegale}>
                        {sedeLegale}
                    </span>
                </div>
            {:else}
                <div class="flex items-center gap-1.5 opacity-60">
                    <MapPin size={12} class="text-gray-400 shrink-0" />
                    <p class="text-[9px] font-bold uppercase text-gray-400 italic">Sede non specificata</p>
                </div>
            {/if}
        </div>
    </div>

    <div class="p-4 border-t border-gray-50 bg-gray-50/50 flex items-center justify-between gap-2">
        <div class="flex gap-2">
        </div>

        {#if canViewDetails}
            <button
                    onclick={(e) => { e.stopPropagation(); onView?.(); }}
                    class="flex items-center gap-1 text-[10px] font-black uppercase text-[#1B4B6B] hover:gap-2 transition-all px-2 ml-auto"
            >
                Apri Dettagli <ChevronRight size={14} />
            </button>
        {/if}
    </div>
</div>