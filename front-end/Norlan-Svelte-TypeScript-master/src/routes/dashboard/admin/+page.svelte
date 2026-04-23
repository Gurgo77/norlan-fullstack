<script lang="ts">
	import { onMount } from 'svelte';
	import { fade } from 'svelte/transition';
	import {
		Building2, FileClock, AlertCircle
	} from 'lucide-svelte';
	import { searchState } from '$lib/searchState.svelte';

	import { AnagraficaService } from '$lib/services/AnagraficaService';
	import { DocumentoService } from '$lib/services/DocumentoService';
	import { Azienda } from '$lib/models/Azienda'; // Assicurati che l'import sia corretto

	// Definiamo un'interfaccia per i dati della tabella scadenze per evitare "any"
	interface ScadenzaTabella {
		idAzienda: number;
		azienda: string;
		doc: string;
		status: 'critical' | 'warning';
	}

	let aziende = $state<Azienda[]>([]);
	let scadenzeImminenti = $state<ScadenzaTabella[]>([]);
	let isLoading = $state(true);

	onMount(async () => {
		try {
			// Castiamo il risultato a Azienda[] per allinearlo allo stato
			const datiAziende = await AnagraficaService.getAllAziende();
			aziende = datiAziende as Azienda[];

			const documentiInScadenza = await DocumentoService.getDocumentiInScadenza(7);

			scadenzeImminenti = documentiInScadenza.map(doc => {
				const dataScad = new Date(doc.dataScadenza).getTime();
				const oggi = new Date().getTime();
				const giorniMancanti = Math.ceil((dataScad - oggi) / (1000 * 3600 * 24));

				const statusLevel: 'critical' | 'warning' = giorniMancanti <= 3 ? 'critical' : 'warning';

				return {
					idAzienda: doc.idAzienda,
					azienda: doc.ragioneSocialeAzienda,
					doc: `${doc.tipologia} - ${doc.modulo}`,
					status: statusLevel
				};
			});
		} catch (error) {
			console.error("Errore nel caricamento:", error);
		} finally {
			isLoading = false;
		}
	});

	// La logica di filtro usando la barra di ricerca superiore ($derived state di Svelte 5)
	const aziendeFiltrate = $derived(
			aziende.filter(a => a.ragioneSociale?.toLowerCase().includes(searchState.query.toLowerCase()))
	);

	const scadenzeFiltrate = $derived(
			scadenzeImminenti.filter(s => s.azienda.toLowerCase().includes(searchState.query.toLowerCase()))
	);
</script>

<div in:fade>
	<div class="mb-10">
		<h1 class="text-4xl font-extrabold text-[#1B4B6B]">Ciao! Felice di rivederti.</h1>
		<p class="text-gray-500 font-bold uppercase text-xs tracking-tighter">Monitoraggio attivo del portale NorLan.</p>
	</div>

	{#if isLoading}
		<div class="flex justify-center items-center h-40">
			<div class="animate-spin rounded-full h-12 w-12 border-b-2 border-[#1B4B6B]"></div>
		</div>
	{:else}
		<div class="grid grid-cols-1 md:grid-cols-2 gap-8 mb-12">
			<div class="bg-white p-8 rounded-3xl shadow-sm border border-gray-100 flex items-center justify-between group hover:shadow-md transition-all">
				<div>
					<p class="text-[10px] font-bold text-gray-400 uppercase tracking-widest mb-1">Aziende Clienti</p>
					<h2 class="text-5xl font-black text-[#1B4B6B]">{aziendeFiltrate.length}</h2>
				</div>
				<div class="p-5 bg-blue-50 text-[#1B4B6B] rounded-2xl group-hover:bg-[#1B4B6B] group-hover:text-white transition-colors">
					<Building2 size={32} />
				</div>
			</div>

			<div class="bg-white p-8 rounded-3xl shadow-sm border border-gray-100 flex items-center justify-between group hover:shadow-md transition-all">
				<div>
					<p class="text-[10px] font-bold text-gray-400 uppercase tracking-widest mb-1">Scadenze 7GG</p>
					<h2 class="text-5xl font-black text-red-600">{scadenzeFiltrate.length}</h2>
				</div>
				<div class="p-5 bg-red-50 text-red-600 rounded-2xl group-hover:bg-red-600 group-hover:text-white transition-colors">
					<FileClock size={32} />
				</div>
			</div>
		</div>

		<div class="bg-white rounded-3xl shadow-sm border border-gray-100 overflow-hidden">
			<div class="p-8 border-b border-gray-50 flex items-center gap-3">
				<AlertCircle class="text-red-600" size={20} />
				<h2 class="font-extrabold text-[#1B4B6B] uppercase text-sm tracking-tight">Scadenze Imminenti</h2>
			</div>

			<div class="overflow-x-auto">
				<table class="w-full text-left">
					<thead class="bg-gray-50/50 text-[10px] font-black text-gray-400 uppercase tracking-widest">
					<tr>
						<th class="px-8 py-4">Azienda</th>
						<th class="px-8 py-4">Documentazione</th>
						<th class="px-8 py-4 text-center">Status</th>
						<th class="px-8 py-4 text-right">Azione</th>
					</tr>
					</thead>
					<tbody class="divide-y divide-gray-50">
					{#each scadenzeFiltrate as scadenza (scadenza.idAzienda + scadenza.doc)}
						<tr class="hover:bg-gray-50/50 transition-colors">
							<td class="px-8 py-6 font-black text-[#1B4B6B] text-xs uppercase">
								<a href="/dashboard/admin/aziende?id={scadenza.idAzienda}" class="hover:text-blue-600 hover:underline">
									{scadenza.azienda}
								</a>
							</td>
							<td class="px-8 py-6 font-bold text-gray-500 text-xs uppercase tracking-tighter">{scadenza.doc}</td>
							<td class="px-8 py-6 text-center">
								<div class="flex justify-center">
									<div class="w-3 h-3 rounded-full {scadenza.status === 'critical' ? 'bg-red-500 animate-pulse' : 'bg-yellow-500'}"></div>
								</div>
							</td>
							<td class="px-8 py-6 text-right">
								<a href="/dashboard/admin/aziende?id={scadenza.idAzienda}" class="inline-block bg-white border-2 border-[#1B4B6B] text-[#1B4B6B] px-4 py-1.5 rounded-lg font-black text-[10px] uppercase hover:bg-[#1B4B6B] hover:text-white transition-all">
									Gestisci
								</a>
							</td>
						</tr>
					{/each}
					{#if scadenzeFiltrate.length === 0}
						<tr>
							<td colspan="4" class="px-8 py-10 text-center text-gray-400 font-bold uppercase text-xs">
								Nessuna scadenza critica nei prossimi 7 giorni.
							</td>
						</tr>
					{/if}
					</tbody>
				</table>
			</div>
		</div>
	{/if}
</div>