<script lang="ts">
	import { onMount } from 'svelte';
	import { fade, scale } from 'svelte/transition';
	import {
		Plus, X, Trash2, Search,
		Calendar, MapPin, Loader2, User,
		CheckSquare, UploadCloud, CheckCircle2, Clock, Building2, Users
	} from 'lucide-svelte';

	// Modelli
	import { CorsoFormazione } from '$lib/models/CorsoFormazione';
	import { Docente, type DocenteData } from '$lib/models/Docente';
	import { StatoCorso } from '$lib/models/Enums';
	import type { CorsoFormazioneRequest } from '$lib/models/CorsoFormazioneRequest';
	import type { IscrizioneCorso } from '$lib/models/IscrizioneCorso';

	// Servizi
	import { FormazioneService } from '$lib/services/FormazioneService';
	import { AnagraficaService } from '$lib/services/AnagraficaService';

	// --- STATO REATTIVO GLOBALE ---
	let corsi = $state<CorsoFormazione[]>([]);
	let docenti = $state<Docente[]>([]);
	let dipendenti = $state<any[]>([]); // Array grezzo dal backend
	let isLoading = $state(true);
	let isSaving = $state(false);
	let searchQuery = $state('');

	// --- STATI MODALI ---
	let showModalNuovo = $state(false);
	let showModalPresenze = $state(false);
	let showModalUpload = $state(false);

	// --- STATI OPERATIVI (MACCHINA A STATI) ---
	let selectedCorso = $state<CorsoFormazione | null>(null);
	let iscrizioniAttuali = $state<IscrizioneCorso[]>([]);
	let presenzeSelezionate = $state<number[]>([]);
	let fileUploads = $state<Record<number, File>>({}); // Mappa idAzienda -> File
	let isActionLoading = $state(false);

	// Form di creazione
	let formCorso = $state<Partial<CorsoFormazioneRequest>>({
		titolo: '', dataOrario: '', luogoFisico: '', idDocente: undefined
	});

	const isFormValid = $derived(
			!!formCorso.titolo && !!formCorso.dataOrario && !!formCorso.luogoFisico && !!formCorso.idDocente
	);

	// --- REGOLA 4: PULIZIA DASHBOARD ADMIN ---
	// Filtro di base: esclude i corsi "CERTIFICATO" perché il lavoro dell'Admin è concluso
	const corsiAttiviAdmin = $derived(
			corsi.filter(c => c.stato !== StatoCorso.CERTIFICATO)
	);

	// Filtro di ricerca applicato solo ai corsi operativi
	const corsiFiltrati = $derived(
			corsiAttiviAdmin.filter(c => (c.titolo || '').toLowerCase().includes(searchQuery.toLowerCase()))
	);

	// --- SEZIONI STRUTTURALI DELLA DASHBOARD ---
	const corsiConclusi = $derived(corsiFiltrati.filter(c =>
			c.stato === StatoCorso.CONCLUSO ||
			c.stato === StatoCorso.ATTESA_FIRMA_DOCENTE ||
			c.stato === StatoCorso.VALIDATO
	));
	const corsiInSvolgimento = $derived(corsiFiltrati.filter(c => c.stato === StatoCorso.IN_SVOLGIMENTO));
	const corsiProgrammati = $derived(corsiFiltrati.filter(c => c.stato === StatoCorso.PROGRAMMATO || !c.stato));

	// --- DERIVAZIONE AZIENDE PER UPLOAD ATTESTATI ---
	const aziendeCoinvolte = $derived.by(() => {
		if (!selectedCorso || iscrizioniAttuali.length === 0 || dipendenti.length === 0) return [];

		const presenti = iscrizioniAttuali.filter(i => i.presenzaConfermata);
		// eslint-disable-next-line svelte/prefer-svelte-reactivity
		const map = new Map<number, { idAzienda: number, ragioneSociale: string, count: number }>();

		presenti.forEach(isc => {
			// Trova il dipendente intero
			const dip = dipendenti.find(d => d.idUtente === isc.idUtente);
			if (!dip) return; // Se per assurdo non c'è, saltiamo

			// ESTRATTORE AGGRESSIVO DELL'ID AZIENDA:
			let idAzienda = null;
			let nomeAzienda = "Azienda Sconosciuta";

			if (dip.azienda && typeof dip.azienda === 'object') {
				idAzienda = dip.azienda.idUtente || dip.azienda.id;
				nomeAzienda = dip.azienda.ragioneSociale || dip.azienda.nome || nomeAzienda;
			} else if (dip.aziendaId) {
				idAzienda = dip.aziendaId;
				nomeAzienda = dip.aziendaRagioneSociale || `Azienda ID ${idAzienda}`;
			} else if (dip.idAzienda) {
				idAzienda = dip.idAzienda;
				nomeAzienda = dip.ragioneSocialeAzienda || `Azienda ID ${idAzienda}`;
			}

			// Se abbiamo trovato un VERO ID Azienda, raggruppiamo.
			if (idAzienda) {
				if (!map.has(idAzienda)) {
					map.set(idAzienda, {
						idAzienda: idAzienda,
						ragioneSociale: nomeAzienda,
						count: 0
					});
				}
				map.get(idAzienda)!.count++;
			} else {
				console.warn(`Dipendente ${isc.emailUtente} non ha un'azienda associata nel database!`);
			}
		});

		return Array.from(map.values());
	});

	// --- INIZIALIZZAZIONE ---
	onMount(async () => {
		try {
			const [corsiRes, docentiRes, dipendentiRes] = await Promise.all([
				FormazioneService.getAllCorsi(),
				AnagraficaService.getAllDocenti(),
				AnagraficaService.getAllDipendenti()
			]);

			corsi = corsiRes;
			docenti = (docentiRes as DocenteData[]).map(d => new Docente(d));
			dipendenti = dipendentiRes;
		} catch (error) {
			console.error("Errore nel caricamento dei dati:", error);
		} finally {
			isLoading = false;
		}
	});

	// --- AZIONI CREAZIONE ED ELIMINAZIONE ---
	async function salvaNuovoCorso() {
		if (!isFormValid) return;
		isSaving = true;
		try {
			const payload = {
				titolo: formCorso.titolo!,
				dataOrario: new Date(formCorso.dataOrario!).toISOString(),
				luogoFisico: formCorso.luogoFisico!,
				docente: { idUtente: formCorso.idDocente! },
				// REGOLA DI INTEGRITÀ: Ogni nuovo corso nasce come PROGRAMMATO
				stato: StatoCorso.PROGRAMMATO
			} as any; // Usiamo any se l'interfaccia Request non lo prevede ancora

			const nuovo = await FormazioneService.createCorso(payload);
			corsi = [...corsi, nuovo];

			// Reset e chiusura
			showModalNuovo = false;
			formCorso = { titolo: '', dataOrario: '', luogoFisico: '', idDocente: undefined };
		} catch (error) {
			alert("Impossibile programmare il corso.");
		} finally {
			isSaving = false;
		}
	}

	async function eliminaCorso(idCorso: number) {
		if (!confirm("Sei sicuro di voler eliminare questo corso?")) return;
		try {
			await FormazioneService.deleteCorso(idCorso);
			corsi = corsi.filter(c => c.idCorso !== idCorso);
			// eslint-disable-next-line @typescript-eslint/no-unused-vars
		} catch (error) {
			alert("Errore durante l'eliminazione.");
		}
	}

	// --- FASE 1 FSM: VALIDAZIONE PRESENZE ---
	async function apriValidazione(corso: CorsoFormazione) {
		selectedCorso = corso;
		isActionLoading = true;
		showModalPresenze = true;
		try {
			iscrizioniAttuali = await FormazioneService.getIscrizioniByCorso(corso.idCorso);
			presenzeSelezionate = iscrizioniAttuali.filter(i => i.presenzaConfermata).map(i => i.idUtente);
			// eslint-disable-next-line @typescript-eslint/no-unused-vars
		} catch (error) {
			alert("Impossibile caricare il registro iscritti.");
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
			console.error("Errore durante la chiamata API validaPresenze:", error);
			alert("Errore durante la validazione del registro.");
		} finally {
			isActionLoading = false;
		}
	}

	// --- FASE 3 FSM: UPLOAD ATTESTATI ---
	async function apriUploadAttestati(corso: CorsoFormazione) {
		selectedCorso = corso;
		isActionLoading = true;
		showModalUpload = true;
		fileUploads = {}; // Reset file
		try {
			// Ricarichiamo le iscrizioni per essere sicuri dei dati
			iscrizioniAttuali = await FormazioneService.getIscrizioniByCorso(corso.idCorso);

			if (aziendeCoinvolte.length === 0) {
				console.warn("Attenzione: Nessuna azienda rilevata per i presenti. Verifica i dati anagrafici dei dipendenti iscritti.");
			}
			// eslint-disable-next-line @typescript-eslint/no-unused-vars
		} catch (error) {
			alert("Impossibile caricare i dati.");
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
			alert("Devi caricare un certificato PDF per tutte le aziende elencate.");
			return;
		}

		isActionLoading = true;
		try {
			const payload = aziendeCoinvolte.map(a => ({
				idAzienda: a.idAzienda,
				file: fileUploads[a.idAzienda]
			}));

			// Effettua la chiamata API
			await FormazioneService.distribuisciAttestati(selectedCorso.idCorso, payload);
			alert("Attestati distribuiti correttamente! Le aziende riceveranno una notifica.");

			// FSM Client-Side: Aggiorniamo lo stato a CERTIFICATO per innescare la sparizione reattiva (Regola 4)
			corsi = corsi.map(c => c.idCorso === selectedCorso!.idCorso ? { ...c, stato: StatoCorso.CERTIFICATO } as CorsoFormazione : c);

			showModalUpload = false;
		} catch (error) {
			console.error("Errore fatale durante l'upload degli attestati:", error);
			alert("Errore durante il caricamento degli attestati. Controlla la console.");
		} finally {
			isActionLoading = false;
		}
	}

	function formattaData(isoString: string) {
		if (!isoString) return '';
		const d = new Date(isoString);
		return d.toLocaleDateString('it-IT', { day: '2-digit', month: 'short', year: 'numeric', hour: '2-digit', minute:'2-digit' });
	}
