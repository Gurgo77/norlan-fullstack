<script lang="ts">
	import { onMount } from 'svelte';
	import { fade, scale } from 'svelte/transition';
	import {
		X, Search, Calendar, Loader2, CheckSquare, UploadCloud, CheckCircle2, Clock, BarChart3, Star, BookOpen, Play, AlertTriangle, Send, FileText, Download
	} from 'lucide-svelte';

	import type { CorsoFormazione } from '$lib/models/CorsoFormazione';
	import { StatoCorso } from '$lib/models/Enums';
	import type { IscrizioneCorso } from '$lib/models/IscrizioneCorso';
	import { AuthService } from '$lib/services/AuthService';
	import { FormazioneService } from '$lib/services/FormazioneService';
	import { FeedbackService } from '$lib/services/FeedbackService';
	import httpClient from '$lib/api/httpClient';

	interface CorsoFormazioneEsteso extends CorsoFormazione {
		numeroIscritti?: number;
		isLoadingIscritti?: boolean;
	}

	interface FeedbackStatsDTO {
		idCorso: number;
		mediaDocenza: number;
		mediaContenuti: number;
		totaleFeedback: number;
		commenti: string[];
	}

	let isLoading = $state(true);
	let corsi = $state<CorsoFormazioneEsteso[]>([]);
	let queryRicerca = $state('');
	let filtroStato = $state<StatoCorso | ''>('');
	let showModalFirma = $state(false);
	let showModalMateriale = $state(false);
	let showModalFeedback = $state(false);
	let actionSuccess = $state<{type: 'OK' | 'ERR', msg: string} | null>(null);
	let showCambioStatoModal = $state(false);
	let corsoDaCambiareStato = $state<CorsoFormazioneEsteso | null>(null);
	let nuovoStatoPrevisto = $state<StatoCorso | null>(null);
	let isActionLoading = $state(false);
	let selectedCorso = $state<CorsoFormazioneEsteso | null>(null);
	let iscrittiPresenti = $state<IscrizioneCorso[]>([]);
	let isUploadingMateriale = $state(false);
	let selectedCorsoMateriale = $state<CorsoFormazione | null>(null);
	let fileMateriale = $state<File | null>(null);
	let titoloMateriale = $state('');
	let isLoadingFeedback = $state(false);
	let statsFeedback = $state<FeedbackStatsDTO | null>(null);
	let materialiCaricati = $state<any[]>([]);
	let isLoadingMateriali = $state(false);

	onMount(async () => {
		const session = AuthService.getSession();
		if (!session) return;

		try {
			const tuttiCorsi = await FormazioneService.getAllCorsi();
			const mieiCorsi = tuttiCorsi.filter(c => c.idDocente === session.idUtente);

			corsi = mieiCorsi.map(c => ({
				...c,
				numeroIscritti: 0,
				isLoadingIscritti: true
			}));

			for (let i = 0; i < corsi.length; i++) {
				try {
					const iscritti = await FormazioneService.getIscrizioniByCorso(corsi[i].idCorso);
					corsi[i].numeroIscritti = iscritti.length;
				} catch{
					console.warn("Errore caricamento iscritti per corso", corsi[i].idCorso);
				} finally {
					corsi[i].isLoadingIscritti = false;
				}
			}
		} catch (error) {
			console.error("Errore durante il recupero dei corsi assegnati:", error);
		} finally {
			isLoading = false;
		}
	});

	const corsiAttiviDocente = $derived(
			corsi.filter(c => c.stato !== StatoCorso.VALIDATO && c.stato !== StatoCorso.CERTIFICATO)
	);

	const corsiFiltrati = $derived(
			corsiAttiviDocente.filter(c => {
				const matchTesto = (c.titolo || '').toLowerCase().includes(queryRicerca.toLowerCase());
				const matchStato = filtroStato === '' || c.stato === filtroStato;
				return matchTesto && matchStato;
			})
	);

	const corsiDaFirmare = $derived(corsiFiltrati.filter(c => c.stato === StatoCorso.ATTESA_FIRMA_DOCENTE));
	const corsiConclusiAttesaAdmin = $derived(corsiFiltrati.filter(c => c.stato === StatoCorso.CONCLUSO));
	const corsiInSvolgimento = $derived(corsiFiltrati.filter(c => c.stato === StatoCorso.IN_SVOLGIMENTO));
	const corsiProgrammati = $derived(corsiFiltrati.filter(c => !c.stato || c.stato === StatoCorso.PROGRAMMATO));

	const corsiArchiviati = $derived(
			corsi.filter(c => (c.stato === StatoCorso.VALIDATO || c.stato === StatoCorso.CERTIFICATO) && (c.titolo || '').toLowerCase().includes(queryRicerca.toLowerCase()))
	);

	function triggerBanner(msg: string, type: 'OK' | 'ERR' = 'OK') {
		actionSuccess = { type, msg };
		setTimeout(() => actionSuccess = null, 4000);
	}

	async function apriStatisticheFeedback(corso: CorsoFormazioneEsteso) {
		selectedCorso = corso;
		showModalFeedback = true;
		isLoadingFeedback = true;
		statsFeedback = null;
		try {
			statsFeedback = await FeedbackService.getStatisticheCorso(corso.idCorso);
		} catch{
			triggerBanner("Errore recupero feedback", 'ERR');
			showModalFeedback = false;
		} finally {
			isLoadingFeedback = false;
		}
	}

	function preparaCambioStatoCorso(corso: CorsoFormazioneEsteso, nuovoStato: StatoCorso) {
		corsoDaCambiareStato = corso;
		nuovoStatoPrevisto = nuovoStato;
		showCambioStatoModal = true;
	}

	async function confermaCambioStatoCorso() {
		if (!corsoDaCambiareStato || !nuovoStatoPrevisto) return;

		isActionLoading = true;
		try {
			await FormazioneService.updateStatoCorso(corsoDaCambiareStato.idCorso, nuovoStatoPrevisto);
			corsi = corsi.map(c =>
					c.idCorso === corsoDaCambiareStato?.idCorso
							? { ...c, stato: nuovoStatoPrevisto as StatoCorso }
							: c
			);

			showCambioStatoModal = false;
			triggerBanner("Stato corso aggiornato!");
		} catch {
			triggerBanner("Errore durante il cambio stato", 'ERR');
		} finally {
			isActionLoading = false;
		}
	}

	async function apriModaleMateriale(corso: CorsoFormazione) {
		selectedCorsoMateriale = corso;
		fileMateriale = null;
		titoloMateriale = '';
		materialiCaricati = [];
		showModalMateriale = true;
		isLoadingMateriali = true;

		try {
			const res = await httpClient.get(`/api/formazione/corsi/${corso.idCorso}/materiali`);
			materialiCaricati = res.data;
		} catch {
			console.error("Errore nel recupero materiali");
		} finally {
			isLoadingMateriali = false;
		}
	}

	async function scaricaMateriale(idMateriale: number, nome: string) {
		try {
			const res = await httpClient.get(`/api/formazione/materiali/${idMateriale}/download`, { responseType: 'blob' });
			const url = window.URL.createObjectURL(new Blob([res.data]));
			const a = document.createElement('a');
			a.href = url;
			a.download = nome.endsWith('.pdf') ? nome : nome + ".pdf";
			a.click();
			window.URL.revokeObjectURL(url);
		} catch {
			triggerBanner("Errore nel download del file", 'ERR');
		}
	}

	async function caricaMaterialeDidattico() {
		if (!selectedCorsoMateriale || !fileMateriale || !titoloMateriale.trim()) return;
		isUploadingMateriale = true;
		try {
			const formData = new FormData();
			formData.append('file', fileMateriale);
			formData.append('titoloDocumento', titoloMateriale);
			await httpClient.post(`/api/formazione/corsi/${selectedCorsoMateriale.idCorso}/materiali`, formData, {
				headers: { 'Content-Type': 'multipart/form-data' }
			});

			triggerBanner("Materiale didattico caricato con successo!");

			const res = await httpClient.get(`/api/formazione/corsi/${selectedCorsoMateriale.idCorso}/materiali`);
			materialiCaricati = res.data;

			fileMateriale = null;
			titoloMateriale = '';
		} catch {
			triggerBanner("Errore durante l'upload", 'ERR');
		} finally {
			isUploadingMateriale = false;
		}
	}

	async function apriValidazioneRegistro(corso: CorsoFormazioneEsteso) {
		selectedCorso = corso;
		isActionLoading = true;
		showModalFirma = true;
		try {
			const iscritti = await FormazioneService.getIscrizioniByCorso(corso.idCorso);
			iscrittiPresenti = iscritti.filter((i: IscrizioneCorso) => i.presenzaConfermata);
		} catch { showModalFirma = false; } finally { isActionLoading = false; }
	}

	async function controfirmaRegistro() {
		if (!selectedCorso) return;
		isActionLoading = true;
		try {
			await FormazioneService.controfirmaRegistro(selectedCorso.idCorso);
			corsi = corsi.map(c => c.idCorso === selectedCorso!.idCorso ? { ...c, stato: StatoCorso.VALIDATO } : c);
			showModalFirma = false;
			triggerBanner("Registro firmato elettronicamente!");
		} catch {
			triggerBanner("Errore durante la firma", 'ERR');
		} finally {
			isActionLoading = false;
		}
	}

	function formattaData(iso: string) {
		if (!iso) return 'N.D.';
		return new Date(iso).toLocaleDateString('it-IT', { day: '2-digit', month: 'short', year: 'numeric' });
	}
