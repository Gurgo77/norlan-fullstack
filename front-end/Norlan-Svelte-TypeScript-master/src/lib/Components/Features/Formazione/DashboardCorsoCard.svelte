<script lang="ts">
    /**
     * Componente Svelte per la visualizzazione a card di un corso di formazione.
     * Renderizza dinamicamente informazioni (stato, date, luogo) e un pannello di azioni
     * diversificato in base al ruolo dell'utente (docente, admin, dipendente o azienda) e allo stato del corso.
     */
    import {
        BookOpen,
        Download,
        CheckCircle,
        Clock,
        PlayCircle,
        UserCheck,
        Calendar,
        MapPin,
        FileText,
        Trash2,
        BarChart,
        CheckSquare,
        UploadCloud
    } from 'lucide-svelte';

    export type StatoCorso = 'DA_INIZIARE' | 'IN_SVOLGIMENTO' | 'COMPLETATO';
    export type RuoloUtente = 'dipendente' | 'docente' | 'azienda' | 'admin';

    export interface DashboardCorso {
        id: number | string;
        titolo: string;
        stato: StatoCorso;
        luogo?: string;
        dataSvolgimento?: string;
        materiali?: { id: number; titolo: string; path?: string }[];
        numeroIscritti?: number;
    }

    interface Props {
        corso: DashboardCorso;
        ruolo: RuoloUtente;
        onValidaPresenze?: () => void;
        onFirmaRegistro?: () => void;
        onAzioneCorso?: () => void;
        onAnnullaIscrizione?: () => void;
        onEliminaCorso?: () => void;
        onDownloadAttestato?: () => void;
        onConcludiCorso?: () => void;
        onDownloadMateriale?: (id: number) => void;
    }

    // eslint-disable-next-line @typescript-eslint/no-unused-vars
    let {
        corso,
        ruolo,
        onValidaPresenze,
        onFirmaRegistro,
        onAzioneCorso,
        onAnnullaIscrizione,
        onEliminaCorso,
        onDownloadAttestato,
        onConcludiCorso,
        onDownloadMateriale
    }: Props = $props();
    // Funzione helper che mappa lo stato attuale del corso a un set di stili (colori, background), icone e testi predefiniti per l'interfaccia
    const getStatoConfig = (stato: StatoCorso) => {
        switch (stato) {
            case 'COMPLETATO':
                return { label: 'Completato', text: 'text-emerald-700', bg: 'bg-emerald-50', border: 'border-emerald-200', icon: CheckCircle };
            case 'IN_SVOLGIMENTO':
                return { label: 'In Svolgimento', text: 'text-amber-700', bg: 'bg-amber-50', border: 'border-amber-200', icon: Clock };
            case 'DA_INIZIARE':
            default:
                return { label: 'In Programma', text: 'text-blue-700', bg: 'bg-blue-50', border: 'border-blue-200', icon: Calendar };
        }
    };

    let sc = $derived(getStatoConfig(corso.stato));
</script>

