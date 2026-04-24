<script lang="ts">
	import { onMount } from 'svelte';
	import { fade, scale } from 'svelte/transition';
	import {
		Users, Search, Filter, Mail, FileBadge, Upload, BookOpen, Loader2, CheckCircle2
	} from 'lucide-svelte';

	// IMPORT SERVIZI E MODELLI
	import { IscrizioneCorso } from '$lib/models/IscrizioneCorso';
	import { AuthService } from '$lib/services/AuthService';
	import { FormazioneService } from '$lib/services/FormazioneService';

	// STATO REATTIVO (Svelte 5)
	let isLoading = $state(true);
	let studenti = $state<IscrizioneCorso[]>([]);
	let queryRicerca = $state('');
	let filtroCorso = $state('');
	let isActionLoading = $state<{ [key: string]: boolean }>({});

	onMount(async () => {
		const session = AuthService.getSession(); //
		if (!session) return;

		try {
			// 1. Recupero i corsi assegnati al Docente
			const tuttiCorsi = await FormazioneService.getAllCorsi();
			const mieiCorsi = tuttiCorsi.filter(c => c.idDocente === session.idUtente);

			// 2. Fetch parallelo delle iscrizioni per ogni corso
			const iscrizioniPromises = mieiCorsi.map(corso =>
					FormazioneService.getIscrizioniByCorso(corso.idCorso)
			);

			const iscrizioniResults = await Promise.all(iscrizioniPromises);

			// Appiattisco l'array di array
			studenti = iscrizioniResults.flat();
		} catch (error) {
			console.error("Errore nel caricamento degli iscritti:", error);
		} finally {
			isLoading = false;
		}
	});

	const studentiFiltrati = $derived(
			studenti.filter(s => {
				const matchTesto = s.emailUtente.toLowerCase().includes(queryRicerca.toLowerCase());
				const matchCorso = filtroCorso === '' || s.idCorso.toString() === filtroCorso;
				return matchTesto && matchCorso;
			})
	);

	const corsiUnici = $derived(
			Array.from(new Set(studenti.map(s => s.idCorso))).map(id => {
				const studente = studenti.find(s => s.idCorso === id);
				return { id, titolo: studente?.titoloCorso };
			})
	);

	function getIniziale(email: string) {
		return email ? email.charAt(0).toUpperCase() : 'S';
	}

	// --- AZIONI REALI SUL BACKEND ---

	async function handleTogglePresenza(studente: IscrizioneCorso) {
		const actionKey = `${studente.idUtente}-${studente.idCorso}-presenza`;
		isActionLoading[actionKey] = true;

		try {
			await FormazioneService.validaPresenza(studente.idCorso, studente.idUtente); //
			// Modifica ottimistica lato UI per evitare ricaricamenti pesanti
			studente.presenzaConfermata = !studente.presenzaConfermata;
		} catch (error) {
			console.error("Errore validazione presenza", error);
		} finally {
			isActionLoading[actionKey] = false;
		}
	}

	async function handleCaricaAttestato(studente: IscrizioneCorso) {
		const actionKey = `${studente.idUtente}-${studente.idCorso}-attestato`;
		isActionLoading[actionKey] = true;

		try {
			// Simulo l'apertura e il caricamento di un file
			// Nella realtà qui andrebbe una logica di upload file multipart
			const mockUploadedPath = `/uploads/attestati/corso_${studente.idCorso}_utente_${studente.idUtente}.pdf`;
			await FormazioneService.sbloccaCertificato(studente.idCorso, studente.idUtente, mockUploadedPath);

			studente.pathAttestato = mockUploadedPath;
		} catch (error) {
			console.error("Errore caricamento certificato", error);
		} finally {
			isActionLoading[actionKey] = false;
		}
	}
</script>

