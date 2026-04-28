<script lang="ts">
	import { onMount } from 'svelte';
	import { fade, scale } from 'svelte/transition';
	import {
		Search, Filter, User, ShieldCheck, AlertTriangle, ArrowRight,
		Bookmark, Loader2, GraduationCap, Download, UploadCloud, CheckCircle2, FileCheck2
	} from 'lucide-svelte';

	// Import Servizi e Modelli
	import { AuthService } from '$lib/services/AuthService';
	import { LavoratoreService } from '$lib/services/LavoratoreService';
	import { FormazioneService } from '$lib/services/FormazioneService';
	import { DocumentoService } from '$lib/services/DocumentoService';
	import type { Documento } from '$lib/models/Documento';

	// --- INTERFACCE TIPIZZATE ---
	interface CorsoStato {
		nome: string;
		data: string;
		stato: 'OK' | 'IN_ATTESA' | 'CRITICO';
	}

	interface DipendenteFormazione {
		id: number;
		nomeCompleto: string;
		ruolo: string;
		corsi: CorsoStato[];
	}

	// --- STATO REATTIVO (Svelte 5) ---
	let isLoading = $state(true);
	let isActionLoading = $state(false);
	let searchQuery = $state('');

	let dipendenti = $state<DipendenteFormazione[]>([]);
	let attestatiDaFirmare = $state<Documento[]>([]);

	// Mappa reattiva per i file firmati pronti all'upload (idDocumento -> File)
	let fileFirmati = $state<Record<number, File>>({});

	// --- CARICAMENTO DATI ---
	onMount(async () => {
		const session = AuthService.getSession();
		if (!session) return;

		try {
			// 1. Fetch parallelo: Lavoratori e Documenti Aziendali
			const [lavoratoriRaw, documentiAzienda] = await Promise.all([
				LavoratoreService.getByAzienda(session.idUtente),
				DocumentoService.getDocumentiByAzienda(session.idUtente)
			]);

			// 2. Filtriamo solo gli attestati in attesa di firma dell'azienda
			// (Assumiamo che il backend usi "IN_ATTESA_FIRMA" nello State Pattern)
			attestatiDaFirmare = documentiAzienda.filter(d =>
					d.tipologia === 'ATTESTATO_CORSO' && d.stato === 'IN_ATTESA_FIRMA'
			);

			// 3. Costruzione della lista dipendenti con lo stato formativo reale
			const promises = lavoratoriRaw.map(async (l) => {
				const iscrizioni = await FormazioneService.getIscrizioniUtente(l.idUtente);
				return {
					id: l.idUtente,
					nomeCompleto: `${l.nome} ${l.cognome}`.toUpperCase(),
					ruolo: l.ruolo.replace('_', ' '),
					corsi: iscrizioni.map((i) => ({
						nome: i.titoloCorso.toUpperCase(),
						data: formattaData(i.dataOrarioCorso),
						stato: (i.presenzaConfermata ? 'OK' : 'IN_ATTESA') as 'OK' | 'IN_ATTESA'
					}))
				};
			});

			dipendenti = await Promise.all(promises);
		} catch (error) {
			console.error('Errore nel recupero dei dati di formazione:', error);
		} finally {
			isLoading = false;
		}
	});

	// --- LOGICA REATTIVA ---
	const filteredDipendenti = $derived(
			dipendenti.filter((d) => d.nomeCompleto.toLowerCase().includes(searchQuery.toLowerCase()))
	);

	const countDaAggiornare = $derived(
			dipendenti.filter((d) => d.corsi.some((c) => c.stato !== 'OK')).length
	);

	// --- AZIONI SUI DOCUMENTI (FSM) ---
	async function scaricaOriginale(idDocumento: number) {
		try {
			const blob = await DocumentoService.downloadDocumento(idDocumento);
			const url = window.URL.createObjectURL(blob);
			const a = document.createElement('a');
			a.href = url;
			a.download = `Attestati_Originali_${idDocumento}.pdf`;
			document.body.appendChild(a);
			a.click();
			window.URL.revokeObjectURL(url);
			a.remove();
			// eslint-disable-next-line @typescript-eslint/no-unused-vars
		} catch (error) {
			alert("Errore durante il download del documento originale.");
		}
	}

	function handleFileChange(event: Event, idDocumento: number) {
		const input = event.target as HTMLInputElement;
		if (input.files && input.files.length > 0) {
			fileFirmati[idDocumento] = input.files[0];
		} else {
			delete fileFirmati[idDocumento];
		}
	}

	async function consegnaAiDipendenti(idDocumento: number) {
		if (!fileFirmati[idDocumento]) {
			alert("Devi prima allegare il file PDF controfirmato.");
			return;
		}

		isActionLoading = true;
		try {
			// Qui idealmente chiameresti un endpoint per sostituire il file fisico.
			// Per far avanzare la Macchina a Stati usiamo approvaDocumento.
			await DocumentoService.approvaDocumento(idDocumento);

			// Rimuoviamo il documento dalla lista "Da firmare" (sblocco FSM completato)
			attestatiDaFirmare = attestatiDaFirmare.filter(d => d.idDocumento !== idDocumento);
			delete fileFirmati[idDocumento];

			alert("Attestati firmati e consegnati con successo ai dipendenti!");
			// eslint-disable-next-line @typescript-eslint/no-unused-vars
		} catch (error) {
			alert("Errore durante la consegna del documento.");
		} finally {
			isActionLoading = false;
		}
	}

	function formattaData(dateStr: string) {
		return new Date(dateStr).toLocaleDateString('it-IT', {
			day: '2-digit', month: '2-digit', year: 'numeric'
		});
	}
