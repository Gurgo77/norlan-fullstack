<script lang="ts">
	import { onMount } from 'svelte';
	import { fade } from 'svelte/transition';
	import {
		Activity, FileText, ShieldAlert,
		Search, Download, Clock, User
	} from 'lucide-svelte';

	// Importiamo il servizio e il modello reale
	import { SistemaService } from '$lib/services/SistemaService';
	import type { LogSincronizzazione } from '$lib/models/LogSincronizzazione';

	// --- STATO REATTIVO ---
	let logs = $state<LogSincronizzazione[]>([]);
	let isLoading = $state(true);
	let searchQuery = $state('');

	// --- AZIONI ---
	onMount(async () => {
		try {
			// Recupero i veri log dal backend tramite l'endpoint unificato
			logs = await SistemaService.getAllLogs();

			// Ordina i log dal più recente al più vecchio
			logs.sort((a, b) => new Date(b.dataEvento).getTime() - new Date(a.dataEvento).getTime());
		} catch (error) {
			console.error("Errore durante il recupero dei log di sistema:", error);
		} finally {
			isLoading = false;
		}
	});

	// --- LOGICA DERIVATA (Senza "any") ---
	const filteredLogs = $derived(
			logs.filter(l =>
					l.descrizioneEvento.toLowerCase().includes(searchQuery.toLowerCase()) ||
					(l.noteTecniche && l.noteTecniche.toLowerCase().includes(searchQuery.toLowerCase()))
			)
	);

	// Calcolo dinamico per le card in alto in base ai log reali
	const operazioniTotali = $derived(logs.length);
	const accessiRilevati = $derived(logs.filter(l => l.descrizioneEvento.toLowerCase().includes('accesso') || l.descrizioneEvento.toLowerCase().includes('login')).length);
	const documentiProcessati = $derived(logs.filter(l => l.descrizioneEvento.toLowerCase().includes('documento') || l.descrizioneEvento.toLowerCase().includes('upload')).length);
	const erroriSicurezza = $derived(logs.filter(l => !l.esitoPositivo).length);

	function esportaReport() {
		alert('Funzionalità di esportazione CSV/PDF in preparazione...');
	}
</script>