</script>

<div in:fade class="pb-20 max-w-7xl mx-auto">
	{#if actionSuccess}
		<div class="fixed top-24 right-8 z-[250] {actionSuccess.type === 'OK' ? 'bg-emerald-600' : 'bg-red-600'} text-white px-6 py-4 rounded-2xl shadow-2xl flex items-center gap-4 border border-white/20 transition-all" in:scale out:fade>
			{#if actionSuccess.type === 'OK'}<CheckCircle2 size={24} />{:else}<AlertTriangle size={24} />{/if}
			<p class="text-sm font-black uppercase tracking-tight">{actionSuccess.msg}</p>
		</div>
	{/if}
	<div class="mb-10">
		<h1 class="text-4xl font-black text-[#1B4B6B] uppercase tracking-tighter">Pannello Docenza</h1>
		<p class="text-gray-400 font-bold uppercase text-[10px] tracking-widest mt-1">Gestione lezioni e analisi qualità</p>
	</div>
	<div class="bg-white p-4 rounded-3xl shadow-sm border border-gray-100 flex flex-col lg:flex-row gap-4 mb-10">
		<div class="relative flex-1">
			<Search class="absolute left-4 top-1/2 -translate-y-1/2 text-gray-400" size={20} />
			<input bind:value={queryRicerca} type="text" placeholder="CERCA CORSO..." class="w-full bg-gray-50 border-none rounded-2xl py-4 pl-12 pr-6 text-xs font-bold uppercase outline-none focus:ring-4 focus:ring-[#1B4B6B]/5" />
		</div>
	</div>
	{#if isLoading}
		<div class="py-32 text-center"><Loader2 size={48} class="animate-spin text-[#1B4B6B] mx-auto" /></div>
	{:else}
		{#if corsiDaFirmare.length > 0}
			<div class="mb-14">
				<h2 class="text-xl font-extrabold text-[#1B4B6B] uppercase mb-6 flex items-center gap-3"><CheckSquare size={24}/> Registri da Firmare</h2>
				<div class="grid grid-cols-1 md:grid-cols-2 xl:grid-cols-3 gap-6">
					{#each corsiDaFirmare as corso (corso.idCorso)}
						<div class="bg-white rounded-2xl border-2 border-[#1B4B6B]/20 p-6 flex flex-col gap-4 shadow-sm">
							<h3 class="font-black text-[#1B4B6B] uppercase leading-tight">{corso.titolo}</h3>
							<button onclick={() => apriValidazioneRegistro(corso)} class="w-full py-3 bg-[#1B4B6B] text-white rounded-xl font-bold uppercase text-[10px] flex items-center justify-center gap-2 hover:bg-[#153a54] transition-colors">
								<CheckSquare size={14} /> Firma Registro
							</button>
							<button onclick={() => apriStatisticheFeedback(corso)} class="w-full py-3 bg-[#1B4B6B]/5 text-[#1B4B6B] rounded-xl font-bold uppercase text-[10px] flex items-center justify-center gap-2 hover:bg-[#1B4B6B]/10 transition-colors">
								<BarChart3 size={14} /> Vedi Feedback
							</button>
						</div>
					{/each}
				</div>
			</div>
		{/if}
		{#if corsiConclusiAttesaAdmin.length > 0}
			<div class="mb-14">
				<h2 class="text-xl font-extrabold text-[#1B4B6B] uppercase mb-6 flex items-center gap-3"><Clock size={24}/> Attesa Admin</h2>
				<div class="grid grid-cols-1 md:grid-cols-2 xl:grid-cols-3 gap-6">
					{#each corsiConclusiAttesaAdmin as corso (corso.idCorso)}
						<div class="bg-white rounded-2xl border border-gray-100 p-6 flex flex-col gap-4 shadow-sm">
							<h3 class="font-black text-[#1B4B6B] uppercase leading-tight">{corso.titolo}</h3>
							<p class="text-[10px] font-bold text-gray-500 uppercase">Lezioni terminate. In attesa di validazione presenze.</p>
							<button onclick={() => apriStatisticheFeedback(corso)} class="w-full py-3 bg-[#1B4B6B]/5 text-[#1B4B6B] rounded-xl font-bold uppercase text-[10px] flex items-center justify-center gap-2 hover:bg-[#1B4B6B]/10 transition-colors">
								<BarChart3 size={14} /> Vedi Feedback
							</button>
						</div>
					{/each}
				</div>
			</div>
		{/if}
		<div class="mb-14">
			<h2 class="text-xl font-extrabold text-blue-700 uppercase mb-6 flex items-center gap-3"><Play size={24}/> Corsi In Svolgimento</h2>
			{#if corsiInSvolgimento.length === 0}
				<div class="p-8 border-2 border-dashed border-gray-200 rounded-2xl text-center text-gray-400 font-bold uppercase text-xs italic">Nessun corso attivo al momento.</div>
			{:else}
				<div class="grid grid-cols-1 md:grid-cols-2 gap-6">
					{#each corsiInSvolgimento as corso (corso.idCorso)}
						<div class="bg-white rounded-2xl border border-blue-100 p-6 shadow-sm">
							<h3 class="font-black text-[#1B4B6B] uppercase text-lg mb-4">{corso.titolo}</h3>
							<div class="flex gap-2">
								<button onclick={() => apriModaleMateriale(corso)} class="flex-1 py-3 bg-blue-50 text-blue-700 rounded-xl font-bold uppercase text-[10px] flex items-center justify-center gap-2 hover:bg-blue-100 transition-colors">
									<UploadCloud size={14} /> Materiale
								</button>
								<button onclick={() => preparaCambioStatoCorso(corso, StatoCorso.CONCLUSO)} class="flex-1 py-3 bg-[#1B4B6B] text-white rounded-xl font-bold uppercase text-[10px] flex items-center justify-center gap-2 hover:bg-[#153a54] transition-colors">
									<CheckCircle2 size={14} /> Concludi
								</button>
							</div>
						</div>
					{/each}
				</div>
			{/if}
		</div>
		<div class="mb-14">
			<h2 class="text-xl font-extrabold text-gray-700 uppercase mb-6 flex items-center gap-3"><Calendar size={24}/> Corsi Programmati</h2>
			{#if corsiProgrammati.length === 0}
				<div class="p-8 border-2 border-dashed border-gray-100 rounded-2xl text-center text-gray-400 font-bold uppercase text-xs italic">Nessun corso programmato.</div>
			{:else}
				<div class="grid grid-cols-1 md:grid-cols-2 xl:grid-cols-3 gap-6">
					{#each corsiProgrammati as corso (corso.idCorso)}
						<div class="bg-white rounded-2xl border border-gray-100 p-6 shadow-sm">
							<div class="flex justify-between mb-3">
								<span class="text-[9px] font-black px-2 py-0.5 bg-gray-100 rounded uppercase text-gray-600">Futuro</span>
							</div>
							<h3 class="font-black text-[#1B4B6B] uppercase text-sm mb-3">{corso.titolo}</h3>
							<p class="text-[10px] font-bold text-gray-500 flex items-center gap-1.5 mb-4"><Clock size={12}/> {formattaData(corso.dataOrario)}</p>
							<button onclick={() => preparaCambioStatoCorso(corso, StatoCorso.IN_SVOLGIMENTO)} class="w-full py-2.5 border-2 border-[#1B4B6B] text-[#1B4B6B] rounded-xl font-bold uppercase text-[10px] flex items-center justify-center gap-2 hover:bg-[#1B4B6B] hover:text-white transition-colors">
								<Play size={14} fill="currentColor" /> Inizia Corso
							</button>
						</div>
					{/each}
				</div>
			{/if}
		</div>
		<div class="mb-14 opacity-70 hover:opacity-100 transition-opacity">
			<h2 class="text-xl font-extrabold text-gray-500 uppercase mb-6 flex items-center gap-3 border-b pb-2"><BookOpen size={24}/> Archivio Storico</h2>
			{#if corsiArchiviati.length === 0}
				<p class="text-xs font-bold text-gray-300 uppercase italic">Nessun corso archiviato.</p>
			{:else}
				<div class="grid grid-cols-1 md:grid-cols-2 xl:grid-cols-4 gap-6">
					{#each corsiArchiviati as corso (corso.idCorso)}
						<div class="bg-gray-50/50 rounded-2xl border border-gray-200 p-5 flex flex-col justify-between">
							<h3 class="font-bold text-[#1B4B6B] text-xs uppercase line-clamp-1 mb-4">{corso.titolo}</h3>
							<button onclick={() => apriStatisticheFeedback(corso)} class="w-full py-2 bg-white text-[#1B4B6B] border border-[#1B4B6B]/20 rounded-xl font-bold uppercase text-[9px] flex items-center justify-center gap-2 hover:bg-[#1B4B6B]/5 transition-colors">
								<BarChart3 size={12} /> Analisi Feedback
							</button>
						</div>
					{/each}
				</div>
			{/if}
		</div>
	{/if}
</div>
{#if showCambioStatoModal}
	<div class="fixed inset-0 bg-[#1B4B6B]/40 backdrop-blur-sm flex items-center justify-center z-[130] p-4" transition:fade>
		<div class="bg-white rounded-3xl shadow-2xl w-full max-w-md overflow-hidden" in:scale>
			<div class="bg-[#1B4B6B] p-6 text-white flex justify-between items-center">
				<h2 class="text-lg font-black uppercase tracking-tighter flex items-center gap-2">Aggiornamento Corso</h2>
				<button onclick={() => (showCambioStatoModal = false)} class="text-white hover:rotate-90 transition-all duration-300"><X size={24}/></button>
			</div>
			<div class="p-8 text-center">
				{#if nuovoStatoPrevisto === StatoCorso.IN_SVOLGIMENTO}
					<div class="w-20 h-20 bg-blue-50 text-blue-600 rounded-full flex items-center justify-center mx-auto mb-6"><Play size={40}/></div>
					<h2 class="text-xl font-black text-[#1B4B6B] uppercase tracking-tighter mb-2">Iniziare la lezione?</h2>
					<p class="text-sm text-gray-500 mb-8">Il corso <span class="font-bold text-[#1B4B6B]">{corsoDaCambiareStato?.titolo}</span> passerà allo stato IN SVOLGIMENTO.</p>
				{:else if nuovoStatoPrevisto === StatoCorso.CONCLUSO}
					<div class="w-20 h-20 bg-emerald-50 text-emerald-600 rounded-full flex items-center justify-center mx-auto mb-6"><CheckCircle2 size={40}/></div>
					<h2 class="text-xl font-black text-[#1B4B6B] uppercase tracking-tighter mb-2">Concludere il corso?</h2>
					<p class="text-sm text-gray-500 mb-8">Il corso <span class="font-bold text-[#1B4B6B]">{corsoDaCambiareStato?.titolo}</span> passerà allo stato CONCLUSO.</p>
				{/if}
				<div class="flex flex-col gap-3">
					<button onclick={confermaCambioStatoCorso} disabled={isActionLoading} class="w-full bg-[#1B4B6B] text-white py-4 rounded-2xl font-black uppercase text-[10px] shadow-lg disabled:opacity-50 transition-all hover:bg-[#153a54] flex items-center justify-center gap-2">
						{#if isActionLoading}<Loader2 size={16} class="animate-spin" />{/if}
						Sì, Procedi
					</button>
					<button onclick={() => (showCambioStatoModal = false)} disabled={isActionLoading} class="w-full py-4 text-[10px] font-black uppercase text-gray-400 hover:text-gray-600 transition-colors">Annulla</button>
				</div>
			</div>
		</div>
	</div>
{/if}
{#if showModalFeedback && selectedCorso}
	<div class="fixed inset-0 z-[200] flex items-center justify-center p-4 bg-black/60 backdrop-blur-sm" transition:fade>
		<div class="bg-white w-full max-w-2xl rounded-[2.5rem] shadow-2xl flex flex-col max-h-[90vh] overflow-hidden" in:scale>
			<div class="bg-[#1B4B6B] p-8 text-white flex justify-between items-center">
				<div>
					<h2 class="text-xl font-black uppercase tracking-tighter flex items-center gap-3"><BarChart3 size={24}/> Qualità Didattica</h2>
					<p class="text-[10px] font-bold uppercase opacity-70 mt-1">{selectedCorso.titolo}</p>
				</div>
				<button onclick={() => showModalFeedback = false} class="text-white hover:rotate-90 transition-all duration-300"><X size={28} /></button>
			</div>
			<div class="p-8 overflow-y-auto custom-scrollbar flex-1 bg-gray-50/30">
				{#if isLoadingFeedback}
					<div class="py-20 text-center flex flex-col items-center gap-4">
						<Loader2 class="animate-spin text-[#1B4B6B]" size={48} />
						<span class="text-[10px] font-black uppercase tracking-widest text-gray-400">Analisi risposte in corso...</span>
					</div>
				{:else if statsFeedback}
					{#if statsFeedback.totaleFeedback === 0}
						<div class="py-20 text-center border-2 border-dashed border-gray-200 rounded-3xl bg-white">
							<Star size={48} class="mx-auto text-gray-200 mb-4" />
							<p class="text-xs font-bold text-gray-400 uppercase tracking-widest">Nessun feedback disponibile.</p>
						</div>
					{:else}
						<div class="space-y-8 mb-10">
							<div class="bg-white p-6 rounded-3xl border border-gray-100 shadow-sm">
								<div class="flex justify-between items-end mb-4">
									<div>
										<p class="text-[10px] font-black text-gray-400 uppercase tracking-widest mb-1">Qualità Docenza</p>
										<div class="flex items-center gap-2">
											<span class="text-4xl font-black text-[#1B4B6B]">{statsFeedback.mediaDocenza.toFixed(1)}</span>
											<div class="flex text-yellow-400"><Star size={16} fill="currentColor"/></div>
										</div>
									</div>
									<span class="text-[10px] font-bold text-gray-400 uppercase">{Math.round((statsFeedback.mediaDocenza / 5) * 100)}% gradimento</span>
								</div>
								<div class="w-full h-3 bg-gray-100 rounded-full overflow-hidden">
									<div class="h-full bg-[#1B4B6B] transition-all duration-1000" style="width: {(statsFeedback.mediaDocenza / 5) * 100}%"></div>
								</div>
							</div>
							<div class="bg-white p-6 rounded-3xl border border-gray-100 shadow-sm">
								<div class="flex justify-between items-end mb-4">
									<div>
										<p class="text-[10px] font-black text-gray-400 uppercase tracking-widest mb-1">Efficacia Contenuti</p>
										<div class="flex items-center gap-2">
											<span class="text-4xl font-black text-blue-800">{statsFeedback.mediaContenuti.toFixed(1)}</span>
											<div class="flex text-yellow-400"><Star size={16} fill="currentColor"/></div>
										</div>
									</div>
									<span class="text-[10px] font-bold text-gray-400 uppercase">{Math.round((statsFeedback.mediaContenuti / 5) * 100)}% gradimento</span>
								</div>
								<div class="w-full h-3 bg-gray-100 rounded-full overflow-hidden">
									<div class="h-full bg-blue-900 transition-all duration-1000" style="width: {(statsFeedback.mediaContenuti / 5) * 100}%"></div>
								</div>
							</div>
						</div>
						<div class="flex items-center justify-between mb-4">
							<h3 class="text-sm font-black text-[#1B4B6B] uppercase tracking-widest">Commenti Anonimi</h3>
							<span class="bg-[#1B4B6B]/10 text-[#1B4B6B] text-[10px] font-black px-4 py-1.5 rounded-full uppercase">
                                {statsFeedback.totaleFeedback} / {selectedCorso.numeroIscritti || 0} Risposte
                            </span>
						</div>
						<div class="space-y-3">
							{#each statsFeedback.commenti as commento}
								<div class="bg-white p-4 rounded-xl border border-gray-100 shadow-sm text-xs text-gray-600 italic leading-relaxed">
									"{commento}"
								</div>
							{/each}
						</div>
					{/if}
				{/if}
			</div>
			<div class="p-8 bg-white border-t border-gray-100 text-center">
				<button onclick={() => showModalFeedback = false} class="px-10 py-4 bg-gray-900 text-white font-black rounded-2xl uppercase text-[10px] tracking-widest hover:bg-black transition-all">Chiudi Analisi</button>
			</div>
		</div>
	</div>
{/if}
{#if showModalMateriale && selectedCorsoMateriale}
	<div class="fixed inset-0 z-[100] flex items-center justify-center p-4 bg-black/60 backdrop-blur-sm" in:fade>
		<div class="bg-white w-full max-w-xl rounded-3xl shadow-2xl flex flex-col max-h-[90vh] overflow-hidden" in:scale>
			<div class="bg-[#1B4B6B] p-6 text-white flex justify-between items-center rounded-t-3xl shrink-0">
				<h2 class="text-lg font-extrabold uppercase flex items-center gap-2"><UploadCloud size={20}/> Materiale Didattico</h2>
				<button onclick={() => showModalMateriale = false} class="text-white hover:rotate-90 transition-all duration-300"><X size={24} /></button>
			</div>
			<div class="p-8 overflow-y-auto custom-scrollbar flex-1 bg-gray-50/30">
				<div class="mb-10">
					<h3 class="text-[10px] font-black text-gray-400 uppercase tracking-widest mb-4">Documenti Caricati ({materialiCaricati.length})</h3>
					{#if isLoadingMateriali}
						<div class="py-6 text-center"><Loader2 class="animate-spin mx-auto text-[#1B4B6B]" size={24} /></div>
					{:else if materialiCaricati.length > 0}
						<div class="space-y-3">
							{#each materialiCaricati as mat (mat.idMateriale)}
								<div class="flex items-center justify-between p-4 bg-white border border-gray-100 rounded-2xl group hover:border-[#1B4B6B] transition-all shadow-sm">
									<div class="flex items-center gap-4">
										<div class="p-3 bg-[#1B4B6B]/10 text-[#1B4B6B] rounded-xl transition-colors">
											<FileText size={20} />
										</div>
										<div>
											<p class="text-xs font-extrabold text-[#1B4B6B] uppercase line-clamp-1">{mat.titoloDocumento}</p>
											<p class="text-[9px] font-bold text-gray-400 uppercase tracking-widest">{new Date(mat.dataCaricamento).toLocaleDateString()}</p>
										</div>
									</div>
									<button onclick={() => scaricaMateriale(mat.idMateriale, mat.titoloDocumento)} class="p-3 bg-gray-50 text-gray-400 rounded-xl hover:bg-[#1B4B6B] hover:text-white transition-all shadow-sm">
										<Download size={16} />
									</button>
								</div>
							{/each}
						</div>
					{:else}
						<div class="p-8 text-center border-2 border-dashed border-gray-200 rounded-2xl bg-white">
							<p class="text-[10px] font-bold text-gray-300 uppercase italic">Nessun file condiviso finora.</p>
						</div>
					{/if}
				</div>
				<div class="pt-8 border-t border-gray-200">
					<h3 class="text-[10px] font-black text-gray-400 uppercase tracking-widest mb-4">Nuovo Caricamento</h3>
					<div class="space-y-4">
						<input bind:value={titoloMateriale} type="text" placeholder="TITOLO DOCUMENTO (ES. SLIDE MODULO 1)" class="w-full p-4 bg-white border border-gray-200 rounded-2xl text-xs font-bold uppercase outline-none focus:ring-2 focus:ring-[#1B4B6B]/20 transition-all" />
						<div class="border-2 border-dashed border-gray-200 rounded-2xl p-10 text-center bg-white group hover:bg-gray-50 hover:border-[#1B4B6B]/30 transition-all relative cursor-pointer">
							<input type="file" accept=".pdf,.doc,.docx,.zip,.xls,.xlsx" onchange={(e) => fileMateriale = e.currentTarget.files?.[0] || null} class="absolute inset-0 w-full h-full opacity-0 cursor-pointer" />
							<div class="flex flex-col items-center gap-3 pointer-events-none">
								<div class="p-4 bg-gray-50 rounded-full text-[#1B4B6B] shadow-sm"><UploadCloud size={24} /></div>
								{#if fileMateriale}
									<span class="text-xs font-black text-[#1B4B6B] truncate w-[80%] mx-auto">{fileMateriale.name}</span>
									<span class="text-[9px] font-bold text-emerald-500 uppercase tracking-widest">Pronto all'invio</span>
								{:else}
									<span class="text-[10px] font-black text-gray-400 uppercase tracking-widest">Trascina o clicca per allegare file</span>
								{/if}
							</div>
						</div>
					</div>
				</div>
			</div>
			<div class="p-6 bg-white rounded-b-3xl border-t border-gray-100 flex gap-4 shrink-0">
				<button onclick={() => showModalMateriale = false} class="flex-1 py-4 text-gray-400 font-extrabold rounded-xl border border-gray-100 hover:bg-gray-50 uppercase text-[10px] transition-colors">Chiudi</button>
				<button onclick={caricaMaterialeDidattico} disabled={isUploadingMateriale || !fileMateriale || !titoloMateriale.trim()} class="flex-1 py-4 bg-[#1B4B6B] text-white font-extrabold rounded-xl hover:bg-[#153a54] uppercase text-[10px] shadow-lg shadow-blue-900/20 disabled:opacity-50 flex items-center justify-center gap-2 transition-all">
					{#if isUploadingMateriale} <Loader2 class="animate-spin" size={16} /> Attendi... {:else} <Send size={16} /> Invia File {/if}
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
				<button onclick={() => showModalFirma = false} class="text-white hover:rotate-90 transition-all duration-300"><X size={24} /></button>
			</div>
			<div class="p-6 overflow-y-auto custom-scrollbar flex-1 bg-gray-50/30">
				{#if isActionLoading}
					<div class="py-10 text-center"><Loader2 class="animate-spin mx-auto text-[#1B4B6B]" size={32} /></div>
				{:else}
					<div class="bg-[#1B4B6B]/5 border border-[#1B4B6B]/20 p-4 rounded-xl mb-6 flex gap-3">
						<BookOpen class="text-[#1B4B6B] shrink-0" size={20} />
						<p class="text-xs font-bold text-[#1B4B6B] uppercase">Verifica la lista dei dipendenti presenti. Apponi la firma per procedere al rilascio degli attestati.</p>
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
								<p class="text-xs font-bold text-gray-400 uppercase text-gray-300">Nessun dipendente validato.</p>
							</div>
						{/each}
					</div>
				{/if}
			</div>
			<div class="p-6 bg-white rounded-b-3xl border-t border-gray-100 flex gap-4">
				<button onclick={() => showModalFirma = false} disabled={isActionLoading} class="flex-1 py-4 text-gray-400 font-extrabold rounded-xl border border-gray-200 hover:bg-gray-50 uppercase text-[10px] transition-colors">Annulla</button>
				<button onclick={controfirmaRegistro} disabled={isActionLoading || iscrittiPresenti.length === 0} class="flex-1 py-4 bg-emerald-600 text-white font-extrabold rounded-xl hover:bg-emerald-700 uppercase text-[10px] flex items-center justify-center gap-2 shadow-lg shadow-emerald-900/20 transition-all">
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
</style>