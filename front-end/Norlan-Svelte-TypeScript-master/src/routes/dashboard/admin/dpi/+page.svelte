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
                        return { ...dip, dpis: dpisRaw as unknown as DpiSafe[] };
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
            console.error("Errore DPI:", error);
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
        if (!azienda || !azienda.email) return;
        const subject = encodeURIComponent(`URGENTE: Rinnovo DPI Scaduto - ${dipendente.nome} ${dipendente.cognome}`);
        const nomeDpiReale = (dpi.tipo === 'ALTRO' && dpi.nomeDpi) ? dpi.nomeDpi : dpi.tipo.replace(/_/g, ' ');
        const dataScad = formattaData(dpi.dataScadenzaRevisione || dpi.dataScadenza);
        const body = encodeURIComponent(`Il DPI (${nomeDpiReale}) di ${dipendente.nome} ${dipendente.cognome} è SCADUTO il ${dataScad}.`);
        window.open(`https://mail.google.com/mail/?view=cm&fs=1&to=${azienda.email}&su=${subject}&body=${body}`, '_blank');
    }

    async function sollecitaViaChat(dpi: DpiSafe, dipendente: DipendenteEsteso) {
        if (!dipendente || !dipendente.idAzienda) return;
        const nomeDpiReale = (dpi.tipo === 'ALTRO' && dpi.nomeDpi) ? dpi.nomeDpi : dpi.tipo.replace(/_/g, ' ');
        const dataScad = formattaData(dpi.dataScadenzaRevisione || dpi.dataScadenza);
        const msg = encodeURIComponent(`DPI (${nomeDpiReale}) di ${dipendente.nome} ${dipendente.cognome} scaduto il ${dataScad}.`);
        await goto(`${resolveRoute('/dashboard/admin/comunicazioni')}?chatId=${dipendente.idAzienda}&msg=${msg}`);
    }
</script>

<div in:fade class="max-w-7xl mx-auto p-6 pb-20">
    <div class="mb-10 flex flex-col md:flex-row justify-between items-start gap-6">
        <div>
            <h1 class="text-4xl font-extrabold text-[#1B4B6B] uppercase tracking-tighter flex items-center gap-3">
                <ShieldCheck class="text-[#1B4B6B]" size={36} />
                Gestione Globale DPI
            </h1>
        </div>

        <div class="grid grid-cols-1 sm:grid-cols-3 gap-4 w-full md:w-auto">
            <StatCard titolo="DPI Totali" valore={globalStats.totali} icona={HardHat} />
            <StatCard titolo="Scaduti" valore={globalStats.scaduti} icona={ShieldOff} bgIcona="bg-red-50" testoIcona="text-red-600" />
            <StatCard titolo="In Scadenza" valore={globalStats.inScadenza} icona={Clock} bgIcona="bg-amber-50" testoIcona="text-amber-600" />
        </div>
    </div>

    <div class="mb-8 flex gap-4">
        <div class="relative w-full max-w-[400px] group">
            <Search class="absolute left-4 top-1/2 -translate-y-1/2 text-gray-400 group-focus-within:text-[#1B4B6B]" size={18} />
            <input bind:value={searchQuery} type="text" placeholder="Cerca lavoratore o azienda..." class="w-full pl-12 pr-4 py-3.5 bg-white border border-gray-200 rounded-xl text-xs focus:ring-2 focus:ring-[#1B4B6B] outline-none font-bold uppercase shadow-sm" />
        </div>
    </div>

    {#if isLoading}
        <div class="py-32 text-center flex flex-col items-center">
            <Loader2 size={48} class="animate-spin text-[#1B4B6B] mb-4" />
        </div>
    {:else}
        <div class="space-y-8">
            {#each Object.entries(dipendentiRaggruppati()) as [nomeAzienda, gruppo], i (nomeAzienda ?? i)}
                <div class="bg-white rounded-3xl shadow-sm border border-gray-100 overflow-hidden" in:scale={{start: 0.98, duration: 300}}>
                    <button onclick={() => toggleAzienda(gruppo.idAzienda)} class="w-full p-6 bg-gray-50/50 hover:bg-gray-100/50 flex items-center justify-between border-b border-gray-100">
                        <div class="flex items-center gap-4">
                            <div class="p-3 bg-[#1B4B6B]/10 text-[#1B4B6B] rounded-xl"><Building2 size={24} /></div>
                            <h2 class="text-xl font-black text-[#1B4B6B] uppercase tracking-tighter">{nomeAzienda}</h2>
                        </div>
                        <div class="text-gray-400">
                            {#if aziendeEspanse[gruppo.idAzienda]}<ChevronDown size={24} />{:else}<ChevronRight size={24} />{/if}
                        </div>
                    </button>

                    {#if aziendeEspanse[gruppo.idAzienda]}
                        <div transition:slide class="p-6 grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 xl:grid-cols-4 gap-6 bg-gray-50/30">
                            {#each gruppo.dipendenti as dip, j (dip.idUtente ?? j)}
                                <div class="bg-white rounded-2xl border border-gray-200 shadow-sm flex flex-col h-full overflow-hidden">
                                    <div class="p-4 border-b border-gray-50 flex items-center gap-3 bg-white">
                                        <div class="w-10 h-10 bg-[#1B4B6B]/10 text-[#1B4B6B] rounded-xl flex items-center justify-center font-black text-sm">
                                            {dip.nome[0]}{dip.cognome[0]}
                                        </div>
                                        <div class="overflow-hidden">
                                            <h3 class="font-extrabold text-[#1B4B6B] text-sm uppercase leading-tight truncate">{dip.nome} {dip.cognome}</h3>
                                            <p class="text-[9px] font-bold text-gray-400 mt-0.5 truncate">{dip.codiceFiscale}</p>
                                        </div>
                                    </div>

                                    <div class="p-4 flex-1 flex flex-col gap-3 bg-gray-50/50">
                                        <div class="space-y-3">
                                            {#each (dip.dpis || []) as dpi, k (dpi.idAssegnazione ?? dpi.id ?? k)}
                                                {@const nomeDpiReale = (dpi.tipo === 'ALTRO' && dpi.nomeDpi) ? dpi.nomeDpi : dpi.tipo.replace(/_/g, ' ')}
                                                {@const dataScad = dpi.dataScadenzaRevisione || dpi.dataScadenza}
                                                {@const scaduto = isScaduto(dataScad)}

                                                <div class="flex flex-col gap-2">
                                                    <AlertCard
                                                            titolo={nomeDpiReale}
                                                            sottotitolo={scaduto ? 'Dispositivo Scaduto' : 'Dispositivo in Regola'}
                                                            stato={scaduto ? 'SCADUTO' : 'VALIDO'}
                                                            data={formattaData(dataScad)}
                                                            icona={scaduto ? ShieldOff : CheckCircle2}
                                                            variante={scaduto ? 'danger' : 'success'}
                                                    />
                                                    {#if scaduto}
                                                        <div class="flex gap-2 -mt-1">
                                                            <button onclick={() => sollecitaViaEmail(dpi, dip)} class="flex-1 py-1.5 bg-red-600 text-white rounded-md text-[8px] font-black uppercase flex items-center justify-center gap-1"><Mail size={10} /> Email</button>
                                                            <button onclick={() => sollecitaViaChat(dpi, dip)} class="flex-1 py-1.5 bg-[#1B4B6B] text-white rounded-md text-[8px] font-black uppercase flex items-center justify-center gap-1"><MessageSquare size={10} /> Chat</button>
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
    .custom-scrollbar::-webkit-scrollbar-thumb { background: #CBD5E1; border-radius: 10px; }
</style>