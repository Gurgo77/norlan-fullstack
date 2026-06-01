<script lang="ts">
	import { onMount } from 'svelte';
	import { fade, scale } from 'svelte/transition';
	import {
		Search, Loader2, BookOpen, Play, Calendar, CheckCircle2,
		Download, Star, FileText, AlertTriangle, Send, Info
	} from 'lucide-svelte';
	import { AuthService } from '$lib/services/AuthService';
	import { FormazioneService } from '$lib/services/FormazioneService';
	import { FeedbackService } from '$lib/services/FeedbackService';
	import type { CorsoFormazione } from '$lib/models/CorsoFormazione';
	import { StatoCorso } from '$lib/models/Enums';
	import httpClient from '$lib/api/httpClient';
	import DashboardCorsoCard from '$lib/Components/Features/Formazione/DashboardCorsoCard.svelte';
	import ModalCard from '$lib/Components/UI/ModalCard.svelte';

	// Stato locale
	let isLoading = $state(true);
	let mieiCorsi = $state<CorsoFormazione[]>([]);
	let queryRicerca = $state('');

	// Stato modali
	let showModalMateriale = $state(false);
	let showModalFeedback = $state(false);

	// Variabili per Materiale
	let materialiCaricati = $state<any[]>([]);
	let isLoadingMateriali = $state(false);

	// Variabili per Feedback
	let isSubmittingFeedback = $state(false);
	let actionSuccess = $state<{type: 'OK' | 'ERR', msg: string} | null>(null);

	let feedbackData = $state({
		idCorso: 0,
		idUtente: 0,
		ratingDocenza: 5,
		ratingContenuti: 5,
		commento: ''
	});

	onMount(async () => {
		const session = AuthService.getSession();
		if (!session) return;
		try {
			const iscrizioni = await FormazioneService.getIscrizioniUtente(session.idUtente);

			mieiCorsi = iscrizioni.map(isc => ({
				idCorso: isc.idCorso,
				titolo: isc.titoloCorso,
				dataOrario: isc.dataOrarioCorso,
				stato: isc.statoCorso || StatoCorso.PROGRAMMATO,
				luogoFisico: 'Sede NorLan / Aula Virtuale',
				capacitaMassima: 0,
				idDocente: 0,
				emailDocente: ''
			})) as CorsoFormazione[];

		} catch (error) {
			console.error("Errore recupero dei miei corsi:", error);
		} finally {
			isLoading = false;
		}
	});

	const corsiFiltrati = $derived(mieiCorsi.filter(c => {
		return (c.titolo || '').toLowerCase().includes(queryRicerca.toLowerCase());
	}));

	const corsiProgrammati = $derived(corsiFiltrati.filter(c => !c.stato || c.stato === StatoCorso.PROGRAMMATO));
	const corsiInSvolgimento = $derived(corsiFiltrati.filter(c => c.stato === StatoCorso.IN_SVOLGIMENTO));
	const corsiCompletati = $derived(corsiFiltrati.filter(c =>
			c.stato === StatoCorso.CONCLUSO ||
			c.stato === StatoCorso.ATTESA_FIRMA_DOCENTE ||
			c.stato === StatoCorso.VALIDATO ||
			c.stato === StatoCorso.CERTIFICATO
	));

	function triggerBanner(msg: string, type: 'OK' | 'ERR' = 'OK') {
		actionSuccess = { type, msg };
		setTimeout(() => actionSuccess = null, 4000);
	}

	// GESTIONE MATERIALE DIDATTICO
	async function apriModaleMateriale(idCorso: number) {
		materialiCaricati = [];
		showModalMateriale = true;
		isLoadingMateriali = true;
		try {
			const res = await httpClient.get(`/api/formazione/corsi/${idCorso}/materiali`);
			materialiCaricati = res.data;
		} catch {
			console.error("Errore caricamento materiali");
		} finally {
			isLoadingMateriali = false;
		}
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
			triggerBanner("Impossibile scaricare il file.", 'ERR');
		}
	}

	// GESTIONE FEEDBACK
	function apriLasciaFeedback(idCorso: number) {
		const session = AuthService.getSession();
		if (!session) return;
		feedbackData = {
			idCorso: idCorso,
			idUtente: session.idUtente,
			ratingDocenza: 5,
			ratingContenuti: 5,
			commento: ''
		};
		showModalFeedback = true;
	}

	async function confermaFeedback() {
		isSubmittingFeedback = true;
		try {
			await FeedbackService.inviaFeedback(feedbackData as any);
			triggerBanner("Feedback inviato con successo! Grazie.");
			showModalFeedback = false;
		} catch (error: any) {
			const msg = typeof error.response?.data === 'string' ? error.response.data : "Errore durante l'invio del feedback.";
			triggerBanner(msg, 'ERR');
		} finally {
			isSubmittingFeedback = false;
		}
	}

	// GESTIONE ATTESTATO: Usa il metodo reale del backend, e gestisce lo stato se non è pronto
	async function scaricaAttestato(corso: CorsoFormazione) {
		if (corso.stato !== StatoCorso.CERTIFICATO) {
			triggerBanner("Attestato non ancora caricato", "ERR");
			return;
		}
		try {
			const session = AuthService.getSession();
			triggerBanner("Download in corso...", "OK");
			const blob = await FormazioneService.downloadAttestato(corso.idCorso, session!.idUtente);
			const url = window.URL.createObjectURL(blob);
			const link = document.createElement('a');
			link.href = url;
			link.download = `Attestato_${corso.titolo.replace(/\s+/g, '_')}.pdf`;
			document.body.appendChild(link);
			link.click();
			document.body.removeChild(link);
			window.URL.revokeObjectURL(url);
		} catch {
			triggerBanner("Impossibile scaricare l'attestato dal server.", "ERR");
		}
	}

	function formattaData(iso: string) {
		if (!iso) return 'N.D.';
		return new Date(iso).toLocaleDateString('it-IT', { day: '2-digit', month: 'short', year: 'numeric', hour: '2-digit', minute:'2-digit' });
	}
