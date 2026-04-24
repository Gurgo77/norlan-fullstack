<script lang="ts">
	import { onMount } from 'svelte';
	import { fade, scale } from 'svelte/transition';
	import {
		ChevronLeft,
		ChevronRight,
		Calendar as CalendarIcon,
		Clock,
		MapPin,
		Loader2,
		Plus
	} from 'lucide-svelte';

	// IMPORT SERVIZI E MODELLI
	import { EventoCalendario } from '$lib/models/EventoCalendario';
	import { AuthService } from '$lib/services/AuthService';
	import { FormazioneService } from '$lib/services/FormazioneService';

	const GIORNI = ['LUN', 'MAR', 'MER', 'GIO', 'VEN', 'SAB', 'DOM'];
	const MESI = [
		'GENNAIO',
		'FEBBRAIO',
		'MARZO',
		'APRILE',
		'MAGGIO',
		'GIUGNO',
		'LUGLIO',
		'AGOSTO',
		'SETTEMBRE',
		'OTTOBRE',
		'NOVEMBRE',
		'DICEMBRE'
	];

	let isLoading = $state(true);
	let dataCorrente = $state(new Date());
	let eventi = $state<EventoCalendario[]>([]);
	let giornoSelezionato = $state<number>(new Date().getDate());

	onMount(async () => {
		const session = AuthService.getSession(); //
		if (!session) return;

		try {
			// 1. Recupera i corsi globali
			const corsi = await FormazioneService.getAllCorsi(); //

			// 2. Filtra i corsi assegnati al docente loggato
			const mieiCorsi = corsi.filter((c) => c.idDocente === session.idUtente);

			// 3. Mappa i corsi nel formato "EventoCalendario"
			eventi = mieiCorsi.map((c) => {
				const dataInizio = new Date(c.dataOrario);
				// Ipotizziamo una durata base di 4 ore per i corsi
				const dataFine = new Date(dataInizio.getTime() + 4 * 60 * 60 * 1000);

				return new EventoCalendario({
					idEvento: `corso-${c.idCorso}`,
					titolo: c.titolo,
					dataIso: c.dataOrario, // Salviamo la data ISO completa fornita dal BE
					orarioInizio: dataInizio.toLocaleTimeString('it-IT', {
						hour: '2-digit',
						minute: '2-digit'
					}),
					orarioFine: dataFine.toLocaleTimeString('it-IT', { hour: '2-digit', minute: '2-digit' }),
					tipo: 'CORSO',
					luogo: c.luogoFisico
				});
			});
		} catch (error) {
			console.error('Errore nel caricamento del calendario docente:', error);
		} finally {
			isLoading = false;
		}
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
			eventi
					.filter((e) => {
						const d = new Date(e.dataIso);
						return (
								d.getDate() === giornoSelezionato &&
								d.getMonth() === mese &&
								d.getFullYear() === anno
						);
					})
					.sort((a, b) => (a.orarioInizio || '').localeCompare(b.orarioInizio || ''))
	);

	function getEventiGiorno(g: number) {
		return eventi.filter((e) => {
			const d = new Date(e.dataIso);
			return d.getDate() === g && d.getMonth() === mese && d.getFullYear() === anno;
		});
	}

	function cambiaMese(delta: number) {
		dataCorrente = new Date(anno, mese + delta, 1);
		giornoSelezionato = 1;
	}

	// FIX: Helper function per bypassare il problema del "!" (non-null assertion) nel template HTML
	function impostaGiorno(giorno: number | null) {
		if (giorno !== null) {
			giornoSelezionato = giorno;
		}
	}
</script>

<div in:fade class="mx-auto max-w-[1600px] space-y-6">
	<div class="flex items-end justify-between">
		<div>
			<h1 class="text-4xl font-black uppercase tracking-tighter text-[#1B4B6B]">
				{MESI[mese]}
				{anno}
			</h1>
			<p class="mt-1 text-[10px] font-bold uppercase tracking-widest text-gray-400">
				Gestione pianificazione didattica
			</p>
		</div>
		<div class="flex items-center gap-3">
			<button
					onclick={() => cambiaMese(-1)}
					class="rounded-xl border border-gray-100 bg-white p-3 text-[#1B4B6B] transition-all hover:bg-gray-50"
			><ChevronLeft size={20} /></button
			>
			<button
					onclick={() => {
					dataCorrente = new Date();
					giornoSelezionato = new Date().getDate();
				}}
					class="rounded-xl border border-gray-100 bg-white px-6 py-3 text-[10px] font-black uppercase text-[#1B4B6B] transition-all hover:bg-gray-50"
			>Oggi</button
			>
			<button
					onclick={() => cambiaMese(1)}
					class="rounded-xl border border-gray-100 bg-white p-3 text-[#1B4B6B] transition-all hover:bg-gray-50"
			><ChevronRight size={20} /></button
			>
		</div>
	</div>

	{#if isLoading}
		<div class="flex flex-col items-center justify-center gap-4 py-32">
			<Loader2 size={48} class="animate-spin text-[#1B4B6B]" />
			<span class="text-[10px] font-black uppercase tracking-widest text-gray-400">
				Sincronizzazione calendario didattico...
			</span>
		</div>
	{:else}
		<div class="grid h-[750px] grid-cols-1 gap-6 lg:grid-cols-4">
			<div
					class="flex flex-col rounded-[2.5rem] border border-gray-100 bg-white p-8 shadow-sm lg:col-span-3"
			>
				<div class="mb-6 grid grid-cols-7">
					{#each GIORNI as g (g)}
						<div class="text-center text-[10px] font-black uppercase tracking-[0.2em] text-gray-300">
							{g}
						</div>
					{/each}
				</div>

				<div class="grid flex-1 grid-cols-7 grid-rows-5 gap-3">
					{#each giorniGriglia as cella, i (i)}
						{#if cella.giorno === null}
							<div class="rounded-2xl border border-dashed border-gray-50 bg-gray-50/50"></div>
						{:else}
							{@const evs = getEventiGiorno(cella.giorno)}
							{@const isOggi =
							cella.giorno === new Date().getDate() &&
							mese === new Date().getMonth() &&
							anno === new Date().getFullYear()}
							<button
									onclick={() => impostaGiorno(cella.giorno)}
									class="group relative flex flex-col justify-between rounded-3xl border-2 p-4 text-left transition-all {cella.weekend
									? 'bg-gray-50/80'
									: 'bg-white'} {giornoSelezionato === cella.giorno
									? 'z-10 border-[#1B4B6B] shadow-xl shadow-[#1B4B6B]/10'
									: 'border-gray-50 hover:border-gray-200'}"
							>
								<div class="flex items-start justify-between">
									<span
											class="text-sm font-black {isOggi
											? 'flex h-7 w-7 items-center justify-center rounded-full bg-[#1B4B6B] text-white shadow-lg'
											: 'text-gray-400'}"
									>
										{cella.giorno}
									</span>
								</div>

								<div class="space-y-1">
									{#each evs.slice(0, 2) as ev (ev.idEvento)}
										<div class="flex items-center gap-2">
											<div
													class="h-2 w-2 shrink-0 rounded-full {ev.tipo === 'CORSO'
													? 'bg-blue-500'
													: ev.tipo === 'SCADENZA'
														? 'bg-red-500'
														: 'bg-amber-500'}"
											></div>
											<p class="truncate text-[9px] font-black uppercase text-[#1B4B6B]">
												{ev.titolo}
											</p>
										</div>
									{/each}
									{#if evs.length > 2}
										<p class="mt-1 text-[8px] font-black uppercase tracking-widest text-gray-300">
											+ {evs.length - 2} ALTRI
										</p>
									{/if}
								</div>
							</button>
						{/if}
					{/each}
				</div>
			</div>

			<div
					class="relative flex flex-col overflow-hidden rounded-[2.5rem] bg-[#1B4B6B] p-8 text-white shadow-2xl shadow-[#1B4B6B]/30"
			>
				<div class="absolute -right-10 -top-10 h-40 w-40 rounded-full bg-white/5 blur-3xl"></div>

				<div class="relative z-10">
					<h3 class="text-5xl font-black leading-none tracking-tighter">{giornoSelezionato}</h3>
					<p class="mt-2 text-xs font-black uppercase tracking-[0.3em] opacity-50">
						{MESI[mese]}
						{anno}
					</p>
				</div>

				<div class="custom-scrollbar relative z-10 mt-12 flex-1 space-y-6 overflow-y-auto pr-2">
					{#if eventiDelGiorno.length === 0}
						<div class="py-20 text-center opacity-30">
							<CalendarIcon class="mx-auto mb-4" size={40} strokeWidth={1} />
							<p class="text-[10px] font-black uppercase leading-loose tracking-widest">
								Nessuna attività<br />programmata
							</p>
						</div>
					{:else}
						{#each eventiDelGiorno as ev (ev.idEvento)}
							<div
									in:scale={{ duration: 300, start: 0.95 }}
									class="group rounded-3xl border border-white/10 bg-white/10 p-6 backdrop-blur-md transition-all hover:bg-white/15"
							>
								<div class="mb-4 flex items-start justify-between">
									<div class="rounded-xl bg-white/10 p-2 text-white"><Clock size={16} /></div>
									<span
											class="rounded-full bg-white px-3 py-1 text-[9px] font-black uppercase tracking-widest text-[#1B4B6B]"
									>{ev.tipo}</span
									>
								</div>
								<h4 class="mb-4 text-lg font-black uppercase leading-tight">{ev.titolo}</h4>
								<div class="space-y-2 opacity-60">
									{#if ev.orarioInizio}
										<p class="flex items-center gap-2 text-[10px] font-black uppercase tracking-widest">
											<Clock size={12} />
											{ev.orarioInizio} - {ev.orarioFine}
										</p>
									{/if}
									{#if ev.luogo}
										<p class="flex items-center gap-2 text-[10px] font-black uppercase tracking-widest">
											<MapPin size={12} />
											{ev.luogo}
										</p>
									{/if}
								</div>
							</div>
						{/each}
					{/if}
				</div>

				<button
						class="mt-8 flex w-full items-center justify-center gap-3 rounded-[2rem] bg-white py-5 text-xs font-black uppercase tracking-widest text-[#1B4B6B] shadow-xl transition-transform hover:scale-[1.02]"
				>
					<Plus size={18} /> Aggiungi Nota
				</button>
			</div>
		</div>
	{/if}
</div>

<style>
	.custom-scrollbar::-webkit-scrollbar {
		width: 4px;
	}
	.custom-scrollbar::-webkit-scrollbar-thumb {
		background: rgba(255, 255, 255, 0.2);
		border-radius: 10px;
	}
</style>