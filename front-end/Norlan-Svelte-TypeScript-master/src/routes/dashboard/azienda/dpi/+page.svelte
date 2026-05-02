<script lang="ts">
	import { onMount } from 'svelte';
	import { fade } from 'svelte/transition';
	import {
		Search, Plus, ShieldCheck, AlertTriangle, Clock, Loader2
	} from 'lucide-svelte';

	// Import Servizi e Interfacce (rimosso l'import inutilizzato di AssegnazioneDPIDTO)
	import { LavoratoreService } from '$lib/services/LavoratoreService';
	import { AuthService } from '$lib/services/AuthService';

	// --- INTERFACCE LOCALI ---
	// Svincoliamo l'interfaccia dal DTO per evitare errori TypeScript, dichiarando tutti i campi necessari
	interface DpiRegistro {
		idAssegnazione?: number;
		id?: number;
		nomeCompletoDipendente: string;
		statoDerivato: 'OK' | 'DA_REVISIONARE' | 'SCADUTO';
		tipo?: string;
		nomeDpi?: string;
		dataConsegna?: string;
		dataScadenzaRevisione?: string;
	}

	// --- STATO REATTIVO (Svelte 5) ---
	let isLoading = $state(true);
	let searchQuery = $state('');
	let filtroAttivo = $state('TUTTI');
	let registro = $state<DpiRegistro[]>([]);

	// --- LOGICA DI CARICAMENTO ---
	onMount(async () => {
		const session = AuthService.getSession();
		if (!session) return;

		try {
			// 1. Recupero i dipendenti dell'azienda
			const dipendenti = await LavoratoreService.getByAzienda(session.idUtente);

			// 2. Recupero i DPI per ogni dipendente e li unisco in un unico registro
			const promises = dipendenti.map(async (d) => {
				const dpiLavoratore = await LavoratoreService.getDpiByLavoratore(d.idUtente);

				// Usiamo (dpi: any) per ignorare il vecchio modello TypeScript e accogliere i nuovi dati del backend
				return dpiLavoratore.map((dpi: any) => ({
					...dpi,
					nomeCompletoDipendente: `${d.nome} ${d.cognome}`.toUpperCase(),
					statoDerivato: calcolaStato(dpi.dataScadenzaRevisione)
				})) as DpiRegistro[];
			});

			const risultati = await Promise.all(promises);
			registro = risultati.flat();

		} catch (error) {
			console.error("Errore nel caricamento del registro DPI:", error);
		} finally {
			isLoading = false;
		}
	});

	/**
	 * Calcola lo stato in base alla data di scadenza
	 */
	function calcolaStato(dataScadenzaStr: string | undefined): 'OK' | 'DA_REVISIONARE' | 'SCADUTO' {
		if (!dataScadenzaStr) return 'OK'; // Fallback se la data manca
		const oggi = new Date();
		const scadenza = new Date(dataScadenzaStr);
		const diffGiorni = Math.ceil((scadenza.getTime() - oggi.getTime()) / (1000 * 3600 * 24));

		if (diffGiorni < 0) return 'SCADUTO';
		if (diffGiorni <= 30) return 'DA_REVISIONARE';
		return 'OK';
	}

	function formattaData(dateStr: string | undefined) {
		if (!dateStr) return 'N.D.';
		return new Date(dateStr).toLocaleDateString('it-IT');
	}

	// --- LOGICA REATTIVA ---
	const filteredRegistro = $derived(
			registro.filter(d => {
				const tipoSafe = d.tipo ? d.tipo.toString().toLowerCase() : '';
				const nomeDpiSafe = d.nomeDpi ? d.nomeDpi.toLowerCase() : '';
				const matchSearch = d.nomeCompletoDipendente.toLowerCase().includes(searchQuery.toLowerCase()) ||
						tipoSafe.includes(searchQuery.toLowerCase()) ||
						nomeDpiSafe.includes(searchQuery.toLowerCase());
				const matchFiltro = filtroAttivo === 'TUTTI' || d.statoDerivato === filtroAttivo;
				return matchSearch && matchFiltro;
			})
	);

	const stats = $derived({
		scaduti: registro.filter(d => d.statoDerivato === 'SCADUTO').length
	});
</script>

