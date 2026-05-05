<script lang="ts">
	import { onMount } from 'svelte';
	import { fade, scale } from 'svelte/transition';
	import {
		FileBadge, Download, Search, Loader2,
		FileText, Calendar
	} from 'lucide-svelte';

	import type { IscrizioneCorso } from '$lib/models/IscrizioneCorso';
	import { AuthService } from '$lib/services/AuthService';
	import { LavoratoreService, type DipendenteDTO } from '$lib/services/LavoratoreService';
	import { FormazioneService } from '$lib/services/FormazioneService';

	let isLoading = $state(true);
	let searchQuery = $state('');

	let utente = $state<DipendenteDTO | null>(null);
	let iscrizioni = $state<IscrizioneCorso[]>([]);

	onMount(async () => {
		const session = AuthService.getSession();
		if (!session) return;

		try {
			const [dipendenteData, iscrizioniData] = await Promise.all([
				LavoratoreService.getById(session.idUtente),
				FormazioneService.getIscrizioniUtente(session.idUtente)
			]);

			utente = dipendenteData;
			iscrizioni = iscrizioniData;
		} catch (error) {
			console.error("Errore durante il recupero dei dati:", error);
		} finally {
			isLoading = false;
		}
	});

	async function scaricaAttestato(idCorso: number) {
		if (!utente) return;

		try {
			const blob = await FormazioneService.downloadAttestato(idCorso, utente.idUtente);

			if (!blob || blob.size === 0) {
				throw new Error("Il file restituito dal server è vuoto o corrotto.");
			}

			const url = URL.createObjectURL(blob);
			const a = document.createElement('a');
			a.href = url;
			a.download = `Attestato_Corso_${idCorso}_NorLan.pdf`;
			document.body.appendChild(a);
			a.click();
			document.body.removeChild(a);
			URL.revokeObjectURL(url);

		} catch (e) {
			console.error("Errore nel download dell'attestato:", e);
			alert("Impossibile scaricare il file. Verifica che il documento sia effettivamente presente sul server.");
		}
	}

	attestatiDisponibili = $derived(
			iscrizioni.filter(i =>
					i.presenzaConfermata === true &&
					i.idDocumento !== undefined &&
					i.idDocumento !== null &&
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
								<span class="text-[8px] font-black uppercase tracking-widest px-2 py-0.5 rounded bg-emerald-50 text-emerald-600">Certificato Convalidato</span>
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
						<button
								onclick={() => scaricaAttestato(iscrizione.idCorso)}
								class="flex-[2] md:flex-none bg-[#1B4B6B] text-white px-8 py-4 rounded-2xl text-[10px] font-black uppercase tracking-widest flex items-center justify-center gap-3 hover:bg-[#153a54] transition-all shadow-lg shadow-blue-900/10 cursor-pointer"
						>
							<Download size={18} /> Scarica Attestato
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