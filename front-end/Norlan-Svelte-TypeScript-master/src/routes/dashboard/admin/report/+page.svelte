<script lang="ts">
	import { onMount } from 'svelte';
	import { fade } from 'svelte/transition';
	import {
		Activity, FileText, ShieldAlert, Search, Download,
		User, ChevronLeft, ChevronRight, Filter
	} from 'lucide-svelte';

	import { SistemaService } from '$lib/services/SistemaService';
	import type { LogSincronizzazione } from '$lib/models/LogSincronizzazione';
	import StatCard from '$lib/Components/UI/StatCard.svelte';

	let { children } = $props();

	let logs = $state<LogSincronizzazione[]>([]);
	let isLoading = $state(true);
	let searchQuery = $state('');

	type FiltroGravita = 'TUTTI' | 'INFO' | 'ERROR';
	const filtriDisponibili: FiltroGravita[] = ['TUTTI' , 'INFO' , 'ERROR'];
	let filtroGravita = $state<FiltroGravita>('TUTTI');

	let currentPage = $state(1);
	const itemsPerPage = 15;

	$effect(() => {
		void searchQuery;
		void filtroGravita;
		currentPage = 1;
	});

	onMount(async () => {
		try {
			logs = await SistemaService.getAllLogs();
			logs.sort((a, b) => new Date(b.dataEvento).getTime() - new Date(a.dataEvento).getTime());
		} catch (error) {
			console.error("Errore durante il recupero dei log di sistema:", error);
		} finally {
			isLoading = false;
		}
	});

	function getCategory(desc: string): string {
		const test = desc.toLowerCase();
		if (test.includes('accesso') || test.includes('login') || test.includes('password')) return 'SICUREZZA';
		if (test.includes('documento') || test.includes('upload') || test.includes('file')) return 'DOCUMENTI';
		if (test.includes('corso') || test.includes('formazione') || test.includes('registro')) return 'FORMAZIONE';
		return 'SISTEMA';
	}

	const filteredLogs = $derived(
			logs.filter(l => {
				const matchSearch = l.descrizioneEvento.toLowerCase().includes(searchQuery.toLowerCase()) ||
						(l.noteTecniche && l.noteTecniche.toLowerCase().includes(searchQuery.toLowerCase()));
				const matchGravita = filtroGravita === 'TUTTI' ||
						(filtroGravita === 'ERROR' && !l.esitoPositivo) ||
						(filtroGravita === 'INFO' && l.esitoPositivo);
				return matchSearch && matchGravita;
			})
	);

	const totalPages = $derived(Math.ceil(filteredLogs.length / itemsPerPage) || 1);
	const paginatedLogs = $derived(
			filteredLogs.slice((currentPage - 1) * itemsPerPage, currentPage * itemsPerPage)
	);

	const operazioniTotali = $derived(logs.length);
	const accessiRilevati = $derived(logs.filter(l => getCategory(l.descrizioneEvento) === 'SICUREZZA').length);
	const documentiProcessati = $derived(logs.filter(l => getCategory(l.descrizioneEvento) === 'DOCUMENTI').length);
	const erroriSicurezza = $derived(logs.filter(l => !l.esitoPositivo).length);

	function esportaReport() {
		if (filteredLogs.length === 0) {
			alert('Nessun log da esportare con i filtri attuali.');
			return;
		}

		const headers = ['Data Ora', 'Esito', 'Categoria', 'Azione', 'Dettagli Tecnici'];
		const csvRows = filteredLogs.map(l => {
			const dataStr = new Date(l.dataEvento).toLocaleString('it-IT').replace(',', '');
			const esitoStr = l.esitoPositivo ? 'INFO' : 'ERROR';
			const catStr = getCategory(l.descrizioneEvento);
			const azioneStr = `"${l.descrizioneEvento.replace(/"/g, '""')}"`;
			const noteStr = `"${(l.noteTecniche || '').replace(/"/g, '""')}"`;

			return [dataStr, esitoStr, catStr, azioneStr, noteStr].join(',');
		});

		const csvContent = [headers.join(','), ...csvRows].join('\n');
		const blob = new Blob([csvContent], { type: 'text/csv;charset=utf-8;' });
		const url = URL.createObjectURL(blob);
		const link = document.createElement('a');
		link.href = url;
		link.download = `NorLan_Audit_Log_${new Date().toISOString().split('T')[0]}.csv`;
		link.click();
		URL.revokeObjectURL(url);
	}
