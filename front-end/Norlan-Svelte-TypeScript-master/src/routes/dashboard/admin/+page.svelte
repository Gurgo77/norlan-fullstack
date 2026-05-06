<script lang="ts">
	import { onMount } from 'svelte';
	import { fade, scale } from 'svelte/transition';
	import { resolveRoute } from '$app/paths';
	import {
		Building2, FileClock, AlertCircle, LayoutDashboard,
		Users, GraduationCap, ArrowRight, UserSquare2, HardHat, FileText, CheckCircle2
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

	interface ScadenzaTabella {
		id: string;
		tipo: 'DOCUMENTO' | 'DPI';
		idAzienda: number;
		azienda: string;
		dettaglio: string;
		status: 'red' | 'yellow' | 'green';
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
		dpiTotali: 0
	});
	let scadenzeImminenti = $state<ScadenzaTabella[]>([]);
	let avvisiCriticiCount = $state(0);

	const dataOggi = new Intl.DateTimeFormat('it-IT', {
		weekday: 'long', day: 'numeric', month: 'long', year: 'numeric'
	}).format(new Date());

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
					return dpis.map((dpi: AssegnazioneDPIDTO) => {
						const dpiEsteso: DpiEsteso = {
							...dpi,
							nomeDipendente: `${d.nome} ${d.cognome}`,
							idAzienda: (d as DipendenteDTO & { idAzienda?: number }).idAzienda || 0
						};
						return dpiEsteso;
					});
				} catch {
					return [];
				}
			});

			const allDpis: DpiEsteso[] = (await Promise.all(dpiPromises)).flat();

			stats = {
				aziende: aziendeList.length,
				dipendenti: dipendentiList.length,
				docenti: docentiList.length,
				corsiAttivi: corsiList.filter(c => c.stato === 'PROGRAMMATO' || c.stato === 'IN_SVOLGIMENTO').length,
				dpiTotali: allDpis.length
			};

			const oggiMs = new Date().getTime();

			const calcolaStatusEStato = (giorniMancanti: number): { status: 'red' | 'yellow' | 'green', testo: string } => {
				if (giorniMancanti < 0) return { status: 'red', testo: 'SCADUTO' };
				if (giorniMancanti === 0) return { status: 'red', testo: 'SCADE OGGI' };
				if (giorniMancanti <= 7) return { status: 'red', testo: `SCADRÀ TRA ${giorniMancanti} GG` };
				if (giorniMancanti <= 30) return { status: 'yellow', testo: `SCADRÀ TRA ${giorniMancanti} GG` };
				return { status: 'green', testo: 'A POSTO' };
			};

			const scadenzeDocs: ScadenzaTabella[] = documentiList
					.filter(doc => doc.tipologia !== 'ATTESTATO_CORSO')
					.map(doc => {
						const dataScad = new Date(doc.dataScadenza).getTime();
						const diff = Math.ceil((dataScad - oggiMs) / (1000 * 3600 * 24));
						const calc = calcolaStatusEStato(diff);

						return {
							id: `doc_${doc.idDocumento}`,
							tipo: 'DOCUMENTO',
							idAzienda: doc.idAzienda,
							azienda: doc.ragioneSocialeAzienda,
							dettaglio: `${doc.tipologia.replace(/_/g, ' ')} - ${doc.modulo}`,
							status: calc.status,
							testoScadenza: calc.testo,
							dataScadenza: new Date(doc.dataScadenza).toLocaleDateString('it-IT'),
							timestampScadenza: dataScad,
							linkRedirect: resolveRoute('/dashboard/admin/scadenziario')
						};
					});

			const scadenzeDpi: ScadenzaTabella[] = allDpis.filter(dpi => {
				return dpi.dataScadenzaRevisione && dpi.dataScadenzaRevisione !== '9999-12-31';
			}).map(dpi => {
				const scadMs = new Date(dpi.dataScadenzaRevisione!).getTime();
				const diff = Math.ceil((scadMs - oggiMs) / (1000 * 3600 * 24));
				const calc = calcolaStatusEStato(diff);

				const az = aziendeList.find(a => String(a.idUtente) === String(dpi.idAzienda));

				return {
					id: `dpi_${dpi.idAssegnazione}`,
					tipo: 'DPI',
					idAzienda: az ? az.idUtente : 0,
					azienda: az ? az.ragioneSociale : 'Azienda N.D.',
					dettaglio: `${dpi.tipo!.replace(/_/g, ' ')} (${dpi.nomeDipendente})`,
					status: calc.status,
					testoScadenza: calc.testo,
					dataScadenza: new Date(dpi.dataScadenzaRevisione!).toLocaleDateString('it-IT'),
					timestampScadenza: scadMs,
					linkRedirect: resolveRoute('/dashboard/admin/dpi')
				};
			});

			const statusOrder = { 'red': 1, 'yellow': 2, 'green': 3 };

			scadenzeImminenti = [...scadenzeDocs, ...scadenzeDpi].sort((a, b) => {
				if (statusOrder[a.status] !== statusOrder[b.status]) {
					return statusOrder[a.status] - statusOrder[b.status];
				}
				return a.timestampScadenza - b.timestampScadenza;
			});

			avvisiCriticiCount = scadenzeImminenti.filter(s => s.status !== 'green').length;

		} catch (error) {
			console.error("Si è verificato un errore durante il caricamento dei dati della dashboard amministrativa:", error);
		} finally {
			isLoading = false;
		}
	});

	const scadenzeFiltrate = $derived(
			scadenzeImminenti.filter(s => s.azienda.toLowerCase().includes(searchState.query.toLowerCase()) ||
					s.dettaglio.toLowerCase().includes(searchState.query.toLowerCase()))
	);
