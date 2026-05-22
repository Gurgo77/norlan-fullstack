<script lang="ts">
    /**
     * Componente Svelte per la visualizzazione a card di documenti, DPI o attestati.
     * Calcola dinamicamente lo stato (scaduto, in scadenza, valido) in base alla data
     * e mostra le informazioni principali insieme a una pulsantiera per le azioni rapide.
     */
    import { fade } from 'svelte/transition';
    import {
        FileText, ShieldCheck, Award, Download,
        Trash2, Edit3, RefreshCw, Calendar, AlertTriangle,
        CheckCircle, Clock, AlertCircle
    } from 'lucide-svelte';

    let {
        tipo = 'DOCUMENTO',
        titolo,
        sottotitolo,
        dataScadenza,
        dataSecondaria,
        labelDataSecondaria = 'Consegna',
        onDownload,
        onDelete,
        onEdit,
        onUpdate
    } = $props<{
        tipo: 'DOCUMENTO' | 'DPI' | 'ATTESTATO';
        titolo: string;
        sottotitolo?: string;
        dataScadenza?: string;
        dataSecondaria?: string;
        labelDataSecondaria?: string;
        onDownload?: () => void;
        onDelete?: () => void;
        onEdit?: () => void;
        onUpdate?: () => void;
    }>();
    // Funzione helper che determina lo stile e il testo del badge di stato in base ai giorni mancanti alla scadenza
    function getConfigStato(data?: string) {
        if (!data || data === '9999-12-31') return { colore: 'text-green-600', bg: 'bg-green-50', border: 'border-green-100', label: 'Valido', icona: CheckCircle, peso: 3 };

        const oggi = new Date();
        const scad = new Date(data);
        const diff = Math.ceil((scad.getTime() - oggi.getTime()) / (1000 * 3600 * 24));

        if (diff < 0) return { colore: 'text-red-600', bg: 'bg-red-50', border: 'border-red-200', label: 'Scaduto', icona: AlertCircle, peso: 1 };
        if (diff <= 30) return { colore: 'text-yellow-600', bg: 'bg-yellow-50', border: 'border-yellow-200', label: `In scadenza`, icona: Clock, peso: 2 };

        return { colore: 'text-green-600', bg: 'bg-green-50', border: 'border-green-100', label: 'Valido', icona: CheckCircle, peso: 3 };
    }
    // Variabili derivate e reattive: si aggiornano automaticamente se cambiano le date o la tipologia del documento
    const stato = $derived(getConfigStato(dataScadenza));
    const IconaStato = $derived(stato.icona);
    const IconaTipo = $derived(tipo === 'DPI' ? ShieldCheck : tipo === 'ATTESTATO' ? Award : FileText);
</script>
<!-- Contenitore animato della card: lo sfondo diventa rosso se il documento è scaduto (peso === 1) -->
<div class="p-5 rounded-2xl border transition-all {stato.peso === 1 ? 'bg-red-50/30 border-red-200' : 'bg-white border-gray-100 hover:shadow-md'}" in:fade>
    <div class="flex justify-between items-start mb-3">
        <div class="flex items-center gap-3">
            <div class="p-2 rounded-xl {stato.bg} {stato.colore}">
                <IconaTipo size={18} />
            </div>
            <div class="overflow-hidden pr-2">
                <h4 class="font-extrabold uppercase text-xs leading-tight truncate {stato.peso === 1 ? 'text-red-700' : 'text-[#1B4B6B]'}">{titolo}</h4>
                {#if sottotitolo}
                    <p class="text-[9px] text-gray-400 font-bold uppercase mt-0.5 truncate">{sottotitolo}</p>
                {/if}
            </div>
        </div>
        <!-- Pulsantiera orizzontale in alto a destra: renderizza dinamicamente i bottoni solo se la relativa prop è definita -->
        <div class="flex gap-1 shrink-0">
            {#if onDownload}
                <button onclick={onDownload} class="p-1.5 text-gray-400 hover:text-[#1B4B6B] transition-all bg-white rounded-lg shadow-sm border border-gray-50"><Download size={14} /></button>
            {/if}

            {#if onUpdate}
                <button onclick={onUpdate} class="p-1.5 text-gray-400 hover:text-blue-600 transition-all bg-white rounded-lg shadow-sm border border-gray-50" title="Aggiorna revisione">
                    <RefreshCw size={14} />
                </button>
            {/if}

            {#if onEdit}
                <button onclick={onEdit} class="p-1.5 text-gray-400 hover:text-blue-600 transition-all bg-white rounded-lg shadow-sm border border-gray-50"><Edit3 size={14} /></button>
            {/if}

            {#if onDelete}
                <button onclick={onDelete} class="p-1.5 text-gray-400 hover:text-red-600 transition-all bg-white rounded-lg shadow-sm border border-gray-50"><Trash2 size={14} /></button>
            {/if}
        </div>
    </div>
    <!-- Sezione inferiore della card dedicata ai metadati (data consegna/scadenza) e al badge di stato -->
    <div class="flex flex-col gap-2 pt-3 border-t {stato.peso === 1 ? 'border-red-100' : 'border-gray-50'}">
        {#if dataSecondaria}
            <div class="flex items-center gap-1.5 text-gray-400">
                <Calendar size={10} />
                <span class="text-[9px] font-bold uppercase">{labelDataSecondaria}: {new Date(dataSecondaria).toLocaleDateString()}</span>
            </div>
        {/if}
        <div class="flex items-center justify-between">
            {#if tipo !== 'ATTESTATO'}
                <div class="flex items-center gap-1.5 {stato.peso === 1 ? 'text-red-600 font-black' : 'text-gray-400'}">
                    <AlertTriangle size={10} />
                    <span class="text-[9px] font-bold uppercase">
                {dataScadenza && dataScadenza !== '9999-12-31' ? `Scad: ${new Date(dataScadenza).toLocaleDateString()}` : 'Senza Scadenza'}
            </span>
                </div>
            {/if}

            <div class="inline-flex items-center gap-1 px-2 py-0.5 rounded-md border {stato.border} {stato.bg} {stato.colore}">
                <IconaStato size={10} />
                <span class="text-[8px] font-black uppercase tracking-wider">{stato.label}</span>
            </div>
        </div>
    </div>
</div>