<script lang="ts">
	import { onMount } from 'svelte';
	import { fade } from 'svelte/transition';
	import {
		FileText, Trash2, Search,
		AlertCircle, CheckCircle, Clock, Building2, Download, ShieldAlert, FileCheck
	} from 'lucide-svelte';

	// Import Modelli ed Enums
	import { Documento } from '$lib/models/Documento';
	import { Azienda, type AziendaData } from '$lib/models/Azienda';
	import { TipoDocumento, StatoDocumento } from '$lib/models/Enums';

	// Import Servizi
	import { DocumentoService } from '$lib/services/DocumentoService';
	import { AnagraficaService } from '$lib/services/AnagraficaService';

	// --- STATO REATTIVO ---
	let documenti = $state<Documento[]>([]);
	let aziende = $state<Azienda[]>([]);
	let isLoading = $state(true);
	let searchQuery = $state('');

	// --- AZIONI ---
	onMount(async () => {
		try {
			// Fetch parallelo di aziende e documenti dal database
			const [resAziende, resDocumenti] = await Promise.all([
				AnagraficaService.getAllAziende(),
				DocumentoService.getAllDocumenti()
			]);

			// Mappatura sicura dei dati
			const aziendeRaw = resAziende as AziendaData[];
			aziende = aziendeRaw.map(a => new Azienda(a));
			documenti = resDocumenti;

		} catch (error) {
			console.error("Errore nel caricamento dello scadenziario:", error);
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
			console.error("Errore eliminazione:", error);
			alert("Impossibile eliminare il documento.");
		}
	}

	// --- LOGICA DI DOWNLOAD ROBUSTA ---
	async function handleDownload(idDocumento: number, filename: string) {
		try {
			const blob = await DocumentoService.downloadDocumento(idDocumento);

			// Controllo di sicurezza: se il file è vuoto, fermiamo tutto
			if (!blob || blob.size === 0) {
				throw new Error("Il file restituito dal server è vuoto o corrotto.");
			}

			const url = window.URL.createObjectURL(blob);
			const a = document.createElement('a');
			a.href = url;

			// Puliamo il nome del file da eventuali spazi e assicuriamoci che abbia l'estensione pdf
			const nomePulito = filename.replace(/\s+/g, '_');
			a.download = nomePulito.toLowerCase().endsWith('.pdf') ? nomePulito : `${nomePulito}.pdf`;

			// Aggiungiamo l'elemento al DOM, clicchiamo e lo rimuoviamo (fondamentale per Firefox/Safari)
			document.body.appendChild(a);
			a.click();
			document.body.removeChild(a);

			window.URL.revokeObjectURL(url);
		} catch (error) {
			console.error("Errore download:", error);
			alert("Impossibile scaricare il file. Verifica che il documento sia effettivamente presente sul server.");
		}
	}

	// --- LOGICA DI VISUALIZZAZIONE ---
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
				// 1. Escludi gli attestati dei dipendenti
				if (d.tipologia === TipoDocumento.ATTESTATO_CORSO) return false;

				// 2. Mantieni solo documenti scaduti o in scadenza (entro 30 giorni)
				const oggi = new Date().getTime();
				const scadenza = new Date(d.dataScadenza).getTime();
				const giorniRimanenti = Math.ceil((scadenza - oggi) / (1000 * 3600 * 24));
				if (giorniRimanenti > 30) return false;

				// 3. Applica il filtro della barra di ricerca
				const matchRicerca = d.ragioneSocialeAzienda.toLowerCase().includes(searchQuery.toLowerCase()) ||
						d.tipologia.toLowerCase().includes(searchQuery.toLowerCase());

				return matchRicerca;
			}).sort((a, b) => new Date(a.dataScadenza).getTime() - new Date(b.dataScadenza).getTime())
	);

	// SISTEMAZIONE CONTATORI
	const statistiche = $derived({
		inScadenza: filteredDocumenti.filter(d => {
			const giorni = Math.ceil((new Date(d.dataScadenza).getTime() - new Date().getTime()) / (1000 * 3600 * 24));
			return giorni >= 0 && giorni <= 30; // Solo i documenti con scadenza imminente (esclude i già scaduti)
		}).length,
		scaduti: filteredDocumenti.filter(d => {
			const giorni = Math.ceil((new Date(d.dataScadenza).getTime() - new Date().getTime()) / (1000 * 3600 * 24));
			return giorni < 0; // Solo i documenti oltre la data di scadenza
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
		<!-- Card Pratiche in Scadenza -->
		<div class="bg-white p-6 rounded-2xl shadow-sm border border-gray-100 flex items-center justify-between hover:shadow-md hover:border-[#1B4B6B]/30 transition-all cursor-default">
			<div>
				<p class="text-[10px] font-bold text-gray-400 uppercase tracking-wider mb-1">Pratiche in Scadenza</p>
				<h2 class="text-3xl font-extrabold text-[#1B4B6B]">{statistiche.inScadenza}</h2>
			</div>
			<div class="bg-blue-50 p-4 rounded-xl text-[#1B4B6B]"><FileText size={24} /></div>
		</div>

		<!-- Card Pratiche Scadute -->
		<div class="bg-white p-6 rounded-2xl shadow-sm border border-red-100 flex items-center justify-between hover:shadow-md transition-all cursor-default">
			<div>
				<p class="text-[10px] font-bold text-red-400 uppercase tracking-wider mb-1">Pratiche Scadute</p>
				<h2 class="text-3xl font-extrabold text-red-600">{statistiche.scaduti}</h2>
			</div>
			<div class="bg-red-50 p-4 rounded-xl text-red-600"><ShieldAlert size={24} /></div>
		</div>
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

		<div class="overflow-x-auto">
			<table class="w-full text-left">
				<thead class="bg-gray-50 text-[10px] font-bold text-gray-400 uppercase tracking-widest">
				<tr>
					<th class="px-6 py-4">Azienda</th>
					<th class="px-6 py-4">Documento</th>
					<th class="px-6 py-4">Status Scadenza</th>
					<th class="px-6 py-4">Iter</th>
					<th class="px-8 py-5 text-right">Azioni</th>
				</tr>
				</thead>
				<tbody class="divide-y divide-gray-50">
				{#if isLoading}
					<tr><td colspan="5" class="px-6 py-12 text-center text-gray-400 font-bold uppercase text-xs">Caricamento database NorLan...</td></tr>
				{:else if filteredDocumenti.length === 0}
					<tr><td colspan="5" class="px-6 py-12 text-center text-gray-400 font-bold uppercase text-xs">Tutti i documenti aziendali sono attualmente in regola.</td></tr>
				{:else}
					{#each filteredDocumenti as doc (doc.idDocumento)}
						{@const stato = getStatoScadenza(doc.dataScadenza)}
						{@const IconaStato = stato.IconaDef}
						<tr class="hover:bg-white hover:shadow-lg transition-all group relative">
							<td class="px-6 py-4">
								<div class="flex items-center gap-3">
									<div class="bg-gray-100 p-2 rounded-lg text-[#1B4B6B]"><Building2 size={16} /></div>
									<span class="font-extrabold text-[#1B4B6B] text-xs uppercase">{doc.ragioneSocialeAzienda}</span>
								</div>
							</td>
							<td class="px-6 py-4">
								<div class="flex items-center gap-2 font-black text-[#1B4B6B] text-xs uppercase"><FileCheck size={14} class="text-blue-500" />{doc.tipologia.replace('_', ' ')}</div>
								<p class="text-[9px] font-bold text-gray-400 uppercase tracking-widest mt-0.5">{doc.modulo}</p>
							</td>
							<td class="px-6 py-4">
								<div class="inline-flex items-center gap-2 px-3 py-1.5 rounded-lg border {stato.border} {stato.bg} {stato.colore}">
									<IconaStato size={14} />
									<span class="text-[10px] font-black uppercase tracking-wider">{stato.label}</span>
								</div>
							</td>
							<td class="px-6 py-4">
                        <span class="text-[9px] font-black px-2 py-1 rounded bg-gray-100 text-gray-500 uppercase tracking-widest">
                            {doc.stato.replace(/_/g, ' ')}
                        </span>
							</td>
							<td class="px-6 py-4 text-right opacity-0 group-hover:opacity-100 transition-opacity">
								<div class="flex items-center justify-end gap-2">
									<button onclick={() => handleDownload(doc.idDocumento, `${doc.ragioneSocialeAzienda}_${doc.tipologia}`)} class="p-2 text-[#1B4B6B] hover:bg-blue-50 rounded-lg"><Download size={16} /></button>
									<button onclick={() => eliminaDocumento(doc.idDocumento)} class="p-2 text-red-500 hover:bg-red-50 rounded-lg"><Trash2 size={16} /></button>
								</div>
							</td>
						</tr>
					{/each}
				{/if}
				</tbody>
			</table>
		</div>
	</div>
</div>

<style>
	tr:hover { background-color: white !important; }
</style>