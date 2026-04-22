<script lang="ts">
	import { onMount } from 'svelte';
	import { fade, scale } from 'svelte/transition';
	import {
		ChevronLeft, ChevronRight, Calendar as CalendarIcon,
		Clock, MapPin, AlertTriangle, GraduationCap, Loader2, Plus
	} from 'lucide-svelte';
	import { EventoCalendario, type EventoCalendarioData } from '$lib/models/EventoCalendario';

	const GIORNI = ['LUN', 'MAR', 'MER', 'GIO', 'VEN', 'SAB', 'DOM'];
	const MESI = ['GENNAIO', 'FEBBRAIO', 'MARZO', 'APRILE', 'MAGGIO', 'GIUGNO', 'LUGLIO', 'AGOSTO', 'SETTEMBRE', 'OTTOBRE', 'NOVEMBRE', 'DICEMBRE'];

	let isLoading = $state(true);
	let dataCorrente = $state(new Date());
	let eventi = $state<EventoCalendario[]>([]);
	let giornoSelezionato = $state<number>(new Date().getDate());

	onMount(() => {
		setTimeout(() => {
			const mockData: EventoCalendarioData[] = [
				{ idEvento: '1', titolo: 'CORSO ANTINCENDIO PRATICO', dataIso: '2026-04-15', orarioInizio: '09:00', orarioFine: '13:00', tipo: 'CORSO', luogo: 'CAMPO PROVE' },
				{ idEvento: '2', titolo: 'RICEVIMENTO STUDENTI', dataIso: '2026-04-15', orarioInizio: '15:30', orarioFine: '17:00', tipo: 'MEETING', luogo: 'AULA VIRTUALE' },
				{ idEvento: '3', titolo: 'SCADENZA DVR AZIENDA EDE', dataIso: '2026-04-20', tipo: 'SCADENZA' },
				{ idEvento: '4', titolo: 'FORMAZIONE GENERALE SICUREZZA', dataIso: '2026-04-25', orarioInizio: '14:30', orarioFine: '18:30', tipo: 'CORSO', luogo: 'SEDE NORLAN' }
			];
			eventi = mockData.map(d => new EventoCalendario(d));
			isLoading = false;
		}, 600);
	});

	const mese = $derived(dataCorrente.getMonth());
	const anno = $derived(dataCorrente.getFullYear());

	const giorniGriglia = $derived.by(() => {
		const primoGiornoMese = new Date(anno, mese, 1).getDay();
		const offset = primoGiornoMese === 0 ? 6 : primoGiornoMese - 1;
		const totaleGiorni = new Date(anno, mese + 1, 0).getDate();

		const celle = [];
		for (let i = 0; i < offset; i++) celle.push({ giorno: null, weekend: false });
		for (let i = 1; i <= totaleGiorni; i++) {
			const d = new Date(anno, mese, i).getDay();
			celle.push({ giorno: i, weekend: d === 0 || d === 6 });
		}
		while (celle.length % 7 !== 0) celle.push({ giorno: null, weekend: false });
		return celle;
	});

	const eventiDelGiorno = $derived(
		eventi.filter(e => {
			const d = new Date(e.dataIso);
			return d.getDate() === giornoSelezionato && d.getMonth() === mese && d.getFullYear() === anno;
		}).sort((a, b) => (a.orarioInizio || '').localeCompare(b.orarioInizio || ''))
	);

	function getEventiGiorno(g: number) {
		return eventi.filter(e => {
			const d = new Date(e.dataIso);
			return d.getDate() === g && d.getMonth() === mese && d.getFullYear() === anno;
		});
	}

	function cambiaMese(delta: number) {
		dataCorrente = new Date(anno, mese + delta, 1);
		giornoSelezionato = 1;
	}
</script>

