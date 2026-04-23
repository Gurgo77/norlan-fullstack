<script lang="ts">
	import { onMount } from 'svelte';
	import { fade } from 'svelte/transition';
	import {
		ShieldCheck, AlertTriangle, FileText, HardHat,
		Calendar, MessageSquare, Download, RefreshCw,
		Users, BellRing, Clock, Loader2, Plus
	} from 'lucide-svelte';

	// Servizi
	import { AuthService } from '$lib/services/AuthService';
	import { DocumentoService } from '$lib/services/DocumentoService';
	import { LavoratoreService, type DipendenteDTO, type AssegnazioneDPIDTO } from '$lib/services/LavoratoreService';
	import { FormazioneService } from '$lib/services/FormazioneService';

	// Modelli
	import type { Documento } from '$lib/models/Documento';
	import type { CorsoFormazione } from '$lib/models/CorsoFormazione';

	// --- STATO REATTIVO ---
	let isLoading = $state(true);
	let idAzienda = $state<number | null>(null);

	// Dati reali dal backend
	let documentiScadenza = $state<Documento[]>([]);
	let dipendenti = $state<DipendenteDTO[]>([]);
	let prossimiCorsi = $state<CorsoFormazione[]>([]);
	let dpiInScadenza = $state<AssegnazioneDPIDTO[]>([]);

	// Stato aggregato per la UI
	const alertScadenze = $derived(
			documentiScadenza.filter(d => {
				const giorni = Math.ceil((new Date(d.dataScadenza).getTime() - new Date().getTime()) / (1000 * 3600 * 24));
				return giorni <= 30;
			})
	);

	const infoStato = $derived(() => {
		const haScaduti = documentiScadenza.some(d => d.scaduto);
		const haDpiScaduti = dpiInScadenza.length > 0;

		if (haScaduti || haDpiScaduti) {
			return { label: 'CRITICO', color: 'bg-red-500', icon: AlertTriangle };
		}
		if (alertScadenze.length > 0) {
			return { label: 'ATTENZIONE', color: 'bg-yellow-500', icon: Clock };
		}
		return { label: 'A NORMA', color: 'bg-green-500', icon: ShieldCheck };
	});

	const status = $derived(infoStato());

	// --- CARICAMENTO DATI ---
	onMount(async () => {
		const session = AuthService.getSession(); //
		if (!session) return;
		idAzienda = session.idUtente;

		try {
			// Caricamento parallelo di tutte le sezioni della dashboard
			const [docs, lavoratori, corsi, dpi] = await Promise.all([
				DocumentoService.getDocumentiByAzienda(idAzienda), //
				LavoratoreService.getByAzienda(idAzienda), //
				FormazioneService.getAllCorsi(), //
				LavoratoreService.getDpiInScadenza(30) //
			]);

			documentiScadenza = docs;
			dipendenti = lavoratori;
			prossimiCorsi = corsi.filter(c => c.stato === 'PROGRAMMATO').slice(0, 3);
			dpiInScadenza = dpi;

		} catch (error) {
			console.error("Errore nel caricamento dei dati dashboard:", error);
		} finally {
			isLoading = false;
		}
	});

	function formattaData(dateStr: string) {
		return new Date(dateStr).toLocaleDateString('it-IT', { day: '2-digit', month: '2-digit', year: 'numeric' });
	}
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

			<a
					href="/dashboard/azienda/documenti"
					class="bg-white text-[#1B4B6B] border-2 border-[#1B4B6B] px-8 py-3.5 rounded-xl font-extrabold uppercase text-xs shadow-sm hover:bg-[#1B4B6B] hover:text-white transition-all flex items-center gap-3"
			>
				<Plus size={18} />
				Nuova Richiesta
			</a>
		</div>

		<div class="grid grid-cols-1 lg:grid-cols-3 gap-8 mb-10">
			<div class="lg:col-span-2 bg-white p-8 rounded-3xl shadow-sm border border-gray-100 flex items-center gap-8 hover:shadow-2xl hover:shadow-blue-900/10 transition-all duration-300 group cursor-default">
				<div class="relative">
					<div class="w-24 h-24 rounded-full flex items-center justify-center {status.color} text-white shadow-lg">
						<status.icon size={40} />
					</div>
				</div>
				<div>
					<p class="text-[10px] font-black text-gray-400 uppercase tracking-widest mb-1">Status Compliance</p>
					<h2 class="text-3xl font-black text-[#1B4B6B] uppercase">{status.label}</h2>
					<p class="text-sm font-medium text-gray-500 mt-1 italic">Situazione aggiornata NorLan</p>
				</div>
			</div>

			<div class="bg-[#1B4B6B] p-8 rounded-3xl shadow-xl text-white relative overflow-hidden hover:shadow-2xl hover:shadow-blue-900/40 transition-all duration-300 group">
				<BellRing class="absolute -right-4 -bottom-4 text-white/10 group-hover:scale-110 transition-transform duration-500" size={120} />
				<p class="text-[10px] font-black text-white/50 uppercase tracking-widest mb-4">Alert Scadenze</p>
				<div class="space-y-3 relative z-10">
					{#each alertScadenze as alert (alert.idDocumento)}
						<div class="flex items-center gap-3 bg-white/10 p-3 rounded-xl border border-white/10">
							<div class="w-2 h-2 rounded-full {alert.scaduto ? 'bg-red-400' : 'bg-yellow-400'}"></div>
							<span class="text-xs font-bold uppercase truncate">{alert.tipologia}</span>
						</div>
					{/each}
					{#if alertScadenze.length === 0}
						<p class="text-xs font-bold text-white/40 uppercase italic">Nessun alert attivo</p>
					{/if}
				</div>
			</div>
		</div>

		<div class="grid grid-cols-1 xl:grid-cols-3 gap-8">
			<div class="xl:col-span-2 space-y-8">
				<section class="bg-white rounded-3xl shadow-sm border border-gray-100 overflow-hidden hover:shadow-2xl hover:shadow-blue-900/10 transition-all duration-300">
					<div class="p-6 border-b border-gray-50 bg-gray-50/30 flex justify-between items-center">
						<div class="flex items-center gap-3">
							<HardHat class="text-[#1B4B6B]" size={20} />
							<h3 class="font-black text-[#1B4B6B] uppercase text-xs tracking-widest">Monitoraggio DPI</h3>
						</div>
					</div>
					<div class="overflow-x-auto">
						<table class="w-full text-left">
							<tbody class="divide-y divide-gray-50">
							{#each dpiInScadenza as dpi (dpi.id)}
								<tr class="hover:bg-white transition-colors">
									<td class="px-6 py-5 font-bold text-[#1B4B6B] text-xs uppercase">{dpi.nomeDpi}</td>
									<td class="px-6 py-5 text-[10px] font-black text-gray-400 uppercase">Scadenza Revisione</td>
									<td class="px-6 py-5">
                                <span class="text-[10px] font-black px-2 py-1 rounded {new Date(dpi.dataScadenza) < new Date() ? 'bg-red-50 text-red-600' : 'bg-green-50 text-green-600'}">
                                    {formattaData(dpi.dataScadenza)}
                                </span>
									</td>
									<td class="px-6 py-5 text-right">
										<button class="p-2 text-gray-400 hover:text-[#1B4B6B] transition-colors"><RefreshCw size={16} /></button>
									</td>
								</tr>
							{/each}
							{#if dpiInScadenza.length === 0}
								<tr><td colspan="4" class="p-10 text-center text-xs font-bold text-gray-300 uppercase italic">Tutti i DPI risultano a norma</td></tr>
							{/if}
							</tbody>
						</table>
					</div>
				</section>

				<section class="grid grid-cols-1 md:grid-cols-2 gap-8">
					<a href="/dashboard/azienda/documenti" class="bg-white p-8 rounded-3xl border border-gray-100 shadow-sm flex flex-col justify-between h-48 hover:shadow-2xl hover:shadow-blue-900/10 transition-all duration-300 group">
						<div class="flex justify-between items-start">
							<div class="p-3 bg-blue-50 text-[#1B4B6B] rounded-xl group-hover:bg-[#1B4B6B] group-hover:text-white transition-colors"><FileText size={24} /></div>
							<button class="p-2 text-gray-300 hover:text-[#1B4B6B] transition-colors"><Download size={20} /></button>
						</div>
						<div>
							<h4 class="font-black text-[#1B4B6B] uppercase text-sm">Archivio Documentale</h4>
							<p class="text-[10px] font-bold text-gray-400 uppercase mt-1 italic">Scarica DVR e Verbali</p>
						</div>
					</a>

					<a href="/dashboard/azienda/dipendenti" class="bg-white p-8 rounded-3xl border-2 border-dashed border-gray-200 flex flex-col items-center justify-center text-center hover:shadow-2xl hover:shadow-blue-900/10 hover:border-[#1B4B6B]/40 transition-all duration-300 group">
						<div class="w-12 h-12 rounded-full bg-gray-50 flex items-center justify-center text-gray-400 group-hover:bg-[#1B4B6B] group-hover:text-white transition-all mb-4">
							<Users size={24} />
						</div>
						<h4 class="font-black text-[#1B4B6B] uppercase text-xs">Gestione Personale</h4>
						<p class="text-[9px] font-medium text-gray-400 uppercase mt-1">Lavoratori censiti: {dipendenti.length}</p>
					</a>
				</section>
			</div>

			<div class="space-y-8">
				<section class="bg-white p-8 rounded-3xl shadow-sm border border-gray-100 hover:shadow-2xl hover:shadow-blue-900/10 transition-all duration-300 group">
					<h3 class="font-black text-[#1B4B6B] uppercase text-xs tracking-widest mb-6 flex items-center gap-2">
						<Calendar size={18} /> Prossimi Corsi
					</h3>
					{#each prossimiCorsi as corso (corso.idCorso)}
						<div class="flex gap-4 p-3 rounded-2xl hover:bg-gray-50 transition-colors mb-2">
							<div class="flex flex-col items-center justify-center w-12 h-12 bg-gray-100 rounded-xl shrink-0 group-hover:bg-[#1B4B6B] group-hover:text-white transition-colors">
								<span class="text-[9px] font-bold uppercase">{new Date(corso.dataOrario).toLocaleString('it-IT', {month: 'short'})}</span>
								<span class="text-sm font-black">{new Date(corso.dataOrario).getDate()}</span>
							</div>
							<div class="overflow-hidden">
								<p class="text-xs font-black text-[#1B4B6B] uppercase leading-tight truncate">{corso.titolo}</p>
								<p class="text-[9px] font-bold text-gray-400 uppercase mt-1">{corso.luogoFisico}</p>
							</div>
						</div>
					{/each}
					{#if prossimiCorsi.length === 0}
						<p class="text-xs font-bold text-gray-300 uppercase italic">Nessun corso in programma</p>
					{/if}
				</section>

				<section class="bg-white p-8 rounded-3xl shadow-sm border border-gray-100 hover:shadow-2xl hover:shadow-blue-900/10 transition-all duration-300 group">
					<h3 class="font-black text-[#1B4B6B] uppercase text-xs tracking-widest mb-6 flex items-center gap-2">
						<MessageSquare size={18} /> Chat Diretta
					</h3>
					<div class="p-4 bg-gray-50 rounded-2xl mb-4 border border-gray-100">
						<p class="text-[10px] font-bold text-[#1B4B6B] uppercase mb-1">NorLan Staff</p>
						<p class="text-xs font-medium text-gray-500 italic">Hai bisogno di assistenza con le pratiche?</p>
					</div>
					<a href="/dashboard/azienda/comunicazioni" class="w-full py-3 bg-[#1B4B6B] text-white rounded-xl font-black uppercase text-center block text-[10px] hover:bg-[#153a54] transition-all">Apri Chat</a>
				</section>
			</div>
		</div>
	</div>
{/if}

<style>
	:global(body) { background-color: #F9FAFB; }
</style>