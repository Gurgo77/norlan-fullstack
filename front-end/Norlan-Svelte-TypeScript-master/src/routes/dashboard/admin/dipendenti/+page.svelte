<script lang="ts">
    import { onMount } from 'svelte';
    import { page } from '$app/stores';
    import { goto } from '$app/navigation';
    import { fade, scale, slide } from 'svelte/transition';
    import {
        Users, UserPlus, Trash2, Search, Mail, Building2,
        IdCard, Loader2, X, ChevronRight, AlertTriangle, ChevronLeft,
        FileText, ShieldCheck, Download, Calendar, MessageSquare, Plus
    } from 'lucide-svelte';

    // Servizi e Modelli
    import { LavoratoreService, type DipendenteDTO, type DipendenteRequest } from '$lib/services/LavoratoreService';
    import { AnagraficaService } from '$lib/services/AnagraficaService';
    import { DocumentoService } from '$lib/services/DocumentoService';
    import { Azienda, type AziendaData } from '$lib/models/Azienda';
    import { Documento } from '$lib/models/Documento';
    import type { AssegnazioneDPI } from '$lib/models/AssegnazioneDPI';

    interface DipendenteEsteso extends DipendenteDTO {
        nomeAzienda?: string;
        idAzienda?: string | number;
    }

    // Interfaccia estesa per tollerare la nuova proprietà nomeDpi senza far arrabbiare ESLint/TS
    interface DpiEsteso extends AssegnazioneDPI {
        nomeDpi?: string;
    }

    interface FormDPI {
        tipo: string;
        nomeDpi: string;
        dataConsegna: string;
        dataScadenzaRevisione: string;
    }

    // --- STATO REATTIVO ---
    let lavoratori = $state<DipendenteEsteso[]>([]);
    let aziende = $state<Azienda[]>([]);
    let isLoading = $state(true);
    let searchQuery = $state('');

    let selectedDipendente = $state<DipendenteEsteso | null>(null);
    let documentiCorrenti = $state<Documento[]>([]);
    let dpiCorrenti = $state<DpiEsteso[]>([]);
    let isLoadingDettaglio = $state(false);

    let showAddModal = $state(false);
    let showDeleteModal = $state(false);
    let isSaving = $state(false);

    let dipendenteDaEliminare = $state<DipendenteEsteso | null>(null);

    let formDipendente = $state({
        nome: '', cognome: '', codiceFiscale: '', email: '', idAzienda: '', password: ''
    });

    // --- STATI ATTESTATI E DPI (Aggiunta ed eliminazione) ---
    let showDeleteDocModal = $state(false);
    let docDaEliminare = $state<Documento | null>(null);

    let showDeleteDpiModal = $state(false);
    let dpiDaEliminare = $state<DpiEsteso | null>(null);

    let showAddDpiModal = $state(false);
    let isSavingDpi = $state(false);
    let formDpi = $state<FormDPI>({
        tipo: '',
        nomeDpi: '',
        dataConsegna: '',
        dataScadenzaRevisione: ''
    });

    // --- LOGICA DERIVATA ---
    const filteredLavoratori = $derived(
        lavoratori.filter(l =>
            l.nome.toLowerCase().includes(searchQuery.toLowerCase()) ||
            l.cognome.toLowerCase().includes(searchQuery.toLowerCase()) ||
            l.codiceFiscale.toLowerCase().includes(searchQuery.toLowerCase()) ||
            (l.nomeAzienda && l.nomeAzienda.toLowerCase().includes(searchQuery.toLowerCase()))
        )
    );

    const lavoratoriRaggruppati = $derived(() => {
        const gruppi: Record<string, DipendenteEsteso[]> = {};

        const ordinati = [...filteredLavoratori].sort((a, b) => {
            const azA = a.nomeAzienda || "Senza Azienda";
            const azB = b.nomeAzienda || "Senza Azienda";
            return azA.localeCompare(azB);
        });

        for (const lavoratore of ordinati) {
            const azienda = lavoratore.nomeAzienda || "Senza Azienda assegnata";
            if (!gruppi[azienda]) {
                gruppi[azienda] = [];
            }
            gruppi[azienda].push(lavoratore);
        }
        return gruppi;
    });

    const isFormValid = $derived(
        formDipendente.nome.trim() !== '' && formDipendente.cognome.trim() !== '' &&
        formDipendente.codiceFiscale.length === 16 && formDipendente.idAzienda !== '' &&
        formDipendente.password.trim() !== ''
    );

    // --- AZIONI ---
    onMount(async () => {
        try {
            const [resLavoratori, resAziende] = await Promise.all([
                LavoratoreService.getAll(),
                AnagraficaService.getAllAziende()
            ]);

            const aziendeList = (resAziende as AziendaData[]).map(a => new Azienda(a));
            aziende = aziendeList;

            lavoratori = resLavoratori.map((l: DipendenteDTO) => {
                const item = l as DipendenteDTO & { idAzienda?: number | string };
                const aziendaAssoc = aziendeList.find(a => String(a.idUtente) === String(item.idAzienda));
                return {
                    ...l,
                    nomeAzienda: aziendaAssoc ? aziendaAssoc.ragioneSociale : "Azienda non specificata",
                    idAzienda: item.idAzienda
                };
            });

            const idDaUrl = $page.url.searchParams.get('id');
            if (idDaUrl) {
                const dipendenteTrovato = lavoratori.find(l => String(l.idUtente) === idDaUrl);
                if (dipendenteTrovato) {
                    await apriDettaglio(dipendenteTrovato);
                }
            }
        } catch (error) {
            console.error("Errore caricamento dati:", error);
        } finally {
            isLoading = false;
        }
    });

    async function apriDettaglio(lavoratore: DipendenteEsteso) {
        selectedDipendente = lavoratore;
        isLoadingDettaglio = true;
        try {
            const [resDocs, resDpis] = await Promise.all([
                DocumentoService.getDocumentiByAzienda(lavoratore.idUtente),
                LavoratoreService.getDpiByLavoratore(lavoratore.idUtente)
            ]);

            documentiCorrenti = resDocs.sort((a, b) => {
                const oggi = new Date().getTime();
                const scadA = new Date(a.dataScadenza).getTime();
                const scadB = new Date(b.dataScadenza).getTime();
                const aScaduto = scadA < oggi;
                const bScaduto = scadB < oggi;

                if (aScaduto && !bScaduto) return -1;
                if (!aScaduto && bScaduto) return 1;
                return scadA - scadB;
            });

            const dpis = resDpis as unknown as DpiEsteso[];
            dpiCorrenti = dpis.sort((a, b) => {
                const aScaduto = isDPIScaduto(a.dataScadenzaRevisione);
                const bScaduto = isDPIScaduto(b.dataScadenzaRevisione);

                if (aScaduto && !bScaduto) return -1;
                if (!aScaduto && bScaduto) return 1;

                const scadA = new Date(a.dataScadenzaRevisione || '9999-12-31').getTime();
                const scadB = new Date(b.dataScadenzaRevisione || '9999-12-31').getTime();
                return scadA - scadB;
            });

        } catch (error) {
            console.error("Errore caricamento dettaglio dipendente:", error);
        } finally {
            isLoadingDettaglio = false;
        }
    }

    function apreGmail(email: string) {
        if (!email) {
            alert("Nessuna email registrata per questo dipendente.");
            return;
        }
        window.open(`https://mail.google.com/mail/?view=cm&fs=1&to=${email}`, '_blank');
    }

    async function vaiInChat(idUtente: string | number | undefined) {
        if (!idUtente) return;
        // eslint-disable-next-line svelte/no-navigation-without-resolve
        return await goto(`/dashboard/admin/comunicazioni?chatId=${idUtente}`);
    }

    async function salvaDipendente() {
        if (!isFormValid) return;
        isSaving = true;
        try {
            const payload = {
                nome: formDipendente.nome,
                cognome: formDipendente.cognome,
                codiceFiscale: formDipendente.codiceFiscale,
                email: formDipendente.email,
                passwordHash: formDipendente.password
            };

            const nuovo = await LavoratoreService.create(
                formDipendente.idAzienda,
                payload as unknown as DipendenteRequest
            );

            const aziendaSelezionata = aziende.find(a => String(a.idUtente) === String(formDipendente.idAzienda));
            const esteso: DipendenteEsteso = {
                ...nuovo,
                nomeAzienda: aziendaSelezionata?.ragioneSociale,
                idAzienda: formDipendente.idAzienda
            };

            lavoratori = [esteso, ...lavoratori];
            showAddModal = false;
            formDipendente = { nome: '', cognome: '', codiceFiscale: '', email: '', idAzienda: '', password: '' };
        } catch (error) {
            console.error("Dettaglio errore registrazione:", error);
            alert("Errore durante la registrazione. Controlla la console per i dettagli.");
        } finally {
            isSaving = false;
        }
    }

    function preparaEliminazione(l: DipendenteEsteso | null) {
        if (!l) return;
        dipendenteDaEliminare = l;
        showDeleteModal = true;
    }

    async function confermaEliminazione() {
        if (!dipendenteDaEliminare) return;
        try {
            await LavoratoreService.delete(dipendenteDaEliminare.idUtente);
            lavoratori = lavoratori.filter(l => String(l.idUtente) !== String(dipendenteDaEliminare?.idUtente));
            showDeleteModal = false;
            if (selectedDipendente && String(selectedDipendente.idUtente) === String(dipendenteDaEliminare.idUtente)) {
                selectedDipendente = null;
            }
            dipendenteDaEliminare = null;
        } catch {
            alert("Impossibile eliminare il dipendente.");
        }
    }

    async function scaricaDoc(doc: Documento) {
        try {
            const b = await DocumentoService.downloadDocumento(doc.idDocumento);
            const u = URL.createObjectURL(b);
            const a = document.createElement('a');
            a.href = u;
            a.download = doc.filePath.split('/').pop() || 'attestato.pdf';
            a.click();
            URL.revokeObjectURL(u);
        } catch {
            alert("Errore download.");
        }
    }

    function preparaEliminaDoc(doc: Documento) { docDaEliminare = doc; showDeleteDocModal = true; }

    async function confermaEliminaDoc() {
        if (!docDaEliminare) return;
        try {
            await DocumentoService.deleteDocumento(docDaEliminare.idDocumento);
            documentiCorrenti = documentiCorrenti.filter(d => d.idDocumento !== docDaEliminare?.idDocumento);
            showDeleteDocModal = false;
            docDaEliminare = null;
        } catch {
            alert("Errore eliminazione attestato.");
        }
    }

    function isDPIScaduto(dataScadenza?: string) {
        if (!dataScadenza || dataScadenza === '9999-12-31') return false;
        // eslint-disable-next-line svelte/prefer-svelte-reactivity
        const oggi = new Date();
        oggi.setHours(0, 0, 0, 0);
        // eslint-disable-next-line svelte/prefer-svelte-reactivity
        const scad = new Date(dataScadenza);
        scad.setHours(0, 0, 0, 0);
        return scad < oggi;
    }

    function sollecitaViaEmail(dpi: DpiEsteso) {
        if (!selectedDipendente || !selectedDipendente.idAzienda) return;
        const azienda = aziende.find(a => String(a.idUtente) === String(selectedDipendente?.idAzienda));

        if (!azienda || !azienda.email) {
            alert("Attenzione: L'azienda associata non ha un'email registrata.");
            return;
        }

        const subject = encodeURIComponent(`URGENTE: Rinnovo DPI Scaduto - ${selectedDipendente.nome} ${selectedDipendente.cognome}`);
        // eslint-disable-next-line svelte/prefer-svelte-reactivity
        const dataScad = new Date(dpi.dataScadenzaRevisione || '').toLocaleDateString();
        const nomeDpiReale = dpi.tipo === 'ALTRO' && dpi.nomeDpi ? dpi.nomeDpi : dpi.tipo.replace(/_/g, ' ');

        const body = encodeURIComponent(
            `Gentile ${azienda.ragioneSociale},\n\n` +
            `Vi segnaliamo che il DPI (${nomeDpiReale}) assegnato a ${selectedDipendente.nome} ${selectedDipendente.cognome} è SCADUTO il ${dataScad}.\n\n` +
            `Vi invitiamo a provvedere al rinnovo immediato.\n\nCordiali saluti,\nTeam NorLan`
        );
        window.open(`https://mail.google.com/mail/?view=cm&fs=1&to=${azienda.email}&su=${subject}&body=${body}`, '_blank');
    }

    async function sollecitaViaChat(dpi: DpiEsteso) {
        if (!selectedDipendente || !selectedDipendente.idAzienda) return;

        // eslint-disable-next-line svelte/prefer-svelte-reactivity
        const dataScad = new Date(dpi.dataScadenzaRevisione || '').toLocaleDateString();
        const nomeDpiReale = dpi.tipo === 'ALTRO' && dpi.nomeDpi ? dpi.nomeDpi : dpi.tipo.replace(/_/g, ' ');

        const testoMessaggio = encodeURIComponent(
            `Salve, vi segnaliamo che il DPI (${nomeDpiReale}) assegnato al lavoratore ${selectedDipendente.nome} ${selectedDipendente.cognome} risulta scaduto in data ${dataScad}. Vi invitiamo a rinnovare questo dispositivo il prima possibile.`
        );

        await goto(`/dashboard/admin/comunicazioni?chatId=${selectedDipendente.idAzienda}&msg=${testoMessaggio}`);
    }

    async function salvaDpi() {
        if (!selectedDipendente) return;
        isSavingDpi = true;
        try {
            const payload = {
                tipo: formDpi.tipo,
                nomeDpi: formDpi.tipo === 'ALTRO' ? formDpi.nomeDpi : undefined,
                dataConsegna: formDpi.dataConsegna ? formDpi.dataConsegna : undefined,
                dataScadenzaRevisione: formDpi.dataScadenzaRevisione
            };

            const nuovoDpi = await LavoratoreService.assegnaDpi(
                selectedDipendente.idUtente,
                payload as any
            );

            dpiCorrenti = [...dpiCorrenti, nuovoDpi as unknown as DpiEsteso];

            showAddDpiModal = false;
            formDpi = { tipo: '', nomeDpi: '', dataConsegna: '', dataScadenzaRevisione: '' };
        } catch (error) {
            console.error("Errore salvataggio DPI:", error);
            alert("Errore durante l'assegnazione del DPI.");
        } finally {
            isSavingDpi = false;
        }
    }

    function preparaEliminaDPI(dpi: DpiEsteso) { dpiDaEliminare = dpi; showDeleteDpiModal = true; }

    async function confermaEliminaDPI() {
        if (!dpiDaEliminare) return;
        try {
            const idReale = dpiDaEliminare.idAssegnazione || (dpiDaEliminare as any).id;
            if (idReale) {
                await LavoratoreService.deleteDpi(idReale);
            }

            dpiCorrenti = dpiCorrenti.filter(d => d !== dpiDaEliminare);
            showDeleteDpiModal = false;
            dpiDaEliminare = null;
        } catch (error) {
            console.error("Errore eliminazione DPI dal Database:", error);
            alert("Errore eliminazione DPI.");
        }
    }

    function formattaScadenza(data?: string) {
        if (!data || data === '9999-12-31') return 'Senza scadenza';
        // eslint-disable-next-line svelte/prefer-svelte-reactivity
        return new Date(data).toLocaleDateString();
    }
