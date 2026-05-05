<script lang="ts">
	import { onMount } from 'svelte';
	import { fade, scale, slide } from 'svelte/transition';
	import { goto } from '$app/navigation';
	import { base } from '$app/paths';
	import {
		Building2, Plus, Trash2, ShieldCheck, ChevronRight, ChevronLeft,
		Loader2, Search, Phone, User, Globe, Users, UserCheck, MapPin,
		FileText, Download, Calendar, X, AlertTriangle, Upload, UserPlus,
		Mail, MessageSquare
	} from 'lucide-svelte';

	import { Azienda, type AziendaData } from '$lib/models/Azienda';
	import { Documento } from '$lib/models/Documento';
	import { AnagraficaService, type AuthRequestDTO } from '$lib/services/AnagraficaService';
	import { LavoratoreService, type DipendenteDTO, type DipendenteRequest } from '$lib/services/LavoratoreService';
	import { DocumentoService } from '$lib/services/DocumentoService';
	import { ModuloServizio, TipoDocumento } from '$lib/models/Enums';

	let aziende = $state<Azienda[]>([]);
	let dipendentiCorrenti = $state<DipendenteDTO[]>([]);
	let documentiCorrenti = $state<Documento[]>([]);
	let isLoading = $state(true);
	let searchQuery = $state('');

	let selectedAzienda = $state<Azienda | null>(null);
	let dynamicHasDipendenti = $state(false);
	let showModal = $state(false);
	let isSaving = $state(false);

	let showUploadModal = $state(false);
	let isUploading = $state(false);
	let uploadFile = $state<File | null>(null);
	let formDocumento = $state({ modulo: ModuloServizio.SICUREZZA, tipologia: TipoDocumento.DVR, dataRilascio: '', dataScadenza: '' });
	let idDocumentoDaAggiornare = $state<number | null>(null);

	let showDipendenteModal = $state(false);
	let isSavingDipendente = $state(false);
	let formDipendente = $state({ nome: '', cognome: '', codiceFiscale: '', email: '', password: '' });

	let showDeleteDocModal = $state(false);
	let docDaEliminare = $state<Documento | null>(null);
	let showDeleteDipModal = $state(false);
	let dipDaEliminare = $state<DipendenteDTO | null>(null);

	let formAzienda = $state({ email: '', password: '', ragioneSociale: '', partitaIva: '', sedeLegale: '', pec: '', telefono: '', cellulare: '', referenteAziendale: '', hasDipendenti: false });
	let showDeleteModal = $state(false);
	let aziendaDaEliminare = $state<Azienda | null>(null);
	let confermaTesto = $state('');

	const isFormValid = $derived(formAzienda.ragioneSociale.trim() !== '' && formAzienda.partitaIva.length === 11 && formAzienda.email.trim() !== '' && formAzienda.password.trim() !== '');

	const isDipendenteValid = $derived(
			formDipendente.nome.trim() !== '' &&
			formDipendente.cognome.trim() !== '' &&
			formDipendente.codiceFiscale.length === 16 &&
			formDipendente.email.trim() !== '' &&
			formDipendente.password.trim() !== ''
	);

	const filteredAziende = $derived(aziende.filter(a => a.ragioneSociale.toLowerCase().includes(searchQuery.toLowerCase())));
	const isConfermaValida = $derived(confermaTesto.trim().toUpperCase() === 'ELIMINA');

	const documentiAziendaliFiltrati = $derived(documentiCorrenti.filter(doc => doc.tipologia !== 'ATTESTATO_CORSO'));

	onMount(async () => {
		try {
			const res = await AnagraficaService.getAllAziende();
			const data = res as AziendaData[];
			aziende = data.map(item => new Azienda(item));
			await Promise.all(aziende.map(async (a, i) => {
				aziende[i].hasDipendenti = await AnagraficaService.hasDipendenti(a.idUtente);
			}));
		} catch { console.error("Errore onMount"); } finally { isLoading = false; }
	});

	async function apriDettaglio(aziendaPreview: Azienda) {
		selectedAzienda = aziendaPreview;
		try {
			const fullData = await AnagraficaService.getAziendaById(aziendaPreview.idUtente);
			selectedAzienda = new Azienda(fullData);
			documentiCorrenti = await DocumentoService.getDocumentiByAzienda(aziendaPreview.idUtente);
			const hasDip = await AnagraficaService.hasDipendenti(aziendaPreview.idUtente);
			dynamicHasDipendenti = hasDip;
			const idx = aziende.findIndex(a => a.idUtente === aziendaPreview.idUtente);
			if (idx !== -1) aziende[idx].hasDipendenti = hasDip;
			dipendentiCorrenti = await LavoratoreService.getByAzienda(aziendaPreview.idUtente);
		} catch { console.error("Errore dettaglio"); }
	}

	async function vaiADettaglioDipendente(idUtente: string | number) {
		return await goto(`${base}/dashboard/admin/dipendenti?id=${idUtente}`);
	}

	function apreGmail(email: string) {
		if (!email) return;
		window.open(`https://mail.google.com/mail/?view=cm&fs=1&to=${email}`, '_blank');
	}

	async function vaiInChat(idUtente: string | number | undefined) {
		if (!idUtente) return;
		return await goto(`${base}/dashboard/admin/comunicazioni?chatId=${idUtente}`);
	}

	function preparaEliminaDoc(doc: Documento) { docDaEliminare = doc; showDeleteDocModal = true; }

	async function confermaEliminaDoc() {
		if (!docDaEliminare) return;
		try {
			await DocumentoService.deleteDocumento(docDaEliminare.idDocumento);
			documentiCorrenti = documentiCorrenti.filter(d => d.idDocumento !== docDaEliminare?.idDocumento);
			showDeleteDocModal = false; docDaEliminare = null;
		} catch { alert("Errore eliminazione documento."); }
	}

	function preparaEliminaDip(dip: DipendenteDTO) { dipDaEliminare = dip; showDeleteDipModal = true; }

	async function confermaEliminaDip() {
		if (!dipDaEliminare || !selectedAzienda) return;
		try {
			await LavoratoreService.delete(dipDaEliminare.idUtente);
			dipendentiCorrenti = dipendentiCorrenti.filter(d => d.idUtente !== dipDaEliminare?.idUtente);
			if (dipendentiCorrenti.length === 0) {
				dynamicHasDipendenti = false;
				const idx = aziende.findIndex(a => a.idUtente === selectedAzienda?.idUtente);
				if (idx !== -1) aziende[idx].hasDipendenti = false;
			}
			showDeleteDipModal = false; dipDaEliminare = null;
		} catch { alert("Errore eliminazione dipendente."); }
	}

	async function salvaNuovoDipendente() {
		if (!isDipendenteValid || !selectedAzienda) return;
		isSavingDipendente = true;
		try {
			const idAz = selectedAzienda.idUtente;

			const payload = {
				nome: formDipendente.nome,
				cognome: formDipendente.cognome,
				codiceFiscale: formDipendente.codiceFiscale,
				email: formDipendente.email.trim(),
				passwordHash: formDipendente.password,
				richiedeCambioPassword: true
			} as unknown as DipendenteRequest;

			const nuovo = await LavoratoreService.create(idAz, payload);
			dipendentiCorrenti = [...dipendentiCorrenti, nuovo];

			if (!dynamicHasDipendenti) {
				dynamicHasDipendenti = true;
				const idx = aziende.findIndex(a => a.idUtente === idAz);
				if (idx !== -1) aziende[idx].hasDipendenti = true;
			}

			showDipendenteModal = false;
			formDipendente = { nome: '', cognome: '', codiceFiscale: '', email: '', password: '' };
		} catch (error) {
			console.error("Errore Creazione Dipendente:", error);
			alert("Ops! Creazione fallita. Assicurati che l'email o il Codice Fiscale non siano già registrati a sistema.");
		} finally {
			isSavingDipendente = false;
		}
	}

	function apriModalUploadNuovo() {
		idDocumentoDaAggiornare = null;
		formDocumento = { modulo: ModuloServizio.SICUREZZA, tipologia: TipoDocumento.DVR, dataRilascio: '', dataScadenza: '' };
		showUploadModal = true;
	}

	function preparaAggiornamento(doc: Documento) {
		idDocumentoDaAggiornare = doc.idDocumento;
		formDocumento.modulo = doc.modulo;
		formDocumento.tipologia = doc.tipologia;
		formDocumento.dataRilascio = '';
		formDocumento.dataScadenza = '';
		showUploadModal = true;
	}

	async function gestisciUpload() {
		if (!uploadFile || !selectedAzienda || !formDocumento.dataScadenza || !formDocumento.dataRilascio) return;
		isUploading = true;
		try {
			const fd = new FormData();
			fd.append('file', uploadFile);
			fd.append('modulo', formDocumento.modulo);
			fd.append('tipologia', formDocumento.tipologia);
			fd.append('dataScadenzaStr', formDocumento.dataScadenza);
			fd.append('dataRilascioStr', formDocumento.dataRilascio);

			await DocumentoService.uploadDocumento(selectedAzienda.idUtente, fd);

			if (idDocumentoDaAggiornare !== null) {
				await DocumentoService.deleteDocumento(idDocumentoDaAggiornare);
			}

			documentiCorrenti = await DocumentoService.getDocumentiByAzienda(selectedAzienda.idUtente);

			showUploadModal = false;
			uploadFile = null;
			idDocumentoDaAggiornare = null;
		} catch (error) {
			console.error("Errore Dettagliato Upload:", error);
			const err = error as { response?: { data?: Record<string, unknown> | string }, message?: string };
			const data = err.response?.data;
			const messaggioServer = (typeof data === 'string' ? data : data?.message) || err.message || "Errore sconosciuto";
			alert("Ops! L'upload è fallito. Il server dice: " + JSON.stringify(messaggioServer));
		} finally {
			isUploading = false;
		}
	}

	async function scaricaDoc(doc: Documento) {
		try {
			const b = await DocumentoService.downloadDocumento(doc.idDocumento);
			const u = URL.createObjectURL(b);
			const a = document.createElement('a');
			a.href = u;
			a.download = doc.filePath.split('/').pop() || 'doc.pdf';
			a.click();
			URL.revokeObjectURL(u);
		} catch { alert("Errore download."); }
	}

	async function salvaNuovaAzienda() {
		if (!isFormValid) return;
		isSaving = true;
		try {
			const payload: AuthRequestDTO & { sedeLegale?: string; pec?: string; telefono?: string; cellulare?: string; referenteAziendale?: string; hasDipendenti?: boolean; } = { ...formAzienda, ruolo: 'AZIENDA' };
			await AnagraficaService.registraUtente(payload);
			const res = await AnagraficaService.getAllAziende();
			aziende = (res as AziendaData[]).map(item => new Azienda(item));
			showModal = false;
			formAzienda = { email: '', password: '', ragioneSociale: '', partitaIva: '', sedeLegale: '', pec: '', telefono: '', cellulare: '', referenteAziendale: '', hasDipendenti: false };
		} catch { alert("Errore creazione."); } finally { isSaving = false; }
	}

	function preparaEliminazione(a: Azienda | null) { if (!a) return; aziendaDaEliminare = a; confermaTesto = ''; showDeleteModal = true; }

	async function confermaEliminazione() {
		if (isConfermaValida && aziendaDaEliminare) {
			try {
				await AnagraficaService.deleteAzienda(aziendaDaEliminare.idUtente);
				aziende = aziende.filter(a => a.idUtente !== aziendaDaEliminare?.idUtente);
				showDeleteModal = false; selectedAzienda = null;
			} catch { alert("Errore eliminazione azienda."); }
		}
	}
