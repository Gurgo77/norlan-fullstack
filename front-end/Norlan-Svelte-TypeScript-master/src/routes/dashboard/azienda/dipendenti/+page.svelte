<script lang="ts">
	import { onMount } from 'svelte';
	import { page } from '$app/stores';
	import { goto } from '$app/navigation';
	import { resolveRoute } from '$app/paths';
	import { fade, scale, slide } from 'svelte/transition';
	import {
		Users, UserPlus, Trash2, Search, Mail, Building2,
		IdCard, Loader2, X, AlertTriangle, ChevronLeft,
		FileText, ShieldCheck, Download, Plus, Calendar, MessageSquare,
		AlertCircle, CheckCircle, Clock, RefreshCw, Edit3, Save, Lock
	} from 'lucide-svelte';

	import { LavoratoreService, type DipendenteDTO } from '$lib/services/LavoratoreService';
	import { AuthService } from '$lib/services/AuthService';
	import { DocumentoService } from '$lib/services/DocumentoService';
	import { Documento } from '$lib/models/Documento';
	import type { AssegnazioneDPI } from '$lib/models/AssegnazioneDPI';
	import StatCard from '$lib/Components/UI/StatCard.svelte';
	import {AnagraficaService} from "$lib/services/AnagraficaService";
	import AlertCard from '$lib/Components/UI/AlertCard.svelte';
	import DipendenteCard from '$lib/Components/Features/Anagrafica/DipendenteCard.svelte';
	import DpiCard from '$lib/Components/Features/Documentale/DpiCard.svelte';

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

	function getStatoScadenza(dataScadenza: string | undefined) {
		if (!dataScadenza) return { colore: 'text-green-600', bg: 'bg-green-50', border: 'border-green-200', label: 'Valido', IconaDef: CheckCircle, peso: 3, raw: 'OK' as 'OK'|'WARNING'|'DANGER' };
		const oggi = new Date();
		const scadenza = new Date(dataScadenza);
		const diffTempo = scadenza.getTime() - oggi.getTime();
		const giorniRimanenti = Math.ceil(diffTempo / (1000 * 3600 * 24));
		if (giorniRimanenti < 0) return { colore: 'text-red-600', bg: 'bg-red-50', border: 'border-red-200', label: 'Scaduto', IconaDef: AlertCircle, peso: 1, raw: 'DANGER' as 'OK'|'WARNING'|'DANGER' };
		if (giorniRimanenti <= 30) return { colore: 'text-yellow-600', bg: 'bg-yellow-50', border: 'border-yellow-200', label: `In scadenza (${giorniRimanenti}gg)`, IconaDef: Clock, peso: 2, raw: 'WARNING' as 'OK'|'WARNING'|'DANGER' };
		return { colore: 'text-green-600', bg: 'bg-green-50', border: 'border-green-200', label: 'Valido', IconaDef: CheckCircle, peso: 3, raw: 'OK' as 'OK'|'WARNING'|'DANGER' };
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
			console.error("Si è verificato un errore durante il caricamento dell'anagrafica lavoratori:", error);
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
			documentiCorrenti = resDocs;
			dpiCorrenti = resDpis as unknown as DpiEsteso[];
		} catch (error) {
			console.error("Si è verificato un problema durante il recupero dei dettagli del lavoratore selezionato:", error);
		} finally {
			isLoadingDettaglio = false;
		}
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
		try {
			const payload = {
				nome: formDipendente.nome,
				cognome: formDipendente.cognome,
				codiceFiscale: formDipendente.codiceFiscale.toUpperCase(),
				email: formDipendente.email,
				passwordHash: isEditing ? undefined : formDipendente.passwordHash
			};

			if (isEditing && formDipendente.idUtente) {
				const aggiornato = await LavoratoreService.update(formDipendente.idUtente, payload as any);
				lavoratori = lavoratori.map(l => l.idUtente === aggiornato.idUtente ? { ...aggiornato, nomeAzienda: nomeAziendaCorrente } : l);
				if (selectedDipendente?.idUtente === aggiornato.idUtente) {
					selectedDipendente = { ...aggiornato, nomeAzienda: nomeAziendaCorrente };
				}
			} else {
				const nuovo = await LavoratoreService.create(idAziendaCorrente, payload as any);
				lavoratori = [{ ...nuovo, nomeAzienda: nomeAziendaCorrente }, ...lavoratori];
			}

			showAddModal = false;
		} catch (error) {
			console.error("Si è verificato un errore durante la procedura di salvataggio del lavoratore:", error);
			alert("Impossibile completare l'operazione. Verificare i dati e riprovare.");
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
		} catch (error) {
			const err = error as ServerError;
			if (err.response?.status === 409) {
				console.error("Errore di integrità dati durante la rimozione del dipendente: record correlati presenti.");
				alert("Impossibile procedere: il lavoratore non può essere eliminato perché sono presenti documenti, DPI o iscrizioni a corsi collegati al suo profilo.");
			} else {
				console.error("Si è verificato un errore durante la rimozione del lavoratore dal sistema:", error);
				alert("Si è verificato un errore durante l'eliminazione.");
			}
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
		} catch (error) {
			console.error("Si è verificato un problema tecnico durante il download del file selezionato:", error);
			alert("Errore durante il download del documento.");
		}
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
			console.error("Si è verificato un errore durante il salvataggio dei dati relativi al Dispositivo di Protezione Individuale:", error);
			alert("Errore durante il salvataggio dei dati del dispositivo.");
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
			console.error("Impossibile rimuovere il dispositivo di protezione dal record del lavoratore:", error);
			alert("Errore durante l'eliminazione dell'assegnazione.");
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
			<div class="bg-white rounded-3xl shadow-xl border border-gray-100 overflow-hidden mb-12">
				<div class="bg-[#1B4B6B] p-10 text-white flex justify-between items-end relative">
					<div class="flex items-center gap-6"><div class="w-24 h-24 bg-white text-[#1B4B6B] rounded-3xl flex items-center justify-center font-black text-4xl shadow-lg">{selectedDipendente.nome[0]}{selectedDipendente.cognome[0]}</div><div><div class="flex items-center gap-3 mb-3"><span class="bg-white/20 border border-white/20 text-white text-[10px] font-black px-4 py-1.5 rounded-full uppercase flex items-center gap-2"><Building2 size={12}/> {selectedDipendente.nomeAzienda}</span></div><h1 class="text-5xl font-extrabold uppercase tracking-tighter">{selectedDipendente.nome} {selectedDipendente.cognome}</h1></div></div>
					<div class="flex items-center gap-3">
						<button onclick={apriModaleModifica} class="flex items-center gap-2 bg-white text-[#1B4B6B] px-6 py-3.5 rounded-2xl transition-all font-extrabold uppercase text-[10px] shadow-xl hover:bg-gray-100 hover:scale-105">
							<Edit3 size={16} /> Modifica Dati
						</button>
						<button onclick={() => apreGmail(selectedDipendente?.email || '')} class="flex items-center gap-2 bg-white/20 text-white border border-white/20 px-6 py-3.5 rounded-2xl transition-all font-extrabold uppercase text-[10px] shadow-xl hover:bg-white/30 hover:scale-105">
							<Mail size={16} /> Manda Mail
						</button>
						<button onclick={() => vaiInChat(selectedDipendente?.idUtente)} class="flex items-center gap-2 bg-white/20 text-white border border-white/20 px-6 py-3.5 rounded-2xl transition-all font-extrabold uppercase text-[10px] shadow-xl hover:bg-white/30 hover:scale-105">
							<MessageSquare size={16} /> Contatta
						</button>
						<button onclick={() => preparaEliminazione(selectedDipendente)} class="flex items-center gap-2 bg-red-600 text-white px-6 py-3.5 rounded-2xl transition-all font-extrabold uppercase text-[10px] border border-white/10 shadow-xl hover:bg-red-700 hover:scale-105">
							<Trash2 size={16} /> Rimuovi
						</button>
					</div>
				</div>
				<div class="p-8 grid grid-cols-1 md:grid-cols-2 gap-8 bg-gray-50/30"><div><p class="text-[10px] font-bold text-gray-400 uppercase mb-1 flex items-center gap-1"><IdCard size={12}/> Codice Fiscale</p><p class="text-lg font-mono font-extrabold text-[#1B4B6B] tracking-widest">{selectedDipendente.codiceFiscale}</p></div><div><p class="text-[10px] font-bold text-gray-400 uppercase mb-1 flex items-center gap-1"><Mail size={12}/> Email Contatto</p><p class="text-lg font-bold text-[#1B4B6B] lowercase">{selectedDipendente.email || 'Indirizzo non disponibile'}</p></div></div>
			</div>

			{#if isLoadingDettaglio}
				<div class="py-20 text-center"><Loader2 size={40} class="animate-spin mx-auto text-[#1B4B6B]" /></div>
			{:else}
				<div class="grid grid-cols-1 lg:grid-cols-2 gap-8">
					<div class="space-y-6">
						<StatCard titolo="Attestati Totali" valore={documentiCorrenti.length} icona={FileText} bgIcona="bg-blue-50" />
						{#if sortedDocumentiCorrenti.length > 0}
							<div class="space-y-4">
								{#each sortedDocumentiCorrenti as doc (doc.idDocumento)}
									{@const stato = getStatoScadenza(doc.dataScadenza)}
									{@const IconaStato = stato.IconaDef}
									<div class="bg-white p-5 rounded-2xl border border-gray-100 shadow-sm hover:shadow-md transition-all"><div class="flex justify-between items-start mb-3"><div class="flex items-center gap-3"><div class="p-2 {stato.bg} {stato.colore} rounded-xl"><FileText size={16} /></div><div><h4 class="font-extrabold text-[#1B4B6B] uppercase text-xs leading-tight">{doc.tipologia.replace(/_/g, ' ')}</h4><p class="text-[9px] text-gray-400 font-bold uppercase mt-0.5">{doc.modulo}</p></div></div><div class="flex gap-1"><button onclick={() => scaricaDoc(doc)} class="p-1.5 text-gray-400 hover:text-[#1B4B6B] transition-all bg-gray-50 rounded-lg hover:bg-gray-100"><Download size={14} /></button></div></div><div class="flex items-center justify-between pt-3 border-t border-gray-50"><div class="flex items-center gap-1.5 text-gray-400"><Calendar size={10} /><span class="text-[9px] font-bold uppercase">Scad: {new Date(doc.dataScadenza).toLocaleDateString()}</span></div><div class="inline-flex items-center gap-1.5 px-2 py-1 rounded-md border {stato.border} {stato.bg} {stato.colore}"><IconaStato size={12} /><span class="text-[8px] font-black uppercase tracking-wider">{stato.label}</span></div></div></div>
								{/each}
							</div>
						{:else}
							<div class="bg-white rounded-3xl p-8 text-center border-2 border-dashed border-gray-200 text-gray-300 uppercase font-bold text-[10px] italic">Nessun attestato formativo rilevato</div>
						{/if}
					</div>
					<div class="space-y-6">
						<div class="flex items-stretch gap-4">
							<div class="flex-1">
								<StatCard titolo="DPI Consegnati" valore={dpiCorrenti.length} icona={ShieldCheck} bgIcona="bg-emerald-50" testoIcona="text-emerald-600" />
							</div>
							<button onclick={openNewDpiModal} class="bg-[#1B4B6B] text-white px-5 rounded-3xl flex flex-col items-center justify-center gap-2 hover:bg-[#1B4B6B]/90 transition-all shadow-md shrink-0">
								<Plus size={24} />
								<span class="text-[9px] font-black uppercase tracking-widest">Assegna</span>
							</button>
						</div>
						{#if sortedDpiCorrenti.length > 0}
							<div class="space-y-4">
								{#each sortedDpiCorrenti as dpi (dpi.idAssegnazione || dpi.id || Math.random())}
									{@const nomeDpiReale = dpi.tipo === 'ALTRO' && dpi.nomeDpi ? dpi.nomeDpi : (dpi.tipo || '').replace(/_/g, ' ')}
									{@const stato = getStatoScadenza(dpi.dataScadenzaRevisione)}
									<div class="relative group">
										<DpiCard
												ruolo="azienda"
												dpi={{
                                     id: dpi.idAssegnazione || dpi.id || 0,
                                     nome: nomeDpiReale,
                                     stato: stato.raw,
                                     dataRevisione: dpi.dataScadenzaRevisione ? new Date(dpi.dataScadenzaRevisione).toLocaleDateString() : 'N.D.',
                                  }}
												onModifica={openUpdateDpiModal}
										/>
										<button
												onclick={() => preparaEliminaDPI(dpi.idAssegnazione || dpi.id || 0)}
												class="absolute -top-2 -right-2 bg-red-100 text-red-600 p-2 rounded-full opacity-0 group-hover:opacity-100 transition-opacity shadow-md hover:bg-red-200"
												title="Rimuovi DPI"
										>
											<Trash2 size={14} />
										</button>
									</div>
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

	{#if showAddModal}
		<div class="fixed inset-0 bg-[#1B4B6B]/40 backdrop-blur-sm flex items-center justify-center z-[100] p-4" transition:fade>
			<div class="bg-white rounded-3xl shadow-2xl w-full max-w-2xl overflow-hidden" in:scale>
				<div class="bg-[#1B4B6B] p-6 text-white flex justify-between items-center">
					<h2 class="text-xl font-black uppercase tracking-tighter flex items-center gap-2">
						{#if isEditing}<Edit3 size={20}/> Modifica Dati Lavoratore{:else}<UserPlus size={20}/> Registra Nuovo Lavoratore{/if}
					</h2>
					<button onclick={() => (showAddModal = false)} class="text-white hover:rotate-90 transition-all duration-300"><X size={24}/></button>
				</div>
				<div class="p-8 grid grid-cols-1 md:grid-cols-2 gap-6">
					<div class="space-y-4">
						<label class="block text-[10px] font-bold text-[#1B4B6B] uppercase">Azienda di Appartenenza</label>
						<div class="w-full p-3 bg-gray-200 border-none rounded-xl text-sm font-bold text-gray-500 flex items-center gap-2 cursor-not-allowed">
							<Building2 size={16} /> {nomeAziendaCorrente}
						</div>
						<label class="block text-[10px] font-bold text-[#1B4B6B] uppercase">Nome *</label>
						<input bind:value={formDipendente.nome} placeholder="Es: Mario" class="w-full p-3 bg-gray-50 border-none rounded-xl text-sm focus:ring-2 focus:ring-[#1B4B6B] outline-none" />
						<label class="block text-[10px] font-bold text-[#1B4B6B] uppercase">Cognome *</label>
						<input bind:value={formDipendente.cognome} placeholder="Es: Rossi" class="w-full p-3 bg-gray-50 border-none rounded-xl text-sm focus:ring-2 focus:ring-[#1B4B6B] outline-none" />
					</div>
					<div class="space-y-4">
						<label class="block text-[10px] font-bold text-[#1B4B6B] uppercase">Email Accesso</label>
						<input bind:value={formDipendente.email} type="email" placeholder="m.rossi@email.it" class="w-full p-3 bg-gray-50 border-none rounded-xl text-sm focus:ring-2 focus:ring-[#1B4B6B] outline-none" />
						<label class="block text-[10px] font-bold text-[#1B4B6B] uppercase">Codice Fiscale *</label>
						<input bind:value={formDipendente.codiceFiscale} maxlength="16" placeholder="RSSMRA80A01H501W" class="w-full p-3 bg-gray-50 border-none rounded-xl text-sm font-mono focus:ring-2 focus:ring-[#1B4B6B] outline-none uppercase" />

						{#if !isEditing}
							<label class="block text-[10px] font-bold text-[#1B4B6B] uppercase">Password di Accesso *</label>
							<input bind:value={formDipendente.passwordHash} type="text" placeholder="Password temporanea" class="w-full p-3 bg-gray-50 border-none rounded-xl text-sm focus:ring-2 focus:ring-[#1B4B6B] outline-none" />
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
				<div class="p-8 bg-gray-50 flex justify-end gap-4 border-t border-gray-100">
					<button onclick={() => (showAddModal = false)} class="px-6 py-3 text-[10px] font-black uppercase text-gray-400 hover:text-gray-600 transition-colors">Annulla</button>
					<button onclick={salvaDipendente} disabled={!isFormValid || isSaving} class="bg-[#1B4B6B] text-white px-8 py-3 rounded-xl text-[10px] font-black uppercase shadow-lg disabled:opacity-50 flex items-center gap-2 hover:bg-[#1B4B6B]/90 transition-all">
						{#if isSaving}<Loader2 size={14} class="animate-spin"/>{:else if isEditing}<Save size={14}/>{:else}<UserPlus size={14}/>{/if}
						{isEditing ? 'Aggiorna Dati' : 'Registra Dipendente'}
					</button>
				</div>
			</div>
		</div>
	{/if}

	{#if showDeleteModal}
		<div class="fixed inset-0 bg-red-900/20 backdrop-blur-sm flex items-center justify-center z-[110] p-4" transition:fade>
			<div class="bg-white rounded-3xl shadow-2xl w-full max-w-sm overflow-hidden" in:scale>
				<div class="p-8 text-center"><div class="w-20 h-20 bg-red-50 text-red-600 rounded-full flex items-center justify-center mx-auto mb-6"><AlertTriangle size={40}/></div><h2 class="text-2xl font-black text-[#1B4B6B] uppercase tracking-tighter mb-2">Rimuovere dipendente?</h2><p class="text-sm text-gray-400 mb-8">Il lavoratore <span class="font-bold text-[#1B4B6B]">{dipendenteDaEliminare?.nome} {dipendenteDaEliminare?.cognome}</span> verrà rimosso definitivamente dal sistema.</p><div class="flex flex-col gap-3"><button onclick={confermaEliminazione} class="w-full bg-red-600 text-white py-4 rounded-2xl font-black uppercase text-[10px] shadow-lg shadow-red-200 transition-all hover:bg-red-700">Sì, elimina definitivamente</button><button onclick={() => { showDeleteModal = false; dipendenteDaEliminare = null; }} class="w-full py-4 text-[10px] font-black uppercase text-gray-400 hover:text-gray-600">No, annulla l'operazione</button></div></div>
			</div>
		</div>
	{/if}

	{#if showDpiModal}
		<div class="fixed inset-0 bg-[#1B4B6B]/40 backdrop-blur-sm flex items-center justify-center z-[110] p-4" transition:fade>
			<div class="bg-white rounded-3xl shadow-2xl w-full max-w-lg overflow-hidden" in:scale>
				<div class="bg-[#1B4B6B] p-6 text-white flex justify-between items-center"><h2 class="text-xl font-black uppercase tracking-tighter flex items-center gap-2"><ShieldCheck size={20}/> {formDpi.idAssegnazione ? 'Aggiorna DPI Lavoratore' : 'Registra Consegna DPI'}</h2><button onclick={() => (showDpiModal = false)} class="text-white hover:rotate-90 transition-all duration-300"><X size={24}/></button></div>
				<div class="p-8 space-y-6"><div><label class="block text-[10px] font-bold text-[#1B4B6B] uppercase mb-1">Tipologia DPI *</label><select bind:value={formDpi.tipo} disabled={!!formDpi.idAssegnazione} class="w-full p-3 bg-gray-50 border-none rounded-xl text-sm font-bold uppercase focus:ring-2 focus:ring-[#1B4B6B] outline-none disabled:opacity-50"><option value="">Seleziona DPI...</option><option value="ELMETTO">Elmetto</option><option value="GUANTI">Guanti</option><option value="SCARPE_ANTINFORTUNISTICHE">Scarpe Antinfortunistiche</option><option value="OCCHIALI">Occhiali</option><option value="ALTRO">Altro</option></select></div>{#if formDpi.tipo === 'ALTRO'}<div class="space-y-1" transition:slide><label class="block text-[10px] font-bold text-[#1B4B6B] uppercase">Nome DPI Personalizzato *</label><input bind:value={formDpi.nomeDpi} disabled={!!formDpi.idAssegnazione} type="text" placeholder="Specifica il nome del DPI..." class="w-full p-3 bg-gray-50 border-none rounded-xl text-sm focus:ring-2 focus:ring-[#1B4B6B] outline-none disabled:opacity-50" /></div>{/if}<div class="grid grid-cols-2 gap-4 mt-4"><div><label class="block text-[10px] font-bold text-[#1B4B6B] uppercase mb-1">Nuova Data Consegna *</label><input type="date" max="9999-12-31" bind:value={formDpi.dataConsegna} class="w-full p-3 bg-gray-50 border-none rounded-xl text-sm font-bold focus:ring-2 focus:ring-[#1B4B6B] outline-none" /></div><div><label class="block text-[10px] font-bold text-[#1B4B6B] uppercase mb-1">Scadenza Revisione *</label><input type="date" max="9999-12-31" bind:value={formDpi.dataScadenzaRevisione} class="w-full p-3 bg-gray-50 border-none rounded-xl text-sm font-bold focus:ring-2 focus:ring-[#1B4B6B] outline-none" /></div></div></div>
				<div class="p-8 bg-gray-50 flex justify-end gap-4 border-t border-gray-100"><button onclick={() => (showDpiModal = false)} class="px-6 py-3 text-[10px] font-black uppercase text-gray-400 hover:text-gray-600 transition-colors">Annulla</button><button onclick={salvaDPI} disabled={isSavingDpi || !formDpi.tipo || (formDpi.tipo === 'ALTRO' && !formDpi.nomeDpi.trim()) || !formDpi.dataConsegna || !formDpi.dataScadenzaRevisione} class="bg-[#1B4B6B] text-white px-8 py-3 rounded-xl text-[10px] font-black uppercase shadow-lg disabled:opacity-50 flex items-center gap-2 hover:bg-[#1B4B6B]/90 transition-colors">{#if isSavingDpi}<Loader2 size={14} class="animate-spin" />{/if} {isSavingDpi ? 'Salvataggio...' : 'Conferma'}</button></div>
			</div>
		</div>
	{/if}

	{#if showDeleteDpiModal}
		<div class="fixed inset-0 bg-red-900/20 backdrop-blur-sm flex items-center justify-center z-[130] p-4" transition:fade>
			<div class="bg-white rounded-3xl shadow-2xl w-full max-w-sm overflow-hidden" in:scale>
				<div class="p-8 text-center"><div class="w-20 h-20 bg-red-50 text-red-600 rounded-full flex items-center justify-center mx-auto mb-6"><Trash2 size={40}/></div><h2 class="text-2xl font-black text-[#1B4B6B] uppercase tracking-tighter mb-2">Rimuovere DPI?</h2><p class="text-sm text-gray-400 mb-8">Stai annullando l'assegnazione di: <br><span class="font-bold text-[#1B4B6B]">{dpiDaEliminare?.tipo.replace(/_/g, ' ')}</span></p><div class="flex flex-col gap-3"><button onclick={confermaEliminaDPI} class="w-full bg-red-600 text-white py-4 rounded-2xl font-black uppercase text-[10px] shadow-lg shadow-red-200 transition-all hover:bg-red-700">Conferma Rimozione</button><button onclick={() => { showDeleteDpiModal = false; dpiDaEliminare = null; }} class="w-full py-4 text-[10px] font-black uppercase text-gray-400 hover:text-gray-600">Annulla</button></div></div>
			</div>
		</div>
	{/if}
</div>

<style>
	.custom-scrollbar::-webkit-scrollbar { width: 3px; }
	.custom-scrollbar::-webkit-scrollbar-thumb { background: rgba(27, 75, 107, 0.1); border-radius: 10px; }
</style>