<!-- Contenitore principale della card con effetti di transizione all'hover e layout strutturato a colonna -->
<div id="corso-{corso.id}" class="group flex h-full flex-col overflow-hidden rounded-3xl border border-gray-100 bg-white shadow-sm transition-all duration-300 hover:-translate-y-1 hover:border-[#1B4B6B]/30 hover:shadow-xl">
    <div class="relative flex items-start gap-4 p-6 pb-4">
        <div class="flex h-12 w-12 shrink-0 items-center justify-center rounded-2xl bg-[#1B4B6B]/10 text-[#1B4B6B] transition-colors group-hover:bg-[#1B4B6B] group-hover:text-white">
            <BookOpen size={20} />
        </div>
        <div class="flex-1 pr-2">
            <h3 class="line-clamp-2 text-lg font-extrabold uppercase leading-tight text-[#1B4B6B]" title={corso.titolo}>{corso.titolo}</h3>
        </div>
    </div>

    <div class="flex flex-1 flex-col justify-start px-6 pb-4">
        <div class="mb-4 inline-flex items-center gap-1.5 self-start rounded-lg border px-3 py-1.5 {sc.bg} {sc.border} {sc.text}">
            <svelte:component this={sc.icon} size={14} />
            <span class="text-[10px] font-black uppercase tracking-wider">{sc.label}</span>
        </div>

        <div class="mb-4 space-y-2">
            {#if corso.dataSvolgimento}
                <div class="flex items-center gap-2 text-[10px] font-bold uppercase text-gray-500">
                    <Calendar size={14} class="text-[#1B4B6B] shrink-0" />
                    <span class="truncate">
                        {#if corso.stato === 'COMPLETATO'}CONSEGUITO IL:{:else}DATA DI INIZIO:{/if} {corso.dataSvolgimento}
                    </span>
                </div>
            {/if}
            {#if corso.luogo}
                <div class="flex items-center gap-2 text-[10px] font-bold uppercase text-gray-500">
                    <MapPin size={14} class="text-[#1B4B6B] shrink-0" />
                    <span class="truncate">{corso.luogo}</span>
                </div>
            {/if}
        </div>
    </div>

    <div class="mt-auto flex items-center justify-between gap-2 border-t border-gray-50 bg-gray-50/50 p-4">
        <!-- Pannello delle azioni specifico per il ruolo "docente": gestisce l'avvio del corso, il caricamento materiali, la firma e la conclusione -->
        {#if ruolo === 'docente'}
            {#if corso.stato === 'DA_INIZIARE'}
                <div class="flex w-full gap-2">
                    <button
                            onclick={onAzioneCorso}
                            disabled={corso.numeroIscritti === 0}
                            class="flex flex-1 items-center justify-center gap-2 rounded-xl bg-blue-50 border border-blue-100 px-4 py-2.5 text-[10px] font-black uppercase text-blue-600 shadow-sm transition-all hover:bg-blue-100 disabled:opacity-50 disabled:cursor-not-allowed">
                        <PlayCircle size={14} />
                        {corso.numeroIscritti === 0 ? 'Nessun Iscritto' : 'Avvia Corso'}
                    </button>
                </div>
            {:else if corso.stato === 'IN_SVOLGIMENTO'}
                <div class="flex w-full gap-2">
                    <button
                            onclick={onAzioneCorso}
                            class="flex flex-1 items-center justify-center gap-2 rounded-xl bg-[#1B4B6B] px-4 py-2.5 text-[10px] font-black uppercase text-white shadow-md transition-all hover:bg-[#1B4B6B]/90 hover:shadow-lg">
                        <UploadCloud size={14} /> Materiale
                    </button>
                    {#if onConcludiCorso}
                        <button
                                onclick={onConcludiCorso}
                                disabled={corso.numeroIscritti === 0}
                                class="flex flex-1 items-center justify-center gap-2 rounded-xl bg-emerald-600 px-4 py-2.5 text-[10px] font-black uppercase text-white shadow-md transition-all hover:bg-emerald-700 hover:shadow-lg disabled:opacity-50 disabled:cursor-not-allowed">
                            <CheckCircle size={14} /> Concludi
                        </button>
                    {/if}
                </div>
            {:else if corso.stato === 'COMPLETATO'}
                <div class="flex w-full gap-2">
                    {#if onFirmaRegistro}
                        <button onclick={onFirmaRegistro} class="flex-1 flex items-center justify-center gap-2 rounded-xl border border-gray-200 bg-white px-4 py-2.5 text-[10px] font-black uppercase text-[#1B4B6B] shadow-sm transition-all hover:border-[#1B4B6B] hover:bg-[#1B4B6B] hover:text-white">
                            <CheckSquare size={14} /> Firma
                        </button>
                    {/if}
                    {#if onAzioneCorso}
                        <button onclick={onAzioneCorso} class="flex-1 flex items-center justify-center gap-2 rounded-xl border border-[#1B4B6B]/20 bg-[#1B4B6B]/5 px-4 py-2.5 text-[10px] font-black uppercase text-[#1B4B6B] transition-all hover:bg-[#1B4B6B]/10">
                            <BarChart size={14} /> Feedback
                        </button>
                    {/if}
                </div>
            {/if}
        {/if}

        <!-- Pannello delle azioni specifico per il ruolo "admin": permette l'eliminazione, la validazione presenze e la visualizzazione dei feedback -->
        {#if ruolo === 'admin'}
            <div class="flex w-full gap-2">
                {#if corso.stato === 'DA_INIZIARE'}
                    {#if onEliminaCorso}
                        <button onclick={onEliminaCorso} class="flex flex-1 items-center justify-center gap-2 rounded-xl border border-red-100 bg-red-50 px-4 py-2.5 text-[10px] font-black uppercase text-red-600 transition-all hover:bg-red-100 hover:border-red-200">
                            <Trash2 size={14} /> Elimina Corso
                        </button>
                    {:else}
                        <div class="flex flex-1 items-center justify-center gap-2 rounded-xl border border-dashed border-gray-200 bg-white px-4 py-2.5 text-[10px] font-black uppercase text-gray-400">
                            <Clock size={14} /> In Programmazione
                        </div>
                    {/if}
                {:else if corso.stato === 'COMPLETATO'}
                    {#if onValidaPresenze}
                        <button onclick={onValidaPresenze} class="flex flex-1 items-center justify-center gap-2 rounded-xl bg-[#1B4B6B] px-4 py-2.5 text-[10px] font-black uppercase text-white shadow-md transition-all hover:bg-[#1B4B6B]/90">
                            <UserCheck size={14} /> Valida Presenze
                        </button>
                    {:else if onAzioneCorso}
                        <button onclick={onAzioneCorso} class="flex flex-1 items-center justify-center gap-2 rounded-xl border border-[#1B4B6B]/20 bg-white px-4 py-2.5 text-[10px] font-black uppercase text-[#1B4B6B] transition-all hover:bg-[#1B4B6B]/5">
                            <BarChart size={14} /> Vedi Feedback
                        </button>
                    {/if}
                {:else if corso.stato === 'IN_SVOLGIMENTO'}
                    <div class="flex flex-1 items-center justify-center gap-2 rounded-xl border border-dashed border-blue-200 bg-blue-50 px-4 py-2.5 text-[10px] font-black uppercase text-blue-600">
                        <PlayCircle size={14} /> In Svolgimento
                    </div>
                {/if}
            </div>
        {/if}

        <!-- Pannello delle azioni per "dipendente" e "azienda": consente di consultare/scaricare materiali, attestati o annullare l'iscrizione -->
        {#if ruolo === 'dipendente' || ruolo === 'azienda'}
            <div class="flex w-full gap-2">
                {#if corso.stato === 'IN_SVOLGIMENTO'}
                    {#if corso.materiali && corso.materiali.length > 0}
                        <div class="mt-3 space-y-2">
                            {#each corso.materiali as m}
                                <button
                                        onclick={() => onDownloadMateriale?.(m.id)}
                                        class="flex w-full items-center justify-between rounded-lg bg-gray-50 p-2 text-[9px] font-bold uppercase text-[#1B4B6B] hover:bg-gray-100 transition-colors"
                                >
                                    <div class="flex items-center gap-2 truncate">
                                        <FileText size={12} />
                                        <span class="truncate">{m.titolo}</span>
                                    </div>
                                    <Download size={12} class="shrink-0" />
                                </button>
                            {/each}
                        </div>
                    {/if}
                {:else if corso.stato === 'COMPLETATO'}
                    <button
                            onclick={onDownloadAttestato}
                            class="flex flex-1 items-center justify-center gap-2 rounded-xl bg-emerald-600 px-4 py-2.5 text-[10px] font-black uppercase text-white shadow-md transition-all hover:bg-emerald-700 hover:shadow-lg"
                    >
                        <Download size={14} /> Scarica Attestato
                    </button>
                {:else if corso.stato === 'DA_INIZIARE'}
                    {#if ruolo === 'azienda' && onAnnullaIscrizione}
                        <button onclick={onAnnullaIscrizione} class="flex flex-1 items-center justify-center gap-2 rounded-xl border border-red-100 bg-red-50 px-4 py-2.5 text-[10px] font-black uppercase text-red-600 transition-all hover:bg-red-100 hover:border-red-200">
                            <Trash2 size={14} /> Annulla Iscrizione
                        </button>
                    {:else}
                        <div class="flex flex-1 items-center justify-center gap-2 rounded-xl border border-dashed border-gray-200 bg-white px-4 py-2.5 text-[10px] font-black uppercase text-gray-400">
                            <Clock size={14} /> In attesa di avvio
                        </div>
                    {/if}
                {/if}
            </div>
        {/if}
    </div>
</div>