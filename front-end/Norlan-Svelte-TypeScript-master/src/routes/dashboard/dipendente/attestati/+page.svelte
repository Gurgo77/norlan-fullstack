<script lang="ts">
	import { onMount } from 'svelte';
	import { fade, scale } from 'svelte/transition';
	import {
		FileBadge, Download, Search, Loader2,
		FileText, Calendar, CheckCircle2, Info
	} from 'lucide-svelte';

	// IMPORT MODELLI E SERVIZI UFFICIALI
	import type { IscrizioneCorso } from '$lib/models/IscrizioneCorso';
	import { AuthService } from '$lib/services/AuthService';
	import { LavoratoreService, type DipendenteDTO } from '$lib/services/LavoratoreService';
	import { FormazioneService } from '$lib/services/FormazioneService';
	import httpClient from '$lib/api/httpClient';

	// 1. STATO CON RUNE (Svelte 5)
	let isLoading = $state(true);
	let searchQuery = $state('');

	let utente = $state<DipendenteDTO | null>(null);
	let iscrizioni = $state<IscrizioneCorso[]>([]);

	/**
	 * Funzione per il download fisico del file PDF
	 */
	async function scaricaAttestato(idCorso: number) {
		if (!utente) return;
		try {
			// Utilizziamo l'endpoint che abbiamo creato nel backend
			const response = await httpClient.get(`/formazione/corsi/${idCorso}/iscrizioni/${utente.idUtente}/certificato/download`, {
				responseType: 'blob'
			});

			const url = URL.createObjectURL(response.data);
			const a = document.createElement('a');
			a.href = url;
			a.download = `Attestato_Corso_${idCorso}.pdf`;
			document.body.appendChild(a);
			a.click();
			document.body.removeChild(a);
			URL.revokeObjectURL(url);
		} catch (e) {
			console.error("Errore download", e);
			alert("Errore nel download dell'attestato. Il file potrebbe non essere ancora disponibile o l'azienda non ha completato le procedure di firma.");
		}
	}

	onMount(async () => {
		const session = AuthService.getSession();
		if (!session) return;

		try {
			// Fetch parallelo
			const [dipendenteData, iscrizioniData] = await Promise.all([
				LavoratoreService.getById(session.idUtente),
				FormazioneService.getIscrizioniUtente(session.idUtente)
			]);

			utente = dipendenteData;
			iscrizioni = iscrizioniData;
		} catch (error) {
			console.error("Errore durante il recupero dei dati degli attestati:", error);
		} finally {
			isLoading = false;
		}
	});

	/**
	 * LOGICA REATTIVA FSM
	 * Un attestato è disponibile per il dipendente se e solo se:
	 * 1. L'admin ha validato la presenza (presenzaConfermata = true)
	 * 2. L'azienda ha completato la Macchina a Stati Documentale (il backend DTO ha popolato il pathAttestato o il documento)
	 * * Nota: Nel DTO di IscrizioneCorso che abbiamo modificato nel backend (convertToDTO),
	 * pathAttestato viene popolato solo se il DocumentoAttestato esiste!
	 */
	const attestatiDisponibili = $derived(
			iscrizioni.filter(i =>
					i.presenzaConfermata === true &&
					i.pathAttestato !== null &&
					i.pathAttestato !== undefined &&
					i.pathAttestato.trim() !== '' &&
					i.titoloCorso.toLowerCase().includes(searchQuery.toLowerCase())
			)
	);

	function formattaData(dataIso: string) {
		return new Date(dataIso).toLocaleDateString('it-IT', {
			day: '2-digit', month: 'long', year: 'numeric'
		}).toUpperCase();
	}
</script>

