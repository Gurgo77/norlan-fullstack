<script lang="ts">
	import { onMount } from 'svelte';
	import { fade, scale } from 'svelte/transition';
	import {
		FileText,
		Download,
		RefreshCw,
		Search,
		Calendar,
		ShieldCheck,
		AlertCircle,
		Clock,
		FileDown,
		Loader2
	} from 'lucide-svelte';

	// Import Servizi e Modelli
	import { DocumentoService } from '$lib/services/DocumentoService';
	import { AuthService } from '$lib/services/AuthService';
	import { Documento } from '$lib/models/Documento';
	import { ModuloServizio, TipoDocumento } from '$lib/models/Enums'; // Aggiunto import TipoDocumento

	// --- STATO REATTIVO (Svelte 5) ---
	let isLoading = $state(true);
	let searchQuery = $state('');
	let filtroCategoria = $state<ModuloServizio | 'TUTTI'>('TUTTI');
	let documenti = $state<Documento[]>([]);

	// --- AZIONI ---
	onMount(async () => {
		const session = AuthService.getSession();
		if (!session) return;

		try {
			// Recupero dati reali dal database
			const tuttiDocs = await DocumentoService.getDocumentiByAzienda(session.idUtente);

			// FILTRO: Escludi gli attestati di corso dall'archivio documentale generale
			documenti = tuttiDocs.filter(doc => doc.tipologia !== TipoDocumento.ATTESTATO_CORSO);
		} catch (error) {
			console.error('Errore nel caricamento documenti:', error);
		} finally {
			isLoading = false;
		}
	});

	async function handleDownload(id: number, titolo: string) {
		try {
			const blob = await DocumentoService.downloadDocumento(id);
			const url = window.URL.createObjectURL(blob);
			const a = document.createElement('a');
			a.href = url;
			a.download = `${titolo.replace(/\s+/g, '_')}.pdf`;
			document.body.appendChild(a);
			a.click();
			window.URL.revokeObjectURL(url);
		} catch (error) {
			console.error('Errore download:', error);
		}
	}

	async function richiediAggiornamento(id: number) {
		if (!confirm('Inviare richiesta di aggiornamento allo Staff NorLan?')) return;
		try {
			await DocumentoService.richiediRinnovo(id);
			alert('Richiesta inviata con successo.');
		} catch (error) {
			console.error('Errore rinnovo:', error);
		}
	}

	// --- LOGICA REATTIVA ---
	const filteredDocs = $derived(
			documenti.filter((doc) => {
				const matchSearch = doc.tipologia.toLowerCase().includes(searchQuery.toLowerCase());
				const matchCat = filtroCategoria === 'TUTTI' || doc.modulo === filtroCategoria;
				return matchSearch && matchCat;
			})
	);

	const stats = $derived({
		totali: documenti.length,
		scaduti: documenti.filter((d) => d.scaduto).length,
		validi: documenti.filter((d) => !d.scaduto).length
	});

	function getStatoDocumento(doc: Documento): 'VALIDO' | 'IN_SCADENZA' | 'SCADUTO' {
		if (doc.scaduto) return 'SCADUTO';
		const scadenza = new Date(doc.dataScadenza);
		const diff = Math.ceil((scadenza.getTime() - new Date().getTime()) / (1000 * 3600 * 24));
		return diff <= 30 ? 'IN_SCADENZA' : 'VALIDO';
	}

	const getStatusStyle = (stato: string) => {
		switch (stato) {
			case 'VALIDO':
				return 'bg-green-50 text-green-600 border-green-100';
			case 'IN_SCADENZA':
				return 'bg-yellow-50 text-yellow-600 border-yellow-100';
			case 'SCADUTO':
				return 'bg-red-50 text-red-600 border-red-100';
			default:
				return 'bg-gray-50 text-gray-600';
		}
	};
</script>

