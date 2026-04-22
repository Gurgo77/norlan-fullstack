<script lang="ts">
	import { onMount } from 'svelte';
	import { fade } from 'svelte/transition';
	import {
		Activity, FileText, ShieldAlert,
		Search, Download, Clock, User
	} from 'lucide-svelte';

	interface LogData {
		id?: number;
		timestamp?: string;
		utente?: string;
		azione?: string;
		livello?: 'INFO' | 'WARNING' | 'ERROR';
		dettaglio?: string;
	}

	class LogAttivita {
		id: number;
		timestamp: string;
		utente: string;
		azione: string;
		livello: 'INFO' | 'WARNING' | 'ERROR';
		dettaglio: string;

		constructor(data: LogData) {
			this.id = data.id || Math.floor(Math.random() * 100000);
			this.timestamp = data.timestamp || new Date().toISOString();
			this.utente = data.utente || 'Sistema';
			this.azione = data.azione || 'Azione Sconosciuta';
			this.livello = data.livello || 'INFO';
			this.dettaglio = data.dettaglio || '';
		}
	}

	let logs = $state<LogAttivita[]>([]);
	let isLoading = $state(true);
	let searchQuery = $state('');

	onMount(() => {
		const mockLogs: LogData[] = [
			{ id: 1, timestamp: new Date().toISOString(), utente: 'admin@norlan.it', azione: 'Accesso effettuato', livello: 'INFO', dettaglio: 'Login completato con successo via Web' },
			{ id: 2, timestamp: new Date(Date.now() - 3600000).toISOString(), utente: 'info@fiat.it', azione: 'Download Documento', livello: 'INFO', dettaglio: 'Scaricato DVR Aggiornato 2026' },
			{ id: 3, timestamp: new Date(Date.now() - 7200000).toISOString(), utente: 'Sistema NorLan', azione: 'Scadenza Imminente Rilevata', livello: 'WARNING', dettaglio: 'Corso Antincendio per Tech Hub in scadenza tra 5 giorni' },
			{ id: 4, timestamp: new Date(Date.now() - 86400000).toISOString(), utente: 'Sconosciuto', azione: 'Tentativo Accesso Fallito', livello: 'ERROR', dettaglio: 'Password errata ripetuta per admin@norlan.it' },
			{ id: 5, timestamp: new Date(Date.now() - 90000000).toISOString(), utente: 'admin@norlan.it', azione: 'Creazione Corso', livello: 'INFO', dettaglio: 'Programmato nuovo corso Sicurezza Lavoratori' }
		];

		// FIX TYPE: Cast esplicito per evitare errori TS
		logs = (mockLogs as LogData[]).map(data => new LogAttivita(data)).sort((a, b) => new Date(b.timestamp).getTime() - new Date(a.timestamp).getTime());

		// Simulo un caricamento brevissimo per usare isLoading (ESLint fix)
		setTimeout(() => { isLoading = false; }, 300);
	});

	const filteredLogs = $derived(
		logs.filter(l =>
			l.utente.toLowerCase().includes(searchQuery.toLowerCase()) ||
			l.azione.toLowerCase().includes(searchQuery.toLowerCase())
		)
	);

	function esportaReport() {
		alert('Esportazione in preparazione...');
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
				<p class="text-[10px] font-bold text-gray-400 uppercase tracking-wider mb-0.5">Operazioni Oggi</p>
				<h2 class="text-2xl font-extrabold text-[#1B4B6B]">124</h2>
			</div>
		</div>

		<div class="bg-white p-6 rounded-2xl shadow-sm border border-gray-100 flex items-center gap-4 hover:shadow-md hover:border-[#1B4B6B]/30 transition-all cursor-default group">
			<div class="bg-green-50 p-4 rounded-xl text-green-600 shrink-0 group-hover:bg-green-600 group-hover:text-white transition-colors"><User size={24} /></div>
			<div>
				<p class="text-[10px] font-bold text-gray-400 uppercase tracking-wider mb-0.5">Accessi Unici</p>
				<h2 class="text-2xl font-extrabold text-[#1B4B6B]">18</h2>
			</div>
		</div>

		<div class="bg-white p-6 rounded-2xl shadow-sm border border-gray-100 flex items-center gap-4 hover:shadow-md hover:border-[#1B4B6B]/30 transition-all cursor-default group">
			<div class="bg-indigo-50 p-4 rounded-xl text-indigo-600 shrink-0 group-hover:bg-indigo-600 group-hover:text-white transition-colors"><FileText size={24} /></div>
			<div>
				<p class="text-[10px] font-bold text-gray-400 uppercase tracking-wider mb-0.5">Doc. Processati</p>
				<h2 class="text-2xl font-extrabold text-[#1B4B6B]">45</h2>
			</div>
		</div>

		<div class="bg-white p-6 rounded-2xl shadow-sm border border-red-50 flex items-center gap-4 hover:shadow-md hover:border-red-200 transition-all cursor-default group">
			<div class="bg-red-50 p-4 rounded-xl text-red-600 shrink-0 group-hover:bg-red-600 group-hover:text-white transition-colors"><ShieldAlert size={24} /></div>
			<div>
				<p class="text-[10px] font-bold text-red-400 uppercase tracking-wider mb-0.5">Avvisi Sicurezza</p>
				<h2 class="text-2xl font-extrabold text-red-600">1</h2>
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
					placeholder="Cerca per utente o azione..."
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
					<th class="px-6 py-4">Livello</th>
					<th class="px-6 py-4">Utente / Sistema</th>
					<th class="px-6 py-4">Azione Registrata</th>
					<th class="px-6 py-4">Dettagli</th>
				</tr>
				</thead>
				<tbody class="divide-y divide-gray-50">
				{#if isLoading}
					<tr>
						<td colspan="5" class="px-6 py-12 text-center text-gray-400 font-bold uppercase text-xs">
							Caricamento registri in corso...
						</td>
					</tr>
				{:else}
					{#each filteredLogs as log (log.id)}
						<tr class="hover:bg-white hover:shadow-lg transition-all group relative">
							<td class="px-6 py-4 whitespace-nowrap">
          <span class="text-xs font-bold text-[#1B4B6B]">
           {new Date(log.timestamp).toLocaleString('it-IT', { day: '2-digit', month: '2-digit', year: 'numeric', hour: '2-digit', minute: '2-digit', second: '2-digit' })}
          </span>
							</td>
							<td class="px-6 py-4">
          <span class="text-[9px] font-black px-2.5 py-1 rounded-md uppercase tracking-widest {log.livello === 'INFO' ? 'bg-blue-50 text-blue-600' : log.livello === 'WARNING' ? 'bg-yellow-50 text-yellow-600' : 'bg-red-50 text-red-600'}">
           {log.livello}
          </span>
							</td>
							<td class="px-6 py-4">
								<span class="font-extrabold text-[#1B4B6B] text-xs uppercase">{log.utente}</span>
							</td>
							<td class="px-6 py-4">
								<span class="font-bold text-gray-700 text-xs uppercase">{log.azione}</span>
							</td>
							<td class="px-6 py-4 text-xs font-medium text-gray-500 max-w-md truncate">
								{log.dettaglio}
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