<script lang="ts">
	import { onMount } from 'svelte';
	import { fade, slide, scale } from 'svelte/transition';
	import {
		FileText, Download, RefreshCw, Search,
		Filter, Calendar, ShieldCheck, AlertCircle,
		Clock, FileDown, Loader2
	} from 'lucide-svelte';

	// 1. DEFINIZIONE TIPI (Zero "any", tutto blindato)
	interface Documento {
		id: string;
		titolo: string;
		categoria: 'SICUREZZA' | 'IGIENE' | 'NOMINE' | 'ALTRO';
		dataCaricamento: string;
		dataScadenza: string | null;
		stato: 'VALIDO' | 'IN_SCADENZA' | 'SCADUTO';
		dimensione: string;
	}

	// 2. STATO REATTIVO
	let isLoading = $state(true);
	let searchQuery = $state('');
	let filtroCategoria = $state('TUTTI');

	let documenti = $state<Documento[]>([]);

	onMount(() => {
		// Mock dei dati (In futuro: fetch via DTO dal backend)
		setTimeout(() => {
			documenti = [
				{ id: '1', titolo: 'DVR - Valutazione Rischi 2026', categoria: 'SICUREZZA', dataCaricamento: '12/01/2026', dataScadenza: '12/01/2027', stato: 'VALIDO', dimensione: '4.2 MB' },
				{ id: '2', titolo: 'Manuale HACCP Autocontrollo', categoria: 'IGIENE', dataCaricamento: '05/03/2024', dataScadenza: '05/03/2026', stato: 'SCADUTO', dimensione: '2.8 MB' },
				{ id: '3', titolo: 'Nomina Medico Competente', categoria: 'NOMINE', dataCaricamento: '10/10/2025', dataScadenza: '10/10/2026', stato: 'IN_SCADENZA', dimensione: '1.1 MB' },
				{ id: '4', titolo: 'Piano Emergenza ed Evacuazione', categoria: 'SICUREZZA', dataCaricamento: '20/02/2026', dataScadenza: null, stato: 'VALIDO', dimensione: '3.5 MB' },
				{ id: '5', titolo: 'Verbale Consegna DPI Collettivi', categoria: 'SICUREZZA', dataCaricamento: '01/04/2026', dataScadenza: null, stato: 'VALIDO', dimensione: '0.5 MB' }
			];
			isLoading = false;
		}, 700);
	});

	// 3. LOGICA FILTRI (Reattività Svelte 5)
	const filteredDocs = $derived(
		documenti.filter(doc => {
			const matchSearch = doc.titolo.toLowerCase().includes(searchQuery.toLowerCase());
			const matchCat = filtroCategoria === 'TUTTI' || doc.categoria === filtroCategoria;
			return matchSearch && matchCat;
		})
	);

	const stats = $derived({
		totali: documenti.length,
		scaduti: documenti.filter(d => d.stato === 'SCADUTO').length,
		validi: documenti.filter(d => d.stato === 'VALIDO').length
	});

	// Helper per i colori degli stati
	const getStatusStyle = (stato: string) => {
		switch (stato) {
			case 'VALIDO': return 'bg-green-50 text-green-600 border-green-100';
			case 'IN_SCADENZA': return 'bg-yellow-50 text-yellow-600 border-yellow-100';
			case 'SCADUTO': return 'bg-red-50 text-red-600 border-red-100';
			default: return 'bg-gray-50 text-gray-600';
		}
	};
</script>

