<script lang="ts">
    import { FileText, Download, CheckCircle2, Clock, AlertTriangle, Trash2, Calendar, Building2 } from 'lucide-svelte';

    export type DocStato = 'OK' | 'WARNING' | 'DANGER' | 'INFO';
    export type RuoloUtente = 'dipendente' | 'azienda' | 'admin';

    export interface DocInfo {
        id: number | string;
        titolo: string;
        sottotitolo?: string;
        stato: DocStato;
        dataScadenza?: string;
        dataCaricamento?: string;
    }

    interface Props {
        documento: DocInfo;
        ruolo?: RuoloUtente;
        onDownload?: (id: number | string) => void;
        onDelete?: (id: number | string) => void;
        onManage?: (id: number | string) => void;
    }

    let { documento, ruolo = 'dipendente', onDownload, onDelete, onManage }: Props = $props();

    const getStatoConfig = (stato: DocStato) => {
        switch (stato) {
            case 'OK': return { label: 'Valido', text: 'text-emerald-700', bg: 'bg-emerald-50', border: 'border-emerald-200', bar: 'bg-emerald-500', icon: CheckCircle2 };
            case 'WARNING': return { label: 'In Scadenza', text: 'text-orange-600', bg: 'bg-orange-50', border: 'border-orange-200', bar: 'bg-orange-500', icon: Clock };
            case 'DANGER': return { label: 'Scaduto', text: 'text-red-700', bg: 'bg-red-50', border: 'border-red-200', bar: 'bg-red-500', icon: AlertTriangle };
            case 'INFO':
            default: return { label: 'Informativo', text: 'text-blue-700', bg: 'bg-blue-50', border: 'border-blue-200', bar: 'bg-blue-500', icon: FileText };
        }
    };

    let sc = $derived(getStatoConfig(documento.stato));
</script>

<div class="group flex h-full flex-col overflow-hidden rounded-[2rem] border border-gray-100 bg-white shadow-sm transition-all duration-300 hover:shadow-lg relative">
    <div class="absolute top-0 left-0 w-full h-1.5 {sc.bar}"></div>

    <div class="relative flex items-start gap-3 p-5 pb-2 mt-1">
        <div class="flex h-10 w-10 shrink-0 items-center justify-center rounded-xl bg-[#1B4B6B]/10 text-[#1B4B6B]">
            <FileText size={18} />
        </div>
        <div class="flex-1 min-w-0">
            <h3 class="text-[14px] font-black uppercase leading-[1.2] text-[#1B4B6B] break-words" style="hyphens: auto;" title={documento.titolo}>
                {documento.titolo}
            </h3>
            {#if documento.sottotitolo}
                <p class="mt-1 text-[9px] font-black uppercase text-gray-400 truncate" title={documento.sottotitolo}>{documento.sottotitolo}</p>
            {/if}
        </div>
    </div>

    <div class="flex flex-1 flex-col justify-start px-5 pb-5 mt-3">
        <div class="mb-4 inline-flex items-center gap-1.5 self-start rounded-lg border px-2.5 py-1 {sc.bg} {sc.border} {sc.text}">
            <svelte:component this={sc.icon} size={12} />
            <span class="text-[9px] font-black uppercase tracking-wider">{sc.label}</span>
        </div>

        <div class="space-y-2">
            {#if documento.dataScadenza}
                <div class="flex items-center gap-2 text-[10px] font-bold uppercase {documento.stato === 'DANGER' ? 'text-red-500' : 'text-[#1B4B6B]'}">
                    <Calendar size={13} class="shrink-0 {documento.stato === 'DANGER' ? 'text-red-500' : 'text-[#1B4B6B]'}" />
                    <span class="truncate">Scadenza: {documento.dataScadenza}</span>
                </div>
            {/if}
            {#if documento.dataCaricamento}
                <div class="flex items-center gap-2 text-[10px] font-bold uppercase text-gray-400">
                    <CheckCircle2 size={13} class="shrink-0 text-gray-300" />
                    <span class="truncate">Caricato: {documento.dataCaricamento}</span>
                </div>
            {/if}
        </div>
    </div>

    <div class="mt-auto border-t border-gray-50 bg-gray-50/50 p-4 flex gap-2">
        {#if onManage}
            <button
                    onclick={() => onManage(documento.id)}
                    class="flex-[0.8] flex items-center justify-center gap-1.5 py-2.5 rounded-xl bg-white text-[#1B4B6B] border border-gray-200 text-[10px] font-black uppercase shadow-sm hover:bg-gray-50 transition-colors"
            >
                <Building2 size={14} /> Gestisci
            </button>
        {/if}

        {#if onDownload}
            <button
                    onclick={() => onDownload(documento.id)}
                    class="flex-1 flex items-center justify-center gap-2 py-2.5 rounded-xl bg-[#1B4B6B] text-white text-[10px] font-black uppercase shadow-sm hover:bg-[#153a54] transition-colors"
            >
                <Download size={14} /> Scarica
            </button>
        {/if}

        {#if onDelete && ruolo !== 'dipendente'}
            <button
                    onclick={() => onDelete(documento.id)}
                    class="p-2.5 rounded-xl bg-white text-red-600 border border-gray-200 hover:bg-red-50 hover:border-red-200 hover:text-red-700 transition-colors shrink-0 shadow-sm"
            >
                <Trash2 size={14} />
            </button>
        {/if}
    </div>
</div>