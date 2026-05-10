<script lang="ts">
	import { onMount } from 'svelte';
	import { fade, scale } from 'svelte/transition';
	import {
		Search, Loader2, FileText
	} from 'lucide-svelte';

	import type { IscrizioneCorso } from '$lib/models/IscrizioneCorso';
	import { AuthService } from '$lib/services/AuthService';
	import { LavoratoreService, type DipendenteDTO } from '$lib/services/LavoratoreService';
	import { FormazioneService } from '$lib/services/FormazioneService';
	import DocCard, { type DocInfo } from '$lib/Components/Features/Documentale/DocumentoCard.svelte';
	import { gestisciDownloadStandard } from '$lib/utils/downloadUtils';

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

	function scaricaAttestato(idCorso: number | string) {
		if (!utente) return;

		gestisciDownloadStandard(
				FormazioneService.downloadAttestato(Number(idCorso), utente.idUtente),
				`Attestato_Corso_${idCorso}_NorLan.pdf`
		);
	}

	const attestatiDisponibili = $derived(
			iscrizioni
					.filter(i =>
							i.presenzaConfermata === true &&
							i.idDocumento !== undefined &&
							i.idDocumento !== null &&
							i.titoloCorso.toLowerCase().includes(searchQuery.toLowerCase())
					)
					.map(i => {
						const info: DocInfo = {
							id: i.idCorso,
							titolo: i.titoloCorso,
							sottotitolo: "Certificato di Formazione",
							stato: 'OK', // Gli attestati presi dal lavoratore sono considerati validi
							dataCaricamento: formattaData(i.dataOrarioCorso),
							dataScadenza: i.dataOrarioCorso // Usiamo la data corso come riferimento se non c'è vera scadenza, modificalo se il backend ha la dataScadenza reale
						};
						return info;
					})
	);

	function formattaData(dataIso: string) {
		return new Date(dataIso).toLocaleDateString('it-IT', {
			day: '2-digit', month: 'long', year: 'numeric'
		}).toUpperCase();
	}
</script>

<div in:fade class="max-w-7xl mx-auto space-y-8 pb-10 p-6">

	<div class="flex flex-col md:flex-row justify-between items-start md:items-end gap-6">
		<div>
			<h1 class="text-4xl font-black text-[#1B4B6B] uppercase tracking-tighter">I Miei Attestati</h1>
			<p class="text-gray-400 font-bold uppercase text-[10px] tracking-widest mt-1">
				Dipendente: <span class="text-[#1B4B6B]">{utente?.nome || '...'} {utente?.cognome || ''}</span>
			</p>
		</div>
	</div>

	<div class="flex flex-col gap-4">
		<div class="relative group max-w-md">
			<Search class="absolute left-4 top-1/2 -translate-y-1/2 text-gray-300 group-focus-within:text-[#1B4B6B] transition-colors" size={20} />
			<input
					bind:value={searchQuery}
					type="text"
					placeholder="CERCA PER TITOLO CORSO..."
					class="w-full pl-12 pr-6 py-4 bg-white border border-gray-100 rounded-[1.5rem] text-xs font-bold uppercase outline-none focus:ring-4 focus:ring-[#1B4B6B]/5 shadow-sm transition-all"
			/>
		</div>
	</div>

	{#if isLoading}
		<div class="py-32 flex flex-col items-center justify-center gap-4">
			<Loader2 size={48} class="animate-spin text-[#1B4B6B]" />
			<span class="text-[10px] font-black text-gray-400 uppercase tracking-widest">Apertura archivio...</span>
		</div>
	{:else}
		<div class="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 xl:grid-cols-4 gap-6">
			{#each attestatiDisponibili as documento (documento.id)}
				<div in:scale={{duration: 200}}>
					<DocCard
							{documento}
							ruolo="dipendente"
							onDownload={scaricaAttestato}
					/>
				</div>
			{/each}
		</div>

		{#if attestatiDisponibili.length === 0}
			<div class="py-32 text-center bg-gray-50/50 rounded-[2.5rem] border border-dashed border-gray-200 col-span-full">
				<FileText size={48} class="mx-auto text-gray-200 mb-4" />
				<h3 class="text-xl font-black text-[#1B4B6B] uppercase italic">Nessun attestato</h3>
				<p class="text-[10px] font-bold text-gray-400 uppercase mt-2">Completa i corsi e attendi la validazione aziendale per sbloccare i certificati</p>
			</div>
		{/if}
	{/if}
</div>

<style>
	:global(body) { background-color: #F9FAFB; }
</style>