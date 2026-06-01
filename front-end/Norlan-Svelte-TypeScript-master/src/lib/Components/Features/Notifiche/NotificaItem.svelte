<script lang="ts">
    import { Trash2, Info, AlertCircle } from 'lucide-svelte';
    import type { Notifica } from '$lib/models/Notifica';

    interface Props {
        notifica: Notifica;
        onElimina: (id: number) => void;
    }

    let { notifica, onElimina }: Props = $props();

    function formattaData(iso: string) {
        if (!iso) return '';
        return new Date(iso).toLocaleDateString('it-IT', { day: '2-digit', month: 'short', hour: '2-digit', minute: '2-digit' });
    }
</script>

<div class="p-4 border-b border-gray-50 hover:bg-gray-50/50 transition-colors flex gap-3 group relative bg-white">
    <div class="w-8 h-8 rounded-full bg-blue-50 text-blue-600 flex items-center justify-center shrink-0 mt-0.5">
        <!-- Usiamo AlertCircle se il messaggio contiene 'scadenza' -->
        {#if notifica.messaggio?.toLowerCase().includes('scadenza')}
            <AlertCircle size={16} />
        {:else}
            <Info size={16} />
        {/if}
    </div>

    <div class="flex-1 min-w-0 pr-2">
        <h4 class="text-xs font-black text-[#1B4B6B] uppercase truncate leading-tight mb-0.5">Notifica di Sistema</h4>
        <p class="text-[11px] text-gray-500 font-medium leading-snug line-clamp-2">{notifica.messaggio}</p>
        <span class="text-[9px] font-bold text-gray-400 uppercase tracking-widest mt-2 block">{formattaData(notifica.dataInvio)}</span>
    </div>

    <div class="flex flex-col shrink-0 border-l border-gray-100 pl-3 justify-center">
        <button
                onclick={(e) => { e.stopPropagation(); onElimina(notifica.idNotifica); }}
                class="text-red-400 hover:text-red-600 transition-colors bg-red-50 hover:bg-red-100 p-2 rounded-full"
                title="Elimina"
        >
            <Trash2 size={16} />
        </button>
    </div>
</div>