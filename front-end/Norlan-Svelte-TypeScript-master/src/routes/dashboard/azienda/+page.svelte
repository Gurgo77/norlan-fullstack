<script lang="ts">
	import { onMount } from 'svelte';
	import { fade } from 'svelte/transition'; // Rimosso scale e slide (ESLint Fix)
	import {
		ShieldCheck, AlertTriangle, FileText, HardHat,
		Calendar, MessageSquare, Download, RefreshCw,
		Users, BellRing, Clock, Loader2, Plus
	} from 'lucide-svelte';

	// --- INTERFACCE ---
	interface ComplianceItem {
		nome: string;
		stato: 'VERDE' | 'GIALLO' | 'ROSSO';
		scadenza: string;
	}

	interface DpiItem {
		id: number;
		tipo: string;
		dipendente: string;
		ultimaRevisione: string;
		prossimaRevisione: string;
		stato: 'OK' | 'SCADUTO' | 'IN_REVISIONE';
	}

	let isLoading = $state(true);

	let compliance = $state<ComplianceItem[]>([
		{ nome: 'DVR (Valutazione Rischi)', stato: 'VERDE', scadenza: '2027-05-10' },
		{ nome: 'HACCP', stato: 'GIALLO', scadenza: '2026-06-15' },
		{ nome: 'Nomina Medico Competente', stato: 'ROSSO', scadenza: '2026-03-01' }
	]);

	let registroDpi = $state<DpiItem[]>([
		{ id: 1, tipo: 'Elmetto Protettivo', dipendente: 'Mario Rossi', ultimaRevisione: '2025-01-10', prossimaRevisione: '2026-01-10', stato: 'SCADUTO' },
		{ id: 2, tipo: 'Imbracatura Sicurezza', dipendente: 'Luigi Bianchi', ultimaRevisione: '2026-01-05', prossimaRevisione: '2027-01-05', stato: 'OK' }
	]);

	const infoStato = $derived(
		compliance.some(c => c.stato === 'ROSSO')
			? { label: 'CRITICO', color: 'bg-red-500', icon: AlertTriangle }
			: compliance.some(c => c.stato === 'GIALLO')
				? { label: 'ATTENZIONE', color: 'bg-yellow-500', icon: Clock }
				: { label: 'A NORMA', color: 'bg-green-500', icon: ShieldCheck }
	);

	onMount(() => {
		setTimeout(() => {
			isLoading = false;
		}, 600);
	});
</script>