<div in:fade>
	<div class="mb-10 flex flex-col items-end justify-between gap-6 md:flex-row">
		<div>
			<h1 class="text-4xl font-extrabold text-[#1B4B6B]">I MIEI DOCUMENTI</h1>
			<p class="text-xs font-bold uppercase tracking-tighter text-gray-500">
				Archivio digitale e scadenziario documentale NorLan.
			</p>
		</div>

		<div class="flex gap-4">
			<div class="flex items-center gap-3 rounded-2xl border border-gray-100 bg-white px-6 py-3 shadow-sm">
				<ShieldCheck class="text-green-500" size={20} />
				<div class="leading-none">
					<p class="text-[10px] font-black uppercase text-gray-400">A Norma</p>
					<p class="text-lg font-black text-[#1B4B6B]">{stats.validi}</p>
				</div>
			</div>
			<div class="flex items-center gap-3 rounded-2xl border border-gray-100 bg-white px-6 py-3 shadow-sm">
				<AlertCircle class="text-red-500" size={20} />
				<div class="leading-none">
					<p class="text-[10px] font-black uppercase text-gray-400">Scaduti</p>
					<p class="text-lg font-black text-red-600">{stats.scaduti}</p>
				</div>
			</div>
		</div>
	</div>

	<div class="mb-8 flex flex-wrap items-center justify-between gap-4 rounded-3xl border border-gray-100 bg-white p-6 shadow-sm">
		<div class="flex flex-wrap gap-3">
			<button
					onclick={() => (filtroCategoria = 'TUTTI')}
					class="rounded-xl px-4 py-2 text-[10px] font-black uppercase tracking-widest transition-all {filtroCategoria ===
             'TUTTI'
                ? 'bg-[#1B4B6B] text-white shadow-lg'
                : 'bg-gray-50 text-gray-400 hover:bg-gray-100'}"
			>
				Tutti
			</button>

			{#each Object.values(ModuloServizio) as cat (cat)}
				<button
						onclick={() => (filtroCategoria = cat)}
						class="rounded-xl px-4 py-2 text-[10px] font-black uppercase tracking-widest transition-all {filtroCategoria ===
                cat
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
		<div class="rounded-3xl border-2 border-dashed border-gray-100 bg-white p-20 text-center" in:scale>
			<FileText size={48} class="mx-auto mb-4 text-gray-200" />
			<h3 class="font-black uppercase text-[#1B4B6B]">Nessun Documento Trovato</h3>
			<p class="mt-1 text-xs font-bold uppercase text-gray-400">
				L'archivio non contiene file per questa categoria.
			</p>
		</div>
	{:else}
		<div class="grid grid-cols-1 gap-8 md:grid-cols-2 xl:grid-cols-3">
			{#each filteredDocs as doc (doc.idDocumento)}
				{@const status = getStatoDocumento(doc)}
				<div class="group flex flex-col overflow-hidden rounded-3xl border border-gray-100 bg-white shadow-sm transition-all hover:shadow-xl" in:scale>
					<div class="h-1.5 w-full {status === 'VALIDO' ? 'bg-green-500' : status === 'IN_SCADENZA' ? 'bg-yellow-500' : 'bg-red-500'}"></div>

					<div class="flex-1 p-8">
						<div class="mb-6 flex items-start justify-between">
							<div class="rounded-2xl bg-gray-50 p-3 text-[#1B4B6B] transition-all group-hover:bg-[#1B4B6B] group-hover:text-white">
								<FileText size={24} />
							</div>
							<span class="rounded border px-2 py-1 text-[8px] font-black uppercase {getStatusStyle(status)}">
                         {status.replace('_', ' ')}
                      </span>
						</div>

						<h3 class="mb-4 min-h-[3.5rem] text-lg font-black uppercase leading-tight text-[#1B4B6B] line-clamp-2">
							{doc.tipologia.replace(/_/g, ' ')}
						</h3>

						<div class="space-y-3">
							<div class="flex items-center gap-2 text-[10px] font-bold uppercase tracking-tighter text-gray-400">
								<Calendar size={14} class="text-[#1B4B6B]" />
								<span>Caricato: {new Date(doc.dataCaricamento).toLocaleDateString('it-IT')}</span>
							</div>
							<div class="flex items-center gap-2 text-[10px] font-bold uppercase tracking-tighter {doc.scaduto ? 'text-red-500' : 'text-gray-400'}">
								<Clock size={14} class={doc.scaduto ? 'text-red-500' : 'text-[#1B4B6B]'} />
								<span>Scadenza: {new Date(doc.dataScadenza).toLocaleDateString('it-IT')}</span>
							</div>
						</div>
					</div>

					<div class="flex gap-2 border-t border-gray-50 bg-gray-50/50 p-4">
						<button
								onclick={() => handleDownload(doc.idDocumento, doc.tipologia)}
								class="flex flex-1 items-center justify-center gap-2 rounded-xl border border-gray-200 bg-white py-3 text-[10px] font-black uppercase text-[#1B4B6B] transition-all hover:border-[#1B4B6B] hover:bg-[#1B4B6B] hover:text-white"
						>
							<Download size={14} /> Scarica
						</button>
						<button
								onclick={() => richiediAggiornamento(doc.idDocumento)}
								class="rounded-xl border border-gray-200 bg-white p-3 text-gray-400 transition-all hover:text-yellow-600"
						>
							<RefreshCw size={14} />
						</button>
					</div>
				</div>
			{/each}
		</div>
	{/if}

	<div class="mt-12 flex items-center gap-4 rounded-3xl border border-[#1B4B6B]/10 bg-[#1B4B6B]/5 p-6">
		<div class="rounded-2xl bg-[#1B4B6B] p-3 text-white shadow-lg">
			<FileDown size={20} />
		</div>
		<div>
			<p class="text-xs font-black uppercase tracking-tight text-[#1B4B6B]">Richiesta Documenti Speciali</p>
			<p class="text-[10px] font-medium uppercase tracking-tighter text-gray-500">
				Se desideri archiviare nuovi verbali o certificati non presenti, scrivi allo staff tramite la
				<a href="/dashboard/azienda/comunicazioni" class="font-black text-[#1B4B6B] underline">Chat NorLan</a>.
			</p>
		</div>
	</div>
</div>

<style>
	:global(body) {
		background-color: #f9fafb;
	}
</style>