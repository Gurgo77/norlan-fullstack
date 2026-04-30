<script lang="ts">
	import { onMount } from 'svelte';
	import { fade, scale } from 'svelte/transition';
	import {
		BookOpen, Search, Filter, Clock, MapPin, Users, Loader2,
		CheckSquare, Play, CheckCircle2, Calendar, UserCheck, X, UploadCloud
	} from 'lucide-svelte';

	// IMPORT SERVIZI E MODELLI UFFICIALI
	import type { CorsoFormazione } from '$lib/models/CorsoFormazione';
	import { StatoCorso } from '$lib/models/Enums';
	import type { IscrizioneCorso } from '$lib/models/IscrizioneCorso';
	import { AuthService } from '$lib/services/AuthService';
	import { FormazioneService } from '$lib/services/FormazioneService';
	import httpClient from '$lib/api/httpClient';

	// STATO CON RUNE SVELTE 5
	let isLoading = $state(true);
	let corsi = $state<CorsoFormazione[]>([]);
	let queryRicerca = $state('');
	let filtroStato = $state<StatoCorso | ''>('');

	// STATI MODALE FIRMA
	let showModalFirma = $state(false);
	let isActionLoading = $state(false);
	let selectedCorso = $state<CorsoFormazione | null>(null);
	let iscrittiPresenti = $state<IscrizioneCorso[]>([]);

	// STATI MODALE UPLOAD MATERIALE
	let showModalMateriale = $state(false);
	let isUploadingMateriale = $state(false);
	let selectedCorsoMateriale = $state<CorsoFormazione | null>(null);
	let fileMateriale = $state<File | null>(null);
	let titoloMateriale = $state('');

	onMount(async () => {
		const session = AuthService.getSession();
		if (!session) return;

		try {
			const tuttiCorsi = await FormazioneService.getAllCorsi();
			corsi = tuttiCorsi.filter(c => c.idDocente === session.idUtente);
		} catch (error) {
			console.error("Errore durante il recupero dei corsi assegnati:", error);
		} finally {
			isLoading = false;
		}
	});

	// --- REGOLA 3: SPARIZIONE POST-FIRMA ---
	const corsiAttiviDocente = $derived(
			corsi.filter(c =>
					c.stato !== StatoCorso.VALIDATO &&
					c.stato !== StatoCorso.CERTIFICATO
			)
	);

	// LOGICA DI FILTRO REATTIVA SUI CORSI ATTIVI
	const corsiFiltrati = $derived(
			corsiAttiviDocente.filter(c => {
				const matchTesto = (c.titolo || '').toLowerCase().includes(queryRicerca.toLowerCase());
				const matchStato = filtroStato === '' || c.stato === filtroStato;
				return matchTesto && matchStato;
			})
	);

	// SEZIONI STRUTTURALI (FSM)
	const corsiDaFirmare = $derived(corsiFiltrati.filter(c => c.stato === StatoCorso.ATTESA_FIRMA_DOCENTE));
	const corsiConclusiAttesaAdmin = $derived(corsiFiltrati.filter(c => c.stato === StatoCorso.CONCLUSO));
	const corsiInSvolgimento = $derived(corsiFiltrati.filter(c => c.stato === StatoCorso.IN_SVOLGIMENTO));
	const corsiProgrammati = $derived(corsiFiltrati.filter(c => !c.stato || c.stato === StatoCorso.PROGRAMMATO));

	// AZIONI DI STATO
	async function cambiaStatoCorso(idCorso: number, nuovoStato: StatoCorso) {
		if (!confirm(`Sei sicuro di voler impostare il corso come: ${nuovoStato.replace('_', ' ')}?`)) return;

		try {
			await FormazioneService.updateStatoCorso(idCorso, nuovoStato);
			corsi = corsi.map(c => c.idCorso === idCorso ? { ...c, stato: nuovoStato } as CorsoFormazione : c);
		} catch (error: any) {
			console.error(error);
			alert(error.response?.data?.message || "Errore durante l'aggiornamento dello stato del corso.");
		}
	}

	// AZIONI MODALE MATERIALE
	function apriModaleMateriale(corso: CorsoFormazione) {
		selectedCorsoMateriale = corso;
		fileMateriale = null;
		titoloMateriale = '';
		showModalMateriale = true;
	}

	async function caricaMaterialeDidattico() {
		if (!selectedCorsoMateriale || !fileMateriale || !titoloMateriale.trim()) return;
		isUploadingMateriale = true;

		try {
			const formData = new FormData();
			formData.append('file', fileMateriale);
			// Corretto: il backend si aspetta esattamente "titoloDocumento"
			formData.append('titoloDocumento', titoloMateriale);

			// Corretto: rotta al plurale "/materiali" e rimosso l'header manuale
			await httpClient.post(`/api/formazione/corsi/${selectedCorsoMateriale.idCorso}/materiali`, formData, {
				headers: {
					'Content-Type': 'multipart/form-data'
				}
			});
			alert("Materiale didattico caricato con successo!");
			showModalMateriale = false;
		} catch (error) {
			console.error("Errore upload materiale:", error);
			alert("Errore durante il caricamento del materiale. Assicurati che il file sia valido.");
		} finally {
			isUploadingMateriale = false;
		}
	}

	// AZIONI MODALE FIRMA
	async function apriValidazioneRegistro(corso: CorsoFormazione) {
		selectedCorso = corso;
		isActionLoading = true;
		showModalFirma = true;
		iscrittiPresenti = [];

		try {
			const iscritti = await FormazioneService.getIscrizioniByCorso(corso.idCorso);
			iscrittiPresenti = iscritti.filter((i: IscrizioneCorso) => i.presenzaConfermata);
		} catch (error) {
			alert("Impossibile caricare il registro degli iscritti.");
			showModalFirma = false;
		} finally {
			isActionLoading = false;
		}
	}

	async function controfirmaRegistro() {
		if (!selectedCorso) return;
		isActionLoading = true;

		try {
			await FormazioneService.controfirmaRegistro(selectedCorso.idCorso);
			corsi = corsi.map(c => c.idCorso === selectedCorso!.idCorso ? { ...c, stato: StatoCorso.VALIDATO } as CorsoFormazione : c);
			alert("Registro controfirmato con successo. Il corso è ora archiviato e gestito dall'amministrazione.");
			showModalFirma = false;
		} catch (error) {
			alert("Errore durante la controfirma del registro.");
		} finally {
			isActionLoading = false;
		}
	}

	function formattaData(stringaIso: string) {
		if (!stringaIso) return 'Data non definita';
		const d = new Date(stringaIso);
		return d.toLocaleDateString('it-IT', { day: '2-digit', month: 'short', year: 'numeric', hour: '2-digit', minute: '2-digit' });
	}