</script>

<div in:fade class="pb-20">
	<div class="mb-10 flex justify-between items-start">
		<div>
			<h1 class="text-4xl font-extrabold text-[#1B4B6B]">GESTIONE FORMAZIONE</h1>
			<p class="text-gray-500 font-bold uppercase text-xs tracking-tighter">Pianificazione corsi, aule e docenze.</p>
		</div>

		<button
				onclick={() => showModalNuovo = true}
				class="bg-white text-[#1B4B6B] border-2 border-[#1B4B6B] px-8 py-3.5 rounded-xl font-extrabold uppercase text-xs shadow-lg hover:bg-[#1B4B6B] hover:text-white transition-all flex items-center gap-3"
		>
			<Plus size={18} /> Programma Corso
		</button>
	</div>

	<div class="mb-8 relative w-72 group">
		<Search class="absolute left-3 top-1/2 -translate-y-1/2 text-gray-400 group-focus-within:text-[#1B4B6B] transition-colors" size={16} />
		<input
				bind:value={searchQuery}
				type="text"
				placeholder="Cerca per titolo..."
				class="w-full pl-10 pr-4 py-2.5 bg-white border border-gray-200 rounded-xl text-xs focus:ring-2 focus:ring-[#1B4B6B] outline-none transition-all font-bold uppercase shadow-sm"
		/>
	</div>

	{#if isLoading}
		<div class="py-20 text-center"><Loader2 size={40} class="animate-spin mx-auto text-[#1B4B6B]" /></div>
	{:else}

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
								<h3 class="font-extrabold text-[#1B4B6B] uppercase leading-tight">{corso.titolo || 'Nessun Titolo'}</h3>
								<p class="text-xs font-bold text-gray-500 uppercase">{formattaData(corso.dataOrario)}</p>
							</div>
							<div class="p-4 bg-gray-50 border-t border-gray-100 flex flex-col gap-2">
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
							</div>
						</div>
					{/each}
				</div>
			{/if}
		</div>

		<div class="mb-14">
			<div class="flex items-center gap-3 mb-6 border-b border-gray-200 pb-3">
				<div class="p-2 bg-blue-100 text-blue-700 rounded-lg"><Loader2 size={20} class="animate-spin-slow"/></div>
				<h2 class="text-xl font-extrabold text-[#1B4B6B] uppercase tracking-tight">Corsi In Svolgimento</h2>
			</div>
			{#if corsiInSvolgimento.length === 0}
				<div class="p-8 border-2 border-dashed border-gray-200 rounded-2xl text-center text-gray-400 font-bold uppercase text-xs">Nessun corso attualmente in aula.</div>
			{:else}
				<div class="grid grid-cols-1 xl:grid-cols-3 gap-6">
					{#each corsiInSvolgimento as corso (corso.idCorso)}
						<div class="bg-white rounded-2xl shadow-sm border border-blue-100 p-6 relative overflow-hidden">
							<div class="absolute top-0 left-0 w-1.5 h-full bg-blue-500"></div>
							<h3 class="font-extrabold text-[#1B4B6B] uppercase mb-2 ml-2">{corso.titolo}</h3>
							<div class="flex items-center gap-2 text-xs font-bold text-gray-500 ml-2">
								<MapPin size={14} /> {corso.luogoFisico}
							</div>
						</div>
					{/each}
				</div>
			{/if}
		</div>

		<div class="mb-14">
			<div class="flex items-center gap-3 mb-6 border-b border-gray-200 pb-3">
				<div class="p-2 bg-gray-100 text-gray-700 rounded-lg"><Calendar size={20}/></div>
				<h2 class="text-xl font-extrabold text-[#1B4B6B] uppercase tracking-tight">Corsi Programmati</h2>
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
						</div>
					{/each}
				</div>
			{/if}
		</div>
	{/if}
</div>

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
									<div>
										<p class="text-sm font-extrabold text-[#1B4B6B] uppercase">{isc.emailUtente}</p>
									</div>
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
					<p class="text-xs font-bold text-gray-500 mb-6 uppercase">Carica un certificato cumulativo in PDF per ciascuna azienda. Il sistema provvederà a inoltrarlo automaticamente e a collegarlo al profilo di tutti i dipendenti iscritti dell'azienda.</p>

					{#if aziendeCoinvolte.length === 0}
						<div class="p-6 bg-amber-50 border border-amber-200 rounded-xl text-center">
							<p class="text-amber-700 font-bold uppercase text-xs">Attenzione: Non è stata rilevata nessuna azienda valida per i dipendenti presenti. Impossibile procedere con la distribuzione.</p>
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
					{#if isActionLoading} <Loader2 class="animate-spin" size={16} /> Elaborazione... {:else} Invia Documenti alle Aziende {/if}
				</button>
			</div>
		</div>
	</div>
{/if}

<style>
	.custom-scrollbar::-webkit-scrollbar { width: 5px; }
	.custom-scrollbar::-webkit-scrollbar-thumb { background: #E2E8F0; border-radius: 10px; }
	.animate-spin-slow { animation: spin 3s linear infinite; }
</style>