<script lang="ts">
    import { onMount } from 'svelte';
    import { goto } from '$app/navigation';
    import { fade, slide } from 'svelte/transition';
    import {
        Search, Building2, Loader2, Mail, MessageSquare,
        HardHat, ShieldOff, Clock, ShieldCheck
    } from 'lucide-svelte';
    import { LavoratoreService } from '$lib/services/LavoratoreService';
    import { AnagraficaService } from '$lib/services/AnagraficaService';
    import { resolveRoute } from "$app/paths";
    import DpiCard from '$lib/Components/Features/Documentale/DpiCard.svelte';

    let isLoading = $state(true);
    let searchQuery = $state('');
    let filtroStato = $state('TUTTI');
    let aziende = $state<any[]>([]);
    let dipendenti = $state<any[]>([]);

    onMount(async () => {
        try {
            const [resAziende, resDipendenti] = await Promise.all([
                AnagraficaService.getAllAziende(),
                LavoratoreService.getAll()
            ]);
            aziende = resAziende;
            const workers = await Promise.all(resDipendenti.map(async (dip: any) => {
                const dpis = await LavoratoreService.getDpiByLavoratore(dip.idUtente);
                const az = aziende.find(a => String(a.idUtente) === String(dip.idAzienda));
                const dpisSafe = dpis.map((d: any, index: number) => ({
                    ...d,
                    _uniqueKey: d.idAssegnazione || d.id || `temp-${dip.idUtente}-${index}`
                }));
                return { ...dip, nomeAzienda: az?.ragioneSociale || "N.D.", emailAzienda: az?.email, dpis: dpisSafe };
            }));
            dipendenti = workers;
        } catch (f) {
            console.error(f);
        } finally {
            isLoading = false;
        }
    });

    const calcolaStato = (d?: string) => {
        if (!d || d === '9999-12-31') return 'OK';
        const diff = Math.ceil((new Date(d).getTime() - new Date().getTime()) / (1000 * 3600 * 24));
        return diff < 0 ? 'DANGER' : diff <= 30 ? 'WARNING' : 'OK';
    };

    const gruppiFiltrati = $derived(() => {
        const map: Record<string, any> = {};
        dipendenti.forEach(dip => {
            const query = searchQuery.toLowerCase();
            const matchSearch = dip.nome.toLowerCase().includes(query) || dip.nomeAzienda.toLowerCase().includes(query);
            if (!matchSearch) return;

            const dpisFiltrati = dip.dpis.filter((dpi: any) => {
                const s = calcolaStato(dpi.dataScadenzaRevisione || dpi.dataScadenza);
                return filtroStato === 'TUTTI' || s === filtroStato;
            });

            if (dpisFiltrati.length > 0) {
                if (!map[dip.nomeAzienda]) {
                    map[dip.nomeAzienda] = { idAzienda: dip.idAzienda, nome: dip.nomeAzienda, email: dip.emailAzienda, lavoratori: [] };
                }
                map[dip.nomeAzienda].lavoratori.push({ ...dip, dpis: dpisFiltrati });
            }
        });
        return Object.values(map);
    });

    const globalStats = $derived({
        totali: dipendenti.reduce((acc, d) => acc + d.dpis.length, 0),
        scaduti: dipendenti.reduce((acc, d) => acc + d.dpis.filter((dpi: any) => calcolaStato(dpi.dataScadenzaRevisione || dpi.dataScadenza) === 'DANGER').length, 0),
        warning: dipendenti.reduce((acc, d) => acc + d.dpis.filter((dpi: any) => calcolaStato(dpi.dataScadenzaRevisione || dpi.dataScadenza) === 'WARNING').length, 0)
    });

    function formattaData(d?: string) {
        return (!d || d === '9999-12-31') ? 'N.D.' : new Date(d).toLocaleDateString('it-IT');
    }

    async function sollecitaAzienda(gruppo: any, canale: 'email' | 'chat') {
        const msg = `Gentile Amministrazione di ${gruppo.nome},\n\nVi segnaliamo che dal nostro sistema di monitoraggio risultano dei Dispositivi di Protezione Individuale (DPI) scaduti o anomali assegnati ai vostri lavoratori.\n\nVi invitiamo a verificare la sezione DPI del vostro pannello e a procedere con l'aggiornamento o la sostituzione nel più breve tempo possibile per ripristinare la conformità aziendale.\n\nCordiali saluti,\nStaff NorLan`;

        if (canale === 'email' && gruppo.email) {
            window.open(`https://mail.google.com/mail/?view=cm&fs=1&to=${gruppo.email}&su=Avviso Urgente: Rinnovo DPI Scaduti - NorLan&body=${encodeURIComponent(msg)}`, '_blank');
        } else {
            await goto(`${resolveRoute('/dashboard/admin/comunicazioni')}?chatId=${gruppo.idAzienda}&msg=${encodeURIComponent(msg)}`);
        }
    }
