<script lang="ts">
	import { onMount } from 'svelte';
	import { fade, scale } from 'svelte/transition';
	import { BookOpen, PlayCircle, Loader2, Search, Calendar, Download, MessageSquare, Send, CheckCircle2, X } from 'lucide-svelte';
	import type { IscrizioneCorso } from '$lib/models/IscrizioneCorso';
	import { AuthService } from '$lib/services/AuthService';
	import { FormazioneService } from '$lib/services/FormazioneService';
	import { FeedbackService } from '$lib/services/FeedbackService';
	import AlertCard from '$lib/Components/UI/AlertCard.svelte';

	interface MaterialeDidatticoDTO {
		idMateriale: number;
		titoloDocumento: string;
		percorsoFile?: string;
	}

	interface IscrizioneConMateriali extends IscrizioneCorso {
		materiali: MaterialeDidatticoDTO[];
		isLoadingMateriali: boolean;
		feedbackInviatoLocalmente?: boolean;
	}

	let isLoading = $state(true);
	let searchQuery = $state('');
	let iscrizioni = $state<IscrizioneConMateriali[]>([]);
	let currentUserId = $state<number | null>(null);

	let showModalFeedback = $state(false);
	let selectedCorsoPerFeedback = $state<IscrizioneConMateriali | null>(null);
	let ratingDocenza = $state(0);
	let ratingContenuti = $state(0);
	let commentoFeedback = $state('');
	let isSubmittingFeedback = $state(false);
	let feedbackSuccess = $state(false);

	onMount(async () => {
		const session = AuthService.getSession();
		if (!session) return;
		currentUserId = session.idUtente;

		try {
			const iscrizioniBase = await FormazioneService.getIscrizioniUtente(session.idUtente);
			iscrizioni = iscrizioniBase.map((isc: IscrizioneCorso) => ({
				...isc,
				materiali: [],
				isLoadingMateriali: true,
				feedbackInviatoLocalmente: false
			}));

			for (let i = 0; i < iscrizioni.length; i++) {
				const corsoId = iscrizioni[i].idCorso;
				const stato = iscrizioni[i].statoCorso;

				if (stato === 'PROGRAMMATO' || stato === 'IN_SVOLGIMENTO') {
					try {
						const materialiList = await FormazioneService.getMaterialiByCorso(corsoId);
						iscrizioni[i].materiali = materialiList;
					} catch (e) {
						console.warn(`Impossibile caricare materiali per il corso ${corsoId}`, e);
					} finally {
						iscrizioni[i].isLoadingMateriali = false;
					}
				} else {
					iscrizioni[i].isLoadingMateriali = false;
				}
			}
		} catch (error) {
			console.error('Errore nel recupero dei corsi:', error);
		} finally {
			isLoading = false;
		}
	});

	const iscrizioniAttive = $derived(
			iscrizioni.filter((i) => {
				const isOperativo = i.statoCorso === 'PROGRAMMATO' || i.statoCorso === 'IN_SVOLGIMENTO';
				const matchSearch = (i.titoloCorso || '').toLowerCase().includes(searchQuery.toLowerCase());
				return isOperativo && matchSearch;
			})
	);

	const corsiDaRecensire = $derived(
			iscrizioni.filter(i => (i.statoCorso === 'CONCLUSO' || i.statoCorso === 'ATTESA_FIRMA_DOCENTE' || i.statoCorso === 'VALIDATO' || i.statoCorso === 'CERTIFICATO') && i.presenzaConfermata === true && !i.feedbackInviatoLocalmente)
	);

	function formatData(isoString: string) {
		if (!isoString) return 'Data non definita';
		return new Date(isoString)
				.toLocaleDateString('it-IT', { day: '2-digit', month: 'long', hour: '2-digit', minute: '2-digit' })
				.toUpperCase();
	}

	async function scaricaMateriale(idMateriale: number, titolo: string) {
		try {
			const blob = await FormazioneService.downloadMateriale(idMateriale);
			const url = window.URL.createObjectURL(blob);
			const a = document.createElement('a');
			a.href = url;
			a.download = `${titolo.replace(/\s+/g, '_')}.pdf`;
			document.body.appendChild(a);
			a.click();
			window.URL.revokeObjectURL(url);
		} catch (error) {
			console.error('Errore download materiale:', error);
			alert("Impossibile scaricare il materiale al momento.");
		}
	}

	function apriModaleFeedback(iscrizione: IscrizioneConMateriali) {
		selectedCorsoPerFeedback = iscrizione;
		ratingDocenza = 0;
		ratingContenuti = 0;
		commentoFeedback = '';
		feedbackSuccess = false;
		showModalFeedback = true;
	}

	async function submitFeedback() {
		if (!currentUserId || !selectedCorsoPerFeedback || ratingDocenza === 0 || ratingContenuti === 0) {
			alert("Campi mancanti: controlla di aver inserito sia il rating docenza che contenuti.");
			return;
		}

		isSubmittingFeedback = true;
		try {
			const payload = {
				idUtente: currentUserId,
				idCorso: selectedCorsoPerFeedback.idCorso,
				ratingDocenza: ratingDocenza,
				ratingContenuti: ratingContenuti,
				commento: commentoFeedback.trim() === '' ? undefined : commentoFeedback.trim()
			};
			await FeedbackService.inviaFeedback(payload as any);

			const index = iscrizioni.findIndex(i => i.idCorso === selectedCorsoPerFeedback!.idCorso);
			if (index !== -1) {
				iscrizioni[index].feedbackInviatoLocalmente = true;
				feedbackSuccess = true;
				setTimeout(() => {
					showModalFeedback = false;
				}, 2000);
			}
		} catch (error: any) {
			console.error("Errore invio feedback:", error);
			const errorData = error.response?.data;
			const errorMsg = typeof errorData === 'string' ? errorData : "Si è verificato un errore durante l'invio del feedback.";
			alert(errorMsg);

			if (errorMsg.includes('già registrato')) {
				const index = iscrizioni.findIndex(i => i.idCorso === selectedCorsoPerFeedback!.idCorso);
				if (index !== -1) iscrizioni[index].feedbackInviatoLocalmente = true;
				showModalFeedback = false;
			}
		} finally {
			isSubmittingFeedback = false;
		}
	}
