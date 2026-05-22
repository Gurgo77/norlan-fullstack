<script lang="ts">
	import { onMount } from 'svelte';
	import { fade, scale } from 'svelte/transition';
	import { goto } from '$app/navigation';
	import {
		Search, ShieldAlert, Clock,
		AlertTriangle, Loader2, Trash2
	} from 'lucide-svelte';

	import { Documento } from '$lib/models/Documento';
	import { TipoDocumento } from '$lib/models/Enums';
	import { DocumentoService } from '$lib/services/DocumentoService';
	import StatCard from '$lib/Components/UI/StatCard.svelte';
	import DocCard from '$lib/Components/Features/Documentale/DocumentoCard.svelte';
	import ModalCard from '$lib/Components/UI/ModalCard.svelte';
	import { scaricaDocumentoUniversale, eliminaDocumentoUniversale } from '$lib/utils/documentoUtils';
	import {getInfoScadenza, calcolaGiorniRimanenti, formattaDataScadenza, ordinaPerScadenza} from '$lib/utils/scadenzeUtils';
	/*
Modulo Scadenziario Documentale (Dashboard Admin).
Fornisce una vista centralizzata e ad alta priorità sui documenti aziendali
in scadenza (entro 30 giorni) o già scaduti. Esclude le pratiche non critiche
e permette l'intervento rapido (download, navigazione all'azienda, eliminazione).
*/
	let documenti = $state<Documento[]>([]);
	let isLoading = $state(true);
	let searchQuery = $state('');

	let showDeleteModal = $state(false);
	let isDeleting = $state(false);
	let docDaEliminare = $state<Documento | null>(null);
	let errorMessage = $state('');

	// Recupera tutti i documenti all'avvio della vista
	onMount(async () => {
		try {
			documenti = await DocumentoService.getAllDocumenti();
		} catch (error) {
			console.error("Errore caricamento:", error);
		} finally {
			isLoading = false;
		}
	});

	// Reindirizza l'admin al profilo dell'azienda associata al documento critico
	function vaiAdAzienda(idAzienda: number | undefined) {
		if (!idAzienda) {
			console.error("ID Azienda mancante nel documento");
			return;
		}
		goto(`/dashboard/admin/aziende?id=${idAzienda}`);
	}

	function preparaEliminazione(doc: Documento) {
		docDaEliminare = doc;
		errorMessage = '';
		showDeleteModal = true;
	}

	// Gestisce l'eliminazione del documento con catch specifico per errori API
	async function confermaEliminazione() {
		if (!docDaEliminare) return;
		isDeleting = true;
		errorMessage = '';

		const res = await eliminaDocumentoUniversale(docDaEliminare.idDocumento);

		if (res.error) {
			errorMessage = res.msg;
		} else {
			documenti = documenti.filter(d => d.idDocumento !== docDaEliminare?.idDocumento);
			showDeleteModal = false;
			docDaEliminare = null;
		}
		isDeleting = false;
	}

	// Pipeline reattiva: filtra per data (max 30gg), tipologia e testo, poi ordina per urgenza
	const filteredDocumenti = $derived.by(() => {
		const filtrati = documenti.filter(d => {
			if (d.tipologia === TipoDocumento.ATTESTATO_CORSO) return false;

			const giorni = calcolaGiorniRimanenti(d.dataScadenza);
			if (giorni > 30) return false;

			const matchSearch = d.ragioneSocialeAzienda.toLowerCase().includes(searchQuery.toLowerCase()) ||
					d.tipologia.toLowerCase().includes(searchQuery.toLowerCase());

			return matchSearch;
		});

		return ordinaPerScadenza(filtrati, 'dataScadenza');
	});

	const statistiche = $derived({
		inScadenza: filteredDocumenti.filter(d => getInfoScadenza(d.dataScadenza).stato === 'WARNING').length,
		scaduti: filteredDocumenti.filter(d => getInfoScadenza(d.dataScadenza).stato === 'DANGER').length
	});

	async function scarica(id: number, path: string) {
		await scaricaDocumentoUniversale(id, path);
	}
</script>