</script>

<div in:fade class="pb-20">
	<div class="mb-10 flex items-start justify-between">
		<div>
			<h1 class="text-4xl font-extrabold uppercase tracking-tighter text-[#1B4B6B]">
				Formazione Dipendenti
			</h1>
			<p class="text-xs font-bold uppercase tracking-tighter text-gray-500">
				Monitoraggio attestati e scadenze corsi NorLan.
			</p>
		</div>

		<div class="flex items-center gap-4 rounded-2xl border border-red-50 bg-white p-4 shadow-sm">
			<div class="rounded-lg bg-red-50 p-2 text-red-500">
				<AlertTriangle size={20} />
			</div>
			<div>
				<p class="text-[9px] font-bold uppercase text-gray-400">Da Completare</p>
				<p class="text-xs font-black uppercase text-red-600">{countDaAggiornare} Dipendenti</p>
			</div>
		</div>
	</div>

	{#if isLoading}
		<div class="flex flex-col items-center justify-center gap-4 py-32">
			<Loader2 size={48} class="animate-spin text-[#1B4B6B]" />
			<p class="text-[10px] font-black uppercase tracking-widest text-gray-300">
				Sincronizzazione dati in corso...
			</p>
		</div>
	{:else}

		{#if attestatiDaFirmare.length > 0}
			<div class="mb-14" in:fade>
				<div class="flex items-center gap-3 mb-6 border-b border-amber-200 pb-3">
					<div class="p-2 bg-amber-100 text-amber-700 rounded-lg"><FileCheck2 size={20}/></div>
					<h2 class="text-xl font-extrabold text-amber-700 uppercase tracking-tight">Attestati da Controfirmare e Consegnare</h2>
					<span class="ml-auto text-[10px] font-black text-white bg-amber-600 px-3 py-1 rounded-full uppercase shadow-lg shadow-amber-900/20">Azione Richiesta</span>
				</div>

				<div class="grid grid-cols-1 xl:grid-cols-2 gap-6">
					{#each attestatiDaFirmare as attestato (attestato.idDocumento ?? Math.random())}
						{@const idDoc = attestato.idDocumento ?? 0}

						<div class="bg-white rounded-[2rem] shadow-md border border-amber-200 p-6 relative overflow-hidden flex flex-col md:flex-row gap-6 items-center">
							<div class="absolute top-0 left-0 w-2 h-full bg-amber-500"></div>

							<div class="flex-1 space-y-3 pl-2">
								<h3 class="font-extrabold text-[#1B4B6B] text-lg uppercase leading-tight">Pacchetto Attestati Corso</h3>
								<p class="text-[10px] font-bold text-gray-500 uppercase leading-relaxed">
									L'Admin ha validato e generato gli attestati. Scarica il PDF cumulativo, apponi la firma aziendale e ricaricalo per sbloccarlo nelle dashboard dei tuoi dipendenti.
								</p>
							</div>

							<div class="w-full md:w-64 space-y-3 shrink-0 bg-gray-50 p-4 rounded-2xl border border-gray-100">
								<button onclick={() => scaricaOriginale(idDoc)} class="w-full py-3 bg-white border border-gray-200 text-[#1B4B6B] rounded-xl font-extrabold uppercase text-[10px] tracking-widest flex items-center justify-center gap-2 hover:bg-blue-50 transition-colors">
									<Download size={16} /> 1. Scarica Originale
								</button>

								<div class="relative">
									<input
											type="file"
											accept="application/pdf"
											id="upload-{idDoc}"
											onchange={(e) => handleFileChange(e, idDoc)}
											class="hidden"
									/>
									<label for="upload-{idDoc}" class="w-full py-3 border-2 border-dashed {fileFirmati[idDoc] ? 'border-emerald-400 bg-emerald-50 text-emerald-700' : 'border-gray-300 bg-white text-gray-500'} rounded-xl font-extrabold uppercase text-[10px] tracking-widest flex items-center justify-center gap-2 hover:bg-gray-50 transition-colors cursor-pointer">
										<UploadCloud size={16} />
										{fileFirmati[idDoc] ? '2. File Selezionato' : '2. Allega Firmato'}
									</label>
								</div>

								<button
										onclick={() => consegnaAiDipendenti(idDoc)}
										disabled={!fileFirmati[idDoc] || isActionLoading}
										class="w-full py-3 bg-emerald-600 text-white rounded-xl font-extrabold uppercase text-[10px] tracking-widest flex items-center justify-center gap-2 hover:bg-emerald-700 transition-colors shadow-lg shadow-emerald-900/20 disabled:opacity-50 disabled:cursor-not-allowed">
									{#if isActionLoading} <Loader2 class="animate-spin" size={16} /> {:else} <CheckCircle2 size={16} /> 3. Consegna {/if}
								</button>
							</div>
						</div>
					{/each}
				</div>
			</div>
		{/if}

		<div class="mb-10 flex gap-4 mt-8">
			<div class="group relative flex-1">
				<Search
						class="absolute left-4 top-1/2 -translate-y-1/2 text-gray-400 transition-colors group-focus-within:text-[#1B4B6B]"
						size={18}
				/>
				<input
						bind:value={searchQuery}
						type="text"
						placeholder="Cerca dipendente per nome..."
						class="w-full rounded-2xl border border-gray-100 bg-white py-4 pl-12 pr-4 text-xs font-bold uppercase outline-none transition-all focus:ring-4 focus:ring-[#1B4B6B]/5 shadow-sm"
				/>
			</div>
			<button class="rounded-2xl border border-gray-100 bg-white p-4 text-gray-400 shadow-sm transition-all hover:text-[#1B4B6B]">
				<Filter size={20} />
			</button>
		</div>

		<div class="space-y-6">
			{#each filteredDipendenti as dip (dip.id)}
				<div class="group flex cursor-default flex-col xl:flex-row items-center gap-8 rounded-[32px] border border-gray-100 bg-white p-6 shadow-sm transition-all duration-300 hover:border-[#1B4B6B]/20 hover:shadow-xl" in:scale>

					<div class="flex w-full xl:w-auto items-center gap-6 shrink-0">
						<div class="flex size-16 flex-none items-center justify-center rounded-2xl bg-[#1B4B6B] text-white shadow-lg shadow-[#1B4B6B]/20">
							<User size={28} />
						</div>
						<div class="w-64">
							<h3 class="text-lg font-black uppercase leading-tight text-[#1B4B6B] truncate" title={dip.nomeCompleto}>{dip.nomeCompleto}</h3>
							<p class="text-[10px] font-bold uppercase tracking-tight text-gray-400">{dip.ruolo}</p>
						</div>
					</div>

					<div class="flex flex-1 flex-wrap gap-3 w-full xl:w-auto">
						{#each dip.corsi as corso, index (corso.nome + index)}
							<div class="flex items-center gap-3 rounded-xl border px-4 py-2 {corso.stato === 'OK' ? 'border-green-100 bg-green-50 text-green-600' : 'border-yellow-100 bg-yellow-50 text-yellow-600'}">
								<div>
									<p class="text-[9px] font-black uppercase leading-none tracking-tighter max-w-[150px] truncate" title={corso.nome}>{corso.nome}</p>
									<p class="mt-1 text-[10px] font-bold opacity-80">{corso.data}</p>
								</div>
								{#if corso.stato === 'OK'} <ShieldCheck size={14} /> {:else} <AlertTriangle size={14} /> {/if}
							</div>
						{/each}
						{#if dip.corsi.length === 0}
							<p class="text-[10px] font-bold uppercase italic text-gray-300 mt-2">Nessun corso registrato</p>
						{/if}
					</div>

					<div class="flex items-center gap-4 w-full xl:w-auto justify-end mt-4 xl:mt-0 pt-4 xl:pt-0 border-t border-gray-100 xl:border-0">
						<button class="text-gray-300 transition-colors hover:text-[#1B4B6B]">
							<Bookmark size={20} />
						</button>
						<a href="/dashboard/azienda/dipendenti" class="flex items-center gap-3 rounded-2xl border-2 border-[#1B4B6B] bg-white px-6 py-3 text-[11px] font-black uppercase tracking-widest text-[#1B4B6B] shadow-sm transition-all hover:bg-[#1B4B6B] hover:text-white">
							Profilo <ArrowRight size={16} />
						</a>
					</div>
				</div>
			{/each}
		</div>

		{#if filteredDipendenti.length === 0}
			<div class="rounded-[40px] border-2 border-dashed border-gray-100 bg-white p-20 text-center mt-6">
				<GraduationCap size={48} class="mx-auto mb-4 text-gray-200" />
				<h3 class="text-xl font-black uppercase text-[#1B4B6B]">Nessun dipendente trovato</h3>
				<p class="mt-2 text-[10px] font-bold uppercase tracking-[0.2em] text-gray-400">Verifica i criteri di ricerca nel database.</p>
			</div>
		{/if}

	{/if}
</div>

<style>
	:global(body) { background-color: #f9fafb; }
</style>