<div in:fade>
	<div class="mb-10 flex flex-col md:flex-row justify-between items-end gap-6">
		<div>
			<h1 class="text-4xl font-extrabold text-[#1B4B6B]">I MIEI DOCUMENTI</h1>
			<p class="text-gray-500 font-bold uppercase text-xs tracking-tighter">Archivio digitale e scadenziario documentale.</p>
		</div>

		<div class="flex gap-4">
			<div class="bg-white px-6 py-3 rounded-2xl shadow-sm border border-gray-100 flex items-center gap-3">
				<ShieldCheck class="text-green-500" size={20} />
				<div class="leading-none">
					<p class="text-[10px] font-black text-gray-400 uppercase">Validi</p>
					<p class="text-lg font-black text-[#1B4B6B]">{stats.validi}</p>
				</div>
			</div>
			<div class="bg-white px-6 py-3 rounded-2xl shadow-sm border border-gray-100 flex items-center gap-3">
				<AlertCircle class="text-red-500" size={20} />
				<div class="leading-none">
					<p class="text-[10px] font-black text-gray-400 uppercase">Scaduti</p>
					<p class="text-lg font-black text-red-600">{stats.scaduti}</p>
				</div>
			</div>
		</div>
	</div>

	<div class="bg-white p-6 rounded-3xl shadow-sm border border-gray-100 mb-8 flex flex-wrap gap-4 items-center justify-between">
		<div class="flex flex-wrap gap-3">
			<button
				onclick={() => filtroCategoria = 'TUTTI'}
				class="px-4 py-2 rounded-xl text-[10px] font-black uppercase tracking-widest transition-all {filtroCategoria === 'TUTTI' ? 'bg-[#1B4B6B] text-white shadow-lg' : 'bg-gray-50 text-gray-400 hover:bg-gray-100'}"
			>Tutti</button>
			<button
				onclick={() => filtroCategoria = 'SICUREZZA'}
				class="px-4 py-2 rounded-xl text-[10px] font-black uppercase tracking-widest transition-all {filtroCategoria === 'SICUREZZA' ? 'bg-[#1B4B6B] text-white shadow-lg' : 'bg-gray-50 text-gray-400 hover:bg-gray-100'}"
			>Sicurezza</button>
			<button
				onclick={() => filtroCategoria = 'IGIENE'}
				class="px-4 py-2 rounded-xl text-[10px] font-black uppercase tracking-widest transition-all {filtroCategoria === 'IGIENE' ? 'bg-[#1B4B6B] text-white shadow-lg' : 'bg-gray-50 text-gray-400 hover:bg-gray-100'}"
			>Igiene</button>
			<button
				onclick={() => filtroCategoria = 'NOMINE'}
				class="px-4 py-2 rounded-xl text-[10px] font-black uppercase tracking-widest transition-all {filtroCategoria === 'NOMINE' ? 'bg-[#1B4B6B] text-white shadow-lg' : 'bg-gray-50 text-gray-400 hover:bg-gray-100'}"
			>Nomine</button>
		</div>

		<div class="relative w-full md:w-72">
			<Search class="absolute left-3 top-1/2 -translate-y-1/2 text-gray-400" size={16} />
			<input
				bind:value={searchQuery}
				type="text"
				placeholder="Cerca documento..."
				class="w-full pl-10 pr-4 py-2.5 bg-gray-50 border border-transparent rounded-xl text-xs font-bold uppercase focus:ring-2 focus:ring-[#1B4B6B] outline-none transition-all"
			/>
		</div>
	</div>

	{#if isLoading}
		<div class="h-64 flex flex-col items-center justify-center gap-4 text-gray-300">
			<Loader2 size={48} class="animate-spin text-[#1B4B6B]" />
			<p class="text-[10px] font-black uppercase tracking-widest">Sincronizzazione Archivio...</p>
		</div>
	{:else if filteredDocs.length === 0}
		<div class="bg-white border-2 border-dashed border-gray-100 rounded-3xl p-20 text-center" in:scale>
			<FileText size={48} class="mx-auto text-gray-200 mb-4" />
			<h3 class="text-[#1B4B6B] font-black uppercase">Nessun Documento Trovato</h3>
			<p class="text-gray-400 text-xs font-bold uppercase mt-1">Prova a cambiare filtri o termini di ricerca.</p>
		</div>
	{:else}
		<div class="grid grid-cols-1 md:grid-cols-2 xl:grid-cols-3 gap-8">
			{#each filteredDocs as doc (doc.id)}
				<div class="bg-white rounded-3xl shadow-sm border border-gray-100 hover:shadow-xl transition-all group overflow-hidden flex flex-col" in:scale>
					<div class="h-1.5 w-full {doc.stato === 'VALIDO' ? 'bg-green-500' : doc.stato === 'IN_SCADENZA' ? 'bg-yellow-500' : 'bg-red-500'}"></div>

					<div class="p-8 flex-1">
						<div class="flex justify-between items-start mb-6">
							<div class="p-3 bg-gray-50 rounded-2xl text-[#1B4B6B] group-hover:bg-[#1B4B6B] group-hover:text-white transition-all">
								<FileText size={24} />
							</div>
							<span class="text-[8px] font-black px-2 py-1 rounded border uppercase {getStatusStyle(doc.stato)}">
								{doc.stato.replace('_', ' ')}
							</span>
						</div>

						<h3 class="font-black text-[#1B4B6B] text-lg leading-tight uppercase mb-4 line-clamp-2 min-h-[3.5rem]">
							{doc.titolo}
						</h3>

						<div class="space-y-3">
							<div class="flex items-center gap-2 text-[10px] font-bold text-gray-400 uppercase tracking-tighter">
								<Calendar size={14} class="text-[#1B4B6B]" />
								<span>Caricato: {doc.dataCaricamento}</span>
							</div>
							{#if doc.dataScadenza}
								<div class="flex items-center gap-2 text-[10px] font-bold {doc.stato === 'SCADUTO' ? 'text-red-500' : 'text-gray-400'} uppercase tracking-tighter">
									<Clock size={14} class={doc.stato === 'SCADUTO' ? 'text-red-500' : 'text-[#1B4B6B]'} />
									<span>Scadenza: {doc.dataScadenza}</span>
								</div>
							{/if}
						</div>
					</div>

					<div class="p-4 bg-gray-50/50 border-t border-gray-50 flex gap-2">
						<button class="flex-1 flex items-center justify-center gap-2 bg-white border border-gray-200 py-3 rounded-xl text-[10px] font-black text-[#1B4B6B] uppercase hover:bg-[#1B4B6B] hover:text-white hover:border-[#1B4B6B] transition-all">
							<Download size={14} />
							Scarica ({doc.dimensione})
						</button>
						<button
							title="Richiedi Aggiornamento"
							class="p-3 bg-white border border-gray-200 rounded-xl text-gray-400 hover:text-yellow-600 hover:border-yellow-200 transition-all"
						>
							<RefreshCw size={14} />
						</button>
					</div>
				</div>
			{/each}
		</div>
	{/if}

	<div class="mt-12 p-6 bg-[#1B4B6B]/5 rounded-3xl border border-[#1B4B6B]/10 flex items-center gap-4">
		<div class="p-3 bg-[#1B4B6B] text-white rounded-2xl shadow-lg">
			<FileDown size={20} />
		</div>
		<div>
			<p class="text-xs font-black text-[#1B4B6B] uppercase tracking-tight">Richiesta Documenti</p>
			<p class="text-[10px] font-medium text-gray-500 uppercase tracking-tighter">Se non trovi un documento o desideri caricarne uno nuovo, scrivi allo staff tramite la <a href="/dashboard/azienda/comunicazioni" class="text-[#1B4B6B] font-black underline">Chat NorLan</a>.</p>
		</div>
	</div>
</div>

<style>
    :global(body) { background-color: #F9FAFB; }
</style>