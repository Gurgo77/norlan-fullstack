<script lang="ts">
	import { onMount } from 'svelte';
	import { page } from '$app/stores';
	import { goto } from '$app/navigation';
	import { fade, scale, slide } from 'svelte/transition';
	import {
		Users, UserPlus, Trash2, Search, Mail, Building2,
		IdCard, Loader2, X, ChevronRight, AlertTriangle, ChevronLeft,
		FileText, ShieldCheck, Download, Plus, Calendar, MessageSquare, Upload
	} from 'lucide-svelte';

	// Servizi e Modelli
	import { LavoratoreService, type DipendenteDTO, type DipendenteRequest } from '$lib/services/LavoratoreService';
	import { AuthService } from '$lib/services/AuthService';
	import { DocumentoService } from '$lib/services/DocumentoService';
	import { Documento } from '$lib/models/Documento';
	import type { AssegnazioneDPI } from '$lib/models/AssegnazioneDPI';
	import { ModuloServizio, TipoDocumento, TipoDPI } from '$lib/models/Enums';

	// Interfaccia estesa per la vista
	interface DipendenteEsteso extends DipendenteDTO {
		nomeAzienda?: string;
	}

	// --- STATO REATTIVO ---
	let lavoratori = $state<DipendenteEsteso[]>([]);
	let isLoading = $state(true);
	let searchQuery = $state('');
	let idAziendaCorrente = $state<number | string>('');
	let nomeAziendaCorrente = $state<string>('La tua Azienda');

	let selectedDipendente = $state<DipendenteEsteso | null>(null);
	let documentiCorrenti = $state<Documento[]>([]);
	let dpiCorrenti = $state<AssegnazioneDPI[]>([]);
	let isLoadingDettaglio = $state(false);

	let showAddModal = $state(false);
	let showDeleteModal = $state(false);
	let isSaving = $state(false);

	let dipendenteDaEliminare = $state<DipendenteEsteso | null>(null);
	let formDipendente = $state({
		nome: '', cognome: '', codiceFiscale: '', email: ''
	});

	// --- STATI DOCUMENTI ---
	let showUploadModal = $state(false);
	let isUploading = $state(false);
	let uploadFile = $state<File | null>(null);
	let formDocumento = $state({ modulo: ModuloServizio.SICUREZZA, tipologia: TipoDocumento.ATTESTATO_FORMAZIONE, dataScadenza: '' });

	let showDeleteDocModal = $state(false);
	let docDaEliminare = $state<Documento | null>(null);

	// --- STATI DPI ---
	let showDpiModal = $state(false);
	let isSavingDpi = $state(false);
	let formDpi = $state({ tipo: '' as unknown as TipoDPI, dataConsegna: '' });

	let showDeleteDpiModal = $state(false);
	let dpiDaEliminare = $state<AssegnazioneDPI | null>(null);

	// --- LOGICA DERIVATA ---
	const filteredLavoratori = $derived(
			lavoratori.filter(l =>
					l.nome.toLowerCase().includes(searchQuery.toLowerCase()) ||
					l.cognome.toLowerCase().includes(searchQuery.toLowerCase()) ||
					l.codiceFiscale.toLowerCase().includes(searchQuery.toLowerCase())
			)
	);

	const isFormValid = $derived(
			formDipendente.nome.trim() !== '' && formDipendente.cognome.trim() !== '' &&
			formDipendente.codiceFiscale.length === 16
	);

	// --- AZIONI ---

	onMount(async () => {
		try {
			const session = AuthService.getSession();
			if (!session) return;

			idAziendaCorrente = session.idUtente;
			nomeAziendaCorrente = session.ragioneSociale || "La tua Azienda";

			const resLavoratori = await LavoratoreService.getByAzienda(idAziendaCorrente);

			lavoratori = resLavoratori.map((l: DipendenteDTO) => ({
				...l,
				nomeAzienda: nomeAziendaCorrente
			}));

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
			documentiCorrenti = await DocumentoService.getDocumentiByAzienda(lavoratore.idUtente);
			// dpiCorrenti = await LavoratoreService.getDpiByLavoratore(lavoratore.idUtente); // Da integrare col backend
		} catch {
			console.error("Errore dettaglio dipendente");
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
		return await goto(`/dashboard/azienda/comunicazioni?chatId=${idUtente}`);
	}

	async function salvaDipendente() {
		if (!isFormValid || !idAziendaCorrente) return;
		isSaving = true;
		try {
			const payload: DipendenteRequest = {
				nome: formDipendente.nome,
				cognome: formDipendente.cognome,
				codiceFiscale: formDipendente.codiceFiscale,
				email: formDipendente.email
			};
			const nuovo = await LavoratoreService.create(idAziendaCorrente, payload);
			const esteso: DipendenteEsteso = { ...nuovo, nomeAzienda: nomeAziendaCorrente };
			lavoratori = [esteso, ...lavoratori];
			showAddModal = false;
			formDipendente = { nome: '', cognome: '', codiceFiscale: '', email: '' };
		} catch {
			alert("Errore durante la registrazione.");
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

	// --- LOGICA DOCUMENTI ---
	async function scaricaDoc(doc: Documento) {
		try {
			const b = await DocumentoService.downloadDocumento(doc.idDocumento);
			const u = URL.createObjectURL(b);
			const a = document.createElement('a');
			a.href = u;
			a.download = doc.filePath.split('/').pop() || 'doc.pdf';
			a.click();
			URL.revokeObjectURL(u);
		} catch {
			alert("Errore download.");
		}
	}

	function preparaAggiornamento(doc: Documento) {
		formDocumento.modulo = doc.modulo;
		formDocumento.tipologia = doc.tipologia;
		showUploadModal = true;
	}

	async function gestisciUpload() {
		if (!uploadFile || !selectedDipendente || !formDocumento.dataScadenza) return;
		isUploading = true;
		try {
			const fd = new FormData();
			fd.append('file', uploadFile);
			fd.append('modulo', formDocumento.modulo);
			fd.append('tipologia', formDocumento.tipologia);
			fd.append('dataScadenzaStr', formDocumento.dataScadenza);

			await DocumentoService.uploadDocumento(selectedDipendente.idUtente, fd);
			documentiCorrenti = await DocumentoService.getDocumentiByAzienda(selectedDipendente.idUtente);
			showUploadModal = false;
			uploadFile = null;
		} catch {
			alert("Errore upload.");
		} finally {
			isUploading = false;
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
			alert("Errore eliminazione documento.");
		}
	}

	// --- LOGICA DPI ---
	async function salvaDPI() {
		if (!formDpi.tipo || !formDpi.dataConsegna || !selectedDipendente) return;
		isSavingDpi = true;
		try {
			const nuovoDpi: AssegnazioneDPI = {
				idAssegnazione: Date.now(),
				idDipendente: selectedDipendente.idUtente,
				tipo: formDpi.tipo,
				dataConsegna: formDpi.dataConsegna,
				dataScadenzaRevisione: '',
				daRevisionare: false
			};

			dpiCorrenti = [...dpiCorrenti, nuovoDpi];
			showDpiModal = false;
			formDpi = { tipo: '' as unknown as TipoDPI, dataConsegna: '' };
		} catch {
			alert("Errore salvataggio DPI.");
		} finally {
			isSavingDpi = false;
		}
	}

	function preparaEliminaDPI(dpi: AssegnazioneDPI) { dpiDaEliminare = dpi; showDeleteDpiModal = true; }

	async function confermaEliminaDPI() {
		if (!dpiDaEliminare) return;
		try {
			dpiCorrenti = dpiCorrenti.filter(d => d !== dpiDaEliminare);
			showDeleteDpiModal = false;
			dpiDaEliminare = null;
		} catch {
			alert("Errore eliminazione DPI.");
		}
	}
</script>

<div in:fade class="max-w-7xl mx-auto p-6">

	{#if !selectedDipendente}
		<div class="mb-10 flex justify-between items-start">
			<div>
				<h1 class="text-4xl font-extrabold text-[#1B4B6B] uppercase tracking-tighter">Anagrafica Dipendenti</h1>
				<p class="text-gray-500 font-bold uppercase text-xs tracking-tighter">Gestione dei lavoratori </p>
			</div>
			<button
					onclick={() => (showAddModal = true)}
					class="bg-white text-purple-600 border-2 border-purple-600 px-8 py-3.5 rounded-xl font-extrabold uppercase text-xs shadow-lg hover:bg-purple-600 hover:text-white transition-all flex items-center gap-3"
			>
				<UserPlus size={18} /> Aggiungi Dipendente
			</button>
		</div>

		<div class="mb-8 flex gap-4">
			<div class="relative w-72 group">
				<Search class="absolute left-3 top-1/2 -translate-y-1/2 text-gray-400 group-focus-within:text-[#1B4B6B] transition-colors" size={16} />
				<input bind:value={searchQuery} type="text" placeholder="Cerca lavoratore..." class="w-full pl-10 pr-4 py-2.5 bg-white border border-gray-200 rounded-xl text-xs focus:ring-2 focus:ring-[#1B4B6B] outline-none transition-all font-bold uppercase" />
			</div>
		</div>

		{#if isLoading}
			<div class="py-20 text-center"><Loader2 size={40} class="animate-spin mx-auto text-[#1B4B6B]" /></div>
		{:else}
			<div class="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6">
				{#each filteredLavoratori as l (l.idUtente)}
					<div class="bg-white rounded-3xl border border-gray-100 shadow-sm hover:shadow-xl transition-all group relative flex flex-col h-full overflow-hidden" in:scale>

						<div role="button" tabindex="0" onclick={() => apriDettaglio(l)} onkeydown={(e) => e.key === 'Enter' && apriDettaglio(l)} class="p-6 pb-4 cursor-pointer flex-1">
							<button
									onclick={(e) => { e.stopPropagation(); preparaEliminazione(l); }}
									class="absolute top-4 right-4 p-2 text-gray-300 hover:text-red-600 opacity-0 group-hover:opacity-100 transition-all z-10 hover:bg-red-50 rounded-lg"
							>
								<Trash2 size={18} />
							</button>

							<div class="flex items-center gap-4 mb-6">
								<div class="w-14 h-14 bg-purple-50 text-purple-600 rounded-2xl flex items-center justify-center font-black text-lg group-hover:bg-purple-600 group-hover:text-white transition-all">
									{l.nome[0]}{l.cognome[0]}
								</div>
								<div>
									<h3 class="font-extrabold text-[#1B4B6B] text-lg uppercase leading-tight">{l.nome} {l.cognome}</h3>
									<div class="flex items-center gap-1 text-gray-400 mt-1">
										<Building2 size={12} class="text-[#1B4B6B]"/>
										<span class="text-[9px] font-bold uppercase truncate max-w-[150px] text-[#1B4B6B]">
                                            {l.nomeAzienda}
                                        </span>
									</div>
								</div>
							</div>

							<div class="space-y-3 pt-4 border-t border-gray-50">
								<div class="flex items-center justify-between text-[10px] font-bold uppercase">
									<span class="text-gray-400 flex items-center gap-1"><IdCard size={12}/> C. Fiscale</span>
									<span class="text-[#1B4B6B] font-mono tracking-wider">{l.codiceFiscale}</span>
								</div>
							</div>
						</div>

						<button onclick={() => apriDettaglio(l)} class="mt-auto w-full p-6 pt-4 border-t border-gray-50 flex justify-between items-center hover:bg-gray-50/50 transition-colors">
							<div class="flex items-center gap-2"><FileText size={16} class="text-purple-600"/><span class="text-[10px] font-bold text-gray-400 uppercase italic">Vedi Dettagli Lavoratore</span></div>
							<ChevronRight size={20} class="text-[#1B4B6B]" />
						</button>
					</div>
				{:else}
					<div class="col-span-full py-20 text-center bg-gray-50 rounded-3xl border-2 border-dashed border-gray-200">
						<Users size={48} class="mx-auto text-gray-300 mb-4" />
						<p class="text-gray-400 font-bold uppercase text-xs">Nessun dipendente trovato</p>
					</div>
				{/each}
			</div>
		{/if}

	{:else}
		<div in:fade>
			<button onclick={() => (selectedDipendente = null)} class="flex items-center gap-2 text-[#1B4B6B] font-extrabold uppercase text-[10px] mb-8 hover:gap-3 transition-all"><ChevronLeft size={16} /> Torna all'elenco dipendenti</button>

			<div class="bg-white rounded-3xl shadow-xl border border-gray-100 overflow-hidden mb-12">
				<div class="bg-purple-600 p-10 text-white flex justify-between items-end relative">
					<div class="flex items-center gap-6">
						<div class="w-24 h-24 bg-white text-purple-600 rounded-3xl flex items-center justify-center font-black text-4xl shadow-lg">
							{selectedDipendente.nome[0]}{selectedDipendente.cognome[0]}
						</div>
						<div>
							<div class="flex items-center gap-3 mb-3">
								<span class="bg-purple-500/50 border border-white/20 text-white text-[10px] font-black px-4 py-1.5 rounded-full uppercase flex items-center gap-2"><Building2 size={12}/> {selectedDipendente.nomeAzienda}</span>
							</div>
							<h1 class="text-5xl font-extrabold uppercase tracking-tighter">{selectedDipendente.nome} {selectedDipendente.cognome}</h1>
						</div>
					</div>

					<div class="flex items-center gap-3">
						<button onclick={() => apreGmail(selectedDipendente?.email || '')} class="flex items-center gap-2 bg-white text-purple-600 px-6 py-3.5 rounded-2xl transition-all font-extrabold uppercase text-[10px] shadow-xl hover:bg-gray-100 hover:scale-105">
							<Mail size={16} /> Manda Mail
						</button>
						<button onclick={() => vaiInChat(selectedDipendente?.idUtente)} class="flex items-center gap-2 bg-[#1B4B6B] text-white px-6 py-3.5 rounded-2xl transition-all font-extrabold uppercase text-[10px] shadow-xl hover:bg-[#1B4B6B]/90 hover:scale-105">
							<MessageSquare size={16} /> Contatta
						</button>
						<button onclick={() => preparaEliminazione(selectedDipendente)} class="flex items-center gap-2 bg-red-600/90 text-white px-6 py-3.5 rounded-2xl transition-all font-extrabold uppercase text-[10px] border border-white/10 shadow-xl hover:bg-red-700 hover:scale-105">
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
				<div class="py-20 text-center"><Loader2 size={40} class="animate-spin mx-auto text-purple-600" /></div>
			{:else}
				<div class="grid grid-cols-1 lg:grid-cols-2 gap-8">

					<div class="space-y-6">
						<div class="flex items-center justify-between">
							<div class="flex items-center gap-3">
								<div class="p-2.5 bg-blue-100 text-blue-600 rounded-xl shadow-inner"><FileText size={20} /></div>
								<h2 class="text-xl font-black text-[#1B4B6B] uppercase tracking-tighter">Documenti ({documentiCorrenti.length})</h2>
							</div>
							<button onclick={() => (showUploadModal = true)} class="bg-[#1B4B6B] text-white px-4 py-2 rounded-xl font-bold uppercase text-[9px] flex items-center gap-2 hover:bg-[#1B4B6B]/90 transition-all shadow-md">
								<Plus size={14} /> Aggiungi
							</button>
						</div>

						{#if documentiCorrenti.length > 0}
							<div class="space-y-4">
								{#each documentiCorrenti as doc (doc.idDocumento)}
									<div class="bg-white p-5 rounded-2xl border border-gray-100 shadow-sm hover:shadow-md transition-all">
										<div class="flex justify-between items-start mb-3">
											<div class="flex items-center gap-3">
												<div class="p-2 bg-gray-50 rounded-xl text-[#1B4B6B]"><FileText size={16} /></div>
												<div>
													<h4 class="font-extrabold text-[#1B4B6B] uppercase text-xs leading-tight">{doc.tipologia.replace(/_/g, ' ')}</h4>
													<p class="text-[9px] text-gray-400 font-bold uppercase mt-0.5">{doc.modulo}</p>
												</div>
											</div>
											<div class="flex gap-1">
												<button onclick={() => scaricaDoc(doc)} class="p-1.5 text-gray-400 hover:text-[#1B4B6B] transition-all bg-gray-50 rounded-lg hover:bg-gray-100"><Download size={14} /></button>
												<button onclick={() => preparaEliminaDoc(doc)} class="p-1.5 text-gray-400 hover:text-red-600 transition-all bg-gray-50 rounded-lg hover:bg-red-50"><Trash2 size={14} /></button>
											</div>
										</div>
										<div class="flex items-center justify-between pt-3 border-t border-gray-50">
											<div class="flex items-center gap-1.5 text-gray-400">
												<Calendar size={10} />
												<span class="text-[9px] font-bold uppercase">Scad: {new Date(doc.dataScadenza).toLocaleDateString()}</span>
											</div>
											{#if doc.scaduto}
												<button onclick={() => preparaAggiornamento(doc)} class="text-[8px] font-black px-3 py-1 bg-[#1B4B6B] text-white rounded-md uppercase shadow-sm hover:bg-[#1B4B6B]/80 hover:scale-105 transition-all">Aggiorna</button>
											{:else}
												<span class="text-[8px] font-black px-2 py-1 bg-green-50 text-green-600 border border-green-100 rounded-md uppercase">Valido</span>
											{/if}
										</div>
									</div>
								{/each}
							</div>
						{:else}
							<div class="bg-white rounded-3xl p-8 text-center border-2 border-dashed border-gray-200 text-gray-300 uppercase font-bold text-[10px] italic">
								Nessun documento associato al lavoratore
							</div>
						{/if}
					</div>

					<div class="space-y-6">
						<div class="flex items-center justify-between">
							<div class="flex items-center gap-3">
								<div class="p-2.5 bg-green-100 text-green-600 rounded-xl shadow-inner"><ShieldCheck size={20} /></div>
								<h2 class="text-xl font-black text-[#1B4B6B] uppercase tracking-tighter">DPI Consegnati ({dpiCorrenti.length})</h2>
							</div>
							<button onclick={() => (showDpiModal = true)} class="bg-green-600 text-white px-4 py-2 rounded-xl font-bold uppercase text-[9px] flex items-center gap-2 hover:bg-green-700 transition-all shadow-md">
								<Plus size={14} /> Assegna DPI
							</button>
						</div>

						{#if dpiCorrenti.length > 0}
							<div class="space-y-4">
								{#each dpiCorrenti as dpi (dpi.idAssegnazione)}
									<div class="bg-white p-5 rounded-2xl border border-gray-100 shadow-sm hover:shadow-md transition-all">
										<div class="flex justify-between items-start mb-3">
											<div class="flex items-center gap-3">
												<div class="p-2 bg-green-50 rounded-xl text-green-600"><ShieldCheck size={16} /></div>
												<div>
													<h4 class="font-extrabold text-[#1B4B6B] uppercase text-xs leading-tight">{dpi.tipo.replace(/_/g, ' ')}</h4>
												</div>
											</div>
											<button onclick={() => preparaEliminaDPI(dpi)} class="p-1.5 text-gray-400 hover:text-red-600 transition-all bg-gray-50 rounded-lg hover:bg-red-50"><Trash2 size={14} /></button>
										</div>
										<div class="flex items-center gap-1.5 pt-3 border-t border-gray-50 text-gray-400">
											<Calendar size={10} />
											<span class="text-[9px] font-bold uppercase">Consegnato: {new Date(dpi.dataConsegna).toLocaleDateString()}</span>
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
			<div class="bg-white rounded-3xl shadow-2xl w-full max-w-2xl overflow-hidden" in:scale>
				<div class="bg-purple-600 p-6 text-white flex justify-between items-center">
					<h2 class="text-xl font-black uppercase tracking-tighter flex items-center gap-2"><UserPlus size={20}/> Registra Nuovo Lavoratore</h2>
					<button onclick={() => (showAddModal = false)} class="hover:rotate-90 transition-transform"><X size={24}/></button>
				</div>
				<div class="p-8 grid grid-cols-1 md:grid-cols-2 gap-6">
					<div class="space-y-4">
						<label class="block text-[10px] font-bold text-[#1B4B6B] uppercase">Azienda di Appartenenza</label>
						<div class="w-full p-3 bg-gray-200 border-none rounded-xl text-sm font-bold text-gray-500 flex items-center gap-2 cursor-not-allowed">
							<Building2 size={16} /> {nomeAziendaCorrente}
						</div>

						<label class="block text-[10px] font-bold text-[#1B4B6B] uppercase">Nome *</label>
						<input bind:value={formDipendente.nome} placeholder="Es: Mario" class="w-full p-3 bg-gray-50 border-none rounded-xl text-sm focus:ring-2 focus:ring-purple-600 outline-none" />

						<label class="block text-[10px] font-bold text-[#1B4B6B] uppercase">Cognome *</label>
						<input bind:value={formDipendente.cognome} placeholder="Es: Rossi" class="w-full p-3 bg-gray-50 border-none rounded-xl text-sm focus:ring-2 focus:ring-purple-600 outline-none" />
					</div>
					<div class="space-y-4">
						<label class="block text-[10px] font-bold text-[#1B4B6B] uppercase">Email Accesso</label>
						<input bind:value={formDipendente.email} type="email" placeholder="m.rossi@email.it" class="w-full p-3 bg-gray-50 border-none rounded-xl text-sm focus:ring-2 focus:ring-purple-600 outline-none" />

						<label class="block text-[10px] font-bold text-[#1B4B6B] uppercase">Codice Fiscale *</label>
						<input bind:value={formDipendente.codiceFiscale} maxlength="16" placeholder="RSSMRA80A01H501W" class="w-full p-3 bg-gray-50 border-none rounded-xl text-sm font-mono focus:ring-2 focus:ring-purple-600 outline-none" />
					</div>
				</div>
				<div class="p-8 bg-gray-50 flex justify-end gap-4 border-t border-gray-100">
					<button onclick={() => (showAddModal = false)} class="px-6 py-3 text-[10px] font-black uppercase text-gray-400 hover:text-gray-600 transition-colors">Annulla</button>
					<button onclick={salvaDipendente} disabled={!isFormValid || isSaving} class="bg-purple-600 text-white px-8 py-3 rounded-xl text-[10px] font-black uppercase shadow-lg disabled:opacity-50 flex items-center gap-2">
						{#if isSaving}<Loader2 size={14} class="animate-spin"/>{:else}<UserPlus size={14}/>{/if} Registra Dipendente
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

	{#if showUploadModal}
		<div class="fixed inset-0 bg-[#1B4B6B]/40 backdrop-blur-sm flex items-center justify-center z-[110] p-4" transition:fade>
			<div class="bg-white rounded-3xl shadow-2xl w-full max-w-lg overflow-hidden" in:scale>
				<div class="bg-[#1B4B6B] p-6 text-white flex justify-between items-center">
					<h2 class="text-xl font-black uppercase tracking-tighter flex items-center gap-2"><FileText size={20}/> Carica Documento Lavoratore</h2>
					<button onclick={() => (showUploadModal = false)}><X size={24}/></button>
				</div>
				<div class="p-8 space-y-6">
					<div class="grid grid-cols-2 gap-4">
						<div>
							<label class="block text-[10px] font-bold text-[#1B4B6B] uppercase mb-1">Modulo</label>
							<select bind:value={formDocumento.modulo} class="w-full p-3 bg-gray-50 border-none rounded-xl text-sm font-bold uppercase">
								{#each Object.values(ModuloServizio) as mod (mod)}<option value={mod}>{mod}</option>{/each}
							</select>
						</div>
						<div>
							<label class="block text-[10px] font-bold text-[#1B4B6B] uppercase mb-1">Tipologia</label>
							<select bind:value={formDocumento.tipologia} class="w-full p-3 bg-gray-50 border-none rounded-xl text-sm font-bold uppercase">
								{#each Object.values(TipoDocumento) as tipo (tipo)}<option value={tipo}>{tipo.replace(/_/g, ' ')}</option>{/each}
							</select>
						</div>
					</div>
					<div>
						<label class="block text-[10px] font-bold text-[#1B4B6B] uppercase mb-1">Data Scadenza *</label>
						<input type="date" bind:value={formDocumento.dataScadenza} class="w-full p-3 bg-gray-50 border-none rounded-xl text-sm font-bold" />
					</div>
					<div class="border-2 border-dashed border-gray-100 rounded-3xl p-8 text-center bg-gray-50/50 group">
						<input type="file" accept=".pdf,.doc,.docx" id="fileUpload" class="hidden" onchange={(e) => uploadFile = e.currentTarget.files?.[0] || null} />
						<label for="fileUpload" class="cursor-pointer flex flex-col items-center gap-3">
							<div class="p-4 bg-white rounded-full text-[#1B4B6B] shadow-sm group-hover:scale-110 transition-transform"><Upload size={24} /></div>
							<span class="text-xs font-black text-gray-500 uppercase tracking-tighter">{uploadFile ? uploadFile.name : 'Seleziona file'}</span>
						</label>
					</div>
				</div>
				<div class="p-8 bg-gray-50 flex justify-end gap-4 border-t">
					<button onclick={() => (showUploadModal = false)} class="px-6 py-3 text-[10px] font-black uppercase text-gray-400">Annulla</button>
					<button onclick={gestisciUpload} disabled={isUploading || !uploadFile || !formDocumento.dataScadenza} class="bg-[#1B4B6B] text-white px-8 py-3 rounded-xl text-[10px] font-black uppercase shadow-lg disabled:opacity-50 flex items-center gap-2">
						{#if isUploading}<Loader2 size={14} class="animate-spin" />{/if} {isUploading ? 'Salvataggio...' : 'Conferma'}
					</button>
				</div>
			</div>
		</div>
	{/if}

	{#if showDeleteDocModal}
		<div class="fixed inset-0 bg-red-900/20 backdrop-blur-sm flex items-center justify-center z-[130] p-4" transition:fade>
			<div class="bg-white rounded-3xl shadow-2xl w-full max-w-md overflow-hidden" in:scale>
				<div class="p-8 text-center">
					<div class="w-20 h-20 bg-red-50 text-red-600 rounded-full flex items-center justify-center mx-auto mb-6"><Trash2 size={40}/></div>
					<h2 class="text-2xl font-black text-[#1B4B6B] uppercase tracking-tighter mb-2">Eliminare il documento?</h2>
					<p class="text-sm text-gray-400 mb-8">Stai per rimuovere definitivamente: <br><span class="font-bold text-[#1B4B6B]">{docDaEliminare?.tipologia.replace(/_/g, ' ')}</span></p>
					<div class="flex flex-col gap-3">
						<button onclick={confermaEliminaDoc} class="w-full bg-red-600 text-white py-4 rounded-2xl font-black uppercase text-[10px] shadow-lg shadow-red-200 transition-all hover:bg-red-700">Conferma Eliminazione</button>
						<button onclick={() => (showDeleteDocModal = false)} class="w-full py-4 text-[10px] font-black uppercase text-gray-400 hover:text-gray-600">Annulla</button>
					</div>
				</div>
			</div>
		</div>
	{/if}

	{#if showDpiModal}
		<div class="fixed inset-0 bg-[#1B4B6B]/40 backdrop-blur-sm flex items-center justify-center z-[110] p-4" transition:fade>
			<div class="bg-white rounded-3xl shadow-2xl w-full max-w-lg overflow-hidden" in:scale>
				<div class="bg-green-600 p-6 text-white flex justify-between items-center">
					<h2 class="text-xl font-black uppercase tracking-tighter flex items-center gap-2"><ShieldCheck size={20}/> Registra Consegna DPI</h2>
					<button onclick={() => (showDpiModal = false)}><X size={24}/></button>
				</div>
				<div class="p-8 space-y-6">
					<div>
						<label class="block text-[10px] font-bold text-[#1B4B6B] uppercase mb-1">Tipologia DPI *</label>
						<select bind:value={formDpi.tipo} class="w-full p-3 bg-gray-50 border-none rounded-xl text-sm font-bold uppercase">
							<option value="">Seleziona DPI...</option>
							{#each Object.values(TipoDPI) as tipoDpi (tipoDpi)}
								<option value={tipoDpi}>{tipoDpi.replace(/_/g, ' ')}</option>
							{/each}
						</select>
					</div>
					<div>
						<label class="block text-[10px] font-bold text-[#1B4B6B] uppercase mb-1">Data Consegna *</label>
						<input type="date" bind:value={formDpi.dataConsegna} class="w-full p-3 bg-gray-50 border-none rounded-xl text-sm font-bold" />
					</div>
				</div>
				<div class="p-8 bg-gray-50 flex justify-end gap-4 border-t">
					<button onclick={() => (showDpiModal = false)} class="px-6 py-3 text-[10px] font-black uppercase text-gray-400">Annulla</button>
					<button onclick={salvaDPI} disabled={isSavingDpi || !formDpi.tipo || !formDpi.dataConsegna} class="bg-green-600 text-white px-8 py-3 rounded-xl text-[10px] font-black uppercase shadow-lg disabled:opacity-50 flex items-center gap-2">
						{#if isSavingDpi}<Loader2 size={14} class="animate-spin" />{/if} {isSavingDpi ? 'Salvataggio...' : 'Conferma'}
					</button>
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