<script lang="ts">
    import { onMount } from 'svelte';
    import { goto } from '$app/navigation';
    import { fade, scale, slide } from 'svelte/transition';
    import {
        ShieldCheck, Search, Building2, Loader2, Calendar,
        AlertTriangle, CheckCircle2, ChevronDown, ChevronRight,
        Mail, MessageSquare, HardHat, ShieldOff, Clock
    } from 'lucide-svelte';

    import { LavoratoreService, type DipendenteDTO } from '$lib/services/LavoratoreService';
    import { AnagraficaService } from '$lib/services/AnagraficaService';
    import { Azienda, type AziendaData } from '$lib/models/Azienda';
    import { SvelteDate } from 'svelte/reactivity';
    import { resolveRoute } from "$app/paths";

    // Importazione nuovi componenti UI
    import StatCard from '$lib/Components/UI/StatCard.svelte';
    import AlertCard from '$lib/Components/UI/AlertCard.svelte';

    interface DpiSafe {
        idAssegnazione?: number;
        id?: number;
        idDipendente?: number;
        tipo: string;
        nomeDpi?: string;
        dataConsegna?: string;
        dataScadenzaRevisione?: string;
        dataScadenza?: string;
        daRevisionare?: boolean;
    }

    interface DipendenteEsteso extends DipendenteDTO {
        idAzienda?: number | string;
        nomeAzienda?: string;
        dpis?: DpiSafe[];
    }

    interface GruppoAzienda {
        idAzienda: string;
        dipendenti: DipendenteEsteso[];
    }

    let isLoading = $state(true);
    let searchQuery = $state('');
    let aziende = $state<Azienda[]>([]);
    let dipendenti = $state<DipendenteEsteso[]>([]);
    let aziendeEspanse = $state<Record<string, boolean>>({});

    function toggleAzienda(idAzienda: string) {
        aziendeEspanse[idAzienda] = !aziendeEspanse[idAzienda];
    }

    onMount(async () => {
        try {
            const [resAziende, resDipendenti] = await Promise.all([
                AnagraficaService.getAllAziende(),
                LavoratoreService.getAll()
            ]);

            const aziendeList = (resAziende as AziendaData[]).map(a => new Azienda(a));
            aziende = aziendeList;

            const dipendentiConAzienda = (resDipendenti as DipendenteDTO[]).map(d => {
                const item = d as DipendenteDTO & { idAzienda?: number | string };
                const aziendaAssoc = aziendeList.find(a => String(a.idUtente) === String(item.idAzienda));
                return {
                    ...d,
                    idAzienda: item.idAzienda,
                    nomeAzienda: aziendaAssoc ? aziendaAssoc.ragioneSociale : "Senza Azienda"
                } as DipendenteEsteso;
            });

            const dipendentiCompleti = await Promise.all(
                dipendentiConAzienda.map(async (dip) => {
                    if (!dip.idUtente) return { ...dip, dpis: [] };
                    try {
                        const dpisRaw = await LavoratoreService.getDpiByLavoratore(dip.idUtente);
                        const dpis = dpisRaw as unknown as DpiSafe[];
                        return { ...dip, dpis };
                    } catch {
                        return { ...dip, dpis: [] };
                    }
                })
            );

            dipendenti = dipendentiCompleti;

            aziendeList.forEach(a => {
                aziendeEspanse[String(a.idUtente)] = true;
            });
            aziendeEspanse["Senza Azienda"] = true;

        } catch (error) {
            console.error("Errore durante il caricamento dei dati relativi ai dispositivi di protezione individuale:", error);
        } finally {
            isLoading = false;
        }
    });

    const filteredDipendenti = $derived(
        dipendenti.filter(d => {
            if (!d.dpis || d.dpis.length === 0) return false;
            const query = searchQuery.toLowerCase();
            return d.nome.toLowerCase().includes(query) ||
                d.cognome.toLowerCase().includes(query) ||
                d.codiceFiscale.toLowerCase().includes(query) ||
                (d.nomeAzienda && d.nomeAzienda.toLowerCase().includes(query));
        })
    );

    const dipendentiRaggruppati = $derived(() => {
        const gruppi: Record<string, GruppoAzienda> = {};
        const ordinati = [...filteredDipendenti].sort((a, b) => {
            const azA = a.nomeAzienda || "Senza Azienda";
            const azB = b.nomeAzienda || "Senza Azienda";
            return azA.localeCompare(azB);
        });

        for (const dip of ordinati) {
            const nomeAzienda = dip.nomeAzienda || "Senza Azienda";
            const idAzienda = String(dip.idAzienda || "Senza Azienda");
            if (!gruppi[nomeAzienda]) {
                gruppi[nomeAzienda] = { idAzienda, dipendenti: [] };
            }
            gruppi[nomeAzienda].dipendenti.push(dip);
        }
        return gruppi;
    });

    // Statistiche per StatCards
    const globalStats = $derived({
        totali: dipendenti.reduce((acc, d) => acc + (d.dpis?.length || 0), 0),
        scaduti: dipendenti.reduce((acc, d) => acc + (d.dpis?.filter(dpi => isScaduto(dpi.dataScadenzaRevisione || dpi.dataScadenza)).length || 0), 0),
        inScadenza: dipendenti.reduce((acc, d) => {
            const warning = d.dpis?.filter(dpi => {
                const data = dpi.dataScadenzaRevisione || dpi.dataScadenza;
                if (!data || data === '9999-12-31') return false;
                const scad = new Date(data).getTime();
                const oggi = new Date().getTime();
                const diff = Math.ceil((scad - oggi) / (1000 * 3600 * 24));
                return diff >= 0 && diff <= 30;
            }).length || 0;
            return acc + warning;
        }, 0)
    });

    function isScaduto(data: string | undefined) {
        if (!data || data === '9999-12-31') return false;
        const oggi = new SvelteDate();
        oggi.setHours(0, 0, 0, 0);
        const scad = new SvelteDate(data);
        scad.setHours(0, 0, 0, 0);
        return scad < oggi;
    }

    function formattaData(data: string | undefined) {
        if (!data || data === '9999-12-31') return 'Nessuna Scadenza';
        return new Date(data).toLocaleDateString();
    }

    function sollecitaViaEmail(dpi: DpiSafe, dipendente: DipendenteEsteso) {
        if (!dipendente || !dipendente.idAzienda) return;
        const azienda = aziende.find(a => String(a.idUtente) === String(dipendente.idAzienda));
        if (!azienda || !azienda.email) {
            alert("Impossibile procedere: l'azienda associata non dispone di un indirizzo email registrato nel sistema.");
            return;
        }
        const subject = encodeURIComponent(`URGENTE: Rinnovo DPI Scaduto - ${dipendente.nome} ${dipendente.cognome}`);
        const dataScadenzaReale = dpi.dataScadenzaRevisione || dpi.dataScadenza || '';
        const dataScad = dataScadenzaReale ? new Date(dataScadenzaReale).toLocaleDateString() : 'N/D';
        const nomeDpiReale = (dpi.tipo === 'ALTRO' && dpi.nomeDpi) ? dpi.nomeDpi : dpi.tipo.replace(/_/g, ' ');
        const body = encodeURIComponent(
            `Gentile ${azienda.ragioneSociale},\n\n` +
            `Vi segnaliamo che il DPI (${nomeDpiReale}) assegnato a ${dipendente.nome} ${dipendente.cognome} è SCADUTO il ${dataScad}.\n\n` +
            `Vi invitiamo a provvedere al rinnovo immediato.\n\nCordiali saluti,\nTeam NorLan`
        );
        window.open(`https://mail.google.com/mail/?view=cm&fs=1&to=${azienda.email}&su=${subject}&body=${body}`, '_blank');
    }

    async function sollecitaViaChat(dpi: DpiSafe, dipendente: DipendenteEsteso) {
        if (!dipendente || !dipendente.idAzienda) return;
        const dataScadenzaReale = dpi.dataScadenzaRevisione || dpi.dataScadenza || '';
        const dataScad = dataScadenzaReale ? new Date(dataScadenzaReale).toLocaleDateString() : 'N/D';
        const nomeDpiReale = (dpi.tipo === 'ALTRO' && dpi.nomeDpi) ? dpi.nomeDpi : dpi.tipo.replace(/_/g, ' ');
        const testoMessaggio = encodeURIComponent(
            `Salve, vi segnaliamo che il DPI (${nomeDpiReale}) assegnato al lavoratore ${dipendente.nome} ${dipendente.cognome} risulta scaduto in data ${dataScad}. Vi invitiamo a rinnovare questo dispositivo il prima possibile.`
        );
        await goto(`${resolveRoute('/dashboard/admin/comunicazioni')}?chatId=${dipendente.idAzienda}&msg=${testoMessaggio}`);
    }
