<script lang="ts">
	import { onMount } from 'svelte';
	import { fade, scale } from 'svelte/transition';
	import {
		Users, Search, Filter, MessageSquare, BookOpen, Loader2, ChevronRight, GraduationCap
	} from 'lucide-svelte';

	// IMPORT SERVIZI E MODELLI
	import { AuthService } from '$lib/services/AuthService';
	import { FormazioneService } from '$lib/services/FormazioneService';
	import { StatoCorso } from '$lib/models/Enums';

	// Interfacce aggiornate per raggruppamento per Corso
	interface StudenteDettaglio {
		idUtente: number;
		emailUtente: string;
	}

	interface CorsoRaggruppato {
		idCorso: number;
		titoloCorso: string;
		studenti: StudenteDettaglio[];
	}

	// STATO REATTIVO (Svelte 5)
	let isLoading = $state(true);
	let corsiRaggruppati = $state<CorsoRaggruppato[]>([]);
	let queryRicerca = $state('');
	let filtroCorso = $state('');
	let totaleStudentiUnici = $state(0);

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

			// 2. Costruisco la struttura raggruppata per Corso
			const mappaCorsi = new Map<number, CorsoRaggruppato>();
			const setStudentiUnici = new Set<number>();

			for (const corso of mieiCorsiAttivi) {
				const iscritti = await FormazioneService.getIscrizioniByCorso(corso.idCorso);

				mappaCorsi.set(corso.idCorso, {
					idCorso: corso.idCorso,
					titoloCorso: corso.titolo || "Corso senza titolo",
					studenti: iscritti.map(i => {
						setStudentiUnici.add(i.idUtente);
						return {
							idUtente: i.idUtente,
							emailUtente: i.emailUtente
						};
					})
				});
			}

			corsiRaggruppati = Array.from(mappaCorsi.values());
			totaleStudentiUnici = setStudentiUnici.size;

		} catch (error) {
			console.error("Errore nel caricamento degli studenti:", error);
		} finally {
			isLoading = false;
		}
	});

	// Filtro reattivo (Filtra i corsi, e all'interno dei corsi filtra gli studenti se c'è una query)
	const corsiFiltrati = $derived(
			corsiRaggruppati
					.filter(corso => filtroCorso === '' || corso.idCorso.toString() === filtroCorso)
					.map(corso => {
						// Filtro interno: mantengo solo gli studenti che matchano la ricerca testo (o tutti se vuota)
						const studentiMatch = corso.studenti.filter(stud =>
								stud.emailUtente.toLowerCase().includes(queryRicerca.toLowerCase())
						);
						return { ...corso, studenti: studentiMatch };
					})
					// Escludo i corsi che, dopo il filtro studenti, sono vuoti (a meno che la query non sia vuota, ma è meglio nasconderli se non c'è chi cerchi)
					.filter(corso => corso.studenti.length > 0)
	);

	function getIniziale(email: string) {
		return email ? email.charAt(0).toUpperCase() : 'S';
	}
</script>

