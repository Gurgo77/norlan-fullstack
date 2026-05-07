<script lang="ts">
	import { onMount } from 'svelte';
	import { fade, scale } from 'svelte/transition';
	import { Plus, X, Trash2, Search, Calendar, MapPin, Loader2, User, CheckSquare, UploadCloud, CheckCircle2, Clock, Building2, Users, BarChart, Star, BookOpen } from 'lucide-svelte';

	import { CorsoFormazione } from '$lib/models/CorsoFormazione';
	import { Docente, type DocenteData } from '$lib/models/Docente';
	import { StatoCorso } from '$lib/models/Enums';
	import type { CorsoFormazioneRequest } from '$lib/models/CorsoFormazioneRequest';
	import type { IscrizioneCorso } from '$lib/models/IscrizioneCorso';
	import { FormazioneService } from '$lib/services/FormazioneService';
	import { AnagraficaService } from '$lib/services/AnagraficaService';
	import { FeedbackService } from '$lib/services/FeedbackService';
	import type { DipendenteDTO } from '$lib/services/LavoratoreService';
	import { SvelteMap } from 'svelte/reactivity';
	import StatCard from '$lib/Components/UI/StatCard.svelte';
	import AlertCard from '$lib/Components/UI/AlertCard.svelte';
	import DashboardCorsoCard from '$lib/Components/Features/Formazione/DashboardCorsoCard.svelte';

	interface FeedbackStatsDTO {
		idCorso: number;
		mediaDocenza: number;
		mediaContenuti: number;
		totaleFeedback: number;
		commenti: string[];
	}

	let corsi = $state<CorsoFormazione[]>([]);
	let docenti = $state<Docente[]>([]);
	let dipendenti = $state<DipendenteDTO[]>([]);
	let isLoading = $state(true);
	let isSaving = $state(false);
	let searchQuery = $state('');

	let showModalNuovo = $state(false);
	let showModalPresenze = $state(false);
	let showModalUpload = $state(false);
	let showModalFeedback = $state(false);
	let showModalElimina = $state(false);

	let selectedCorso = $state<CorsoFormazione | null>(null);
	let idCorsoDaEliminare = $state<number | null>(null);
	let iscrizioniAttuali = $state<IscrizioneCorso[]>([]);
	let presenzeSelezionate = $state<number[]>([]);
	let fileUploads = $state<Record<number, File>>({});
	let isActionLoading = $state(false);

	let isLoadingFeedback = $state(false);
	let statsFeedback = $state<FeedbackStatsDTO | null>(null);

	let formCorso = $state<Partial<CorsoFormazioneRequest>>({
		titolo: '', dataOrario: '', luogoFisico: '', idDocente: undefined
	});

	const isFormValid = $derived(
			!!formCorso.titolo && !!formCorso.dataOrario && !!formCorso.luogoFisico && !!formCorso.idDocente
	);

	const corsiAttiviAdmin = $derived(corsi.filter(c => c.stato !== StatoCorso.CERTIFICATO));
	const corsiFiltrati = $derived(corsiAttiviAdmin.filter(c => (c.titolo || '').toLowerCase().includes(searchQuery.toLowerCase())));

	const corsiConclusi = $derived(corsiFiltrati.filter(c => c.stato === StatoCorso.CONCLUSO || c.stato === StatoCorso.ATTESA_FIRMA_DOCENTE || c.stato === StatoCorso.VALIDATO));
	const corsiInSvolgimento = $derived(corsiFiltrati.filter(c => c.stato === StatoCorso.IN_SVOLGIMENTO));
	const corsiProgrammati = $derived(corsiFiltrati.filter(c => c.stato === StatoCorso.PROGRAMMATO || !c.stato));
	const corsiArchiviati = $derived(corsi.filter(c => c.stato === StatoCorso.CERTIFICATO && (c.titolo || '').toLowerCase().includes(searchQuery.toLowerCase())));

	const aziendeCoinvolte = $derived.by(() => {
		if (!selectedCorso || iscrizioniAttuali.length === 0 || dipendenti.length === 0) return [];
		const presenti = iscrizioniAttuali.filter(i => i.presenzaConfermata);
		const map = new SvelteMap<number, { idAzienda: number, ragioneSociale: string, count: number }>();

		presenti.forEach(isc => {
			const dip = dipendenti.find(d => d.idUtente === isc.idUtente);
			if (!dip) return;

			let idAzienda = null;
			let nomeAzienda = "Azienda Sconosciuta";
			const dipConAzienda = dip as DipendenteDTO & { azienda?: { idUtente?: number, id?: number, ragioneSociale?: string, nome?: string }, aziendaId?: number, aziendaRagioneSociale?: string, idAzienda?: number, ragioneSocialeAzienda?: string };

			if (dipConAzienda.azienda && typeof dipConAzienda.azienda === 'object') {
				idAzienda = dipConAzienda.azienda.idUtente || dipConAzienda.azienda.id;
				nomeAzienda = dipConAzienda.azienda.ragioneSociale || dipConAzienda.azienda.nome || nomeAzienda;
			} else if (dipConAzienda.aziendaId) {
				idAzienda = dipConAzienda.aziendaId;
				nomeAzienda = dipConAzienda.aziendaRagioneSociale || `Azienda ID ${idAzienda}`;
			} else if (dipConAzienda.idAzienda) {
				idAzienda = dipConAzienda.idAzienda;
				nomeAzienda = dipConAzienda.ragioneSocialeAzienda || `Azienda ID ${idAzienda}`;
			}

			if (idAzienda) {
				if (!map.has(idAzienda)) {
					map.set(idAzienda, { idAzienda: idAzienda, ragioneSociale: nomeAzienda, count: 0 });
				}
				map.get(idAzienda)!.count++;
			}
		});
		return Array.from(map.values());
	});

	onMount(async () => {
		try {
			const [corsiRes, docentiRes, dipendentiRes] = await Promise.all([
				FormazioneService.getAllCorsi(),
				AnagraficaService.getAllDocenti(),
				AnagraficaService.getAllDipendenti()
			]);
			corsi = corsiRes;
			docenti = (docentiRes as DocenteData[]).map(d => new Docente(d));
			dipendenti = dipendentiRes as DipendenteDTO[];
		} catch (error) {
			console.error(error);
		} finally {
			isLoading = false;
		}
	});

	async function salvaNuovoCorso() {
		if (!isFormValid) return;
		isSaving = true;
		try {
			const payload = {
				titolo: formCorso.titolo!,
				dataOrario: new Date(formCorso.dataOrario!).toISOString(),
				luogoFisico: formCorso.luogoFisico!,
				idDocente: formCorso.idDocente!,
				capacitaMassima: 50
			} as CorsoFormazioneRequest;

			const nuovo = await FormazioneService.createCorso(payload);
			corsi = [...corsi, nuovo];
			showModalNuovo = false;
			formCorso = { titolo: '', dataOrario: '', luogoFisico: '', idDocente: undefined };
		} catch (error) {
			const err = error as { response?: { status?: number } };
			if (err.response?.status === 409) {
				alert("Impossibile creare il corso: i dati inseriti sono in conflitto.");
			} else {
				alert("Si è verificato un errore durante la programmazione del corso.");
			}
		} finally {
			isSaving = false;
		}
	}

	function preparaEliminaCorso(id: number) {
		idCorsoDaEliminare = id;
		showModalElimina = true;
	}

	async function confermaEliminaCorso() {
		if (idCorsoDaEliminare === null) return;
		try {
			await FormazioneService.deleteCorso(idCorsoDaEliminare);
			corsi = corsi.filter(c => c.idCorso !== idCorsoDaEliminare);
			showModalElimina = false;
			idCorsoDaEliminare = null;
		} catch (error) {
			alert("Si è verificato un errore durante l'eliminazione.");
		}
	}

	async function apriValidazione(corso: CorsoFormazione) {
		selectedCorso = corso;
		isActionLoading = true;
		showModalPresenze = true;
		try {
			iscrizioniAttuali = await FormazioneService.getIscrizioniByCorso(corso.idCorso);
			presenzeSelezionate = iscrizioniAttuali.filter(i => i.presenzaConfermata).map(i => i.idUtente);
		} catch {
			alert("Errore durante il caricamento del registro iscritti.");
			showModalPresenze = false;
		} finally {
			isActionLoading = false;
		}
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
			corsi = corsi.map(c => c.idCorso === selectedCorso!.idCorso ? { ...c, stato: StatoCorso.ATTESA_FIRMA_DOCENTE } as CorsoFormazione : c);
			showModalPresenze = false;
		} catch (error) {
			alert("Si è verificato un errore durante la validazione del registro.");
		} finally {
			isActionLoading = false;
		}
	}

	async function apriUploadAttestati(corso: CorsoFormazione) {
		selectedCorso = corso;
		isActionLoading = true;
		showModalUpload = true;
		fileUploads = {};
		try {
			iscrizioniAttuali = await FormazioneService.getIscrizioniByCorso(corso.idCorso);
		} catch {
			alert("Errore durante il caricamento dei dati.");
			showModalUpload = false;
		} finally {
			isActionLoading = false;
		}
	}

	function handleFileChange(event: Event, idAzienda: number) {
		const input = event.target as HTMLInputElement;
		if (input.files && input.files.length > 0) {
			fileUploads[idAzienda] = input.files[0];
		} else {
			delete fileUploads[idAzienda];
		}
	}

	async function confermaDistribuzioneAttestati() {
		if (!selectedCorso) return;
		const missing = aziendeCoinvolte.some(a => !fileUploads[a.idAzienda]);
		if (missing) {
			alert("È necessario caricare un certificato PDF per tutte le aziende elencate.");
			return;
		}

		isActionLoading = true;
		try {
			const payload = aziendeCoinvolte.map(a => ({ idAzienda: a.idAzienda, file: fileUploads[a.idAzienda] }));
			await FormazioneService.distribuisciAttestati(selectedCorso.idCorso, payload);
			alert("Attestati distribuiti correttamente.");
			corsi = corsi.map(c => c.idCorso === selectedCorso!.idCorso ? { ...c, stato: StatoCorso.CERTIFICATO } as CorsoFormazione : c);
			showModalUpload = false;
		} catch (error) {
			alert("Si è verificato un errore durante il caricamento degli attestati.");
		} finally {
			isActionLoading = false;
		}
	}

	async function apriStatisticheFeedback(corso: CorsoFormazione) {
		selectedCorso = corso;
		showModalFeedback = true;
		isLoadingFeedback = true;
		statsFeedback = null;
		try {
			statsFeedback = await FeedbackService.getStatisticheCorso(corso.idCorso);
		} catch {
			alert("Errore durante il caricamento dei feedback.");
			showModalFeedback = false;
		} finally {
			isLoadingFeedback = false;
		}
	}

	function formattaData(isoString: string) {
		if (!isoString) return '';
		const d = new Date(isoString);
		return d.toLocaleDateString('it-IT', { day: '2-digit', month: 'short', year: 'numeric' });
	}