<div in:fade>
	<div class="mb-10 flex justify-between items-start">
		<div>
			<h1 class="text-4xl font-extrabold text-[#1B4B6B] uppercase tracking-tighter">Registro DPI</h1>
			<p class="text-gray-500 font-bold uppercase text-xs tracking-tighter">Gestione assegnazione e ispezione attrezzature NorLan.</p>
		</div>

		<div class="flex items-center gap-6">
			<div class="bg-white p-4 rounded-2xl shadow-sm border border-red-50 flex items-center gap-4">
				<div class="bg-red-50 p-2 rounded-lg text-red-500"><AlertTriangle size={20} /></div>
				<div>
					<p class="text-[9px] font-bold text-gray-400 uppercase">Scaduti</p>
					<p class="text-xs font-black text-red-600 uppercase">{stats.scaduti}</p>
				</div>
			</div>

			<a
					href="/dashboard/azienda/dipendenti"
					class="bg-white text-[#1B4B6B] border-2 border-[#1B4B6B] px-8 py-3.5 rounded-xl font-extrabold uppercase text-xs shadow-lg hover:bg-[#1B4B6B] hover:text-white transition-all flex items-center gap-3"
			>
				<Plus size={18} />
				Assegna Nuovo DPI
			</a>
		</div>
	</div>

	<div class="bg-white p-6 rounded-3xl shadow-sm border border-gray-100 mb-8 flex flex-col md:flex-row justify-between items-center gap-6">
		<div class="flex gap-2">
			{#each ['TUTTI', 'OK', 'DA_REVISIONARE', 'SCADUTO'] as f (f)}
				<button
						onclick={() => (filtroAttivo = f)}
						class="px-4 py-2 rounded-xl text-[10px] font-black uppercase transition-all {filtroAttivo === f ? 'bg-[#1B4B6B] text-white' : 'bg-gray-50 text-gray-400 hover:bg-gray-100'}"
				>
					{f.replace('_', ' ')}
				</button>
			{/each}
		</div>

		<div class="relative w-full md:w-96">
			<Search class="absolute left-4 top-1/2 -translate-y-1/2 text-gray-400" size={18} />
			<input
					bind:value={searchQuery}
					type="text"
					placeholder="Cerca dipendente o DPI..."
					class="w-full pl-12 pr-4 py-3 bg-gray-50 border-transparent rounded-2xl focus:ring-2 focus:ring-[#1B4B6B]/10 outline-none font-bold text-xs uppercase transition-all"
			/>
		</div>
	</div>

	<div class="bg-white rounded-3xl shadow-sm border border-gray-100 overflow-hidden">
		<div class="overflow-x-auto">
			<table class="w-full text-left">
				<thead class="bg-gray-50/50 text-[10px] font-bold text-gray-400 uppercase tracking-widest">
				<tr>
					<th class="px-8 py-5 text-center w-20">DPI</th>
					<th class="px-6 py-5 text-center">Dipendente</th>
					<th class="px-6 py-5 text-center">Tipo Dispositivo</th>
					<th class="px-6 py-5 text-center">Data Consegna</th>
					<th class="px-6 py-5 text-center">Revisione</th>
					<th class="px-6 py-5 text-center">Stato</th>
				</tr>
				</thead>
				<tbody class="divide-y divide-gray-50">
				{#if isLoading}
					<tr><td colspan="6" class="px-8 py-20 text-center text-gray-300 font-black uppercase text-xs tracking-widest"><Loader2 size={32} class="animate-spin mx-auto mb-2" />Sincronizzazione...</td></tr>
				{:else}
					{#each filteredRegistro as item (item.idAssegnazione || item.id)}
						<!-- Logica condizionale stringente per il nome DPI -->
						{@const nomeDpiReale = (item.tipo === 'ALTRO' && item.nomeDpi) ? item.nomeDpi : (item.tipo || 'NON DEFINITO').replace(/_/g, ' ')}

						<tr class="hover:bg-white hover:shadow-xl hover:shadow-blue-900/5 transition-all group relative">

							<td class="px-8 py-6 text-center">
								<div class="h-12 px-4 bg-gray-50 rounded-xl inline-flex items-center justify-center text-gray-400 group-hover:bg-[#1B4B6B] group-hover:text-white transition-colors font-black text-[9px] uppercase tracking-widest min-w-[3rem] text-center">
									DPI
								</div>
							</td>

							<td class="px-6 py-6 text-center"><span class="font-black text-[#1B4B6B] text-xs uppercase">{item.nomeCompletoDipendente}</span></td>

							<td class="px-6 py-6 text-center">
								<span class="font-bold text-[#1B4B6B] text-xs uppercase">{nomeDpiReale}</span>
							</td>

							<td class="px-6 py-6 text-xs text-gray-400 font-medium text-center">{formattaData(item.dataConsegna)}</td>
							<td class="px-6 py-6 text-xs font-black text-[#1B4B6B] text-center">{formattaData(item.dataScadenzaRevisione)}</td>

							<td class="px-6 py-6 text-center">
								<div class="inline-flex items-center gap-2 px-3 py-1 rounded-full border text-[9px] font-black uppercase {item.statoDerivato === 'OK' ? 'bg-green-50 text-green-600 border-green-100' : item.statoDerivato === 'DA_REVISIONARE' ? 'bg-yellow-50 text-yellow-600 border-yellow-100' : 'bg-red-50 text-red-600 border-red-100'}">
									{#if item.statoDerivato === 'OK'}<ShieldCheck size={12} />{:else if item.statoDerivato === 'DA_REVISIONARE'}<Clock size={12} />{:else}<AlertTriangle size={12} />{/if}
									{item.statoDerivato.replace('_', ' ')}
								</div>
							</td>
						</tr>
					{/each}
					{#if filteredRegistro.length === 0}
						<tr><td colspan="6" class="px-8 py-10 text-center text-gray-400 font-bold uppercase text-xs">Nessun DPI trovato nei registri.</td></tr>
					{/if}
				{/if}
				</tbody>
			</table>
		</div>
	</div>
</div>

<style>
	:global(body) { background-color: #F9FAFB; }
</style>