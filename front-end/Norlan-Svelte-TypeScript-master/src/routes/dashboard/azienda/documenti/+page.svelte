<script lang="ts">
	import { onMount } from 'svelte';
	import { fade, scale } from 'svelte/transition';
	import { FileText, Download, Search, Calendar, ShieldCheck, AlertCircle, Clock, FileDown, Loader2 } from 'lucide-svelte';

	import { DocumentoService } from '$lib/services/DocumentoService';
	import { AuthService } from '$lib/services/AuthService';
	import { Documento } from '$lib/models/Documento';
	import { ModuloServizio, TipoDocumento } from '$lib/models/Enums';
	import StatCard from '$lib/Components/UI/StatCard.svelte';
	import AlertCard from '$lib/Components/UI/AlertCard.svelte';
	import { scaricaDocumentoUniversale } from '$lib/utils/documentoUtils';

	import { getInfoScadenza, ordinaPerScadenza, formattaDataScadenza } from '$lib/utils/scadenzeUtils';
	/*
    Modulo Archivio Documentale (Client Panel).
    Fornisce all'Azienda l'accesso completo, filtrabile e ordinato al proprio archivio digitale.
    Permette il download dei file, il monitoraggio visivo delle scadenze tramite KPI
    e la navigazione rapida verso il supporto tecnico per la richiesta di integrazioni.
    */
	let isLoading = $state(true);
	let searchQuery = $state('');
	let filtroCategoria = $state<ModuloServizio | 'TUTTI'>('TUTTI');
	let documenti = $state<Documento[]>([]);
	// Recupera i file aziendali alla creazione del componente (escludendo gli attestati)
	onMount(async () => {
		const session = AuthService.getSession();
		if (!session) return;

		try {
			const tuttiDocs = await DocumentoService.getDocumentiByAzienda(session.idUtente);
			documenti = tuttiDocs.filter(doc => doc.tipologia !== TipoDocumento.ATTESTATO_CORSO);
		} catch (error) {
			console.error('Si è verificato un errore durante il caricamento dell\'archivio documentale:', error);
		} finally {
			isLoading = false;
		}
	});

	// Gestisce l'interazione per lo scaricamento del file fisico
	async function handleDownload(id: number, path: string) {
		await scaricaDocumentoUniversale(id, path);
	}

	// Ricalcola istantaneamente la lista visibile al variare della ricerca o dei filtri
	const filteredDocs = $derived.by(() => {
		const filtrati = documenti.filter((doc) => {
			const matchSearch = doc.tipologia.toLowerCase().includes(searchQuery.toLowerCase());
			const matchCat = filtroCategoria === 'TUTTI' || doc.modulo === filtroCategoria;
			return matchSearch && matchCat;
		});

		return ordinaPerScadenza(filtrati, 'dataScadenza');
	});

	const stats = $derived({
		totali: documenti.length,
		validi: documenti.filter((d) => getInfoScadenza(d.dataScadenza).stato === 'OK').length,
		inScadenza: documenti.filter((d) => getInfoScadenza(d.dataScadenza).stato === 'WARNING').length,
		scaduti: documenti.filter((d) => getInfoScadenza(d.dataScadenza).stato === 'DANGER').length
	});
</script>