</script>

<div in:fade class="pb-20">
	<div class="mb-10 flex justify-between items-start">
		<div>
			<h1 class="text-4xl font-extrabold text-[#1B4B6B]">GESTIONE FORMAZIONE</h1>
			<p class="text-gray-500 font-bold uppercase text-xs tracking-tighter">Pianificazione corsi, aule e docenze.</p>
		</div>
		<button onclick={() => showModalNuovo = true} class="bg-white text-[#1B4B6B] border-2 border-[#1B4B6B] px-8 py-3.5 rounded-xl font-extrabold uppercase text-xs shadow-lg hover:bg-[#1B4B6B] hover:text-white transition-all flex items-center gap-3">
			<Plus size={18} /> Programma Corso
		</button>
	</div>

	<div class="mb-8 relative w-72 group">
		<Search class="absolute left-3 top-1/2 -translate-y-1/2 text-gray-400 group-focus-within:text-[#1B4B6B] transition-colors" size={16} />
		<input bind:value={searchQuery} type="text" placeholder="Cerca per titolo..." class="w-full pl-10 pr-4 py-2.5 bg-white border border-gray-200 rounded-xl text-xs focus:ring-2 focus:ring-[#1B4B6B] outline-none transition-all font-bold uppercase shadow-sm" />
	</div>

	{#if isLoading}
		<div class="py-20 text-center">
			<Loader2 size={40} class="animate-spin mx-auto text-[#1B4B6B]" />
		</div>
	{:else}

		<div class="mb-14">
			<div class="flex flex-col md:flex-row items-start md:items-center justify-between gap-4 mb-6 border-b border-gray-200 pb-4">
				<div class="flex items-center gap-3">
					<div class="p-2 bg-emerald-100 text-emerald-700 rounded-lg">
						<CheckCircle2 size={20}/>
					</div>
					<h2 class="text-xl font-extrabold text-[#1B4B6B] uppercase tracking-tight">Corsi Conclusi & Validazioni</h2>
				</div>
				<div class="w-full md:w-auto">
					<StatCard titolo="Da Processare" valore={corsiConclusi.length} icona={CheckSquare} bgIcona="bg-emerald-50" testoIcona="text-emerald-600" />
				</div>
			</div>

			{#if corsiConclusi.length === 0}
				<div class="p-8 border-2 border-dashed border-gray-200 rounded-2xl text-center text-gray-400 font-bold uppercase text-xs">
					Nessun corso concluso da processare.
				</div>
			{:else}
				<div class="grid grid-cols-1 md:grid-cols-2 xl:grid-cols-3 gap-6">
					{#each corsiConclusi as corso (corso.idCorso)}
						<div class="flex flex-col gap-3" in:scale>
							<AlertCard
									titolo={corso.titolo || 'Nessun Titolo'}
									sottotitolo="ID: #{corso.idCorso}"
									variante={corso.stato === StatoCorso.VALIDATO ? 'success' : 'warning'}
									icona={corso.stato === StatoCorso.VALIDATO ? CheckCircle2 : (corso.stato === StatoCorso.ATTESA_FIRMA_DOCENTE ? Clock : CheckSquare)}
									stato={corso.stato ? corso.stato.replace('_', ' ') : 'SCONOSCIUTO'}
									data={formattaData(corso.dataOrario)}
							/>
							<div class="p-4 bg-white rounded-2xl border border-gray-100 shadow-sm flex flex-col gap-2">
								{#if corso.stato === StatoCorso.CONCLUSO}
									<button onclick={() => apriValidazione(corso)} class="w-full py-2.5 bg-[#1B4B6B] text-white rounded-xl font-bold uppercase text-[10px] tracking-widest flex items-center justify-center gap-2 hover:bg-blue-800 transition-colors shadow-lg shadow-blue-900/20">
										<CheckSquare size={14} /> Valida Presenze
									</button>
								{:else if corso.stato === StatoCorso.ATTESA_FIRMA_DOCENTE}
									<div class="w-full py-2.5 bg-gray-200 text-gray-500 rounded-xl font-bold uppercase text-[10px] tracking-widest flex items-center justify-center gap-2 cursor-not-allowed">
										<Clock size={14} /> In Attesa Firma Docente
									</div>
								{:else if corso.stato === StatoCorso.VALIDATO}
									<button onclick={() => apriUploadAttestati(corso)} class="w-full py-2.5 bg-emerald-600 text-white rounded-xl font-bold uppercase text-[10px] tracking-widest flex items-center justify-center gap-2 hover:bg-emerald-700 transition-colors shadow-lg shadow-emerald-900/20">
										<UploadCloud size={14} /> Genera & Invia Attestati
									</button>
								{/if}
								<button onclick={() => apriStatisticheFeedback(corso)} class="w-full py-2.5 mt-1 bg-[#1B4B6B]/5 text-[#1B4B6B] rounded-xl font-bold uppercase text-[10px] tracking-widest flex items-center justify-center gap-2 hover:bg-[#1B4B6B]/10 transition-all shadow-sm">
									<BarChart size={14} /> Metriche Feedback
								</button>
							</div>
						</div>
					{/each}
				</div>
			{/if}
		</div>

		<div class="mb-14">
			<div class="flex items-center gap-3 mb-6 border-b border-gray-200 pb-3">
				<div class="p-2 bg-blue-100 text-blue-700 rounded-lg">
					<Loader2 size={20} class="animate-spin-slow"/>
				</div>
				<h2 class="text-xl font-extrabold text-[#1B4B6B] uppercase tracking-tight">Corsi In Svolgimento</h2>
			</div>

			{#if corsiInSvolgimento.length === 0}
				<div class="p-8 border-2 border-dashed border-gray-200 rounded-2xl text-center text-gray-400 font-bold uppercase text-xs">
					Nessun corso attualmente in aula.
				</div>
			{:else}
				<div class="grid grid-cols-1 md:grid-cols-2 xl:grid-cols-3 gap-6">
					{#each corsiInSvolgimento as corso (corso.idCorso)}
						<div in:scale>
							<DashboardCorsoCard
									ruolo="admin"
									corso={{
                                    id: corso.idCorso,
                                    titolo: corso.titolo,
                                    stato: 'IN_SVOLGIMENTO',
                                    dataSvolgimento: formattaData(corso.dataOrario),
                                    luogo: corso.luogoFisico
                                }}
							/>
						</div>
					{/each}
				</div>
			{/if}
		</div>

		<div class="mb-14">
			<div class="flex items-center gap-3 mb-6 border-b border-gray-200 pb-3">
				<div class="p-2 bg-gray-100 text-gray-700 rounded-lg">
					<Calendar size={20}/>
				</div>
				<h2 class="text-xl font-extrabold text-[#1B4B6B] uppercase tracking-tight">Corsi Programmati</h2>
			</div>

			{#if corsiProgrammati.length === 0}
				<div class="p-8 border-2 border-dashed border-gray-200 rounded-2xl text-center text-gray-400 font-bold uppercase text-xs">
					Nessun corso futuro programmato.
				</div>
			{:else}
				<div class="grid grid-cols-1 md:grid-cols-2 xl:grid-cols-4 gap-6">
					{#each corsiProgrammati as corso (corso.idCorso)}
						<div in:scale>
							<DashboardCorsoCard
									ruolo="admin"
									corso={{
                                    id: corso.idCorso,
                                    titolo: corso.titolo,
                                    stato: 'DA_INIZIARE',
                                    dataSvolgimento: formattaData(corso.dataOrario),
                                    luogo: corso.luogoFisico
                                }}
									onEliminaCorso={() => preparaEliminaCorso(corso.idCorso)}
							/>
						</div>
					{/each}
				</div>
			{/if}
		</div>

		<div class="mb-14">
			<div class="flex items-center gap-3 mb-6 border-b border-gray-200 pb-3 opacity-60 hover:opacity-100 transition-opacity">
				<div class="p-2 bg-[#1B4B6B]/10 text-[#1B4B6B] rounded-lg">
					<BookOpen size={20}/>
				</div>
				<h2 class="text-xl font-extrabold text-[#1B4B6B] uppercase tracking-tight">Archivio Corsi Certificati</h2>
			</div>

			{#if corsiArchiviati.length === 0}
				<p class="text-[10px] font-bold text-gray-300 uppercase italic">Nessun corso archiviato.</p>
			{:else}
				<div class="grid grid-cols-1 md:grid-cols-2 xl:grid-cols-4 gap-6">
					{#each corsiArchiviati as corso (corso.idCorso)}
						<div in:scale>
							<DashboardCorsoCard
									ruolo="admin"
									corso={{
                                    id: corso.idCorso,
                                    titolo: corso.titolo,
                                    stato: 'COMPLETATO',
                                    dataSvolgimento: formattaData(corso.dataOrario),
                                    luogo: corso.luogoFisico
                                }}
									onAzioneCorso={() => apriStatisticheFeedback(corso)}
							/>
						</div>
					{/each}
				</div>
			{/if}
		</div>
	{/if}
</div>

{#if showModalNuovo}
	<div class="fixed inset-0 z-[100] flex items-center justify-center p-4 bg-black/60 backdrop-blur-sm" in:fade>
		<div class="bg-white w-full max-w-xl rounded-3xl shadow-2xl flex flex-col max-h-[90vh]" in:scale>
			<div class="bg-[#1B4B6B] p-6 text-white flex justify-between items-center rounded-t-3xl">
				<h2 class="text-lg font-extrabold uppercase">Programma Nuovo Corso</h2>
				<button onclick={() => showModalNuovo = false} class="text-white hover:rotate-90 transition-all duration-300">
					<X size={24} />
				</button>
			</div>
			<div class="p-8 overflow-y-auto custom-scrollbar flex-1 bg-gray-50/30">
				<div class="flex flex-col gap-6">
					<div class="space-y-2">
						<label class="text-[10px] font-bold text-gray-400 uppercase">Titolo del Corso *</label>
						<input bind:value={formCorso.titolo} type="text" class="w-full px-4 py-3 bg-white border border-gray-200 rounded-xl font-bold uppercase text-xs outline-none focus:border-[#1B4B6B]" />
					</div>
					<div class="space-y-2">
						<label class="text-[10px] font-bold text-gray-400 uppercase">Data di Inizio *</label>
						<input bind:value={formCorso.dataOrario} type="date" max="9999-12-31" class="w-full px-4 py-3 bg-white border border-gray-200 rounded-xl font-bold text-xs outline-none focus:border-[#1B4B6B]" />
					</div>
					<div class="space-y-2">
						<label class="text-[10px] font-bold text-gray-400 uppercase">Luogo Di Svolgimento *</label>
						<input bind:value={formCorso.luogoFisico} type="text" class="w-full px-4 py-3 bg-white border border-gray-200 rounded-xl font-bold uppercase text-xs outline-none focus:border-[#1B4B6B]" />
					</div>
					<div class="space-y-2">
						<label class="text-[10px] font-bold text-gray-400 uppercase">Assegnazione Docente *</label>
						<select bind:value={formCorso.idDocente} class="w-full px-4 py-3 bg-white border border-gray-200 rounded-xl font-bold uppercase text-xs cursor-pointer outline-none focus:border-[#1B4B6B]">
							<option value={undefined} disabled>-- Seleziona un docente --</option>
							{#each docenti as docente (docente.idUtente)}
								<option value={docente.idUtente}>{docente.nome} {docente.cognome}</option>
							{/each}
						</select>
					</div>
				</div>
			</div>
			<div class="p-6 bg-white rounded-b-3xl border-t border-gray-100 flex gap-4">
				<button onclick={() => showModalNuovo = false} class="flex-1 py-3 text-gray-400 font-extrabold rounded-xl border border-gray-200 hover:bg-gray-50 uppercase text-[10px] transition-colors">
					Annulla
				</button>
				<button onclick={salvaNuovoCorso} disabled={!isFormValid || isSaving} class="flex-1 py-3 bg-[#1B4B6B] text-white font-extrabold rounded-xl hover:bg-blue-800 uppercase text-[10px] disabled:opacity-50 transition-colors">
					{isSaving ? 'Creazione...' : 'Salva Corso'}
				</button>
			</div>
		</div>
	</div>
{/if}

{#if showModalPresenze}
	<div class="fixed inset-0 z-[100] flex items-center justify-center p-4 bg-black/60 backdrop-blur-sm" in:fade>
		<div class="bg-white w-full max-w-2xl rounded-3xl shadow-2xl flex flex-col max-h-[90vh]" in:scale>
			<div class="bg-[#1B4B6B] p-6 text-white flex justify-between items-center rounded-t-3xl">
				<h2 class="text-lg font-extrabold uppercase">Validazione Presenze: {selectedCorso?.titolo}</h2>
				<button onclick={() => showModalPresenze = false} class="text-white hover:rotate-90 transition-all duration-300">
					<X size={24} />
				</button>
			</div>
			<div class="p-6 overflow-y-auto custom-scrollbar flex-1 bg-gray-50/30">
				{#if isActionLoading}
					<div class="py-10 text-center">
						<Loader2 class="animate-spin mx-auto text-[#1B4B6B]" size={32} />
					</div>
				{:else}
					<p class="text-xs font-bold text-gray-500 mb-6 uppercase">Seleziona i partecipanti.</p>
					<div class="space-y-2">
						{#each iscrizioniAttuali as isc (isc.idUtente)}
							<label class="flex items-center justify-between p-4 bg-white border border-gray-200 rounded-xl cursor-pointer hover:border-[#1B4B6B] transition-colors">
								<div class="flex items-center gap-4">
									<input type="checkbox" checked={presenzeSelezionate.includes(isc.idUtente)} onchange={() => togglePresenza(isc.idUtente)} class="w-5 h-5 accent-[#1B4B6B] rounded border-gray-300">
									<div>
										<p class="text-sm font-extrabold text-[#1B4B6B] uppercase">{isc.emailUtente}</p>
									</div>
								</div>
								<span class="text-[10px] font-bold text-gray-400 uppercase px-2 py-1 bg-gray-100 rounded">ID #{isc.idUtente}</span>
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

{#if showModalUpload}
	<div class="fixed inset-0 z-[100] flex items-center justify-center p-4 bg-black/60 backdrop-blur-sm" in:fade>
		<div class="bg-white w-full max-w-3xl rounded-3xl shadow-2xl flex flex-col max-h-[90vh]" in:scale>
			<div class="bg-emerald-600 p-6 text-white flex justify-between items-center rounded-t-3xl">
				<h2 class="text-lg font-extrabold uppercase">Distribuzione Attestati Aziendali</h2>
				<button onclick={() => showModalUpload = false} class="text-white hover:rotate-90 transition-all duration-300">
					<X size={24} />
				</button>
			</div>
			<div class="p-6 overflow-y-auto custom-scrollbar flex-1 bg-gray-50/30">
				{#if isActionLoading && aziendeCoinvolte.length === 0}
					<div class="py-10 text-center">
						<Loader2 class="animate-spin mx-auto text-emerald-600" size={32} />
					</div>
				{:else}
					<p class="text-xs font-bold text-gray-500 mb-6 uppercase">Carica un certificato cumulativo in PDF per ciascuna azienda.</p>

					{#if aziendeCoinvolte.length === 0}
						<div class="p-6 bg-amber-50 border border-amber-200 rounded-xl text-center">
							<p class="text-amber-700 font-bold uppercase text-xs">Attenzione: Nessuna azienda valida rilevata.</p>
						</div>
					{/if}

					<div class="space-y-4 mt-4">
						{#each aziendeCoinvolte as azienda (azienda.idAzienda)}
							<div class="p-5 bg-white border border-gray-200 rounded-2xl flex flex-col md:flex-row md:items-center justify-between gap-4">
								<div class="flex items-start gap-4">
									<div class="p-3 bg-gray-100 rounded-xl text-gray-500">
										<Building2 size={20} />
									</div>
									<div>
										<h3 class="text-sm font-extrabold text-[#1B4B6B] uppercase">{azienda.ragioneSociale}</h3>
										<p class="text-[10px] font-bold text-emerald-600 flex items-center gap-1.5 mt-1 uppercase">
											<Users size={12} /> {azienda.count} dipendente/i
										</p>
									</div>
								</div>
								<div class="shrink-0">
									<input
											type="file"
											accept="application/pdf"
											onchange={(e) => handleFileChange(e, azienda.idAzienda)}
											class="block w-full text-xs text-gray-500 file:mr-4 file:py-2 file:px-4 file:rounded-xl file:border-0 file:text-[10px] file:font-extrabold file:uppercase file:bg-emerald-50 file:text-emerald-700 hover:file:bg-emerald-100 transition-colors"
									/>
								</div>
							</div>
						{/each}
					</div>
				{/if}
			</div>
			<div class="p-6 bg-white rounded-b-3xl border-t border-gray-100 flex gap-4">
				<button onclick={confermaDistribuzioneAttestati} disabled={isActionLoading || aziendeCoinvolte.length === 0} class="w-full py-4 bg-emerald-600 text-white font-extrabold rounded-xl hover:bg-emerald-700 uppercase text-xs shadow-lg shadow-emerald-900/20 disabled:opacity-50 flex justify-center items-center gap-2">
					{#if isActionLoading}
						<Loader2 class="animate-spin" size={16} /> Elaborazione...
					{:else}
						Invia Documenti alle Aziende
					{/if}
				</button>
			</div>
		</div>
	</div>
{/if}

{#if showModalFeedback && selectedCorso}
	<div class="fixed inset-0 z-[150] flex items-center justify-center p-4 bg-black/60 backdrop-blur-sm" transition:fade>
		<div class="bg-white w-full max-w-2xl rounded-[2.5rem] shadow-2xl flex flex-col max-h-[90vh] overflow-hidden" transition:scale>
			<div class="bg-[#1B4B6B] p-8 text-white flex justify-between items-center">
				<div>
					<h2 class="text-xl font-black uppercase tracking-tighter flex items-center gap-3">
						<BarChart size={24}/> Analisi Qualità Corso
					</h2>
					<p class="text-[10px] font-bold uppercase opacity-70 mt-1">{selectedCorso.titolo}</p>
				</div>
				<button onclick={() => showModalFeedback = false} class="text-white hover:rotate-90 transition-all duration-300">
					<X size={28} />
				</button>
			</div>

			<div class="p-8 overflow-y-auto custom-scrollbar flex-1 bg-gray-50/30">
				{#if isLoadingFeedback}
					<div class="py-20 text-center flex flex-col items-center gap-4">
						<Loader2 class="animate-spin text-[#1B4B6B]" size={48} />
						<span class="text-[10px] font-black uppercase tracking-widest text-gray-400">Analisi in corso...</span>
					</div>
				{:else if statsFeedback}
					{#if statsFeedback.totaleFeedback === 0}
						<div class="py-20 text-center border-2 border-dashed border-gray-200 rounded-3xl bg-white">
							<Star size={48} class="mx-auto text-gray-200 mb-4" />
							<p class="text-xs font-bold text-gray-400 uppercase tracking-widest">Nessun feedback rilasciato.</p>
						</div>
					{:else}
						<div class="grid grid-cols-1 md:grid-cols-2 gap-6 mb-10">
							<div class="bg-white p-8 rounded-3xl border border-gray-100 shadow-sm text-center">
								<p class="text-[10px] font-black text-gray-400 uppercase tracking-widest mb-2">Docenza</p>
								<div class="flex items-baseline justify-center gap-1">
									<span class="text-6xl font-black text-[#1B4B6B]">{statsFeedback.mediaDocenza.toFixed(1)}</span>
									<span class="text-xl font-bold text-gray-300">/5</span>
								</div>
							</div>
							<div class="bg-white p-8 rounded-3xl border border-gray-100 shadow-sm text-center">
								<p class="text-[10px] font-black text-gray-400 uppercase tracking-widest mb-2">Contenuti</p>
								<div class="flex items-baseline justify-center gap-1">
									<span class="text-6xl font-black text-[#1B4B6B]">{statsFeedback.mediaContenuti.toFixed(1)}</span>
									<span class="text-xl font-bold text-gray-300">/5</span>
								</div>
							</div>
						</div>

						<div class="flex items-center justify-between mb-6">
							<h3 class="text-sm font-black text-[#1B4B6B] uppercase tracking-widest">Commenti</h3>
							<span class="bg-[#1B4B6B]/10 text-[#1B4B6B] text-[10px] font-black px-4 py-1.5 rounded-full uppercase">
                                {statsFeedback.totaleFeedback} Risposte
                            </span>
						</div>

						<div class="space-y-4">
							{#each statsFeedback.commenti as commento (commento)}
								<div class="bg-white p-5 rounded-2xl border border-gray-100 shadow-sm flex gap-4 items-start">
									<div class="p-3 bg-gray-50 rounded-xl text-gray-400 shrink-0">
										<User size={16}/>
									</div>
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

{#if showModalElimina}
	<div class="fixed inset-0 bg-red-900/20 backdrop-blur-sm flex items-center justify-center z-[200] p-4" transition:fade>
		<div class="bg-white rounded-3xl shadow-2xl w-full max-w-sm overflow-hidden" in:scale>
			<div class="p-8 text-center">
				<div class="w-20 h-20 bg-red-50 text-red-600 rounded-full flex items-center justify-center mx-auto mb-6">
					<Trash2 size={40}/>
				</div>
				<h2 class="text-2xl font-black text-[#1B4B6B] uppercase tracking-tighter mb-2">Eliminare il corso?</h2>
				<p class="text-sm text-gray-400 mb-8">Questa azione è definitiva.</p>
				<div class="flex flex-col gap-3">
					<button onclick={confermaEliminaCorso} class="w-full bg-red-600 text-white py-4 rounded-2xl font-black uppercase text-[10px] shadow-lg shadow-red-200 transition-all hover:bg-red-700">
						Sì, elimina definitivamente
					</button>
					<button onclick={() => { showModalElimina = false; idCorsoDaEliminare = null; }} class="w-full py-4 text-[10px] font-black uppercase text-gray-400 hover:text-gray-600">
						No, annulla
					</button>
				</div>
			</div>
		</div>
	</div>
{/if}

<style>
	.custom-scrollbar::-webkit-scrollbar { width: 5px; }
	.custom-scrollbar::-webkit-scrollbar-thumb { background: #E2E8F0; border-radius: 10px; }
</style>