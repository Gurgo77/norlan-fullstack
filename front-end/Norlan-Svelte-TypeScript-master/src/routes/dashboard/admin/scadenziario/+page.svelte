<script lang="ts">
	import { onMount } from 'svelte';
	import { fade } from 'svelte/transition';
	import {
		FileText, Trash2, Search,
		AlertCircle, CheckCircle, Clock, Download, ShieldAlert, FileCheck
	} from 'lucide-svelte';

	import { Documento } from '$lib/models/Documento';
	import { TipoDocumento, StatoDocumento } from '$lib/models/Enums';
	import { DocumentoService } from '$lib/services/DocumentoService';
	import StatCard from '$lib/Components/UI/StatCard.svelte';
	import AlertCard from '$lib/Components/UI/AlertCard.svelte';

	let documenti = $state<Documento[]>([]);
	let isLoading = $state(true);
	let searchQuery = $state('');

	onMount(async () => {
		try {
			documenti = await DocumentoService.getAllDocumenti();
		} catch (error) {
			console.error("Errore durante il caricamento dei dati dello scadenziario:", error);
		} finally {
			isLoading = false;
		}
	});

	async function eliminaDocumento(idDocumento: number) {
		if (!confirm("Sei sicuro di voler eliminare questo documento in modo permanente?")) return;

		try {
			await DocumentoService.deleteDocumento(idDocumento);
			documenti = documenti.filter(d => d.idDocumento !== idDocumento);
		} catch (error) {
			console.error("Errore durante la procedura di eliminazione:", error);
			alert("Si è verificato un errore durante l'eliminazione del documento.");
		}
	}

	async function handleDownload(idDocumento: number, filename: string) {
		try {
			const blob = await DocumentoService.downloadDocumento(idDocumento);
			if (!blob || blob.size === 0) {
				throw new Error("Il file restituito dal server è vuoto o corrotto.");
			}

			const url = window.URL.createObjectURL(blob);
			const a = document.createElement('a');
			a.href = url;

			const nomePulito = filename.replace(/\s+/g, '_');
			a.download = nomePulito.toLowerCase().endsWith('.pdf') ? nomePulito : `${nomePulito}.pdf`;
			document.body.appendChild(a);
			a.click();
			document.body.removeChild(a);
			window.URL.revokeObjectURL(url);
		} catch (error) {
			console.error("Errore durante il download del documento:", error);
			alert("Impossibile procedere con il download. Verificare la disponibilità del file sul server.");
		}
	}

	function getStatoScadenza(dataScadenza: string) {
		const oggi = new Date();
		const scadenza = new Date(dataScadenza);
		const diffTempo = scadenza.getTime() - oggi.getTime();
		const giorniRimanenti = Math.ceil(diffTempo / (1000 * 3600 * 24));

		if (giorniRimanenti < 0) return { colore: 'text-red-600', bg: 'bg-red-50', border: 'border-red-200', label: 'Scaduto', IconaDef: AlertCircle };
		if (giorniRimanenti <= 30) return { colore: 'text-yellow-600', bg: 'bg-yellow-50', border: 'border-yellow-200', label: `In scadenza (${giorniRimanenti}gg)`, IconaDef: Clock };
		return { colore: 'text-green-600', bg: 'bg-green-50', border: 'border-green-200', label: 'Valido', IconaDef: CheckCircle };
	}

	const filteredDocumenti = $derived(
			documenti.filter(d => {
				if (d.tipologia === TipoDocumento.ATTESTATO_CORSO) return false;
				const oggi = new Date().getTime();
				const scadenza = new Date(d.dataScadenza).getTime();
				const giorniRimanenti = Math.ceil((scadenza - oggi) / (1000 * 3600 * 24));
				if (giorniRimanenti > 30) return false;
				const matchRicerca = d.ragioneSocialeAzienda.toLowerCase().includes(searchQuery.toLowerCase()) ||
						d.tipologia.toLowerCase().includes(searchQuery.toLowerCase());

				return matchRicerca;
			}).sort((a, b) => new Date(a.dataScadenza).getTime() - new Date(b.dataScadenza).getTime())
	);

	const statistiche = $derived({
		inScadenza: filteredDocumenti.filter(d => {
			const giorni = Math.ceil((new Date(d.dataScadenza).getTime() - new Date().getTime()) / (1000 * 3600 * 24));
			return giorni >= 0 && giorni <= 30;
		}).length,
		scaduti: filteredDocumenti.filter(d => {
			const giorni = Math.ceil((new Date(d.dataScadenza).getTime() - new Date().getTime()) / (1000 * 3600 * 24));
			return giorni < 0;
		}).length,
		inAttesa: filteredDocumenti.filter(d => d.stato === StatoDocumento.IN_ATTESA_FIRMA).length
	});
