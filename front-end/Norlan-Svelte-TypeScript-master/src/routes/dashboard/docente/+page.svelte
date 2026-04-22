<script lang="ts">
	import { onMount } from 'svelte';
	import { fade, scale, slide } from 'svelte/transition';
	import {
		LayoutDashboard, BookOpen, Users, Clock, ArrowRight,
		AlertTriangle, MessageSquare, FileBadge, CheckCircle2,
		MapPin, Calendar, Loader2
	} from 'lucide-svelte';

	interface DashboardStats {
		corsiAttivi: number;
		studentiTotali: number;
		oreMese: number;
	}

	interface LezioneImminente {
		id: string;
		titolo: string;
		dataStr: string;
		orario: string;
		luogo: string;
		isOggi: boolean;
	}

	interface ActionItem {
		id: string;
		tipo: 'MESSAGGIO' | 'ATTESTATI' | 'PRESENZE';
		testo: string;
		urgenza: 'ALTA' | 'MEDIA';
	}

	let isLoading = $state(true);
	let stats = $state<DashboardStats>({ corsiAttivi: 0, studentiTotali: 0, oreMese: 0 });
	let prossimeLezioni = $state<LezioneImminente[]>([]);
	let actionItems = $state<ActionItem[]>([]);

	const dataOggi = new Intl.DateTimeFormat('it-IT', {
		weekday: 'long', day: 'numeric', month: 'long', year: 'numeric'
	}).format(new Date());

	onMount(() => {
		setTimeout(() => {
			stats = {
				corsiAttivi: 4,
				studentiTotali: 128,
				oreMese: 24
			};

			prossimeLezioni = [
				{
					id: 'c1',
					titolo: 'SICUREZZA SUL LAVORO - RISCHIO ALTO',
					dataStr: 'OGGI',
					orario: '14:30 - 18:30',
					luogo: 'AULA MAGNA - SEDE CENTRALE',
					isOggi: true
				},
				{
					id: 'c2',
					titolo: 'AGGIORNAMENTO ANTINCENDIO',
					dataStr: '25 APRILE 2026',
					orario: '09:00 - 13:00',
					luogo: 'CAMPO PROVE ESTERNO',
					isOggi: false
				}
			];

			actionItems = [
				{
					id: 'a1',
					tipo: 'PRESENZE',
					testo: 'Conferma presenze per il corso Antincendio di ieri',
					urgenza: 'ALTA'
				},
				{
					id: 'a2',
					tipo: 'ATTESTATI',
					testo: '3 attestati in attesa di caricamento',
					urgenza: 'MEDIA'
				},
				{
					id: 'a3',
					tipo: 'MESSAGGIO',
					testo: 'Nuovo messaggio da Staff NorLan',
					urgenza: 'MEDIA'
				}
			];

			isLoading = false;
		}, 700);
	});

	function getActionIcon(tipo: string) {
		switch(tipo) {
			case 'PRESENZE': return CheckCircle2;
			case 'ATTESTATI': return FileBadge;
			case 'MESSAGGIO': return MessageSquare;
			default: return AlertTriangle;
		}
	}
</script>

