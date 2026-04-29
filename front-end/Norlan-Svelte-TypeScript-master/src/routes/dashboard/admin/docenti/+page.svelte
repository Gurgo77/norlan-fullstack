<script lang="ts">
    import { onMount } from 'svelte';
    import { fade, scale, slide } from 'svelte/transition';
    import { goto } from '$app/navigation';
    import {
        GraduationCap, Plus, Trash2, Search, Mail, MessageSquare,
        ChevronRight, ChevronLeft, Loader2, X, AlertTriangle, BookOpen,
        Calendar, Users, CheckCircle2
    } from 'lucide-svelte';

    // Modelli e Servizi
    import type { DocenteData } from '$lib/models/Docente';
    import type { CorsoFormazione } from '$lib/models/CorsoFormazione';
    import { AnagraficaService, type AuthRequestDTO } from '$lib/services/AnagraficaService';
    import { FormazioneService } from '$lib/services/FormazioneService';

    // --- STATO REATTIVO (Svelte 5) ---
    let docenti = $state<DocenteData[]>([]);
    let corsiDocente = $state<CorsoFormazione[]>([]);

    let isLoading = $state(true);
    let isLoadingDettaglio = $state(false);
    let searchQuery = $state('');

    let selectedDocente = $state<DocenteData | null>(null);

    // Modali
    let showAddModal = $state(false);
    let isSaving = $state(false);

    let showDeleteModal = $state(false);
    let docenteDaEliminare = $state<DocenteData | null>(null);

    // Form di registrazione
    let formDocente = $state({
        nome: '',
        cognome: '',
        specializzazioneTecnica: '',
        email: '',
        password: ''
    });

    // --- LOGICA DERIVATA ---
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
        formDocente.password.trim() !== ''
    );


    // --- AZIONI ---
    onMount(async () => {
        try {
            const res = await AnagraficaService.getAllDocenti();
            docenti = res as DocenteData[];
        } catch (error) {
            console.error("Errore caricamento docenti:", error);
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
            console.error("Errore caricamento corsi docente:", error);
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

        // eslint-disable-next-line svelte/no-navigation-without-resolve
        goto(`/dashboard/admin/comunicazioni?chatId=${idUtente}`);
    }

    async function salvaDocente() {
        if (!isFormValid) return;
        isSaving = true;
        try {
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
            showAddModal = false;
            formDocente = { nome: '', cognome: '', specializzazioneTecnica: '', email: '', password: '' };
        } catch (error) {
            console.error("Errore salvataggio docente:", error);
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
        // Abbiamo tolto il controllo sulla parola digitata!
        if (!docenteDaEliminare) return;

        try {
            await AnagraficaService.deleteDocente(docenteDaEliminare.idUtente);
            docenti = docenti.filter(d => d.idUtente !== docenteDaEliminare?.idUtente);
            showDeleteModal = false;
            if (selectedDocente?.idUtente === docenteDaEliminare.idUtente) selectedDocente = null;
            docenteDaEliminare = null;
        } catch (error) {
            console.error("Errore eliminazione:", error);
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
                    onclick={() => (showAddModal = true)}
                    class="bg-white text-docente border-2 border-docente px-8 py-3.5 rounded-xl font-extrabold uppercase text-xs shadow-lg transition-all flex items-center gap-3 hover:bg-docente hover:text-white"
            >
                <Plus size={18} /> Nuovo Docente
            </button>
        </div>

        <div class="mb-8 relative w-72 group">
            <Search class="absolute left-3 top-1/2 -translate-y-1/2 text-gray-400 group-focus-within:text-[#1B4B6B] transition-colors" size={16} />
            <input bind:value={searchQuery} type="text" placeholder="Cerca formatore..." class="w-full pl-10 pr-4 py-2.5 bg-white border border-gray-200 rounded-xl text-xs focus:ring-2 focus:ring-[#1B4B6B] outline-none transition-all font-bold uppercase" />
        </div>

        {#if isLoading}
            <div class="py-20 text-center"><Loader2 size={40} class="animate-spin mx-auto text-docente" /></div>
        {:else}
            <div class="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6">
                {#each filteredDocenti as d (d.idUtente)}
                    <div class="bg-white rounded-3xl border border-gray-100 shadow-sm hover:shadow-xl hover:-translate-y-1 transition-all group relative flex flex-col h-full overflow-hidden hover:border-docente/30" in:scale>
                        <div role="button" tabindex="0" onclick={() => apriDettaglio(d)} onkeydown={(e) => e.key === 'Enter' && apriDettaglio(d)} class="p-6 pb-4 cursor-pointer flex-1">
                            <button
                                    onclick={(e) => { e.stopPropagation(); preparaEliminazione(d); }}
                                    class="absolute top-4 right-4 p-2 text-gray-300 hover:text-red-600 opacity-0 group-hover:opacity-100 transition-all z-10 hover:bg-red-50 rounded-lg"
                            >
                                <Trash2 size={18} />
                            </button>

                            <div class="flex items-center gap-4 mb-5">
                                <div class="w-14 h-14 bg-docente-light text-docente rounded-2xl flex items-center justify-center font-black text-lg group-hover:bg-docente group-hover:text-white transition-all">
                                    {d.nome[0]}{d.cognome[0]}
                                </div>
                                <div>
                                    <h3 class="font-extrabold text-[#1B4B6B] text-lg uppercase leading-tight">{d.nome} {d.cognome}</h3>
                                    <p class="text-[10px] text-gray-400 font-bold uppercase mt-1 flex items-center gap-1"><GraduationCap size={12}/> Formatore</p>
                                </div>
                            </div>

                            <div class="space-y-3 pt-4 border-t border-gray-50">
                                <div>
                                    <p class="text-[9px] text-gray-400 font-bold uppercase mb-0.5">Specializzazione</p>
                                    <p class="text-[11px] font-black text-[#1B4B6B] uppercase leading-tight truncate">{d.specializzazioneTecnica || 'Non specificata'}</p>
                                </div>
                            </div>
                        </div>

                        <button onclick={() => apriDettaglio(d)} class="mt-auto w-full p-6 pt-4 border-t border-gray-50 flex justify-between items-center hover:bg-docente-light transition-colors">
                            <div class="flex items-center gap-2"><BookOpen size={16} class="text-docente"/><span class="text-[10px] font-bold text-gray-400 uppercase italic">Vedi Scheda</span></div>
                            <ChevronRight size={20} class="text-[#1B4B6B]" />
                        </button>
                    </div>
                {/each}
            </div>
        {/if}
    {:else}
        <div in:fade>
            <button onclick={() => (selectedDocente = null)} class="flex items-center gap-2 text-[#1B4B6B] font-extrabold uppercase text-[10px] mb-8 hover:gap-3 transition-all"><ChevronLeft size={16} /> Torna all'elenco docenti</button>

            <div class="bg-white rounded-3xl shadow-xl border border-gray-100 overflow-hidden mb-12">
                <div class="bg-docente p-10 text-white flex justify-between items-end relative">
                    <div class="flex items-center gap-6">
                        <div class="w-24 h-24 bg-white text-docente rounded-3xl flex items-center justify-center font-black text-4xl shadow-lg">
                            {selectedDocente.nome[0]}{selectedDocente.cognome[0]}
                        </div>
                        <div>
                            <div class="flex items-center gap-3 mb-3">
                                <span class="bg-white/20 border border-white/20 text-white text-[10px] font-black px-4 py-1.5 rounded-full uppercase flex items-center gap-2"><GraduationCap size={12}/> Docente Specializzato</span>
                            </div>
                            <h1 class="text-5xl font-extrabold uppercase tracking-tighter">{selectedDocente.nome} {selectedDocente.cognome}</h1>
                        </div>
                    </div>

                    <div class="flex items-center gap-3">
                        <button onclick={() => apreGmail(selectedDocente?.email || '')} class="flex items-center gap-2 bg-white text-docente px-6 py-3.5 rounded-2xl transition-all font-extrabold uppercase text-[10px] shadow-xl hover:bg-gray-100 hover:scale-105">
                            <Mail size={16} /> Manda Mail
                        </button>
                        <button onclick={() => vaiInChat(selectedDocente?.idUtente)} class="flex items-center gap-2 bg-[#1B4B6B] text-white px-6 py-3.5 rounded-2xl transition-all font-extrabold uppercase text-[10px] shadow-xl hover:bg-[#1B4B6B]/90 hover:scale-105">
                            <MessageSquare size={16} /> Contatta
                        </button>
                        <button onclick={() => preparaEliminazione(selectedDocente)} class="flex items-center gap-2 bg-red-600/90 text-white px-6 py-3.5 rounded-2xl transition-all font-extrabold uppercase text-[10px] border border-white/10 shadow-xl hover:bg-red-700 hover:scale-105">
                            <Trash2 size={16} /> Rimuovi
                        </button>
                    </div>
                </div>

                <div class="p-8 grid grid-cols-1 md:grid-cols-2 gap-8 bg-gray-50/30">
                    <div>
                        <p class="text-[10px] font-bold text-gray-400 uppercase mb-1 flex items-center gap-1"><BookOpen size={12}/> Specializzazione</p>
                        <p class="text-lg font-extrabold text-[#1B4B6B] uppercase tracking-wide">{selectedDocente.specializzazioneTecnica || 'Non specificata'}</p>
                    </div>
                    <div>
                        <p class="text-[10px] font-bold text-gray-400 uppercase mb-1 flex items-center gap-1"><Mail size={12}/> Account di Accesso</p>
                        <p class="text-lg font-bold text-[#1B4B6B] lowercase">{selectedDocente.email}</p>
                    </div>
                </div>
            </div>

            <div in:slide class="space-y-6">
                <div class="flex items-center gap-3 mb-6">
                    <div class="p-2.5 bg-docente-light text-docente rounded-xl shadow-inner"><BookOpen size={20} /></div>
                    <h2 class="text-xl font-black text-[#1B4B6B] uppercase tracking-tighter">Corsi Assegnati ({corsiDocente.length})</h2>
                </div>

                {#if isLoadingDettaglio}
                    <div class="py-10 text-center"><Loader2 size={30} class="animate-spin mx-auto text-docente" /></div>
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
                <div class="bg-docente p-6 text-white flex justify-between items-center">
                    <h2 class="text-xl font-black uppercase tracking-tighter flex items-center gap-2"><GraduationCap size={20}/> Registra Docente</h2>
                    <button onclick={() => (showAddModal = false)} class="hover:rotate-90 transition-transform"><X size={24}/></button>
                </div>
                <div class="p-8 space-y-4">
                    <div class="grid grid-cols-2 gap-4">
                        <div class="space-y-1">
                            <label class="block text-[10px] font-bold text-docente uppercase">Nome *</label>
                            <input bind:value={formDocente.nome} placeholder="Es: Mario" class="w-full p-3 bg-gray-50 border-none rounded-xl text-sm focus:ring-2 focus:ring-docente outline-none" />
                        </div>
                        <div class="space-y-1">
                            <label class="block text-[10px] font-bold text-docente uppercase">Cognome *</label>
                            <input bind:value={formDocente.cognome} placeholder="Es: Rossi" class="w-full p-3 bg-gray-50 border-none rounded-xl text-sm focus:ring-2 focus:ring-docente outline-none" />
                        </div>
                    </div>

                    <div class="space-y-1">
                        <label class="block text-[10px] font-bold text-docente uppercase">Specializzazione *</label>
                        <input bind:value={formDocente.specializzazioneTecnica} placeholder="Es: Sicurezza sul Lavoro" class="w-full p-3 bg-gray-50 border-none rounded-xl text-sm focus:ring-2 focus:ring-docente outline-none" />
                    </div>

                    <div class="space-y-1">
                        <label class="block text-[10px] font-bold text-docente uppercase">Email *</label>
                        <input bind:value={formDocente.email} placeholder="docente@norlan.it" class="w-full p-3 bg-gray-50 border-none rounded-xl text-sm focus:ring-2 focus:ring-docente outline-none" />
                    </div>

                    <div class="space-y-1">
                        <label class="block text-[10px] font-bold text-docente uppercase tracking-tight">Password Temporanea *</label>
                        <input bind:value={formDocente.password} type="password" placeholder="••••••••" class="w-full p-3 bg-gray-50 border-none rounded-xl text-sm focus:ring-2 focus:ring-docente outline-none" />
                        <div class="mt-2 flex items-start gap-2">
                            <div class="mt-0.5 text-orange-500"><AlertTriangle size={12}/></div>
                            <p class="text-[8px] text-gray-400 font-bold uppercase leading-tight">
                                Nota: Il docente dovrà obbligatoriamente cambiare questa password al suo primo accesso.
                            </p>
                        </div>
                    </div>
                </div>
                <div class="p-8 bg-gray-50 flex justify-end gap-4 border-t border-gray-100">
                    <button onclick={() => (showAddModal = false)} class="px-6 py-3 text-[10px] font-black uppercase text-gray-400 hover:text-gray-600 transition-colors">Annulla</button>
                    <button
                            onclick={salvaDocente}
                            disabled={!isFormValid || isSaving}
                            class="bg-docente text-white px-8 py-3 rounded-xl text-[10px] font-black uppercase shadow-lg hover:bg-docente-dark disabled:opacity-50 flex items-center gap-2"
                    >
                        {#if isSaving}<Loader2 size={14} class="animate-spin"/>{/if}
                        Registra Docente
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