<div in:fade>
	<div class="mb-10 flex justify-between items-start">
		<div>
			<h1 class="text-4xl font-extrabold text-[#1B4B6B]">REPORT & LOG</h1>
			<p class="text-gray-500 font-bold uppercase text-xs tracking-tighter">Tracciamento delle attività e statistiche di sistema.</p>
		</div>

		<button
				onclick={esportaReport}
				class="bg-white text-[#1B4B6B] border-2 border-[#1B4B6B] px-8 py-3.5 rounded-xl font-extrabold uppercase text-xs shadow-sm hover:bg-[#1B4B6B] hover:text-white transition-all flex items-center gap-3"
		>
			<Download size={18} />
			Esporta Registri
		</button>
	</div>

	<div class="grid grid-cols-1 md:grid-cols-4 gap-6 mb-8">
		<div class="bg-white p-6 rounded-2xl shadow-sm border border-gray-100 flex items-center gap-4 hover:shadow-md hover:border-[#1B4B6B]/30 transition-all cursor-default group">
			<div class="bg-blue-50 p-4 rounded-xl text-[#1B4B6B] shrink-0 group-hover:bg-[#1B4B6B] group-hover:text-white transition-colors"><Activity size={24} /></div>
			<div>
				<p class="text-[10px] font-bold text-gray-400 uppercase tracking-wider mb-0.5">Operazioni Totali</p>
				<h2 class="text-2xl font-extrabold text-[#1B4B6B]">{operazioniTotali}</h2>
			</div>
		</div>

		<div class="bg-white p-6 rounded-2xl shadow-sm border border-gray-100 flex items-center gap-4 hover:shadow-md hover:border-[#1B4B6B]/30 transition-all cursor-default group">
			<div class="bg-green-50 p-4 rounded-xl text-green-600 shrink-0 group-hover:bg-green-600 group-hover:text-white transition-colors"><User size={24} /></div>
			<div>
				<p class="text-[10px] font-bold text-gray-400 uppercase tracking-wider mb-0.5">Accessi Rilevati</p>
				<h2 class="text-2xl font-extrabold text-[#1B4B6B]">{accessiRilevati}</h2>
			</div>
		</div>

		<div class="bg-white p-6 rounded-2xl shadow-sm border border-gray-100 flex items-center gap-4 hover:shadow-md hover:border-[#1B4B6B]/30 transition-all cursor-default group">
			<div class="bg-indigo-50 p-4 rounded-xl text-indigo-600 shrink-0 group-hover:bg-indigo-600 group-hover:text-white transition-colors"><FileText size={24} /></div>
			<div>
				<p class="text-[10px] font-bold text-gray-400 uppercase tracking-wider mb-0.5">Doc. Processati</p>
				<h2 class="text-2xl font-extrabold text-[#1B4B6B]">{documentiProcessati}</h2>
			</div>
		</div>

		<div class="bg-white p-6 rounded-2xl shadow-sm border border-red-50 flex items-center gap-4 hover:shadow-md hover:border-red-200 transition-all cursor-default group">
			<div class="bg-red-50 p-4 rounded-xl text-red-600 shrink-0 group-hover:bg-red-600 group-hover:text-white transition-colors"><ShieldAlert size={24} /></div>
			<div>
				<p class="text-[10px] font-bold text-red-400 uppercase tracking-wider mb-0.5">Avvisi / Errori</p>
				<h2 class="text-2xl font-extrabold text-red-600">{erroriSicurezza}</h2>
			</div>
		</div>
	</div>

	<div class="bg-white rounded-2xl shadow-sm border border-gray-100 overflow-hidden">
		<div class="p-6 border-b border-gray-50 flex justify-between items-center bg-gray-50/30">
			<div class="relative w-96">
				<Search class="absolute left-3 top-1/2 -translate-y-1/2 text-gray-400" size={16} />
				<input
						bind:value={searchQuery}
						type="text"
						placeholder="Cerca per azione o dettaglio..."
						class="w-full pl-10 pr-4 py-2 bg-white border border-gray-200 rounded-xl text-xs focus:ring-2 focus:ring-[#1B4B6B] outline-none transition-all font-bold uppercase"
				/>
			</div>
			<div class="flex items-center gap-2 text-gray-400 font-bold text-[10px] uppercase">
				<Clock size={14} />
				<span>Ultimo aggiornamento: Ora</span>
			</div>
		</div>

		<div class="overflow-x-auto">
			<table class="w-full text-left">
				<thead class="bg-gray-50 text-[10px] font-bold text-gray-400 uppercase tracking-widest">
				<tr>
					<th class="px-6 py-4">Data e Ora</th>
					<th class="px-6 py-4">Esito</th>
					<th class="px-6 py-4">Sistema</th>
					<th class="px-6 py-4">Azione Registrata</th>
					<th class="px-6 py-4">Dettagli Tecnici</th>
				</tr>
				</thead>
				<tbody class="divide-y divide-gray-50">
				{#if isLoading}
					<tr>
						<td colspan="5" class="px-6 py-12 text-center text-gray-400 font-bold uppercase text-xs">
							Caricamento registri in corso...
						</td>
					</tr>
				{:else if filteredLogs.length === 0}
					<tr>
						<td colspan="5" class="px-6 py-12 text-center text-gray-400 font-bold uppercase text-xs">
							Nessun log trovato.
						</td>
					</tr>
				{:else}
					{#each filteredLogs as log (log.idLog)}
						<tr class="hover:bg-white hover:shadow-lg transition-all group relative">
							<td class="px-6 py-4 whitespace-nowrap">
                         <span class="text-xs font-bold text-[#1B4B6B]">
                          {new Date(log.dataEvento).toLocaleString('it-IT', { day: '2-digit', month: '2-digit', year: 'numeric', hour: '2-digit', minute: '2-digit', second: '2-digit' })}
                         </span>
							</td>
							<td class="px-6 py-4">
                         <span class="text-[9px] font-black px-2.5 py-1 rounded-md uppercase tracking-widest {log.esitoPositivo ? 'bg-blue-50 text-blue-600' : 'bg-red-50 text-red-600'}">
                          {log.esitoPositivo ? 'INFO' : 'ERROR'}
                         </span>
							</td>
							<td class="px-6 py-4">
								<span class="font-extrabold text-gray-400 text-[10px] uppercase">SISTEMA</span>
							</td>
							<td class="px-6 py-4">
								<span class="font-bold text-[#1B4B6B] text-xs uppercase">{log.descrizioneEvento}</span>
							</td>
							<td class="px-6 py-4 text-xs font-medium text-gray-500 max-w-md truncate">
								{log.noteTecniche || '-'}
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
	.custom-scrollbar::-webkit-scrollbar { width: 5px; }
	.custom-scrollbar::-webkit-scrollbar-thumb { background: #E2E8F0; border-radius: 10px; }
</style>