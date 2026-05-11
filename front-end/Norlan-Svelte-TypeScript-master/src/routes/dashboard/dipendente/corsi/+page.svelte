<script lang="ts">
	import { onMount } from 'svelte';
	import { fade, scale } from 'svelte/transition';
	import {
		Search, Loader2, CheckSquare, UploadCloud, CheckCircle2,
		Clock, BarChart3, Star, BookOpen, Play, AlertTriangle,
		Send, FileText, Download, Trash2, Calendar
	} from 'lucide-svelte';
	import type { CorsoFormazione } from '$lib/models/CorsoFormazione';
	import { StatoCorso } from '$lib/models/Enums';
	import type { IscrizioneCorso } from '$lib/models/IscrizioneCorso';
	import { AuthService } from '$lib/services/AuthService';
	import { FormazioneService } from '$lib/services/FormazioneService';
	import { FeedbackService } from '$lib/services/FeedbackService';
	import httpClient from '$lib/api/httpClient';
	import AlertCard from '$lib/Components/UI/AlertCard.svelte';
	import DashboardCorsoCard from '$lib/Components/Features/Formazione/DashboardCorsoCard.svelte';
	import ModalCard from '$lib/Components/UI/ModalCard.svelte';

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

	let showModalFirma = $state(false);
	let showModalMateriale = $state(false);
	let showModalFeedback = $state(false);
	let showCambioStatoModal = $state(false);
	let showModalEliminaMateriale = $state(false);

	let actionSuccess = $state<{type: 'OK' | 'ERR', msg: string} | null>(null);
	let corsoDaCambiareStato = $state<CorsoFormazioneEsteso | null>(null);
	let nuovoStatoPrevisto = $state<StatoCorso | null>(null);
	let isActionLoading = $state(false);
	let selectedCorso = $state<CorsoFormazioneEsteso | null>(null);
	let iscrittiPresenti = $state<IscrizioneCorso[]>([]);

	let isUploadingMateriale = $state(false);
	let selectedCorsoMateriale = $state<CorsoFormazione | null>(null);
	let fileMateriale = $state<File | null>(null);
	let titoloMateriale = $state('');
	let idMaterialeDaEliminare = $state<number | null>(null);

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
			corsi = mieiCorsi.map(c => ({ ...c, numeroIscritti: 0, isLoadingIscritti: true }));
			for (let i = 0; i < corsi.length; i++) {
				try {
					const iscritti = await FormazioneService.getIscrizioniByCorso(corsi[i].idCorso);
					corsi[i].numeroIscritti = iscritti.length;
				} catch{
					console.warn("Errore caricamento iscritti", corsi[i].idCorso);
				} finally {
					corsi[i].isLoadingIscritti = false;
				}
			}
		} catch (error) {
			console.error("Errore recupero corsi:", error);
		} finally {
			isLoading = false;
		}
	});

	const corsiAttiviDocente = $derived(corsi.filter(c => c.stato !== StatoCorso.VALIDATO && c.stato !== StatoCorso.CERTIFICATO));
	const corsiFiltrati = $derived(corsiAttiviDocente.filter(c => {
		return (c.titolo || '').toLowerCase().includes(queryRicerca.toLowerCase());
	}));

	const corsiDaFirmare = $derived(corsiFiltrati.filter(c => c.stato === StatoCorso.ATTESA_FIRMA_DOCENTE));
	const corsiConclusiAttesaAdmin = $derived(corsiFiltrati.filter(c => c.stato === StatoCorso.CONCLUSO));
	const corsiInSvolgimento = $derived(corsiFiltrati.filter(c => c.stato === StatoCorso.IN_SVOLGIMENTO));
	const corsiProgrammati = $derived(corsiFiltrati.filter(c => !c.stato || c.stato === StatoCorso.PROGRAMMATO));
	const corsiArchiviati = $derived(corsi.filter(c => (c.stato === StatoCorso.VALIDATO || c.stato === StatoCorso.CERTIFICATO) && (c.titolo || '').toLowerCase().includes(queryRicerca.toLowerCase())));

	function triggerBanner(msg: string, type: 'OK' | 'ERR' = 'OK') { actionSuccess = { type, msg }; setTimeout(() => actionSuccess = null, 4000); }

	async function apriStatisticheFeedback(corso: CorsoFormazioneEsteso) {
		selectedCorso = corso; showModalFeedback = true; isLoadingFeedback = true; statsFeedback = null;
		try { statsFeedback = await FeedbackService.getStatisticheCorso(corso.idCorso); }
		catch{ triggerBanner("Errore feedback", 'ERR'); showModalFeedback = false; }
		finally { isLoadingFeedback = false; }
	}

	function preparaCambioStatoCorso(corso: CorsoFormazioneEsteso, nuovoStato: StatoCorso) { corsoDaCambiareStato = corso; nuovoStatoPrevisto = nuovoStato; showCambioStatoModal = true; }

	async function confermaCambioStatoCorso() {
		if (!corsoDaCambiareStato || !nuovoStatoPrevisto) return;
		isActionLoading = true;
		try {
			await FormazioneService.updateStatoCorso(corsoDaCambiareStato.idCorso, nuovoStatoPrevisto);
			corsi = corsi.map(c => c.idCorso === corsoDaCambiareStato?.idCorso ? { ...c, stato: nuovoStatoPrevisto as StatoCorso } : c);
			showCambioStatoModal = false; triggerBanner("Stato aggiornato!");
		} catch { triggerBanner("Errore cambio stato", 'ERR'); }
		finally { isActionLoading = false; }
	}

	async function apriModaleMateriale(corso: CorsoFormazione) {
		selectedCorsoMateriale = corso; fileMateriale = null; titoloMateriale = ''; materialiCaricati = []; showModalMateriale = true; isLoadingMateriali = true;
		try { const res = await httpClient.get(`/api/formazione/corsi/${corso.idCorso}/materiali`); materialiCaricati = res.data; }
		catch { console.error("Errore materiali"); }
		finally { isLoadingMateriali = false; }
	}

	async function scaricaMateriale(idMateriale: number, path: string) {
		try {
			const blob = await FormazioneService.downloadMateriale(idMateriale);
			const nomeFile = path ? path.split('/').pop() : 'materiale.pdf';
			const url = window.URL.createObjectURL(blob);
			const link = document.createElement('a');
			link.href = url;
			link.download = nomeFile as string;
			document.body.appendChild(link);
			link.click();
			document.body.removeChild(link);
			window.URL.revokeObjectURL(url);
		} catch {
			triggerBanner("Impossibile scaricare.", 'ERR');
		}
	}

	function chiediConfermaEliminaMateriale(id: number) {
		idMaterialeDaEliminare = id;
		showModalEliminaMateriale = true;
	}

	async function confermaEliminaMateriale() {
		if (idMaterialeDaEliminare === null) return;
		isActionLoading = true;
		try {
			await httpClient.delete(`/api/formazione/materiali/${idMaterialeDaEliminare}`);
			triggerBanner("Materiale eliminato!");
			materialiCaricati = materialiCaricati.filter(m => m.idMateriale !== idMaterialeDaEliminare);
			showModalEliminaMateriale = false;
		} catch {
			triggerBanner("Errore eliminazione.", 'ERR');
		} finally {
			isActionLoading = false;
			idMaterialeDaEliminare = null;
		}
	}

	async function caricaMaterialeDidattico() {
		if (!selectedCorsoMateriale || !fileMateriale || !titoloMateriale.trim()) return;
		isUploadingMateriale = true;
		try {
			const formData = new FormData();
			formData.append('file', fileMateriale);
			formData.append('titoloDocumento', titoloMateriale);
			await FormazioneService.uploadMateriale(selectedCorsoMateriale.idCorso, formData);
			triggerBanner("Caricato!");
			const refresh = await httpClient.get(`/api/formazione/corsi/${selectedCorsoMateriale.idCorso}/materiali`);
			materialiCaricati = refresh.data;
			fileMateriale = null; titoloMateriale = '';
		} catch {
			triggerBanner("Errore upload", 'ERR');
		} finally {
			isUploadingMateriale = false;
		}
	}

	async function apriValidazioneRegistro(corso: CorsoFormazioneEsteso) {
		selectedCorso = corso; isActionLoading = true; showModalFirma = true;
		try {
			const iscritti = await FormazioneService.getIscrizioniByCorso(corso.idCorso);
			iscrittiPresenti = iscritti.filter((i: IscrizioneCorso) => i.presenzaConfermata);
		} catch {
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
			corsi = corsi.map(c => c.idCorso === selectedCorso!.idCorso ? { ...c, stato: StatoCorso.VALIDATO } : c);
			showModalFirma = false; triggerBanner("Registro firmato!");
		} catch { triggerBanner("Errore firma", 'ERR'); }
		finally { isActionLoading = false; }
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
		<div class="py-32 text-center">
			<Loader2 size={48} class="animate-spin text-[#1B4B6B]" />
		</div>
	{:else}
		{#if corsiDaFirmare.length > 0}
			<div class="mb-14">
				<h2 class="text-xl font-extrabold text-[#1B4B6B] uppercase mb-6 flex items-center gap-3"><CheckSquare size={24}/> Registri da Firmare</h2>
				<div class="grid grid-cols-1 md:grid-cols-2 xl:grid-cols-3 gap-6">
					{#each corsiDaFirmare as corso (corso.idCorso)}
						<DashboardCorsoCard
								ruolo="docente"
								corso={{ id: corso.idCorso, titolo: corso.titolo, stato: 'COMPLETATO', dataSvolgimento: formattaData(corso.dataOrario), luogo: 'Sede NorLan / Aula Virtuale' }}
								onFirmaRegistro={() => apriValidazioneRegistro(corso)}
								onAzioneCorso={() => apriStatisticheFeedback(corso)}
						/>
					{/each}
				</div>
			</div>
		{/if}

		{#if corsiConclusiAttesaAdmin.length > 0}
			<div class="mb-14">
				<h2 class="text-xl font-extrabold text-[#1B4B6B] uppercase mb-6 flex items-center gap-3"><Clock size={24}/> Attesa Admin</h2>
				<div class="grid grid-cols-1 md:grid-cols-2 xl:grid-cols-3 gap-6">
					{#each corsiConclusiAttesaAdmin as corso (corso.idCorso)}
						<div class="flex flex-col gap-3">
							<AlertCard titolo={corso.titolo} sottotitolo="..." variante="warning" icona={Clock} stato="" />
							<button onclick={() => apriStatisticheFeedback(corso)} class="w-full py-3 bg-[#1B4B6B]/5 text-[#1B4B6B] rounded-xl font-bold uppercase text-[10px] flex items-center justify-center gap-2 hover:bg-[#1B4B6B]/10 transition-colors"><BarChart3 size={14} /> Vedi Feedback</button>
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
				<div class="grid grid-cols-1 md:grid-cols-2 xl:grid-cols-3 gap-6">
					{#each corsiInSvolgimento as corso (corso.idCorso)}
						<div in:scale={{ duration: 300 }}>
							<DashboardCorsoCard
									ruolo="docente"
									corso={{
                                id: corso.idCorso,
                                titolo: corso.titolo,
                                stato: 'IN_SVOLGIMENTO',
                                dataSvolgimento: formattaData(corso.dataOrario),
                                luogo: 'Sede NorLan / Aula Virtuale',
                                numeroIscritti: corso.numeroIscritti
                            }}
									onAzioneCorso={() => apriModaleMateriale(corso)}
									onConcludiCorso={() => preparaCambioStatoCorso(corso, StatoCorso.CONCLUSO)}
							/>
						</div>
					{/each}
				</div>
			{/if}
		</div>

		<div class="mb-14">
			<h2 class="text-xl font-extrabold text-gray-700 uppercase mb-6 flex items-center gap-3"><Calendar size={24}/> Corsi Programmati</h2>
			{#if corsiProgrammati.length === 0}
				<div class="p-8 border-2 border-dashed border-gray-200 rounded-2xl text-center text-gray-400 font-bold uppercase text-xs italic">Nessun corso programmato.</div>
			{:else}
				<div class="grid grid-cols-1 md:grid-cols-2 xl:grid-cols-3 gap-6">
					{#each corsiProgrammati as corso (corso.idCorso)}
						<DashboardCorsoCard
								ruolo="docente"
								corso={{
                             id: corso.idCorso,
                             titolo: corso.titolo,
                             stato: 'DA_INIZIARE',
                             dataSvolgimento: formattaData(corso.dataOrario),
                             luogo: 'Sede NorLan / Aula Virtuale',
                             numeroIscritti: corso.numeroIscritti
                         }}
								onAzioneCorso={() => preparaCambioStatoCorso(corso, StatoCorso.IN_SVOLGIMENTO)}
						/>
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
						<DashboardCorsoCard
								ruolo="docente"
								corso={{ id: corso.idCorso, titolo: corso.titolo, stato: 'COMPLETATO', dataSvolgimento: formattaData(corso.dataOrario), luogo: 'Sede NorLan / Aula Virtuale' }}
								onAzioneCorso={() => apriStatisticheFeedback(corso)}
						/>
					{/each}
				</div>
			{/if}
		</div>
	{/if}
</div>

<ModalCard bind:isOpen={showCambioStatoModal} maxWidth="max-w-md">
	{#snippet title()}
		<span class="font-black uppercase tracking-tighter">Aggiornamento Corso</span>
	{/snippet}
	<div class="text-center py-4">
		{#if nuovoStatoPrevisto === StatoCorso.IN_SVOLGIMENTO}
			<div class="w-20 h-20 bg-blue-50 text-blue-600 rounded-full flex items-center justify-center mx-auto mb-6"><Play size={40}/></div>
			<h2 class="text-xl font-black text-[#1B4B6B] uppercase tracking-tighter mb-2">Iniziare la lezione?</h2>
			<p class="text-sm text-gray-500 mb-8">Il corso <span class="font-bold text-[#1B4B6B]">{corsoDaCambiareStato?.titolo}</span> passerà allo stato IN SVOLGIMENTO.</p>
		{:else if nuovoStatoPrevisto === StatoCorso.CONCLUSO}
			<div class="w-20 h-20 bg-emerald-50 text-emerald-600 rounded-full flex items-center justify-center mx-auto mb-6"><CheckCircle2 size={40}/></div>
			<h2 class="text-xl font-black text-[#1B4B6B] uppercase tracking-tighter mb-2">Concludere il corso?</h2>
			<p class="text-sm text-gray-500 mb-8">Il corso <span class="font-bold text-[#1B4B6B]">{corsoDaCambiareStato?.titolo}</span> passerà allo stato CONCLUSO.</p>
		{/if}
	</div>
	{#snippet footer()}
		<div class="flex flex-col gap-3 w-full">
			<button onclick={confermaCambioStatoCorso} disabled={isActionLoading} class="w-full bg-[#1B4B6B] text-white py-4 rounded-2xl font-black uppercase text-[10px] shadow-lg disabled:opacity-50 transition-all hover:bg-[#153a54] flex items-center justify-center gap-2">
				{#if isActionLoading}<Loader2 size={16} class="animate-spin" />{/if} Sì, Procedi
			</button>
			<button onclick={() => (showCambioStatoModal = false)} disabled={isActionLoading} class="w-full py-2 text-[10px] font-black uppercase text-gray-400 hover:text-gray-600 transition-colors">
				Annulla
			</button>
		</div>
	{/snippet}
</ModalCard>

<ModalCard bind:isOpen={showModalFeedback} maxWidth="max-w-2xl">
	{#snippet title()}
		<div class="flex items-center gap-3">
			<BarChart3 size={24}/>
			<div class="flex flex-col">
				<span class="font-black uppercase tracking-tighter text-lg leading-none">Qualità Didattica</span>
				<span class="text-[10px] font-bold uppercase opacity-70 mt-1">{selectedCorso?.titolo}</span>
			</div>
		</div>
	{/snippet}
	<div>
		{#if isLoadingFeedback}
			<div class="py-20 text-center flex flex-col items-center gap-4">
				<Loader2 class="animate-spin text-[#1B4B6B]" size={48} />
				<span class="text-[10px] font-black uppercase tracking-widest text-gray-400">Analisi risposte...</span>
			</div>
		{:else if statsFeedback}
			{#if statsFeedback.totaleFeedback === 0}
				<div class="py-20 text-center border-2 border-dashed border-gray-200 rounded-3xl bg-white">
					<Star size={48} class="mx-auto text-gray-200 mb-4" />
					<p class="text-xs font-bold text-gray-400 uppercase tracking-widest">Nessun feedback.</p>
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
					<span class="bg-[#1B4B6B]/10 text-[#1B4B6B] text-[10px] font-black px-4 py-1.5 rounded-full uppercase">{statsFeedback.totaleFeedback} / {selectedCorso?.numeroIscritti || 0} Risposte</span>
				</div>
				<div class="space-y-3">
					{#each statsFeedback.commenti as commento}
						<div class="bg-white p-4 rounded-xl border border-gray-100 shadow-sm text-xs text-gray-600 italic leading-relaxed">"{commento}"</div>
					{/each}
				</div>
			{/if}
		{/if}
	</div>
	{#snippet footer()}
		<button onclick={() => showModalFeedback = false} class="w-full py-4 bg-gray-900 text-white font-black rounded-2xl uppercase text-[10px] tracking-widest hover:bg-black transition-all">
			Chiudi Analisi
		</button>
	{/snippet}
</ModalCard>

<ModalCard bind:isOpen={showModalMateriale} maxWidth="max-w-xl">
	{#snippet title()}
		<UploadCloud size={20}/> <span class="font-black uppercase tracking-tighter">Materiale Didattico</span>
	{/snippet}
	<div class="space-y-8">
		<div>
			<h3 class="text-[10px] font-black text-gray-400 uppercase tracking-widest mb-4">Documenti Caricati ({materialiCaricati.length})</h3>
			{#if isLoadingMateriali}
				<div class="py-6 text-center"><Loader2 class="animate-spin mx-auto text-[#1B4B6B]" size={24} /></div>
			{:else if materialiCaricati.length > 0}
				<div class="space-y-3">
					{#each materialiCaricati as mat (mat.idMateriale)}
						<div class="flex items-center justify-between p-4 bg-white border border-gray-100 rounded-2xl group hover:border-[#1B4B6B] transition-all shadow-sm">
							<div class="flex items-center gap-4">
								<div class="p-3 bg-[#1B4B6B]/10 text-[#1B4B6B] rounded-xl transition-colors"><FileText size={20} /></div>
								<div>
									<p class="text-xs font-extrabold text-[#1B4B6B] uppercase line-clamp-1">{mat.titoloDocumento}</p>
									<p class="text-[9px] font-bold text-gray-400 uppercase tracking-widest">{new Date(mat.dataCaricamento).toLocaleDateString()}</p>
								</div>
							</div>
							<div class="flex items-center gap-2">
								<button onclick={() => scaricaMateriale(mat.idMateriale, mat.percorsoFile)} class="p-3 bg-gray-50 text-gray-400 rounded-xl hover:bg-[#1B4B6B] hover:text-white transition-all shadow-sm">
									<Download size={16} />
								</button>
								<button onclick={() => chiediConfermaEliminaMateriale(mat.idMateriale)} class="p-3 bg-red-50 text-red-500 rounded-xl hover:bg-red-600 hover:text-white transition-all shadow-sm">
									<Trash2 size={16} />
								</button>
							</div>
						</div>
					{/each}
				</div>
			{:else}
				<div class="p-8 text-center border-2 border-dashed border-gray-200 rounded-2xl bg-white"><p class="text-[10px] font-bold text-gray-300 uppercase italic">Nessun file.</p></div>
			{/if}
		</div>
		<div class="pt-8 border-t border-gray-200">
			<h3 class="text-[10px] font-black text-gray-400 uppercase tracking-widest mb-4">Nuovo Caricamento</h3>
			<div class="space-y-4">
				<input bind:value={titoloMateriale} type="text" placeholder="TITOLO DOCUMENTO..." class="w-full p-4 bg-white border border-gray-200 rounded-2xl text-xs font-bold uppercase outline-none focus:ring-2 focus:ring-[#1B4B6B]/20 transition-all" />
				<div class="border-2 border-dashed border-gray-200 rounded-2xl p-10 text-center bg-white group hover:bg-gray-50 hover:border-[#1B4B6B]/30 transition-all relative cursor-pointer">
					<input type="file" accept=".pdf,.doc,.docx,.zip,.xls,.xlsx" onchange={(e) => fileMateriale = e.currentTarget.files?.[0] || null} class="absolute inset-0 w-full h-full opacity-0 cursor-pointer" />
					<div class="flex flex-col items-center gap-3 pointer-events-none">
						<div class="p-4 bg-gray-50 rounded-full text-[#1B4B6B] shadow-sm"><UploadCloud size={24} /></div>
						{#if fileMateriale}
							<span class="text-xs font-black text-[#1B4B6B] truncate w-[80%] mx-auto">{fileMateriale.name}</span>
							<span class="text-[9px] font-bold text-emerald-500 uppercase tracking-widest">Pronto</span>
						{:else}
							<span class="text-[10px] font-black text-gray-400 uppercase tracking-widest">Allega file</span>
						{/if}
					</div>
				</div>
			</div>
		</div>
	</div>
	{#snippet footer()}
		<button onclick={() => showModalMateriale = false} class="flex-1 py-4 text-gray-400 font-extrabold rounded-xl border border-gray-100 hover:bg-gray-50 uppercase text-[10px] transition-colors">Chiudi</button>
		<button onclick={caricaMaterialeDidattico} disabled={isUploadingMateriale || !fileMateriale || !titoloMateriale.trim()} class="flex-1 py-4 bg-[#1B4B6B] text-white font-extrabold rounded-xl hover:bg-[#153a54] uppercase text-[10px] shadow-lg disabled:opacity-50 flex items-center justify-center gap-2 transition-all">
			{#if isUploadingMateriale} <Loader2 class="animate-spin" size={16} /> {:else} <Send size={16} /> Invia {/if}
		</button>
	{/snippet}
</ModalCard>

<ModalCard bind:isOpen={showModalFirma} maxWidth="max-w-2xl">
	{#snippet title()}
		<span class="font-black uppercase tracking-tighter">Firma Registro</span>
	{/snippet}
	<div class="space-y-4">
		{#if isActionLoading}
			<div class="py-10 text-center"><Loader2 class="animate-spin mx-auto text-[#1B4B6B]" size={32} /></div>
		{:else}
			<div class="bg-[#1B4B6B]/5 border border-[#1B4B6B]/20 p-4 rounded-xl mb-6 flex gap-3">
				<BookOpen class="text-[#1B4B6B] shrink-0" size={20} />
				<p class="text-xs font-bold text-[#1B4B6B] uppercase">Verifica i presenti e firma per gli attestati.</p>
			</div>
			<div class="space-y-2">
				{#each iscrittiPresenti as isc}
					<div class="flex items-center justify-between p-4 bg-white border border-gray-200 rounded-xl">
						<p class="text-sm font-extrabold text-[#1B4B6B] uppercase">{isc.emailUtente}</p>
					</div>
				{:else}
					<div class="p-6 text-center border-2 border-dashed border-gray-200 rounded-xl">Nessun presente.</div>
				{/each}
			</div>
		{/if}
	</div>
	{#snippet footer()}
		<button onclick={() => showModalFirma = false} disabled={isActionLoading} class="flex-1 py-4 text-gray-400 font-extrabold rounded-xl border border-gray-100 uppercase text-[10px]">Annulla</button>
		<button onclick={controfirmaRegistro} disabled={isActionLoading || iscrittiPresenti.length === 0} class="flex-1 py-4 bg-emerald-600 text-white font-extrabold rounded-xl uppercase text-[10px]">Apponi Firma</button>
	{/snippet}
</ModalCard>

<ModalCard bind:isOpen={showModalEliminaMateriale} maxWidth="max-w-md" headerClass="bg-red-600">
	{#snippet title()}
		<Trash2 size={20}/> <span class="font-black uppercase tracking-tighter">Eliminare il materiale?</span>
	{/snippet}
	<div class="text-center py-4">
		<div class="w-20 h-20 bg-red-50 text-red-600 rounded-full flex items-center justify-center mx-auto mb-6"><Trash2 size={40}/></div>
		<p class="text-sm text-gray-400 mb-8 uppercase font-bold">Questa azione è definitiva.</p>
	</div>
	{#snippet footer()}
		<div class="flex flex-col w-full gap-3">
			<button onclick={confermaEliminaMateriale} class="w-full bg-red-600 text-white py-4 rounded-2xl font-black uppercase text-[10px] shadow-lg transition-all hover:bg-red-700">Sì, elimina</button>
			<button onclick={() => { showModalEliminaMateriale = false; }} class="w-full py-4 text-[10px] font-black uppercase text-gray-400 hover:text-gray-600">No, annulla</button>
		</div>
	{/snippet}
</ModalCard>

<style>
	:global(body) { background-color: #F9FAFB; }
</style>