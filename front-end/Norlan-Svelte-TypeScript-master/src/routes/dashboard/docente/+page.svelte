<script lang="ts">
	import { onMount } from 'svelte';
	import { fade } from 'svelte/transition';
	import {
		LayoutDashboard, BookOpen, Users, Clock, ArrowRight,
		AlertTriangle, MessageSquare, FileBadge, CheckCircle2,
		MapPin, Calendar, Loader2
	} from 'lucide-svelte';

	import { FormazioneService } from '$lib/services/FormazioneService';
	import { AuthService } from '$lib/services/AuthService';

	interface DashboardStats {
		corsiAttivi: number;
		studentiTotali: number;
	}

	interface LezioneImminente {
		id: number;
		titolo: string;
		dataStr: string;
		orario: string;
		luogo: string;
		isOggi: boolean;
	}

	interface ActionItem {
		id: string;
		tipo: 'MESSAGGIO' | 'ATTESTATI' | 'PRESENZE' | 'SISTEMA';
		testo: string;
		urgenza: 'ALTA' | 'MEDIA';
	}

	let isLoading = $state(true);
	let nomeDocente = $state('Docente');

	let stats = $state<DashboardStats>({ corsiAttivi: 0, studentiTotali: 0});
	let prossimeLezioni = $state<LezioneImminente[]>([]);
	let actionItems = $state<ActionItem[]>([]);

	const dataOggi = new Intl.DateTimeFormat('it-IT', {
		weekday: 'long', day: 'numeric', month: 'long', year: 'numeric'
	}).format(new Date());

	onMount(async () => {
		const session = AuthService.getSession(); //
		if (!session) return;

		nomeDocente = session.email.split('@')[0]; // Fallback visivo rapido, il layout carica l'anagrafica completa

		try {
			// 1. Recupero tutti i corsi globali
			const tuttiCorsi = await FormazioneService.getAllCorsi();

			// 2. Filtro solo i corsi assegnati a questo docente
			const mieiCorsi = tuttiCorsi.filter(c => c.idDocente === session.idUtente);

			// --- CALCOLO STATISTICHE ---
			const corsiAttivi = mieiCorsi.filter(c => c.stato === 'PROGRAMMATO' || c.stato === 'IN_SVOLGIMENTO');

			let studentiCount = 0;
			let oreQuestoMese = 0;
			const meseCorrente = new Date().getMonth();

			// Calcolo asincrono degli iscritti per ogni corso attivo
			for (const corso of corsiAttivi) {
				try {
					// Recupero gli iscritti al corso specifico
					const iscritti = await FormazioneService.getIscrizioniByCorso(corso.idCorso);
					studentiCount += iscritti.length;

					// Calcolo ore (Se il corso è nel mese corrente. Assumiamo durata standard di 4h se non specificata dal BE)
					if (new Date(corso.dataOrario).getMonth() === meseCorrente) {
						oreQuestoMese += 4;
					}
				} catch(e) { console.warn("Errore fetch iscritti:", e); }
			}

			stats = {
				corsiAttivi: corsiAttivi.length,
				studentiTotali: studentiCount
			};

			// --- CALCOLO LEZIONI IMMINENTI ---
			// eslint-disable-next-line svelte/prefer-svelte-reactivity
			const oggiZero = new Date();
			oggiZero.setHours(0,0,0,0);

			const corsiFuturi = mieiCorsi
					.filter(c => new Date(c.dataOrario).getTime() >= oggiZero.getTime())
					.sort((a, b) => new Date(a.dataOrario).getTime() - new Date(b.dataOrario).getTime())
					.slice(0, 3); // Prende le prossime 3 lezioni

			prossimeLezioni = corsiFuturi.map(corso => {
				const dataCorso = new Date(corso.dataOrario);
				const isOggi = dataCorso.toDateString() === new Date().toDateString();

				return {
					id: corso.idCorso,
					titolo: corso.titolo,
					dataStr: isOggi ? 'OGGI' : dataCorso.toLocaleDateString('it-IT', { day: '2-digit', month: 'long', year: 'numeric' }).toUpperCase(),
					orario: dataCorso.toLocaleTimeString('it-IT', { hour: '2-digit', minute: '2-digit' }),
					luogo: corso.luogoFisico,
					isOggi: isOggi
				};
			});

			// --- GENERAZIONE ACTION ITEMS (DA FARE) DINAMICA ---
			const actions: ActionItem[] = [];

			// Controllo se ci sono corsi "Conclusi" oggi che richiedono la conferma presenze
			const corsiDaConfermare = mieiCorsi.filter(c => {
				const d = new Date(c.dataOrario);
				return d.toDateString() === new Date().toDateString() && d.getTime() < new Date().getTime();
			});

			if (corsiDaConfermare.length > 0) {
				actions.push({
					id: 'a1',
					tipo: 'PRESENZE',
					testo: `Conferma presenze per il corso ${corsiDaConfermare[0].titolo}`,
					urgenza: 'ALTA'
				});
			}

			if (actions.length === 0) {
				actions.push({
					id: 's1',
					tipo: 'SISTEMA',
					testo: 'Registro elettronico sincronizzato e aggiornato.',
					urgenza: 'MEDIA'
				});
			}

			actionItems = actions;

		} catch (error) {
			console.error("Errore di caricamento dashboard docente:", error);
		} finally {
			isLoading = false;
		}
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
			<h1 class="text-4xl font-black text-[#1B4B6B] uppercase tracking-tighter">Benvenuto, {nomeDocente}</h1>
		</div>
	</div>

	{#if isLoading}
		<div class="py-32 flex flex-col items-center justify-center gap-4">
			<Loader2 size={48} class="animate-spin text-[#1B4B6B]" />
			<span class="text-[10px] font-black text-gray-400 uppercase tracking-widest">Calcolo Statistiche Didattiche...</span>
		</div>
	{:else}
		<div class="grid grid-cols-1 md:grid-cols-2 gap-6">
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
					<p class="text-[10px] font-black text-gray-400 uppercase tracking-widest mb-1">Studenti Iscritti</p>
					<p class="text-4xl font-black text-[#1B4B6B] leading-none">{stats.studentiTotali}</p>
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
									<span class="text-[10px] font-black uppercase tracking-widest text-center px-1 leading-tight">{lezione.dataStr}</span>
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
					{#if prossimeLezioni.length === 0}
						<div class="bg-white rounded-[2.5rem] border border-gray-100 p-12 text-center shadow-sm">
							<Calendar size={48} class="mx-auto text-gray-200 mb-4" />
							<h3 class="text-xl font-black text-[#1B4B6B] uppercase">Nessuna Lezione in programma</h3>
							<p class="text-[10px] font-bold text-gray-400 uppercase tracking-widest mt-2">Non hai corsi futuri assegnati nel registro.</p>
						</div>
					{/if}
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
								<div class="w-12 h-12 rounded-xl flex items-center justify-center shrink-0 {item.urgenza === 'ALTA' ? 'bg-red-50 text-red-500' : (item.urgenza === 'MEDIA' && item.tipo !== 'SISTEMA' ? 'bg-blue-50 text-blue-600' : 'bg-emerald-50 text-emerald-500')}">
									<Icon size={20} />
								</div>
								<div class="flex-1 pt-1">
									<div class="flex items-center justify-between mb-1">
										<span class="text-[8px] font-black uppercase tracking-widest {item.urgenza === 'ALTA' ? 'text-red-500' : (item.urgenza === 'MEDIA' && item.tipo !== 'SISTEMA' ? 'text-blue-500' : 'text-emerald-500')}">{item.tipo}</span>
									</div>
									<p class="text-xs font-bold text-[#1B4B6B] leading-snug">{item.testo}</p>
								</div>
							</div>
						{/each}
					</div>
				</div>
			</div>

		</div>
	{/if}
</div>

<style>
	:global(body) { background-color: #F9FAFB; }
</style>