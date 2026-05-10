<script lang="ts">
    import { onMount } from 'svelte';
    import { page } from '$app/stores';
    import { goto } from '$app/navigation';
    import { resolveRoute } from '$app/paths';
    import { fade, scale, slide } from 'svelte/transition';
    import {
        Users, UserPlus, Trash2, Search, Mail, Building2,
        IdCard, Loader2, AlertTriangle, ChevronLeft,
        FileText, ShieldCheck, Plus, Edit3, Save, Lock
    } from 'lucide-svelte';

    import { LavoratoreService, type DipendenteDTO } from '$lib/services/LavoratoreService';
    import { AuthService } from '$lib/services/AuthService';
    import { DocumentoService } from '$lib/services/DocumentoService';
    import { FormazioneService } from '$lib/services/FormazioneService';
    import { Documento } from '$lib/models/Documento';
    import type { AssegnazioneDPI } from '$lib/models/AssegnazioneDPI';
    import StatCard from '$lib/Components/UI/StatCard.svelte';
    import { AnagraficaService } from "$lib/services/AnagraficaService";
    import AlertCard from '$lib/Components/UI/AlertCard.svelte';
    import DipendenteCard from '$lib/Components/Features/Anagrafica/DipendenteCard.svelte';
    import DettagliCard from '$lib/Components/Features/Anagrafica/DettagliCard.svelte';
    import DettagliDocCard from '$lib/Components/Features/Documentale/DettagliDocCard.svelte';
    import ModalCard from '$lib/Components/UI/ModalCard.svelte';
    import { caricaDettagliEntita } from "$lib/utils/dettaglioUtils";
    import { creaUtenteUniversale, aggiornaUtenteUniversale, eliminaUtenteUniversale } from '$lib/utils/anagraficaUtils';
    import { scaricaDocumentoUniversale } from '$lib/utils/documentoUtils';
    import { ordinaPerScadenza, formattaDataScadenza } from '$lib/utils/scadenzeUtils';

    interface ServerError {
        response?: {
            status?: number;
            data?: string;
        };
    }

    interface DipendenteEsteso extends DipendenteDTO {
        nomeAzienda?: string;
    }

    interface DpiEsteso extends AssegnazioneDPI {
        id?: number;
        nomeDpi?: string;
    }

    interface FormDPI {
        idAssegnazione: number | null;
        tipo: string;
        nomeDpi: string;
        dataConsegna: string;
        dataScadenzaRevisione: string;
    }

    let lavoratori = $state<DipendenteEsteso[]>([]);
    let isLoading = $state(true);
    let searchQuery = $state('');
    let idAziendaCorrente = $state<number | string>('');
    let nomeAziendaCorrente = $state<string>('La tua Azienda');
    let selectedDipendente = $state<DipendenteEsteso | null>(null);
    let documentiCorrenti = $state<Documento[]>([]);
    let dpiCorrenti = $state<DpiEsteso[]>([]);
    let isLoadingDettaglio = $state(false);
    let showAddModal = $state(false);
    let isEditing = $state(false);
    let showDeleteModal = $state(false);
    let isSaving = $state(false);
    let dipendenteDaEliminare = $state<DipendenteEsteso | null>(null);
    let formDipendente = $state({
        idUtente: null as number | null,
        nome: '',
        cognome: '',
        codiceFiscale: '',
        email: '',
        passwordHash: ''
    });
    let showDpiModal = $state(false);
    let isSavingDpi = $state(false);
    let formDpi = $state<FormDPI>({ idAssegnazione: null, tipo: '', nomeDpi: '', dataConsegna: '', dataScadenzaRevisione: '' });
    let showDeleteDpiModal = $state(false);
    let dpiDaEliminare = $state<DpiEsteso | null>(null);

    const filteredLavoratori = $derived(
        lavoratori.filter(l =>
            l.nome.toLowerCase().includes(searchQuery.toLowerCase()) ||
            l.cognome.toLowerCase().includes(searchQuery.toLowerCase()) ||
            l.codiceFiscale.toLowerCase().includes(searchQuery.toLowerCase())
        )
    );

    const isFormValid = $derived(
        formDipendente.nome.trim() !== '' &&
        formDipendente.cognome.trim() !== '' &&
        formDipendente.codiceFiscale.length === 16 &&
        (isEditing ? true : formDipendente.passwordHash.trim() !== '')
    );

    const sortedDocumentiCorrenti = $derived(
        ordinaPerScadenza(documentiCorrenti, 'dataScadenza')
    );

    const sortedDpiCorrenti = $derived(
        ordinaPerScadenza(dpiCorrenti, 'dataScadenzaRevisione')
    );

    onMount(async () => {
        try {
            const session = AuthService.getSession();
            if (!session) return;
            idAziendaCorrente = session.idUtente;

            const resLavoratori = await LavoratoreService.getByAzienda(idAziendaCorrente);
            const profile = await AnagraficaService.getAziendaById(session.idUtente);
            nomeAziendaCorrente = (profile as any).ragioneSociale || "La tua Azienda";

            lavoratori = resLavoratori.map((l: DipendenteDTO) => ({ ...l, nomeAzienda: nomeAziendaCorrente }));
            const idDaUrl = $page.url.searchParams.get('id');
            if (idDaUrl) {
                const dipendenteTrovato = lavoratori.find(l => String(l.idUtente) === idDaUrl);
                if (dipendenteTrovato) await apriDettaglio(dipendenteTrovato);
            }
        } catch (error) {
            console.error(error);
        } finally {
            isLoading = false;
        }
    });

    async function apriDettaglio(lavoratore: DipendenteEsteso) {
        selectedDipendente = lavoratore;
        isLoadingDettaglio = true;
        const res = await caricaDettagliEntita(Number(lavoratore.idUtente), 'DIPENDENTE') as any;

        if (res.error) {
            alert("Si è verificato un errore nel caricamento dei dati del dipendente.");
        }
        documentiCorrenti = res.documenti;
        dpiCorrenti = res.dpi;
        isLoadingDettaglio = false;
    }

    function apreGmail(email: string) {
        if (!email) return;
        window.open(`https://mail.google.com/mail/?view=cm&fs=1&to=${email}`, '_blank');
    }

    async function vaiInChat(idUtente: string | number | undefined) {
        if (!idUtente) return;
        return await goto(`${resolveRoute('/dashboard/azienda/comunicazioni')}?chatId=${idUtente}`);
    }

    function apriModaleRegistrazione() {
        isEditing = false;
        formDipendente = { idUtente: null, nome: '', cognome: '', codiceFiscale: '', email: '', passwordHash: '' };
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
            passwordHash: ''
        };
        showAddModal = true;
    }

    async function salvaDipendente() {
        if (!isFormValid || !idAziendaCorrente) return;
        isSaving = true;

        const payload = {
            nome: formDipendente.nome,
            cognome: formDipendente.cognome,
            codiceFiscale: formDipendente.codiceFiscale.toUpperCase(),
            email: formDipendente.email,
            passwordHash: isEditing ? undefined : formDipendente.passwordHash
        };

        let res;
        if (isEditing && formDipendente.idUtente) {
            res = await aggiornaUtenteUniversale<DipendenteDTO>('DIPENDENTE', formDipendente.idUtente, payload);
        } else {
            res = await creaUtenteUniversale<DipendenteDTO>('DIPENDENTE', {
                ...payload,
                idAzienda: idAziendaCorrente
            });
        }

        if (res.error || !res.data) {
            alert(res.msg);
        } else {
            const esteso: DipendenteEsteso = {
                ...res.data,
                nomeAzienda: nomeAziendaCorrente
            };

            if (isEditing) {
                lavoratori = lavoratori.map(l => l.idUtente === res.data!.idUtente ? esteso : l);
                if (selectedDipendente?.idUtente === res.data!.idUtente) {
                    selectedDipendente = esteso;
                }
            } else {
                lavoratori = [esteso, ...lavoratori];
            }
            showAddModal = false;
        }
        isSaving = false;
    }

    function preparaEliminazione(l: DipendenteEsteso | null) {
        if (!l) return;
        dipendenteDaEliminare = l;
        showDeleteModal = true;
    }

    async function confermaEliminazione() {
        if (!dipendenteDaEliminare) return;

        const res = await eliminaUtenteUniversale('DIPENDENTE', dipendenteDaEliminare.idUtente);

        if (res.error) {
            alert(res.msg);
        } else {
            lavoratori = lavoratori.filter(l => String(l.idUtente) !== String(dipendenteDaEliminare?.idUtente));
            showDeleteModal = false;
            if (selectedDipendente && String(selectedDipendente.idUtente) === String(dipendenteDaEliminare.idUtente)) {
                selectedDipendente = null;
            }
            dipendenteDaEliminare = null;
        }
    }

    async function scaricaDoc(doc: any) {
        await scaricaDocumentoUniversale(doc.idDocumento || doc.id, doc.filePath);
    }

    function openNewDpiModal() {
        formDpi = { idAssegnazione: null, tipo: '', nomeDpi: '', dataConsegna: '', dataScadenzaRevisione: '' };
        showDpiModal = true;
    }

    function openUpdateDpiModal(idDpi: number | string) {
        const dpi = dpiCorrenti.find(d => d.idAssegnazione === idDpi || d.id === idDpi);
        if (!dpi) return;
        formDpi = {
            idAssegnazione: dpi.idAssegnazione || dpi.id || null,
            tipo: dpi.tipo || '',
            nomeDpi: dpi.nomeDpi || '',
            dataConsegna: '',
            dataScadenzaRevisione: ''
        };
        showDpiModal = true;
    }

    async function salvaDPI() {
        if (!selectedDipendente) return;
        isSavingDpi = true;
        try {
            const payload = {
                idAssegnazione: formDpi.idAssegnazione,
                tipo: formDpi.tipo,
                nomeDpi: formDpi.tipo === 'ALTRO' ? formDpi.nomeDpi : '',
                dataConsegna: formDpi.dataConsegna || undefined,
                dataScadenzaRevisione: formDpi.dataScadenzaRevisione
            };

            const savedDpi = await LavoratoreService.assegnaDpi(selectedDipendente.idUtente, payload as any);
            const dpiSalvato = savedDpi as unknown as DpiEsteso;

            if (formDpi.idAssegnazione) {
                dpiCorrenti = dpiCorrenti.map(d => (d.idAssegnazione === formDpi.idAssegnazione || d.id === formDpi.idAssegnazione) ? dpiSalvato : d);
            } else {
                dpiCorrenti = [...dpiCorrenti, dpiSalvato];
            }
            showDpiModal = false;
        } catch (error) {
            console.error(error);
        } finally {
            isSavingDpi = false;
        }
    }

    function preparaEliminaDPI(idDpi: number | string) {
        const dpi = dpiCorrenti.find(d => d.idAssegnazione === idDpi || d.id === idDpi);
        if(!dpi) return;
        dpiDaEliminare = dpi;
        showDeleteDpiModal = true;
    }

    async function confermaEliminaDPI() {
        if (!dpiDaEliminare) return;
        try {
            const idReale = dpiDaEliminare.idAssegnazione || dpiDaEliminare.id;
            if (idReale) await LavoratoreService.deleteDpi(idReale);
            dpiCorrenti = dpiCorrenti.filter(d => d !== dpiDaEliminare);
            showDeleteDpiModal = false;
            dpiDaEliminare = null;
        } catch (error) {
            console.error(error);
        }
    }