<div in:fade class="max-w-[1600px] mx-auto space-y-6">
	<div class="flex justify-between items-end">
		<div>
			<h1 class="text-4xl font-black text-[#1B4B6B] uppercase tracking-tighter">{MESI[mese]} {anno}</h1>
			<p class="text-gray-400 font-bold uppercase text-[10px] tracking-widest mt-1">Gestione pianificazione didattica</p>
		</div>
		<div class="flex items-center gap-3">
			<button onclick={() => cambiaMese(-1)} class="p-3 rounded-xl bg-white border border-gray-100 text-[#1B4B6B] hover:bg-gray-50 transition-all"><ChevronLeft size={20} /></button>
			<button onclick={() => { dataCorrente = new Date(); giornoSelezionato = new Date().getDate(); }} class="px-6 py-3 rounded-xl bg-white border border-gray-100 text-[10px] font-black uppercase text-[#1B4B6B] hover:bg-gray-50 transition-all">Oggi</button>
			<button onclick={() => cambiaMese(1)} class="p-3 rounded-xl bg-white border border-gray-100 text-[#1B4B6B] hover:bg-gray-50 transition-all"><ChevronRight size={20} /></button>
		</div>
	</div>

	<div class="grid grid-cols-1 lg:grid-cols-4 gap-6 h-[750px]">
		<div class="lg:col-span-3 bg-white rounded-[2.5rem] border border-gray-100 shadow-sm p-8 flex flex-col">
			<div class="grid grid-cols-7 mb-6">
				{#each GIORNI as g (g)}
					<div class="text-center text-[10px] font-black text-gray-300 uppercase tracking-[0.2em]">{g}</div>
				{/each}
			</div>

			<div class="grid grid-cols-7 grid-rows-5 gap-3 flex-1">
				{#each giorniGriglia as cella, i (i)}
					{#if cella.giorno === null}
						<div class="rounded-2xl bg-gray-50/50 border border-gray-50 border-dashed"></div>
					{:else}
						{@const evs = getEventiGiorno(cella.giorno)}
						{@const isOggi = cella.giorno === new Date().getDate() && mese === new Date().getMonth() && anno === new Date().getFullYear()}
						<button
							onclick={() => giornoSelezionato = cella.giorno!}
							class="rounded-3xl border-2 p-4 text-left transition-all relative group flex flex-col justify-between
								{cella.weekend ? 'bg-gray-50/80' : 'bg-white'}
								{giornoSelezionato === cella.giorno ? 'border-[#1B4B6B] shadow-xl shadow-[#1B4B6B]/10 z-10' : 'border-gray-50 hover:border-gray-200'}">

							<div class="flex justify-between items-start">
								<span class="text-sm font-black {isOggi ? 'bg-[#1B4B6B] text-white w-7 h-7 flex items-center justify-center rounded-full shadow-lg' : 'text-gray-400'}">
									{cella.giorno}
								</span>
							</div>

							<div class="space-y-1">
								{#each evs.slice(0, 2) as ev}
									<div class="flex items-center gap-2">
										<div class="w-2 h-2 rounded-full shrink-0 {ev.tipo === 'CORSO' ? 'bg-blue-500' : ev.tipo === 'SCADENZA' ? 'bg-red-500' : 'bg-amber-500'}"></div>
										<p class="text-[9px] font-black text-[#1B4B6B] uppercase truncate">{ev.titolo}</p>
									</div>
								{/each}
								{#if evs.length > 2}
									<p class="text-[8px] font-black text-gray-300 uppercase tracking-widest mt-1">+ {evs.length - 2} ALTRI</p>
								{/if}
							</div>
						</button>
					{/if}
				{/each}
			</div>
		</div>

		<div class="bg-[#1B4B6B] rounded-[2.5rem] p-8 text-white flex flex-col shadow-2xl shadow-[#1B4B6B]/30 relative overflow-hidden">
			<div class="absolute -right-10 -top-10 w-40 h-40 bg-white/5 rounded-full blur-3xl"></div>

			<div class="relative z-10">
				<h3 class="text-5xl font-black tracking-tighter leading-none">{giornoSelezionato}</h3>
				<p class="text-xs font-black uppercase tracking-[0.3em] opacity-50 mt-2">{MESI[mese]} {anno}</p>
			</div>

			<div class="mt-12 space-y-6 flex-1 overflow-y-auto pr-2 custom-scrollbar relative z-10">
				{#if eventiDelGiorno.length === 0}
					<div class="py-20 text-center opacity-30">
						<CalendarIcon class="mx-auto mb-4" size={40} strokeWidth={1} />
						<p class="text-[10px] font-black uppercase tracking-widest leading-loose">Nessuna attività<br/>programmata</p>
					</div>
				{:else}
					{#each eventiDelGiorno as ev (ev.idEvento)}
						<div in:scale={{duration: 300, start: 0.95}} class="bg-white/10 backdrop-blur-md rounded-3xl p-6 border border-white/10 hover:bg-white/15 transition-all group">
							<div class="flex justify-between items-start mb-4">
								<div class="p-2 rounded-xl bg-white/10 text-white"><Clock size={16} /></div>
								<span class="text-[9px] font-black bg-white text-[#1B4B6B] px-3 py-1 rounded-full uppercase tracking-widest">{ev.tipo}</span>
							</div>
							<h4 class="font-black text-lg uppercase leading-tight mb-4">{ev.titolo}</h4>
							<div class="space-y-2 opacity-60">
								{#if ev.orarioInizio}
									<p class="text-[10px] font-black uppercase tracking-widest flex items-center gap-2"><Clock size={12} /> {ev.orarioInizio} - {ev.orarioFine}</p>
								{/if}
								{#if ev.luogo}
									<p class="text-[10px] font-black uppercase tracking-widest flex items-center gap-2"><MapPin size={12} /> {ev.luogo}</p>
								{/if}
							</div>
						</div>
					{/each}
				{/if}
			</div>

			<button class="mt-8 w-full bg-white text-[#1B4B6B] py-5 rounded-[2rem] text-xs font-black uppercase tracking-widest flex items-center justify-center gap-3 hover:scale-[1.02] transition-transform shadow-xl">
				<Plus size={18} /> Aggiungi Nota
			</button>
		</div>
	</div>
</div>

<style>
    .custom-scrollbar::-webkit-scrollbar { width: 4px; }
    .custom-scrollbar::-webkit-scrollbar-thumb { background: rgba(255,255,255,0.2); border-radius: 10px; }
</style>