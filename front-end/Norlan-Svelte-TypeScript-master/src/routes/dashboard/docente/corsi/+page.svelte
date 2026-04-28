<script lang="ts">
	import { onMount } from 'svelte';
	import { fade, scale } from 'svelte/transition';
	import {
		BookOpen, Search, Filter, Clock, MapPin, Users, FileText, ArrowRight, Loader2
	} from 'lucide-svelte';

	// IMPORT SERVIZI E MODELLI UFFICIALI
	import type { CorsoFormazione } from '$lib/models/CorsoFormazione';
	import { StatoCorso } from '$lib/models/Enums';
	import { AuthService } from '$lib/services/AuthService';
	import { FormazioneService } from '$lib/services/FormazioneService';

	// STATO CON RUNE SVELTE 5
	let isLoading = $state(true);
	let corsi = $state<CorsoFormazione[]>([]);
	let queryRicerca = $state('');

	// Tipizzato con l'enum o stringa vuota per il reset
	let filtroStato = $state<StatoCorso | ''>('');

	onMount(async () => {
		const session = AuthService.getSession(); //
		if (!session) return;

		try {
			// Recupero l'elenco completo dei corsi dal backend
			const tuttiCorsi = await FormazioneService.getAllCorsi();

			// Filtro i corsi per mostrare solo quelli assegnati al docente loggato
			corsi = tuttiCorsi.filter(c => c.idDocente === session.idUtente);
		} catch (error) {
			console.error("Errore durante il recupero dei corsi assegnati:", error);
		} finally {
			isLoading = false;
		}
	});

	// LOGICA DI FILTRO REATTIVA
	const corsiFiltrati = $derived(
			corsi.filter(c => {
				const matchTesto = c.titolo.toLowerCase().includes(queryRicerca.toLowerCase());
				const matchStato = filtroStato === '' || c.stato === filtroStato;
				return matchTesto && matchStato;
			})
	);

	function formattaData(stringaIso: string) {
		if (!stringaIso) return 'Data non definita';
		const d = new Date(stringaIso);
		return d.toLocaleDateString('it-IT', { day: '2-digit', month: 'short', year: 'numeric', hour: '2-digit', minute: '2-digit' });
	}
</script>

