<script lang="ts">
    import { onMount } from 'svelte';
    import { fade, scale, slide } from 'svelte/transition';
    import { goto } from '$app/navigation';
    import {
        GraduationCap, Plus, Trash2, Search, Mail, MessageSquare,
        ChevronRight, ChevronLeft, Loader2, X, AlertTriangle, BookOpen,
        Calendar, Edit3
    } from 'lucide-svelte';

    import type { DocenteData } from '$lib/models/Docente';
    import type { CorsoFormazione } from '$lib/models/CorsoFormazione';
    import { AnagraficaService, type AuthRequestDTO } from '$lib/services/AnagraficaService';
    import { FormazioneService } from '$lib/services/FormazioneService';
    import { resolveRoute } from "$app/paths";
    import DettagliCard from '$lib/Components/Features/Anagrafica/DettagliCard.svelte';
    import DocenteCard from '$lib/Components/Features/Anagrafica/DocenteDashboardCard.svelte';

    let docenti = $state<DocenteData[]>([]);
    let corsiDocente = $state<CorsoFormazione[]>([]);
    let isLoading = $state(true);
    let isLoadingDettaglio = $state(false);
    let searchQuery = $state('');
    let selectedDocente = $state<DocenteData | null>(null);
    let showAddModal = $state(false);
    let isSaving = $state(false);
    let isEditing = $state(false);
    let showDeleteModal = $state(false);
    let docenteDaEliminare = $state<DocenteData | null>(null);
    let formDocente = $state({
        idUtente: null as number | null,
        nome: '',
        cognome: '',
        specializzazioneTecnica: '',
        email: '',
        password: ''
    });

    const filteredDocenti = $derived(
        docenti.filter(d =>
            d.nome.toLowerCase().includes(searchQuery.toLowerCase()) ||
            d.cognome.toLowerCase().includes(searchQuery.toLowerCase()) ||
            (d.specializzazioneTecnica && d.specializzazioneTecnica.toLowerCase().includes(searchQuery.toLowerCase()))
        )
    );

    const isFormValid = $derived(
        formDocente.nome.trim() !== '' &&
        formDocente.cognome.trim() !== '' &&
        formDocente.specializzazioneTecnica.trim() !== '' &&
        formDocente.email.trim() !== '' &&
        (isEditing ? true : formDocente.password.trim() !== '')
    );

    onMount(async () => {
        try {
            const res = await AnagraficaService.getAllDocenti();
            docenti = res as DocenteData[];
        } catch (error) {
            console.error("Errore durante il recupero dell'elenco docenti:", error);
        } finally {
            isLoading = false;
        }
    });

    async function apriDettaglio(docente: DocenteData) {
        selectedDocente = docente;
        isLoadingDettaglio = true;
        try {
            const tuttiCorsi = await FormazioneService.getAllCorsi();
            corsiDocente = tuttiCorsi.filter(c => c.idDocente === docente.idUtente);
        } catch (error) {
            console.error("Errore durante il caricamento dei corsi associati al docente:", error);
            corsiDocente = [];
        } finally {
            isLoadingDettaglio = false;
        }
    }

    function apreGmail(email: string) {
        if (!email) return;
        window.open(`https://mail.google.com/mail/?view=cm&fs=1&to=${email}`, '_blank');
    }

    function vaiInChat(idUtente: string | number | undefined) {
        if (!idUtente) return;
        goto(`${resolveRoute('/dashboard/admin/comunicazioni')}?chatId=${idUtente}`);
    }

    function apriModaleRegistrazione() {
        isEditing = false;
        formDocente = { idUtente: null, nome: '', cognome: '', specializzazioneTecnica: '', email: '', password: '' };
        showAddModal = true;
    }

    function apriModaleModifica() {
        if (!selectedDocente) return;
        isEditing = true;
        formDocente = {
            idUtente: selectedDocente.idUtente,
            nome: selectedDocente.nome,
            cognome: selectedDocente.cognome,
            specializzazioneTecnica: selectedDocente.specializzazioneTecnica || '',
            email: selectedDocente.email,
            password: ''
        };
        showAddModal = true;
    }

    async function salvaDocente() {
        if (!isFormValid) return;
        isSaving = true;
        try {
            if (isEditing && formDocente.idUtente) {
                const payload = {
                    nome: formDocente.nome.trim(),
                    cognome: formDocente.cognome.trim(),
                    email: formDocente.email.trim(),
                    specializzazioneTecnica: formDocente.specializzazioneTecnica.trim()
                };

                const aggiornato = (await AnagraficaService.updateDocente(formDocente.idUtente, payload)) as DocenteData;

                docenti = docenti.map(d => d.idUtente === aggiornato.idUtente ? aggiornato : d);

                if (selectedDocente?.idUtente === aggiornato.idUtente) {
                    selectedDocente = aggiornato;
                }
            } else {
                const payload = {
                    ruolo: 'DOCENTE',
                    nome: formDocente.nome,
                    cognome: formDocente.cognome,
                    email: formDocente.email.trim(),
                    password: formDocente.password,
                    specializzazione: formDocente.specializzazioneTecnica
                } as unknown as AuthRequestDTO;

                await AnagraficaService.registraUtente(payload);
                const res = await AnagraficaService.getAllDocenti();
                docenti = res as DocenteData[];
            }

            showAddModal = false;
        } catch (error) {
            console.error("Si è verificato un errore durante il salvataggio dell'anagrafica docente:", error);
            alert("Operazione fallita. Verificare i dati e riprovare.");
        } finally {
            isSaving = false;
        }
    }

    function preparaEliminazione(d: DocenteData | null) {
        if (!d) return;
        docenteDaEliminare = d;
        showDeleteModal = true;
    }

    async function confermaEliminazione() {
        if (!docenteDaEliminare) return;
        try {
            await AnagraficaService.deleteDocente(docenteDaEliminare.idUtente);
            docenti = docenti.filter(d => d.idUtente !== docenteDaEliminare?.idUtente);
            showDeleteModal = false;
            if (selectedDocente?.idUtente === docenteDaEliminare.idUtente) selectedDocente = null;
            docenteDaEliminare = null;
        } catch (error) {
            console.error("Errore durante la rimozione del docente dal sistema:", error);
        }
    }
