<script lang="ts">
	/*
Dashboard Principale Admin (Home).
Pannello di controllo globale che aggrega e sintetizza i dati da tutti i sottomoduli.
Fornisce KPI in tempo reale, una vista prioritaria sulle criticità (scadenze documenti e DPI)
e il calendario dei prossimi corsi formativi, fungendo da hub di smistamento operativo.
*/
	import { onMount } from 'svelte';
	import { fade, scale } from 'svelte/transition';
	import { resolveRoute } from '$app/paths';
	import {
		Building2, AlertCircle, LayoutDashboard,
		Users, GraduationCap, UserSquare2, HardHat, FileText, CheckCircle2, Calendar
	} from 'lucide-svelte';
	import { searchState } from '$lib/searchState.svelte';

	import { AnagraficaService } from '$lib/services/AnagraficaService';
	import { DocumentoService } from '$lib/services/DocumentoService';
	import { LavoratoreService } from '$lib/services/LavoratoreService';
	import { FormazioneService } from '$lib/services/FormazioneService';
	import type { Azienda } from '$lib/models/Azienda';
	import type { DipendenteDTO, AssegnazioneDPIDTO } from '$lib/services/LavoratoreService';
	import type { DocenteData } from '$lib/models/Docente';
	import type { CorsoFormazione } from '$lib/models/CorsoFormazione';
	import type { Documento } from '$lib/models/Documento';
	import StatCard from '$lib/Components/UI/StatCard.svelte';
	import AlertCard from '$lib/Components/UI/AlertCard.svelte';
	import { getInfoScadenza } from '$lib/utils/scadenzeUtils';


	interface ScadenzaTabella {
		id: string;
		tipo: 'DOCUMENTO' | 'DPI';
		idAzienda: number;
		azienda: string;
		dettaglio: string;
		status: 'danger' | 'warning' | 'success';
		testoScadenza: string;
		dataScadenza: string;
		timestampScadenza: number;
		linkRedirect: string;
	}

	interface DpiEsteso extends AssegnazioneDPIDTO {
		nomeDipendente: string;
		idAzienda: number;
		dataScadenzaRevisione?: string;
		idAssegnazione?: number;
		tipo?: string;
	}

	let isLoading = $state(true);
	let stats = $state({
		aziende: 0,
		dipendenti: 0,
		docenti: 0,
		corsiAttivi: 0,
		dpiCompliance: 100
	});

	let scadenzeDocs = $state<ScadenzaTabella[]>([]);
	let scadenzeDpi = $state<ScadenzaTabella[]>([]);
	let corsiRecenti = $state<CorsoFormazione[]>([]);
	let avvisiCriticiCount = $state(0);

	const dataOggi = new Intl.DateTimeFormat('it-IT', {
		weekday: 'long', day: 'numeric', month: 'long', year: 'numeric'
	}).format(new Date());

	// Carica l'intero ecosistema dati, compila le metriche (stats) e normalizza le scadenze
	onMount(async () => {
		try {
			const [datiAziende, datiDipendenti, datiDocenti, datiCorsi, tuttiDocumenti] = await Promise.all([
				AnagraficaService.getAllAziende(),
				LavoratoreService.getAll(),
				AnagraficaService.getAllDocenti(),
				FormazioneService.getAllCorsi(),
				DocumentoService.getAllDocumenti()
			]);

			const aziendeList = datiAziende as Azienda[];
			const dipendentiList = datiDipendenti as DipendenteDTO[];
			const docentiList = datiDocenti as DocenteData[];
			const corsiList = datiCorsi as CorsoFormazione[];
			const documentiList = tuttiDocumenti as Documento[];

			const dpiPromises = dipendentiList.map(async (d: DipendenteDTO) => {
				try {
					const dpis = await LavoratoreService.getDpiByLavoratore(d.idUtente);
					return dpis.map((dpi: AssegnazioneDPIDTO) => ({
						...dpi,
						nomeDipendente: `${d.nome} ${d.cognome}`,
						idAzienda: (d as DipendenteDTO & { idAzienda?: number }).idAzienda || 0
					}));
				} catch {
					return [];
				}
			});

			const allDpis: DpiEsteso[] = (await Promise.all(dpiPromises)).flat();

			const docsProcessati: ScadenzaTabella[] = documentiList
					.filter(doc => doc.tipologia !== 'ATTESTATO_CORSO')
					.map(doc => {
						const info = getInfoScadenza(doc.dataScadenza);
						return {
							id: `doc_${doc.idDocumento}`,
							tipo: 'DOCUMENTO',
							idAzienda: doc.idAzienda,
							azienda: doc.ragioneSocialeAzienda,
							dettaglio: `${doc.tipologia.replace(/_/g, ' ')} - ${doc.modulo}`,
							status: info.stato === 'DANGER' ? 'danger' : info.stato === 'WARNING' ? 'warning' : 'success',
							testoScadenza: info.label.toUpperCase(),
							dataScadenza: new Date(doc.dataScadenza).toLocaleDateString('it-IT'),
							timestampScadenza: new Date(doc.dataScadenza).getTime(),
							linkRedirect: resolveRoute('/dashboard/admin/scadenziario')
						};
					});

			const dpiProcessati: ScadenzaTabella[] = allDpis.filter(dpi => {
				return dpi.dataScadenzaRevisione && dpi.dataScadenzaRevisione !== '9999-12-31';
			}).map(dpi => {
				const info = getInfoScadenza(dpi.dataScadenzaRevisione);
				const az = aziendeList.find(a => String(a.idUtente) === String(dpi.idAzienda));
				return {
					id: `dpi_${dpi.idAssegnazione}`,
					tipo: 'DPI',
					idAzienda: az ? az.idUtente : 0,
					azienda: az ? az.ragioneSociale : 'Azienda N.D.',
					dettaglio: `${dpi.tipo!.replace(/_/g, ' ')} (${dpi.nomeDipendente})`,
					status: info.stato === 'DANGER' ? 'danger' : info.stato === 'WARNING' ? 'warning' : 'success',
					testoScadenza: info.label.toUpperCase(),
					dataScadenza: new Date(dpi.dataScadenzaRevisione!).toLocaleDateString('it-IT'),
					timestampScadenza: new Date(dpi.dataScadenzaRevisione!).getTime(),
					linkRedirect: resolveRoute('/dashboard/admin/dpi')
				};
			});

			const statusOrder = { 'danger': 1, 'warning': 2, 'success': 3 };

			scadenzeDocs = docsProcessati.sort((a, b) => {
				if (statusOrder[a.status] !== statusOrder[b.status]) return statusOrder[a.status] - statusOrder[b.status];
				return a.timestampScadenza - b.timestampScadenza;
			});

			scadenzeDpi = dpiProcessati.sort((a, b) => {
				if (statusOrder[a.status] !== statusOrder[b.status]) return statusOrder[a.status] - statusOrder[b.status];
				return a.timestampScadenza - b.timestampScadenza;
			});

			corsiRecenti = corsiList
					.filter(c => c.stato === 'PROGRAMMATO' || c.stato === 'IN_SVOLGIMENTO')
					.sort((a, b) => new Date(a.dataOrario).getTime() - new Date(b.dataOrario).getTime());

			const dpiNonConformi = dpiProcessati.filter(d => d.status !== 'success').length;
			const totaliDpi = dpiProcessati.length;
			const perc = totaliDpi > 0 ? Math.round(((totaliDpi - dpiNonConformi) / totaliDpi) * 100) : 100;

			stats = {
				aziende: aziendeList.length,
				dipendenti: dipendentiList.length,
				docenti: docentiList.length,
				corsiAttivi: corsiRecenti.length,
				dpiCompliance: perc
			};

			avvisiCriticiCount = [...scadenzeDocs, ...scadenzeDpi].filter(s => s.status !== 'success').length;

		} catch (error) {
			console.error(error);
		} finally {
			isLoading = false;
		}
	});

	// Intercetta l'input della barra di ricerca globale (header) per filtrare i widget
	const docsDaMostrare = $derived(
			scadenzeDocs.filter(s => s.azienda.toLowerCase().includes(searchState.query.toLowerCase()) ||
					s.dettaglio.toLowerCase().includes(searchState.query.toLowerCase())).slice(0, 5)
	);

	const dpiDaMostrare = $derived(
			scadenzeDpi.filter(s => s.azienda.toLowerCase().includes(searchState.query.toLowerCase()) ||
					s.dettaglio.toLowerCase().includes(searchState.query.toLowerCase())).slice(0, 5)
	);

	const corsiDaMostrare = $derived(
			corsiRecenti.filter(c => c.titolo.toLowerCase().includes(searchState.query.toLowerCase())).slice(0, 3)
	);
