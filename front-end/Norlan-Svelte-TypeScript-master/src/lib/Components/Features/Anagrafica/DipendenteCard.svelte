<script lang="ts">
    import {
        Trash2,
        Edit3,
        MessageSquare,
        ChevronRight,
        BookOpen,
        IdCard,
        Building2
    } from 'lucide-svelte';

    interface CorsoBreve {
        idCorso: number;
        titolo: string;
        stato?: string;
    }

    interface Props {
        idUtente?: number;
        nome: string;
        cognome: string;
        ruolo?: string;
        codiceFiscale?: string;
        azienda?: string;
        corsi?: CorsoBreve[];
        canEdit?: boolean;
        canDelete?: boolean;
        canContact?: boolean;
        canViewDetails?: boolean;
        onEdit?: () => void;
        onDelete?: () => void;
        onContact?: () => void;
        onViewDetails?: () => void;
    }

    let {
        idUtente,
        nome,
        cognome,
        ruolo = 'Dipendente',
        codiceFiscale = '',
        azienda = '',
        corsi = [],
        canEdit = false,
        canDelete = false,
        canContact = true,
        canViewDetails = true,
        onEdit,
        onDelete,
        onContact,
        onViewDetails
    }: Props = $props();

    let iniziali = $derived(`${nome?.[0] || ''}${cognome?.[0] || ''}`.toUpperCase());
</script>

<div class="bg-white rounded-3xl border border-gray-100 shadow-sm hover:shadow-xl transition-all group relative flex flex-col h-full overflow-hidden">

    {#if canDelete}
        <button
                onclick={(e) => { e.stopPropagation(); onDelete?.(); }}
                class="absolute top-4 right-4 p-2 text-gray-300 hover:text-red-600 opacity-0 group-hover:opacity-100 transition-all z-10 hover:bg-red-50 rounded-lg"
                title="Rimuovi dipendente"
        >
            <Trash2 size={18} />
        </button>
    {/if}

    <div
            role={canViewDetails ? "button" : "none"}
            tabindex={canViewDetails ? 0 : -1}
            onclick={() => canViewDetails && onViewDetails?.()}
            onkeydown={(e) => canViewDetails && e.key === 'Enter' && onViewDetails?.()}
            class="p-6 pb-4 flex-1 flex flex-col {canViewDetails ? 'cursor-pointer' : ''}"
    >
        <div class="flex items-center gap-4 mb-5">
            <div class="w-14 h-14 bg-[#1B4B6B]/10 text-[#1B4B6B] rounded-2xl flex items-center justify-center font-black text-lg group-hover:bg-[#1B4B6B] group-hover:text-white transition-all shrink-0">
                {iniziali}
            </div>
            <div class="pr-6 overflow-hidden">
                <h3 class="font-extrabold text-[#1B4B6B] text-lg uppercase leading-tight truncate" title="{nome} {cognome}">
                    {nome} {cognome}
                </h3>
                <p class="text-[10px] font-bold uppercase text-gray-400 mt-0.5 truncate">
                    {ruolo}
                    {#if idUtente}
                        <span class="ml-1 opacity-50">| ID: #{idUtente}</span>
                    {/if}
                </p>
                {#if azienda}
                    <div class="flex items-center gap-1 text-gray-400 mt-1">
                        <Building2 size={10} class="text-[#1B4B6B]" />
                        <span class="text-[9px] font-bold uppercase truncate text-[#1B4B6B]">{azienda}</span>
                    </div>
                {/if}
            </div>
        </div>

        {#if codiceFiscale}
            <div class="mb-5 flex items-center justify-between text-[10px] font-bold uppercase bg-gray-50 p-2.5 rounded-xl border border-gray-100">
                <span class="text-gray-400 flex items-center gap-1"><IdCard size={12}/> C. Fiscale</span>
                <span class="text-[#1B4B6B] font-mono tracking-wider">{codiceFiscale}</span>
            </div>
        {/if}

        <div class="mt-auto pt-2">
            {#if corsi.length > 0}
                <div class="flex items-center gap-1.5 mb-2.5">
                    <BookOpen size={12} class="text-[#1B4B6B]" />
                    <span class="text-[9px] font-black uppercase text-[#1B4B6B] tracking-widest">Corsi Attuali ({corsi.length})</span>
                </div>
                <div class="flex flex-wrap gap-1.5">
                    {#each corsi.slice(0, 2) as corso}
                        <span class="text-[9px] font-bold uppercase px-2 py-1 bg-blue-50 text-blue-700 border border-blue-100 rounded-md truncate max-w-[140px]" title={corso.titolo}>
                            {corso.titolo}
                        </span>
                    {/each}
                    {#if corsi.length > 2}
                        <span class="text-[9px] font-bold uppercase px-2 py-1 bg-gray-100 text-gray-500 border border-gray-200 rounded-md">
                            +{corsi.length - 2}
                        </span>
                    {/if}
                </div>
            {:else}
                <div class="flex items-center gap-1.5 opacity-60">
                    <BookOpen size={12} class="text-gray-400" />
                    <p class="text-[9px] font-bold uppercase text-gray-400 italic">Nessun corso attivo assegnato</p>
                </div>
            {/if}
        </div>
    </div>

    <div class="p-4 border-t border-gray-50 bg-gray-50/50 flex items-center justify-between gap-2">
        <div class="flex gap-2">
            {#if canEdit}
                <button
                        onclick={(e) => { e.stopPropagation(); onEdit?.(); }}
                        class="p-2 text-gray-400 hover:text-[#1B4B6B] hover:bg-white rounded-lg transition-all border border-transparent hover:border-gray-200 shadow-sm hover:shadow-md"
                        title="Modifica Dati"
                >
                    <Edit3 size={16} />
                </button>
            {/if}
            {#if canContact}
                <button
                        onclick={(e) => { e.stopPropagation(); onContact?.(); }}
                        class="p-2 text-gray-400 hover:text-[#1B4B6B] hover:bg-white rounded-lg transition-all border border-transparent hover:border-gray-200 shadow-sm hover:shadow-md"
                        title="Invia Messaggio"
                >
                    <MessageSquare size={16} />
                </button>
            {/if}
        </div>

        {#if canViewDetails}
            <button
                    onclick={(e) => { e.stopPropagation(); onViewDetails?.(); }}
                    class="flex items-center gap-1 text-[10px] font-black uppercase text-[#1B4B6B] hover:gap-2 transition-all px-2"
            >
                Apri Profilo <ChevronRight size={14} />
            </button>
        {/if}
    </div>
</div>