</script>

<div in:fade class="max-w-[1600px] mx-auto p-6 pb-20">
    <div class="mb-10 flex flex-col xl:flex-row justify-between items-end gap-6">
        <div class="w-full xl:w-auto">
            <h1 class="text-4xl font-black text-[#1B4B6B] uppercase tracking-tighter">Controllo Compliance DPI</h1>

            <div class="flex flex-wrap items-center gap-3 mt-5">
                {#each ['TUTTI', 'DANGER', 'WARNING', 'OK'] as s}
                    <button
                            onclick={() => filtroStato = s}
                            class="px-5 py-2 rounded-full text-[10px] font-black uppercase transition-all shadow-sm
                        {filtroStato === s ? 'bg-[#1B4B6B] text-white' : 'bg-white text-gray-400 hover:bg-gray-50'}">
                        {s === 'DANGER' ? 'Scaduti' : s === 'WARNING' ? 'In Scadenza' : s}
                        ({s === 'TUTTI' ? globalStats.totali : s === 'DANGER' ? globalStats.scaduti : s === 'WARNING' ? globalStats.warning : (globalStats.totali - globalStats.scaduti - globalStats.warning)})
                    </button>
                {/each}
            </div>
        </div>

        <div class="flex items-center gap-3 w-full xl:w-auto overflow-x-auto pb-2 xl:pb-0">
            <div class="bg-white rounded-2xl px-5 py-3 flex items-center gap-3 shadow-sm min-w-[130px] transition-all duration-300 hover:-translate-y-1 hover:shadow-lg hover:shadow-blue-900/10 cursor-default border border-transparent hover:border-blue-100">
                <div class="p-2 bg-gray-50 text-gray-500 rounded-xl"><HardHat size={16}/></div>
                <div>
                    <p class="text-[9px] font-black text-gray-400 uppercase tracking-widest">Asset Totali</p>
                    <p class="text-xl font-black text-[#1B4B6B] leading-none mt-1">{globalStats.totali}</p>
                </div>
            </div>

            <div class="bg-white rounded-2xl px-5 py-3 flex items-center gap-3 shadow-sm min-w-[130px] transition-all duration-300 hover:-translate-y-1 hover:shadow-lg hover:shadow-red-900/10 cursor-default border border-transparent hover:border-red-100">
                <div class="p-2 bg-red-50 text-red-600 rounded-xl"><ShieldOff size={16}/></div>
                <div>
                    <p class="text-[9px] font-black text-red-400 uppercase tracking-widest">Scaduti</p>
                    <p class="text-xl font-black text-red-600 leading-none mt-1">{globalStats.scaduti}</p>
                </div>
            </div>

            <div class="bg-white rounded-2xl px-5 py-3 flex items-center gap-3 shadow-sm min-w-[130px] transition-all duration-300 hover:-translate-y-1 hover:shadow-lg hover:shadow-amber-900/10 cursor-default border border-transparent hover:border-amber-100">
                <div class="p-2 bg-amber-50 text-amber-600 rounded-xl"><Clock size={16}/></div>
                <div>
                    <p class="text-[9px] font-black text-amber-500 uppercase tracking-widest">In Scadenza</p>
                    <p class="text-xl font-black text-amber-600 leading-none mt-1">{globalStats.warning}</p>
                </div>
            </div>
        </div>
    </div>

    <div class="mb-12 relative max-w-xl">
        <Search class="absolute left-5 top-1/2 -translate-y-1/2 text-gray-400" size={18} />
        <input bind:value={searchQuery} type="text" placeholder="Cerca azienda o dipendente..." class="w-full pl-14 pr-4 py-4 bg-white border border-gray-100 rounded-2xl text-xs font-bold uppercase shadow-sm outline-none focus:ring-2 focus:ring-[#1B4B6B]/5 transition-all" />
    </div>

    {#if isLoading}
        <div class="py-40 text-center"><Loader2 size={48} class="animate-spin text-[#1B4B6B] mx-auto opacity-20" /></div>
    {:else}
        <div class="space-y-12">
            {#each gruppiFiltrati() as gruppo (gruppo.nome)}
                <div class="bg-white rounded-[2.5rem] border border-gray-100 shadow-sm overflow-hidden" transition:slide>

                    <div class="p-6 md:px-8 bg-gray-50/50 flex flex-col sm:flex-row sm:items-center justify-between border-b border-gray-100 gap-4">
                        <div class="flex items-center gap-4">
                            <div class="w-12 h-12 bg-[#1B4B6B] text-white rounded-2xl flex items-center justify-center shadow-lg shrink-0"><Building2 size={20}/></div>
                            <h2 class="text-2xl font-black text-[#1B4B6B] uppercase tracking-tighter leading-none">{gruppo.nome}</h2>
                        </div>

                        <div class="flex items-center gap-2 p-1.5 bg-red-50/50 rounded-2xl">
                            <div class="px-3">
                                <span class="text-[10px] font-black text-red-600 uppercase">Sollecita:</span>
                            </div>
                            <button onclick={() => sollecitaAzienda(gruppo, 'email')} class="p-2.5 bg-white text-red-600 rounded-xl shadow-sm hover:bg-red-600 hover:text-white transition-all group">
                                <Mail size={16} class="group-hover:scale-110 transition-transform" />
                            </button>
                            <button onclick={() => sollecitaAzienda(gruppo, 'chat')} class="p-2.5 bg-[#1B4B6B] text-white rounded-xl shadow-sm hover:bg-[#153a54] transition-all group">
                                <MessageSquare size={16} class="group-hover:scale-110 transition-transform" />
                            </button>
                        </div>
                    </div>

                    <div class="p-6 md:px-8">
                        {#each gruppo.lavoratori as dip (dip.idUtente)}
                            <div class="mb-10 last:mb-0">

                                <div class="flex items-center gap-3 mb-6">
                                    <div class="w-8 h-8 bg-gray-100 rounded-lg flex items-center justify-center text-[10px] font-black text-[#1B4B6B] shrink-0">{dip.nome[0]}{dip.cognome[0]}</div>
                                    <h3 class="text-sm font-black text-[#1B4B6B] uppercase">{dip.nome} {dip.cognome}</h3>
                                    <span class="text-[9px] font-bold text-gray-400 uppercase tracking-widest ml-2 hidden sm:inline-block">C.F: {dip.codiceFiscale}</span>
                                </div>

                                <div class="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 xl:grid-cols-4 gap-4">
                                    {#each dip.dpis as dpi (dpi._uniqueKey)}
                                        <DpiCard ruolo="admin" dpi={{
                                            id: dpi._uniqueKey,
                                            nome: (dpi.tipo === 'ALTRO' && dpi.nomeDpi) ? dpi.nomeDpi : (dpi.tipo || 'DPI').replace(/_/g, ' '),
                                            stato: calcolaStato(dpi.dataScadenzaRevisione || dpi.dataScadenza),
                                            dataRevisione: formattaData(dpi.dataScadenzaRevisione || dpi.dataScadenza),
                                            dataConsegna: formattaData(dpi.dataConsegna)
                                        }} />
                                    {/each}
                                </div>

                            </div>
                        {/each}
                    </div>
                </div>
            {/each}

            {#if gruppiFiltrati().length === 0}
                <div class="py-32 text-center bg-white rounded-[3rem] border border-gray-100 shadow-sm">
                    <ShieldCheck size={48} class="mx-auto text-gray-200 mb-4" />
                    <h3 class="text-xl font-black text-[#1B4B6B] uppercase italic">Nessun Risultato</h3>
                </div>
            {/if}
        </div>
    {/if}
</div>

<style>
    :global(body) { background-color: #F9FAFB; }
</style>