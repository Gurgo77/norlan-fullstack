<script lang="ts">
    import { HardHat, CheckCircle2, Clock, AlertTriangle, Calendar, RefreshCw, ShieldAlert, Edit3, Mail, MessageSquare } from 'lucide-svelte';

    export type DpiStato = 'OK' | 'WARNING' | 'DANGER';
    export type RuoloUtente = 'dipendente' | 'azienda' | 'admin';

    export interface DpiInfo {
        id: number | string;
        nome: string;
        matricola?: string;
        stato: DpiStato;
        dataRevisione?: string;
        dataConsegna?: string;
    }

    interface Props {
        dpi: DpiInfo;
        ruolo?: RuoloUtente;
        onRichiediSostituzione?: (id: number | string) => void;
        onModifica?: (id: number | string) => void;
        onEmail?: (id: number | string) => void;
        onChat?: (id: number | string) => void;
    }

    let { dpi, ruolo = 'dipendente', onRichiediSostituzione, onModifica, onEmail, onChat }: Props = $props();

    const getStatoConfig = (stato: DpiStato) => {
        switch (stato) {
            case 'OK':
                return { label: 'Conforme', text: 'text-emerald-700', bg: 'bg-emerald-50', border: 'border-emerald-200', icon: CheckCircle2 };
            case 'WARNING':
                return { label: 'In Scadenza', text: 'text-amber-700', bg: 'bg-amber-50', border: 'border-amber-200', icon: Clock };
            case 'DANGER':
                return { label: 'Scaduto/Anomalo', text: 'text-red-700', bg: 'bg-red-50', border: 'border-red-200', icon: AlertTriangle };
            default:
                return { label: 'Sconosciuto', text: 'text-gray-700', bg: 'bg-gray-50', border: 'border-gray-200', icon: ShieldAlert };
        }
    };

    let sc = $derived(getStatoConfig(dpi.stato));
</script>

<div class="group flex h-full flex-col overflow-hidden rounded-3xl border border-gray-100 bg-white shadow-sm transition-all duration-300 hover:-translate-y-1 hover:border-[#1B4B6B]/30 hover:shadow-xl relative">
    {#if dpi.stato === 'DANGER'}
        <div class="absolute top-0 left-0 w-full h-1 bg-red-500"></div>
    {/if}

    <div class="relative flex items-start gap-4 p-6 pb-4">
        <div class="flex h-12 w-12 shrink-0 items-center justify-center rounded-2xl bg-[#1B4B6B]/10 text-[#1B4B6B] transition-colors group-hover:bg-[#1B4B6B] group-hover:text-white">
            <HardHat size={20} />
        </div>
        <div class="flex-1 pr-2">
            <h3 class="line-clamp-2 text-lg font-extrabold uppercase leading-tight text-[#1B4B6B] break-all" style="hyphens: auto;" title={dpi.nome}>{dpi.nome}</h3>
            {#if ruolo !== 'admin' && dpi.matricola}
                <p class="mt-1 text-[10px] font-black uppercase text-gray-400">MATRICOLA: {dpi.matricola}</p>
            {/if}
        </div>
    </div>

    <div class="flex flex-1 flex-col justify-start px-6 pb-4">
        <div class="mb-4 inline-flex items-center gap-1.5 self-start rounded-lg border px-3 py-1.5 {sc.bg} {sc.border} {sc.text}">
            <svelte:component this={sc.icon} size={14} />
            <span class="text-[10px] font-black uppercase tracking-wider">{sc.label}</span>
        </div>

        <div class="space-y-2 mb-2">
            {#if dpi.dataRevisione}
                <div class="flex items-center gap-2 text-[10px] font-bold uppercase {dpi.stato === 'DANGER' ? 'text-red-500' : 'text-gray-500'}">
                    <Calendar size={14} class={dpi.stato === 'DANGER' ? 'text-red-500' : 'text-[#1B4B6B] shrink-0'} />
                    <span class="truncate">Scadenza: {dpi.dataRevisione}</span>
                </div>
            {/if}
            {#if dpi.dataConsegna}
                <div class="flex items-center gap-2 text-[10px] font-bold uppercase text-gray-400">
                    <CheckCircle2 size={14} class="text-gray-300 shrink-0" />
                    <span class="truncate">Consegnato: {dpi.dataConsegna}</span>
                </div>
            {/if}
        </div>
    </div>

    {#if ruolo !== 'admin'}
        <div class="mt-auto flex items-center justify-between gap-2 border-t border-gray-50 bg-gray-50/50 p-4">
            {#if ruolo === 'dipendente'}
                {#if dpi.stato === 'DANGER' || dpi.stato === 'WARNING'}
                    <button onclick={() => onRichiediSostituzione?.(dpi.id)} class="flex flex-1 items-center justify-center gap-2 rounded-xl border border-red-200 bg-red-50 px-4 py-2.5 text-[10px] font-bold uppercase text-red-600 transition-all hover:bg-red-100 hover:border-red-300">
                        <RefreshCw size={14} /> Richiedi Sostituzione
                    </button>
                {:else}
                    <div class="flex flex-1 items-center justify-center gap-2 rounded-xl border border-dashed border-gray-200 bg-white px-4 py-2.5 text-[10px] font-bold uppercase text-gray-400">
                        <CheckCircle2 size={14} /> DPI Regolare
                    </div>
                {/if}
            {/if}

            {#if ruolo === 'azienda'}
                <button onclick={() => onModifica?.(dpi.id)} class="flex flex-1 items-center justify-center gap-2 rounded-xl bg-[#1B4B6B] px-4 py-2.5 text-[10px] font-bold uppercase text-white shadow-md transition-all hover:bg-[#1B4B6B]/90">
                    <Edit3 size={14} /> Aggiorna Revisione
                </button>
            {/if}
        </div>
    {/if}
</div>