<script lang="ts">
	import { onMount } from 'svelte';
	import { fade, scale } from 'svelte/transition';
	import {
		Users, Search, Filter, Mail, FileBadge, Upload, BookOpen, Loader2, CheckCircle2, X
	} from 'lucide-svelte';

	// IMPORT SERVIZI E MODELLI
	import { IscrizioneCorso } from '$lib/models/IscrizioneCorso';
	import { AuthService } from '$lib/services/AuthService';
	import { FormazioneService } from '$lib/services/FormazioneService';

	// Interfaccia per il raggruppamento lato client
	interface IscrizioneDettaglio {
		idCorso: number;
		titoloCorso: string;
		presenzaConfermata: boolean;
		pathAttestato: string | null;
		isActionLoadingPresenza: boolean;
		isActionLoadingAttestato: boolean;
	}

	interface StudenteRaggruppato {
		idUtente: number;
		emailUtente: string;
		iscrizioni: IscrizioneDettaglio[];
	}

	// STATO REATTIVO (Svelte 5)
	let isLoading = $state(true);
	let studentiRaggruppati = $state<StudenteRaggruppato[]>([]);
	let queryRicerca = $state('');
	let filtroCorso = $state('');
	let corsiUnici = $state<{ id: number, titolo: string }[]>([]);

	// STATI PER LA MODALE DI UPLOAD
	let showUploadModal = $state(false);
	let isUploading = $state(false);
	let uploadFile = $state<File | null>(null);
	let targetStudente = $state<StudenteRaggruppato | null>(null);
	let targetIscrizione = $state<IscrizioneDettaglio | null>(null);

	onMount(async () => {
		const session = AuthService.getSession();
		if (!session) return;

		try {
			// 1. Recupero i corsi assegnati al Docente
			const tuttiCorsi = await FormazioneService.getAllCorsi();
			const mieiCorsi = tuttiCorsi.filter(c => c.idDocente === session.idUtente);

			// Popolo la select dei filtri con i corsi del docente
			corsiUnici = mieiCorsi.map(c => ({ id: c.idCorso, titolo: c.titolo }));

			// 2. Fetch parallelo delle iscrizioni per ogni corso
			const iscrizioniPromises = mieiCorsi.map(corso =>
					FormazioneService.getIscrizioniByCorso(corso.idCorso)
			);

			const iscrizioniResults = await Promise.all(iscrizioniPromises);

			// Appiattisco l'array di array
			const iscrizioniFlat = iscrizioniResults.flat();

			// 3. Raggruppo le iscrizioni per idUtente
			const mappaStudenti = new Map<number, StudenteRaggruppato>();

			iscrizioniFlat.forEach(iscrizione => {
				if (!mappaStudenti.has(iscrizione.idUtente)) {
					mappaStudenti.set(iscrizione.idUtente, {
						idUtente: iscrizione.idUtente,
						emailUtente: iscrizione.emailUtente,
						iscrizioni: []
					});
				}

				// Aggiungo il corso all'array delle iscrizioni dello studente
				mappaStudenti.get(iscrizione.idUtente)!.iscrizioni.push({
					idCorso: iscrizione.idCorso,
					titoloCorso: iscrizione.titoloCorso,
					presenzaConfermata: iscrizione.presenzaConfermata,
					pathAttestato: iscrizione.pathAttestato,
					isActionLoadingPresenza: false,
					isActionLoadingAttestato: false
				});
			});

			studentiRaggruppati = Array.from(mappaStudenti.values());

		} catch (error) {
			console.error("Errore nel caricamento degli iscritti:", error);
		} finally {
			isLoading = false;
		}
	});

	// Filtro applicato sugli studenti raggruppati
	const studentiFiltrati = $derived(
			studentiRaggruppati.filter(studente => {
				const matchTesto = studente.emailUtente.toLowerCase().includes(queryRicerca.toLowerCase());
				const matchCorso = filtroCorso === '' || studente.iscrizioni.some(isc => isc.idCorso.toString() === filtroCorso);
				return matchTesto && matchCorso;
			})
	);

	function getIniziale(email: string) {
		return email ? email.charAt(0).toUpperCase() : 'S';
	}

	// --- AZIONI ---

	async function handleTogglePresenza(studente: StudenteRaggruppato, iscrizione: IscrizioneDettaglio) {
		iscrizione.isActionLoadingPresenza = true;
		try {
			await FormazioneService.validaPresenza(iscrizione.idCorso, studente.idUtente);
			iscrizione.presenzaConfermata = !iscrizione.presenzaConfermata;
		} catch (error) {
			console.error("Errore validazione presenza", error);
		} finally {
			iscrizione.isActionLoadingPresenza = false;
		}
	}

	function apriModaleUpload(studente: StudenteRaggruppato, iscrizione: IscrizioneDettaglio) {
		targetStudente = studente;
		targetIscrizione = iscrizione;
		uploadFile = null;
		showUploadModal = true;
	}

	async function confermaUploadAttestato() {
		if (!targetStudente || !targetIscrizione || !uploadFile) return;

		isUploading = true;
		try {
			// Chiamata al servizio che usa FormData per il MultipartFile
			const percorsoServer = await FormazioneService.uploadAttestato(
					targetIscrizione.idCorso,
					targetStudente.idUtente,
					uploadFile
			);

			targetIscrizione.pathAttestato = percorsoServer;
			showUploadModal = false;
		} catch (error) {
			console.error("Errore durante l'upload fisico dell'attestato:", error);
			alert("Errore nel caricamento del file. Riprova.");
		} finally {
			isUploading = false;
		}
	}
