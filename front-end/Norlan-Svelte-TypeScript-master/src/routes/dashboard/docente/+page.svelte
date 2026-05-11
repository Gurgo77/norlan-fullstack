<script lang="ts">
	import { onMount } from 'svelte';
	import { fade, scale } from 'svelte/transition';
	import { resolveRoute } from '$app/paths';
	import { SvelteDate } from 'svelte/reactivity';
	import {
		LayoutDashboard, BookOpen, Users, ArrowRight,
		MessageSquare, CheckCircle2, Calendar, Loader2,
		CheckSquare
	} from 'lucide-svelte';
	import { FormazioneService } from '$lib/services/FormazioneService';
	import { AuthService } from '$lib/services/AuthService';
	import type { CorsoFormazione } from '$lib/models/CorsoFormazione';
	import { StatoCorso } from '$lib/models/Enums';
	import { AnagraficaService } from '$lib/services/AnagraficaService';
	import StatCard from '$lib/Components/UI/StatCard.svelte';
	import AlertCard from '$lib/Components/UI/AlertCard.svelte';
	import DashboardCorsoCard from '$lib/Components/Features/Formazione/DashboardCorsoCard.svelte';

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

	function apriRegistro() {
		window.location.href = resolveRoute('/dashboard/docente/corsi');
	}
</script>

<div in:fade class="max-w-7xl mx-auto space-y-6 md:space-y-8 pb-10 p-4 md:p-6">
	<div class="flex flex-col md:flex-row justify-between items-start md:items-end gap-4">
		<div>
			<div class="flex items-center gap-3 mb-2">
				<div class="p-2 bg-[#1B4B6B] rounded-xl text-white shadow-sm shrink-0">
					<LayoutDashboard size={20} />
				</div>
				<p class="text-[9px] md:text-[10px] font-black text-gray-400 uppercase tracking-widest">{dataOggi}</p>
			</div>
			<h1 class="text-2xl md:text-4xl font-black text-[#1B4B6B] uppercase tracking-tighter leading-tight">Benvenuto, {nomeDocente}</h1>
			<p class="text-gray-400 font-bold uppercase text-[10px] tracking-widest mt-1">
				Area Riservata Docente
			</p>
		</div>
	</div>

	{#if isLoading}
		<div class="py-24 md:py-32 flex flex-col items-center justify-center gap-4">
			<Loader2 size={48} class="animate-spin text-[#1B4B6B]" />
			<span class="text-[10px] font-black text-gray-400 uppercase tracking-widest text-center">Sincronizzazione dati didattici...</span>
		</div>
	{:else}
		<div class="grid grid-cols-1 md:grid-cols-2 gap-4 md:gap-6">
			<StatCard titolo="Corsi in Aula" valore={stats.corsiInCorso} icona={BookOpen} bgIcona="bg-blue-50" testoIcona="text-blue-600" hoverBgIcona="group-hover:bg-blue-600"/>
			<StatCard titolo="Studenti Iscritti" valore={stats.studentiTotali} icona={Users} bgIcona="bg-emerald-50" testoIcona="text-emerald-600" hoverBgIcona="group-hover:bg-emerald-500"/>
		</div>

		<div class="grid grid-cols-1 xl:grid-cols-3 gap-8">

			<div class="xl:col-span-2 space-y-6">
				<div class="flex items-center justify-between px-1 md:px-2">
					<h2 class="text-lg md:text-xl font-black text-[#1B4B6B] uppercase tracking-tighter">Prossime Lezioni</h2>
					<a href="{resolveRoute('/dashboard/docente/corsi')}" class="text-[10px] font-black text-[#1B4B6B] uppercase tracking-widest hover:underline flex items-center gap-1">
						Tutti i corsi <ArrowRight size={12} />
					</a>
				</div>

				<div class="grid grid-cols-1 sm:grid-cols-2 gap-4 md:gap-6">
					{#each lezioniImminenti as lezione (lezione.idCorso)}
						<div in:scale>
							<DashboardCorsoCard
									ruolo="docente"
									corso={{
                                    id: lezione.idCorso,
                                    titolo: lezione.titolo,
                                    stato: lezione.stato === 'IN_SVOLGIMENTO' ? 'IN_SVOLGIMENTO' : 'DA_INIZIARE',
                                    dataSvolgimento: `${lezione.dataStr} - ${lezione.orario}`,
                                    luogo: lezione.luogo
                                }}
									onAzioneCorso={() => apriRegistro()}
							/>
						</div>
					{/each}
				</div>

				{#if lezioniImminenti.length === 0}
					<div class="bg-white rounded-3xl border border-dashed border-gray-200 p-8 md:p-12 text-center shadow-sm">
						<Calendar size={40} class="mx-auto text-gray-200 mb-4" />
						<h3 class="text-base md:text-lg font-black text-[#1B4B6B] uppercase">Nessuna lezione a breve</h3>
						<p class="text-[10px] font-bold text-gray-400 uppercase tracking-widest mt-2">Non ci sono corsi programmati nei prossimi giorni.</p>
					</div>
				{/if}
			</div>

			<div class="space-y-6">
				<div class="flex items-center justify-between px-1 md:px-2">
					<h2 class="text-lg md:text-xl font-black text-[#1B4B6B] uppercase tracking-tighter">Azioni Richieste</h2>
				</div>

				<div class="bg-white rounded-[2rem] md:rounded-3xl border border-gray-100 p-5 md:p-6 shadow-sm">
					{#if corsiDaValidare.length > 0}
						<p class="text-[9px] font-black uppercase tracking-widest text-red-500 mb-4 border-b border-red-50 pb-2">Registri da chiudere</p>
						<div class="space-y-3 mb-6">
							{#each corsiDaValidare as corso}
								<div in:fade>
									<AlertCard
											titolo={corso.titolo}
											sottotitolo="Conferma le presenze per finire"
											variante="danger"
											icona={CheckSquare}
											href={resolveRoute('/dashboard/docente/corsi')}
									/>
								</div>
							{/each}
						</div>
					{:else}
						<div class="p-6 text-center border-2 border-dashed border-gray-100 rounded-2xl mb-6 bg-gray-50/50">
							<CheckCircle2 size={24} class="mx-auto text-green-400 mb-2" />
							<p class="text-[10px] font-bold uppercase text-gray-400 tracking-widest">Tutti i registri sono a norma</p>
						</div>
					{/if}

					<p class="text-[9px] font-black uppercase tracking-widest text-[#1B4B6B] mb-4 border-b border-gray-50 pb-2">Scorciatoie Rapide</p>
					<div class="grid grid-cols-2 gap-3">
						<a href="{resolveRoute('/dashboard/docente/studenti')}" class="flex flex-col items-center justify-center p-4 bg-gray-50 rounded-2xl border border-transparent hover:border-[#1B4B6B]/20 hover:bg-blue-50 transition-all text-[#1B4B6B] group">
							<Users size={20} class="mb-2 opacity-50 group-hover:opacity-100 transition-opacity shrink-0" />
							<span class="text-[9px] font-black uppercase tracking-widest text-center">I Tuoi<br>Studenti</span>
						</a>
						<a href="{resolveRoute('/dashboard/docente/messaggi')}" class="flex flex-col items-center justify-center p-4 bg-gray-50 rounded-2xl border border-transparent hover:border-[#1B4B6B]/20 hover:bg-blue-50 transition-all text-[#1B4B6B] group">
							<MessageSquare size={20} class="mb-2 opacity-50 group-hover:opacity-100 transition-opacity shrink-0" />
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