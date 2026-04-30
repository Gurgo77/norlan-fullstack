<script lang="ts">
	import { onMount } from 'svelte';
	import { fade, scale } from 'svelte/transition';
	import {
		Building2, FileClock, AlertCircle, LayoutDashboard,
		Users, GraduationCap, ArrowRight, UserSquare2, ShieldAlert,
		HardHat, FileText, CheckCircle2
	} from 'lucide-svelte';
	import { searchState } from '$lib/searchState.svelte';

	// Servizi
	import { AnagraficaService } from '$lib/services/AnagraficaService';
	import { DocumentoService } from '$lib/services/DocumentoService';
	import { LavoratoreService } from '$lib/services/LavoratoreService';
	import { FormazioneService } from '$lib/services/FormazioneService';

	// Modelli e DTO rigorosi
	import type { Azienda } from '$lib/models/Azienda';
	import type { DipendenteDTO, AssegnazioneDPIDTO } from '$lib/services/LavoratoreService';
	import type { DocenteData } from '$lib/models/Docente';
	import type { CorsoFormazione } from '$lib/models/CorsoFormazione';
	import type { Documento } from '$lib/models/Documento';

	// Interfaccia per la tabella scadenze
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

	// Interfaccia per l'unione dei dati DPI con il nome del dipendente
	interface DpiEsteso extends AssegnazioneDPIDTO {
		nomeDipendente: string;
		idAzienda: number;
	}

	// Stato
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
			// Fetch parallelo tipizzato
			const [datiAziende, datiDipendenti, datiDocenti, datiCorsi, tuttiDocumenti] = await Promise.all([
				AnagraficaService.getAllAziende(),
				LavoratoreService.getAll(),
				AnagraficaService.getAllDocenti(),
				FormazioneService.getAllCorsi(),
				DocumentoService.getAllDocumenti()
			]);

			// Casting rigorosi ai tipi del tuo sistema
			const aziendeList = datiAziende as Azienda[];
			const dipendentiList = datiDipendenti as DipendenteDTO[];
			const docentiList = datiDocenti as DocenteData[];
			const corsiList = datiCorsi as CorsoFormazione[];
			const documentiList = tuttiDocumenti as Documento[];

			// Recuperiamo TUTTI i DPI in modo Type-Safe
			const dpiPromises = dipendentiList.map(async (d: DipendenteDTO) => {
				try {
					const dpis = await LavoratoreService.getDpiByLavoratore(d.idUtente);
					return dpis.map((dpi: AssegnazioneDPIDTO) => {
						const dpiEsteso: DpiEsteso = {
							...dpi,
							nomeDipendente: `${d.nome} ${d.cognome}`,
							// Usiamo l'idAzienda mappato nel DTO del backend o fallback a 0
							idAzienda: d.idAzienda || 0
						};
						return dpiEsteso;
					});
				} catch(e) {
					return [];
				}
			});

			const allDpis: DpiEsteso[] = (await Promise.all(dpiPromises)).flat();

			// 1. Popolamento Statistiche
			stats = {
				aziende: aziendeList.length,
				dipendenti: dipendentiList.length,
				docenti: docentiList.length,
				corsiAttivi: corsiList.filter(c => c.stato === 'PROGRAMMATO' || c.stato === 'IN_SVOLGIMENTO').length,
				dpiTotali: allDpis.length
			};

			const oggiMs = new Date().getTime();

			// 2. Helper per il Semaforo e il Testo Dinamico
			const calcolaStatusEStato = (giorniMancanti: number): { status: 'red' | 'yellow' | 'green', testo: string } => {
				if (giorniMancanti < 0) return { status: 'red', testo: 'SCADUTO' };
				if (giorniMancanti === 0) return { status: 'red', testo: 'SCADE OGGI' };
				if (giorniMancanti <= 7) return { status: 'red', testo: `SCADRÀ TRA ${giorniMancanti} GG` };
				if (giorniMancanti <= 30) return { status: 'yellow', testo: `SCADRÀ TRA ${giorniMancanti} GG` };
				return { status: 'green', testo: 'A POSTO' };
			};

			// 3. Elaborazione Scadenze: DOCUMENTI (escludiamo gli attestati)
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
							linkRedirect: '/dashboard/admin/scadenziario'
						};
					});

			// 4. Elaborazione Scadenze: DPI
			const scadenzeDpi: ScadenzaTabella[] = allDpis.filter(dpi => {
				// Utilizziamo dataScadenzaRevisione mappata dal DTO backend
				return dpi.dataScadenzaRevisione && dpi.dataScadenzaRevisione !== '9999-12-31';
			}).map(dpi => {
				const scadMs = new Date(dpi.dataScadenzaRevisione).getTime();
				const diff = Math.ceil((scadMs - oggiMs) / (1000 * 3600 * 24));
				const calc = calcolaStatusEStato(diff);

				const az = aziendeList.find(a => String(a.idUtente) === String(dpi.idAzienda));

				return {
					id: `dpi_${dpi.idAssegnazione}`,
					tipo: 'DPI',
					idAzienda: az ? az.idUtente : 0,
					azienda: az ? az.ragioneSociale : 'Azienda N.D.',
					dettaglio: `${dpi.tipo.replace(/_/g, ' ')} (${dpi.nomeDipendente})`,
					status: calc.status,
					testoScadenza: calc.testo,
					dataScadenza: new Date(dpi.dataScadenzaRevisione).toLocaleDateString('it-IT'),
					timestampScadenza: scadMs,
					linkRedirect: '/dashboard/admin/dpi'
				};
			});

			// 5. Uniamo e Ordiniamo
			const statusOrder = { 'red': 1, 'yellow': 2, 'green': 3 };

			scadenzeImminenti = [...scadenzeDocs, ...scadenzeDpi].sort((a, b) => {
				if (statusOrder[a.status] !== statusOrder[b.status]) {
					return statusOrder[a.status] - statusOrder[b.status];
				}
				return a.timestampScadenza - b.timestampScadenza;
			});

			// Contiamo i warning e critical
			avvisiCriticiCount = scadenzeImminenti.filter(s => s.status !== 'green').length;

		} catch (error) {
			console.error("Errore nel caricamento della dashboard:", error);
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
			<!-- Card Aziende -->
			<a href="/dashboard/admin/aziende" class="bg-white p-6 rounded-3xl shadow-sm border border-gray-100 flex flex-col justify-between group hover:shadow-xl hover:border-[#1B4B6B]/30 hover:-translate-y-1 transition-all cursor-pointer" in:scale={{duration: 200, delay: 0}}>
				<div class="flex justify-between items-start mb-4">
					<div class="p-4 bg-blue-50 text-blue-600 rounded-2xl group-hover:bg-blue-600 group-hover:text-white transition-colors">
						<Building2 size={24} />
					</div>
					<ArrowRight size={20} class="text-gray-300 group-hover:text-[#1B4B6B] transition-colors" />
				</div>
				<div>
					<h2 class="text-4xl font-black text-[#1B4B6B]">{stats.aziende}</h2>
					<p class="text-[10px] font-black text-gray-400 uppercase tracking-widest mt-1">Aziende Clienti</p>
				</div>
			</a>

			<!-- Card Dipendenti -->
			<a href="/dashboard/admin/dipendenti" class="bg-white p-6 rounded-3xl shadow-sm border border-gray-100 flex flex-col justify-between group hover:shadow-xl hover:border-purple-600/30 hover:-translate-y-1 transition-all cursor-pointer" in:scale={{duration: 200, delay: 50}}>
				<div class="flex justify-between items-start mb-4">
					<div class="p-4 bg-purple-50 text-purple-600 rounded-2xl group-hover:bg-purple-600 group-hover:text-white transition-colors">
						<Users size={24} />
					</div>
					<ArrowRight size={20} class="text-gray-300 group-hover:text-purple-600 transition-colors" />
				</div>
				<div>
					<h2 class="text-4xl font-black text-[#1B4B6B]">{stats.dipendenti}</h2>
					<p class="text-[10px] font-black text-gray-400 uppercase tracking-widest mt-1">Personale Censito</p>
				</div>
			</a>

			<!-- Card Docenti -->
			<a href="/dashboard/admin/docenti" class="bg-white p-6 rounded-3xl shadow-sm border border-gray-100 flex flex-col justify-between group hover:shadow-xl hover:border-teal-600/30 hover:-translate-y-1 transition-all cursor-pointer" in:scale={{duration: 200, delay: 100}}>
				<div class="flex justify-between items-start mb-4">
					<div class="p-4 bg-teal-50 text-teal-600 rounded-2xl group-hover:bg-teal-600 group-hover:text-white transition-colors">
						<UserSquare2 size={24} />
					</div>
					<ArrowRight size={20} class="text-gray-300 group-hover:text-teal-600 transition-colors" />
				</div>
				<div>
					<h2 class="text-4xl font-black text-[#1B4B6B]">{stats.docenti}</h2>
					<p class="text-[10px] font-black text-gray-400 uppercase tracking-widest mt-1">Docenti Attivi</p>
				</div>
			</a>

			<!-- Card Formazione -->
			<a href="/dashboard/admin/formazione" class="bg-white p-6 rounded-3xl shadow-sm border border-gray-100 flex flex-col justify-between group hover:shadow-xl hover:border-orange-500/30 hover:-translate-y-1 transition-all cursor-pointer" in:scale={{duration: 200, delay: 150}}>
				<div class="flex justify-between items-start mb-4">
					<div class="p-4 bg-orange-50 text-orange-600 rounded-2xl group-hover:bg-orange-600 group-hover:text-white transition-colors">
						<GraduationCap size={24} />
					</div>
					<ArrowRight size={20} class="text-gray-300 group-hover:text-orange-600 transition-colors" />
				</div>
				<div>
					<h2 class="text-4xl font-black text-[#1B4B6B]">{stats.corsiAttivi}</h2>
					<p class="text-[10px] font-black text-gray-400 uppercase tracking-widest mt-1">Corsi in Programma</p>
				</div>
			</a>

			<!-- Card DPI -->
			<a href="/dashboard/admin/dpi" class="bg-white p-6 rounded-3xl shadow-sm border border-gray-100 flex flex-col justify-between group hover:shadow-xl hover:border-green-600/30 hover:-translate-y-1 transition-all cursor-pointer" in:scale={{duration: 200, delay: 200}}>
				<div class="flex justify-between items-start mb-4">
					<div class="p-4 bg-green-50 text-green-600 rounded-2xl group-hover:bg-green-600 group-hover:text-white transition-colors">
						<HardHat size={24} />
					</div>
					<ArrowRight size={20} class="text-gray-300 group-hover:text-green-600 transition-colors" />
				</div>
				<div>
					<h2 class="text-4xl font-black text-[#1B4B6B]">{stats.dpiTotali}</h2>
					<p class="text-[10px] font-black text-gray-400 uppercase tracking-widest mt-1">DPI Assegnati</p>
				</div>
			</a>
		</div>

		<!-- Sezione Tabella: Scadenziario Misto -->
		<div class="grid grid-cols-1 xl:grid-cols-3 gap-8">
			<div class="xl:col-span-2 bg-white rounded-3xl shadow-sm border border-gray-100 flex flex-col overflow-hidden">
				<div class="p-6 border-b border-gray-50 flex justify-between items-center bg-gray-50/30 shrink-0">
					<div class="flex items-center gap-3">
						<AlertCircle class="text-[#1B4B6B]" size={20} />
						<h2 class="font-black text-[#1B4B6B] uppercase text-sm tracking-tight">Panoramica Globale Scadenze</h2>
					</div>
				</div>

				<div class="overflow-x-auto flex-1 max-h-[600px] overflow-y-auto custom-scrollbar-data">
					<table class="w-full text-left">
						<thead class="bg-white text-[10px] font-black text-gray-400 uppercase tracking-widest border-b border-gray-100 sticky top-0 z-10">
						<tr>
							<th class="px-6 py-4">Azienda</th>
							<th class="px-6 py-4">Dettaglio Pratica</th>
							<th class="px-6 py-4 text-center">Status</th>
							<th class="px-6 py-4 text-right">Azione</th>
						</tr>
						</thead>
						<tbody class="divide-y divide-gray-50">
						{#each scadenzeFiltrate as scadenza (scadenza.id)}
							<tr class="hover:bg-gray-50/50 transition-colors group">
								<td class="px-6 py-4 font-black text-[#1B4B6B] text-xs uppercase">
									<a href="/dashboard/admin/aziende?id={scadenza.idAzienda}" class="hover:text-blue-600 transition-colors">
										{scadenza.azienda}
									</a>
								</td>
								<td class="px-6 py-4">
									<div class="flex items-center gap-2">
										<div class="p-1.5 rounded-lg {scadenza.tipo === 'DOCUMENTO' ? 'bg-blue-50 text-blue-600' : 'bg-purple-50 text-purple-600'}">
											{#if scadenza.tipo === 'DOCUMENTO'}
												<FileText size={14} />
											{:else}
												<HardHat size={14} />
											{/if}
										</div>
										<div>
											<p class="font-bold text-[#1B4B6B] text-[11px] uppercase tracking-tight">{scadenza.dettaglio}</p>
											<div class="flex items-center gap-1.5 mt-0.5">
												<div class="w-2 h-2 rounded-full {
                                          scadenza.status === 'red' ? 'bg-red-500 animate-pulse shadow-[0_0_6px_rgba(239,68,68,0.8)]' :
                                          scadenza.status === 'yellow' ? 'bg-yellow-400' :
                                          'bg-green-500'
                                      }"></div>
												<p class="text-[8px] font-black text-gray-400 uppercase tracking-widest">{scadenza.tipo}</p>
											</div>
										</div>
									</div>
								</td>
								<td class="px-6 py-4 text-center">
									<div class="flex flex-col items-center gap-1">
                                <span class="text-[10px] font-black uppercase px-2.5 py-1.5 rounded-md w-fit {
                                    scadenza.status === 'red' ? 'bg-red-50 text-red-600 border border-red-100' :
                                    scadenza.status === 'yellow' ? 'bg-yellow-50 text-yellow-600 border border-yellow-100' :
                                    'bg-green-50 text-green-600 border border-green-100'
                                }">
                                   {scadenza.testoScadenza}
                                </span>
										{#if scadenza.status !== 'green'}
											<span class="text-[9px] font-bold text-gray-400">il {scadenza.dataScadenza}</span>
										{/if}
									</div>
								</td>
								<td class="px-6 py-4 text-right">
									<a href={scadenza.linkRedirect} class="inline-flex items-center gap-2 bg-white border-2 border-gray-200 text-gray-500 px-4 py-1.5 rounded-xl font-black text-[9px] uppercase hover:border-[#1B4B6B] hover:text-[#1B4B6B] transition-all">
										Gestisci
									</a>
								</td>
							</tr>
						{/each}
						{#if scadenzeFiltrate.length === 0}
							<tr>
								<td colspan="4" class="px-6 py-16 text-center">
									<CheckCircle2 size={32} class="mx-auto text-gray-200 mb-3" />
									<p class="text-gray-400 font-bold uppercase text-[10px] tracking-widest">Nessuna documentazione registrata.</p>
								</td>
							</tr>
						{/if}
						</tbody>
					</table>
				</div>
			</div>

			<!-- Colonna Destra: Quick Info -->
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

				<a href="/dashboard/admin/comunicazioni" class="bg-white rounded-3xl p-6 border border-gray-100 shadow-sm flex items-center justify-between group hover:border-[#1B4B6B] hover:shadow-md transition-all cursor-pointer">
					<div>
						<h4 class="text-xs font-black text-[#1B4B6B] uppercase mb-1">Supporto Clienti</h4>
						<p class="text-[10px] font-bold text-gray-400 uppercase">Apri la Chat NorLan</p>
					</div>
					<div class="p-3 bg-gray-50 text-gray-400 rounded-xl group-hover:bg-[#1B4B6B] group-hover:text-white transition-colors">
						<ArrowRight size={18} />
					</div>
				</a>
			</div>
		</div>
	{/if}
</div>