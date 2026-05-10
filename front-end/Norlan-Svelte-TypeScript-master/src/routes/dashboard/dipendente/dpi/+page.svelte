<script lang="ts">
	import { onMount } from 'svelte';
	import { fade, scale } from 'svelte/transition';
	import { goto } from '$app/navigation';
	import { HardHat, Search, Loader2 } from 'lucide-svelte';

	import { AuthService } from '$lib/services/AuthService';
	import { LavoratoreService, type DipendenteDTO } from '$lib/services/LavoratoreService';
	import DpiCard from '$lib/Components/Features/Documentale/DpiCard.svelte';
	import { getInfoScadenza, formattaDataScadenza, ordinaPerScadenza, type StatoScadenza } from '$lib/utils/scadenzeUtils';

	type OpzioneFiltro = 'TUTTI' | StatoScadenza;

	interface Dpi {
		id: number;
		nome: string;
		matricola: string;
		stato: StatoScadenza;
		revisione: string;
		consegna: string;
		dataPerSort: string;
	}

	interface DpiBackendData {
		idAssegnazione?: number;
		id?: number;
		tipo?: string;
		nomeDpi?: string;
		dataScadenzaRevisione?: string;
		dataScadenza?: string;
		dataConsegna?: string;
	}

	let isLoading = $state(true);
	let searchQuery = $state('');
	let filtroStato = $state<OpzioneFiltro>('TUTTI');

	let dotazioni = $state<Dpi[]>([]);
	let utente = $state<DipendenteDTO | null>(null);

	const opzioniFiltro: OpzioneFiltro[] = ['TUTTI', 'OK', 'WARNING', 'DANGER'];

	onMount(async () => {
		const session = AuthService.getSession();
		if (!session) return;

		try {
			const [dipendenteData, dpiDataRaw] = await Promise.all([
				LavoratoreService.getById(session.idUtente),
				LavoratoreService.getDpiByLavoratore(session.idUtente)
			]);

			utente = dipendenteData;

			const elencoDalServer = dpiDataRaw as unknown as DpiBackendData[];
			const tempDotazioni: Dpi[] = elencoDalServer.map((d) => {
				const idReale = d.idAssegnazione || d.id || Date.now();
				const dataScadenza = d.dataScadenzaRevisione || d.dataScadenza;
				const info = getInfoScadenza(dataScadenza);

				return {
					id: idReale,
					nome: d.tipo ? d.tipo.replace(/_/g, ' ') : (d.nomeDpi || 'SCONOSCIUTO'),
					matricola: `DPI-${idReale}`,
					stato: info.stato,
					revisione: formattaDataScadenza(dataScadenza),
					consegna: formattaDataScadenza(d.dataConsegna),
					dataPerSort: dataScadenza || ''
				};
			});

			dotazioni = ordinaPerScadenza(tempDotazioni, 'dataPerSort');

		} catch (error) {
			console.error("Si è verificato un errore nel caricamento dei DPI:", error);
		} finally {
			isLoading = false;
		}
	});

	const dpiFiltrati = $derived(
			dotazioni.filter(d => {
				const search = searchQuery.toLowerCase();
				const matchSearch = d.nome.toLowerCase().includes(search) || d.matricola.toLowerCase().includes(search);
				const matchFiltro = filtroStato === 'TUTTI' || d.stato === filtroStato;
				return matchSearch && matchFiltro;
			})
	);

	const conteggi = $derived({
		critici: dotazioni.filter(d => d.stato === 'DANGER').length,
		attenzione: dotazioni.filter(d => d.stato === 'WARNING').length
	});

	function richiediSostituzione(idDpi: number | string) {
		const dpi = dotazioni.find(d => d.id === idDpi);
		if (!dpi) return;
		const testo = `Salve, vorrei segnalare la necessità di sostituire o revisionare il seguente dispositivo: ${dpi.nome} (Matricola: ${dpi.matricola}).`;
		goto(`/dashboard/dipendente/messaggi?testo=${encodeURIComponent(testo)}`);
	}
</script>

