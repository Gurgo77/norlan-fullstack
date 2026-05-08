<script lang="ts">
    import { onMount } from 'svelte';
    import { page } from '$app/stores';
    import { goto } from '$app/navigation';
    import { resolveRoute } from '$app/paths';
    import { fade, scale } from 'svelte/transition';
    import {
        Users, UserPlus, Trash2, Search, Mail, Building2,
        IdCard, Loader2, X, AlertTriangle, ChevronLeft,
        FileText, ShieldCheck, Download, Calendar, MessageSquare, AlertCircle, CheckCircle, Clock, Edit3
    } from 'lucide-svelte';

    import { LavoratoreService, type DipendenteDTO, type DipendenteRequest } from '$lib/services/LavoratoreService';
    import { AnagraficaService } from '$lib/services/AnagraficaService';
    import { DocumentoService } from '$lib/services/DocumentoService';
    import { Azienda, type AziendaData } from '$lib/models/Azienda';
    import { Documento } from '$lib/models/Documento';
    import type { AssegnazioneDPI } from '$lib/models/AssegnazioneDPI';
    import DipendenteCard from '$lib/Components/Features/Anagrafica/DipendenteCard.svelte';
    import DettagliCard from '$lib/Components/Features/Anagrafica/DettagliCard.svelte';
    import DettagliDocCard from '$lib/Components/Features/Documentale/DettagliDocCard.svelte';
    import { FormazioneService } from '$lib/services/FormazioneService';

    interface DipendenteEsteso extends DipendenteDTO {
        nomeAzienda?: string;
        idAzienda?: string | number;
    }

    interface DpiEsteso extends AssegnazioneDPI {
        nomeDpi?: string;
        id?: number;
    }

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
    let isEditing = $state(false);

    let dipendenteDaEliminare = $state<DipendenteEsteso | null>(null);

    let formDipendente = $state({
        idUtente: null as number | null,
        nome: '',
        cognome: '',
        codiceFiscale: '',
        email: '',
        idAzienda: '' as string | number,
        password: ''
    });

    let showDeleteDocModal = $state(false);
    let docDaEliminare = $state<Documento | null>(null);

    let showDeleteDpiModal = $state(false);
    let dpiDaEliminare = $state<DpiEsteso | null>(null);

    const filteredLavoratori = $derived(
        lavoratori.filter(l =>
            l.nome.toLowerCase().includes(searchQuery.toLowerCase()) ||
            l.cognome.toLowerCase().includes(searchQuery.toLowerCase()) ||
            l.codiceFiscale.toLowerCase().includes(searchQuery.toLowerCase()) ||
            (l.nomeAzienda && l.nomeAzienda.toLowerCase().includes(searchQuery.toLowerCase()))
        )
    );

    const lavoratoriRaggruppati = $derived.by(() => {
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
        formDipendente.nome.trim() !== '' &&
        formDipendente.cognome.trim() !== '' &&
        formDipendente.codiceFiscale.length === 16 &&
        formDipendente.idAzienda !== '' &&
        (isEditing ? true : formDipendente.password.trim() !== '')
    );

    function getStatoScadenza(dataScadenza: string | undefined) {
        if (!dataScadenza) return { colore: 'text-green-600', bgBadge: 'bg-green-50', borderBadge: 'border-green-200', bgTop: 'bg-green-500', label: 'Valido', IconaDef: CheckCircle, peso: 3 };

        const oggi = new Date();
        const scadenza = new Date(dataScadenza);
        const diffTempo = scadenza.getTime() - oggi.getTime();
        const giorniRimanenti = Math.ceil(diffTempo / (1000 * 3600 * 24));

        if (giorniRimanenti < 0) return { colore: 'text-red-600', bgBadge: 'bg-red-50', borderBadge: 'border-red-200', bgTop: 'bg-red-500', label: 'Scaduto', IconaDef: AlertCircle, peso: 1 };
        if (giorniRimanenti <= 30) return { colore: 'text-yellow-600', bgBadge: 'bg-yellow-50', borderBadge: 'border-yellow-200', bgTop: 'bg-yellow-500', label: `In scadenza (${giorniRimanenti}gg)`, IconaDef: Clock, peso: 2 };

        return { colore: 'text-green-600', bgBadge: 'bg-green-50', borderBadge: 'border-green-200', bgTop: 'bg-green-500', label: 'Valido', IconaDef: CheckCircle, peso: 3 };
    }

    const sortedDocumentiCorrenti = $derived(
        [...documentiCorrenti].sort((a, b) => {
            const statoA = getStatoScadenza(a.dataScadenza);
            const statoB = getStatoScadenza(b.dataScadenza);
            if (statoA.peso !== statoB.peso) return statoA.peso - statoB.peso;
            return new Date(a.dataScadenza).getTime() - new Date(b.dataScadenza).getTime();
        })
    );

    const sortedDpiCorrenti = $derived(
        [...dpiCorrenti].sort((a, b) => {
            const statoA = getStatoScadenza(a.dataScadenzaRevisione);
            const statoB = getStatoScadenza(b.dataScadenzaRevisione);
            if (statoA.peso !== statoB.peso) return statoA.peso - statoB.peso;
            const dataA = a.dataScadenzaRevisione ? new Date(a.dataScadenzaRevisione).getTime() : Infinity;
            const dataB = b.dataScadenzaRevisione ? new Date(b.dataScadenzaRevisione).getTime() : Infinity;
            return dataA - dataB;
        })
    );

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
            console.error("Errore durante il caricamento dei dati:", error);
        } finally {
            isLoading = false;
        }
    });

    async function apriDettaglio(lavoratore: DipendenteEsteso) {
        selectedDipendente = lavoratore;
        isLoadingDettaglio = true;
        try {
            const [resIscrizioni, resDpis] = await Promise.all([
                FormazioneService.getIscrizioniUtente(lavoratore.idUtente as number),
                LavoratoreService.getDpiByLavoratore(lavoratore.idUtente as number)
            ]);
            documentiCorrenti = resIscrizioni
                .filter(i => i.idDocumento != null)
                .map(i => ({
                    idDocumento: i.idDocumento!,
                    modulo: i.titoloCorso || 'Corso di Formazione',
                    tipologia: 'ATTESTATO_CORSO',
                    dataScadenza: i.dataOrarioCorso,
                    filePath: '',
                    stato: 'APPROVATO',
                    scaduto: false
                } as Documento));

            dpiCorrenti = resDpis as unknown as DpiEsteso[];
        } catch (error) {
            console.error("Errore durante il caricamento dei dettagli del dipendente:", error);
        } finally {
            isLoadingDettaglio = false;
        }
    }

    function apreGmail(email: string) {
        if (!email) {
            alert("Nessun indirizzo email registrato per questo dipendente.");
            return;
        }
        window.open(`https://mail.google.com/mail/?view=cm&fs=1&to=${email}`, '_blank');
    }

    async function vaiInChat(idUtente: string | number | undefined) {
        if (!idUtente) return;
        return await goto(`${resolveRoute('/dashboard/admin/comunicazioni')}?chatId=${idUtente}`);
    }

    function apriModaleRegistrazione() {
        isEditing = false;
        formDipendente = { idUtente: null, nome: '', cognome: '', codiceFiscale: '', email: '', idAzienda: '', password: '' };
        showAddModal = true;
    }

    function apriModaleModifica() {
        if (!selectedDipendente) return;
        isEditing = true;
        formDipendente = {
            idUtente: selectedDipendente.idUtente,
            nome: selectedDipendente.nome,
            cognome: selectedDipendente.cognome,
            codiceFiscale: selectedDipendente.codiceFiscale,
            email: selectedDipendente.email,
            idAzienda: selectedDipendente.idAzienda || '',
            password: ''
        };
        showAddModal = true;
    }

    async function salvaDipendente() {
        if (!isFormValid) return;
        isSaving = true;
        try {
            const payload = {
                nome: formDipendente.nome,
                cognome: formDipendente.cognome,
                codiceFiscale: formDipendente.codiceFiscale.toUpperCase(),
                email: formDipendente.email,
                passwordHash: isEditing ? undefined : formDipendente.password
            };

            const aziendaSelezionata = aziende.find(a => String(a.idUtente) === String(formDipendente.idAzienda));

            if (isEditing && formDipendente.idUtente) {
                const aggiornato = await LavoratoreService.update(formDipendente.idUtente, payload as any);
                const esteso: DipendenteEsteso = {
                    ...aggiornato,
                    nomeAzienda: aziendaSelezionata?.ragioneSociale || "Azienda non specificata",
                    idAzienda: formDipendente.idAzienda
                };

                lavoratori = lavoratori.map(l => l.idUtente === aggiornato.idUtente ? esteso : l);
                if (selectedDipendente?.idUtente === aggiornato.idUtente) {
                    selectedDipendente = esteso;
                }
            } else {
                const nuovo = await LavoratoreService.create(
                    formDipendente.idAzienda as number,
                    payload as unknown as DipendenteRequest
                );
                const esteso: DipendenteEsteso = {
                    ...nuovo,
                    nomeAzienda: aziendaSelezionata?.ragioneSociale || "Azienda non specificata",
                    idAzienda: formDipendente.idAzienda
                };
                lavoratori = [esteso, ...lavoratori];
            }

            showAddModal = false;
        } catch (error) {
            console.error("Dettaglio dell'errore:", error);
            alert("Si è verificato un errore durante l'operazione sul dipendente.");
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
            alert("Si è verificato un errore durante l'eliminazione del dipendente.");
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
            alert("Si è verificato un errore durante il download del documento.");
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
            alert("Si è verificato un errore durante l'eliminazione dell'attestato.");
        }
    }

    function sollecitaViaEmail(dpi: DpiEsteso) {
        if (!selectedDipendente || !selectedDipendente.idAzienda) return;
        const azienda = aziende.find(a => String(a.idUtente) === String(selectedDipendente?.idAzienda));

        if (!azienda || !azienda.email) {
            alert("L'azienda associata non ha un indirizzo email registrato nel sistema.");
            return;
        }

        const subject = encodeURIComponent(`URGENTE: Rinnovo DPI Scaduto - ${selectedDipendente.nome} ${selectedDipendente.cognome}`);
        const dataScad = new Date(dpi.dataScadenzaRevisione || '').toLocaleDateString();
        const nomeDpiReale = dpi.tipo === 'ALTRO' && dpi.nomeDpi ? dpi.nomeDpi : (dpi.tipo || '').replace(/_/g, ' ');

        const body = encodeURIComponent(
            `Gentile ${azienda.ragioneSociale},\n\n` +
            `Vi segnaliamo che il DPI (${nomeDpiReale}) assegnato a ${selectedDipendente.nome} ${selectedDipendente.cognome} è SCADUTO il ${dataScad}.\n\n` +
            `Vi invitiamo a provvedere al rinnovo immediato.\n\nCordiali saluti,\nTeam NorLan`
        );
        window.open(`https://mail.google.com/mail/?view=cm&fs=1&to=${azienda.email}&su=${subject}&body=${body}`, '_blank');
    }

    async function sollecitaViaChat(dpi: DpiEsteso) {
        if (!selectedDipendente || !selectedDipendente.idAzienda) return;

        const dataScad = new Date(dpi.dataScadenzaRevisione || '').toLocaleDateString();
        const nomeDpiReale = dpi.tipo === 'ALTRO' && dpi.nomeDpi ? dpi.nomeDpi : (dpi.tipo || '').replace(/_/g, ' ');

        const testoMessaggio = encodeURIComponent(
            `Salve, vi segnaliamo che il DPI (${nomeDpiReale}) assegnato al lavoratore ${selectedDipendente.nome} ${selectedDipendente.cognome} risulta scaduto in data ${dataScad}. Vi invitiamo a rinnovare questo dispositivo il prima possibile.`
        );

        await goto(`${resolveRoute('/dashboard/admin/comunicazioni')}?chatId=${selectedDipendente.idAzienda}&msg=${testoMessaggio}`);
    }

    function preparaEliminaDPI(dpi: DpiEsteso) { dpiDaEliminare = dpi; showDeleteDpiModal = true; }

    async function confermaEliminaDPI() {
        if (!dpiDaEliminare) return;
        try {
            const idReale = dpiDaEliminare.idAssegnazione || dpiDaEliminare.id;
            if (idReale) {
                await LavoratoreService.deleteDpi(idReale);
            }

            dpiCorrenti = dpiCorrenti.filter(d => d !== dpiDaEliminare);
            showDeleteDpiModal = false;
            dpiDaEliminare = null;
        } catch (error) {
            console.error("Errore durante l'eliminazione del DPI dal database:", error);
            alert("Si è verificato un errore durante l'eliminazione del dispositivo di protezione individuale.");
        }
    }

    function formattaScadenza(data?: string) {
        if (!data || data === '9999-12-31') return 'Senza scadenza';
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
                    onclick={apriModaleRegistrazione}
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
            {#each Object.entries(lavoratoriRaggruppati) as [nomeAzienda, dipendentiAzienda] (nomeAzienda)}
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
                            <div in:scale>
                                <DipendenteCard
                                        idUtente={l.idUtente}
                                        nome={l.nome}
                                        cognome={l.cognome}
                                        codiceFiscale={l.codiceFiscale}
                                        azienda={l.nomeAzienda}
                                        canContact={true}
                                        canEdit={true}
                                        canDelete={true}
                                        canViewDetails={true}
                                        onEdit={() => { selectedDipendente = l; apriModaleModifica(); }}
                                        onDelete={() => preparaEliminazione(l)}
                                        onContact={() => vaiInChat(l.idUtente)}
                                        onViewDetails={() => apriDettaglio(l)}
                                />
                            </div>
                        {/each}
                    </div>
                </div>
            {/each}
        {/if}

    {:else}
        <div in:fade>
            <button onclick={() => (selectedDipendente = null)} class="flex items-center gap-2 text-[#1B4B6B] font-extrabold uppercase text-[10px] mb-8 hover:gap-3 transition-all"><ChevronLeft size={16} /> Torna all'elenco dipendenti</button>

            <DettagliCard
                    nome={selectedDipendente.nome}
                    cognome={selectedDipendente.cognome}
                    sottotitolo={selectedDipendente.nomeAzienda}
                    onEdit={apriModaleModifica}
                    onMail={() => apreGmail(selectedDipendente?.email || '')}
                    onContact={() => vaiInChat(selectedDipendente?.idUtente)}
                    onDelete={() => preparaEliminazione(selectedDipendente)}
                    items={[
                    { label: 'Codice Fiscale', value: selectedDipendente.codiceFiscale, icon: IdCard, isMono: true },
                    { label: 'Email di Contatto', value: selectedDipendente.email || 'Nessuna email fornita', icon: Mail },
                    { label: 'Posizione', value: 'Dipendente Operativo', icon: Building2 }
                ]}
            />

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

                        {#if sortedDocumentiCorrenti.length > 0}
                            <div class="space-y-4">
                                {#each sortedDocumentiCorrenti as doc (doc.idDocumento)}
                                    <DettagliDocCard
                                            tipo="ATTESTATO"
                                            titolo={doc.tipologia.replace(/_/g, ' ')}
                                            sottotitolo={doc.modulo}
                                            dataScadenza={doc.dataScadenza}
                                            onDownload={() => scaricaDoc(doc)}
                                            onDelete={() => preparaEliminaDoc(doc)}
                                    />
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
                                <div class="p-2.5 bg-[#1B4B6B]/10 text-[#1B4B6B] rounded-xl shadow-inner"><ShieldCheck size={20} /></div>
                                <h2 class="text-xl font-black text-[#1B4B6B] uppercase tracking-tighter">DPI Consegnati ({dpiCorrenti.length})</h2>
                            </div>
                        </div>

                        {#if sortedDpiCorrenti.length > 0}
                            <div class="space-y-4">
                                {#each sortedDpiCorrenti as dpi (dpi.idAssegnazione || Math.random())}
                                    {@const nomeDpiReale = dpi.tipo === 'ALTRO' && dpi.nomeDpi ? dpi.nomeDpi : (dpi.tipo || '').replace(/_/g, ' ')}
                                    {@const stato = getStatoScadenza(dpi.dataScadenzaRevisione)}
                                    <div class="flex flex-col gap-2">
                                        <DettagliDocCard
                                                tipo="DPI"
                                                titolo={nomeDpiReale}
                                                dataScadenza={dpi.dataScadenzaRevisione}
                                                dataSecondaria={dpi.dataConsegna}
                                                labelDataSecondaria="Consegnato"
                                                onDelete={() => preparaEliminaDPI(dpi)}
                                        />

                                        {#if stato.peso === 1}
                                            <div class="bg-red-50/50 border border-red-100 rounded-xl p-3 shadow-sm">
                                                <p class="text-[8px] font-black uppercase text-red-500 mb-2 text-center tracking-tighter">
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
            <div class="bg-white rounded-3xl shadow-2xl w-full max-w-2xl overflow-hidden" in:scale>
                <div class="bg-[#1B4B6B] p-6 text-white flex justify-between items-center">
                    <h2 class="text-xl font-black uppercase tracking-tighter flex items-center gap-2">
                        {#if isEditing}<Edit3 size={20}/> Modifica Dati Lavoratore{:else}<UserPlus size={20}/> Registra Lavoratore{/if}
                    </h2>
                    <button onclick={() => (showAddModal = false)} class="hover:rotate-90 transition-transform duration-300"><X size={24}/></button>
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
                        <label class="block text-[10px] font-bold text-[#1B4B6B] uppercase tracking-tight">Password di Accesso *</label>
                        {#if !isEditing}
                            <input bind:value={formDipendente.password} type="password" placeholder="••••••••" class="w-full p-3 bg-gray-50 border-none rounded-xl text-sm focus:ring-2 focus:ring-[#1B4B6B] outline-none" />
                            <div class="mt-2 flex items-start gap-2">
                                <div class="mt-0.5 text-orange-500"><AlertTriangle size={12}/></div>
                                <p class="text-[8px] text-gray-400 font-bold uppercase leading-tight">
                                    Nota: Il lavoratore dovrà obbligatoriamente cambiare questa password al suo primo accesso.
                                </p>
                            </div>
                        {:else}
                            <div class="p-4 bg-blue-50 border border-blue-100 rounded-xl">
                                <p class="text-[10px] font-bold text-blue-800 uppercase leading-tight">Nota di Sicurezza</p>
                                <p class="text-[9px] text-blue-600 uppercase mt-1 leading-relaxed">Per motivi di privacy, la password può essere modificata solo dal dipendente tramite il proprio pannello o reset password.</p>
                            </div>
                        {/if}
                    </div>
                </div>

                <div class="p-8 bg-gray-50 flex justify-end gap-4 border-t border-gray-100">
                    <button onclick={() => (showAddModal = false)} class="px-6 py-3 text-[10px] font-black uppercase text-gray-400 hover:text-gray-600 transition-colors">Annulla</button>
                    <button
                            onclick={salvaDipendente}
                            disabled={!isFormValid || isSaving}
                            class="bg-[#1B4B6B] text-white px-8 py-3 rounded-xl text-[10px] font-black uppercase shadow-lg disabled:opacity-50 flex items-center gap-2 hover:bg-[#153a54] transition-colors"
                    >
                        {#if isSaving}<Loader2 size={14} class="animate-spin"/>{:else if isEditing}<Edit3 size={14}/>{:else}<UserPlus size={14}/>{/if}
                        {isEditing ? 'Aggiorna Dati' : 'Registra Dipendente'}
                    </button>
                </div>
            </div>
        </div>
    {/if}

    {#if showDeleteModal}
        <div class="fixed inset-0 bg-red-900/20 backdrop-blur-sm flex items-center justify-center z-[110] p-4" transition:fade>
            <div class="bg-white rounded-3xl shadow-2xl w-full max-w-md overflow-hidden" in:scale>
                <div class="p-8 text-center">
                    <div class="w-20 h-20 bg-red-50 text-red-600 rounded-full flex items-center justify-center mx-auto mb-6"><AlertTriangle size={40}/></div>
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
                    <p class="text-sm text-gray-400 mb-8">Stai annullando l'assegnazione di: <br><span class="font-bold text-[#1B4B6B]">{dpiDaEliminare?.tipo?.replace(/_/g, ' ')}</span></p>
                    <div class="flex flex-col gap-3">
                        <button onclick={confermaEliminaDPI} class="w-full bg-red-600 text-white py-4 rounded-2xl font-black uppercase text-[10px] shadow-lg shadow-red-200 transition-all hover:bg-red-700">Conferma Rimozione</button>
                        <button onclick={() => (showDeleteDpiModal = false)} class="w-full py-4 text-[10px] font-black uppercase text-gray-400 hover:text-gray-600">Annulla</button>
                    </div>
                </div>
            </div>
        </div>
    {/if}

</div>