</script>

<div in:fade class="max-w-7xl mx-auto p-6">
    {#if !selectedDipendente}
        <div class="mb-10 flex justify-between items-start">
            <div>
                <h1 class="text-4xl font-extrabold text-[#1B4B6B] uppercase tracking-tighter">Anagrafica Dipendenti</h1>
                <p class="text-gray-500 font-bold uppercase text-xs tracking-tighter">Gestione dei lavoratori della società</p>
            </div>
            <button onclick={apriModaleRegistrazione} class="bg-white text-[#1B4B6B] border-2 border-[#1B4B6B] px-8 py-3.5 rounded-xl font-extrabold uppercase text-xs shadow-lg hover:bg-[#1B4B6B] hover:text-white transition-all flex items-center gap-3">
                <UserPlus size={18} /> Aggiungi Dipendente
            </button>
        </div>

        <div class="mb-8 flex gap-4">
            <div class="relative w-72 group">
                <Search class="absolute left-3 top-1/2 -translate-y-1/2 text-gray-400 group-focus-within:text-[#1B4B6B] transition-colors" size={16} />
                <input bind:value={searchQuery} type="text" placeholder="Cerca lavoratore..." class="w-full pl-10 pr-4 py-2.5 bg-white border border-gray-200 rounded-xl text-xs focus:ring-2 focus:ring-[#1B4B6B] outline-none transition-all font-bold uppercase shadow-sm" />
            </div>
        </div>

        {#if isLoading}
            <div class="py-20 text-center"><Loader2 size={40} class="animate-spin mx-auto text-[#1B4B6B]" /></div>
        {:else}
            <div class="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6">
                {#each filteredLavoratori as l (l.idUtente)}
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
                {:else}
                    <div class="col-span-full py-20 text-center bg-gray-50 rounded-3xl border-2 border-dashed border-gray-200"><Users size={48} class="mx-auto text-gray-300 mb-4" /><p class="text-gray-400 font-bold uppercase text-xs tracking-widest">Nessun dipendente censito nel sistema</p></div>
                {/each}
            </div>
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
                    { label: 'Email di Contatto', value: selectedDipendente.email || 'Indirizzo non disponibile', icon: Mail },
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
                                {#each sortedDocumentiCorrenti as doc (doc.idDocumento || doc.id || Math.random())}
                                    <DettagliDocCard
                                            tipo="ATTESTATO"
                                            titolo={(doc.tipologia || 'Documento').replace(/_/g, ' ')}
                                            sottotitolo={doc.modulo || ''}
                                            dataScadenza={formattaDataScadenza(doc.dataScadenza)}
                                            onDownload={() => scaricaDoc(doc)}
                                    />
                                {/each}
                            </div>
                        {:else}
                            <div class="bg-white rounded-3xl p-8 text-center border-2 border-dashed border-gray-200 text-gray-300 uppercase font-bold text-[10px] italic">Nessun attestato formativo rilevato</div>
                        {/if}
                    </div>

                    <div class="space-y-6">
                        <div class="flex items-center justify-between">
                            <div class="flex items-center gap-3">
                                <div class="p-2.5 bg-[#1B4B6B]/10 text-[#1B4B6B] rounded-xl shadow-inner"><ShieldCheck size={20} /></div>
                                <h2 class="text-xl font-black text-[#1B4B6B] uppercase tracking-tighter">DPI Consegnati ({dpiCorrenti.length})</h2>
                            </div>
                            <button onclick={openNewDpiModal} class="bg-[#1B4B6B] text-white px-5 py-2.5 rounded-xl flex items-center gap-2 hover:bg-[#1B4B6B]/90 transition-all shadow-md">
                                <Plus size={16} />
                                <span class="text-[10px] font-black uppercase tracking-widest">Assegna</span>
                            </button>
                        </div>

                        {#if sortedDpiCorrenti.length > 0}
                            <div class="space-y-4">
                                {#each sortedDpiCorrenti as dpi (dpi.idAssegnazione || dpi.id || Math.random())}
                                    {@const nomeDpiReale = dpi.tipo === 'ALTRO' && dpi.nomeDpi ? dpi.nomeDpi : (dpi.tipo || '').replace(/_/g, ' ')}

                                    <DettagliDocCard
                                            tipo="DPI"
                                            titolo={nomeDpiReale}
                                            dataScadenza={formattaDataScadenza(dpi.dataScadenzaRevisione)}
                                            dataSecondaria={formattaDataScadenza(dpi.dataConsegna)}
                                            labelDataSecondaria="Consegnato"
                                            onUpdate={() => openUpdateDpiModal(dpi.idAssegnazione || dpi.id || 0)}
                                            onDelete={() => preparaEliminaDPI(dpi.idAssegnazione || dpi.id || 0)}
                                    />
                                {/each}
                            </div>
                        {:else}
                            <div class="bg-white rounded-3xl p-8 text-center border-2 border-dashed border-gray-200 text-gray-300 uppercase font-bold text-[10px] italic"><ShieldCheck size={32} class="mx-auto mb-3 opacity-20"/>Nessun dispositivo assegnato al dipendente</div>
                        {/if}
                    </div>
                </div>
            {/if}
        </div>
    {/if}
</div>

<ModalCard bind:isOpen={showAddModal} maxWidth="max-w-2xl">
    {#snippet title()}
        {#if isEditing}
            <Edit3 size={20}/> <span class="font-black uppercase tracking-tighter">Modifica Dati Lavoratore</span>
        {:else}
            <UserPlus size={20}/> <span class="font-black uppercase tracking-tighter">Registra Nuovo Lavoratore</span>
        {/if}
    {/snippet}

    <div class="grid grid-cols-1 md:grid-cols-2 gap-6">
        <div class="space-y-4">
            <div>
                <label class="block text-[10px] font-bold text-[#1B4B6B] uppercase mb-1">Azienda di Appartenenza</label>
                <div class="w-full p-3 bg-gray-200 border-none rounded-xl text-sm font-bold text-gray-500 flex items-center gap-2 cursor-not-allowed">
                    <Building2 size={16} /> {nomeAziendaCorrente}
                </div>
            </div>
            <div>
                <label class="block text-[10px] font-bold text-[#1B4B6B] uppercase mb-1">Nome *</label>
                <input bind:value={formDipendente.nome} placeholder="Es: Mario" class="w-full p-3 bg-gray-50 border-none rounded-xl text-sm focus:ring-2 focus:ring-[#1B4B6B] outline-none" />
            </div>
            <div>
                <label class="block text-[10px] font-bold text-[#1B4B6B] uppercase mb-1">Cognome *</label>
                <input bind:value={formDipendente.cognome} placeholder="Es: Rossi" class="w-full p-3 bg-gray-50 border-none rounded-xl text-sm focus:ring-2 focus:ring-[#1B4B6B] outline-none" />
            </div>
        </div>
        <div class="space-y-4">
            <div>
                <label class="block text-[10px] font-bold text-[#1B4B6B] uppercase mb-1">Email Accesso</label>
                <input bind:value={formDipendente.email} type="email" placeholder="m.rossi@email.it" class="w-full p-3 bg-gray-50 border-none rounded-xl text-sm focus:ring-2 focus:ring-[#1B4B6B] outline-none" />
            </div>
            <div>
                <label class="block text-[10px] font-bold text-[#1B4B6B] uppercase mb-1">Codice Fiscale *</label>
                <input bind:value={formDipendente.codiceFiscale} maxlength="16" placeholder="RSSMRA80A01H501W" class="w-full p-3 bg-gray-50 border-none rounded-xl text-sm font-mono focus:ring-2 focus:ring-[#1B4B6B] outline-none uppercase" />
            </div>

            {#if !isEditing}
                <div>
                    <label class="block text-[10px] font-bold text-[#1B4B6B] uppercase mb-1">Password di Accesso *</label>
                    <input bind:value={formDipendente.passwordHash} type="text" placeholder="Password temporanea" class="w-full p-3 bg-gray-50 border-none rounded-xl text-sm focus:ring-2 focus:ring-[#1B4B6B] outline-none" />
                </div>
            {:else}
                <AlertCard
                        titolo="Nota di Sicurezza"
                        sottotitolo="Per motivi di privacy, la password può essere modificata solo dal dipendente tramite il proprio pannello o reset password."
                        variante="info"
                        icona={Lock}
                />
            {/if}
        </div>
    </div>

    {#snippet footer()}
        <button onclick={() => (showAddModal = false)} class="flex-1 py-3 text-[10px] font-black uppercase text-gray-400 hover:text-gray-600 transition-colors">Annulla</button>
        <button onclick={salvaDipendente} disabled={!isFormValid || isSaving} class="flex-1 bg-[#1B4B6B] text-white py-3 rounded-xl text-[10px] font-black uppercase shadow-lg disabled:opacity-50 flex items-center justify-center gap-2 hover:bg-[#1B4B6B]/90 transition-all">
            {#if isSaving}<Loader2 size={14} class="animate-spin"/>{:else if isEditing}<Save size={14}/>{:else}<UserPlus size={14}/>{/if}
            {isEditing ? 'Aggiorna Dati' : 'Registra Dipendente'}
        </button>
    {/snippet}
</ModalCard>

<ModalCard bind:isOpen={showDeleteModal} maxWidth="max-w-md" headerClass="bg-red-600">
    {#snippet title()}
        <AlertTriangle size={20}/> <span class="font-black uppercase tracking-tighter">Rimuovere dipendente?</span>
    {/snippet}

    <div class="text-center py-4">
        <div class="w-20 h-20 bg-red-50 text-red-600 rounded-full flex items-center justify-center mx-auto mb-6">
            <AlertTriangle size={40}/>
        </div>
        <p class="text-sm text-gray-400">
            Il lavoratore <span class="font-bold text-[#1B4B6B]">{dipendenteDaEliminare?.nome} {dipendenteDaEliminare?.cognome}</span> verrà rimosso definitivamente dal sistema.
        </p>
    </div>

    {#snippet footer()}
        <div class="flex flex-col w-full gap-3">
            <button onclick={confermaEliminazione} class="w-full bg-red-600 text-white py-4 rounded-2xl font-black uppercase text-[10px] shadow-lg shadow-red-200 transition-all hover:bg-red-700">
                Sì, elimina definitivamente
            </button>
            <button onclick={() => { showDeleteModal = false; dipendenteDaEliminare = null; }} class="w-full py-2 text-[10px] font-black uppercase text-gray-400 hover:text-gray-600">
                No, annulla l'operazione
            </button>
        </div>
    {/snippet}
</ModalCard>

<ModalCard bind:isOpen={showDpiModal} maxWidth="max-w-lg">
    {#snippet title()}
        <ShieldCheck size={20}/> <span class="font-black uppercase tracking-tighter">{formDpi.idAssegnazione ? 'Aggiorna DPI Lavoratore' : 'Registra Consegna DPI'}</span>
    {/snippet}

    <div class="space-y-6">
        <div>
            <label class="block text-[10px] font-bold text-[#1B4B6B] uppercase mb-1">Tipologia DPI *</label>
            <select bind:value={formDpi.tipo} disabled={!!formDpi.idAssegnazione} class="w-full p-3 bg-gray-50 border-none rounded-xl text-sm font-bold uppercase focus:ring-2 focus:ring-[#1B4B6B] outline-none disabled:opacity-50">
                <option value="">Seleziona DPI...</option>
                <option value="ELMETTO">Elmetto</option>
                <option value="GUANTI">Guanti</option>
                <option value="SCARPE_ANTINFORTUNISTICHE">Scarpe Antinfortunistiche</option>
                <option value="OCCHIALI">Occhiali</option>
                <option value="ALTRO">Altro</option>
            </select>
        </div>
        {#if formDpi.tipo === 'ALTRO'}
            <div class="space-y-1" transition:slide>
                <label class="block text-[10px] font-bold text-[#1B4B6B] uppercase mb-1">Nome DPI Personalizzato *</label>
                <input bind:value={formDpi.nomeDpi} disabled={!!formDpi.idAssegnazione} type="text" placeholder="Specifica il nome del DPI..." class="w-full p-3 bg-gray-50 border-none rounded-xl text-sm focus:ring-2 focus:ring-[#1B4B6B] outline-none disabled:opacity-50" />
            </div>
        {/if}
        <div class="grid grid-cols-2 gap-4 mt-4">
            <div>
                <label class="block text-[10px] font-bold text-[#1B4B6B] uppercase mb-1">Nuova Data Consegna *</label>
                <input type="date" max="9999-12-31" bind:value={formDpi.dataConsegna} class="w-full p-3 bg-gray-50 border-none rounded-xl text-sm font-bold focus:ring-2 focus:ring-[#1B4B6B] outline-none" />
            </div>
            <div>
                <label class="block text-[10px] font-bold text-[#1B4B6B] uppercase mb-1">Scadenza Revisione *</label>
                <input type="date" max="9999-12-31" bind:value={formDpi.dataScadenzaRevisione} class="w-full p-3 bg-gray-50 border-none rounded-xl text-sm font-bold focus:ring-2 focus:ring-[#1B4B6B] outline-none" />
            </div>
        </div>
    </div>

    {#snippet footer()}
        <button onclick={() => (showDpiModal = false)} class="flex-1 py-3 text-[10px] font-black uppercase text-gray-400 hover:text-gray-600 transition-colors">Annulla</button>
        <button onclick={salvaDPI} disabled={isSavingDpi || !formDpi.tipo || (formDpi.tipo === 'ALTRO' && !formDpi.nomeDpi.trim()) || !formDpi.dataConsegna || !formDpi.dataScadenzaRevisione} class="flex-1 bg-[#1B4B6B] text-white py-3 rounded-xl text-[10px] font-black uppercase shadow-lg disabled:opacity-50 flex items-center justify-center gap-2 hover:bg-[#1B4B6B]/90 transition-colors">
            {#if isSavingDpi}<Loader2 size={14} class="animate-spin" />{/if} {isSavingDpi ? 'Salvataggio...' : 'Conferma'}
        </button>
    {/snippet}
</ModalCard>

<ModalCard bind:isOpen={showDeleteDpiModal} maxWidth="max-w-md" headerClass="bg-red-600">
    {#snippet title()}
        <Trash2 size={20}/> <span class="font-black uppercase tracking-tighter">Rimuovere DPI?</span>
    {/snippet}

    <div class="text-center py-4">
        <div class="w-20 h-20 bg-red-50 text-red-600 rounded-full flex items-center justify-center mx-auto mb-6">
            <Trash2 size={40}/>
        </div>
        <p class="text-sm text-gray-400">
            Stai annullando l'assegnazione di: <br><span class="font-bold text-[#1B4B6B]">{dpiDaEliminare?.tipo.replace(/_/g, ' ')}</span>
        </p>
    </div>

    {#snippet footer()}
        <div class="flex flex-col w-full gap-3">
            <button onclick={confermaEliminaDPI} class="w-full bg-red-600 text-white py-4 rounded-2xl font-black uppercase text-[10px] shadow-lg shadow-red-200 transition-all hover:bg-red-700">
                Conferma Rimozione
            </button>
            <button onclick={() => { showDeleteDpiModal = false; dpiDaEliminare = null; }} class="w-full py-2 text-[10px] font-black uppercase text-gray-400 hover:text-gray-600">
                Annulla
            </button>
        </div>
    {/snippet}
</ModalCard>