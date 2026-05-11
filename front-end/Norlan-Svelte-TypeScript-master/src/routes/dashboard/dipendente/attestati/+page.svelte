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
	import { scaricaDocumentoUniversale } from '$lib/utils/documentoUtils';
	import DocCard from '$lib/Components/Features/Documentale/DocumentoCard.svelte';

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

	async function scaricaAttestato(idDocumento: number, filePath: string) {
		await scaricaDocumentoUniversale(idDocumento, filePath);
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
						return {
							info: {
								id: i.idDocumento as number,
								titolo: i.titoloCorso,
								sottotitolo: "Certificato di Formazione",
								stato: 'OK' as const,
								dataCaricamento: formattaData(i.dataOrarioCorso),
								dataScadenza: i.dataOrarioCorso
							},
							path: (i as any).filePathDocumento || ''
						};
					})
	);

	function formattaData(dataIso: string) {
		return new Date(dataIso).toLocaleDateString('it-IT', {
			day: '2-digit', month: 'long', year: 'numeric'
		}).toUpperCase();
	}
</script>

<div in:fade class="max-w-7xl mx-auto space-y-6 md:space-y-8 pb-10 p-4 md:p-6">

	<div class="flex flex-col md:flex-row justify-between items-start md:items-center gap-4">
		<div>
			<h1 class="text-2xl md:text-4xl font-black text-[#1B4B6B] uppercase tracking-tighter leading-tight">I Miei Attestati</h1>
			<p class="text-gray-400 font-bold uppercase text-[9px] md:text-[10px] tracking-widest mt-1 md:mt-2">
				Dipendente: <span class="text-[#1B4B6B]">{utente?.nome || '...'} {utente?.cognome || ''}</span>
			</p>
		</div>
	</div>

	<div class="flex flex-col gap-4">
		<div class="relative group w-full md:max-w-md">
			<Search class="absolute left-4 top-1/2 -translate-y-1/2 text-gray-300 group-focus-within:text-[#1B4B6B] transition-colors" size={20} />
			<input
					bind:value={searchQuery}
					type="text"
					placeholder="CERCA PER TITOLO CORSO..."
					class="w-full rounded-2xl md:rounded-[1.5rem] border border-gray-100 bg-white py-3.5 md:py-4 pl-12 pr-6 text-xs font-bold uppercase shadow-sm outline-none transition-all focus:ring-4 focus:ring-[#1B4B6B]/5"
			/>
		</div>
	</div>

	{#if isLoading}
		<div class="py-24 md:py-32 flex flex-col items-center justify-center gap-4">
			<Loader2 size={48} class="animate-spin text-[#1B4B6B]" />
			<span class="text-[10px] font-black text-gray-400 uppercase tracking-widest">Apertura archivio...</span>
		</div>
	{:else}
		<div class="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 xl:grid-cols-4 gap-4 md:gap-6">
			{#each attestatiDisponibili as item (item.info.id)}
				<div in:scale={{duration: 200}}>
					<DocCard
							documento={item.info}
							ruolo="dipendente"
							mostraScadenza={false}
							onDownload={() => scaricaAttestato(item.info.id, item.path)}
					/>
				</div>
			{/each}
		</div>

		{#if attestatiDisponibili.length === 0}
			<div class="py-20 md:py-32 text-center bg-gray-50/50 rounded-3xl md:rounded-[2.5rem] border border-dashed border-gray-200 col-span-full px-4">
				<FileText size={48} class="mx-auto text-gray-200 mb-4" />
				<h3 class="text-lg md:text-xl font-black text-[#1B4B6B] uppercase italic leading-tight">Nessun attestato disponibile</h3>
				<p class="text-[10px] font-bold text-gray-400 uppercase mt-2 max-w-xs mx-auto">Completa i corsi e attendi la validazione aziendale per sbloccare i certificati</p>
			</div>
		{/if}
	{/if}
</div>

<style>
	:global(body) { background-color: #F9FAFB; }
</style>