<div in:fade class="max-w-7xl mx-auto p-4 md:p-6 space-y-6 md:space-y-8">
	<div class="flex flex-col md:flex-row justify-between items-start gap-4">
		<div>
			<h1 class="text-2xl md:text-4xl font-extrabold text-[#1B4B6B] uppercase tracking-tighter">SCADENZIARIO</h1>
			<p class="text-gray-500 font-bold uppercase text-[9px] md:text-[10px] tracking-widest mt-1">Monitoraggio pratiche in scadenza.</p>
		</div>
	</div>

	<!-- KPI Dashboard: Contatori in tempo reale delle criticità attuali -->
	<div class="grid grid-cols-1 md:grid-cols-2 gap-4 md:gap-6">
		<StatCard
				titolo="Pratiche in Scadenza"
				valore={statistiche.inScadenza}
				icona={Clock}
				bgIcona="bg-orange-50"
				testoIcona="text-orange-600"
				hoverBgIcona="group-hover:bg-orange-600"
		/>
		<StatCard
				titolo="Pratiche Scadute"
				valore={statistiche.scaduti}
				icona={ShieldAlert}
				bgIcona="bg-red-50"
				testoIcona="text-red-600"
				hoverBgIcona="group-hover:bg-red-600"
		/>
	</div>

	<!-- Input di Ricerca: Filtra rapidamente i documenti in scadenza per azienda -->
	<div class="relative w-full max-w-md">
		<Search class="absolute left-4 top-1/2 -translate-y-1/2 text-gray-300" size={18} />
		<input
				bind:value={searchQuery}
				type="text"
				placeholder="Cerca azienda o documento..."
				class="w-full pl-12 pr-6 py-3 md:py-4 bg-white border border-gray-100 rounded-2xl text-xs font-bold uppercase outline-none focus:ring-4 focus:ring-[#1B4B6B]/5 shadow-sm transition-all"
		/>
	</div>

	{#if isLoading}
		<div class="py-20 text-center"><Loader2 size={40} class="animate-spin mx-auto text-[#1B4B6B]" /></div>
	{:else if filteredDocumenti.length === 0}
		<div class="py-20 md:py-24 text-center bg-gray-50/50 rounded-[2rem] md:rounded-[2.5rem] border border-dashed border-gray-200">
			<h3 class="text-lg md:text-xl font-black text-[#1B4B6B] uppercase italic">Nessuna criticità</h3>
		</div>
	{:else}
		<div class="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 xl:grid-cols-4 gap-4 md:gap-6">
			<!-- Grid Documenti: Visualizzazione schede per ogni pratica in warning/danger -->
			{#each filteredDocumenti as doc (doc.idDocumento)}
				{@const info = getInfoScadenza(doc.dataScadenza)}
				<div in:scale>
					<DocCard
							documento={{
                         id: doc.idDocumento,
                         titolo: doc.tipologia.replace(/_/g, ' '),
                         sottotitolo: doc.ragioneSocialeAzienda,
                         stato: info.stato,
                         dataScadenza: formattaDataScadenza(doc.dataScadenza)
                      }}
							ruolo="admin"
							onDownload={() => scarica(doc.idDocumento, doc.filePath)}
							onDelete={() => preparaEliminazione(doc)}
							onManage={() => vaiAdAzienda(doc.idAzienda)}
					/>
				</div>
			{/each}
		</div>
	{/if}
</div>

<!-- Modale di Eliminazione: Conferma distruttiva con gestione errori visuale (errorMessage) -->
<ModalCard bind:isOpen={showDeleteModal} maxWidth="max-w-md" headerClass="bg-red-600">
	{#snippet title()}
		<Trash2 size={20}/> <span class="font-black uppercase tracking-tighter">Elimina Documento?</span>
	{/snippet}

	<div class="text-center py-4">
		<div class="w-16 h-16 md:w-20 md:h-20 bg-red-50 text-red-600 rounded-full flex items-center justify-center mx-auto mb-6">
			<Trash2 size={32} class="md:hidden"/><Trash2 size={40} class="hidden md:block"/>
		</div>
		<p class="text-sm text-gray-400 mb-6 px-2">Stai per rimuovere definitivamente: <br>
			<span class="font-bold text-[#1B4B6B]">{docDaEliminare?.tipologia.replace(/_/g, ' ')}</span>
		</p>

		{#if errorMessage}
			<div class="mb-6 p-4 bg-red-50 border border-red-100 rounded-2xl flex items-start gap-3 text-left">
				<AlertTriangle size={18} class="text-red-600 shrink-0 mt-0.5" />
				<p class="text-[11px] font-bold text-red-600 uppercase leading-tight">{errorMessage}</p>
			</div>
		{/if}
	</div>

	{#snippet footer()}
		<div class="flex flex-col w-full gap-3">
			<button
					onclick={confermaEliminazione}
					disabled={isDeleting}
					class="w-full bg-red-600 text-white py-4 rounded-2xl font-black uppercase text-[10px] shadow-lg shadow-red-200 transition-all hover:bg-red-700 disabled:opacity-50 flex items-center justify-center"
			>
				{#if isDeleting}<Loader2 size={14} class="animate-spin mr-2"/>{/if} Sì, elimina definitivamente
			</button>
			<button
					onclick={() => { showDeleteModal = false; errorMessage = ''; }}
					class="w-full py-2 text-[10px] font-black uppercase text-gray-400 hover:text-gray-600"
			>
				No, annulla
			</button>
		</div>
	{/snippet}
</ModalCard>

<style>
	:global(body) { background-color: #F9FAFB; }
</style>