<div in:fade class="space-y-8">
	<div class="flex flex-col md:flex-row justify-between items-start md:items-center gap-6">
		<div>
			<h1 class="text-4xl font-black text-[#1B4B6B] uppercase tracking-tighter">Corsi Assegnati</h1>
			<p class="text-gray-400 font-bold uppercase text-[10px] tracking-widest mt-1">Monitoraggio e gestione della formazione attiva</p>
		</div>

		<div class="flex gap-4">
			<div class="bg-white px-6 py-4 rounded-2xl shadow-sm border border-gray-100 flex items-center gap-4">
				<div class="p-2 bg-blue-50 rounded-xl text-[#1B4B6B]">
					<BookOpen size={20} />
				</div>
				<div>
					<p class="text-[9px] font-black text-gray-300 uppercase tracking-widest">Totale</p>
					<p class="text-sm font-black text-[#1B4B6B] uppercase">{corsi.length} Corsi</p>
				</div>
			</div>
		</div>
	</div>

	<div class="bg-white p-4 rounded-3xl shadow-sm border border-gray-100 flex flex-col lg:flex-row gap-4">
		<div class="relative flex-1 group">
			<Search class="absolute left-4 top-1/2 -translate-y-1/2 text-gray-400 group-focus-within:text-[#1B4B6B] transition-colors" size={20} />
			<input
					bind:value={queryRicerca}
					type="text"
					placeholder="CERCA PER TITOLO CORSO..."
					class="w-full bg-gray-50 border-none rounded-2xl py-4 pl-12 pr-6 text-xs font-bold text-[#1B4B6B] placeholder:text-gray-300 focus:ring-4 focus:ring-[#1B4B6B]/5 transition-all uppercase outline-none"
			/>
		</div>
		<div class="relative min-w-[240px]">
			<Filter class="absolute left-4 top-1/2 -translate-y-1/2 text-gray-400" size={20} />
			<select
					bind:value={filtroStato}
					class="w-full bg-gray-50 border-none rounded-2xl py-4 pl-12 pr-10 text-xs font-bold text-[#1B4B6B] focus:ring-4 focus:ring-[#1B4B6B]/5 transition-all uppercase outline-none appearance-none cursor-pointer"
			>
				<option value="">TUTTI GLI STATI</option>
				<option value={StatoCorso.PROGRAMMATO}>PROGRAMMATI</option>
				<option value={StatoCorso.IN_SVOLGIMENTO}>IN SVOLGIMENTO</option>
				<option value={StatoCorso.CONCLUSO}>CONCLUSI</option>
			</select>
		</div>
	</div>

	{#if isLoading}
		<div class="py-32 flex flex-col items-center justify-center gap-4">
			<Loader2 size={48} class="animate-spin text-[#1B4B6B]" />
			<span class="text-[10px] font-black text-gray-400 uppercase tracking-[0.2em]">Caricamento registri didattici...</span>
		</div>
	{:else if corsiFiltrati.length === 0}
		<div class="py-32 bg-white rounded-3xl border border-gray-100 border-dashed flex flex-col items-center justify-center text-center">
			<BookOpen size={48} class="text-gray-200 mb-4" />
			<h3 class="font-black text-[#1B4B6B] uppercase">Nessun corso trovato</h3>
			<p class="text-[10px] font-bold text-gray-400 uppercase mt-1">Prova a cambiare i filtri di ricerca</p>
		</div>
	{:else}
		<div class="grid grid-cols-1 xl:grid-cols-2 2xl:grid-cols-3 gap-8">
			{#each corsiFiltrati as corso (corso.idCorso)}
				<div
						in:scale={{duration: 400}}
						class="bg-white rounded-[2rem] border border-gray-100 shadow-sm hover:shadow-2xl hover:border-[#1B4B6B]/20 transition-all duration-500 overflow-hidden group flex flex-col"
				>
					<div class="p-6 border-b border-gray-50 flex justify-between items-center bg-gray-50/30">
						<div class="flex items-center gap-3">
							<div class="w-10 h-10 rounded-xl bg-white shadow-sm flex items-center justify-center text-[#1B4B6B] group-hover:bg-[#1B4B6B] group-hover:text-white transition-colors duration-500">
								<BookOpen size={20} />
							</div>
							<span class="text-[10px] font-black text-gray-300 uppercase tracking-widest">#{corso.idCorso}</span>
						</div>
						<div class="px-3 py-1 rounded-lg text-[9px] font-black uppercase border
    						{corso.stato === StatoCorso.IN_SVOLGIMENTO ? 'bg-amber-50 text-amber-600 border-amber-100' :
    							 corso.stato === StatoCorso.PROGRAMMATO ? 'bg-blue-50 text-blue-600 border-blue-100' :
    							 !corso.stato ? 'bg-gray-50 text-gray-500 border-gray-200' :
     							'bg-emerald-50 text-emerald-600 border-emerald-100'}"
							>
							{corso.stato ? corso.stato.replace('_', ' ') : 'STATO MANCANTE'}
						</div>
					</div>

					<div class="p-8 flex-1 space-y-6">
						<h3 class="text-xl font-black text-[#1B4B6B] uppercase leading-tight group-hover:text-blue-700 transition-colors">
							{corso.titolo}
						</h3>

						<div class="grid grid-cols-1 gap-4">
							<div class="flex items-center gap-4">
								<div class="w-10 h-10 rounded-xl bg-gray-50 flex items-center justify-center text-[#1B4B6B] shrink-0">
									<Clock size={18} />
								</div>
								<div>
									<p class="text-[9px] font-black text-gray-300 uppercase tracking-widest">Orario Lezione</p>
									<p class="text-xs font-bold text-[#1B4B6B] uppercase">{formattaData(corso.dataOrario)}</p>
								</div>
							</div>
							<div class="flex items-center gap-4">
								<div class="w-10 h-10 rounded-xl bg-gray-50 flex items-center justify-center text-[#1B4B6B] shrink-0">
									<MapPin size={18} />
								</div>
								<div>
									<p class="text-[9px] font-black text-gray-300 uppercase tracking-widest">Sede Formativa</p>
									<p class="text-xs font-bold text-[#1B4B6B] uppercase">{corso.luogoFisico}</p>
								</div>
							</div>
							<div class="flex items-center gap-4">
								<div class="w-10 h-10 rounded-xl bg-gray-50 flex items-center justify-center text-[#1B4B6B] shrink-0">
									<Users size={18} />
								</div>
								<div>
									<p class="text-[9px] font-black text-gray-300 uppercase tracking-widest">Capacità Aula</p>
									<p class="text-xs font-bold text-[#1B4B6B] uppercase">{corso.capacitaMassima} Posti Disponibili</p>
								</div>
							</div>
						</div>
					</div>

					<div class="p-4 bg-gray-50/50 border-t border-gray-100 flex gap-3">
						<button class="flex-1 bg-white border border-gray-200 py-4 rounded-2xl text-[10px] font-black text-[#1B4B6B] uppercase hover:bg-gray-100 transition-all flex items-center justify-center gap-2">
							<FileText size={16} />
							Materiale
						</button>
						<a
								href="/dashboard/docente/corsi/{corso.idCorso}"
								class="flex-1 bg-[#1B4B6B] py-4 rounded-2xl text-[10px] font-black text-white uppercase hover:bg-[#153a54] transition-all flex items-center justify-center gap-2 shadow-lg shadow-[#1B4B6B]/10"
						>
							Gestisci
							<ArrowRight size={16} />
						</a>
					</div>
				</div>
			{/each}
		</div>
	{/if}
</div>

<style>
	:global(body) { background-color: #F9FAFB; }
</style>