</script>

<div in:fade class="pb-20">
	<div class="flex flex-col md:flex-row justify-between items-start md:items-center gap-6 mb-10">
		<div>
			<h1 class="text-4xl font-black text-[#1B4B6B] uppercase tracking-tighter">I Miei Corsi</h1>
			<p class="text-gray-400 font-bold uppercase text-[10px] tracking-widest mt-1">Gestione lezioni e firme registri</p>
		</div>
	</div>

	<div class="bg-white p-4 rounded-3xl shadow-sm border border-gray-100 flex flex-col lg:flex-row gap-4 mb-10">
		<div class="relative flex-1 group">
			<Search class="absolute left-4 top-1/2 -translate-y-1/2 text-gray-400 group-focus-within:text-[#1B4B6B] transition-colors" size={20} />
			<input
					bind:value={queryRicerca}
					type="text"
					placeholder="CERCA PER TITOLO CORSO..."
					class="w-full bg-gray-50 border-none rounded-2xl py-4 pl-12 pr-6 text-xs font-bold text-[#1B4B6B] placeholder:text-gray-300 focus:ring-4 focus:ring-[#1B4B6B]/5 transition-all uppercase outline-none"
			/>
		</div>
		<div class="relative min-w-[240px]">
			<Filter class="absolute left-4 top-1/2 -translate-y-1/2 text-gray-400" size={20} />
			<select
					bind:value={filtroStato}
					class="w-full bg-gray-50 border-none rounded-2xl py-4 pl-12 pr-10 text-xs font-bold text-[#1B4B6B] focus:ring-4 focus:ring-[#1B4B6B]/5 transition-all uppercase outline-none appearance-none cursor-pointer"
			>
				<option value="">TUTTI GLI STATI</option>
				<option value={StatoCorso.PROGRAMMATO}>PROGRAMMATI</option>
				<option value={StatoCorso.IN_SVOLGIMENTO}>IN SVOLGIMENTO</option>
				<option value={StatoCorso.ATTESA_FIRMA_DOCENTE}>DA FIRMARE</option>
			</select>
		</div>
	</div>

	{#if isLoading}
		<div class="py-32 flex flex-col items-center justify-center gap-4">
			<Loader2 size={48} class="animate-spin text-[#1B4B6B]" />
			<span class="text-[10px] font-black text-gray-400 uppercase tracking-[0.2em]">Caricamento dati...</span>
		</div>
	{:else}

		{#if corsiDaFirmare.length > 0}
			<div class="mb-14" in:fade>
				<div class="flex items-center gap-3 mb-6 border-b border-rose-200 pb-3">
					<div class="p-2 bg-rose-100 text-rose-700 rounded-lg"><UserCheck size={20}/></div>
					<h2 class="text-xl font-extrabold text-rose-700 uppercase tracking-tight">Registri da Controfirmare</h2>
				</div>

				<div class="grid grid-cols-1 md:grid-cols-2 xl:grid-cols-3 gap-6">
					{#each corsiDaFirmare as corso (corso.idCorso)}
						<div class="bg-white rounded-2xl shadow-sm border border-rose-200 flex flex-col h-full overflow-hidden relative">
							<div class="absolute top-0 left-0 w-full h-1.5 bg-rose-500"></div>
							<div class="p-6 flex-1 flex flex-col gap-3">
								<h3 class="font-extrabold text-[#1B4B6B] uppercase leading-tight">{corso.titolo}</h3>
								<p class="text-[10px] font-bold text-rose-600 uppercase">Validato da Admin - In attesa di tua firma legale</p>
							</div>
							<div class="p-4 bg-rose-50 border-t border-rose-100">
								<button onclick={() => apriValidazioneRegistro(corso)} class="w-full py-3 bg-rose-600 text-white rounded-xl font-bold uppercase text-[10px] tracking-widest flex items-center justify-center gap-2 hover:bg-rose-700 transition-colors">
									<CheckSquare size={14} /> Verifica e Controfirma
								</button>
							</div>
						</div>
					{/each}
				</div>
			</div>
		{/if}

		{#if corsiConclusiAttesaAdmin.length > 0}
			<div class="mb-14">
				<div class="flex items-center gap-3 mb-6 border-b border-gray-200 pb-3">
					<div class="p-2 bg-amber-100 text-amber-700 rounded-lg"><Clock size={20}/></div>
					<h2 class="text-xl font-extrabold text-[#1B4B6B] uppercase tracking-tight">Conclusi (Attesa Validazione Admin)</h2>
				</div>
				<div class="grid grid-cols-1 md:grid-cols-2 xl:grid-cols-3 gap-6">
					{#each corsiConclusiAttesaAdmin as corso (corso.idCorso)}
						<div class="bg-white rounded-2xl shadow-sm border border-gray-100 p-6 flex flex-col gap-2 relative overflow-hidden group hover:border-amber-200 transition-colors">
							<h3 class="font-extrabold text-[#1B4B6B] uppercase">{corso.titolo}</h3>
							<p class="text-[10px] font-bold text-gray-500 uppercase mt-2">Lezioni terminate. Il responsabile amministrativo deve confermare le presenze prima della tua firma.</p>
						</div>
					{/each}
				</div>
			</div>
		{/if}

		<div class="mb-14">
			<div class="flex items-center gap-3 mb-6 border-b border-gray-200 pb-3">
				<div class="p-2 bg-blue-100 text-blue-700 rounded-lg"><Loader2 size={20} class="animate-spin-slow"/></div>
				<h2 class="text-xl font-extrabold text-[#1B4B6B] uppercase tracking-tight">In Svolgimento</h2>
			</div>
			{#if corsiInSvolgimento.length === 0}
				<div class="p-8 border-2 border-dashed border-gray-200 rounded-2xl text-center text-gray-400 font-bold uppercase text-xs">Nessun corso in aula al momento.</div>
			{:else}
				<div class="grid grid-cols-1 xl:grid-cols-2 2xl:grid-cols-3 gap-6">
					{#each corsiInSvolgimento as corso (corso.idCorso)}
						<div class="bg-white rounded-2xl shadow-sm border border-blue-200 p-6 flex flex-col relative overflow-hidden">
							<div class="absolute top-0 left-0 w-1.5 h-full bg-blue-500"></div>
							<div class="flex-1">
								<h3 class="font-extrabold text-[#1B4B6B] uppercase mb-4 ml-2">{corso.titolo}</h3>
								<div class="space-y-2 ml-2">
									<div class="flex items-center gap-2 text-xs font-bold text-gray-500"><MapPin size={14} /> {corso.luogoFisico}</div>
									<div class="flex items-center gap-2 text-xs font-bold text-gray-500"><Users size={14} /> {corso.capacitaMassima || 0} Iscritti</div>
								</div>
							</div>

							<div class="mt-6 flex flex-col gap-2">
								<button onclick={() => apriModaleMateriale(corso)} class="w-full py-2.5 bg-blue-50 text-blue-700 border border-blue-200 rounded-xl font-bold uppercase text-[10px] tracking-widest flex items-center justify-center gap-2 hover:bg-blue-100 transition-colors">
									<UploadCloud size={14} /> Carica Materiale
								</button>
								<button onclick={() => cambiaStatoCorso(corso.idCorso, StatoCorso.CONCLUSO)} class="w-full py-2.5 bg-[#1B4B6B] text-white rounded-xl font-bold uppercase text-[10px] tracking-widest flex items-center justify-center gap-2 hover:bg-blue-800 transition-colors shadow-lg shadow-blue-900/10">
									<CheckCircle2 size={14} /> Concludi Corso
								</button>
							</div>

						</div>
					{/each}
				</div>
			{/if}
		</div>

		<div class="mb-14">
			<div class="flex items-center gap-3 mb-6 border-b border-gray-200 pb-3">
				<div class="p-2 bg-gray-100 text-gray-700 rounded-lg"><Calendar size={20}/></div>
				<h2 class="text-xl font-extrabold text-[#1B4B6B] uppercase tracking-tight">Programmati (Futuri)</h2>
			</div>
			{#if corsiProgrammati.length === 0}
				<div class="p-8 border-2 border-dashed border-gray-200 rounded-2xl text-center text-gray-400 font-bold uppercase text-xs">Nessun corso futuro assegnato.</div>
			{:else}
				<div class="grid grid-cols-1 md:grid-cols-2 xl:grid-cols-3 gap-6">
					{#each corsiProgrammati as corso (corso.idCorso)}
						<div class="bg-white rounded-2xl shadow-sm border border-gray-100 p-5 flex flex-col hover:border-[#1B4B6B]/30 transition-all">
							<h3 class="font-extrabold text-[#1B4B6B] text-sm uppercase mb-3 line-clamp-2">{corso.titolo}</h3>
							<div class="flex-1 space-y-2 mb-4">
								<p class="text-[10px] font-bold text-gray-500 flex items-center gap-1.5"><Clock size={12}/> {formattaData(corso.dataOrario)}</p>
								<p class="text-[10px] font-bold text-gray-500 flex items-center gap-1.5"><MapPin size={12}/> {corso.luogoFisico}</p>
							</div>

							<div class="mt-4 flex flex-col gap-2">
								<button disabled class="w-full py-2.5 bg-gray-50 text-gray-400 border border-gray-100 rounded-xl font-bold uppercase text-[10px] tracking-widest flex items-center justify-center gap-2 cursor-not-allowed disabled:opacity-50">
									<UploadCloud size={14} /> Carica Materiale
								</button>
								<button
										onclick={() => cambiaStatoCorso(corso.idCorso, StatoCorso.IN_SVOLGIMENTO)}
										disabled={!!corso.stato && corso.stato !== StatoCorso.PROGRAMMATO}
										class="w-full py-2.5 border-2 border-[#1B4B6B] text-[#1B4B6B] rounded-xl font-bold uppercase text-[10px] tracking-widest flex items-center justify-center gap-2 hover:bg-[#1B4B6B] hover:text-white transition-colors disabled:opacity-50 disabled:cursor-not-allowed"
								>
									<Play size={14} fill="currentColor" /> Inizia Corso
								</button>
							</div>
						</div>
					{/each}
				</div>
			{/if}
		</div>

	{/if}
</div>

{#if showModalMateriale}
	<div class="fixed inset-0 z-[100] flex items-center justify-center p-4 bg-black/60 backdrop-blur-sm" in:fade>
		<div class="bg-white w-full max-w-lg rounded-3xl shadow-2xl flex flex-col" in:scale>
			<div class="bg-[#1B4B6B] p-6 text-white flex justify-between items-center rounded-t-3xl">
				<h2 class="text-lg font-extrabold uppercase flex items-center gap-2"><UploadCloud size={20}/> Materiale Didattico</h2>
				<button onclick={() => showModalMateriale = false} class="hover:text-red-400"><X size={24} /></button>
			</div>

			<div class="p-8 space-y-6">
				<div>
					<label class="block text-[10px] font-bold text-[#1B4B6B] uppercase mb-1">Titolo Documento *</label>
					<input
							bind:value={titoloMateriale}
							type="text"
							placeholder="Es. Slide Lezione 1"
							class="w-full p-3 bg-gray-50 border border-gray-200 rounded-xl text-sm font-bold uppercase focus:outline-none focus:ring-2 focus:ring-[#1B4B6B]/20"
					/>
				</div>

				<div class="border-2 border-dashed border-gray-200 rounded-2xl p-8 text-center bg-gray-50 group hover:bg-gray-100 transition-colors relative cursor-pointer">
					<input
							type="file"
							onchange={(e) => fileMateriale = e.currentTarget.files?.[0] || null}
							class="absolute inset-0 w-full h-full opacity-0 cursor-pointer"
					/>
					<div class="flex flex-col items-center gap-3 pointer-events-none">
						<div class="p-4 bg-white rounded-full text-[#1B4B6B] shadow-sm">
							<UploadCloud size={24} />
						</div>
						{#if fileMateriale}
							<span class="text-xs font-black text-[#1B4B6B] truncate w-full px-4">{fileMateriale.name}</span>
							<span class="text-[9px] font-bold text-green-500 uppercase tracking-widest">Pronto per il caricamento</span>
						{:else}
							<span class="text-[10px] font-black text-gray-400 uppercase tracking-widest">Clicca o trascina il file qui</span>
						{/if}
					</div>
				</div>
			</div>

			<div class="p-6 bg-gray-50 rounded-b-3xl border-t border-gray-100 flex gap-4">
				<button onclick={() => showModalMateriale = false} class="flex-1 py-3 text-gray-400 font-extrabold rounded-xl border border-gray-200 hover:bg-white uppercase text-[10px] transition-colors">Annulla</button>
				<button
						onclick={caricaMaterialeDidattico}
						disabled={isUploadingMateriale || !fileMateriale || !titoloMateriale.trim()}
						class="flex-1 py-3 bg-[#1B4B6B] text-white font-extrabold rounded-xl hover:bg-[#153a54] uppercase text-[10px] shadow-lg shadow-blue-900/20 disabled:opacity-50 disabled:cursor-not-allowed flex items-center justify-center gap-2 transition-colors"
				>
					{#if isUploadingMateriale}
						<Loader2 class="animate-spin" size={16} /> Attendi...
					{:else}
						Conferma Upload
					{/if}
				</button>
			</div>
		</div>
	</div>
{/if}

{#if showModalFirma}
	<div class="fixed inset-0 z-[100] flex items-center justify-center p-4 bg-black/60 backdrop-blur-sm" in:fade>
		<div class="bg-white w-full max-w-2xl rounded-3xl shadow-2xl flex flex-col max-h-[90vh]" in:scale>
			<div class="bg-[#1B4B6B] p-6 text-white flex justify-between items-center rounded-t-3xl">
				<h2 class="text-lg font-extrabold uppercase">Firma Registro Didattico</h2>
				<button onclick={() => showModalFirma = false} class="hover:text-red-400"><X size={24} /></button>
			</div>

			<div class="p-6 overflow-y-auto custom-scrollbar flex-1 bg-gray-50/30">
				{#if isActionLoading && iscrittiPresenti.length === 0}
					<div class="py-10 text-center"><Loader2 class="animate-spin mx-auto text-[#1B4B6B]" size={32} /></div>
				{:else}
					<div class="bg-blue-50 border border-blue-100 p-4 rounded-xl mb-6 flex gap-3">
						<BookOpen class="text-blue-600 shrink-0" size={20} />
						<p class="text-xs font-bold text-blue-800 uppercase">Questa è la lista definitiva dei dipendenti che la segreteria ha accertato essere stati presenti in aula. Conferma la validità dell'elenco per sbloccare il rilascio degli attestati.</p>
					</div>

					<h3 class="text-[10px] font-black text-gray-400 uppercase tracking-widest mb-3">Elenco Allievi Presenti</h3>
					<div class="space-y-2">
						{#each iscrittiPresenti as isc}
							<div class="flex items-center justify-between p-4 bg-white border border-gray-200 rounded-xl">
								<div class="flex items-center gap-3">
									<div class="w-8 h-8 rounded-full bg-emerald-100 flex items-center justify-center text-emerald-600"><CheckCircle2 size={16} /></div>
									<p class="text-sm font-extrabold text-[#1B4B6B] uppercase">{isc.emailUtente}</p>
								</div>
								<span class="text-[10px] font-bold text-gray-400 uppercase">Id: #{isc.idUtente}</span>
							</div>
						{:else}
							<div class="p-6 text-center border-2 border-dashed border-gray-200 rounded-xl">
								<p class="text-xs font-bold text-gray-400 uppercase">Nessun dipendente validato dall'admin.</p>
							</div>
						{/each}
					</div>
				{/if}
			</div>

			<div class="p-6 bg-white rounded-b-3xl border-t border-gray-100 flex gap-4">
				<button onclick={() => showModalFirma = false} disabled={isActionLoading} class="flex-1 py-4 text-gray-400 font-extrabold rounded-xl border border-gray-200 hover:bg-gray-50 uppercase text-[10px] disabled:opacity-50">Annulla</button>
				<button onclick={controfirmaRegistro} disabled={isActionLoading || iscrittiPresenti.length === 0} class="flex-1 py-4 bg-emerald-600 text-white font-extrabold rounded-xl hover:bg-emerald-700 uppercase text-[10px] disabled:opacity-50 flex items-center justify-center gap-2 shadow-lg shadow-emerald-900/20">
					{#if isActionLoading} <Loader2 class="animate-spin" size={16} /> Attendi... {:else} Apponi Firma Elettronica {/if}
				</button>
			</div>
		</div>
	</div>
{/if}

<style>
	:global(body) { background-color: #F9FAFB; }
	.custom-scrollbar::-webkit-scrollbar { width: 5px; }
	.custom-scrollbar::-webkit-scrollbar-thumb { background: #E2E8F0; border-radius: 10px; }
	.animate-spin-slow { animation: spin 3s linear infinite; }
</style>