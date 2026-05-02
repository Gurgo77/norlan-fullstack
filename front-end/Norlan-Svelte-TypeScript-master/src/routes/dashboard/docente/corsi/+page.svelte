<script lang="ts">
	import { onMount } from 'svelte';
	import { fade, scale } from 'svelte/transition';
	import {
		Plus, X, Trash2, Search, Filter,
		Calendar, MapPin, Loader2, User,
		CheckSquare, UploadCloud, CheckCircle2, Clock, Building2, Users,
		BarChart3, Star, BookOpen, Play // <-- Aggiunto Play per il bottone di inizio corso
	} from 'lucide-svelte';

	// Modelli
	import { CorsoFormazione } from '$lib/models/CorsoFormazione';
	import { Docente, type DocenteData } from '$lib/models/Docente';
	import { StatoCorso } from '$lib/models/Enums';
	import type { CorsoFormazioneRequest } from '$lib/models/CorsoFormazioneRequest';
	import type { IscrizioneCorso } from '$lib/models/IscrizioneCorso';

	// Definiamo localmente l'interfaccia DTO del Feedback
	interface FeedbackStatsDTO {
		idCorso: number;
		mediaDocenza: number;
		mediaContenuti: number;
		totaleFeedback: number;
		commenti: string[];
	}

	// Servizi
	import { FormazioneService } from '$lib/services/FormazioneService';
	import { AnagraficaService } from '$lib/services/AnagraficaService';
	import { AuthService } from '$lib/services/AuthService';
	import { FeedbackService } from '$lib/services/FeedbackService';
	import httpClient from '$lib/api/httpClient';

	// Estensione per gestire il numero iscritti
	interface CorsoFormazioneEsteso extends CorsoFormazione {
		numeroIscritti?: number;
		isLoadingIscritti?: boolean;
	}

	// --- STATO REATTIVO GLOBALE ---
	let corsi = $state<CorsoFormazioneEsteso[]>([]);
	let docenti = $state<Docente[]>([]);
	let dipendenti = $state<any[]>([]); // Array grezzo dal backend
	let isLoading = $state(true);
	let isSaving = $state(false);
	let searchQuery = $state('');
	let filtroStato = $state<StatoCorso | ''>('');

	// --- STATI MODALI ---
	let showModalNuovo = $state(false);
	let showModalPresenze = $state(false);
	let showModalUpload = $state(false);
	let showModalMateriale = $state(false); // Modale per il materiale didattico
	let showModalFeedback = $state(false);

	// --- STATI OPERATIVI ---
	let selectedCorso = $state<CorsoFormazioneEsteso | null>(null);
	let iscrizioniAttuali = $state<IscrizioneCorso[]>([]);
	let presenzeSelezionate = $state<number[]>([]);
	let fileUploads = $state<Record<number, File>>({});
	let isActionLoading = $state(false);

	let isLoadingFeedback = $state(false);
	let statsFeedback = $state<FeedbackStatsDTO | null>(null);

	// Variabili per upload materiale
	let isUploadingMateriale = $state(false);
	let selectedCorsoMateriale = $state<CorsoFormazione | null>(null);
	let fileMateriale = $state<File | null>(null);
	let titoloMateriale = $state('');

	// Form di creazione
	let formCorso = $state<Partial<CorsoFormazioneRequest>>({
		titolo: '', dataOrario: '', luogoFisico: '', idDocente: undefined
	});

	const isFormValid = $derived(
			!!formCorso.titolo && !!formCorso.dataOrario && !!formCorso.luogoFisico && !!formCorso.idDocente
	);

	// --- LOGICA FILTRI (REGOLA 4) ---
	const corsiAttiviAdmin = $derived(
			corsi.filter(c => c.stato !== StatoCorso.CERTIFICATO)
	);

	const corsiFiltrati = $derived(
			corsiAttiviAdmin.filter(c => {
				const matchTesto = (c.titolo || '').toLowerCase().includes(searchQuery.toLowerCase());
				const matchStato = filtroStato === '' || c.stato === filtroStato;
				return matchTesto && matchStato;
			})
	);

	const corsiConclusi = $derived(corsiFiltrati.filter(c =>
			c.stato === StatoCorso.CONCLUSO ||
			c.stato === StatoCorso.ATTESA_FIRMA_DOCENTE ||
			c.stato === StatoCorso.VALIDATO
	));
	const corsiInSvolgimento = $derived(corsiFiltrati.filter(c => c.stato === StatoCorso.IN_SVOLGIMENTO));
	const corsiProgrammati = $derived(corsiFiltrati.filter(c => c.stato === StatoCorso.PROGRAMMATO || !c.stato));

	// Archivio per i corsi certificati
	const corsiArchiviati = $derived(
			corsi.filter(c => c.stato === StatoCorso.CERTIFICATO && (c.titolo || '').toLowerCase().includes(searchQuery.toLowerCase()))
	);

	// --- DERIVAZIONE AZIENDE PER UPLOAD ATTESTATI ---
	const aziendeCoinvolte = $derived.by(() => {
		if (!selectedCorso || iscrizioniAttuali.length === 0 || dipendenti.length === 0) return [];
		const presenti = iscrizioniAttuali.filter(i => i.presenzaConfermata);
		const map = new Map<number, { idAzienda: number, ragioneSociale: string, count: number }>();

		presenti.forEach(isc => {
			const dip = dipendenti.find(d => d.idUtente === isc.idUtente);
			if (!dip) return;
			let idAzienda = dip.azienda?.idUtente || dip.azienda?.id || dip.aziendaId || dip.idAzienda;
			let nomeAzienda = dip.azienda?.ragioneSociale || dip.aziendaRagioneSociale || dip.ragioneSocialeAzienda || "Azienda Sconosciuta";

			if (idAzienda) {
				if (!map.has(idAzienda)) {
					map.set(idAzienda, { idAzienda: idAzienda, ragioneSociale: nomeAzienda, count: 0 });
				}
				map.get(idAzienda)!.count++;
			}
		});
		return Array.from(map.values());
	});

	// --- CARICAMENTO ---
	onMount(async () => {
		try {
			const [corsiRes, docentiRes, dipendentiRes] = await Promise.all([
				FormazioneService.getAllCorsi(),
				AnagraficaService.getAllDocenti(),
				AnagraficaService.getAllDipendenti()
			]);

			corsi = corsiRes.map(c => ({ ...c, numeroIscritti: 0, isLoadingIscritti: true }));
			docenti = (docentiRes as DocenteData[]).map(d => new Docente(d));
			dipendenti = dipendentiRes;

			for (let i = 0; i < corsi.length; i++) {
				try {
					const iscritti = await FormazioneService.getIscrizioniByCorso(corsi[i].idCorso);
					corsi[i].numeroIscritti = iscritti.length;
				} catch (e) {
					console.warn("Errore conteggio iscritti", e);
				} finally {
					corsi[i].isLoadingIscritti = false;
				}
			}
		} catch (error) {
			console.error("Errore init:", error);
		} finally {
			isLoading = false;
		}
	});

	// --- AZIONI CORSO ---
	async function salvaNuovoCorso() {
		if (!isFormValid) return;
		isSaving = true;
		try {
			const payload = { ...formCorso, dataOrario: new Date(formCorso.dataOrario!).toISOString(), stato: StatoCorso.PROGRAMMATO };
			const nuovo = await FormazioneService.createCorso(payload as any);
			corsi = [...corsi, { ...nuovo, numeroIscritti: 0, isLoadingIscritti: false }];
			showModalNuovo = false;
			formCorso = { titolo: '', dataOrario: '', luogoFisico: '', idDocente: undefined };
		} catch { alert("Errore salvataggio."); } finally { isSaving = false; }
	}

	async function eliminaCorso(idCorso: number) {
		if (!confirm("Eliminare definitivamente?")) return;
		try {
			await FormazioneService.deleteCorso(idCorso);
			corsi = corsi.filter(c => c.idCorso !== idCorso);
		} catch { alert("Errore eliminazione."); }
	}

	async function cambiaStatoCorso(idCorso: number, nuovoStato: StatoCorso) {
		if (!confirm(`Impostare come ${nuovoStato.replace('_', ' ')}?`)) return;
		try {
			await FormazioneService.updateStatoCorso(idCorso, nuovoStato);
			corsi = corsi.map(c => c.idCorso === idCorso ? { ...c, stato: nuovoStato } : c);
		} catch { alert("Errore aggiornamento stato."); }
	}

	// --- AZIONI MATERIALE ---
	function apriModaleMateriale(corso: CorsoFormazioneEsteso) {
		selectedCorsoMateriale = corso;
		fileMateriale = null;
		titoloMateriale = '';
		showModalMateriale = true;
	}

	async function caricaMaterialeDidattico() {
		if (!selectedCorsoMateriale || !fileMateriale || !titoloMateriale.trim()) return;
		isUploadingMateriale = true;
		try {
			const formData = new FormData();
			formData.append('file', fileMateriale);
			formData.append('titoloDocumento', titoloMateriale);
			await httpClient.post(`/api/formazione/corsi/${selectedCorsoMateriale.idCorso}/materiali`, formData, {
				headers: { 'Content-Type': 'multipart/form-data' }
			});
			alert("Materiale didattico caricato con successo!");
			showModalMateriale = false;
		} catch (error) {
			alert("Errore durante il caricamento del materiale.");
		} finally {
			isUploadingMateriale = false;
		}
	}

	// --- AZIONI REGISTRI ED ATTESTATI ---
	async function apriValidazione(corso: CorsoFormazioneEsteso) {
		selectedCorso = corso;
		isActionLoading = true;
		showModalPresenze = true;
		try {
			iscrizioniAttuali = await FormazioneService.getIscrizioniByCorso(corso.idCorso);
			presenzeSelezionate = iscrizioniAttuali.filter(i => i.presenzaConfermata).map(i => i.idUtente);
		} catch { showModalPresenze = false; } finally { isActionLoading = false; }
	}

	function togglePresenza(idUtente: number) {
		if (presenzeSelezionate.includes(idUtente)) {
			presenzeSelezionate = presenzeSelezionate.filter(id => id !== idUtente);
		} else {
			presenzeSelezionate = [...presenzeSelezionate, idUtente];
		}
	}

	async function confermaPresenze() {
		if (!selectedCorso) return;
		isActionLoading = true;
		try {
			await FormazioneService.validaPresenzeAdmin(selectedCorso.idCorso, presenzeSelezionate);
			corsi = corsi.map(c => c.idCorso === selectedCorso!.idCorso ? { ...c, stato: StatoCorso.ATTESA_FIRMA_DOCENTE } : c);
			showModalPresenze = false;
		} catch { alert("Errore validazione."); } finally { isActionLoading = false; }
	}

	async function apriUploadAttestati(corso: CorsoFormazioneEsteso) {
		selectedCorso = corso;
		showModalUpload = true;
		isActionLoading = true;
		fileUploads = {};
		try { iscrizioniAttuali = await FormazioneService.getIscrizioniByCorso(corso.idCorso); }
		finally { isActionLoading = false; }
	}

	function handleFileChange(event: Event, idAzienda: number) {
		const input = event.target as HTMLInputElement;
		if (input.files?.length) fileUploads[idAzienda] = input.files[0];
	}

	async function confermaDistribuzioneAttestati() {
		if (!selectedCorso) return;
		if (aziendeCoinvolte.some(a => !fileUploads[a.idAzienda])) return alert("Carica tutti i PDF.");
		isActionLoading = true;
		try {
			const payload = aziendeCoinvolte.map(a => ({ idAzienda: a.idAzienda, file: fileUploads[a.idAzienda] }));
			await FormazioneService.distribuisciAttestati(selectedCorso.idCorso, payload);
			corsi = corsi.map(c => c.idCorso === selectedCorso!.idCorso ? { ...c, stato: StatoCorso.CERTIFICATO } : c);
			showModalUpload = false;
		} catch { alert("Errore upload."); } finally { isActionLoading = false; }
	}

	// --- AZIONE FEEDBACK ---
	async function apriStatisticheFeedback(corso: CorsoFormazioneEsteso) {
		selectedCorso = corso;
		showModalFeedback = true;
		isLoadingFeedback = true;
		statsFeedback = null;
		try {
			statsFeedback = await FeedbackService.getStatisticheCorso(corso.idCorso);
		} catch (error) {
			console.error(error);
			alert("Impossibile caricare i feedback.");
			showModalFeedback = false;
		} finally {
			isLoadingFeedback = false;
		}
	}

	function formattaData(iso: string) {
		if (!iso) return '';
		return new Date(iso).toLocaleDateString('it-IT', { day: '2-digit', month: 'short', year: 'numeric', hour: '2-digit', minute:'2-digit' });
	}