</script>

<div in:fade class="pb-20 max-w-7xl mx-auto p-4 md:p-6">
	{#if actionSuccess}
		<div class="fixed top-24 right-4 md:right-8 z-[250] {actionSuccess.type === 'OK' ? 'bg-emerald-600' : 'bg-red-600'} text-white px-6 py-4 rounded-2xl shadow-2xl flex items-center gap-4 border border-white/20 transition-all" in:scale out:fade>
			{#if actionSuccess.type === 'OK'}<CheckCircle2 size={24} />{:else}<AlertTriangle size={24} />{/if}
			<p class="text-sm font-black uppercase tracking-tight">{actionSuccess.msg}</p>
		</div>
	{/if}

	<div class="mb-8 md:mb-10">
		<h1 class="text-3xl md:text-4xl font-black text-[#1B4B6B] uppercase tracking-tighter">La Mia Formazione</h1>
		<p class="text-gray-400 font-bold uppercase text-[9px] md:text-[10px] tracking-widest mt-1">Consulta i tuoi corsi e scarica il materiale</p>
	</div>

	<div class="bg-white p-4 rounded-2xl md:rounded-3xl shadow-sm border border-gray-100 flex flex-col lg:flex-row gap-4 mb-10">
		<div class="relative flex-1">
			<Search class="absolute left-4 top-1/2 -translate-y-1/2 text-gray-400" size={20} />
			<input bind:value={queryRicerca} type="text" placeholder="CERCA UN CORSO..." class="w-full bg-gray-50 border-none rounded-xl md:rounded-2xl py-3 md:py-4 pl-12 pr-6 text-xs font-bold uppercase outline-none focus:ring-4 focus:ring-[#1B4B6B]/5" />
		</div>
	</div>

	{#if isLoading}
		<div class="py-32 text-center">
			<Loader2 size={48} class="animate-spin text-[#1B4B6B] mx-auto" />
			<p class="text-[10px] font-black uppercase tracking-widest text-gray-400 mt-4">Caricamento corsi...</p>
		</div>
	{:else}

		{#if corsiInSvolgimento.length > 0}
			<div class="mb-14">
				<h2 class="text-lg md:text-xl font-extrabold text-blue-700 uppercase mb-6 flex items-center gap-3"><Play size={24} class="shrink-0"/> In Svolgimento</h2>
				<div class="grid grid-cols-1 md:grid-cols-2 xl:grid-cols-3 gap-6">
					{#each corsiInSvolgimento as corso (corso.idCorso)}
						<div class="flex flex-col gap-3">
							<DashboardCorsoCard
									ruolo="dipendente"
									corso={{ id: corso.idCorso, titolo: corso.titolo, stato: 'IN_SVOLGIMENTO', dataSvolgimento: formattaData(corso.dataOrario), luogo: corso.luogoFisico || 'Aula Virtuale / Sede' }}
							/>
							<button onclick={() => apriModaleMateriale(corso.idCorso)} class="w-full py-2.5 bg-blue-50 border border-blue-100 text-blue-600 rounded-xl font-extrabold uppercase text-[10px] flex items-center justify-center gap-2 hover:bg-blue-100 transition-colors shadow-sm">
								<BookOpen size={14} /> Materiale di Studio
							</button>
						</div>
					{/each}
				</div>
			</div>
		{/if}

		<div class="mb-14">
			<h2 class="text-lg md:text-xl font-extrabold text-gray-700 uppercase mb-6 flex items-center gap-3"><Calendar size={24} class="shrink-0"/> Prossimi Corsi</h2>
			{#if corsiProgrammati.length === 0}
				<div class="p-8 border-2 border-dashed border-gray-200 rounded-2xl text-center text-gray-400 font-bold uppercase text-xs italic bg-gray-50/50">Nessun corso in programma al momento.</div>
			{:else}
				<div class="grid grid-cols-1 md:grid-cols-2 xl:grid-cols-3 gap-6">
					{#each corsiProgrammati as corso (corso.idCorso)}
						<div class="flex flex-col gap-3">
							<DashboardCorsoCard
									ruolo="dipendente"
									corso={{ id: corso.idCorso, titolo: corso.titolo, stato: 'DA_INIZIARE', dataSvolgimento: formattaData(corso.dataOrario), luogo: corso.luogoFisico || 'Aula Virtuale / Sede' }}
							/>
							<button onclick={() => apriModaleMateriale(corso.idCorso)} class="w-full py-2.5 bg-gray-50 border border-gray-200 text-gray-600 rounded-xl font-extrabold uppercase text-[10px] flex items-center justify-center gap-2 hover:bg-gray-100 transition-colors shadow-sm">
								<BookOpen size={14} /> Materiale di Studio
							</button>
						</div>
					{/each}
				</div>
			{/if}
		</div>

		<div class="mb-14 opacity-90 hover:opacity-100 transition-opacity">
			<h2 class="text-lg md:text-xl font-extrabold text-gray-500 uppercase mb-6 flex items-center gap-3 border-b pb-2"><CheckCircle2 size={24} class="shrink-0"/> Corsi Completati</h2>
			{#if corsiCompletati.length === 0}
				<p class="text-xs font-bold text-gray-300 uppercase italic">Nessun corso completato in archivio.</p>
			{:else}
				<div class="grid grid-cols-1 md:grid-cols-2 xl:grid-cols-4 gap-6">
					{#each corsiCompletati as corso (corso.idCorso)}
						<div class="flex flex-col gap-3 bg-white p-2 rounded-3xl border border-gray-100 shadow-sm">
							<!-- Usa il bottone Scarica Attestato NATIVO della DashboardCorsoCard e ci aggancia la nostra funzione -->
							<DashboardCorsoCard
									ruolo="dipendente"
									corso={{ id: corso.idCorso, titolo: corso.titolo, stato: 'COMPLETATO', dataSvolgimento: formattaData(corso.dataOrario), luogo: corso.luogoFisico || 'Aula Virtuale / Sede' }}
									onDownloadAttestato={() => scaricaAttestato(corso)}
							/>

							<div class="flex gap-2 px-2 pb-2">
								<button onclick={() => apriModaleMateriale(corso.idCorso)} class="flex-1 py-2 bg-gray-50 border border-gray-200 text-gray-600 rounded-xl font-extrabold uppercase text-[9px] flex items-center justify-center gap-1 hover:bg-gray-100 transition-colors">
									<BookOpen size={12} /> Materiale
								</button>
								<button onclick={() => apriLasciaFeedback(corso.idCorso)} class="flex-1 py-2 bg-yellow-50 border border-yellow-200 text-yellow-600 rounded-xl font-extrabold uppercase text-[9px] flex items-center justify-center gap-1 hover:bg-yellow-100 transition-colors">
									<Star size={12} fill="currentColor"/> Feedback
								</button>
							</div>
						</div>
					{/each}
				</div>
			{/if}
		</div>
	{/if}
</div>

<ModalCard bind:isOpen={showModalMateriale} maxWidth="max-w-xl">
	{#snippet title()}
		<BookOpen size={20} class="shrink-0 text-[#1B4B6B]"/> <span class="font-black uppercase tracking-tighter">Materiale di Studio</span>
	{/snippet}
	<div class="space-y-4 max-h-[60vh] overflow-y-auto px-1 py-2">
		{#if isLoadingMateriali}
			<div class="py-10 text-center"><Loader2 class="animate-spin mx-auto text-[#1B4B6B]" size={32} /></div>
		{:else if materialiCaricati.length > 0}
			<div class="space-y-3">
				{#each materialiCaricati as mat (mat.idMateriale)}
					<div class="flex items-center justify-between p-4 bg-white border border-gray-100 rounded-2xl hover:border-[#1B4B6B] transition-all shadow-sm gap-4">
						<div class="flex items-center gap-4 min-w-0">
							<div class="p-3 bg-[#1B4B6B]/10 text-[#1B4B6B] rounded-xl shrink-0"><FileText size={20} /></div>
							<div class="min-w-0">
								<p class="text-xs font-extrabold text-[#1B4B6B] uppercase truncate block">{mat.titoloDocumento}</p>
								<p class="text-[9px] font-bold text-gray-400 uppercase tracking-widest">{new Date(mat.dataCaricamento).toLocaleDateString()}</p>
							</div>
						</div>
						<button onclick={() => scaricaMateriale(mat.idMateriale, mat.percorsoFile)} class="p-3 bg-[#1B4B6B]/10 text-[#1B4B6B] rounded-xl hover:bg-[#1B4B6B] hover:text-white transition-all shadow-sm shrink-0">
							<Download size={16} />
						</button>
					</div>
				{/each}
			</div>
		{:else}
			<div class="p-8 text-center border-2 border-dashed border-gray-200 rounded-2xl bg-gray-50 flex flex-col items-center gap-3">
				<Info class="text-gray-300" size={32}/>
				<p class="text-[10px] font-bold text-gray-400 uppercase tracking-widest">Il docente non ha ancora caricato materiali per questo corso.</p>
			</div>
		{/if}
	</div>
	{#snippet footer()}
		<button onclick={() => showModalMateriale = false} class="w-full py-4 text-gray-400 font-extrabold rounded-xl border border-gray-100 hover:bg-gray-50 uppercase text-[10px] transition-colors">Chiudi</button>
	{/snippet}
</ModalCard>

<ModalCard bind:isOpen={showModalFeedback} maxWidth="max-w-md">
	{#snippet title()}
		<Star size={20} class="shrink-0 text-yellow-500"/> <span class="font-black uppercase tracking-tighter">Valuta il Corso</span>
	{/snippet}
	<div class="space-y-6 px-2 py-4">
		<div>
			<label class="text-[10px] font-black text-gray-400 uppercase tracking-widest block mb-3 text-center">Valutazione del Docente</label>
			<input type="range" min="1" max="5" bind:value={feedbackData.ratingDocenza} class="w-full h-2 bg-gray-200 rounded-lg appearance-none cursor-pointer" />
			<div class="text-center text-3xl font-black text-[#1B4B6B] mt-2">{feedbackData.ratingDocenza} <span class="text-base text-gray-300">/ 5</span></div>
		</div>
		<div class="pt-4 border-t border-gray-100">
			<label class="text-[10px] font-black text-gray-400 uppercase tracking-widest block mb-3 text-center">Valutazione dei Contenuti</label>
			<input type="range" min="1" max="5" bind:value={feedbackData.ratingContenuti} class="w-full h-2 bg-gray-200 rounded-lg appearance-none cursor-pointer" />
			<div class="text-center text-3xl font-black text-blue-800 mt-2">{feedbackData.ratingContenuti} <span class="text-base text-gray-300">/ 5</span></div>
		</div>
		<div class="pt-4 border-t border-gray-100">
			<label class="text-[10px] font-black text-gray-400 uppercase tracking-widest block mb-2">Lascia un commento anonimo (Opzionale)</label>
			<textarea bind:value={feedbackData.commento} placeholder="Raccontaci la tua esperienza formativa..." class="w-full p-4 bg-gray-50 border border-gray-200 rounded-xl text-xs font-bold resize-none outline-none focus:ring-2 focus:ring-[#1B4B6B]/20 transition-all" rows="4"></textarea>
		</div>
	</div>
	{#snippet footer()}
		<div class="flex w-full gap-3">
			<button onclick={() => showModalFeedback = false} class="flex-1 py-4 text-gray-400 font-extrabold rounded-xl border border-gray-100 hover:bg-gray-50 uppercase text-[10px] transition-colors">Annulla</button>
			<button onclick={confermaFeedback} disabled={isSubmittingFeedback} class="flex-1 py-4 bg-[#1B4B6B] text-white font-extrabold rounded-xl hover:bg-[#153a54] uppercase text-[10px] shadow-lg disabled:opacity-50 flex items-center justify-center gap-2 transition-all">
				{#if isSubmittingFeedback} <Loader2 class="animate-spin" size={16} /> {:else} <Send size={16} /> Invia Valutazione {/if}
			</button>
		</div>
	{/snippet}
</ModalCard>

<style>
	:global(body) { background-color: #F9FAFB; }
</style>