</script>

<div in:fade class="mx-auto max-w-7xl space-y-8 pb-10">
	<div class="flex flex-col items-start justify-between gap-6 md:flex-row md:items-end">
		<div>
			<h1 class="text-4xl font-black uppercase tracking-tighter text-[#1B4B6B]">I Miei Corsi</h1>
			<p class="mt-1 text-[10px] font-bold uppercase tracking-widest text-gray-400">
				Prossime lezioni e aule virtuali in programma
			</p>
		</div>
	</div>

	{#if corsiDaRecensire.length > 0}
		<div class="space-y-3" in:fade>
			{#each corsiDaRecensire as corsoRec (corsoRec.idCorso)}
				<button onclick={() => apriModaleFeedback(corsoRec)} class="w-full text-left transition-transform active:scale-[0.99]">
					<AlertCard
							titolo="Feedback Richiesto"
							sottotitolo={corsoRec.titoloCorso}
							variante="warning"
							icona={MessageSquare}
							stato="Pendente"
							data="Valuta ora il corso concluso"
					/>
				</button>
			{/each}
		</div>
	{/if}

	<div class="group relative w-full">
		<Search class="absolute left-4 top-1/2 -translate-y-1/2 text-gray-400 transition-colors group-focus-within:text-[#1B4B6B]" size={20} />
		<input bind:value={searchQuery} type="text" placeholder="CERCA CORSO IN PROGRAMMA..." class="w-full rounded-[1.5rem] border border-gray-100 bg-white py-4 pl-12 pr-6 text-xs font-bold uppercase shadow-sm outline-none transition-all focus:ring-4 focus:ring-[#1B4B6B]/5" />
	</div>

	{#if isLoading}
		<div class="flex flex-col items-center justify-center gap-4 py-32">
			<Loader2 size={48} class="animate-spin text-[#1B4B6B]"/>
			<span class="text-[10px] font-black uppercase tracking-widest text-gray-400">
                Sincronizzazione registro corsi...
            </span>
		</div>
	{:else}
		<div class="grid grid-cols-1 gap-8 lg:grid-cols-2">
			{#each iscrizioniAttive as iscrizione (iscrizione.idCorso)}
				<div in:scale={{ duration: 300 }} class="group flex flex-col overflow-hidden rounded-[2.5rem] border border-gray-100 bg-white shadow-sm transition-all hover:shadow-xl md:flex-row">
					<div class="flex w-full flex-col items-center justify-center border-b border-gray-50 bg-gray-50/30 p-6 md:w-32 md:border-b-0 md:border-r">
						<div class="mb-2 rounded-2xl bg-[#1B4B6B] p-4 text-white shadow-lg">
							<BookOpen size={28}/>
						</div>
						<span class="text-center text-[8px] font-black uppercase tracking-widest text-[#1B4B6B]">
                            {iscrizione.statoCorso === 'IN_SVOLGIMENTO' ? 'IN CORSO' : 'IN ARRIVO'}
                        </span>
					</div>

					<div class="flex flex-1 flex-col justify-between p-8">
						<div>
							<h3 class="mb-4 text-xl font-black uppercase leading-tight text-[#1B4B6B]">
								{iscrizione.titoloCorso}
							</h3>
							<div class="flex flex-wrap gap-4 mb-4">
								<div class="flex items-center gap-2 text-[10px] font-bold uppercase text-gray-400">
									<Calendar size={14} class="text-[#1B4B6B]"/>
									{formatData(iscrizione.dataOrarioCorso)}
								</div>
							</div>

							{#if iscrizione.isLoadingMateriali}
								<div class="flex items-center gap-2 text-[10px] font-bold uppercase text-gray-400">
									<Loader2 size={12} class="animate-spin"/> Controllo materiali...
								</div>
							{:else if iscrizione.materiali.length > 0}
								<div class="space-y-2 mt-4 pt-4 border-t border-gray-100">
									<p class="text-[9px] font-black uppercase tracking-widest text-gray-400 mb-2"> Materiali Didattici</p>
									{#each iscrizione.materiali as mat}
										<button onclick={() => scaricaMateriale(mat.idMateriale, mat.titoloDocumento)} class="w-full flex items-center justify-between p-3 rounded-xl border border-gray-200 hover:border-[#1B4B6B] hover:bg-blue-50 transition-colors group/mat">
											<div class="flex items-center gap-2 overflow-hidden">
												<PlayCircle size={14} class="text-[#1B4B6B] shrink-0"/>
												<span class="text-[10px] font-bold text-[#1B4B6B] uppercase truncate">{mat.titoloDocumento}</span>
											</div>
											<Download size={14} class="text-gray-400 group-hover/mat:text-[#1B4B6B]"/>
										</button>
									{/each}
								</div>
							{:else}
								<div class="mt-4 pt-4 border-t border-gray-100">
									<p class="text-[9px] font-bold uppercase tracking-widest text-gray-400">
										Nessun materiale caricato dal docente.
									</p>
								</div>
							{/if}
						</div>
					</div>
				</div>
			{/each}

			{#if iscrizioniAttive.length === 0 && !isLoading}
				<div class="col-span-1 rounded-[2.5rem] border border-dashed border-gray-200 bg-gray-50 py-20 text-center lg:col-span-2">
					<BookOpen size={48} class="mx-auto mb-4 text-gray-300"/>
					<h3 class="text-xl font-black uppercase italic text-[#1B4B6B]">Nessun corso in programma</h3>
					<p class="mt-2 text-[10px] font-bold uppercase text-gray-400">
						Hai completato tutte le tue attività formative o non ti è stato assegnato nulla.
					</p>
				</div>
			{/if}
		</div>
	{/if}
</div>

{#if showModalFeedback && selectedCorsoPerFeedback}
	<div class="fixed inset-0 bg-[#1B4B6B]/40 backdrop-blur-sm flex items-center justify-center z-[100] p-4"  transition:fade>
		<div class="bg-white rounded-3xl shadow-2xl w-full max-w-md overflow-hidden relative" in:scale>
			{#if feedbackSuccess}
				<div class="absolute inset-0 z-20 bg-white flex flex-col items-center justify-center p-8 text-center"  in:fade>
					<CheckCircle2 size={80} class="text-green-500 mb-4"/>
					<h2 class="text-2xl font-black text-[#1B4B6B] uppercase">Grazie!</h2>
					<p class="text-xs font-bold text-gray-500 uppercase mt-2">Il tuo feedback è stato salvato nei nostri sistemi.</p>
				</div>
			{/if}
			<div class="bg-purple-600 p-6 text-white flex justify-between items-center">
				<h2 class="text-lg font-black uppercase tracking-tighter flex items-center gap-2">
					<MessageSquare size={20}/> Valutazione Qualitativa
				</h2>
				<button onclick={() => showModalFeedback = false} class="hover:text-purple-300 transition-colors">
					<X size={24}/>
				</button>
			</div>
			<div class="p-8 space-y-6">
				<div>
					<p class="text-[10px] font-black uppercase text-purple-600 tracking-widest mb-1">Corso Terminato</p>
					<h3 class="text-[#1B4B6B] font-extrabold text-sm uppercase leading-tight">{selectedCorsoPerFeedback.titoloCorso}</h3>
				</div>
				<div class="space-y-2">
					<label class="block text-[10px] font-black text-gray-400 uppercase tracking-widest">Preparazione Docente *</label>
					<div class="flex gap-2 justify-between px-4">
						{#each [1, 2, 3, 4, 5] as star}
							<button type="button" onclick={() => ratingDocenza = star} class="text-4xl focus:outline-none transition-colors {ratingDocenza >= star ? 'text-yellow-400' : 'text-gray-200 hover:text-yellow-200'}">
								★
							</button>
						{/each}
					</div>
				</div>
				<div class="space-y-2 border-t border-gray-50 pt-4">
					<label class="block text-[10px] font-black text-gray-400 uppercase tracking-widest">Qualità dei Contenuti *</label>
					<div class="flex gap-2 justify-between px-4">
						{#each [1, 2, 3, 4, 5] as star}
							<button type="button" onclick={() => ratingContenuti = star} class="text-4xl focus:outline-none transition-colors {ratingContenuti >= star ? 'text-yellow-400' : 'text-gray-200 hover:text-yellow-200'}">
								★
							</button>
						{/each}
					</div>
				</div>
				<div class="space-y-1 border-t border-gray-50 pt-4">
					<label class="block text-[10px] font-black text-gray-400 uppercase tracking-widest">Note o Suggerimenti (Opzionale)</label>
					<textarea bind:value={commentoFeedback} rows="3" maxlength="1000" placeholder="Lascia un commento..." class="w-full p-3 bg-gray-50 border-none rounded-xl text-sm font-medium focus:ring-2 focus:ring-purple-600 outline-none resize-none"></textarea>
				</div>
			</div>
			<div class="p-6 bg-gray-50 flex justify-between items-center border-t border-gray-100">
				<button onclick={() => showModalFeedback = false} class="text-[10px] font-black uppercase text-gray-400 hover:text-gray-600 transition-colors">
					Annulla
				</button>
				<button onclick={submitFeedback} disabled={isSubmittingFeedback || ratingDocenza === 0 || ratingContenuti === 0} class="bg-purple-600 text-white px-8 py-3 rounded-xl text-[10px] font-black uppercase shadow-lg disabled:opacity-50 disabled:grayscale flex items-center gap-2 hover:bg-purple-700 transition-all">
					{#if isSubmittingFeedback}
						<Loader2 size={14} class="animate-spin"/>
					{:else}
						<Send size={14}/>
					{/if}
					Invia Valutazione
				</button>
			</div>
		</div>
	</div>
{/if}

<style>
	:global(body) { background-color: #f9fafb; }
</style>