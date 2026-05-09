<script lang="ts">
    import { HardHat, AlertTriangle, CheckCircle2, ShieldAlert, PenBox, Trash2 } from 'lucide-svelte';

    interface Props {
        ruolo: 'azienda' | 'dipendente' | 'admin';
        dpi: {
            id: number | string;
            nome: string;
            matricola?: string;
            stato: 'OK' | 'WARNING' | 'DANGER';
            dataRevisione: string;
            dataConsegna?: string;
        };
        onModifica?: (id: number | string) => void;
        onElimina?: (id: number | string) => void;
    }

    let { ruolo, dpi, onModifica, onElimina }: Props = $props();

    let configStato = $derived({
        'OK': { colore: 'text-green-500', bg: 'bg-green-50', icona: CheckCircle2, label: 'REGOLARE' },
        'WARNING': { colore: 'text-amber-500', bg: 'bg-amber-50', icona: AlertTriangle, label: 'IN SCADENZA' },
        'DANGER': { colore: 'text-red-500', bg: 'bg-red-50', icona: ShieldAlert, label: 'SCADUTO' }
    }[dpi.stato]);
</script>

<div class="bg-white rounded-3xl p-6 border-2 {dpi.stato === 'DANGER' ? 'border-red-100 shadow-red-900/5' : 'border-gray-50 hover:border-gray-100'} shadow-sm hover:shadow-xl transition-all group flex flex-col h-full relative overflow-hidden">

    {#if dpi.stato === 'DANGER'}
        <div class="absolute top-0 left-0 w-full h-1 bg-red-500"></div>
    {/if}
    {#if dpi.stato === 'WARNING'}
        <div class="absolute top-0 left-0 w-full h-1 bg-amber-400"></div>
    {/if}

    <div class="flex justify-between items-start mb-5">
        <div class="bg-gray-50 p-3 rounded-2xl text-gray-400 group-hover:bg-[#1B4B6B] group-hover:text-white transition-colors">
            <HardHat size={24} />
        </div>

        <div class="flex items-center gap-2">
            <span class="{configStato.bg} {configStato.colore} flex items-center gap-1.5 px-3 py-1.5 rounded-lg text-[9px] font-black tracking-widest">
                <svelte:component this={configStato.icona} size={12} />
                {configStato.label}
            </span>

            {#if ruolo === 'azienda' || ruolo === 'admin'}
                <button
                        onclick={() => onElimina && onElimina(dpi.id)}
                        class="p-1.5 text-gray-300 hover:text-red-500 hover:bg-red-50 rounded-lg transition-colors"
                        title="Elimina DPI"
                >
                    <Trash2 size={16} />
                </button>
            {/if}
        </div>
    </div>

    <div class="flex-1">
        <h3 class="text-lg font-black text-[#1B4B6B] uppercase tracking-tighter leading-tight mb-1">{dpi.nome}</h3>
        {#if dpi.matricola}
            <p class="text-[10px] font-bold text-gray-400 uppercase tracking-widest">
                <span class="opacity-50">Note:</span> {dpi.matricola}
            </p>
        {/if}
    </div>

    <div class="mt-6 pt-5 border-t border-gray-50 grid grid-cols-2 gap-4">
        <div>
            <p class="text-[9px] font-black text-gray-300 uppercase tracking-widest mb-0.5">Consegna</p>
            <p class="text-xs font-bold text-gray-600 uppercase tracking-tighter">{dpi.dataConsegna || 'N.D.'}</p>
        </div>
        <div>
            <p class="text-[9px] font-black text-gray-300 uppercase tracking-widest mb-0.5">Revisione</p>
            <p class="text-xs font-bold {dpi.stato === 'DANGER' ? 'text-red-500' : 'text-[#1B4B6B]'} uppercase tracking-tighter">{dpi.dataRevisione}</p>
        </div>
    </div>

    {#if ruolo === 'azienda' || ruolo === 'admin'}
        <button
                onclick={() => onModifica && onModifica(dpi.id)}
                class="mt-5 w-full bg-gray-50 hover:bg-[#1B4B6B] text-gray-400 hover:text-white py-3 rounded-xl flex items-center justify-center gap-2 text-[10px] font-black uppercase tracking-widest transition-all"
        >
            <PenBox size={14} /> Aggiorna Revisione
        </button>
    {/if}
</div>