</script>

<div in:fade>
	<div class="mb-10 flex justify-between items-start">
		<div>
			<h1 class="text-4xl font-extrabold text-[#1B4B6B]">SCADENZIARIO</h1>
			<p class="text-gray-500 font-bold uppercase text-xs tracking-tighter">Monitoraggio pratiche in scadenza.</p>
		</div>
	</div>

	<div class="grid grid-cols-1 md:grid-cols-2 gap-6 mb-8">
		<StatCard titolo="Pratiche in Scadenza" valore={statistiche.inScadenza} icona={FileText} />
		<StatCard titolo="Pratiche Scadute" valore={statistiche.scaduti} icona={ShieldAlert} bgIcona="bg-red-50" testoIcona="text-red-600" hoverBgIcona="group-hover:bg-red-600" />
	</div>

	<div class="bg-white rounded-2xl shadow-sm border border-gray-100 overflow-hidden">
		<div class="p-6 border-b border-gray-50 flex justify-between items-center bg-gray-50/30">
			<div class="relative w-96">
				<Search class="absolute left-3 top-1/2 -translate-y-1/2 text-gray-400" size={16} />
				<input
						bind:value={searchQuery}
						type="text"
						placeholder="Filtra per azienda o tipo documento..."
						class="w-full pl-10 pr-4 py-2 bg-white border border-gray-200 rounded-xl text-xs focus:ring-2 focus:ring-[#1B4B6B] outline-none transition-all font-bold uppercase"
				/>
			</div>
		</div>

		<div class="p-6 space-y-4">
			{#if isLoading}
				<div class="py-12 text-center text-gray-400 font-bold uppercase text-xs">Caricamento database NorLan...</div>
			{:else if filteredDocumenti.length === 0}
				<div class="py-12 text-center text-gray-400 font-bold uppercase text-xs">Tutti i documenti aziendali sono attualmente in regola.</div>
			{:else}
				{#each filteredDocumenti as doc (doc.idDocumento)}
					{@const statoInfo = getStatoScadenza(doc.dataScadenza)}
					<div class="group relative">
						<AlertCard
								titolo={doc.ragioneSocialeAzienda}
								sottotitolo="{doc.tipologia.replace(/_/g, ' ')} - {doc.modulo}"
								variante={statoInfo.label === 'Scaduto' ? 'danger' : 'warning'}
								icona={FileCheck}
								stato={doc.stato.replace(/_/g, ' ')}
								data="Scadenza: {new Date(doc.dataScadenza).toLocaleDateString('it-IT')}"
						/>
						<div class="absolute right-4 top-1/2 -translate-y-1/2 flex items-center gap-2 opacity-0 group-hover:opacity-100 transition-opacity bg-white/90 backdrop-blur-sm p-1.5 rounded-xl shadow-sm border border-gray-100">
							<button onclick={() => handleDownload(doc.idDocumento, `${doc.ragioneSocialeAzienda}_${doc.tipologia}`)} class="p-2 text-[#1B4B6B] hover:bg-blue-50 rounded-lg transition-colors"><Download size={16} /></button>
							<button onclick={() => eliminaDocumento(doc.idDocumento)} class="p-2 text-red-500 hover:bg-red-50 rounded-lg transition-colors"><Trash2 size={16} /></button>
						</div>
					</div>
				{/each}
			{/if}
		</div>
	</div>
</div>

<style>
	:global(body) { background-color: #F9FAFB; }
</style>