<div in:fade class="max-w-7xl mx-auto space-y-8 pb-10">

	<div class="flex flex-col md:flex-row justify-between items-start md:items-end gap-6">
		<div>
			<h1 class="text-4xl font-black text-[#1B4B6B] uppercase tracking-tighter">I Miei Attestati</h1>
			<p class="text-gray-400 font-bold uppercase text-[10px] tracking-widest mt-1">
				Dipendente: <span class="text-[#1B4B6B]">{utente?.nome || '...'} {utente?.cognome || ''}</span>
			</p>
		</div>

		<div class="bg-emerald-50 border border-emerald-100 px-6 py-4 rounded-[2rem] flex items-center gap-4">
			<div class="p-2 bg-emerald-500 text-white rounded-xl shadow-lg shadow-emerald-500/20">
				<CheckCircle2 size={20} />
			</div>
			<p class="text-sm font-black text-[#1B4B6B] uppercase leading-none">Formazione in Regola</p>
		</div>
	</div>

	<div class="relative group max-w-md">
		<Search class="absolute left-4 top-1/2 -translate-y-1/2 text-gray-300 group-focus-within:text-[#1B4B6B] transition-colors" size={20} />
		<input
				bind:value={searchQuery}
				type="text"
				placeholder="CERCA PER TITOLO CORSO..."
				class="w-full pl-12 pr-6 py-4 bg-white border border-gray-100 rounded-[1.5rem] text-xs font-bold uppercase outline-none focus:ring-4 focus:ring-[#1B4B6B]/5 shadow-sm transition-all"
		/>
	</div>

	{#if isLoading}
		<div class="py-32 flex flex-col items-center justify-center gap-4">
			<Loader2 size={48} class="animate-spin text-[#1B4B6B]" />
			<span class="text-[10px] font-black text-gray-400 uppercase tracking-widest">Apertura archivio...</span>
		</div>
	{:else}
		<div class="grid grid-cols-1 gap-4">
			{#each attestatiDisponibili as iscrizione (iscrizione.idCorso)}
				<div
						in:scale={{duration: 200}}
						class="bg-white p-6 rounded-[2.5rem] border border-gray-100 shadow-sm flex flex-col md:flex-row items-center justify-between gap-6 hover:shadow-xl hover:border-blue-100 transition-all group"
				>
					<div class="flex items-center gap-6 flex-1 min-w-0">
						<div class="w-16 h-16 rounded-2xl flex items-center justify-center shrink-0 bg-amber-50 text-amber-500">
							<FileBadge size={30} />
						</div>

						<div class="min-w-0">
							<div class="flex items-center gap-2 mb-1">
								<span class="text-[8px] font-black uppercase tracking-widest px-2 py-0.5 rounded bg-emerald-50 text-emerald-600">Certificato Convalidato Azienda</span>
							</div>
							<h3 class="text-base font-black text-[#1B4B6B] uppercase truncate group-hover:text-blue-700 transition-colors">
								{iscrizione.titoloCorso}
							</h3>
							<div class="flex items-center gap-4 mt-2">
								<div class="flex items-center gap-1.5 text-[9px] font-bold text-gray-400 uppercase">
									<Calendar size={12} /> Conseguito il: {formattaData(iscrizione.dataOrarioCorso)}
								</div>
							</div>
						</div>
					</div>

					<div class="flex items-center gap-3 w-full md:w-auto">
						<button class="flex-1 md:flex-none p-4 bg-gray-50 text-[#1B4B6B] rounded-2xl hover:bg-gray-100 transition-all">
							<Info size={20} />
						</button>
						<button
								onclick={() => scaricaAttestato(iscrizione.idCorso)}
								class="flex-[2] md:flex-none bg-[#1B4B6B] text-white px-8 py-4 rounded-2xl text-[10px] font-black uppercase tracking-widest flex items-center justify-center gap-3 hover:bg-[#153a54] transition-all shadow-lg shadow-blue-900/10 cursor-pointer"
						>
							<Download size={18} /> Scarica PDF Finale
						</button>
					</div>
				</div>
			{/each}

			{#if attestatiDisponibili.length === 0}
				<div class="py-32 text-center bg-gray-50/50 rounded-[2.5rem] border border-dashed border-gray-200">
					<FileText size={48} class="mx-auto text-gray-200 mb-4" />
					<h3 class="text-xl font-black text-[#1B4B6B] uppercase italic">Nessun attestato</h3>
					<p class="text-[10px] font-bold text-gray-400 uppercase mt-2">Completa i corsi e attendi la validazione aziendale per sbloccare i certificati</p>
				</div>
			{/if}
		</div>
	{/if}
</div>

<style>
	:global(body) { background-color: #F9FAFB; }
</style>