</script>

<div in:fade class="max-w-7xl mx-auto p-6">
	{#if !selectedAzienda}
		<div class="mb-10 flex justify-between items-start">
			<div>
				<h1 class="text-4xl font-extrabold text-[#1B4B6B] uppercase tracking-tighter">Anagrafiche Aziende</h1>
				<p class="text-gray-500 font-bold uppercase text-xs tracking-tighter">Gestione centralizzata NorLan.</p>
			</div>
			<button onclick={() => (showModal = true)} class="bg-white text-[#1B4B6B] border-2 border-[#1B4B6B] px-8 py-3.5 rounded-xl font-extrabold uppercase text-xs shadow-lg hover:bg-[#1B4B6B] hover:text-white transition-all flex items-center gap-3">
				<Plus size={18} /> Nuova Azienda
			</button>
		</div>

		<div class="mb-8 relative w-72 group">
			<Search class="absolute left-3 top-1/2 -translate-y-1/2 text-gray-400 group-focus-within:text-[#1B4B6B] transition-colors" size={16} />
			<input bind:value={searchQuery} type="text" placeholder="Filtra azienda..." class="w-full pl-10 pr-4 py-2.5 bg-white border border-gray-200 rounded-xl text-xs focus:ring-2 focus:ring-[#1B4B6B] outline-none transition-all font-bold uppercase" />
		</div>

		{#if isLoading}
			<div class="py-20 text-center"><Loader2 size={40} class="animate-spin mx-auto text-[#1B4B6B]" /></div>
		{:else}
			<div class="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-8">
				{#each filteredAziende as a (a.idUtente)}
					<div class="bg-white rounded-2xl shadow-sm border border-gray-100 hover:shadow-xl hover:border-[#1B4B6B]/20 hover:-translate-y-1 transition-all group relative flex flex-col h-full overflow-hidden" in:scale>
						<div role="button" tabindex="0" onclick={() => apriDettaglio(a)} onkeydown={(e) => e.key === 'Enter' && apriDettaglio(a)} class="p-8 pb-4 cursor-pointer flex-1">
							<div class="flex justify-between items-start mb-6">
								<div class="p-4 bg-gray-50 rounded-2xl text-[#1B4B6B] group-hover:bg-[#1B4B6B] group-hover:text-white transition-all"><Building2 size={28} /></div>
								<button onclick={(e) => { e.stopPropagation(); preparaEliminazione(a); }} class="text-gray-300 hover:text-red-600 transition-colors p-2 rounded-lg hover:bg-red-50 z-10"><Trash2 size={20} /></button>
							</div>
							<h3 class="font-extrabold text-[#1B4B6B] text-xl mb-1 uppercase truncate">{a.ragioneSociale}</h3>
							<div class="flex items-center gap-2 mb-4">
								<p class="text-[10px] text-gray-400 font-bold uppercase">P.IVA: {a.partitaIva}</p>
								<span class="text-[8px] font-black px-2 py-0.5 rounded border border-[#1B4B6B]/20 bg-[#1B4B6B]/5 text-[#1B4B6B] uppercase">
                            {a.hasDipendenti ? 'Con Personale' : 'Individuale'}
                         </span>
							</div>
						</div>
						<button onclick={() => apriDettaglio(a)} class="mt-auto w-full p-6 pt-4 border-t border-gray-50 flex justify-between items-center hover:bg-gray-50/50 transition-colors">
							<div class="flex items-center gap-2"><ShieldCheck size={16} class="text-[#1B4B6B]"/><span class="text-[10px] font-bold text-gray-400 uppercase italic">Verificata</span></div>
							<ChevronRight size={20} class="text-[#1B4B6B]" />
						</button>
					</div>
				{/each}
			</div>
		{/if}

	{:else}
		<div in:fade>
			<button onclick={() => (selectedAzienda = null)} class="flex items-center gap-2 text-[#1B4B6B] font-extrabold uppercase text-[10px] mb-8 hover:gap-3 transition-all"><ChevronLeft size={16} /> Torna all'elenco</button>

			<div class="bg-white rounded-3xl shadow-xl border border-gray-100 overflow-hidden mb-12">
				<div class="bg-[#1B4B6B] p-10 text-white flex justify-between items-end relative">
					<div>
						<div class="flex items-center gap-3 mb-4">
							{#if dynamicHasDipendenti}
								<span class="bg-white/20 border border-white/20 text-white text-[10px] font-black px-4 py-1.5 rounded-full uppercase flex items-center gap-2"><Users size={12}/> Azienda con Personale</span>
							{:else}
								<span class="bg-white/20 border border-white/20 text-white text-[10px] font-black px-4 py-1.5 rounded-full uppercase flex items-center gap-2"><User size={12}/> Ditta Individuale</span>
							{/if}
						</div>
						<h1 class="text-5xl font-extrabold uppercase tracking-tighter">{selectedAzienda.ragioneSociale}</h1>
					</div>

					<div class="flex items-center gap-3">
						<button onclick={() => apreGmail(selectedAzienda?.email || '')} class="flex items-center gap-2 bg-white text-[#1B4B6B] px-6 py-3.5 rounded-2xl transition-all font-extrabold uppercase text-[10px] shadow-xl hover:bg-gray-100">
							<Mail size={16} /> Manda Mail
						</button>
						<button onclick={() => vaiInChat(selectedAzienda?.idUtente || '')} class="flex items-center gap-2 bg-white/20 border border-white/20 text-white px-6 py-3.5 rounded-2xl transition-all font-extrabold uppercase text-[10px] shadow-xl hover:bg-white/30">
							<MessageSquare size={16} /> Contatta Azienda
						</button>
						<button onclick={() => preparaEliminazione(selectedAzienda)} class="flex items-center gap-2 bg-red-600 text-white px-6 py-3.5 rounded-2xl transition-all font-extrabold uppercase text-[10px] border border-red-500/20 shadow-xl hover:bg-red-700">
							<Trash2 size={16} /> Elimina
						</button>
					</div>
				</div>

				<div class="p-12 grid grid-cols-1 lg:grid-cols-3 gap-16 bg-gray-50/30">
					<div class="space-y-8">
						<h2 class="text-[#1B4B6B] font-black uppercase text-xs tracking-widest border-b border-gray-200 pb-4 flex items-center gap-2"><Globe size={16} /> Profilo Legale</h2>
						<div class="space-y-6">
							<div><p class="text-[10px] font-bold text-gray-400 uppercase mb-1">Partita IVA</p><p class="text-base font-extrabold text-[#1B4B6B] tracking-widest">{selectedAzienda.partitaIva}</p></div>
							<div><p class="text-[10px] font-bold text-gray-400 uppercase mb-1 flex items-center gap-1"><MapPin size={10}/> Sede Legale</p><p class="text-sm font-bold text-[#1B4B6B] uppercase leading-relaxed">{selectedAzienda.sedeLegale || 'N.D.'}</p></div>
						</div>
					</div>
					<div class="space-y-8">
						<h2 class="text-[#1B4B6B] font-black uppercase text-xs tracking-widest border-b border-gray-200 pb-4 flex items-center gap-2"><Phone size={16} /> Recapiti</h2>
						<div class="space-y-6">
							<div><p class="text-[10px] font-bold text-gray-400 uppercase mb-1">Email Accesso</p><p class="text-sm font-bold text-[#1B4B6B] lowercase">{selectedAzienda.email}</p></div>
							<div><p class="text-[10px] font-bold text-gray-400 uppercase mb-1">PEC Certificata</p><p class="text-sm font-bold text-[#1B4B6B] lowercase">{selectedAzienda.pec || '-'}</p></div>
							<div class="grid grid-cols-2 gap-4">
								<div><p class="text-[10px] font-bold text-gray-400 uppercase mb-1">Telefono</p><p class="text-sm font-bold text-[#1B4B6B]">{selectedAzienda.telefono || '-'}</p></div>
								<div><p class="text-[10px] font-bold text-gray-400 uppercase mb-1">Cellulare</p><p class="text-sm font-bold text-[#1B4B6B]">{selectedAzienda.cellulare || '-'}</p></div>
							</div>
						</div>
					</div>
					<div class="space-y-8">
						<h2 class="text-[#1B4B6B] font-black uppercase text-xs tracking-widest border-b border-gray-200 pb-4 flex items-center gap-2"><UserCheck size={16} /> Responsabile</h2>
						<div class="space-y-6">
							<div><p class="text-[10px] font-bold text-gray-400 uppercase mb-1">Referente Aziendale</p><p class="text-sm font-extrabold text-[#1B4B6B] uppercase">{selectedAzienda.referenteAziendale || 'Non assegnato'}</p></div>
						</div>
					</div>
				</div>
			</div>

			<div in:slide class="space-y-8 mb-16">
				<div class="flex items-center justify-between">
					<div class="flex items-center gap-4">
						<div class="p-3 bg-[#1B4B6B]/10 text-[#1B4B6B] rounded-2xl shadow-inner"><FileText size={24} /></div>
						<h2 class="text-2xl font-black text-[#1B4B6B] uppercase tracking-tighter">Documentazione ({documentiAziendaliFiltrati.length})</h2>
					</div>
					<button onclick={apriModalUploadNuovo} class="bg-white text-[#1B4B6B] border-2 border-[#1B4B6B] px-6 py-2.5 rounded-xl font-extrabold uppercase text-[10px] flex items-center gap-2 hover:bg-[#1B4B6B] hover:text-white transition-all shadow-md">
						<Plus size={16} /> Carica Documento
					</button>
				</div>

				{#if documentiAziendaliFiltrati.length > 0}
					<div class="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6">
						{#each documentiAziendaliFiltrati as doc (doc.idDocumento)}
							<div class="bg-white p-6 rounded-3xl border transition-all group relative overflow-hidden
                            {doc.scaduto ? 'border-red-200 shadow-md shadow-red-50' : 'border-gray-100 shadow-sm hover:shadow-xl'}">

								{#if doc.scaduto}
									<div class="absolute top-0 left-0 w-full h-1.5 bg-red-500"></div>
								{/if}

								<div class="flex justify-between items-start mb-4">
									<div class="flex items-center gap-4">
										<div class="p-3 rounded-2xl transition-all
                                     {doc.scaduto ? 'bg-red-50 text-red-600' : 'bg-[#1B4B6B]/10 text-[#1B4B6B] group-hover:bg-[#1B4B6B] group-hover:text-white'}">
											<FileText size={20} />
										</div>
										<div>
											<h4 class="font-extrabold text-[#1B4B6B] uppercase text-sm leading-tight">{doc.tipologia.replace(/_/g, ' ')}</h4>
											<p class="text-[9px] text-gray-400 font-bold uppercase">{doc.modulo}</p>
										</div>
									</div>
									<div class="flex gap-2">
										<button onclick={() => scaricaDoc(doc)} class="p-2 text-gray-400 hover:text-[#1B4B6B] transition-all" title="Scarica"><Download size={18} /></button>
										<button onclick={() => preparaEliminaDoc(doc)} class="p-2 text-gray-400 hover:text-red-600 transition-all" title="Elimina"><Trash2 size={18} /></button>
									</div>
								</div>
								<div class="mt-4 pt-4 border-t border-gray-50 flex items-center justify-between">
									<div class="flex items-center gap-2 {doc.scaduto ? 'text-red-500 font-black' : 'text-gray-400'}">
										<Calendar size={12} />
										<span class="text-[9px] font-bold uppercase italic">Scadenza: {new Date(doc.dataScadenza).toLocaleDateString()}</span>
									</div>

									{#if doc.scaduto}
										<button onclick={() => preparaAggiornamento(doc)} class="text-[8px] font-black px-4 py-1.5 bg-red-600 text-white rounded-lg uppercase shadow-md hover:bg-red-700 transition-all hover:scale-105">Aggiorna Ora</button>
									{:else}
										<span class="text-[8px] font-black px-2 py-1 bg-green-50 text-green-600 border border-green-100 rounded-lg uppercase">Valido</span>
									{/if}
								</div>
							</div>
						{/each}
					</div>
				{:else}
					<div class="bg-white rounded-3xl p-10 text-center border-2 border-dashed border-gray-200 text-gray-300 uppercase font-bold text-xs italic">Nessun documento caricato</div>
				{/if}
			</div>

			<div in:slide class="space-y-8 mb-20">
				<div class="flex items-center justify-between">
					<div class="flex items-center gap-4">
						<div class="p-3 bg-[#1B4B6B]/10 text-[#1B4B6B] rounded-2xl shadow-inner"><Users size={24} /></div>
						<h2 class="text-2xl font-black text-[#1B4B6B] uppercase tracking-tighter">Personale ({dipendentiCorrenti.length})</h2>
					</div>
					<button onclick={() => (showDipendenteModal = true)} class="bg-white text-[#1B4B6B] border-2 border-[#1B4B6B] px-6 py-2.5 rounded-xl font-extrabold uppercase text-[10px] flex items-center gap-2 hover:bg-[#1B4B6B] hover:text-white transition-all shadow-md">
						<UserPlus size={16} /> Aggiungi Dipendente
					</button>
				</div>

				{#if dipendentiCorrenti.length > 0}
					<div class="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-6">
						{#each dipendentiCorrenti as d (d.idUtente)}
							<div
									role="button"
									tabindex="0"
									onclick={() => vaiADettaglioDipendente(d.idUtente)}
									onkeydown={(e) => e.key === 'Enter' && vaiADettaglioDipendente(d.idUtente)}
									class="bg-white p-6 rounded-3xl border border-gray-100 shadow-sm hover:shadow-xl transition-all relative group cursor-pointer hover:border-[#1B4B6B]/30"
							>
								<button onclick={(e) => { e.stopPropagation(); preparaEliminaDip(d); }} class="absolute top-4 right-4 p-2 text-gray-300 hover:text-red-600 opacity-0 group-hover:opacity-100 transition-all z-10" title="Elimina"><Trash2 size={16} /></button>
								<div class="flex items-center gap-4 mb-4">
									<div class="w-12 h-12 bg-gray-50 rounded-2xl flex items-center justify-center text-[#1B4B6B] font-black text-sm group-hover:bg-[#1B4B6B] group-hover:text-white transition-all">{d.nome[0]}{d.cognome[0]}</div>
									<h4 class="font-extrabold text-[#1B4B6B] uppercase text-sm leading-tight">{d.nome}<br>{d.cognome}</h4>
								</div>
								<div class="space-y-2 pt-4 border-t border-gray-50">
									<p class="text-[8px] text-gray-300 font-black uppercase">Codice Fiscale</p>
									<p class="text-[10px] font-mono font-bold text-gray-600">{d.codiceFiscale}</p>
								</div>
								<div class="mt-4 pt-2 flex justify-between items-center opacity-0 group-hover:opacity-100 transition-opacity">
									<span class="text-[9px] font-black text-[#1B4B6B] uppercase tracking-tighter">Vedi Profilo</span>
									<ChevronRight size={14} class="text-[#1B4B6B]" />
								</div>
							</div>
						{/each}
					</div>
				{:else}
					<div class="bg-white rounded-3xl p-10 text-center border-2 border-dashed border-gray-200 text-gray-300 uppercase font-bold text-xs italic">Nessun dipendente registrato</div>
				{/if}
			</div>
		</div>
	{/if}

	{#if showDipendenteModal}
		<div class="fixed inset-0 bg-[#1B4B6B]/40 backdrop-blur-sm flex items-center justify-center z-[120] p-4" transition:fade>
			<div class="bg-white rounded-3xl shadow-2xl w-full max-w-md overflow-hidden" in:scale>
				<div class="bg-[#1B4B6B] p-6 text-white flex justify-between items-center">
					<h2 class="text-xl font-black uppercase tracking-tighter flex items-center gap-2"><UserPlus size={20}/> Registra Dipendente</h2>
					<button onclick={() => (showDipendenteModal = false)} class="hover:text-red-400 hover:rotate-90 transition-all duration-300"><X size={24}/></button>
				</div>
				<div class="p-8 space-y-4">
					<div><label class="block text-[10px] font-bold text-[#1B4B6B] uppercase mb-1">Nome *</label><input bind:value={formDipendente.nome} class="w-full p-3 bg-gray-50 border-none rounded-xl text-sm focus:ring-[#1B4B6B]" /></div>
					<div><label class="block text-[10px] font-bold text-[#1B4B6B] uppercase mb-1">Cognome *</label><input bind:value={formDipendente.cognome} class="w-full p-3 bg-gray-50 border-none rounded-xl text-sm focus:ring-[#1B4B6B]" /></div>
					<div><label class="block text-[10px] font-bold text-[#1B4B6B] uppercase mb-1">Codice Fiscale *</label><input bind:value={formDipendente.codiceFiscale} maxlength="16" class="w-full p-3 bg-gray-50 border-none rounded-xl text-sm font-mono focus:ring-[#1B4B6B]" /></div>

					<div><label class="block text-[10px] font-bold text-[#1B4B6B] uppercase mb-1">Email Accesso *</label><input bind:value={formDipendente.email} type="email" placeholder="m.rossi@email.it" class="w-full p-3 bg-gray-50 border-none rounded-xl text-sm focus:ring-[#1B4B6B]" /></div>

					<div>
						<label class="block text-[10px] font-bold text-[#1B4B6B] uppercase mb-1">Password Temporanea *</label>
						<input bind:value={formDipendente.password} type="password" placeholder="••••••••" class="w-full p-3 bg-gray-50 border-none rounded-xl text-sm focus:ring-[#1B4B6B]" />
					</div>
				</div>
				<div class="p-8 bg-gray-50 flex justify-end gap-4 border-t">
					<button onclick={() => (showDipendenteModal = false)} class="px-6 py-3 text-[10px] font-black uppercase text-gray-400 hover:text-gray-600 transition-colors">Annulla</button>
					<button onclick={salvaNuovoDipendente} disabled={!isDipendenteValid || isSavingDipendente} class="bg-[#1B4B6B] text-white px-8 py-3 rounded-xl text-[10px] font-black uppercase shadow-lg disabled:opacity-50 flex items-center gap-2 hover:bg-[#153a54] transition-colors">
						{#if isSavingDipendente}<Loader2 size={14} class="animate-spin" />{/if} {isSavingDipendente ? 'Salvataggio...' : 'Conferma e Salva'}
					</button>
				</div>
			</div>
		</div>
	{/if}

	{#if showUploadModal}
		<div class="fixed inset-0 bg-[#1B4B6B]/40 backdrop-blur-sm flex items-center justify-center z-[110] p-4" transition:fade>
			<div class="bg-white rounded-3xl shadow-2xl w-full max-w-lg overflow-hidden" in:scale>
				<div class="bg-[#1B4B6B] p-6 text-white flex justify-between items-center">
					<h2 class="text-xl font-black uppercase tracking-tighter flex items-center gap-2"><FileText size={20}/> Carica Documento</h2>
					<button onclick={() => (showUploadModal = false)} class="hover:text-red-400 hover:rotate-90 transition-all duration-300"><X size={24}/></button>
				</div>
				<div class="p-8 space-y-6">
					<div class="grid grid-cols-2 gap-4">
						<div><label class="block text-[10px] font-bold text-[#1B4B6B] uppercase mb-1">Modulo</label><select bind:value={formDocumento.modulo} class="w-full p-3 bg-gray-50 border-none rounded-xl text-sm font-bold uppercase focus:ring-[#1B4B6B]">{#each Object.values(ModuloServizio) as mod (mod)}<option value={mod}>{mod}</option>{/each}</select></div>
						<div><label class="block text-[10px] font-bold text-[#1B4B6B] uppercase mb-1">Tipologia</label><select bind:value={formDocumento.tipologia} class="w-full p-3 bg-gray-50 border-none rounded-xl text-sm font-bold uppercase focus:ring-[#1B4B6B]">{#each Object.values(TipoDocumento) as tipo (tipo)}<option value={tipo}>{tipo.replace(/_/g, ' ')}</option>{/each}</select></div>
					</div>
					<div class="grid grid-cols-2 gap-4">
						<div>
							<label class="block text-[10px] font-bold text-[#1B4B6B] uppercase mb-1">Data di Rilascio *</label>
							<input
									type="date"
									max="9999-12-31"
									bind:value={formDocumento.dataRilascio}
									class="w-full p-3 bg-gray-50 border-none rounded-xl text-sm font-bold focus:ring-2 focus:ring-[#1B4B6B] outline-none"
							/>
						</div>
						<div>
							<label class="block text-[10px] font-bold text-[#1B4B6B] uppercase mb-1">Data Scadenza *</label>
							<input
									type="date"
									max="9999-12-31"
									bind:value={formDocumento.dataScadenza}
									class="w-full p-3 bg-gray-50 border-none rounded-xl text-sm font-bold focus:ring-2 focus:ring-[#1B4B6B] outline-none"
							/>
						</div>
					</div>
					<div class="border-2 border-dashed border-gray-100 rounded-3xl p-8 text-center bg-gray-50/50 group"><input type="file" accept=".pdf,.doc,.docx" id="fileUpload" class="hidden" onchange={(e) => uploadFile = e.currentTarget.files?.[0] || null} /><label for="fileUpload" class="cursor-pointer flex flex-col items-center gap-3"><div class="p-4 bg-white rounded-full text-[#1B4B6B] shadow-sm group-hover:scale-110 transition-transform"><Upload size={24} /></div><span class="text-xs font-black text-gray-500 uppercase tracking-tighter">{uploadFile ? uploadFile.name : 'Seleziona file'}</span></label></div>
				</div>
				<div class="p-8 bg-gray-50 flex justify-end gap-4 border-t"><button onclick={() => (showUploadModal = false)} class="px-6 py-3 text-[10px] font-black uppercase text-gray-400">Annulla</button><button onclick={gestisciUpload} disabled={isUploading || !uploadFile || !formDocumento.dataScadenza || !formDocumento.dataRilascio} class="bg-[#1B4B6B] text-white px-8 py-3 rounded-xl text-[10px] font-black uppercase shadow-lg disabled:opacity-50 flex items-center gap-2 hover:bg-[#153a54]">{#if isUploading}<Loader2 size={14} class="animate-spin" />{/if} {isUploading ? 'Salvataggio...' : 'Conferma'}</button></div>
			</div>
		</div>
	{/if}

	{#if showModal}
		<div class="fixed inset-0 bg-[#1B4B6B]/40 backdrop-blur-sm flex items-center justify-center z-[100] p-4" transition:fade>
			<div class="bg-white rounded-3xl shadow-2xl w-full max-w-2xl overflow-hidden" in:scale>
				<div class="bg-[#1B4B6B] p-6 text-white flex justify-between items-center">
					<h2 class="text-xl font-black uppercase tracking-tighter flex items-center gap-2"><Building2 size={20}/> Nuova Anagrafica Aziendale</h2>
					<button onclick={() => (showModal = false)} class="hover:text-red-400 hover:rotate-90 transition-all duration-300"><X size={24}/></button>
				</div>
				<div class="p-8 max-h-[80vh] overflow-y-auto">
					<div class="grid grid-cols-1 md:grid-cols-2 gap-x-8 gap-y-6">
						<div class="col-span-full mb-2">
							<label class="block text-[10px] font-bold text-[#1B4B6B] uppercase mb-3">Tipo di Organizzazione *</label>
							<div class="grid grid-cols-2 gap-4">
								<button type="button" onclick={() => formAzienda.hasDipendenti = false} class="flex items-center justify-center gap-3 p-4 rounded-2xl border-2 transition-all {!formAzienda.hasDipendenti ? 'border-[#1B4B6B] bg-[#1B4B6B]/5' : 'border-gray-100 hover:border-gray-200'}">
									<User size={20} class={!formAzienda.hasDipendenti ? 'text-[#1B4B6B]' : 'text-gray-400'} />
									<span class="text-[10px] font-black uppercase {!formAzienda.hasDipendenti ? 'text-[#1B4B6B]' : 'text-gray-400'}">Ditta Individuale</span>
								</button>
								<button type="button" onclick={() => formAzienda.hasDipendenti = true} class="flex items-center justify-center gap-3 p-4 rounded-2xl border-2 transition-all {formAzienda.hasDipendenti ? 'border-[#1B4B6B] bg-[#1B4B6B]/5' : 'border-gray-100 hover:border-gray-200'}">
									<Users size={20} class={formAzienda.hasDipendenti ? 'text-[#1B4B6B]' : 'text-gray-400'} />
									<span class="text-[10px] font-black uppercase {formAzienda.hasDipendenti ? 'text-[#1B4B6B]' : 'text-gray-400'}">Azienda con Personale</span>
								</button>
							</div>
						</div>
						<div class="space-y-4">
							<div><label class="block text-[10px] font-bold text-[#1B4B6B] uppercase mb-1">Ragione Sociale *</label><input bind:value={formAzienda.ragioneSociale} placeholder="Es: Norlan Srl" class="w-full p-3 bg-gray-50 border-none rounded-xl text-sm focus:ring-[#1B4B6B]" /></div>
							<div><label class="block text-[10px] font-bold text-[#1B4B6B] uppercase mb-1">Partita IVA (11 cifre) *</label><input bind:value={formAzienda.partitaIva} maxlength="11" placeholder="01234567890" class="w-full p-3 bg-gray-50 border-none rounded-xl text-sm focus:ring-[#1B4B6B]" /></div>
							<div><label class="block text-[10px] font-bold text-[#1B4B6B] uppercase mb-1">PEC Certificata</label><input bind:value={formAzienda.pec} placeholder="azienda@pec.it" class="w-full p-3 bg-gray-50 border-none rounded-xl text-sm focus:ring-[#1B4B6B]" /></div>
							<div><label class="block text-[10px] font-bold text-[#1B4B6B] uppercase mb-1">Sede Legale</label><input bind:value={formAzienda.sedeLegale} placeholder="Indirizzo completo" class="w-full p-3 bg-gray-50 border-none rounded-xl text-sm focus:ring-[#1B4B6B]" /></div>
						</div>
						<div class="space-y-4">
							<div><label class="block text-[10px] font-bold text-[#1B4B6B] uppercase mb-1">Email Accesso *</label><input bind:value={formAzienda.email} type="email" placeholder="admin@azienda.it" class="w-full p-3 bg-gray-50 border-none rounded-xl text-sm focus:ring-[#1B4B6B]" /></div>
							<div><label class="block text-[10px] font-bold text-[#1B4B6B] uppercase mb-1">Password *</label><input bind:value={formAzienda.password} type="password" placeholder="••••••••" class="w-full p-3 bg-gray-50 border-none rounded-xl text-sm focus:ring-[#1B4B6B]" /></div>
							<div><label class="block text-[10px] font-bold text-[#1B4B6B] uppercase mb-1">Referente Aziendale</label><input bind:value={formAzienda.referenteAziendale} placeholder="Nome e Cognome" class="w-full p-3 bg-gray-50 border-none rounded-xl text-sm focus:ring-[#1B4B6B]" /></div>
							<div class="grid grid-cols-2 gap-3">
								<div><label class="block text-[10px] font-bold text-[#1B4B6B] uppercase mb-1">Telefono</label><input bind:value={formAzienda.telefono} placeholder="Fisso" class="w-full p-3 bg-gray-50 border-none rounded-xl text-sm focus:ring-[#1B4B6B]" /></div>
								<div><label class="block text-[10px] font-bold text-[#1B4B6B] uppercase mb-1">Cellulare</label><input bind:value={formAzienda.cellulare} placeholder="Mobile" class="w-full p-3 bg-gray-50 border-none rounded-xl text-sm focus:ring-[#1B4B6B]" /></div>
							</div>
						</div>
					</div>
				</div>
				<div class="p-8 bg-gray-50 flex justify-end gap-4 border-t">
					<button onclick={() => (showModal = false)} class="px-6 py-3 text-[10px] font-black uppercase text-gray-400 hover:text-gray-600 transition-colors">Annulla</button>
					<button onclick={salvaNuovaAzienda} disabled={!isFormValid || isSaving} class="bg-[#1B4B6B] text-white px-8 py-3 rounded-xl text-[10px] font-black uppercase shadow-lg flex items-center gap-2 hover:bg-[#153a54]">
						{#if isSaving}<Loader2 size={14} class="animate-spin"/>{:else}<Plus size={14}/>{/if} Salva Azienda
					</button>
				</div>
			</div>
		</div>
	{/if}

	{#if showDeleteModal}
		<div class="fixed inset-0 bg-red-900/20 backdrop-blur-sm flex items-center justify-center z-[130] p-4" transition:fade>
			<div class="bg-white rounded-3xl shadow-2xl w-full max-w-md overflow-hidden" in:scale>
				<div class="p-8 text-center">
					<div class="w-20 h-20 bg-red-50 text-red-600 rounded-full flex items-center justify-center mx-auto mb-6"><AlertTriangle size={40}/></div>
					<h2 class="text-2xl font-black text-[#1B4B6B] uppercase tracking-tighter mb-2">Eliminare l'azienda?</h2>
					<p class="text-sm text-gray-400 mb-8">Questa azione è irreversibile. Digita <span class="font-black text-red-600">ELIMINA</span> per confermare la cancellazione di <span class="font-bold text-[#1B4B6B]">{aziendaDaEliminare?.ragioneSociale}</span>.</p>

					<div class="relative w-full mb-6 group">
						<input
								bind:value={confermaTesto}
								maxlength="7"
								class="absolute inset-0 w-full h-full opacity-0 z-10 cursor-text uppercase"
						/>
						<div class="w-full p-4 bg-gray-50 border-2 border-transparent group-focus-within:border-red-600 rounded-2xl text-center font-black uppercase transition-all flex justify-center items-center text-2xl tracking-[0.3em] pl-[0.3em]">
							{#each 'ELIMINA'.split('') as char, i (i)}
                         <span class={confermaTesto.length > i ? (confermaTesto.toUpperCase()[i] === char ? 'text-red-600' : 'text-orange-600') : 'text-gray-300 transition-colors'}>
                            {confermaTesto.toUpperCase()[i] || char}
                         </span>
							{/each}
						</div>
					</div>

					<div class="flex flex-col gap-3">
						<button onclick={confermaEliminazione} disabled={!isConfermaValida} class="w-full bg-red-600 text-white py-4 rounded-2xl font-black uppercase text-[10px] shadow-lg shadow-red-200 disabled:opacity-30 disabled:shadow-none transition-all hover:bg-red-700">Sì, elimina definitivamente</button>
						<button onclick={() => { showDeleteModal = false; confermaTesto = ''; }} class="w-full py-4 text-[10px] font-black uppercase text-gray-400 hover:text-gray-600">No, annulla l'operazione</button>
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

	{#if showDeleteDipModal}
		<div class="fixed inset-0 bg-red-900/20 backdrop-blur-sm flex items-center justify-center z-[130] p-4" transition:fade>
			<div class="bg-white rounded-3xl shadow-2xl w-full max-w-md overflow-hidden" in:scale>
				<div class="p-8 text-center">
					<div class="w-20 h-20 bg-red-50 text-red-600 rounded-full flex items-center justify-center mx-auto mb-6"><Users size={40}/></div>
					<h2 class="text-2xl font-black text-[#1B4B6B] uppercase tracking-tighter mb-2">Rimuovere dipendente?</h2>
					<p class="text-sm text-gray-400 mb-8">Il lavoratore <span class="font-bold text-[#1B4B6B]">{dipDaEliminare?.nome} {dipDaEliminare?.cognome}</span> verrà rimosso dall'anagrafica aziendale.</p>
					<div class="flex flex-col gap-3">
						<button onclick={confermaEliminaDip} class="w-full bg-red-600 text-white py-4 rounded-2xl font-black uppercase text-[10px] shadow-lg shadow-red-200 transition-all hover:bg-red-700">Rimuovi Dipendente</button>
						<button onclick={() => (showDeleteDipModal = false)} class="w-full py-4 text-[10px] font-black uppercase text-gray-400 hover:text-gray-600">Annulla</button>
					</div>
				</div>
			</div>
		</div>
	{/if}
</div>