<div in:fade class="max-w-7xl mx-auto space-y-8 pb-10">

	<div class="flex flex-col md:flex-row justify-between items-start md:items-end gap-4">
		<div>
			<div class="flex items-center gap-3 mb-2">
				<div class="p-2 bg-[#1B4B6B] rounded-xl text-white">
					<LayoutDashboard size={20} />
				</div>
				<p class="text-[10px] font-black text-gray-400 uppercase tracking-widest">{dataOggi}</p>
			</div>
			<h1 class="text-4xl font-black text-[#1B4B6B] uppercase tracking-tighter">Benvenuto, Docente</h1>
		</div>
	</div>

	{#if isLoading}
		<div class="py-32 flex flex-col items-center justify-center gap-4">
			<Loader2 size={48} class="animate-spin text-[#1B4B6B]" />
			<span class="text-[10px] font-black text-gray-400 uppercase tracking-widest">Caricamento Overview...</span>
		</div>
	{:else}
		<div class="grid grid-cols-1 md:grid-cols-3 gap-6">
			<div class="bg-white p-8 rounded-[2rem] border border-gray-100 shadow-sm flex items-center gap-6 hover:-translate-y-1 transition-transform group">
				<div class="w-16 h-16 rounded-2xl bg-blue-50 flex items-center justify-center text-blue-600 group-hover:bg-blue-600 group-hover:text-white transition-colors">
					<BookOpen size={28} />
				</div>
				<div>
					<p class="text-[10px] font-black text-gray-400 uppercase tracking-widest mb-1">Corsi Attivi</p>
					<p class="text-4xl font-black text-[#1B4B6B] leading-none">{stats.corsiAttivi}</p>
				</div>
			</div>

			<div class="bg-white p-8 rounded-[2rem] border border-gray-100 shadow-sm flex items-center gap-6 hover:-translate-y-1 transition-transform group">
				<div class="w-16 h-16 rounded-2xl bg-emerald-50 flex items-center justify-center text-emerald-600 group-hover:bg-emerald-500 group-hover:text-white transition-colors">
					<Users size={28} />
				</div>
				<div>
					<p class="text-[10px] font-black text-gray-400 uppercase tracking-widest mb-1">Studenti Totali</p>
					<p class="text-4xl font-black text-[#1B4B6B] leading-none">{stats.studentiTotali}</p>
				</div>
			</div>

			<div class="bg-white p-8 rounded-[2rem] border border-gray-100 shadow-sm flex items-center gap-6 hover:-translate-y-1 transition-transform group">
				<div class="w-16 h-16 rounded-2xl bg-amber-50 flex items-center justify-center text-amber-600 group-hover:bg-amber-500 group-hover:text-white transition-colors">
					<Clock size={28} />
				</div>
				<div>
					<p class="text-[10px] font-black text-gray-400 uppercase tracking-widest mb-1">Ore questo mese</p>
					<p class="text-4xl font-black text-[#1B4B6B] leading-none">{stats.oreMese}h</p>
				</div>
			</div>
		</div>

		<div class="grid grid-cols-1 xl:grid-cols-3 gap-8">

			<div class="xl:col-span-2 space-y-6">
				<div class="flex items-center justify-between px-2">
					<h2 class="text-xl font-black text-[#1B4B6B] uppercase tracking-tighter">Lezioni Imminenti</h2>
					<a href="/dashboard/docente/calendario" class="text-[10px] font-black text-[#1B4B6B] uppercase tracking-widest hover:underline flex items-center gap-1">
						Vedi Calendario <ArrowRight size={12} />
					</a>
				</div>

				<div class="space-y-4">
					{#each prossimeLezioni as lezione (lezione.id)}
						<div class="bg-white rounded-[2.5rem] border {lezione.isOggi ? 'border-[#1B4B6B] shadow-xl shadow-blue-900/10' : 'border-gray-100 shadow-sm'} p-8 flex flex-col md:flex-row items-start md:items-center justify-between gap-6 relative overflow-hidden group">

							{#if lezione.isOggi}
								<div class="absolute top-0 right-0 w-32 h-32 bg-[#1B4B6B]/5 rounded-full blur-3xl"></div>
							{/if}

							<div class="flex items-center gap-6 relative z-10">
								<div class="w-20 h-20 rounded-[1.5rem] flex flex-col items-center justify-center shrink-0 {lezione.isOggi ? 'bg-[#1B4B6B] text-white shadow-md' : 'bg-gray-50 text-[#1B4B6B]'}">
									<Calendar size={24} class="mb-1" />
									<span class="text-[10px] font-black uppercase tracking-widest">{lezione.dataStr}</span>
								</div>

								<div>
									{#if lezione.isOggi}
										<div class="inline-block bg-red-500 text-white text-[9px] font-black px-3 py-1 rounded-lg uppercase tracking-widest mb-3 animate-pulse">
											IN PROGRAMMA OGGI
										</div>
									{/if}
									<h3 class="text-xl font-black text-[#1B4B6B] uppercase leading-tight mb-3 group-hover:text-blue-700 transition-colors">{lezione.titolo}</h3>
									<div class="flex flex-wrap items-center gap-4 text-[10px] font-bold text-gray-400 uppercase tracking-widest">
										<span class="flex items-center gap-1.5"><Clock size={14} class={lezione.isOggi ? 'text-[#1B4B6B]' : ''} /> {lezione.orario}</span>
										<span class="flex items-center gap-1.5"><MapPin size={14} class={lezione.isOggi ? 'text-[#1B4B6B]' : ''} /> {lezione.luogo}</span>
									</div>
								</div>
							</div>

							<div class="w-full md:w-auto relative z-10">
								<a href="/dashboard/docente/corsi/{lezione.id}" class="w-full md:w-auto px-8 py-4 rounded-2xl text-[10px] font-black uppercase tracking-widest transition-all flex items-center justify-center gap-2 {lezione.isOggi ? 'bg-[#1B4B6B] text-white hover:bg-[#153a54]' : 'bg-gray-50 text-[#1B4B6B] hover:bg-gray-100'}">
									Vai al Registro <ArrowRight size={16} />
								</a>
							</div>
						</div>
					{/each}
				</div>
			</div>

			<div class="space-y-6">
				<div class="flex items-center justify-between px-2">
					<h2 class="text-xl font-black text-[#1B4B6B] uppercase tracking-tighter">Da Fare</h2>
				</div>

				<div class="bg-gray-50 rounded-[2.5rem] border border-gray-100 p-8 h-full">
					<div class="space-y-4">
						{#each actionItems as item (item.id)}
							{@const Icon = getActionIcon(item.tipo)}
							<div class="bg-white p-5 rounded-[1.5rem] border border-gray-100 shadow-sm flex items-start gap-4 hover:-translate-y-1 hover:shadow-md transition-all cursor-pointer">
								<div class="w-12 h-12 rounded-xl flex items-center justify-center shrink-0 {item.urgenza === 'ALTA' ? 'bg-red-50 text-red-500' : 'bg-blue-50 text-blue-600'}">
									<Icon size={20} />
								</div>
								<div class="flex-1 pt-1">
									<div class="flex items-center justify-between mb-1">
										<span class="text-[8px] font-black uppercase tracking-widest {item.urgenza === 'ALTA' ? 'text-red-500' : 'text-blue-500'}">{item.tipo}</span>
									</div>
									<p class="text-xs font-bold text-[#1B4B6B] leading-snug">{item.testo}</p>
								</div>
							</div>
						{/each}

						{#if actionItems.length === 0}
							<div class="py-12 text-center opacity-40">
								<CheckCircle2 size={48} class="mx-auto mb-4 text-[#1B4B6B]" />
								<p class="text-[10px] font-black text-[#1B4B6B] uppercase tracking-widest">Tutto in regola!<br/>Nessuna azione richiesta</p>
							</div>
						{/if}
					</div>
				</div>
			</div>

		</div>
	{/if}
</div>

<style>
    :global(body) { background-color: #F9FAFB; }
</style>