</script>

<div in:fade class="space-y-8 max-w-7xl mx-auto pb-20">
	<div class="flex flex-col md:flex-row justify-between items-start md:items-center gap-6">
		<div>
			<h1 class="text-4xl font-black text-[#1B4B6B] uppercase tracking-tighter">Gestione Studenti</h1>
			<p class="text-gray-400 font-bold uppercase text-[10px] tracking-widest mt-1">Registri presenze e caricamento certificazioni ufficiali</p>
		</div>

		<div class="bg-white px-6 py-4 rounded-3xl shadow-sm border border-gray-100 flex items-center gap-4">
			<div class="p-3 bg-blue-50 rounded-2xl text-[#1B4B6B]">
				<Users size={24} />
			</div>
			<div>
				<p class="text-[10px] font-black text-gray-300 uppercase tracking-widest">Database Lavoratori</p>
				<p class="text-lg font-black text-[#1B4B6B] uppercase">{studentiRaggruppati.length} ANAGRAFICHE</p>
			</div>
		</div>
	</div>

	<div class="bg-white p-4 rounded-[2.5rem] shadow-sm border border-gray-100 flex flex-col lg:flex-row gap-4">
		<div class="relative flex-1 group">
			<Search class="absolute left-6 top-1/2 -translate-y-1/2 text-gray-400 group-focus-within:text-[#1B4B6B] transition-colors" size={20} />
			<input
					bind:value={queryRicerca}
					type="text"
					placeholder="CERCA PER EMAIL..."
					class="w-full bg-gray-50 border-none rounded-2xl py-5 pl-16 pr-6 text-xs font-bold text-[#1B4B6B] focus:ring-2 focus:ring-[#1B4B6B]/20 transition-all uppercase outline-none"
			/>
		</div>
		<div class="relative min-w-[300px]">
			<Filter class="absolute left-6 top-1/2 -translate-y-1/2 text-gray-400" size={20} />
			<select
					bind:value={filtroCorso}
					class="w-full bg-gray-50 border-none rounded-2xl py-5 pl-16 pr-10 text-xs font-bold text-[#1B4B6B] focus:ring-2 focus:ring-[#1B4B6B]/20 transition-all uppercase outline-none appearance-none cursor-pointer"
			>
				<option value="">FILTRA PER CORSO (TUTTI)</option>
				{#each corsiUnici as corso (corso.id)}
					<option value={corso.id.toString()}>{corso.titolo}</option>
				{/each}
			</select>
		</div>
	</div>

	{#if isLoading}
		<div class="py-32 flex flex-col items-center justify-center gap-4">
			<Loader2 size={48} class="animate-spin text-[#1B4B6B]" />
			<span class="text-[10px] font-black text-gray-400 uppercase tracking-widest">Caricamento registri...</span>
		</div>
	{:else if studentiFiltrati.length === 0}
		<div class="py-32 bg-white rounded-[2.5rem] border border-gray-100 border-dashed flex flex-col items-center justify-center text-center">
			<Users size={64} class="text-gray-200 mb-6" />
			<h3 class="font-black text-[#1B4B6B] uppercase text-2xl">Nessun risultato</h3>
		</div>
	{:else}
		<div class="grid grid-cols-1 gap-6">
			{#each studentiFiltrati as studente (studente.idUtente)}
				<div in:scale={{duration: 300}} class="bg-white rounded-[2.5rem] border border-gray-100 shadow-sm hover:shadow-xl transition-all duration-300 p-8 group flex flex-col gap-6">

					<div class="flex items-center justify-between gap-6 border-b border-gray-50 pb-6">
						<div class="flex items-center gap-6">
							<div class="w-16 h-16 rounded-[1.2rem] bg-[#1B4B6B] text-white flex items-center justify-center font-black text-2xl shadow-lg shrink-0">
								{getIniziale(studente.emailUtente)}
							</div>
							<div class="min-w-0">
								<h4 class="font-black text-[#1B4B6B] uppercase text-xl tracking-tight">{studente.emailUtente}</h4>
								<span class="inline-block mt-1 px-3 py-1 bg-gray-50 text-gray-400 rounded-lg text-[9px] font-black uppercase tracking-widest border border-gray-100">
                             {studente.iscrizioni.length} Cors{studente.iscrizioni.length > 1 ? 'i' : 'o'} Attivi
                         </span>
							</div>
						</div>
						<a href="/dashboard/docente/messaggi" class="hidden sm:flex items-center justify-center gap-2 bg-gray-50 text-[#1B4B6B] px-6 py-4 rounded-2xl text-[10px] font-black uppercase hover:bg-[#1B4B6B] hover:text-white transition-all">
							<Mail size={18} /> Chat
						</a>
					</div>

					<div class="flex flex-col gap-4">
						{#each studente.iscrizioni as iscrizione (iscrizione.idCorso)}
							{#if filtroCorso === '' || iscrizione.idCorso.toString() === filtroCorso}
								<div class="flex flex-col xl:flex-row items-start xl:items-center justify-between gap-4 p-5 rounded-2xl bg-gray-50/50 border border-gray-100/50 hover:bg-gray-50 transition-colors">

									<div class="flex items-center gap-3 min-w-0 flex-1">
										<div class="p-2 bg-white rounded-lg shadow-sm border border-gray-100 text-[#1B4B6B] shrink-0">
											<BookOpen size={18} />
										</div>
										<h5 class="font-bold text-[#1B4B6B] text-sm uppercase truncate">{iscrizione.titoloCorso}</h5>
									</div>

									<div class="flex flex-wrap sm:flex-nowrap items-center gap-3 w-full xl:w-auto">
										<button
												onclick={() => handleTogglePresenza(studente, iscrizione)}
												disabled={iscrizione.isActionLoadingPresenza}
												class="flex-1 sm:flex-none relative flex items-center justify-center gap-2 px-5 py-3 rounded-xl border transition-all disabled:opacity-50
                                  {iscrizione.presenzaConfermata ? 'bg-emerald-50 border-emerald-100 text-emerald-600' : 'bg-white border-gray-200 text-gray-400 hover:bg-gray-100'}"
										>
											{#if iscrizione.isActionLoadingPresenza}
												<Loader2 size={16} class="animate-spin" />
											{:else}
												<CheckCircle2 size={16} />
												<span class="text-[9px] font-black uppercase tracking-widest">
                                        {iscrizione.presenzaConfermata ? 'Presente' : 'Segna Presenza'}
                                     </span>
											{/if}
										</button>

										{#if iscrizione.presenzaConfermata}
											<div class="flex-1 sm:flex-none">
												<button
														onclick={() => apriModaleUpload(studente, iscrizione)}
														disabled={iscrizione.isActionLoadingAttestato}
														class="w-full sm:w-auto flex items-center justify-center gap-2 bg-[#1B4B6B] text-white border border-[#1B4B6B] px-5 py-3 rounded-xl text-[9px] font-black uppercase tracking-widest hover:bg-[#153a54] transition-all shadow-lg shadow-[#1B4B6B]/20 disabled:opacity-50"
												>
													{#if iscrizione.isActionLoadingAttestato}
														<Loader2 size={16} class="animate-spin" /> Caricamento...
													{:else}
														<Upload size={16} /> Carica Attestato
													{/if}
												</button>
											</div>
										{:else}
											<div class="flex-1 sm:flex-none flex items-center justify-center gap-2 bg-white text-gray-300 border border-gray-100 px-5 py-3 rounded-xl text-[9px] font-black uppercase tracking-widest cursor-not-allowed">
												<FileBadge size={16} /> Blocca Certificato
											</div>
										{/if}
									</div>
								</div>
							{/if}
						{/each}
					</div>
				</div>
			{/each}
		</div>
	{/if}

	{#if showUploadModal}
		<div class="fixed inset-0 bg-[#1B4B6B]/40 backdrop-blur-sm flex items-center justify-center z-[110] p-4" transition:fade>
			<div class="bg-white rounded-3xl shadow-2xl w-full max-w-lg overflow-hidden" in:scale>
				<div class="bg-[#1B4B6B] p-6 text-white flex justify-between items-center">
					<h2 class="text-xl font-black uppercase tracking-tighter flex items-center gap-2">
						<Upload size={20}/> Caricamento File
					</h2>
					<button onclick={() => (showUploadModal = false)} class="hover:rotate-90 transition-transform"><X size={24}/></button>
				</div>

				<div class="p-8 space-y-6">
					<div class="text-center">
						<p class="text-[10px] font-black uppercase text-gray-400 tracking-widest">Assegnazione Certificato a</p>
						<p class="text-lg font-black text-[#1B4B6B] truncate">{targetStudente?.emailUtente}</p>
						<p class="text-[10px] font-bold text-gray-500 mt-2 uppercase flex items-center justify-center gap-1.5">
							<BookOpen size={14}/> {targetIscrizione?.titoloCorso}
						</p>
					</div>

					<div class="border-2 border-dashed border-gray-200 rounded-3xl p-10 text-center bg-gray-50 hover:bg-gray-100 transition-colors group cursor-pointer relative">
						<input type="file" accept=".pdf" id="pdfUploadInput" class="absolute inset-0 w-full h-full opacity-0 cursor-pointer" onchange={(e) => uploadFile = e.currentTarget.files?.[0] || null} />
						<div class="flex flex-col items-center gap-3 pointer-events-none">
							<div class="p-4 bg-white rounded-full text-[#1B4B6B] shadow-sm group-hover:scale-110 transition-transform">
								<FileBadge size={32} />
							</div>
							{#if uploadFile}
								<span class="text-sm font-black text-[#1B4B6B] truncate max-w-[250px]">{uploadFile.name}</span>
								<span class="text-[9px] font-bold text-green-500 uppercase tracking-widest">File pronto per l'invio</span>
							{:else}
								<span class="text-xs font-black text-gray-400 uppercase tracking-widest">Seleziona l'attestato in formato PDF</span>
							{/if}
						</div>
					</div>
				</div>

				<div class="p-6 bg-gray-50 flex justify-end gap-4 border-t border-gray-100">
					<button onclick={() => (showUploadModal = false)} class="px-6 py-3 text-[10px] font-black uppercase text-gray-400 hover:text-gray-600 transition-colors">Annulla</button>
					<button
							onclick={confermaUploadAttestato}
							disabled={isUploading || !uploadFile}
							class="bg-[#1B4B6B] text-white px-8 py-3 rounded-xl text-[10px] font-black uppercase shadow-lg disabled:opacity-50 flex items-center gap-2 hover:bg-[#153a54] transition-colors"
					>
						{#if isUploading}
							<Loader2 size={14} class="animate-spin" /> Upload in corso...
						{:else}
							<Upload size={14} /> Invia File
						{/if}
					</button>
				</div>
			</div>
		</div>
	{/if}
</div>

<style>
	:global(body) { background-color: #F9FAFB; }
</style>