<div in:fade class="space-y-8 max-w-7xl mx-auto pb-20">
	<div class="flex flex-col md:flex-row justify-between items-start md:items-center gap-6">
		<div>
			<h1 class="text-4xl font-black text-[#1B4B6B] uppercase tracking-tighter">I Miei Studenti</h1>
			<p class="text-gray-400 font-bold uppercase text-[10px] tracking-widest mt-1">Classi e allievi dei corsi attivi</p>
		</div>

		<div class="bg-white px-6 py-4 rounded-3xl shadow-sm border border-gray-100 flex items-center gap-4">
			<div class="p-3 bg-blue-50 rounded-2xl text-[#1B4B6B]">
				<Users size={24} />
			</div>
			<div>
				<p class="text-[10px] font-black text-gray-300 uppercase tracking-widest">Studenti Totali</p>
				<p class="text-lg font-black text-[#1B4B6B] uppercase">{totaleStudentiUnici} ISCRITTI UNICI</p>
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
				{#each corsiRaggruppati as corso (corso.idCorso)}
					<option value={corso.idCorso.toString()}>{corso.titoloCorso}</option>
				{/each}
			</select>
		</div>
	</div>

	{#if isLoading}
		<div class="py-32 flex flex-col items-center justify-center gap-4">
			<Loader2 size={48} class="animate-spin text-[#1B4B6B]" />
			<span class="text-[10px] font-black text-gray-400 uppercase tracking-widest">Caricamento elenco corsi...</span>
		</div>
	{:else if corsiFiltrati.length === 0}
		<div class="py-32 bg-white rounded-[2.5rem] border border-gray-100 border-dashed flex flex-col items-center justify-center text-center">
			<GraduationCap size={64} class="text-gray-200 mb-6" />
			<h3 class="font-black text-[#1B4B6B] uppercase text-2xl">Nessun risultato</h3>
			<p class="text-gray-400 font-bold uppercase text-[10px] tracking-widest mt-2">Non ci sono studenti che corrispondono ai criteri di ricerca</p>
		</div>
	{:else}
		<div class="space-y-10">
			{#each corsiFiltrati as corso (corso.idCorso)}
				<div in:scale={{duration: 300}} class="bg-white rounded-[2.5rem] border border-[#1B4B6B]/10 shadow-sm overflow-hidden flex flex-col">

					<!-- HEADER CORSO -->
					<div class="bg-gray-50/80 p-8 border-b border-gray-100 flex items-center justify-between">
						<div class="flex items-center gap-4">
							<div class="p-3 bg-white border border-gray-200 text-[#1B4B6B] rounded-2xl shadow-sm">
								<BookOpen size={24} />
							</div>
							<div>
								<h2 class="font-black text-[#1B4B6B] uppercase text-xl tracking-tight leading-none">{corso.titoloCorso}</h2>
								<p class="text-[10px] font-bold text-gray-500 uppercase mt-2 tracking-widest flex items-center gap-2">
									<Users size={12}/> {corso.studenti.length} iscritti in questa classe
								</p>
							</div>
						</div>
					</div>

					<!-- LISTA STUDENTI DEL CORSO -->
					<div class="p-8">
						<div class="grid grid-cols-1 lg:grid-cols-2 gap-4">
							{#each corso.studenti as studente (studente.idUtente)}
								<div class="flex items-center justify-between p-5 rounded-2xl border border-gray-100 hover:border-[#1B4B6B]/30 hover:shadow-md transition-all bg-white group">
									<div class="flex items-center gap-4">
										<div class="w-12 h-12 rounded-xl bg-[#1B4B6B]/10 text-[#1B4B6B] flex items-center justify-center font-black text-lg group-hover:bg-[#1B4B6B] group-hover:text-white transition-colors">
											{getIniziale(studente.emailUtente)}
										</div>
										<div>
											<h4 class="font-bold text-[#1B4B6B] uppercase text-sm truncate max-w-[200px] xl:max-w-[300px]" title={studente.emailUtente}>
												{studente.emailUtente}
											</h4>
											<p class="text-[9px] font-black text-gray-400 uppercase tracking-widest mt-1">ID: #{studente.idUtente}</p>
										</div>
									</div>

									<!-- TASTO CONTATTA: Bianco -> Blu -->
									<a href="/dashboard/docente/messaggi?chatId={studente.idUtente}"
									   class="flex items-center justify-center gap-2 bg-white text-[#1B4B6B] border-2 border-[#1B4B6B] px-5 py-2.5 rounded-xl text-[10px] font-black uppercase hover:bg-[#1B4B6B] hover:text-white transition-all shadow-sm">
										<MessageSquare size={14} /> Contatta
									</a>
								</div>
							{/each}
						</div>
					</div>
				</div>
			{/each}
		</div>
	{/if}
</div>

<style>
	:global(body) { background-color: #F9FAFB; }
</style>