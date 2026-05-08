<script lang="ts">
	import { onMount } from 'svelte';
	import { fade, scale } from 'svelte/transition';
	import { BookOpen, Loader2, Search, MessageSquare, Send, CheckCircle2, X } from 'lucide-svelte';
	import type { IscrizioneCorso } from '$lib/models/IscrizioneCorso';
	import { AuthService } from '$lib/services/AuthService';
	import { FormazioneService } from '$lib/services/FormazioneService';
	import { FeedbackService } from '$lib/services/FeedbackService';
	import AlertCard from '$lib/Components/UI/AlertCard.svelte';
	import DashboardCorsoCard from '$lib/Components/Features/Formazione/DashboardCorsoCard.svelte';
	import ModalCard from '$lib/Components/UI/ModalCard.svelte';

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
			iscrizioni.filter(
					(i) =>
							(i.statoCorso === 'CONCLUSO' ||
									i.statoCorso === 'ATTESA_FIRMA_DOCENTE' ||
									i.statoCorso === 'VALIDATO' ||
									i.statoCorso === 'CERTIFICATO') &&
							i.presenzaConfermata === true &&
							!i.feedbackInviatoLocalmente
			)
	);

	function formatDataSoloGiorno(isoString: string) {
		if (!isoString) return 'Data non definita';
		return new Date(isoString)
				.toLocaleDateString('it-IT', { day: '2-digit', month: 'long', year: 'numeric' })
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
			alert('Impossibile scaricare il materiale al momento.');
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
		if (
				!currentUserId ||
				!selectedCorsoPerFeedback ||
				ratingDocenza === 0 ||
				ratingContenuti === 0
		) {
			alert('Campi mancanti: controlla di aver inserito sia il rating docenza che contenuti.');
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

			const index = iscrizioni.findIndex((i) => i.idCorso === selectedCorsoPerFeedback!.idCorso);
			if (index !== -1) {
				iscrizioni[index].feedbackInviatoLocalmente = true;
				feedbackSuccess = true;
				setTimeout(() => {
					showModalFeedback = false;
				}, 2000);
			}
		} catch (error: any) {
			console.error('Errore invio feedback:', error);
			const errorData = error.response?.data;
			const errorMsg =
					typeof errorData === 'string'
							? errorData
							: "Si è verificato un errore durante l'invio del feedback.";
			alert(errorMsg);

			if (errorMsg.includes('già registrato')) {
				const index = iscrizioni.findIndex((i) => i.idCorso === selectedCorsoPerFeedback!.idCorso);
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
				<button
						onclick={() => apriModaleFeedback(corsoRec)}
						class="w-full text-left transition-transform active:scale-[0.99]"
				>
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
		<Search
				class="absolute left-4 top-1/2 -translate-y-1/2 text-gray-400 transition-colors group-focus-within:text-[#1B4B6B]"
				size={20}
		/>
		<input
				bind:value={searchQuery}
				type="text"
				placeholder="CERCA CORSO IN PROGRAMMA..."
				class="w-full rounded-[1.5rem] border border-gray-100 bg-white py-4 pl-12 pr-6 text-xs font-bold uppercase shadow-sm outline-none transition-all focus:ring-4 focus:ring-[#1B4B6B]/5"
		/>
	</div>

	{#if isLoading}
		<div class="flex flex-col items-center justify-center gap-4 py-32">
			<Loader2 size={48} class="animate-spin text-[#1B4B6B]" />
			<span class="text-[10px] font-black uppercase tracking-widest text-gray-400">
             Sincronizzazione registro corsi...
          </span>
		</div>
	{:else}
		<div class="grid grid-cols-1 gap-8 lg:grid-cols-3">
			{#each iscrizioniAttive as iscrizione (iscrizione.idCorso)}
				<div in:scale={{ duration: 300 }}>
					<DashboardCorsoCard
							ruolo="dipendente"
							corso={{
                      id: iscrizione.idCorso,
                      titolo: iscrizione.titoloCorso,
                      stato: iscrizione.statoCorso === 'IN_SVOLGIMENTO' ? 'IN_SVOLGIMENTO' : 'DA_INIZIARE',
                      dataSvolgimento: formatDataSoloGiorno(iscrizione.dataOrarioCorso),
                      luogo: 'Sede NorLan / Aula Virtuale',
                      materiali: iscrizione.materiali.map((m) => ({
                         id: m.idMateriale,
                         titolo: m.titoloDocumento
                      }))
                   }}
							onDownloadMateriale={(id, titolo) => scaricaMateriale(id, titolo)}
					/>
				</div>
			{/each}

			{#if iscrizioniAttive.length === 0 && !isLoading}
				<div
						class="col-span-1 py-20 text-center rounded-[2.5rem] border border-dashed border-gray-200 bg-gray-50 lg:col-span-3"
				>
					<BookOpen size={48} class="mx-auto mb-4 text-gray-300" />
					<h3 class="text-xl font-black uppercase italic text-[#1B4B6B]">
						Nessun corso in programma
					</h3>
					<p class="mt-2 text-[10px] font-bold uppercase text-gray-400">
						Hai completato tutte le tue attività formative o non ti è stato assegnato nulla.
					</p>
				</div>
			{/if}
		</div>
	{/if}
</div>

<ModalCard bind:isOpen={showModalFeedback} maxWidth="max-w-md">
	{#snippet title()}
		<MessageSquare size={20} /> <span class="font-black uppercase tracking-tighter">Valutazione Qualitativa</span>
	{/snippet}

	<div class="relative">
		{#if feedbackSuccess}
			<div
					class="absolute inset-0 z-20 flex flex-col items-center justify-center bg-white p-8 text-center rounded-b-3xl"
					in:fade
			>
				<CheckCircle2 size={80} class="mb-4 text-green-500" />
				<h2 class="text-2xl font-black uppercase text-[#1B4B6B]">Grazie!</h2>
				<p class="mt-2 text-xs font-bold uppercase text-gray-500">
					Il tuo feedback è stato salvato nei nostri sistemi.
				</p>
			</div>
		{/if}

		<div class="space-y-6 p-8">
			<div>
				<p class="mb-1 text-[10px] font-black uppercase tracking-widest text-[#1B4B6B]">
					Corso Terminato
				</p>
				<h3 class="text-sm font-extrabold uppercase leading-tight text-[#1B4B6B]">
					{selectedCorsoPerFeedback?.titoloCorso}
				</h3>
			</div>
			<div class="space-y-2">
				<label class="block text-[10px] font-black uppercase tracking-widest text-gray-400"
				>Preparazione Docente *</label>
				<div class="flex justify-between gap-2 px-4">
					{#each [1, 2, 3, 4, 5] as star}
						<button
								type="button"
								onclick={() => (ratingDocenza = star)}
								class="text-4xl transition-colors focus:outline-none {ratingDocenza >= star
                                ? 'text-yellow-400'
                                : 'text-gray-200 hover:text-yellow-200'}"
						>
							★
						</button>
					{/each}
				</div>
			</div>
			<div class="space-y-2 border-t border-gray-50 pt-4">
				<label class="block text-[10px] font-black uppercase tracking-widest text-gray-400"
				>Qualità dei Contenuti *</label>
				<div class="flex justify-between gap-2 px-4">
					{#each [1, 2, 3, 4, 5] as star}
						<button
								type="button"
								onclick={() => (ratingContenuti = star)}
								class="text-4xl transition-colors focus:outline-none {ratingContenuti >= star
                                ? 'text-yellow-400'
                                : 'text-gray-200 hover:text-yellow-200'}"
						>
							★
						</button>
					{/each}
				</div>
			</div>
			<div class="space-y-1 border-t border-gray-50 pt-4">
				<label class="block text-[10px] font-black uppercase tracking-widest text-gray-400"
				>Note o Suggerimenti (Opzionale)</label>
				<textarea
						bind:value={commentoFeedback}
						rows="3"
						maxlength="1000"
						placeholder="Lascia un commento..."
						class="w-full resize-none rounded-xl bg-gray-50 p-3 text-sm font-medium outline-none border-none focus:ring-2 focus:ring-[#1B4B6B]"
				></textarea>
			</div>
		</div>
	</div>

	{#snippet footer()}
		<button
				onclick={() => (showModalFeedback = false)}
				class="flex-1 py-3 text-[10px] font-black uppercase text-gray-400 hover:text-gray-600 transition-colors"
		>
			Annulla
		</button>
		<button
				onclick={submitFeedback}
				disabled={isSubmittingFeedback || ratingDocenza === 0 || ratingContenuti === 0}
				class="flex-1 flex items-center justify-center gap-2 rounded-xl bg-[#1B4B6B] px-8 py-3 text-[10px] font-black uppercase text-white shadow-lg transition-all hover:bg-[#153a54] disabled:grayscale disabled:opacity-50"
		>
			{#if isSubmittingFeedback}
				<Loader2 size={14} class="animate-spin" />
			{:else}
				<Send size={14} />
			{/if}
			Invia Valutazione
		</button>
	{/snippet}
</ModalCard>

<style>
	:global(body) {
		background-color: #f9fafb;
	}
</style>