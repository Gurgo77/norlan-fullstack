<script lang="ts">
	import { onMount } from 'svelte';
	import { fade, scale } from 'svelte/transition';
	import { resolveRoute } from '$app/paths';
	import { SvelteDate } from 'svelte/reactivity';
	import {
		LayoutDashboard, BookOpen, Users, Clock, ArrowRight,
		MessageSquare, CheckCircle2, MapPin, Calendar, Loader2,
		Play, CheckSquare
	} from 'lucide-svelte';
	import { FormazioneService } from '$lib/services/FormazioneService';
	import { AuthService } from '$lib/services/AuthService';
	import type { CorsoFormazione } from '$lib/models/CorsoFormazione';
	import { StatoCorso } from '$lib/models/Enums';
	import { AnagraficaService } from '$lib/services/AnagraficaService';
	import StatCard from '$lib/Components/UI/StatCard.svelte';

	interface DashboardStats {
		corsiInCorso: number;
		studentiTotali: number;
		materialiCaricati: number;
	}

	interface LezioneImminente {
		idCorso: number;
		titolo: string;
		dataCompleta: Date;
		dataStr: string;
		orario: string;
		luogo: string;
		isOggi: boolean;
		stato: StatoCorso;
	}

	let isLoading = $state(true);
	let nomeDocente = $state('Docente');
	let stats = $state<DashboardStats>({ corsiInCorso: 0, studentiTotali: 0, materialiCaricati: 0 });
	let lezioniImminenti = $state<LezioneImminente[]>([]);
	let corsiDaValidare = $state<CorsoFormazione[]>([]);

	const dataOggi = new Intl.DateTimeFormat('it-IT', {
		weekday: 'long', day: 'numeric', month: 'long', year: 'numeric'
	}).format(new Date());

	onMount(async () => {
		const session = AuthService.getSession();
		if (!session) return;

		try {
			const profilo = await AnagraficaService.getDocenteById(session.idUtente);
			nomeDocente = (profilo as { nome?: string }).nome || session.email.split('@')[0];
			const tuttiCorsi = await FormazioneService.getAllCorsi();
			const mieiCorsi = tuttiCorsi.filter(c => c.idDocente === session.idUtente);
			const corsiInSvolgimento = mieiCorsi.filter(c => c.stato === StatoCorso.IN_SVOLGIMENTO);
			const corsiProgrammati = mieiCorsi.filter(c => c.stato === StatoCorso.PROGRAMMATO || !c.stato);
			corsiDaValidare = mieiCorsi.filter(c => c.stato === StatoCorso.CONCLUSO);

			let totaleStudenti = 0;
			let totaleMateriali = 0;
			const conteggiPromises = mieiCorsi.filter(c => c.stato !== StatoCorso.CERTIFICATO).map(async (corso) => {
				const iscritti = await FormazioneService.getIscrizioniByCorso(corso.idCorso);
				const materiali = await FormazioneService.getMaterialiByCorso(corso.idCorso);
				return { numIscritti: iscritti.length, numMateriali: materiali.length };
			});

			const risultatiConteggi = await Promise.all(conteggiPromises);
			risultatiConteggi.forEach(res => {
				totaleStudenti += res.numIscritti;
				totaleMateriali += res.numMateriali;
			});

			stats = {
				corsiInCorso: corsiInSvolgimento.length,
				studentiTotali: totaleStudenti,
				materialiCaricati: totaleMateriali
			};

			const corsiAttivi = [...corsiInSvolgimento, ...corsiProgrammati];
			const oggiZero = new SvelteDate();
			oggiZero.setHours(0, 0, 0, 0);

			const lezioniElaborate = corsiAttivi
					.map(corso => {
						const d = new Date(corso.dataOrario);
						return {
							idCorso: corso.idCorso,
							titolo: corso.titolo,
							dataCompleta: d,
							dataStr: d.toLocaleDateString('it-IT', { day: '2-digit', month: 'short' }).toUpperCase(),
							orario: d.toLocaleTimeString('it-IT', { hour: '2-digit', minute: '2-digit' }),
							luogo: corso.luogoFisico,
							isOggi: d.toDateString() === new Date().toDateString(),
							stato: corso.stato || StatoCorso.PROGRAMMATO
						};
					})
					.filter(lez => lez.dataCompleta.getTime() >= oggiZero.getTime() || lez.stato === StatoCorso.IN_SVOLGIMENTO)
					.sort((a, b) => a.dataCompleta.getTime() - b.dataCompleta.getTime())
					.slice(0, 3);

			lezioniImminenti = lezioniElaborate;

		} catch (error) {
			console.error(error);
		} finally {
			isLoading = false;
		}
	});
