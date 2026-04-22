<script lang="ts">
	import { onMount } from 'svelte';
	import { fade, scale } from 'svelte/transition';
	import {
		ShieldAlert, HardHat, Calendar, FileBadge, Download,
		MessageSquare, Star, Clock, AlertTriangle, CheckCircle2,
		X, Send, Loader2, ChevronRight
	} from 'lucide-svelte';

	// Importiamo il tuo modello ufficiale
	import { Dipendente } from '$lib/models/Dipendente';
	import { Ruolo } from '$lib/models/Enums';

	// Usiamo le INTERFACE (che non sono classi) qui dentro per comodità
	interface Impegno {
		id: number;
		tipo: string;
		titolo: string;
		data: string;
		icona: any;
		colore: string;
	}

	interface Dpi {
		id: number;
		nome: string;
		matricola: string;
		stato: 'OK' | 'WARNING' | 'DANGER';
		revisione: string;
	}

	interface Materiale {
		id: number;
		titolo: string;
		tipo: 'ATTESTATO' | 'DOCUMENTO';
		sbloccato: boolean;
	}

	// Stato della pagina con le Rune di Svelte 5
	let isLoading = $state(true);
	let isChatOpen = $state(false);
	let chatMessage = $state('');

	let utente = $state<Dipendente | null>(null);
	let statoFormazione = $state<'OK' | 'WARNING' | 'DANGER'>('WARNING');
	let statoDPI = $state<'OK' | 'WARNING' | 'DANGER'>('DANGER');

	let impegni = $state<Impegno[]>([]);
	let dotazioniDPI = $state<Dpi[]>([]);
	let materiali = $state<Materiale[]>([]);

	onMount(() => {
		setTimeout(() => {
			// Inizializziamo il tuo modello
			utente = new Dipendente({
				idUtente: 501,
				nome: 'Mario',
				cognome: 'Rossi',
				email: 'm.rossi@edilizia-spa.it',
				ruolo: Ruolo.DIPENDENTE,
				codiceFiscale: 'RSSMRA85M01H501Z',
				idAzienda: 10,
				ragioneSocialeAzienda: 'EDILIZIA SPA'
			});

			impegni = [
				{ id: 1, tipo: 'CORSO', titolo: 'AGGIORNAMENTO ANTINCENDIO', data: 'Domani, 09:00', icona: Calendar, colore: 'text-blue-500' },
				{ id: 2, tipo: 'FEEDBACK', titolo: 'VALUTA CORSO SICUREZZA', data: 'Scade in 2 giorni', icona: Star, colore: 'text-amber-500' }
			];

			dotazioniDPI = [
				{ id: 1, nome: 'ELMETTO PROTETTIVO', matricola: 'DPI-1042', stato: 'OK', revisione: '10/2027' },
				{ id: 2, nome: 'IMBRACATURA', matricola: 'DPI-0988', stato: 'DANGER', revisione: 'Scaduta' }
			];

			materiali = [
				{ id: 1, titolo: 'ATTESTATO SICUREZZA BASE', tipo: 'ATTESTATO', sbloccato: true },
				{ id: 2, titolo: 'ATTESTATO LAVORI IN QUOTA', tipo: 'ATTESTATO', sbloccato: false }
			];

			isLoading = false;
		}, 700);
	});

	function getColorByStatus(stato: string) {
		switch(stato) {
			case 'OK': return 'bg-emerald-500 shadow-emerald-500/30 text-white border-emerald-600';
			case 'WARNING': return 'bg-amber-400 shadow-amber-500/30 text-[#1B4B6B] border-amber-500';
			case 'DANGER': return 'bg-red-500 shadow-red-500/30 text-white border-red-600';
			default: return 'bg-gray-200 text-gray-500';
		}
	}
</script>

