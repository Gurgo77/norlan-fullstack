<script lang="ts">
	import { onMount } from 'svelte';
	import { fade } from 'svelte/transition';
	import {
		Search, HardHat, Plus, Download,
		ShieldCheck, AlertTriangle, Clock, Loader2, ArrowRight
	} from 'lucide-svelte';

	// --- INTERFACCE (ESLint & TS Fix) ---
	interface DpiAssegnato {
		id: number;
		dipendente: string;
		tipoDispositivo: string;
		dataConsegna: string;
		revisione: string;
		stato: 'OK' | 'DA_REVISIONARE' | 'SCADUTO';
	}

	let isLoading = $state(true);
	let searchQuery = $state('');
	let filtroAttivo = $state('TUTTI');

	let registro = $state<DpiAssegnato[]>([
		{ id: 1, dipendente: 'MARIO ROSSI', tipoDispositivo: 'ELMETTO PROTETTIVO CON VISIERA', dataConsegna: '10/01/2025', revisione: '10/01/2026', stato: 'OK' },
		{ id: 2, dipendente: 'LUIGI BIANCHI', tipoDispositivo: 'IMBRACATURA ANTICADUTA MOD. X2', dataConsegna: '15/05/2024', revisione: '15/05/2025', stato: 'DA_REVISIONARE' },
		{ id: 3, dipendente: 'MARIO ROSSI', tipoDispositivo: 'GUANTI DIELETTRICI CLASSE 0', dataConsegna: '01/02/2025', revisione: '01/08/2025', stato: 'OK' },
		{ id: 4, dipendente: 'FRANCESCO NERI', tipoDispositivo: 'SCARPE ANTINFORTUNISTICHE S3', dataConsegna: '10/10/2023', revisione: '10/10/2024', stato: 'SCADUTO' }
	]);

	onMount(() => {
		setTimeout(() => {
			isLoading = false;
		}, 500);
	});

	const filteredRegistro = $derived(
		registro.filter(d => {
			const matchSearch = d.dipendente.toLowerCase().includes(searchQuery.toLowerCase()) ||
				d.tipoDispositivo.toLowerCase().includes(searchQuery.toLowerCase());
			const matchFiltro = filtroAttivo === 'TUTTI' || d.stato === filtroAttivo;
			return matchSearch && matchFiltro;
		})
	);

	const stats = $derived({
		scaduti: registro.filter(d => d.stato === 'SCADUTO').length
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

			<button
				class="bg-white text-[#1B4B6B] border-2 border-[#1B4B6B] px-8 py-3.5 rounded-xl font-extrabold uppercase text-xs shadow-lg hover:bg-[#1B4B6B] hover:text-white transition-all flex items-center gap-3"
			>
				<Plus size={18} />
				Assegna Nuovo DPI
			</button>
		</div>
	</div>

	<div class="bg-white p-6 rounded-3xl shadow-sm border border-gray-100 mb-8 flex flex-col md:flex-row justify-between items-center gap-6">
		<div class="flex gap-2">
			{#each ['TUTTI', 'OK', 'DA_REVISIONARE', 'SCADUTO'] as f}
				<button
					onclick={() => filtroAttivo = f}
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
					<th class="px-8 py-5 text-center w-20"></th>
					<th class="px-6 py-5">Dipendente</th>
					<th class="px-6 py-5">Tipo Dispositivo</th>
					<th class="px-6 py-5">Data Consegna</th>
					<th class="px-6 py-5">Revisione</th>
					<th class="px-6 py-5">Stato</th>
					<th class="px-8 py-5 text-right">Azioni</th>
				</tr>
				</thead>
				<tbody class="divide-y divide-gray-50">
				{#if isLoading}
					<tr><td colspan="7" class="px-8 py-20 text-center text-gray-300 font-black uppercase text-xs tracking-widest"><Loader2 size={32} class="animate-spin mx-auto mb-2" />Sincronizzazione...</td></tr>
				{:else}
					{#each filteredRegistro as item (item.id)}
						<tr class="hover:bg-white hover:shadow-xl hover:shadow-blue-900/5 transition-all group relative">
							<td class="px-8 py-6 text-center">
								<div class="size-12 bg-gray-50 rounded-xl flex items-center justify-center text-gray-400 group-hover:bg-[#1B4B6B] group-hover:text-white transition-colors">
									<HardHat size={24} />
								</div>
							</td>
							<td class="px-6 py-6"><span class="font-black text-[#1B4B6B] text-xs uppercase">{item.dipendente}</span></td>
							<td class="px-6 py-6">
								<div class="flex items-center gap-2">
									<HardHat size={14} class="text-[#1B4B6B] opacity-40" />
									<span class="font-bold text-[#1B4B6B] text-xs uppercase">{item.tipoDispositivo}</span>
								</div>
							</td>
							<td class="px-6 py-6 text-xs text-gray-400 font-medium">{item.dataConsegna}</td>
							<td class="px-6 py-6 text-xs font-black text-[#1B4B6B]">{item.revisione}</td>
							<td class="px-6 py-6">
								<div class="inline-flex items-center gap-2 px-3 py-1 rounded-full border text-[9px] font-black uppercase {item.stato === 'OK' ? 'bg-green-50 text-green-600 border-green-100' : item.stato === 'DA_REVISIONARE' ? 'bg-yellow-50 text-yellow-600 border-yellow-100' : 'bg-red-50 text-red-600 border-red-100'}">
									{#if item.stato === 'OK'}<ShieldCheck size={12} />{:else if item.stato === 'DA_REVISIONARE'}<Clock size={12} />{:else}<AlertTriangle size={12} />{/if}
									{item.stato.replace('_', ' ')}
								</div>
							</td>
							<td class="px-8 py-6 text-right">
								<div class="flex justify-end gap-3 items-center">
									<button class="p-2 text-gray-300 hover:text-[#1B4B6B] transition-colors"><Download size={18} /></button>

									<button class="flex items-center gap-2 bg-white text-[#1B4B6B] border-2 border-[#1B4B6B] px-5 py-2.5 rounded-xl font-black text-[10px] uppercase tracking-widest hover:bg-[#1B4B6B] hover:text-white transition-all shadow-sm">
										Gestisci
										<ArrowRight size={14} />
									</button>
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
    :global(body) { background-color: #F9FAFB; }
</style>