</script>

<div in:fade class="p-4 md:p-6 lg:p-0">
	<div class="mb-6 md:mb-10 flex flex-col md:flex-row justify-between items-start md:items-center gap-4">
		<div>
			<h1 class="text-2xl md:text-4xl font-extrabold text-[#1B4B6B] uppercase tracking-tighter">REPORT & LOG</h1>
			<p class="text-gray-500 font-bold uppercase text-[10px] md:text-xs tracking-tighter mt-1">Tracciamento delle attività e statistiche di sistema.</p>
		</div>

		<button
				onclick={esportaReport}
				class="w-full md:w-auto justify-center bg-white text-[#1B4B6B] border-2 border-[#1B4B6B] px-8 py-3.5 rounded-xl font-extrabold uppercase text-xs shadow-sm hover:bg-[#1B4B6B] hover:text-white transition-all flex items-center gap-3"
		>
			<Download size={18} />
			Esporta Registri
		</button>
	</div>

	<div class="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-4 md:gap-6 mb-8">
		<StatCard titolo="Operazioni Totali" valore={operazioniTotali} icona={Activity} bgIcona="bg-blue-50" testoIcona="text-[#1B4B6B]" hoverBgIcona="group-hover:bg-[#1B4B6B]"/>
		<StatCard titolo="Accessi Rilevati" valore={accessiRilevati} icona={User} bgIcona="bg-green-50" testoIcona="text-green-600" hoverBgIcona="group-hover:bg-green-600"/>
		<StatCard titolo="Doc. Processati" valore={documentiProcessati} icona={FileText} bgIcona="bg-indigo-50" testoIcona="text-indigo-600" hoverBgIcona="group-hover:bg-indigo-600"/>
		<StatCard titolo="Avvisi / Errori" valore={erroriSicurezza} icona={ShieldAlert} bgIcona="bg-red-50" testoIcona="text-red-600" hoverBgIcona="group-hover:bg-red-600"/>
	</div>

	<div class="bg-white rounded-2xl shadow-sm border border-gray-100 overflow-hidden">
		<div class="p-4 md:p-6 border-b border-gray-50 flex flex-col md:flex-row justify-between items-start md:items-center bg-gray-50/30 gap-4">
			<div class="flex flex-col sm:flex-row items-start sm:items-center gap-4 w-full md:w-auto">
				<div class="relative w-full md:w-72">
					<Search class="absolute left-3 top-1/2 -translate-y-1/2 text-gray-400" size={16} />
					<input
							bind:value={searchQuery}
							type="text"
							placeholder="Cerca per azione o dettaglio..."
							class="w-full pl-10 pr-4 py-2.5 bg-white border border-gray-200 rounded-xl text-xs focus:ring-2 focus:ring-[#1B4B6B] outline-none transition-all font-bold uppercase"
					/>
				</div>
				<div class="flex bg-gray-100 p-1 rounded-xl w-full sm:w-auto justify-center">
					{#each filtriDisponibili as filtro (filtro)}
						<button
								onclick={() => filtroGravita = filtro}
								class="flex-1 sm:flex-none px-4 py-1.5 rounded-lg text-[9px] font-black uppercase transition-all {filtroGravita === filtro ? 'bg-white shadow text-[#1B4B6B]' : 'text-gray-400 hover:text-gray-600'}"
						>
							{filtro}
						</button>
					{/each}
				</div>
			</div>

			<div class="flex items-center gap-2 text-gray-400 font-bold text-[10px] uppercase">
				<Filter size={14} />
				<span>{filteredLogs.length} Risultati</span>
			</div>
		</div>

		<div class="overflow-x-auto min-h-[400px]">
			<table class="w-full text-left min-w-[800px]">
				<thead class="bg-gray-50 text-[10px] font-bold text-gray-400 uppercase tracking-widest">
				<tr>
					<th class="px-6 py-4">Data e Ora</th>
					<th class="px-6 py-4">Esito</th>
					<th class="px-6 py-4">Categoria</th>
					<th class="px-6 py-4">Azione Registrata</th>
					<th class="px-6 py-4">Dettagli Tecnici</th>
				</tr>
				</thead>
				<tbody class="divide-y divide-gray-50">
				{#if isLoading}
					<tr>
						<td colspan="5" class="px-6 py-20 text-center text-gray-400 font-bold uppercase text-xs">
							<div class="flex flex-col items-center gap-3">
								<Activity class="animate-pulse text-[#1B4B6B]" size={32} />
								Sincronizzazione registri di sicurezza...
							</div>
						</td>
					</tr>
				{:else if paginatedLogs.length === 0}
					<tr>
						<td colspan="5" class="px-6 py-20 text-center text-gray-400 font-bold uppercase text-xs">
							<ShieldAlert size={32} class="mx-auto mb-3 opacity-20" />
							Nessun log trovato per i filtri impostati.
						</td>
					</tr>
				{:else}
					{#each paginatedLogs as log (log.idLog)}
						<tr class="transition-all group relative {log.esitoPositivo ? 'hover:bg-gray-50 bg-white' : 'bg-red-50/40 hover:bg-red-50/80'}">
							<td class="px-6 py-4 whitespace-nowrap">
                         <span class="text-xs font-bold {log.esitoPositivo ? 'text-[#1B4B6B]' : 'text-red-700'}">
                          {new Date(log.dataEvento).toLocaleString('it-IT', { day: '2-digit', month: '2-digit', year: 'numeric', hour: '2-digit', minute: '2-digit', second: '2-digit' })}
                         </span>
							</td>
							<td class="px-6 py-4">
                         <span class="text-[9px] font-black px-2.5 py-1 rounded-md uppercase tracking-widest {log.esitoPositivo ? 'bg-blue-50 text-blue-600' : 'bg-red-600 text-white shadow-sm shadow-red-200'}">
                          {log.esitoPositivo ? 'INFO' : 'ERROR'}
                         </span>
							</td>
							<td class="px-6 py-4">
                         <span class="font-extrabold text-[10px] uppercase {log.esitoPositivo ? 'text-gray-400' : 'text-red-400'}">
                             {getCategory(log.descrizioneEvento)}
                         </span>
							</td>
							<td class="px-6 py-4">
								<span class="font-bold text-xs uppercase {log.esitoPositivo ? 'text-[#1B4B6B]' : 'text-red-700'}">{log.descrizioneEvento}</span>
							</td>
							<td class="px-6 py-4 text-xs font-medium max-w-md truncate {log.esitoPositivo ? 'text-gray-500' : 'text-red-500/80'}">
								{log.noteTecniche || '-'}
							</td>
						</tr>
					{/each}
				{/if}
				</tbody>
			</table>
		</div>
		{#if totalPages > 1}
			<div class="p-4 border-t border-gray-50 flex flex-col sm:flex-row items-center justify-between bg-gray-50/30 gap-4">
				<p class="text-[10px] font-bold text-gray-400 uppercase tracking-widest">
					Pagina {currentPage} di {totalPages}
				</p>
				<div class="flex gap-2">
					<button
							disabled={currentPage === 1}
							onclick={() => currentPage--}
							class="p-2 rounded-lg bg-white border border-gray-200 text-gray-500 disabled:opacity-50 hover:bg-gray-50 transition-colors"
					>
						<ChevronLeft size={16} />
					</button>
					<button
							disabled={currentPage === totalPages}
							onclick={() => currentPage++}
							class="p-2 rounded-lg bg-white border border-gray-200 text-gray-500 disabled:opacity-50 hover:bg-gray-50 transition-colors"
					>
						<ChevronRight size={16} />
					</button>
				</div>
			</div>
		{/if}
	</div>
</div>

<style>
	.custom-scrollbar::-webkit-scrollbar { width: 5px; }
	.custom-scrollbar::-webkit-scrollbar-thumb { background: #E2E8F0; border-radius: 10px; }
</style>