<script lang="ts">
	import { onMount } from 'svelte';
	import { fade, scale } from 'svelte/transition';
	import {
		Users, Search, Filter, BookOpen, Loader2, GraduationCap, ChevronLeft, IdCard, Mail
	} from 'lucide-svelte';
	import { SvelteMap, SvelteSet } from 'svelte/reactivity';
	import { AuthService } from '$lib/services/AuthService';
	import { FormazioneService } from '$lib/services/FormazioneService';
	import { StatoCorso } from '$lib/models/Enums';
	import StatCard from '$lib/Components/UI/StatCard.svelte';
	import DipendenteCard from '$lib/Components/Features/Anagrafica/DipendenteCard.svelte';
	import DettagliCard from '$lib/Components/Features/Anagrafica/DettagliCard.svelte';
	import { caricaDettagliEntita } from '$lib/utils/dettaglioUtils';

	/*
Modulo Gestione Studenti (Docent Panel).
Permette al docente di visualizzare l'elenco degli iscritti raggruppati per corso,
effettuare ricerche cross-corso e visualizzare il profilo anagrafico di dettaglio
di ogni studente. Integra logiche di filtraggio reattivo per una gestione snella di classi numerose.
*/
	interface StudenteDettaglio {
		idUtente: number;
		emailUtente: string;
	}

	interface StudenteCompleto {
		idUtente: number;
		email: string;
		nome: string;
		cognome: string;
		codiceFiscale: string;
	}

	interface CorsoRaggruppato {
		idCorso: number;
		titoloCorso: string;
		studenti: StudenteDettaglio[];
	}

	let isLoading = $state(true);
	let isLoadingDettaglio = $state(false);
	let corsiRaggruppati = $state<CorsoRaggruppato[]>([]);
	let queryRicerca = $state('');
	let filtroCorso = $state('');
	let totaleStudentiUnici = $state(0);

	let selectedStudente = $state<StudenteCompleto | null>(null);

	// Recupera tutti i corsi del docente, mappa gli iscritti e calcola il conteggio studenti unici
	onMount(async () => {
		const session = AuthService.getSession();
		if (!session) return;

		try {
			const tuttiCorsi = await FormazioneService.getAllCorsi();
			const mieiCorsiAttivi = tuttiCorsi.filter(c =>
					c.idDocente === session.idUtente &&
					(c.stato === StatoCorso.PROGRAMMATO || c.stato === StatoCorso.IN_SVOLGIMENTO)
			);

			let mappaCorsi = new SvelteMap<number, CorsoRaggruppato>();
			let setStudentiUnici = new SvelteSet<number>();

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

	const corsiFiltrati = $derived(
			corsiRaggruppati
					.filter(corso => filtroCorso === '' || corso.idCorso.toString() === filtroCorso)
					.map(corso => {
						const studentiMatch = corso.studenti.filter(stud =>
								stud.emailUtente.toLowerCase().includes(queryRicerca.toLowerCase())
						);
						return { ...corso, studenti: studentiMatch };
					})
					.filter(corso => corso.studenti.length > 0)
	);

	// Recupera i dati anagrafici approfonditi dello studente selezionato dal backend
	async function apriDettaglioStudente(studente: StudenteDettaglio) {
		isLoadingDettaglio = true;
		const res = await caricaDettagliEntita(studente.idUtente, 'STUDENTE') as any;

		if (res.error) {
			alert("Impossibile recuperare il profilo dello studente.");
			selectedStudente = null;
		} else {
			selectedStudente = {
				idUtente: studente.idUtente,
				email: res.info?.email || studente.emailUtente,
				nome: res.info?.nome || 'Studente',
				cognome: res.info?.cognome || 'Non Trovato',
				codiceFiscale: res.info?.codiceFiscale || 'N.D.'
			};
		}
		isLoadingDettaglio = false;
	}

	const dettagliStudenteItems = $derived(
			selectedStudente ? [
				{ label: 'Codice Fiscale', value: selectedStudente.codiceFiscale, icon: IdCard as any, isMono: true },
				{ label: 'Email di Contatto', value: selectedStudente.email, icon: Mail as any }
			] : []
	);
</script>

<div in:fade class="space-y-6 md:space-y-8 max-w-7xl mx-auto pb-20 p-4 md:p-6">
	<!-- Vista a elenco (Stato default) -->
	{#if !selectedStudente}
		<div class="flex flex-col lg:flex-row justify-between items-start lg:items-center gap-6">
			<div>
				<h1 class="text-2xl md:text-4xl font-black text-[#1B4B6B] uppercase tracking-tighter leading-tight">I Miei Studenti</h1>
				<p class="text-gray-400 font-bold uppercase text-[10px] tracking-widest mt-1">Classi e allievi dei corsi attivi</p>
			</div>
			<div class="w-full lg:w-auto">
				<!-- Barra di ricerca e Filtro (dropdown corsi) -->
				<StatCard
						titolo="Iscritti Unici"
						valore={totaleStudentiUnici}
						icona={Users}
						bgIcona="bg-blue-50"
						testoIcona="text-[#1B4B6B]"
				/>
			</div>
		</div>

		<div class="bg-white p-3 md:p-4 rounded-2xl md:rounded-[2.5rem] shadow-sm border border-gray-100 flex flex-col md:flex-row gap-3 md:gap-4">
			<div class="relative flex-1 group">
				<Search class="absolute left-4 md:left-6 top-1/2 -translate-y-1/2 text-gray-400 group-focus-within:text-[#1B4B6B] transition-colors" size={20} />
				<input
						bind:value={queryRicerca}
						type="text"
						placeholder="CERCA STUDENTE..."
						class="w-full bg-gray-50 border-none rounded-xl md:rounded-2xl py-3.5 md:py-5 pl-12 md:pl-16 pr-6 text-xs font-bold text-[#1B4B6B] focus:ring-2 focus:ring-[#1B4B6B]/20 transition-all uppercase outline-none"
				/>
			</div>
			<div class="relative w-full md:w-auto md:min-w-[300px]">
				<Filter class="absolute left-4 md:left-6 top-1/2 -translate-y-1/2 text-gray-400" size={20} />
				<select
						bind:value={filtroCorso}
						class="w-full bg-gray-50 border-none rounded-xl md:rounded-2xl py-3.5 md:py-5 pl-12 md:pl-16 pr-10 text-xs font-bold text-[#1B4B6B] focus:ring-2 focus:ring-[#1B4B6B]/20 transition-all uppercase outline-none appearance-none cursor-pointer"
				>
					<option value="">FILTRA PER CORSO (TUTTI)</option>
					{#each corsiRaggruppati as corso (corso.idCorso)}
						<option value={corso.idCorso.toString()}>{corso.titoloCorso}</option>
					{/each}
				</select>
			</div>
		</div>

		{#if isLoading}
			<div class="py-24 md:py-32 flex flex-col items-center justify-center gap-4">
				<Loader2 size={48} class="animate-spin text-[#1B4B6B]" />
				<span class="text-[10px] font-black text-gray-400 uppercase tracking-widest">Caricamento elenco corsi...</span>
			</div>
		{:else if corsiFiltrati.length === 0}
			<div class="py-20 md:py-32 bg-white rounded-3xl md:rounded-[2.5rem] border border-gray-100 border-dashed flex flex-col items-center justify-center text-center p-6">
				<GraduationCap size={48} class="md:w-16 md:h-16 text-gray-200 mb-4 md:mb-6" />
				<h3 class="font-black text-[#1B4B6B] uppercase text-xl md:text-2xl">Nessun risultato</h3>
				<p class="text-gray-400 font-bold uppercase text-[9px] md:text-[10px] tracking-widest mt-2 max-w-xs md:max-w-none">Non ci sono studenti che corrispondono ai criteri di ricerca</p>
			</div>
		{:else}
			<div class="space-y-6 md:space-y-10">
				{#each corsiFiltrati as corso (corso.idCorso)}
					<div in:scale={{duration: 300}} class="bg-white rounded-3xl md:rounded-[2.5rem] border border-[#1B4B6B]/10 shadow-sm overflow-hidden flex flex-col">
						<div class="bg-gray-50/80 p-5 md:p-8 border-b border-gray-100 flex items-center justify-between">
							<div class="flex items-center gap-3 md:gap-4 min-w-0">
								<div class="p-2.5 md:p-3 bg-white border border-gray-200 text-[#1B4B6B] rounded-xl md:rounded-2xl shadow-sm shrink-0">
									<BookOpen size={20} class="md:w-6 md:h-6" />
								</div>
								<div class="min-w-0">
									<h2 class="font-black text-[#1B4B6B] uppercase text-base md:text-xl tracking-tight leading-tight truncate">{corso.titoloCorso}</h2>
									<p class="text-[9px] md:text-[10px] font-bold text-gray-500 uppercase mt-1 md:mt-2 tracking-widest flex items-center gap-2">
										<Users size={12}/> {corso.studenti.length} iscritti in questa classe
									</p>
								</div>
							</div>
						</div>
						<div class="p-4 md:p-8">
							<div class="grid grid-cols-1 lg:grid-cols-2 gap-4">
								{#each corso.studenti as studente (studente.idUtente)}
									<!-- Card studente: triggera l'apertura del dettaglio o l'avvio chat -->
									<DipendenteCard
											idUtente={studente.idUtente}
											nome={studente.emailUtente}
											cognome=""
											ruolo="Studente"
											canEdit={false}
											canDelete={false}
											canContact={true}
											canViewDetails={true}
											onViewDetails={() => apriDettaglioStudente(studente)}
											onContact={() => window.location.href = `/dashboard/docente/messaggi?chatId=${studente.idUtente}`}
									/>
								{/each}
							</div>
						</div>
					</div>
				{/each}
			</div>
		{/if}
	{:else}
		<div in:fade>
			<button onclick={() => (selectedStudente = null)} class="flex items-center gap-2 text-[#1B4B6B] font-extrabold uppercase text-[10px] mb-6 md:mb-8 hover:gap-3 transition-all">
				<ChevronLeft size={16} /> Torna all'elenco dei corsi
			</button>

			{#if isLoadingDettaglio}
				<div class="py-20 flex flex-col items-center justify-center gap-4">
					<Loader2 size={48} class="animate-spin text-[#1B4B6B]" />
					<span class="text-[10px] font-black text-gray-400 uppercase tracking-widest">Recupero dati studente...</span>
				</div>
			{:else}
				<DettagliCard
						nome={selectedStudente.nome}
						cognome={selectedStudente.cognome}
						sottotitolo="Studente / Corsista"
						items={dettagliStudenteItems}
				/>
			{/if}
		</div>
	{/if}
</div>

<style>
	:global(body) { background-color: #F9FAFB; }
</style>