</script>

<div in:fade class="max-w-7xl mx-auto space-y-8 pb-10">
	<div class="flex flex-col md:flex-row justify-between items-start md:items-end gap-4">
		<div>
			<div class="flex items-center gap-3 mb-2">
				<div class="p-2 bg-[#1B4B6B] rounded-xl text-white shadow-sm">
					<LayoutDashboard size={20} />
				</div>
				<p class="text-[10px] font-black text-gray-400 uppercase tracking-widest">{dataOggi}</p>
			</div>
			<h1 class="text-4xl font-black text-[#1B4B6B] uppercase tracking-tighter">Benvenuto, {nomeDocente}</h1>
			<p class="text-gray-400 font-bold uppercase text-[10px] tracking-widest mt-1">
				Area Riservata Docente
			</p>
		</div>
	</div>

	{#if isLoading}
		<div class="py-32 flex flex-col items-center justify-center gap-4">
			<Loader2 size={48} class="animate-spin text-[#1B4B6B]" />
			<span class="text-[10px] font-black text-gray-400 uppercase tracking-widest">Sincronizzazione dati didattici...</span>
		</div>
	{:else}
		<div class="grid grid-cols-1 md:grid-cols-2 gap-6">
			<StatCard titolo="Corsi in Aula" valore={stats.corsiInCorso} icona={BookOpen} bgIcona="bg-blue-50" testoIcona="text-blue-600" hoverBgIcona="group-hover:bg-blue-600"/>
			<StatCard titolo="Studenti Iscritti" valore={stats.studentiTotali} icona={Users} bgIcona="bg-emerald-50" testoIcona="text-emerald-600" hoverBgIcona="group-hover:bg-emerald-500"/>
		</div>

		<div class="grid grid-cols-1 xl:grid-cols-3 gap-8">
			<div class="xl:col-span-2 space-y-6">
				<div class="flex items-center justify-between px-2">
					<h2 class="text-xl font-black text-[#1B4B6B] uppercase tracking-tighter">Prossime Lezioni</h2>
					<a href="{resolveRoute('/dashboard/docente/corsi')}" class="text-[10px] font-black text-[#1B4B6B] uppercase tracking-widest hover:underline flex items-center gap-1">
						Tutti i corsi <ArrowRight size={12} />
					</a>
				</div>

				<div class="space-y-4">
					{#each lezioniImminenti as lezione (lezione.idCorso)}
						<div class="bg-white rounded-3xl border {lezione.isOggi ? 'border-[#1B4B6B] shadow-xl shadow-blue-900/5' : 'border-gray-100 shadow-sm'} p-6 flex flex-col md:flex-row items-start md:items-center justify-between gap-6 relative overflow-hidden group hover:border-[#1B4B6B]/30 transition-all">
							{#if lezione.isOggi}
								<div class="absolute top-0 right-0 w-32 h-32 bg-[#1B4B6B]/5 rounded-full blur-3xl"></div>
							{/if}
							<div class="flex items-center gap-6 relative z-10 w-full md:w-auto">
								<div class="w-16 h-16 rounded-2xl flex flex-col items-center justify-center shrink-0 {lezione.isOggi ? 'bg-[#1B4B6B] text-white shadow-md' : 'bg-gray-50 text-[#1B4B6B]'}">
									<span class="text-xs font-black uppercase tracking-tighter">{lezione.isOggi ? 'OGGI' : lezione.dataStr.split(' ')[0]}</span>
									{#if !lezione.isOggi}
										<span class="text-[9px] font-bold uppercase">{lezione.dataStr.split(' ')[1]}</span>
									{/if}
								</div>
								<div class="flex-1 min-w-0">
									{#if lezione.stato === StatoCorso.IN_SVOLGIMENTO}
										<span class="inline-block bg-blue-100 text-blue-700 text-[8px] font-black px-2 py-0.5 rounded uppercase tracking-widest mb-1">In Svolgimento</span>
									{/if}
									<h3 class="text-lg font-black text-[#1B4B6B] uppercase leading-tight mb-2 truncate group-hover:text-blue-700 transition-colors" title={lezione.titolo}>{lezione.titolo}</h3>

									<div class="flex flex-wrap items-center gap-4 text-[10px] font-bold text-gray-400 uppercase tracking-widest">
										<span class="flex items-center gap-1.5"><Clock size={12} class={lezione.isOggi ? 'text-[#1B4B6B]' : ''} /> {lezione.orario}</span>
										<span class="flex items-center gap-1.5"><MapPin size={12} class={lezione.isOggi ? 'text-[#1B4B6B]' : ''} /> {lezione.luogo}</span>
									</div>
								</div>
							</div>
							<div class="w-full md:w-auto relative z-10 shrink-0">
								<a href="{resolveRoute('/dashboard/docente/corsi')}" class="w-full md:w-auto px-6 py-3 rounded-xl text-[10px] font-black uppercase tracking-widest transition-all flex items-center justify-center gap-2 {lezione.stato === StatoCorso.IN_SVOLGIMENTO ? 'bg-[#1B4B6B] text-white shadow-md' : 'bg-gray-50 text-[#1B4B6B] hover:bg-gray-100'}">
									{#if lezione.stato === StatoCorso.IN_SVOLGIMENTO}
										Apri Corso <Play size={14} fill="currentColor" />
									{:else}
										Dettagli <ArrowRight size={14} />
									{/if}
								</a>
							</div>
						</div>
					{/each}

					{#if lezioniImminenti.length === 0}
						<div class="bg-white rounded-3xl border border-dashed border-gray-200 p-12 text-center shadow-sm">
							<Calendar size={40} class="mx-auto text-gray-200 mb-4" />
							<h3 class="text-lg font-black text-[#1B4B6B] uppercase">Nessuna lezione a breve</h3>
							<p class="text-[10px] font-bold text-gray-400 uppercase tracking-widest mt-2">Non ci sono corsi programmati nei prossimi giorni.</p>
						</div>
					{/if}
				</div>
			</div>

			<div class="space-y-6">
				<div class="flex items-center justify-between px-2">
					<h2 class="text-xl font-black text-[#1B4B6B] uppercase tracking-tighter">Azioni Richieste</h2>
				</div>

				<div class="bg-white rounded-3xl border border-gray-100 p-6 h-full shadow-sm">
					{#if corsiDaValidare.length > 0}
						<p class="text-[9px] font-black uppercase tracking-widest text-red-500 mb-4 border-b border-red-50 pb-2">Registri da chiudere</p>
						<div class="space-y-3 mb-6">
							{#each corsiDaValidare as corso}
								<a href="{resolveRoute('/dashboard/docente/corsi')}" class="block bg-red-50 p-4 rounded-2xl border border-red-100 hover:bg-red-100 transition-colors group">
									<div class="flex items-start gap-3">
										<div class="p-2 bg-white rounded-lg text-red-500 shrink-0 shadow-sm"><CheckSquare size={16} /></div>
										<div>
											<p class="text-xs font-black text-red-700 uppercase leading-tight">{corso.titolo}</p>
											<p class="text-[9px] font-bold text-red-500 uppercase mt-1">Conferma le presenze per finire</p>
										</div>
									</div>
								</a>
							{/each}
						</div>
					{:else}
						<div class="p-6 text-center border-2 border-dashed border-gray-100 rounded-2xl mb-6">
							<CheckCircle2 size={24} class="mx-auto text-green-400 mb-2" />
							<p class="text-[10px] font-bold uppercase text-gray-400 tracking-widest">Tutti i registri sono a norma</p>
						</div>
					{/if}

					<p class="text-[9px] font-black uppercase tracking-widest text-[#1B4B6B] mb-4 border-b border-gray-50 pb-2">Scorciatoie Rapide</p>
					<div class="grid grid-cols-2 gap-3">
						<a href="{resolveRoute('/dashboard/docente/studenti')}" class="flex flex-col items-center justify-center p-4 bg-gray-50 rounded-2xl border border-transparent hover:border-[#1B4B6B]/20 hover:bg-blue-50 transition-all text-[#1B4B6B] group">
							<Users size={20} class="mb-2 opacity-50 group-hover:opacity-100 transition-opacity" />
							<span class="text-[9px] font-black uppercase tracking-widest text-center">I Tuoi<br>Studenti</span>
						</a>
						<a href="{resolveRoute('/dashboard/docente/messaggi')}" class="flex flex-col items-center justify-center p-4 bg-gray-50 rounded-2xl border border-transparent hover:border-[#1B4B6B]/20 hover:bg-blue-50 transition-all text-[#1B4B6B] group">
							<MessageSquare size={20} class="mb-2 opacity-50 group-hover:opacity-100 transition-opacity" />
							<span class="text-[9px] font-black uppercase tracking-widest text-center">Apri<br>Chat</span>
						</a>
					</div>
				</div>
			</div>
		</div>
	{/if}
</div>

<style>
	:global(body) { background-color: #F9FAFB; }
</style>