<div in:fade class="space-y-8 max-w-7xl mx-auto pb-20">
	<div class="flex flex-col md:flex-row justify-between items-start md:items-center gap-6">
		<div>
			<h1 class="text-4xl font-black text-[#1B4B6B] uppercase tracking-tighter">Studenti</h1>
			<p class="text-gray-400 font-bold uppercase text-[10px] tracking-widest mt-1">Gestione presenze, comunicazioni e rilascio attestati</p>
		</div>

		<div class="flex gap-4">
			<div class="bg-white px-6 py-4 rounded-3xl shadow-sm border border-gray-100 flex items-center gap-4">
				<div class="p-3 bg-blue-50 rounded-2xl text-[#1B4B6B]">
					<Users size={24} />
				</div>
				<div>
					<p class="text-[10px] font-black text-gray-300 uppercase tracking-widest">Totale Iscritti</p>
					<p class="text-lg font-black text-[#1B4B6B] uppercase">{studenti.length} STUDENTI</p>
				</div>
			</div>
		</div>
	</div>

	<div class="bg-white p-4 rounded-[2.5rem] shadow-sm border border-gray-100 flex flex-col lg:flex-row gap-4">
		<div class="relative flex-1 group">
			<Search class="absolute left-6 top-1/2 -translate-y-1/2 text-gray-400 group-focus-within:text-[#1B4B6B] transition-colors" size={20} />
			<input
					bind:value={queryRicerca}
					type="text"
					placeholder="CERCA STUDENTE PER EMAIL..."
					class="w-full bg-gray-50 border-none rounded-2xl py-5 pl-16 pr-6 text-xs font-bold text-[#1B4B6B] placeholder:text-gray-400 focus:ring-2 focus:ring-[#1B4B6B]/20 transition-all uppercase outline-none"
			/>
		</div>
		<div class="relative min-w-[300px]">
			<Filter class="absolute left-6 top-1/2 -translate-y-1/2 text-gray-400" size={20} />
			<select
					bind:value={filtroCorso}
					class="w-full bg-gray-50 border-none rounded-2xl py-5 pl-16 pr-10 text-xs font-bold text-[#1B4B6B] focus:ring-2 focus:ring-[#1B4B6B]/20 transition-all uppercase outline-none appearance-none cursor-pointer"
			>
				<option value="">TUTTI I CORSI</option>
				{#each corsiUnici as corso (corso.id)}
					<option value={corso.id.toString()}>{corso.titolo}</option>
				{/each}
			</select>
		</div>
	</div>

	{#if isLoading}
		<div class="py-32 flex flex-col items-center justify-center gap-4">
			<Loader2 size={48} class="animate-spin text-[#1B4B6B]" />
			<span class="text-[10px] font-black text-gray-400 uppercase tracking-widest">Sincronizzazione registri didattici...</span>
		</div>
	{:else if studentiFiltrati.length === 0}
		<div class="py-32 bg-white rounded-[2.5rem] border border-gray-100 border-dashed flex flex-col items-center justify-center text-center shadow-sm">
			<Users size={64} class="text-gray-200 mb-6" strokeWidth={1.5} />
			<h3 class="font-black text-[#1B4B6B] uppercase text-2xl tracking-tighter">Nessuno studente trovato</h3>
			<p class="text-[10px] font-bold text-gray-400 uppercase mt-2 tracking-widest">Modifica i parametri di ricerca o il filtro corso</p>
		</div>
	{:else}
		<div class="grid grid-cols-1 gap-6">
			{#each studentiFiltrati as studente (studente.idUtente + '-' + studente.idCorso)}
				<div
						in:scale={{duration: 300}}
						class="bg-white rounded-[2.5rem] border border-gray-100 shadow-sm hover:shadow-xl transition-all duration-300 p-8 flex flex-col xl:flex-row items-start xl:items-center justify-between gap-8 group"
				>
					<div class="flex items-center gap-6 w-full xl:w-auto">
						<div class="w-16 h-16 rounded-[1.2rem] bg-[#1B4B6B] text-white flex items-center justify-center font-black text-2xl shadow-lg shadow-blue-900/20 shrink-0">
							{getIniziale(studente.emailUtente)}
						</div>
						<div class="flex-1 min-w-0">
							<h4 class="font-black text-[#1B4B6B] uppercase text-xl truncate tracking-tight">{studente.emailUtente}</h4>
							<div class="flex items-center gap-3 mt-2 text-[10px] font-bold uppercase tracking-widest text-gray-400">
								<span class="flex items-center gap-1.5"><BookOpen size={14} class="text-[#1B4B6B]" /> {studente.titoloCorso}</span>
							</div>
						</div>
					</div>

					<div class="flex flex-wrap xl:flex-nowrap items-center gap-4 w-full xl:w-auto">
						<a
								href="/dashboard/docente/messaggi"
								class="flex-1 xl:flex-none flex items-center justify-center gap-2 bg-gray-50 border border-transparent text-[#1B4B6B] px-6 py-4 rounded-2xl text-[10px] font-black uppercase hover:bg-gray-100 transition-all"
						>
							<Mail size={18} /> Chat
						</a>

						<div class="h-12 w-px bg-gray-100 hidden xl:block"></div>

						<button
								onclick={() => handleTogglePresenza(studente)}
								disabled={isActionLoading[`${studente.idUtente}-${studente.idCorso}-presenza`]}
								class="flex-1 xl:flex-none relative flex items-center justify-center gap-3 px-6 py-4 rounded-2xl border transition-all cursor-pointer disabled:opacity-50
                      {studente.presenzaConfermata ? 'bg-emerald-50 border-emerald-100 text-emerald-600 shadow-sm' : 'bg-gray-50 border-gray-100 text-gray-400 hover:bg-gray-100'}"
						>
							{#if isActionLoading[`${studente.idUtente}-${studente.idCorso}-presenza`]}
								<Loader2 size={18} class="animate-spin" /> Elaborazione...
							{:else}
								<CheckCircle2 size={18} />
								<span class="text-[10px] font-black uppercase tracking-widest">
                            {studente.presenzaConfermata ? 'Presenza Registrata' : 'Segna Presente'}
                         </span>
							{/if}
						</button>

						{#if studente.presenzaConfermata}
							<div class="flex-1 xl:flex-none">
								{#if !!studente.pathAttestato && studente.pathAttestato.trim() !== ''}
									<button class="w-full xl:w-auto flex items-center justify-center gap-2 bg-emerald-500 text-white border border-emerald-500 px-6 py-4 rounded-2xl text-[10px] font-black uppercase tracking-widest shadow-lg shadow-emerald-500/20 cursor-default">
										<FileBadge size={18} />
										Attestato Generato
									</button>
								{:else}
									<button
											onclick={() => handleCaricaAttestato(studente)}
											disabled={isActionLoading[`${studente.idUtente}-${studente.idCorso}-attestato`]}
											class="w-full xl:w-auto flex items-center justify-center gap-2 bg-[#1B4B6B] text-white border border-[#1B4B6B] px-6 py-4 rounded-2xl text-[10px] font-black uppercase tracking-widest hover:bg-[#153a54] transition-all shadow-lg shadow-[#1B4B6B]/20 disabled:opacity-50"
									>
										{#if isActionLoading[`${studente.idUtente}-${studente.idCorso}-attestato`]}
											<Loader2 size={18} class="animate-spin" /> Caricamento...
										{:else}
											<Upload size={18} /> Carica Attestato
										{/if}
									</button>
								{/if}
							</div>
						{:else}
							<div class="flex-1 xl:flex-none flex items-center justify-center gap-2 bg-gray-50 text-gray-300 border border-gray-50 px-6 py-4 rounded-2xl text-[10px] font-black uppercase tracking-widest cursor-not-allowed">
								<FileBadge size={18} />
								Richiede Presenza
							</div>
						{/if}
					</div>
				</div>
			{/each}
		</div>
	{/if}
</div>

<style>
	:global(body) { background-color: #F9FAFB; }
</style>