</script>

<div in:fade class="pb-20">
	<div class="mb-10 flex justify-between items-start">
		<div>
			<h1 class="text-4xl font-extrabold text-[#1B4B6B]">GESTIONE FORMAZIONE</h1>
			<p class="text-gray-500 font-bold uppercase text-xs tracking-tighter">Pianificazione corsi, aule e monitoraggio qualità.</p>
		</div>
		<button onclick={() => showModalNuovo = true} class="bg-white text-[#1B4B6B] border-2 border-[#1B4B6B] px-8 py-3.5 rounded-xl font-extrabold uppercase text-xs shadow-lg hover:bg-[#1B4B6B] hover:text-white transition-all flex items-center gap-3">
			<Plus size={18} /> Programma Corso
		</button>
	</div>

	<div class="bg-white p-4 rounded-3xl shadow-sm border border-gray-100 flex flex-col lg:flex-row gap-4 mb-10">
		<div class="relative flex-1 group">
			<Search class="absolute left-3 top-1/2 -translate-y-1/2 text-gray-400 group-focus-within:text-[#1B4B6B] transition-colors" size={16} />
			<input bind:value={searchQuery} type="text" placeholder="Cerca per titolo..." class="w-full pl-10 pr-4 py-2.5 bg-white border border-gray-200 rounded-xl text-xs focus:ring-2 focus:ring-[#1B4B6B] outline-none transition-all font-bold uppercase shadow-sm" />
		</div>
		<div class="relative min-w-[240px]">
			<Filter class="absolute left-4 top-1/2 -translate-y-1/2 text-gray-400" size={20} />
			<select bind:value={filtroStato} class="w-full bg-gray-50 border-none rounded-2xl py-4 pl-12 pr-10 text-xs font-bold text-[#1B4B6B] focus:ring-4 focus:ring-[#1B4B6B]/5 transition-all uppercase outline-none appearance-none cursor-pointer">
				<option value="">TUTTI GLI STATI</option>
				<option value={StatoCorso.PROGRAMMATO}>PROGRAMMATI</option>
				<option value={StatoCorso.IN_SVOLGIMENTO}>IN SVOLGIMENTO</option>
				<option value={StatoCorso.ATTESA_FIRMA_DOCENTE}>DA FIRMARE</option>
			</select>
		</div>
	</div>

	{#if isLoading}
		<div class="py-32 flex flex-col items-center justify-center gap-4">
			<Loader2 size={48} class="animate-spin text-[#1B4B6B]" />
			<span class="text-[10px] font-black text-gray-400 uppercase tracking-[0.2em]">Caricamento dati...</span>
		</div>
	{:else}

		<!-- CORSI CONCLUSI -->
		<div class="mb-14">
			<div class="flex items-center gap-3 mb-6 border-b border-gray-200 pb-3">
				<div class="p-2 bg-emerald-100 text-emerald-700 rounded-lg"><CheckCircle2 size={20}/></div>
				<h2 class="text-xl font-extrabold text-[#1B4B6B] uppercase tracking-tight">Corsi Conclusi & Validazioni</h2>
				<span class="ml-auto text-xs font-bold text-gray-400 uppercase">{corsiConclusi.length} Elementi</span>
			</div>

			{#if corsiConclusi.length === 0}
				<div class="p-8 border-2 border-dashed border-gray-200 rounded-2xl text-center text-gray-400 font-bold uppercase text-xs">Nessun corso concluso da processare.</div>
			{:else}
				<div class="grid grid-cols-1 md:grid-cols-2 xl:grid-cols-3 gap-6">
					{#each corsiConclusi as corso (corso.idCorso)}
						<div class="bg-white rounded-2xl shadow-sm border border-gray-100 flex flex-col h-full overflow-hidden" in:scale>
							<div class="p-5 border-b border-gray-50 bg-gray-50/50 flex justify-between items-start">
                         <span class="text-[9px] font-black px-2 py-0.5 rounded uppercase border {corso.stato === StatoCorso.VALIDATO ? 'border-emerald-200 text-emerald-700 bg-emerald-50' : 'border-amber-200 text-amber-700 bg-amber-50'}">
                             {corso.stato ? corso.stato.replace('_', ' ') : 'SCONOSCIUTO'}
                         </span>
								<span class="text-[10px] font-bold text-gray-400 uppercase">ID: #{corso.idCorso}</span>
							</div>
							<div class="p-5 flex-1 flex flex-col gap-3">
								<h3 class="font-extrabold text-[#1B4B6B] uppercase leading-tight">{corso.titolo}</h3>
								<p class="text-xs font-bold text-gray-500 uppercase">{formattaData(corso.dataOrario)}</p>
							</div>
							<div class="p-4 bg-gray-50 border-t border-gray-100 flex flex-col gap-2">
								{#if corso.stato === StatoCorso.CONCLUSO}
									<button onclick={() => apriValidazione(corso)} class="w-full py-2.5 bg-[#1B4B6B] text-white rounded-xl font-bold uppercase text-[10px] tracking-widest flex items-center justify-center gap-2 hover:bg-blue-800 transition-colors shadow-sm">
										<CheckSquare size={14} /> Valida Presenze
									</button>
								{:else if corso.stato === StatoCorso.ATTESA_FIRMA_DOCENTE}
									<div class="w-full py-2.5 bg-gray-200 text-gray-500 rounded-xl font-bold uppercase text-[10px] tracking-widest flex items-center justify-center gap-2 cursor-not-allowed">
										<Clock size={14} /> In Attesa Firma Docente
									</div>
								{:else if corso.stato === StatoCorso.VALIDATO}
									<button onclick={() => apriUploadAttestati(corso)} class="w-full py-2.5 bg-emerald-600 text-white rounded-xl font-bold uppercase text-[10px] tracking-widest flex items-center justify-center gap-2 hover:bg-emerald-700 transition-colors shadow-sm">
										<UploadCloud size={14} /> Genera & Invia Attestati
									</button>
								{/if}
								<!-- Bottone Feedback -->
								<button onclick={() => apriStatisticheFeedback(corso)} class="w-full py-2.5 bg-purple-50 text-purple-700 rounded-xl font-bold uppercase text-[10px] tracking-widest flex items-center justify-center gap-2 hover:bg-purple-100 transition-colors shadow-sm mt-1">
									<BarChart3 size={14} /> Metriche Feedback
								</button>
							</div>
						</div>
					{/each}
				</div>
			{/if}
		</div>

		<!-- CORSI IN SVOLGIMENTO -->
		<div class="mb-14">
			<div class="flex items-center gap-3 mb-6 border-b border-gray-200 pb-3">
				<div class="p-2 bg-blue-100 text-blue-700 rounded-lg"><Loader2 size={20} class="animate-spin-slow"/></div>
				<h2 class="text-xl font-extrabold text-[#1B4B6B] uppercase tracking-tight">In Svolgimento</h2>
			</div>
			{#if corsiInSvolgimento.length === 0}
				<div class="p-8 border-2 border-dashed border-gray-200 rounded-2xl text-center text-gray-400 font-bold uppercase text-xs">Nessun corso in aula al momento.</div>
			{:else}
				<div class="grid grid-cols-1 xl:grid-cols-2 2xl:grid-cols-3 gap-6">
					{#each corsiInSvolgimento as corso (corso.idCorso)}
						<div class="bg-white rounded-2xl shadow-sm border border-blue-200 p-6 flex flex-col relative overflow-hidden">
							<div class="absolute top-0 left-0 w-1.5 h-full bg-blue-500"></div>
							<div class="flex-1">
								<h3 class="font-extrabold text-[#1B4B6B] uppercase mb-4 ml-2">{corso.titolo}</h3>
								<div class="space-y-2 ml-2">
									<div class="flex items-center gap-2 text-xs font-bold text-gray-500"><MapPin size={14} /> {corso.luogoFisico}</div>
									<div class="flex items-center gap-2 text-xs font-bold text-gray-500">
										<Users size={14} />
										{#if corso.isLoadingIscritti}
											<Loader2 size={12} class="animate-spin" />
										{:else}
											{corso.numeroIscritti} Iscritti
										{/if}
									</div>
								</div>
							</div>
							<div class="mt-6 flex flex-col gap-2">
								<button onclick={() => apriModaleMateriale(corso)} class="w-full py-2.5 bg-blue-50 text-blue-700 border border-blue-200 rounded-xl font-bold uppercase text-[10px] tracking-widest flex items-center justify-center gap-2 hover:bg-blue-100 transition-colors">
									<UploadCloud size={14} /> Carica Materiale
								</button>
								<button onclick={() => cambiaStatoCorso(corso.idCorso, StatoCorso.CONCLUSO)} class="w-full py-2.5 bg-[#1B4B6B] text-white rounded-xl font-bold uppercase text-[10px] tracking-widest flex items-center justify-center gap-2 hover:bg-blue-800 transition-colors shadow-sm">
									<CheckCircle2 size={14} /> Concludi Corso
								</button>
							</div>
						</div>
					{/each}
				</div>
			{/if}
		</div>

		<!-- CORSI PROGRAMMATI -->
		<div class="mb-14">
			<div class="flex items-center gap-3 mb-6 border-b border-gray-200 pb-3">
				<div class="p-2 bg-gray-100 text-gray-700 rounded-lg"><Calendar size={20}/></div>
				<h2 class="text-xl font-extrabold text-[#1B4B6B] uppercase tracking-tight">Programmati (Futuri)</h2>
			</div>
			{#if corsiProgrammati.length === 0}
				<div class="p-8 border-2 border-dashed border-gray-200 rounded-2xl text-center text-gray-400 font-bold uppercase text-xs">Nessun corso futuro programmato.</div>
			{:else}
				<div class="grid grid-cols-1 md:grid-cols-2 xl:grid-cols-4 gap-6">
					{#each corsiProgrammati as corso (corso.idCorso)}
						<div class="bg-white rounded-2xl shadow-sm border border-gray-100 p-5 group hover:border-[#1B4B6B]/30 transition-all">
							<div class="flex justify-between items-start mb-4">
								<span class="text-[9px] font-black px-2 py-0.5 rounded uppercase border border-gray-200 text-gray-600 bg-gray-50">Programmato</span>
								<button onclick={() => eliminaCorso(corso.idCorso)} class="text-gray-300 hover:text-red-600"><Trash2 size={14} /></button>
							</div>
							<h3 class="font-extrabold text-[#1B4B6B] text-sm uppercase mb-3 line-clamp-2">{corso.titolo}</h3>
							<p class="text-[10px] font-bold text-gray-500 flex items-center gap-1.5 mb-1"><Calendar size={12}/> {formattaData(corso.dataOrario)}</p>
							<p class="text-[10px] font-bold text-gray-500 flex items-center gap-1.5"><User size={12}/> {corso.emailDocente || 'N/D'}</p>

							<div class="mt-4 flex flex-col gap-2">
								<button onclick={() => cambiaStatoCorso(corso.idCorso, StatoCorso.IN_SVOLGIMENTO)} class="w-full py-2.5 border-2 border-[#1B4B6B] text-[#1B4B6B] rounded-xl font-bold uppercase text-[10px] tracking-widest flex items-center justify-center gap-2 hover:bg-[#1B4B6B] hover:text-white transition-colors">
									<Play size={14} fill="currentColor" /> Inizia Corso
								</button>
							</div>
						</div>
					{/each}
				</div>
			{/if}
		</div>

		<!-- ARCHIVIO STORICO CERTIFICATI -->
		<div class="mb-14">
			<div class="flex items-center gap-3 mb-6 border-b border-gray-100 pb-3 opacity-60 hover:opacity-100 transition-opacity">
				<div class="p-2 bg-purple-100 text-purple-700 rounded-lg"><BookOpen size={20}/></div>
				<h2 class="text-xl font-extrabold text-[#1B4B6B] uppercase tracking-tight">Archivio Corsi Certificati</h2>
			</div>

			{#if corsiArchiviati.length === 0}
				<p class="text-[10px] font-bold text-gray-300 uppercase italic">Nessun corso archiviato corrisponde alla ricerca.</p>
			{:else}
				<div class="grid grid-cols-1 md:grid-cols-2 xl:grid-cols-4 gap-6">
					{#each corsiArchiviati as corso (corso.idCorso)}
						<div class="bg-gray-50/50 rounded-2xl border border-gray-200 p-5 flex flex-col justify-between">
							<span class="text-[9px] font-black px-2 py-0.5 rounded uppercase border border-gray-300 text-gray-500 bg-white w-fit mb-3">CERTIFICATO</span>
							<h3 class="font-bold text-[#1B4B6B] text-xs uppercase line-clamp-1 mb-4">{corso.titolo}</h3>
							<div class="mt-auto pt-4 flex flex-col gap-2 border-t border-gray-200">
								<button onclick={() => apriStatisticheFeedback(corso)} class="w-full py-2 bg-white text-purple-700 border border-purple-200 rounded-xl font-bold uppercase text-[10px] tracking-widest flex items-center justify-center gap-2 hover:bg-purple-50 transition-colors shadow-sm">
									<BarChart3 size={12} /> Vedi Feedback
								</button>
							</div>
						</div>
					{/each}
				</div>
			{/if}
		</div>
	{/if}
</div>

<!-- MODALE NUOVO CORSO -->
{#if showModalNuovo}
	<div class="fixed inset-0 z-[100] flex items-center justify-center p-4 bg-black/60 backdrop-blur-sm" in:fade>
		<div class="bg-white w-full max-w-2xl rounded-3xl shadow-2xl flex flex-col max-h-[90vh]" in:scale>
			<div class="bg-[#1B4B6B] p-6 text-white flex justify-between items-center rounded-t-3xl">
				<h2 class="text-lg font-extrabold uppercase">Programma Nuovo Corso</h2>
				<button onclick={() => showModalNuovo = false} class="hover:text-red-400"><X size={24} /></button>
			</div>
			<div class="p-8 overflow-y-auto custom-scrollbar flex-1 bg-gray-50/30">
				<div class="grid grid-cols-1 md:grid-cols-2 gap-6">
					<div class="col-span-2 space-y-2">
						<label class="text-[10px] font-bold text-gray-400 uppercase">Titolo del Corso *</label>
						<input bind:value={formCorso.titolo} type="text" class="w-full px-4 py-3 bg-white border border-gray-200 rounded-xl font-bold uppercase text-xs" />
					</div>
					<div class="space-y-2">
						<label class="text-[10px] font-bold text-gray-400 uppercase">Inizio Sessione *</label>
						<input bind:value={formCorso.dataOrario} type="datetime-local" class="w-full px-4 py-3 bg-white border border-gray-200 rounded-xl font-bold text-xs" />
					</div>
					<div class="space-y-2">
						<label class="text-[10px] font-bold text-gray-400 uppercase">Sede Aula *</label>
						<input bind:value={formCorso.luogoFisico} type="text" class="w-full px-4 py-3 bg-white border border-gray-200 rounded-xl font-bold uppercase text-xs" />
					</div>
					<div class="col-span-2 space-y-2 pt-2">
						<label class="text-[10px] font-bold text-gray-400 uppercase">Assegnazione Docente *</label>
						<select bind:value={formCorso.idDocente} class="w-full px-4 py-3 bg-white border border-gray-200 rounded-xl font-bold uppercase text-xs cursor-pointer">
							<option value={undefined} disabled>-- Seleziona un docente --</option>
							{#each docenti as docente (docente.idUtente)}
								<option value={docente.idUtente}>{docente.nome} {docente.cognome}</option>
							{/each}
						</select>
					</div>
				</div>
			</div>
			<div class="p-6 bg-white rounded-b-3xl border-t border-gray-100 flex gap-4">
				<button onclick={() => showModalNuovo = false} class="flex-1 py-3 text-gray-400 font-extrabold rounded-xl border border-gray-200 hover:bg-gray-50 uppercase text-[10px]">Annulla</button>
				<button onclick={salvaNuovoCorso} disabled={!isFormValid || isSaving} class="flex-1 py-3 bg-[#1B4B6B] text-white font-extrabold rounded-xl hover:bg-blue-800 uppercase text-[10px] disabled:opacity-50">
					{isSaving ? 'Creazione...' : 'Salva Corso'}
				</button>
			</div>
		</div>
	</div>
{/if}

<!-- MODALE MATERIALE -->
{#if showModalMateriale}
	<div class="fixed inset-0 z-[100] flex items-center justify-center p-4 bg-black/60 backdrop-blur-sm" in:fade>
		<div class="bg-white w-full max-w-lg rounded-3xl shadow-2xl flex flex-col" in:scale>
			<div class="bg-[#1B4B6B] p-6 text-white flex justify-between items-center rounded-t-3xl">
				<h2 class="text-lg font-extrabold uppercase flex items-center gap-2"><UploadCloud size={20}/> Materiale Didattico</h2>
				<button onclick={() => showModalMateriale = false} class="hover:text-red-400"><X size={24} /></button>
			</div>
			<div class="p-8 space-y-6">
				<div>
					<label class="block text-[10px] font-bold text-[#1B4B6B] uppercase mb-1">Titolo Documento *</label>
					<input bind:value={titoloMateriale} type="text" placeholder="Es. Slide Lezione 1" class="w-full p-3 bg-gray-50 border border-gray-200 rounded-xl text-sm font-bold uppercase focus:outline-none focus:ring-2 focus:ring-[#1B4B6B]/20" />
				</div>
				<div class="border-2 border-dashed border-gray-200 rounded-2xl p-8 text-center bg-gray-50 group hover:bg-gray-100 transition-colors relative cursor-pointer">
					<input type="file" onchange={(e) => fileMateriale = e.currentTarget.files?.[0] || null} class="absolute inset-0 w-full h-full opacity-0 cursor-pointer" />
					<div class="flex flex-col items-center gap-3 pointer-events-none">
						<div class="p-4 bg-white rounded-full text-[#1B4B6B] shadow-sm">
							<UploadCloud size={24} />
						</div>
						{#if fileMateriale}
							<span class="text-xs font-black text-[#1B4B6B] truncate w-full px-4">{fileMateriale.name}</span>
							<span class="text-[9px] font-bold text-green-500 uppercase tracking-widest">Pronto per il caricamento</span>
						{:else}
							<span class="text-[10px] font-black text-gray-400 uppercase tracking-widest">Clicca o trascina il file qui</span>
						{/if}
					</div>
				</div>
			</div>
			<div class="p-6 bg-gray-50 rounded-b-3xl border-t border-gray-100 flex gap-4">
				<button onclick={() => showModalMateriale = false} class="flex-1 py-3 text-gray-400 font-extrabold rounded-xl border border-gray-200 hover:bg-white uppercase text-[10px] transition-colors">Annulla</button>
				<button onclick={caricaMaterialeDidattico} disabled={isUploadingMateriale || !fileMateriale || !titoloMateriale.trim()} class="flex-1 py-3 bg-[#1B4B6B] text-white font-extrabold rounded-xl hover:bg-[#153a54] uppercase text-[10px] shadow-lg shadow-blue-900/20 disabled:opacity-50 flex items-center justify-center gap-2">
					{#if isUploadingMateriale} <Loader2 class="animate-spin" size={16} /> Attendi... {:else} Conferma Upload {/if}
				</button>
			</div>
		</div>
	</div>
{/if}

<!-- MODALE PRESENZE -->
{#if showModalPresenze}
	<div class="fixed inset-0 z-[100] flex items-center justify-center p-4 bg-black/60 backdrop-blur-sm" in:fade>
		<div class="bg-white w-full max-w-2xl rounded-3xl shadow-2xl flex flex-col max-h-[90vh]" in:scale>
			<div class="bg-[#1B4B6B] p-6 text-white flex justify-between items-center rounded-t-3xl">
				<h2 class="text-lg font-extrabold uppercase">Validazione Presenze: {selectedCorso?.titolo}</h2>
				<button onclick={() => showModalPresenze = false} class="hover:text-red-400"><X size={24} /></button>
			</div>
			<div class="p-6 overflow-y-auto custom-scrollbar flex-1 bg-gray-50/30">
				{#if isActionLoading}
					<div class="py-10 text-center"><Loader2 class="animate-spin mx-auto text-[#1B4B6B]" size={32} /></div>
				{:else}
					<p class="text-xs font-bold text-gray-500 mb-6 uppercase">Seleziona i dipendenti che hanno effettivamente partecipato al corso.</p>
					<div class="space-y-2">
						{#each iscrizioniAttuali as isc}
							<label class="flex items-center justify-between p-4 bg-white border border-gray-200 rounded-xl cursor-pointer hover:border-[#1B4B6B] transition-colors">
								<div class="flex items-center gap-4">
									<input type="checkbox" checked={presenzeSelezionate.includes(isc.idUtente)} onchange={() => togglePresenza(isc.idUtente)} class="w-5 h-5 accent-[#1B4B6B] rounded border-gray-300">
									<p class="text-sm font-extrabold text-[#1B4B6B] uppercase">{isc.emailUtente}</p>
								</div>
								<span class="text-[10px] font-bold text-gray-400 uppercase px-2 py-1 bg-gray-100 rounded">Dipendente #{isc.idUtente}</span>
							</label>
						{/each}
					</div>
				{/if}
			</div>
			<div class="p-6 bg-white rounded-b-3xl border-t border-gray-100">
				<button onclick={confermaPresenze} disabled={isActionLoading} class="w-full py-4 bg-[#1B4B6B] text-white font-extrabold rounded-xl hover:bg-blue-800 uppercase text-xs shadow-lg shadow-blue-900/20 disabled:opacity-50">
					Chiudi Registro e Invia a Docente
				</button>
			</div>
		</div>
	</div>
{/if}

<!-- MODALE UPLOAD ATTESTATI -->
{#if showModalUpload}
	<div class="fixed inset-0 z-[100] flex items-center justify-center p-4 bg-black/60 backdrop-blur-sm" in:fade>
		<div class="bg-white w-full max-w-3xl rounded-3xl shadow-2xl flex flex-col max-h-[90vh]" in:scale>
			<div class="bg-emerald-600 p-6 text-white flex justify-between items-center rounded-t-3xl">
				<h2 class="text-lg font-extrabold uppercase">Distribuzione Attestati Aziendali</h2>
				<button onclick={() => showModalUpload = false} class="hover:text-emerald-200"><X size={24} /></button>
			</div>
			<div class="p-6 overflow-y-auto custom-scrollbar flex-1 bg-gray-50/30">
				{#if isActionLoading && aziendeCoinvolte.length === 0}
					<div class="py-10 text-center"><Loader2 class="animate-spin mx-auto text-emerald-600" size={32} /></div>
				{:else}
					<p class="text-xs font-bold text-gray-500 mb-6 uppercase">Carica un certificato cumulativo in PDF per ciascuna azienda.</p>

					{#if aziendeCoinvolte.length === 0}
						<div class="p-6 bg-amber-50 border border-amber-200 rounded-xl text-center">
							<p class="text-amber-700 font-bold uppercase text-xs">Attenzione: Non è stata rilevata nessuna azienda valida per i dipendenti presenti.</p>
						</div>
					{/if}

					<div class="space-y-4 mt-4">
						{#each aziendeCoinvolte as azienda (azienda.idAzienda)}
							<div class="p-5 bg-white border border-gray-200 rounded-2xl flex flex-col md:flex-row md:items-center justify-between gap-4">
								<div class="flex items-start gap-4">
									<div class="p-3 bg-gray-100 rounded-xl text-gray-500"><Building2 size={20} /></div>
									<div>
										<h3 class="text-sm font-extrabold text-[#1B4B6B] uppercase">{azienda.ragioneSociale}</h3>
										<p class="text-[10px] font-bold text-emerald-600 flex items-center gap-1.5 mt-1 uppercase">
											<Users size={12} /> Copre {azienda.count} dipendente/i validato/i
										</p>
									</div>
								</div>
								<div class="shrink-0">
									<input type="file" accept="application/pdf" onchange={(e) => handleFileChange(e, azienda.idAzienda)} class="block w-full text-xs text-gray-500 file:mr-4 file:py-2 file:px-4 file:rounded-xl file:border-0 file:text-[10px] file:font-extrabold file:uppercase file:bg-emerald-50 file:text-emerald-700 hover:file:bg-emerald-100 transition-colors" />
								</div>
							</div>
						{/each}
					</div>
				{/if}
			</div>
			<div class="p-6 bg-white rounded-b-3xl border-t border-gray-100 flex gap-4">
				<button onclick={confermaDistribuzioneAttestati} disabled={isActionLoading || aziendeCoinvolte.length === 0} class="w-full py-4 bg-emerald-600 text-white font-extrabold rounded-xl hover:bg-emerald-700 uppercase text-xs shadow-lg shadow-emerald-900/20 disabled:opacity-50 flex justify-center items-center gap-2">
					{#if isActionLoading} <Loader2 class="animate-spin" size={16} /> Elaborazione... {:else} Invia Documenti alle Aziende {/if}
				</button>
			</div>
		</div>
	</div>
{/if}

<!-- MODALE FEEDBACK STATS -->
{#if showModalFeedback && selectedCorso}
	<div class="fixed inset-0 z-[150] flex items-center justify-center p-4 bg-black/60 backdrop-blur-sm" in:fade>
		<div class="bg-white w-full max-w-2xl rounded-[2.5rem] shadow-2xl flex flex-col max-h-[90vh] overflow-hidden" in:scale>
			<div class="bg-purple-600 p-8 text-white flex justify-between items-center">
				<div>
					<h2 class="text-xl font-black uppercase tracking-tighter flex items-center gap-3">
						<BarChart3 size={24}/> Analisi Qualità Corso
					</h2>
					<p class="text-[10px] font-bold uppercase opacity-70 mt-1">{selectedCorso.titolo}</p>
				</div>
				<button onclick={() => showModalFeedback = false} class="hover:rotate-90 transition-transform"><X size={28} /></button>
			</div>

			<div class="p-8 overflow-y-auto custom-scrollbar flex-1 bg-gray-50/30">
				{#if isLoadingFeedback}
					<div class="py-20 text-center flex flex-col items-center gap-4">
						<Loader2 class="animate-spin text-purple-600" size={48} />
						<span class="text-[10px] font-black uppercase tracking-widest text-gray-400">Elaborazione metriche in corso...</span>
					</div>
				{:else if statsFeedback}
					{#if statsFeedback.totaleFeedback === 0}
						<div class="py-20 text-center border-2 border-dashed border-gray-200 rounded-3xl bg-white">
							<Star size={48} class="mx-auto text-gray-200 mb-4" />
							<p class="text-xs font-bold text-gray-400 uppercase tracking-widest">Nessun dipendente ha ancora rilasciato feedback.</p>
						</div>
					{:else}
						<div class="grid grid-cols-1 md:grid-cols-2 gap-6 mb-10">
							<div class="bg-white p-8 rounded-3xl border border-gray-100 shadow-sm text-center">
								<p class="text-[10px] font-black text-gray-400 uppercase tracking-widest mb-2">Media Docenza</p>
								<div class="flex items-baseline justify-center gap-1">
									<span class="text-6xl font-black text-purple-600">{statsFeedback.mediaDocenza.toFixed(1)}</span>
									<span class="text-xl font-bold text-gray-300">/5</span>
								</div>
								<div class="flex justify-center gap-1 mt-4">
									{#each [1,2,3,4,5] as s}
										<Star size={16} fill={statsFeedback.mediaDocenza >= s ? "#EAB308" : "none"} class={statsFeedback.mediaDocenza >= s ? "text-yellow-400" : "text-gray-200"} />
									{/each}
								</div>
							</div>
							<div class="bg-white p-8 rounded-3xl border border-gray-100 shadow-sm text-center">
								<p class="text-[10px] font-black text-gray-400 uppercase tracking-widest mb-2">Media Contenuti</p>
								<div class="flex items-baseline justify-center gap-1">
									<span class="text-6xl font-black text-purple-600">{statsFeedback.mediaContenuti.toFixed(1)}</span>
									<span class="text-xl font-bold text-gray-300">/5</span>
								</div>
								<div class="flex justify-center gap-1 mt-4">
									{#each [1,2,3,4,5] as s}
										<Star size={16} fill={statsFeedback.mediaContenuti >= s ? "#EAB308" : "none"} class={statsFeedback.mediaContenuti >= s ? "text-yellow-400" : "text-gray-200"} />
									{/each}
								</div>
							</div>
						</div>

						<div class="flex items-center justify-between mb-6">
							<h3 class="text-sm font-black text-[#1B4B6B] uppercase tracking-widest">Commenti dei Partecipanti</h3>
							<span class="bg-purple-100 text-purple-700 text-[10px] font-black px-4 py-1.5 rounded-full uppercase">
                                {statsFeedback.totaleFeedback} Risposte
                            </span>
						</div>

						<div class="space-y-4">
							{#each statsFeedback.commenti as commento}
								<div class="bg-white p-5 rounded-2xl border border-gray-100 shadow-sm flex gap-4 items-start">
									<div class="p-3 bg-gray-50 rounded-xl text-gray-400 shrink-0"><User size={16}/></div>
									<p class="text-xs font-medium text-gray-600 leading-relaxed italic mt-1">"{commento}"</p>
								</div>
							{/each}
						</div>
					{/if}
				{/if}
			</div>

			<div class="p-8 bg-white border-t border-gray-100">
				<button onclick={() => showModalFeedback = false} class="w-full py-4 bg-gray-900 text-white font-black rounded-2xl uppercase text-[10px] tracking-widest hover:bg-gray-800 transition-colors shadow-lg">
					Chiudi Analisi
				</button>
			</div>
		</div>
	</div>
{/if}

<style>
	:global(body) { background-color: #F9FAFB; }
	.custom-scrollbar::-webkit-scrollbar { width: 5px; }
	.custom-scrollbar::-webkit-scrollbar-thumb { background: #E2E8F0; border-radius: 10px; }
	.animate-spin-slow { animation: spin 3s linear infinite; }
</style>