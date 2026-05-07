<script lang="ts">
    import { BookOpen, Download, CheckCircle, Clock, PlayCircle, UserCheck, Calendar, MapPin, FileText, Trash2, BarChart, CheckSquare } from 'lucide-svelte';

    export type StatoCorso = 'DA_INIZIARE' | 'IN_SVOLGIMENTO' | 'COMPLETATO';
    export type RuoloUtente = 'dipendente' | 'docente' | 'azienda' | 'admin';

    export interface DashboardCorso {
        id: number | string;
        titolo: string;
        stato: StatoCorso;
        avanzamento?: number;
        dataCompletamento?: string;
        luogo?: string;
        dataSvolgimento?: string;
        materiali?: { id: number; titolo: string }[];
    }

    interface Props {
        corso: DashboardCorso;
        ruolo: RuoloUtente;
        onDownloadMateriale?: (id: number, titolo: string) => void;
        onValidaPresenze?: () => void;
        onFirmaRegistro?: () => void;
        onAzioneCorso?: () => void;
        onAnnullaIscrizione?: () => void;
        onEliminaCorso?: () => void;
    }

    let { corso, ruolo, onDownloadMateriale, onValidaPresenze, onFirmaRegistro, onAzioneCorso, onAnnullaIscrizione, onEliminaCorso }: Props = $props();

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

        {#if corso.materiali && corso.materiali.length > 0}
            <div class="mt-2 border-t border-gray-50 pt-4">
                <p class="mb-2 text-[9px] font-black uppercase tracking-widest text-gray-400">Materiali Didattici</p>
                <div class="grid grid-cols-1 gap-2">
                    {#each corso.materiali as mat}
                        <button onclick={() => onDownloadMateriale?.(mat.id, mat.titolo)} class="flex w-full items-center justify-between rounded-xl border border-gray-100 bg-gray-50/50 p-2 transition-all hover:border-[#1B4B6B] hover:bg-white">
                            <div class="flex items-center gap-2 overflow-hidden">
                                <FileText size={12} class="shrink-0 text-[#1B4B6B]" />
                                <span class="truncate text-[9px] font-bold uppercase text-[#1B4B6B]">{mat.titolo}</span>
                            </div>
                            <Download size={12} class="text-gray-400 shrink-0" />
                        </button>
                    {/each}
                </div>
            </div>
        {/if}

        {#if (ruolo === 'dipendente' || ruolo === 'azienda') && corso.stato === 'IN_SVOLGIMENTO' && corso.avanzamento !== undefined}
            <div class="mt-4">
                <div class="mb-1.5 flex justify-between text-[10px] font-bold uppercase text-gray-500">
                    <span>Avanzamento</span>
                    <span class="text-[#1B4B6B]">{corso.avanzamento}%</span>
                </div>
                <div class="h-2 w-full overflow-hidden rounded-full bg-gray-100">
                    <div class="h-full bg-[#1B4B6B] transition-all duration-700 ease-out" style="width: {corso.avanzamento}%"></div>
                </div>
            </div>
        {/if}
    </div>

    <div class="mt-auto flex items-center justify-between gap-2 border-t border-gray-50 bg-gray-50/50 p-4">
        {#if ruolo === 'docente'}
            {#if corso.stato === 'IN_SVOLGIMENTO'}
                {#if onAzioneCorso}
                    <button onclick={onAzioneCorso} class="flex flex-1 items-center justify-center gap-2 rounded-xl bg-[#1B4B6B] px-4 py-2.5 text-[10px] font-bold uppercase text-white shadow-md transition-all hover:bg-[#1B4B6B]/90 hover:shadow-lg">
                        <PlayCircle size={14} /> Apri Corso
                    </button>
                {/if}
            {:else if corso.stato === 'COMPLETATO' && onFirmaRegistro}
                <button onclick={onFirmaRegistro} class="flex flex-1 items-center justify-center gap-2 rounded-xl border border-gray-200 bg-white px-4 py-2.5 text-[10px] font-bold uppercase text-[#1B4B6B] shadow-sm transition-all hover:border-[#1B4B6B] hover:bg-[#1B4B6B] hover:text-white">
                    <CheckSquare size={14} /> Firma Registro
                </button>
            {/if}
        {/if}

        {#if ruolo === 'admin'}
            {#if corso.stato === 'DA_INIZIARE'}
                {#if onEliminaCorso}
                    <button onclick={onEliminaCorso} class="flex flex-1 items-center justify-center gap-2 rounded-xl border border-red-100 bg-red-50 px-4 py-2.5 text-[10px] font-bold uppercase text-red-600 transition-all hover:bg-red-100 hover:border-red-200">
                        <Trash2 size={14} /> Elimina Corso
                    </button>
                {:else}
                    <div class="flex flex-1 items-center justify-center gap-2 rounded-xl border border-dashed border-gray-200 bg-white px-4 py-2.5 text-[10px] font-bold uppercase text-gray-400">
                        <Clock size={14} /> In Programmazione
                    </div>
                {/if}
            {:else if corso.stato === 'COMPLETATO'}
                {#if onValidaPresenze}
                    <button onclick={onValidaPresenze} class="flex flex-1 items-center justify-center gap-2 rounded-xl bg-[#1B4B6B] px-4 py-2.5 text-[10px] font-bold uppercase text-white shadow-md transition-all hover:bg-[#1B4B6B]/90">
                        <UserCheck size={14} /> Valida Presenze
                    </button>
                {:else if onAzioneCorso}
                    <button onclick={onAzioneCorso} class="flex flex-1 items-center justify-center gap-2 rounded-xl border border-[#1B4B6B]/20 bg-white px-4 py-2.5 text-[10px] font-bold uppercase text-[#1B4B6B] transition-all hover:bg-[#1B4B6B]/5">
                        <BarChart size={14} /> Vedi Feedback
                    </button>
                {/if}
            {:else if corso.stato === 'IN_SVOLGIMENTO'}
                <div class="flex flex-1 items-center justify-center gap-2 rounded-xl border border-dashed border-blue-200 bg-blue-50 px-4 py-2.5 text-[10px] font-bold uppercase text-blue-600">
                    <PlayCircle size={14} /> In Svolgimento
                </div>
            {/if}
        {/if}

        {#if ruolo === 'dipendente' || ruolo === 'azienda'}
            {#if corso.stato === 'IN_SVOLGIMENTO'}
                {#if onAzioneCorso}
                    <button onclick={onAzioneCorso} class="flex flex-1 items-center justify-center gap-2 rounded-xl bg-[#1B4B6B] px-4 py-2.5 text-xs font-bold uppercase text-white shadow-md transition-all hover:bg-[#1B4B6B]/90 hover:shadow-lg">
                        Riprendi Corso <PlayCircle size={16} />
                    </button>
                {:else}
                    <div class="flex flex-1 items-center justify-center gap-2 rounded-xl border border-dashed border-amber-200 bg-amber-50 px-4 py-2.5 text-[10px] font-bold uppercase text-amber-600">
                        <Clock size={14} /> Corso in Svolgimento
                    </div>
                {/if}
            {:else if corso.stato === 'COMPLETATO'}
                <button onclick={onAzioneCorso} class="flex flex-1 items-center justify-center gap-2 rounded-xl bg-emerald-600 px-4 py-2.5 text-xs font-bold uppercase text-white shadow-md transition-all hover:bg-emerald-700 hover:shadow-lg">
                    Scarica Attestato <Download size={16} />
                </button>
            {:else if corso.stato === 'DA_INIZIARE'}
                {#if ruolo === 'azienda' && onAnnullaIscrizione}
                    <button onclick={onAnnullaIscrizione} class="flex flex-1 items-center justify-center gap-2 rounded-xl border border-red-100 bg-red-50 px-4 py-2.5 text-[10px] font-bold uppercase text-red-600 transition-all hover:bg-red-100 hover:border-red-200">
                        <Trash2 size={14} /> Annulla Iscrizione
                    </button>
                {:else}
                    <div class="flex flex-1 items-center justify-center gap-2 rounded-xl border border-dashed border-gray-200 bg-white px-4 py-2.5 text-[10px] font-bold uppercase text-gray-400">
                        <Clock size={14} /> In attesa di avvio
                    </div>
                {/if}
            {/if}
        {/if}
    </div>
</div>