<div in:fade class="p-4 md:p-6 pb-20">
	<!-- Dashboard KPI: Indicatori sintetici sullo stato di validità dell'intero archivio -->
	<div class="mb-8 md:mb-10 flex flex-col lg:flex-row lg:items-center justify-between gap-6">
		<div class="w-full lg:w-auto">
			<h1 class="text-2xl md:text-4xl font-extrabold text-[#1B4B6B] uppercase tracking-tighter">I MIEI DOCUMENTI</h1>
			<p class="text-[10px] md:text-xs font-bold uppercase tracking-tighter text-gray-500 mt-1">
				Archivio digitale e scadenziario documentale NorLan.
			</p>
		</div>

		<div class="w-full lg:w-auto overflow-x-auto pb-2 -mx-2 px-2 sm:mx-0 sm:px-0 custom-scrollbar-data">
			<div class="flex gap-4 min-w-max">
				<StatCard titolo="A Norma" valore={stats.validi} icona={ShieldCheck} bgIcona="bg-transparent" testoIcona="text-green-500"/>
				<StatCard titolo="In Scadenza" valore={stats.inScadenza} icona={Clock} bgIcona="bg-transparent" testoIcona="text-orange-500"/>
				<StatCard titolo="Scaduti" valore={stats.scaduti} icona={AlertCircle} bgIcona="bg-transparent" testoIcona="text-red-500"/>
			</div>
		</div>
	</div>

	<!-- Barra di Controllo: Filtri a scorrimento orizzontale (Categorie) e input di ricerca testuale -->
	<div class="mb-8 flex flex-col md:flex-row md:items-center justify-between gap-6 rounded-2xl md:rounded-3xl border border-gray-100 bg-white p-4 md:p-6 shadow-sm">
		<div class="flex gap-2 overflow-x-auto pb-2 md:pb-0 custom-scrollbar-data">
			<button
					onclick={() => (filtroCategoria = 'TUTTI')}
					class="whitespace-nowrap rounded-xl px-4 py-2 text-[10px] font-black uppercase tracking-widest transition-all {filtroCategoria === 'TUTTI'
                ? 'bg-[#1B4B6B] text-white shadow-lg'
                : 'bg-gray-50 text-gray-400 hover:bg-gray-100'}"
			>
				Tutti
			</button>
			{#each Object.values(ModuloServizio) as cat (cat)}
				<button
						onclick={() => (filtroCategoria = cat)}
						class="whitespace-nowrap rounded-xl px-4 py-2 text-[10px] font-black uppercase tracking-widest transition-all {filtroCategoria === cat
                   ? 'bg-[#1B4B6B] text-white shadow-lg'
                   : 'bg-gray-50 text-gray-400 hover:bg-gray-100'}"
				>
					{cat}
				</button>
			{/each}
		</div>

		<div class="relative w-full md:w-72">
			<Search class="absolute left-3 top-1/2 -translate-y-1/2 text-gray-400" size={16} />
			<input
					bind:value={searchQuery}
					type="text"
					placeholder="Cerca documento..."
					class="w-full rounded-xl border border-transparent bg-gray-50 py-2.5 pl-10 pr-4 text-xs font-bold uppercase outline-none transition-all focus:ring-2 focus:ring-[#1B4B6B]"
			/>
		</div>
	</div>

	{#if isLoading}
		<div class="flex h-64 flex-col items-center justify-center gap-4 text-gray-300">
			<Loader2 size={48} class="animate-spin text-[#1B4B6B]" />
			<p class="text-[10px] font-black uppercase tracking-widest">
				Sincronizzazione Archivio Digitale...
			</p>
		</div>
	{:else if filteredDocs.length === 0}
		<div class="rounded-3xl border-2 border-dashed border-gray-100 bg-white p-12 md:p-20 text-center" in:scale>
			<FileText size={48} class="mx-auto mb-4 text-gray-200" />
			<h3 class="font-black uppercase text-[#1B4B6B]">Nessun Documento Trovato</h3>
			<p class="mt-1 text-xs font-bold uppercase text-gray-400">
				L'archivio non contiene file per questa categoria.
			</p>
		</div>
	{:else}
		<div class="grid grid-cols-1 gap-6 md:gap-8 md:grid-cols-2 xl:grid-cols-3">
			{#each filteredDocs as doc (doc.idDocumento)}
				{@const info = getInfoScadenza(doc.dataScadenza)}
				<!-- Card del singolo documento con feedback visivo sullo stato (colori dinamici via info.stato) -->
				<div class="group flex flex-col overflow-hidden rounded-3xl border border-gray-100 bg-white shadow-sm transition-all hover:shadow-xl" in:scale>
					<div class="h-1.5 w-full {info.bgTop}"></div>
					<div class="flex-1 p-6 md:p-8">
						<div class="mb-6 flex items-start justify-between">
							<div class="rounded-2xl bg-gray-50 p-3 text-[#1B4B6B] transition-all group-hover:bg-[#1B4B6B] group-hover:text-white">
								<FileText size={24} />
							</div>
							<span class="rounded border px-2 py-1 text-[8px] font-black uppercase {info.bgBadge} {info.colore} {info.borderBadge}">
                         {info.label}
                      </span>
						</div>
						<h3 class="mb-4 min-h-[3rem] text-base md:text-lg font-black uppercase leading-tight text-[#1B4B6B] line-clamp-2">
							{doc.tipologia.replace(/_/g, ' ')}
						</h3>
						<div class="space-y-3">
							<div class="flex items-center gap-2 text-[9px] md:text-[10px] font-bold uppercase tracking-tighter text-gray-400">
								<Calendar size={14} class="text-[#1B4B6B]" />
								<span>Caricato: {formattaDataScadenza(doc.dataCaricamento)}</span>
							</div>
							<div class="flex items-center gap-2 text-[9px] md:text-[10px] font-bold uppercase tracking-tighter {info.stato === 'DANGER' ? 'text-red-500' : 'text-gray-400'}">
								<info.icona size={14} class={info.stato === 'DANGER' ? 'text-red-500' : 'text-[#1B4B6B]'} />
								<span>Scadenza: {formattaDataScadenza(doc.dataScadenza)}</span>
							</div>
						</div>
					</div>
					<div class="flex gap-2 border-t border-gray-50 bg-gray-50/50 p-4">
						<button
								onclick={() => handleDownload(doc.idDocumento, doc.filePath)}
								class="flex flex-1 items-center justify-center gap-2 rounded-xl border border-gray-200 bg-white py-3 text-[10px] font-black uppercase text-[#1B4B6B] transition-all hover:border-[#1B4B6B] hover:bg-[#1B4B6B] hover:text-white"
						>
							<Download size={14} /> Scarica
						</button>
					</div>
				</div>
			{/each}
		</div>
	{/if}

	<div class="mt-12">
		<!-- Cross-Navigation: Banner CTA a fondo pagina per richiedere documenti mancanti via Chat -->
		<AlertCard
				titolo="Richiesta Documenti Speciali"
				sottotitolo="Se desideri archiviare nuovi verbali o certificati non presenti, scrivi allo staff tramite la Chat NorLan."
				variante="info"
				icona={FileDown}
				href="/dashboard/azienda/comunicazioni?chatId=1"
		/>
	</div>
</div>

<style>
	:global(body) {
		background-color: #f9fafb;
	}
	.custom-scrollbar-data::-webkit-scrollbar {
		height: 4px;
	}
	.custom-scrollbar-data::-webkit-scrollbar-track {
		background: transparent;
	}
	.custom-scrollbar-data::-webkit-scrollbar-thumb {
		background: #E2E8F0;
		border-radius: 10px;
	}
</style>