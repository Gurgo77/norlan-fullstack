<script lang="ts">
	import { onMount } from 'svelte';
	import { fade, scale } from 'svelte/transition';
	import {
		Users, Search, Filter, MessageSquare, BookOpen, Loader2, ChevronRight
	} from 'lucide-svelte';

	// IMPORT SERVIZI E MODELLI
	import { AuthService } from '$lib/services/AuthService';
	import { FormazioneService } from '$lib/services/FormazioneService';
	import { StatoCorso } from '$lib/models/Enums';

	// Interfaccia snellita per il raggruppamento
	interface IscrizioneDettaglio {
		idCorso: number;
		titoloCorso: string;
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

	onMount(async () => {
		const session = AuthService.getSession();
		if (!session) return;

		try {
			// 1. Recupero i corsi assegnati al Docente
			const tuttiCorsi = await FormazioneService.getAllCorsi();

			// FILTRO: Visualizziamo solo i corsi operativi (Attivi)
			const mieiCorsiAttivi = tuttiCorsi.filter(c =>
					c.idDocente === session.idUtente &&
					(c.stato === StatoCorso.PROGRAMMATO || c.stato === StatoCorso.IN_SVOLGIMENTO)
			);

			corsiUnici = mieiCorsiAttivi.map(c => ({ id: c.idCorso, titolo: c.titolo }));

			// 2. Fetch delle iscrizioni per i corsi attivi
			const iscrizioniPromises = mieiCorsiAttivi.map(corso =>
					FormazioneService.getIscrizioniByCorso(corso.idCorso)
			);

			const iscrizioniResults = await Promise.all(iscrizioniPromises);
			const iscrizioniFlat = iscrizioniResults.flat();

			// 3. Raggruppo per studente
			const mappaStudenti = new Map<number, StudenteRaggruppato>();

			iscrizioniFlat.forEach(iscrizione => {
				if (!mappaStudenti.has(iscrizione.idUtente)) {
					mappaStudenti.set(iscrizione.idUtente, {
						idUtente: iscrizione.idUtente,
						emailUtente: iscrizione.emailUtente,
						iscrizioni: []
					});
				}

				mappaStudenti.get(iscrizione.idUtente)!.iscrizioni.push({
					idCorso: iscrizione.idCorso,
					titoloCorso: iscrizione.titoloCorso
				});
			});

			studentiRaggruppati = Array.from(mappaStudenti.values());

		} catch (error) {
			console.error("Errore nel caricamento degli studenti:", error);
		} finally {
			isLoading = false;
		}
	});

	// Filtro reattivo
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
</script>

<div in:fade class="space-y-8 max-w-7xl mx-auto pb-20">
	<div class="flex flex-col md:flex-row justify-between items-start md:items-center gap-6">
		<div>
			<h1 class="text-4xl font-black text-[#1B4B6B] uppercase tracking-tighter">I Miei Studenti</h1>
			<p class="text-gray-400 font-bold uppercase text-[10px] tracking-widest mt-1">Monitoraggio allievi iscritti ai tuoi corsi attivi</p>
		</div>

		<div class="bg-white px-6 py-4 rounded-3xl shadow-sm border border-gray-100 flex items-center gap-4">
			<div class="p-3 bg-blue-50 rounded-2xl text-[#1B4B6B]">
				<Users size={24} />
			</div>
			<div>
				<p class="text-[10px] font-black text-gray-300 uppercase tracking-widest">Studenti Attivi</p>
				<p class="text-lg font-black text-[#1B4B6B] uppercase">{studentiRaggruppati.length} ISCRITTI</p>
			</div>
		</div>
	</div>

	<div class="bg-white p-4 rounded-[2.5rem] shadow-sm border border-gray-100 flex flex-col lg:flex-row gap-4">
		<div class="relative flex-1 group">
			<Search class="absolute left-6 top-1/2 -translate-y-1/2 text-gray-400 group-focus-within:text-[#1B4B6B] transition-colors" size={20} />
			<input
					bind:value={queryRicerca}
					type="text"
					placeholder="CERCA STUDENTE..."
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
					{#each [] as _}{/each}
				{/each}
			</select>
		</div>
	</div>

	{#if isLoading}
		<div class="py-32 flex flex-col items-center justify-center gap-4">
			<Loader2 size={48} class="animate-spin text-[#1B4B6B]" />
			<span class="text-[10px] font-black text-gray-400 uppercase tracking-widest">Caricamento elenco studenti...</span>
		</div>
	{:else if studentiFiltrati.length === 0}
		<div class="py-32 bg-white rounded-[2.5rem] border border-gray-100 border-dashed flex flex-col items-center justify-center text-center">
			<Users size={64} class="text-gray-200 mb-6" />
			<h3 class="font-black text-[#1B4B6B] uppercase text-2xl">Nessun iscritto trovato</h3>
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
                             ID: #{studente.idUtente}
                         </span>
							</div>
						</div>
						<a href="/dashboard/docente/messaggi?chatId={studente.idUtente}" class="flex items-center justify-center gap-2 bg-[#1B4B6B] text-white px-8 py-4 rounded-2xl text-[10px] font-black uppercase hover:bg-[#153a54] transition-all shadow-lg shadow-[#1B4B6B]/20">
							<MessageSquare size={18} /> Apri Chat
						</a>
					</div>

					<div class="grid grid-cols-1 md:grid-cols-2 xl:grid-cols-3 gap-4">
						{#each studente.iscrizioni as iscrizione (iscrizione.idCorso)}
							{#if filtroCorso === '' || iscrizione.idCorso.toString() === filtroCorso}
								<div class="flex items-center gap-3 p-4 rounded-xl bg-gray-50 border border-gray-100/50">
									<div class="p-2 bg-white rounded-lg shadow-sm border border-gray-100 text-[#1B4B6B] shrink-0">
										<BookOpen size={16} />
									</div>
									<h5 class="font-bold text-[#1B4B6B] text-[11px] uppercase truncate">{iscrizione.titoloCorso}</h5>
									<ChevronRight size={14} class="ml-auto text-gray-300" />
								</div>
							{/if}
						{/each}
					</div>
				</div>
			{/each}
		</div>
	{/if}
</div>

<style>
	:global(body) { background-color: #F9FAFB; }
</style>