</script>

<div in:fade class="max-w-7xl mx-auto p-6 pb-20">
    <div class="mb-10 flex flex-col md:flex-row justify-between items-start gap-6">
        <div>
            <h1 class="text-4xl font-extrabold text-[#1B4B6B] uppercase tracking-tighter flex items-center gap-3">
                <ShieldCheck class="text-[#1B4B6B]" size={36} />
                Gestione Globale DPI
            </h1>
            <p class="text-gray-500 font-bold uppercase text-xs tracking-tighter mt-1">
                Monitoraggio e riepilogo dei Dispositivi di Protezione Individuale di tutti i lavoratori.
            </p>
        </div>

        <div class="grid grid-cols-1 sm:grid-cols-3 gap-4 w-full md:w-auto">
            <StatCard titolo="DPI Totali" valore={globalStats.totali} icona={HardHat} />
            <StatCard titolo="Scaduti" valore={globalStats.scaduti} icona={ShieldOff} bgIcona="bg-red-50" testoIcona="text-red-600" />
            <StatCard titolo="In Scadenza" valore={globalStats.inScadenza} icona={Clock} bgIcona="bg-amber-50" testoIcona="text-amber-600" />
        </div>
    </div>

    <div class="mb-8 flex gap-4">
        <div class="relative w-full max-w-[400px] group">
            <Search class="absolute left-4 top-1/2 -translate-y-1/2 text-gray-400 group-focus-within:text-[#1B4B6B] transition-colors" size={18} />
            <input
                    bind:value={searchQuery}
                    type="text"
                    placeholder="Cerca per lavoratore, azienda o C.Fiscale..."
                    class="w-full pl-12 pr-4 py-3.5 bg-white border border-gray-200 rounded-xl text-xs focus:ring-2 focus:ring-[#1B4B6B] outline-none transition-all font-bold uppercase shadow-sm"
            />
        </div>
    </div>

    {#if isLoading}
        <div class="py-32 text-center flex flex-col items-center">
            <Loader2 size={48} class="animate-spin text-[#1B4B6B] mb-4" />
            <p class="text-[10px] font-black uppercase tracking-widest text-gray-400">Sincronizzazione inventario globale...</p>
        </div>
    {:else if filteredDipendenti.length === 0}
        <div class="py-20 text-center bg-gray-50 rounded-3xl border-2 border-dashed border-gray-200">
            <ShieldCheck size={48} class="mx-auto text-gray-300 mb-4" />
            <p class="text-gray-400 font-bold uppercase text-xs">Nessun lavoratore con DPI assegnati trovato</p>
        </div>
    {:else}
        <div class="space-y-8">
            {#each Object.entries(dipendentiRaggruppati()) as [nomeAzienda, gruppo], i (nomeAzienda ?? i)}
                <div class="bg-white rounded-3xl shadow-sm border border-gray-100 overflow-hidden" in:scale={{start: 0.98, duration: 300}}>
                    <button
                            onclick={() => toggleAzienda(gruppo.idAzienda)}
                            class="w-full p-6 bg-gray-50/50 hover:bg-gray-100/50 flex items-center justify-between transition-colors border-b border-gray-100"
                    >
                        <div class="flex items-center gap-4">
                            <div class="p-3 bg-[#1B4B6B]/10 text-[#1B4B6B] rounded-xl">
                                <Building2 size={24} />
                            </div>
                            <div class="text-left">
                                <h2 class="text-xl font-black text-[#1B4B6B] uppercase tracking-tighter leading-none">
                                    {nomeAzienda}
                                </h2>
                                <p class="text-[10px] font-bold text-gray-400 uppercase tracking-widest mt-1.5">
                                    {gruppo.dipendenti.length} Lavorator{gruppo.dipendenti.length === 1 ? 'e' : 'i'} con DPI assegnati
                                </p>
                            </div>
                        </div>
                        <div class="text-gray-400">
                            {#if aziendeEspanse[gruppo.idAzienda]}
                                <ChevronDown size={24} />
                            {:else}
                                <ChevronRight size={24} />
                            {/if}
                        </div>
                    </button>

                    {#if aziendeEspanse[gruppo.idAzienda]}
                        <div transition:slide class="p-6 grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 xl:grid-cols-4 gap-6 bg-gray-50/30">
                            {#each gruppo.dipendenti as dip, j (dip.idUtente ?? j)}
                                <div class="bg-white rounded-2xl border border-gray-200 shadow-sm flex flex-col h-full hover:shadow-md transition-shadow overflow-hidden">

                                    <div class="p-4 border-b border-gray-50 flex items-center gap-3 bg-white">
                                        <div class="w-10 h-10 bg-[#1B4B6B]/10 text-[#1B4B6B] rounded-xl flex items-center justify-center font-black text-sm shrink-0">
                                            {dip.nome[0]}{dip.cognome[0]}
                                        </div>
                                        <div class="overflow-hidden">
                                            <h3 class="font-extrabold text-[#1B4B6B] text-sm uppercase leading-tight truncate" title="{dip.nome} {dip.cognome}">
                                                {dip.nome} {dip.cognome}
                                            </h3>
                                            <p class="text-[9px] font-bold text-gray-400 uppercase mt-0.5 truncate">{dip.codiceFiscale}</p>
                                        </div>
                                    </div>

                                    <div class="p-4 flex-1 flex flex-col gap-3 bg-gray-50/50">
                                        <p class="text-[9px] font-black text-[#1B4B6B] uppercase tracking-widest border-b border-gray-200 pb-1.5 flex items-center justify-between">
                                            Elenco DPI
                                            <span class="bg-[#1B4B6B]/10 px-1.5 py-0.5 rounded text-[#1B4B6B]">{dip.dpis?.length || 0}</span>
                                        </p>

                                        <div class="space-y-3 overflow-y-auto max-h-[300px] custom-scrollbar pr-1">
                                            {#each (dip.dpis || []) as dpi, k (dpi.idAssegnazione ?? dpi.id ?? k)}
                                                {@const nomeDpiReale = (dpi.tipo === 'ALTRO' && dpi.nomeDpi) ? dpi.nomeDpi : dpi.tipo.replace(/_/g, ' ')}
                                                {@const dataScadenzaReale = dpi.dataScadenzaRevisione || dpi.dataScadenza || ''}
                                                {@const scaduto = isScaduto(dataScadenzaReale)}

                                                <div class="flex flex-col gap-2">
                                                    <AlertCard
                                                            titolo={nomeDpiReale}
                                                            sottotitolo={scaduto ? 'Dispositivo Scaduto' : 'Dispositivo in Regola'}
                                                            stato={scaduto ? 'SCADUTO' : 'VALIDO'}
                                                            data={formattaData(dataScadenzaReale)}
                                                            icona={scaduto ? ShieldOff : CheckCircle2}
                                                            variante={scaduto ? 'danger' : 'success'}
                                                    />

                                                    {#if scaduto}
                                                        <div class="px-2 py-2 bg-red-50/50 border-x border-b border-red-100 rounded-b-xl -mt-3 pt-4">
                                                            <p class="text-[8px] font-black uppercase text-red-400 mb-2 text-center tracking-tighter">
                                                                Sollecita {dip.nomeAzienda}:
                                                            </p>
                                                            <div class="flex gap-2">
                                                                <button
                                                                        onclick={() => sollecitaViaEmail(dpi, dip)}
                                                                        class="flex-1 py-1.5 bg-red-600 text-white rounded-md text-[8px] font-black uppercase flex items-center justify-center gap-1 shadow-sm hover:bg-red-700 transition-colors"
                                                                >
                                                                    <Mail size={10} /> Email
                                                                </button>
                                                                <button
                                                                        onclick={() => sollecitaViaChat(dpi, dip)}
                                                                        class="flex-1 py-1.5 bg-[#1B4B6B] text-white rounded-md text-[8px] font-black uppercase flex items-center justify-center gap-1 shadow-sm hover:bg-[#1B4B6B]/90 transition-colors"
                                                                >
                                                                    <MessageSquare size={10} /> Chat
                                                                </button>
                                                            </div>
                                                        </div>
                                                    {/if}
                                                </div>
                                            {/each}
                                        </div>
                                    </div>
                                </div>
                            {/each}
                        </div>
                    {/if}
                </div>
            {/each}
        </div>
    {/if}
</div>

<style>
    .custom-scrollbar::-webkit-scrollbar { width: 4px; }
    .custom-scrollbar::-webkit-scrollbar-track { background: transparent; }
    .custom-scrollbar::-webkit-scrollbar-thumb { background: #CBD5E1; border-radius: 10px; }
</style>