</script>

<div in:fade class="pb-20 max-w-[1600px] mx-auto">
	<div class="mb-10 flex flex-col md:flex-row justify-between items-start md:items-end gap-4">
		<div>
			<div class="flex items-center gap-3 mb-2">
				<div class="p-2 bg-[#1B4B6B] rounded-xl text-white shadow-sm">
					<LayoutDashboard size={20} />
				</div>
				<p class="text-[10px] font-black text-gray-400 uppercase tracking-widest">{dataOggi}</p>
			</div>
			<h1 class="text-4xl font-extrabold text-[#1B4B6B] uppercase tracking-tighter">Benvenuto Admin</h1>
			<p class="text-gray-500 font-bold uppercase text-xs tracking-tighter mt-1">Centro di Controllo Globale NorLan.</p>
		</div>
	</div>

	{#if isLoading}
		<div class="flex flex-col justify-center items-center h-64 gap-4">
			<div class="animate-spin rounded-full h-12 w-12 border-b-4 border-[#1B4B6B]"></div>
			<span class="text-[10px] font-black uppercase tracking-widest text-gray-400">Analisi del sistema in corso...</span>
		</div>
	{:else}
		<div class="grid grid-cols-1 md:grid-cols-3 xl:grid-cols-5 gap-6 mb-12">
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

			<div in:scale={{duration: 200, delay: 200}} class="h-full">
				<StatCard titolo="Gestione DPI" valore={stats.dpiTotali} icona={HardHat} href={resolveRoute('/dashboard/admin/dpi')} />
			</div>
		</div>

		<div class="grid grid-cols-1 xl:grid-cols-3 gap-8">
			<div class="xl:col-span-2 bg-white rounded-3xl shadow-sm border border-gray-100 flex flex-col overflow-hidden">
				<div class="p-6 border-b border-gray-50 flex justify-between items-center bg-gray-50/30 shrink-0">
					<div class="flex items-center gap-3">
						<AlertCircle class="text-[#1B4B6B]" size={20} />
						<h2 class="font-black text-[#1B4B6B] uppercase text-sm tracking-tight">Panoramica Globale Scadenze</h2>
					</div>
				</div>

				<div class="p-6 space-y-4 max-h-[600px] overflow-y-auto custom-scrollbar-data">
					{#each scadenzeFiltrate as scadenza (scadenza.id)}
						<AlertCard
								titolo={scadenza.azienda}
								sottotitolo={scadenza.dettaglio}
								variante={scadenza.status === 'red' ? 'danger' : scadenza.status === 'yellow' ? 'warning' : 'success'}
								icona={scadenza.tipo === 'DOCUMENTO' ? FileText : HardHat}
								stato={scadenza.testoScadenza}
								data={scadenza.status !== 'green' ? scadenza.dataScadenza : ''}
								href={scadenza.linkRedirect}
						/>
					{:else}
						<div class="py-16 text-center">
							<CheckCircle2 size={32} class="mx-auto text-gray-200 mb-3" />
							<p class="text-gray-400 font-bold uppercase text-[10px] tracking-widest">Nessuna documentazione registrata.</p>
						</div>
					{/each}
				</div>
			</div>
			<div class="space-y-6">
				<div class="bg-[#1B4B6B] rounded-3xl p-8 text-white shadow-xl relative overflow-hidden">
					<div class="absolute -right-4 -top-4 opacity-10">
						<FileClock size={120} />
					</div>
					<h3 class="text-sm font-black uppercase tracking-widest mb-2 relative z-10">Allarmi in Evidenza</h3>
					<p class="text-4xl font-black leading-none mb-4 relative z-10">{avvisiCriticiCount}</p>
					<p class="text-[10px] font-bold text-blue-200 uppercase leading-relaxed relative z-10">
						Documenti o DPI scaduti e in scadenza a breve. Agisci tempestivamente per garantire la compliance.
					</p>
				</div>

				<a href={resolveRoute('/dashboard/admin/comunicazioni')} class="bg-white rounded-3xl p-6 border border-gray-100 shadow-sm flex items-center justify-between group hover:border-[#1B4B6B] hover:shadow-md transition-all cursor-pointer">
					<div>
						<h4 class="text-xs font-black text-[#1B4B6B] uppercase mb-1">Supporto Clienti</h4>
						<p class="text-[10px] font-bold text-gray-400 uppercase">Apri la Chat NorLan</p>
					</div>
					<div class="p-3 bg-[#1B4B6B]/10 text-[#1B4B6B] rounded-xl group-hover:bg-[#1B4B6B] group-hover:text-white transition-colors">
						<ArrowRight size={18} />
					</div>
				</a>
			</div>
		</div>
	{/if}
</div>