</script>

<div in:fade class="max-w-7xl mx-auto p-6">

    {#if !selectedDipendente}
        <div class="mb-10 flex justify-between items-start">
            <div>
                <h1 class="text-4xl font-extrabold text-[#1B4B6B] uppercase tracking-tighter">Gestione Personale</h1>
                <p class="text-gray-500 font-bold uppercase text-xs tracking-tighter">Anagrafica centralizzata lavoratori NorLan.</p>
            </div>
            <button
                    onclick={() => (showAddModal = true)}
                    class="bg-white text-[#1B4B6B] border-2 border-[#1B4B6B] px-8 py-3.5 rounded-xl font-extrabold uppercase text-xs shadow-lg hover:bg-[#1B4B6B] hover:text-white transition-all flex items-center gap-3"
            >
                <UserPlus size={18} /> Nuovo Dipendente
            </button>
        </div>

        <div class="mb-8 flex gap-4">
            <div class="relative w-72 group">
                <Search class="absolute left-3 top-1/2 -translate-y-1/2 text-gray-400 group-focus-within:text-[#1B4B6B] transition-colors" size={16} />
                <input bind:value={searchQuery} type="text" placeholder="Cerca lavoratore o azienda..." class="w-full pl-10 pr-4 py-2.5 bg-white border border-gray-200 rounded-xl text-xs focus:ring-2 focus:ring-[#1B4B6B] outline-none transition-all font-bold uppercase" />
            </div>
        </div>

        {#if isLoading}
            <div class="py-20 text-center"><Loader2 size={40} class="animate-spin mx-auto text-[#1B4B6B]" /></div>
        {:else if filteredLavoratori.length === 0}
            <div class="py-20 text-center bg-gray-50 rounded-3xl border-2 border-dashed border-gray-200">
                <Users size={48} class="mx-auto text-gray-300 mb-4" />
                <p class="text-gray-400 font-bold uppercase text-xs">Nessun dipendente trovato</p>
            </div>
        {:else}
            <!-- Correzione dell'errore (key) aggiunta qui -->
            {#each Object.entries(lavoratoriRaggruppati()) as [nomeAzienda, dipendentiAzienda] (nomeAzienda)}
                <div class="mb-12">
                    <div class="flex items-center gap-3 mb-6 pb-2 border-b-2 border-gray-100">
                        <div class="p-2 bg-[#1B4B6B]/10 text-[#1B4B6B] rounded-lg">
                            <Building2 size={20} />
                        </div>
                        <h2 class="text-2xl font-black text-[#1B4B6B] uppercase tracking-tighter">
                            {nomeAzienda}
                        </h2>
                        <span class="ml-2 bg-gray-100 text-gray-500 text-[10px] px-3 py-1 rounded-full font-black uppercase">
                            {dipendentiAzienda.length} Lavorator{dipendentiAzienda.length === 1 ? 'e' : 'i'}
                        </span>
                    </div>

                    <div class="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6">
                        {#each dipendentiAzienda as l (l.idUtente)}
                            <div class="bg-white rounded-3xl border border-gray-100 shadow-sm hover:shadow-xl transition-all group relative flex flex-col h-full overflow-hidden hover:border-[#1B4B6B]/30" in:scale>

                                <div role="button" tabindex="0" onclick={() => apriDettaglio(l)} onkeydown={(e) => e.key === 'Enter' && apriDettaglio(l)} class="p-6 pb-4 cursor-pointer flex-1">
                                    <button
                                            onclick={(e) => { e.stopPropagation(); preparaEliminazione(l); }}
                                            class="absolute top-4 right-4 p-2 text-gray-300 hover:text-red-600 opacity-0 group-hover:opacity-100 transition-all z-10 hover:bg-red-50 rounded-lg"
                                    >
                                        <Trash2 size={18} />
                                    </button>

                                    <div class="flex items-center gap-4 mb-6">
                                        <div class="w-14 h-14 bg-[#1B4B6B]/10 text-[#1B4B6B] rounded-2xl flex items-center justify-center font-black text-lg group-hover:bg-[#1B4B6B] group-hover:text-white transition-all">
                                            {l.nome[0]}{l.cognome[0]}
                                        </div>
                                        <div>
                                            <h3 class="font-extrabold text-[#1B4B6B] text-lg uppercase leading-tight">{l.nome} {l.cognome}</h3>
                                            <div class="flex items-center gap-1 text-gray-400 mt-1">
                                                <IdCard size={12} class="text-[#1B4B6B]"/>
                                                <span class="text-[9px] font-bold uppercase truncate max-w-[150px] text-[#1B4B6B]">
                                                    {l.codiceFiscale}
                                                </span>
                                            </div>
                                        </div>
                                    </div>
                                </div>

                                <button onclick={() => apriDettaglio(l)} class="mt-auto w-full p-6 pt-4 border-t border-gray-50 flex justify-between items-center hover:bg-gray-50/50 transition-colors">
                                    <div class="flex items-center gap-2"><FileText size={16} class="text-[#1B4B6B]"/><span class="text-[10px] font-bold text-gray-400 uppercase italic">Vedi Dettagli Lavoratore</span></div>
                                    <ChevronRight size={20} class="text-[#1B4B6B]" />
                                </button>
                            </div>
                        {/each}
                    </div>
                </div>
            {/each}
        {/if}

    {:else}
        <div in:fade>
            <button onclick={() => (selectedDipendente = null)} class="flex items-center gap-2 text-[#1B4B6B] font-extrabold uppercase text-[10px] mb-8 hover:gap-3 transition-all"><ChevronLeft size={16} /> Torna all'elenco dipendenti</button>

            <div class="bg-white rounded-3xl shadow-xl border border-gray-100 overflow-hidden mb-12">
                <div class="bg-[#1B4B6B] p-10 text-white flex justify-between items-end relative">
                    <div class="flex items-center gap-6">
                        <div class="w-24 h-24 bg-white text-[#1B4B6B] rounded-3xl flex items-center justify-center font-black text-4xl shadow-lg">
                            {selectedDipendente.nome[0]}{selectedDipendente.cognome[0]}
                        </div>
                        <div>
                            <div class="flex items-center gap-3 mb-3">
                                <span class="bg-white/20 border border-white/20 text-white text-[10px] font-black px-4 py-1.5 rounded-full uppercase flex items-center gap-2"><Building2 size={12}/> {selectedDipendente.nomeAzienda}</span>
                            </div>
                            <h1 class="text-5xl font-extrabold uppercase tracking-tighter">{selectedDipendente.nome} {selectedDipendente.cognome}</h1>
                        </div>
                    </div>

                    <div class="flex items-center gap-3">
                        <button onclick={() => apreGmail(selectedDipendente?.email || '')} class="flex items-center gap-2 bg-white text-[#1B4B6B] px-6 py-3.5 rounded-2xl transition-all font-extrabold uppercase text-[10px] shadow-xl hover:bg-gray-100 hover:scale-105">
                            <Mail size={16} /> Manda Mail
                        </button>
                        <button onclick={() => vaiInChat(selectedDipendente?.idUtente)} class="flex items-center gap-2 bg-white/20 border border-white/20 text-white px-6 py-3.5 rounded-2xl transition-all font-extrabold uppercase text-[10px] shadow-xl hover:bg-white/30 hover:scale-105">
                            <MessageSquare size={16} /> Contatta
                        </button>
                        <button onclick={() => preparaEliminazione(selectedDipendente)} class="flex items-center gap-2 bg-red-600 text-white px-6 py-3.5 rounded-2xl transition-all font-extrabold uppercase text-[10px] border border-red-500/20 shadow-xl hover:bg-red-700 hover:scale-105">
                            <Trash2 size={16} /> Rimuovi
                        </button>
                    </div>
                </div>

                <div class="p-8 grid grid-cols-1 md:grid-cols-2 gap-8 bg-gray-50/30">
                    <div>
                        <p class="text-[10px] font-bold text-gray-400 uppercase mb-1 flex items-center gap-1"><IdCard size={12}/> Codice Fiscale</p>
                        <p class="text-lg font-mono font-extrabold text-[#1B4B6B] tracking-widest">{selectedDipendente.codiceFiscale}</p>
                    </div>
                    <div>
                        <p class="text-[10px] font-bold text-gray-400 uppercase mb-1 flex items-center gap-1"><Mail size={12}/> Email Contatto</p>
                        <p class="text-lg font-bold text-[#1B4B6B] lowercase">{selectedDipendente.email || 'Nessuna email fornita'}</p>
                    </div>
                </div>
            </div>

            {#if isLoadingDettaglio}
                <div class="py-20 text-center"><Loader2 size={40} class="animate-spin mx-auto text-[#1B4B6B]" /></div>
            {:else}
                <div class="grid grid-cols-1 lg:grid-cols-2 gap-8">

                    <div class="space-y-6">
                        <div class="flex items-center justify-between">
                            <div class="flex items-center gap-3">
                                <div class="p-2.5 bg-[#1B4B6B]/10 text-[#1B4B6B] rounded-xl shadow-inner"><FileText size={20} /></div>
                                <h2 class="text-xl font-black text-[#1B4B6B] uppercase tracking-tighter">Attestati ({documentiCorrenti.length})</h2>
                            </div>
                        </div>

                        {#if documentiCorrenti.length > 0}
                            <div class="space-y-4">
                                {#each documentiCorrenti as doc (doc.idDocumento)}
                                    {@const docScaduto = new Date(doc.dataScadenza).getTime() < new Date().getTime()}
                                    <div class="p-5 rounded-2xl border shadow-sm transition-all {docScaduto ? 'bg-red-50/50 border-red-200' : 'bg-white border-gray-100 hover:shadow-md'}">
                                        <div class="flex justify-between items-start mb-3">
                                            <div class="flex items-center gap-3">
                                                <div class="p-2 rounded-xl {docScaduto ? 'bg-red-100 text-red-600' : 'bg-gray-50 text-[#1B4B6B]'}">
                                                    <FileText size={16} />
                                                </div>
                                                <div>
                                                    <h4 class="font-extrabold uppercase text-xs leading-tight {docScaduto ? 'text-red-700' : 'text-[#1B4B6B]'}">{doc.tipologia.replace(/_/g, ' ')}</h4>
                                                    <p class="text-[9px] text-gray-400 font-bold uppercase mt-0.5">{doc.modulo}</p>
                                                </div>
                                            </div>
                                            <div class="flex gap-1">
                                                <button onclick={() => scaricaDoc(doc)} class="p-1.5 text-gray-400 hover:text-[#1B4B6B] transition-all bg-white rounded-lg hover:bg-gray-100 shadow-sm"><Download size={14} /></button>
                                                <button onclick={() => preparaEliminaDoc(doc)} class="p-1.5 text-gray-400 hover:text-red-600 transition-all bg-white rounded-lg hover:bg-red-50 shadow-sm"><Trash2 size={14} /></button>
                                            </div>
                                        </div>
                                        <div class="flex items-center justify-between pt-3 border-t border-gray-50">
                                            <div class="flex items-center gap-1.5 text-gray-400">
                                                <Calendar size={10} />
                                                <span class="text-[9px] font-bold uppercase">Scad: {new Date(doc.dataScadenza).toLocaleDateString()}</span>
                                            </div>
                                            {#if docScaduto}
                                                <span class="text-[8px] font-black px-2 py-1 bg-red-50 text-red-600 border border-red-100 rounded-md uppercase">Scaduto</span>
                                            {:else}
                                                <span class="text-[8px] font-black px-2 py-1 bg-green-50 text-green-600 border border-green-100 rounded-md uppercase">Valido</span>
                                            {/if}
                                        </div>
                                    </div>
                                {/each}
                            </div>
                        {:else}
                            <div class="bg-white rounded-3xl p-8 text-center border-2 border-dashed border-gray-200 text-gray-300 uppercase font-bold text-[10px] italic">
                                Nessun attestato associato al lavoratore
                            </div>
                        {/if}
                    </div>

                    <div class="space-y-6">
                        <div class="flex items-center justify-between">
                            <div class="flex items-center gap-3">
                                <div class="p-2.5 bg-green-100 text-green-600 rounded-xl shadow-inner"><ShieldCheck size={20} /></div>
                                <h2 class="text-xl font-black text-[#1B4B6B] uppercase tracking-tighter">DPI Consegnati ({dpiCorrenti.length})</h2>
                            </div>
                            <button onclick={() => showAddDpiModal = true} class="bg-green-600 text-white px-4 py-2 rounded-xl font-bold uppercase text-[10px] flex items-center gap-2 hover:bg-green-700 transition-all shadow-md">
                                <Plus size={16} /> Assegna DPI
                            </button>
                        </div>

                        {#if dpiCorrenti.length > 0}
                            <div class="space-y-4">
                                {#each dpiCorrenti as dpi (dpi.idAssegnazione)}
                                    {@const scaduto = isDPIScaduto(dpi.dataScadenzaRevisione)}
                                    {@const nomeDpiReale = dpi.tipo === 'ALTRO' && dpi.nomeDpi ? dpi.nomeDpi : (dpi.tipo || '').replace(/_/g, ' ')}

                                    <div class="p-5 rounded-2xl border shadow-sm transition-all {scaduto ? 'bg-red-50/50 border-red-200' : 'bg-white border-gray-100 hover:shadow-md'}">
                                        <div class="flex justify-between items-start mb-3">
                                            <div class="flex items-center gap-3">
                                                <div class="p-2 rounded-xl {scaduto ? 'bg-red-100 text-red-600' : 'bg-green-50 text-green-600'}">
                                                    <ShieldCheck size={16} />
                                                </div>
                                                <div>
                                                    <h4 class="font-extrabold uppercase text-xs leading-tight {scaduto ? 'text-red-700' : 'text-[#1B4B6B]'}">
                                                        {nomeDpiReale}
                                                    </h4>
                                                </div>
                                            </div>
                                            <button onclick={() => preparaEliminaDPI(dpi)} class="p-1.5 text-gray-400 hover:text-red-600 transition-all rounded-lg {scaduto ? 'hover:bg-red-100 bg-white' : 'hover:bg-red-50 bg-gray-50'}">
                                                <Trash2 size={14} />
                                            </button>
                                        </div>

                                        <div class="flex flex-col gap-2 pt-3 border-t {scaduto ? 'border-red-100' : 'border-gray-50'}">
                                            <div class="flex items-center gap-1.5 {scaduto ? 'text-red-400' : 'text-gray-400'}">
                                                <Calendar size={10} />
                                                <span class="text-[9px] font-bold uppercase">Consegnato: {formattaScadenza(dpi.dataConsegna)}</span>
                                            </div>
                                            <div class="flex items-center justify-between">
                                                <div class="flex items-center gap-1.5 {scaduto ? 'text-red-600 font-black' : 'text-gray-400'}">
                                                    <AlertTriangle size={10} />
                                                    <span class="text-[9px] font-bold uppercase">
                                                        {scaduto ? 'Scaduto il:' : 'Scadenza:'} {formattaScadenza(dpi.dataScadenzaRevisione)}
                                                    </span>
                                                </div>
                                            </div>

                                            {#if scaduto}
                                                <div class="mt-2 pt-2 border-t border-red-100 border-dashed">
                                                    <p class="text-[8px] font-black uppercase text-red-400 mb-1.5 text-center tracking-tighter">
                                                        Invia sollecito a {selectedDipendente.nomeAzienda}:
                                                    </p>
                                                    <div class="flex gap-2">
                                                        <button
                                                                onclick={() => sollecitaViaEmail(dpi)}
                                                                class="flex-1 py-2 bg-red-600 text-white rounded-lg text-[9px] font-black uppercase flex items-center justify-center gap-1.5 shadow-sm hover:bg-red-700 transition-colors"
                                                        >
                                                            <Mail size={12} /> Email
                                                        </button>
                                                        <button
                                                                onclick={() => sollecitaViaChat(dpi)}
                                                                class="flex-1 py-2 bg-[#1B4B6B] text-white rounded-lg text-[9px] font-black uppercase flex items-center justify-center gap-1.5 shadow-sm hover:bg-[#1B4B6B]/90 transition-colors"
                                                        >
                                                            <MessageSquare size={12} /> Chat
                                                        </button>
                                                    </div>
                                                </div>
                                            {/if}
                                        </div>
                                    </div>
                                {/each}
                            </div>
                        {:else}
                            <div class="bg-white rounded-3xl p-8 text-center border-2 border-dashed border-gray-200 text-gray-300 uppercase font-bold text-[10px] italic">
                                <ShieldCheck size={32} class="mx-auto mb-3 opacity-20"/>
                                Nessun DPI registrato per il lavoratore
                            </div>
                        {/if}
                    </div>

                </div>
            {/if}
        </div>
    {/if}

    {#if showAddModal}
        <div class="fixed inset-0 bg-[#1B4B6B]/40 backdrop-blur-sm flex items-center justify-center z-[100] p-4" transition:fade>
            <div class="bg-white rounded-3xl shadow-2xl w-full max-w-lg overflow-hidden" in:scale>
                <div class="bg-[#1B4B6B] p-6 text-white flex justify-between items-center">
                    <h2 class="text-xl font-black uppercase tracking-tighter flex items-center gap-2"><UserPlus size={20}/> Registra Lavoratore</h2>
                    <button onclick={() => (showAddModal = false)} class="hover:rotate-90 transition-transform"><X size={24}/></button>
                </div>

                <div class="p-8 space-y-4">
                    <div class="space-y-1">
                        <label class="block text-[10px] font-bold text-[#1B4B6B] uppercase">Azienda di Appartenenza *</label>
                        <select bind:value={formDipendente.idAzienda} class="w-full p-3 bg-gray-50 border-none rounded-xl text-sm focus:ring-2 focus:ring-[#1B4B6B] outline-none font-bold">
                            <option value="">Seleziona azienda...</option>
                            {#each aziende as a (a.idUtente)}
                                <option value={a.idUtente}>{a.ragioneSociale}</option>
                            {/each}
                        </select>
                    </div>

                    <div class="grid grid-cols-2 gap-4">
                        <div class="space-y-1">
                            <label class="block text-[10px] font-bold text-[#1B4B6B] uppercase">Nome *</label>
                            <input bind:value={formDipendente.nome} placeholder="Es: Mario" class="w-full p-3 bg-gray-50 border-none rounded-xl text-sm focus:ring-2 focus:ring-[#1B4B6B] outline-none" />
                        </div>
                        <div class="space-y-1">
                            <label class="block text-[10px] font-bold text-[#1B4B6B] uppercase">Cognome *</label>
                            <input bind:value={formDipendente.cognome} placeholder="Es: Rossi" class="w-full p-3 bg-gray-50 border-none rounded-xl text-sm focus:ring-2 focus:ring-[#1B4B6B] outline-none" />
                        </div>
                    </div>

                    <div class="grid grid-cols-2 gap-4">
                        <div class="space-y-1">
                            <label class="block text-[10px] font-bold text-[#1B4B6B] uppercase">Codice Fiscale *</label>
                            <input bind:value={formDipendente.codiceFiscale} maxlength="16" placeholder="RSSMRA..." class="w-full p-3 bg-gray-50 border-none rounded-xl text-sm font-mono focus:ring-2 focus:ring-[#1B4B6B] outline-none uppercase" />
                        </div>
                        <div class="space-y-1">
                            <label class="block text-[10px] font-bold text-[#1B4B6B] uppercase">Email Accesso *</label>
                            <input bind:value={formDipendente.email} type="email" placeholder="m.rossi@email.it" class="w-full p-3 bg-gray-50 border-none rounded-xl text-sm focus:ring-2 focus:ring-[#1B4B6B] outline-none" />
                        </div>
                    </div>

                    <div class="space-y-1">
                        <label class="block text-[10px] font-bold text-[#1B4B6B] uppercase tracking-tight">Password Temporanea *</label>
                        <input bind:value={formDipendente.password} type="password" placeholder="••••••••" class="w-full p-3 bg-gray-50 border-none rounded-xl text-sm focus:ring-2 focus:ring-[#1B4B6B] outline-none" />
                        <div class="mt-2 flex items-start gap-2">
                            <div class="mt-0.5 text-orange-500"><AlertTriangle size={12}/></div>
                            <p class="text-[8px] text-gray-400 font-bold uppercase leading-tight">
                                Nota: Il lavoratore dovrà obbligatoriamente cambiare questa password al suo primo accesso.
                            </p>
                        </div>
                    </div>
                </div>

                <div class="p-8 bg-gray-50 flex justify-end gap-4 border-t border-gray-100">
                    <button onclick={() => (showAddModal = false)} class="px-6 py-3 text-[10px] font-black uppercase text-gray-400 hover:text-gray-600 transition-colors">Annulla</button>
                    <button
                            onclick={salvaDipendente}
                            disabled={!isFormValid || isSaving}
                            class="bg-[#1B4B6B] text-white px-8 py-3 rounded-xl text-[10px] font-black uppercase shadow-lg disabled:opacity-50 flex items-center gap-2 hover:bg-[#153a54] transition-colors"
                    >
                        {#if isSaving}<Loader2 size={14} class="animate-spin"/>{:else}<UserPlus size={14}/>{/if} Registra
                    </button>
                </div>
            </div>
        </div>
    {/if}

    <!-- MODALE INSERIMENTO DPI -->
    {#if showAddDpiModal}
        <div class="fixed inset-0 bg-[#1B4B6B]/40 backdrop-blur-sm flex items-center justify-center z-[100] p-4" transition:fade>
            <div class="bg-white rounded-3xl shadow-2xl w-full max-w-lg overflow-hidden" in:scale>
                <div class="bg-green-600 p-6 text-white flex justify-between items-center">
                    <h2 class="text-xl font-black uppercase tracking-tighter flex items-center gap-2"><ShieldCheck size={20}/> Assegna DPI</h2>
                    <button onclick={() => (showAddDpiModal = false)} class="hover:rotate-90 transition-transform"><X size={24}/></button>
                </div>

                <div class="p-8 space-y-4">
                    <div class="space-y-1">
                        <label class="block text-[10px] font-bold text-[#1B4B6B] uppercase">Tipologia DPI *</label>
                        <select bind:value={formDpi.tipo} class="w-full p-3 bg-gray-50 border-none rounded-xl text-sm focus:ring-2 focus:ring-green-600 outline-none font-bold">
                            <option value="">Seleziona tipologia...</option>
                            <option value="ELMETTO">Elmetto</option>
                            <option value="GUANTI">Guanti</option>
                            <option value="SCARPE_ANTINFORTUNISTICHE">Scarpe Antinfortunistiche</option>
                            <option value="OCCHIALI">Occhiali</option>
                            <option value="ALTRO">Altro</option>
                        </select>
                    </div>

                    {#if formDpi.tipo === 'ALTRO'}
                        <div class="space-y-1" transition:slide>
                            <label class="block text-[10px] font-bold text-[#1B4B6B] uppercase">Nome DPI Personalizzato *</label>
                            <input bind:value={formDpi.nomeDpi} type="text" placeholder="Specifica il nome del DPI..." class="w-full p-3 bg-gray-50 border-none rounded-xl text-sm focus:ring-2 focus:ring-green-600 outline-none" />
                        </div>
                    {/if}

                    <div class="grid grid-cols-2 gap-4">
                        <div class="space-y-1">
                            <label class="block text-[10px] font-bold text-[#1B4B6B] uppercase">Data Consegna</label>
                            <input bind:value={formDpi.dataConsegna} type="date" class="w-full p-3 bg-gray-50 border-none rounded-xl text-sm focus:ring-2 focus:ring-green-600 outline-none" />
                        </div>
                        <div class="space-y-1">
                            <label class="block text-[10px] font-bold text-[#1B4B6B] uppercase">Scadenza Revisione *</label>
                            <input bind:value={formDpi.dataScadenzaRevisione} type="date" class="w-full p-3 bg-gray-50 border-none rounded-xl text-sm focus:ring-2 focus:ring-green-600 outline-none" />
                        </div>
                    </div>
                </div>

                <div class="p-8 bg-gray-50 flex justify-end gap-4 border-t border-gray-100">
                    <button onclick={() => (showAddDpiModal = false)} class="px-6 py-3 text-[10px] font-black uppercase text-gray-400 hover:text-gray-600 transition-colors">Annulla</button>
                    <button
                            onclick={salvaDpi}
                            disabled={!formDpi.tipo || (formDpi.tipo === 'ALTRO' && !formDpi.nomeDpi.trim()) || !formDpi.dataScadenzaRevisione || isSavingDpi}
                            class="bg-green-600 text-white px-8 py-3 rounded-xl text-[10px] font-black uppercase shadow-lg disabled:opacity-50 flex items-center gap-2 hover:bg-green-700 transition-colors"
                    >
                        {#if isSavingDpi}<Loader2 size={14} class="animate-spin"/>{:else}<ShieldCheck size={14}/>{/if} Assegna
                    </button>
                </div>
            </div>
        </div>
    {/if}

    {#if showDeleteModal}
        <div class="fixed inset-0 bg-red-900/20 backdrop-blur-sm flex items-center justify-center z-[110] p-4" transition:fade>
            <div class="bg-white rounded-3xl shadow-2xl w-full max-w-md overflow-hidden" in:scale>
                <div class="p-8 text-center">
                    <div class="w-20 h-20 bg-red-50 text-red-600 rounded-full flex items-center justify-center mx-auto mb-6"><Trash2 size={40}/></div>
                    <h2 class="text-2xl font-black text-[#1B4B6B] uppercase tracking-tighter mb-2">Rimuovere dipendente?</h2>
                    <p class="text-sm text-gray-400 mb-8">Il lavoratore <span class="font-bold text-[#1B4B6B]">{dipendenteDaEliminare?.nome} {dipendenteDaEliminare?.cognome}</span> verrà rimosso definitivamente dal sistema.</p>
                    <div class="flex flex-col gap-3">
                        <button onclick={confermaEliminazione} class="w-full bg-red-600 text-white py-4 rounded-2xl font-black uppercase text-[10px] shadow-lg shadow-red-200 transition-all hover:bg-red-700">Sì, elimina definitivamente</button>
                        <button onclick={() => (showDeleteModal = false)} class="w-full py-4 text-[10px] font-black uppercase text-gray-400 hover:text-gray-600">No, annulla l'operazione</button>
                    </div>
                </div>
            </div>
        </div>
    {/if}

    {#if showDeleteDocModal}
        <div class="fixed inset-0 bg-red-900/20 backdrop-blur-sm flex items-center justify-center z-[130] p-4" transition:fade>
            <div class="bg-white rounded-3xl shadow-2xl w-full max-w-md overflow-hidden" in:scale>
                <div class="p-8 text-center">
                    <div class="w-20 h-20 bg-red-50 text-red-600 rounded-full flex items-center justify-center mx-auto mb-6"><Trash2 size={40}/></div>
                    <h2 class="text-2xl font-black text-[#1B4B6B] uppercase tracking-tighter mb-2">Eliminare l'attestato?</h2>
                    <p class="text-sm text-gray-400 mb-8">Stai per rimuovere definitivamente: <br><span class="font-bold text-[#1B4B6B]">{docDaEliminare?.tipologia.replace(/_/g, ' ')}</span></p>
                    <div class="flex flex-col gap-3">
                        <button onclick={confermaEliminaDoc} class="w-full bg-red-600 text-white py-4 rounded-2xl font-black uppercase text-[10px] shadow-lg shadow-red-200 transition-all hover:bg-red-700">Conferma Eliminazione</button>
                        <button onclick={() => (showDeleteDocModal = false)} class="w-full py-4 text-[10px] font-black uppercase text-gray-400 hover:text-gray-600">Annulla</button>
                    </div>
                </div>
            </div>
        </div>
    {/if}

    {#if showDeleteDpiModal}
        <div class="fixed inset-0 bg-red-900/20 backdrop-blur-sm flex items-center justify-center z-[130] p-4" transition:fade>
            <div class="bg-white rounded-3xl shadow-2xl w-full max-w-md overflow-hidden" in:scale>
                <div class="p-8 text-center">
                    <div class="w-20 h-20 bg-red-50 text-red-600 rounded-full flex items-center justify-center mx-auto mb-6"><Trash2 size={40}/></div>
                    <h2 class="text-2xl font-black text-[#1B4B6B] uppercase tracking-tighter mb-2">Rimuovere DPI?</h2>
                    <p class="text-sm text-gray-400 mb-8">Stai annullando l'assegnazione di: <br><span class="font-bold text-[#1B4B6B]">{dpiDaEliminare?.tipo.replace(/_/g, ' ')}</span></p>
                    <div class="flex flex-col gap-3">
                        <button onclick={confermaEliminaDPI} class="w-full bg-red-600 text-white py-4 rounded-2xl font-black uppercase text-[10px] shadow-lg shadow-red-200 transition-all hover:bg-red-700">Conferma Rimozione</button>
                        <button onclick={() => (showDeleteDpiModal = false)} class="w-full py-4 text-[10px] font-black uppercase text-gray-400 hover:text-gray-600">Annulla</button>
                    </div>
                </div>
            </div>
        </div>
    {/if}

</div>