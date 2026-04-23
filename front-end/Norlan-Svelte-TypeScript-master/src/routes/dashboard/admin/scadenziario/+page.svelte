<script lang="ts">
	import { onMount } from 'svelte';
	import { fade, scale } from 'svelte/transition';
	import {
		FileText, Upload, X, Trash2, Search,
		AlertCircle, CheckCircle, Clock, Building2, Download, ShieldAlert, FileCheck, Loader2
	} from 'lucide-svelte';

	// Import Modelli ed Enums
	import { Documento, type DocumentoData } from '$lib/models/Documento';
	import { Azienda, type AziendaData } from '$lib/models/Azienda';
	import { ModuloServizio, TipoDocumento, StatoDocumento } from '$lib/models/Enums';

	// Import Servizi
	import { DocumentoService } from '$lib/services/DocumentoService';
	import { AnagraficaService } from '$lib/services/AnagraficaService';

	// --- STATO REATTIVO ---
	let documenti = $state<Documento[]>([]);
	let aziende = $state<Azienda[]>([]);
	let isLoading = $state(true);
	let isSaving = $state(false);
	let searchQuery = $state('');
	let showModal = $state(false);

	// Gestione File Upload
	let selectedFile = $state<File | null>(null);

	let formDoc = $state({
		idAzienda: 0,
		modulo: ModuloServizio.SICUREZZA,
		tipologia: TipoDocumento.DVR,
		dataScadenza: '',
		stato: StatoDocumento.CARICATO
	});

	// --- AZIONI ---
	onMount(async () => {
		try {
			// Fetch parallelo di aziende e documenti dal database
			const [resAziende, resDocumenti] = await Promise.all([
				AnagraficaService.getAllAziende(),
				DocumentoService.getAllDocumenti()
			]);

			// Mappatura sicura dei dati (cast da unknown)
			const aziendeRaw = resAziende as AziendaData[];
			aziende = aziendeRaw.map(a => new Azienda(a));
			documenti = resDocumenti;

		} catch (error) {
			console.error("Errore nel caricamento dello scadenziario:", error);
		} finally {
			isLoading = false;
		}
	});

	async function salvaNuovoDocumento() {
		if (!formDoc.idAzienda || !formDoc.dataScadenza || !selectedFile) {
			alert("Compila tutti i campi e seleziona un file PDF.");
			return;
		}

		isSaving = true;

		try {
			// Costruiamo il FormData per il backend (Multipart/Form-Data)
			const formData = new FormData();
			formData.append('file', selectedFile);
			formData.append('modulo', formDoc.modulo);
			formData.append('tipologia', formDoc.tipologia);
			formData.append('dataScadenza', formDoc.dataScadenza);

			// Invio al backend
			const nuovoDoc = await DocumentoService.uploadDocumento(formDoc.idAzienda, formData);

			// Aggiornamento UI locale
			documenti = [nuovoDoc, ...documenti];

			// Reset e chiusura
			showModal = false;
			selectedFile = null;
			formDoc = {
				idAzienda: 0,
				modulo: ModuloServizio.SICUREZZA,
				tipologia: TipoDocumento.DVR,
				dataScadenza: '',
				stato: StatoDocumento.CARICATO
			};
		} catch (error) {
			console.error("Errore durante l'upload:", error);
			alert("Errore durante il caricamento del documento.");
		} finally {
			isSaving = false;
		}
	}

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

	async function handleDownload(idDocumento: number, filename: string) {
		try {
			const blob = await DocumentoService.downloadDocumento(idDocumento);
			const url = window.URL.createObjectURL(blob);
			const a = document.createElement('a');
			a.href = url;
			a.download = filename.toLowerCase().endsWith('.pdf') ? filename : `${filename}.pdf`;
			document.body.appendChild(a);
			a.click();
			window.URL.revokeObjectURL(url);
		} catch (error) {
			console.error("Errore download:", error);
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
			documenti.filter(d =>
					d.ragioneSocialeAzienda.toLowerCase().includes(searchQuery.toLowerCase()) ||
					d.tipologia.toLowerCase().includes(searchQuery.toLowerCase())
			).sort((a, b) => new Date(a.dataScadenza).getTime() - new Date(b.dataScadenza).getTime())
	);

	const statistiche = $derived({
		totali: documenti.length,
		scaduti: documenti.filter(d => new Date(d.dataScadenza) < new Date()).length,
		inAttesa: documenti.filter(d => d.stato === StatoDocumento.IN_ATTESA_FIRMA).length
	});

	function handleFileChange(e: Event) {
		const target = e.target as HTMLInputElement;
		if (target.files && target.files.length > 0) {
			selectedFile = target.files[0];
		}
	}
</script>

<div in:fade>
	<div class="mb-10 flex justify-between items-start">
		<div>
			<h1 class="text-4xl font-extrabold text-[#1B4B6B]">SCADENZIARIO</h1>
			<p class="text-gray-500 font-bold uppercase text-xs tracking-tighter">Monitoraggio scadenze e archivio documentale.</p>
		</div>

		<button
				onclick={() => showModal = true}
				class="bg-white text-[#1B4B6B] border-2 border-[#1B4B6B] px-8 py-3.5 rounded-xl font-extrabold uppercase text-xs shadow-sm hover:bg-[#1B4B6B] hover:text-white transition-all flex items-center gap-3"
		>
			<Upload size={18} />
			Carica Documento
		</button>
	</div>

	<div class="grid grid-cols-1 md:grid-cols-3 gap-6 mb-8">
		<div class="bg-white p-6 rounded-2xl shadow-sm border border-gray-100 flex items-center justify-between hover:shadow-md hover:border-[#1B4B6B]/30 transition-all cursor-default">
			<div>
				<p class="text-[10px] font-bold text-gray-400 uppercase tracking-wider mb-1">Archivio Globale</p>
				<h2 class="text-3xl font-extrabold text-[#1B4B6B]">{statistiche.totali}</h2>
			</div>
			<div class="bg-blue-50 p-4 rounded-xl text-[#1B4B6B]"><FileText size={24} /></div>
		</div>
		<div class="bg-white p-6 rounded-2xl shadow-sm border border-red-100 flex items-center justify-between hover:shadow-md transition-all cursor-default">
			<div>
				<p class="text-[10px] font-bold text-red-400 uppercase tracking-wider mb-1">Pratiche Scadute</p>
				<h2 class="text-3xl font-extrabold text-red-600">{statistiche.scaduti}</h2>
			</div>
			<div class="bg-red-50 p-4 rounded-xl text-red-600"><ShieldAlert size={24} /></div>
		</div>
		<div class="bg-white p-6 rounded-2xl shadow-sm border border-yellow-100 flex items-center justify-between hover:shadow-md transition-all cursor-default">
			<div>
				<p class="text-[10px] font-bold text-yellow-500 uppercase tracking-wider mb-1">In Attesa di Firma</p>
				<h2 class="text-3xl font-extrabold text-yellow-600">{statistiche.inAttesa}</h2>
			</div>
			<div class="bg-yellow-50 p-4 rounded-xl text-yellow-600"><Clock size={24} /></div>
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
					<tr><td colspan="5" class="px-6 py-12 text-center text-gray-400 font-bold uppercase text-xs">Nessun documento trovato.</td></tr>
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

{#if showModal}
	<div class="fixed inset-0 z-[100] flex items-center justify-center p-4 bg-black/60 backdrop-blur-sm" in:fade>
		<div class="bg-white w-full max-w-2xl rounded-3xl shadow-2xl overflow-hidden flex flex-col max-h-[90vh]" in:scale>
			<div class="bg-[#1B4B6B] p-8 text-white flex justify-between items-center shrink-0">
				<h2 class="text-2xl font-extrabold uppercase tracking-tight">Caricamento Documento</h2>
				<button onclick={() => showModal = false}><X size={28} /></button>
			</div>
			<div class="p-8 overflow-y-auto custom-scrollbar flex-1 bg-gray-50/30">
				<div class="grid grid-cols-1 md:grid-cols-2 gap-8">
					<div class="col-span-2 space-y-2">
						<label class="text-[10px] font-bold text-gray-400 uppercase tracking-widest ml-1">Azienda *</label>
						<select bind:value={formDoc.idAzienda} class="w-full px-5 py-3 bg-white border border-gray-200 rounded-2xl font-extrabold uppercase text-xs text-[#1B4B6B] outline-none focus:ring-2 focus:ring-[#1B4B6B]">
							<option value={0} disabled>Seleziona un'azienda...</option>
							{#each aziende as azienda (azienda.idUtente)}
								<option value={azienda.idUtente}>{azienda.ragioneSociale}</option>
							{/each}
						</select>
					</div>
					<div class="space-y-2">
						<label class="text-[10px] font-bold text-gray-400 uppercase tracking-widest ml-1">Modulo Servizio</label>
						<select bind:value={formDoc.modulo} class="w-full px-5 py-3 bg-white border border-gray-200 rounded-2xl font-bold uppercase text-xs outline-none">
							{#each Object.values(ModuloServizio) as m}
								<option value={m}>{m}</option>
							{/each}
						</select>
					</div>
					<div class="space-y-2">
						<label class="text-[10px] font-bold text-gray-400 uppercase tracking-widest ml-1">Tipologia Documentale</label>
						<select bind:value={formDoc.tipologia} class="w-full px-5 py-3 bg-white border border-gray-200 rounded-2xl font-bold uppercase text-xs outline-none">
							{#each Object.values(TipoDocumento) as t}
								<option value={t}>{t.replace('_', ' ')}</option>
							{/each}
						</select>
					</div>
					<div class="col-span-2 md:col-span-1 space-y-2">
						<label class="text-[10px] font-bold text-gray-400 uppercase tracking-widest ml-1">Data Scadenza *</label>
						<input bind:value={formDoc.dataScadenza} type="date" class="w-full px-5 py-3 bg-white border border-gray-200 rounded-2xl font-bold text-xs outline-none" />
					</div>

					<div class="col-span-2">
						<label class="text-[10px] font-bold text-gray-400 uppercase tracking-widest ml-1 mb-2 block">File Sorgente (PDF)</label>
						<div class="relative group border-2 border-dashed border-gray-200 rounded-2xl p-8 flex flex-col items-center justify-center transition-all cursor-pointer {selectedFile ? 'border-green-400 bg-green-50' : 'hover:bg-gray-50 hover:border-[#1B4B6B]'}">
							<input type="file" accept="application/pdf" class="absolute inset-0 opacity-0 cursor-pointer" onchange={handleFileChange} />
							{#if selectedFile}
								<FileCheck size={32} class="text-green-500 mb-3" />
								<p class="font-black text-xs uppercase text-green-600 truncate max-w-xs">{selectedFile.name}</p>
							{:else}
								<Upload size={32} class="text-gray-400 mb-3 group-hover:text-[#1B4B6B]" />
								<p class="font-bold text-xs uppercase tracking-widest text-gray-400 group-hover:text-[#1B4B6B]">Sfoglia o Trascina file</p>
							{/if}
						</div>
					</div>
				</div>
			</div>
			<div class="p-8 border-t border-gray-100 flex gap-4 bg-white rounded-b-3xl">
				<button onclick={() => showModal = false} class="flex-1 px-6 py-4 border-2 border-gray-100 text-gray-400 font-extrabold rounded-2xl uppercase text-xs">Annulla</button>
				<button
						onclick={salvaNuovoDocumento}
						disabled={isSaving}
						class="flex-1 px-6 py-4 bg-[#1B4B6B] text-white font-extrabold rounded-2xl shadow-xl uppercase text-xs disabled:opacity-50 flex items-center justify-center gap-2"
				>
					{#if isSaving} <Loader2 size={16} class="animate-spin" /> Elaborazione... {:else} Carica Documento {/if}
				</button>
			</div>
		</div>
	</div>
{/if}

<style>
	.custom-scrollbar::-webkit-scrollbar { width: 5px; }
	.custom-scrollbar-thumb { background: #E2E8F0; border-radius: 10px; }
	tr:hover { background-color: white !important; }
</style>