</script>

<div in:fade class="pb-10 md:pb-20 max-w-[1600px] mx-auto">
	<!-- Messaggio di Benvenuto: Include la data odierna formattata dinamicamente -->
	<div class="mb-6 md:mb-10 flex flex-col md:flex-row justify-between items-start md:items-end gap-4">
		<div>
			<div class="flex items-center gap-3 mb-2">
				<div class="p-2 bg-[#1B4B6B] rounded-xl text-white shadow-sm">
					<LayoutDashboard size={20} />
				</div>
				<p class="text-[10px] font-black text-gray-400 uppercase tracking-widest">{dataOggi}</p>
			</div>
			<h1 class="text-2xl md:text-4xl font-extrabold text-[#1B4B6B] uppercase tracking-tighter">Benvenuto Admin</h1>
			<p class="text-gray-500 font-bold uppercase text-[10px] md:text-xs tracking-tighter mt-1">Centro di Controllo Globale NorLan.</p>
		</div>
	</div>

	{#if isLoading}
		<div class="flex flex-col justify-center items-center h-64 gap-4">
			<div class="animate-spin rounded-full h-12 w-12 border-b-4 border-[#1B4B6B]"></div>
			<span class="text-[10px] font-black uppercase tracking-widest text-gray-400">Analisi del sistema in corso...</span>
		</div>
	{:else}
		<!-- KPI Summary: 5 StatCard animate in ingresso per dare evidenza numerica del volume di dati -->
		<div class="grid grid-cols-1 sm:grid-cols-2 md:grid-cols-3 xl:grid-cols-5 gap-4 md:gap-6 mb-8 md:mb-10">
			<div in:scale={{duration: 200, delay: 0}} class="h-full">
				<StatCard titolo="Aziende Clienti" valore={stats.aziende} icona={Building2} href={resolveRoute('/dashboard/admin/aziende')} />
			</div>
			<div in:scale={{duration: 200, delay: 50}} class="h-full">
				<StatCard titolo="Personale Aziende" valore={stats.dipendenti} icona={Users} href={resolveRoute('/dashboard/admin/dipendenti')} />
			</div>
			<div in:scale={{duration: 200, delay: 100}} class="h-full">
				<StatCard titolo="Docenti Attivi" valore={stats.docenti} icona={UserSquare2} href={resolveRoute('/dashboard/admin/docenti')} />
			</div>
			<div in:scale={{duration: 200, delay: 150}} class="h-full">
				<StatCard titolo="Corsi in Programma" valore={stats.corsiAttivi} icona={GraduationCap} href={resolveRoute('/dashboard/admin/formazione')} />
			</div>
			<div in:scale={{duration: 200, delay: 200}} class="h-full sm:col-span-2 md:col-span-1 xl:col-span-1">
				<StatCard titolo="Compliance DPI" valore="{stats.dpiCompliance}%" icona={HardHat} href={resolveRoute('/dashboard/admin/dpi')} />
			</div>
		</div>

		<!-- Layout a 3 Colonne: Widget per Documenti, DPI e Corsi in Arrivo -->
		<div class="grid grid-cols-1 lg:grid-cols-3 gap-4 md:gap-6">
			<!-- Liste scrollabili con link diretti (Vedi tutto / Gestisci) per il reindirizzamento veloce -->
			<div class="bg-white rounded-[2rem] md:rounded-3xl shadow-sm border border-gray-100 flex flex-col overflow-hidden h-[350px] md:h-[420px]">
				<div class="p-4 md:p-5 border-b border-gray-50 flex justify-between items-center bg-gray-50/30 shrink-0">
					<div class="flex items-center gap-2 md:gap-3">
						<FileText class="text-[#1B4B6B]" size={16} />
						<h2 class="font-black text-[#1B4B6B] uppercase text-[10px] md:text-xs tracking-tight">Scadenze Documenti</h2>
					</div>
					<a href={resolveRoute('/dashboard/admin/scadenziario')} class="text-[9px] font-black text-[#1B4B6B] uppercase hover:underline">Vedi tutto</a>
				</div>
				<div class="p-3 md:p-4 space-y-3 overflow-y-auto custom-scrollbar-data">
					{#each docsDaMostrare as scadenza (scadenza.id)}
						<AlertCard
								titolo={scadenza.azienda}
								sottotitolo={scadenza.dettaglio}
								variante={scadenza.status}
								icona={FileText}
								stato={scadenza.testoScadenza}
								data={scadenza.status !== 'success' ? scadenza.dataScadenza : ''}
								href={scadenza.linkRedirect}
						/>
					{:else}
						<div class="py-10 text-center">
							<CheckCircle2 size={24} class="mx-auto text-gray-200 mb-2" />
							<p class="text-gray-400 font-bold uppercase text-[9px] tracking-widest">Nessun documento in scadenza.</p>
						</div>
					{/each}
				</div>
			</div>

			<div class="bg-white rounded-[2rem] md:rounded-3xl shadow-sm border border-gray-100 flex flex-col overflow-hidden h-[350px] md:h-[420px]">
				<div class="p-4 md:p-5 border-b border-gray-50 flex justify-between items-center bg-gray-50/30 shrink-0">
					<div class="flex items-center gap-2 md:gap-3">
						<HardHat class="text-[#1B4B6B]" size={16} />
						<h2 class="font-black text-[#1B4B6B] uppercase text-[10px] md:text-xs tracking-tight">Scadenze DPI</h2>
					</div>
					<a href={resolveRoute('/dashboard/admin/dpi')} class="text-[9px] font-black text-[#1B4B6B] uppercase hover:underline">Vedi tutto</a>
				</div>
				<div class="p-3 md:p-4 space-y-3 overflow-y-auto custom-scrollbar-data">
					{#each dpiDaMostrare as scadenza (scadenza.id)}
						<AlertCard
								titolo={scadenza.azienda}
								sottotitolo={scadenza.dettaglio}
								variante={scadenza.status}
								icona={HardHat}
								stato={scadenza.testoScadenza}
								data={scadenza.status !== 'success' ? scadenza.dataScadenza : ''}
								href={scadenza.linkRedirect}
						/>
					{:else}
						<div class="py-10 text-center">
							<CheckCircle2 size={24} class="mx-auto text-gray-200 mb-2" />
							<p class="text-gray-400 font-bold uppercase text-[9px] tracking-widest">Nessun DPI in scadenza.</p>
						</div>
					{/each}
				</div>
			</div>

			<div class="flex flex-col gap-4 md:gap-6 h-auto md:h-[420px]">
				<div class="bg-white rounded-[2rem] md:rounded-3xl shadow-sm border border-gray-100 flex flex-col overflow-hidden flex-1 min-h-[200px]">
					<div class="p-4 border-b border-gray-50 flex justify-between items-center bg-gray-50/30 shrink-0">
						<div class="flex items-center gap-2">
							<Calendar class="text-[#1B4B6B]" size={16} />
							<h2 class="font-black text-[#1B4B6B] uppercase text-[10px] tracking-tight">Corsi in Arrivo</h2>
						</div>
						<a href={resolveRoute('/dashboard/admin/formazione')} class="text-[9px] font-black text-[#1B4B6B] uppercase hover:underline">Gestisci</a>
					</div>
					<div class="p-3 space-y-2 overflow-y-auto custom-scrollbar-data flex-1">
						{#each corsiDaMostrare as corso (corso.idCorso)}
							<AlertCard
									titolo={corso.titolo}
									sottotitolo={corso.luogoFisico}
									variante="info"
									icona={GraduationCap}
									data={new Date(corso.dataOrario).toLocaleDateString('it-IT')}
									href={resolveRoute('/dashboard/admin/formazione')}
							/>
						{:else}
							<div class="py-6 text-center">
								<CheckCircle2 size={20} class="mx-auto text-gray-200 mb-1" />
								<p class="text-gray-400 font-bold uppercase text-[9px] tracking-widest">Nessun corso in programma.</p>
							</div>
						{/each}
					</div>
				</div>

				<!-- Widget "Allarmi in Evidenza": Box colorato (background primario) per isolare il numero totale di criticità -->
				<div class="bg-[#1B4B6B] rounded-[2rem] md:rounded-3xl p-6 text-white shadow-xl relative overflow-hidden shrink-0 min-h-[140px]">
					<div class="absolute -right-4 -top-4 opacity-10">
						<AlertCircle size={100} />
					</div>
					<h3 class="text-[10px] font-black uppercase tracking-widest mb-1 relative z-10">Allarmi in Evidenza</h3>
					<p class="text-3xl font-black leading-none mb-2 relative z-10">{avvisiCriticiCount}</p>
					<p class="text-[9px] font-bold text-blue-200 uppercase leading-relaxed relative z-10">
						Totale criticità tra documenti e DPI rilevati.
					</p>
				</div>
			</div>
		</div>
	{/if}
</div>