</script>

<div in:fade class="max-w-7xl mx-auto p-6">
    {#if !selectedDocente}
        <div class="mb-10 flex justify-between items-start">
            <div>
                <h1 class="text-4xl font-extrabold text-[#1B4B6B] uppercase tracking-tighter">Corpo Docenti</h1>
                <p class="text-gray-500 font-bold uppercase text-xs tracking-tighter">Gestione formatori e specialisti NorLan.</p>
            </div>
            <button
                    onclick={apriModaleRegistrazione}
                    class="bg-white text-[#1B4B6B] border-2 border-[#1B4B6B] px-8 py-3.5 rounded-xl font-extrabold uppercase text-xs shadow-lg transition-all flex items-center gap-3 hover:bg-[#1B4B6B] hover:text-white"
            >
                <Plus size={18} /> Nuovo Docente
            </button>
        </div>

        <div class="mb-8 relative w-72 group">
            <Search class="absolute left-3 top-1/2 -translate-y-1/2 text-gray-400 group-focus-within:text-[#1B4B6B] transition-colors" size={16} />
            <input bind:value={searchQuery} type="text" placeholder="Cerca formatore..." class="w-full pl-10 pr-4 py-2.5 bg-white border border-gray-200 rounded-xl text-xs focus:ring-2 focus:ring-[#1B4B6B] outline-none transition-all font-bold uppercase" />
        </div>

        {#if isLoading}
            <div class="py-20 text-center"><Loader2 size={40} class="animate-spin mx-auto text-[#1B4B6B]" /></div>
        {:else}
            <div class="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6">
                {#each filteredDocenti as d (d.idUtente)}
                    <DocenteCard
                            docente={d}
                            onclick={() => apriDettaglio(d)}
                            onDelete={() => preparaEliminazione(d)}
                    />
                {/each}
            </div>
        {/if}
    {:else}
        <div in:fade>
            <button onclick={() => (selectedDocente = null)} class="flex items-center gap-2 text-[#1B4B6B] font-extrabold uppercase text-[10px] mb-8 hover:gap-3 transition-all"><ChevronLeft size={16} /> Torna all'elenco docenti</button>

            <DettagliCard
                    nome={selectedDocente.nome}
                    cognome={selectedDocente.cognome}
                    sottotitolo="Docente Formatore"
                    onEdit={apriModaleModifica}
                    onMail={() => apreGmail(selectedDocente?.email || '')}
                    onContact={() => vaiInChat(selectedDocente?.idUtente)}
                    onDelete={() => preparaEliminazione(selectedDocente)}
                    items={[
                    { label: 'Specializzazione', value: selectedDocente.specializzazioneTecnica || 'Non specificata', icon: BookOpen },
                    { label: 'Account di Accesso', value: selectedDocente.email, icon: Mail }
                ]}
            />

            <div in:slide class="space-y-6">
                <div class="flex items-center gap-3 mb-6">
                    <div class="p-2.5 bg-[#1B4B6B]/10 text-[#1B4B6B] rounded-xl shadow-inner"><BookOpen size={20} /></div>
                    <h2 class="text-xl font-black text-[#1B4B6B] uppercase tracking-tighter">Corsi Assegnati ({corsiDocente.length})</h2>
                </div>

                {#if isLoadingDettaglio}
                    <div class="py-10 text-center"><Loader2 size={30} class="animate-spin mx-auto text-[#1B4B6B]" /></div>
                {:else if corsiDocente.length > 0}
                    <div class="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6">
                        {#each corsiDocente as corso (corso.idCorso)}
                            <div class="bg-white p-6 rounded-2xl border border-gray-100 shadow-sm">
                                <h4 class="font-extrabold text-[#1B4B6B] uppercase text-sm leading-tight mb-4">{corso.titolo}</h4>
                                <div class="space-y-2 pt-4 border-t border-gray-50 text-[10px] font-bold text-gray-500 uppercase">
                                    <div class="flex items-center gap-2"><Calendar size={12}/> {new Date(corso.dataOrario).toLocaleDateString()}</div>
                                </div>
                            </div>
                        {/each}
                    </div>
                {:else}
                    <div class="bg-white rounded-3xl p-8 text-center border-2 border-dashed border-gray-200 text-gray-300 uppercase font-bold text-[10px] italic">
                        Nessun corso formativo assegnato attualmente
                    </div>
                {/if}
            </div>
        </div>
    {/if}

    {#if showAddModal}
        <div class="fixed inset-0 bg-[#1B4B6B]/40 backdrop-blur-sm flex items-center justify-center z-[100] p-4" transition:fade>
            <div class="bg-white rounded-3xl shadow-2xl w-full max-w-lg overflow-hidden" in:scale>
                <div class="bg-[#1B4B6B] p-6 text-white flex justify-between items-center">
                    <h2 class="text-xl font-black uppercase tracking-tighter flex items-center gap-2">
                        {#if isEditing}
                            <Edit3 size={20}/> Modifica Formatore
                        {:else}
                            <GraduationCap size={20}/> Registra Docente
                        {/if}
                    </h2>
                    <button onclick={() => (showAddModal = false)} class="text-white hover:rotate-90 transition-all duration-300"><X size={24}/></button>
                </div>
                <div class="p-8 space-y-4">
                    <div class="grid grid-cols-2 gap-4">
                        <div class="space-y-1">
                            <label class="block text-[10px] font-bold text-[#1B4B6B] uppercase">Nome *</label>
                            <input bind:value={formDocente.nome} placeholder="Es: Mario" class="w-full p-3 bg-gray-50 border-none rounded-xl text-sm focus:ring-2 focus:ring-[#1B4B6B] outline-none" />
                        </div>
                        <div class="space-y-1">
                            <label class="block text-[10px] font-bold text-[#1B4B6B] uppercase">Cognome *</label>
                            <input bind:value={formDocente.cognome} placeholder="Es: Rossi" class="w-full p-3 bg-gray-50 border-none rounded-xl text-sm focus:ring-2 focus:ring-[#1B4B6B] outline-none" />
                        </div>
                    </div>

                    <div class="space-y-1">
                        <label class="block text-[10px] font-bold text-[#1B4B6B] uppercase">Specializzazione *</label>
                        <input bind:value={formDocente.specializzazioneTecnica} placeholder="Es: Sicurezza sul Lavoro" class="w-full p-3 bg-gray-50 border-none rounded-xl text-sm focus:ring-2 focus:ring-[#1B4B6B] outline-none" />
                    </div>

                    <div class="space-y-1">
                        <label class="block text-[10px] font-bold text-[#1B4B6B] uppercase">Email *</label>
                        <input bind:value={formDocente.email} placeholder="docente@norlan.it" class="w-full p-3 bg-gray-50 border-none rounded-xl text-sm focus:ring-2 focus:ring-[#1B4B6B] outline-none" />
                    </div>

                    {#if !isEditing}
                        <div class="space-y-1">
                            <label class="block text-[10px] font-bold text-[#1B4B6B] uppercase tracking-tight">Password Temporanea *</label>
                            <input bind:value={formDocente.password} type="password" placeholder="••••••••" class="w-full p-3 bg-gray-50 border-none rounded-xl text-sm focus:ring-2 focus:ring-[#1B4B6B] outline-none" />
                            <div class="mt-2 flex items-start gap-2">
                                <div class="mt-0.5 text-orange-500"><AlertTriangle size={12}/></div>
                                <p class="text-[8px] text-gray-400 font-bold uppercase leading-tight">
                                    Nota: Il docente dovrà obbligatoriamente cambiare questa password al suo primo accesso.
                                </p>
                            </div>
                        </div>
                    {:else}
                        <div class="p-4 bg-blue-50 border border-blue-100 rounded-xl mt-4">
                            <p class="text-[10px] font-bold text-blue-800 uppercase leading-tight">Nota di Sicurezza</p>
                            <p class="text-[9px] text-blue-600 uppercase mt-1 leading-relaxed">
                                Per motivi di sicurezza, la password può essere modificata solo dal docente tramite il suo pannello.
                            </p>
                        </div>
                    {/if}
                </div>
                <div class="p-8 bg-gray-50 flex justify-end gap-4 border-t border-gray-100">
                    <button onclick={() => (showAddModal = false)} class="px-6 py-3 text-[10px] font-black uppercase text-gray-400 hover:text-gray-600 transition-colors">Annulla</button>
                    <button
                            onclick={salvaDocente}
                            disabled={!isFormValid || isSaving}
                            class="bg-[#1B4B6B] text-white px-8 py-3 rounded-xl text-[10px] font-black uppercase shadow-lg hover:bg-[#153a54] disabled:opacity-50 flex items-center gap-2"
                    >
                        {#if isSaving}
                            <Loader2 size={14} class="animate-spin"/>
                        {:else if isEditing}
                            <Edit3 size={14}/>
                        {:else}
                            <GraduationCap size={14}/>
                        {/if}
                        {isEditing ? 'Aggiorna Dati' : 'Registra Docente'}
                    </button>
                </div>
            </div>
        </div>
    {/if}

    {#if showDeleteModal}
        <div class="fixed inset-0 bg-red-900/20 backdrop-blur-sm flex items-center justify-center z-[110] p-4" transition:fade>
            <div class="bg-white rounded-3xl shadow-2xl w-full max-w-sm overflow-hidden" in:scale>
                <div class="p-8 text-center">
                    <div class="w-20 h-20 bg-red-50 text-red-600 rounded-full flex items-center justify-center mx-auto mb-6"><Trash2 size={40}/></div>
                    <h2 class="text-2xl font-black text-[#1B4B6B] uppercase tracking-tighter mb-2">Rimuovere Docente?</h2>
                    <p class="text-sm text-gray-400 mb-8">
                        Il docente <span class="font-bold text-[#1B4B6B]">{docenteDaEliminare?.nome} {docenteDaEliminare?.cognome}</span> verrà rimosso definitivamente dal sistema.
                    </p>
                    <div class="flex flex-col gap-3">
                        <button onclick={confermaEliminazione} class="w-full bg-red-600 text-white py-4 rounded-2xl font-black uppercase text-[10px] shadow-lg shadow-red-200 transition-all hover:bg-red-700">
                            Sì, elimina definitivamente
                        </button>
                        <button onclick={() => (showDeleteModal = false)} class="w-full py-4 text-[10px] font-black uppercase text-gray-400 hover:text-gray-600">
                            No, annulla l'operazione
                        </button>
                    </div>
                </div>
            </div>
        </div>
    {/if}
</div>