{#if isLoading}
	<div class="h-full w-full flex flex-col items-center justify-center gap-4" in:fade>
		<Loader2 size={48} class="animate-spin text-[#1B4B6B]" />
		<p class="font-bold uppercase text-[10px] tracking-widest text-gray-400">Analisi Compliance NorLan...</p>
	</div>
{:else}
	<div in:fade>
		<div class="mb-10 flex justify-between items-start">
			<div>
				<h1 class="text-4xl font-extrabold text-[#1B4B6B] uppercase tracking-tighter">Dashboard Aziendale</h1>
				<p class="text-gray-500 font-bold uppercase text-xs tracking-tighter">Monitoraggio sicurezza e scadenze.</p>
			</div>

			<button
				class="bg-white text-[#1B4B6B] border-2 border-[#1B4B6B] px-8 py-3.5 rounded-xl font-extrabold uppercase text-xs shadow-sm hover:bg-[#1B4B6B] hover:text-white transition-all flex items-center gap-3"
			>
				<Plus size={18} />
				Nuova Richiesta
			</button>
		</div>

		<div class="grid grid-cols-1 lg:grid-cols-3 gap-8 mb-10">
			<div class="lg:col-span-2 bg-white p-8 rounded-3xl shadow-sm border border-gray-100 flex items-center gap-8 hover:shadow-2xl hover:shadow-blue-900/10 hover:border-[#1B4B6B]/20 hover:-translate-y-1 transition-all duration-300 group cursor-default">
				<div class="relative">
					<div class="w-24 h-24 rounded-full flex items-center justify-center {infoStato.color} text-white shadow-lg">
						<infoStato.icon size={40} />
					</div>
				</div>
				<div>
					<p class="text-[10px] font-black text-gray-400 uppercase tracking-widest mb-1">Status Compliance</p>
					<h2 class="text-3xl font-black text-[#1B4B6B] uppercase">{infoStato.label}</h2>
					<p class="text-sm font-medium text-gray-500 mt-1 italic">Situazione aggiornata NorLan</p>
				</div>
			</div>

			<div class="bg-[#1B4B6B] p-8 rounded-3xl shadow-xl text-white relative overflow-hidden hover:shadow-2xl hover:shadow-blue-900/40 hover:-translate-y-1 transition-all duration-300 group">
				<BellRing class="absolute -right-4 -bottom-4 text-white/10 group-hover:scale-110 transition-transform duration-500" size={120} />
				<p class="text-[10px] font-black text-white/50 uppercase tracking-widest mb-4">Alert Scadenze</p>
				<div class="space-y-3 relative z-10">
					{#each compliance.filter(c => c.stato !== 'VERDE') as alert}
						<div class="flex items-center gap-3 bg-white/10 p-3 rounded-xl border border-white/10">
							<div class="w-2 h-2 rounded-full {alert.stato === 'ROSSO' ? 'bg-red-400' : 'bg-yellow-400'}"></div>
							<span class="text-xs font-bold uppercase truncate">{alert.nome}</span>
						</div>
					{/each}
				</div>
			</div>
		</div>

		<div class="grid grid-cols-1 xl:grid-cols-3 gap-8">
			<div class="xl:col-span-2 space-y-8">
				<section class="bg-white rounded-3xl shadow-sm border border-gray-100 overflow-hidden hover:shadow-2xl hover:shadow-blue-900/10 hover:border-[#1B4B6B]/20 hover:-translate-y-1 transition-all duration-300">
					<div class="p-6 border-b border-gray-50 bg-gray-50/30 flex justify-between items-center">
						<div class="flex items-center gap-3">
							<HardHat class="text-[#1B4B6B]" size={20} />
							<h3 class="font-black text-[#1B4B6B] uppercase text-xs tracking-widest">Monitoraggio DPI</h3>
						</div>
					</div>
					<div class="overflow-x-auto">
						<table class="w-full text-left">
							<tbody class="divide-y divide-gray-50">
							{#each registroDpi as dpi (dpi.id)}
								<tr class="hover:bg-white transition-colors">
									<td class="px-6 py-5 font-bold text-[#1B4B6B] text-xs uppercase">{dpi.tipo}</td>
									<td class="px-6 py-5 text-xs font-medium text-gray-500 uppercase">{dpi.dipendente}</td>
									<td class="px-6 py-5">
            <span class="text-[10px] font-black px-2 py-1 rounded {dpi.stato === 'SCADUTO' ? 'bg-red-50 text-red-600' : 'bg-green-50 text-green-600'}">
             {dpi.prossimaRevisione}
            </span>
									</td>
									<td class="px-6 py-5 text-right">
										<button class="p-2 text-gray-400 hover:text-[#1B4B6B] transition-colors"><RefreshCw size={16} /></button>
									</td>
								</tr>
							{/each}
							</tbody>
						</table>
					</div>
				</section>

				<section class="grid grid-cols-1 md:grid-cols-2 gap-8">
					<div class="bg-white p-8 rounded-3xl border border-gray-100 shadow-sm flex flex-col justify-between h-48 hover:shadow-2xl hover:shadow-blue-900/10 hover:border-[#1B4B6B]/20 hover:-translate-y-1 transition-all duration-300 group cursor-pointer">
						<div class="flex justify-between items-start">
							<div class="p-3 bg-blue-50 text-[#1B4B6B] rounded-xl group-hover:bg-[#1B4B6B] group-hover:text-white transition-colors"><FileText size={24} /></div>
							<button class="p-2 text-gray-300 hover:text-[#1B4B6B] transition-colors"><Download size={20} /></button>
						</div>
						<div>
							<h4 class="font-black text-[#1B4B6B] uppercase text-sm">Archivio Documentale</h4>
							<p class="text-[10px] font-bold text-gray-400 uppercase mt-1 italic">Scarica DVR e Verbali</p>
						</div>
					</div>

					<div class="bg-white p-8 rounded-3xl border-2 border-dashed border-gray-200 flex flex-col items-center justify-center text-center hover:shadow-2xl hover:shadow-blue-900/10 hover:border-[#1B4B6B]/40 hover:-translate-y-1 transition-all duration-300 group cursor-pointer">
						<div class="w-12 h-12 rounded-full bg-gray-50 flex items-center justify-center text-gray-400 group-hover:bg-[#1B4B6B] group-hover:text-white transition-all mb-4">
							<Users size={24} />
						</div>
						<h4 class="font-black text-[#1B4B6B] uppercase text-xs">Gestione Personale</h4>
						<p class="text-[9px] font-medium text-gray-400 uppercase mt-1">Stato formazione lavoratori</p>
					</div>
				</section>
			</div>

			<div class="space-y-8">
				<section class="bg-white p-8 rounded-3xl shadow-sm border border-gray-100 hover:shadow-2xl hover:shadow-blue-900/10 hover:border-[#1B4B6B]/20 hover:-translate-y-1 transition-all duration-300 group">
					<h3 class="font-black text-[#1B4B6B] uppercase text-xs tracking-widest mb-6 flex items-center gap-2">
						<Calendar size={18} /> Prossimi Corsi
					</h3>
					<div class="flex gap-4 p-3 rounded-2xl hover:bg-gray-50 transition-colors">
						<div class="flex flex-col items-center justify-center w-12 h-12 bg-gray-100 rounded-xl shrink-0 group-hover:bg-[#1B4B6B] group-hover:text-white transition-colors">
							<span class="text-[9px] font-bold uppercase">Apr</span>
							<span class="text-sm font-black">22</span>
						</div>
						<div><p class="text-xs font-black text-[#1B4B6B] uppercase leading-tight">Sicurezza</p></div>
					</div>
				</section>

				<section class="bg-white p-8 rounded-3xl shadow-sm border border-gray-100 hover:shadow-2xl hover:shadow-blue-900/10 hover:border-[#1B4B6B]/20 hover:-translate-y-1 transition-all duration-300 group">
					<h3 class="font-black text-[#1B4B6B] uppercase text-xs tracking-widest mb-6 flex items-center gap-2">
						<MessageSquare size={18} /> Chat
					</h3>
					<div class="p-4 bg-gray-50 rounded-2xl mb-4 border border-gray-100">
						<p class="text-[10px] font-bold text-[#1B4B6B] uppercase mb-1">NorLan Staff</p>
						<p class="text-xs font-medium text-gray-500 italic">"Nuovi aggiornamenti..."</p>
					</div>
					<button class="w-full py-3 bg-[#1B4B6B] text-white rounded-xl font-black uppercase text-[10px] hover:bg-[#153a54] transition-all">Apri</button>
				</section>
			</div>
		</div>
	</div>
{/if}

<style>
    :global(body) { background-color: #F9FAFB; }
</style>