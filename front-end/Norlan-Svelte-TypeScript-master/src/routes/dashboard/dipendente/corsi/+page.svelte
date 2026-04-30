<script lang="ts">
	import { onMount } from 'svelte';
	import { fade, scale } from 'svelte/transition';
	import {
		BookOpen,
		PlayCircle,
		Loader2,
		Search,
		Calendar,
		Download
	} from 'lucide-svelte';

	// IMPORT SERVIZI E MODELLI UFFICIALI
	import type { IscrizioneCorso } from '$lib/models/IscrizioneCorso';
	import type { MaterialeDidatticoDTO } from '$lib/models/MaterialeDidatticoDTO';
	import { AuthService } from '$lib/services/AuthService';
	import { FormazioneService } from '$lib/services/FormazioneService';

	// Estensione dell'interfaccia per gestire i materiali nel frontend
	interface IscrizioneConMateriali extends IscrizioneCorso {
		materiali: MaterialeDidatticoDTO[];
		isLoadingMateriali: boolean;
	}

	// --- STATO REATTIVO (Svelte 5) ---
	let isLoading = $state(true);
	let searchQuery = $state('');
	let iscrizioni = $state<IscrizioneConMateriali[]>([]);

	// --- CARICAMENTO DATI ---
	onMount(async () => {
		const session = AuthService.getSession();
		if (!session) return;

		try {
			// Recupero iscrizioni reali del dipendente loggato
			const iscrizioniBase = await FormazioneService.getIscrizioniUtente(session.idUtente);

			// Inizializziamo l'array con i campi extra per i materiali
			iscrizioni = iscrizioniBase.map((isc: IscrizioneCorso) => ({
				...isc,
				materiali: [],
				isLoadingMateriali: true
			}));

			// Recupero asincrono dei materiali per ogni corso attivo
			for (let i = 0; i < iscrizioni.length; i++) {
				const corsoId = iscrizioni[i].idCorso;
				try {
					const materialiList = await FormazioneService.getMaterialiByCorso(corsoId);
					// @ts-ignore - aggiriamo i controlli reattivi per l'aggiornamento diretto
					iscrizioni[i].materiali = materialiList;
				} catch (e) {
					console.warn(`Impossibile caricare materiali per il corso ${corsoId}`, e);
				} finally {
					// @ts-ignore
					iscrizioni[i].isLoadingMateriali = false;
				}
			}

		} catch (error) {
			console.error('Errore nel recupero dei corsi:', error);
		} finally {
			isLoading = false;
		}
	});

	// --- LOGICA REATTIVA (REGOLA 6) ---
	const iscrizioniAttive = $derived(
			iscrizioni.filter((i) => {
				// Il corso scompare non appena passa a CONCLUSO (o stati successivi).
				// Il dipendente non deve più farci nulla: scaricherà l'attestato dall'altra pagina.
				const isOperativo = i.statoCorso === 'PROGRAMMATO' || i.statoCorso === 'IN_SVOLGIMENTO';
				const matchSearch = (i.titoloCorso || '').toLowerCase().includes(searchQuery.toLowerCase());

				return isOperativo && matchSearch;
			})
	);

	function formatData(isoString: string) {
		if (!isoString) return 'Data non definita';
		return new Date(isoString)
				.toLocaleDateString('it-IT', {
					day: '2-digit',
					month: 'long',
					hour: '2-digit',
					minute: '2-digit'
				})
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

	<div class="group relative max-w-md">
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
		<div class="grid grid-cols-1 gap-8 lg:grid-cols-2">
			{#each iscrizioniAttive as iscrizione (iscrizione.idCorso)}
				<div
						in:scale={{ duration: 300 }}
						class="group flex flex-col overflow-hidden rounded-[2.5rem] border border-gray-100 bg-white shadow-sm transition-all hover:shadow-xl md:flex-row"
				>
					<div class="flex w-full flex-col items-center justify-center border-b border-gray-50 bg-gray-50/30 p-6 md:w-32 md:border-b-0 md:border-r">
						<div class="mb-2 rounded-2xl bg-[#1B4B6B] p-4 text-white shadow-lg">
							<BookOpen size={28} />
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
									<Calendar size={14} class="text-[#1B4B6B]" />
									{formatData(iscrizione.dataOrarioCorso)}
								</div>
							</div>

							<!-- AREA MATERIALI DIDATTICI -->
							{#if iscrizione.isLoadingMateriali}
								<div class="flex items-center gap-2 text-[10px] font-bold uppercase text-gray-400">
									<Loader2 size={12} class="animate-spin" /> Controllo materiali...
								</div>
							{:else if iscrizione.materiali.length > 0}
								<div class="space-y-2 mt-4 pt-4 border-t border-gray-100">
									<p class="text-[9px] font-black uppercase tracking-widest text-gray-400 mb-2">Materiali Didattici</p>
									{#each iscrizione.materiali as mat}
										<button
												onclick={() => scaricaMateriale(mat.idMateriale, mat.titoloDocumento)}
												class="w-full flex items-center justify-between p-3 rounded-xl border border-gray-200 hover:border-[#1B4B6B] hover:bg-blue-50 transition-colors group/mat"
										>
											<div class="flex items-center gap-2 overflow-hidden">
												<PlayCircle size={14} class="text-[#1B4B6B] shrink-0" />
												<span class="text-[10px] font-bold text-[#1B4B6B] uppercase truncate">{mat.titoloDocumento}</span>
											</div>
											<Download size={14} class="text-gray-400 group-hover/mat:text-[#1B4B6B]" />
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
					<BookOpen size={48} class="mx-auto mb-4 text-gray-300" />
					<h3 class="text-xl font-black uppercase italic text-[#1B4B6B]">Nessun corso in programma</h3>
					<p class="mt-2 text-[10px] font-bold uppercase text-gray-400">
						Hai completato tutte le tue attività formative o non ti è stato assegnato nulla.
					</p>
				</div>
			{/if}
		</div>
	{/if}
</div>

<style>
	:global(body) {
		background-color: #f9fafb;
	}
</style>