<div in:fade class="max-w-[1600px] mx-auto space-y-8 pb-20">
	<div class="flex items-end justify-between">
		<div>
			<h1 class="text-4xl font-black text-[#1B4B6B] uppercase tracking-tighter">Ciao, {utente?.nome || 'Dipendente'}</h1>
			<p class="text-gray-400 font-bold uppercase text-[10px] tracking-widest mt-1">
				Azienda: <span class="text-[#1B4B6B]">{utente?.ragioneSocialeAzienda || '...'}</span>
			</p>
		</div>
	</div>

	{#if isLoading}
		<div class="py-32 flex flex-col items-center justify-center gap-4">
			<Loader2 size={48} class="animate-spin text-[#1B4B6B]" />
			<span class="text-[10px] font-black text-gray-400 uppercase tracking-widest">Sincronizzazione...</span>
		</div>
	{:else}
		<div class="grid grid-cols-1 md:grid-cols-2 gap-6">
			<div class="bg-white rounded-[2.5rem] p-8 border border-gray-100 shadow-sm flex items-center justify-between group hover:-translate-y-1 transition-all">
				<div class="flex items-center gap-6">
					<div class="w-20 h-20 rounded-full border-4 flex items-center justify-center shadow-xl {getColorByStatus(statoFormazione)}">
						{#if statoFormazione === 'OK'} <CheckCircle2 size={32} />
						{:else if statoFormazione === 'WARNING'} <AlertTriangle size={32} />
						{:else} <ShieldAlert size={32} /> {/if}
					</div>
					<div>
						<p class="text-[10px] font-black text-gray-400 uppercase tracking-widest mb-1">Stato Formazione</p>
						<h2 class="text-2xl font-black text-[#1B4B6B] uppercase leading-none">In Scadenza</h2>
					</div>
				</div>
				<ChevronRight size={24} class="text-gray-200 group-hover:text-[#1B4B6B]" />
			</div>

			<div class="bg-white rounded-[2.5rem] p-8 border border-gray-100 shadow-sm flex items-center justify-between group hover:-translate-y-1 transition-all">
				<div class="flex items-center gap-6">
					<div class="w-20 h-20 rounded-full border-4 flex items-center justify-center shadow-xl {getColorByStatus(statoDPI)}">
						{#if statoDPI === 'OK'} <CheckCircle2 size={32} />
						{:else if statoDPI === 'WARNING'} <AlertTriangle size={32} />
						{:else} <HardHat size={32} /> {/if}
					</div>
					<div>
						<p class="text-[10px] font-black text-gray-400 uppercase tracking-widest mb-1">Registro DPI</p>
						<h2 class="text-2xl font-black text-[#1B4B6B] uppercase leading-none">Azione Richiesta</h2>
					</div>
				</div>
				<ChevronRight size={24} class="text-gray-200 group-hover:text-[#1B4B6B]" />
			</div>
		</div>

		<div class="grid grid-cols-1 xl:grid-cols-3 gap-8">
			<div class="xl:col-span-2 space-y-8">
				<div class="bg-white rounded-[2.5rem] border border-gray-100 shadow-sm p-8">
					<h3 class="text-2xl font-black text-[#1B4B6B] uppercase tracking-tighter mb-8">Prossimi Impegni</h3>
					<div class="space-y-4">
						{#each impegni as impegno (impegno.id)}
							<div class="flex items-center gap-5 p-5 rounded-2xl border border-gray-50 hover:border-gray-200 transition-all group bg-gray-50/30 hover:bg-white">
								<div class="w-14 h-14 bg-white rounded-[1.2rem] shadow-sm flex items-center justify-center {impegno.colore}">
									<impegno.icona size={24} />
								</div>
								<div class="flex-1">
									<p class="text-[9px] font-black uppercase tracking-widest {impegno.colore} mb-1">{impegno.tipo}</p>
									<h4 class="text-sm font-black text-[#1B4B6B] uppercase leading-tight">{impegno.titolo}</h4>
								</div>
								<div class="text-right text-[10px] font-bold text-gray-400 uppercase tracking-widest">
									<Clock size={12} class="inline mr-1" /> {impegno.data}
								</div>
							</div>
						{/each}
					</div>
				</div>

				<div class="bg-white rounded-[2.5rem] border border-gray-100 shadow-sm p-8">
					<h3 class="text-2xl font-black text-[#1B4B6B] uppercase tracking-tighter mb-8">Dotazioni DPI</h3>
					<div class="overflow-x-auto">
						<table class="w-full text-left">
							<thead>
							<tr class="border-b border-gray-100">
								<th class="pb-4 text-[10px] font-black text-gray-400 uppercase tracking-widest">Dispositivo</th>
								<th class="pb-4 text-[10px] font-black text-gray-400 uppercase tracking-widest">Matricola</th>
								<th class="pb-4 text-[10px] font-black text-gray-400 uppercase tracking-widest text-right">Stato</th>
							</tr>
							</thead>
							<tbody>
							{#each dotazioniDPI as dpi}
								<tr class="border-b border-gray-50 group hover:bg-gray-50/50 transition-colors">
									<td class="py-5 text-xs font-black text-[#1B4B6B] uppercase">{dpi.nome}</td>
									<td class="py-5 text-[10px] font-bold text-gray-400 uppercase">{dpi.matricola}</td>
									<td class="py-5 text-right">
											<span class="inline-flex px-3 py-1.5 rounded-lg text-[9px] font-black uppercase tracking-widest
												{dpi.stato === 'OK' ? 'bg-emerald-50 text-emerald-600' :
												 dpi.stato === 'WARNING' ? 'bg-amber-50 text-amber-600' :
												 'bg-red-50 text-red-600'}">
												{dpi.stato}
											</span>
									</td>
								</tr>
							{/each}
							</tbody>
						</table>
					</div>
				</div>
			</div>

			<div class="space-y-8">
				<div class="bg-gray-50 rounded-[2.5rem] border border-gray-100 p-8">
					<h3 class="text-xl font-black text-[#1B4B6B] uppercase tracking-tighter mb-6">Area Download</h3>
					<div class="space-y-3">
						{#each materiali as mat}
							<div class="bg-white p-4 rounded-2xl border border-gray-100 flex items-center justify-between transition-all {mat.sbloccato ? 'hover:shadow-md cursor-pointer' : 'opacity-40 grayscale cursor-not-allowed'}">
								<div class="flex items-center gap-4">
									<div class="p-2.5 rounded-xl {mat.tipo === 'ATTESTATO' ? 'bg-amber-50 text-amber-600' : 'bg-blue-50 text-blue-600'}">
										{#if mat.tipo === 'ATTESTATO'} <FileBadge size={18} /> {:else} <Download size={18} /> {/if}
									</div>
									<p class="text-[10px] font-black text-[#1B4B6B] uppercase">{mat.titolo}</p>
								</div>
								{#if mat.sbloccato} <Download size={16} class="text-gray-300" /> {/if}
							</div>
						{/each}
					</div>
				</div>
			</div>
		</div>
	{/if}
</div>

<div class="fixed bottom-8 right-8 z-50 flex flex-col items-end">
	{#if isChatOpen}
		<div transition:scale={{duration: 200, start: 0.9}} class="bg-white w-80 h-96 rounded-[2rem] shadow-2xl border border-gray-100 flex flex-col overflow-hidden mb-4">
			<div class="bg-[#1B4B6B] p-5 text-white flex justify-between items-center">
				<span class="text-xs font-black uppercase tracking-widest">Supporto NorLan</span>
				<button onclick={() => isChatOpen = false}><X size={16} /></button>
			</div>
			<div class="flex-1 bg-gray-50 p-4"></div>
			<form class="p-3 bg-white border-t flex gap-2" onsubmit={(e) => {e.preventDefault(); chatMessage = '';}}>
				<input bind:value={chatMessage} type="text" placeholder="Scrivi..." class="flex-1 bg-gray-50 border-none rounded-xl px-4 py-2 text-xs font-bold outline-none" />
				<button type="submit" class="w-10 h-10 bg-[#1B4B6B] text-white rounded-xl flex items-center justify-center">
					<Send size={14} />
				</button>
			</form>
		</div>
	{/if}

	<button
		onclick={() => isChatOpen = !isChatOpen}
		class="w-16 h-16 bg-[#1B4B6B] text-white rounded-full shadow-2xl flex items-center justify-center hover:scale-110 transition-transform"
	>
		<MessageSquare size={24} />
	</button>
</div>

<style>
    :global(body) { background-color: #F9FAFB; }
</style>