<div in:fade class="max-w-[1600px] mx-auto space-y-8 pb-10">

	<div class="flex flex-col lg:flex-row justify-between items-start lg:items-center gap-8">
		<div>
			<h1 class="text-4xl font-black text-[#1B4B6B] uppercase tracking-tighter">I Miei DPI</h1>
			<p class="text-gray-400 font-bold uppercase text-[10px] tracking-widest mt-1">
				Utente: <span class="text-[#1B4B6B]">{utente?.nome ?? '...'} {utente?.cognome ?? ''}</span> | Gestione dotazioni e scadenze
			</p>
		</div>

		<div class="flex gap-4">
			<div class="bg-white px-6 py-4 rounded-2xl border border-gray-100 shadow-sm flex items-center gap-4">
				<div class="w-10 h-10 bg-red-50 text-red-500 rounded-xl flex items-center justify-center font-black">{conteggi.critici}</div>
				<span class="text-[9px] font-black text-gray-400 uppercase tracking-widest leading-tight">Scaduti</span>
			</div>
			<div class="bg-white px-6 py-4 rounded-2xl border border-gray-100 shadow-sm flex items-center gap-4">
				<div class="w-10 h-10 bg-amber-50 text-amber-500 rounded-xl flex items-center justify-center font-black">{conteggi.attenzione}</div>
				<span class="text-[9px] font-black text-gray-400 uppercase tracking-widest leading-tight">In Scadenza</span>
			</div>
		</div>
	</div>

	<div class="flex flex-col md:flex-row gap-4">
		<div class="relative flex-1 group">
			<Search class="absolute left-4 top-1/2 -translate-y-1/2 text-gray-400 group-focus-within:text-[#1B4B6B] transition-colors" size={20} />
			<input
					bind:value={searchQuery}
					type="text"
					placeholder="Cerca per nome o matricola..."
					class="w-full pl-12 pr-6 py-4 bg-white border border-gray-100 rounded-[1.5rem] text-xs font-bold uppercase outline-none focus:ring-4 focus:ring-[#1B4B6B]/5 shadow-sm transition-all"
			/>
		</div>

		<div class="flex bg-white shadow-sm border border-gray-100 p-1.5 rounded-[1.5rem] gap-1 overflow-x-auto">
			{#each opzioniFiltro as opzione (opzione)}
				<button
						onclick={() => impostaFiltro(opzione)}
						class="px-5 py-2.5 rounded-xl text-[9px] font-black uppercase tracking-widest transition-all whitespace-nowrap
                {filtroStato === opzione ? 'bg-[#1B4B6B] text-white shadow-md' : 'text-gray-400 hover:text-[#1B4B6B]'}"
				>
					{opzione}
				</button>
			{/each}
		</div>
	</div>

	{#if isLoading}
		<div class="py-32 flex flex-col items-center justify-center gap-4">
			<Loader2 size={48} class="animate-spin text-[#1B4B6B]" />
			<span class="text-[10px] font-black text-gray-400 uppercase tracking-widest">Sincronizzazione inventario DPI...</span>
		</div>
	{:else}
		<div class="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6">
			{#each dpiFiltrati as dpi (dpi.id)}
				<div in:scale={{duration: 300}}>
					<DpiCard
							ruolo="dipendente"
							dpi={{
                      id: dpi.id,
                      nome: dpi.nome,
                      matricola: dpi.matricola,
                      stato: dpi.stato,
                      dataRevisione: dpi.revisione,
                      dataConsegna: dpi.consegna
                   }}
							onRichiediSostituzione={richiediSostituzione}
					/>
				</div>
			{/each}
		</div>

		{#if dpiFiltrati.length === 0}
			<div class="py-20 bg-white border border-gray-100 rounded-[2.5rem] flex flex-col items-center justify-center text-center shadow-sm">
				<HardHat size={48} class="text-gray-200 mb-4" />
				<h3 class="text-xl font-black text-[#1B4B6B] uppercase italic">Nessun dispositivo trovato</h3>
				<p class="text-[10px] font-bold text-gray-400 uppercase tracking-widest mt-2">L'inventario non contiene corrispondenze per i criteri inseriti.</p>
			</div>
		{/if}
	{/if}
</div>

<style>
	:global(body) { background-color: #F9FAFB; }
</style>